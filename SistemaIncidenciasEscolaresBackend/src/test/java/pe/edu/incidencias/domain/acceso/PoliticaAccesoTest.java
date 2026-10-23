package pe.edu.incidencias.domain.acceso;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

class PoliticaAccesoTest {
    private static final Instant INICIO = Instant.parse("2026-10-21T15:00:00Z");
    private static final Instant AHORA = INICIO.plusSeconds(100);
    private static final UUID ADMIN = id(1);
    private static final UUID SUJETO = id(2);
    private static final UUID DIRECTOR = id(3);
    private static final UUID MIEMBRO = id(4);
    private static final UUID DOCENTE = id(5);
    private static final UUID AIP = id(6);
    private static final UUID OTRO_TOE = id(7);
    private static final UUID ESTUDIANTE = id(50);

    @ParameterizedTest
    @EnumSource(RolUsuario.class)
    void matrizBasicaPorRolNoConfundeAdministracionOImportacionConAtencion(RolUsuario rol) {
        var politica = politica(conRol(rol));
        var contexto = ContextoAcceso.deHecho(hecho(SUJETO));
        boolean institucional = Set.of(RolUsuario.TOE, RolUsuario.MIEMBRO_COMITE, RolUsuario.DIRECTOR).contains(rol);
        boolean registrador = institucional || Set.of(RolUsuario.DOCENTE, RolUsuario.AUXILIAR, RolUsuario.DIRECTIVO).contains(rol);

        assertEquals(registrador || rol == RolUsuario.AIP,
                politica.evaluar(SUJETO, Permiso.BUSCAR_ESTUDIANTE, AHORA).permitida());
        assertEquals(registrador, politica.evaluar(SUJETO, Permiso.REGISTRAR_INCIDENCIA, AHORA).permitida());
        assertEquals(rol == RolUsuario.AIP, politica.evaluar(SUJETO, Permiso.IMPORTAR_NOMINA, AHORA).permitida());
        assertEquals(rol == RolUsuario.ADMINISTRADOR_OPERATIVO,
                politica.evaluar(SUJETO, Permiso.ADMINISTRAR_USUARIOS, AHORA).permitida());
        assertEquals(rol == RolUsuario.TOE || rol == RolUsuario.DIRECTOR,
                politica.evaluar(SUJETO, Permiso.DEFINIR_CONFIGURACION, AHORA).permitida());

        for (var permiso : List.of(Permiso.CONSULTAR_HECHO, Permiso.ATENDER, Permiso.ACLARAR_PARTICIPACION,
                Permiso.CORREGIR_HECHO, Permiso.RELACIONAR_REPORTES, Permiso.CONSULTAR_CONTEXTO_INSTITUCIONAL,
                Permiso.CONSULTAR_DOCUMENTO_RESERVADO, Permiso.GENERAR_INFORME, Permiso.CONSULTAR_ESTADISTICAS,
                Permiso.EXPORTAR_ATENCION, Permiso.EXCLUIR_USUARIO, Permiso.RETIRAR_EXCLUSION))
            assertEquals(institucional, politica.evaluar(SUJETO, permiso, contexto, AHORA).permitida(), permiso.name());
    }

    @ParameterizedTest
    @EnumSource(value = RolUsuario.class, names = {"DOCENTE", "AUXILIAR", "DIRECTIVO", "TOE", "MIEMBRO_COMITE", "DIRECTOR"})
    void registradoresConsultanYRectificanSuAporteSinConcederloAlAutorAjeno(RolUsuario rol) {
        var politica = politica(conRol(rol));
        var hecho = hecho(SUJETO);
        var propio = ContextoAcceso.deAporte(hecho, hecho.principal().id());
        var ajeno = ContextoAcceso.deAporte(hecho(DOCENTE), id(100));

        for (var permiso : List.of(Permiso.CONSULTAR_APORTE_PROPIO, Permiso.CORREGIR_APORTE_PROPIO,
                Permiso.ACLARAR_APORTE_PROPIO, Permiso.REGISTRAR_ACCION_INMEDIATA)) {
            assertTrue(politica.evaluar(SUJETO, permiso, propio, AHORA).permitida(), permiso.name());
            assertEquals(PoliticaAcceso.Resultado.APORTE_AJENO,
                    politica.evaluar(SUJETO, permiso, ajeno, AHORA).resultado(), permiso.name());
        }
    }

