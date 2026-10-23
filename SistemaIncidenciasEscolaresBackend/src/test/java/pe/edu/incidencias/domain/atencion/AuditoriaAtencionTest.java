package pe.edu.incidencias.domain.atencion;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.proteccion.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.atencion.DatosPruebaAtencion.*;

class AuditoriaAtencionTest {
    @Test void politicaExternaIncompletaNoEludeExclusionesDelRegistro() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100)
                .registrarActuacion(id(200), realizada(300, List.of(), 110), directorio(), cambio(110))
                .excluirUsuario(id(200), id(900), DIRECTOR, directorio(), motivo(120));
        var incompleta = new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias());
        assertThrows(SecurityException.class, () -> registro.actuacionesDelCaso(id(200), DIRECTOR, incompleta, reloj(130)));
        assertThrows(SecurityException.class, () -> registro.inmediatasDelHecho(id(100), DIRECTOR, incompleta, reloj(130)));
        assertEquals(1, registro.actuacionesDelCaso(id(200), TOE, incompleta, reloj(130)).size());
    }

    @Test void retirarVinculoNoLiberaUnaDenunciaQuePermaneceEnLaConclusion() {
        var registro = abrir(base(), 200, Set.of(id(100), id(101)), 100)
                .cerrarCaso(id(200), conclusion(110), directorio(), cambio(110));
        var restricciones = new PoliticaAcceso(directorio(), registro.restricciones()).excluir(id(900), DIRECTOR,
                ContextoAcceso.deHecho(registro.hechoVigente(id(101))), motivo(120));
        var cargado = RegistroAtenciones.cargarAmbito(List.of(registro.hechoVigente(id(100)), registro.hechoVigente(id(101))),
                registro.casos(), Map.of(), restricciones, List.of(), registro.indice());
        var corregido = cargado.retirarVinculo(id(200), id(101), directorio(), motivo(130));
        assertTrue(corregido.caso(id(200)).orElseThrow().hechosReservados().contains(id(101)));
        assertThrows(SecurityException.class, () -> corregido.consultarCaso(id(200), DIRECTOR, directorio(), reloj(140)));
        assertNotNull(corregido.consultarCaso(id(200), TOE, directorio(), reloj(140)));
    }

    @Test void bloqueosDeGobiernoDelComiteProtegenContenidoEmbebido() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100)
                .convocarComite(id(200), id(600), HOY, "Especial gravedad", Optional.empty(), directorio(), cambio(110));
        var director = new RegistroCambio(DIRECTOR, reloj(130), Optional.empty());
        registro = registro.registrarIntervencionComite(id(200), id(601), id(600), reloj(120), Set.of(DIRECTOR),
                Optional.empty(), Map.of(id(602), "Decisión institucional"), List.of(), Optional.empty(), directorio(), director);
        for (long recurso : List.of(600L, 601L, 602L)) {
            var alcance = new AlcanceProteccion(Optional.empty(), Set.of(), Set.of(id(recurso)));
            var autorizacion = new AutorizacionProteccion(id(800), ADMIN, Set.of(AutorizacionProteccion.Operacion.BLOQUEAR),
                    alcance, new Vigencia(INICIO, Optional.empty()), "Responsable", "Constancia externa", "Decisión documentada", INICIO);
            var control = ControlProteccion.vacio().bloquear(id(801), alcance, autorizacion,
                    new RegistroCambio(ADMIN, reloj(140), Optional.of("Protección documentada")), directorio());
            var protegido = registro.conProteccion(control);
            assertThrows(SecurityException.class, () -> protegido.consultarCaso(id(200), TOE, directorio(), reloj(150)));
        }
    }

    @Test void actuacionesYNuevasRevisionesConservanAlOperadorRealSinReescribirAutoresAnteriores() {
        var ajena = Actuacion.realizada(id(300), "Acción atribuida a otro operador", HOY, DIRECTOR, Set.of(), Set.of(), List.of(),
                new RegistroCambio(DIRECTOR, reloj(100), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> base().abrirCaso(id(200), Set.of(id(100)), HOY, TOE,
                List.of(ajena), Optional.empty(), directorio(), cambio(100)));

        var acuerdo = Compromiso.acordar(id(400), "Acuerdo real", estudiante(), Optional.empty(), true, cambio(110));
        var accion = realizada(300, List.of(acuerdo), 110);
        var registro = abrir(base(), 200, Set.of(id(100)), 100).registrarActuacion(id(200), accion, directorio(), cambio(110));
        var seguimientoDirector = new RegistroCambio(DIRECTOR, reloj(120), Optional.empty());
        var seguido = acuerdo.registrarSeguimiento(HOY, "Seguimiento real", Optional.empty(), false, seguimientoDirector);
        var actualizado = accion.actualizarCompromiso(seguido, seguimientoDirector);
        var correcto = registro.registrarActuacion(id(200), actualizado, directorio(), seguimientoDirector);
        assertEquals(TOE, correcto.caso(id(200)).orElseThrow().actuaciones().getFirst().revisiones().getFirst().cambio().autorId());
        assertThrows(IllegalArgumentException.class, () -> registro.registrarActuacion(id(200), actualizado, directorio(), cambio(130)));
        var autorFalsoSoloEnCompromiso = accion.actualizarCompromiso(seguido, cambio(130));
        assertThrows(IllegalArgumentException.class, () -> registro.registrarActuacion(id(200), autorFalsoSoloEnCompromiso, directorio(), cambio(130)));
    }

    @Test void derivacionNoPermiteAtribuirElRegistroAUnOperadorDistinto() {
        var derivacion = Derivacion.preparada(id(700), Set.of(id(100)), Derivacion.Supuesto.OTRO,
                new Derivacion.Destino(Derivacion.TipoDestino.OTRA_ENTIDAD, "Entidad competente"), "Derivación preparada", Optional.empty(),
                new RegistroCambio(DIRECTOR, reloj(110), Optional.empty()));
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        assertThrows(IllegalArgumentException.class, () -> registro.registrarDerivacion(id(200), derivacion, directorio(), cambio(120)));
        assertTrue(registro.caso(id(200)).orElseThrow().derivaciones().isEmpty());
    }

    @Test void autorRegistraProteccionUrgenteGraveSinCasoNiAcuerdosFormales() {
        var propio = new RegistroCambio(DOCENTE, reloj(50), Optional.empty());
        var registro = base();
        var urgente = Actuacion.realizada(id(300), "Se documenta la protección urgente realizada", HOY, DOCENTE,
                Set.of(id(100)), Set.of(ESTUDIANTE), List.of(), propio);
        var guardado = registro.registrarInmediata(id(100), urgente, directorio(), propio);
        assertTrue(guardado.casos().isEmpty());
        assertEquals(1, guardado.inmediatasDelHecho(id(100), TOE, new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias()), reloj(60)).size());
        var compromiso = Compromiso.acordar(id(400), "Acuerdo que requiere atención", estudiante(), Optional.empty(), false, propio);
        var conAcuerdo = Actuacion.realizada(id(301), "Protección y acuerdo", HOY, DOCENTE, Set.of(id(100)), Set.of(), List.of(compromiso), propio);
        assertThrows(IllegalArgumentException.class, () -> registro.registrarInmediata(id(100), conAcuerdo, directorio(), propio));
        var autoriaFalsa = Actuacion.realizada(id(302), "Protección con autor falso", HOY, DOCENTE,
                Set.of(id(100)), Set.of(), List.of(), cambio(50));
        assertThrows(IllegalArgumentException.class, () -> registro.registrarInmediata(id(100), autoriaFalsa, directorio(), propio));
    }

    @Test void consultaPropiaPermaneceTrasAtencionSinExponerSeguimientosInstitucionales() {
        var propio = new RegistroCambio(DOCENTE, reloj(50), Optional.empty());
        var compromiso = Compromiso.acordar(id(400), "Compromiso cotidiano propio", estudiante(), Optional.empty(), true, propio);
        var accion = Actuacion.realizada(id(300), "Conversación propia", HOY, DOCENTE, Set.of(id(100)), Set.of(), List.of(compromiso), propio);
        var inmediato = RegistroAtenciones.iniciar(List.of(hechoLeve(100)), RestriccionesAmbito.vacias())
                .registrarInmediata(id(100), accion, directorio(), propio);
        var abierta = abrir(inmediato, 200, Set.of(id(100)), 100);
        var observado = compromiso.registrarSeguimiento(HOY, "Seguimiento institucional reservado", Optional.of(false), true, cambio(110));
        var actualizado = abierta.actualizarInmediata(id(100), accion.actualizarCompromiso(observado, cambio(110)), directorio(), cambio(110));
        var vista = actualizado.inmediatasDelAutor(id(100), DOCENTE, directorio(), reloj(120)).getFirst();
        assertEquals(1, vista.revisionesPropias().size());
        assertEquals("Conversación propia", vista.revisionesPropias().getFirst().relato());
        var propias = vista.revisionesPropias().getFirst().compromisos().getFirst().revisionesPropias();
        assertEquals(1, propias.size());
        assertEquals(Compromiso.Estado.ACORDADO, propias.getFirst().estado());
        assertTrue(propias.getFirst().observacion().isEmpty());
        assertThrows(SecurityException.class, () -> actualizado.inmediatasDelAutor(id(100), AIP, directorio(), reloj(120)));
        var excluido = actualizado.excluirUsuario(id(200), id(900), DOCENTE, directorio(), motivo(130));
        assertThrows(SecurityException.class, () -> excluido.inmediatasDelAutor(id(100), DOCENTE, directorio(), reloj(140)));
        assertSame(actualizado.proteccion(), actualizado.proteccion());
    }
}
