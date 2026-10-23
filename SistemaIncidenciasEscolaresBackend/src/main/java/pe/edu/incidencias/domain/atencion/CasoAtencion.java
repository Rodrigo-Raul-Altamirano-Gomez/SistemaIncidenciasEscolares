package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.incidencia.ReporteIncidencia;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Atención local compartida. Las transiciones protegidas se exponen mediante RegistroAtenciones. */
public final class CasoAtencion {
    public enum Estado { PENDIENTE, CERRADA }
    public record Vinculo(UUID hechoId, boolean vigente, RegistroCambio cambio) { }
    public record RevisionCierre(Optional<CierreLocal> cierre, RegistroCambio cambio) { }
    public record Continuidad(UUID destinoId, Set<UUID> hechosIds, RegistroCambio registro, Optional<RegistroCambio> correccion) {
        public Continuidad { hechosIds = Set.copyOf(hechosIds); }
        public Continuidad(UUID destinoId, Set<UUID> hechosIds, RegistroCambio registro) { this(destinoId, hechosIds, registro, Optional.empty()); }
    }

    private final UUID id;
    private final UUID responsableId;
    private final LocalDate fechaInicio;
    private final RegistroCambio apertura;
    private final Set<UUID> hechosActuales;
    private final Set<UUID> hechosHistoricos;
    private final Set<UUID> antecedentes;
    private final List<Vinculo> vinculos;
    private final List<RegistroCambio> cambios;
    private final Map<UUID, Actuacion> actuaciones;
    private final Map<UUID, Derivacion> derivaciones;
    private final List<RevisionCierre> cierres;
    private final EncargadoAtencion encargado;
    private final List<ConvocatoriaComite> convocatorias;
    private final List<IntervencionComite> intervencionesComite;
    private final List<DecisionComite> decisionesComite;
    private final List<ActoComite> actosComite;
    private final List<CambioResponsable> cambiosResponsable;
    private final List<Continuidad> continuidades;

    private CasoAtencion(UUID id, UUID responsableId, LocalDate fechaInicio, RegistroCambio apertura,
                          Set<UUID> hechosActuales, Set<UUID> hechosHistoricos, Set<UUID> antecedentes,
                          List<Vinculo> vinculos, List<RegistroCambio> cambios, Map<UUID, Actuacion> actuaciones,
                          Map<UUID, Derivacion> derivaciones, List<RevisionCierre> cierres,
                          EncargadoAtencion encargado, List<ConvocatoriaComite> convocatorias,
                          List<IntervencionComite> intervencionesComite, List<DecisionComite> decisionesComite,
                          List<ActoComite> actosComite, List<CambioResponsable> cambiosResponsable, List<Continuidad> continuidades) {
        this.id = id;
        this.responsableId = responsableId;
        this.fechaInicio = fechaInicio;
        this.apertura = apertura;
        this.hechosActuales = Set.copyOf(hechosActuales);
        this.hechosHistoricos = Set.copyOf(hechosHistoricos);
        this.antecedentes = Set.copyOf(antecedentes);
        this.vinculos = List.copyOf(vinculos);
        this.cambios = List.copyOf(cambios);
        this.actuaciones = Map.copyOf(actuaciones);
        this.derivaciones = Map.copyOf(derivaciones);
        this.cierres = List.copyOf(cierres);
        this.encargado = encargado;
        this.convocatorias = List.copyOf(convocatorias);
        this.intervencionesComite = List.copyOf(intervencionesComite);
        this.decisionesComite = List.copyOf(decisionesComite);
        this.actosComite = List.copyOf(actosComite);
        this.cambiosResponsable = List.copyOf(cambiosResponsable);
        this.continuidades = List.copyOf(continuidades);
    }

