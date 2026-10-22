package pe.edu.incidencias.domain.estudiante;

import java.util.Objects;
import java.util.Optional;

/** Grupo informado, incluso parcialmente; la ausencia total se representa con Optional.empty(). */
public record GrupoEscolar(Optional<String> grado, Optional<String> seccion) {
    public GrupoEscolar {
        grado = validar(grado, "grado");
        seccion = validar(seccion, "seccion");
        if (grado.isEmpty() && seccion.isEmpty()) {
            throw new IllegalArgumentException("Un grupo informado necesita grado o sección.");
        }
    }

    public GrupoEscolar(String grado, String seccion) {
        this(Optional.of(grado), Optional.of(seccion));
    }

    private static Optional<String> validar(Optional<String> valor, String campo) {
        Objects.requireNonNull(valor, campo);
        return valor.map(texto -> {
            if (texto.isBlank()) {
                throw new IllegalArgumentException(campo + " no puede estar vacío.");
            }
            return texto.strip();
        });
    }
}
