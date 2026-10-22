package pe.edu.incidencias.domain.documento;

import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.estudiante.DocumentoIdentidad;
import pe.edu.incidencias.domain.estudiante.Estudiante;
import pe.edu.incidencias.domain.incidencia.*;
import pe.edu.incidencias.domain.institucion.DatosInstitucion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static pe.edu.incidencias.domain.documento.InformeCaso.*;

/** Proyección pura. Cada operación protegida debe usar una instancia con los registros actuales. */
public final class GeneradorInformeCaso {
    private final RegistroAtenciones atenciones;
    private final RegistroActas actas;
    private final ReferenciasOficiales referencias;
    private final DirectorioUsuarios directorio;
    private final Map<UUID, Estudiante> estudiantes;

    public GeneradorInformeCaso(RegistroAtenciones atenciones, RegistroActas actas, ReferenciasOficiales referencias,
                                DirectorioUsuarios directorio, Map<UUID, Estudiante> estudiantes) {
        this.atenciones = Objects.requireNonNull(atenciones);
        this.actas = Objects.requireNonNull(actas);
        this.referencias = Objects.requireNonNull(referencias);
        this.directorio = Objects.requireNonNull(directorio);
        this.estudiantes = Map.copyOf(estudiantes);
        this.estudiantes.forEach((id, estudiante) -> {
            if (!id.equals(estudiante.id())) throw new IllegalArgumentException("La identidad del estudiante no coincide con su clave");
        });
    }

    public InformeCaso previsualizar(UUID casoId, DatosInstitucion institucion, String motivo, DestinoInforme destino,
                                     Optional<RevisionSalida> revision, Map<String, String> contextoInterno,
                                     UUID usuario, Instant corte, Instant ahora) {
        if (corte.isAfter(ahora)) throw new IllegalArgumentException("El corte no puede ser futuro");
        var caso = atenciones.consultarCaso(casoId, usuario, directorio, ahora);
        autorizar(casoId, usuario, Permiso.GENERAR_INFORME, ahora);
        boolean externo = destino.tipo() == DestinoInforme.Tipo.TERCERO;
        if (externo != revision.isPresent()) throw new IllegalArgumentException("Solo la salida a terceros requiere revisión explícita");
        if (externo) {
            autorizar(casoId, usuario, Permiso.EXPORTAR_ATENCION, ahora);
            var control = revision.orElseThrow();
            if (!control.casoId().equals(casoId) || control.revision().fecha().isBefore(corte)
                    || control.revision().fecha().isAfter(ahora)) throw new IllegalArgumentException("La revisión debe corresponder al caso y cubrir su corte");
            autorizar(casoId, control.revision().autorId(), Permiso.EXPORTAR_ATENCION, ahora);
        }
        var hechos = atenciones.hechosDelCaso(casoId, usuario, directorio, ahora);
        var acciones = atenciones.actuacionesDelCaso(casoId, usuario, directorio, ahora);
        var documentos = actas.consultar(casoId, usuario, ahora, atenciones, directorio);
        var referenciasCaso = referencias.consultar(atenciones.contexto(casoId), usuario, ahora,
                new PoliticaAcceso(directorio, atenciones.restricciones(), atenciones.proteccion()));
        exigirCorte(caso.ultimaModificacion(), corte);
        atenciones.contexto(casoId).ambito().casosIds().forEach(id ->
                atenciones.caso(id).ifPresent(relacionado -> exigirCorte(relacionado.ultimaModificacion(), corte)));
        hechos.forEach(h -> {
            h.reportes().forEach(r -> exigirCorte(r.ultimaModificacion(), corte));
            h.vinculos().forEach(v -> exigirCorte(v.cambio().fecha(), corte));
            h.precisiones().forEach(p -> exigirCorte(p.cambio().fecha(), corte));
            h.aclaraciones().forEach(a -> exigirCorte(a.cambio().fecha(), corte));
            h.cambiosPrincipal().forEach(c -> exigirCorte(c.cambio().fecha(), corte));
        });
        acciones.forEach(a -> exigirCorte(a.ultimaModificacion(), corte));
        documentos.forEach(a -> exigirCorte(a.vigente().registro().fecha(), corte));
        referenciasCaso.forEach(r -> exigirCorte(r.registro().fecha(), corte));
        boolean violencia = hechos.stream().anyMatch(h -> h.clasificacionVigente()
                .map(c -> c.naturaleza() == pe.edu.incidencias.domain.catalogo.NaturalezaCategoria.VIOLENCIA_PROTECCION)
                .orElseGet(() -> h.reportes().stream().filter(r -> !r.anulado()).anyMatch(r -> r.datos().clasificacion().naturaleza()
                        == pe.edu.incidencias.domain.catalogo.NaturalezaCategoria.VIOLENCIA_PROTECCION)))
                || caso.derivaciones().stream().anyMatch(d -> d.supuesto() == Derivacion.Supuesto.VIOLENCIA_FAMILIAR);
        var proyector = new Proyector(revision, violencia, hechos);
        var contextoProyectado = new java.util.LinkedHashMap<String, String>();
        (externo ? revision.orElseThrow().contextoSeleccionado() : contextoInterno)
                .forEach((clave, valor) -> contextoProyectado.put(clave, proyector.texto("contexto:" + clave, valor)));
        var accionesProyectadas = acciones.stream().map(proyector::accion).toList();
        boolean pendiente = acciones.stream().anyMatch(Actuacion::pendiente)
                || caso.derivaciones().stream().anyMatch(d -> d.estado() == Derivacion.Estado.PREPARADA);
        var destinoRevisado = externo ? DestinoInforme.tercero(proyector.texto("destinatarioInforme", destino.destinatario().orElseThrow()),
                proyector.texto("finalidadInforme", destino.finalidad())) : destino;
        return new InformeCaso(Optional.empty(), casoId, institucion, corte, ahora, usuario, Optional.empty(), proyector.texto("motivoInforme", motivo),
                destinoRevisado, caso.estado(), hechos.stream().map(proyector::hecho).toList(), accionesProyectadas,
                caso.derivaciones().stream().map(proyector::derivacion).toList(), caso.cierre().map(proyector::cierre),
                documentos.stream().map(proyector::acta).toList(), referenciasCaso.stream()
                        .filter(r -> !externo || revision.orElseThrow().referenciasAdmitidas().contains(r.id()))
                        .map(proyector::referencia).toList(),
                contextoProyectado, pendiente,
                revision.map(r -> new RegistroCambio(r.revision().autorId(), r.revision().fecha(), Optional.empty())),
                revision.map(RevisionSalida::baseComunicacion), revision.map(RevisionSalida::evaluacionRiesgo), proyector.gobierno(caso));
    }

