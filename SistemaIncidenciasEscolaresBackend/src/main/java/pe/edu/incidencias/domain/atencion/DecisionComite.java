package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record DecisionComite(UUID id, UUID intervencionId, String contenido,
                             RegistroCambio registro, Optional<String> motivoSustitucion) {
    public DecisionComite {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(intervencionId, "intervencionId");
        Objects.requireNonNull(registro, "registro");
        if (contenido == null || contenido.isBlank()) throw new IllegalArgumentException("La decisión necesita contenido suficiente");
        motivoSustitucion = Objects.requireNonNull(motivoSustitucion, "motivoSustitucion");
        if (motivoSustitucion.filter(String::isBlank).isPresent()) throw new IllegalArgumentException("Motivo de sustitución vacío");
    }
}
