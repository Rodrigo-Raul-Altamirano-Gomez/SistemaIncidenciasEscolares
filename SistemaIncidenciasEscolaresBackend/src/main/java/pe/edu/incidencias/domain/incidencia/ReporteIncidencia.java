package pe.edu.incidencias.domain.incidencia;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Comunicación original y rectificaciones legibles. Las capacidades institucionales se autorizan fuera del agregado. */
public final class ReporteIncidencia {
    public static final ZoneId ZONA_ESCOLAR = ZoneId.of("America/Lima");
    public enum TipoCorreccion { RUTINARIA, SUSTANCIAL }
    public record Revision(DatosReporte datos, RegistroCambio cambio) { }
    public record AclaracionAutor(String texto, RegistroCambio cambio) { }

    private final UUID id;
    private final List<Revision> revisiones;
    private final Optional<RegistroCambio> inicioAtencion;
    private final Optional<RegistroCambio> anulacion;
    private final List<AclaracionAutor> aclaraciones;

    private ReporteIncidencia(UUID id, List<Revision> revisiones, Optional<RegistroCambio> inicioAtencion,
                              Optional<RegistroCambio> anulacion, List<AclaracionAutor> aclaraciones) {
        this.id = Objects.requireNonNull(id, "id");
        this.revisiones = List.copyOf(revisiones);
        this.inicioAtencion = inicioAtencion;
        this.anulacion = anulacion;
        this.aclaraciones = List.copyOf(aclaraciones);
    }

    public static ReporteIncidencia registrar(UUID id, DatosReporte datos, UUID autorId, Instant fechaRegistro) {
        var cambio = new RegistroCambio(autorId, fechaRegistro, Optional.empty());
        validarFecha(datos, fechaRegistro);
        if (datos.participaciones().stream().anyMatch(p -> p.papel() == PapelParticipacion.ATRIBUCION_CORROBORADA
                || p.papel() == PapelParticipacion.ATRIBUCION_DESCARTADA))
            throw new IllegalArgumentException("Una atribución institucional se registra mediante aclaración de atención");
        return new ReporteIncidencia(id, List.of(new Revision(datos, cambio)), Optional.empty(), Optional.empty(), List.of());
    }

    public UUID id() { return id; }
    public UUID autorId() { return revisiones.getFirst().cambio().autorId(); }
    public Instant fechaRegistro() { return revisiones.getFirst().cambio().fecha(); }
    public DatosReporte datos() { return revisiones.getLast().datos(); }
    public List<Revision> revisiones() { return revisiones; }
    public List<AclaracionAutor> aclaraciones() { return aclaraciones; }
    public Optional<RegistroCambio> inicioAtencion() { return inicioAtencion; }
    public Optional<RegistroCambio> anulacion() { return anulacion; }
    public boolean anulado() { return anulacion.isPresent(); }

    public ReporteIncidencia corregirPorAutor(DatosReporte nuevosDatos, RegistroCambio cambio) {
        if (!autorId().equals(cambio.autorId())) throw new IllegalArgumentException("Solo el autor corrige su aporte");
        if (inicioAtencion.isPresent()) throw new IllegalStateException("Tras la atención el autor añade una aclaración");
        if (nuevosDatos.participaciones().stream().anyMatch(p -> p.papel() == PapelParticipacion.ATRIBUCION_CORROBORADA
                || p.papel() == PapelParticipacion.ATRIBUCION_DESCARTADA))
            throw new IllegalArgumentException("El autor ordinario no corrobora institucionalmente");
        return corregir(nuevosDatos, cambio);
    }

    public ReporteIncidencia corregirInstitucionalmente(DatosReporte nuevosDatos, RegistroCambio cambio) {
        return corregirInstitucionalmente(nuevosDatos, cambio, TipoCorreccion.SUSTANCIAL);
    }

    /** El operador distingue una errata de un cambio de hechos; no se infiere semántica del texto. */
    public ReporteIncidencia corregirInstitucionalmente(DatosReporte nuevosDatos, RegistroCambio cambio,
                                                        TipoCorreccion tipo) {
        Objects.requireNonNull(tipo, "tipo");
        if (inicioAtencion.isPresent() && cambioSustancial(nuevosDatos)
                && (tipo == TipoCorreccion.SUSTANCIAL || cambioDePersonasOClasificacion(nuevosDatos)))
            cambio.exigirMotivo();
        return corregir(nuevosDatos, cambio).marcarAtencionInstitucional(cambio);
    }

