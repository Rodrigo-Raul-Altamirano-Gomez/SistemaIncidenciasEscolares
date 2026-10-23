package pe.edu.incidencias.domain.estudiante;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Funciones puras de búsqueda. Las coincidencias no exponen documentos completos ni contexto. */
public final class BusquedaEstudiantes {
    private BusquedaEstudiantes() { }

    public static List<Coincidencia> porNombre(Collection<Estudiante> estudiantes, String fragmento) {
        Objects.requireNonNull(estudiantes, "estudiantes");
        String consulta = normalizar(fragmento);
        if (consulta.isBlank()) {
            throw new IllegalArgumentException("Debe indicar un fragmento de nombre.");
        }
        return estudiantes.stream()
                .filter(estudiante -> normalizar(estudiante.nombreCompleto()).contains(consulta))
                .map(BusquedaEstudiantes::coincidencia).toList();
    }

    /** El llamador deberá autorizar el uso del identificador como criterio. */
    public static List<Coincidencia> porDocumento(Collection<Estudiante> estudiantes, DocumentoIdentidad documento) {
        Objects.requireNonNull(estudiantes, "estudiantes");
        Objects.requireNonNull(documento, "documento");
        return estudiantes.stream().filter(estudiante -> estudiante.documento().filter(documento::equals).isPresent())
                .map(BusquedaEstudiantes::coincidencia).toList();
    }

    public static List<Coincidencia> porAnio(Collection<Estudiante> estudiantes, int anio) {
        Objects.requireNonNull(estudiantes, "estudiantes");
        return estudiantes.stream().filter(estudiante -> estudiante.matricula(anio).isPresent())
                .map(estudiante -> new Coincidencia(estudiante.id(), estudiante.nombreCompleto(),
                        List.of(referencia(estudiante.matricula(anio).orElseThrow())))).toList();
    }

    public static List<Coincidencia> porGrupoEn(Collection<Estudiante> estudiantes, int anio,
                                               LocalDate fecha, GrupoEscolar grupo) {
        Objects.requireNonNull(estudiantes, "estudiantes");
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(grupo, "grupo");
        if (fecha.getYear() != anio) {
            throw new IllegalArgumentException("La fecha debe pertenecer al año escolar indicado.");
        }
        return estudiantes.stream()
                .filter(estudiante -> estudiante.grupoEn(anio, fecha).filter(conocido -> mismoGrupo(conocido, grupo)).isPresent())
                .map(estudiante -> new Coincidencia(estudiante.id(), estudiante.nombreCompleto(),
                        List.of(new ReferenciaEscolar(anio, fecha, estudiante.grupoEn(anio, fecha))))).toList();
    }

    public static String normalizar(String texto) {
        Objects.requireNonNull(texto, "texto");
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("(?U)\\s+", " ").strip().toLowerCase(Locale.ROOT);
    }

    private static boolean mismoGrupo(GrupoEscolar primero, GrupoEscolar segundo) {
        return coincideCampo(primero.grado(), segundo.grado()) && coincideCampo(primero.seccion(), segundo.seccion());
    }

    private static boolean coincideCampo(Optional<String> valor, Optional<String> criterio) {
        return criterio.isEmpty() || valor.map(BusquedaEstudiantes::normalizar)
                .equals(criterio.map(BusquedaEstudiantes::normalizar));
    }

    private static Coincidencia coincidencia(Estudiante estudiante) {
        return new Coincidencia(estudiante.id(), estudiante.nombreCompleto(), estudiante.matriculas().values().stream()
                .max(Comparator.comparingInt(MatriculaAnual::anio)).stream()
                .map(BusquedaEstudiantes::referencia).toList());
    }

    private static ReferenciaEscolar referencia(MatriculaAnual matricula) {
        var ultimo = matricula.historialGrupos().lastEntry();
        return new ReferenciaEscolar(matricula.anio(), ultimo.getKey(), ultimo.getValue());
    }

    /** Grupo al día consultado o, sin fecha de consulta, último registrado en ese año. */
    public record ReferenciaEscolar(int anio, LocalDate fechaReferencia, Optional<GrupoEscolar> grupo) { }

    public record Coincidencia(UUID id, String nombreCompleto, List<ReferenciaEscolar> referenciasEscolares) {
        public Coincidencia { referenciasEscolares = List.copyOf(referenciasEscolares); }
        public Coincidencia(UUID id, String nombreCompleto) { this(id, nombreCompleto, List.of()); }
    }
}
