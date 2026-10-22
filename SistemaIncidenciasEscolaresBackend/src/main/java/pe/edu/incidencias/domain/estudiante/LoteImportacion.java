package pe.edu.incidencias.domain.estudiante;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Solo columnas seleccionadas: el adaptador no entrega celdas omitidas ni el archivo a este agregado. */
public record LoteImportacion(LoteMatricula matricula, AlcanceImportacion alcance,
                              Map<Integer, FilaContexto> contexto) {
    public sealed interface Entrada permits Familiar, Texto, Antecedente { }
    public record Familiar(ContactoFamiliar contacto) implements Entrada {
        public Familiar { Objects.requireNonNull(contacto, "contacto"); }
    }
    public record Texto(String valor) implements Entrada {
        public Texto { valor = AlcanceImportacion.texto(valor); }
    }
    public record Antecedente(String contenido, ContextoEstudiante.Calidad calidad,
                              Optional<LocalDate> fecha, Optional<String> periodo) implements Entrada {
        public Antecedente {
            contenido = AlcanceImportacion.texto(contenido);
            Objects.requireNonNull(calidad, "calidad");
            Objects.requireNonNull(fecha, "fecha");
            periodo = ContactoFamiliar.limpiar(periodo);
        }
    }
    public record FilaContexto(Map<String, Entrada> valores, Set<String> borrar) {
        public FilaContexto {
            valores = Map.copyOf(valores);
            borrar = Set.copyOf(borrar);
            if (borrar.stream().anyMatch(valores::containsKey))
                throw new IllegalArgumentException("Un concepto no se aporta y borra a la vez");
        }
        public static FilaContexto vacia() { return new FilaContexto(Map.of(), Set.of()); }
    }
    public LoteImportacion {
        Objects.requireNonNull(matricula, "matricula");
        Objects.requireNonNull(alcance, "alcance");
        contexto = Map.copyOf(contexto);
        var filas = new HashSet<Integer>();
        matricula.filas().forEach(f -> filas.add(f.numero()));
        if (!filas.containsAll(contexto.keySet())) throw new IllegalArgumentException("Contexto de fila ajena al lote");
    }
}
