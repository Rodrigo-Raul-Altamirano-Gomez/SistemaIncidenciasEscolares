package pe.edu.incidencias.domain.acceso;

import pe.edu.incidencias.domain.incidencia.*;
import pe.edu.incidencias.domain.proteccion.ControlProteccion;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Política pura sobre instantáneas actuales. No autentica sesiones ni persiste las decisiones. */
public final class PoliticaAcceso {
    public enum Resultado { PERMITIDO, CUENTA_NO_VIGENTE, ROL_NO_AUTORIZADO, FALTA_AMBITO,
        EXCLUIDO, APORTE_AJENO, REPORTE_NO_DISPONIBLE, CORRECCION_TRAS_ATENCION, FALTA_MOTIVO_SUSTITUCION,
        BLOQUEO_JURIDICO }
    public record Decision(Resultado resultado, boolean porSustitucion, Optional<String> motivoSustitucion) {
        public boolean permitida() { return resultado == Resultado.PERMITIDO; }
    }
    /** Vista propia sin aportes ajenos, revisiones institucionales, motivos de exclusión ni autorías de atención. */
    public record AportePropio(UUID reporteId, UUID autorId, Instant fechaRegistro,
                               List<ReporteIncidencia.Revision> revisionesPropias,
                               List<ReporteIncidencia.AclaracionAutor> aclaracionesPropias,
                               boolean relacionadoParaAtencion, boolean anulado) {
        public AportePropio {
            revisionesPropias = List.copyOf(revisionesPropias);
            aclaracionesPropias = List.copyOf(aclaracionesPropias);
        }
    }
    public record Destinatarios(List<UUID> usuarios, boolean requiereDesignacion) {
        public Destinatarios { usuarios = List.copyOf(usuarios); }
    }
    private record Identificacion(UUID cuentaId, Instant fecha) { }

    private final DirectorioUsuarios directorio;
    private final RestriccionesAmbito restricciones;
    private final ControlProteccion proteccion;

    public PoliticaAcceso(DirectorioUsuarios directorio, RestriccionesAmbito restricciones) {
        this(directorio, restricciones, ControlProteccion.vacio());
    }

    public PoliticaAcceso(DirectorioUsuarios directorio, RestriccionesAmbito restricciones, ControlProteccion proteccion) {
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.restricciones = Objects.requireNonNull(restricciones, "restricciones");
        this.proteccion = Objects.requireNonNull(proteccion, "proteccion");
    }

    /** Filtro jurídico para poblaciones: no concede permiso funcional ni acceso a expedientes. */
    public boolean puedeConsultarPersona(UUID personaId, Instant ahora) {
        return proteccion.permitePersona(personaId, ahora);
    }
    public void exigirPersonaDisponible(UUID personaId, Instant ahora) {
        if (!puedeConsultarPersona(personaId, ahora)) throw new SecurityException("Persona con bloqueo jurídico vigente");
    }
    public void exigirRecurso(UUID recursoId, Instant ahora) {
        if (!proteccion.permiteRecurso(recursoId, ahora)) throw new SecurityException("Recurso con bloqueo jurídico vigente");
    }
    public Decision evaluarPersona(UUID usuario, Permiso permiso, UUID personaId, Instant ahora) {
        validarInstante(ahora);
        Decision decision;
        if (permiso == Permiso.CONSULTAR_ESTADISTICAS || permiso == Permiso.CONSULTAR_CONTEXTO_INSTITUCIONAL
                || permiso == Permiso.CONSULTAR_HECHO || permiso == Permiso.EXPORTAR_ATENCION) {
            var cuenta = directorio.usuario(usuario).filter(u -> u.activaEn(ahora));
            decision = cuenta.isEmpty() ? denegar(Resultado.CUENTA_NO_VIGENTE)
                    : permitirSi(esInstitucional(cuenta.get().rolesVigentes(ahora)));
        } else decision = evaluar(usuario, permiso, ahora);
        return decision.permitida() && !puedeConsultarPersona(personaId, ahora)
                ? denegar(Resultado.BLOQUEO_JURIDICO) : decision;
    }

