package pe.edu.incidencias.domain.estudiante;

import java.util.Set;

/** Alcance expreso de borrado o sustitución de un grupo ya informado para la misma fecha. */
public record RectificacionMatricula(Set<Campo> borrar, boolean sustituirGrupo) {
    public enum Campo { DOCUMENTO, NACIMIENTO, SEXO, SITUACION, GRUPO }
    public RectificacionMatricula {
        borrar = Set.copyOf(borrar);
        if (sustituirGrupo && borrar.contains(Campo.GRUPO))
            throw new IllegalArgumentException("No se puede borrar y sustituir el grupo a la vez");
    }
}
