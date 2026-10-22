package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.incidencia.PersonaInvolucrada;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.incidencia.ReporteIncidencia;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Acuerdo con observaciones explícitas; finalizar la atención no altera su cumplimiento ni seguimiento. */
public final class Compromiso {
    public enum Estado { ACORDADO, CUMPLIDO, NO_CUMPLIDO, CANCELADO }
    public record Revision(String contenido, PersonaInvolucrada asumidoPor, Optional<LocalDate> fechaObjetivo,
                           Estado estado, boolean pendienteSeguimiento, Optional<LocalDate> fechaObservacion,
                           Optional<String> observacion, RegistroCambio cambio) { }

    private final UUID id;
    private final List<Revision> revisiones;

    private Compromiso(UUID id, List<Revision> revisiones) {
        this.id = Objects.requireNonNull(id, "id");
        this.revisiones = List.copyOf(revisiones);
    }

    public static Compromiso acordar(UUID id, String contenido, PersonaInvolucrada asumidoPor,
                                     Optional<LocalDate> objetivo, boolean seguimientoExplicito,
                                     RegistroCambio cambio) {
        validarAcuerdo(contenido, asumidoPor, objetivo);
        Objects.requireNonNull(cambio, "cambio");
        return new Compromiso(id, List.of(new Revision(contenido, asumidoPor, objetivo, Estado.ACORDADO,
                seguimientoExplicito || objetivo.isPresent(), Optional.empty(), Optional.empty(), cambio)));
    }

    public UUID id() { return id; }
    public String contenido() { return actual().contenido(); }
    public PersonaInvolucrada asumidoPor() { return actual().asumidoPor(); }
    public Optional<LocalDate> fechaObjetivo() { return actual().fechaObjetivo(); }
    public Estado estado() { return actual().estado(); }
    public boolean pendienteSeguimiento() { return actual().pendienteSeguimiento(); }
    public List<Revision> revisiones() { return revisiones; }
    public Instant ultimaModificacion() { return actual().cambio().fecha(); }

    public boolean vencidoEn(LocalDate hoy) {
        Objects.requireNonNull(hoy, "hoy");
        return pendienteSeguimiento() && estado() != Estado.CUMPLIDO && estado() != Estado.CANCELADO
                && fechaObjetivo().filter(fecha -> fecha.isBefore(hoy)).isPresent();
    }

    /** Ausencia de observación de cumplimiento no se transforma en cumplimiento ni incumplimiento. */
    public Compromiso registrarSeguimiento(LocalDate fecha, String observacion, Optional<Boolean> cumplimientoObservado,
                                           boolean continuaSeguimiento, RegistroCambio cambio) {
        validarCambio(cambio);
        if (estado() == Estado.CANCELADO) throw new IllegalStateException("El compromiso está cancelado");
        validarFechaEfectiva(fecha, cambio);
        texto(observacion, "observacion");
        Objects.requireNonNull(cumplimientoObservado, "cumplimientoObservado");
        var ultimaFecha = revisiones.stream().flatMap(r -> r.fechaObservacion().stream()).reduce((a, b) -> b);
        if (ultimaFecha.filter(anterior -> fecha.isBefore(anterior)).isPresent())
            throw new IllegalArgumentException("El seguimiento no puede preceder al último ya documentado");
        var nuevoEstado = cumplimientoObservado.map(cumplio -> cumplio ? Estado.CUMPLIDO : Estado.NO_CUMPLIDO).orElse(estado());
        return agregar(new Revision(contenido(), asumidoPor(), fechaObjetivo(), nuevoEstado, continuaSeguimiento,
                Optional.of(fecha), Optional.of(observacion), cambio));
    }

    public Compromiso cancelar(RegistroCambio cambio) {
        validarCambio(cambio);
        cambio.exigirMotivo();
        if (estado() == Estado.CANCELADO) throw new IllegalStateException("El compromiso ya está cancelado");
        return agregar(new Revision(contenido(), asumidoPor(), fechaObjetivo(), Estado.CANCELADO, false,
                Optional.empty(), Optional.empty(), cambio));
    }

    /** Rectificación/sustitución sustancial del acuerdo dentro de su identidad, con su contenido anterior conservado. */
    public Compromiso sustituir(String contenido, PersonaInvolucrada asumidoPor, Optional<LocalDate> objetivo,
                                boolean seguimientoExplicito, RegistroCambio cambio) {
        validarCambio(cambio);
        cambio.exigirMotivo();
        if (estado() == Estado.CANCELADO) throw new IllegalStateException("No se sustituye un compromiso cancelado");
        validarAcuerdo(contenido, asumidoPor, objetivo);
        return agregar(new Revision(contenido, asumidoPor, objetivo, Estado.ACORDADO,
                seguimientoExplicito || objetivo.isPresent(), Optional.empty(), Optional.empty(), cambio));
    }

    boolean esContinuacionDe(Compromiso anterior) {
        return id.equals(anterior.id) && revisiones.size() >= anterior.revisiones.size()
                && revisiones.subList(0, anterior.revisiones.size()).equals(anterior.revisiones);
    }

    private Revision actual() { return revisiones.getLast(); }
    private Compromiso agregar(Revision revision) {
        var nuevas = new ArrayList<>(revisiones);
        nuevas.add(revision);
        return new Compromiso(id, nuevas);
    }
    private void validarCambio(RegistroCambio cambio) {
        Objects.requireNonNull(cambio, "cambio");
        if (cambio.fecha().isBefore(ultimaModificacion())) throw new IllegalArgumentException("El cambio precede al compromiso vigente");
    }
    private static void validarAcuerdo(String contenido, PersonaInvolucrada asumidoPor, Optional<LocalDate> objetivo) {
        texto(contenido, "contenido");
        Objects.requireNonNull(asumidoPor, "asumidoPor");
        Objects.requireNonNull(objetivo, "objetivo");
    }
    private static void validarFechaEfectiva(LocalDate fecha, RegistroCambio cambio) {
        Objects.requireNonNull(fecha, "fecha");
        if (fecha.isAfter(cambio.fecha().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate()))
            throw new IllegalArgumentException("La fecha de seguimiento no puede ser futura");
    }
    private static void texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(campo + " es obligatorio");
    }
}