    @Test
    void autorOrdinarioNoRecibeHechoInstitucionalNiAportesDelOtroReportante() {
        var politica = politica(completo());
        var principal = reporte(100, DOCENTE, "Relato propio ficticio", PapelParticipacion.TESTIGO);
        var ajeno = reporte(101, DIRECTOR, "Relato ajeno reservado ficticio", PapelParticipacion.PRESUNTA_RESPONSABILIDAD);
        var consolidado = Hecho.de(principal).vincularDuplicado(ajeno, cambio(DIRECTOR, 30));
        var propio = ContextoAcceso.deAporte(consolidado, principal.id());

        assertTrue(politica.evaluar(DOCENTE, Permiso.CONSULTAR_APORTE_PROPIO, propio, AHORA).permitida());
        assertFalse(politica.evaluar(DOCENTE, Permiso.CONSULTAR_HECHO, propio, AHORA).permitida());
        var vista = politica.consultarAportePropio(DOCENTE, propio, AHORA);
        assertEquals(principal.id(), vista.reporteId());
        assertEquals(1, vista.revisionesPropias().size());
        assertEquals("Relato propio ficticio", vista.revisionesPropias().getFirst().datos().descripcion());
        assertThrows(SecurityException.class, () -> politica.consultarAportePropio(DOCENTE,
                ContextoAcceso.deAporte(consolidado, ajeno.id()), AHORA));
        assertTrue(politica.filtrarHechosConsultables(DOCENTE, List.of(consolidado), AHORA).isEmpty());
        assertEquals(List.of(consolidado), politica.filtrarHechosConsultables(MIEMBRO, List.of(consolidado), AHORA));
        assertThrows(UnsupportedOperationException.class, () -> vista.revisionesPropias().clear());
    }

    @Test
    void aportePropioNoFiltraCambiosInstitucionalesNiDevuelveSuContenidoReservado() {
        var politica = politica(completo());
        var original = reporte(100, DOCENTE, "Comunicación inicial ficticia", PapelParticipacion.TESTIGO);
        var atendido = original.corregirInstitucionalmente(datos("Rectificación institucional reservada",
                PapelParticipacion.AFECTADO), cambio(DIRECTOR, 30));
        var contexto = ContextoAcceso.deAporte(Hecho.de(atendido), original.id());
        var vista = politica.consultarAportePropio(DOCENTE, contexto, AHORA);

        assertTrue(vista.relacionadoParaAtencion());
        assertEquals(original.revisiones(), vista.revisionesPropias());
        assertEquals(PoliticaAcceso.Resultado.CORRECCION_TRAS_ATENCION,
                politica.evaluar(DOCENTE, Permiso.CORREGIR_APORTE_PROPIO, contexto, AHORA).resultado());
        assertEquals(PoliticaAcceso.Resultado.CORRECCION_TRAS_ATENCION,
                politica.evaluar(DOCENTE, Permiso.REGISTRAR_ACCION_INMEDIATA, contexto, AHORA).resultado());
        assertTrue(politica.evaluar(DOCENTE, Permiso.ACLARAR_APORTE_PROPIO, contexto, AHORA).permitida());
        assertEquals(2, atendido.revisiones().size());
    }

