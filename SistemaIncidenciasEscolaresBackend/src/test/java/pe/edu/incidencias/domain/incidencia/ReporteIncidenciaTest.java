package pe.edu.incidencias.domain.incidencia;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.catalogo.ClasificacionCategoria;
import pe.edu.incidencias.domain.catalogo.Gravedad;
import pe.edu.incidencias.domain.catalogo.NaturalezaCategoria;
import pe.edu.incidencias.domain.estudiante.GrupoEscolar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReporteIncidenciaTest {
    private static final UUID AUTOR = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TOE = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID ESTUDIANTE = UUID.fromString("20000000-0000-0000-0000-000000000001");
    // En UTC ya es 21; en Perú aún es 20 de octubre, a las 21:00.
    private static final Instant REGISTRO = Instant.parse("2026-10-21T02:00:00Z");
    private static final LocalDate DIA_PERU = LocalDate.of(2026, 10, 20);
    private static final ClasificacionCategoria COTIDIANA = categoria(NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE);

    @Test
    void fechaFuturaSeEvaluaEnPeruAunqueUtcYaSeaElDiaSiguiente() {
        var futuro = datos("Comunicación futura", List.of(participacion(PapelParticipacion.TESTIGO)),
                COTIDIANA, FechaHecho.exacta(LocalDate.of(2026, 10, 21)), Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> ReporteIncidencia.registrar(UUID.randomUUID(), futuro, AUTOR, REGISTRO));
        assertEquals(DIA_PERU, registrar(datos("Comunicación válida")).datos().fechaHecho().fecha());
    }

    @Test
    void horaFuturaDelMismoDiaSeRechazaSinConfundirlaConUnaHoraDelDiaAnterior() {
        var participantes = List.of(participacion(PapelParticipacion.TESTIGO));
        var futuro = datos("Hora futura", participantes, COTIDIANA, FechaHecho.exacta(DIA_PERU),
                Optional.of(LocalTime.of(21, 1)));
        var exacto = datos("Hora presente", participantes, COTIDIANA, FechaHecho.exacta(DIA_PERU),
                Optional.of(LocalTime.of(21, 0)));
        var anterior = datos("Registro tardío", participantes, COTIDIANA, FechaHecho.exacta(DIA_PERU.minusDays(1)),
                Optional.of(LocalTime.of(23, 59)));

        assertThrows(IllegalArgumentException.class, () -> registrar(futuro));
        assertEquals(Optional.of(LocalTime.of(21, 0)), registrar(exacto).datos().hora());
        assertEquals(Optional.of(LocalTime.of(23, 59)), registrar(anterior).datos().hora());
    }

    @Test
    void fechaAproximadaSeConservaSinInventarHoraNiGrupo() {
        var conocido = datos("Fecha recordada aproximadamente", List.of(participacion(PapelParticipacion.AFECTADO)),
                COTIDIANA, FechaHecho.aproximada(DIA_PERU.minusDays(3)), Optional.empty());
        var reporte = registrar(conocido);

        assertTrue(reporte.datos().fechaHecho().aproximada());
        assertEquals(DIA_PERU.minusDays(3), reporte.datos().fechaHecho().fecha());
        assertTrue(reporte.datos().hora().isEmpty());
        assertTrue(reporte.datos().participaciones().getFirst().grupoHistorico().isEmpty());
    }

    @Test
    void fechaAproximadaNoAdmitePrecisionHorariaInventada() {
        assertThrows(IllegalArgumentException.class, () -> datos("Fecha aproximada con hora exacta",
                List.of(participacion(PapelParticipacion.TESTIGO)), COTIDIANA,
                FechaHecho.aproximada(DIA_PERU), Optional.of(LocalTime.NOON)));
    }

    @Test
    void admiteEstudianteProvisionalYPersonaExternaSinDocumentoNiCuenta() {
        var provisional = new PersonaInvolucrada(UUID.randomUUID(), TipoPersona.ESTUDIANTE_NO_IDENTIFICADO,
                "Estudiante de otra sección aún no identificado", Optional.empty(), Optional.empty());
        var externa = new PersonaInvolucrada(UUID.randomUUID(), TipoPersona.EXTERNA,
                "Persona que esperaba junto a la puerta", Optional.empty(), Optional.empty());
        var reporte = registrar(datos("Comunicación con identificación parcial", List.of(
                new Participacion(provisional, PapelParticipacion.AFECTADO, Optional.empty()),
                new Participacion(externa, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, Optional.empty())),
                categoria(NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.POR_CLASIFICAR),
                FechaHecho.exacta(DIA_PERU), Optional.empty()));

        assertEquals(2, reporte.datos().participaciones().size());
        assertTrue(provisional.estudianteId().isEmpty());
        assertTrue(externa.usuarioId().isEmpty());
        assertTrue(reporte.inicioAtencion().isEmpty());
    }

    @Test
    void conservaPapelesIndividualesYGrupoHistoricoSinTrasladarResponsabilidades() {
        var grupo = new GrupoEscolar("Primero", "A");
        var estudiante = new Participacion(PersonaInvolucrada.estudiante(ESTUDIANTE, "Estudiante ficticio"),
                PapelParticipacion.AFECTADO, Optional.of(grupo));
        var familiar = new Participacion(new PersonaInvolucrada(UUID.randomUUID(), TipoPersona.FAMILIAR,
                "Familiar identificado por referencia", Optional.empty(), Optional.of("Familiar del estudiante")),
                PapelParticipacion.PRESUNTA_RESPONSABILIDAD, Optional.empty());
        var testigo = new Participacion(new PersonaInvolucrada(UUID.randomUUID(), TipoPersona.PERSONAL,
                "Auxiliar de patio", Optional.of(TOE), Optional.of("Auxiliar")),
                PapelParticipacion.TESTIGO, Optional.empty());
        var reporte = registrar(datos("Hecho con varias personas", List.of(estudiante, familiar, testigo),
                categoria(NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.GRAVE),
                FechaHecho.exacta(DIA_PERU), Optional.empty()));

        assertEquals(List.of(estudiante, familiar, testigo), reporte.datos().participaciones());
        assertEquals(PapelParticipacion.AFECTADO, reporte.datos().participaciones().getFirst().papel());
        assertEquals(Optional.of(grupo), reporte.datos().participaciones().getFirst().grupoHistorico());
        assertEquals(Optional.of(TOE), testigo.persona().usuarioId());
    }

    @Test
    void unaPersonaNoSeRepiteDentroDelReporteAunqueCambieElPapel() {
        assertThrows(IllegalArgumentException.class, () -> datos("Participación duplicada", List.of(
                participacion(PapelParticipacion.AFECTADO), participacion(PapelParticipacion.TESTIGO)),
                COTIDIANA, FechaHecho.exacta(DIA_PERU), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> datos("Sin persona ni referencia", List.of(),
                COTIDIANA, FechaHecho.exacta(DIA_PERU), Optional.empty()));
    }

    @Test
    void atribucionCotidianaRequiereEstudianteYDisciplinaLeve() {
        var atribuida = List.of(participacion(PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA));
        assertDoesNotThrow(() -> registrar(datos("Conducta cotidiana atribuida", atribuida, COTIDIANA,
                FechaHecho.exacta(DIA_PERU), Optional.empty())));

        for (var clasificacion : List.of(
                categoria(NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.GRAVE),
                categoria(NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.MUY_GRAVE),
                categoria(NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.POR_CLASIFICAR),
                categoria(NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.LEVE),
                categoria(NaturalezaCategoria.OTRA_CONVIVENCIA, Gravedad.LEVE))) {
            assertThrows(IllegalArgumentException.class, () -> datos("No es una atribución cotidiana",
                    atribuida, clasificacion, FechaHecho.exacta(DIA_PERU), Optional.empty()));
        }
        for (var tipo : List.of(TipoPersona.ESTUDIANTE_NO_IDENTIFICADO, TipoPersona.PERSONAL,
                TipoPersona.FAMILIAR, TipoPersona.EXTERNA)) {
            var persona = new PersonaInvolucrada(UUID.randomUUID(), tipo, "Referencia conocida",
                    Optional.empty(), Optional.empty());
            assertThrows(IllegalArgumentException.class, () -> new Participacion(persona,
                    PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA, Optional.empty()));
        }
    }

    @Test
    void registrarNoPermiteCorroborarODescartarInstitucionalmenteDesdeElAporteInicial() {
        for (var papel : List.of(PapelParticipacion.ATRIBUCION_CORROBORADA, PapelParticipacion.ATRIBUCION_DESCARTADA)) {
            var datos = datos("Conclusión institucional prematura", List.of(participacion(papel)), COTIDIANA,
                    FechaHecho.exacta(DIA_PERU), Optional.empty());
            assertThrows(IllegalArgumentException.class, () -> registrar(datos));
        }
    }

    @Test
    void registroCotidianoNoExigeAtencionNiAprobacionYConservaSuAutoria() {
        var reporte = registrar(datos("Nota cotidiana"));

        assertEquals(AUTOR, reporte.autorId());
        assertEquals(REGISTRO, reporte.fechaRegistro());
        assertEquals(1, reporte.revisiones().size());
        assertTrue(reporte.inicioAtencion().isEmpty());
        assertTrue(reporte.aclaraciones().isEmpty());
        assertFalse(reporte.anulado());
    }

    @Test
    void autorCorrigeAntesDeAtencionConservandoElAporteOriginalYAmbasFechas() {
        var original = registrar(datos("Descripción original"));
        var cambio = cambio(AUTOR, 60, Optional.empty());
        var corregido = original.corregirPorAutor(datos("Descripción rectificada"), cambio);

        assertEquals(original.id(), corregido.id());
        assertEquals(AUTOR, corregido.autorId());
        assertEquals(REGISTRO, corregido.fechaRegistro());
        assertEquals("Descripción original", original.datos().descripcion());
        assertEquals("Descripción original", corregido.revisiones().getFirst().datos().descripcion());
        assertEquals("Descripción rectificada", corregido.datos().descripcion());
        assertEquals(cambio, corregido.revisiones().getLast().cambio());
        assertEquals(2, corregido.revisiones().size());
    }

    @Test
    void soloElAutorCorrigeSuAporteYNoPuedeCorroborarComoAutorOrdinario() {
        var reporte = registrar(datos("Descripción"));
        assertThrows(IllegalArgumentException.class,
                () -> reporte.corregirPorAutor(datos("Otra descripción"), cambio(TOE, 60, Optional.empty())));
        var corroborados = datos("No debe convertirse en corroboración", List.of(
                participacion(PapelParticipacion.ATRIBUCION_CORROBORADA)), COTIDIANA,
                FechaHecho.exacta(DIA_PERU), Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> reporte.corregirPorAutor(corroborados, cambio(AUTOR, 60, Optional.empty())));
    }

    @Test
    void trasAtencionElAutorSoloAgregaAclaracionSinReescribirLaVersionAtendida() {
        var atendido = registrar(datos("Descripción atendida"))
                .marcarAtencionInstitucional(cambio(TOE, 60, Optional.empty()));
        assertThrows(IllegalStateException.class,
                () -> atendido.corregirPorAutor(datos("Reescritura"), cambio(AUTOR, 120, Optional.empty())));

        var aclarado = atendido.aclararPorAutor("Precisión posterior del reportante", cambio(AUTOR, 120, Optional.empty()));
        assertEquals("Descripción atendida", aclarado.datos().descripcion());
        assertEquals(atendido.revisiones(), aclarado.revisiones());
        assertEquals(1, aclarado.aclaraciones().size());
        assertEquals(AUTOR, aclarado.aclaraciones().getFirst().cambio().autorId());
        assertTrue(atendido.aclaraciones().isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> atendido.aclararPorAutor("Aclaración ajena", cambio(TOE, 120, Optional.empty())));
        assertThrows(IllegalArgumentException.class,
                () -> atendido.aclararPorAutor(" ", cambio(AUTOR, 120, Optional.empty())));
    }

    @Test
    void correccionInstitucionalSustancialTrasAtencionExigeMotivoYConservaLaHistoria() {
        var atendido = registrar(datos("Descripción original"))
                .marcarAtencionInstitucional(cambio(TOE, 60, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> atendido.corregirInstitucionalmente(
                datos("Descripción sustancialmente corregida"), cambio(TOE, 120, Optional.empty())));

        var motivo = Optional.of("Se rectifica la descripción tras contrastar la comunicación");
        var corregido = atendido.corregirInstitucionalmente(datos("Descripción sustancialmente corregida"),
                cambio(TOE, 120, motivo));

        assertEquals("Descripción original", corregido.revisiones().getFirst().datos().descripcion());
        assertEquals("Descripción sustancialmente corregida", corregido.datos().descripcion());
        assertEquals(motivo, corregido.revisiones().getLast().cambio().motivo());
        assertEquals(AUTOR, corregido.autorId());
        assertEquals(TOE, corregido.revisiones().getLast().cambio().autorId());
        assertEquals(atendido.inicioAtencion(), corregido.inicioAtencion());
    }

    @Test
    void cambioDeParticipacionTrasAtencionTambienExigeCausa() {
        var atendido = registrar(datos("Descripción"))
                .marcarAtencionInstitucional(cambio(TOE, 60, Optional.empty()));
        var corroborados = datos("Descripción", List.of(participacion(PapelParticipacion.ATRIBUCION_CORROBORADA)),
                COTIDIANA, FechaHecho.exacta(DIA_PERU), Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> atendido.corregirInstitucionalmente(corroborados,
                cambio(TOE, 120, Optional.empty())));
        assertEquals(PapelParticipacion.ATRIBUCION_CORROBORADA,
                atendido.corregirInstitucionalmente(corroborados,
                        cambio(TOE, 120, Optional.of("Aclaración institucional documentada")))
                        .datos().participaciones().getFirst().papel());
    }

    @Test
    void anularExigeCausaYConservaElContenidoOriginal() {
        var original = registrar(datos("Reporte creado por error"));
        assertThrows(IllegalArgumentException.class,
                () -> original.anularPorError(cambio(TOE, 60, Optional.empty())));
        var cambio = cambio(TOE, 60, Optional.of("Se registró en la identidad equivocada"));
        var anulado = original.anularPorError(cambio);

        assertTrue(anulado.anulado());
        assertFalse(original.anulado());
        assertEquals(original.id(), anulado.id());
        assertEquals(original.datos(), anulado.datos());
        assertEquals(original.revisiones(), anulado.revisiones());
        assertEquals(Optional.of(cambio), anulado.anulacion());
        assertEquals(AUTOR, anulado.autorId());
        assertThrows(IllegalStateException.class,
                () -> anulado.corregirInstitucionalmente(datos("Cambio posterior"), cambio(TOE, 120, Optional.empty())));
        assertThrows(IllegalStateException.class,
                () -> anulado.aclararPorAutor("Aclaración posterior", cambio(AUTOR, 120, Optional.empty())));
    }

    @Test
    void cambiosConFechaAnteriorAlContenidoVigenteSeRechazan() {
        var atendido = registrar(datos("Descripción"))
                .marcarAtencionInstitucional(cambio(TOE, 60, Optional.empty()));

        assertThrows(IllegalArgumentException.class,
                () -> atendido.aclararPorAutor("Aclaración fuera de orden", cambio(AUTOR, 30, Optional.empty())));
        assertThrows(IllegalArgumentException.class,
                () -> atendido.anularPorError(cambio(TOE, 30, Optional.of("Error de registro"))));
    }

    @Test
    void entradasYRevisionesSonInmutablesYNoCompartenLaListaRecibida() {
        var entrada = new ArrayList<>(List.of(participacion(PapelParticipacion.TESTIGO)));
        var datos = datos("Descripción", entrada, COTIDIANA, FechaHecho.exacta(DIA_PERU), Optional.empty());
        entrada.clear();
        var reporte = registrar(datos);

        assertEquals(1, reporte.datos().participaciones().size());
        assertThrows(UnsupportedOperationException.class, () -> datos.participaciones().clear());
        assertThrows(UnsupportedOperationException.class, () -> reporte.revisiones().clear());
        assertThrows(UnsupportedOperationException.class, () -> reporte.aclaraciones().clear());
    }

    private static ReporteIncidencia registrar(DatosReporte datos) {
        return ReporteIncidencia.registrar(UUID.randomUUID(), datos, AUTOR, REGISTRO);
    }

    private static RegistroCambio cambio(UUID autor, long segundosDesdeRegistro, Optional<String> motivo) {
        return new RegistroCambio(autor, REGISTRO.plusSeconds(segundosDesdeRegistro), motivo);
    }

    private static DatosReporte datos(String descripcion) {
        return datos(descripcion, List.of(participacion(PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA)),
                COTIDIANA, FechaHecho.exacta(DIA_PERU), Optional.empty());
    }

    private static DatosReporte datos(String descripcion, List<Participacion> participaciones,
                                      ClasificacionCategoria clasificacion, FechaHecho fecha,
                                      Optional<LocalTime> hora) {
        return new DatosReporte(fecha, 2026, clasificacion, descripcion, participaciones, false, hora,
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static Participacion participacion(PapelParticipacion papel) {
        return new Participacion(PersonaInvolucrada.estudiante(ESTUDIANTE, "Estudiante ficticio"), papel, Optional.empty());
    }

    private static ClasificacionCategoria categoria(NaturalezaCategoria naturaleza, Gravedad gravedad) {
        return new ClasificacionCategoria(UUID.randomUUID(), "Categoría de prueba", naturaleza, gravedad, false);
    }
}