    /** Identifica y conserva la instantánea ya preparada; emitir no envía, firma ni registra en un instrumento externo. */
    public InformeCaso emitir(InformeCaso vista, UUID informeId, RegistroCambio emision) {
        Objects.requireNonNull(vista);
        if (!atenciones.proteccion().permiteRecurso(informeId, emision.fecha())) throw new SecurityException("Informe bloqueado");
        atenciones.consultarCaso(vista.casoId(), emision.autorId(), directorio, emision.fecha());
        autorizarInstantanea(vista, emision.autorId(), emision.fecha());
        autorizar(vista.casoId(), emision.autorId(), Permiso.GENERAR_INFORME, emision.fecha());
        if (vista.destino().tipo() == DestinoInforme.Tipo.TERCERO) {
            autorizar(vista.casoId(), emision.autorId(), Permiso.EXPORTAR_ATENCION, emision.fecha());
            autorizar(vista.casoId(), vista.revisionExterna().orElseThrow().autorId(), Permiso.EXPORTAR_ATENCION, emision.fecha());
        }
        var registroEmision = vista.destino().tipo() == DestinoInforme.Tipo.TERCERO
                ? new RegistroCambio(emision.autorId(), emision.fecha(), Optional.empty()) : emision;
        return vista.emitir(informeId, registroEmision);
    }

