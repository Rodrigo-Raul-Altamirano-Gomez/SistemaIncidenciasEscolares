package pe.edu.incidencias.domain.documento;

import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.catalogo.ClasificacionCategoria;
import pe.edu.incidencias.domain.estudiante.DocumentoIdentidad;
import pe.edu.incidencias.domain.incidencia.FechaHecho;
import pe.edu.incidencias.domain.incidencia.PapelParticipacion;
import pe.edu.incidencias.domain.incidencia.TipoPersona;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.institucion.DatosInstitucion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Instantánea de datos preparados para renderizar. No contiene bytes PDF ni equivale a envío/firma oficial. */
public record InformeCaso(Optional<UUID> id, UUID casoId, DatosInstitucion institucion, Instant corte,
                           Instant preparadoEl, UUID preparadoPor, Optional<RegistroCambio> emision,
                           String motivo, DestinoInforme destino, CasoAtencion.Estado estadoLocal,
                           List<HechoInforme> hechos, List<AccionInforme> actuaciones,
                           List<DerivacionInforme> derivaciones, Optional<CierreInforme> cierre,
                           List<ActaInforme> actas, List<ReferenciaInforme> referencias,
                           Map<String, String> contextoSeleccionado, boolean seguimientoPendiente,
                           Optional<RegistroCambio> revisionExterna, Optional<String> baseComunicacion,
                           Optional<String> evaluacionRiesgo, GobiernoInforme gobierno) {
    public record ReporteInforme(UUID id, Optional<UUID> autorId, Instant registradoEl, FechaHecho fechaComunicada,
                                 ClasificacionCategoria clasificacionComunicada, String relato, boolean anulado,
                                 List<VersionReporteInforme> versiones, List<String> aclaracionesAutor) {
        public ReporteInforme { versiones = List.copyOf(versiones); aclaracionesAutor = List.copyOf(aclaracionesAutor); }
    }
    public record VersionReporteInforme(Instant registradaEl, Optional<UUID> autorId, FechaHecho fecha,
                                        ClasificacionCategoria clasificacion, String relato, List<PersonaFuenteInforme> personas) {
        public VersionReporteInforme { personas = List.copyOf(personas); }
    }
    public record PersonaFuenteInforme(UUID referenciaInterna, String identificacion, PapelParticipacion papel,
                                        Optional<String> contacto, Optional<DocumentoIdentidad> documento) {
        public PersonaFuenteInforme(UUID referenciaInterna, String identificacion, PapelParticipacion papel) {
            this(referenciaInterna, identificacion, papel, Optional.empty(), Optional.empty());
        }
    }
    public record PapelFuente(UUID reporteId, PapelParticipacion papel) { }
    public record ParticipanteInforme(UUID referenciaInterna, TipoPersona tipo, String identificacion,
                                      Optional<DocumentoIdentidad> documento, List<PapelFuente> fuentes,
                                      Optional<PapelParticipacion> papelVigente, boolean discrepancia, Optional<String> contacto) {
        public ParticipanteInforme { fuentes = List.copyOf(fuentes); }
        public ParticipanteInforme(UUID referenciaInterna, TipoPersona tipo, String identificacion,
                                    Optional<DocumentoIdentidad> documento, List<PapelFuente> fuentes,
                                    Optional<PapelParticipacion> papelVigente, boolean discrepancia) {
            this(referenciaInterna, tipo, identificacion, documento, fuentes, papelVigente, discrepancia, Optional.empty());
        }
    }
    public record HechoInforme(UUID id, UUID reportePrincipalId, Optional<FechaHecho> fechaVigente, Optional<ClasificacionCategoria> clasificacionVigente,
                                boolean datosDiscrepantes, List<ReporteInforme> reportes, List<ParticipanteInforme> personas,
                                List<PrecisionInforme> precisiones, List<AclaracionPapelInforme> aclaracionesPapel,
                                List<CambioPrincipalInforme> cambiosPrincipal) {
        public HechoInforme {
            reportes = List.copyOf(reportes); personas = List.copyOf(personas);
            precisiones = List.copyOf(precisiones); aclaracionesPapel = List.copyOf(aclaracionesPapel);
            cambiosPrincipal = List.copyOf(cambiosPrincipal);
        }
    }
    public record CambioPrincipalInforme(UUID anteriorId, UUID nuevoId, Instant registradoEl, String motivo) { }
    public record PrecisionInforme(FechaHecho fecha, ClasificacionCategoria clasificacion, Instant registradaEl,
                                   String autor, String motivo) { }
    public record AclaracionPapelInforme(UUID personaId, PapelParticipacion papel, String fundamento, Instant registradaEl) { }
    public record SeguimientoInforme(LocalDate fecha, String observacion, Compromiso.Estado estado, boolean pendiente) { }
    public record CompromisoInforme(UUID id, String contenido, String asumidoPor, Optional<LocalDate> objetivo,
                                    Compromiso.Estado estado, boolean pendiente, List<SeguimientoInforme> seguimientos) {
        public CompromisoInforme { seguimientos = List.copyOf(seguimientos); }
    }
    public record AccionInforme(UUID id, Actuacion.Estado estado, String relato, Optional<LocalDate> prevista,
                                Optional<LocalDate> efectiva, List<CompromisoInforme> compromisos) {
        public AccionInforme { compromisos = List.copyOf(compromisos); }
    }
    public record DerivacionInforme(UUID id, Derivacion.Estado estado, String destinatario, String relato,
                                    Optional<LocalDate> efectiva, Optional<String> referencia) { }
    public record CierreInforme(LocalDate fecha, CierreLocal.TipoCierre tipo, String resultado) { }
    public record ActaInforme(UUID id, LocalDate fecha, String descripcion, Optional<UUID> archivoId) { }
    public record ReferenciaInforme(UUID id, InstrumentoOficial instrumento, Optional<String> codigo,
                                    Optional<LocalDate> registradaEl, String detalle) { }
    public record MiembroInforme(String identificacion, Set<pe.edu.incidencias.domain.acceso.RolUsuario> roles) {
        public MiembroInforme { roles = Set.copyOf(roles); }
    }
    public record ConvocatoriaInforme(UUID id, LocalDate fecha, String motivo, List<MiembroInforme> composicion,
                                      List<String> miembrosElegibles, String autor, Optional<String> motivoSustitucion) {
        public ConvocatoriaInforme { composicion = List.copyOf(composicion); miembrosElegibles = List.copyOf(miembrosElegibles); }
    }
    public record IntervencionInforme(UUID id, UUID convocatoriaId, Instant ocurridaEn, List<String> participantes, String autor,
                                      Optional<String> motivoSustitucion) {
        public IntervencionInforme { participantes = List.copyOf(participantes); }
    }
    public record DecisionInforme(UUID id, UUID intervencionId, String contenido, Instant registradaEl,
                                  String autor, Optional<String> motivoSustitucion) { }
    public record ActoInforme(ActoComite.Tipo tipo, UUID referenciaId, UUID intervencionId, Instant registradoEl,
                              String autor, Optional<String> motivoSustitucion) { }
    public record CambioResponsableInforme(String anterior, String nuevo, Instant registradoEl, Optional<String> motivo) { }
    public record ContinuidadInforme(UUID destinoId, Set<UUID> hechosIds, Instant registradaEl, Optional<String> motivo,
                                     Optional<Instant> corregidaEl, Optional<String> motivoCorreccion) {
        public ContinuidadInforme { hechosIds = Set.copyOf(hechosIds); }
    }
    public record AntecedenteInforme(UUID casoId, CasoAtencion.Estado estadoLocal, boolean trabajoPendiente) { }
    public record GobiernoInforme(EncargadoAtencion encargado, String responsable, List<ConvocatoriaInforme> convocatorias,
                                  List<IntervencionInforme> intervenciones, List<DecisionInforme> decisiones,
                                  List<ActoInforme> actos, List<CambioResponsableInforme> cambiosResponsable,
                                  List<ContinuidadInforme> continuidades, List<AntecedenteInforme> antecedentes) {
        public GobiernoInforme {
            convocatorias = List.copyOf(convocatorias); intervenciones = List.copyOf(intervenciones);
            decisiones = List.copyOf(decisiones); actos = List.copyOf(actos);
            cambiosResponsable = List.copyOf(cambiosResponsable); continuidades = List.copyOf(continuidades);
            antecedentes = List.copyOf(antecedentes);
        }
    }

    public InformeCaso {
        java.util.Objects.requireNonNull(id);
        java.util.Objects.requireNonNull(casoId);
        java.util.Objects.requireNonNull(institucion);
        java.util.Objects.requireNonNull(corte);
        java.util.Objects.requireNonNull(preparadoEl);
        java.util.Objects.requireNonNull(preparadoPor);
        java.util.Objects.requireNonNull(emision);
        java.util.Objects.requireNonNull(destino);
        java.util.Objects.requireNonNull(estadoLocal);
        java.util.Objects.requireNonNull(cierre);
        java.util.Objects.requireNonNull(revisionExterna);
        java.util.Objects.requireNonNull(evaluacionRiesgo);
        java.util.Objects.requireNonNull(baseComunicacion);
        java.util.Objects.requireNonNull(gobierno);
        if (motivo == null || motivo.isBlank()) throw new IllegalArgumentException("El informe requiere motivo de emisión");
        if (corte.isAfter(preparadoEl)) throw new IllegalArgumentException("El corte no puede ser futuro");
        if (id.isPresent() != emision.isPresent()) throw new IllegalArgumentException("Solo el informe emitido tiene identificador de emisión");
        if (emision.filter(e -> e.fecha().isBefore(preparadoEl)).isPresent()) throw new IllegalArgumentException("La emisión precede a la preparación");
        if ((destino.tipo() == DestinoInforme.Tipo.TERCERO) != revisionExterna.isPresent())
            throw new IllegalArgumentException("La salida a terceros debe conservar revisión explícita");
        if (revisionExterna.isPresent() != evaluacionRiesgo.isPresent() || evaluacionRiesgo.filter(String::isBlank).isPresent())
            throw new IllegalArgumentException("La salida a terceros conserva su evaluación del riesgo");
        if (revisionExterna.isPresent() != baseComunicacion.isPresent() || baseComunicacion.filter(String::isBlank).isPresent())
            throw new IllegalArgumentException("La salida a terceros conserva su base de comunicación declarada");
        hechos = List.copyOf(hechos);
        actuaciones = List.copyOf(actuaciones);
        derivaciones = List.copyOf(derivaciones);
        actas = List.copyOf(actas);
        referencias = List.copyOf(referencias);
        contextoSeleccionado = Map.copyOf(contextoSeleccionado);
    }
    public boolean emitido() { return emision.isPresent(); }
    InformeCaso emitir(UUID id, RegistroCambio emision) {
        if (emitido()) throw new IllegalStateException("El informe ya fue emitido");
        return new InformeCaso(Optional.of(id), casoId, institucion, corte, preparadoEl, preparadoPor, Optional.of(emision),
                motivo, destino, estadoLocal, hechos, actuaciones, derivaciones, cierre, actas, referencias,
                contextoSeleccionado, seguimientoPendiente, revisionExterna, baseComunicacion, evaluacionRiesgo, gobierno);
    }
}
