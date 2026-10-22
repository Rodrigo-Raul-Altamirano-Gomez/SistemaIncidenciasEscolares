package pe.edu.incidencias.domain.incidencia;

import java.time.LocalDate;
import java.util.Objects;

/** Una fecha aproximada se conserva, pero no aporta precisión suficiente al contador automático. */
public record FechaHecho(LocalDate fecha, boolean aproximada) {
    public FechaHecho { Objects.requireNonNull(fecha, "fecha"); }
    public static FechaHecho exacta(LocalDate fecha) { return new FechaHecho(fecha, false); }
    public static FechaHecho aproximada(LocalDate referencia) { return new FechaHecho(referencia, true); }
}
