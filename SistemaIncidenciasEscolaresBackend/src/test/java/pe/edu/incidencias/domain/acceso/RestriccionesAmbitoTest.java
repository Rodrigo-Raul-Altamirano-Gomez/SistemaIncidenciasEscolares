package pe.edu.incidencias.domain.acceso;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.catalogo.ClasificacionCategoria;
import pe.edu.incidencias.domain.catalogo.Gravedad;
import pe.edu.incidencias.domain.catalogo.NaturalezaCategoria;
import pe.edu.incidencias.domain.incidencia.*;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RestriccionesAmbitoTest {
    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");
    private static final UUID OPERADOR = id(1);
    private static final UUID EXCLUIDO = id(2);
    private static final AmbitoReserva HECHO = new AmbitoReserva(Set.of(id(10)), Set.of(id(11)), Set.of());

    @Test
    void ambitoNecesitaReferenciasYHaceCopiasInmutablesDeLasTresClases() {
        assertThrows(IllegalArgumentException.class, () -> new AmbitoReserva(Set.of(), Set.of(), Set.of()));
        assertThrows(NullPointerException.class, () -> new AmbitoReserva(null, Set.of(), Set.of()));
        var hechos = new HashSet<>(Set.of(id(10)));
        var reportes = new HashSet<>(Set.of(id(11)));
        var casos = new HashSet<>(Set.of(id(12)));
        var ambito = new AmbitoReserva(hechos, reportes, casos);
        hechos.clear();
        reportes.clear();
        casos.clear();
        assertEquals(Set.of(id(10)), ambito.hechosIds());
        assertEquals(Set.of(id(11)), ambito.reportesIds());
        assertEquals(Set.of(id(12)), ambito.casosIds());
        assertThrows(UnsupportedOperationException.class, () -> ambito.hechosIds().clear());
        assertThrows(UnsupportedOperationException.class, () -> ambito.reportesIds().clear());
        assertThrows(UnsupportedOperationException.class, () -> ambito.casosIds().clear());
    }

    @Test
    void referenciasSeCruzanSoloDentroDeLaMismaClaseDeIdentificador() {
        var soloHecho = new AmbitoReserva(Set.of(id(10)), Set.of(), Set.of());
        var soloReporte = new AmbitoReserva(Set.of(), Set.of(id(11)), Set.of());
        var soloCaso = new AmbitoReserva(Set.of(), Set.of(), Set.of(id(12)));
        assertTrue(HECHO.seCruzaCon(soloHecho));
        assertTrue(HECHO.seCruzaCon(soloReporte));
        assertFalse(HECHO.seCruzaCon(soloCaso));
        assertFalse(soloHecho.seCruzaCon(new AmbitoReserva(Set.of(), Set.of(id(10)), Set.of())));
        assertTrue(soloCaso.seCruzaCon(new AmbitoReserva(Set.of(), Set.of(), Set.of(id(12)))));
    }

    @Test
    void unionNoPierdeLasReferenciasOriginalesNiDuplicaIdentificadores() {
        var otro = new AmbitoReserva(Set.of(id(10), id(20)), Set.of(id(21)), Set.of(id(22)));
        var unido = HECHO.unir(otro);
        assertEquals(Set.of(id(10), id(20)), unido.hechosIds());
        assertEquals(Set.of(id(11), id(21)), unido.reportesIds());
        assertEquals(Set.of(id(22)), unido.casosIds());
        assertEquals(HECHO, HECHO.unir(HECHO));
        assertEquals(Set.of(id(10)), HECHO.hechosIds());
    }

    @Test
    void ambitoDeHechoConservaFuenteAnuladaEIdentidadEstableAlCambiarPrincipal() {
        var principal = reporte(100);
        var duplicado = reporte(101);
        var original = Hecho.de(principal).vincularDuplicado(duplicado, cambio(1));
        var anulado = original.actualizarReporte(principal.anularPorError(cambio(2)));
        var corregido = anulado.cambiarPrincipal(duplicado.id(), cambio(3));
        var ambito = AmbitoReserva.deHecho(corregido);
        assertEquals(Set.of(original.id()), ambito.hechosIds());
        assertEquals(Set.of(principal.id(), duplicado.id()), ambito.reportesIds());
        assertTrue(ambito.casosIds().isEmpty());
        assertEquals(AmbitoReserva.deHecho(original), ambito);
    }

    @Test
    void exclusionSeAplicaSoloAlUsuarioYAmbitoRelacionados() {
        var restricciones = RestriccionesAmbito.vacias().excluir(id(100), EXCLUIDO, HECHO, cambio(1));
        assertTrue(restricciones.estaExcluido(EXCLUIDO, HECHO));
        assertFalse(restricciones.estaExcluido(OPERADOR, HECHO));
        var otroHecho = new AmbitoReserva(Set.of(id(30)), Set.of(id(31)), Set.of());
        assertFalse(restricciones.estaExcluido(EXCLUIDO, otroHecho));
        assertTrue(restricciones.estaExcluido(EXCLUIDO,
                new AmbitoReserva(Set.of(), Set.of(id(11)), Set.of())));
    }

    @Test
    void exclusionDeFuenteSeMantieneAlUnirDuplicadosOAgregarUnaAtencion() {
        var restricciones = RestriccionesAmbito.vacias().excluir(id(100), EXCLUIDO,
                new AmbitoReserva(Set.of(), Set.of(id(11)), Set.of()), cambio(1));
        var casoCompartido = new AmbitoReserva(Set.of(id(40)), Set.of(id(41)), Set.of(id(42)));
        assertTrue(restricciones.estaExcluido(EXCLUIDO, HECHO.unir(casoCompartido)));
        assertFalse(restricciones.estaExcluido(EXCLUIDO, casoCompartido));
    }

    @Test
    void altaYRetiroConservanMotivoAutorFechaYLaInstantaneaAnterior() {
        var vacias = RestriccionesAmbito.vacias();
        var aplicadas = vacias.excluir(id(100), EXCLUIDO, HECHO, cambio(1));
        var retiradas = aplicadas.retirar(id(100), cambio(2));
        assertTrue(vacias.exclusiones().isEmpty());
        assertTrue(aplicadas.estaExcluido(EXCLUIDO, HECHO));
        assertFalse(retiradas.estaExcluido(EXCLUIDO, HECHO));
        var constancia = retiradas.exclusiones().getFirst();
        assertEquals(cambio(1), constancia.registro());
        assertEquals(Optional.of(cambio(2)), constancia.retiro());
        assertFalse(constancia.activa());
        assertEquals(List.of(RestriccionesAmbito.Operacion.EXCLUIR, RestriccionesAmbito.Operacion.RETIRAR),
                retiradas.historial().stream().map(RestriccionesAmbito.Evento::operacion).toList());
        assertEquals(id(100), retiradas.historial().getLast().exclusionId());
    }

    @Test
    void retirarUnaExclusionNoRetiraOtraFuenteActivaDeLaMismaPersona() {
        var ambas = RestriccionesAmbito.vacias().excluir(id(100), EXCLUIDO, HECHO, cambio(1))
                .excluir(id(101), EXCLUIDO, HECHO, cambio(2));
        var soloUna = ambas.retirar(id(100), cambio(3));
        assertTrue(soloUna.estaExcluido(EXCLUIDO, HECHO));
        assertEquals(1, soloUna.exclusiones().stream().filter(RestriccionesAmbito.Exclusion::activa).count());
        var ninguna = soloUna.retirar(id(101), cambio(4));
        assertFalse(ninguna.estaExcluido(EXCLUIDO, HECHO));
        assertEquals(2, ninguna.exclusiones().size());
    }

    @Test
    void autoexclusionVoluntariaSeAdmitePeroElPropioRetiroSeProhibe() {
        var porSiMismo = new RegistroCambio(EXCLUIDO, AHORA, Optional.of("Declaro mi conflicto"));
        var restricciones = RestriccionesAmbito.vacias().excluir(id(100), EXCLUIDO, HECHO, porSiMismo);
        assertTrue(restricciones.estaExcluido(EXCLUIDO, HECHO));
        assertThrows(IllegalArgumentException.class, () -> restricciones.retirar(id(100),
                new RegistroCambio(EXCLUIDO, AHORA.plusSeconds(1), Optional.of("Pretendo retirarla"))));
    }

    @Test
    void exclusionYRetiroExigenCausaBreve() {
        var sinCausa = new RegistroCambio(OPERADOR, AHORA, Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> RestriccionesAmbito.vacias().excluir(id(100),
                EXCLUIDO, HECHO, sinCausa));
        var aplicada = RestriccionesAmbito.vacias().excluir(id(100), EXCLUIDO, HECHO, cambio(1));
        assertThrows(IllegalArgumentException.class, () -> aplicada.retirar(id(100),
                new RegistroCambio(OPERADOR, AHORA.plusSeconds(2), Optional.empty())));
    }

    @Test
    void reintentarElMismoComandoNoDuplicaHistoriaNiReviveUnaExclusionRetirada() {
        var aplicada = RestriccionesAmbito.vacias().excluir(id(100), EXCLUIDO, HECHO, cambio(1));
        assertSame(aplicada, aplicada.excluir(id(100), EXCLUIDO, HECHO, cambio(1)));
        var retirada = aplicada.retirar(id(100), cambio(2));
        assertSame(retirada, retirada.retirar(id(100), cambio(2)));
        assertSame(retirada, retirada.excluir(id(100), EXCLUIDO, HECHO, cambio(1)));
        assertEquals(2, retirada.historial().size());
        assertFalse(retirada.estaExcluido(EXCLUIDO, HECHO));
    }

    @Test
    void identificadoresNoSeReutilizanParaCambiarDestinatarioAmbitoOAutor() {
        var aplicada = RestriccionesAmbito.vacias().excluir(id(100), EXCLUIDO, HECHO, cambio(1));
        assertThrows(IllegalArgumentException.class, () -> aplicada.excluir(id(100), id(3), HECHO, cambio(1)));
        assertThrows(IllegalArgumentException.class, () -> aplicada.excluir(id(100), EXCLUIDO,
                new AmbitoReserva(Set.of(id(40)), Set.of(), Set.of()), cambio(1)));
        assertThrows(IllegalArgumentException.class, () -> aplicada.excluir(id(100), EXCLUIDO, HECHO, cambio(2)));
    }

    @Test
    void cambiosAnterioresYRetiroDesconocidoSeRechazan() {
        var aplicada = RestriccionesAmbito.vacias().excluir(id(100), EXCLUIDO, HECHO, cambio(2));
        assertThrows(IllegalArgumentException.class, () -> aplicada.retirar(id(100), cambio(1)));
        assertThrows(IllegalArgumentException.class, () -> aplicada.excluir(id(101), id(3), HECHO, cambio(1)));
        assertThrows(IllegalArgumentException.class, () -> aplicada.retirar(id(999), cambio(3)));
        var retirada = aplicada.retirar(id(100), cambio(3));
        assertThrows(IllegalStateException.class, () -> retirada.retirar(id(100), cambio(4)));
    }

    @Test
    void historialYColeccionesSonInmutables() {
        var aplicada = RestriccionesAmbito.vacias().excluir(id(100), EXCLUIDO, HECHO, cambio(1));
        assertThrows(UnsupportedOperationException.class, () -> aplicada.exclusiones().clear());
        assertThrows(UnsupportedOperationException.class, () -> aplicada.historial().clear());
    }

    @Test
    void noExisteUnaMutacionPublicaQueOmitaLaPoliticaDeAutorizacion() throws ReflectiveOperationException {
        var alta = RestriccionesAmbito.class.getDeclaredMethod("excluir", UUID.class, UUID.class,
                AmbitoReserva.class, RegistroCambio.class);
        var retiro = RestriccionesAmbito.class.getDeclaredMethod("retirar", UUID.class, RegistroCambio.class);
        assertFalse(Modifier.isPublic(alta.getModifiers()));
        assertFalse(Modifier.isPublic(retiro.getModifiers()));
        assertEquals(0, RestriccionesAmbito.class.getConstructors().length);
    }

    private static ReporteIncidencia reporte(int numero) {
        var clasificacion = new ClasificacionCategoria(id(50), "Hecho ficticio", NaturalezaCategoria.OTRA_CONVIVENCIA,
                Gravedad.POR_CLASIFICAR, false);
        var persona = PersonaInvolucrada.estudiante(id(51), "Estudiante ficticio");
        var datos = new DatosReporte(FechaHecho.exacta(LocalDate.of(2026, 10, 20)), 2026, clasificacion,
                "Relato ficticio para comprobar las referencias de reserva",
                List.of(new Participacion(persona, PapelParticipacion.TESTIGO, Optional.empty())), false,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return ReporteIncidencia.registrar(id(numero), datos, id(numero + 1000), AHORA);
    }

    private static RegistroCambio cambio(int segundos) {
        return new RegistroCambio(OPERADOR, AHORA.plusSeconds(segundos), Optional.of("Conflicto o retiro motivado ficticio"));
    }

    private static UUID id(long numero) { return new UUID(0, numero); }
}