    public Decision evaluar(UUID usuarioId, Permiso permiso, Instant ahora) {
        return evaluar(usuarioId, permiso, Optional.empty(), ahora);
    }
    public Decision evaluar(UUID usuarioId, Permiso permiso, ContextoAcceso contexto, Instant ahora) {
        return evaluar(usuarioId, permiso, Optional.of(contexto), ahora);
    }
    private Decision evaluar(UUID usuarioId, Permiso permiso, Optional<ContextoAcceso> contexto, Instant ahora) {
        Objects.requireNonNull(permiso, "permiso");
        validarInstante(ahora);
        var usuario = directorio.usuario(usuarioId);
        if (usuario.isEmpty() || !usuario.get().activaEn(ahora)) return denegar(Resultado.CUENTA_NO_VIGENTE);
        if (permiso.requiereAmbito() && contexto.isEmpty()) return denegar(Resultado.FALTA_AMBITO);
        if (contexto.filter(c -> !proteccion.permite(c, ahora)).isPresent()) return denegar(Resultado.BLOQUEO_JURIDICO);
        var vigentes = usuario.get().rolesVigentes(ahora);
        var efectivas = contexto.map(c -> restriccionesActualizadas(c, ahora)).orElse(restricciones);
        if (contexto.filter(c -> efectivas.estaExcluido(usuarioId, c.ambito())).isPresent()) return denegar(Resultado.EXCLUIDO);
        if (esDeAporte(permiso)) {
            var aporte = contexto.flatMap(ContextoAcceso::aporte);
            if (aporte.isEmpty()) return denegar(Resultado.REPORTE_NO_DISPONIBLE);
            if (!aporte.get().autorId().equals(usuarioId)) return denegar(Resultado.APORTE_AJENO);
            if (!puedeRegistrar(vigentes)) return denegar(Resultado.ROL_NO_AUTORIZADO);
            if (permiso == Permiso.CONSULTAR_APORTE_PROPIO) return permitir();
            if (aporte.get().anulado()) return denegar(Resultado.REPORTE_NO_DISPONIBLE);
            if (permiso == Permiso.CORREGIR_APORTE_PROPIO && aporte.get().inicioAtencion().isPresent())
                return denegar(Resultado.CORRECCION_TRAS_ATENCION);
            if (permiso == Permiso.REGISTRAR_ACCION_INMEDIATA) {
                if (aporte.get().inicioAtencion().isPresent()) return denegar(Resultado.CORRECCION_TRAS_ATENCION);
            }
            return permitir();
        }
        return switch (permiso) {
            case BUSCAR_ESTUDIANTE -> permitirSi(puedeRegistrar(vigentes) || vigentes.contains(RolUsuario.AIP));
            case REGISTRAR_INCIDENCIA -> permitirSi(puedeRegistrar(vigentes));
            case IMPORTAR_NOMINA -> permitirSi(vigentes.contains(RolUsuario.AIP));
            case ADMINISTRAR_USUARIOS -> permitirSi(vigentes.contains(RolUsuario.ADMINISTRADOR_OPERATIVO));
            case DEFINIR_CONFIGURACION -> permitirSi(vigentes.contains(RolUsuario.TOE) || vigentes.contains(RolUsuario.DIRECTOR));
            case CONVOCAR_COMITE -> {
                if (vigentes.contains(RolUsuario.TOE)) yield permitir();
                var toes = directorio.usuarios().stream().filter(u -> u.rolesVigentes(ahora).contains(RolUsuario.TOE)).toList();
                boolean conflictoToes = !toes.isEmpty() && toes.stream().allMatch(u -> efectivas.estaExcluido(u.id(), contexto.orElseThrow().ambito()));
                yield sustitucion(esComite(vigentes) && conflictoToes, contexto.orElseThrow());
            }
            case REGISTRAR_ACTO_COMITE -> {
                if (vigentes.contains(RolUsuario.DIRECTOR)) yield permitir();
                boolean directorElegible = directorio.usuarios().stream()
                        .anyMatch(u -> u.rolesVigentes(ahora).contains(RolUsuario.DIRECTOR)
                                && !efectivas.estaExcluido(u.id(), contexto.orElseThrow().ambito()));
                yield sustitucion(esComite(vigentes) && !directorElegible, contexto.orElseThrow());
            }
            case CONSULTAR_HECHO, ATENDER, ACLARAR_PARTICIPACION, CORREGIR_HECHO, RELACIONAR_REPORTES,
                    CONSULTAR_CONTEXTO_INSTITUCIONAL, CONSULTAR_DOCUMENTO_RESERVADO, GENERAR_INFORME,
                    CONSULTAR_ESTADISTICAS, EXPORTAR_ATENCION, EXCLUIR_USUARIO, RETIRAR_EXCLUSION ->
                    permitirSi(esInstitucional(vigentes));
            case CONSULTAR_APORTE_PROPIO, CORREGIR_APORTE_PROPIO, ACLARAR_APORTE_PROPIO,
                    REGISTRAR_ACCION_INMEDIATA -> throw new IllegalStateException("La operación de aporte ya debía estar evaluada");
        };
    }

