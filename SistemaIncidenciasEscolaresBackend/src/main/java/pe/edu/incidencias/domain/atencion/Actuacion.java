package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.incidencia.ReporteIncidencia;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Acción prevista o realizada. Los alcances vacíos indican que se dirige al conjunto de la atención. */
public final class Actuacion {
    public enum Estado { PREVISTA, REALIZADA, CANCELADA }
    public record Revision(Estado estado, String relato, Optional<LocalDate> fechaPrevista,
                           Optional<LocalDate> fechaEfectiva, List<Compromiso> compromisos,
                           RegistroCambio cambio) {
        public Revision { compromisos = List.copyOf(compromisos); }
    }

    private final UUID id;
    private final UUID responsableId;
    private final Set<UUID> hechosIds;
    private final Set<UUID> personasIds;
    private final List<Revision> revisiones;

    private Actuacion(UUID id, UUID responsableId, Set<UUID> hechosIds, Set<UUID> personasIds, List<Revision> revisiones) {
        this.id = Objects.requireNonNull(id, "id");
        this.responsableId = Objects.requireNonNull(responsableId, "responsableId");
        this.hechosIds = Set.copyOf(hechosIds);
        this.personasIds = Set.copyOf(personasIds);
        this.revisiones = List.copyOf(revisiones);
    }

    public static Actuacion realizada(UUID id, String relato, LocalDate fecha, UUID responsableId,
                                      Set<UUID> hechos, Set<UUID> personas, List<Compromiso> compromisos,
                                      RegistroCambio cambio) {
        Objects.requireNonNull(cambio, "cambio");
        validarRelato(relato);
        validarFechaEfectiva(fecha, cambio);
        validarCompromisos(compromisos, cambio);
        return new Actuacion(id, responsableId, hechos, personas, List.of(new Revision(Estado.REALIZADA,
                relato, Optional.empty(), Optional.of(fecha), compromisos, cambio)));
    }

    public static Actuacion prevista(UUID id, String descripcion, LocalDate fechaPrevista, UUID responsableId,
                                     Set<UUID> hechos, Set<UUID> personas, RegistroCambio cambio) {
        validarRelato(descripcion);
        Objects.requireNonNull(fechaPrevista, "fechaPrevista");
        Objects.requireNonNull(cambio, "cambio");
        return new Actuacion(id, responsableId, hechos, personas, List.of(new Revision(Estado.PREVISTA,
                descripcion, Optional.of(fechaPrevista), Optional.empty(), List.of(), cambio)));
    }

    public UUID id() { return id; }
    public UUID responsableId() { return responsableId; }
    public Set<UUID> hechosIds() { return hechosIds; }
    public Set<UUID> personasIds() { return personasIds; }
    public Estado estado() { return actual().estado(); }
    public String relato() { return actual().relato(); }
    public Optional<LocalDate> fechaPrevista() { return actual().fechaPrevista(); }
    public Optional<LocalDate> fechaEfectiva() { return actual().fechaEfectiva(); }
    public List<Compromiso> compromisos() { return actual().compromisos(); }
    public List<Revision> revisiones() { return revisiones; }
    public Instant ultimaModificacion() { return actual().cambio().fecha(); }
    public boolean pendiente() {
        return estado() == Estado.PREVISTA || compromisos().stream().anyMatch(Compromiso::pendienteSeguimiento);
    }

    public Actuacion realizar(LocalDate fecha, String relatoResultado, RegistroCambio cambio) {
        validarCambio(cambio);
        if (estado() != Estado.PREVISTA) throw new IllegalStateException("Solo una actuación prevista puede completarse");
        validarFechaEfectiva(fecha, cambio);
        validarRelato(relatoResultado);
        return agregar(new Revision(Estado.REALIZADA, relatoResultado, fechaPrevista(), Optional.of(fecha), compromisos(), cambio));
    }