    @ParameterizedTest
    @EnumSource(value = RolUsuario.class, names = {"AIP", "ADMINISTRADOR_OPERATIVO"})
    void niImportadorNiAdministradorObtienenUnCasoPorSerAutorHistorico(RolUsuario rol) {
        var politica = politica(conRol(rol));
        var hecho = hecho(SUJETO);
        var contexto = ContextoAcceso.deAporte(hecho, hecho.principal().id());
        assertFalse(politica.evaluar(SUJETO, Permiso.CONSULTAR_APORTE_PROPIO, contexto, AHORA).permitida());
        assertFalse(politica.evaluar(SUJETO, Permiso.CONSULTAR_HECHO, contexto, AHORA).permitida());
        assertFalse(politica.evaluar(SUJETO, Permiso.ATENDER, contexto, AHORA).permitida());
        assertTrue(politica.filtrarHechosConsultables(SUJETO, List.of(hecho), AHORA).isEmpty());
    }

    @Test
    void documentarAccionUrgentePropiaNoEsperaAResolverLaClasificacion() {
        var politica = politica(completo());
        var propio = reporte(100, DOCENTE, "Nota cotidiana ficticia", PapelParticipacion.TESTIGO);
        var datos = datos("Aporte de protección ficticio", PapelParticipacion.PRESUNTA_RESPONSABILIDAD);
        var grave = new DatosReporte(datos.fechaHecho(), datos.anioEscolar(),
                new ClasificacionCategoria(id(52), "Protección ficticia", NaturalezaCategoria.VIOLENCIA_PROTECCION,
                        Gravedad.GRAVE, true), datos.descripcion(), datos.participaciones(), false,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        var ajeno = ReporteIncidencia.registrar(id(101), grave, DIRECTOR, INICIO.plusSeconds(20));
        var hecho = Hecho.de(propio).vincularDuplicado(ajeno, cambio(DIRECTOR, 30));
        var contexto = ContextoAcceso.deAporte(hecho, propio.id());

        assertTrue(politica.evaluar(DOCENTE, Permiso.REGISTRAR_ACCION_INMEDIATA, contexto, AHORA).permitida());
        assertTrue(politica.evaluar(DOCENTE, Permiso.CONSULTAR_APORTE_PROPIO, contexto, AHORA).permitida());
        var precisado = hecho.precisarDatos(FechaHecho.exacta(LocalDate.of(2026, 10, 20)), grave.clasificacion(),
                motivo(DIRECTOR, 40));
        var vigente = ContextoAcceso.deAporte(precisado, propio.id());
        assertEquals(PoliticaAcceso.Resultado.CORRECCION_TRAS_ATENCION,
                politica.evaluar(DOCENTE, Permiso.REGISTRAR_ACCION_INMEDIATA, vigente, AHORA).resultado());
        assertTrue(politica.evaluar(DOCENTE, Permiso.ACLARAR_APORTE_PROPIO, vigente, AHORA).permitida());
    }

    @Test
    void membresiaVencidaRetiraPermisoInstitucionalSinBorrarElRolDocente() {
        var fin = AHORA.plusSeconds(1);
        var directorio = conRol(RolUsuario.DOCENTE).asignarRol(SUJETO, id(91), RolUsuario.MIEMBRO_COMITE,
                new Vigencia(INICIO, Optional.of(fin)), cambio(ADMIN, 10));
        var politica = politica(directorio);
        var contexto = ContextoAcceso.deHecho(hecho(SUJETO));
        assertTrue(politica.evaluar(SUJETO, Permiso.ATENDER, contexto, AHORA).permitida());
        assertFalse(politica.evaluar(SUJETO, Permiso.ATENDER, contexto, fin).permitida());
        assertTrue(politica.evaluar(SUJETO, Permiso.REGISTRAR_INCIDENCIA, fin).permitida());
        assertEquals(2, directorio.usuario(SUJETO).orElseThrow().designaciones().size());
    }

    @Test
    void docenteMiembroYAdministradorAcumulaCapacidadesSinObtenerConfiguracionPorAdministrar() {
        var directorio = conRol(RolUsuario.DOCENTE)
                .asignarRol(SUJETO, id(91), RolUsuario.MIEMBRO_COMITE, sinFin(), cambio(ADMIN, 10))
                .asignarRol(SUJETO, id(92), RolUsuario.ADMINISTRADOR_OPERATIVO, sinFin(), cambio(ADMIN, 11));
        var politica = politica(directorio);
        var contexto = ContextoAcceso.deHecho(hecho(SUJETO));
        assertTrue(politica.evaluar(SUJETO, Permiso.CONSULTAR_HECHO, contexto, AHORA).permitida());
        assertTrue(politica.evaluar(SUJETO, Permiso.ADMINISTRAR_USUARIOS, AHORA).permitida());
        assertTrue(politica.evaluar(SUJETO, Permiso.REGISTRAR_INCIDENCIA, AHORA).permitida());
        assertFalse(politica.evaluar(SUJETO, Permiso.DEFINIR_CONFIGURACION, AHORA).permitida());
        assertEquals(3, directorio.usuario(SUJETO).orElseThrow().rolesVigentes(AHORA).size());
    }

    @Test
    void bajaImpideLaSiguienteLecturaSinBorrarLaDesignacion() {
        var directorio = conRol(RolUsuario.MIEMBRO_COMITE).desactivarUsuario(SUJETO, cambio(ADMIN, 30));
        var politica = politica(directorio);
        var contexto = ContextoAcceso.deHecho(hecho(SUJETO));
        assertEquals(PoliticaAcceso.Resultado.CUENTA_NO_VIGENTE,
                politica.evaluar(SUJETO, Permiso.CONSULTAR_HECHO, contexto, AHORA).resultado());
        assertEquals(PoliticaAcceso.Resultado.CUENTA_NO_VIGENTE,
                politica.evaluar(SUJETO, Permiso.REGISTRAR_INCIDENCIA, AHORA).resultado());
        assertEquals(1, directorio.usuario(SUJETO).orElseThrow().designaciones().size());
    }

    @Test
    void permisosProtegidosRequierenAmbitoInclusoParaElDirector() {
        var politica = politica(completo());
        for (var permiso : Permiso.values()) if (permiso.requiereAmbito())
            assertEquals(PoliticaAcceso.Resultado.FALTA_AMBITO,
                    politica.evaluar(DIRECTOR, permiso, AHORA).resultado(), permiso.name());
        assertEquals(PoliticaAcceso.Resultado.CUENTA_NO_VIGENTE,
                politica.evaluar(id(999), Permiso.REGISTRAR_INCIDENCIA, AHORA).resultado());
    }

    @Test
    void convocatoriaOrdinariaCorrespondeAToeSinAprobacionDelDirector() {
        var politica = politica(completo());
        var contexto = ContextoAcceso.deHecho(hecho(DOCENTE));
        assertTrue(politica.evaluar(SUJETO, Permiso.CONVOCAR_COMITE, contexto, AHORA).permitida());
        assertFalse(politica.evaluar(SUJETO, Permiso.CONVOCAR_COMITE, contexto, AHORA).porSustitucion());
        assertFalse(politica.evaluar(MIEMBRO, Permiso.CONVOCAR_COMITE, contexto, AHORA).permitida());
        assertFalse(politica.evaluar(DIRECTOR, Permiso.CONVOCAR_COMITE, contexto, AHORA).permitida());
        assertFalse(politica.evaluar(MIEMBRO, Permiso.CONVOCAR_COMITE,
                contexto.conMotivoSustitucion("Motivo no habilitante"), AHORA).permitida());
    }

    @Test
    void sustitucionDeToeExigeConflictoDeTodosLosToeVigentesYMotivoReal() {
        var directorio = completo().registrarUsuario(OTRO_TOE, "Otro TOE ficticio", "otro.toe", cambio(ADMIN, 10))
                .asignarRol(OTRO_TOE, id(97), RolUsuario.TOE, sinFin(), cambio(ADMIN, 11));
        var contexto = ContextoAcceso.deHecho(hecho(DOCENTE));
        var politica = politica(directorio);
        var unaExclusion = politica.excluir(id(200), SUJETO, contexto, motivo(DIRECTOR, 100));
        var una = new PoliticaAcceso(directorio, unaExclusion);
        var sustitucion = contexto.conMotivoSustitucion("TOE impedidos por conflicto registrado");
        assertFalse(una.evaluar(MIEMBRO, Permiso.CONVOCAR_COMITE, sustitucion, AHORA).permitida());

        var exclusiones = una.excluir(id(201), OTRO_TOE, contexto, motivo(DIRECTOR, 101));
        var ambos = new PoliticaAcceso(directorio, exclusiones);
        assertEquals(PoliticaAcceso.Resultado.FALTA_MOTIVO_SUSTITUCION,
                ambos.evaluar(MIEMBRO, Permiso.CONVOCAR_COMITE, contexto, AHORA.plusSeconds(1)).resultado());
        var decision = ambos.evaluar(MIEMBRO, Permiso.CONVOCAR_COMITE, sustitucion, AHORA.plusSeconds(1));
        assertTrue(decision.permitida());
        assertTrue(decision.porSustitucion());
        assertEquals(sustitucion.motivoSustitucion(), decision.motivoSustitucion());
        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO,
                ambos.evaluar(SUJETO, Permiso.CONVOCAR_COMITE, sustitucion, AHORA.plusSeconds(1)).resultado());
    }

