package pe.edu.incidencias.domain.incidencia;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** El ID de un estudiante identificado es su identidad persistente; los demás son referencias explícitas. */
public record PersonaInvolucrada(UUID id, TipoPersona tipo, String descripcion,
                                Optional<UUID> usuarioId, Optional<String> funcionORelacion) {
    public PersonaInvolucrada {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(descripcion, "descripcion");
        if (descripcion.isBlank()) throw new IllegalArgumentException("Se necesita una descripción conocida");
        usuarioId = Objects.requireNonNull(usuarioId, "usuarioId");
        funcionORelacion = Objects.requireNonNull(funcionORelacion, "funcionORelacion");
        if (usuarioId.isPresent() && tipo != TipoPersona.PERSONAL)
            throw new IllegalArgumentException("Solo el personal se relaciona con una cuenta");
        if (funcionORelacion.filter(String::isBlank).isPresent())
            throw new IllegalArgumentException("La función o relación informada no puede estar vacía");
    }

    public static PersonaInvolucrada estudiante(UUID id, String nombre) {
        return new PersonaInvolucrada(id, TipoPersona.ESTUDIANTE, nombre, Optional.empty(), Optional.empty());
    }

    public Optional<UUID> estudianteId() {
        return tipo == TipoPersona.ESTUDIANTE ? Optional.of(id) : Optional.empty();
    }
}
