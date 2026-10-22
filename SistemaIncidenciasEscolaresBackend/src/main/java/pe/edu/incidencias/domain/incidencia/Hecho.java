package pe.edu.incidencias.domain.incidencia;

import pe.edu.incidencias.domain.catalogo.ClasificacionCategoria;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** Un hecho con comunicaciones separadas. No constituye una vista autorizada para un usuario. */
public final class Hecho {
    public enum Operacion { VINCULAR_DUPLICADO, RETIRAR_DUPLICADO }
    public record Vinculo(Operacion operacion, UUID reporteId, RegistroCambio cambio) { }
    public record PrecisionDatos(FechaHecho fechaHecho, ClasificacionCategoria clasificacion,
                                RegistroCambio cambio) {
        public PrecisionDatos {
            Objects.requireNonNull(fechaHecho, "fechaHecho");
            Objects.requireNonNull(clasificacion, "clasificacion");
            Objects.requireNonNull(cambio, "cambio").exigirMotivo();
        }
    }
    public record CambioPrincipal(UUID anteriorId, UUID nuevoId, RegistroCambio cambio) { }
    public record AporteParticipacion(UUID reporteId, UUID autorId, Instant fechaRegistro,
                                      Participacion participacion) { }
    public record AclaracionParticipacion(UUID personaId, PapelParticipacion papel, String fundamento,
                                          RegistroCambio cambio, Map<UUID, PapelParticipacion> fuentes) {
        public AclaracionParticipacion { fuentes = Map.copyOf(fuentes); }
    }
    public record ParticipacionConsolidada(UUID personaId, List<AporteParticipacion> aportes,
                                           Optional<AclaracionParticipacion> aclaracionVigente,
                                           boolean discrepancia, Optional<PapelParticipacion> papelVigente) {
        public ParticipacionConsolidada { aportes = List.copyOf(aportes); }
        public PersonaInvolucrada persona() {
            var base = aportes.getFirst().participacion().persona();
            var cuenta = aportes.stream().flatMap(a -> a.participacion().persona().usuarioId().stream()).findFirst();
            var relacion = aportes.stream().flatMap(a -> a.participacion().persona().funcionORelacion().stream()).findFirst();
            return new PersonaInvolucrada(base.id(), base.tipo(), base.descripcion(), cuenta, relacion);
        }
    }

    private final UUID id;
    private final UUID principalId;
    private final List<ReporteIncidencia> reportes;
    private final List<Vinculo> vinculos;
    private final List<AclaracionParticipacion> aclaraciones;
    private final List<PrecisionDatos> precisiones;
    private final List<CambioPrincipal> cambiosPrincipal;

    private Hecho(UUID id, UUID principalId, List<ReporteIncidencia> reportes, List<Vinculo> vinculos,
                  List<AclaracionParticipacion> aclaraciones, List<PrecisionDatos> precisiones,
                  List<CambioPrincipal> cambiosPrincipal) {
        this.id = id;
        this.principalId = principalId;
        this.reportes = List.copyOf(reportes);
        this.vinculos = List.copyOf(vinculos);
        this.aclaraciones = List.copyOf(aclaraciones);
        this.precisiones = List.copyOf(precisiones);
        this.cambiosPrincipal = List.copyOf(cambiosPrincipal);
    }

    public static Hecho de(ReporteIncidencia principal) {
        Objects.requireNonNull(principal, "principal");
        return new Hecho(principal.id(), principal.id(), List.of(principal), List.of(), List.of(), List.of(), List.of());
    }
    /** Identidad del hecho, estable aunque cambie el reporte principal. */
    public UUID id() { return id; }
    /** Comunicación seleccionada, no una proyección de los datos institucionales vigentes del hecho. */
    public ReporteIncidencia principal() {
        return reportes.stream().filter(r -> r.id().equals(principalId)).findFirst().orElseThrow();
    }
    public List<ReporteIncidencia> reportes() { return reportes; }
    public List<Vinculo> vinculos() { return vinculos; }
    public List<AclaracionParticipacion> aclaraciones() { return aclaraciones; }
    public List<PrecisionDatos> precisiones() { return precisiones; }
    public List<CambioPrincipal> cambiosPrincipal() { return cambiosPrincipal; }
    public boolean anulado() { return reportes.stream().allMatch(ReporteIncidencia::anulado); }