    @Test
    void serMiembroSinToeDesignadoNoInventaSupuestoDeConflictoParaConvocar() {
        var politica = politica(conRol(RolUsuario.MIEMBRO_COMITE));
        var contexto = ContextoAcceso.deHecho(hecho(SUJETO)).conMotivoSustitucion("No hay TOE designado");
        assertFalse(politica.evaluar(SUJETO, Permiso.CONVOCAR_COMITE, contexto, AHORA).permitida());
        assertTrue(politica.evaluar(SUJETO, Permiso.ATENDER, contexto, AHORA).permitida());
    }

    @Test
    void actosColectivosCorrespondenAlDirectorYNoAlToePorEseSoloRol() {
        var politica = politica(completo());
        var contexto = ContextoAcceso.deHecho(hecho(DOCENTE));
        assertTrue(politica.evaluar(DIRECTOR, Permiso.REGISTRAR_ACTO_COMITE, contexto, AHORA).permitida());
        assertFalse(politica.evaluar(DIRECTOR, Permiso.REGISTRAR_ACTO_COMITE, contexto, AHORA).porSustitucion());
        assertFalse(politica.evaluar(SUJETO, Permiso.REGISTRAR_ACTO_COMITE,
                contexto.conMotivoSustitucion("TOE por su solo rol"), AHORA).permitida());
        assertFalse(politica.evaluar(MIEMBRO, Permiso.REGISTRAR_ACTO_COMITE,
                contexto.conMotivoSustitucion("Director sigue elegible"), AHORA).permitida());
    }

