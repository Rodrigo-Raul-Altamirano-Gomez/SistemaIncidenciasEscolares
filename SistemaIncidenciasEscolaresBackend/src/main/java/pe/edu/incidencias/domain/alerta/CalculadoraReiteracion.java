package pe.edu.incidencias.domain.alerta;

import pe.edu.incidencias.domain.catalogo.Categoria;
import pe.edu.incidencias.domain.catalogo.Gravedad;
import pe.edu.incidencias.domain.catalogo.NaturalezaCategoria;
import pe.edu.incidencias.domain.catalogo.ParametrosReiteracion;
import pe.edu.incidencias.domain.incidencia.Hecho;
import pe.edu.incidencias.domain.incidencia.PapelParticipacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Calcula fundamentos; no emite notificaciones ni abre casos. El día de evaluación debe ser el local de Perú. */
public final class CalculadoraReiteracion {
    private CalculadoraReiteracion() { }

    public record Atribucion(UUID hechoId, PapelParticipacion caracter) { }
    public record Fundamento(UUID estudianteId, UUID categoriaId, LocalDate evaluadoEl,
                             LocalDate inicioInclusive, LocalDate finInclusive, int ventanaDias,
                             int umbral, List<Atribucion> atribuciones) {
        public Fundamento { atribuciones = List.copyOf(atribuciones); }
        public int cantidad() { return atribuciones.size(); }
        public long cantidad(PapelParticipacion caracter) {
            return atribuciones.stream().filter(a -> a.caracter() == caracter).count();
        }
        public boolean alcanzaUmbral() { return cantidad() >= umbral; }
    }
    public record Evaluacion(ParametrosReiteracion.EstadoReiteracion configuracion,
                             Optional<Fundamento> fundamento, List<UUID> hechosConDatosDiscrepantes) {
        public Evaluacion { hechosConDatosDiscrepantes = List.copyOf(hechosConDatosDiscrepantes); }
        public boolean alcanzaUmbral() { return fundamento.map(Fundamento::alcanzaUmbral).orElse(false); }
    }

    public static Evaluacion evaluar(UUID estudianteId, Categoria categoria, ParametrosReiteracion parametros,
                                     LocalDate hoyEnPeru, List<Hecho> hechos) {
        return evaluar(estudianteId, categoria.id(), parametros.evaluar(categoria), hoyEnPeru, hechos);
    }

    /** Reutiliza una configuración explícita, también la conservada en el último fundamento. */
    public static Evaluacion evaluar(UUID estudianteId, UUID categoriaId, ParametrosReiteracion.EstadoReiteracion estado,
                                     LocalDate hoyEnPeru, List<Hecho> hechos) {
        Objects.requireNonNull(estudianteId, "estudianteId");
        Objects.requireNonNull(hoyEnPeru, "hoyEnPeru");
        hechos = List.copyOf(hechos);
        validarHechosUnicos(hechos);
        if (!estado.habilitada()) return new Evaluacion(estado, Optional.empty(), List.of());
        int dias = estado.ventanaDias().orElseThrow();
        var inicio = hoyEnPeru.minusDays(dias - 1L);
        var atribuciones = new ArrayList<Atribucion>();
        var discrepantes = new ArrayList<UUID>();
        for (var hecho : hechos) {
            if (hecho.anulado()) continue;
            if (hecho.requierePrecisionDatos()) {
                if (hecho.reportes().stream().filter(r -> !r.anulado())
                        .anyMatch(r -> r.datos().clasificacion().categoriaId().equals(categoriaId)))
                    discrepantes.add(hecho.id());
                continue;
            }
            var clasificacion = hecho.clasificacionVigente().orElseThrow();
            if (!clasificacion.categoriaId().equals(categoriaId)) continue;
            var fecha = hecho.fechaVigente().orElseThrow();
            if (fecha.aproximada() || fecha.fecha().isBefore(inicio) || fecha.fecha().isAfter(hoyEnPeru)) continue;
            hecho.participaciones().stream()
                    .filter(p -> p.persona().estudianteId().filter(estudianteId::equals).isPresent())
                    .flatMap(p -> p.papelVigente().stream())
                    .filter(PapelParticipacion::elegibleParaReiteracion)
                    .filter(papel -> papel != PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA
                            || (clasificacion.naturaleza() == NaturalezaCategoria.CONDUCTA_DISCIPLINARIA
                            && clasificacion.gravedad() == Gravedad.LEVE))
                    .findFirst().ifPresent(papel -> atribuciones.add(new Atribucion(hecho.id(), papel)));
        }
        var fundamento = new Fundamento(estudianteId, categoriaId, hoyEnPeru, inicio, hoyEnPeru,
                dias, estado.umbral().orElseThrow(), atribuciones);
        return new Evaluacion(estado, Optional.of(fundamento), discrepantes);
    }

    private static void validarHechosUnicos(List<Hecho> hechos) {
        var reportes = new HashSet<UUID>();
        var idsHechos = new HashSet<UUID>();
        for (var hecho : hechos) {
            if (!idsHechos.add(hecho.id())) throw new IllegalArgumentException("El mismo hecho no puede contarse dos veces");
            for (var reporte : hecho.reportes())
                if (!reportes.add(reporte.id()))
                    throw new IllegalArgumentException("Un reporte no puede contarse en dos hechos ni repetirse en la evaluación");
        }
    }
}