    /** Sin precisión expresa, solo se utiliza un valor coincidente entre las fuentes activas. */
    public Optional<FechaHecho> fechaVigente() {
        if (anulado()) return Optional.empty();
        return precisiones.isEmpty() ? valorCoincidente(DatosReporte::fechaHecho)
                : Optional.of(precisiones.getLast().fechaHecho());
    }

    public Optional<ClasificacionCategoria> clasificacionVigente() {
        if (anulado()) return Optional.empty();
        return precisiones.isEmpty() ? valorCoincidente(DatosReporte::clasificacion)
                : Optional.of(precisiones.getLast().clasificacion());
    }

    public boolean requierePrecisionDatos() {
        return !anulado() && (fechaVigente().isEmpty() || clasificacionVigente().isEmpty());
    }

    /** Precisión institucional del hecho; las comunicaciones y sus discrepancias no se reescriben. */
    public Hecho precisarDatos(FechaHecho fecha, ClasificacionCategoria clasificacion, RegistroCambio cambio) {
        validarCambio(cambio);
        if (anulado()) throw new IllegalStateException("No se precisa un hecho sin reportes válidos");
        var precision = new PrecisionDatos(fecha, clasificacion, cambio);
        if (fecha.fecha().isAfter(cambio.fecha().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate()))
            throw new IllegalArgumentException("La fecha efectiva del hecho no puede ser futura");
        var nuevas = new ArrayList<>(precisiones);
        nuevas.add(precision);
        var atendidos = reportes.stream().map(r -> r.anulado() ? r : r.marcarAtencionInstitucional(cambio)).toList();
        return new Hecho(id, principalId, atendidos, vinculos, aclaraciones, nuevas, cambiosPrincipal);
    }

