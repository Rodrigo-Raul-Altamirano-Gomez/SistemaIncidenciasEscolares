package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.incidencia.Hecho;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.incidencia.ReporteIncidencia;
import pe.edu.incidencias.domain.catalogo.Gravedad;
import pe.edu.incidencias.domain.catalogo.NaturalezaCategoria;
import pe.edu.incidencias.domain.proteccion.ControlProteccion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Colección de dominio: aplica invariantes entre atenciones y devuelve otro estado, sin persistencia ni casos de uso. */
public final class RegistroAtenciones {
    public record CompromisoPropio(UUID id, List<Compromiso.Revision> revisionesPropias) {
        public CompromisoPropio { revisionesPropias = List.copyOf(revisionesPropias); }
    }
    public record RevisionPropia(Actuacion.Estado estado, String relato, Optional<LocalDate> fechaPrevista,
                                 Optional<LocalDate> fechaEfectiva, List<CompromisoPropio> compromisos,
                                 RegistroCambio cambio) {
        public RevisionPropia { compromisos = List.copyOf(compromisos); }
    }
    public record AccionInmediataPropia(UUID id, UUID responsableId, List<RevisionPropia> revisionesPropias) {
        public AccionInmediataPropia { revisionesPropias = List.copyOf(revisionesPropias); }
    }
    public record Duplicidad(Hecho origen, UUID principalId, Optional<UUID> casoContinuidad,
                              Set<UUID> antecedentesGenerados, RegistroCambio registro, Optional<RegistroCambio> correccion) {
        public Duplicidad { antecedentesGenerados = Set.copyOf(antecedentesGenerados); }
    }
    /** Índice autoritativo del ámbito: IDs, sin cargar el contenido de otras atenciones. */
    public record Indice(Map<UUID, Set<UUID>> reservasPorHecho, Map<UUID, UUID> pendientesPorHecho) {
        public Indice {
            var copia = new LinkedHashMap<UUID, Set<UUID>>();
            reservasPorHecho.forEach((id, casos) -> copia.put(id, Set.copyOf(casos)));
            reservasPorHecho = Map.copyOf(copia);
            pendientesPorHecho = Map.copyOf(pendientesPorHecho);
            for (var entrada : pendientesPorHecho.entrySet()) if (!reservasPorHecho.getOrDefault(entrada.getKey(), Set.of()).contains(entrada.getValue()))
                throw new IllegalArgumentException("La atención pendiente debe constar también en el índice de reserva");
        }
        public static Indice vacio() { return new Indice(Map.of(), Map.of()); }
    }
    private final Map<UUID, Hecho> hechos;
    private final Map<UUID, CasoAtencion> casos;
    private final Map<UUID, List<Actuacion>> inmediatas;
    private final RestriccionesAmbito restricciones;
    private final List<Duplicidad> duplicidades;
    private final Indice indice;
    private final ControlProteccion proteccion;
    private final ControlProteccion proteccionEfectiva;

    private RegistroAtenciones(Map<UUID, Hecho> hechos, Map<UUID, CasoAtencion> casos,
                               Map<UUID, List<Actuacion>> inmediatas, RestriccionesAmbito restricciones) {
        this(hechos, casos, inmediatas, restricciones, List.of());
    }
    private RegistroAtenciones(Map<UUID, Hecho> hechos, Map<UUID, CasoAtencion> casos,
                               Map<UUID, List<Actuacion>> inmediatas, RestriccionesAmbito restricciones,
                               List<Duplicidad> duplicidades) {
        this(hechos, casos, inmediatas, restricciones, duplicidades, Indice.vacio());
    }
    private RegistroAtenciones(Map<UUID, Hecho> hechos, Map<UUID, CasoAtencion> casos,
                               Map<UUID, List<Actuacion>> inmediatas, RestriccionesAmbito restricciones,
                               List<Duplicidad> duplicidades, Indice indiceAnterior) {
        this(hechos, casos, inmediatas, restricciones, duplicidades, indiceAnterior, ControlProteccion.vacio());
    }
    private RegistroAtenciones(Map<UUID, Hecho> hechos, Map<UUID, CasoAtencion> casos,
                               Map<UUID, List<Actuacion>> inmediatas, RestriccionesAmbito restricciones,
                               List<Duplicidad> duplicidades, Indice indiceAnterior, ControlProteccion proteccion) {
        this.proteccion = Objects.requireNonNull(proteccion);
        this.hechos = Map.copyOf(hechos);
        this.casos = Map.copyOf(casos);
        var acciones = new LinkedHashMap<UUID, List<Actuacion>>();
        inmediatas.forEach((id, valores) -> acciones.put(id, List.copyOf(valores)));
        this.inmediatas = Map.copyOf(acciones);
        this.restricciones = Objects.requireNonNull(restricciones, "restricciones");
        this.duplicidades = List.copyOf(duplicidades);
        var reservas = new LinkedHashMap<UUID, Set<UUID>>();
        indiceAnterior.reservasPorHecho().forEach((id, referencias) -> {
            var restantes = new HashSet<>(referencias);
            restantes.removeAll(casos.keySet());
            if (!restantes.isEmpty()) reservas.computeIfAbsent(hechoCanonico(id), _ -> new HashSet<>()).addAll(restantes);
        });
        var pendientes = new LinkedHashMap<UUID, UUID>();
        indiceAnterior.pendientesPorHecho().forEach((id, caso) -> {
            if (!casos.containsKey(caso)) pendientes.put(hechoCanonico(id), caso);
        });
        for (var caso : casos.values()) {
            for (var id : normalizar(caso.hechosReservados())) reservas.computeIfAbsent(id, _ -> new HashSet<>()).add(caso.id());
            if (caso.estado() == CasoAtencion.Estado.PENDIENTE) for (var id : normalizar(caso.hechosActuales())) {
                var previo = pendientes.putIfAbsent(id, caso.id());
                if (previo != null && !previo.equals(caso.id())) throw new IllegalStateException("El índice ya identifica otra atención pendiente del hecho");
            }
        }
        this.indice = new Indice(reservas, pendientes);
        this.proteccionEfectiva = calcularProteccion();
    }

    private RegistroAtenciones copiarEstado(Map<UUID, Hecho> hechos, Map<UUID, CasoAtencion> casos,
                                             Map<UUID, List<Actuacion>> inmediatas, RestriccionesAmbito restricciones) {
        return new RegistroAtenciones(hechos, casos, inmediatas, restricciones, duplicidades, indice, proteccion);
    }

