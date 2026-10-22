package pe.edu.incidencias.domain.estudiante;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** No decide identidad por nombre ni elige un candidato de un documento ambiguo. */
public final class ConciliacionMatricula {
    private ConciliacionMatricula() { }

    public enum Estado { ALTA_PROPUESTA, COINCIDENCIA_SEGURA, REQUIERE_CONCILIACION }
    /** Identidad mínima para conciliación; no entrega matrícula completa, contactos ni contexto anterior. */
    public record Candidato(UUID id, String nombreCompleto, Optional<DocumentoIdentidad> documento) { }
    public record Resultado(Estado estado, List<Candidato> candidatos) {
        public Resultado { candidatos = List.copyOf(candidatos); }
    }

    public static Resultado evaluar(FilaMatricula fila, List<Estudiante> estudiantes) {
        Objects.requireNonNull(fila, "fila");
        estudiantes = List.copyOf(estudiantes);
        var porDocumento = estudiantes.stream().filter(e -> fila.documento().isPresent()
                && e.documento().equals(fila.documento())).toList();
        if (!porDocumento.isEmpty()) {
            boolean segura = porDocumento.size() == 1 && !contradice(fila, porDocumento.getFirst());
            return resultado(segura ? Estado.COINCIDENCIA_SEGURA : Estado.REQUIERE_CONCILIACION, porDocumento);
        }
        var porNombre = estudiantes.stream().filter(e -> BusquedaEstudiantes.normalizar(e.nombreCompleto())
                .equals(BusquedaEstudiantes.normalizar(fila.nombreCompleto()))).toList();
        return resultado(porNombre.isEmpty() ? Estado.ALTA_PROPUESTA : Estado.REQUIERE_CONCILIACION, porNombre);
    }

    private static boolean contradice(FilaMatricula fila, Estudiante estudiante) {
        return !BusquedaEstudiantes.normalizar(fila.nombreCompleto()).equals(
                BusquedaEstudiantes.normalizar(estudiante.nombreCompleto()))
                || (fila.nacimiento().isPresent() && estudiante.fechaNacimiento().isPresent()
                && !fila.nacimiento().equals(estudiante.fechaNacimiento()));
    }

    private static Resultado resultado(Estado estado, List<Estudiante> estudiantes) {
        return new Resultado(estado, estudiantes.stream().sorted(Comparator.comparing(Estudiante::id))
                .map(e -> new Candidato(e.id(), e.nombreCompleto(), e.documento())).toList());
    }
}
