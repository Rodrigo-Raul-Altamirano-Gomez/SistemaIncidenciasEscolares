package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.incidencia.ReporteIncidencia;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Personas que intervinieron efectivamente; no equivale a convocar a toda la composición. */
public record IntervencionComite(UUID id, UUID convocatoriaId, Instant ocurridaEn, Set<UUID> participantesIds,
                                 RegistroCambio registro, Optional<String> motivoSustitucion) {
    public IntervencionComite {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(convocatoriaId, "convocatoriaId");
        Objects.requireNonNull(ocurridaEn, "ocurridaEn");
        Objects.requireNonNull(registro, "registro");
        motivoSustitucion = Objects.requireNonNull(motivoSustitucion, "motivoSustitucion");
        if (motivoSustitucion.filter(String::isBlank).isPresent()) throw new IllegalArgumentException("Motivo vacío");
        participantesIds = Set.copyOf(participantesIds);
        if (participantesIds.isEmpty()) throw new IllegalArgumentException("Debe conocerse al menos un participante efectivo");
        if (ocurridaEn.isAfter(registro.fecha()))
            throw new IllegalArgumentException("La intervención efectiva no puede ser futura");
    }
    public LocalDate fecha() { return ocurridaEn.atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate(); }
}
