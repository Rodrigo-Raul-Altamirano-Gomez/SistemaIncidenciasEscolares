package pe.edu.incidencias.domain.catalogo;

import java.util.Objects;
import java.util.UUID;

/** Copia histórica de la clasificación usada al registrar un hecho. */
public record ClasificacionCategoria(
        UUID categoriaId,
        String nombre,
        NaturalezaCategoria naturaleza,
        Gravedad gravedad,
        boolean avisoInmediato) {

    public ClasificacionCategoria {
        Objects.requireNonNull(categoriaId, "La categoría es obligatoria");
        nombre = Objects.requireNonNull(nombre, "El nombre es obligatorio").strip();
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre de categoría no puede estar vacío");
        }
        Objects.requireNonNull(naturaleza, "La naturaleza es obligatoria");
        Objects.requireNonNull(gravedad, "La gravedad es obligatoria");
    }

    public boolean requiereAvisoInmediato() {
        return avisoInmediato
                || naturaleza == NaturalezaCategoria.VIOLENCIA_PROTECCION
                || gravedad == Gravedad.GRAVE
                || gravedad == Gravedad.MUY_GRAVE;
    }
}
