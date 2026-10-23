package pe.edu.incidencias.domain.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CatalogoTest {
    private static final UUID CATEGORIA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID AUTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTRO_AUTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Instant FECHA = Instant.parse("2026-10-20T15:00:00Z");

    @ParameterizedTest
    @EnumSource(value = Gravedad.class, names = {"GRAVE", "MUY_GRAVE"})
    void gravedadObligaAvisoAunqueElIndicadorExplicitoSeaFalso(Gravedad gravedad) {
        var clasificacion = new ClasificacionCategoria(CATEGORIA_ID, "Categoría ficticia",
                NaturalezaCategoria.OTRA_CONVIVENCIA, gravedad, false);

        assertTrue(clasificacion.requiereAvisoInmediato());
    }

    @ParameterizedTest
    @EnumSource(Gravedad.class)
    void violenciaSiempreAvisaDesdeElPrimerRegistro(Gravedad gravedad) {
        var clasificacion = new ClasificacionCategoria(CATEGORIA_ID, "Protección ficticia",
                NaturalezaCategoria.VIOLENCIA_PROTECCION, gravedad, false);

        assertTrue(clasificacion.requiereAvisoInmediato());
    }

    @Test
    void categoriaLeveSinAvisoNoCreaUnAvisoPorDefecto() {
        assertFalse(crear(OptionalInt.empty()).clasificacionParaNuevoHecho().requiereAvisoInmediato());
    }

    @Test
    void porClasificarAdmiteAvisoExplicitoSinInventarGravedad() {
        var clasificacion = new ClasificacionCategoria(CATEGORIA_ID, "Por clasificar",
                NaturalezaCategoria.OTRA_CONVIVENCIA, Gravedad.POR_CLASIFICAR, true);

        assertTrue(clasificacion.requiereAvisoInmediato());
        assertEquals(Gravedad.POR_CLASIFICAR, clasificacion.gravedad());
    }

    @Test
    void cambioDeCatalogoConservaClasificacionUsadaYAutorDeCadaVersion() {
        Categoria original = crear(OptionalInt.of(4));
        ClasificacionCategoria usada = original.clasificacionParaNuevoHecho();
        DefinicionCategoria nueva = new DefinicionCategoria("Nuevo nombre ficticio", "Nueva descripción",
                NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.GRAVE, true, true, OptionalInt.of(7));

        Categoria actualizada = original.actualizar(nueva, OTRO_AUTOR_ID, FECHA.plusSeconds(1));

        assertAll(
                () -> assertEquals(Gravedad.LEVE, usada.gravedad()),
                () -> assertFalse(usada.requiereAvisoInmediato()),
                () -> assertEquals("Conducta ficticia", original.nombre()),
                () -> assertEquals("Nuevo nombre ficticio", actualizada.nombre()),
                () -> assertEquals(CATEGORIA_ID, actualizada.id()),
                () -> assertEquals(2, actualizada.revisiones().size()),
                () -> assertEquals(AUTOR_ID, actualizada.revisiones().getFirst().autorId()),
                () -> assertEquals(OTRO_AUTOR_ID, actualizada.revisiones().getLast().autorId()),
                () -> assertEquals(FECHA.plusSeconds(1), actualizada.revisiones().getLast().fecha()),
                () -> assertEquals(4, actualizada.revisiones().getFirst().definicion().umbralReiteracion().getAsInt()),
                () -> assertEquals(7, actualizada.umbralReiteracion().getAsInt()));
    }

    @Test
    void desactivarProhibeNuevosUsosSinOcultarHistoria() {
        Categoria original = crear(OptionalInt.of(4));
        ClasificacionCategoria historica = original.clasificacionParaNuevoHecho();

        Categoria desactivada = original.desactivar(OTRO_AUTOR_ID, FECHA.plusSeconds(1));

        assertAll(
                () -> assertThrows(IllegalStateException.class, desactivada::clasificacionParaNuevoHecho),
                () -> assertFalse(desactivada.activa()),
                () -> assertTrue(desactivada.revisiones().getFirst().activa()),
                () -> assertEquals("Conducta ficticia", historica.nombre()),
                () -> assertTrue(original.activa()),
                () -> assertEquals(4, desactivada.umbralReiteracion().getAsInt()));

        Categoria reactivada = desactivada.activar(AUTOR_ID, FECHA.plusSeconds(2));
        assertTrue(reactivada.activa());
        assertEquals(historica, reactivada.clasificacionParaNuevoHecho());
        assertEquals(3, reactivada.revisiones().size());
    }

    @Test
    void historiaDeCategoriaNoPuedeModificarseDesdeFuera() {
        Categoria categoria = crear(OptionalInt.empty());
        assertThrows(UnsupportedOperationException.class, () -> categoria.revisiones().clear());
    }

    @Test
    void reaplicarValoresSinCambiosNoInflaHistoriasDeCategoriaNiVentana() {
        var categoria = crear(OptionalInt.empty());
        assertSame(categoria, categoria.actualizar(definicion(true, OptionalInt.empty()), OTRO_AUTOR_ID, FECHA.plusSeconds(1)));
        assertSame(categoria, categoria.activar(OTRO_AUTOR_ID, FECHA.plusSeconds(1)));
        var desactivada = categoria.desactivar(AUTOR_ID, FECHA.plusSeconds(1));
        assertSame(desactivada, desactivada.desactivar(OTRO_AUTOR_ID, FECHA.plusSeconds(2)));
        var parametros = ParametrosReiteracion.sinConfigurar(AUTOR_ID, FECHA);
        assertSame(parametros, parametros.retirarVentana(OTRO_AUTOR_ID, FECHA.plusSeconds(1)));
        var configurados = parametros.configurarVentana(30, AUTOR_ID, FECHA.plusSeconds(1));
        assertSame(configurados, configurados.configurarVentana(30, OTRO_AUTOR_ID, FECHA.plusSeconds(2)));
        assertThrows(IllegalArgumentException.class, () -> configurados.configurarVentana(30, AUTOR_ID, FECHA));
    }

    @Test
    void noSeAceptanUmbralesNoPositivosNiUnUmbralSinReiteracion() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> definicion(true, OptionalInt.of(0))),
                () -> assertThrows(IllegalArgumentException.class, () -> definicion(true, OptionalInt.of(-1))),
                () -> assertThrows(IllegalArgumentException.class, () -> definicion(false, OptionalInt.of(4))));
    }

    @Test
    void nombreDescripcionNaturalezaYGravedadSonNecesarios() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new DefinicionCategoria(" ", "Descripción",
                        NaturalezaCategoria.OTRA_CONVIVENCIA, Gravedad.LEVE, false, false, OptionalInt.empty())),
                () -> assertThrows(IllegalArgumentException.class, () -> new DefinicionCategoria("Nombre", " ",
                        NaturalezaCategoria.OTRA_CONVIVENCIA, Gravedad.LEVE, false, false, OptionalInt.empty())),
                () -> assertThrows(NullPointerException.class, () -> new ClasificacionCategoria(CATEGORIA_ID,
                        "Nombre", null, Gravedad.LEVE, false)),
                () -> assertThrows(NullPointerException.class, () -> new ClasificacionCategoria(CATEGORIA_ID,
                        "Nombre", NaturalezaCategoria.OTRA_CONVIVENCIA, null, false)));
    }

    @Test
    void noSeAceptanCambiosDeCatalogoConFechaAnteriorALaRevisionActual() {
        Categoria categoria = crear(OptionalInt.empty());
        assertThrows(IllegalArgumentException.class,
                () -> categoria.desactivar(AUTOR_ID, FECHA.minusSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> categoria.actualizar(definicion(true, OptionalInt.of(4)), AUTOR_ID, FECHA.minusSeconds(1)));
    }

    @Test
    void sinParametrosAprobadosNoSeInventaVentanaNiUmbral() {
        ParametrosReiteracion parametros = ParametrosReiteracion.sinConfigurar(AUTOR_ID, FECHA);
        var estado = parametros.evaluar(crear(OptionalInt.empty()));

        assertAll(
                () -> assertFalse(estado.habilitada()),
                () -> assertTrue(estado.ventanaDias().isEmpty()),
                () -> assertTrue(estado.umbral().isEmpty()),
                () -> assertEquals(Set.of(ParametrosReiteracion.ParametroFaltante.UMBRAL_CATEGORIA,
                        ParametrosReiteracion.ParametroFaltante.VENTANA_DIAS), estado.parametrosFaltantes()));
    }

    @Test
    void faltaDeCadaParametroSeExponePorSeparado() {
        ParametrosReiteracion sinVentana = ParametrosReiteracion.sinConfigurar(AUTOR_ID, FECHA);
        assertEquals(Set.of(ParametrosReiteracion.ParametroFaltante.VENTANA_DIAS),
                sinVentana.evaluar(crear(OptionalInt.of(4))).parametrosFaltantes());

        ParametrosReiteracion conVentana = sinVentana.configurarVentana(20, AUTOR_ID, FECHA.plusSeconds(1));
        assertEquals(Set.of(ParametrosReiteracion.ParametroFaltante.UMBRAL_CATEGORIA),
                conVentana.evaluar(crear(OptionalInt.empty())).parametrosFaltantes());
    }

    @Test
    void cadaCategoriaUsaSuUmbralConUnaSolaVentanaComun() {
        ParametrosReiteracion parametros = ParametrosReiteracion.sinConfigurar(AUTOR_ID, FECHA)
                .configurarVentana(20, AUTOR_ID, FECHA.plusSeconds(1));
        Categoria categoriaA = crear(OptionalInt.of(4));
        Categoria categoriaB = Categoria.crear(UUID.fromString("00000000-0000-0000-0000-000000000004"),
                definicion(true, OptionalInt.of(7)), AUTOR_ID, FECHA);

        var estadoA = parametros.evaluar(categoriaA);
        var estadoB = parametros.evaluar(categoriaB);

        assertAll(
                () -> assertTrue(estadoA.habilitada()),
                () -> assertTrue(estadoB.habilitada()),
                () -> assertEquals(4, estadoA.umbral().getAsInt()),
                () -> assertEquals(7, estadoB.umbral().getAsInt()),
                () -> assertEquals(20, estadoA.ventanaDias().getAsInt()),
                () -> assertEquals(estadoA.ventanaDias(), estadoB.ventanaDias()),
                () -> assertTrue(estadoA.parametrosFaltantes().isEmpty()));
    }

    @Test
    void categoriaQueNoAdmiteReiteracionNoLaHabilitaNiPresentaParametrosComoPendientes() {
        Categoria categoria = Categoria.crear(CATEGORIA_ID, definicion(false, OptionalInt.empty()), AUTOR_ID, FECHA);
        var estado = ParametrosReiteracion.sinConfigurar(AUTOR_ID, FECHA).evaluar(categoria);

        assertFalse(estado.habilitada());
        assertFalse(estado.admiteReiteracion());
        assertTrue(estado.parametrosFaltantes().isEmpty());
    }

    @Test
    void cambioDeVentanaNoReescribeLaConfiguracionAnterior() {
        ParametrosReiteracion original = ParametrosReiteracion.sinConfigurar(AUTOR_ID, FECHA)
                .configurarVentana(20, AUTOR_ID, FECHA.plusSeconds(1));
        var fundamentoAnterior = original.evaluar(crear(OptionalInt.of(4)));

        ParametrosReiteracion actualizada = original.configurarVentana(10, OTRO_AUTOR_ID, FECHA.plusSeconds(2));

        assertAll(
                () -> assertEquals(20, fundamentoAnterior.ventanaDias().getAsInt()),
                () -> assertEquals(20, original.ventanaDias().getAsInt()),
                () -> assertEquals(10, actualizada.ventanaDias().getAsInt()),
                () -> assertEquals(3, actualizada.revisiones().size()),
                () -> assertEquals(OTRO_AUTOR_ID, actualizada.revisiones().getLast().autorId()),
                () -> assertThrows(UnsupportedOperationException.class, () -> actualizada.revisiones().clear()));

        ParametrosReiteracion retirada = actualizada.retirarVentana(AUTOR_ID, FECHA.plusSeconds(3));
        assertFalse(retirada.evaluar(crear(OptionalInt.of(4))).habilitada());
        assertEquals(4, retirada.revisiones().size());
    }

    @Test
    void ventanaPositivaConAutorFechaYOrdenCronologico() {
        ParametrosReiteracion parametros = ParametrosReiteracion.sinConfigurar(AUTOR_ID, FECHA);
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> parametros.configurarVentana(0, AUTOR_ID, FECHA)),
                () -> assertThrows(IllegalArgumentException.class, () -> parametros.configurarVentana(-1, AUTOR_ID, FECHA)),
                () -> assertThrows(NullPointerException.class, () -> parametros.configurarVentana(20, null, FECHA)),
                () -> assertThrows(NullPointerException.class, () -> parametros.configurarVentana(20, AUTOR_ID, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> parametros.configurarVentana(20, AUTOR_ID, FECHA.minusSeconds(1))));
    }

    private Categoria crear(OptionalInt umbral) {
        return Categoria.crear(CATEGORIA_ID, definicion(true, umbral), AUTOR_ID, FECHA);
    }

    private DefinicionCategoria definicion(boolean admiteReiteracion, OptionalInt umbral) {
        return new DefinicionCategoria("Conducta ficticia", "Descripción ficticia para las pruebas",
                NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE, false, admiteReiteracion, umbral);
    }
}