    /** La autorización usa el ámbito actual y toda su historia; el contenido emitido conserva su corte original. */
    public InformeCaso consultarEmitido(InformeCaso informe, UUID usuario, Instant ahora) {
        if (!informe.emitido()) throw new IllegalArgumentException("Una previsualización no es un informe emitido");
        if (!atenciones.proteccion().permiteRecurso(informe.id().orElseThrow(), ahora)) throw new SecurityException("Informe bloqueado");
        if (informe.emision().orElseThrow().fecha().isAfter(ahora)) throw new IllegalArgumentException("La emisión no puede ser futura");
        atenciones.consultarCaso(informe.casoId(), usuario, directorio, ahora);
        autorizarInstantanea(informe, usuario, ahora);
        autorizar(informe.casoId(), usuario, Permiso.CONSULTAR_DOCUMENTO_RESERVADO, ahora);
        if (informe.destino().tipo() == DestinoInforme.Tipo.TERCERO) autorizar(informe.casoId(), usuario, Permiso.EXPORTAR_ATENCION, ahora);
        return informe;
    }

    private void autorizarInstantanea(InformeCaso informe, UUID usuario, Instant ahora) {
        var ids = informe.hechos().stream().map(HechoInforme::id).collect(Collectors.toSet());
        var reportes = informe.hechos().stream().flatMap(h -> h.reportes().stream()).map(ReporteInforme::id).collect(Collectors.toSet());
        var contexto = atenciones.contextoDocumento(informe.casoId(), new AmbitoReserva(ids, reportes, java.util.Set.of(informe.casoId())));
        PoliticaAcceso.exigir(new PoliticaAcceso(directorio, atenciones.restricciones(), atenciones.proteccion())
                .evaluar(usuario, Permiso.CONSULTAR_DOCUMENTO_RESERVADO, contexto, ahora));
    }

    private void autorizar(UUID caso, UUID usuario, Permiso permiso, Instant ahora) {
        PoliticaAcceso.exigir(new PoliticaAcceso(directorio, atenciones.restricciones(), atenciones.proteccion())
                .evaluar(usuario, permiso, atenciones.contexto(caso), ahora));
    }
    private static void exigirCorte(Instant cambio, Instant corte) {
        if (cambio.isAfter(corte)) throw new IllegalArgumentException("El registro vigente contiene cambios posteriores al corte; se necesita su versión histórica");
    }

