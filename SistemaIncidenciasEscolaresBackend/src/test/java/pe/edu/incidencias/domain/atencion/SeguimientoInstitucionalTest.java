package pe.edu.incidencias.domain.atencion;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.alerta.*;
import pe.edu.incidencias.domain.incidencia.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.atencion.DatosPruebaAtencion.*;

class SeguimientoInstitucionalTest {
    @Test
    void leerYAbrirConIntervencionYCierreActualizaAvisosEnElMismoResultado() {
        var hecho = hecho(100, PapelParticipacion.PRESUNTA_RESPONSABILIDAD);
        var contexto = ContextoAcceso.deHecho(hecho);
        var politica = new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias());
        var anteriores = RegistroAlertas.vacio().evaluarHecho(hecho, contexto, politica, cambio(30));
        var aviso = anteriores.notificaciones().getFirst();
        var leidas = anteriores.leerNotificacion(aviso.id(), contexto, politica, aviso.destinatarioId(), reloj(40));
        assertTrue(leidas.alertas().getFirst().pendiente());

        var nuevasAtenciones = RegistroAtenciones.iniciar(List.of(hecho), RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(hecho.id()), HOY, TOE, List.of(realizada(300, List.of(), 100)),
                        Optional.of(conclusion(100)), directorio(), cambio(100));
        var conjunto = SeguimientoInstitucional.actualizar(nuevasAtenciones, leidas, directorio(), cambio(100));

        assertSame(nuevasAtenciones, conjunto.atenciones());
        assertEquals(CasoAtencion.Estado.CERRADA, conjunto.atenciones().caso(id(200)).orElseThrow().estado());
        var gestionada = conjunto.alertas().alertas().getFirst();
        assertFalse(gestionada.pendiente());
        assertEquals(Optional.of(id(200)), gestionada.disposicion().orElseThrow().referenciaId());
        assertTrue(conjunto.alertas().notificacionesConsultables(TOE, Map.of(gestionada.id(), conjunto.atenciones().contexto(id(200))),
                new PoliticaAcceso(directorio(), conjunto.atenciones().restricciones()), reloj(110)).isEmpty());
        assertTrue(anteriores.alertas().getFirst().pendiente());
    }

    @Test
    void actualizarAtencionDeOtroHechoNoGestionaUnaSenalAjena() {
        var ajeno = hecho(102, PapelParticipacion.AFECTADO);
        var alertas = RegistroAlertas.vacio().evaluarHecho(ajeno, ContextoAcceso.deHecho(ajeno),
                new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias()), cambio(30));
        var actualizado = SeguimientoInstitucional.actualizar(abrir(base(), 200, Set.of(id(100)), 100),
                alertas, directorio(), cambio(100));
        assertTrue(actualizado.alertas().alertas().getFirst().pendiente());
        assertEquals(1, actualizado.atenciones().casos().size());
    }

    @Test
    void repetirCoordinacionNoCreaOtraGestionONotificacion() {
        var hecho = hecho(100, PapelParticipacion.PRESUNTA_RESPONSABILIDAD);
        var alertas = RegistroAlertas.vacio().evaluarHecho(hecho, ContextoAcceso.deHecho(hecho),
                new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias()), cambio(30));
        var primero = SeguimientoInstitucional.actualizar(abrir(base(), 200, Set.of(id(100)), 100),
                alertas, directorio(), cambio(100));
        var segundo = SeguimientoInstitucional.actualizar(primero.atenciones(), primero.alertas(), directorio(), cambio(110));
        assertEquals(primero.alertas().alertas(), segundo.alertas().alertas());
        assertEquals(primero.alertas().notificaciones(), segundo.alertas().notificaciones());
        assertEquals(1, segundo.atenciones().casos().size());
    }
}