    public AportePropio consultarAportePropio(UUID usuarioId, ContextoAcceso contexto, Instant ahora) {
        exigir(evaluar(usuarioId, Permiso.CONSULTAR_APORTE_PROPIO, contexto, ahora));
        var aporte = contexto.aporte().orElseThrow();
        return new AportePropio(aporte.id(), aporte.autorId(), aporte.fechaRegistro(),
                aporte.revisiones().stream().filter(r -> r.cambio().autorId().equals(usuarioId)).toList(),
                aporte.aclaraciones(), aporte.inicioAtencion().isPresent(), aporte.anulado());
    }

    /** Solo hechos autónomos; para relaciones con casos o atenciones debe usarse el cierre completo de contextos. */
    public List<Hecho> filtrarHechosConsultables(UUID usuarioId, List<Hecho> hechos, Instant ahora) {
        return List.copyOf(hechos).stream().filter(h -> evaluar(usuarioId, Permiso.CONSULTAR_HECHO,
                ContextoAcceso.deHecho(h), ahora).permitida()).toList();
    }

    /** Para consultas vinculadas a casos debe conservarse su cierre de relaciones, también al filtrar. */
    public List<ContextoAcceso> filtrarContextosConsultables(UUID usuarioId, List<ContextoAcceso> contextos, Instant ahora) {
        return List.copyOf(contextos).stream().filter(c -> evaluar(usuarioId, Permiso.CONSULTAR_HECHO, c, ahora).permitida()).toList();
    }

    /** TOE conserva recepción habitual; si está impedido se selecciona el comité elegible, sin convocarlo. */
    public Destinatarios destinatariosAtencion(ContextoAcceso contexto, Instant ahora) {
        var elegibles = directorio.usuarios().stream().filter(u -> evaluar(u.id(), Permiso.ATENDER, contexto, ahora).permitida()).toList();
        var toes = elegibles.stream().filter(u -> u.rolesVigentes(ahora).contains(RolUsuario.TOE)).toList();
        var destinatarios = (toes.isEmpty() ? elegibles.stream().filter(u -> esComite(u.rolesVigentes(ahora))).toList() : toes)
                .stream().map(Usuario::id).toList();
        return new Destinatarios(destinatarios, destinatarios.isEmpty());
    }

    public RestriccionesAmbito excluir(UUID exclusionId, UUID usuarioId, ContextoAcceso contexto, RegistroCambio cambio) {
        return excluir(exclusionId, usuarioId, contexto, contexto.ambito(), cambio);
    }

    public RestriccionesAmbito excluir(UUID exclusionId, UUID usuarioId, ContextoAcceso contexto, AmbitoReserva origen, RegistroCambio cambio) {
        exigir(evaluar(cambio.autorId(), Permiso.EXCLUIR_USUARIO, contexto, cambio.fecha()));
        if (usuarioId.equals(cambio.autorId())) throw new SecurityException("La exclusión expresa la registra otro responsable no implicado");
        if (directorio.usuario(usuarioId).isEmpty()) throw new IllegalArgumentException("La cuenta a excluir no existe");
        if (!contexto.ambito().hechosIds().containsAll(origen.hechosIds()) || !contexto.ambito().reportesIds().containsAll(origen.reportesIds())
                || !contexto.ambito().casosIds().containsAll(origen.casosIds())) throw new IllegalArgumentException("El origen no pertenece al contexto autorizado");
        return restriccionesActualizadas(contexto, cambio.fecha()).excluir(exclusionId, usuarioId, origen, cambio);
    }