    private final class Proyector {
        private final Optional<RevisionSalida> revision;
        private final boolean violencia;
        private final List<PersonaInvolucrada> personas;
        Proyector(Optional<RevisionSalida> revision, boolean violencia, List<Hecho> hechos) {
            this.revision = revision;
            this.violencia = violencia;
            this.personas = hechos.stream().flatMap(h -> h.reportes().stream()).flatMap(r -> r.revisiones().stream())
                    .flatMap(r -> r.datos().participaciones().stream()).map(Participacion::persona).distinct().toList();
        }
        private boolean externo() { return revision.isPresent(); }
        private String texto(String clave, String original) {
            String resultado = revision.map(r -> r.texto(clave, original)).orElse(original);
            if (externo()) for (var persona : personas) {
                boolean contactoOmitido = !revision.orElseThrow().contactosAdmitidos().contains(persona.id());
                boolean documentoOmitido = persona.tipo() != TipoPersona.ESTUDIANTE
                        && !revision.orElseThrow().documentosExternosAdmitidos().contains(persona.id());
                if (contactoOmitido && persona.contacto().filter(resultado::contains).isPresent()
                        || documentoOmitido && persona.documento().map(DocumentoIdentidad::numero).filter(resultado::contains).isPresent())
                    throw new IllegalArgumentException("Debe adecuarse el fragmento con datos de contacto/documento no seleccionados: " + clave);
                boolean protegida = !incluir(persona) || violencia && persona.tipo() == TipoPersona.ESTUDIANTE;
                if (!protegida) continue;
                var nombres = new java.util.HashSet<String>();
                nombres.add(persona.descripcion());
                Optional.ofNullable(estudiantes.get(persona.id())).map(Estudiante::nombreCompleto).ifPresent(nombres::add);
                for (var nombre : nombres) if (resultado.toLowerCase(java.util.Locale.ROOT).contains(nombre.toLowerCase(java.util.Locale.ROOT)))
                    throw new IllegalArgumentException("Debe adecuarse el fragmento que identifica una persona protegida: " + clave);
            }
            return resultado;
        }
        private boolean incluir(PersonaInvolucrada persona) { return !externo() || revision.orElseThrow().personasAdmitidas().contains(persona.id()); }
        private String autor(UUID id) {
            return texto("autor:" + id, directorio.usuario(id).map(Usuario::nombreLaboral).orElse("Referencia institucional " + id));
        }
        private Optional<DocumentoIdentidad> documento(PersonaInvolucrada persona) {
            if (persona.tipo() != TipoPersona.ESTUDIANTE) return !externo()
                    || revision.orElseThrow().documentosExternosAdmitidos().contains(persona.id()) ? persona.documento() : Optional.empty();
            var documento = Optional.ofNullable(estudiantes.get(persona.id())).flatMap(Estudiante::documento);
            return externo() && violencia ? documento.filter(d -> d.tipo().equals("DNI")) : documento;
        }
        private Optional<String> contacto(PersonaInvolucrada persona) {
            return !externo() || revision.orElseThrow().contactosAdmitidos().contains(persona.id()) ? persona.contacto() : Optional.empty();
        }
        private String identidad(PersonaInvolucrada persona) {
            if (!externo() || !violencia) return Optional.ofNullable(estudiantes.get(persona.id())).map(Estudiante::nombreCompleto).orElse(persona.descripcion());
            if (persona.tipo() == TipoPersona.ESTUDIANTE_NO_IDENTIFICADO)
                return "Estudiante no identificado; DNI no disponible; referencia " + persona.id();
            if (persona.tipo() != TipoPersona.ESTUDIANTE) return texto("persona:" + persona.id(), persona.descripcion());
            String nombre = Optional.ofNullable(estudiantes.get(persona.id())).map(Estudiante::nombreCompleto).orElse(persona.descripcion());
            String iniciales = java.util.Arrays.stream(nombre.strip().split("\\s+"))
                    .filter(p -> !p.isBlank()).map(p -> new String(Character.toChars(p.codePointAt(0))).toUpperCase(java.util.Locale.ROOT) + ".")
                    .collect(Collectors.joining(" "));
            return iniciales + documento(persona).map(d -> " · DNI " + d.numero())
                    .orElse(" · DNI no disponible; referencia " + persona.id());
        }
        private HechoInforme hecho(Hecho hecho) {
            var personas = hecho.participaciones().stream().filter(p -> incluir(p.persona())).map(p -> new ParticipanteInforme(
                    p.personaId(), p.persona().tipo(), identidad(p.persona()), documento(p.persona()), p.aportes().stream()
                    .map(a -> new PapelFuente(a.reporteId(), a.participacion().papel())).toList(), p.papelVigente(),
                    p.discrepancia() || p.datosPersonalesDiscrepantes(), contacto(p.persona()))).toList();
            var precisiones = new ArrayList<PrecisionInforme>();
            for (int i = 0; i < hecho.precisiones().size(); i++) {
                var p = hecho.precisiones().get(i);
                precisiones.add(new PrecisionInforme(p.fechaHecho(), p.clasificacion(), p.cambio().fecha(), autor(p.cambio().autorId()),
                        texto("precision:" + hecho.id() + ":" + i, p.cambio().motivo().orElseThrow())));
            }
            var aclaraciones = new ArrayList<AclaracionPapelInforme>();
            for (int i = 0; i < hecho.aclaraciones().size(); i++) {
                var a = hecho.aclaraciones().get(i);
                if (externo() && !revision.orElseThrow().personasAdmitidas().contains(a.personaId())) continue;
                aclaraciones.add(new AclaracionPapelInforme(a.personaId(), a.papel(), texto("papel:" + hecho.id() + ":" + i, a.fundamento()), a.cambio().fecha()));
            }
            var cambiosPrincipal = new ArrayList<CambioPrincipalInforme>();
            for (int i = 0; i < hecho.cambiosPrincipal().size(); i++) {
                var c = hecho.cambiosPrincipal().get(i);
                cambiosPrincipal.add(new CambioPrincipalInforme(c.anteriorId(), c.nuevoId(), c.cambio().fecha(),
                        texto("principal:" + hecho.id() + ":" + i, c.cambio().motivo().orElseThrow())));
            }
            return new HechoInforme(hecho.id(), hecho.principal().id(), hecho.fechaVigente(), hecho.clasificacionVigente(), hecho.discrepanciaFechaOCategoria(),
                    hecho.reportes().stream().map(this::reporte).toList(), personas, precisiones, aclaraciones, cambiosPrincipal);
        }
        private ReporteInforme reporte(ReporteIncidencia reporte) {
            var versiones = new ArrayList<VersionReporteInforme>();
            for (int i = 0; i < reporte.revisiones().size(); i++) {
                var v = reporte.revisiones().get(i);
                versiones.add(new VersionReporteInforme(v.cambio().fecha(), externo() ? Optional.empty() : Optional.of(v.cambio().autorId()),
                        v.datos().fechaHecho(), v.datos().clasificacion(), texto("reporte:" + reporte.id() + ":version:" + i, v.datos().descripcion()),
                        v.datos().participaciones().stream().filter(p -> incluir(p.persona())).map(p ->
                                new PersonaFuenteInforme(p.persona().id(), identidad(p.persona()), p.papel(),
                                        contacto(p.persona()), documento(p.persona()))).toList()));
            }
            var aclaraciones = new ArrayList<String>();
            for (int i = 0; i < reporte.aclaraciones().size(); i++)
                aclaraciones.add(texto("aclaracion:" + reporte.id() + ":" + i, reporte.aclaraciones().get(i).texto()));
            return new ReporteInforme(reporte.id(), externo() ? Optional.empty() : Optional.of(reporte.autorId()), reporte.fechaRegistro(),
                    reporte.datos().fechaHecho(), reporte.datos().clasificacion(), versiones.getLast().relato(), reporte.anulado(), versiones, aclaraciones);
        }
        private AccionInforme accion(Actuacion accion) {
            return new AccionInforme(accion.id(), accion.estado(), texto("actuacion:" + accion.id(), accion.relato()),
                    accion.fechaPrevista(), accion.fechaEfectiva(), accion.compromisos().stream().map(this::compromiso).toList());
        }
        private CompromisoInforme compromiso(Compromiso compromiso) {
            var seguimientos = new ArrayList<SeguimientoInforme>();
            for (int i = 0; i < compromiso.revisiones().size(); i++) {
                var r = compromiso.revisiones().get(i);
                if (r.fechaObservacion().isPresent()) seguimientos.add(new SeguimientoInforme(r.fechaObservacion().get(),
                        texto("seguimiento:" + compromiso.id() + ":" + i, r.observacion().orElseThrow()), r.estado(), r.pendienteSeguimiento()));
            }
            String asumido = incluir(compromiso.asumidoPor()) ? identidad(compromiso.asumidoPor()) : texto("asumido:" + compromiso.id(), compromiso.asumidoPor().descripcion());
            return new CompromisoInforme(compromiso.id(), texto("compromiso:" + compromiso.id(), compromiso.contenido()), asumido,
                    compromiso.fechaObjetivo(), compromiso.estado(), compromiso.pendienteSeguimiento(), seguimientos);
        }
        private DerivacionInforme derivacion(Derivacion derivacion) {
            return new DerivacionInforme(derivacion.id(), derivacion.estado(), texto("destinoDerivacion:" + derivacion.id(), derivacion.destino().nombre()),
                    texto("derivacion:" + derivacion.id(), derivacion.relato()), derivacion.fechaEfectiva(),
                    derivacion.referencia().map(r -> texto("referenciaDerivacion:" + derivacion.id(), r)));
        }
        private CierreInforme cierre(CierreLocal cierre) {
            return new CierreInforme(cierre.fecha(), cierre.tipo(), texto("cierre", cierre.resultado()));
        }
        private ActaInforme acta(ActaAdjunta acta) {
            return new ActaInforme(acta.id(), acta.vigente().fechaActa(), texto("acta:" + acta.id(), acta.vigente().descripcion()),
                    externo() ? Optional.empty() : Optional.of(acta.vigente().archivo().id()));
        }
        private ReferenciaInforme referencia(ReferenciaOficial referencia) {
            String detalle = referencia.origenComunicado() + referencia.protocoloYVersion().map(p -> " · " + p).orElse("")
                    + referencia.observacion().map(o -> " · " + o).orElse("");
            return new ReferenciaInforme(referencia.id(), referencia.instrumento(), referencia.codigoOAsiento()
                    .map(c -> texto("codigo:" + referencia.id(), c)), referencia.fechaRegistroExterno(), texto("referencia:" + referencia.id(), detalle));
        }
        private GobiernoInforme gobierno(CasoAtencion caso) {
            var convocatorias = caso.convocatorias().stream().map(c -> new ConvocatoriaInforme(c.id(), c.fecha(),
                    texto("convocatoria:" + c.id(), c.motivoEspecial()), c.composicion().miembros().stream()
                    .map(m -> new MiembroInforme(texto("autor:" + m.usuarioId(), m.nombreLaboral()), m.roles())).toList(),
                    c.miembrosElegibles().stream().sorted().map(this::autor).toList(), autor(c.registro().autorId()),
                    c.motivoSustitucion().map(m -> texto("sustitucionConvocatoria:" + c.id(), m)))).toList();
            var intervenciones = caso.intervencionesComite().stream().map(i -> new IntervencionInforme(i.id(), i.convocatoriaId(), i.ocurridaEn(),
                    i.participantesIds().stream().sorted().map(this::autor).toList(), autor(i.registro().autorId()),
                    i.motivoSustitucion().map(m -> texto("sustitucionIntervencion:" + i.id(), m)))).toList();
            var decisiones = caso.decisionesComite().stream().map(d -> new DecisionInforme(d.id(), d.intervencionId(),
                    texto("decision:" + d.id(), d.contenido()), d.registro().fecha(), autor(d.registro().autorId()),
                    d.motivoSustitucion().map(m -> texto("sustitucionDecision:" + d.id(), m)))).toList();
            var actos = new ArrayList<ActoInforme>();
            for (int i = 0; i < caso.actosComite().size(); i++) {
                var a = caso.actosComite().get(i);
                String clave = "sustitucionActo:" + a.referenciaId() + ":" + i;
                actos.add(new ActoInforme(a.tipo(), a.referenciaId(), a.intervencionId(), a.registro().fecha(), autor(a.registro().autorId()),
                        a.motivoSustitucion().map(m -> texto(clave, m))));
            }
            var cambios = new ArrayList<CambioResponsableInforme>();
            for (int i = 0; i < caso.cambiosResponsable().size(); i++) {
                var c = caso.cambiosResponsable().get(i);
                String clave = "responsable:" + i;
                cambios.add(new CambioResponsableInforme(autor(c.anteriorId()), autor(c.nuevoId()), c.registro().fecha(),
                        c.registro().motivo().map(m -> texto(clave, m))));
            }
            var continuidades = new ArrayList<ContinuidadInforme>();
            for (int i = 0; i < caso.continuidades().size(); i++) {
                var c = caso.continuidades().get(i);
                String clave = "continuidad:" + i;
                String correccion = "correccionContinuidad:" + i;
                continuidades.add(new ContinuidadInforme(c.destinoId(), c.hechosIds(), c.registro().fecha(),
                        c.registro().motivo().map(m -> texto(clave, m)), c.correccion().map(RegistroCambio::fecha),
                        c.correccion().flatMap(RegistroCambio::motivo).map(m -> texto(correccion, m))));
            }
            var antecedentes = caso.antecedentes().stream().sorted().map(id -> {
                var anterior = atenciones.caso(id).orElseThrow(() -> new IllegalArgumentException("No existe la atención anterior citada"));
                return new AntecedenteInforme(id, anterior.estado(), anterior.requiereTrabajo());
            }).toList();
            return new GobiernoInforme(caso.encargado(), autor(caso.responsableId()), convocatorias, intervenciones, decisiones, actos, cambios, continuidades, antecedentes);
        }
    }
}
