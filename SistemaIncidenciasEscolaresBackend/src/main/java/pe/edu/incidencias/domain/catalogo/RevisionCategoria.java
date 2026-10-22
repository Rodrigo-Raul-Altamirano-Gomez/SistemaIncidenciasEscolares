package pe.edu.incidencias.domain.catalogo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Estado completo posterior a un cambio, incluida la creación. */
public record RevisionCategoria(
        DefinicionCategoria definicion,
        boolean activa,
        UUID autorId,
        Instant fecha) {

    public RevisionCategoria {
        Objects.requireNonNull(definicion, "La definición es obligatoria");
        Objects.requireNonNull(autorId, "El autor es obligatorio");
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
    }
}
