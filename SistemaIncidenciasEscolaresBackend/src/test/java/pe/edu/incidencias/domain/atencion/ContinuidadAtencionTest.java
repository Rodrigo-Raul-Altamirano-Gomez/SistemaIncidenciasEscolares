package pe.edu.incidencias.domain.atencion;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.RestriccionesAmbito;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.atencion.DatosPruebaAtencion.*;

class ContinuidadAtencionTest {
    private static RegistroAtenciones dosCasos() {
        return abrir(abrir(base(), 200, Set.of(id(100)), 100), 201, Set.of(id(101)), 110);
    }

    @Test void continuarNoDuplicaHechoNiFusionaHistoriasYCierraFuenteSinOtroTrabajo() {
        var anterior = dosCasos().registrarActuacion(id(200), realizada(300, List.of(), 120), directorio(), cambio(120));
        var nuevo = anterior.continuarAtencion(id(200), id(201), Set.of(id(100)), directorio(), cambio(130));
        var fuente = nuevo.caso(id(200)).orElseThrow();
        var destino = nuevo.caso(id(201)).orElseThrow();
        assertEquals(CierreLocal.TipoCierre.DUPLICIDAD, fuente.cierre().orElseThrow().tipo());
        assertEquals(Optional.of(id(201)), fuente.continuidadHacia());
        assertEquals(Set.of(id(100)), fuente.hechosHistoricos());
        assertTrue(fuente.hechosActuales().isEmpty());
        assertEquals(Set.of(id(100), id(101)), destino.hechosActuales());
        assertEquals(Set.of(id(200)), destino.antecedentes());
        assertTrue(destino.actuaciones().isEmpty());
        assertEquals(id(300), fuente.actuaciones().getFirst().id());
        assertEquals(1, nuevo.casos().stream().filter(c -> c.estado() == CasoAtencion.Estado.PENDIENTE && c.hechosActuales().contains(id(100))).count());
        assertEquals(Set.of(id(100)), anterior.caso(id(200)).orElseThrow().hechosActuales());
    }

    @Test void seleccionParcialConservaOtrosHechosPendientesEnFuente() {
        var anterior = abrir(base(), 200, Set.of(id(100), id(102)), 100);
        var nuevo = abrir(anterior, 201, Set.of(id(101)), 110).continuarAtencion(id(200), id(201), Set.of(id(100)), directorio(), cambio(120));
        var fuente = nuevo.caso(id(200)).orElseThrow();
        assertEquals(CasoAtencion.Estado.PENDIENTE, fuente.estado());
        assertEquals(Set.of(id(102)), fuente.hechosActuales());
        assertEquals(Set.of(id(100), id(102)), fuente.hechosHistoricos());
        assertEquals(Set.of(id(100), id(101)), nuevo.caso(id(201)).orElseThrow().hechosActuales());
    }

    @Test void trabajosPreviosNoDesaparecenYSoloDestinoReclamaHechoVigente() {
        var prevista = Actuacion.prevista(id(300), "Trabajo previo que sigue pendiente", HOY, TOE, Set.of(id(100)), Set.of(), cambio(120));
        var anterior = dosCasos().registrarActuacion(id(200), prevista, directorio(), cambio(120));
        var nuevo = anterior.continuarAtencion(id(200), id(201), Set.of(id(100)), directorio(), cambio(130));
        var fuente = nuevo.caso(id(200)).orElseThrow();
        assertEquals(CasoAtencion.Estado.PENDIENTE, fuente.estado());
        assertTrue(fuente.hechosActuales().isEmpty());
        assertEquals(List.of(prevista), fuente.actuaciones());
        assertEquals(Set.of(id(200), id(201)), nuevo.conTrabajoPendiente(TOE, directorio(), reloj(140)).stream().map(CasoAtencion::id).collect(java.util.stream.Collectors.toSet()));
        var completada = prevista.realizar(HOY, "Trabajo previo efectivamente realizado", cambio(150));
        var concluido = nuevo.registrarActuacion(id(200), completada, directorio(), cambio(150));
        assertEquals(CierreLocal.TipoCierre.DUPLICIDAD, concluido.caso(id(200)).orElseThrow().cierre().orElseThrow().tipo());
        assertEquals(2, concluido.caso(id(200)).orElseThrow().actuaciones().getFirst().revisiones().size());
        assertEquals(List.of(concluido.caso(id(201)).orElseThrow()), concluido.conTrabajoPendiente(TOE, directorio(), reloj(160)));
    }

