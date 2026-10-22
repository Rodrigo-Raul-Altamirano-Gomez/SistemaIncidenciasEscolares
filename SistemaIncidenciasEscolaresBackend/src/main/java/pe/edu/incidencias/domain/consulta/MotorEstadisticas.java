package pe.edu.incidencias.domain.consulta;

import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.alerta.*;
import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.estudiante.*;
import pe.edu.incidencias.domain.incidencia.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static pe.edu.incidencias.domain.consulta.ConsultaEstadistica.Dimension.*;
import static pe.edu.incidencias.domain.consulta.ConsultaEstadistica.Medida.*;
import static pe.edu.incidencias.domain.consulta.ResultadoEstadistico.NO_INFORMADO;

/** Motor puro sobre un ámbito cargado por aplicación. La completitud del ámbito no se deduce de una muestra. */
public final class MotorEstadisticas {
    private record Unidad(String id, Optional<LocalDate> fecha, Map<ConsultaEstadistica.Dimension, String> datos,
                          Optional<UUID> estudianteId, ContextoAcceso contexto) { }
    public record Comparacion(ResultadoEstadistico anterior, ResultadoEstadistico actual, long diferencia) { }
    public record Poblacion(int anioEscolar, LocalDate fechaReferencia, Optional<String> grado,
                            Optional<String> seccion, Optional<String> sexo, String coberturaDeclarada) {
        public Poblacion {
            Objects.requireNonNull(fechaReferencia);
            Objects.requireNonNull(grado); Objects.requireNonNull(seccion); Objects.requireNonNull(sexo);
            if (anioEscolar < 1 || fechaReferencia.getYear() != anioEscolar)
                throw new IllegalArgumentException("Referencia anual incoherente");
            if (coberturaDeclarada == null || coberturaDeclarada.isBlank())
                throw new IllegalArgumentException("Debe declararse cobertura de nómina, incluso si es parcial");
            if (java.util.stream.Stream.of(grado, seccion, sexo).flatMap(Optional::stream).anyMatch(String::isBlank))
                throw new IllegalArgumentException("Filtro poblacional vacío");
        }
    }
    public record Proporcion(long numerador, long denominador, Optional<BigDecimal> valor,
                              String unidad, Poblacion poblacion, ConsultaEstadistica.Periodo periodo,
                              long sinGrupo, long sinSexo, Optional<String> motivoNoCalculable) { }

    private final RegistroAtenciones atenciones;
    private final RegistroAlertas alertas;
    private final RegistroMatriculas matriculas;
    private final PoliticaAcceso politica;
    private final Set<UUID> universo;

    /** IDs de hechos cargados, no cifras aportadas por cliente. Se resuelven versiones y duplicidades en el registro. */
    public MotorEstadisticas(RegistroAtenciones atenciones, RegistroAlertas alertas, RegistroMatriculas matriculas,
                            Set<UUID> hechosCargados, PoliticaAcceso politicaActual) {
        this.atenciones = Objects.requireNonNull(atenciones);
        this.alertas = Objects.requireNonNull(alertas);
        this.matriculas = Objects.requireNonNull(matriculas);
        this.politica = Objects.requireNonNull(politicaActual);
        this.universo = hechosCargados.stream().map(atenciones::hechoVigente).map(Hecho::id).collect(Collectors.toUnmodifiableSet());
        for (var caso : atenciones.casos()) if (!universo.containsAll(caso.hechosReservados().stream()
                .map(atenciones::hechoVigente).map(Hecho::id).toList()))
            throw new IllegalArgumentException("El universo debe incluir los hechos de las atenciones cargadas");
        for (var alerta : alertas.alertas()) if (!universo.containsAll(alerta.hechosHistoricos().stream()
                .map(atenciones::hechoVigente).map(Hecho::id).toList()))
            throw new IllegalArgumentException("El universo debe incluir las fuentes de señales cargadas");
    }

