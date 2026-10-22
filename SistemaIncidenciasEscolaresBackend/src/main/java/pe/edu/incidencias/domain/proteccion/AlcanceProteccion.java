package pe.edu.incidencias.domain.proteccion;

import pe.edu.incidencias.domain.acceso.AmbitoReserva;
import pe.edu.incidencias.domain.acceso.ContextoAcceso;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Selección explícita de datos, nunca todos los expedientes por defecto. */
public record AlcanceProteccion(Optional<AmbitoReserva> ambito, Set<UUID> personas, Set<UUID> recursos) {
    public AlcanceProteccion {
        ambito = Objects.requireNonNull(ambito, "ambito");
        personas = Set.copyOf(personas);
        recursos = Set.copyOf(recursos);
        if (ambito.isEmpty() && personas.isEmpty() && recursos.isEmpty())
            throw new IllegalArgumentException("El alcance debe identificar datos concretos");
    }
    public static AlcanceProteccion persona(UUID persona) {
        return new AlcanceProteccion(Optional.empty(), Set.of(persona), Set.of());
    }
    public static AlcanceProteccion deAmbito(AmbitoReserva ambito) {
        return new AlcanceProteccion(Optional.of(ambito), Set.of(), Set.of());
    }
    public boolean contiene(AlcanceProteccion otro) {
        return personas.containsAll(otro.personas) && recursos.containsAll(otro.recursos)
                && (otro.ambito.isEmpty() || ambito.filter(a ->
                a.hechosIds().containsAll(otro.ambito.get().hechosIds())
                        && a.reportesIds().containsAll(otro.ambito.get().reportesIds())
                        && a.casosIds().containsAll(otro.ambito.get().casosIds())).isPresent());
    }
    public boolean afecta(ContextoAcceso contexto) {
        if (ambito.filter(a -> a.seCruzaCon(contexto.ambito())).isPresent()) return true;
        if (contexto.ambito().hechosIds().stream().anyMatch(recursos::contains)
                || contexto.ambito().reportesIds().stream().anyMatch(recursos::contains)
                || contexto.ambito().casosIds().stream().anyMatch(recursos::contains)) return true;
        // Incluye las fuentes anteriores: una rectificación no debe eludir un bloqueo vigente.
        return contexto.hechos().stream().flatMap(h -> h.reportes().stream())
                .flatMap(r -> r.revisiones().stream()).flatMap(r -> r.datos().participaciones().stream())
                .anyMatch(p -> personas.contains(p.persona().id()));
    }
}
