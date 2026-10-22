package pe.edu.incidencias.domain.alerta;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Resultado que gestiona una señal; el texto puede proceder de la actuación ya documentada. */
public record DisposicionAlerta(Tipo tipo, String resultado, Optional<UUID> referenciaId, RegistroCambio registro) {
    public enum Tipo { ATENCION, INTERVENCION_SIN_CASO, DUPLICIDAD, DESCARTE_FUNDAMENTADO }
    public DisposicionAlerta {
        Objects.requireNonNull(tipo, "tipo");
        if (resultado == null || resultado.isBlank()) throw new IllegalArgumentException("Se requiere un resultado o fundamento suficiente");
        referenciaId = Objects.requireNonNull(referenciaId, "referenciaId");
        Objects.requireNonNull(registro, "registro");
        if ((tipo == Tipo.ATENCION || tipo == Tipo.DUPLICIDAD) && referenciaId.isEmpty())
            throw new IllegalArgumentException("La gestión debe referenciar la atención o asunto principal");
    }
}
