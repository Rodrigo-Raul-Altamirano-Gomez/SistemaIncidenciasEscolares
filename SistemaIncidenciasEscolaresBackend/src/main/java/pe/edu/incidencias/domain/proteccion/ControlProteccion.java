package pe.edu.incidencias.domain.proteccion;

import pe.edu.incidencias.domain.acceso.ContextoAcceso;
import pe.edu.incidencias.domain.acceso.DirectorioUsuarios;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import pe.edu.incidencias.domain.acceso.AmbitoReserva;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Controles actuales que deben aportarse a cada lectura, modificación y derivado protegido. */
public final class ControlProteccion {
    public record Bloqueo(UUID id, AlcanceProteccion alcance, String constancia, RegistroCambio registro,
                          Optional<RegistroCambio> levantamiento, Optional<String> constanciaLevantamiento) {
        public Bloqueo {
            Objects.requireNonNull(id); Objects.requireNonNull(alcance); Objects.requireNonNull(constancia);
            Objects.requireNonNull(registro); Objects.requireNonNull(levantamiento); Objects.requireNonNull(constanciaLevantamiento);
        }
        boolean vigente(Instant ahora) { return !registro.fecha().isAfter(ahora) && levantamiento.map(c -> c.fecha().isAfter(ahora)).orElse(true); }
    }
    private final List<Bloqueo> bloqueos;
    private final Map<UUID, AmbitoReserva> recursosRelacionados;
    private final Map<UUID, AmbitoReserva> personasRelacionadas;
    private ControlProteccion(List<Bloqueo> bloqueos) { this(bloqueos, Map.of(), Map.of()); }
    private ControlProteccion(List<Bloqueo> bloqueos, Map<UUID, AmbitoReserva> recursos, Map<UUID, AmbitoReserva> personas) {
        this.bloqueos = List.copyOf(bloqueos); this.recursosRelacionados = Map.copyOf(recursos);
        this.personasRelacionadas = Map.copyOf(personas);
    }
    /** Cierre de reservas de los datos embebidos en atenciones. Amplía protección; jamás crea permisos. */
    public ControlProteccion conRelaciones(Map<UUID, AmbitoReserva> recursos, Map<UUID, AmbitoReserva> personas) {
        var r = new HashMap<>(recursosRelacionados); var p = new HashMap<>(personasRelacionadas);
        recursos.forEach((id, a) -> r.merge(id, a, AmbitoReserva::unir));
        personas.forEach((id, a) -> p.merge(id, a, AmbitoReserva::unir));
        return new ControlProteccion(bloqueos, r, p);
    }
    public static ControlProteccion vacio() { return new ControlProteccion(List.of()); }
    public List<Bloqueo> bloqueos() { return bloqueos; }
    public ControlProteccion bloquear(UUID id, AlcanceProteccion alcance, AutorizacionProteccion autorizacion,
                                       RegistroCambio cambio, DirectorioUsuarios directorio) {
        validarTiempo(cambio.fecha());
        autorizacion.exigir(cambio.autorId(), AutorizacionProteccion.Operacion.BLOQUEAR, alcance, cambio.fecha(), directorio);
        cambio.exigirMotivo();
        if (bloqueos.stream().anyMatch(b -> b.id().equals(id))) throw new IllegalArgumentException("El bloqueo ya existe");
        var nuevos = new ArrayList<>(bloqueos);
        nuevos.add(new Bloqueo(id, alcance, autorizacion.constancia(), cambio, Optional.empty(), Optional.empty()));
        return new ControlProteccion(nuevos, recursosRelacionados, personasRelacionadas);
    }
    public ControlProteccion levantar(UUID id, AutorizacionProteccion autorizacion,
                                      RegistroCambio cambio, DirectorioUsuarios directorio) {
        validarTiempo(cambio.fecha()); cambio.exigirMotivo();
        var bloqueo = bloqueos.stream().filter(b -> b.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El bloqueo no existe"));
        autorizacion.exigir(cambio.autorId(), AutorizacionProteccion.Operacion.LEVANTAR_BLOQUEO,
                bloqueo.alcance(), cambio.fecha(), directorio);
        if (bloqueo.levantamiento().isPresent()) throw new IllegalStateException("El bloqueo ya fue levantado");
        return new ControlProteccion(bloqueos.stream().map(b -> b.id().equals(id)
                ? new Bloqueo(b.id(), b.alcance(), b.constancia(), b.registro(), Optional.of(cambio), Optional.of(autorizacion.constancia())) : b).toList(),
                recursosRelacionados, personasRelacionadas);
    }
    public boolean permite(ContextoAcceso contexto, Instant ahora) {
        validarTiempo(ahora);
        return bloqueos.stream().noneMatch(b -> b.vigente(ahora) && (b.alcance().afecta(contexto)
                || b.alcance().recursos().stream().map(recursosRelacionados::get).filter(Objects::nonNull)
                        .anyMatch(a -> a.seCruzaCon(contexto.ambito()))
                || b.alcance().personas().stream().map(personasRelacionadas::get).filter(Objects::nonNull)
                        .anyMatch(a -> a.seCruzaCon(contexto.ambito()))));
    }
    public boolean permitePersona(UUID persona, Instant ahora) {
        Objects.requireNonNull(persona); validarTiempo(ahora);
        return bloqueos.stream().noneMatch(b -> b.vigente(ahora) && b.alcance().personas().contains(persona));
    }
    public boolean permiteRecurso(UUID recurso, Instant ahora) {
        Objects.requireNonNull(recurso); validarTiempo(ahora);
        return bloqueos.stream().noneMatch(b -> b.vigente(ahora) && (b.alcance().recursos().contains(recurso)
                || b.alcance().ambito().filter(a -> a.hechosIds().contains(recurso) || a.reportesIds().contains(recurso)
                || a.casosIds().contains(recurso)).isPresent()));
    }
    private void validarTiempo(Instant ahora) {
        Objects.requireNonNull(ahora);
        if (bloqueos.stream().anyMatch(b -> b.registro().fecha().isAfter(ahora)
                || b.levantamiento().filter(c -> c.fecha().isAfter(ahora)).isPresent()))
            throw new IllegalArgumentException("Debe usarse el control vigente, sin decisiones futuras");
    }
}
