package pe.edu.incidencias.domain.alerta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pe.edu.incidencias.domain.catalogo.*;
import pe.edu.incidencias.domain.incidencia.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraReiteracionTest {
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();
    private static final UUID TOE = UUID.randomUUID();
    private static final Instant REGISTRO = Instant.parse("2026-10-21T00:00:00Z"); // 20 de octubre, Perú
    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);
    private final Categoria categoria = categoria(NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE, OptionalInt.of(3));
    private final ParametrosReiteracion parametros = ParametrosReiteracion.sinConfigurar(TOE, REGISTRO)
            .configurarVentana(10, TOE, REGISTRO);

    @Test
    void ventanaIncluyeOnceYVeintePeroNoDiezNiNecesitaCorroboracionCotidiana() {
        var hechos = List.of(cotidiano(10), cotidiano(11), cotidiano(15), cotidiano(20));
        var resultado = evaluar(hechos);

        assertTrue(resultado.alcanzaUmbral());
        var fundamento = resultado.fundamento().orElseThrow();
        assertEquals(3, fundamento.cantidad());
        assertEquals(HOY.minusDays(9), fundamento.inicioInclusive());
        assertEquals(HOY, fundamento.finInclusive());
        assertEquals(3, fundamento.cantidad(PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA));
        assertEquals(0, fundamento.cantidad(PapelParticipacion.ATRIBUCION_CORROBORADA));
        assertEquals(1, hechos.getFirst().principal().revisiones().size());
    }

    @ParameterizedTest
    @EnumSource(value = PapelParticipacion.class, names = {"AFECTADO", "TESTIGO", "OTRO", "DESCONOCIDO", "ATRIBUCION_DESCARTADA"})
    void participacionesSinAtribucionNoIncrementanResponsabilidad(PapelParticipacion papel) {
        var hecho = hecho(categoria, FechaHecho.exacta(HOY), papel);
        assertEquals(0, evaluar(List.of(hecho)).fundamento().orElseThrow().cantidad());
    }

    @Test
    void unaFechaAproximadaNoCuentaAunqueLaReferenciaCaigaEnLaVentana() {
        var hecho = hecho(categoria, FechaHecho.aproximada(HOY), PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA);
        assertEquals(0, evaluar(List.of(hecho)).fundamento().orElseThrow().cantidad());
    }

    @Test
    void cargaTardiaUsaLaFechaDelHechoYNoLaDeRegistro() {
        var antiguo = hecho(categoria, FechaHecho.exacta(LocalDate.of(2025, 10, 20)),
                PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA);
        assertEquals(0, evaluar(List.of(antiguo)).fundamento().orElseThrow().cantidad());
    }

    @Test
    void ventanaSeDesplazaSinSimularUnAvisoPasado() {
        var hechos = List.of(cotidiano(11), cotidiano(15), cotidiano(20));
        var resultado = CalculadoraReiteracion.evaluar(ESTUDIANTE, categoria, parametros, HOY.plusDays(1), hechos);
        assertFalse(resultado.alcanzaUmbral());
        assertEquals(2, resultado.fundamento().orElseThrow().cantidad());
    }

    @Test
    void participanteSoloEnDuplicadoCuentaUnaVezYNoRecibePapelDeOtraPersona() {
        var otro = new Participacion(PersonaInvolucrada.estudiante(UUID.randomUUID(), "Alfa ficticio"),
                PapelParticipacion.AFECTADO, Optional.empty());
        var principal = ReporteIncidencia.registrar(UUID.randomUUID(), datos(categoria, FechaHecho.exacta(HOY), List.of(otro)), AUTOR, REGISTRO);
        var duplicado = cotidiano(20).principal();
        var conjunto = Hecho.de(principal).vincularDuplicado(duplicado, cambio());
        assertEquals(1, evaluar(List.of(conjunto)).fundamento().orElseThrow().cantidad());
        var resultadoOtro = CalculadoraReiteracion.evaluar(otro.persona().id(), categoria, parametros, HOY, List.of(conjunto));
        assertEquals(0, resultadoOtro.fundamento().orElseThrow().cantidad());
        assertEquals(2, conjunto.reportes().size());
    }

    @Test
    void versionesCoherentesDeLaMismaPersonaEnDuplicadosNoDuplicanOcurrencias() {
        var principal = cotidiano(20);
        var conjunto = principal.vincularDuplicado(cotidiano(20).principal(), cambio());
        assertEquals(1, evaluar(List.of(conjunto)).fundamento().orElseThrow().cantidad());
        assertEquals(1, conjunto.participaciones().size());
        assertEquals(2, conjunto.participaciones().getFirst().aportes().size());
    }

    @Test
    void discrepanciaDePapelExcluyeHastaAclararSinAlterarOriginales() {
        var principal = cotidiano(20);
        var testigo = hecho(categoria, FechaHecho.exacta(HOY), PapelParticipacion.TESTIGO);
        var conjunto = principal.vincularDuplicado(testigo.principal(), cambio());
        assertEquals(0, evaluar(List.of(conjunto)).fundamento().orElseThrow().cantidad());

        var aclarado = conjunto.aclararParticipacion(ESTUDIANTE, PapelParticipacion.ATRIBUCION_CORROBORADA,
                "Resultado ficticio de la intervención", cambio());
        var fundamento = evaluar(List.of(aclarado)).fundamento().orElseThrow();
        assertEquals(1, fundamento.cantidad(PapelParticipacion.ATRIBUCION_CORROBORADA));
        assertEquals(PapelParticipacion.TESTIGO, aclarado.reportes().getLast().datos().participaciones().getFirst().papel());
    }

    @Test
    void presuntasResponsabilidadesDeViolenciaSeDistinguenDeLasCorroboradas() {
        var violencia = categoria(NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.GRAVE, OptionalInt.of(2));
        var presunto = hecho(violencia, FechaHecho.exacta(HOY), PapelParticipacion.PRESUNTA_RESPONSABILIDAD);
        var corroborado = hecho(violencia, FechaHecho.exacta(HOY.minusDays(1)), PapelParticipacion.ATRIBUCION_CORROBORADA);
        var resultado = CalculadoraReiteracion.evaluar(ESTUDIANTE, violencia, parametros, HOY, List.of(presunto, corroborado));

        assertTrue(presunto.requiereAvisoInmediato());
        assertTrue(resultado.alcanzaUmbral());
        assertEquals(1, resultado.fundamento().orElseThrow().cantidad(PapelParticipacion.PRESUNTA_RESPONSABILIDAD));
        assertEquals(1, resultado.fundamento().orElseThrow().cantidad(PapelParticipacion.ATRIBUCION_CORROBORADA));
    }

    @Test
    void responsabilidadDelFamiliarNoSeTransfiereAlEstudianteAfectado() {
        var violencia = categoria(NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.POR_CLASIFICAR, OptionalInt.of(1));
        var familiar = new PersonaInvolucrada(UUID.randomUUID(), TipoPersona.FAMILIAR, "Familiar referido",
                Optional.empty(), Optional.empty());
        var datos = datos(violencia, FechaHecho.exacta(HOY), List.of(
                new Participacion(PersonaInvolucrada.estudiante(ESTUDIANTE, "Estudiante ficticio"), PapelParticipacion.AFECTADO, Optional.empty()),
                new Participacion(familiar, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, Optional.empty())));
        var hecho = Hecho.de(ReporteIncidencia.registrar(UUID.randomUUID(), datos, AUTOR, REGISTRO));
        assertEquals(0, CalculadoraReiteracion.evaluar(ESTUDIANTE, violencia, parametros, HOY, List.of(hecho))
                .fundamento().orElseThrow().cantidad());
        assertTrue(hecho.requiereAvisoInmediato());
    }

    @Test
    void ausenciaDeParametrosEsExplicitaYNoSuprimeAvisoPorGravedad() {
        var violencia = categoria(NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.GRAVE, OptionalInt.empty());
        var hecho = hecho(violencia, FechaHecho.exacta(HOY), PapelParticipacion.DESCONOCIDO);
        var resultado = CalculadoraReiteracion.evaluar(ESTUDIANTE, violencia,
                ParametrosReiteracion.sinConfigurar(TOE, REGISTRO), HOY, List.of(hecho));
        assertFalse(resultado.alcanzaUmbral());
        assertTrue(resultado.fundamento().isEmpty());
        assertEquals(2, resultado.configuracion().parametrosFaltantes().size());
        assertTrue(hecho.requiereAvisoInmediato());
    }

    @Test
    void anularOCorregirPapelRecalculaSinMutarElFundamentoYaEmitido() {
        var hecho = cotidiano(20);
        var anterior = evaluar(List.of(hecho)).fundamento().orElseThrow();
        var anulado = hecho.actualizarReporte(hecho.principal().anularPorError(
                new RegistroCambio(TOE, REGISTRO, Optional.of("Registro de ensayo creado por error"))));
        assertEquals(0, evaluar(List.of(anulado)).fundamento().orElseThrow().cantidad());
        var descartado = hecho.aclararParticipacion(ESTUDIANTE, PapelParticipacion.ATRIBUCION_DESCARTADA,
                "La atención descartó la atribución comunicada", cambio());
        assertEquals(0, evaluar(List.of(descartado)).fundamento().orElseThrow().cantidad());
        assertEquals(1, anterior.cantidad());
        assertEquals(10, anterior.ventanaDias());
        assertEquals(3, anterior.umbral());
        assertThrows(UnsupportedOperationException.class, () -> anterior.atribuciones().clear());
    }

    @Test
    void rechazaListasQuePodrianContarElMismoReporteEnDosHechos() {
        var uno = cotidiano(20);
        var dos = cotidiano(20).vincularDuplicado(uno.principal(), cambio());
        assertThrows(IllegalArgumentException.class, () -> evaluar(List.of(uno, uno)));
        assertThrows(IllegalArgumentException.class, () -> evaluar(List.of(uno, dos)));
    }

    @Test
    void discrepanciaDeFechaSeExponeYNoSeResuelveSilenciosamente() {
        var conjunto = cotidiano(20).vincularDuplicado(cotidiano(19).principal(), cambio());
        var resultado = evaluar(List.of(conjunto));
        assertEquals(0, resultado.fundamento().orElseThrow().cantidad());
        assertEquals(List.of(conjunto.id()), resultado.hechosConDatosDiscrepantes());
    }

    @Test
    void categoriaDistintaNoAumentaElContadorSolicitado() {
        var otra = categoria(NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE, OptionalInt.of(1));
        var hecho = hecho(otra, FechaHecho.exacta(HOY), PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA);
        assertEquals(0, evaluar(List.of(hecho)).fundamento().orElseThrow().cantidad());
    }

    @Test
    void categoriaContradictoriaSeDiagnosticaAlConsultarCualquieraDeLasFuentes() {
        var otra = categoria(NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE, OptionalInt.of(1));
        var duplicado = hecho(otra, FechaHecho.exacta(HOY), PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA);
        var conjunto = cotidiano(20).vincularDuplicado(duplicado.principal(), cambio());
        assertEquals(List.of(conjunto.id()), evaluar(List.of(conjunto)).hechosConDatosDiscrepantes());
        var segunOtra = CalculadoraReiteracion.evaluar(ESTUDIANTE, otra, parametros, HOY, List.of(conjunto));
        assertEquals(List.of(conjunto.id()), segunOtra.hechosConDatosDiscrepantes());
        assertEquals(0, segunOtra.fundamento().orElseThrow().cantidad());
    }

    private CalculadoraReiteracion.Evaluacion evaluar(List<Hecho> hechos) {
        return CalculadoraReiteracion.evaluar(ESTUDIANTE, categoria, parametros, HOY, hechos);
    }
    private Hecho cotidiano(int dia) {
        return hecho(categoria, FechaHecho.exacta(HOY.withDayOfMonth(dia)), PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA);
    }
    private Hecho hecho(Categoria categoria, FechaHecho fecha, PapelParticipacion papel) {
        var original = papel == PapelParticipacion.ATRIBUCION_CORROBORADA || papel == PapelParticipacion.ATRIBUCION_DESCARTADA
                ? PapelParticipacion.PRESUNTA_RESPONSABILIDAD : papel;
        var datos = datos(categoria, fecha, List.of(new Participacion(
                PersonaInvolucrada.estudiante(ESTUDIANTE, "Estudiante ficticio"), original, Optional.empty())));
        var hecho = Hecho.de(ReporteIncidencia.registrar(UUID.randomUUID(), datos, AUTOR, REGISTRO));
        return original == papel ? hecho : hecho.aclararParticipacion(ESTUDIANTE, papel, "Atención ficticia documentada", cambio());
    }
    private DatosReporte datos(Categoria categoria, FechaHecho fecha, List<Participacion> participaciones) {
        return new DatosReporte(fecha, fecha.fecha().getYear(), categoria.clasificacionParaNuevoHecho(),
                "Descripción ficticia del hecho comunicado", participaciones, false,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    private Categoria categoria(NaturalezaCategoria naturaleza, Gravedad gravedad, OptionalInt umbral) {
        return Categoria.crear(UUID.randomUUID(), new DefinicionCategoria("Categoría ficticia", "Solo para pruebas",
                naturaleza, gravedad, false, true, umbral), TOE, REGISTRO);
    }
    private RegistroCambio cambio() { return new RegistroCambio(TOE, REGISTRO, Optional.empty()); }
}
