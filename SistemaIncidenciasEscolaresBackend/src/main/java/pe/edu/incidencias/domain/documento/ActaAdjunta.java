package pe.edu.incidencias.domain.documento;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.incidencia.ReporteIncidencia;
import pe.edu.incidencias.domain.acceso.AmbitoReserva;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Documento elaborado fuera del sistema y vinculado una vez al caso. */
public final class ActaAdjunta {
    public record Revision(LocalDate fechaActa, String descripcion, ArchivoAdjunto archivo,
                           Optional<UUID> sustituyeArchivoId, RegistroCambio registro,
                           Optional<String> motivoSustitucion, Optional<AmbitoReserva> ambitoIncorporacion) { }
    private final UUID id;
    private final UUID casoId;
    private final List<Revision> revisiones;
    private ActaAdjunta(UUID id, UUID casoId, List<Revision> revisiones) {
        this.id = Objects.requireNonNull(id);
        this.casoId = Objects.requireNonNull(casoId);
        this.revisiones = List.copyOf(revisiones);
    }
    public static ActaAdjunta incorporar(UUID id, UUID casoId, LocalDate fechaActa, String descripcion,
                                         ArchivoAdjunto archivo, RegistroCambio registro) {
        validar(fechaActa, descripcion, archivo, registro);
        return new ActaAdjunta(id, casoId, List.of(new Revision(fechaActa, descripcion, archivo, Optional.empty(), registro, Optional.empty(), Optional.empty())));
    }
    public UUID id() { return id; }
    public UUID casoId() { return casoId; }
    public List<Revision> revisiones() { return revisiones; }
    public Revision vigente() { return revisiones.getLast(); }
    public ActaAdjunta sustituir(LocalDate fechaActa, String descripcion, ArchivoAdjunto archivo, RegistroCambio registro) {
        validar(fechaActa, descripcion, archivo, registro);
        registro.exigirMotivo();
        if (registro.fecha().isBefore(vigente().registro().fecha())) throw new IllegalArgumentException("La sustitución precede a la versión vigente");
        if (revisiones.stream().anyMatch(r -> r.archivo().id().equals(archivo.id()))) throw new IllegalArgumentException("La sustitución necesita otra referencia de archivo");
        var nuevas = new ArrayList<>(revisiones);
        nuevas.add(new Revision(fechaActa, descripcion, archivo, Optional.of(vigente().archivo().id()), registro, Optional.empty(), Optional.empty()));
        return new ActaAdjunta(id, casoId, nuevas);
    }
    /** Se fija desde la decisión de acceso, únicamente sobre la versión recién incorporada. */
    ActaAdjunta conReserva(Optional<String> motivo, AmbitoReserva ambito) {
        var actual = vigente();
        var nuevas = new ArrayList<>(revisiones);
        nuevas.set(nuevas.size() - 1, new Revision(actual.fechaActa(), actual.descripcion(), actual.archivo(),
                actual.sustituyeArchivoId(), actual.registro(), motivo, Optional.of(ambito)));
        return new ActaAdjunta(id, casoId, nuevas);
    }
    /** Completa metadatos de dominio ya incorporados sin permitir alterar las versiones conocidas. */
    ActaAdjunta conPrefijoIncorporado(ActaAdjunta anterior) {
        if (!id.equals(anterior.id) || !casoId.equals(anterior.casoId) || revisiones.size() < anterior.revisiones.size())
            throw new IllegalArgumentException("La versión del acta pierde historia");
        var nuevas = new ArrayList<>(revisiones);
        for (int i = 0; i < anterior.revisiones.size(); i++) {
            var recibida = nuevas.get(i);
            var conocida = anterior.revisiones.get(i);
            var normalizada = new Revision(recibida.fechaActa(), recibida.descripcion(), recibida.archivo(), recibida.sustituyeArchivoId(),
                    recibida.registro(), recibida.motivoSustitucion().or(() -> conocida.motivoSustitucion()),
                    recibida.ambitoIncorporacion().or(() -> conocida.ambitoIncorporacion()));
            if (!normalizada.equals(conocida)) throw new IllegalArgumentException("Se intentó modificar una revisión incorporada");
            nuevas.set(i, conocida);
        }
        return new ActaAdjunta(id, casoId, nuevas);
    }
    public boolean esContinuacionDe(ActaAdjunta anterior) {
        return id.equals(anterior.id) && casoId.equals(anterior.casoId) && revisiones.size() >= anterior.revisiones.size()
                && revisiones.subList(0, anterior.revisiones.size()).equals(anterior.revisiones);
    }
    private static void validar(LocalDate fecha, String descripcion, ArchivoAdjunto archivo, RegistroCambio registro) {
        Objects.requireNonNull(fecha);
        Objects.requireNonNull(archivo);
        Objects.requireNonNull(registro);
        if (descripcion == null || descripcion.isBlank()) throw new IllegalArgumentException("Se necesita descripción del acta");
        if (fecha.isAfter(registro.fecha().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate())) throw new IllegalArgumentException("El acta no puede tener fecha efectiva futura");
    }
}
