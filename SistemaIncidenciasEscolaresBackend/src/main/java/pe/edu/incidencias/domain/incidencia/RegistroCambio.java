package pe.edu.incidencias.domain.incidencia;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record RegistroCambio(UUID autorId, Instant fecha, Optional<String> motivo) {
    public RegistroCambio {
        Objects.requireNonNull(autorId, "autorId");
        Objects.requireNonNull(fecha, "fecha");
        motivo = Objects.requireNonNull(motivo, "motivo");
        if (motivo.filter(String::isBlank).isPresent()) throw new IllegalArgumentException("Motivo vacío");
    }
    public void exigirMotivo() {
        if (motivo.isEmpty()) throw new IllegalArgumentException("Este cambio requiere una causa breve");
    }
}