    @Test void compromisoPendienteYDerivacionPreparadaSeConservanEnFuente() {
        var compromiso = Compromiso.acordar(id(400), "Acuerdo pendiente", estudiante(), Optional.empty(), true, cambio(120));
        var derivacion = Derivacion.preparada(id(500), Set.of(id(100)), Derivacion.Supuesto.OTRO,
                new Derivacion.Destino(Derivacion.TipoDestino.OTRA_ENTIDAD, "Entidad de apoyo"), "Preparación pendiente", Optional.empty(), cambio(120));
        var anterior = dosCasos().registrarActuacion(id(200), realizada(300, List.of(compromiso), 120), directorio(), cambio(120))
                .registrarDerivacion(id(200), derivacion, directorio(), cambio(120));
        var nuevo = anterior.continuarAtencion(id(200), id(201), Set.of(id(100)), directorio(), cambio(130));
        var fuente = nuevo.caso(id(200)).orElseThrow();
        assertEquals(CasoAtencion.Estado.PENDIENTE, fuente.estado());
        assertEquals(Compromiso.Estado.ACORDADO, fuente.actuaciones().getFirst().compromisos().getFirst().estado());
        assertEquals(Derivacion.Estado.PREPARADA, fuente.derivaciones().getFirst().estado());
        var efectuada = derivacion.efectuar(HOY, "Comunicación realizada", Optional.empty(), cambio(140));
        var actualizado = nuevo.registrarDerivacion(id(200), efectuada, directorio(), cambio(140));
        assertEquals(CasoAtencion.Estado.PENDIENTE, actualizado.caso(id(200)).orElseThrow().estado());
        assertTrue(actualizado.caso(id(201)).orElseThrow().derivaciones().isEmpty());
    }

    @Test void compromisoDeAccionInmediataConservaPendienteYSeCompletaSinCopiarAccion() {
        var compromiso = Compromiso.acordar(id(400), "Compromiso inmediato pendiente", estudiante(), Optional.empty(), true,
                new RegistroCambio(DOCENTE, reloj(50), Optional.empty()));
        var accion = Actuacion.realizada(id(300), "Acción cotidiana realizada", HOY, DOCENTE, Set.of(id(100)), Set.of(ESTUDIANTE), List.of(compromiso),
                new RegistroCambio(DOCENTE, reloj(50), Optional.empty()));
        var registro = RegistroAtenciones.iniciar(List.of(hechoLeve(100), hechoLeve(101)), RestriccionesAmbito.vacias())
                .registrarInmediata(id(100), accion, directorio(), new RegistroCambio(DOCENTE, reloj(50), Optional.empty()));
        var nuevo = abrir(abrir(registro, 200, Set.of(id(100)), 100), 201, Set.of(id(101)), 110)
                .continuarAtencion(id(200), id(201), Set.of(id(100)), directorio(), cambio(120));
        assertEquals(CasoAtencion.Estado.PENDIENTE, nuevo.caso(id(200)).orElseThrow().estado());
        assertTrue(nuevo.caso(id(200)).orElseThrow().actuaciones().isEmpty());
        var cumplido = compromiso.registrarSeguimiento(HOY, "Acuerdo cumplido y documentado", Optional.of(true), false, cambio(130));
        var actualizada = accion.actualizarCompromiso(cumplido, cambio(130));
        var concluido = nuevo.actualizarInmediata(id(100), actualizada, directorio(), cambio(130));
        assertEquals(CierreLocal.TipoCierre.DUPLICIDAD, concluido.caso(id(200)).orElseThrow().cierre().orElseThrow().tipo());
        assertEquals(id(300), concluido.actuacionesDelCaso(id(201), TOE, directorio(), reloj(140)).getFirst().id());
    }