    public ResultadoEstadistico calcular(ConsultaEstadistica consulta, UUID usuarioId, Instant ahora) {
        var unidades = unidades(consulta.medida(), usuarioId, ahora);
        var seleccion = unidades.stream().filter(u -> cumple(u, consulta)).toList();
        var total = ids(seleccion).size();
        var grupos = new LinkedHashMap<List<String>, List<Unidad>>();
        for (var unidad : seleccion) {
            var coordenadas = consulta.agrupaciones().stream().map(d -> unidad.datos().getOrDefault(d, NO_INFORMADO)).toList();
            grupos.computeIfAbsent(coordenadas, _ -> new ArrayList<>()).add(unidad);
        }
        var celdas = grupos.values().stream().map(grupo -> new ResultadoEstadistico.Celda(
                consulta.agrupaciones().stream().map(d -> grupo.getFirst().datos().getOrDefault(d, NO_INFORMADO)).toList(),
                ids(grupo).size())).toList();
        if (consulta.agrupaciones().isEmpty() && celdas.isEmpty()) celdas = List.of(new ResultadoEstadistico.Celda(List.of(), 0));
        var sinDato = new EnumMap<ConsultaEstadistica.Dimension, Long>(ConsultaEstadistica.Dimension.class);
        for (var dimension : ConsultaEstadistica.Dimension.values()) sinDato.put(dimension,
                (long) ids(seleccion.stream().filter(u -> !u.datos().containsKey(dimension)
                        || NO_INFORMADO.equals(u.datos().get(dimension))).toList()).size());
        long sinFecha = ids(unidades.stream().filter(u -> u.fecha().isEmpty()).toList()).size();
        var fuentes = seleccion.stream().map(u -> u.contexto().ambito()).reduce(AmbitoReserva::unir);
        var recursos = seleccion.stream().map(u -> UUID.fromString(u.id().substring(0, 36))).collect(Collectors.toSet());
        var advertencias = List.of("Ámbito autorizado cargado; no se infiere cobertura institucional completa.",
                "Subtotales por grupos, papeles o categorías pueden no ser aditivos; cada total cuenta identidades únicas de su unidad.",
                "NO_INFORMADO y DISCREPANTE no representan cero ni responsabilidad confirmada.",
                "Grupo histórico a fecha del hecho; sexo informado de la identidad al corte, sin inferencia.",
                "Fechas aproximadas conservan año/mes de referencia; día de semana y hora requieren fecha exacta.");
        return new ResultadoEstadistico(consulta, ahora, total, celdas,
                new ResultadoEstadistico.Cobertura(ids(unidades).size(), sinFecha, sinDato), advertencias, fuentes, recursos);
    }

    public Comparacion comparar(ConsultaEstadistica definicion, ConsultaEstadistica.Periodo anterior,
                                 UUID usuario, Instant ahora) {
        var previo = calcular(definicion.en(anterior), usuario, ahora);
        var actual = calcular(definicion, usuario, ahora);
        return new Comparacion(previo, actual, actual.total() - previo.total());
    }

    /** Referencia anual explícita; nunca divide un total institucional por un grupo ajeno. */
    public Proporcion proporcionEstudiantes(ConsultaEstadistica consulta, Poblacion poblacion,
                                            UUID usuario, Instant ahora) {
        if (!consulta.medida().estudiantes()) throw new IllegalArgumentException("Se requiere medida de estudiantes distintos");
        validarFiltrosPoblacion(consulta, poblacion);
        var cohorte = cohorte(poblacion, usuario, ahora);
        var seleccionados = unidades(consulta.medida(), usuario, ahora).stream().filter(u -> cumple(u, consulta))
                .filter(u -> Integer.toString(poblacion.anioEscolar()).equals(u.datos().get(ANIO_ESCOLAR)))
                .flatMap(u -> u.estudianteId().stream()).filter(cohorte::containsKey).collect(Collectors.toSet());
        return proporcion(seleccionados.size(), cohorte, poblacion, consulta.periodo(), "porcentaje de estudiantes distintos de la nómina anual");
    }

