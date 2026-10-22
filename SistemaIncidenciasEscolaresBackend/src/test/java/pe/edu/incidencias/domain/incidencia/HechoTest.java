package pe.edu.incidencias.domain.incidencia;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.alerta.CalculadoraReiteracion;
import pe.edu.incidencias.domain.catalogo.Categoria;
import pe.edu.incidencias.domain.catalogo.ClasificacionCategoria;
import pe.edu.incidencias.domain.catalogo.DefinicionCategoria;
import pe.edu.incidencias.domain.catalogo.Gravedad;
import pe.edu.incidencias.domain.catalogo.NaturalezaCategoria;
import pe.edu.incidencias.domain.catalogo.ParametrosReiteracion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HechoTest {
    private static final Instant REGISTRO = Instant.parse("2026-10-21T15:00:00Z");
    private static final LocalDate FECHA_HECHO = LocalDate.of(2026, 10, 20);
    private static final UUID OPERADOR = id(1);
    private static final UUID CATEGORIA_ID = id(2);
    private static final PersonaInvolucrada ANA = PersonaInvolucrada.estudiante(id(10), "Ana Ficticia");
    private static final PersonaInvolucrada LUIS = PersonaInvolucrada.estudiante(id(11), "Luis Ficticio");
    private static final ClasificacionCategoria LEVE = new ClasificacionCategoria(CATEGORIA_ID,
            "Conducta ficticia", NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE, false);

    @Test
    void vincularConservaRelatosAutoresYProcedenciaSinMutarElPrincipal() {
        ReporteIncidencia principal = reporte(100, 20, "Relato del primer docente",
                participacion(ANA, PapelParticipacion.TESTIGO));
        ReporteIncidencia duplicado = reporte(101, 21, "Aporte distinto del auxiliar",
                participacion(LUIS, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA));
        Hecho original = Hecho.de(principal);

        Hecho vinculado = original.vincularDuplicado(duplicado, cambio(1));

        assertAll(
                () -> assertEquals(principal.id(), vinculado.id()),
                () -> assertSame(principal, vinculado.principal()),
                () -> assertEquals(List.of(principal, duplicado), vinculado.reportes()),
                () -> assertEquals("Aporte distinto del auxiliar", vinculado.reportes().getLast().datos().descripcion()),
                () -> assertEquals(id(21), buscar(vinculado, LUIS.id()).aportes().getFirst().autorId()),
                () -> assertEquals(duplicado.id(), buscar(vinculado, LUIS.id()).aportes().getFirst().reporteId()),
                () -> assertEquals(REGISTRO, buscar(vinculado, LUIS.id()).aportes().getFirst().fechaRegistro()),
                () -> assertEquals(1, original.reportes().size()),
                () -> assertEquals(Hecho.Operacion.VINCULAR_DUPLICADO, vinculado.vinculos().getFirst().operacion()));
    }

    @Test
    void atribucionQueSoloConstaEnDuplicadoParticipaDelMismoHechoComputable() {
        Hecho hecho = Hecho.de(reporte(100, 20, "Relato principal", participacion(ANA, PapelParticipacion.TESTIGO)))
                .vincularDuplicado(reporte(101, 21, "Otro aporte",
                        participacion(LUIS, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA)), cambio(1));

        assertEquals(Optional.of(PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA),
                buscar(hecho, LUIS.id()).papelVigente());
        assertEquals(1, contar(hecho, LUIS.id()));
        assertEquals(0, contar(hecho, ANA.id()));
    }

    @Test
    void personaInequivocaSeConsolidaUnaVezConAmbasFuentesYUnaOcurrencia() {
        Hecho hecho = Hecho.de(reporte(100, 20, "Primer relato",
                        participacion(ANA, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA)))
                .vincularDuplicado(reporte(101, 21, "Segundo relato",
                        participacion(ANA, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA)), cambio(1));

        assertEquals(1, hecho.participaciones().size());
        assertEquals(2, buscar(hecho, ANA.id()).aportes().size());
        assertFalse(buscar(hecho, ANA.id()).discrepancia());
        assertEquals(1, contar(hecho, ANA.id()));
    }

    @Test
    void externosConMismaDescripcionEIdentidadesDistintasNoSeUnificanPorConjetura() {
        var primeraPersona = new PersonaInvolucrada(id(30), TipoPersona.EXTERNA,
                "Adulto de identidad desconocida", Optional.empty(), Optional.empty());
        var segundaPersona = new PersonaInvolucrada(id(31), TipoPersona.EXTERNA,
                "Adulto de identidad desconocida", Optional.empty(), Optional.empty());
        Hecho hecho = Hecho.de(reporte(100, 20, "Primer relato",
                        participacion(primeraPersona, PapelParticipacion.PRESUNTA_RESPONSABILIDAD)))
                .vincularDuplicado(reporte(101, 21, "Segundo relato",
                        participacion(segundaPersona, PapelParticipacion.PRESUNTA_RESPONSABILIDAD)), cambio(1));

        assertEquals(2, hecho.participaciones().size());
        assertEquals(List.of(primeraPersona.id(), segundaPersona.id()),
                hecho.participaciones().stream().map(Hecho.ParticipacionConsolidada::personaId).toList());
        assertTrue(hecho.participaciones().stream().allMatch(p -> p.persona().estudianteId().isEmpty()));
    }

    @Test
    void contradiccionQuedaFueraDelContadorHastaAclararseYConservaPapelesOriginales() {
        Hecho contradictorio = contradictorio();

        assertTrue(buscar(contradictorio, ANA.id()).discrepancia());
        assertTrue(buscar(contradictorio, ANA.id()).papelVigente().isEmpty());
        assertEquals(0, contar(contradictorio, ANA.id()));

        Hecho aclarado = contradictorio.aclararParticipacion(ANA.id(), PapelParticipacion.ATRIBUCION_CORROBORADA,
                "Aclaración institucional ficticia", cambio(2));

        assertEquals(1, contar(aclarado, ANA.id()));
        assertEquals(Optional.of(PapelParticipacion.ATRIBUCION_CORROBORADA), buscar(aclarado, ANA.id()).papelVigente());
        assertTrue(buscar(aclarado, ANA.id()).aclaracionVigente().isPresent());
        assertEquals(List.of(PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA, PapelParticipacion.TESTIGO),
                buscar(aclarado, ANA.id()).aportes().stream().map(a -> a.participacion().papel()).toList());
        assertTrue(buscar(contradictorio, ANA.id()).papelVigente().isEmpty());
    }

    @Test
    void aclaracionSoloAfectaAPersonaYHechoSeleccionados() {
        Hecho compartido = Hecho.de(reporte(100, 20, "Primer relato",
                        participacion(ANA, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA),
                        participacion(LUIS, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA)))
                .vincularDuplicado(reporte(101, 21, "Segundo relato",
                        participacion(ANA, PapelParticipacion.TESTIGO), participacion(LUIS, PapelParticipacion.TESTIGO)), cambio(1));
        Hecho otroHecho = Hecho.de(reporte(102, 20, "Otro suceso", participacion(ANA, PapelParticipacion.TESTIGO)));

        Hecho aclarado = compartido.aclararParticipacion(ANA.id(), PapelParticipacion.ATRIBUCION_CORROBORADA,
                "Aclaración solo de Ana en el primer hecho", cambio(2));

        assertEquals(Optional.of(PapelParticipacion.ATRIBUCION_CORROBORADA), buscar(aclarado, ANA.id()).papelVigente());
        assertTrue(buscar(aclarado, LUIS.id()).papelVigente().isEmpty());
        assertTrue(buscar(aclarado, LUIS.id()).aclaracionVigente().isEmpty());
        assertEquals(Optional.of(PapelParticipacion.TESTIGO), buscar(otroHecho, ANA.id()).papelVigente());
        assertTrue(otroHecho.aclaraciones().isEmpty());
    }

    @Test
    void unaFuenteNuevaInvalidaAclaracionAnteriorAunqueRepitaUnoDeLosPapeles() {
        Hecho aclarado = contradictorio().aclararParticipacion(ANA.id(), PapelParticipacion.ATRIBUCION_CORROBORADA,
                "Aclaración de las dos fuentes conocidas", cambio(2));

        Hecho conNuevaFuente = aclarado.vincularDuplicado(reporte(102, 22, "Tercer aporte conocido después",
                participacion(ANA, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA)), cambio(3));

        assertEquals(1, conNuevaFuente.aclaraciones().size());
        assertEquals(2, conNuevaFuente.aclaraciones().getFirst().fuentes().size());
        assertEquals(3, buscar(conNuevaFuente, ANA.id()).aportes().size());
        assertTrue(buscar(conNuevaFuente, ANA.id()).aclaracionVigente().isEmpty());
        assertTrue(buscar(conNuevaFuente, ANA.id()).papelVigente().isEmpty());
        assertEquals(0, contar(conNuevaFuente, ANA.id()));
        assertEquals(1, contar(aclarado, ANA.id()));
    }

    @Test
    void descartarAtribucionExcluyeDelContadorSinBorrarParticipanteNiFuentes() {
        Hecho original = contradictorio();
        Hecho descartado = original.aclararParticipacion(ANA.id(), PapelParticipacion.ATRIBUCION_DESCARTADA,
                "La atribución no corresponde a esta persona", cambio(2));

        assertEquals(0, contar(descartado, ANA.id()));
        assertEquals(Optional.of(PapelParticipacion.ATRIBUCION_DESCARTADA), buscar(descartado, ANA.id()).papelVigente());
        assertEquals(2, buscar(descartado, ANA.id()).aportes().size());
        assertEquals(original.reportes().stream().map(ReporteIncidencia::id).toList(),
                descartado.reportes().stream().map(ReporteIncidencia::id).toList());
        assertEquals(original.reportes().stream().map(ReporteIncidencia::revisiones).toList(),
                descartado.reportes().stream().map(ReporteIncidencia::revisiones).toList());
        assertFalse(descartado.anulado());
    }

    @Test
    void retirarDuplicadoRequiereCausaPuedeRevertirseYConservaHistoriaDelVinculo() {
        ReporteIncidencia duplicado = reporte(101, 21, "Aporte conservado",
                participacion(LUIS, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA));
        Hecho vinculado = Hecho.de(reporte(100, 20, "Principal", participacion(ANA, PapelParticipacion.TESTIGO)))
                .vincularDuplicado(duplicado, cambio(1));

        assertThrows(IllegalArgumentException.class, () -> vinculado.retirarDuplicado(duplicado.id(), cambio(2)));
        Hecho retirado = vinculado.retirarDuplicado(duplicado.id(),
                cambioConMotivo(2, "Se comprobó que corresponde a otro hecho"));
        Hecho relacionadoOtraVez = retirado.vincularDuplicado(duplicado, cambio(3));

        assertEquals(1, retirado.reportes().size());
        assertEquals(2, vinculado.reportes().size());
        assertEquals(List.of(Hecho.Operacion.VINCULAR_DUPLICADO, Hecho.Operacion.RETIRAR_DUPLICADO,
                        Hecho.Operacion.VINCULAR_DUPLICADO),
                relacionadoOtraVez.vinculos().stream().map(Hecho.Vinculo::operacion).toList());
        assertSame(duplicado, relacionadoOtraVez.reportes().getLast());
        assertEquals(1, contar(relacionadoOtraVez, LUIS.id()));
        assertEquals(0, contar(retirado, LUIS.id()));
    }

    @Test
    void noPuedeRetirarseElPrincipalNiDuplicarseUnReporteYaVinculado() {
        ReporteIncidencia principal = reporte(100, 20, "Principal", participacion(ANA, PapelParticipacion.TESTIGO));
        Hecho hecho = Hecho.de(principal);

        assertThrows(IllegalArgumentException.class, () -> hecho.vincularDuplicado(principal, cambio(1)));
        assertThrows(IllegalArgumentException.class,
                () -> hecho.retirarDuplicado(principal.id(), cambioConMotivo(1, "Motivo ficticio")));
    }

    @Test
    void anularPrincipalConservaElHechoYElComputoDeLosOtrosReportesValidos() {
        ReporteIncidencia principal = reporte(100, 20, "Principal que pide atención", LEVE, true,
                participacion(ANA, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA));
        Hecho original = Hecho.de(principal).vincularDuplicado(reporte(101, 21, "Aporte adicional",
                participacion(LUIS, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA)), cambio(1));
        ReporteIncidencia anulado = principal.anularPorError(cambioConMotivo(2, "Registro creado por error"));

        Hecho hecho = original.actualizarReporte(anulado);

        assertTrue(original.requiereAvisoInmediato());
        assertFalse(hecho.anulado());
        assertFalse(hecho.requiereAvisoInmediato());
        assertEquals(1, hecho.participaciones().size());
        assertEquals(0, contar(hecho, ANA.id()));
        assertEquals(1, contar(hecho, LUIS.id()));
        assertEquals(2, hecho.reportes().size());
        assertEquals(1, hecho.principal().revisiones().size());
        assertEquals(Optional.of("Registro creado por error"), hecho.principal().anulacion().orElseThrow().motivo());
    }

    @Test
    void anularSoloDuplicadoConservaElHechoPrincipalSinContarNiAvisarPorLaFuenteAnulada() {
        ReporteIncidencia duplicado = reporte(101, 21, "Aporte que pide atención", LEVE, true,
                participacion(LUIS, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA));
        Hecho original = Hecho.de(reporte(100, 20, "Principal",
                        participacion(ANA, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA)))
                .vincularDuplicado(duplicado, cambio(1));

        Hecho hecho = original.actualizarReporte(duplicado.anularPorError(cambioConMotivo(2, "Aporte creado por error")));

        assertFalse(hecho.anulado());
        assertFalse(hecho.requiereAvisoInmediato());
        assertEquals(1, contar(hecho, ANA.id()));
        assertEquals(0, contar(hecho, LUIS.id()));
        assertEquals(2, hecho.reportes().size());
        assertTrue(hecho.reportes().getLast().anulado());
    }

    @Test
    void coleccionesConsolidadasYSusFuentesSonInmutables() {
        Hecho hecho = contradictorio();
        var participacion = buscar(hecho, ANA.id());

        assertAll(
                () -> assertThrows(UnsupportedOperationException.class, () -> hecho.reportes().clear()),
                () -> assertThrows(UnsupportedOperationException.class, () -> hecho.vinculos().clear()),
                () -> assertThrows(UnsupportedOperationException.class, () -> hecho.participaciones().clear()),
                () -> assertThrows(UnsupportedOperationException.class, () -> participacion.aportes().clear()));
    }

    private Hecho contradictorio() {
        return Hecho.de(reporte(100, 20, "Atribución del docente",
                        participacion(ANA, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA)))
                .vincularDuplicado(reporte(101, 21, "El auxiliar la identifica como testigo",
                        participacion(ANA, PapelParticipacion.TESTIGO)), cambio(1));
    }

    private int contar(Hecho hecho, UUID estudianteId) {
        var definicion = new DefinicionCategoria("Conducta ficticia", "Parámetros ficticios de prueba",
                NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE, false, true, OptionalInt.of(2));
        var categoria = Categoria.crear(CATEGORIA_ID, definicion, OPERADOR, REGISTRO);
        var parametros = ParametrosReiteracion.sinConfigurar(OPERADOR, REGISTRO)
                .configurarVentana(30, OPERADOR, REGISTRO);
        return CalculadoraReiteracion.evaluar(estudianteId, categoria, parametros,
                LocalDate.of(2026, 10, 21), List.of(hecho)).fundamento().orElseThrow().cantidad();
    }

    private Hecho.ParticipacionConsolidada buscar(Hecho hecho, UUID personaId) {
        return hecho.participaciones().stream().filter(p -> p.personaId().equals(personaId)).findFirst().orElseThrow();
    }

    private ReporteIncidencia reporte(int numero, int autor, String descripcion, Participacion... participaciones) {
        return reporte(numero, autor, descripcion, LEVE, false, participaciones);
    }

    private ReporteIncidencia reporte(int numero, int autor, String descripcion, ClasificacionCategoria clasificacion,
                                       boolean solicitaAtencion, Participacion... participaciones) {
        var datos = new DatosReporte(FechaHecho.exacta(FECHA_HECHO), 2026, clasificacion, descripcion,
                List.of(participaciones), solicitaAtencion, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return ReporteIncidencia.registrar(id(numero), datos, id(autor), REGISTRO);
    }

    private Participacion participacion(PersonaInvolucrada persona, PapelParticipacion papel) {
        return new Participacion(persona, papel, Optional.empty());
    }

    private RegistroCambio cambio(int segundos) {
        return new RegistroCambio(OPERADOR, REGISTRO.plusSeconds(segundos), Optional.empty());
    }

    private RegistroCambio cambioConMotivo(int segundos, String motivo) {
        return new RegistroCambio(OPERADOR, REGISTRO.plusSeconds(segundos), Optional.of(motivo));
    }

    private static UUID id(int numero) {
        return new UUID(0L, numero);
    }
}