    /** Estructura interna de fuentes e historia; no es una vista autorizada. */
    public List<Duplicidad> duplicidades() { return duplicidades; }
    public Indice indice() { return indice; }
    public UUID hechoCanonico(UUID id) {
        var visitados = new HashSet<UUID>();
        UUID actual = id;
        while (visitados.add(actual)) {
            UUID candidato = actual;
            var relacion = duplicidades.stream().filter(d -> d.correccion().isEmpty() && d.origen().id().equals(candidato))
                    .reduce((a, b) -> b);
            if (relacion.isEmpty()) return actual;
            actual = relacion.get().principalId();
        }
        throw new IllegalStateException("Relaciones de duplicidad cíclicas");
    }
    public Hecho hechoVigente(UUID id) {
        return Optional.ofNullable(hechos.get(hechoCanonico(id))).orElseThrow(() -> new IllegalArgumentException("El hecho no pertenece al ámbito cargado"));
    }
    private Set<UUID> normalizar(Set<UUID> ids) {
        return ids.stream().map(this::hechoCanonico).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** Recibe hechos existentes; no registra incidencias ni les atribuye nuevos autores. */
    public static RegistroAtenciones iniciar(Collection<Hecho> hechos, RestriccionesAmbito restricciones) {
        var porId = new LinkedHashMap<UUID, Hecho>();
        var reportes = new HashSet<UUID>();
        for (var hecho : List.copyOf(hechos)) {
            if (porId.putIfAbsent(hecho.id(), hecho) != null) throw new IllegalArgumentException("Hecho repetido");
            for (var reporte : hecho.reportes()) if (!reportes.add(reporte.id()))
                throw new IllegalArgumentException("Un reporte no representa dos hechos distintos");
        }
        return new RegistroAtenciones(porId, Map.of(), Map.of(), restricciones);
    }
    public static RegistroAtenciones iniciar(Collection<Hecho> hechos, RestriccionesAmbito restricciones, ControlProteccion proteccion) {
        return iniciar(hechos, restricciones).conProteccion(proteccion);
    }

    /** Reconstituye solo el contenido necesario más su índice de reserva/unicidad, desde una instantánea consistente. */
    public static RegistroAtenciones cargarAmbito(Collection<Hecho> hechos, Collection<CasoAtencion> casos,
                                                   Map<UUID, List<Actuacion>> inmediatas, RestriccionesAmbito restricciones,
                                                   List<Duplicidad> duplicidades, Indice indice) {
        var inicial = iniciar(hechos, restricciones);
        var porId = new LinkedHashMap<UUID, CasoAtencion>();
        for (var caso : casos) if (porId.putIfAbsent(caso.id(), caso) != null) throw new IllegalArgumentException("Atención repetida");
        var registro = new RegistroAtenciones(inicial.hechos, porId, inmediatas, restricciones, duplicidades, indice);
        for (var caso : casos) {
            if (!porId.keySet().containsAll(caso.antecedentes())) throw new IllegalArgumentException("Falta el antecedente que contiene la atención cargada");
            registro.validarHechos(registro.normalizar(caso.hechosReservados()));
        }
        for (var reporte : inmediatas.keySet()) registro.hechoDeReporte(reporte);
        registro.validarPendientes(porId, inmediatas);
        return registro;
    }
    public static RegistroAtenciones cargarAmbito(Collection<Hecho> hechos, Collection<CasoAtencion> casos,
                                                   Map<UUID, List<Actuacion>> inmediatas, RestriccionesAmbito restricciones,
                                                   List<Duplicidad> duplicidades, Indice indice, ControlProteccion proteccion) {
        return cargarAmbito(hechos, casos, inmediatas, restricciones, duplicidades, indice).conProteccion(proteccion);
    }

    public List<CasoAtencion> casos() { return casos.values().stream().sorted(java.util.Comparator.comparing(c -> c.id().toString())).toList(); }
    public Optional<CasoAtencion> caso(UUID id) { return Optional.ofNullable(casos.get(id)); }
    public RestriccionesAmbito restricciones() { return restricciones; }
    public ControlProteccion proteccion() { return proteccionEfectiva; }

    private ControlProteccion calcularProteccion() {
        var recursos = new LinkedHashMap<UUID, AmbitoReserva>();
        var personas = new LinkedHashMap<UUID, AmbitoReserva>();
        for (var caso : casos()) {
            var ambito = contexto(caso.id()).ambito();
            caso.derivaciones().forEach(d -> recursos.merge(d.id(), ambito, AmbitoReserva::unir));
            caso.convocatorias().forEach(c -> recursos.merge(c.id(), ambito, AmbitoReserva::unir));
            caso.intervencionesComite().forEach(i -> recursos.merge(i.id(), ambito, AmbitoReserva::unir));
            caso.decisionesComite().forEach(d -> recursos.merge(d.id(), ambito, AmbitoReserva::unir));
            for (var accion : caso.actuaciones()) relacionarProteccion(accion, ambito, recursos, personas);
        }
        inmediatas.forEach((reporte, acciones) -> {
            var ambito = contextoDeHechos(Set.of(hechoDeReporte(reporte).id())).ambito();
            acciones.forEach(a -> relacionarProteccion(a, ambito, recursos, personas));
        });
        return proteccion.conRelaciones(recursos, personas);
    }
    private static void relacionarProteccion(Actuacion accion, AmbitoReserva ambito,
                                             Map<UUID, AmbitoReserva> recursos, Map<UUID, AmbitoReserva> personas) {
        recursos.merge(accion.id(), ambito, AmbitoReserva::unir);
        accion.personasIds().forEach(id -> personas.merge(id, ambito, AmbitoReserva::unir));
        for (var compromiso : accion.revisiones().stream().flatMap(r -> r.compromisos().stream()).toList()) {
            recursos.merge(compromiso.id(), ambito, AmbitoReserva::unir);
            personas.merge(compromiso.asumidoPor().id(), ambito, AmbitoReserva::unir);
        }
    }
    /** La aplicación debe cargar los controles actuales antes de ofrecer cualquier operación protegida. */
    public RegistroAtenciones conProteccion(ControlProteccion actual) {
        return new RegistroAtenciones(hechos, casos, inmediatas, restricciones, duplicidades, indice, actual);
    }
    /** Filtro jurídico de una atención y sus recursos embebidos; no concede permiso funcional. */
    public boolean casoDisponible(UUID casoId, Instant ahora) { return proteccion().permite(contexto(casoId), ahora); }

    public RegistroAtenciones incorporarHecho(Hecho hecho, DirectorioUsuarios directorio, RegistroCambio cambio) {
        if (hechos.containsKey(hecho.id())) throw new IllegalArgumentException("El hecho ya está incorporado");
        validarReportesUnicos(hecho);
        var contexto = new ContextoAcceso(new AmbitoReserva(Set.of(hecho.id()), Set.of(),
                indice.reservasPorHecho().getOrDefault(hecho.id(), Set.of())), List.of(hecho), Optional.empty(), Optional.empty());
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), Permiso.ATENDER, contexto, cambio.fecha()));
        var nuevos = new LinkedHashMap<>(hechos);
        nuevos.put(hecho.id(), hecho);
        return copiarEstado(nuevos, casos, inmediatas, politica.restriccionesActualizadas(contexto, cambio.fecha()));
    }

    /** Admite precisiones, identificación, anulación y nuevos reportes; la desvinculación entre hechos necesita coordinación aparte. */
    public RegistroAtenciones actualizarHecho(Hecho actualizado, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var anterior = Optional.ofNullable(hechos.get(actualizado.id())).orElseThrow(() -> new IllegalArgumentException("El hecho no existe"));
        var contextoAnterior = contextoDeHecho(anterior.id(), Optional.empty());
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), Permiso.CORREGIR_HECHO, contextoAnterior, cambio.fecha()));
        if (!continua(anterior.vinculos(), actualizado.vinculos()) || !continua(anterior.precisiones(), actualizado.precisiones())
                || !continua(anterior.aclaraciones(), actualizado.aclaraciones()) || !continua(anterior.cambiosPrincipal(), actualizado.cambiosPrincipal()))
            throw new IllegalArgumentException("La versión del hecho perdería historia");
        for (var previo : anterior.reportes()) {
            var nuevo = actualizado.reportes().stream().filter(r -> r.id().equals(previo.id())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("La desvinculación de reportes requiere coordinar su destino y reserva"));
            if (!continua(previo.revisiones(), nuevo.revisiones()) || !continua(previo.aclaraciones(), nuevo.aclaraciones())
                    || (previo.inicioAtencion().isPresent() && !previo.inicioAtencion().equals(nuevo.inicioAtencion()))
                    || (previo.anulacion().isPresent() && !previo.anulacion().equals(nuevo.anulacion())))
                throw new IllegalArgumentException("La versión del reporte perdería historia");
        }
        validarReportesUnicos(actualizado);
        var nuevos = new LinkedHashMap<>(hechos);
        nuevos.put(actualizado.id(), actualizado);
        var antes = politica.restriccionesActualizadas(contextoAnterior, cambio.fecha());
        var provisional = copiarEstado(nuevos, casos, inmediatas, antes);
        if (!provisional.proteccion().permite(provisional.contextoDeHecho(actualizado.id(), Optional.empty()), cambio.fecha()))
            throw new SecurityException("El contenido propuesto incorpora datos bloqueados");
        var actuales = new PoliticaAcceso(directorio, antes, proteccion()).restriccionesActualizadas(
                provisional.contextoDeHecho(actualizado.id(), Optional.empty()), cambio.fecha());
        return copiarEstado(nuevos, casos, inmediatas, actuales);
    }

    public RegistroAtenciones excluirUsuario(UUID casoId, UUID exclusionId, UUID usuarioId,
                                             DirectorioUsuarios directorio, RegistroCambio cambio) {
        return excluirUsuario(casoId, exclusionId, usuarioId, new AmbitoReserva(Set.of(), Set.of(), Set.of(casoId)), directorio, cambio);
    }
    public RegistroAtenciones excluirUsuario(UUID casoId, UUID exclusionId, UUID usuarioId, AmbitoReserva origen,
                                             DirectorioUsuarios directorio, RegistroCambio cambio) {
        var actuales = new PoliticaAcceso(directorio, restricciones, proteccion()).excluir(exclusionId, usuarioId, contexto(casoId), origen, cambio);
        return copiarEstado(hechos, casos, inmediatas, actuales);
    }

    public RegistroAtenciones retirarExclusion(UUID casoId, UUID exclusionId, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var actuales = new PoliticaAcceso(directorio, restricciones, proteccion()).retirarExclusion(exclusionId, contexto(casoId), cambio);
        return copiarEstado(hechos, casos, inmediatas, actuales);
    }

    public RegistroAtenciones abrirCaso(UUID casoId, Set<UUID> hechosIds, LocalDate fechaInicio, UUID responsableId,
                                        List<Actuacion> actuaciones, Optional<CierreLocal> cierre,
                                        DirectorioUsuarios directorio, RegistroCambio cambio) {
        if (casos.containsKey(casoId)) throw new IllegalArgumentException("La atención ya existe; no se duplica un guardado");
        hechosIds = normalizar(hechosIds);
        validarHechos(hechosIds);
        if (hechosIds.stream().anyMatch(id -> hechos.get(id).anulado())) throw new IllegalArgumentException("No se inicia atención sobre un hecho sin reportes válidos");
        var nuevo = CasoAtencion.abrir(casoId, responsableId, fechaInicio, hechosIds, cambio);
        var contexto = contextoCon(nuevo);
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), Permiso.ATENDER, contexto, cambio.fecha()));
        PoliticaAcceso.exigir(politica.evaluar(responsableId, Permiso.ATENDER, contexto, cambio.fecha()));
        var idsActuaciones = new HashSet<UUID>();
        for (var actuacion : List.copyOf(actuaciones)) {
            if (!idsActuaciones.add(actuacion.id())) throw new IllegalArgumentException("La misma actuación no se incorpora dos veces");
            validarIdentidadesActuacion(actuacion, Optional.empty());
            nuevo = nuevo.registrarActuacion(actuacion, cambio);
        }
        if (cierre.isPresent()) nuevo = nuevo.cerrar(cierre.get(), cambio, this::hechoCanonico);
        return guardar(nuevo, politica.restriccionesActualizadas(contexto, cambio.fecha()), cambio);
    }

    public RegistroAtenciones vincularHechos(UUID casoId, Set<UUID> nuevosHechos, DirectorioUsuarios directorio, RegistroCambio cambio) {
        nuevosHechos = normalizar(nuevosHechos);
        validarHechos(nuevosHechos);
        var nuevo = requerirCaso(casoId).vincular(nuevosHechos, cambio);
        var actuales = autorizar(nuevo, Permiso.RELACIONAR_REPORTES, directorio, cambio);
        return guardar(nuevo, actuales, cambio);
    }

    public RegistroAtenciones retirarVinculo(UUID casoId, UUID hechoId, DirectorioUsuarios directorio, RegistroCambio cambio) {
        hechoId = hechoCanonico(hechoId);
        var antes = autorizar(requerirCaso(casoId), Permiso.RELACIONAR_REPORTES, directorio, cambio);
        var nuevo = requerirCaso(casoId).retirar(hechoId, cambio);
        var provisional = copiarEstado(hechos, casos, inmediatas, antes);
        return provisional.guardar(nuevo, provisional.autorizar(nuevo, Permiso.RELACIONAR_REPORTES, directorio, cambio), cambio);
    }

    public RegistroAtenciones citarAntecedente(UUID casoId, UUID anteriorId, DirectorioUsuarios directorio, RegistroCambio cambio) {
        if (requerirCaso(anteriorId).estado() != CasoAtencion.Estado.CERRADA)
            throw new IllegalStateException("La atención previa pendiente debe resolverse expresamente; no se fusionan atenciones");
        var nuevo = requerirCaso(casoId).citar(anteriorId, cambio);
        return guardar(nuevo, autorizar(nuevo, Permiso.ATENDER, directorio, cambio), cambio);
    }

    public RegistroAtenciones registrarActuacion(UUID casoId, Actuacion actuacion, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var anterior = requerirCaso(casoId);
        if (anterior.esActuacionFormal(actuacion.id())) throw new SecurityException("La revisión de una actuación formal conserva su vía y autoría de comité");
        validarIdentidadesActuacion(actuacion, Optional.of(casoId));
        var nuevo = anterior.registrarActuacion(actuacion, cambio);
        return guardar(nuevo, autorizar(nuevo, Permiso.ATENDER, directorio, cambio), cambio);
    }

    public RegistroAtenciones registrarDerivacion(UUID casoId, Derivacion derivacion, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var anterior = requerirCaso(casoId);
        if (anterior.encargado() == EncargadoAtencion.COMITE || anterior.esDerivacionFormal(derivacion.id()))
            throw new SecurityException("La derivación del comité requiere registro formal");
        if (casos().stream().filter(c -> !c.id().equals(casoId)).flatMap(c -> c.derivaciones().stream())
                .anyMatch(d -> d.id().equals(derivacion.id()))) throw new IllegalArgumentException("La misma derivación no se copia a otro caso");
        var caso = requerirCaso(casoId);
        var nuevo = caso.registrarDerivacion(derivacion, cambio, this::hechoCanonico);
        return guardar(nuevo, autorizar(nuevo, Permiso.ATENDER, directorio, cambio), cambio);
    }

    public RegistroAtenciones cerrarCaso(UUID casoId, CierreLocal cierre, DirectorioUsuarios directorio, RegistroCambio cambio) {
        if (requerirCaso(casoId).encargado() == EncargadoAtencion.COMITE)
            throw new SecurityException("El cierre del comité requiere registro formal");
        var nuevo = requerirCaso(casoId).cerrar(cierre, cambio, this::hechoCanonico);
        return guardar(nuevo, autorizar(nuevo, Permiso.ATENDER, directorio, cambio), cambio);
    }

    public RegistroAtenciones corregirCierre(UUID casoId, Optional<CierreLocal> cierre, DirectorioUsuarios directorio, RegistroCambio cambio) {
        if (requerirCaso(casoId).encargado() == EncargadoAtencion.COMITE || requerirCaso(casoId).tieneCierreFormal())
            throw new SecurityException("La corrección del cierre del comité requiere registro formal");
        var nuevo = requerirCaso(casoId).corregirCierre(cierre, cambio, this::hechoCanonico);
        return guardar(nuevo, autorizar(nuevo, Permiso.ATENDER, directorio, cambio), cambio);
    }

    /** Composición institucional derivada de designaciones; no crea un comité particular por caso. */
    public ComposicionComite composicionComite(UUID casoId, UUID usuarioId, DirectorioUsuarios directorio, Instant ahora) {
        consultarCaso(casoId, usuarioId, directorio, ahora);
        return ComposicionComite.vigente(directorio, ahora);
    }

    public RegistroAtenciones convocarComite(UUID casoId, UUID convocatoriaId, LocalDate fecha, String motivoEspecial,
                                              Optional<String> motivoSustitucion, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var caso = requerirCaso(casoId);
        var contexto = conSustitucion(contexto(casoId), motivoSustitucion);
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        var decision = politica.evaluar(cambio.autorId(), Permiso.CONVOCAR_COMITE, contexto, cambio.fecha());
        PoliticaAcceso.exigir(decision);
        var actuales = politica.restriccionesActualizadas(contexto, cambio.fecha());
        var composicion = ComposicionComite.vigente(directorio, cambio.fecha());
        var elegibles = composicion.usuariosIds().stream().filter(id -> !actuales.estaExcluido(id, contexto.ambito()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        var nuevo = caso.convocar(new ConvocatoriaComite(convocatoriaId, fecha, motivoEspecial, composicion,
                elegibles, cambio, decision.motivoSustitucion()), cambio);
        return guardar(nuevo, actuales, cambio);
    }

    /** La fecha real y los participantes se distinguen del instante de documentación y de quien lo registra. */
    public RegistroAtenciones registrarIntervencionComite(UUID casoId, UUID intervencionId, UUID convocatoriaId,
                                                           Instant ocurridaEn, Set<UUID> participantes,
                                                           Optional<String> motivoSustitucion,
                                                           DirectorioUsuarios directorio, RegistroCambio cambio) {
        return registrarIntervencionComite(casoId, intervencionId, convocatoriaId, ocurridaEn, participantes,
                motivoSustitucion, Map.of(), List.of(), Optional.empty(), directorio, cambio);
    }

    /** Reunión, decisiones, actuaciones/compromisos y cierre opcional en una sola transición. */
    public RegistroAtenciones registrarIntervencionComite(UUID casoId, UUID intervencionId, UUID convocatoriaId,
                                                           Instant ocurridaEn, Set<UUID> participantes,
                                                           Optional<String> motivoSustitucion, Map<UUID, String> decisiones,
                                                           List<Actuacion> actuaciones, Optional<CierreLocal> cierre,
                                                           DirectorioUsuarios directorio, RegistroCambio cambio) {
        var formal = autorizarFormal(casoId, motivoSustitucion, directorio, cambio);
        for (var participante : Set.copyOf(participantes)) {
            var usuario = directorio.usuario(participante).orElseThrow(() -> new IllegalArgumentException("Participante desconocido"));
            var roles = usuario.rolesVigentes(ocurridaEn);
            if (!roles.contains(RolUsuario.DIRECTOR) && !roles.contains(RolUsuario.MIEMBRO_COMITE))
                throw new IllegalArgumentException("El participante no integraba el comité al intervenir");
            if (formal.restricciones().exclusiones().stream().anyMatch(e -> e.usuarioId().equals(participante)
                    && e.ambito().seCruzaCon(contexto(casoId).ambito()) && !e.registro().fecha().isAfter(ocurridaEn)
                    && e.retiro().map(r -> r.fecha().isAfter(ocurridaEn)).orElse(true)))
                throw new SecurityException("Un participante estaba excluido del ámbito al intervenir");
        }
        var nuevo = requerirCaso(casoId).intervenir(new IntervencionComite(intervencionId, convocatoriaId,
                ocurridaEn, participantes, cambio, formal.decision().motivoSustitucion()), cambio);
        for (var decision : Map.copyOf(decisiones).entrySet()) {
            nuevo = nuevo.decidir(new DecisionComite(decision.getKey(), intervencionId, decision.getValue(), cambio,
                    formal.decision().motivoSustitucion()), cambio);
            nuevo = nuevo.registrarActoComite(acto(ActoComite.Tipo.DECISION, decision.getKey(), intervencionId, formal, cambio), cambio);
        }
        var ids = new HashSet<UUID>();
        for (var actuacion : List.copyOf(actuaciones)) {
            if (!ids.add(actuacion.id())) throw new IllegalArgumentException("Actuación repetida en la intervención");
            validarIdentidadesActuacion(actuacion, Optional.of(casoId));
            nuevo = nuevo.registrarActuacion(actuacion, cambio);
            validarFechaActo(nuevo, intervencionId, actuacion.fechaEfectiva());
            nuevo = nuevo.registrarActoComite(acto(ActoComite.Tipo.ACTUACION, actuacion.id(), intervencionId, formal, cambio), cambio);
        }
        if (cierre.isPresent()) {
            nuevo = nuevo.cerrar(cierre.get(), cambio, this::hechoCanonico);
            validarFechaActo(nuevo, intervencionId, Optional.of(cierre.get().fecha()));
            nuevo = nuevo.registrarActoComite(acto(ActoComite.Tipo.CIERRE, casoId, intervencionId, formal, cambio), cambio);
        }
        return guardar(nuevo, formal.restricciones(), cambio);
    }

    public RegistroAtenciones registrarDecisionComite(UUID casoId, UUID decisionId, UUID intervencionId, String contenido,
                                                       Optional<String> motivoSustitucion, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var formal = autorizarFormal(casoId, motivoSustitucion, directorio, cambio);
        var nuevo = requerirCaso(casoId).decidir(new DecisionComite(decisionId, intervencionId, contenido,
                cambio, formal.decision().motivoSustitucion()), cambio);
        return guardar(nuevo.registrarActoComite(acto(ActoComite.Tipo.DECISION, decisionId, intervencionId, formal, cambio), cambio), formal.restricciones(), cambio);
    }

    public RegistroAtenciones registrarActuacionComite(UUID casoId, Actuacion actuacion, UUID intervencionId,
                                                       Optional<String> motivoSustitucion, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var formal = autorizarFormal(casoId, motivoSustitucion, directorio, cambio);
        validarIdentidadesActuacion(actuacion, Optional.of(casoId));
        var nuevo = requerirCaso(casoId).registrarActuacion(actuacion, cambio);
        validarFechaActo(nuevo, intervencionId, actuacion.fechaEfectiva());
        return guardar(nuevo.registrarActoComite(acto(ActoComite.Tipo.ACTUACION, actuacion.id(), intervencionId, formal, cambio), cambio), formal.restricciones(), cambio);
    }

    public RegistroAtenciones registrarDerivacionComite(UUID casoId, Derivacion derivacion, UUID intervencionId,
                                                        Optional<String> motivoSustitucion, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var formal = autorizarFormal(casoId, motivoSustitucion, directorio, cambio);
        if (casos().stream().filter(c -> !c.id().equals(casoId)).flatMap(c -> c.derivaciones().stream()).anyMatch(d -> d.id().equals(derivacion.id())))
            throw new IllegalArgumentException("La misma derivación no se copia a otro caso");
        var anterior = requerirCaso(casoId);
        var nuevo = anterior.registrarDerivacion(derivacion, cambio, this::hechoCanonico);
        validarFechaActo(nuevo, intervencionId, derivacion.fechaEfectiva());
        nuevo = nuevo.registrarActoComite(acto(ActoComite.Tipo.DERIVACION, derivacion.id(), intervencionId, formal, cambio), cambio);
        if (anterior.estado() == CasoAtencion.Estado.PENDIENTE && nuevo.estado() == CasoAtencion.Estado.CERRADA)
            nuevo = nuevo.registrarActoComite(acto(ActoComite.Tipo.CIERRE, casoId, intervencionId, formal, cambio), cambio);
        return guardar(nuevo, formal.restricciones(), cambio);
    }

    public RegistroAtenciones cerrarCasoComite(UUID casoId, CierreLocal cierre, UUID intervencionId,
                                               Optional<String> motivoSustitucion, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var formal = autorizarFormal(casoId, motivoSustitucion, directorio, cambio);
        var nuevo = requerirCaso(casoId).cerrar(cierre, cambio, this::hechoCanonico);
        validarFechaActo(nuevo, intervencionId, Optional.of(cierre.fecha()));
        return guardar(nuevo.registrarActoComite(acto(ActoComite.Tipo.CIERRE, casoId, intervencionId, formal, cambio), cambio), formal.restricciones(), cambio);
    }

    public RegistroAtenciones corregirCierreComite(UUID casoId, Optional<CierreLocal> cierre, UUID intervencionId,
                                                  Optional<String> motivoSustitucion, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var formal = autorizarFormal(casoId, motivoSustitucion, directorio, cambio);
        var nuevo = requerirCaso(casoId).corregirCierre(cierre, cambio, this::hechoCanonico);
        validarFechaActo(nuevo, intervencionId, cierre.map(CierreLocal::fecha));
        return guardar(nuevo.registrarActoComite(acto(ActoComite.Tipo.CORRECCION_CIERRE, casoId, intervencionId, formal, cambio), cambio), formal.restricciones(), cambio);
    }

    public RegistroAtenciones reasignarResponsable(UUID casoId, UUID nuevoResponsable, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var caso = requerirCaso(casoId);
        var contexto = contexto(casoId);
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), Permiso.ATENDER, contexto, cambio.fecha()));
        PoliticaAcceso.exigir(politica.evaluar(nuevoResponsable, Permiso.ATENDER, contexto, cambio.fecha()));
        return guardar(caso.reasignar(nuevoResponsable, cambio), politica.restriccionesActualizadas(contexto, cambio.fecha()), cambio);
    }

    /** Selección explícita de la atención vigente; conserva cada historia y sus trabajos sin fusionarlos. */
    public RegistroAtenciones continuarAtencion(UUID origenId, UUID destinoId, Set<UUID> hechosIds,
                                                DirectorioUsuarios directorio, RegistroCambio cambio) {
        hechosIds = normalizar(hechosIds);
        var origen = requerirCaso(origenId);
        var destino = requerirCaso(destinoId);
        if (destino.estado() != CasoAtencion.Estado.PENDIENTE) throw new IllegalStateException("El destino debe estar pendiente");
        var nuevaFuente = origen.continuarEn(destinoId, hechosIds, cambio);
        var nuevoDestino = destino.vincular(hechosIds, cambio).citar(origenId, cambio);
        var propuestos = new LinkedHashMap<>(casos);
        propuestos.put(origenId, nuevaFuente);
        propuestos.put(destinoId, nuevoDestino);
        var contexto = cerrarAmbito(Set.of(origenId, destinoId), Set.of(), propuestos, Optional.empty());
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), Permiso.RELACIONAR_REPORTES, contexto, cambio.fecha()));
        var actuales = politica.restriccionesActualizadas(contexto, cambio.fecha());
        propuestos.put(origenId, concluirContinuidadSiCorresponde(nuevaFuente, inmediatas, cambio));
        validarPendientes(propuestos, inmediatas);
        return copiarEstado(hechos, propuestos, inmediatas, actuales);
    }

    /** Relaciona fuentes de dos hechos existentes; el destino de atención se decide, no se fusionan actuaciones. */
    public RegistroAtenciones relacionarDuplicados(UUID principalId, UUID origenId, Optional<UUID> casoContinuidad,
                                                   DirectorioUsuarios directorio, RegistroCambio cambio) {
        var principal = hechoVigente(principalId);
        var origen = hechoVigente(origenId);
        for (var id : Set.of(principal.id(), origen.id())) {
            var pendiente = indice.pendientesPorHecho().get(id);
            if (pendiente != null && !casos.containsKey(pendiente)) throw new IllegalArgumentException("Debe cargarse la atención existente que decidirá la continuidad");
        }
        if (principal.id().equals(origen.id())) throw new IllegalArgumentException("Los reportes ya representan el mismo hecho");
        var casosAfectados = casos().stream().filter(c -> normalizar(c.hechosReservados()).stream()
                .anyMatch(Set.of(principal.id(), origen.id())::contains)).toList();
        if (casosAfectados.stream().anyMatch(c -> c.ultimaModificacion().isAfter(cambio.fecha())))
            throw new IllegalArgumentException("Una atención afectada contiene cambios posteriores a la relación");
        var contexto = cerrarAmbito(casosAfectados.stream().map(CasoAtencion::id).collect(java.util.stream.Collectors.toSet()),
                Set.of(principal.id(), origen.id()), casos, Optional.empty());
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), Permiso.RELACIONAR_REPORTES, contexto, cambio.fecha()));
        var pendientes = casos().stream().filter(c -> c.estado() == CasoAtencion.Estado.PENDIENTE)
                .filter(c -> normalizar(c.hechosActuales()).stream().anyMatch(Set.of(principal.id(), origen.id())::contains)).toList();
        if (pendientes.size() > 1 && casoContinuidad.isEmpty())
            throw new IllegalArgumentException("Debe elegirse en qué atención continuará el hecho");
        Optional<UUID> destino = casoContinuidad.isPresent() ? casoContinuidad : pendientes.stream().findFirst().map(CasoAtencion::id);
        if (destino.isPresent() && pendientes.stream().noneMatch(c -> c.id().equals(destino.get())))
            throw new IllegalArgumentException("El destino debe ser una atención pendiente de alguno de los hechos");
        var consolidado = principal;
        for (var reporte : origen.reportes()) consolidado = consolidado.vincularDuplicado(reporte, cambio);
        var fuentes = new LinkedHashMap<>(hechos);
        fuentes.remove(origen.id());
        fuentes.put(principal.id(), consolidado);
        var relaciones = new ArrayList<>(duplicidades);
        var antecedentesGenerados = new HashSet<UUID>();
        if (destino.isPresent()) for (var caso : casos()) if (!caso.id().equals(destino.get())
                && normalizar(caso.hechosReservados()).stream().anyMatch(Set.of(principal.id(), origen.id())::contains)
                && !requerirCaso(destino.get()).antecedentes().contains(caso.id())) antecedentesGenerados.add(caso.id());
        relaciones.add(new Duplicidad(origen, principal.id(), destino, antecedentesGenerados, cambio, Optional.empty()));
        var propuestos = new LinkedHashMap<>(casos);
        if (destino.isPresent()) {
            var elegido = requerirCaso(destino.get()).reemplazarReferenciaDuplicada(origen.id(), principal.id(), cambio)
                    .vincular(Set.of(principal.id()), cambio).reservarFuenteDuplicada(origen.id(), cambio);
            for (var anterior : pendientes) if (!anterior.id().equals(elegido.id())) {
                var retirados = anterior.hechosActuales().stream().filter(id -> Set.of(principal.id(), origen.id()).contains(hechoCanonico(id)))
                        .collect(java.util.stream.Collectors.toSet());
                propuestos.put(anterior.id(), anterior.continuarEn(elegido.id(), retirados, cambio));
                elegido = elegido.citar(anterior.id(), cambio);
            }
            for (var anterior : casos()) if (!anterior.id().equals(elegido.id()) && anterior.estado() == CasoAtencion.Estado.CERRADA
                    && normalizar(anterior.hechosReservados()).stream().anyMatch(Set.of(principal.id(), origen.id())::contains))
                elegido = elegido.citar(anterior.id(), cambio);
            propuestos.put(elegido.id(), elegido);
        }
        var provisional = new RegistroAtenciones(fuentes, propuestos, inmediatas,
                politica.restriccionesActualizadas(contexto, cambio.fecha()), relaciones, indice, proteccion);
        for (var caso : List.copyOf(propuestos.values())) propuestos.put(caso.id(), provisional.concluirContinuidadSiCorresponde(caso, inmediatas, cambio));
        provisional = new RegistroAtenciones(fuentes, propuestos, inmediatas, provisional.restricciones, relaciones, indice, proteccion);
        provisional.validarPendientes(propuestos, inmediatas);
        var actuales = new PoliticaAcceso(directorio, provisional.restricciones, provisional.proteccion()).restriccionesActualizadas(
                provisional.contextoDeHechos(Set.of(principal.id())), cambio.fecha());
        return new RegistroAtenciones(fuentes, propuestos, inmediatas, actuales, relaciones, indice, proteccion);
    }

    /** Deshace una identificación equivocada; conserva las revisiones recibidas y no reabre atenciones. */
    public RegistroAtenciones separarDuplicado(UUID origenId, Optional<UUID> casoDelSeparado,
                                               DirectorioUsuarios directorio, RegistroCambio cambio) {
        cambio.exigirMotivo();
        var relacion = duplicidades.stream().filter(d -> d.origen().id().equals(origenId) && d.correccion().isEmpty())
                .reduce((a, b) -> b).orElseThrow(() -> new IllegalArgumentException("No hay duplicidad vigente de ese origen"));
        if (!hechos.containsKey(relacion.principalId())) throw new IllegalStateException("Debe corregirse primero la duplicidad posterior que absorbió al principal");
        var casosAfectados = casos().stream().filter(c -> c.hechosHistoricos().contains(origenId)
                || relacion.casoContinuidad().filter(c.id()::equals).isPresent()).map(CasoAtencion::id).collect(java.util.stream.Collectors.toSet());
        var contexto = cerrarAmbito(casosAfectados, Set.of(relacion.principalId()), casos, Optional.empty());
        if (casoDelSeparado.isPresent()) contexto = contextoDocumento(casoDelSeparado.get(), contexto.ambito());
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), Permiso.RELACIONAR_REPORTES, contexto, cambio.fecha()));
        var principal = hechoVigente(relacion.principalId());
        var fuente = relacion.origen();
        var ids = fuente.reportes().stream().map(ReporteIncidencia::id).collect(java.util.stream.Collectors.toSet());
        if (ids.contains(principal.principal().id())) throw new IllegalStateException("Seleccione expresamente un principal válido que permanezca antes de separar");
        for (var previo : fuente.reportes()) {
            var actual = principal.reportes().stream().filter(r -> r.id().equals(previo.id())).findFirst().orElseThrow();
            fuente = fuente.actualizarReporte(actual);
            principal = principal.retirarDuplicado(previo.id(), cambio);
        }
        var fuentes = new LinkedHashMap<>(hechos);
        fuentes.put(principal.id(), principal);
        fuentes.put(fuente.id(), fuente);
        var relaciones = duplicidades.stream().map(d -> d == relacion
                ? new Duplicidad(d.origen(), d.principalId(), d.casoContinuidad(), d.antecedentesGenerados(), d.registro(), Optional.of(cambio)) : d).toList();
        var propuestos = new LinkedHashMap<>(casos);
        var antecedentesEquivocados = relacion.antecedentesGenerados().stream().filter(id -> {
            var previo = requerirCaso(id);
            return previo.hechosHistoricos().contains(origenId) && (!previo.hechosHistoricos().contains(relacion.principalId())
                    || previo.continuidades().stream().anyMatch(c -> c.registro().equals(relacion.registro()) && c.hechosIds().contains(origenId)));
        }).collect(java.util.stream.Collectors.toSet());
        for (var caso : casos()) if (caso.hechosHistoricos().contains(origenId) || relacion.casoContinuidad().filter(caso.id()::equals).isPresent())
            propuestos.put(caso.id(), caso.corregirRelacionDuplicada(origenId,
                    relacion.casoContinuidad().filter(caso.id()::equals).isPresent() ? antecedentesEquivocados : Set.of(), relacion.registro(), cambio));
        if (casoDelSeparado.isPresent()) {
            var elegido = propuestos.get(casoDelSeparado.get()).vincular(Set.of(origenId), cambio);
            for (var anterior : List.copyOf(propuestos.values())) if (!anterior.id().equals(elegido.id())
                    && anterior.estado() == CasoAtencion.Estado.PENDIENTE && anterior.hechosActuales().contains(origenId)) {
                propuestos.put(anterior.id(), anterior.continuarEn(elegido.id(), Set.of(origenId), cambio));
                elegido = elegido.citar(anterior.id(), cambio);
            }
            propuestos.put(elegido.id(), elegido);
        }
        var provisional = new RegistroAtenciones(fuentes, propuestos, inmediatas,
                politica.restriccionesActualizadas(contexto, cambio.fecha()), relaciones, indice, proteccion);
        provisional.validarPendientes(propuestos, inmediatas);
        var actuales = new PoliticaAcceso(directorio, provisional.restricciones, provisional.proteccion()).restriccionesActualizadas(
                provisional.contextoDeHechos(Set.of(principal.id(), fuente.id())), cambio.fecha());
        return new RegistroAtenciones(fuentes, propuestos, inmediatas, actuales, relaciones, indice, proteccion);
    }

    public RegistroAtenciones registrarInmediata(UUID reporteId, Actuacion actuacion, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var hecho = hechoDeReporte(reporteId);
        var contexto = contextoDeHecho(hecho.id(), Optional.of(reporteId));
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), Permiso.REGISTRAR_ACCION_INMEDIATA, contexto, cambio.fecha()));
        if (actuacion.estado() != Actuacion.Estado.REALIZADA) throw new IllegalArgumentException("La acción inmediata ya debe estar realizada");
        validarCompromisosInmediatos(hecho, actuacion);
        if (!actuacion.hechosIds().isEmpty() && !actuacion.hechosIds().equals(Set.of(hecho.id())))
            throw new IllegalArgumentException("La acción inmediata no incorpora otros hechos");
        validarIdentidadesActuacion(actuacion, Optional.empty());
        return guardarInmediata(reporteId, actuacion, politica.restriccionesActualizadas(contexto, cambio.fecha()), cambio);
    }

    public RegistroAtenciones actualizarInmediata(UUID reporteId, Actuacion actualizada, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var anterior = inmediatas.getOrDefault(reporteId, List.of()).stream().filter(a -> a.id().equals(actualizada.id())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La acción inmediata no existe en ese aporte"));
        if (!actualizada.esContinuacionDe(anterior)) throw new IllegalArgumentException("Se perdería historia de la acción inmediata");
        var compromisos = actualizada.compromisos().stream().map(Compromiso::id).collect(java.util.stream.Collectors.toSet());
        if (java.util.stream.Stream.concat(casos().stream().flatMap(c -> c.actuaciones().stream()), inmediatas.values().stream().flatMap(List::stream))
                .filter(a -> !a.id().equals(actualizada.id())).flatMap(a -> a.compromisos().stream())
                .anyMatch(c -> compromisos.contains(c.id()))) throw new IllegalArgumentException("Un compromiso no se copia a otra actuación");
        var contexto = contextoDeHecho(hechoDeReporte(reporteId).id(), Optional.of(reporteId));
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        var permisoPropio = politica.evaluar(cambio.autorId(), Permiso.REGISTRAR_ACCION_INMEDIATA, contexto, cambio.fecha());
        if (!permisoPropio.permitida()) PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), Permiso.ATENDER, contexto, cambio.fecha()));
        if (permisoPropio.permitida()) validarCompromisosInmediatos(hechoDeReporte(reporteId), actualizada);
        return guardarInmediata(reporteId, actualizada, politica.restriccionesActualizadas(contexto, cambio.fecha()), cambio);
    }

    public CasoAtencion consultarCaso(UUID casoId, UUID usuarioId, DirectorioUsuarios directorio, Instant ahora) {
        if (requerirCaso(casoId).ultimaModificacion().isAfter(ahora)) throw new IllegalArgumentException("El caso contiene cambios posteriores al instante de consulta");
        PoliticaAcceso.exigir(new PoliticaAcceso(directorio, restricciones, proteccion()).evaluar(usuarioId, Permiso.CONSULTAR_HECHO, contexto(casoId), ahora));
        return requerirCaso(casoId);
    }

    /** La lectura del aporte propio sigue disponible tras la atención; no proyecta revisiones institucionales. */
    public List<AccionInmediataPropia> inmediatasDelAutor(UUID reporteId, UUID usuarioId, DirectorioUsuarios directorio, Instant ahora) {
        var contexto = contextoDeHecho(hechoDeReporte(reporteId).id(), Optional.of(reporteId));
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        PoliticaAcceso.exigir(politica.evaluar(usuarioId, Permiso.CONSULTAR_APORTE_PROPIO, contexto, ahora));
        var acciones = inmediatas.getOrDefault(reporteId, List.of());
        if (acciones.stream().anyMatch(a -> a.ultimaModificacion().isAfter(ahora)))
            throw new IllegalArgumentException("La actuación contiene cambios futuros");
        return acciones.stream().map(a -> new AccionInmediataPropia(a.id(), a.responsableId(), a.revisiones().stream()
                .filter(r -> r.cambio().autorId().equals(usuarioId))
                .map(r -> new RevisionPropia(r.estado(), r.relato(), r.fechaPrevista(), r.fechaEfectiva(), r.compromisos().stream()
                        .map(c -> new CompromisoPropio(c.id(), c.revisiones().stream()
                                .filter(v -> v.cambio().autorId().equals(usuarioId)).toList()))
                        .filter(c -> !c.revisionesPropias().isEmpty()).toList(), r.cambio())).toList()))
                .filter(a -> !a.revisionesPropias().isEmpty()).toList();
    }

    /** Referencias a acciones inmediatas y propias: una acción conserva un único ID y una sola ocurrencia. */
    public List<Actuacion> actuacionesDelCaso(UUID casoId, UUID usuarioId, DirectorioUsuarios directorio, Instant ahora) {
        return actuacionesDelCaso(casoId, usuarioId, new PoliticaAcceso(directorio, restricciones, proteccion()), ahora);
    }
    public List<Actuacion> actuacionesDelCaso(UUID casoId, UUID usuarioId, PoliticaAcceso politicaActual, Instant ahora) {
        var caso = requerirCaso(casoId);
        if (caso.ultimaModificacion().isAfter(ahora)) throw new IllegalArgumentException("El caso contiene cambios futuros");
        PoliticaAcceso.exigir(politicaActual.evaluar(usuarioId, Permiso.CONSULTAR_HECHO, contexto(casoId), ahora));
        if (restricciones.estaExcluido(usuarioId, contexto(casoId).ambito())) throw new SecurityException("Usuario excluido del ámbito");
        if (!proteccion().permite(contexto(casoId), ahora)) throw new SecurityException("Ámbito con bloqueo jurídico");
        var resultado = new LinkedHashMap<UUID, Actuacion>();
        caso.actuaciones().forEach(a -> resultado.put(a.id(), a));
        for (var hechoId : caso.hechosReservados()) for (var reporte : hechoVigente(hechoId).reportes())
            inmediatas.getOrDefault(reporte.id(), List.of()).forEach(a -> resultado.put(a.id(), a));
        if (resultado.values().stream().anyMatch(a -> a.ultimaModificacion().isAfter(ahora)))
            throw new IllegalArgumentException("La actuación contiene cambios futuros");
        return resultado.values().stream().sorted(java.util.Comparator.comparing(a -> a.id().toString())).toList();
    }

    public List<Actuacion> inmediatasDelHecho(UUID hechoId, UUID usuarioId, PoliticaAcceso politicaActual, Instant ahora) {
        PoliticaAcceso.exigir(politicaActual.evaluar(usuarioId, Permiso.CONSULTAR_HECHO, contextoDeHechos(Set.of(hechoId)), ahora));
        if (restricciones.estaExcluido(usuarioId, contextoDeHechos(Set.of(hechoId)).ambito())) throw new SecurityException("Usuario excluido del ámbito");
        if (!proteccion().permite(contextoDeHechos(Set.of(hechoId)), ahora)) throw new SecurityException("Ámbito con bloqueo jurídico");
        var resultado = hechoVigente(hechoId).reportes().stream().flatMap(r -> inmediatas.getOrDefault(r.id(), List.of()).stream())
                .distinct().sorted(java.util.Comparator.comparing(Actuacion::id)).toList();
        if (resultado.stream().anyMatch(a -> a.ultimaModificacion().isAfter(ahora)))
            throw new IllegalArgumentException("La actuación contiene cambios futuros");
        return resultado;
    }

    public List<Hecho> hechosDelCaso(UUID casoId, UUID usuarioId, DirectorioUsuarios directorio, Instant ahora) {
        var caso = consultarCaso(casoId, usuarioId, directorio, ahora);
        return normalizar(caso.hechosReservados()).stream().sorted().map(this::hechoVigente).toList();
    }

    public List<CasoAtencion> conTrabajoPendiente(UUID usuarioId, DirectorioUsuarios directorio, Instant ahora) {
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        return casos().stream().filter(c -> c.requiereTrabajo() || tieneInmediatasPendientes(c))
                .filter(c -> politica.evaluar(usuarioId, Permiso.CONSULTAR_HECHO, contexto(c.id()), ahora).permitida()).toList();
    }

    public ContextoAcceso contexto(UUID casoId) { return contextoCon(requerirCaso(casoId)); }

    /** Un documento conservado protege también las fuentes que contenía antes de corregir vínculos. */
    public ContextoAcceso contextoDocumento(UUID casoId, AmbitoReserva fuentesConservadas) {
        var actual = contexto(casoId);
        var ids = new HashSet<>(fuentesConservadas.hechosIds());
        ids.addAll(fuentesConservadas.reportesIds());
        var adicionales = ids.isEmpty() ? actual : contextoDeHechos(ids);
        var porId = new LinkedHashMap<UUID, Hecho>();
        actual.hechos().forEach(h -> porId.put(h.id(), h));
        adicionales.hechos().forEach(h -> porId.put(h.id(), h));
        return new ContextoAcceso(actual.ambito().unir(adicionales.ambito()).unir(fuentesConservadas),
                List.copyOf(porId.values()), Optional.empty(), Optional.empty());
    }

    /** Cierra las relaciones institucionales incluso cuando se recibe el ID de un aporte absorbido. */
    public ContextoAcceso contextoDeHechos(Set<UUID> ids) {
        var actuales = ids.stream().map(this::hechoCanonico).map(id -> hechos.containsKey(id) ? id : hechoDeReporte(id).id())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return cerrarAmbito(Set.of(), actuales, casos, Optional.empty());
    }

    private ContextoAcceso contextoCon(CasoAtencion candidato) {
        var propuestos = new LinkedHashMap<>(casos);
        propuestos.put(candidato.id(), candidato);
        return cerrarAmbito(Set.of(candidato.id()), candidato.hechosReservados(), propuestos, Optional.empty());
    }
    private ContextoAcceso contextoDeHecho(UUID hechoId, Optional<UUID> reporte) {
        return cerrarAmbito(Set.of(), Set.of(hechoId), casos, reporte);
    }
    private ContextoAcceso cerrarAmbito(Set<UUID> casosIniciales, Set<UUID> hechosIniciales,
                                        Map<UUID, CasoAtencion> universo, Optional<UUID> reporte) {
        var casosConContenido = new HashSet<>(casosIniciales);
        var hechosRelacionados = new HashSet<>(normalizar(hechosIniciales));
        boolean crecio;
        do {
            crecio = false;
            for (var caso : universo.values()) {
                if (casosConContenido.contains(caso.id())) {
                    crecio |= hechosRelacionados.addAll(normalizar(caso.hechosReservados()));
                    crecio |= casosConContenido.addAll(caso.antecedentes());
                }
            }
        } while (crecio);
        if (!universo.keySet().containsAll(casosConContenido)) throw new IllegalArgumentException("Falta el contenido de un antecedente consultado");
        var casosRelacionados = new HashSet<>(casosConContenido);
        for (var hecho : hechosRelacionados) casosRelacionados.addAll(indice.reservasPorHecho().getOrDefault(hecho, Set.of()));
        for (var caso : universo.values()) if (normalizar(caso.hechosReservados()).stream().anyMatch(hechosRelacionados::contains))
            casosRelacionados.add(caso.id());
        var fuentes = hechosRelacionados.stream().sorted().map(hechos::get).toList();
        for (var relacion : duplicidades) if (relacion.correccion().isEmpty() && hechosRelacionados.contains(hechoCanonico(relacion.origen().id())))
            hechosRelacionados.add(relacion.origen().id());
        return new ContextoAcceso(new AmbitoReserva(hechosRelacionados, Set.of(), casosRelacionados), fuentes, reporte, Optional.empty());
    }

    private RestriccionesAmbito autorizar(CasoAtencion candidato, Permiso permiso, DirectorioUsuarios directorio, RegistroCambio cambio) {
        var contexto = contextoCon(candidato);
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), permiso, contexto, cambio.fecha()));
        return politica.restriccionesActualizadas(contexto, cambio.fecha());
    }
    private record AutorizacionFormal(PoliticaAcceso.Decision decision, RestriccionesAmbito restricciones) { }
    private AutorizacionFormal autorizarFormal(UUID casoId, Optional<String> motivoSustitucion,
                                                DirectorioUsuarios directorio, RegistroCambio cambio) {
        if (requerirCaso(casoId).encargado() != EncargadoAtencion.COMITE)
            throw new IllegalStateException("El comité debe haber sido convocado expresamente en este caso");
        var contexto = conSustitucion(contexto(casoId), motivoSustitucion);
        var politica = new PoliticaAcceso(directorio, restricciones, proteccion());
        var decision = politica.evaluar(cambio.autorId(), Permiso.REGISTRAR_ACTO_COMITE, contexto, cambio.fecha());
        PoliticaAcceso.exigir(decision);
        return new AutorizacionFormal(decision, politica.restriccionesActualizadas(contexto, cambio.fecha()));
    }
    private static ContextoAcceso conSustitucion(ContextoAcceso contexto, Optional<String> motivo) {
        Objects.requireNonNull(motivo, "motivoSustitucion");
        return motivo.map(contexto::conMotivoSustitucion).orElse(contexto);
    }
    private static ActoComite acto(ActoComite.Tipo tipo, UUID referenciaId, UUID intervencionId,
                                    AutorizacionFormal formal, RegistroCambio cambio) {
        return new ActoComite(tipo, referenciaId, intervencionId, cambio, formal.decision().motivoSustitucion());
    }
    private static void validarFechaActo(CasoAtencion caso, UUID intervencionId, Optional<LocalDate> fecha) {
        var intervencion = caso.intervencionesComite().stream().filter(i -> i.id().equals(intervencionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe la intervención efectiva"));
        if (fecha.filter(f -> f.isBefore(intervencion.fecha())).isPresent())
            throw new IllegalArgumentException("El acto formal no precede a la intervención efectiva");
    }
    private RegistroAtenciones guardar(CasoAtencion candidato, RestriccionesAmbito actuales, RegistroCambio cambio) {
        var anterior = casos.get(candidato.id());
        for (var actuacion : candidato.actuaciones()) {
            var previa = anterior == null ? Optional.<Actuacion>empty() : anterior.actuaciones().stream().filter(a -> a.id().equals(actuacion.id())).findFirst();
            validarAutoriasActuacion(actuacion, previa, cambio);
        }
        for (var derivacion : candidato.derivaciones()) {
            var previa = anterior == null ? Optional.<Derivacion>empty() : anterior.derivaciones().stream().filter(d -> d.id().equals(derivacion.id())).findFirst();
            int conservadas = previa.map(d -> d.revisiones().size()).orElse(0);
            derivacion.revisiones().stream().skip(conservadas).forEach(r -> validarAutorRegistro(r.cambio(), cambio));
        }
        var nuevos = new LinkedHashMap<>(casos);
        nuevos.put(candidato.id(), concluirContinuidadSiCorresponde(candidato, inmediatas, cambio));
        validarPendientes(nuevos, inmediatas);
        var fuentes = new LinkedHashMap<>(hechos);
        for (var hechoId : normalizar(candidato.hechosReservados())) {
            var hecho = fuentes.get(hechoId);
            for (var reporte : hecho.reportes()) if (!reporte.anulado() && reporte.inicioAtencion().isEmpty())
                hecho = hecho.actualizarReporte(reporte.marcarAtencionInstitucional(cambio));
            fuentes.put(hechoId, hecho);
        }
        var resultado = copiarEstado(fuentes, nuevos, inmediatas, actuales);
        if (!resultado.casoDisponible(candidato.id(), cambio.fecha())) throw new SecurityException("El contenido propuesto incorpora datos bloqueados");
        return resultado;
    }
    private RegistroAtenciones guardarInmediata(UUID reporteId, Actuacion actuacion, RestriccionesAmbito actuales, RegistroCambio cambio) {
        if (actuacion.ultimaModificacion().isAfter(cambio.fecha())) throw new IllegalArgumentException("La acción es posterior al guardado");
        var anterior = inmediatas.getOrDefault(reporteId, List.of()).stream().filter(a -> a.id().equals(actuacion.id())).findFirst();
        validarAutoriasActuacion(actuacion, anterior, cambio);
        var acciones = new LinkedHashMap<>(inmediatas);
        var lista = new ArrayList<>(acciones.getOrDefault(reporteId, List.of()));
        lista.removeIf(a -> a.id().equals(actuacion.id()));
        lista.add(actuacion);
        acciones.put(reporteId, lista);
        var nuevos = new LinkedHashMap<>(casos);
        casos.forEach((id, caso) -> nuevos.put(id, concluirContinuidadSiCorresponde(caso, acciones, cambio)));
        validarPendientes(nuevos, acciones);
        var resultado = copiarEstado(hechos, nuevos, acciones, actuales);
        if (!resultado.proteccion().permite(resultado.contextoDeHechos(Set.of(hechoDeReporte(reporteId).id())), cambio.fecha()))
            throw new SecurityException("La acción propuesta incorpora datos bloqueados");
        return resultado;
    }
    private static void validarCompromisosInmediatos(Hecho hecho, Actuacion actuacion) {
        boolean cotidiana = hecho.clasificacionVigente().filter(c -> c.naturaleza() == NaturalezaCategoria.CONDUCTA_DISCIPLINARIA
                && c.gravedad() == Gravedad.LEVE).isPresent();
        if (!cotidiana && !actuacion.compromisos().isEmpty())
            throw new IllegalArgumentException("La protección inmediata no incorpora acuerdos de atención formal");
    }

    private static void validarAutoriasActuacion(Actuacion actual, Optional<Actuacion> anterior, RegistroCambio guardado) {
        var compromisos = new LinkedHashMap<UUID, Compromiso>();
        anterior.ifPresent(a -> a.compromisos().forEach(c -> compromisos.put(c.id(), c)));
        int conservadas = anterior.map(a -> a.revisiones().size()).orElse(0);
        for (var revision : actual.revisiones().subList(conservadas, actual.revisiones().size())) {
            validarAutorRegistro(revision.cambio(), guardado);
            for (var compromiso : revision.compromisos()) {
                var previo = compromisos.put(compromiso.id(), compromiso);
                int conocidas = previo == null ? 0 : previo.revisiones().size();
                compromiso.revisiones().stream().skip(conocidas).forEach(r -> validarAutorRegistro(r.cambio(), guardado));
            }
        }
    }

    private static void validarAutorRegistro(RegistroCambio registro, RegistroCambio guardado) {
        if (!registro.autorId().equals(guardado.autorId()) || registro.fecha().isAfter(guardado.fecha()))
            throw new IllegalArgumentException("Los registros nuevos conservan al operador real y no pueden ser futuros");
    }
    private CasoAtencion concluirContinuidadSiCorresponde(CasoAtencion caso, Map<UUID, List<Actuacion>> acciones, RegistroCambio cambio) {
        if (caso.estado() != CasoAtencion.Estado.PENDIENTE || !caso.hechosActuales().isEmpty()
                || caso.continuidadHacia().isEmpty() || caso.tieneSeguimientoPendiente() || tieneInmediatasPendientes(caso, acciones)) return caso;
        var motivo = "La atención vigente continúa en el caso " + caso.continuidadHacia().orElseThrow();
        var registro = new RegistroCambio(cambio.autorId(), cambio.fecha(), Optional.of(motivo));
        return caso.cerrar(new CierreLocal(cambio.fecha().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate(), motivo,
                CierreLocal.TipoCierre.DUPLICIDAD, registro, Optional.empty()), registro);
    }
    private void validarPendientes(Map<UUID, CasoAtencion> universo, Map<UUID, List<Actuacion>> acciones) {
        var pendientes = new HashSet<UUID>();
        for (var caso : universo.values()) if (caso.estado() == CasoAtencion.Estado.PENDIENTE) {
            if (caso.hechosActuales().isEmpty() && (caso.continuidadHacia().isEmpty()
                    || (!caso.tieneSeguimientoPendiente() && !tieneInmediatasPendientes(caso, acciones))))
                throw new IllegalStateException("La fuente sin hechos vigentes solo queda pendiente por trabajos previos trazables");
            for (var hechoId : normalizar(caso.hechosActuales())) if (!pendientes.add(hechoId))
                throw new IllegalStateException("Un hecho solo puede tener una atención local pendiente");
        }
    }
    private void validarHechos(Set<UUID> ids) {
        if (!hechos.keySet().containsAll(ids)) throw new IllegalArgumentException("Se refieren hechos inexistentes");
    }
    private CasoAtencion requerirCaso(UUID casoId) {
        return caso(casoId).orElseThrow(() -> new IllegalArgumentException("La atención no existe"));
    }
    private Hecho hechoDeReporte(UUID reporteId) {
        return hechos.values().stream().filter(h -> h.reportes().stream().anyMatch(r -> r.id().equals(reporteId))).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El reporte no existe"));
    }
    private boolean tieneInmediatasPendientes(CasoAtencion caso) {
        return tieneInmediatasPendientes(caso, inmediatas);
    }
    private boolean tieneInmediatasPendientes(CasoAtencion caso, Map<UUID, List<Actuacion>> acciones) {
        return normalizar(caso.hechosReservados()).stream().map(this::hechoVigente).flatMap(h -> h.reportes().stream())
                .flatMap(r -> acciones.getOrDefault(r.id(), List.of()).stream()).anyMatch(Actuacion::pendiente);
    }
    private void validarIdentidadesActuacion(Actuacion candidata, Optional<UUID> casoDestino) {
        var existentes = new ArrayList<Actuacion>();
        inmediatas.values().forEach(existentes::addAll);
        for (var caso : casos()) for (var a : caso.actuaciones()) {
            if (a.id().equals(candidata.id()) && !casoDestino.filter(caso.id()::equals).isPresent())
                throw new IllegalArgumentException("Una actuación no se copia entre atenciones");
            existentes.add(a);
        }
        if (inmediatas.values().stream().flatMap(List::stream).anyMatch(a -> a.id().equals(candidata.id())))
            throw new IllegalArgumentException("La acción inmediata debe referenciarse sin copiarse como actuación");
        var idsCompromisos = candidata.compromisos().stream().map(Compromiso::id).collect(java.util.stream.Collectors.toSet());
        if (existentes.stream().filter(a -> !a.id().equals(candidata.id())).flatMap(a -> a.compromisos().stream())
                .anyMatch(c -> idsCompromisos.contains(c.id()))) throw new IllegalArgumentException("El mismo compromiso no se duplica en otra actuación");
    }

    private void validarReportesUnicos(Hecho candidato) {
        var ids = candidato.reportes().stream().map(ReporteIncidencia::id).collect(java.util.stream.Collectors.toSet());
        if (hechos.values().stream().filter(h -> !h.id().equals(candidato.id())).flatMap(h -> h.reportes().stream())
                .anyMatch(r -> ids.contains(r.id()))) throw new IllegalArgumentException("Un reporte no se incorpora a dos hechos");
    }

    private static boolean continua(List<?> anterior, List<?> nuevo) {
        return nuevo.size() >= anterior.size() && nuevo.subList(0, anterior.size()).equals(anterior);
    }
}