    public Proporcion frecuenciaPorCien(ConsultaEstadistica consulta, Poblacion poblacion, UUID usuario, Instant ahora) {
        if (consulta.medida() != HECHOS) throw new IllegalArgumentException("Se requiere medida de hechos");
        validarFiltrosPoblacion(consulta, poblacion);
        var cohorte = cohorte(poblacion, usuario, ahora);
        var hechos = unidades(HECHOS, usuario, ahora).stream().filter(u -> cumple(u, consulta))
                .filter(u -> Integer.toString(poblacion.anioEscolar()).equals(u.datos().get(ANIO_ESCOLAR)))
                .filter(u -> u.estudianteId().filter(cohorte::containsKey).isPresent()).map(Unidad::id).distinct().count();
        return proporcion(hechos, cohorte, poblacion, consulta.periodo(), "hechos por cada cien estudiantes; no es porcentaje de personas ni probabilidad");
    }

    private static void validarFiltrosPoblacion(ConsultaEstadistica consulta, Poblacion poblacion) {
        if (consulta.filtros().containsKey(ANIO_ESCOLAR)
                && !consulta.filtros().get(ANIO_ESCOLAR).equals(Set.of(Integer.toString(poblacion.anioEscolar()))))
            throw new IllegalArgumentException("Año de numerador y nómina incompatible");
        var esperados = Map.of(GRADO, poblacion.grado(), SECCION, poblacion.seccion(), SEXO, poblacion.sexo());
        esperados.forEach((dimension, esperado) -> {
            if (consulta.filtros().containsKey(dimension) && (esperado.isEmpty()
                    || !consulta.filtros().get(dimension).equals(Set.of(esperado.get()))))
                throw new IllegalArgumentException("Filtro poblacional del numerador no coincide con denominador");
        });
    }

    private Map<UUID, Estudiante> cohorte(Poblacion poblacion, UUID usuario, Instant ahora) {
        var resultado = new LinkedHashMap<UUID, Estudiante>();
        for (var estudiante : matriculas.estudiantes().values()) {
            if (!atenciones.proteccion().permitePersona(estudiante.id(), ahora)) continue;
            if (!politica.evaluarPersona(usuario, Permiso.CONSULTAR_ESTADISTICAS, estudiante.id(), ahora).permitida()) continue;
            if (estudiante.matricula(poblacion.anioEscolar()).isEmpty()) continue;
            var grupo = estudiante.grupoEn(poblacion.anioEscolar(), poblacion.fechaReferencia());
            if (poblacion.grado().isPresent() && !grupo.flatMap(GrupoEscolar::grado).equals(poblacion.grado())) continue;
            if (poblacion.seccion().isPresent() && !grupo.flatMap(GrupoEscolar::seccion).equals(poblacion.seccion())) continue;
            if (poblacion.sexo().isPresent() && !estudiante.sexoInformado().equals(poblacion.sexo())) continue;
            resultado.put(estudiante.id(), estudiante);
        }
        return Map.copyOf(resultado);
    }

