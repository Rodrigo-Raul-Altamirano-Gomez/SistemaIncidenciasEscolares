package pe.edu.incidencias.domain.alerta;

import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.catalogo.Gravedad;
import pe.edu.incidencias.domain.incidencia.Hecho;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Proyección pura, sin persistencia ni entrega de mensajes. Autoriza de nuevo en cada consulta. */
public final class BandejaAtencion {
    private BandejaAtencion() { }

    public static List<EntradaBandeja> proyectar(UUID usuarioId, RegistroAtenciones atenciones,
                                                 RegistroAlertas alertas, DirectorioUsuarios directorio, Instant ahora) {
        return proyectar(usuarioId, atenciones, alertas, directorio, Map.of(), ahora);
    }

    /** Los contextos suplementarios solo amplían la reserva; las versiones actuales proceden del registro. */
    public static List<EntradaBandeja> proyectar(UUID usuarioId, RegistroAtenciones atenciones,
                                                 RegistroAlertas alertas, DirectorioUsuarios directorio,
                                                 Map<UUID, ContextoAcceso> suplementarios, Instant ahora) {
        var politica = new PoliticaAcceso(directorio, atenciones.restricciones(), atenciones.proteccion());
        var contextos = new LinkedHashMap<UUID, ContextoAcceso>();
        for (var alerta : alertas.alertas()) if (alerta.pendiente()) {
            var actual = atenciones.contextoDeHechos(alerta.hechosHistoricos());
            var extra = suplementarios.get(alerta.id());
            if (extra != null) {
                var fuentes = new LinkedHashMap<UUID, Hecho>();
                extra.hechos().forEach(h -> fuentes.put(h.id(), h));
                actual.hechos().forEach(h -> fuentes.put(h.id(), h));
                actual = new ContextoAcceso(actual.ambito().unir(extra.ambito()), List.copyOf(fuentes.values()),
                        Optional.empty(), extra.motivoSustitucion());
            }
            contextos.put(alerta.id(), actual);
        }
        var pendientes = alertas.pendientes(usuarioId, contextos, politica, ahora);
        var casos = atenciones.conTrabajoPendiente(usuarioId, directorio, ahora);
        var asignadas = new HashSet<UUID>();
        var resultado = new ArrayList<EntradaBandeja>();
        for (var caso : casos) {
            atenciones.consultarCaso(caso.id(), usuarioId, directorio, ahora);
            var relacionadas = pendientes.stream().filter(a -> !asignadas.contains(a.id()))
                    .filter(a -> caso.estado() == CasoAtencion.Estado.PENDIENTE)
                    .filter(a -> contextoContieneActivador(contextos.get(a.id()), a, caso.hechosActuales())).toList();
            relacionadas.forEach(a -> asignadas.add(a.id()));
            var fundamentosDelCaso = new ArrayList<>(relacionadas);
            for (var historica : alertas.alertas()) if (historica.disposicion().filter(d -> d.tipo() == DisposicionAlerta.Tipo.ATENCION
                    && d.referenciaId().filter(caso.id()::equals).isPresent()).isPresent()) {
                var contexto = atenciones.contextoDeHechos(historica.hechosHistoricos());
                if (politica.evaluar(usuarioId, Permiso.CONSULTAR_HECHO, contexto, ahora).permitida())
                    fundamentosDelCaso.add(alertas.consultar(historica.id(), contexto, politica, usuarioId, ahora));
            }
            var acciones = atenciones.actuacionesDelCaso(caso.id(), usuarioId, directorio, ahora);
            var previstas = acciones.stream().filter(a -> a.estado() == Actuacion.Estado.PREVISTA).map(Actuacion::id).collect(Collectors.toSet());
            var acuerdos = acciones.stream().flatMap(a -> a.compromisos().stream()).filter(Compromiso::pendienteSeguimiento).toList();
            var derivaciones = caso.derivaciones().stream().filter(d -> d.estado() == Derivacion.Estado.PREPARADA).map(Derivacion::id).collect(Collectors.toSet());
            var vencimientos = new ArrayList<LocalDate>();
            acciones.stream().filter(a -> a.estado() == Actuacion.Estado.PREVISTA).flatMap(a -> a.fechaPrevista().stream()).forEach(vencimientos::add);
            acuerdos.stream().flatMap(a -> a.fechaObjetivo().stream()).forEach(vencimientos::add);
            var hechos = atenciones.hechosDelCaso(caso.id(), usuarioId, directorio, ahora);
            var gravedades = new HashSet<Gravedad>();
            for (var hecho : hechos) {
                if (hecho.clasificacionVigente().isPresent()) gravedades.add(hecho.clasificacionVigente().orElseThrow().gravedad());
                else hecho.reportes().stream().filter(r -> !r.anulado()).forEach(r -> gravedades.add(r.datos().clasificacion().gravedad()));
            }
            var desde = relacionadas.stream().map(Alerta::creadaEn).min(Comparator.naturalOrder())
                    .filter(f -> f.isBefore(caso.apertura().fecha())).orElse(caso.apertura().fecha());
            resultado.add(new EntradaBandeja(caso.id(), Optional.of(caso.id()), ids(relacionadas), motivos(fundamentosDelCaso), gravedades,
                    caso.hechosHistoricos(), desde, Optional.of(caso.responsableId()), vencimientos.stream().min(Comparator.naturalOrder()),
                    caso.estado() == CasoAtencion.Estado.PENDIENTE, previstas,
                    acuerdos.stream().map(Compromiso::id).collect(Collectors.toSet()), derivaciones));
        }
        for (var alerta : pendientes) if (!asignadas.contains(alerta.id())) {
            var gravedades = alerta.fundamentosVigentes().values().stream().filter(FundamentoAlerta::vigente)
                    .flatMap(f -> f.clasificaciones().values().stream()).map(c -> c.gravedad()).collect(Collectors.toSet());
            resultado.add(new EntradaBandeja(alerta.id(), Optional.empty(), Set.of(alerta.id()), alerta.motivos(), gravedades,
                    alerta.hechosVigentes(), alerta.creadaEn(), Optional.empty(), Optional.empty(), false, Set.of(), Set.of(), Set.of()));
        }
        return resultado.stream().sorted(Comparator.comparing(EntradaBandeja::desde).thenComparing(e -> e.asuntoId().toString())).toList();
    }
    private static boolean contextoContieneActivador(ContextoAcceso contexto, Alerta alerta, Set<UUID> hechosCaso) {
        return contexto.hechos().stream().filter(h -> hechosCaso.contains(h.id())).anyMatch(h -> alerta.hechosActivadores().contains(h.id())
                || h.reportes().stream().anyMatch(r -> alerta.hechosActivadores().contains(r.id())));
    }
    private static Set<UUID> ids(List<Alerta> alertas) { return alertas.stream().map(Alerta::id).collect(Collectors.toUnmodifiableSet()); }
    private static Set<MotivoAlerta> motivos(List<Alerta> alertas) {
        return alertas.stream().flatMap(a -> a.motivos().stream()).collect(Collectors.toUnmodifiableSet());
    }
}
