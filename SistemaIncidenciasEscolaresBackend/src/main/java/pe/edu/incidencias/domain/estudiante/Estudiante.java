package pe.edu.incidencias.domain.estudiante;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Identidad persistente. Toda modificación devuelve otra instantánea con el mismo identificador. */
public final class Estudiante {
    private final UUID id;
    private final String nombreCompleto;
    private final Optional<DocumentoIdentidad> documento;
    private final Optional<LocalDate> fechaNacimiento;
    private final Optional<String> sexoInformado;
    private final Map<Integer, MatriculaAnual> matriculas;

    private Estudiante(UUID id, String nombreCompleto, Optional<DocumentoIdentidad> documento,
                       Optional<LocalDate> fechaNacimiento, Optional<String> sexoInformado,
                       Map<Integer, MatriculaAnual> matriculas) {
        this.id = Objects.requireNonNull(id, "id");
        this.nombreCompleto = textoObligatorio(nombreCompleto, "nombreCompleto");
        this.documento = Objects.requireNonNull(documento, "documento");
        this.fechaNacimiento = Objects.requireNonNull(fechaNacimiento, "fechaNacimiento");
        this.sexoInformado = Objects.requireNonNull(sexoInformado, "sexoInformado")
                .map(valor -> textoObligatorio(valor, "sexoInformado"));
        this.matriculas = Map.copyOf(matriculas);
    }

    public static Estudiante crear(UUID id, String nombreCompleto) {
        return new Estudiante(id, nombreCompleto, Optional.empty(), Optional.empty(), Optional.empty(), Map.of());
    }

    public UUID id() {
        return id;
    }

    public String nombreCompleto() {
        return nombreCompleto;
    }

    public Optional<DocumentoIdentidad> documento() {
        return documento;
    }

    public Optional<LocalDate> fechaNacimiento() {
        return fechaNacimiento;
    }

    public Optional<String> sexoInformado() {
        return sexoInformado;
    }

    public Map<Integer, MatriculaAnual> matriculas() {
        return matriculas;
    }

    /** Valores omitidos conservan lo existente. Los borrados quedan fuera de esta operación. */
    public Estudiante incorporarIdentidad(Optional<String> nombre, Optional<DocumentoIdentidad> documento,
                                          Optional<LocalDate> nacimiento, Optional<String> sexo) {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(documento, "documento");
        Objects.requireNonNull(nacimiento, "nacimiento");
        Objects.requireNonNull(sexo, "sexo");
        return new Estudiante(id, nombre.orElse(nombreCompleto), documento.or(() -> this.documento),
                nacimiento.or(() -> fechaNacimiento), sexo.or(() -> sexoInformado), matriculas);
    }

    /** Un año ya existente no se reemplaza accidentalmente al reintentar una carga. */
    public Estudiante vincularMatricula(MatriculaAnual matricula) {
        Objects.requireNonNull(matricula, "matricula");
        var existente = matriculas.get(matricula.anio());
        if (existente != null) {
            if (existente.equals(matricula)) {
                return this;
            }
            throw new IllegalArgumentException("El vínculo anual ya existe; incorpore sus cambios de manera explícita.");
        }
        var nuevasMatriculas = new HashMap<>(matriculas);
        nuevasMatriculas.put(matricula.anio(), matricula);
        return copiarConMatriculas(nuevasMatriculas);
    }

    public Estudiante incorporarGrupo(int anio, LocalDate fechaEfectiva, Optional<GrupoEscolar> grupo) {
        var matricula = matriculaRequerida(anio).incorporarGrupo(fechaEfectiva, grupo);
        return actualizarMatricula(matricula);
    }

    public Estudiante incorporarSituacion(int anio, Optional<String> situacion) {
        return actualizarMatricula(matriculaRequerida(anio).incorporarSituacion(situacion));
    }

    public Optional<MatriculaAnual> matricula(int anio) {
        return Optional.ofNullable(matriculas.get(anio));
    }

    /** No es editor libre: solo el registro de lotes aplica solicitudes revisadas y trazadas. */
    Estudiante rectificar(int anio, LocalDate fecha, FilaMatricula fila, RectificacionMatricula solicitud) {
        var borrar = solicitud.borrar();
        if (borrar.contains(RectificacionMatricula.Campo.DOCUMENTO) && fila.documento().isPresent()
                || borrar.contains(RectificacionMatricula.Campo.NACIMIENTO) && fila.nacimiento().isPresent()
                || borrar.contains(RectificacionMatricula.Campo.SEXO) && fila.sexo().isPresent()
                || borrar.contains(RectificacionMatricula.Campo.SITUACION) && fila.situacion().isPresent()
                || borrar.contains(RectificacionMatricula.Campo.GRUPO) && fila.grupo().isPresent())
            throw new IllegalArgumentException("Un campo no se borra y aporta a la vez");
        if (solicitud.sustituirGrupo() && fila.grupo().isEmpty())
            throw new IllegalArgumentException("La sustitución expresa requiere grupo informado");
        var nuevas = new HashMap<>(matriculas);
        if (solicitud.sustituirGrupo() || borrar.contains(RectificacionMatricula.Campo.GRUPO)
                || borrar.contains(RectificacionMatricula.Campo.SITUACION)) {
            nuevas.put(anio, matriculaRequerida(anio).rectificar(fecha, fila.grupo(),
                    solicitud.sustituirGrupo() || borrar.contains(RectificacionMatricula.Campo.GRUPO),
                    borrar.contains(RectificacionMatricula.Campo.SITUACION)));
        }
        return new Estudiante(id, nombreCompleto,
                borrar.contains(RectificacionMatricula.Campo.DOCUMENTO) ? Optional.empty() : documento,
                borrar.contains(RectificacionMatricula.Campo.NACIMIENTO) ? Optional.empty() : fechaNacimiento,
                borrar.contains(RectificacionMatricula.Campo.SEXO) ? Optional.empty() : sexoInformado, nuevas);
    }

    public Optional<GrupoEscolar> grupoEn(int anio, LocalDate fecha) {
        Objects.requireNonNull(fecha, "fecha");
        return matricula(anio).flatMap(matricula -> matricula.grupoEn(fecha));
    }

    private MatriculaAnual matriculaRequerida(int anio) {
        return matricula(anio).orElseThrow(() -> new IllegalArgumentException("No existe vínculo con ese año escolar."));
    }

    private Estudiante actualizarMatricula(MatriculaAnual matricula) {
        if (matricula.equals(matriculas.get(matricula.anio()))) {
            return this;
        }
        var nuevasMatriculas = new HashMap<>(matriculas);
        nuevasMatriculas.put(matricula.anio(), matricula);
        return copiarConMatriculas(nuevasMatriculas);
    }

    private Estudiante copiarConMatriculas(Map<Integer, MatriculaAnual> nuevasMatriculas) {
        return new Estudiante(id, nombreCompleto, documento, fechaNacimiento, sexoInformado, nuevasMatriculas);
    }

    private static String textoObligatorio(String valor, String campo) {
        Objects.requireNonNull(valor, campo);
        if (valor.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío.");
        }
        return valor.strip();
    }
}
