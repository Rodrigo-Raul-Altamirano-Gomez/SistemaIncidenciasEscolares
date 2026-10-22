package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.util.Objects;
import java.util.UUID;

public record CambioResponsable(UUID anteriorId, UUID nuevoId, RegistroCambio registro) {
    public CambioResponsable {
        Objects.requireNonNull(anteriorId, "anteriorId");
        Objects.requireNonNull(nuevoId, "nuevoId");
        Objects.requireNonNull(registro, "registro");
        if (anteriorId.equals(nuevoId)) throw new IllegalArgumentException("El responsable no ha cambiado");
    }
}