    private boolean cambioDePersonasOClasificacion(DatosReporte nuevos) {
        return !datos().participaciones().equals(nuevos.participaciones())
                || !datos().clasificacion().equals(nuevos.clasificacion())
                || datos().anioEscolar() != nuevos.anioEscolar();
    }

    private boolean cambioSustancial(DatosReporte nuevos) {
        return !datos().participaciones().equals(nuevos.participaciones())
                || !datos().fechaHecho().equals(nuevos.fechaHecho())
                || !datos().clasificacion().equals(nuevos.clasificacion())
                || !datos().descripcion().equals(nuevos.descripcion())
                || datos().anioEscolar() != nuevos.anioEscolar();
    }

    private ReporteIncidencia corregir(DatosReporte nuevosDatos, RegistroCambio cambio) {
        exigirActivo();
        validarCambio(cambio);
        validarFecha(nuevosDatos, cambio.fecha());
        var nuevas = new ArrayList<>(revisiones);
        nuevas.add(new Revision(nuevosDatos, cambio));
        return new ReporteIncidencia(id, nuevas, inicioAtencion, anulacion, aclaraciones);
    }

    public ReporteIncidencia marcarAtencionInstitucional(RegistroCambio cambio) {
        exigirActivo();
        validarCambio(cambio);
        if (inicioAtencion.isPresent()) return this;
        return new ReporteIncidencia(id, revisiones, Optional.of(cambio), anulacion, aclaraciones);
    }

    public ReporteIncidencia aclararPorAutor(String texto, RegistroCambio cambio) {
        exigirActivo();
        validarCambio(cambio);
        if (!autorId().equals(cambio.autorId())) throw new IllegalArgumentException("La aclaración corresponde al autor");
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException("Aclaración vacía");
        var nuevas = new ArrayList<>(aclaraciones);
        nuevas.add(new AclaracionAutor(texto, cambio));
        return new ReporteIncidencia(id, revisiones, inicioAtencion, anulacion, nuevas);
    }

    /** Uso exclusivo para un registro creado por error; no expresa falta de corroboración. */
    public ReporteIncidencia anularPorError(RegistroCambio cambio) {
        exigirActivo();
        validarCambio(cambio);
        cambio.exigirMotivo();
        return new ReporteIncidencia(id, revisiones, inicioAtencion, Optional.of(cambio), aclaraciones);
    }

    public Instant ultimaModificacion() {
        var ultima = revisiones.getLast().cambio().fecha();
        if (inicioAtencion.isPresent() && inicioAtencion.get().fecha().isAfter(ultima)) ultima = inicioAtencion.get().fecha();
        if (!aclaraciones.isEmpty() && aclaraciones.getLast().cambio().fecha().isAfter(ultima)) ultima = aclaraciones.getLast().cambio().fecha();
        if (anulacion.isPresent() && anulacion.get().fecha().isAfter(ultima)) ultima = anulacion.get().fecha();
        return ultima;
    }

    private void validarCambio(RegistroCambio cambio) {
        if (cambio.fecha().isBefore(ultimaModificacion())) throw new IllegalArgumentException("El cambio precede al contenido vigente");
    }
    private void exigirActivo() { if (anulado()) throw new IllegalStateException("El reporte está anulado"); }
    private static void validarFecha(DatosReporte datos, Instant registro) {
        Objects.requireNonNull(datos, "datos");
        var fechaLocal = registro.atZone(ZONA_ESCOLAR).toLocalDate();
        if (datos.fechaHecho().fecha().isAfter(fechaLocal)) throw new IllegalArgumentException("El hecho no puede ser futuro");
        if (datos.hora().isPresent() && datos.fechaHecho().fecha().equals(fechaLocal)
                && datos.hora().get().isAfter(registro.atZone(ZONA_ESCOLAR).toLocalTime()))
            throw new IllegalArgumentException("La hora efectiva no puede ser futura");
    }
}