    @Test
    void miembroRestanteRegistraComoSustitutoDelDirectorExcluidoConAutoriaReal() {
        var directorio = completo();
        var contexto = ContextoAcceso.deHecho(hecho(DOCENTE));
        var politica = politica(directorio);
        var restricciones = politica.excluir(id(200), DIRECTOR, contexto, motivo(SUJETO, 100));
        var restringida = new PoliticaAcceso(directorio, restricciones);
        assertEquals(PoliticaAcceso.Resultado.FALTA_MOTIVO_SUSTITUCION,
                restringida.evaluar(MIEMBRO, Permiso.REGISTRAR_ACTO_COMITE, contexto, AHORA).resultado());
        var decision = restringida.evaluar(MIEMBRO, Permiso.REGISTRAR_ACTO_COMITE,
                contexto.conMotivoSustitucion("Director excluido del asunto"), AHORA);
        assertTrue(decision.permitida());
        assertTrue(decision.porSustitucion());
        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO,
                restringida.evaluar(DIRECTOR, Permiso.REGISTRAR_ACTO_COMITE, contexto, AHORA).resultado());
        assertFalse(restringida.evaluar(SUJETO, Permiso.REGISTRAR_ACTO_COMITE,
                contexto.conMotivoSustitucion("TOE no es miembro en este escenario"), AHORA).permitida());
        assertEquals(Set.of(RolUsuario.MIEMBRO_COMITE), directorio.usuario(MIEMBRO).orElseThrow().rolesVigentes(AHORA));
    }

    @Test
    void designacionNoVigenteDelDirectorPermiteSustitucionMotivadaDelMiembro() {
        var directorio = completo().finalizarDesignacion(DIRECTOR, id(93), AHORA, cambio(ADMIN, 100));
        var politica = politica(directorio);
        var contexto = ContextoAcceso.deHecho(hecho(DOCENTE)).conMotivoSustitucion("Terminó la designación del director");
        var decision = politica.evaluar(MIEMBRO, Permiso.REGISTRAR_ACTO_COMITE, contexto, AHORA);
        assertTrue(decision.permitida());
        assertTrue(decision.porSustitucion());
        assertFalse(politica.evaluar(DIRECTOR, Permiso.REGISTRAR_ACTO_COMITE, contexto, AHORA).permitida());
        assertEquals(1, directorio.usuario(DIRECTOR).orElseThrow().designaciones().size());
    }

    @Test
    void destinatariosPrefierenToeElegibleYLuegoComiteSinAsignarlesRecepcionAutomatica() {
        var directorio = completo();
        var contexto = ContextoAcceso.deHecho(hecho(DOCENTE));
        var politica = politica(directorio);
        var ordinarios = politica.destinatariosAtencion(contexto, AHORA);
        assertEquals(List.of(SUJETO), ordinarios.usuarios());
        assertFalse(ordinarios.requiereDesignacion());
        var sinToe = new PoliticaAcceso(directorio,
                politica.excluir(id(200), SUJETO, contexto, motivo(DIRECTOR, 100)));
        var sustitutos = sinToe.destinatariosAtencion(contexto, AHORA);
        assertEquals(Set.of(DIRECTOR, MIEMBRO), Set.copyOf(sustitutos.usuarios()));
        assertFalse(sustitutos.requiereDesignacion());
        assertFalse(sustitutos.usuarios().contains(ADMIN));
        assertFalse(sustitutos.usuarios().contains(AIP));
        assertFalse(sustitutos.usuarios().contains(DOCENTE));
        assertThrows(UnsupportedOperationException.class, () -> sustitutos.usuarios().clear());
    }

    @Test
    void sinOperadorInstitucionalSoloSeSenalaNecesidadDeDesignacion() {
        var politica = politica(conRol(RolUsuario.DOCENTE));
        var destinatarios = politica.destinatariosAtencion(ContextoAcceso.deHecho(hecho(SUJETO)), AHORA);
        assertTrue(destinatarios.usuarios().isEmpty());
        assertTrue(destinatarios.requiereDesignacion());
        assertFalse(politica.evaluar(ADMIN, Permiso.CONSULTAR_HECHO, ContextoAcceso.deHecho(hecho(SUJETO)), AHORA).permitida());
    }

    @Test
    void exclusionExpresaRequiereOtroResponsableNoImplicado() {
        var contexto = ContextoAcceso.deHecho(hecho(SUJETO));
        assertThrows(SecurityException.class, () -> politica(completo()).excluir(id(300), SUJETO,
                contexto, motivo(SUJETO, 100)));
    }

    @Test
    void filtrosConContextoConservanReservaDeCasoAunqueNoExistaDenunciaEnElHecho() {
        var directorio = completo();
        var hecho = hecho(DOCENTE);
        var contexto = new ContextoAcceso(new AmbitoReserva(Set.of(), Set.of(), Set.of(id(400))),
                List.of(hecho), Optional.empty(), Optional.empty());
        var restricciones = politica(directorio).excluir(id(401), SUJETO, contexto, motivo(DIRECTOR, 100));
        var politica = new PoliticaAcceso(directorio, restricciones);
        assertTrue(politica.filtrarContextosConsultables(SUJETO, List.of(contexto), AHORA).isEmpty());
        assertEquals(List.of(contexto), politica.filtrarContextosConsultables(MIEMBRO, List.of(contexto), AHORA));
    }

    private static PoliticaAcceso politica(DirectorioUsuarios directorio) {
        return new PoliticaAcceso(directorio, RestriccionesAmbito.vacias());
    }
    private static DirectorioUsuarios inicial() {
        return DirectorioUsuarios.inicializar(ADMIN, "Administrador ficticio", "admin",
                new RegistroCambio(id(99), INICIO, Optional.of("Designación institucional ficticia")));
    }
    private static DirectorioUsuarios conRol(RolUsuario rol) {
        return inicial().registrarUsuario(SUJETO, "Sujeto ficticio", "sujeto", cambio(ADMIN, 1))
                .asignarRol(SUJETO, id(90), rol, sinFin(), cambio(ADMIN, 2));
    }
    private static DirectorioUsuarios completo() {
        var directorio = conRol(RolUsuario.TOE);
        var ids = List.of(DIRECTOR, MIEMBRO, DOCENTE, AIP);
        var roles = List.of(RolUsuario.DIRECTOR, RolUsuario.MIEMBRO_COMITE, RolUsuario.DOCENTE, RolUsuario.AIP);
        for (int indice = 0; indice < ids.size(); indice++) {
            var cuenta = ids.get(indice);
            directorio = directorio.registrarUsuario(cuenta, "Operador ficticio " + cuenta, "usuario" + (indice + 3), cambio(ADMIN, indice * 2 + 3))
                    .asignarRol(cuenta, id(93 + indice), roles.get(indice), sinFin(), cambio(ADMIN, indice * 2 + 4));
        }
        return directorio;
    }
    private static Hecho hecho(UUID autor) {
        return Hecho.de(reporte(100, autor, "Relato ficticio", PapelParticipacion.TESTIGO));
    }
    private static ReporteIncidencia reporte(int numero, UUID autor, String descripcion, PapelParticipacion papel) {
        return ReporteIncidencia.registrar(id(numero), datos(descripcion, papel), autor, INICIO.plusSeconds(20));
    }
    private static DatosReporte datos(String descripcion, PapelParticipacion papel) {
        return new DatosReporte(FechaHecho.exacta(LocalDate.of(2026, 10, 20)), 2026,
                new ClasificacionCategoria(id(51), "Categoría ficticia", NaturalezaCategoria.CONDUCTA_DISCIPLINARIA,
                        Gravedad.LEVE, false), descripcion,
                List.of(new Participacion(PersonaInvolucrada.estudiante(ESTUDIANTE, "Estudiante ficticio"), papel, Optional.empty())),
                false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    private static RegistroCambio cambio(UUID autor, int segundos) {
        return new RegistroCambio(autor, INICIO.plusSeconds(segundos), Optional.empty());
    }
    private static RegistroCambio motivo(UUID autor, int segundos) {
        return new RegistroCambio(autor, INICIO.plusSeconds(segundos), Optional.of("Conflicto de interés ficticio registrado"));
    }
    private static Vigencia sinFin() { return new Vigencia(INICIO, Optional.empty()); }
    private static UUID id(long numero) { return new UUID(0, numero); }
}
