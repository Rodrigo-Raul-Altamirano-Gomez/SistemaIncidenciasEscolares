package pe.edu.incidencias.domain.incidencia;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import pe.edu.incidencias.domain.estudiante.DocumentoIdentidad;

/** El ID de un estudiante identificado es su identidad persistente; los demás son referencias explícitas. */
public record PersonaInvolucrada(UUID id, TipoPersona tipo, String descripcion,
                                Optional<UUID> usuarioId, Optional<String> funcionORelacion,
                                Optional<String> contacto, Optional<DocumentoIdentidad> documento) {
    public PersonaInvolucrada(UUID id, TipoPersona tipo, String descripcion, Optional<UUID> usuarioId,
                              Optional<String> funcionORelacion) {
        this(id, tipo, descripcion, usuarioId, funcionORelacion, Optional.empty(), Optional.empty());
    }
    public PersonaInvolucrada {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(descripcion, "descripcion");
        if (descripcion.isBlank()) throw new IllegalArgumentException("Se necesita una descripción conocida");
        usuarioId = Objects.requireNonNull(usuarioId, "usuarioId");
        funcionORelacion = Objects.requireNonNull(funcionORelacion, "funcionORelacion");
        contacto = Objects.requireNonNull(contacto, "contacto"); documento = Objects.requireNonNull(documento, "documento");
        if (contacto.filter(String::isBlank).isPresent()) throw new IllegalArgumentException("Contacto informado vacío");
        if ((tipo == TipoPersona.ESTUDIANTE || tipo == TipoPersona.ESTUDIANTE_NO_IDENTIFICADO)
                && (contacto.isPresent() || documento.isPresent()))
            throw new IllegalArgumentException("La identidad/contacto del estudiante se mantiene en su registro escolar");
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
