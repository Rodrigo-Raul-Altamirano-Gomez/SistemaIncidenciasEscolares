package pe.edu.incidencias.domain.estudiante;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.estudiante.RegistroMatriculas.Estado.*;

class RegistroMatriculasTest {
    private static final Instant INICIO = Instant.parse("2025-01-01T12:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-10-22T15:00:00Z");
    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);
    private static final GrupoEscolar A = new GrupoEscolar("Primero", "A");
    private static final GrupoEscolar B = new GrupoEscolar("Primero", "B");
    private final UUID admin = UUID.randomUUID();
    private final UUID aip = UUID.randomUUID();
    private final UUID rolAip = UUID.randomUUID();
    private final DirectorioUsuarios directorio = directorio();
    private final PoliticaAcceso politica = politica(directorio);

    @Test
    void previsualizarNoModificaElRegistroYConfirmarIncorporaIdentidadYAnioSinGrupo() {
        var original = registro();
        var fila = fila(7, "Pérez Lucía", "01234567");
        var lote = lote(fila);
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(1, vista.cantidad(ALTA));
        assertTrue(original.estudiantes().isEmpty());
        assertTrue(vista.filas().getFirst().avisos().stream().anyMatch(a -> a.contains("Grupo no informado")));
        var confirmado = original.confirmar(vista, politica, cambio());
        var estudiante = confirmado.estudiantes().get(fila.identidadParaAlta());
        assertEquals("01234567", estudiante.documento().orElseThrow().numero());
        assertTrue(estudiante.matricula(2026).isPresent());
        assertTrue(estudiante.grupoEn(2026, FECHA).isEmpty());
        var evidencia = confirmado.confirmaciones().get(lote.id());
        assertEquals(aip, evidencia.cambio().autorId());
        assertEquals(AHORA, evidencia.cambio().fecha());
        assertEquals("Nómina ficticia", evidencia.procedencia());
        assertEquals("Hoja1", evidencia.hoja());
        assertEquals(7, evidencia.filas().getFirst().fila());
        assertEquals(1, evidencia.cantidad(ALTA));
        assertTrue(original.confirmaciones().isEmpty());
    }

    @Test
    void confirmaNoventaYUnIdentidadesSinGrupoYRepetirNoDuplica() {
        var filas = IntStream.range(0, 91).mapToObj(i -> fila(i == 0 ? 7 : i + 14,
                "Estudiante ficticio " + i, "%08d".formatted(i + 1))).toList();
        var lote = lote(UUID.randomUUID(), 2026, FECHA, Optional.empty(), filas, Map.of());
        var original = registro();
        var confirmado = original.confirmar(original.previsualizar(lote, politica, aip, AHORA), politica, cambio());
        assertEquals(91, confirmado.estudiantes().size());
        assertEquals(91, confirmado.confirmaciones().get(lote.id()).cantidad(ALTA));
        assertTrue(confirmado.estudiantes().values().stream().allMatch(e -> e.grupoEn(2026, FECHA).isEmpty()));
        var segundoLote = lote(UUID.randomUUID(), 2026, FECHA, Optional.empty(), filas, Map.of());
        var segundaVista = confirmado.previsualizar(segundoLote, politica, aip, AHORA);
        assertEquals(91, segundaVista.cantidad(SIN_CAMBIOS));
        var repetido = confirmado.confirmar(segundaVista, politica, cambio());
        assertEquals(91, repetido.estudiantes().size());
        assertTrue(repetido.estudiantes().values().stream().allMatch(e -> e.matriculas().size() == 1));
    }

    @Test
    void documentoSeguroVinculaOtroAnioSinPromocionNiRetiroDeAusentes() {
        var e = estudiante("Lucía", "12345678").vincularMatricula(MatriculaAnual.crear(2025,
                LocalDate.of(2025, 3, 1), Optional.of(A), Optional.of("Matriculada")));
        var ausente = estudiante("Ausente", "87654321");
        var original = registro(e, ausente);
        var lote = lote(fila(7, "Lucía", "12345678"));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(Optional.of(e.id()), vista.filas().getFirst().estudianteId());
        var nuevo = original.confirmar(vista, politica, cambio());
        assertEquals(e.matricula(2025), nuevo.estudiantes().get(e.id()).matricula(2025));
        assertTrue(nuevo.estudiantes().get(e.id()).grupoEn(2026, FECHA).isEmpty());
        assertSame(ausente, nuevo.estudiantes().get(ausente.id()));
    }

    @Test
    void coincidenciaSinDocumentoAunqueUnicaRequiereResolucionExpresa() {
        var e = estudiante("Lucía", null);
        var original = registro(e);
        var fila = fila(7, "LUCÍA", null);
        var lote = lote(fila);
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(1, vista.cantidad(REQUIERE_CONCILIACION));
        assertEquals(e.id(), vista.filas().getFirst().candidatos().getFirst().id());
        var resuelto = resolver(lote, Map.of(7, LoteMatricula.Resolucion.vincular(e.id())));
        var confirmado = original.confirmar(original.previsualizar(resuelto, politica, aip, AHORA), politica, cambio());
        assertEquals(1, confirmado.estudiantes().size());
        assertTrue(confirmado.estudiantes().get(e.id()).matricula(2026).isPresent());
    }

    @Test
    void homonimosConDocumentoNoSeEligenPorOrden() {
        var primero = estudiante("Lucía", "12345678");
        var segundo = estudiante("Lucía", "87654321");
        var vista = registro(primero, segundo).previsualizar(lote(fila(7, "Lucía", null)), politica, aip, AHORA);
        assertEquals(REQUIERE_CONCILIACION, vista.filas().getFirst().estado());
        assertEquals(2, vista.filas().getFirst().candidatos().size());
        assertTrue(vista.filas().getFirst().estudianteId().isEmpty());
    }

    @Test
    void documentoContradictorioRequiereRevisionYPermiteCambioDeNombreExpreso() {
        var e = estudiante("Nombre anterior", "12345678");
        var original = registro(e);
        var lote = lote(fila(7, "Nombre rectificado", "12345678"));
        assertEquals(1, original.previsualizar(lote, politica, aip, AHORA).cantidad(REQUIERE_CONCILIACION));
        var resuelto = resolver(lote, Map.of(7, LoteMatricula.Resolucion.vincular(e.id())));
        var vista = original.previsualizar(resuelto, politica, aip, AHORA);
        assertTrue(vista.filas().getFirst().cambios().contains(RegistroMatriculas.Campo.NOMBRE));
        var nuevo = original.confirmar(vista, politica, cambio());
        assertEquals("Nombre rectificado", nuevo.estudiantes().get(e.id()).nombreCompleto());
        assertEquals("Nombre anterior", e.nombreCompleto());
    }

    @Test
    void vincularIdentidadSinDocumentoPermiteIncorporarDniCorrecto() {
        var e = estudiante("Lucía", null);
        var original = registro(e);
        var lote = resolver(lote(fila(7, "Lucía", "01234567")), Map.of(7, LoteMatricula.Resolucion.vincular(e.id())));
        var nuevo = original.confirmar(original.previsualizar(lote, politica, aip, AHORA), politica, cambio());
        assertEquals(1, nuevo.estudiantes().size());
        assertEquals("01234567", nuevo.estudiantes().get(e.id()).documento().orElseThrow().numero());
    }

    @Test
    void altasSinDocumentoSonValidasCuandoNoHayCandidatos() {
        var original = registro();
        var fila = fila(7, "Lucía", null);
        var nuevo = original.confirmar(original.previsualizar(lote(fila), politica, aip, AHORA), politica, cambio());
        assertTrue(nuevo.estudiantes().get(fila.identidadParaAlta()).documento().isEmpty());
    }

    @Test
    void dosNombresIgualesSinDocumentoEnLoteExigenDecidirPeroAdmitenHomonimosDistintos() {
        var original = registro();
        var lote = lote(fila(7, "Lucía Pérez", null), fila(15, "LUCIA  PÉREZ", null));
        assertEquals(2, original.previsualizar(lote, politica, aip, AHORA).cantidad(REQUIERE_CONCILIACION));
        var resuelto = resolver(lote, Map.of(7, LoteMatricula.Resolucion.crear(), 15, LoteMatricula.Resolucion.crear()));
        var vista = original.previsualizar(resuelto, politica, aip, AHORA);
        assertEquals(2, vista.cantidad(ALTA));
        assertEquals(2, original.confirmar(vista, politica, cambio()).estudiantes().size());
    }

    @Test
    void dniRepetidoBloqueaTodasLasFilasIncluidasContradictoriasOInvalidas() {
        var original = registro(estudiante("Lucía", "12345678"));
        for (var nombre : List.of("Lucía", "Otro nombre", "")) {
            for (boolean invertir : List.of(false, true)) {
                var segura = fila(7, "Lucía", "12345678");
                var conflictiva = fila(15, nombre, "12345678");
                var lote = invertir ? lote(conflictiva, segura) : lote(segura, conflictiva);
                var vista = original.previsualizar(lote, politica, aip, AHORA);
                assertEquals(2, vista.cantidad(ERROR));
                var nuevo = original.confirmar(vista, politica, cambio());
                assertEquals(original.estudiantes(), nuevo.estudiantes());
            }
        }
    }

    @Test
    void excluirUnaFilaRepetidaPermiteIncorporarLaOtra() {
        var original = registro();
        var lote = resolver(lote(fila(7, "Lucía", "12345678"), fila(15, "Lucía", "12345678")),
                Map.of(15, LoteMatricula.Resolucion.excluir()));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(1, vista.cantidad(ALTA));
        assertEquals(1, vista.cantidad(EXCLUIDA));
        assertEquals(1, original.confirmar(vista, politica, cambio()).estudiantes().size());
    }

    @Test
    void documentoAmbiguoEnPadronNoSePuedeApropiarNiConResolucionExpresa() {
        var a = estudiante("Lucía", "12345678");
        var b = estudiante("Otra", "12345678");
        var original = registro(a, b);
        var lote = lote(fila(7, "Lucía", "12345678"));
        assertEquals(1, original.previsualizar(lote, politica, aip, AHORA).cantidad(REQUIERE_CONCILIACION));
        for (var resolucion : List.of(LoteMatricula.Resolucion.vincular(a.id()), LoteMatricula.Resolucion.crear())) {
            assertEquals(1, original.previsualizar(resolver(lote, Map.of(7, resolucion)), politica, aip, AHORA).cantidad(ERROR));
        }
    }

    @Test
    void variasFilasDirigidasAMismoUuidNoAplicanLaUltima() {
        var e = estudiante("Lucía", null);
        var original = registro(e);
        var lote = resolver(lote(fila(7, "Lucía", "12345678"), fila(15, "Lucía", "87654321")),
                Map.of(7, LoteMatricula.Resolucion.vincular(e.id()), 15, LoteMatricula.Resolucion.vincular(e.id())));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(2, vista.cantidad(ERROR));
        assertEquals(original.estudiantes(), original.confirmar(vista, politica, cambio()).estudiantes());
    }

    @Test
    void idsDeAltaRepetidosNoSobrescribenFilas() {
        var original = registro();
        var a = fila(7, "Lucía", "12345678");
        var b = new FilaMatricula(15, a.identidadParaAlta(), "Otra", Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(2, original.previsualizar(lote(a, b), politica, aip, AHORA).cantidad(ERROR));
    }

    @Test
    void idParaAltaExistenteOVinculoInexistenteSonErroresLocalizados() {
        var e = estudiante("Lucía", null);
        var original = registro(e);
        var fila = new FilaMatricula(7, e.id(), "Otra", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(1, original.previsualizar(lote(fila), politica, aip, AHORA).cantidad(ERROR));
        var lote = resolver(lote(fila(15, "Otra", null)), Map.of(15, LoteMatricula.Resolucion.vincular(UUID.randomUUID())));
        assertEquals(15, original.previsualizar(lote, politica, aip, AHORA).filas().getFirst().fila());
        assertEquals(1, original.previsualizar(lote, politica, aip, AHORA).cantidad(ERROR));
    }

    @Test
    void omisionesConservanIdentidadOpcionalGrupoYSituacion() {
        var e = estudiante("Lucía", "12345678").incorporarIdentidad(Optional.empty(), Optional.empty(),
                Optional.of(LocalDate.of(2013, 1, 1)), Optional.of("Femenino"))
                .vincularMatricula(MatriculaAnual.crear(2026, FECHA, Optional.of(A), Optional.of("Matriculada")));
        var original = registro(e);
        var lote = resolver(lote(fila(7, "Lucía", null)), Map.of(7, LoteMatricula.Resolucion.vincular(e.id())));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(1, vista.cantidad(SIN_CAMBIOS));
        var actual = original.confirmar(vista, politica, cambio()).estudiantes().get(e.id());
        assertEquals(e.documento(), actual.documento());
        assertEquals(e.fechaNacimiento(), actual.fechaNacimiento());
        assertEquals(e.sexoInformado(), actual.sexoInformado());
        assertEquals(e.matriculas(), actual.matriculas());
    }

    @Test
    void grupoComunCompletaDesconocidoSinReemplazarConocido() {
        var conocido = estudiante("Lucía", "12345678").vincularMatricula(MatriculaAnual.crear(2026, FECHA, Optional.of(A), Optional.empty()));
        var desconocido = estudiante("Otra", "87654321").vincularMatricula(MatriculaAnual.crear(2026, FECHA, Optional.empty(), Optional.empty()));
        var original = registro(conocido, desconocido);
        var lote = lote(UUID.randomUUID(), 2026, FECHA, Optional.of(B), List.of(fila(7, "Lucía", "12345678"), fila(15, "Otra", "87654321")), Map.of());
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(1, vista.cantidad(SIN_CAMBIOS));
        assertEquals(1, vista.cantidad(ACTUALIZACION));
        var actual = original.confirmar(vista, politica, cambio());
        assertEquals(Optional.of(A), actual.estudiantes().get(conocido.id()).grupoEn(2026, FECHA));
        assertEquals(Optional.of(B), actual.estudiantes().get(desconocido.id()).grupoEn(2026, FECHA));
    }

    @Test
    void cambioDeGrupoPosteriorConservaHistoriaYNoModificaOriginal() {
        var e = estudiante("Lucía", "12345678").vincularMatricula(MatriculaAnual.crear(2026, FECHA, Optional.of(A), Optional.empty()));
        var fechaCambio = LocalDate.of(2026, 7, 1);
        var fila = conGrupo(fila(7, "Lucía", "12345678"), B);
        var lote = lote(UUID.randomUUID(), 2026, fechaCambio, Optional.empty(), List.of(fila), Map.of());
        var original = registro(e);
        var actual = original.confirmar(original.previsualizar(lote, politica, aip, AHORA), politica, cambio()).estudiantes().get(e.id());
        assertEquals(Optional.of(A), actual.grupoEn(2026, FECHA));
        assertEquals(Optional.of(B), actual.grupoEn(2026, fechaCambio));
        assertEquals(Optional.of(A), e.grupoEn(2026, fechaCambio));
    }

    @Test
    void grupoContradictorioEnMismaFechaNoDejaIdentidadParcialmenteRectificada() {
        var e = estudiante("Lucía", "12345678").vincularMatricula(MatriculaAnual.crear(2026, FECHA, Optional.of(A), Optional.empty()));
        var original = registro(e);
        var lote = resolver(lote(conGrupo(fila(7, "Nombre nuevo", "12345678"), B)), Map.of(7, LoteMatricula.Resolucion.vincular(e.id())));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(1, vista.cantidad(ERROR));
        assertSame(e, original.confirmar(vista, politica, cambio()).estudiantes().get(e.id()));
    }

    @Test
    void filasInvalidasNoImpidenIncorporarOtrasAceptables() {
        var original = registro();
        var lote = lote(fila(7, "", null), fila(15, "Válida", "12345678"));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(1, vista.cantidad(ERROR));
        assertEquals(1, vista.cantidad(ALTA));
        var actual = original.confirmar(vista, politica, cambio());
        assertEquals(1, actual.estudiantes().size());
        assertEquals(1, actual.confirmaciones().get(lote.id()).cantidad(ERROR));
    }

    @Test
    void nombreConSoloEspaciosUnicodeEsErrorLocalizableSinCrearEstudiante() {
        var original = registro();
        var lote = lote(fila(7, "\u00a0", null), fila(8, "\u00a0", null));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(2, vista.cantidad(ERROR));
        assertTrue(vista.filas().getFirst().avisos().getFirst().contains("Nombre"));
        assertTrue(original.confirmar(vista, politica, cambio()).estudiantes().isEmpty());
    }

    @Test
    void nacimientoPosteriorALaMatriculaEsErrorSinAlta() {
        var fila = new FilaMatricula(7, UUID.randomUUID(), "Lucía", Optional.empty(), Optional.of(FECHA.plusDays(1)),
                Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(1, registro().previsualizar(lote(fila), politica, aip, AHORA).cantidad(ERROR));
    }

    @Test
    void nacimientoOmitidoNoPermiteCrearMatriculaAnteriorAlNacimientoConocido() {
        var e = estudiante("Lucía", "12345678").incorporarIdentidad(Optional.empty(), Optional.empty(),
                Optional.of(LocalDate.of(2014, 1, 1)), Optional.empty());
        var fila = fila(7, "Lucía", "12345678");
        var lote = lote(UUID.randomUUID(), 2013, LocalDate.of(2013, 3, 1), Optional.empty(), List.of(fila), Map.of());
        var original = registro(e);
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(1, vista.cantidad(ERROR));
        assertSame(e, original.confirmar(vista, politica, cambio()).estudiantes().get(e.id()));
    }

    @Test
    void reintentoMismoLoteConservaResultadoOriginalYEsIdempotenteInclusoReconstituido() {
        var original = registro();
        var lote = lote(fila(7, "Lucía", null));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        var confirmado = original.confirmar(vista, politica, cambio());
        assertSame(confirmado, confirmado.confirmar(vista, politica, cambio()));
        var recargado = RegistroMatriculas.cargar(List.copyOf(confirmado.estudiantes().values()), List.copyOf(confirmado.confirmaciones().values()));
        var repetido = recargado.confirmar(recargado.previsualizar(lote, politica, aip, AHORA), politica, cambio());
        assertSame(recargado, repetido);
        assertEquals(1, repetido.estudiantes().size());
        assertEquals(1, repetido.confirmaciones().get(lote.id()).cantidad(ALTA));
    }

    @Test
    void idLoteNoPuedeReutilizarseConOtrosValoresOResoluciones() {
        var original = registro();
        var lote = lote(fila(7, "Lucía", "12345678"));
        var confirmado = original.confirmar(original.previsualizar(lote, politica, aip, AHORA), politica, cambio());
        var alterado = resolver(lote, Map.of(7, LoteMatricula.Resolucion.excluir()));
        assertThrows(IllegalArgumentException.class, () -> confirmado.previsualizar(alterado, politica, aip, AHORA));
        var otraHoja = new LoteMatricula(lote.id(), lote.procedencia(), "Hoja distinta", 2026, FECHA, Optional.empty(), lote.filas(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> confirmado.previsualizar(otraHoja, politica, aip, AHORA));
    }

    @Test
    void propuestaObsoletaNoSobrescribeNuevosEstudiantes() {
        var original = registro();
        var pendiente = original.previsualizar(lote(fila(7, "Lucía", "12345678")), politica, aip, AHORA);
        var otra = original.previsualizar(lote(fila(15, "Otra", "87654321")), politica, aip, AHORA);
        var actualizado = original.confirmar(otra, politica, cambio());
        assertThrows(IllegalArgumentException.class, () -> actualizado.confirmar(pendiente, politica, cambio()));
        assertEquals(1, actualizado.estudiantes().size());
    }

    @Test
    void administradorSinAipNoPuedeRevisarNiConfirmar() {
        var original = registro();
        var lote = lote(fila(7, "Lucía", null));
        assertThrows(SecurityException.class, () -> original.previsualizar(lote, politica, admin, AHORA));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertThrows(SecurityException.class, () -> original.confirmar(vista, politica, new RegistroCambio(admin, AHORA, Optional.empty())));
    }

    @Test
    void revocarAipEntreRevisionYConfirmacionODesactivarCuentaImpideGuardar() {
        var original = registro();
        var vista = original.previsualizar(lote(fila(7, "Lucía", null)), politica, aip, AHORA);
        var revocacion = new RegistroCambio(admin, AHORA.plusSeconds(1), Optional.empty());
        var sinRol = directorio.finalizarDesignacion(aip, rolAip, revocacion.fecha(), revocacion);
        var sinCuenta = directorio.desactivarUsuario(aip, revocacion);
        for (var directorioNuevo : List.of(sinRol, sinCuenta)) {
            assertThrows(SecurityException.class, () -> original.confirmar(vista, politica(directorioNuevo),
                    new RegistroCambio(aip, AHORA.plusSeconds(2), Optional.empty())));
        }
        assertTrue(original.estudiantes().isEmpty());
    }

    @Test
    void otroAipNoPuedeConfirmarVistaAjenaNiConsultarSuResultado() {
        var otro = UUID.randomUUID();
        var d = directorio.registrarUsuario(otro, "Otro AIP", "otro-aip", new RegistroCambio(admin, INICIO, Optional.empty()))
                .asignarRol(otro, UUID.randomUUID(), RolUsuario.AIP, new Vigencia(INICIO, Optional.empty()), new RegistroCambio(admin, INICIO, Optional.empty()));
        var p = politica(d);
        var original = registro();
        var lote = lote(fila(7, "Lucía", null));
        var vista = original.previsualizar(lote, p, aip, AHORA);
        assertThrows(SecurityException.class, () -> original.confirmar(vista, p, new RegistroCambio(otro, AHORA, Optional.empty())));
        var confirmado = original.confirmar(vista, p, cambio());
        assertThrows(SecurityException.class, () -> confirmado.previsualizar(lote, p, otro, AHORA));
        var vistaDelOtro = original.previsualizar(lote, p, otro, AHORA);
        assertThrows(SecurityException.class, () -> confirmado.confirmar(vistaDelOtro, p,
                new RegistroCambio(otro, AHORA, Optional.empty())));
    }

    @Test
    void rechazaConfirmacionAnteriorAVistaYOtrasOperacionesAnterioresAlRegistro() {
        var original = registro();
        var lote = lote(fila(7, "Lucía", null));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertThrows(IllegalArgumentException.class, () -> original.confirmar(vista, politica, new RegistroCambio(aip, AHORA.minusSeconds(1), Optional.empty())));
        var confirmado = original.confirmar(vista, politica, cambio());
        assertThrows(IllegalArgumentException.class, () -> confirmado.previsualizar(lote, politica, aip, AHORA.minusSeconds(1)));
    }

    @Test
    void evidenciaNoConservaDatosDeCandidatosNoImportadosYColeccionesSonInmutables() {
        var original = registro(estudiante("Lucía", "12345678"));
        var lote = lote(fila(7, "Lucía", null));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertFalse(vista.filas().getFirst().candidatos().isEmpty());
        var confirmado = original.confirmar(vista, politica, cambio());
        assertTrue(confirmado.confirmaciones().get(lote.id()).filas().getFirst().candidatos().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> vista.filas().clear());
        assertThrows(UnsupportedOperationException.class, () -> confirmado.estudiantes().clear());
        assertThrows(UnsupportedOperationException.class, () -> confirmado.confirmaciones().clear());
        assertThrows(UnsupportedOperationException.class, () -> confirmado.confirmaciones().get(lote.id()).filas().clear());
    }

    @Test
    void loteCopiaEntradasYRechazaAnioFilaOResolucionInconsistente() {
        var filas = new ArrayList<>(List.of(fila(7, "Lucía", null)));
        var lote = lote(UUID.randomUUID(), 2026, FECHA, Optional.empty(), filas, Map.of());
        filas.clear();
        assertEquals(1, lote.filas().size());
        assertThrows(UnsupportedOperationException.class, () -> lote.filas().clear());
        assertThrows(IllegalArgumentException.class, () -> lote(UUID.randomUUID(), 2025, FECHA, Optional.empty(), lote.filas(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> lote(lote.filas().getFirst(), lote.filas().getFirst()));
        assertThrows(IllegalArgumentException.class, () -> resolver(lote, Map.of(99, LoteMatricula.Resolucion.excluir())));
        assertThrows(IllegalArgumentException.class, () -> new LoteMatricula.Resolucion(LoteMatricula.Accion.CREAR, Optional.of(UUID.randomUUID())));
    }

    private DirectorioUsuarios directorio() {
        var cambio = new RegistroCambio(admin, INICIO, Optional.of("Designación institucional ficticia"));
        return DirectorioUsuarios.inicializar(admin, "Administrador", "admin", cambio)
                .registrarUsuario(aip, "Operador AIP", "aip", cambio)
                .asignarRol(aip, rolAip, RolUsuario.AIP, new Vigencia(INICIO, Optional.empty()), cambio);
    }
    private static PoliticaAcceso politica(DirectorioUsuarios d) { return new PoliticaAcceso(d, RestriccionesAmbito.vacias()); }
    private RegistroCambio cambio() { return new RegistroCambio(aip, AHORA, Optional.empty()); }
    private static RegistroMatriculas registro(Estudiante... estudiantes) { return RegistroMatriculas.cargar(List.of(estudiantes), List.of()); }
    private static Estudiante estudiante(String nombre, String dni) {
        return Estudiante.crear(UUID.randomUUID(), nombre).incorporarIdentidad(Optional.empty(), Optional.ofNullable(dni)
                .map(d -> new DocumentoIdentidad("DNI", d)), Optional.empty(), Optional.empty());
    }
    private static FilaMatricula fila(int numero, String nombre, String dni) {
        return new FilaMatricula(numero, UUID.randomUUID(), nombre, Optional.ofNullable(dni).map(d -> new DocumentoIdentidad("DNI", d)),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    private static FilaMatricula conGrupo(FilaMatricula f, GrupoEscolar grupo) {
        return new FilaMatricula(f.numero(), f.identidadParaAlta(), f.nombreCompleto(), f.documento(), f.nacimiento(), f.sexo(), Optional.of(grupo), f.situacion());
    }
    private static LoteMatricula lote(FilaMatricula... filas) { return lote(UUID.randomUUID(), 2026, FECHA, Optional.empty(), List.of(filas), Map.of()); }
    private static LoteMatricula lote(UUID id, int anio, LocalDate fecha, Optional<GrupoEscolar> grupo, List<FilaMatricula> filas,
                                      Map<Integer, LoteMatricula.Resolucion> resoluciones) {
        return new LoteMatricula(id, "Nómina ficticia", "Hoja1", anio, fecha, grupo, filas, resoluciones);
    }
    private static LoteMatricula resolver(LoteMatricula lote, Map<Integer, LoteMatricula.Resolucion> resoluciones) {
        return lote(lote.id(), lote.anio(), lote.fechaEfectiva(), lote.grupoComun(), lote.filas(), resoluciones);
    }
}
