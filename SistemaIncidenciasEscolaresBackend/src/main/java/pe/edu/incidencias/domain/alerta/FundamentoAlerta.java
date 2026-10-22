package pe.edu.incidencias.domain.alerta;

import pe.edu.incidencias.domain.catalogo.ClasificacionCategoria;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Corte de una fuente de señal. Motivos vacíos registran un recálculo que retira esa fuente sin borrarla. */
public record FundamentoAlerta(String origen, Set<UUID> hechosIds, Set<MotivoAlerta> motivos,
                               Map<UUID, ClasificacionCategoria> clasificaciones,
                               Optional<CalculadoraReiteracion.Fundamento> reiteracion, Instant evaluadoEn) {
    public FundamentoAlerta {
        if (origen == null || origen.isBlank()) throw new IllegalArgumentException("Se requiere el origen estable del fundamento");
        hechosIds = Set.copyOf(hechosIds);
        motivos = Set.copyOf(motivos);
        clasificaciones = Map.copyOf(clasificaciones);
        reiteracion = Objects.requireNonNull(reiteracion, "reiteracion");
        Objects.requireNonNull(evaluadoEn, "evaluadoEn");
        if (!motivos.isEmpty() && hechosIds.isEmpty()) throw new IllegalArgumentException("Una señal debe tener hechos de sustento");
        if (motivos.contains(MotivoAlerta.REITERACION) && reiteracion.filter(CalculadoraReiteracion.Fundamento::alcanzaUmbral).isEmpty())
            throw new IllegalArgumentException("La señal de reiteración requiere umbral alcanzado y parámetros conservados");
    }
    public boolean vigente() { return !motivos.isEmpty(); }
    boolean mismosDatos(FundamentoAlerta otro) {
        return origen.equals(otro.origen) && hechosIds.equals(otro.hechosIds) && motivos.equals(otro.motivos)
                && clasificaciones.equals(otro.clasificaciones) && reiteracion.equals(otro.reiteracion);
    }
}
