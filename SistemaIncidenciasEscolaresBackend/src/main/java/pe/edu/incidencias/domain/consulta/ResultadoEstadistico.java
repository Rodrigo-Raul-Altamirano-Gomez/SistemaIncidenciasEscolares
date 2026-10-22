package pe.edu.incidencias.domain.consulta;

import pe.edu.incidencias.domain.acceso.AmbitoReserva;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Corte calculado: no contiene nombres, documentos, relatos ni identificadores individuales en sus celdas. */
public final class ResultadoEstadistico {
    public static final String NO_INFORMADO = "NO_INFORMADO";
    public record Celda(List<String> coordenadas, long cantidad) {
        public Celda { coordenadas = List.copyOf(coordenadas); if (cantidad < 0) throw new IllegalArgumentException("Cantidad negativa"); }
    }
    public record Cobertura(long unidadesEvaluadas, long unidadesSinFechaPrecisada,
                            Map<ConsultaEstadistica.Dimension, Long> unidadesSinDato) {
        public Cobertura { unidadesSinDato = Map.copyOf(unidadesSinDato); }
    }
    private final ConsultaEstadistica consulta;
    private final Instant corte;
    private final long total;
    private final List<Celda> celdas;
    private final Cobertura cobertura;
    private final List<String> advertencias;
    private final Optional<AmbitoReserva> fuentes;
    private final Set<UUID> recursos;

    ResultadoEstadistico(ConsultaEstadistica consulta, Instant corte, long total, List<Celda> celdas,
                         Cobertura cobertura, List<String> advertencias, Optional<AmbitoReserva> fuentes, Set<UUID> recursos) {
        this.consulta = Objects.requireNonNull(consulta);
        this.corte = Objects.requireNonNull(corte);
        this.total = total;
        this.celdas = List.copyOf(celdas);
        this.cobertura = Objects.requireNonNull(cobertura);
        this.advertencias = List.copyOf(advertencias);
        this.fuentes = Objects.requireNonNull(fuentes);
        this.recursos = Set.copyOf(recursos);
    }
    public ConsultaEstadistica consulta() { return consulta; }
    public Instant corte() { return corte; }
    public long total() { return total; }
    public List<Celda> celdas() { return celdas; }
    public Cobertura cobertura() { return cobertura; }
    public List<String> advertencias() { return advertencias; }
    // Metadato interno de reserva. No se incluye en las tablas exportables.
    Optional<AmbitoReserva> fuentes() { return fuentes; }
    Set<UUID> recursos() { return recursos; }
}
