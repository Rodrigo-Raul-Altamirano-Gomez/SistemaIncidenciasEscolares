package pe.edu.incidencias.domain.alerta;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Señal persistible: sus fundamentos históricos no se sustituyen al recalcular. */
public final class Alerta {
    public record Revision(Map<String, FundamentoAlerta> fundamentos, RegistroCambio registro,
                           Optional<DisposicionAlerta> disposicionRectificada) {
        public Revision { fundamentos = Map.copyOf(fundamentos); }
        public Revision(Map<String, FundamentoAlerta> fundamentos, RegistroCambio registro) { this(fundamentos, registro, Optional.empty()); }
    }
    private final UUID id;
    private final List<Revision> revisiones;
    private final Optional<DisposicionAlerta> disposicion;
    private final Set<UUID> hechosYaGestionados;
    private final Set<UUID> hechosAlGestionar;

    private Alerta(UUID id, List<Revision> revisiones, Optional<DisposicionAlerta> disposicion, Set<UUID> hechosYaGestionados, Set<UUID> hechosAlGestionar) {
        this.id = id;
        this.revisiones = List.copyOf(revisiones);
        this.disposicion = disposicion;
        this.hechosYaGestionados = Set.copyOf(hechosYaGestionados);
        this.hechosAlGestionar = Set.copyOf(hechosAlGestionar);
    }
    static Alerta crear(UUID id, FundamentoAlerta fundamento, RegistroCambio cambio, Set<UUID> hechosYaGestionados) {
        if (!fundamento.vigente()) throw new IllegalArgumentException("No se crea una señal sin fundamento vigente");
        return new Alerta(id, List.of(new Revision(Map.of(fundamento.origen(), fundamento), cambio)), Optional.empty(), hechosYaGestionados, Set.of());
    }
    public UUID id() { return id; }
    public Instant creadaEn() { return revisiones.getFirst().registro().fecha(); }
    public List<Revision> revisiones() { return revisiones; }
    public Optional<DisposicionAlerta> disposicion() { return disposicion; }
    public Map<String, FundamentoAlerta> fundamentosVigentes() { return revisiones.getLast().fundamentos(); }
    public Set<MotivoAlerta> motivos() {
        var motivos = new HashSet<MotivoAlerta>();
        fundamentosVigentes().values().forEach(f -> motivos.addAll(f.motivos()));
        return Set.copyOf(motivos);
    }
    public Set<UUID> hechosVigentes() {
        var hechos = new HashSet<UUID>();
        fundamentosVigentes().values().stream().filter(FundamentoAlerta::vigente).forEach(f -> hechos.addAll(f.hechosIds()));
        return Set.copyOf(hechos);
    }
    public Set<UUID> hechosHistoricos() {
        var hechos = new HashSet<UUID>();
        revisiones.forEach(r -> r.fundamentos().values().forEach(f -> hechos.addAll(f.hechosIds())));
        return Set.copyOf(hechos);
    }
    public boolean pendiente() { return disposicion.isEmpty() && !motivos().isEmpty(); }
    public Instant pendienteDesde() {
        return revisiones.stream().filter(r -> r.disposicionRectificada().isPresent()).reduce((a, b) -> b)
                .map(r -> r.registro().fecha()).orElse(creadaEn());
    }
    Alerta rectificarDuplicidad(RegistroCambio cambio) {
        validarCambio(cambio);
        cambio.exigirMotivo();
        if (disposicion.filter(d -> d.tipo() == DisposicionAlerta.Tipo.DUPLICIDAD).isEmpty())
            throw new IllegalStateException("Solo se rectifica aquí una disposición de duplicidad equivocada");
        var historia = new ArrayList<>(revisiones);
        historia.add(new Revision(fundamentosVigentes(), cambio, disposicion));
        return new Alerta(id, historia, Optional.empty(), hechosYaGestionados, Set.of());
    }
    /** Hechos que dan novedad al episodio; el resto puede seguir sustentando el umbral histórico. */
    public Set<UUID> hechosActivadores() {
        var nuevos = new HashSet<>(hechosVigentes());
        nuevos.removeAll(hechosYaGestionados);
        return Set.copyOf(nuevos);
    }
    /** Corte fijo de lo efectivamente gestionado; los recálculos posteriores no consumen hechos nuevos. */
    public Set<UUID> hechosGestionados() { return hechosAlGestionar; }
    Alerta conHechosYaGestionados(Set<UUID> hechos) {
        var anteriores = new HashSet<>(hechosYaGestionados);
        anteriores.addAll(hechos);
        return new Alerta(id, revisiones, disposicion, anteriores, hechosAlGestionar);
    }
    public Instant ultimaModificacion() { return disposicion.map(d -> d.registro().fecha()).filter(f -> f.isAfter(revisiones.getLast().registro().fecha())).orElse(revisiones.getLast().registro().fecha()); }

    Alerta registrar(FundamentoAlerta fundamento, RegistroCambio cambio) {
        validarCambio(cambio);
        var anterior = fundamentosVigentes().get(fundamento.origen());
        if (anterior != null && anterior.mismosDatos(fundamento)) return this;
        var fuentes = new LinkedHashMap<>(fundamentosVigentes());
        fuentes.put(fundamento.origen(), fundamento);
        var historia = new ArrayList<>(revisiones);
        historia.add(new Revision(fuentes, cambio));
        return new Alerta(id, historia, disposicion, hechosYaGestionados, hechosAlGestionar);
    }
    Alerta absorber(Alerta otra, RegistroCambio cambio) {
        var resultado = this;
        for (var fundamento : otra.fundamentosVigentes().values()) resultado = resultado.registrar(fundamento, cambio);
        return resultado;
    }
    Alerta gestionar(DisposicionAlerta disposicion) {
        validarCambio(disposicion.registro());
        if (this.disposicion.isPresent()) {
            if (this.disposicion.get().equals(disposicion)) return this;
            throw new IllegalStateException("La señal ya está gestionada");
        }
        return new Alerta(id, revisiones, Optional.of(disposicion), hechosYaGestionados, hechosHistoricos());
    }
    private void validarCambio(RegistroCambio cambio) {
        if (cambio.fecha().isBefore(ultimaModificacion())) throw new IllegalArgumentException("El cambio precede a la señal vigente");
    }
}