    private static Proporcion proporcion(long numerador, Map<UUID, Estudiante> cohorte, Poblacion poblacion,
                                          ConsultaEstadistica.Periodo periodo, String unidad) {
        long denominador = cohorte.size();
        long sinGrupo = cohorte.values().stream().filter(e -> e.grupoEn(poblacion.anioEscolar(), poblacion.fechaReferencia())
                .filter(g -> g.grado().isPresent() && g.seccion().isPresent()).isEmpty()).count();
        long sinSexo = cohorte.values().stream().filter(e -> e.sexoInformado().isEmpty()).count();
        var porcentaje = denominador == 0 ? Optional.<BigDecimal>empty() : Optional.of(BigDecimal.valueOf(numerador)
                .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominador), 2, RoundingMode.HALF_UP));
        return new Proporcion(numerador, denominador, porcentaje, unidad + "; redondeo HALF_UP a dos decimales; referencia anual declarada",
                poblacion, periodo, sinGrupo, sinSexo, denominador == 0 ? Optional.of("Sin denominador válido autorizado") : Optional.empty());
    }

    private List<Unidad> unidades(ConsultaEstadistica.Medida medida, UUID usuario, Instant ahora) {
        Objects.requireNonNull(ahora);
        PoliticaAcceso.exigir(politica.evaluarPersona(usuario, Permiso.CONSULTAR_ESTADISTICAS, new UUID(0, 0), ahora));
        var hechos = new LinkedHashMap<UUID, Hecho>();
        var contextos = new LinkedHashMap<UUID, ContextoAcceso>();
        for (var id : universo.stream().sorted().toList()) {
            var contexto = atenciones.contextoDeHechos(Set.of(id));
            if (!autorizado(usuario, contexto, ahora)) continue;
            var hecho = atenciones.hechoVigente(id);
            if (hecho.anulado()) continue;
            hechos.put(id, hecho); contextos.put(id, contexto);
        }
        // Una cuenta sin rol no obtiene una estadística vacía que aparente autorización.
        var resultado = new ArrayList<Unidad>();
        for (var hecho : hechos.values()) {
            var base = atomos(hecho, contextos.get(hecho.id()));
            if (medida == HECHOS || medida == PARTICIPACIONES || medida.estudiantes()) {
                for (var atomo : base) {
                    if (medida.estudiantes() && atomo.estudianteId().isEmpty()) continue;
                    var papel = papelMedida(medida);
                    if (papel.isPresent() && !papel.get().name().equals(atomo.datos().get(PAPEL))) continue;
                    String id = medida == HECHOS ? hecho.id().toString() : medida == PARTICIPACIONES
                            ? atomo.id() : atomo.estudianteId().orElseThrow().toString();
                    resultado.add(new Unidad(id, atomo.fecha(), atomo.datos(), atomo.estudianteId(), atomo.contexto()));
                }
            } else if (medida == COMUNICACIONES) {
                for (var reporte : hecho.reportes()) if (!reporte.anulado())
                    for (var atomo : base) resultado.add(evento(reporte.id().toString(), fecha(reporte.fechaRegistro()), atomo, Map.of()));
            }
        }
        if (medida.name().startsWith("CASOS") || medida.name().startsWith("ACTUACIONES")
                || medida.name().startsWith("COMPROMISOS") || medida.name().startsWith("SEGUIMIENTOS") || medida == DERIVACIONES_EFECTIVAS) {
            var actuacionesVistas = new HashSet<UUID>();
            for (var caso : atenciones.casos()) {
                if (!atenciones.casoDisponible(caso.id(), ahora)) continue;
                var contexto = atenciones.contexto(caso.id());
                if (!autorizado(usuario, contexto, ahora)) continue;
                if (caso.ultimaModificacion().isAfter(ahora)) throw new IllegalArgumentException("Caso posterior al corte");
                var base = caso.hechosReservados().stream().map(atenciones::hechoVigente).distinct()
                        .filter(h -> !h.anulado()).flatMap(h -> atomos(h, contexto).stream()).toList();
                if (base.isEmpty()) base = List.of(new Unidad(caso.id().toString(), Optional.of(caso.fechaInicio()),
                        Map.of(ESTADO_ATENCION, caso.estado().name()), Optional.empty(), contexto));
                if (medida.name().startsWith("CASOS")) {
                    boolean incluir = medida == CASOS_PENDIENTES ? caso.estado() == CasoAtencion.Estado.PENDIENTE
                            : caso.estado() == CasoAtencion.Estado.CERRADA;
                    if (medida == CASOS_RESUELTOS) incluir &= caso.cierre().filter(c -> c.tipo() != CierreLocal.TipoCierre.APERTURA_ERRONEA
                            && c.tipo() != CierreLocal.TipoCierre.DUPLICIDAD).isPresent();
                    if (incluir) for (var atomo : base) resultado.add(evento(caso.id().toString(),
                            Optional.of(medida == CASOS_PENDIENTES ? caso.fechaInicio() : caso.cierre().orElseThrow().fecha()), atomo,
                            Map.of(TIPO_CIERRE, caso.cierre().map(c -> c.tipo().name()).orElse(NO_INFORMADO))));
                }
                for (var actuacion : atenciones.actuacionesDelCaso(caso.id(), usuario, politica, ahora)) {
                    if (actuacionesVistas.add(actuacion.id())) agregarActuacion(resultado, base, actuacion, medida, ahora);
                }
                if (medida == DERIVACIONES_EFECTIVAS) for (var derivacion : caso.derivaciones())
                    if (derivacion.estado() == Derivacion.Estado.EFECTUADA) {
                        var fuentes = restringir(base, derivacion.hechosIds());
                        for (var atomo : fuentes) resultado.add(evento(derivacion.id().toString(), derivacion.fechaEfectiva(), atomo,
                                Map.of(ENTIDAD_DERIVACION, derivacion.destino().nombre())));
                    }
            }
            for (var hecho : hechos.values()) for (var inmediata : atenciones.inmediatasDelHecho(hecho.id(), usuario, politica, ahora))
                if (actuacionesVistas.add(inmediata.id())) agregarActuacion(resultado, atomos(hecho, contextos.get(hecho.id())), inmediata, medida, ahora);
        }
        if (medida.name().startsWith("ALERTAS")) for (var alerta : alertas.alertas()) {
            if (!atenciones.proteccion().permiteRecurso(alerta.id(), ahora)) continue;
            try { politica.exigirRecurso(alerta.id(), ahora); } catch (SecurityException bloqueada) { continue; }
            if (alerta.ultimaModificacion().isAfter(ahora)) throw new IllegalArgumentException("Señal posterior al corte");
            var contexto = atenciones.contextoDeHechos(alerta.hechosHistoricos());
            if (!autorizado(usuario, contexto, ahora)) continue;
            if (medida == ALERTAS_PENDIENTES && !alerta.pendiente()) continue;
            if (medida == ALERTAS_GESTIONADAS && alerta.disposicion().isEmpty()) continue;
            var motivos = alerta.revisiones().stream().flatMap(r -> r.fundamentos().values().stream())
                    .flatMap(f -> f.motivos().stream()).distinct().toList();
            var base = contexto.hechos().stream().filter(h -> !h.anulado()).flatMap(h -> atomos(h, contexto).stream()).toList();
            if (base.isEmpty()) base = List.of(new Unidad(alerta.id().toString(), fecha(alerta.creadaEn()), Map.of(), Optional.empty(), contexto));
            for (var atomo : base)
                for (var motivo : motivos) resultado.add(evento(alerta.id().toString(), fecha(alerta.creadaEn()), atomo,
                        Map.of(MOTIVO_ALERTA, motivo.name())));
        }
        return List.copyOf(resultado);
    }

    private List<Unidad> atomos(Hecho hecho, ContextoAcceso contexto) {
        var resultado = new ArrayList<Unidad>();
        var fechaHecho = hecho.fechaVigente();
        var fecha = fechaHecho.map(FechaHecho::fecha);
        var clasificacion = hecho.clasificacionVigente();
        var anios = hecho.reportes().stream().filter(r -> !r.anulado()).map(r -> r.datos().anioEscolar()).distinct().toList();
        var horas = hecho.reportes().stream().filter(r -> !r.anulado()).map(r -> r.datos().hora()).distinct().toList();
        for (var participacion : hecho.participaciones()) {
            var datos = new EnumMap<ConsultaEstadistica.Dimension, String>(ConsultaEstadistica.Dimension.class);
            datos.put(ANIO_ESCOLAR, anios.size() == 1 ? anios.getFirst().toString() : NO_INFORMADO);
            clasificacion.ifPresent(c -> { datos.put(CATEGORIA, c.categoriaId().toString()); datos.put(NATURALEZA, c.naturaleza().name()); datos.put(GRAVEDAD, c.gravedad().name()); });
            datos.put(PRECISION_FECHA, fechaHecho.map(f -> f.aproximada() ? "APROXIMADA" : "EXACTA").orElse("DISCREPANTE"));
            temporal(datos, fecha);
            if (fechaHecho.filter(f -> !f.aproximada()).isPresent()) {
                datos.put(DIA_SEMANA, fecha.orElseThrow().getDayOfWeek().name());
                if (horas.size() == 1) horas.getFirst().ifPresent(h -> datos.put(HORA, Integer.toString(h.getHour())));
            } else datos.remove(DIA_SEMANA);
            var persona = participacion.persona();
            datos.put(TIPO_PERSONA, persona.tipo().name());
            datos.put(PAPEL, participacion.papelVigente().map(Enum::name).orElse("DISCREPANTE"));
            datos.put(ATRIBUCION, participacion.papelVigente().map(MotorEstadisticas::atribucion).orElse("DISCREPANTE"));
            Optional<UUID> estudianteId = persona.tipo() == TipoPersona.ESTUDIANTE ? Optional.of(persona.id()) : Optional.empty();
            var estudiante = estudianteId.map(matriculas.estudiantes()::get);
            estudiante.flatMap(Estudiante::sexoInformado).ifPresent(s -> datos.put(SEXO, s));
            datos.put(GRADO, grupoHistorico(participacion, true));
            datos.put(SECCION, grupoHistorico(participacion, false));
            var casos = atenciones.casos().stream().filter(c -> c.hechosReservados().stream()
                    .map(atenciones::hechoCanonico).anyMatch(hecho.id()::equals)).toList();
            datos.put(ESTADO_ATENCION, casos.stream().anyMatch(c -> c.estado() == CasoAtencion.Estado.PENDIENTE)
                    ? "PENDIENTE" : casos.isEmpty() ? "SIN_ATENCION" : "CERRADA");
            datos.put(INTERVENCION_COMITE, casos.stream().anyMatch(c -> !c.intervencionesComite().isEmpty()) ? "SI" : "NO");
            resultado.add(new Unidad(hecho.id() + ":" + persona.id(), fecha, Map.copyOf(datos), estudianteId, contexto));
        }
        return List.copyOf(resultado);
    }

    private void agregarActuacion(List<Unidad> resultado, List<Unidad> base, Actuacion actuacion,
                                  ConsultaEstadistica.Medida medida, Instant ahora) {
        if (actuacion.ultimaModificacion().isAfter(ahora)) throw new IllegalArgumentException("Actuación posterior al corte");
        var fuentes = restringir(base, actuacion.hechosIds());
        if (!actuacion.personasIds().isEmpty()) fuentes = fuentes.stream()
                .filter(u -> actuacion.personasIds().stream().anyMatch(p -> u.id().endsWith(p.toString()))).toList();
        if (medida == ACTUACIONES_REALIZADAS && actuacion.estado() == Actuacion.Estado.REALIZADA
                || medida == ACTUACIONES_PREVISTAS && actuacion.estado() == Actuacion.Estado.PREVISTA)
            for (var atomo : fuentes) resultado.add(evento(actuacion.id().toString(), medida == ACTUACIONES_REALIZADAS
                    ? actuacion.fechaEfectiva() : actuacion.fechaPrevista(), atomo, Map.of()));
        for (var compromiso : actuacion.compromisos()) {
            if (compromiso.estado() == Compromiso.Estado.CANCELADO) continue;
            boolean pendiente = compromiso.pendienteSeguimiento();
            boolean comprobado = compromiso.estado() == Compromiso.Estado.CUMPLIDO || compromiso.estado() == Compromiso.Estado.NO_CUMPLIDO;
            boolean incluir = medida == COMPROMISOS || (medida == COMPROMISOS_PENDIENTES || medida == SEGUIMIENTOS_PENDIENTES) && pendiente
                    || medida == COMPROMISOS_COMPROBADOS && comprobado;
            if (incluir) for (var atomo : fuentes) resultado.add(evento(compromiso.id().toString(),
                    medida == COMPROMISOS_COMPROBADOS ? compromiso.revisiones().getLast().fechaObservacion()
                            : fecha(compromiso.revisiones().getFirst().cambio().fecha()), atomo, Map.of()));
            if (medida == SEGUIMIENTOS_COMPROBADOS) {
                var revisiones = compromiso.revisiones();
                for (int indice = 1; indice < revisiones.size(); indice++) {
                    var revision = revisiones.get(indice);
                    if (revision.fechaObservacion().isPresent() && (revision.estado() == Compromiso.Estado.CUMPLIDO
                            || revision.estado() == Compromiso.Estado.NO_CUMPLIDO))
                        for (var atomo : fuentes) resultado.add(evento(compromiso.id() + ":" + indice,
                                revision.fechaObservacion(), atomo, Map.of()));
                }
            }
        }
    }

    private List<Unidad> restringir(List<Unidad> base, Set<UUID> hechos) {
        if (hechos.isEmpty()) return base;
        if (base.stream().allMatch(u -> u.id().length() == 36)) return base;
        var canonicos = hechos.stream().map(atenciones::hechoCanonico).map(UUID::toString).collect(Collectors.toSet());
        return base.stream().filter(u -> canonicos.contains(u.id().substring(0, 36))).toList();
    }
    private static Unidad evento(String id, Optional<LocalDate> fecha, Unidad atomo, Map<ConsultaEstadistica.Dimension, String> extras) {
        var datos = new EnumMap<ConsultaEstadistica.Dimension, String>(ConsultaEstadistica.Dimension.class);
        datos.putAll(atomo.datos());
        datos.remove(ANIO); datos.remove(MES); datos.remove(DIA_SEMANA); datos.remove(HORA);
        temporal(datos, fecha); datos.putAll(extras);
        return new Unidad(id, fecha, Map.copyOf(datos), atomo.estudianteId(), atomo.contexto());
    }
    private static void temporal(Map<ConsultaEstadistica.Dimension, String> datos, Optional<LocalDate> fecha) {
        fecha.ifPresent(f -> { datos.put(ANIO, Integer.toString(f.getYear())); datos.put(MES, f.toString().substring(0, 7));
            datos.put(DIA_SEMANA, f.getDayOfWeek().name()); });
    }
    private static Optional<LocalDate> fecha(Instant instante) { return Optional.of(instante.atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate()); }
    private static Set<String> ids(List<Unidad> unidades) { return unidades.stream().map(Unidad::id).collect(Collectors.toSet()); }
    private static boolean cumple(Unidad unidad, ConsultaEstadistica consulta) {
        if (unidad.fecha().map(consulta.periodo()::contiene).orElse(consulta.periodo().incluirFechaNoPrecisada())) {
            return consulta.filtros().entrySet().stream().allMatch(f -> f.getValue().contains(unidad.datos().getOrDefault(f.getKey(), NO_INFORMADO)));
        }
        return false;
    }
    private static Optional<PapelParticipacion> papelMedida(ConsultaEstadistica.Medida medida) {
        return Optional.ofNullable(switch (medida) {
            case ESTUDIANTES_COTIDIANA -> PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA;
            case ESTUDIANTES_PRESUNTA -> PapelParticipacion.PRESUNTA_RESPONSABILIDAD;
            case ESTUDIANTES_CORROBORADA -> PapelParticipacion.ATRIBUCION_CORROBORADA;
            case ESTUDIANTES_AFECTADOS -> PapelParticipacion.AFECTADO;
            case ESTUDIANTES_TESTIGOS -> PapelParticipacion.TESTIGO;
            default -> null;
        });
    }
    private static String atribucion(PapelParticipacion papel) {
        return switch (papel) {
            case CONDUCTA_COTIDIANA_ATRIBUIDA -> "COTIDIANA";
            case PRESUNTA_RESPONSABILIDAD -> "PRESUNTA";
            case ATRIBUCION_CORROBORADA -> "CORROBORADA";
            case ATRIBUCION_DESCARTADA -> "DESCARTADA";
            default -> "SIN_ATRIBUCION";
        };
    }

    static String grupoHistorico(Hecho.ParticipacionConsolidada participacion, boolean grado) {
        var valores = participacion.aportes().stream().map(a -> a.participacion().grupoHistorico()
                .flatMap(g -> grado ? g.grado() : g.seccion())).distinct().toList();
        return valores.size() == 1 ? valores.getFirst().orElse(NO_INFORMADO) : "DISCREPANTE";
    }
    private boolean autorizado(UUID usuario, ContextoAcceso contexto, Instant ahora) {
        return atenciones.proteccion().permite(contexto, ahora)
                && !atenciones.restricciones().estaExcluido(usuario, contexto.ambito())
                && politica.evaluar(usuario, Permiso.CONSULTAR_ESTADISTICAS, contexto, ahora).permitida();
    }

}