    static CasoAtencion abrir(UUID id, UUID responsable, LocalDate fechaInicio, Set<UUID> hechos, RegistroCambio cambio) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(responsable, "responsable");
        Objects.requireNonNull(fechaInicio, "fechaInicio");
        if (fechaInicio.isAfter(cambio.fecha().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate()))
            throw new IllegalArgumentException("El inicio efectivo no puede ser futuro");
        if (hechos.isEmpty()) throw new IllegalArgumentException("Una atención debe iniciarse desde al menos un hecho");
        var vinculos = hechos.stream().map(h -> new Vinculo(h, true, cambio)).toList();
        return new CasoAtencion(id, responsable, fechaInicio, cambio, hechos, hechos, Set.of(), vinculos,
                List.of(cambio), Map.of(), Map.of(), List.of(), EncargadoAtencion.TOE,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public UUID id() { return id; }
    public UUID responsableId() { return responsableId; }
    public LocalDate fechaInicio() { return fechaInicio; }
    public RegistroCambio apertura() { return apertura; }
    public Set<UUID> hechosActuales() { return hechosActuales; }
    public Set<UUID> hechosHistoricos() { return hechosHistoricos; }
    /** Hechos cuyo contenido sigue presente, no meros vínculos erróneos conservados como auditoría. */
    public Set<UUID> hechosReservados() {
        var resultado = new HashSet<>(hechosActuales);
        for (var hecho : hechosHistoricos) {
            boolean contenido = actuaciones().stream().anyMatch(a -> a.hechosIds().contains(hecho)
                    || a.hechosIds().isEmpty() && a.revisiones().stream().anyMatch(r -> vinculadoEn(hecho, r.cambio().fecha())))
                    || derivaciones().stream().anyMatch(d -> d.hechosIds().contains(hecho))
                    || convocatorias.stream().anyMatch(c -> vinculadoEn(hecho, c.registro().fecha()))
                    || cierres.stream().anyMatch(c -> contieneConclusion(c) && vinculadoEn(hecho, c.cambio().fecha()))
                    || continuidades.stream().anyMatch(c -> c.correccion().isEmpty() && c.hechosIds().contains(hecho));
            if (contenido) resultado.add(hecho);
        }
        return Set.copyOf(resultado);
    }
    private boolean vinculadoEn(UUID hecho, Instant instante) {
        return vinculos.stream().filter(v -> v.hechoId().equals(hecho) && !v.cambio().fecha().isAfter(instante))
                .reduce((a, b) -> b).map(Vinculo::vigente).orElse(false);
    }
    public Set<UUID> antecedentes() { return antecedentes; }
    public List<Vinculo> vinculos() { return vinculos; }
    public List<RegistroCambio> cambios() { return cambios; }
    public List<Actuacion> actuaciones() { return actuaciones.values().stream().sorted(java.util.Comparator.comparing(a -> a.id().toString())).toList(); }
    public List<Derivacion> derivaciones() { return derivaciones.values().stream().sorted(java.util.Comparator.comparing(d -> d.id().toString())).toList(); }
    public List<RevisionCierre> cierres() { return cierres; }
    public EncargadoAtencion encargado() { return encargado; }
    public List<ConvocatoriaComite> convocatorias() { return convocatorias; }
    public List<IntervencionComite> intervencionesComite() { return intervencionesComite; }
    public List<DecisionComite> decisionesComite() { return decisionesComite; }
    public List<ActoComite> actosComite() { return actosComite; }
    public List<CambioResponsable> cambiosResponsable() { return cambiosResponsable; }
    public List<Continuidad> continuidades() { return continuidades; }
    public Optional<UUID> continuidadHacia() { return continuidades.stream().filter(c -> c.correccion().isEmpty()).reduce((a, b) -> b).map(Continuidad::destinoId); }
    public Optional<CierreLocal> cierre() { return cierres.isEmpty() ? Optional.empty() : cierres.getLast().cierre(); }
    public Estado estado() { return cierre().isPresent() ? Estado.CERRADA : Estado.PENDIENTE; }
    public boolean tieneSeguimientoPendiente() {
        return actuaciones().stream().anyMatch(Actuacion::pendiente)
                || derivaciones().stream().anyMatch(d -> d.estado() == Derivacion.Estado.PREPARADA);
    }
    public boolean requiereTrabajo() { return estado() == Estado.PENDIENTE || tieneSeguimientoPendiente(); }
    public long cantidadActuacionesRealizadas() {
        return actuaciones().stream().filter(a -> a.estado() == Actuacion.Estado.REALIZADA).count();
    }
    public Instant ultimaModificacion() { return cambios.getLast().fecha(); }

    boolean gestionPertinente(Set<UUID> hechos, Instant desde, java.util.function.Function<UUID, UUID> canonico) {
        if (hechosReservados().stream().map(canonico).noneMatch(hechos::contains)) return false;
        if (estado() == Estado.PENDIENTE && (!apertura.fecha().isBefore(desde) || vinculos.stream().anyMatch(v -> v.vigente()
                && hechos.contains(canonico.apply(v.hechoId())) && !v.cambio().fecha().isBefore(desde)))) return true;
        if (actuaciones().stream().anyMatch(a -> (a.hechosIds().isEmpty() || a.hechosIds().stream().map(canonico).anyMatch(hechos::contains))
                && a.revisiones().stream().anyMatch(r -> r.estado() != Actuacion.Estado.CANCELADA && !r.cambio().fecha().isBefore(desde)))) return true;
        if (derivaciones().stream().anyMatch(d -> d.hechosIds().stream().map(canonico).anyMatch(hechos::contains)
                && !d.ultimaModificacion().isBefore(desde))) return true;
        return cierres.stream().anyMatch(c -> c.cierre().filter(v -> v.tipo() != CierreLocal.TipoCierre.DUPLICIDAD
                && v.tipo() != CierreLocal.TipoCierre.APERTURA_ERRONEA).isPresent() && !c.cambio().fecha().isBefore(desde));
    }

    CasoAtencion convocar(ConvocatoriaComite convocatoria, RegistroCambio cambio) {
        validarCambio(cambio);
        if (estado() != Estado.PENDIENTE) throw new IllegalStateException("Solo se convoca sobre una atención pendiente");
        if (convocatorias.stream().anyMatch(c -> c.id().equals(convocatoria.id()))) throw new IllegalArgumentException("Convocatoria repetida");
        if (convocatoria.fecha().isBefore(fechaInicio)) throw new IllegalArgumentException("La convocatoria precede al inicio");
        var nuevas = new ArrayList<>(convocatorias);
        nuevas.add(convocatoria);
        return conGobierno(responsableId, EncargadoAtencion.COMITE, nuevas, intervencionesComite, decisionesComite,
                actosComite, cambiosResponsable, continuidades, cambio);
    }

    CasoAtencion intervenir(IntervencionComite intervencion, RegistroCambio cambio) {
        validarCambio(cambio);
        var convocatoria = convocatorias.stream().filter(c -> c.id().equals(intervencion.convocatoriaId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe esa convocatoria en el caso"));
        if (intervencion.fecha().isBefore(convocatoria.fecha())) throw new IllegalArgumentException("La intervención precede a la convocatoria");
        if (intervencionesComite.stream().anyMatch(i -> i.id().equals(intervencion.id()))) throw new IllegalArgumentException("Intervención repetida");
        var nuevas = new ArrayList<>(intervencionesComite);
        nuevas.add(intervencion);
        return conGobierno(responsableId, encargado, convocatorias, nuevas, decisionesComite, actosComite,
                cambiosResponsable, continuidades, cambio);
    }

    CasoAtencion decidir(DecisionComite decision, RegistroCambio cambio) {
        validarCambio(cambio);
        if (decisionesComite.stream().anyMatch(d -> d.id().equals(decision.id()))) throw new IllegalArgumentException("Decisión repetida");
        validarIntervencion(decision.intervencionId(), cambio);
        var nuevas = new ArrayList<>(decisionesComite);
        nuevas.add(decision);
        return conGobierno(responsableId, encargado, convocatorias, intervencionesComite, nuevas, actosComite,
                cambiosResponsable, continuidades, cambio);
    }

    CasoAtencion registrarActoComite(ActoComite acto, RegistroCambio cambio) {
        validarCambio(cambio);
        validarIntervencion(acto.intervencionId(), cambio);
        var nuevos = new ArrayList<>(actosComite);
        nuevos.add(acto);
        return conGobierno(responsableId, encargado, convocatorias, intervencionesComite, decisionesComite, nuevos,
                cambiosResponsable, continuidades, cambio);
    }

    CasoAtencion reasignar(UUID nuevoResponsable, RegistroCambio cambio) {
        validarCambio(cambio);
        if (responsableId.equals(nuevoResponsable)) return this;
        var nuevos = new ArrayList<>(cambiosResponsable);
        nuevos.add(new CambioResponsable(responsableId, nuevoResponsable, cambio));
        return conGobierno(nuevoResponsable, encargado, convocatorias, intervencionesComite, decisionesComite,
                actosComite, nuevos, continuidades, cambio);
    }

    CasoAtencion continuarEn(UUID destinoId, Set<UUID> hechos, RegistroCambio cambio) {
        validarCambio(cambio);
        if (estado() != Estado.PENDIENTE || id.equals(destinoId) || hechos.isEmpty() || !hechosActuales.containsAll(hechos))
            throw new IllegalArgumentException("La continuidad necesita hechos vigentes y un destino distinto");
        var actuales = new HashSet<>(hechosActuales);
        actuales.removeAll(hechos);
        var nuevosVinculos = new ArrayList<>(vinculos);
        hechos.forEach(h -> nuevosVinculos.add(new Vinculo(h, false, cambio)));
        var nueva = copiar(actuales, hechosHistoricos, antecedentes, nuevosVinculos, actuaciones, derivaciones, cierres, cambio);
        var nuevas = new ArrayList<>(continuidades);
        nuevas.add(new Continuidad(destinoId, hechos, cambio));
        return nueva.conGobierno(responsableId, encargado, convocatorias, intervencionesComite, decisionesComite,
                actosComite, cambiosResponsable, nuevas, cambio);
    }

    CasoAtencion reemplazarReferenciaDuplicada(UUID origen, UUID principal, RegistroCambio cambio) {
        validarCambio(cambio);
        var actuales = new HashSet<>(hechosActuales);
        var historia = new HashSet<>(hechosHistoricos);
        var registros = new ArrayList<>(vinculos);
        if (actuales.remove(origen)) {
            registros.add(new Vinculo(origen, false, cambio));
            if (actuales.add(principal)) registros.add(new Vinculo(principal, true, cambio));
        }
        historia.add(principal);
        return copiar(actuales, historia, antecedentes, registros, actuaciones, derivaciones, cierres, cambio);
    }
    CasoAtencion reservarFuenteDuplicada(UUID origen, RegistroCambio cambio) {
        var historia = new HashSet<>(hechosHistoricos);
        historia.add(origen);
        var registros = new ArrayList<>(vinculos);
        registros.add(new Vinculo(origen, true, cambio));
        return copiar(hechosActuales, historia, antecedentes, registros, actuaciones, derivaciones, cierres, cambio);
    }

    boolean tieneContenidoDesde(Instant desde) {
        return actuaciones().stream().anyMatch(a -> !a.ultimaModificacion().isBefore(desde))
                || derivaciones().stream().anyMatch(d -> !d.ultimaModificacion().isBefore(desde))
                || convocatorias.stream().anyMatch(c -> !c.registro().fecha().isBefore(desde))
                || cierres.stream().anyMatch(c -> contieneConclusion(c) && !c.cambio().fecha().isBefore(desde));
    }

    private static boolean contieneConclusion(RevisionCierre revision) {
        return revision.cierre().filter(c -> c.tipo() == CierreLocal.TipoCierre.CONCLUSION
                || c.tipo() == CierreLocal.TipoCierre.DERIVACION_FAMILIAR_FISCALIA).isPresent();
    }

    CasoAtencion corregirRelacionDuplicada(UUID origen, Set<UUID> antecedentesGenerados, RegistroCambio relacion, RegistroCambio cambio) {
        validarCambio(cambio);
        var anteriores = new HashSet<>(antecedentes);
        if (!tieneContenidoDesde(relacion.fecha())) anteriores.removeAll(antecedentesGenerados);
        var continuidadesNuevas = continuidades.stream().map(c -> c.registro().equals(relacion) && c.hechosIds().contains(origen)
                ? new Continuidad(c.destinoId(), c.hechosIds(), c.registro(), Optional.of(cambio)) : c).toList();
        var actuales = new HashSet<>(hechosActuales);
        var registros = new ArrayList<>(vinculos);
        if (hechosHistoricos.contains(origen) && !actuales.contains(origen)) registros.add(new Vinculo(origen, false, cambio));
        if (hechosHistoricos.contains(origen) && continuidades.stream().anyMatch(c -> c.registro().equals(relacion) && c.hechosIds().contains(origen))) {
            if (actuales.add(origen)) registros.add(new Vinculo(origen, true, cambio));
        }
        var nuevo = copiar(actuales, hechosHistoricos, anteriores, registros, actuaciones, derivaciones, cierres, cambio);
        return nuevo.conGobierno(responsableId, encargado, convocatorias, intervencionesComite, decisionesComite, actosComite,
                cambiosResponsable, continuidadesNuevas, cambio);
    }

    boolean esActuacionFormal(UUID actuacionId) { return actosComite.stream().anyMatch(a -> a.tipo() == ActoComite.Tipo.ACTUACION && a.referenciaId().equals(actuacionId)); }
    boolean esDerivacionFormal(UUID derivacionId) { return actosComite.stream().anyMatch(a -> a.tipo() == ActoComite.Tipo.DERIVACION && a.referenciaId().equals(derivacionId)); }
    boolean tieneCierreFormal() { return actosComite.stream().anyMatch(a -> a.tipo() == ActoComite.Tipo.CIERRE || a.tipo() == ActoComite.Tipo.CORRECCION_CIERRE); }

    private void validarIntervencion(UUID intervencionId, RegistroCambio cambio) {
        var intervencion = intervencionesComite.stream().filter(i -> i.id().equals(intervencionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe esa intervención en el caso"));
        if (intervencion.registro().fecha().isAfter(cambio.fecha())) throw new IllegalArgumentException("El acto precede al registro de la intervención");
    }

    CasoAtencion vincular(Set<UUID> hechos, RegistroCambio cambio) {
        validarCambio(cambio);
        if (estado() != Estado.PENDIENTE) throw new IllegalStateException("Un nuevo hecho no reabre una atención cerrada");
        var actuales = new HashSet<>(hechosActuales);
        var historia = new HashSet<>(hechosHistoricos);
        var registros = new ArrayList<>(vinculos);
        for (var hecho : hechos) if (actuales.add(hecho)) registros.add(new Vinculo(hecho, true, cambio));
        historia.addAll(hechos);
        return copiar(actuales, historia, antecedentes, registros, actuaciones, derivaciones, cierres, cambio);
    }

    CasoAtencion retirar(UUID hechoId, RegistroCambio cambio) {
        validarCambio(cambio);
        cambio.exigirMotivo();
        if (!hechosActuales.contains(hechoId)) throw new IllegalArgumentException("El vínculo no está vigente");
        var actuales = new HashSet<>(hechosActuales);
        actuales.remove(hechoId);
        var registros = new ArrayList<>(vinculos);
        registros.add(new Vinculo(hechoId, false, cambio));
        var nuevo = copiar(actuales, hechosHistoricos, antecedentes, registros, actuaciones, derivaciones, cierres, cambio);
        if (actuales.isEmpty() && estado() == Estado.PENDIENTE)
            return nuevo.cerrar(new CierreLocal(cambio.fecha().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate(),
                    cambio.motivo().orElseThrow(), CierreLocal.TipoCierre.APERTURA_ERRONEA, cambio, Optional.empty()), cambio);
        return nuevo;
    }

    CasoAtencion citar(UUID casoId, RegistroCambio cambio) {
        validarCambio(cambio);
        if (id.equals(casoId)) throw new IllegalArgumentException("Un caso no es su propio antecedente");
        var anteriores = new HashSet<>(antecedentes);
        anteriores.add(casoId);
        return copiar(hechosActuales, hechosHistoricos, anteriores, vinculos, actuaciones, derivaciones, cierres, cambio);
    }

    CasoAtencion registrarActuacion(Actuacion actuacion, RegistroCambio cambio) {
        validarCambio(cambio);
        if (!hechosHistoricos.containsAll(actuacion.hechosIds())) throw new IllegalArgumentException("La actuación refiere hechos ajenos al caso");
        if (actuacion.ultimaModificacion().isAfter(cambio.fecha())) throw new IllegalArgumentException("La actuación es posterior al guardado");
        if (actuacion.fechaEfectiva().filter(f -> f.isBefore(fechaInicio)).isPresent())
            throw new IllegalArgumentException("La actuación del caso precede al inicio; las acciones inmediatas se referencian sin copiarlas");
        var anterior = actuaciones.get(actuacion.id());
        if (anterior != null && !actuacion.esContinuacionDe(anterior)) throw new IllegalArgumentException("No se sobrescribe la historia de una actuación");
        var compromisos = actuacion.compromisos().stream().map(Compromiso::id).collect(java.util.stream.Collectors.toSet());
        if (actuaciones().stream().filter(a -> !a.id().equals(actuacion.id())).flatMap(a -> a.compromisos().stream())
                .anyMatch(c -> compromisos.contains(c.id()))) throw new IllegalArgumentException("No se duplica un compromiso entre actuaciones");
        var nuevas = new LinkedHashMap<>(actuaciones);
        nuevas.put(actuacion.id(), actuacion);
        return copiar(hechosActuales, hechosHistoricos, antecedentes, vinculos, nuevas, derivaciones, cierres, cambio);
    }

    CasoAtencion registrarDerivacion(Derivacion derivacion, RegistroCambio cambio) {
        return registrarDerivacion(derivacion, cambio, java.util.function.Function.identity());
    }
    CasoAtencion registrarDerivacion(Derivacion derivacion, RegistroCambio cambio, java.util.function.Function<UUID, UUID> canonico) {
        validarCambio(cambio);
        if (!hechosHistoricos.containsAll(derivacion.hechosIds())) throw new IllegalArgumentException("La derivación refiere hechos ajenos al caso");
        if (derivacion.ultimaModificacion().isAfter(cambio.fecha())) throw new IllegalArgumentException("La derivación es posterior al guardado");
        if (derivacion.fechaEfectiva().filter(f -> f.isBefore(fechaInicio)).isPresent())
            throw new IllegalArgumentException("La derivación efectiva precede al inicio de la atención");
        var anterior = derivaciones.get(derivacion.id());
        if (anterior != null && (!anterior.hechosIds().equals(derivacion.hechosIds()) || anterior.supuesto() != derivacion.supuesto()
                || !anterior.destino().equals(derivacion.destino()) || derivacion.revisiones().size() < anterior.revisiones().size()
                || !derivacion.revisiones().subList(0, anterior.revisiones().size()).equals(anterior.revisiones())))
            throw new IllegalArgumentException("No se sobrescribe la historia de una derivación");
        var nuevas = new LinkedHashMap<>(derivaciones);
        nuevas.put(derivacion.id(), derivacion);
        var nuevo = copiar(hechosActuales, hechosHistoricos, antecedentes, vinculos, actuaciones, nuevas, cierres, cambio);
        var cobertura = derivacion.hechosIds().stream().map(canonico).collect(java.util.stream.Collectors.toSet());
        var objetoActual = hechosActuales.stream().map(canonico).collect(java.util.stream.Collectors.toSet());
        if (estado() == Estado.PENDIENTE && !hechosActuales.isEmpty()
                && derivacion.esViolenciaFamiliarEfectivaAFiscalia() && cobertura.containsAll(objetoActual))
            return nuevo.cerrar(new CierreLocal(derivacion.fechaEfectiva().orElseThrow(), derivacion.relato(),
                    CierreLocal.TipoCierre.DERIVACION_FAMILIAR_FISCALIA, cambio, Optional.of(derivacion.id())), cambio, canonico);
        return nuevo;
    }

    CasoAtencion cerrar(CierreLocal cierre, RegistroCambio cambio) {
        return cerrar(cierre, cambio, java.util.function.Function.identity());
    }
    CasoAtencion cerrar(CierreLocal cierre, RegistroCambio cambio, java.util.function.Function<UUID, UUID> canonico) {
        validarCambio(cambio);
        if (estado() == Estado.CERRADA) throw new IllegalStateException("El seguimiento no reemplaza un cierre existente");
        validarCierre(cierre, cambio, canonico);
        if (cierre.tipo() == CierreLocal.TipoCierre.CONCLUSION && actuaciones().stream()
                .filter(a -> a.estado() == Actuacion.Estado.REALIZADA).flatMap(a -> a.fechaEfectiva().stream())
                .anyMatch(fecha -> fecha.isAfter(cierre.fecha())))
            throw new IllegalArgumentException("La conclusión inicial precede a una actuación ya realizada; el seguimiento posterior se registra tras el cierre");
        var nuevos = new ArrayList<>(cierres);
        nuevos.add(new RevisionCierre(Optional.of(cierre), cambio));
        return copiar(hechosActuales, hechosHistoricos, antecedentes, vinculos, actuaciones, derivaciones, nuevos, cambio);
    }

    CasoAtencion corregirCierre(Optional<CierreLocal> nuevo, RegistroCambio cambio) {
        return corregirCierre(nuevo, cambio, java.util.function.Function.identity());
    }
    CasoAtencion corregirCierre(Optional<CierreLocal> nuevo, RegistroCambio cambio, java.util.function.Function<UUID, UUID> canonico) {
        validarCambio(cambio);
        cambio.exigirMotivo();
        if (cierre().isEmpty()) throw new IllegalStateException("No existe cierre vigente que corregir");
        if (nuevo.isEmpty() && hechosActuales.isEmpty()) throw new IllegalStateException("No se deja una atención pendiente sin hechos");
        nuevo.ifPresent(c -> validarCierre(c, cambio, canonico));
        var registros = new ArrayList<>(cierres);
        registros.add(new RevisionCierre(nuevo, cambio));
        return copiar(hechosActuales, hechosHistoricos, antecedentes, vinculos, actuaciones, derivaciones, registros, cambio);
    }

    private void validarCierre(CierreLocal cierre, RegistroCambio cambio, java.util.function.Function<UUID, UUID> canonico) {
        if (cierre.fecha().isBefore(fechaInicio)) throw new IllegalArgumentException("El cierre precede al inicio");
        if (cierre.registro().fecha().isAfter(cambio.fecha()) || !cierre.registro().autorId().equals(cambio.autorId()))
            throw new IllegalArgumentException("El cierre debe conservar al operador y un registro coherente");
        if (cierre.tipo() == CierreLocal.TipoCierre.DERIVACION_FAMILIAR_FISCALIA) {
            var origen = derivaciones.get(cierre.derivacionOrigenId().orElseThrow());
            if (origen == null || !origen.esViolenciaFamiliarEfectivaAFiscalia() || hechosActuales.isEmpty()
                    || !origen.hechosIds().stream().map(canonico).collect(java.util.stream.Collectors.toSet())
                            .containsAll(hechosActuales.stream().map(canonico).toList()) || !origen.fechaEfectiva().orElseThrow().equals(cierre.fecha()))
                throw new IllegalArgumentException("El cierre por derivación requiere una ejecución real de cobertura completa");
        }
        if (cierre.tipo() == CierreLocal.TipoCierre.APERTURA_ERRONEA) {
            cambio.exigirMotivo();
            if (!hechosActuales.isEmpty()) throw new IllegalArgumentException("La apertura errónea se conserva sin vínculos vigentes");
        }
        if (cierre.tipo() == CierreLocal.TipoCierre.DUPLICIDAD) cambio.exigirMotivo();
    }

    private void validarCambio(RegistroCambio cambio) {
        if (cambio.fecha().isBefore(ultimaModificacion())) throw new IllegalArgumentException("El cambio precede al caso vigente");
    }
    private CasoAtencion copiar(Set<UUID> actuales, Set<UUID> historicos, Set<UUID> previos, List<Vinculo> vinculos,
                                Map<UUID, Actuacion> actuaciones, Map<UUID, Derivacion> derivaciones,
                                List<RevisionCierre> cierres, RegistroCambio cambio) {
        var cambios = new ArrayList<>(this.cambios);
        cambios.add(cambio);
        return new CasoAtencion(id, responsableId, fechaInicio, apertura, actuales, historicos, previos, vinculos,
                cambios, actuaciones, derivaciones, cierres, encargado, convocatorias, intervencionesComite,
                decisionesComite, actosComite, cambiosResponsable, continuidades);
    }

    private CasoAtencion conGobierno(UUID responsable, EncargadoAtencion encargado, List<ConvocatoriaComite> convocatorias,
                                     List<IntervencionComite> intervenciones, List<DecisionComite> decisiones,
                                     List<ActoComite> actos, List<CambioResponsable> responsables, List<Continuidad> continuidades,
                                     RegistroCambio cambio) {
        var cambios = new ArrayList<>(this.cambios);
        cambios.add(cambio);
        return new CasoAtencion(id, responsable, fechaInicio, apertura, hechosActuales, hechosHistoricos, antecedentes,
                vinculos, cambios, actuaciones, derivaciones, cierres, encargado, convocatorias, intervenciones,
                decisiones, actos, responsables, continuidades);
    }
}
