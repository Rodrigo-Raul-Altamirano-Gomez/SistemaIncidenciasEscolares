package pe.edu.incidencias.domain.proteccion;

import pe.edu.incidencias.domain.acceso.DirectorioUsuarios;
import pe.edu.incidencias.domain.acceso.RestriccionesAmbito;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Evidencia sin relatos/adjuntos. La aplicación guarda resultado y evidencia en la misma unidad recuperable. */
public final class RegistroEvidencia {
    public enum Operacion { INICIO_SESION, CIERRE_SESION, CONSULTA, CREACION, MODIFICACION,
        ANULACION, ELIMINACION, IMPORTACION, EXPORTACION, EXTRACCION_EVIDENCIA, RECUPERACION }
    public enum Resultado { CORRECTO, RECHAZADO, FALLIDO }
    /** Criterios tipados para un listado; nunca se guarda su texto sensible completo. */
    public record Criterios(Optional<Instant> desde, Optional<Instant> hasta, Optional<Integer> anio,
                             Set<UUID> categorias, Set<UUID> personasSeleccionadas) {
        public Criterios {
            Objects.requireNonNull(desde); Objects.requireNonNull(hasta); Objects.requireNonNull(anio);
            categorias = Set.copyOf(categorias); personasSeleccionadas = Set.copyOf(personasSeleccionadas);
            if (desde.isPresent() && hasta.isPresent() && hasta.get().isBefore(desde.get()))
                throw new IllegalArgumentException("El período es inverso");
        }
        public static Criterios ninguno() {
            return new Criterios(Optional.empty(), Optional.empty(), Optional.empty(), Set.of(), Set.of());
        }
    }
    public record Evento(UUID id, UUID usuarioId, Instant fecha, Operacion operacion, Resultado resultado,
                          AlcanceProteccion alcance, Criterios criterios, Optional<UUID> operacionRelacionada) {
        public Evento {
            Objects.requireNonNull(id); Objects.requireNonNull(usuarioId); Objects.requireNonNull(fecha);
            Objects.requireNonNull(operacion); Objects.requireNonNull(resultado); Objects.requireNonNull(alcance);
            Objects.requireNonNull(criterios); Objects.requireNonNull(operacionRelacionada);
            if (!alcance.personas().containsAll(criterios.personasSeleccionadas()))
                throw new IllegalArgumentException("El alcance debe incluir las personas identificadas por los criterios");
        }
        /** Dos años calendario, exclusivamente para evidencia; no impone plazo a los expedientes. */
        public Instant conservacionMinimaHasta() { return fecha.atZone(ZoneOffset.UTC).plusYears(2).toInstant(); }
    }
    public record Filtro(Instant desde, Instant hasta, Optional<UUID> usuario, Set<Operacion> operaciones) {
        public Filtro {
            Objects.requireNonNull(desde); Objects.requireNonNull(hasta); Objects.requireNonNull(usuario);
            operaciones = Set.copyOf(operaciones);
            if (hasta.isBefore(desde)) throw new IllegalArgumentException("El período es inverso");
        }
        boolean incluye(Evento e) {
            return !e.fecha().isBefore(desde) && !e.fecha().isAfter(hasta)
                    && usuario.map(e.usuarioId()::equals).orElse(true)
                    && (operaciones.isEmpty() || operaciones.contains(e.operacion()));
        }
    }
    public record Extraccion(List<Evento> eventos, RegistroEvidencia actualizado) {
        public Extraccion { eventos = List.copyOf(eventos); Objects.requireNonNull(actualizado); }
    }
    private final List<Evento> eventos;
    private RegistroEvidencia(List<Evento> eventos) { this.eventos = List.copyOf(eventos); }
    public static RegistroEvidencia vacio() { return new RegistroEvidencia(List.of()); }
    /** Reconstitución de evidencia almacenada por el adaptador, conservando orden y unicidad. */
    public static RegistroEvidencia cargar(List<Evento> evidencia) {
        evidencia = List.copyOf(evidencia);
        var ids = new HashSet<UUID>();
        Instant anterior = null;
        for (var evento : evidencia) {
            if (!ids.add(evento.id())) throw new IllegalArgumentException("La instantánea repite un evento");
            if (anterior != null && evento.fecha().isBefore(anterior))
                throw new IllegalArgumentException("La evidencia no puede retroceder respecto de la última operación");
            anterior = evento.fecha();
        }
        return new RegistroEvidencia(evidencia);
    }
    public RegistroEvidencia registrar(Evento evento) {
        Objects.requireNonNull(evento);
        var previo = eventos.stream().filter(e -> e.id().equals(evento.id())).findFirst();
        if (previo.isPresent()) {
            if (previo.get().equals(evento)) return this;
            throw new IllegalArgumentException("El ID de evidencia ya corresponde a otro contenido");
        }
        if (!eventos.isEmpty() && evento.fecha().isBefore(eventos.getLast().fecha()))
            throw new IllegalArgumentException("La evidencia no puede retroceder respecto de la última operación");
        var nuevos = new ArrayList<>(eventos); nuevos.add(evento); return new RegistroEvidencia(nuevos);
    }
    /** Devuelve evidencia y el nuevo registro de su extracción; persistir ambos antes de entregar la salida. */
    public Extraccion extraer(UUID eventoExtraccionId, Filtro filtro, AlcanceProteccion alcance,
                               AutorizacionProteccion autorizacion, RegistroCambio cambio,
                               DirectorioUsuarios directorio, RestriccionesAmbito restricciones, ControlProteccion control) {
        autorizacion.exigir(cambio.autorId(), AutorizacionProteccion.Operacion.EXTRAER_EVIDENCIA, alcance, cambio.fecha(), directorio);
        if (filtro.hasta().isAfter(cambio.fecha())) throw new IllegalArgumentException("El corte no puede ser futuro");
        if (!eventos.isEmpty() && eventos.getLast().fecha().isAfter(cambio.fecha())) throw new IllegalArgumentException("La evidencia contiene operaciones futuras");
        var seleccionados = eventos.stream().filter(filtro::incluye).filter(e -> alcance.contiene(e.alcance()))
                .filter(e -> control.permiteRecurso(e.id(), cambio.fecha()))
                .filter(e -> e.alcance().ambito().map(a -> !restricciones.estaExcluido(cambio.autorId(), a)).orElse(true))
                .filter(e -> e.alcance().personas().stream().allMatch(p -> control.permitePersona(p, cambio.fecha())))
                .filter(e -> e.alcance().recursos().stream().allMatch(r -> control.permiteRecurso(r, cambio.fecha())))
                .filter(e -> e.alcance().ambito().map(a -> java.util.stream.Stream.of(a.hechosIds(), a.reportesIds(), a.casosIds())
                        .flatMap(Set::stream).allMatch(r -> control.permiteRecurso(r, cambio.fecha()))).orElse(true)).toList();
        var evento = new Evento(eventoExtraccionId, cambio.autorId(), cambio.fecha(), Operacion.EXTRACCION_EVIDENCIA,
                Resultado.CORRECTO, alcance, new Criterios(Optional.of(filtro.desde()), Optional.of(filtro.hasta()),
                Optional.empty(), Set.of(), alcance.personas()), Optional.of(autorizacion.id()));
        return new Extraccion(seleccionados, registrar(evento));
    }
    /** Retiro decidido institucionalmente, nunca automático ni antes del mínimo de cada evento. */
    public RegistroEvidencia retirarVencidos(Set<UUID> ids, UUID eventoRetiroId, AutorizacionProteccion autorizacion,
                                              RegistroCambio cambio, DirectorioUsuarios directorio) {
        ids = Set.copyOf(ids); cambio.exigirMotivo();
        if (ids.isEmpty() || ids.contains(eventoRetiroId)) throw new IllegalArgumentException("Debe seleccionar evidencia concreta y un evento nuevo");
        var alcance = new AlcanceProteccion(Optional.empty(), Set.of(), ids);
        autorizacion.exigir(cambio.autorId(), AutorizacionProteccion.Operacion.DISPONER, alcance, cambio.fecha(), directorio);
        var seleccion = new ArrayList<Evento>();
        for (var evento : eventos) if (ids.contains(evento.id())) seleccion.add(evento);
        if (seleccion.size() != ids.size()) throw new IllegalArgumentException("Falta evidencia seleccionada");
        if (seleccion.stream().anyMatch(e -> cambio.fecha().isBefore(e.conservacionMinimaHasta())))
            throw new IllegalStateException("La evidencia debe conservarse al menos dos años");
        if (!eventos.isEmpty() && cambio.fecha().isBefore(eventos.getLast().fecha()))
            throw new IllegalArgumentException("El retiro precede a la última evidencia vigente");
        var restantes = new ArrayList<Evento>();
        for (var evento : eventos) if (!ids.contains(evento.id())) restantes.add(evento);
        return new RegistroEvidencia(restantes).registrar(new Evento(eventoRetiroId, cambio.autorId(), cambio.fecha(),
                Operacion.ELIMINACION, Resultado.CORRECTO, alcance, Criterios.ninguno(), Optional.of(autorizacion.id())));
    }
}
