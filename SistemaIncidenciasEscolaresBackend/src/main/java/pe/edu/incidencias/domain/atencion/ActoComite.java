package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Procedencia colectiva de un dato de atención; sus versiones conservan autor real y sustitución. */
public record ActoComite(Tipo tipo, UUID referenciaId, UUID intervencionId,
                         RegistroCambio registro, Optional<String> motivoSustitucion) {
    public enum Tipo { ACTUACION, DERIVACION, DECISION, CIERRE, CORRECCION_CIERRE }
    public ActoComite {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(referenciaId, "referenciaId");
        Objects.requireNonNull(intervencionId, "intervencionId");
        Objects.requireNonNull(registro, "registro");
        motivoSustitucion = Objects.requireNonNull(motivoSustitucion, "motivoSustitucion");
        if (motivoSustitucion.filter(String::isBlank).isPresent()) throw new IllegalArgumentException("Motivo de sustitución vacío");
    }
}
