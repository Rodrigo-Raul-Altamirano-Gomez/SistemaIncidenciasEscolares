package pe.edu.incidencias.domain.alerta;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.catalogo.Gravedad;
import pe.edu.incidencias.domain.incidencia.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.alerta.DatosPruebaAlertas.*;

class BandejaAtencionTest {
    @Test
    void senalNuevaMuestraMotivoGravedadYAntiguedadSinInventarResponsableNiPlazo() {
        var hecho = grave(100);
        var alertas = evaluar(RegistroAlertas.vacio(), hecho, 20);
        var atenciones = RegistroAtenciones.iniciar(List.of(hecho), RestriccionesAmbito.vacias());
        var entrada = BandejaAtencion.proyectar(TOE, atenciones, alertas, directorio(), reloj(30)).getFirst();
        assertTrue(entrada.situacionNueva());
        assertEquals(Set.of(MotivoAlerta.GRAVEDAD), entrada.motivos());
        assertEquals(Set.of(Gravedad.GRAVE), entrada.gravedadesDeclaradas());
        assertEquals(reloj(20), entrada.desde());
        assertTrue(entrada.responsableId().isEmpty());
        assertTrue(entrada.vencimiento().isEmpty());
        assertFalse(entrada.seguimientoPendiente());
        assertFalse(entrada.atencionPendiente());
    }

    @Test
    void anotacionCotidianaSinSenalNoApareceEnLaBandeja() {
        var hecho = leve(100);
        var atenciones = RegistroAtenciones.iniciar(List.of(hecho), RestriccionesAmbito.vacias());
        assertTrue(BandejaAtencion.proyectar(TOE, atenciones, evaluar(RegistroAlertas.vacio(), hecho, 20), directorio(), reloj(30)).isEmpty());
    }

    @Test
    void variosMotivosDelMismoAsuntoSeMuestranEnUnaEntradaDeCaso() {
        var cat = violencia();
        var hechos = List.of(hecho(100, cat, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, true),
                hecho(101, cat, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, false));
        var atenciones = RegistroAtenciones.iniciar(hechos, RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(id(100), id(101)), HOY, TOE, List.of(), Optional.empty(), directorio(), cambio(20));
        var contexto = atenciones.contexto(id(200));
        var alertas = RegistroAlertas.vacio().evaluarHecho(contexto.hechos().stream().filter(h -> h.id().equals(id(100))).findFirst().orElseThrow(), contexto, politica(), cambio(30))
                .evaluarReiteracion(ESTUDIANTE, cat, parametros(), contexto, politica(), cambio(40));
        var entradas = BandejaAtencion.proyectar(TOE, atenciones, alertas, directorio(), reloj(50));
        assertEquals(1, entradas.size());
        assertEquals(Optional.of(id(200)), entradas.getFirst().casoId());
        assertEquals(Set.of(MotivoAlerta.GRAVEDAD, MotivoAlerta.RIESGO_COMUNICADO, MotivoAlerta.REITERACION), entradas.getFirst().motivos());
        assertTrue(entradas.getFirst().atencionPendiente());
        assertEquals(1, entradas.getFirst().alertasIds().size());
    }

    @Test
    void aperturaGestionaAvisoYConservaMotivosEnEntradaUnicaDeAtencionPendiente() {
        var hecho = grave(100);
        var alertas = evaluar(RegistroAlertas.vacio(), hecho, 20);
        var atenciones = RegistroAtenciones.iniciar(List.of(hecho), RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(), Optional.empty(), directorio(), cambio(30));
        var sincronizadas = SeguimientoInstitucional.actualizar(atenciones, alertas, directorio(), cambio(30)).alertas();
        var entradas = BandejaAtencion.proyectar(TOE, atenciones, sincronizadas, directorio(), reloj(40));
        assertEquals(1, entradas.size());
        assertFalse(entradas.getFirst().situacionNueva());
        assertEquals(Set.of(MotivoAlerta.GRAVEDAD), entradas.getFirst().motivos());
        assertEquals(Optional.of(TOE), entradas.getFirst().responsableId());
        assertTrue(entradas.getFirst().atencionPendiente());
    }

