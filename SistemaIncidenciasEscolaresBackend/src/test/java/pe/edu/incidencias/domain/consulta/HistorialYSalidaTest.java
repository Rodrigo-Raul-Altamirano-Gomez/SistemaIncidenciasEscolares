package pe.edu.incidencias.domain.consulta;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.incidencia.*;
import pe.edu.incidencias.domain.estudiante.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.consulta.DatosConsulta.*;
import static pe.edu.incidencias.domain.consulta.ConsultaEstadistica.Dimension.*;
import static pe.edu.incidencias.domain.consulta.ConsultaEstadistica.Medida.*;

class HistorialYSalidaTest {
    @Test void historialConservaAnuladoYActuacionSinInventarParticipacionVigente() {
        var r = registro(hecho(100, 50, PapelParticipacion.AFECTADO));
        var a = pe.edu.incidencias.domain.atencion.Actuacion.realizada(id(300), "Acción real histórica", DIA, TOE, Set.of(), Set.of(), List.of(), cambio(10));
        r = r.abrirCaso(id(200), Set.of(id(100)), DIA, TOE, List.of(a), Optional.empty(), directorio(), cambio(20));
        var h = r.hechoVigente(id(100));
        r = r.actualizarHecho(h.actualizarReporte(h.principal().anularPorError(cambio(30))), directorio(), cambio(30));
        var pagina = new ConsultaHistorial(r, Set.of(id(100)), politica(r)).consultar(
                ConsultaHistorial.Filtro.dePersona(ESTUDIANTE), 0, 10, TOE, BASE.plusSeconds(200));
        assertEquals(1, pagina.entradas().size());
        assertTrue(pagina.entradas().getFirst().hecho().anulado());
        assertTrue(pagina.entradas().getFirst().participacionesBuscadas().isEmpty());
        assertEquals(List.of(a), pagina.entradas().getFirst().actuaciones());
        assertEquals(0, contar(motor(r, 100), HECHOS));
    }
    @Test void autorBuscaSoloSusAportesPorPersonaPeriodoCategoriaYAnio() {
        var r = registro(hecho(100, 50, PapelParticipacion.AFECTADO), hecho(101, 51, PapelParticipacion.TESTIGO));
        var historial = new ConsultaHistorial(r, Set.of(id(100), id(101)), politica(r));
        var filtro = new ConsultaHistorial.Filtro(Optional.of(ESTUDIANTE), Optional.empty(), Optional.of(periodo()), Optional.of(periodo()),
                Map.of(CATEGORIA, Set.of(id(80).toString()), ANIO_ESCOLAR, Set.of("2026")));
        var resultado = historial.buscarAportesPropios(filtro, 0, 10, DOCENTE, BASE.plusSeconds(200));
        assertEquals(1, resultado.aportes().size()); assertEquals(id(100), resultado.aportes().getFirst().reporteId());
        assertTrue(historial.buscarAportesPropios(filtro, 0, 10, TOE, BASE.plusSeconds(200)).aportes().isEmpty());
    }
    @Test void historialDeduplicaIncluyeParticipacionDuplicadaYPaginaSinOmitirAntecedentesPorCambioDeGrupo() {
        var h = hecho(100, 50, PapelParticipacion.AFECTADO).vincularDuplicado(hecho(101, 51, PapelParticipacion.TESTIGO).principal(), cambio(10));
        var r = registro(h, hecho(102, 51, PapelParticipacion.AFECTADO));
        var historial = new ConsultaHistorial(r, Set.of(id(100), id(102)), politica(r));
        var filtro = ConsultaHistorial.Filtro.dePersona(id(51));
        var primero = historial.consultar(filtro, 0, 1, TOE, BASE.plusSeconds(200));
        assertEquals(1, primero.entradas().size()); assertTrue(primero.hayMas());
        assertEquals(2, primero.entradas().getFirst().hecho().participaciones().size());
        assertEquals(1, historial.consultar(filtro, primero.siguienteDesplazamiento(), 1, TOE, BASE.plusSeconds(200)).entradas().size());
    }
    @Test void historialFiltraGrupoSnapshotSinInventarloDelPadronPosterior() {
        var r = registro(hecho(100, 50, PapelParticipacion.AFECTADO));
        var historial = new ConsultaHistorial(r, Set.of(id(100)), politica(r));
        var filtro = new ConsultaHistorial.Filtro(Optional.of(ESTUDIANTE), Optional.empty(), Optional.empty(), Optional.empty(), Map.of(GRADO, Set.of("1"), SECCION, Set.of("A")));
        assertEquals(1, historial.consultar(filtro, 0, 20, TOE, BASE.plusSeconds(200)).entradas().size());
        var desconocido = Hecho.de(reporte(101, List.of(new Participacion(persona(50), PapelParticipacion.TESTIGO, Optional.empty())), FechaHecho.exacta(DIA), Optional.empty()));
        var r2 = registro(desconocido);
        assertEquals(0, new ConsultaHistorial(r2, Set.of(id(101)), politica(r2)).consultar(filtro, 0, 20, TOE, BASE.plusSeconds(200)).entradas().size());
        var q = new ConsultaEstadistica(HECHOS, periodo(), List.of(GRADO), Map.of());
        assertEquals("NO_INFORMADO", motor(r2, 101).calcular(q, TOE, BASE.plusSeconds(200)).celdas().getFirst().coordenadas().getFirst());
    }
    @Test void fuentesHistoricasDiscrepantesSeIdentificanSinResolverConMatricula() {
        var otro = reporte(101, List.of(new Participacion(persona(50), PapelParticipacion.AFECTADO,
                Optional.of(new GrupoEscolar("3", "C")))), FechaHecho.exacta(DIA), Optional.empty());
        var h = hecho(100, 50, PapelParticipacion.AFECTADO).vincularDuplicado(otro, cambio(10));
        var q = new ConsultaEstadistica(HECHOS, periodo(), List.of(GRADO, SECCION), Map.of());
        assertEquals(List.of("DISCREPANTE", "DISCREPANTE"), motor(registro(h), 100).calcular(q, TOE, BASE.plusSeconds(200)).celdas().getFirst().coordenadas());
    }
    @Test void aportePropioNoPermiteHistorialInstitucionalYNoExponeRelatosAjeno() {
        var r = registro(hecho(100, 50, PapelParticipacion.AFECTADO));
        var historial = new ConsultaHistorial(r, Set.of(id(100)), politica(r));
        assertThrows(SecurityException.class, () -> historial.consultar(ConsultaHistorial.Filtro.dePersona(ESTUDIANTE), 0, 10, DOCENTE, BASE.plusSeconds(200)));
        assertEquals(DOCENTE, historial.consultarAportePropio(id(100), DOCENTE, BASE.plusSeconds(200)).autorId());
        assertThrows(SecurityException.class, () -> historial.consultarAportePropio(id(100), TOE, BASE.plusSeconds(200)));
    }
    @Test void reservasOmitenHechoSinTituloTipoNiConteoParticular() {
        var h = hecho(100, 50, PapelParticipacion.AFECTADO);
        var ledger = politica(registro(h)).excluir(id(900), TOE, ContextoAcceso.deHecho(h),
                new RegistroCambio(DIRECTOR, BASE.plusSeconds(10), Optional.of("Exclusión ficticia")));
        var r = pe.edu.incidencias.domain.atencion.RegistroAtenciones.iniciar(List.of(h, hecho(101, 51, PapelParticipacion.TESTIGO)), ledger);
        assertEquals(1, motor(r, 100, 101).calcular(consulta(HECHOS), TOE, BASE.plusSeconds(200)).total());
        assertTrue(new ConsultaHistorial(r, Set.of(id(100), id(101)), politica(r))
                .consultar(ConsultaHistorial.Filtro.dePersona(ESTUDIANTE), 0, 10, TOE, BASE.plusSeconds(200)).entradas().isEmpty());
    }
    @Test void salidaExactaReservadaNoIncluyeNombresNiRelatosYSoportaTresFormatos() {
        var r = registro(hecho(100, 50, PapelParticipacion.AFECTADO));
        var resultado = motor(r, 100).calcular(new ConsultaEstadistica(HECHOS, periodo(), List.of(GRADO), Map.of()), TOE, BASE.plusSeconds(100));
        var salida = SalidaEstadistica.interna(id(800), resultado, r, politica(r), cambio(110));
        for (var formato : SalidaEstadistica.Formato.values()) {
            var tabla = salida.descargar(formato, TOE, r, politica(r), BASE.plusSeconds(200));
            assertEquals(List.of("GRADO", "CANTIDAD"), tabla.columnas());
            assertEquals(List.of(List.of("1", "1")), tabla.filas());
            assertFalse(tabla.toString().contains("Nombre secreto")); assertFalse(tabla.toString().contains("Relato reservado"));
            assertThrows(UnsupportedOperationException.class, () -> tabla.filas().getFirst().clear());
        }
    }
    @Test void descargaNoEntregaSalidaAntiguaCuandoUnaSolaFuentePasaAEstarRestringida() {
        var h = hecho(100, 50, PapelParticipacion.AFECTADO);
        var r = registro(h, hecho(101, 51, PapelParticipacion.TESTIGO));
        var resultado = motor(r, 100, 101).calcular(consulta(HECHOS), TOE, BASE.plusSeconds(100));
        var salida = SalidaEstadistica.interna(id(800), resultado, r, politica(r), cambio(110));
        var ledger = politica(r).excluir(id(900), TOE, ContextoAcceso.deHecho(h),
                new RegistroCambio(DIRECTOR, BASE.plusSeconds(120), Optional.of("Conflicto sobrevenido")));
        var actual = pe.edu.incidencias.domain.atencion.RegistroAtenciones.iniciar(List.of(h, r.hechoVigente(id(101))), ledger);
        assertThrows(SecurityException.class, () -> salida.descargar(SalidaEstadistica.Formato.CSV, TOE, actual, politica(actual), BASE.plusSeconds(200)));
        assertEquals(2, resultado.total());
    }
    @Test void salidaExternaRequiereRevisionYSalidaVaciaNoOmiteValidacionFuncional() {
        var r = registro();
        var resultado = motor(r).calcular(consulta(HECHOS), TOE, BASE.plusSeconds(100));
        var revision = new SalidaEstadistica.RevisionExterna("Estudio agregado", "Constancia institucional", "Entidad competente",
                "Agrupación revisada; no permite identificación", true, cambio(110));
        var salida = SalidaEstadistica.externa(id(800), resultado, revision, r, politica(r));
        assertTrue(salida.descargar(SalidaEstadistica.Formato.PDF, TOE, r, politica(r), BASE.plusSeconds(200)).revisionExterna().isPresent());
        assertThrows(SecurityException.class, () -> salida.descargar(SalidaEstadistica.Formato.PDF, DOCENTE, r, politica(r), BASE.plusSeconds(200)));
        assertThrows(IllegalArgumentException.class, () -> new SalidaEstadistica.RevisionExterna("Fin", "Base", "Destinatario", "Pendiente", false, cambio(110)));
    }
}
