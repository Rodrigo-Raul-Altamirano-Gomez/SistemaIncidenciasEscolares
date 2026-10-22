package pe.edu.incidencias.domain.catalogo;

import java.util.Objects;
import java.util.OptionalInt;

/** Valores aprobados de una categoría; no contiene reglas arbitrarias ni sanciones. */
public record DefinicionCategoria(
        String nombre,
        String descripcion,
        NaturalezaCategoria naturaleza,
        Gravedad gravedad,
        boolean avisoInmediato,
        boolean admiteReiteracion,
        OptionalInt umbralReiteracion) {

    public DefinicionCategoria {
        nombre = textoObligatorio(nombre, "nombre");
        descripcion = textoObligatorio(descripcion, "descripción");
        Objects.requireNonNull(naturaleza, "La naturaleza es obligatoria");
        Objects.requireNonNull(gravedad, "La gravedad es obligatoria");
        Objects.requireNonNull(umbralReiteracion, "Indique el umbral o su ausencia");
        if (umbralReiteracion.isPresent() && umbralReiteracion.getAsInt() <= 0) {
            throw new IllegalArgumentException("El umbral debe ser un entero positivo");
        }
        if (!admiteReiteracion && umbralReiteracion.isPresent()) {
            throw new IllegalArgumentException("Una categoría sin reiteración no puede tener umbral");
        }
    }

    private static String textoObligatorio(String valor, String campo) {
        String texto = Objects.requireNonNull(valor, "El campo " + campo + " es obligatorio").strip();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("El campo " + campo + " no puede estar vacío");
        }
        return texto;
    }
}