    @Test
    void cierreConActuacionesYCompromisosPendientesSigueVisibleConVencimientoRealMasProximo() {
        var hecho = grave(100);
        var acuerdo = Compromiso.acordar(id(400), "Seguimiento concreto", estudiante(), Optional.of(HOY.plusDays(5)), false, cambio(30));
        var realizada = Actuacion.realizada(id(300), "Conversación documentada", HOY, TOE, Set.of(), Set.of(), List.of(acuerdo), cambio(30));
        var prevista = Actuacion.prevista(id(301), "Seguimiento previsto", HOY.plusDays(2), TOE, Set.of(), Set.of(), cambio(30));
        var atenciones = RegistroAtenciones.iniciar(List.of(hecho), RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(realizada, prevista), Optional.of(conclusion(30)), directorio(), cambio(30));
        var alertas = SeguimientoInstitucional.actualizar(atenciones, evaluar(RegistroAlertas.vacio(), hecho, 20), directorio(), cambio(30)).alertas();
        var entrada = BandejaAtencion.proyectar(TOE, atenciones, alertas, directorio(), reloj(40)).getFirst();
        assertFalse(entrada.atencionPendiente());
        assertTrue(entrada.seguimientoPendiente());
        assertEquals(Set.of(id(301)), entrada.actuacionesPrevistas());
        assertEquals(Set.of(id(400)), entrada.compromisosPendientes());
        assertEquals(Optional.of(HOY.plusDays(2)), entrada.vencimiento());
    }

    @Test
    void cierreSinTrabajoPendienteRetiraLaEntradaSinBorrarHistoriaNiAvisos() {
        var hecho = grave(100);
        var atenciones = RegistroAtenciones.iniciar(List.of(hecho), RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(), Optional.of(conclusion(30)), directorio(), cambio(30));
        var alertas = SeguimientoInstitucional.actualizar(atenciones, evaluar(RegistroAlertas.vacio(), hecho, 20), directorio(), cambio(30)).alertas();
        assertTrue(BandejaAtencion.proyectar(TOE, atenciones, alertas, directorio(), reloj(40)).isEmpty());
        assertEquals(1, atenciones.casos().size());
        assertEquals(1, alertas.alertas().size());
        assertEquals(1, alertas.notificaciones().size());
    }

    @Test
    void seguimientoExplicitoSinFechaConservaPendienteConVencimientoDesconocido() {
        var hecho = grave(100);
        var acuerdo = Compromiso.acordar(id(400), "Comprobar evolución del acuerdo", estudiante(), Optional.empty(), true, cambio(30));
        var realizada = Actuacion.realizada(id(300), "Acuerdo documentado", HOY, TOE, Set.of(), Set.of(), List.of(acuerdo), cambio(30));
        var atenciones = RegistroAtenciones.iniciar(List.of(hecho), RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(realizada), Optional.of(conclusion(30)), directorio(), cambio(30));
        var entrada = BandejaAtencion.proyectar(TOE, atenciones, RegistroAlertas.vacio(), directorio(), reloj(40)).getFirst();
        assertEquals(Set.of(id(400)), entrada.compromisosPendientes());
        assertTrue(entrada.vencimiento().isEmpty());
        assertTrue(entrada.actuacionesPrevistas().isEmpty());
    }

