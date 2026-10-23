package pe.edu.incidencias.domain.documento;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.alerta.BandejaAtencion;
import pe.edu.incidencias.domain.alerta.RegistroAlertas;
import pe.edu.incidencias.domain.atencion.RegistroAtenciones;
import pe.edu.incidencias.domain.atencion.Actuacion;
import pe.edu.incidencias.domain.atencion.Compromiso;
import pe.edu.incidencias.domain.incidencia.*;
import pe.edu.incidencias.domain.proteccion.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.documento.DatosPruebaDocumentos.*;

class BloqueoJuridicoIntegracionTest {
    private ControlProteccion bloquear(AlcanceProteccion alcance) {
        var a = new AutorizacionProteccion(id(600), ADMIN, Set.of(AutorizacionProteccion.Operacion.BLOQUEAR), alcance,
                new Vigencia(INICIO, Optional.empty()), "Responsable institucional", "Constancia externa",
                "Limitación temporal expresamente decidida", INICIO);
        return ControlProteccion.vacio().bloquear(id(601), alcance, a, motivo(ADMIN, 130), directorio());
    }
    @ParameterizedTest @EnumSource(value = Permiso.class, mode = EnumSource.Mode.EXCLUDE,
            names = {"BUSCAR_ESTUDIANTE", "REGISTRAR_INCIDENCIA", "IMPORTAR_NOMINA", "ADMINISTRAR_USUARIOS", "DEFINIR_CONFIGURACION"})
    void todoPermisoConAmbitoRespetaBloqueoAntesDeEvaluarRol(Permiso permiso) {
        var p = new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias(), bloquear(AlcanceProteccion.persona(ESTUDIANTE)));
        assertEquals(PoliticaAcceso.Resultado.BLOQUEO_JURIDICO, p.evaluar(TOE, permiso,
                ContextoAcceso.deAporte(hecho(), hecho().principal().id()), reloj(140)).resultado());
    }
    @Test void bloqueoPosteriorRevocaInformeEmitidoSinReescribirlo() {
        var registro = abierto();
        var g = generador(registro);
        var emitido = g.emitir(vista(g), id(900), cambio(TOE, 125));
        assertSame(emitido, g.consultarEmitido(emitido, TOE, reloj(126)));
        var actual = generador(registro.conProteccion(bloquear(AlcanceProteccion.persona(ESTUDIANTE))));
        assertThrows(SecurityException.class, () -> actual.consultarEmitido(emitido, TOE, reloj(140)));
        assertEquals(reloj(120), emitido.corte());
    }
    @Test void bloqueoDeRecursoInformeTambienRevocaDescarga() {
        var registro = abierto(); var g = generador(registro);
        var emitido = g.emitir(vista(g), id(900), cambio(TOE, 125));
        var control = bloquear(new AlcanceProteccion(Optional.empty(), Set.of(), Set.of(id(900))));
        assertThrows(SecurityException.class, () -> generador(registro.conProteccion(control)).consultarEmitido(emitido, TOE, reloj(140)));
        assertNotNull(registro.conProteccion(control).consultarCaso(CASO, TOE, directorio(), reloj(140)));
    }
    @Test void bloqueoDeArchivoRevocaActaPorEnlaceDirecto() {
        var registro = abierto();
        var actas = RegistroActas.vacio().registrar(acta(), registro, directorio(), Optional.empty());
        assertEquals(id(301), actas.archivoParaDescarga(id(300), id(301), TOE, reloj(120), registro, directorio()).id());
        var control = bloquear(new AlcanceProteccion(Optional.empty(), Set.of(), Set.of(id(301))));
        assertThrows(SecurityException.class, () -> actas.archivoParaDescarga(id(300), id(301), TOE,
                reloj(140), registro.conProteccion(control), directorio()));
    }
    @ParameterizedTest @ValueSource(longs = {300, 301, 500})
    void informeConservadoRevalidaActasArchivosYReferenciasQueProyecta(long recurso) {
        var registro = abierto();
        var actas = RegistroActas.vacio().registrar(acta(), registro, directorio(), Optional.empty());
        var referencia = new ReferenciaOficial(id(500), registro.contexto(CASO).ambito(), InstrumentoOficial.SISEVE,
                "Origen comunicado", Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of("Referencia reservada"), cambio(TOE, 110));
        var referencias = ReferenciasOficiales.vacias().registrar(referencia, registro.contexto(CASO), directorio(), registro.restricciones());
        var generador = new GeneradorInformeCaso(registro, actas, referencias, directorio(), Map.of());
        var vista = vista(generador);
        var emitido = generador.emitir(vista, id(900), cambio(TOE, 125));
        var control = bloquear(new AlcanceProteccion(Optional.empty(), Set.of(), Set.of(id(recurso))));
        var actual = new GeneradorInformeCaso(registro.conProteccion(control), actas, referencias, directorio(), Map.of());
        assertThrows(SecurityException.class, () -> actual.emitir(vista, id(901), cambio(TOE, 140)));
        assertThrows(SecurityException.class, () -> actual.consultarEmitido(emitido, TOE, reloj(140)));
        assertEquals(1, emitido.actas().size());
        assertEquals(1, emitido.referencias().size());
        assertNotNull(registro.conProteccion(control).consultarCaso(CASO, TOE, directorio(), reloj(140)));
    }
    @Test void personaRetiradaDeVersionActualSigueProtegidaEnRevisionAnterior() {
        var original = reporte(100, estudiante(), PapelParticipacion.AFECTADO, HOY, "Comunicación original");
        var datos = new DatosReporte(FechaHecho.exacta(HOY), 2026, clasificacion(), "Corrección institucional",
                List.of(new Participacion(otro(), PapelParticipacion.AFECTADO, Optional.empty())), false,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        var corregido = original.corregirInstitucionalmente(datos, motivo(TOE, 120));
        var contexto = ContextoAcceso.deHecho(Hecho.de(corregido));
        var p = new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias(), bloquear(AlcanceProteccion.persona(ESTUDIANTE)));
        assertEquals(PoliticaAcceso.Resultado.BLOQUEO_JURIDICO, p.evaluar(TOE, Permiso.CONSULTAR_HECHO, contexto, reloj(140)).resultado());
    }
    @Test void actualizacionValidaNoPierdeControlDeOtroAmbito() {
        var control = bloquear(AlcanceProteccion.persona(OTRO));
        var registro = abierto().conProteccion(control);
        var actualizado = registro.reasignarResponsable(CASO, DIRECTOR, directorio(), cambio(TOE, 140));
        assertEquals(control.bloqueos(), actualizado.proteccion().bloqueos());
        assertFalse(actualizado.proteccion().permitePersona(OTRO, reloj(150)));
    }
    @Test void consultaDeCasoBajoBloqueoImpideTambienActuaciones() {
        var r = abierto().conProteccion(bloquear(AlcanceProteccion.deAmbito(new AmbitoReserva(Set.of(), Set.of(), Set.of(CASO)))));
        assertThrows(SecurityException.class, () -> r.consultarCaso(CASO, TOE, directorio(), reloj(140)));
        assertThrows(SecurityException.class, () -> r.actuacionesDelCaso(CASO, TOE, directorio(), reloj(140)));
    }
    @Test void busquedaYDenominadorSeFiltranSinConcederPermisoInstitucionalAlAdministrador() {
        var p = new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias(), bloquear(AlcanceProteccion.persona(ESTUDIANTE)));
        assertFalse(p.puedeConsultarPersona(ESTUDIANTE, reloj(140)));
        assertEquals(PoliticaAcceso.Resultado.BLOQUEO_JURIDICO,
                p.evaluarPersona(TOE, Permiso.CONSULTAR_ESTADISTICAS, ESTUDIANTE, reloj(140)).resultado());
        assertFalse(p.evaluarPersona(ADMIN, Permiso.CONSULTAR_ESTADISTICAS, OTRO, reloj(140)).permitida());
    }
    @Test void recursoCompromisoBloqueadoProtegeCasoBandejaYDerivados() {
        var compromiso = Compromiso.acordar(id(401), "Acuerdo pertinente", estudiante(), Optional.empty(), true, cambio(TOE, 110));
        var accion = Actuacion.realizada(id(400), "Conversación", HOY, TOE, Set.of(), Set.of(), List.of(compromiso), cambio(TOE, 110));
        var registro = abierto().registrarActuacion(CASO, accion, directorio(), cambio(TOE, 110));
        var control = bloquear(new AlcanceProteccion(Optional.empty(), Set.of(), Set.of(id(401))));
        var actual = registro.conProteccion(control);
        assertFalse(actual.casoDisponible(CASO, reloj(140)));
        assertThrows(SecurityException.class, () -> actual.consultarCaso(CASO, TOE, directorio(), reloj(140)));
        assertTrue(BandejaAtencion.proyectar(TOE, actual, RegistroAlertas.vacio(), directorio(), reloj(140)).isEmpty());
    }
    @Test void propuestaNoPuedeIntroducirPersonaBloqueadaEnHechoOCasoAntesDisponible() {
        var registro = abierto().conProteccion(bloquear(AlcanceProteccion.persona(OTRO)));
        var h = registro.hechoVigente(id(100)).vincularDuplicado(reporte(101, otro(), PapelParticipacion.TESTIGO, HOY,
                "Nuevo dato sobre otra persona"), cambio(TOE, 140));
        assertThrows(SecurityException.class, () -> registro.actualizarHecho(h, directorio(), cambio(TOE, 140)));
        var compromiso = Compromiso.acordar(id(401), "Acuerdo pertinente", otro(), Optional.empty(), true, cambio(TOE, 140));
        var accion = Actuacion.realizada(id(400), "Conversación", HOY, TOE, Set.of(), Set.of(), List.of(compromiso), cambio(TOE, 140));
        assertThrows(SecurityException.class, () -> registro.registrarActuacion(CASO, accion, directorio(), cambio(TOE, 140)));
        assertTrue(registro.consultarCaso(CASO, TOE, directorio(), reloj(150)).actuaciones().isEmpty());
    }
}
