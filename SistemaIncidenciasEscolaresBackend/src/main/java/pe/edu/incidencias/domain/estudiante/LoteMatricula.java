package pe.edu.incidencias.domain.estudiante;

import java.time.LocalDate;
import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Propuesta revisable. Año, fecha efectiva, procedencia e IDs son explícitos, nunca inferidos. */
public record LoteMatricula(UUID id, String procedencia, String hoja, int anio, LocalDate fechaEfectiva,
                            Optional<GrupoEscolar> grupoComun, List<FilaMatricula> filas,
                            Map<Integer, Resolucion> resoluciones) {
    public enum Accion { VINCULAR, CREAR, EXCLUIR }
    public record Resolucion(Accion accion, Optional<UUID> estudianteId) {
        public Resolucion {
            Objects.requireNonNull(accion, "accion");
            Objects.requireNonNull(estudianteId, "estudianteId");
            if ((accion == Accion.VINCULAR) != estudianteId.isPresent())
                throw new IllegalArgumentException("Solo vincular requiere identidad existente");
        }
        public static Resolucion vincular(UUID id) { return new Resolucion(Accion.VINCULAR, Optional.of(id)); }
        public static Resolucion crear() { return new Resolucion(Accion.CREAR, Optional.empty()); }
        public static Resolucion excluir() { return new Resolucion(Accion.EXCLUIR, Optional.empty()); }
    }

    public LoteMatricula {
        Objects.requireNonNull(id, "id");
        procedencia = texto(procedencia);
        hoja = texto(hoja);
        Year.of(anio);
        if (anio < 1 || Objects.requireNonNull(fechaEfectiva, "fechaEfectiva").getYear() != anio)
            throw new IllegalArgumentException("La fecha efectiva debe pertenecer al año escolar positivo seleccionado");
        Objects.requireNonNull(grupoComun, "grupoComun");
        filas = List.copyOf(filas);
        resoluciones = Map.copyOf(resoluciones);
        var numeros = new HashSet<Integer>();
        for (var fila : filas) {
            if (!numeros.add(fila.numero())) throw new IllegalArgumentException("Fila repetida en la hoja");
        }
        if (!numeros.containsAll(resoluciones.keySet()))
            throw new IllegalArgumentException("Resolución de una fila ajena al lote");
    }

    private static String texto(String valor) {
        if (Objects.requireNonNull(valor, "texto").isBlank()) throw new IllegalArgumentException("Procedencia/hoja vacía");
        return valor.strip();
    }
}
