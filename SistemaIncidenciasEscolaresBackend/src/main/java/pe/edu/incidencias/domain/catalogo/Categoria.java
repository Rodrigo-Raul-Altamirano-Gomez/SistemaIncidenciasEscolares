package pe.edu.incidencias.domain.catalogo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;

/** Agregado inmutable; sus revisiones conservan definiciones anteriores. */
public final class Categoria {
    private final UUID id;
    private final List<RevisionCategoria> revisiones;

    private Categoria(UUID id, List<RevisionCategoria> revisiones) {
        this.id = Objects.requireNonNull(id, "El identificador es obligatorio");
        this.revisiones = List.copyOf(revisiones);
    }

    public static Categoria crear(UUID id, DefinicionCategoria definicion, UUID autorId, Instant fecha) {
        return new Categoria(id, List.of(new RevisionCategoria(definicion, true, autorId, fecha)));
    }

    public UUID id() { return id; }
    public String nombre() { return actual().definicion().nombre(); }
    public String descripcion() { return actual().definicion().descripcion(); }
    public NaturalezaCategoria naturaleza() { return actual().definicion().naturaleza(); }
    public Gravedad gravedad() { return actual().definicion().gravedad(); }
    public boolean avisoInmediato() { return actual().definicion().avisoInmediato(); }
    public boolean activa() { return actual().activa(); }
    public boolean admiteReiteracion() { return actual().definicion().admiteReiteracion(); }
    public OptionalInt umbralReiteracion() { return actual().definicion().umbralReiteracion(); }
    public List<RevisionCategoria> revisiones() { return revisiones; }

    public Categoria actualizar(DefinicionCategoria definicion, UUID autorId, Instant fecha) {
        return agregarRevision(new RevisionCategoria(definicion, activa(), autorId, fecha));
    }

    public Categoria desactivar(UUID autorId, Instant fecha) {
        return agregarRevision(new RevisionCategoria(actual().definicion(), false, autorId, fecha));
    }

    public Categoria activar(UUID autorId, Instant fecha) {
        return agregarRevision(new RevisionCategoria(actual().definicion(), true, autorId, fecha));
    }

    public ClasificacionCategoria clasificacionParaNuevoHecho() {
        if (!activa()) {
            throw new IllegalStateException("Una categoría desactivada no admite nuevos hechos");
        }
        return new ClasificacionCategoria(id, nombre(), naturaleza(), gravedad(), avisoInmediato());
    }

    private RevisionCategoria actual() { return revisiones.getLast(); }

    private Categoria agregarRevision(RevisionCategoria revision) {
        if (revision.fecha().isBefore(actual().fecha())) {
            throw new IllegalArgumentException("La fecha de cambio no puede preceder la revisión vigente");
        }
        List<RevisionCategoria> historia = new ArrayList<>(revisiones);
        historia.add(revision);
        return new Categoria(id, historia);
    }
}
