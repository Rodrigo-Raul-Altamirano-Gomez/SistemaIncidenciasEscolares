package pe.edu.incidencias.domain.catalogo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/** Ventana institucional común, sin un plazo predeterminado. */
public final class ParametrosReiteracion {
    public enum ParametroFaltante { VENTANA_DIAS, UMBRAL_CATEGORIA }

    public record RevisionVentana(OptionalInt ventanaDias, UUID autorId, Instant fecha) {
        public RevisionVentana {
            Objects.requireNonNull(ventanaDias, "Indique la ventana o su ausencia");
            Objects.requireNonNull(autorId, "El autor es obligatorio");
            Objects.requireNonNull(fecha, "La fecha es obligatoria");
            if (ventanaDias.isPresent() && ventanaDias.getAsInt() <= 0) {
                throw new IllegalArgumentException("La ventana debe ser positiva");
            }
        }
    }

    public record EstadoReiteracion(boolean admiteReiteracion, OptionalInt umbral, OptionalInt ventanaDias) {
        public EstadoReiteracion {
            Objects.requireNonNull(umbral, "Indique el umbral o su ausencia");
            Objects.requireNonNull(ventanaDias, "Indique la ventana o su ausencia");
            if (umbral.isPresent() && umbral.getAsInt() <= 0
                    || ventanaDias.isPresent() && ventanaDias.getAsInt() <= 0) {
                throw new IllegalArgumentException("Umbral y ventana deben ser positivos");
            }
            if (!admiteReiteracion && umbral.isPresent()) {
                throw new IllegalArgumentException("Una categoría sin reiteración no puede tener umbral");
            }
        }

        public boolean habilitada() {
            return admiteReiteracion && umbral.isPresent() && ventanaDias.isPresent();
        }

        public Set<ParametroFaltante> parametrosFaltantes() {
            if (!admiteReiteracion) {
                return Set.of();
            }
            if (umbral.isEmpty() && ventanaDias.isEmpty()) {
                return Set.of(ParametroFaltante.UMBRAL_CATEGORIA, ParametroFaltante.VENTANA_DIAS);
            }
            if (umbral.isEmpty()) {
                return Set.of(ParametroFaltante.UMBRAL_CATEGORIA);
            }
            if (ventanaDias.isEmpty()) {
                return Set.of(ParametroFaltante.VENTANA_DIAS);
            }
            return Set.of();
        }
    }

    private final List<RevisionVentana> revisiones;

    private ParametrosReiteracion(List<RevisionVentana> revisiones) {
        this.revisiones = List.copyOf(revisiones);
    }

    public static ParametrosReiteracion sinConfigurar(UUID autorId, Instant fecha) {
        return new ParametrosReiteracion(List.of(new RevisionVentana(OptionalInt.empty(), autorId, fecha)));
    }

    public OptionalInt ventanaDias() { return revisiones.getLast().ventanaDias(); }
    public List<RevisionVentana> revisiones() { return revisiones; }

    public ParametrosReiteracion configurarVentana(int dias, UUID autorId, Instant fecha) {
        return agregarRevision(new RevisionVentana(OptionalInt.of(dias), autorId, fecha));
    }

    public ParametrosReiteracion retirarVentana(UUID autorId, Instant fecha) {
        return agregarRevision(new RevisionVentana(OptionalInt.empty(), autorId, fecha));
    }

    public EstadoReiteracion evaluar(Categoria categoria) {
        Objects.requireNonNull(categoria, "La categoría es obligatoria");
        return new EstadoReiteracion(categoria.admiteReiteracion(), categoria.umbralReiteracion(), ventanaDias());
    }

    private ParametrosReiteracion agregarRevision(RevisionVentana revision) {
        if (revision.fecha().isBefore(revisiones.getLast().fecha())) {
            throw new IllegalArgumentException("La fecha de cambio no puede preceder la revisión vigente");
        }
        List<RevisionVentana> historia = new ArrayList<>(revisiones);
        historia.add(revision);
        return new ParametrosReiteracion(historia);
    }
}
