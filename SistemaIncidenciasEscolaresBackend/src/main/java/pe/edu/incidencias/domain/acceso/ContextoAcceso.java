package pe.edu.incidencias.domain.acceso;

import pe.edu.incidencias.domain.incidencia.Hecho;
import pe.edu.incidencias.domain.incidencia.ReporteIncidencia;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Cierre de relaciones que revelan un mismo asunto; no debe construirse agrupando hechos ajenos por estudiante. */
public record ContextoAcceso(AmbitoReserva ambito, List<Hecho> hechos,
                             Optional<UUID> reporteId, Optional<String> motivoSustitucion) {
    public ContextoAcceso {
        Objects.requireNonNull(ambito, "ambito");
        hechos = List.copyOf(hechos);
        reporteId = Objects.requireNonNull(reporteId, "reporteId");
        motivoSustitucion = Objects.requireNonNull(motivoSustitucion, "motivoSustitucion");
        if (motivoSustitucion.filter(String::isBlank).isPresent()) throw new IllegalArgumentException("Motivo vacío");
        if (hechos.stream().map(Hecho::id).distinct().count() != hechos.size())
            throw new IllegalArgumentException("No deben repetirse versiones del mismo hecho en un contexto");
        for (var hecho : hechos) ambito = ambito.unir(AmbitoReserva.deHecho(hecho));
        if (reporteId.isPresent()) {
            UUID solicitado = reporteId.get();
            if (hechos.stream().flatMap(h -> h.reportes().stream()).filter(r -> r.id().equals(solicitado)).count() != 1)
                throw new IllegalArgumentException("El aporte solicitado debe pertenecer inequívocamente al contexto");
        }
    }
    public static ContextoAcceso deHecho(Hecho hecho) {
        return new ContextoAcceso(AmbitoReserva.deHecho(hecho), List.of(hecho), Optional.empty(), Optional.empty());
    }
    public static ContextoAcceso deAporte(Hecho hecho, UUID reporteId) {
        return new ContextoAcceso(AmbitoReserva.deHecho(hecho), List.of(hecho), Optional.of(reporteId), Optional.empty());
    }
    public ContextoAcceso conMotivoSustitucion(String motivo) {
        return new ContextoAcceso(ambito, hechos, reporteId, Optional.of(motivo));
    }
    public Optional<ReporteIncidencia> aporte() {
        return reporteId.flatMap(id -> hechos.stream().flatMap(h -> h.reportes().stream()).filter(r -> r.id().equals(id)).findFirst());
    }
}
