package pe.edu.incidencias.domain.acceso;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Intervalo institucional: inicio inclusivo y término exclusivo, sin duración predeterminada. */
public record Vigencia(Instant desde, Optional<Instant> hasta) {
    public Vigencia {
        Objects.requireNonNull(desde, "desde");
        Objects.requireNonNull(hasta, "hasta");
        if (hasta.filter(fin -> !fin.isAfter(desde)).isPresent())
            throw new IllegalArgumentException("El término debe ser posterior al inicio de vigencia");
    }

    public boolean contiene(Instant instante) {
        Objects.requireNonNull(instante, "instante");
        return !instante.isBefore(desde) && hasta.map(instante::isBefore).orElse(true);
    }
}
