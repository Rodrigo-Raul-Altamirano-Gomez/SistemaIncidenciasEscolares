package pe.edu.incidencias.domain.acceso;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.catalogo.ClasificacionCategoria;
import pe.edu.incidencias.domain.catalogo.Gravedad;
import pe.edu.incidencias.domain.catalogo.NaturalezaCategoria;
import pe.edu.incidencias.domain.incidencia.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConflictosPreventivosTest {
    private static final Instant INICIO = Instant.parse("2026-10-21T15:00:00Z");
    private static final UUID ADMIN = id(1);
    private static final UUID DIRECTOR = id(2);
    private static final UUID TOE = id(3);
    private static final UUID COMITE = id(4);
    private static final UUID DOCENTE = id(5);
    private static final UUID PERSONAL = id(20);
    private static final UUID ESTUDIANTE = id(21);
    private final DirectorioUsuarios directorio = directorio();

    @Test
    void directorDenunciadoSoloEnDuplicadoSeExcluyeAntesDeSeleccionarDestinatarios() {
        var principal = reporte(100, DOCENTE, Optional.empty(), Optional.empty());
        var duplicado = reporte(101, DOCENTE, Optional.of(DIRECTOR), Optional.of(PapelParticipacion.PRESUNTA_RESPONSABILIDAD));
        var hecho = Hecho.de(principal).vincularDuplicado(duplicado, cambio(TOE, 20));
        var contexto = ContextoAcceso.deHecho(hecho);
        var politica = politica(RestriccionesAmbito.vacias());

        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO,
                politica.evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO, contexto, instante(100)).resultado());
        assertFalse(politica.destinatariosAtencion(contexto, instante(100)).usuarios().contains(DIRECTOR));
        assertEquals(List.of(TOE), politica.destinatariosAtencion(contexto, instante(100)).usuarios());
        var paraPersistir = politica.restriccionesActualizadas(contexto, instante(100));
        assertTrue(paraPersistir.estaExcluido(DIRECTOR, contexto.ambito()));
        assertEquals(Set.of(duplicado.id()), paraPersistir.exclusiones().getFirst().ambito().reportesIds());
        assertTrue(RestriccionesAmbito.vacias().exclusiones().isEmpty());
    }

    @Test
    void precisarDespuesLaCuentaDeLaMismaPersonaActivaLaRestriccion() {
        var original = reporte(100, DOCENTE, Optional.empty(), Optional.of(PapelParticipacion.PRESUNTA_RESPONSABILIDAD));
        var antes = Hecho.de(original);
        var politica = politica(RestriccionesAmbito.vacias());
        assertTrue(politica.evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO, ContextoAcceso.deHecho(antes), instante(100)).permitida());
        var identificado = original.corregirInstitucionalmente(datos(Optional.of(DIRECTOR),
                Optional.of(PapelParticipacion.PRESUNTA_RESPONSABILIDAD)), motivo(COMITE, 110));
        var despues = antes.actualizarReporte(identificado);
        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO, politica.evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO,
                ContextoAcceso.deHecho(despues), instante(120)).resultado());
        assertEquals(2, identificado.revisiones().size());
        assertTrue(original.datos().participaciones().getLast().persona().usuarioId().isEmpty());
    }

    @Test
    void directorComoTestigoNoSeExcluyeAutomaticamente() {
        var hecho = Hecho.de(reporte(100, DOCENTE, Optional.of(DIRECTOR), Optional.of(PapelParticipacion.TESTIGO)));
        var contexto = ContextoAcceso.deHecho(hecho);
        var politica = politica(RestriccionesAmbito.vacias());
        assertTrue(politica.evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO, contexto, instante(100)).permitida());
        assertTrue(politica.restriccionesActualizadas(contexto, instante(100)).exclusiones().isEmpty());
    }

    @Test
    void anularDenunciaYCambiarPrincipalNoRetiranLaExclusion() {
        var denuncia = reporte(100, DOCENTE, Optional.of(DIRECTOR), Optional.of(PapelParticipacion.PRESUNTA_RESPONSABILIDAD));
        var adicional = reporte(101, DOCENTE, Optional.empty(), Optional.empty());
        var inicial = Hecho.de(denuncia).vincularDuplicado(adicional, cambio(TOE, 20));
        var ledger = politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(ContextoAcceso.deHecho(inicial), instante(100));
        var anulado = inicial.actualizarReporte(denuncia.anularPorError(motivo(COMITE, 110)));
        var cambiado = anulado.cambiarPrincipal(adicional.id(), motivo(COMITE, 120));
        var politica = politica(ledger);
        assertEquals(inicial.id(), cambiado.id());
        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO, politica.evaluar(DIRECTOR, Permiso.CONSULTAR_DOCUMENTO_RESERVADO,
                ContextoAcceso.deHecho(cambiado), instante(130)).resultado());
        assertTrue(politica.restriccionesActualizadas(ContextoAcceso.deHecho(cambiado), instante(130))
                .estaExcluido(DIRECTOR, AmbitoReserva.deHecho(cambiado)));
    }

    @Test
    void descartarAtribucionInstitucionalmenteNoRevocaUnaExclusionSinDecisionExpresa() {
        var hecho = denuncia();
        var ledger = politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(ContextoAcceso.deHecho(hecho), instante(100));
        var descartado = hecho.aclararParticipacion(PERSONAL, PapelParticipacion.ATRIBUCION_DESCARTADA,
                "Se descarta la atribución ficticia", motivo(COMITE, 110));
        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO, politica(ledger).evaluar(DIRECTOR,
                Permiso.CONSULTAR_HECHO, ContextoAcceso.deHecho(descartado), instante(120)).resultado());
    }

    @Test
    void cambiarElPapelOriginalATestigoConservaElRegistroDeLaDenunciaPreventiva() {
        var hecho = denuncia();
        var ledger = politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(ContextoAcceso.deHecho(hecho), instante(100));
        var corregido = hecho.principal().corregirInstitucionalmente(datos(Optional.of(DIRECTOR),
                Optional.of(PapelParticipacion.TESTIGO)), motivo(COMITE, 110));
        var contexto = ContextoAcceso.deHecho(hecho.actualizarReporte(corregido));
        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO,
                politica(ledger).evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO, contexto, instante(120)).resultado());
        assertEquals(1, politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(contexto, instante(120)).exclusiones().size());
    }

    @Test
    void otroInstitucionalPuedeRetirarMotivadamenteSinQueEvaluarOSincronizarRevivanLaMismaDenuncia() {
        var hecho = denuncia();
        var contexto = ContextoAcceso.deHecho(hecho);
        var ledger = politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(contexto, instante(100));
        UUID exclusionId = ledger.exclusiones().getFirst().id();
        var retiradas = politica(ledger).retirarExclusion(exclusionId, contexto, motivo(COMITE, 110));
        assertTrue(politica(retiradas).evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO, contexto, instante(120)).permitida());
        var revalidado = politica(retiradas).restriccionesActualizadas(contexto, instante(120));
        assertEquals(1, revalidado.exclusiones().size());
        assertEquals(2, revalidado.historial().size());
        assertFalse(revalidado.estaExcluido(DIRECTOR, contexto.ambito()));
        assertEquals(motivo(COMITE, 110), revalidado.exclusiones().getFirst().retiro().orElseThrow());
    }

    @Test
    void excluidoNoPuedeRetirarseLaRestriccionAunqueSeaAutorAdministradorYDirector() {
        var hecho = Hecho.de(reporte(100, DIRECTOR, Optional.of(DIRECTOR), Optional.of(PapelParticipacion.PRESUNTA_RESPONSABILIDAD)));
        var contexto = ContextoAcceso.deAporte(hecho, hecho.principal().id());
        var ledger = politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(contexto, instante(100));
        var politica = politica(ledger);
        for (var permiso : List.of(Permiso.CONSULTAR_APORTE_PROPIO, Permiso.CONSULTAR_HECHO, Permiso.ATENDER,
                Permiso.CONSULTAR_DOCUMENTO_RESERVADO, Permiso.CONSULTAR_ESTADISTICAS, Permiso.EXPORTAR_ATENCION))
            assertEquals(PoliticaAcceso.Resultado.EXCLUIDO,
                    politica.evaluar(DIRECTOR, permiso, contexto, instante(110)).resultado());
        assertThrows(SecurityException.class, () -> politica.retirarExclusion(ledger.exclusiones().getFirst().id(),
                contexto, motivo(DIRECTOR, 110)));
        assertThrows(SecurityException.class, () -> politica.consultarAportePropio(DIRECTOR, contexto, instante(110)));
    }

    @Test
    void retiroNoAceptaUnContextoParcialQueOculteLaFuenteDelConflicto() {
        var hecho = denuncia();
        var contexto = ContextoAcceso.deHecho(hecho);
        var ledger = politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(contexto, instante(100));
        var parcial = new ContextoAcceso(new AmbitoReserva(Set.of(hecho.id()), Set.of(), Set.of()),
                List.of(), Optional.empty(), Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> politica(ledger).retirarExclusion(
                ledger.exclusiones().getFirst().id(), parcial, motivo(COMITE, 110)));
        assertTrue(ledger.estaExcluido(DIRECTOR, contexto.ambito()));
    }

    @Test
    void unaNuevaFuenteDeDenunciaTrasElRetiroActivaOtraRestriccion() {
        var hecho = denuncia();
        var contexto = ContextoAcceso.deHecho(hecho);
        var ledger = politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(contexto, instante(100));
        var retiradas = politica(ledger).retirarExclusion(ledger.exclusiones().getFirst().id(), contexto, motivo(COMITE, 110));
        var ampliado = hecho.vincularDuplicado(reporte(101, DOCENTE, Optional.of(DIRECTOR),
                Optional.of(PapelParticipacion.PRESUNTA_RESPONSABILIDAD)), cambio(TOE, 120));
        var nuevas = politica(retiradas).restriccionesActualizadas(ContextoAcceso.deHecho(ampliado), instante(130));
        assertEquals(2, nuevas.exclusiones().size());
        assertEquals(1, nuevas.exclusiones().stream().filter(RestriccionesAmbito.Exclusion::activa).count());
        assertTrue(nuevas.estaExcluido(DIRECTOR, AmbitoReserva.deHecho(ampliado)));
        assertFalse(nuevas.exclusiones().getFirst().activa());
    }

    @Test
    void nuevaAtribucionEnOtraRevisionTrasUnPapelNoDenunciadoEsUnNuevoMotivoPreventivo() {
        var hecho = denuncia();
        var contexto = ContextoAcceso.deHecho(hecho);
        var ledger = politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(contexto, instante(100));
        var retiradas = politica(ledger).retirarExclusion(ledger.exclusiones().getFirst().id(), contexto, motivo(COMITE, 110));
        var testigo = hecho.principal().corregirInstitucionalmente(datos(Optional.of(DIRECTOR),
                Optional.of(PapelParticipacion.TESTIGO)), motivo(COMITE, 120));
        var denunciadoDeNuevo = testigo.corregirInstitucionalmente(datos(Optional.of(DIRECTOR),
                Optional.of(PapelParticipacion.PRESUNTA_RESPONSABILIDAD)), motivo(COMITE, 130));
        var contextoNuevo = ContextoAcceso.deHecho(hecho.actualizarReporte(denunciadoDeNuevo));
        var nuevas = politica(retiradas).restriccionesActualizadas(contextoNuevo, instante(140));
        assertEquals(2, nuevas.exclusiones().size());
        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO,
                politica(nuevas).evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO, contextoNuevo, instante(150)).resultado());
    }

    @Test
    void otroHechoDelMismoEstudianteContinuaConsultableSinRevelarLaDenunciaExcluida() {
        var denuncia = denuncia();
        var otro = Hecho.de(reporte(101, DOCENTE, Optional.empty(), Optional.empty()));
        var ledger = politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(ContextoAcceso.deHecho(denuncia), instante(100));
        var politica = politica(ledger);
        assertTrue(politica.evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO, ContextoAcceso.deHecho(otro), instante(110)).permitida());
        assertEquals(List.of(otro), politica.filtrarHechosConsultables(DIRECTOR, List.of(denuncia, otro), instante(110)));
        assertEquals(ESTUDIANTE, denuncia.principal().datos().participaciones().getFirst().persona().id());
        assertEquals(ESTUDIANTE, otro.principal().datos().participaciones().getFirst().persona().id());
    }

    @Test
    void aclaracionDeTestigoAPresuntoActivaRestriccionYDescartePosteriorNoLaRevoca() {
        var original = Hecho.de(reporte(100, DOCENTE, Optional.of(DIRECTOR), Optional.of(PapelParticipacion.TESTIGO)));
        var aclarado = original.aclararParticipacion(PERSONAL, PapelParticipacion.PRESUNTA_RESPONSABILIDAD,
                "Atribución institucional ficticia por confirmar", motivo(COMITE, 110));
        var contexto = ContextoAcceso.deHecho(aclarado);
        var ledger = politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(contexto, instante(120));
        assertTrue(ledger.estaExcluido(DIRECTOR, contexto.ambito()));
        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO,
                politica(ledger).evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO, contexto, instante(120)).resultado());
        var descartado = aclarado.aclararParticipacion(PERSONAL, PapelParticipacion.ATRIBUCION_DESCARTADA,
                "Atribución descartada después", motivo(COMITE, 130));
        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO, politica(ledger).evaluar(DIRECTOR,
                Permiso.CONSULTAR_HECHO, ContextoAcceso.deHecho(descartado), instante(140)).resultado());
    }

    @Test
    void reiterarLaMismaAclaracionNoReviveUnaExclusionRetiradaPorDecisionMotivada() {
        var testigo = Hecho.de(reporte(100, DOCENTE, Optional.of(DIRECTOR), Optional.of(PapelParticipacion.TESTIGO)));
        var presunto = testigo.aclararParticipacion(PERSONAL, PapelParticipacion.PRESUNTA_RESPONSABILIDAD,
                "Aclaración institucional inicial", motivo(COMITE, 110));
        var contexto = ContextoAcceso.deHecho(presunto);
        var ledger = politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(contexto, instante(120));
        var retiradas = politica(ledger).retirarExclusion(ledger.exclusiones().getFirst().id(), contexto, motivo(COMITE, 130));
        var reiterado = presunto.aclararParticipacion(PERSONAL, PapelParticipacion.PRESUNTA_RESPONSABILIDAD,
                "Se mantiene la misma atribución, sin una nueva denuncia", motivo(COMITE, 140));
        var resultado = politica(retiradas).restriccionesActualizadas(ContextoAcceso.deHecho(reiterado), instante(150));
        assertEquals(1, resultado.exclusiones().size());
        assertFalse(resultado.estaExcluido(DIRECTOR, AmbitoReserva.deHecho(reiterado)));
        assertTrue(politica(resultado).evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO,
                ContextoAcceso.deHecho(reiterado), instante(160)).permitida());
    }

    @Test
    void unaNuevaAclaracionDenunciadaTrasRectificacionYRetiroEsOtroEpisodio() {
        var original = denuncia();
        var contexto = ContextoAcceso.deHecho(original);
        var ledger = politica(RestriccionesAmbito.vacias()).restriccionesActualizadas(contexto, instante(100));
        var rectificado = original.actualizarReporte(original.principal().corregirInstitucionalmente(
                datos(Optional.of(DIRECTOR), Optional.of(PapelParticipacion.TESTIGO)), motivo(COMITE, 110)));
        var retiradas = politica(ledger).retirarExclusion(ledger.exclusiones().getFirst().id(),
                ContextoAcceso.deHecho(rectificado), motivo(COMITE, 120));
        var nuevo = rectificado.aclararParticipacion(PERSONAL, PapelParticipacion.PRESUNTA_RESPONSABILIDAD,
                "Nueva atribución posterior que requiere protección", motivo(COMITE, 130));
        var actuales = politica(retiradas).restriccionesActualizadas(ContextoAcceso.deHecho(nuevo), instante(140));
        assertEquals(2, actuales.exclusiones().size());
        assertEquals(1, actuales.exclusiones().stream().filter(RestriccionesAmbito.Exclusion::activa).count());
        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO, politica(actuales).evaluar(DIRECTOR,
                Permiso.CONSULTAR_HECHO, ContextoAcceso.deHecho(nuevo), instante(140)).resultado());
    }

    @Test
    void noPuedeEvaluarseUnContextoConDecisionesDeHechoPosterioresAlInstanteConfiable() {
        var base = denuncia();
        var vinculado = base.vincularDuplicado(reporte(101, DOCENTE, Optional.empty(), Optional.empty()), cambio(COMITE, 120));
        var precisado = base.precisarDatos(base.principal().datos().fechaHecho(),
                base.principal().datos().clasificacion(), motivo(COMITE, 120));
        var cambiado = base.vincularDuplicado(reporte(101, DOCENTE, Optional.empty(), Optional.empty()), cambio(COMITE, 30))
                .cambiarPrincipal(id(101), motivo(COMITE, 120));
        for (var futuro : List.of(vinculado, precisado, cambiado))
            assertThrows(IllegalArgumentException.class, () -> politica(RestriccionesAmbito.vacias())
                    .restriccionesActualizadas(ContextoAcceso.deHecho(futuro), instante(110)));
    }

    private Hecho denuncia() {
        return Hecho.de(reporte(100, DOCENTE, Optional.of(DIRECTOR), Optional.of(PapelParticipacion.PRESUNTA_RESPONSABILIDAD)));
    }
    private PoliticaAcceso politica(RestriccionesAmbito ledger) { return new PoliticaAcceso(directorio, ledger); }
    private static DirectorioUsuarios directorio() {
        var directorio = DirectorioUsuarios.inicializar(ADMIN, "Administrador ficticio", "admin-ficticio",
                new RegistroCambio(id(99), INICIO, Optional.of("Designación institucional ficticia")));
        long designacion = 200;
        for (var usuario : List.of(DIRECTOR, TOE, COMITE, DOCENTE))
            directorio = directorio.registrarUsuario(usuario, "Usuario ficticio " + usuario, "cuenta-" + usuario,
                    cambio(ADMIN, 0));
        var vigencia = new Vigencia(INICIO, Optional.empty());
        directorio = directorio.asignarRol(DIRECTOR, id(designacion++), RolUsuario.DIRECTOR, vigencia, cambio(ADMIN, 0));
        directorio = directorio.asignarRol(DIRECTOR, id(designacion++), RolUsuario.DOCENTE, vigencia, cambio(ADMIN, 0));
        directorio = directorio.asignarRol(DIRECTOR, id(designacion++), RolUsuario.ADMINISTRADOR_OPERATIVO, vigencia, cambio(ADMIN, 0));
        directorio = directorio.asignarRol(TOE, id(designacion++), RolUsuario.TOE, vigencia, cambio(ADMIN, 0));
        directorio = directorio.asignarRol(COMITE, id(designacion++), RolUsuario.MIEMBRO_COMITE, vigencia, cambio(ADMIN, 0));
        return directorio.asignarRol(DOCENTE, id(designacion), RolUsuario.DOCENTE, vigencia, cambio(ADMIN, 0));
    }
    private static ReporteIncidencia reporte(int numero, UUID autor, Optional<UUID> cuenta,
                                            Optional<PapelParticipacion> papelPersonal) {
        return ReporteIncidencia.registrar(id(numero), datos(cuenta, papelPersonal), autor, instante(10));
    }
    private static DatosReporte datos(Optional<UUID> cuenta, Optional<PapelParticipacion> papelPersonal) {
        var estudiante = new Participacion(PersonaInvolucrada.estudiante(ESTUDIANTE, "Estudiante ficticio"),
                PapelParticipacion.AFECTADO, Optional.empty());
        var participaciones = papelPersonal.<List<Participacion>>map(papel -> List.of(estudiante,
                new Participacion(new PersonaInvolucrada(PERSONAL, TipoPersona.PERSONAL, "Trabajador ficticio",
                        cuenta, Optional.of("Función laboral ficticia")), papel, Optional.empty()))).orElseGet(() -> List.of(estudiante));
        var clasificacion = new ClasificacionCategoria(id(30), "Protección ficticia", NaturalezaCategoria.VIOLENCIA_PROTECCION,
                Gravedad.GRAVE, true);
        return new DatosReporte(FechaHecho.exacta(LocalDate.of(2026, 10, 20)), 2026, clasificacion,
                "Relato ficticio de un hecho comunicado", participaciones, false,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    private static RegistroCambio cambio(UUID autor, int segundos) {
        return new RegistroCambio(autor, instante(segundos), Optional.empty());
    }
    private static RegistroCambio motivo(UUID autor, int segundos) {
        return new RegistroCambio(autor, instante(segundos), Optional.of("Decisión institucional ficticia motivada"));
    }
    private static Instant instante(int segundos) { return INICIO.plusSeconds(segundos); }
    private static UUID id(long numero) { return new UUID(0, numero); }
}
