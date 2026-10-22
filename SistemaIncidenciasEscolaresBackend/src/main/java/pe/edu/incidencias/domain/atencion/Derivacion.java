package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Constancia local de una comunicación externa. Registrar un destinatario no ejecuta un envío. */
public final class Derivacion {
    public enum Estado { PREPARADA, EFECTUADA }
    public enum Supuesto { VIOLENCIA_FAMILIAR, OTRO }
    public enum TipoDestino { FISCALIA, OTRA_ENTIDAD }

    public record Destino(TipoDestino tipo, String nombre) {
        public Destino {
            Objects.requireNonNull(tipo, "tipo");
            nombre = textoRequerido(nombre, "nombre de la entidad/destinatario");
        }
    }

    public record Revision(Estado estado, Optional<LocalDate> fechaEfectiva, String relato,
                           Optional<String> referencia, RegistroCambio cambio) {
        public Revision {
            Objects.requireNonNull(estado, "estado");
            fechaEfectiva = Objects.requireNonNull(fechaEfectiva, "fechaEfectiva");
            relato = textoRequerido(relato, "relato");
            referencia = validarReferencia(referencia);
            Objects.requireNonNull(cambio, "cambio");
            if ((estado == Estado.EFECTUADA) != fechaEfectiva.isPresent())
                throw new IllegalArgumentException("Solo la derivación efectuada tiene fecha efectiva");
            fechaEfectiva.ifPresent(fecha -> validarFecha(fecha, cambio));
        }
    }

    private static final ZoneId ZONA_ESCOLAR = ZoneId.of("America/Lima");
    private final UUID id;
    private final Set<UUID> hechosIds;
    private final Supuesto supuesto;
    private final Destino destino;
    private final List<Revision> revisiones;

    private Derivacion(UUID id, Set<UUID> hechosIds, Supuesto supuesto, Destino destino, List<Revision> revisiones) {
        this.id = Objects.requireNonNull(id, "id");
        this.hechosIds = Set.copyOf(hechosIds);
        if (this.hechosIds.isEmpty()) throw new IllegalArgumentException("Se debe indicar qué hechos abarca la derivación");
        this.supuesto = Objects.requireNonNull(supuesto, "supuesto");
        this.destino = Objects.requireNonNull(destino, "destino");
        this.revisiones = List.copyOf(revisiones);
    }

    public static Derivacion preparada(UUID id, Set<UUID> hechosIds, Supuesto supuesto, Destino destino,
                                       String relato, Optional<String> referencia, RegistroCambio cambio) {
        return new Derivacion(id, hechosIds, supuesto, destino,
                List.of(new Revision(Estado.PREPARADA, Optional.empty(), relato, referencia, cambio)));
    }

    public static Derivacion efectuada(UUID id, Set<UUID> hechosIds, Supuesto supuesto, Destino destino,
                                       String relato, Optional<String> referencia, LocalDate fechaEfectiva,
                                       RegistroCambio cambio) {
        return new Derivacion(id, hechosIds, supuesto, destino,
                List.of(new Revision(Estado.EFECTUADA, Optional.of(fechaEfectiva), relato, referencia, cambio)));
    }

    public UUID id() { return id; }
    public Set<UUID> hechosIds() { return hechosIds; }
    public Supuesto supuesto() { return supuesto; }
    public Destino destino() { return destino; }
    public Estado estado() { return actual().estado(); }
    public Optional<LocalDate> fechaEfectiva() { return actual().fechaEfectiva(); }
    public String relato() { return actual().relato(); }
    public Optional<String> referencia() { return actual().referencia(); }
    public List<Revision> revisiones() { return revisiones; }
    public Instant ultimaModificacion() { return actual().cambio().fecha(); }

    public Derivacion efectuar(LocalDate fechaEfectiva, String relato, Optional<String> referencia,
                               RegistroCambio cambio) {
        validarCambio(cambio);
        var siguiente = new Revision(Estado.EFECTUADA, Optional.of(fechaEfectiva), relato,
                validarReferencia(referencia).or(this::referencia), cambio);
        if (estado() == Estado.EFECTUADA) {
            if (actual().equals(siguiente)) return this;
            throw new IllegalStateException("La derivación ya fue efectuada; no puede ejecutarse de nuevo");
        }
        return agregarRevision(siguiente);
    }

    /** La referencia no es requisito para registrar preparación o ejecución. Su cambio conserva la anterior. */
    public Derivacion completarReferencia(String referencia, RegistroCambio cambio) {
        validarCambio(cambio);
        String informada = textoRequerido(referencia, "referencia");
        if (referencia().filter(informada::equals).isPresent()) return this;
        return agregarRevision(new Revision(estado(), fechaEfectiva(), relato(), Optional.of(informada), cambio));
    }

    /** Condición necesaria; el caso debe comprobar además que se cubra todo el objeto de su atención. */
    public boolean esViolenciaFamiliarEfectivaAFiscalia() {
        return estado() == Estado.EFECTUADA && supuesto == Supuesto.VIOLENCIA_FAMILIAR
                && destino.tipo() == TipoDestino.FISCALIA;
    }

    private Revision actual() { return revisiones.getLast(); }
    private Derivacion agregarRevision(Revision revision) {
        var nuevas = new ArrayList<>(revisiones);
        nuevas.add(revision);
        return new Derivacion(id, hechosIds, supuesto, destino, nuevas);
    }
    private void validarCambio(RegistroCambio cambio) {
        Objects.requireNonNull(cambio, "cambio");
        if (cambio.fecha().isBefore(ultimaModificacion()))
            throw new IllegalArgumentException("El cambio precede a la última modificación de la derivación");
    }
    private static void validarFecha(LocalDate fecha, RegistroCambio cambio) {
        if (fecha.isAfter(cambio.fecha().atZone(ZONA_ESCOLAR).toLocalDate()))
            throw new IllegalArgumentException("La fecha efectiva de derivación no puede ser futura");
    }
    private static Optional<String> validarReferencia(Optional<String> referencia) {
        return Objects.requireNonNull(referencia, "referencia").map(valor -> textoRequerido(valor, "referencia"));
    }
    private static String textoRequerido(String texto, String campo) {
        Objects.requireNonNull(texto, campo);
        if (texto.isBlank()) throw new IllegalArgumentException("Debe informarse " + campo);
        return texto.strip();
    }
}