    public Actuacion cancelar(RegistroCambio cambio) {
        validarCambio(cambio);
        cambio.exigirMotivo();
        if (estado() != Estado.PREVISTA) throw new IllegalStateException("Solo se cancela una actuación prevista");
        return agregar(new Revision(Estado.CANCELADA, relato(), fechaPrevista(), Optional.empty(), compromisos(), cambio));
    }

    public Actuacion agregarCompromiso(Compromiso compromiso, RegistroCambio cambio) {
        validarCambio(cambio);
        Objects.requireNonNull(compromiso, "compromiso");
        if (estado() == Estado.CANCELADA) throw new IllegalStateException("No se añade un compromiso a una actuación cancelada");
        if (compromisos().stream().anyMatch(c -> c.id().equals(compromiso.id())))
            throw new IllegalArgumentException("El compromiso ya está vinculado a la actuación");
        if (compromiso.ultimaModificacion().isAfter(cambio.fecha()))
            throw new IllegalArgumentException("El compromiso es posterior al registro de incorporación");
        var nuevos = new ArrayList<>(compromisos());
        nuevos.add(compromiso);
        return agregar(new Revision(estado(), relato(), fechaPrevista(), fechaEfectiva(), nuevos, cambio));
    }

    public Actuacion actualizarCompromiso(Compromiso actualizado, RegistroCambio cambio) {
        validarCambio(cambio);
        Objects.requireNonNull(actualizado, "actualizado");
        var anterior = compromisos().stream().filter(c -> c.id().equals(actualizado.id())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El compromiso no pertenece a la actuación"));
        if (!actualizado.esContinuacionDe(anterior))
            throw new IllegalArgumentException("La versión del compromiso perdería historia");
        if (actualizado.ultimaModificacion().isAfter(cambio.fecha()))
            throw new IllegalArgumentException("La actualización del compromiso es futura respecto de su incorporación");
        var nuevos = compromisos().stream().map(c -> c.id().equals(actualizado.id()) ? actualizado : c).toList();
        return agregar(new Revision(estado(), relato(), fechaPrevista(), fechaEfectiva(), nuevos, cambio));
    }

    public boolean esContinuacionDe(Actuacion anterior) {
        return id.equals(anterior.id) && responsableId.equals(anterior.responsableId)
                && hechosIds.equals(anterior.hechosIds) && personasIds.equals(anterior.personasIds)
                && revisiones.size() >= anterior.revisiones.size()
                && revisiones.subList(0, anterior.revisiones.size()).equals(anterior.revisiones);
    }

    private Revision actual() { return revisiones.getLast(); }
    private Actuacion agregar(Revision revision) {
        var nuevas = new ArrayList<>(revisiones);
        nuevas.add(revision);
        return new Actuacion(id, responsableId, hechosIds, personasIds, nuevas);
    }
    private void validarCambio(RegistroCambio cambio) {
        Objects.requireNonNull(cambio, "cambio");
        if (cambio.fecha().isBefore(ultimaModificacion())) throw new IllegalArgumentException("El cambio precede a la actuación vigente");
    }
    private static void validarCompromisos(List<Compromiso> compromisos, RegistroCambio cambio) {
        var ids = new HashSet<UUID>();
        for (var compromiso : List.copyOf(compromisos)) {
            if (!ids.add(compromiso.id())) throw new IllegalArgumentException("Una actuación no duplica el compromiso");
            if (compromiso.ultimaModificacion().isAfter(cambio.fecha()))
                throw new IllegalArgumentException("Un compromiso no puede incorporarse antes de su registro");
        }
    }
    private static void validarFechaEfectiva(LocalDate fecha, RegistroCambio cambio) {
        Objects.requireNonNull(fecha, "fecha");
        if (fecha.isAfter(cambio.fecha().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate()))
            throw new IllegalArgumentException("La fecha efectiva no puede ser futura");
    }
    private static void validarRelato(String relato) {
        if (relato == null || relato.isBlank()) throw new IllegalArgumentException("Se requiere un relato suficiente");
    }
}
