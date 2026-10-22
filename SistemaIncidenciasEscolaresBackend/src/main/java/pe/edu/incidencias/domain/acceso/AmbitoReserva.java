package pe.edu.incidencias.domain.acceso;

import pe.edu.incidencias.domain.incidencia.Hecho;
import pe.edu.incidencias.domain.incidencia.ReporteIncidencia;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Referencias que revelan el mismo asunto; no se extiende por compartir estudiante. */
public record AmbitoReserva(Set<UUID> hechosIds, Set<UUID> reportesIds, Set<UUID> casosIds) {
    public AmbitoReserva {
        hechosIds = Set.copyOf(Objects.requireNonNull(hechosIds, "hechosIds"));
        reportesIds = Set.copyOf(Objects.requireNonNull(reportesIds, "reportesIds"));
        casosIds = Set.copyOf(Objects.requireNonNull(casosIds, "casosIds"));
        if (hechosIds.isEmpty() && reportesIds.isEmpty() && casosIds.isEmpty())
            throw new IllegalArgumentException("El ámbito necesita al menos una referencia");
    }

    /** Incluye todas las fuentes, también anuladas, y no depende del principal seleccionado. */
    public static AmbitoReserva deHecho(Hecho hecho) {
        Objects.requireNonNull(hecho, "hecho");
        return new AmbitoReserva(Set.of(hecho.id()), hecho.reportes().stream()
                .map(ReporteIncidencia::id).collect(Collectors.toUnmodifiableSet()), Set.of());
    }

    public AmbitoReserva unir(AmbitoReserva otro) {
        Objects.requireNonNull(otro, "otro");
        return new AmbitoReserva(union(hechosIds, otro.hechosIds), union(reportesIds, otro.reportesIds),
                union(casosIds, otro.casosIds));
    }

    public boolean seCruzaCon(AmbitoReserva otro) {
        Objects.requireNonNull(otro, "otro");
        return seCruzan(hechosIds, otro.hechosIds) || seCruzan(reportesIds, otro.reportesIds)
                || seCruzan(casosIds, otro.casosIds);
    }

    private static Set<UUID> union(Set<UUID> primero, Set<UUID> segundo) {
        var resultado = new HashSet<>(primero);
        resultado.addAll(segundo);
        return resultado;
    }

    private static boolean seCruzan(Set<UUID> primero, Set<UUID> segundo) {
        return primero.stream().anyMatch(segundo::contains);
    }
}
