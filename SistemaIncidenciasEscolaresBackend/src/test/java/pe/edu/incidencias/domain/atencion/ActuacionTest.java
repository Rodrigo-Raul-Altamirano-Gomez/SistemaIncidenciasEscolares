package pe.edu.incidencias.domain.atencion;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.incidencia.PersonaInvolucrada;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActuacionTest {
    private static final Instant REGISTRO = Instant.parse("2026-10-21T01:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);
    private static final UUID AUTOR = id(1);
    private static final UUID RESPONSABLE = id(2);

    @Test
    void realizadaDirectaNoExigeProgramacionCompromisosNiRelatoRedundante() {
        var actuacion = realizada(List.of());
        assertEquals(Actuacion.Estado.REALIZADA, actuacion.estado());
        assertEquals(Optional.of(HOY), actuacion.fechaEfectiva());
        assertTrue(actuacion.fechaPrevista().isEmpty());
        assertFalse(actuacion.pendiente());
        assertEquals(RESPONSABLE, actuacion.responsableId());
        assertEquals(AUTOR, actuacion.revisiones().getFirst().cambio().autorId());
        assertTrue(actuacion.hechosIds().isEmpty());
        assertTrue(actuacion.personasIds().isEmpty());
    }

    @Test
    void previstaPuedeSerFuturaYNoEsUnaAccionRealizada() {
        var prevista = prevista();
        assertEquals(Actuacion.Estado.PREVISTA, prevista.estado());
        assertEquals(Optional.of(HOY.plusDays(5)), prevista.fechaPrevista());
        assertTrue(prevista.fechaEfectiva().isEmpty());
        assertTrue(prevista.pendiente());
    }

    @Test
    void realizarCompletaLaMismaActuacionYConservaProgramacionYFuentes() {
        var prevista = prevista();
        var realizada = prevista.realizar(HOY, "Se conversó y se obtuvo un resultado ficticio", cambio(1));
        assertEquals(prevista.id(), realizada.id());
        assertEquals(Actuacion.Estado.REALIZADA, realizada.estado());
        assertEquals(prevista.fechaPrevista(), realizada.fechaPrevista());
        assertEquals(Optional.of(HOY), realizada.fechaEfectiva());
        assertEquals(2, realizada.revisiones().size());
        assertEquals(prevista.revisiones().getFirst(), realizada.revisiones().getFirst());
        assertTrue(realizada.esContinuacionDe(prevista));
        assertFalse(prevista.esContinuacionDe(realizada));
        assertFalse(realizada.pendiente());
        assertEquals(Actuacion.Estado.PREVISTA, prevista.estado());
    }

    @Test
    void cancelarPrevistaExigeCausaYNoLaCuentaComoRealizada() {
        var prevista = prevista();
        assertThrows(IllegalArgumentException.class, () -> prevista.cancelar(cambio(1)));
        var cancelada = prevista.cancelar(motivo(1));
        assertEquals(Actuacion.Estado.CANCELADA, cancelada.estado());
        assertTrue(cancelada.fechaEfectiva().isEmpty());
        assertFalse(cancelada.pendiente());
        assertEquals(motivo(1), cancelada.revisiones().getLast().cambio());
        assertEquals(prevista.revisiones().getFirst(), cancelada.revisiones().getFirst());
    }

    @Test
    void noPermiteRealizarDosVecesNiCancelarLoYaRealizado() {
        var realizada = realizada(List.of());
        assertThrows(IllegalStateException.class, () -> realizada.realizar(HOY, "Segunda ejecución", cambio(1)));
        assertThrows(IllegalStateException.class, () -> realizada.cancelar(motivo(1)));
        var cancelada = prevista().cancelar(motivo(1));
        assertThrows(IllegalStateException.class, () -> cancelada.realizar(HOY, "No se ejecutó", cambio(2)));
    }

    @Test
    void compromisoSinPlazoNoCreaPendienteNiSeVerificaPorAñadirloALaActuacion() {
        var compromiso = compromiso(Optional.empty(), false);
        var realizada = realizada(List.of(compromiso));
        assertFalse(realizada.pendiente());
        assertEquals(Compromiso.Estado.ACORDADO, realizada.compromisos().getFirst().estado());
    }

    @Test
    void compromisosConPlazoOSeguimientoContinuanPendientesTrasLaEjecucion() {
        assertTrue(realizada(List.of(compromiso(Optional.of(HOY), false))).pendiente());
        assertTrue(realizada(List.of(compromiso(Optional.empty(), true))).pendiente());
    }

    @Test
    void cancelarUnaAccionNoCancelaAutomaticamenteUnAcuerdoExistente() {
        var compromiso = compromiso(Optional.of(HOY.plusDays(5)), false);
        var prevista = prevista().agregarCompromiso(compromiso, cambio(1));
        var cancelada = prevista.cancelar(motivo(2));
        assertEquals(Actuacion.Estado.CANCELADA, cancelada.estado());
        assertTrue(cancelada.pendiente());
        assertEquals(Compromiso.Estado.ACORDADO, cancelada.compromisos().getFirst().estado());
    }

    @Test
    void registrarSeguimientoActualizaElMismoCompromisoSinCopiarloComoOtroAcuerdo() {
        var compromiso = compromiso(Optional.of(HOY), false);
        var actuacion = realizada(List.of(compromiso));
        var cumplido = compromiso.registrarSeguimiento(HOY, "Cumplimiento observado", Optional.of(true), false, cambio(1));
        var actualizada = actuacion.actualizarCompromiso(cumplido, cambio(1));
        assertEquals(actuacion.id(), actualizada.id());
        assertEquals(1, actualizada.compromisos().size());
        assertEquals(compromiso.id(), actualizada.compromisos().getFirst().id());
        assertFalse(actualizada.pendiente());
        assertTrue(actuacion.pendiente());
        assertTrue(actualizada.esContinuacionDe(actuacion));
        assertEquals(2, actualizada.compromisos().getFirst().revisiones().size());
    }

    @Test
    void noAdmiteCompromisosDuplicadosNiVersionesQuePierdanHistoria() {
        var compromiso = compromiso(Optional.of(HOY), false);
        assertThrows(IllegalArgumentException.class, () -> realizada(List.of(compromiso, compromiso)));
        var original = realizada(List.of(compromiso));
        assertThrows(IllegalArgumentException.class, () -> original.agregarCompromiso(compromiso, cambio(1)));
        var seguido = compromiso.registrarSeguimiento(HOY, "Observación", Optional.empty(), true, cambio(1));
        var actualizada = original.actualizarCompromiso(seguido, cambio(1));
        assertThrows(IllegalArgumentException.class, () -> actualizada.actualizarCompromiso(compromiso, cambio(2)));
        var ajeno = Compromiso.acordar(id(99), "Otro acuerdo", PersonaInvolucrada.estudiante(id(3), "Ficticio"),
                Optional.empty(), false, cambio(0));
        assertThrows(IllegalArgumentException.class, () -> original.actualizarCompromiso(ajeno, cambio(1)));
    }

    @Test
    void noIntroduceCompromisosDesdeUnaVersionFuturaDelRegistro() {
        var futuro = Compromiso.acordar(id(10), "Acuerdo documentado después", PersonaInvolucrada.estudiante(id(3), "Ficticio"),
                Optional.empty(), false, cambio(10));
        assertThrows(IllegalArgumentException.class, () -> realizada(List.of(futuro)));
        assertThrows(IllegalArgumentException.class, () -> realizada(List.of()).agregarCompromiso(futuro, cambio(9)));
    }

    @Test
    void fechaRealFuturaSeValidaEnPeruYSeAdmiteRegistroTardio() {
        assertThrows(IllegalArgumentException.class, () -> Actuacion.realizada(id(20), "Acción futura indebida",
                HOY.plusDays(1), RESPONSABLE, Set.of(), Set.of(), List.of(), cambio(0)));
        var tardia = Actuacion.realizada(id(20), "Acción ejecutada antes, registrada después", HOY.minusDays(10),
                RESPONSABLE, Set.of(), Set.of(), List.of(), cambio(0));
        assertEquals(Optional.of(HOY.minusDays(10)), tardia.fechaEfectiva());
        assertTrue(tardia.revisiones().getFirst().cambio().motivo().isEmpty());
    }

    @Test
    void relatosYAutorResponsableSonDatosNecesarios() {
        assertThrows(IllegalArgumentException.class, () -> Actuacion.realizada(id(20), " ", HOY,
                RESPONSABLE, Set.of(), Set.of(), List.of(), cambio(0)));
        assertThrows(NullPointerException.class, () -> Actuacion.prevista(id(20), "Acción prevista", HOY,
                null, Set.of(), Set.of(), cambio(0)));
    }

    @Test
    void rechazaCambiosConRegistroAnteriorALaVersionVigente() {
        var prevista = prevista().agregarCompromiso(compromiso(Optional.empty(), false), cambio(10));
        assertThrows(IllegalArgumentException.class, () -> prevista.realizar(HOY, "Resultado", cambio(9)));
        assertThrows(IllegalArgumentException.class, () -> prevista.cancelar(motivo(9)));
    }

    @Test
    void coleccionesDeAlcanceCompromisosYRevisionesSonInmutables() {
        var hechos = new HashSet<>(Set.of(id(30)));
        var personas = new HashSet<>(Set.of(id(3)));
        var compromisos = new ArrayList<>(List.of(compromiso(Optional.empty(), false)));
        var actuacion = Actuacion.realizada(id(20), "Acción dirigida a personas conocidas", HOY, RESPONSABLE,
                hechos, personas, compromisos, cambio(0));
        hechos.clear();
        personas.clear();
        compromisos.clear();
        assertEquals(Set.of(id(30)), actuacion.hechosIds());
        assertEquals(Set.of(id(3)), actuacion.personasIds());
        assertEquals(1, actuacion.compromisos().size());
        assertThrows(UnsupportedOperationException.class, () -> actuacion.hechosIds().clear());
        assertThrows(UnsupportedOperationException.class, () -> actuacion.personasIds().clear());
        assertThrows(UnsupportedOperationException.class, () -> actuacion.compromisos().clear());
        assertThrows(UnsupportedOperationException.class, () -> actuacion.revisiones().clear());
        assertThrows(UnsupportedOperationException.class, () -> actuacion.revisiones().getFirst().compromisos().clear());
    }

    private static Actuacion realizada(List<Compromiso> compromisos) {
        return Actuacion.realizada(id(20), "Conversación ficticia realizada con resultado suficiente", HOY,
                RESPONSABLE, Set.of(), Set.of(), compromisos, cambio(0));
    }
    private static Actuacion prevista() {
        return Actuacion.prevista(id(20), "Conversación futura ficticia", HOY.plusDays(5), RESPONSABLE,
                Set.of(), Set.of(), cambio(0));
    }
    private static Compromiso compromiso(Optional<LocalDate> objetivo, boolean seguimiento) {
        return Compromiso.acordar(id(10), "Acuerdo ficticio", PersonaInvolucrada.estudiante(id(3), "Estudiante ficticio"),
                objetivo, seguimiento, cambio(0));
    }
    private static RegistroCambio cambio(int segundos) { return new RegistroCambio(AUTOR, REGISTRO.plusSeconds(segundos), Optional.empty()); }
    private static RegistroCambio motivo(int segundos) { return new RegistroCambio(AUTOR, REGISTRO.plusSeconds(segundos), Optional.of("Cancelación explicada")); }
    private static UUID id(long numero) { return new UUID(0, numero); }
}
