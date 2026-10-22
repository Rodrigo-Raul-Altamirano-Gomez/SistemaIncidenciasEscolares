package pe.edu.incidencias.domain.atencion;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DerivacionTest {
    private static final UUID DERIVACION = id(1);
    private static final UUID HECHO = id(2);
    private static final UUID AUTOR = id(3);
    private static final Instant REGISTRO = Instant.parse("2026-10-21T01:00:00Z"); // 20 de octubre en Perú
    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);
    private static final Derivacion.Destino FISCALIA = new Derivacion.Destino(Derivacion.TipoDestino.FISCALIA, "Fiscalía ficticia");

    @Test
    void preparacionNoEsEjecucionNiActivaCondicionDeCierreLocal() {
        var preparada = preparada();
        assertEquals(Derivacion.Estado.PREPARADA, preparada.estado());
        assertTrue(preparada.fechaEfectiva().isEmpty());
        assertTrue(preparada.referencia().isEmpty());
        assertFalse(preparada.esViolenciaFamiliarEfectivaAFiscalia());
        assertEquals(1, preparada.revisiones().size());
    }

    @Test
    void puedeRegistrarseEjecucionDirectaSinPreparacionReferenciaNiArchivo() {
        var efectuada = efectuada(Derivacion.Supuesto.VIOLENCIA_FAMILIAR, FISCALIA, HOY.minusDays(2));
        assertEquals(Derivacion.Estado.EFECTUADA, efectuada.estado());
        assertEquals(Optional.of(HOY.minusDays(2)), efectuada.fechaEfectiva());
        assertTrue(efectuada.esViolenciaFamiliarEfectivaAFiscalia());
        assertTrue(efectuada.referencia().isEmpty());
        assertTrue(efectuada.revisiones().getFirst().cambio().motivo().isEmpty());
        assertEquals(Set.of(HECHO), efectuada.hechosIds());
    }

    @Test
    void efectuarLaPreparadaConservaIdentidadPreparacionYAutoresSinDuplicarDerivacion() {
        var preparada = preparada();
        var cambio = new RegistroCambio(id(4), REGISTRO.plusSeconds(1), Optional.empty());
        var efectuada = preparada.efectuar(HOY, "Se comunicó el objeto de atención a Fiscalía", Optional.empty(), cambio);
        assertEquals(preparada.id(), efectuada.id());
        assertEquals(preparada.destino(), efectuada.destino());
        assertEquals(preparada.hechosIds(), efectuada.hechosIds());
        assertEquals(2, efectuada.revisiones().size());
        assertEquals(preparada.revisiones().getFirst(), efectuada.revisiones().getFirst());
        assertEquals(AUTOR, efectuada.revisiones().getFirst().cambio().autorId());
        assertEquals(id(4), efectuada.revisiones().getLast().cambio().autorId());
        assertTrue(efectuada.esViolenciaFamiliarEfectivaAFiscalia());
        assertFalse(preparada.esViolenciaFamiliarEfectivaAFiscalia());
    }

    @Test
    void destinoFiscaliaNoImplicaViolenciaFamiliarNiOtraEntidadEquivaleAFiscalia() {
        var otroSupuesto = efectuada(Derivacion.Supuesto.OTRO, FISCALIA, HOY);
        var otraEntidad = efectuada(Derivacion.Supuesto.VIOLENCIA_FAMILIAR,
                new Derivacion.Destino(Derivacion.TipoDestino.OTRA_ENTIDAD, "Policía ficticia"), HOY);
        var nombreParecido = efectuada(Derivacion.Supuesto.VIOLENCIA_FAMILIAR,
                new Derivacion.Destino(Derivacion.TipoDestino.OTRA_ENTIDAD, "Oficina denominada Fiscalía"), HOY);
        assertFalse(otroSupuesto.esViolenciaFamiliarEfectivaAFiscalia());
        assertFalse(otraEntidad.esViolenciaFamiliarEfectivaAFiscalia());
        assertFalse(nombreParecido.esViolenciaFamiliarEfectivaAFiscalia());
    }

    @Test
    void completarReferenciaDespuesDeEfectuarNoCambiaFechaDestinoNiEstado() {
        var anterior = efectuada(Derivacion.Supuesto.VIOLENCIA_FAMILIAR, FISCALIA, HOY);
        var actualizado = anterior.completarReferencia("Cargo ficticio 001", cambio(2));
        assertEquals(Optional.of("Cargo ficticio 001"), actualizado.referencia());
        assertTrue(anterior.referencia().isEmpty());
        assertEquals(anterior.id(), actualizado.id());
        assertEquals(anterior.destino(), actualizado.destino());
        assertEquals(anterior.fechaEfectiva(), actualizado.fechaEfectiva());
        assertEquals(Derivacion.Estado.EFECTUADA, actualizado.estado());
        assertEquals(2, actualizado.revisiones().size());
        assertEquals(cambio(2).fecha(), actualizado.ultimaModificacion());
    }

    @Test
    void referenciaConocidaEnPreparacionSeConservaAlOmitirlaEnLaEjecucion() {
        var conReferencia = preparada().completarReferencia("Referencia ficticia conocida", cambio(1));
        var efectuada = conReferencia.efectuar(HOY, "Se efectuó la derivación", Optional.empty(), cambio(2));
        assertEquals(Optional.of("Referencia ficticia conocida"), efectuada.referencia());
        assertEquals(3, efectuada.revisiones().size());
    }

    @Test
    void corregirReferenciaPreservaElValorAnteriorYNoExigeMotivoRedundante() {
        var anterior = preparada().completarReferencia("Referencia errada ficticia", cambio(1));
        var corregido = anterior.completarReferencia("Referencia correcta ficticia", cambio(2));
        assertEquals(Optional.of("Referencia errada ficticia"), corregido.revisiones().get(1).referencia());
        assertEquals(Optional.of("Referencia correcta ficticia"), corregido.referencia());
        assertTrue(corregido.revisiones().getLast().cambio().motivo().isEmpty());
        assertEquals(Derivacion.Estado.PREPARADA, corregido.estado());
    }

    @Test
    void ejecucionNoPuedeTenerFechaFuturaAunqueUtcEsteEnElDiaSiguiente() {
        assertThrows(IllegalArgumentException.class,
                () -> efectuada(Derivacion.Supuesto.VIOLENCIA_FAMILIAR, FISCALIA, HOY.plusDays(1)));
        assertThrows(IllegalArgumentException.class,
                () -> preparada().efectuar(HOY.plusDays(1), "Envío ficticio", Optional.empty(), cambio(1)));
    }

    @Test
    void unCambioNoPuedePrecederAlRegistroVigente() {
        var actualizado = preparada().completarReferencia("Referencia ficticia", cambio(2));
        assertThrows(IllegalArgumentException.class, () -> actualizado.completarReferencia("Otra", cambio(1)));
        assertThrows(IllegalArgumentException.class, () -> actualizado.efectuar(HOY, "Se envió", Optional.empty(), cambio(1)));
    }

    @Test
    void retryExactoDeEjecucionNoAgregaRevisionesPeroOtraEjecucionSeRechaza() {
        var efectuada = preparada().efectuar(HOY, "Se efectuó", Optional.empty(), cambio(1));
        assertSame(efectuada, efectuada.efectuar(HOY, "Se efectuó", Optional.empty(), cambio(1)));
        assertThrows(IllegalStateException.class,
                () -> efectuada.efectuar(HOY.minusDays(1), "Otra ejecución", Optional.empty(), cambio(2)));
        assertEquals(2, efectuada.revisiones().size());
    }

    @Test
    void repetirMismaReferenciaNoAgregaHistoriaArtificial() {
        var anterior = preparada().completarReferencia("Referencia ficticia", cambio(1));
        assertSame(anterior, anterior.completarReferencia("Referencia ficticia", cambio(2)));
    }

    @Test
    void coberturaYRevisionesSonCopiasInmutables() {
        var hechos = new HashSet<>(Set.of(HECHO));
        var derivacion = Derivacion.preparada(DERIVACION, hechos, Derivacion.Supuesto.OTRO, FISCALIA,
                "Se prepara comunicación", Optional.empty(), cambio(0));
        hechos.add(id(99));
        assertEquals(Set.of(HECHO), derivacion.hechosIds());
        assertThrows(UnsupportedOperationException.class, () -> derivacion.hechosIds().clear());
        assertThrows(UnsupportedOperationException.class, () -> derivacion.revisiones().clear());
    }

    @Test
    void exigeDestinoRelatoYCoberturaConocidaSinExigirNumeroOArchivo() {
        assertThrows(IllegalArgumentException.class, () -> new Derivacion.Destino(Derivacion.TipoDestino.FISCALIA, " "));
        assertThrows(IllegalArgumentException.class, () -> Derivacion.preparada(DERIVACION, Set.of(),
                Derivacion.Supuesto.OTRO, FISCALIA, "Preparación", Optional.empty(), cambio(0)));
        assertThrows(IllegalArgumentException.class, () -> Derivacion.preparada(DERIVACION, Set.of(HECHO),
                Derivacion.Supuesto.OTRO, FISCALIA, " ", Optional.empty(), cambio(0)));
        assertThrows(IllegalArgumentException.class, () -> preparada().completarReferencia(" ", cambio(1)));
        assertThrows(IllegalArgumentException.class, () -> Derivacion.preparada(DERIVACION, Set.of(HECHO),
                Derivacion.Supuesto.OTRO, FISCALIA, "Preparación", Optional.of(" "), cambio(0)));
    }

    @Test
    void revisionNoPuedeAfirmarPreparadaConFechaEfectivaNiEfectuadaSinFecha() {
        assertThrows(IllegalArgumentException.class, () -> new Derivacion.Revision(Derivacion.Estado.PREPARADA,
                Optional.of(HOY), "Preparación", Optional.empty(), cambio(0)));
        assertThrows(IllegalArgumentException.class, () -> new Derivacion.Revision(Derivacion.Estado.EFECTUADA,
                Optional.empty(), "Ejecución", Optional.empty(), cambio(0)));
    }

    private static Derivacion preparada() {
        return Derivacion.preparada(DERIVACION, Set.of(HECHO), Derivacion.Supuesto.VIOLENCIA_FAMILIAR, FISCALIA,
                "Preparación ficticia de derivación", Optional.empty(), cambio(0));
    }
    private static Derivacion efectuada(Derivacion.Supuesto supuesto, Derivacion.Destino destino, LocalDate fecha) {
        return Derivacion.efectuada(DERIVACION, Set.of(HECHO), supuesto, destino,
                "Comunicación efectuada por los medios institucionales", Optional.empty(), fecha, cambio(0));
    }
    private static RegistroCambio cambio(int segundos) {
        return new RegistroCambio(AUTOR, REGISTRO.plusSeconds(segundos), Optional.empty());
    }
    private static UUID id(long valor) { return new UUID(0, valor); }
}
