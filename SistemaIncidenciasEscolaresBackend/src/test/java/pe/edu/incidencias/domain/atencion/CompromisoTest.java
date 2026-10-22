package pe.edu.incidencias.domain.atencion;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.incidencia.PersonaInvolucrada;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.incidencia.TipoPersona;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CompromisoTest {
    private static final Instant REGISTRO = Instant.parse("2026-10-21T01:00:00Z"); // 20 de octubre en Perú.
    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);
    private static final UUID AUTOR = id(1);
    private static final PersonaInvolucrada ESTUDIANTE = PersonaInvolucrada.estudiante(id(2), "Estudiante ficticio");

    @Test
    void acuerdoSinPlazoNiSeguimientoNoCreaTareaNiSePresumeCumplido() {
        var compromiso = acordar(Optional.empty(), false);
        assertEquals(Compromiso.Estado.ACORDADO, compromiso.estado());
        assertTrue(compromiso.fechaObjetivo().isEmpty());
        assertFalse(compromiso.pendienteSeguimiento());
        assertFalse(compromiso.vencidoEn(HOY.plusYears(2)));
    }

    @Test
    void plazoConocidoCreaPendienteYVenceSoloDespuesDelDiaAcordado() {
        var compromiso = acordar(Optional.of(HOY), false);
        assertTrue(compromiso.pendienteSeguimiento());
        assertFalse(compromiso.vencidoEn(HOY));
        assertTrue(compromiso.vencidoEn(HOY.plusDays(1)));
        assertEquals(Compromiso.Estado.ACORDADO, compromiso.estado());
    }

    @Test
    void seguimientoExpresoSinPlazoEsPendientePeroNuncaVencidoPorAusenciaDeFecha() {
        var compromiso = acordar(Optional.empty(), true);
        assertTrue(compromiso.pendienteSeguimiento());
        assertFalse(compromiso.vencidoEn(HOY.plusYears(2)));
    }

    @Test
    void personaExternaPuedeAsumirUnCompromisoSinDniNiCuenta() {
        var familiar = new PersonaInvolucrada(id(3), TipoPersona.FAMILIAR, "Familiar referido",
                Optional.empty(), Optional.empty());
        var compromiso = Compromiso.acordar(id(10), "Acuerdo familiar ficticio", familiar, Optional.empty(), false, cambio(0));
        assertSame(familiar, compromiso.asumidoPor());
        assertFalse(compromiso.pendienteSeguimiento());
    }

    @Test
    void cumplimientoObservadoSeRegistraSinModificarElAcuerdoOriginal() {
        var original = acordar(Optional.of(HOY), false);
        var cumplido = original.registrarSeguimiento(HOY, "Se observó el cumplimiento ficticio",
                Optional.of(true), false, cambio(1));
        assertEquals(original.id(), cumplido.id());
        assertEquals(Compromiso.Estado.CUMPLIDO, cumplido.estado());
        assertFalse(cumplido.pendienteSeguimiento());
        assertFalse(cumplido.vencidoEn(HOY.plusDays(3)));
        assertEquals(Compromiso.Estado.ACORDADO, original.estado());
        assertEquals(2, cumplido.revisiones().size());
        assertEquals(original.revisiones().getFirst(), cumplido.revisiones().getFirst());
        assertEquals(Optional.of(HOY), cumplido.revisiones().getLast().fechaObservacion());
        assertEquals(AUTOR, cumplido.revisiones().getLast().cambio().autorId());
    }

    @Test
    void incumplimientoObservadoNoCierraElSeguimientoSiHayContinuidadReal() {
        var compromiso = acordar(Optional.of(HOY.minusDays(1)), false)
                .registrarSeguimiento(HOY, "Incumplimiento observado, se continuará acompañamiento",
                        Optional.of(false), true, cambio(1));
        assertEquals(Compromiso.Estado.NO_CUMPLIDO, compromiso.estado());
        assertTrue(compromiso.pendienteSeguimiento());
        assertTrue(compromiso.vencidoEn(HOY));
    }

    @Test
    void terminarLaComprobacionSinConstatarCumplimientoNoLoInventa() {
        var compromiso = acordar(Optional.of(HOY), true).registrarSeguimiento(HOY,
                "La institución documenta que no continúa esta comprobación", Optional.empty(), false, cambio(1));
        assertEquals(Compromiso.Estado.ACORDADO, compromiso.estado());
        assertFalse(compromiso.pendienteSeguimiento());
        assertFalse(compromiso.vencidoEn(HOY.plusDays(5)));
    }

    @Test
    void cumplimientoYContinuidadDeSeguimientoSonDatosDistintos() {
        var compromiso = acordar(Optional.of(HOY.minusDays(1)), true)
                .registrarSeguimiento(HOY, "Cumplió y se acordó seguimiento de protección", Optional.of(true), true, cambio(1));
        assertEquals(Compromiso.Estado.CUMPLIDO, compromiso.estado());
        assertTrue(compromiso.pendienteSeguimiento());
        assertFalse(compromiso.vencidoEn(HOY.plusDays(1)));
    }

    @Test
    void cancelacionRequiereCausaYConservaElContenidoYAutorAnterior() {
        var original = acordar(Optional.of(HOY), true);
        assertThrows(IllegalArgumentException.class, () -> original.cancelar(cambio(1)));
        var cancelado = original.cancelar(motivo(1));
        assertEquals(Compromiso.Estado.CANCELADO, cancelado.estado());
        assertFalse(cancelado.pendienteSeguimiento());
        assertFalse(cancelado.vencidoEn(HOY.plusDays(5)));
        assertEquals(original.contenido(), cancelado.contenido());
        assertEquals(original.revisiones().getFirst(), cancelado.revisiones().getFirst());
        assertEquals(motivo(1), cancelado.revisiones().getLast().cambio());
        assertThrows(IllegalStateException.class, () -> cancelado.registrarSeguimiento(HOY,
                "Seguimiento improcedente", Optional.empty(), true, cambio(2)));
        assertThrows(IllegalStateException.class, () -> cancelado.cancelar(motivo(2)));
    }

    @Test
    void sustitucionConservaLaIdentidadYAmbosAcuerdosConCausa() {
        var original = acordar(Optional.of(HOY), true);
        var otro = PersonaInvolucrada.estudiante(id(4), "Otro estudiante ficticio");
        assertThrows(IllegalArgumentException.class, () -> original.sustituir("Nuevo acuerdo", otro,
                Optional.empty(), false, cambio(1)));
        var sustituto = original.sustituir("Nuevo acuerdo ficticio", otro, Optional.empty(), false, motivo(1));
        assertEquals(original.id(), sustituto.id());
        assertEquals(2, sustituto.revisiones().size());
        assertEquals(ESTUDIANTE, sustituto.revisiones().getFirst().asumidoPor());
        assertEquals(otro, sustituto.asumidoPor());
        assertEquals("Nuevo acuerdo ficticio", sustituto.contenido());
        assertFalse(sustituto.pendienteSeguimiento());
        assertTrue(sustituto.fechaObjetivo().isEmpty());
        assertTrue(sustituto.esContinuacionDe(original));
    }

    @Test
    void observacionesNoPuedenEstarEnElFuturoDePeru() {
        var compromiso = acordar(Optional.empty(), false);
        assertThrows(IllegalArgumentException.class, () -> compromiso.registrarSeguimiento(HOY.plusDays(1),
                "El día UTC ya cambió pero el de Perú no", Optional.of(true), false, cambio(1)));
    }

    @Test
    void registroTardioDeSeguimientoEsValidoSinJustificacionAdicional() {
        var compromiso = acordar(Optional.empty(), true).registrarSeguimiento(HOY.minusDays(10),
                "Acción observada anteriormente y registrada después", Optional.empty(), true, cambio(1));
        assertEquals(Optional.of(HOY.minusDays(10)), compromiso.revisiones().getLast().fechaObservacion());
        assertTrue(compromiso.revisiones().getLast().cambio().motivo().isEmpty());
    }

    @Test
    void rechazaRetrocesoDelRegistroYDelSeguimientoYaDocumentado() {
        var seguido = acordar(Optional.empty(), true).registrarSeguimiento(HOY.minusDays(1),
                "Observación ficticia", Optional.empty(), true, cambio(10));
        assertThrows(IllegalArgumentException.class, () -> seguido.registrarSeguimiento(HOY,
                "Cambio con registro anterior", Optional.empty(), true, cambio(9)));
        assertThrows(IllegalArgumentException.class, () -> seguido.registrarSeguimiento(HOY.minusDays(2),
                "La secuencia observada retrocede", Optional.empty(), true, cambio(11)));
    }

    @Test
    void plazoPuedeSerPasadoOFuturoSinInventarLaFechaDeUnAcuerdo() {
        assertTrue(acordar(Optional.of(HOY.minusDays(30)), false).pendienteSeguimiento());
        assertTrue(acordar(Optional.of(HOY.plusDays(30)), false).pendienteSeguimiento());
    }

    @Test
    void contenidoPersonaYObservacionSonObligatoriosPeroNoDocumentos() {
        assertThrows(IllegalArgumentException.class, () -> Compromiso.acordar(id(10), " ", ESTUDIANTE,
                Optional.empty(), false, cambio(0)));
        assertThrows(NullPointerException.class, () -> Compromiso.acordar(id(10), "Acuerdo", null,
                Optional.empty(), false, cambio(0)));
        assertThrows(IllegalArgumentException.class, () -> acordar(Optional.empty(), false)
                .registrarSeguimiento(HOY, " ", Optional.empty(), false, cambio(1)));
    }

    @Test
    void historialEsInmutableYRechazaDescendenciaDeOtraIdentidad() {
        var original = acordar(Optional.empty(), false);
        assertThrows(UnsupportedOperationException.class, () -> original.revisiones().clear());
        var otro = Compromiso.acordar(id(11), original.contenido(), ESTUDIANTE, Optional.empty(), false, cambio(0));
        assertFalse(otro.esContinuacionDe(original));
    }

    private Compromiso acordar(Optional<LocalDate> plazo, boolean seguimiento) {
        return Compromiso.acordar(id(10), "Acuerdo ficticio suficiente", ESTUDIANTE, plazo, seguimiento, cambio(0));
    }
    private static RegistroCambio cambio(int segundos) { return new RegistroCambio(AUTOR, REGISTRO.plusSeconds(segundos), Optional.empty()); }
    private static RegistroCambio motivo(int segundos) { return new RegistroCambio(AUTOR, REGISTRO.plusSeconds(segundos), Optional.of("Cambio importante explicado")); }
    private static UUID id(long numero) { return new UUID(0, numero); }
}
