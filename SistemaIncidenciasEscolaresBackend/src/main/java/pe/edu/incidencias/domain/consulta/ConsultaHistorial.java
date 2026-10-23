package pe.edu.incidencias.domain.consulta;

import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.incidencia.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static pe.edu.incidencias.domain.estudiante.BusquedaEstudiantes.normalizar;

/** Trayectoria reservada por identidad conocida; no crea padrón externo ni concilia nombres de personas. */
public final class ConsultaHistorial {
    public record Filtro(Optional<UUID> personaId, Optional<String> textoPersona,
                          Optional<ConsultaEstadistica.Periodo> fechasHecho,
                          Optional<ConsultaEstadistica.Periodo> fechasRegistro,
                          Map<ConsultaEstadistica.Dimension, Set<String>> criterios) {
        public Filtro {
            Objects.requireNonNull(personaId); Objects.requireNonNull(textoPersona);
            Objects.requireNonNull(fechasHecho); Objects.requireNonNull(fechasRegistro);
            textoPersona = textoPersona.map(String::strip);
            if (textoPersona.filter(t -> normalizar(t).isBlank()).isPresent()) throw new IllegalArgumentException("Texto de persona vacío");
            var copia = new EnumMap<ConsultaEstadistica.Dimension, Set<String>>(ConsultaEstadistica.Dimension.class);
            criterios.forEach((d, valores) -> {
                if (!Set.of(ConsultaEstadistica.Dimension.CATEGORIA, ConsultaEstadistica.Dimension.GRAVEDAD,
                        ConsultaEstadistica.Dimension.ANIO_ESCOLAR, ConsultaEstadistica.Dimension.ESTADO_ATENCION,
                        ConsultaEstadistica.Dimension.GRADO, ConsultaEstadistica.Dimension.SECCION).contains(d))
                    throw new IllegalArgumentException("Filtro no disponible para historial: " + d);
                if (valores.isEmpty() || valores.stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Filtro vacío");
                copia.put(d, Set.copyOf(valores));
            });
            criterios = Map.copyOf(copia);
        }
        public static Filtro dePersona(UUID personaId) {
            return new Filtro(Optional.of(personaId), Optional.empty(), Optional.empty(), Optional.empty(), Map.of());
        }
    }
    public record Entrada(Hecho hecho, List<Hecho.ParticipacionConsolidada> participacionesBuscadas,
                           List<CasoAtencion> atenciones, List<Actuacion> actuaciones,
                           List<Derivacion> derivaciones, List<UUID> antecedentesCasos) {
        public Entrada {
            participacionesBuscadas = List.copyOf(participacionesBuscadas);
            atenciones = List.copyOf(atenciones); actuaciones = List.copyOf(actuaciones);
            derivaciones = List.copyOf(derivaciones); antecedentesCasos = List.copyOf(antecedentesCasos);
        }
    }
    public record Pagina(List<Entrada> entradas, int siguienteDesplazamiento, boolean hayMas) {
        public Pagina { entradas = List.copyOf(entradas); }
    }
    private final RegistroAtenciones registro;
    private final Set<UUID> universo;
    private final PoliticaAcceso politica;

    public ConsultaHistorial(RegistroAtenciones registro, Set<UUID> hechosCargados, PoliticaAcceso politicaActual) {
        this.registro = Objects.requireNonNull(registro);
        this.politica = Objects.requireNonNull(politicaActual);
        this.universo = hechosCargados.stream().map(registro::hechoVigente).map(Hecho::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public Pagina consultar(Filtro filtro, int desplazamiento, int limite, UUID usuario, Instant ahora) {
        if (desplazamiento < 0 || limite < 1 || limite > 500) throw new IllegalArgumentException("Tramo inválido");
        PoliticaAcceso.exigir(politica.evaluarPersona(usuario, Permiso.CONSULTAR_HECHO, new UUID(0, 0), ahora));
        var entradas = new ArrayList<Entrada>();
        for (var id : universo) {
            var contexto = registro.contextoDeHechos(Set.of(id));
            if (!registro.proteccion().permite(contexto, ahora) || registro.restricciones().estaExcluido(usuario, contexto.ambito())
                    || !politica.evaluar(usuario, Permiso.CONSULTAR_HECHO, contexto, ahora).permitida()) continue;
            var hecho = registro.hechoVigente(id);
            var participaciones = hecho.participaciones().stream().filter(p -> personaCoincide(p.persona(), filtro)).toList();
            if (participaciones.isEmpty() && (!hecho.anulado() || !personaHistoricaCoincide(hecho, filtro))) continue;
            if (!hecho.anulado() && filtro.fechasHecho().isPresent() && !hecho.fechaVigente().map(FechaHecho::fecha)
                    .map(filtro.fechasHecho().get()::contiene).orElse(filtro.fechasHecho().get().incluirFechaNoPrecisada())) continue;
            if (filtro.fechasRegistro().isPresent() && hecho.reportes().stream()
                    .noneMatch(r -> filtro.fechasRegistro().get().contiene(r.fechaRegistro().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate()))) continue;
            var casos = registro.casos().stream().filter(c -> c.hechosReservados().stream().map(registro::hechoCanonico).anyMatch(id::equals)).toList();
            // Un caso puede ampliar el ámbito por antecedentes: cada elemento se autoriza antes de proyectarlo.
            casos = casos.stream().filter(c -> registro.casoDisponible(c.id(), ahora)
                    && !registro.restricciones().estaExcluido(usuario, registro.contexto(c.id()).ambito())
                    && politica.evaluar(usuario, Permiso.CONSULTAR_HECHO, registro.contexto(c.id()), ahora).permitida()).toList();
            if (!cumple(filtro, hecho, participaciones, casos)) continue;
            var actuaciones = new LinkedHashMap<UUID, Actuacion>();
            var derivaciones = new LinkedHashMap<UUID, Derivacion>();
            var antecedentes = new HashSet<UUID>();
            for (var caso : casos) {
                if (caso.ultimaModificacion().isAfter(ahora)) throw new IllegalArgumentException("Caso posterior al corte");
                registro.actuacionesDelCaso(caso.id(), usuario, politica, ahora).forEach(a -> actuaciones.put(a.id(), a));
                caso.derivaciones().forEach(d -> derivaciones.put(d.id(), d));
                antecedentes.addAll(caso.antecedentes());
            }
            registro.inmediatasDelHecho(id, usuario, politica, ahora).forEach(a -> actuaciones.put(a.id(), a));
            entradas.add(new Entrada(hecho, participaciones, casos, List.copyOf(actuaciones.values()),
                    List.copyOf(derivaciones.values()), antecedentes.stream().sorted().toList()));
        }
        entradas.sort(Comparator.comparing((Entrada e) -> e.hecho().anulado()
                        ? e.hecho().reportes().stream().map(r -> r.datos().fechaHecho().fecha()).min(LocalDate::compareTo).orElse(LocalDate.MAX)
                        : e.hecho().fechaVigente().map(FechaHecho::fecha).orElse(LocalDate.MAX))
                .thenComparing(e -> e.hecho().principal().fechaRegistro()).thenComparing(e -> e.hecho().id()));
        int inicio = Math.min(desplazamiento, entradas.size());
        int fin = (int) Math.min((long) inicio + limite, entradas.size());
        return new Pagina(entradas.subList(inicio, fin), fin, fin < entradas.size());
    }

    /** Proyección ordinaria existente: solo autoría propia, sin ampliar privilegios por historial. */
    public PoliticaAcceso.AportePropio consultarAportePropio(UUID reporteId, UUID usuario, Instant ahora) {
        var actual = registro.contextoDeHechos(Set.of(reporteId));
        var contexto = new ContextoAcceso(actual.ambito(), actual.hechos(), Optional.of(reporteId), Optional.empty());
        if (!registro.proteccion().permite(contexto, ahora) || registro.restricciones().estaExcluido(usuario, contexto.ambito()))
            throw new SecurityException("Aporte actualmente restringido");
        return politica.consultarAportePropio(usuario, contexto, ahora);
    }
    private boolean cumple(Filtro filtro, Hecho hecho, List<Hecho.ParticipacionConsolidada> personas, List<CasoAtencion> casos) {
        if (!hecho.anulado() && !personas.stream().anyMatch(p -> java.util.stream.Stream.of(ConsultaEstadistica.Dimension.GRADO,
                ConsultaEstadistica.Dimension.SECCION).allMatch(d -> !filtro.criterios().containsKey(d)
                || filtro.criterios().get(d).contains(MotorEstadisticas.grupoHistorico(p, d == ConsultaEstadistica.Dimension.GRADO))))) return false;
        String estado = casos.stream().anyMatch(c -> c.estado() == CasoAtencion.Estado.PENDIENTE) ? "PENDIENTE"
                : casos.isEmpty() ? "SIN_ATENCION" : "CERRADA";
        if (hecho.anulado()) {
            // Localización histórica explícita; nunca se crea una participación vigente para el resultado.
            return hecho.reportes().stream().filter(r -> filtro.fechasRegistro().map(p -> p.contiene(
                            r.fechaRegistro().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate())).orElse(true))
                    .flatMap(r -> r.revisiones().stream()).anyMatch(r -> filtro.fechasHecho()
                            .map(p -> p.contiene(r.datos().fechaHecho().fecha())).orElse(true)
                            && cumpleDatosPropios(r.datos(), estado, filtro));
        }
        var anios = hecho.reportes().stream().filter(r -> !r.anulado()).map(r -> r.datos().anioEscolar()).distinct().toList();
        String anio = anios.size() == 1 ? anios.getFirst().toString() : ResultadoEstadistico.NO_INFORMADO;
        for (var criterio : filtro.criterios().entrySet()) {
            var valores = criterio.getValue();
            boolean coincide = switch (criterio.getKey()) {
                case CATEGORIA -> valores.contains(hecho.clasificacionVigente().map(c -> c.categoriaId().toString()).orElse(ResultadoEstadistico.NO_INFORMADO));
                case GRAVEDAD -> valores.contains(hecho.clasificacionVigente().map(c -> c.gravedad().name()).orElse(ResultadoEstadistico.NO_INFORMADO));
                case ANIO_ESCOLAR -> valores.contains(anio);
                case ESTADO_ATENCION -> valores.contains(estado);
                case GRADO, SECCION -> true;
                default -> false;
            };
            if (!coincide) return false;
        }
        return true;
    }

    private static boolean personaHistoricaCoincide(Hecho hecho, Filtro filtro) {
        return hecho.reportes().stream().flatMap(r -> r.revisiones().stream()).flatMap(r -> r.datos().participaciones().stream())
                .anyMatch(p -> personaCoincide(p.persona(), filtro));
    }
    private static boolean personaCoincide(PersonaInvolucrada persona, Filtro filtro) {
        return filtro.personaId().map(persona.id()::equals).orElse(true)
                && filtro.textoPersona().map(t -> normalizar(persona.descripcion()).contains(normalizar(t))).orElse(true);
    }

    public record PaginaAportes(List<PoliticaAcceso.AportePropio> aportes, int siguienteDesplazamiento, boolean hayMas) {
        public PaginaAportes { aportes = List.copyOf(aportes); }
    }
    /** Búsqueda del autor sobre sus propios datos visibles; no filtra con correcciones institucionales ajenas. */
    public PaginaAportes buscarAportesPropios(Filtro filtro, int desplazamiento, int limite, UUID usuario, Instant ahora) {
        if (desplazamiento < 0 || limite < 1 || limite > 500) throw new IllegalArgumentException("Tramo inválido");
        PoliticaAcceso.exigir(politica.evaluar(usuario, Permiso.REGISTRAR_INCIDENCIA, ahora));
        var aportes = new ArrayList<PoliticaAcceso.AportePropio>();
        for (var hechoId : universo) {
            var contexto = registro.contextoDeHechos(Set.of(hechoId));
            if (!registro.proteccion().permite(contexto, ahora) || registro.restricciones().estaExcluido(usuario, contexto.ambito())) continue;
            for (var reporte : registro.hechoVigente(hechoId).reportes()) {
                var propio = new ContextoAcceso(contexto.ambito(), contexto.hechos(), Optional.of(reporte.id()), Optional.empty());
                if (!politica.evaluar(usuario, Permiso.CONSULTAR_APORTE_PROPIO, propio, ahora).permitida()) continue;
                var vista = politica.consultarAportePropio(usuario, propio, ahora);
                var datos = vista.revisionesPropias().getLast().datos();
                if (filtro.fechasHecho().filter(p -> !p.contiene(datos.fechaHecho().fecha())).isPresent()) continue;
                if (filtro.fechasRegistro().filter(p -> !p.contiene(vista.fechaRegistro().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate())).isPresent()) continue;
                if (cumpleDatosPropios(datos, vista.relacionadoParaAtencion() ? "CON_ATENCION" : "SIN_ATENCION", filtro)) aportes.add(vista);
            }
        }
        aportes.sort(Comparator.comparing(PoliticaAcceso.AportePropio::fechaRegistro).thenComparing(PoliticaAcceso.AportePropio::reporteId));
        int inicio = Math.min(desplazamiento, aportes.size()); int fin = (int) Math.min((long) inicio + limite, aportes.size());
        return new PaginaAportes(aportes.subList(inicio, fin), fin, fin < aportes.size());
    }
    private static boolean cumpleDatosPropios(DatosReporte datos, String estado, Filtro filtro) {
        return datos.participaciones().stream().filter(p -> personaCoincide(p.persona(), filtro)).anyMatch(p -> filtro.criterios().entrySet().stream().allMatch(e -> {
            String valor = switch (e.getKey()) {
                case CATEGORIA -> datos.clasificacion().categoriaId().toString();
                case GRAVEDAD -> datos.clasificacion().gravedad().name();
                case ANIO_ESCOLAR -> Integer.toString(datos.anioEscolar());
                case GRADO -> p.grupoHistorico().flatMap(pe.edu.incidencias.domain.estudiante.GrupoEscolar::grado).orElse(ResultadoEstadistico.NO_INFORMADO);
                case SECCION -> p.grupoHistorico().flatMap(pe.edu.incidencias.domain.estudiante.GrupoEscolar::seccion).orElse(ResultadoEstadistico.NO_INFORMADO);
                case ESTADO_ATENCION -> estado;
                default -> ResultadoEstadistico.NO_INFORMADO;
            };
            return e.getValue().contains(valor);
        }));
    }
}