    public RestriccionesAmbito retirarExclusion(UUID exclusionId, ContextoAcceso contexto, RegistroCambio cambio) {
        var actuales = restriccionesActualizadas(contexto, cambio.fecha());
        var exclusion = actuales.exclusiones().stream().filter(e -> e.id().equals(exclusionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La exclusión no existe"));
        if (!contexto.ambito().hechosIds().containsAll(exclusion.ambito().hechosIds())
                || !contexto.ambito().reportesIds().containsAll(exclusion.ambito().reportesIds())
                || !contexto.ambito().casosIds().containsAll(exclusion.ambito().casosIds()))
            throw new IllegalArgumentException("Debe aportarse el ámbito completo de la exclusión");
        exigir(evaluar(cambio.autorId(), Permiso.RETIRAR_EXCLUSION, contexto, cambio.fecha()));
        return actuales.retirar(exclusionId, cambio);
    }

    /** Persistir el resultado junto con el hecho antes de publicar avisos o habilitar derivados. */
    public RestriccionesAmbito restriccionesActualizadas(ContextoAcceso contexto, Instant ahora) {
        validarInstante(ahora);
        for (var hecho : contexto.hechos()) {
            if (hecho.vinculos().stream().anyMatch(v -> v.cambio().fecha().isAfter(ahora))
                    || hecho.precisiones().stream().anyMatch(p -> p.cambio().fecha().isAfter(ahora))
                    || hecho.cambiosPrincipal().stream().anyMatch(c -> c.cambio().fecha().isAfter(ahora)))
                throw new IllegalArgumentException("El contexto contiene decisiones futuras del hecho");
        }
        var identificaciones = new HashMap<UUID, Set<Identificacion>>();
        for (var hecho : contexto.hechos()) for (var reporte : hecho.reportes()) {
            if (reporte.ultimaModificacion().isAfter(ahora)) throw new IllegalArgumentException("El contexto contiene información futura");
            for (var revision : reporte.revisiones()) for (var p : revision.datos().participaciones())
                p.persona().usuarioId().ifPresent(cuenta -> identificaciones.computeIfAbsent(p.persona().id(), _ -> new HashSet<>())
                        .add(new Identificacion(cuenta, revision.cambio().fecha())));
        }
        var actuales = restricciones;
        for (var hecho : contexto.hechos()) for (var reporte : hecho.reportes()) {
            Set<UUID> previamenteDenunciados = Set.of();
            for (int indice = 0; indice < reporte.revisiones().size(); indice++) {
                var revision = reporte.revisiones().get(indice);
                var denunciados = new HashSet<UUID>();
                for (var p : revision.datos().participaciones()) {
                    if (p.persona().tipo() != TipoPersona.PERSONAL || !esAtribucionDenunciada(p.papel())) continue;
                    denunciados.add(p.persona().id());
                    if (previamenteDenunciados.contains(p.persona().id())) continue;
                    actuales = materializarPreventiva(actuales, identificaciones, p.persona().id(),
                            "reporte:" + reporte.id() + ":" + indice, revision.cambio(),
                            new AmbitoReserva(Set.of(), Set.of(reporte.id()), Set.of()), ahora);
                }
                previamenteDenunciados = denunciados;
            }
        }
        for (var hecho : contexto.hechos()) {
            var papelesAclarados = new HashMap<UUID, Boolean>();
            for (int indice = 0; indice < hecho.aclaraciones().size(); indice++) {
                var aclaracion = hecho.aclaraciones().get(indice);
                if (aclaracion.cambio().fecha().isAfter(ahora)) throw new IllegalArgumentException("La aclaración es futura");
                boolean antes = papelesAclarados.getOrDefault(aclaracion.personaId(), hecho.reportes().stream()
                        .map(r -> r.revisiones().stream().filter(v -> !v.cambio().fecha().isAfter(aclaracion.cambio().fecha()))
                                .reduce((anterior, siguiente) -> siguiente))
                        .flatMap(Optional::stream)
                        .flatMap(r -> r.datos().participaciones().stream())
                        .anyMatch(p -> p.persona().id().equals(aclaracion.personaId()) && esAtribucionDenunciada(p.papel())));
                boolean denunciado = esAtribucionDenunciada(aclaracion.papel());
                if (denunciado && !antes)
                    actuales = materializarPreventiva(actuales, identificaciones, aclaracion.personaId(),
                            "aclaracion:" + hecho.id() + ":" + indice, aclaracion.cambio(),
                            new AmbitoReserva(Set.of(hecho.id()), Set.of(), Set.of()), ahora);
                papelesAclarados.put(aclaracion.personaId(), denunciado);
            }
        }
        return actuales;
    }

    private RestriccionesAmbito materializarPreventiva(RestriccionesAmbito actuales,
                                                       HashMap<UUID, Set<Identificacion>> identificaciones,
                                                       UUID personaId, String fuente, RegistroCambio origen,
                                                       AmbitoReserva origenReserva, Instant ahora) {
        for (var identificacion : identificaciones.getOrDefault(personaId, Set.of())) {
            var cuenta = directorio.usuario(identificacion.cuentaId());
            var conocimiento = identificacion.fecha().isAfter(origen.fecha()) ? identificacion.fecha() : origen.fecha();
            if (cuenta.isEmpty() || (!esInstitucional(cuenta.get().rolesVigentes(ahora))
                    && !esInstitucional(cuenta.get().rolesVigentes(conocimiento)))) continue;
            var id = UUID.nameUUIDFromBytes(("preventiva:" + fuente + ":" + personaId + ":"
                    + identificacion.cuentaId()).getBytes(StandardCharsets.UTF_8));
            if (actuales.exclusiones().stream().anyMatch(e -> e.id().equals(id))) continue;
            actuales = actuales.excluir(id, identificacion.cuentaId(), origenReserva,
                    new RegistroCambio(origen.autorId(), ahora,
                            Optional.of("Restricción preventiva por atribución comunicada a personal identificado")));
        }
        return actuales;
    }

    private void validarInstante(Instant ahora) {
        Objects.requireNonNull(ahora, "ahora");
        if (directorio.movimientos().getLast().cambio().fecha().isAfter(ahora)
                || (!restricciones.historial().isEmpty() && restricciones.historial().getLast().cambio().fecha().isAfter(ahora)))
            throw new IllegalArgumentException("La evaluación no puede preceder a la configuración vigente");
    }
    private static boolean esAtribucionDenunciada(PapelParticipacion papel) {
        return papel == PapelParticipacion.PRESUNTA_RESPONSABILIDAD || papel == PapelParticipacion.ATRIBUCION_CORROBORADA;
    }
    private static boolean esDeAporte(Permiso permiso) {
        return permiso == Permiso.CONSULTAR_APORTE_PROPIO || permiso == Permiso.CORREGIR_APORTE_PROPIO
                || permiso == Permiso.ACLARAR_APORTE_PROPIO || permiso == Permiso.REGISTRAR_ACCION_INMEDIATA;
    }
    private static boolean puedeRegistrar(Set<RolUsuario> roles) {
        return esInstitucional(roles) || roles.contains(RolUsuario.DOCENTE)
                || roles.contains(RolUsuario.AUXILIAR) || roles.contains(RolUsuario.DIRECTIVO);
    }
    private static boolean esInstitucional(Set<RolUsuario> roles) {
        return roles.contains(RolUsuario.TOE) || esComite(roles);
    }
    private static boolean esComite(Set<RolUsuario> roles) {
        return roles.contains(RolUsuario.MIEMBRO_COMITE) || roles.contains(RolUsuario.DIRECTOR);
    }
    private static Decision sustitucion(boolean habilitado, ContextoAcceso contexto) {
        if (!habilitado) return denegar(Resultado.ROL_NO_AUTORIZADO);
        if (contexto.motivoSustitucion().isEmpty()) return denegar(Resultado.FALTA_MOTIVO_SUSTITUCION);
        return new Decision(Resultado.PERMITIDO, true, contexto.motivoSustitucion());
    }
    private static Decision permitir() { return new Decision(Resultado.PERMITIDO, false, Optional.empty()); }
    private static Decision denegar(Resultado resultado) { return new Decision(resultado, false, Optional.empty()); }
    private static Decision permitirSi(boolean condicion) { return condicion ? permitir() : denegar(Resultado.ROL_NO_AUTORIZADO); }
    public static void exigir(Decision decision) {
        if (!decision.permitida()) throw new SecurityException("Operación no autorizada: " + decision.resultado());
    }
}
