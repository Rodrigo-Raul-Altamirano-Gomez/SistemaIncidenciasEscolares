package pe.edu.incidencias.domain.alerta;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.proteccion.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.alerta.DatosPruebaAlertas.*;

class AuditoriaAlertasTest {
    private PoliticaAcceso bloquear(UUID recurso) {
        var alcance = new AlcanceProteccion(Optional.empty(), Set.of(), Set.of(recurso));
        var autorizacion = new AutorizacionProteccion(id(900), ADMIN, Set.of(AutorizacionProteccion.Operacion.BLOQUEAR),
                alcance, new Vigencia(BASE, Optional.empty()), "Autoridad ficticia", "Constancia ficticia", "Decisión ficticia", BASE);
        var control = ControlProteccion.vacio().bloquear(id(901), alcance, autorizacion,
                new pe.edu.incidencias.domain.incidencia.RegistroCambio(ADMIN, reloj(30), Optional.of("Reserva expresa")), directorio());
        return new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias(), control);
    }

    @Test void bloqueoDeSenalRevocaDetalleGestionPendientesYAvisos() {
        var h = grave(100);
        var registro = evaluar(RegistroAlertas.vacio(), h, 20);
        var alerta = unica(registro);
        var contexto = ContextoAcceso.deHecho(h);
        var politica = bloquear(alerta.id());
        assertThrows(SecurityException.class, () -> registro.consultar(alerta.id(), contexto, politica, TOE, reloj(40)));
        assertThrows(SecurityException.class, () -> registro.disponer(alerta.id(), disposicion(40), contexto, politica));
        assertThrows(SecurityException.class, () -> registro.evaluarHecho(h, contexto, politica, cambio(40)));
        assertTrue(registro.pendientes(TOE, Map.of(alerta.id(), contexto), politica, reloj(40)).isEmpty());
        assertTrue(registro.notificacionesConsultables(TOE, Map.of(alerta.id(), contexto), politica, reloj(40)).isEmpty());
    }

    @Test void bloqueoDeAvisoNoPermiteLecturaDirectaNiEntregaPorListado() {
        var h = grave(100);
        var registro = evaluar(RegistroAlertas.vacio(), h, 20);
        var aviso = registro.notificaciones().getFirst();
        var contexto = ContextoAcceso.deHecho(h);
        var politica = bloquear(aviso.id());
        assertThrows(SecurityException.class, () -> registro.leerNotificacion(aviso.id(), contexto, politica, TOE, reloj(40)));
        assertTrue(registro.notificacionesConsultables(TOE, Map.of(unica(registro).id(), contexto), politica, reloj(40)).isEmpty());
        assertEquals(1, registro.pendientes(TOE, Map.of(unica(registro).id(), contexto), politica, reloj(40)).size());
    }

    @Test void cargaNoAdmiteAvisosDuplicadosParaElMismoDestinatario() {
        var registro = evaluar(RegistroAlertas.vacio(), grave(100), 20);
        var aviso = registro.notificaciones().getFirst();
        var duplicado = new Notificacion(id(999), aviso.alertaId(), aviso.destinatarioId(), aviso.emitidaEn(), List.of());
        assertThrows(IllegalArgumentException.class, () -> RegistroAlertas.cargarAmbito(registro.alertas(),
                List.of(aviso, duplicado), Set.of()));
        var anterior = new Notificacion(id(998), aviso.alertaId(), aviso.destinatarioId(), reloj(19), List.of());
        assertThrows(IllegalArgumentException.class, () -> RegistroAlertas.cargarAmbito(registro.alertas(), List.of(anterior), Set.of()));
    }

    @Test void consultaDeAvisosRechazaLecturasPosterioresAlCorte() {
        var h = grave(100);
        var registro = evaluar(RegistroAlertas.vacio(), h, 20);
        var aviso = registro.notificaciones().getFirst();
        var contexto = ContextoAcceso.deHecho(h);
        var futuro = registro.leerNotificacion(aviso.id(), contexto, politica(), TOE, reloj(50));
        assertThrows(IllegalArgumentException.class, () -> futuro.notificacionesConsultables(TOE,
                Map.of(unica(futuro).id(), contexto), politica(), reloj(40)));
    }
}