    /** Cambia la referencia principal, manteniendo la identidad estable del hecho y todos sus aportes. */
    public Hecho cambiarPrincipal(UUID nuevoPrincipalId, RegistroCambio cambio) {
        validarCambio(cambio);
        cambio.exigirMotivo();
        var nuevo = reportes.stream().filter(r -> r.id().equals(nuevoPrincipalId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El principal debe pertenecer al hecho"));
        if (nuevo.anulado()) throw new IllegalArgumentException("El principal nuevo debe ser un reporte válido");
        if (principalId.equals(nuevoPrincipalId)) return this;
        var cambios = new ArrayList<>(cambiosPrincipal);
        cambios.add(new CambioPrincipal(principalId, nuevoPrincipalId, cambio));
        var atendidos = reportes.stream().map(r -> r.anulado() ? r : r.marcarAtencionInstitucional(cambio)).toList();
        return new Hecho(id, nuevoPrincipalId, atendidos, vinculos, aclaraciones, precisiones, cambios);
    }

    public Hecho vincularDuplicado(ReporteIncidencia duplicado, RegistroCambio cambio) {
        Objects.requireNonNull(duplicado, "duplicado");
        if (anulado()) throw new IllegalStateException("No se vinculan nuevos reportes a un hecho anulado");
        validarCambio(cambio);
        if (reportes.stream().anyMatch(r -> r.id().equals(duplicado.id())))
            throw new IllegalArgumentException("El reporte ya pertenece al hecho");
        if (duplicado.ultimaModificacion().isAfter(cambio.fecha()))
            throw new IllegalArgumentException("El vínculo precede al reporte");
        var nuevos = new ArrayList<>(reportes);
        nuevos.add(duplicado);
        validarIdentidades(nuevos);
        if (nuevos.stream().anyMatch(r -> r.inicioAtencion().isPresent())) {
            nuevos.replaceAll(r -> r.anulado() ? r : r.marcarAtencionInstitucional(cambio));
        }
        var historia = new ArrayList<>(vinculos);
        historia.add(new Vinculo(Operacion.VINCULAR_DUPLICADO, duplicado.id(), cambio));
        return copiar(nuevos, historia, aclaraciones);
    }

    /** El reporte retirado continúa íntegro en el resultado anterior y en su agregado independiente. */
    public Hecho retirarDuplicado(UUID reporteId, RegistroCambio cambio) {
        validarCambio(cambio);
        cambio.exigirMotivo();
        if (principalId.equals(reporteId)) throw new IllegalArgumentException("No se retira el principal como duplicado");
        if (reportes.stream().noneMatch(r -> r.id().equals(reporteId)))
            throw new IllegalArgumentException("El reporte no pertenece al hecho");
        var nuevos = reportes.stream().filter(r -> !r.id().equals(reporteId)).toList();
        var historia = new ArrayList<>(vinculos);
        historia.add(new Vinculo(Operacion.RETIRAR_DUPLICADO, reporteId, cambio));
        return copiar(nuevos, historia, aclaraciones);
    }

    /** Sustituye una referencia por una versión descendiente, sin aceptar pérdida de historia. */
    public Hecho actualizarReporte(ReporteIncidencia actualizado) {
        var anterior = reportes.stream().filter(r -> r.id().equals(actualizado.id())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Reporte ajeno al hecho"));
        if (!continua(anterior.revisiones(), actualizado.revisiones())
                || !continua(anterior.aclaraciones(), actualizado.aclaraciones())
                || (anterior.inicioAtencion().isPresent() && !anterior.inicioAtencion().equals(actualizado.inicioAtencion()))
                || (anterior.anulacion().isPresent() && !anterior.anulacion().equals(actualizado.anulacion())))
            throw new IllegalArgumentException("No se puede reemplazar por una versión incompatible o anterior");
        var nuevos = reportes.stream().map(r -> r.id().equals(actualizado.id()) ? actualizado : r).toList();
        validarIdentidades(nuevos);
        var marcador = nuevos.stream().flatMap(r -> r.inicioAtencion().stream())
                .max(java.util.Comparator.comparing(RegistroCambio::fecha));
        if (marcador.isPresent()) {
            var ultima = nuevos.stream().map(ReporteIncidencia::ultimaModificacion).max(Instant::compareTo).orElseThrow();
            if (instanteVigente().isAfter(ultima)) ultima = instanteVigente();
            var propagacion = new RegistroCambio(marcador.get().autorId(), ultima, Optional.empty());
            nuevos = nuevos.stream().map(r -> r.anulado() ? r : r.marcarAtencionInstitucional(propagacion)).toList();
        }
        return copiar(nuevos, vinculos, aclaraciones);
    }

    public Hecho aclararParticipacion(UUID personaId, PapelParticipacion papel, String fundamento,
                                      RegistroCambio cambio) {
        validarCambio(cambio);
        Objects.requireNonNull(papel, "papel");
        if (fundamento == null || fundamento.isBlank()) throw new IllegalArgumentException("Se necesita fundamento de la aclaración");
        var participacion = participaciones().stream().filter(p -> p.personaId().equals(personaId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Persona ajena al hecho vigente"));
        if (papel == PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA)
            DatosReporte.validarPapel(papel, participacion.persona(), clasificacionVigente()
                    .orElseThrow(() -> new IllegalStateException("Primero debe precisarse la clasificación del hecho")));
        var nuevas = new ArrayList<>(aclaraciones);
        nuevas.add(new AclaracionParticipacion(personaId, papel, fundamento, cambio, firma(participacion.aportes())));
        var atendidos = reportes.stream().map(r -> r.anulado() ? r : r.marcarAtencionInstitucional(cambio)).toList();
        return copiar(atendidos, vinculos, nuevas);
    }

    public List<ParticipacionConsolidada> participaciones() {
        var porPersona = new LinkedHashMap<UUID, List<AporteParticipacion>>();
        if (anulado()) return List.of();
        for (var reporte : reportes) {
            if (reporte.anulado()) continue;
            for (var participacion : reporte.datos().participaciones()) {
                porPersona.computeIfAbsent(participacion.persona().id(), _ -> new ArrayList<>())
                        .add(new AporteParticipacion(reporte.id(), reporte.autorId(), reporte.fechaRegistro(), participacion));
            }
        }
        var resultado = new ArrayList<ParticipacionConsolidada>();
        porPersona.forEach((id, aportes) -> {
            var firma = firma(aportes);
            var ultimaAclaracion = aclaraciones.stream().filter(a -> a.personaId().equals(id)).reduce((a, b) -> b);
            var vigente = ultimaAclaracion.filter(a -> a.fuentes().equals(firma));
            var papeles = firma.values().stream().distinct().toList();
            boolean discrepancia = papeles.size() > 1;
            var papel = vigente.map(AclaracionParticipacion::papel)
                    .or(() -> discrepancia ? Optional.empty() : Optional.of(papeles.getFirst()));
            resultado.add(new ParticipacionConsolidada(id, aportes, vigente, discrepancia, papel));
        });
        return List.copyOf(resultado);
    }

    public boolean requiereAvisoInmediato() {
        if (anulado()) return false;
        boolean solicitud = reportes.stream().filter(r -> !r.anulado()).anyMatch(r -> r.datos().solicitaAtencion());
        return solicitud || clasificacionVigente().map(ClasificacionCategoria::requiereAvisoInmediato)
                .orElseGet(() -> reportes.stream().filter(r -> !r.anulado())
                        .anyMatch(r -> r.datos().clasificacion().requiereAvisoInmediato()));
    }

    /** Discrepancia de las fuentes originales, incluso cuando existe una precisión institucional vigente. */
    public boolean discrepanciaFechaOCategoria() {
        return !anulado() && (valorCoincidente(DatosReporte::fechaHecho).isEmpty()
                || valorCoincidente(DatosReporte::clasificacion).isEmpty());
    }

    private <T> Optional<T> valorCoincidente(Function<DatosReporte, T> dato) {
        var valores = reportes.stream().filter(r -> !r.anulado()).map(r -> dato.apply(r.datos())).distinct().limit(2).toList();
        return valores.size() == 1 ? Optional.of(valores.getFirst()) : Optional.empty();
    }

    private Hecho copiar(List<ReporteIncidencia> nuevosReportes, List<Vinculo> nuevosVinculos,
                         List<AclaracionParticipacion> nuevasAclaraciones) {
        return new Hecho(id, principalId, nuevosReportes, nuevosVinculos, nuevasAclaraciones, precisiones, cambiosPrincipal);
    }

    private static Map<UUID, PapelParticipacion> firma(List<AporteParticipacion> aportes) {
        var firma = new LinkedHashMap<UUID, PapelParticipacion>();
        aportes.forEach(a -> firma.put(a.reporteId(), a.participacion().papel()));
        return Map.copyOf(firma);
    }

    private void validarCambio(RegistroCambio cambio) {
        if (cambio.fecha().isBefore(instanteVigente())) throw new IllegalArgumentException("El cambio precede a la información vigente");
    }

    private Instant instanteVigente() {
        var ultima = reportes.stream().map(ReporteIncidencia::ultimaModificacion).max(Instant::compareTo).orElseThrow();
        if (!vinculos.isEmpty() && vinculos.getLast().cambio().fecha().isAfter(ultima)) ultima = vinculos.getLast().cambio().fecha();
        if (!aclaraciones.isEmpty() && aclaraciones.getLast().cambio().fecha().isAfter(ultima)) ultima = aclaraciones.getLast().cambio().fecha();
        if (!precisiones.isEmpty() && precisiones.getLast().cambio().fecha().isAfter(ultima)) ultima = precisiones.getLast().cambio().fecha();
        if (!cambiosPrincipal.isEmpty() && cambiosPrincipal.getLast().cambio().fecha().isAfter(ultima)) ultima = cambiosPrincipal.getLast().cambio().fecha();
        return ultima;
    }

    private static boolean continua(List<?> anterior, List<?> nuevo) {
        return nuevo.size() >= anterior.size() && nuevo.subList(0, anterior.size()).equals(anterior);
    }
    private static void validarIdentidades(List<ReporteIncidencia> reportes) {
        var identidades = new LinkedHashMap<UUID, PersonaInvolucrada>();
        var personalPorCuenta = new LinkedHashMap<UUID, UUID>();
        var cuentaPorPersona = new LinkedHashMap<UUID, UUID>();
        for (var reporte : reportes) for (var p : reporte.datos().participaciones()) {
            var persona = p.persona();
            var anterior = identidades.putIfAbsent(persona.id(), persona);
            if (anterior != null && (anterior.tipo() != persona.tipo()
                    || (anterior.usuarioId().isPresent() && persona.usuarioId().isPresent()
                    && !anterior.usuarioId().equals(persona.usuarioId()))))
                throw new IllegalArgumentException("La misma identidad no puede representar personas de distinto tipo o cuenta");
            persona.usuarioId().ifPresent(cuenta -> {
                var cuentaPrevia = cuentaPorPersona.putIfAbsent(persona.id(), cuenta);
                if (cuentaPrevia != null && !cuentaPrevia.equals(cuenta))
                    throw new IllegalArgumentException("Una identidad de personal no puede representar dos cuentas");
                var idPrevio = personalPorCuenta.putIfAbsent(cuenta, persona.id());
                if (idPrevio != null && !idPrevio.equals(persona.id()))
                    throw new IllegalArgumentException("Una cuenta de personal no puede representar dos identidades");
            });
        }
    }
}