    @Test void continuidadesEncadenadasConservanReservaHistoricaTransitiva() {
        var registro = abrir(dosCasos(), 202, Set.of(id(102)), 120)
                .continuarAtencion(id(200), id(201), Set.of(id(100)), directorio(), cambio(130))
                .continuarAtencion(id(201), id(202), Set.of(id(100), id(101)), directorio(), cambio(140));
        assertEquals(Set.of(id(200), id(201), id(202)), registro.contexto(id(202)).ambito().casosIds());
        assertEquals(Set.of(id(100), id(101), id(102)), registro.contexto(id(202)).ambito().hechosIds());
        var excluido = registro.excluirUsuario(id(200), id(700), DIRECTOR, directorio(), motivo(150));
        assertThrows(SecurityException.class, () -> excluido.consultarCaso(id(202), DIRECTOR, directorio(), reloj(160)));
        assertEquals(Set.of(id(100)), registro.contextoDeHechos(Set.of(id(100))).ambito().hechosIds());
        assertEquals(Set.of(id(200), id(201), id(202)), registro.contextoDeHechos(Set.of(id(100))).ambito().casosIds());
    }

    @Test void operadorImplicadoEnCualquieraDeLosCasosNoPuedeElegirContinuidad() {
        var registro = dosCasos().excluirUsuario(id(200), id(700), DIRECTOR, directorio(), motivo(120));
        assertThrows(SecurityException.class, () -> registro.continuarAtencion(id(200), id(201), Set.of(id(100)), directorio(), new RegistroCambio(DIRECTOR, reloj(130), Optional.empty())));
        var nuevo = registro.continuarAtencion(id(200), id(201), Set.of(id(100)), directorio(), cambio(130));
        assertThrows(SecurityException.class, () -> nuevo.consultarCaso(id(201), DIRECTOR, directorio(), reloj(140)));
    }

    @Test void transicionRechazaDestinoCerradoFuenteAjenaYRepeticionSinModificarOriginal() {
        var registro = dosCasos();
        assertThrows(IllegalArgumentException.class, () -> registro.continuarAtencion(id(200), id(200), Set.of(id(100)), directorio(), cambio(120)));
        assertThrows(IllegalArgumentException.class, () -> registro.continuarAtencion(id(200), id(201), Set.of(id(102)), directorio(), cambio(120)));
        var cerrado = registro.cerrarCaso(id(201), conclusion(120), directorio(), cambio(120));
        assertThrows(IllegalStateException.class, () -> cerrado.continuarAtencion(id(200), id(201), Set.of(id(100)), directorio(), cambio(130)));
        var nuevo = registro.continuarAtencion(id(200), id(201), Set.of(id(100)), directorio(), cambio(120));
        assertThrows(IllegalArgumentException.class, () -> nuevo.continuarAtencion(id(200), id(201), Set.of(id(100)), directorio(), cambio(130)));
        assertEquals(Set.of(id(100)), registro.caso(id(200)).orElseThrow().hechosActuales());
        assertThrows(UnsupportedOperationException.class, () -> nuevo.caso(id(200)).orElseThrow().continuidades().clear());
    }

    @Test void continuidadNoAutorizaNuevaAtencionPendienteDelMismoHechoNiReaperturaVacia() {
        var nuevo = dosCasos().continuarAtencion(id(200), id(201), Set.of(id(100)), directorio(), cambio(120));
        assertThrows(IllegalStateException.class, () -> abrir(nuevo, 202, Set.of(id(100)), 130));
        assertThrows(IllegalStateException.class, () -> nuevo.corregirCierre(id(200), Optional.empty(), directorio(), motivo(130)));
    }
}