    @Test
    void accionInmediataReferenciadaAportaSeguimientoUnaSolaVezTrasCerrar() {
        var hecho = leve(100);
        var acuerdo = Compromiso.acordar(id(400), "Acuerdo inmediato pendiente", estudiante(), Optional.of(HOY.plusDays(1)), false,
                new RegistroCambio(DOCENTE, reloj(20), Optional.empty()));
        var inmediata = Actuacion.realizada(id(300), "Acción inmediata documentada", HOY, DOCENTE, Set.of(id(100)), Set.of(), List.of(acuerdo),
                new RegistroCambio(DOCENTE, reloj(20), Optional.empty()));
        var atenciones = RegistroAtenciones.iniciar(List.of(hecho), RestriccionesAmbito.vacias())
                .registrarInmediata(id(100), inmediata, directorio(), new RegistroCambio(DOCENTE, reloj(20), Optional.empty()))
                .abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(), Optional.of(conclusion(30)), directorio(), cambio(30));
        var entrada = BandejaAtencion.proyectar(TOE, atenciones, RegistroAlertas.vacio(), directorio(), reloj(40)).getFirst();
        assertEquals(Set.of(id(400)), entrada.compromisosPendientes());
        assertEquals(Optional.of(HOY.plusDays(1)), entrada.vencimiento());
        assertTrue(atenciones.caso(id(200)).orElseThrow().actuaciones().isEmpty());
    }

    @Test
    void exclusionDeHechoRetiradoDelCasoSigueRestringiendoSenalYBandeja() {
        var hechos = List.of(grave(100), grave(101));
        var atenciones = RegistroAtenciones.iniciar(hechos, RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(id(100), id(101)), HOY, TOE, List.of(), Optional.empty(), directorio(), cambio(20))
                .retirarVinculo(id(200), id(101), directorio(), motivo(30))
                .excluirUsuario(id(200), id(600), DIRECTOR, directorio(), motivo(40));
        var actual = atenciones.hechosDelCaso(id(200), TOE, directorio(), reloj(50)).stream().filter(h -> h.id().equals(id(100))).findFirst().orElseThrow();
        var contexto = atenciones.contextoDeHechos(Set.of(id(100)));
        var politica = new PoliticaAcceso(directorio(), atenciones.restricciones());
        var alertas = RegistroAlertas.vacio().evaluarHecho(actual, contexto, politica, cambio(50));
        assertTrue(BandejaAtencion.proyectar(DIRECTOR, atenciones, alertas, directorio(), reloj(60)).isEmpty());
        assertEquals(1, BandejaAtencion.proyectar(TOE, atenciones, alertas, directorio(), reloj(60)).size());
        assertTrue(BandejaAtencion.proyectar(DOCENTE, atenciones, alertas, directorio(), reloj(60)).isEmpty());
        assertTrue(BandejaAtencion.proyectar(ADMIN, atenciones, alertas, directorio(), reloj(60)).isEmpty());
    }

    @Test
    void discrepanciaMuestraGravedadesDeclaradasSinInventarClasificacionVigente() {
        var unido = grave(100).vincularDuplicado(leve(101).principal(), motivo(20));
        var alertas = evaluar(RegistroAlertas.vacio(), unido, 30);
        var atenciones = RegistroAtenciones.iniciar(List.of(unido), RestriccionesAmbito.vacias());
        var entrada = BandejaAtencion.proyectar(TOE, atenciones, alertas, directorio(), reloj(40)).getFirst();
        assertEquals(Set.of(Gravedad.GRAVE, Gravedad.LEVE), entrada.gravedadesDeclaradas());
        assertTrue(unido.clasificacionVigente().isEmpty());
    }

    @Test
    void derivacionPreparadaEsTrabajoRealTrasCierreYNoSeInventaFechaDeEjecucion() {
        var hecho = grave(100);
        var atenciones = RegistroAtenciones.iniciar(List.of(hecho), RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(), Optional.empty(), directorio(), cambio(20));
        var preparada = Derivacion.preparada(id(500), Set.of(id(100)), Derivacion.Supuesto.OTRO,
                new Derivacion.Destino(Derivacion.TipoDestino.OTRA_ENTIDAD, "Entidad referida"), "Derivación pendiente de efectuar", Optional.empty(), cambio(30));
        atenciones = atenciones.registrarDerivacion(id(200), preparada, directorio(), cambio(30)).cerrarCaso(id(200), conclusion(40), directorio(), cambio(40));
        var entrada = BandejaAtencion.proyectar(TOE, atenciones, RegistroAlertas.vacio(), directorio(), reloj(50)).getFirst();
        assertEquals(Set.of(id(500)), entrada.derivacionesPendientes());
        assertTrue(entrada.vencimiento().isEmpty());
        assertFalse(entrada.atencionPendiente());
    }

    @Test
    void contextosSuplementariosNoPermitenEvitarReservaActualNiMutarLaProyeccion() {
        var hecho = grave(100);
        var atenciones = RegistroAtenciones.iniciar(List.of(hecho), RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(), Optional.empty(), directorio(), cambio(20))
                .excluirUsuario(id(200), id(600), DIRECTOR, directorio(), motivo(30));
        var actual = atenciones.hechosDelCaso(id(200), TOE, directorio(), reloj(40)).getFirst();
        var alertas = RegistroAlertas.vacio().evaluarHecho(actual, atenciones.contexto(id(200)), new PoliticaAcceso(directorio(), atenciones.restricciones()), cambio(40));
        var anteriores = Map.of(unica(alertas).id(), ContextoAcceso.deHecho(hecho));
        assertTrue(BandejaAtencion.proyectar(DIRECTOR, atenciones, alertas, directorio(), anteriores, reloj(50)).isEmpty());
        var entradas = BandejaAtencion.proyectar(TOE, atenciones, alertas, directorio(), reloj(50));
        assertThrows(UnsupportedOperationException.class, entradas::clear);
        assertThrows(UnsupportedOperationException.class, () -> entradas.getFirst().hechosIds().clear());
    }
}
