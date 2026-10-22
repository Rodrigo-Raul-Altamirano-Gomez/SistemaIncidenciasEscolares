package pe.edu.incidencias.domain.estudiante;

import java.time.LocalDate;
import java.time.Year;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Vínculo anual y grupos por fecha efectiva, independiente de la identidad del estudiante. */
public final class MatriculaAnual {
    private final int anio;
    private final Optional<String> situacionInformada;
    private final NavigableMap<LocalDate, Optional<GrupoEscolar>> historialGrupos;

    private MatriculaAnual(int anio, Optional<String> situacionInformada,
                           NavigableMap<LocalDate, Optional<GrupoEscolar>> historialGrupos) {
        Year.of(anio);
        if (anio < 1) {
            throw new IllegalArgumentException("El año escolar debe ser positivo.");
        }
        this.anio = anio;
        this.situacionInformada = validarSituacion(situacionInformada);
        this.historialGrupos = Collections.unmodifiableNavigableMap(new TreeMap<>(historialGrupos));
    }

    public static MatriculaAnual crear(int anio, LocalDate fechaEfectiva,
                                      Optional<GrupoEscolar> grupo,
                                      Optional<String> situacionInformada) {
        Objects.requireNonNull(fechaEfectiva, "fechaEfectiva");
        Objects.requireNonNull(grupo, "grupo");
        validarFecha(anio, fechaEfectiva);
        var historial = new TreeMap<LocalDate, Optional<GrupoEscolar>>();
        historial.put(fechaEfectiva, grupo);
        return new MatriculaAnual(anio, situacionInformada, historial);
    }

    public int anio() {
        return anio;
    }

    public Optional<String> situacionInformada() {
        return situacionInformada;
    }

    public NavigableMap<LocalDate, Optional<GrupoEscolar>> historialGrupos() {
        return historialGrupos;
    }

    /** Antes de la primera fecha conocida no se supone un grupo. */
    public Optional<GrupoEscolar> grupoEn(LocalDate fecha) {
        validarFecha(anio, fecha);
        var entrada = historialGrupos.floorEntry(fecha);
        return entrada == null ? Optional.empty() : entrada.getValue();
    }

    /** Omisión significa mantener. Completar lo desconocido no modifica los grupos posteriores. */
    public MatriculaAnual incorporarGrupo(LocalDate fechaEfectiva, Optional<GrupoEscolar> grupo) {
        validarFecha(anio, fechaEfectiva);
        Objects.requireNonNull(grupo, "grupo");
        if (grupo.isEmpty()) {
            return this;
        }
        var existente = historialGrupos.get(fechaEfectiva);
        if (existente != null && existente.isPresent()) {
            if (existente.equals(grupo)) {
                return this;
            }
            throw new IllegalArgumentException("Ya existe otro grupo para esa fecha; requiere rectificación explícita.");
        }
        var nuevoHistorial = new TreeMap<>(historialGrupos);
        nuevoHistorial.put(fechaEfectiva, grupo);
        return new MatriculaAnual(anio, situacionInformada, nuevoHistorial);
    }

    /** La situación solo cambia al recibir un valor expreso; no se deduce de la ausencia en un lote. */
    public MatriculaAnual incorporarSituacion(Optional<String> situacion) {
        situacion = validarSituacion(situacion);
        return situacion.isEmpty() || situacion.equals(situacionInformada)
                ? this : new MatriculaAnual(anio, situacion, historialGrupos);
    }

    private static void validarFecha(int anio, LocalDate fecha) {
        Objects.requireNonNull(fecha, "fecha");
        if (fecha.getYear() != anio) {
            throw new IllegalArgumentException("La fecha efectiva debe pertenecer al año escolar indicado.");
        }
    }

    private static Optional<String> validarSituacion(Optional<String> situacion) {
        Objects.requireNonNull(situacion, "situacionInformada");
        return situacion.map(valor -> {
            if (valor.isBlank()) {
                throw new IllegalArgumentException("La situación informada no puede estar vacía.");
            }
            return valor.strip();
        });
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof MatriculaAnual matricula
                && anio == matricula.anio
                && situacionInformada.equals(matricula.situacionInformada)
                && historialGrupos.equals(matricula.historialGrupos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(anio, situacionInformada, historialGrupos);
    }
}
