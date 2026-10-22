package pe.edu.incidencias.domain.incidencia;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.alerta.CalculadoraReiteracion;
import pe.edu.incidencias.domain.catalogo.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CorreccionesHechoTest {
    private static final Instant REGISTRO = Instant.parse("2026-10-21T01:00:00Z"); // 20 de octubre en Perú
    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);
    private static final UUID ESTUDIANTE = id(1);
    private static final UUID TOE = id(2);
    private final Categoria categoriaA = categoria(10, NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE);
    private final Categoria categoriaB = categoria(11, NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE);
    private final ParametrosReiteracion parametros = ParametrosReiteracion.sinConfigurar(TOE, REGISTRO)
            .configurarVentana(10, TOE, REGISTRO);

    @Test
    void precisionInstitucionalResuelveDatosSinReescribirRelatosNiSusDiferencias() {
        var primero = reporte(20, categoriaA, HOY.minusDays(1));
        var segundo = reporte(21, categoriaB, HOY);
        var original = Hecho.de(primero).vincularDuplicado(segundo, cambio(1));
        assertTrue(original.fechaVigente().isEmpty());
        assertTrue(original.clasificacionVigente().isEmpty());
        assertTrue(original.requierePrecisionDatos());

        var precisado = original.precisarDatos(FechaHecho.exacta(HOY), categoriaB.clasificacionParaNuevoHecho(), motivo(2));

        assertAll(
                () -> assertEquals(original.id(), precisado.id()),
                () -> assertEquals(Optional.of(FechaHecho.exacta(HOY)), precisado.fechaVigente()),
                () -> assertEquals(Optional.of(categoriaB.clasificacionParaNuevoHecho()), precisado.clasificacionVigente()),
                () -> assertFalse(precisado.requierePrecisionDatos()),
                () -> assertTrue(precisado.discrepanciaFechaOCategoria()),
                () -> assertTrue(original.precisiones().isEmpty()),
                () -> assertEquals(TOE, precisado.precisiones().getFirst().cambio().autorId()),
                () -> assertEquals(motivo(2), precisado.precisiones().getFirst().cambio()));
        assertEquals(primero.revisiones(), precisado.reportes().getFirst().revisiones());
        assertEquals(segundo.revisiones(), precisado.reportes().getLast().revisiones());
        assertEquals(primero.autorId(), precisado.principal().autorId());
        assertEquals(primero.fechaRegistro(), precisado.principal().fechaRegistro());
        assertTrue(precisado.reportes().stream().allMatch(r -> r.inicioAtencion().isPresent()));
        assertEquals(1, precisado.participaciones().size());
        assertEquals(2, precisado.participaciones().getFirst().aportes().size());
    }

    @Test
    void calculoUtilizaCategoriaYFechaPrecisadasYConservaElFundamentoAnterior() {
        var original = Hecho.de(reporte(20, categoriaA, HOY.minusDays(20)))
                .vincularDuplicado(reporte(21, categoriaB, HOY), cambio(1));
        assertEquals(List.of(original.id()), evaluar(original, categoriaA).hechosConDatosDiscrepantes());
        assertEquals(List.of(original.id()), evaluar(original, categoriaB).hechosConDatosDiscrepantes());
        var precisado = original.precisarDatos(FechaHecho.exacta(HOY), categoriaB.clasificacionParaNuevoHecho(), motivo(2));
        var fundamento = evaluar(precisado, categoriaB).fundamento().orElseThrow();
        assertEquals(1, fundamento.cantidad());
        assertEquals(original.id(), fundamento.atribuciones().getFirst().hechoId());
        assertTrue(evaluar(precisado, categoriaB).hechosConDatosDiscrepantes().isEmpty());
        assertEquals(0, evaluar(precisado, categoriaA).fundamento().orElseThrow().cantidad());

        var rectificado = precisado.precisarDatos(FechaHecho.exacta(HOY.minusDays(20)),
                categoriaA.clasificacionParaNuevoHecho(), motivo(3));
        assertEquals(2, rectificado.precisiones().size());
        assertEquals(0, evaluar(rectificado, categoriaA).fundamento().orElseThrow().cantidad());
        assertEquals(1, fundamento.cantidad());
        assertEquals(Optional.of(FechaHecho.exacta(HOY)), precisado.fechaVigente());
    }

    @Test
    void precisionAproximadaNoFabricaPrecisionParaElContador() {
        var precisado = Hecho.de(reporte(20, categoriaA, HOY)).precisarDatos(FechaHecho.aproximada(HOY),
                categoriaA.clasificacionParaNuevoHecho(), motivo(1));
        assertTrue(precisado.fechaVigente().orElseThrow().aproximada());
        assertFalse(precisado.requierePrecisionDatos());
        assertEquals(0, evaluar(precisado, categoriaA).fundamento().orElseThrow().cantidad());
    }

    @Test
    void requiereCausaYFechaNoFuturaEnPeruYConservaOrdenDeLasPrecisiones() {
        var original = Hecho.de(reporte(20, categoriaA, HOY));
        assertThrows(IllegalArgumentException.class, () -> original.precisarDatos(FechaHecho.exacta(HOY),
                categoriaA.clasificacionParaNuevoHecho(), cambio(1)));
        assertThrows(IllegalArgumentException.class, () -> original.precisarDatos(FechaHecho.exacta(HOY.plusDays(1)),
                categoriaA.clasificacionParaNuevoHecho(), motivo(1)));
        var precisado = original.precisarDatos(FechaHecho.exacta(HOY), categoriaB.clasificacionParaNuevoHecho(), motivo(3));
        assertThrows(IllegalArgumentException.class, () -> precisado.precisarDatos(FechaHecho.exacta(HOY),
                categoriaA.clasificacionParaNuevoHecho(), motivo(2)));
        assertThrows(IllegalArgumentException.class, () -> precisado.cambiarPrincipal(precisado.principal().id(), motivo(2)));
        assertThrows(UnsupportedOperationException.class, () -> precisado.precisiones().clear());
    }

    @Test
    void precisionDeClasificacionRigeAvisosSinEliminarLaPeticionExpresa() {
        var grave = categoria(12, NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.GRAVE);
        var comunicado = Hecho.de(reporte(20, grave, HOY, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, false));
        assertTrue(comunicado.requiereAvisoInmediato());
        var precisado = comunicado.precisarDatos(FechaHecho.exacta(HOY), categoriaA.clasificacionParaNuevoHecho(), motivo(1));
        assertFalse(precisado.requiereAvisoInmediato());
        assertTrue(comunicado.requiereAvisoInmediato());

        var pedido = precisado.vincularDuplicado(reporte(21, grave, HOY,
                PapelParticipacion.PRESUNTA_RESPONSABILIDAD, true), cambio(2));
        assertTrue(pedido.requiereAvisoInmediato());
        assertEquals(Optional.of(categoriaA.clasificacionParaNuevoHecho()), pedido.clasificacionVigente());
    }

    @Test
    void cambiarNaturalezaNoConvierteLaCotidianaOriginalEnAtribucionDeViolencia() {
        var violencia = categoria(12, NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.GRAVE);
        var original = Hecho.de(reporte(20, categoriaA, HOY));
        var precisado = original.precisarDatos(FechaHecho.exacta(HOY), violencia.clasificacionParaNuevoHecho(), motivo(1));
        assertTrue(precisado.requiereAvisoInmediato());
        assertEquals(0, evaluar(precisado, violencia).fundamento().orElseThrow().cantidad());
        assertEquals(PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA,
                precisado.principal().datos().participaciones().getFirst().papel());
        assertThrows(IllegalArgumentException.class, () -> precisado.aclararParticipacion(ESTUDIANTE,
                PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA, "Aclaración ficticia", cambio(2)));
        var aclarado = precisado.aclararParticipacion(ESTUDIANTE, PapelParticipacion.PRESUNTA_RESPONSABILIDAD,
                "Atribución por confirmar durante la atención", cambio(2));
        assertEquals(1, evaluar(aclarado, violencia).fundamento().orElseThrow()
                .cantidad(PapelParticipacion.PRESUNTA_RESPONSABILIDAD));
    }

    @Test
    void diferenciasDeGravedadDeLaMismaCategoriaNoSeResuelvenPorOrdenDeReportes() {
        var grave = new ClasificacionCategoria(categoriaA.id(), categoriaA.nombre(),
                NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.GRAVE, false);
        var datos = datos(grave, HOY, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, false);
        var conjunto = Hecho.de(reporte(20, categoriaA, HOY)).vincularDuplicado(
                ReporteIncidencia.registrar(id(21), datos, id(121), REGISTRO), cambio(1));
        assertTrue(conjunto.clasificacionVigente().isEmpty());
        assertTrue(conjunto.requiereAvisoInmediato());
        assertEquals(List.of(conjunto.id()), evaluar(conjunto, categoriaA).hechosConDatosDiscrepantes());
        assertFalse(conjunto.precisarDatos(FechaHecho.exacta(HOY), categoriaA.clasificacionParaNuevoHecho(), motivo(2))
                .requierePrecisionDatos());
    }

    @Test
    void principalAnuladoNoOcultaFechaCategoriaParticipacionesNiAvisoDelReporteValido() {
        var principal = reporte(20, categoriaA, HOY.minusDays(20));
        var valido = reporte(21, categoriaB, HOY, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA, true);
        var original = Hecho.de(principal).vincularDuplicado(valido, cambio(1));
        var conAnulacion = original.actualizarReporte(principal.anularPorError(motivo(2)));
        assertFalse(conAnulacion.anulado());
        assertFalse(conAnulacion.discrepanciaFechaOCategoria());
        assertEquals(Optional.of(FechaHecho.exacta(HOY)), conAnulacion.fechaVigente());
        assertEquals(Optional.of(categoriaB.clasificacionParaNuevoHecho()), conAnulacion.clasificacionVigente());
        assertTrue(conAnulacion.requiereAvisoInmediato());
        assertEquals(1, conAnulacion.participaciones().getFirst().aportes().size());
        assertEquals(1, evaluar(conAnulacion, categoriaB).fundamento().orElseThrow().cantidad());
        assertEquals(0, evaluar(conAnulacion, categoriaA).fundamento().orElseThrow().cantidad());
        assertEquals(original.id(), conAnulacion.id());
    }

    @Test
    void corregirPrincipalMantieneIdentidadFuentesYUnSoloHechoComputable() {
        var principal = reporte(20, categoriaA, HOY);
        var duplicado = reporte(21, categoriaA, HOY);
        var original = Hecho.de(principal).vincularDuplicado(duplicado, cambio(1));
        var conAnulacion = original.actualizarReporte(principal.anularPorError(motivo(2)));
        var corregido = conAnulacion.cambiarPrincipal(duplicado.id(), motivo(3));
        assertEquals(original.id(), corregido.id());
        assertEquals(duplicado.id(), corregido.principal().id());
        assertEquals(List.of(principal.id(), duplicado.id()), corregido.reportes().stream().map(ReporteIncidencia::id).toList());
        assertEquals(conAnulacion.reportes().getFirst().revisiones(), corregido.reportes().getFirst().revisiones());
        assertEquals(conAnulacion.reportes().getFirst().anulacion(), corregido.reportes().getFirst().anulacion());
        assertEquals(duplicado.revisiones(), corregido.principal().revisiones());
        assertEquals(1, corregido.cambiosPrincipal().size());
        var cambio = corregido.cambiosPrincipal().getFirst();
        assertEquals(principal.id(), cambio.anteriorId());
        assertEquals(duplicado.id(), cambio.nuevoId());
        assertEquals(motivo(3), cambio.cambio());
        assertEquals(1, evaluar(corregido, categoriaA).fundamento().orElseThrow().cantidad());
        assertThrows(IllegalArgumentException.class, () -> CalculadoraReiteracion.evaluar(ESTUDIANTE, categoriaA,
                parametros, HOY, List.of(conAnulacion, corregido)));
        assertEquals(principal.id(), original.principal().id());
        assertTrue(original.cambiosPrincipal().isEmpty());
    }

    @Test
    void principalPuedeCorregirseEntreReportesValidosYVolverSinCambiarLaPrecision() {
        var principal = reporte(20, categoriaA, HOY.minusDays(1));
        var original = Hecho.de(principal).vincularDuplicado(reporte(21, categoriaB, HOY), cambio(1))
                .precisarDatos(FechaHecho.exacta(HOY), categoriaB.clasificacionParaNuevoHecho(), motivo(2));
        var cambiado = original.cambiarPrincipal(id(21), motivo(3));
        var devuelto = cambiado.cambiarPrincipal(principal.id(), motivo(4));
        assertEquals(original.id(), devuelto.id());
        assertEquals(2, devuelto.cambiosPrincipal().size());
        assertEquals(original.precisiones(), devuelto.precisiones());
        assertEquals(original.clasificacionVigente(), devuelto.clasificacionVigente());
        assertEquals(1, evaluar(devuelto, categoriaB).fundamento().orElseThrow().cantidad());
        assertThrows(UnsupportedOperationException.class, () -> devuelto.cambiosPrincipal().clear());
    }

    @Test
    void cambioDePrincipalExigeCausaYUnReporteRelacionadoValido() {
        var original = Hecho.de(reporte(20, categoriaA, HOY)).vincularDuplicado(reporte(21, categoriaA, HOY), cambio(1));
        assertThrows(IllegalArgumentException.class, () -> original.cambiarPrincipal(id(21), cambio(2)));
        assertThrows(IllegalArgumentException.class, () -> original.cambiarPrincipal(id(99), motivo(2)));
        var anulado = original.actualizarReporte(original.reportes().getLast().anularPorError(motivo(2)));
        assertThrows(IllegalArgumentException.class, () -> anulado.cambiarPrincipal(id(21), motivo(3)));
    }

    @Test
    void soloElPrincipalActualEstaProtegidoFrenteALaRetiradaComoDuplicado() {
        var original = Hecho.de(reporte(20, categoriaA, HOY)).vincularDuplicado(reporte(21, categoriaA, HOY), cambio(1));
        var cambiado = original.cambiarPrincipal(id(21), motivo(2));
        assertThrows(IllegalArgumentException.class, () -> cambiado.retirarDuplicado(id(21), motivo(3)));
        var retirado = cambiado.retirarDuplicado(id(20), motivo(3));
        assertEquals(original.id(), retirado.id());
        assertEquals(1, retirado.reportes().size());
        assertEquals(id(21), retirado.principal().id());
        assertEquals(cambiado.cambiosPrincipal(), retirado.cambiosPrincipal());
        assertEquals(1, evaluar(retirado, categoriaA).fundamento().orElseThrow().cantidad());
        assertThrows(IllegalArgumentException.class, () -> CalculadoraReiteracion.evaluar(ESTUDIANTE, categoriaA,
                parametros, HOY, List.of(retirado, Hecho.de(original.principal()))));
    }

    @Test
    void soloAnularTodosLosReportesExcluyeElHechoYNoLoResucitaUnaPrecision() {
        var original = Hecho.de(reporte(20, categoriaA, HOY)).vincularDuplicado(reporte(21, categoriaA, HOY), cambio(1));
        var unoAnulado = original.actualizarReporte(original.principal().anularPorError(motivo(2)));
        var todos = unoAnulado.actualizarReporte(unoAnulado.reportes().getLast().anularPorError(motivo(3)));
        assertTrue(todos.anulado());
        assertTrue(todos.fechaVigente().isEmpty());
        assertTrue(todos.clasificacionVigente().isEmpty());
        assertTrue(todos.participaciones().isEmpty());
        assertFalse(todos.requiereAvisoInmediato());
        assertEquals(0, evaluar(todos, categoriaA).fundamento().orElseThrow().cantidad());
        assertEquals(2, todos.reportes().size());
        assertThrows(IllegalStateException.class, () -> todos.precisarDatos(FechaHecho.exacta(HOY),
                categoriaA.clasificacionParaNuevoHecho(), motivo(4)));
        assertThrows(IllegalArgumentException.class, () -> todos.cambiarPrincipal(id(21), motivo(4)));
    }

    @Test
    void precisionEHistoriaDePrincipalSeConservanAlActualizarYAclararFuentes() {
        var original = Hecho.de(reporte(20, categoriaA, HOY)).vincularDuplicado(reporte(21, categoriaB, HOY), cambio(1))
                .precisarDatos(FechaHecho.exacta(HOY), categoriaB.clasificacionParaNuevoHecho(), motivo(2))
                .cambiarPrincipal(id(21), motivo(3));
        var ampliado = original.vincularDuplicado(reporte(22, categoriaA, HOY.minusDays(1)), cambio(4));
        var aclarado = ampliado.aclararParticipacion(ESTUDIANTE, PapelParticipacion.ATRIBUCION_CORROBORADA,
                "Atención ficticia documentada", cambio(5));
        var actualizado = aclarado.actualizarReporte(aclarado.principal().aclararPorAutor("Aporte ficticio adicional",
                new RegistroCambio(aclarado.principal().autorId(), REGISTRO.plusSeconds(6), Optional.empty())));
        var retirado = actualizado.retirarDuplicado(id(22), motivo(7));
        assertEquals(original.precisiones(), retirado.precisiones());
        assertEquals(original.cambiosPrincipal(), retirado.cambiosPrincipal());
        assertEquals(original.fechaVigente(), retirado.fechaVigente());
        assertEquals(original.clasificacionVigente(), retirado.clasificacionVigente());
        assertEquals(1, evaluar(retirado, categoriaB).fundamento().orElseThrow().cantidad());
    }

    private CalculadoraReiteracion.Evaluacion evaluar(Hecho hecho, Categoria categoria) {
        return CalculadoraReiteracion.evaluar(ESTUDIANTE, categoria, parametros, HOY, List.of(hecho));
    }
    private ReporteIncidencia reporte(int numero, Categoria categoria, LocalDate fecha) {
        return reporte(numero, categoria, fecha, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA, false);
    }
    private ReporteIncidencia reporte(int numero, Categoria categoria, LocalDate fecha, PapelParticipacion papel, boolean solicita) {
        return ReporteIncidencia.registrar(id(numero), datos(categoria.clasificacionParaNuevoHecho(), fecha, papel, solicita),
                id(numero + 100), REGISTRO);
    }
    private DatosReporte datos(ClasificacionCategoria clasificacion, LocalDate fecha, PapelParticipacion papel, boolean solicita) {
        return new DatosReporte(FechaHecho.exacta(fecha), 2026, clasificacion, "Relato ficticio de " + clasificacion.nombre(),
                List.of(new Participacion(PersonaInvolucrada.estudiante(ESTUDIANTE, "Estudiante ficticio"), papel, Optional.empty())),
                solicita, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    private Categoria categoria(int numero, NaturalezaCategoria naturaleza, Gravedad gravedad) {
        return Categoria.crear(id(numero), new DefinicionCategoria("Categoría ficticia " + numero, "Solo para pruebas",
                naturaleza, gravedad, false, true, OptionalInt.of(1)), TOE, REGISTRO);
    }
    private RegistroCambio cambio(int segundos) {
        return new RegistroCambio(TOE, REGISTRO.plusSeconds(segundos), Optional.empty());
    }
    private RegistroCambio motivo(int segundos) {
        return new RegistroCambio(TOE, REGISTRO.plusSeconds(segundos), Optional.of("Precisión institucional ficticia documentada"));
    }
    private static UUID id(long numero) { return new UUID(0, numero); }
}
