package pe.edu.incidencias.domain.consulta;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.incidencia.*;
import pe.edu.incidencias.domain.estudiante.GrupoEscolar;
import pe.edu.incidencias.domain.alerta.RegistroAlertas;
import pe.edu.incidencias.domain.catalogo.*;
import pe.edu.incidencias.domain.proteccion.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.consulta.DatosConsulta.*;
import static pe.edu.incidencias.domain.consulta.ConsultaEstadistica.Dimension.*;
import static pe.edu.incidencias.domain.consulta.ConsultaEstadistica.Medida.*;

class AuditoriaConsultasTest {
    @Test void hechoAutorizadoNoRevelaCasoCompartidoRestringidoPorOtroHecho() {
        var primero = hecho(100, 50, PapelParticipacion.AFECTADO);
        var segundo = hecho(101, 51, PapelParticipacion.TESTIGO);
        var exclusion = new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias()).excluir(id(900), TOE,
                ContextoAcceso.deHecho(segundo), new RegistroCambio(DIRECTOR, BASE.plusSeconds(5), Optional.of("Conflicto ficticio")));
        var r = pe.edu.incidencias.domain.atencion.RegistroAtenciones.iniciar(List.of(primero, segundo), exclusion)
                .abrirCaso(id(200), Set.of(id(100), id(101)), DIA, DIRECTOR, List.of(), Optional.empty(), directorio(),
                        new RegistroCambio(DIRECTOR, BASE.plusSeconds(20), Optional.empty()));
        // Una política incompleta jamás debe quitar las restricciones conservadas por el registro.
        var politica = new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias(), r.proteccion());
        var historial = new ConsultaHistorial(r, Set.of(id(100)), politica).consultar(
                ConsultaHistorial.Filtro.dePersona(ESTUDIANTE), 0, 10, TOE, BASE.plusSeconds(100));
        assertEquals(1, historial.entradas().size());
        assertTrue(historial.entradas().getFirst().atenciones().isEmpty());
        var m = new MotorEstadisticas(r, RegistroAlertas.vacio(), matriculas(), Set.of(id(100), id(101)), politica);
        var q = new ConsultaEstadistica(HECHOS, periodo(), List.of(ESTADO_ATENCION, INTERVENCION_COMITE), Map.of());
        assertEquals(List.of(new ResultadoEstadistico.Celda(List.of("SIN_ATENCION", "NO"), 1)),
                m.calcular(q, TOE, BASE.plusSeconds(100)).celdas());
    }

    @Test void busquedaDePersonaToleraTildesMayusculasYEspaciosSinCargarPadron() {
        var persona = PersonaInvolucrada.estudiante(ESTUDIANTE, "María  Núñez");
        var h = Hecho.de(reporte(100, List.of(new Participacion(persona, PapelParticipacion.AFECTADO, Optional.empty())),
                FechaHecho.exacta(DIA), Optional.empty()));
        var r = registro(h);
        var filtro = new ConsultaHistorial.Filtro(Optional.empty(), Optional.of("MARIA\u00a0NUNEZ"),
                Optional.empty(), Optional.empty(), Map.of());
        var historial = new ConsultaHistorial(r, Set.of(id(100)), politica(r));
        assertEquals(1, historial.consultar(filtro, 0, 10, TOE, BASE.plusSeconds(100)).entradas().size());
        assertEquals(1, historial.buscarAportesPropios(filtro, 0, 10, DOCENTE, BASE.plusSeconds(100)).aportes().size());
        assertThrows(IllegalArgumentException.class, () -> new ConsultaHistorial.Filtro(Optional.empty(),
                Optional.of("\u00a0"), Optional.empty(), Optional.empty(), Map.of()));
    }

    @Test void senalesPendientesSeAgrupanPorMotivosVigentesSinReactivarGravedadRetirada() {
        var base = hecho(100, 50, PapelParticipacion.AFECTADO).principal().datos();
        var grave = new ClasificacionCategoria(id(81), "Categoría grave ficticia", NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.GRAVE, false);
        var h = Hecho.de(ReporteIncidencia.registrar(id(100), new DatosReporte(base.fechaHecho(), 2026, grave,
                base.descripcion(), base.participaciones(), true, base.hora(), base.lugar(), base.comunicante(), base.canal()), DOCENTE, BASE.plusSeconds(1)));
        var r = registro(h);
        var alertas = RegistroAlertas.vacio().evaluarHecho(h, ContextoAcceso.deHecho(h), politica(r), cambio(10));
        var corregido = h.precisarDatos(base.fechaHecho(), base.clasificacion(), cambio(20));
        var actual = registro(corregido);
        alertas = alertas.evaluarHecho(corregido, ContextoAcceso.deHecho(corregido), politica(actual), cambio(30));
        var m = new MotorEstadisticas(actual, alertas, matriculas(), Set.of(id(100)), politica(actual));
        var consulta = new ConsultaEstadistica(ALERTAS_PENDIENTES, periodo(), List.of(MOTIVO_ALERTA), Map.of());
        assertEquals(List.of(new ResultadoEstadistico.Celda(List.of("RIESGO_COMUNICADO"), 1)),
                m.calcular(consulta, TOE, BASE.plusSeconds(100)).celdas());
        var emitidas = new ConsultaEstadistica(ALERTAS_EMITIDAS, periodo(), List.of(MOTIVO_ALERTA), Map.of());
        assertEquals(2, m.calcular(emitidas, TOE, BASE.plusSeconds(100)).celdas().size());
    }

    @Test void omisionesEnDuplicadoNoContradicenGrupoYHoraConocidos() {
        var duplicado = reporte(101, List.of(new Participacion(persona(50), PapelParticipacion.AFECTADO,
                Optional.of(new GrupoEscolar("1", "a")))), FechaHecho.exacta(DIA), Optional.empty());
        var parcial = reporte(102, List.of(new Participacion(persona(50), PapelParticipacion.AFECTADO,
                Optional.of(new GrupoEscolar(Optional.of("1"), Optional.empty())))), FechaHecho.exacta(DIA), Optional.empty());
        var h = hecho(100, 50, PapelParticipacion.AFECTADO).vincularDuplicado(duplicado, cambio(10)).vincularDuplicado(parcial, cambio(11));
        var r = registro(h);
        var q = new ConsultaEstadistica(HECHOS, periodo(), List.of(GRADO, SECCION, HORA), Map.of());
        assertEquals(List.of("1", "A", "10"), motor(r, 100).calcular(q, TOE, BASE.plusSeconds(100))
                .celdas().getFirst().coordenadas());
        var filtro = new ConsultaHistorial.Filtro(Optional.of(ESTUDIANTE), Optional.empty(), Optional.empty(),
                Optional.empty(), Map.of(SECCION, Set.of("A")));
        assertEquals(1, new ConsultaHistorial(r, Set.of(id(100)), politica(r))
                .consultar(filtro, 0, 10, TOE, BASE.plusSeconds(100)).entradas().size());
    }

    @Test void historialAnuladoConservaFiltroDeEstadoInstitucional() {
        var r = registro(hecho(100, 50, PapelParticipacion.AFECTADO))
                .abrirCaso(id(200), Set.of(id(100)), DIA, TOE, List.of(), Optional.empty(), directorio(), cambio(20));
        var h = r.hechoVigente(id(100));
        r = r.actualizarHecho(h.actualizarReporte(h.principal().anularPorError(cambio(30))), directorio(), cambio(30));
        var filtro = new ConsultaHistorial.Filtro(Optional.of(ESTUDIANTE), Optional.empty(), Optional.empty(),
                Optional.empty(), Map.of(ESTADO_ATENCION, Set.of("PENDIENTE")));
        assertEquals(1, new ConsultaHistorial(r, Set.of(id(100)), politica(r))
                .consultar(filtro, 0, 10, TOE, BASE.plusSeconds(100)).entradas().size());
    }

    @Test void historialAnuladoCombinaFechaYCategoriaDeLaMismaRevision() {
        var original = hecho(100, 50, PapelParticipacion.AFECTADO).principal();
        var base = original.datos();
        var otraCategoria = new ClasificacionCategoria(id(82), "Otra categoría", NaturalezaCategoria.OTRA_CONVIVENCIA, Gravedad.LEVE, false);
        var correccion = new DatosReporte(FechaHecho.exacta(DIA.minusDays(2)), 2026, otraCategoria,
                base.descripcion(), base.participaciones(), false, base.hora(), base.lugar(), base.comunicante(), base.canal());
        var anulado = original.corregirInstitucionalmente(correccion, cambio(10)).anularPorError(cambio(20));
        var r = registro(Hecho.de(anulado));
        var historial = new ConsultaHistorial(r, Set.of(id(100)), politica(r));
        var originalFiltro = new ConsultaHistorial.Filtro(Optional.of(ESTUDIANTE), Optional.empty(),
                Optional.of(new ConsultaEstadistica.Periodo(DIA, DIA, false)), Optional.empty(), Map.of(CATEGORIA, Set.of(id(80).toString())));
        assertEquals(1, historial.consultar(originalFiltro, 0, 10, TOE, BASE.plusSeconds(100)).entradas().size());
        var cruzado = new ConsultaHistorial.Filtro(Optional.of(ESTUDIANTE), Optional.empty(),
                Optional.of(new ConsultaEstadistica.Periodo(DIA.minusDays(2), DIA.minusDays(2), false)), Optional.empty(),
                Map.of(CATEGORIA, Set.of(id(80).toString())));
        assertTrue(historial.consultar(cruzado, 0, 10, TOE, BASE.plusSeconds(100)).entradas().isEmpty());
    }

    @Test void coberturaExportadaRevalidaFuentesFueraDelPeriodoSeleccionado() {
        var r = registro(hecho(100, 50, PapelParticipacion.AFECTADO));
        var q = ConsultaEstadistica.de(HECHOS, new ConsultaEstadistica.Periodo(DIA.minusMonths(2), DIA.minusMonths(1), false));
        var resultado = motor(r, 100).calcular(q, TOE, BASE.plusSeconds(100));
        assertEquals(0, resultado.total());
        assertEquals(1, resultado.cobertura().unidadesEvaluadas());
        var salida = SalidaEstadistica.interna(id(800), resultado, r, politica(r), cambio(110));
        var alcance = AlcanceProteccion.persona(ESTUDIANTE);
        var autorizacion = new AutorizacionProteccion(id(900), ADMIN, Set.of(AutorizacionProteccion.Operacion.BLOQUEAR),
                alcance, new Vigencia(BASE, Optional.empty()), "Autoridad ficticia", "Constancia ficticia", "Decisión ficticia", BASE);
        var bloqueo = ControlProteccion.vacio().bloquear(id(901), alcance, autorizacion,
                new RegistroCambio(ADMIN, BASE.plusSeconds(120), Optional.of("Decisión de reserva")), directorio());
        var actual = r.conProteccion(bloqueo);
        assertThrows(SecurityException.class, () -> salida.descargar(SalidaEstadistica.Formato.CSV, TOE,
                actual, politica(actual), BASE.plusSeconds(130)));
    }

    @Test void salidaDeHechosRevalidaCasoQueAportaEstadoAlCorte() {
        var r = registro(hecho(100, 50, PapelParticipacion.AFECTADO), hecho(101, 51, PapelParticipacion.TESTIGO))
                .abrirCaso(id(200), Set.of(id(100), id(101)), DIA, TOE, List.of(), Optional.empty(), directorio(), cambio(20));
        var q = new ConsultaEstadistica(HECHOS, periodo(), List.of(ESTADO_ATENCION), Map.of(SEXO, Set.of("F")));
        var resultado = motor(r, 100, 101).calcular(q, TOE, BASE.plusSeconds(100));
        assertEquals(List.of(new ResultadoEstadistico.Celda(List.of("PENDIENTE"), 1)), resultado.celdas());
        var salida = SalidaEstadistica.interna(id(800), resultado, r, politica(r), cambio(110));
        var actual = r.excluirUsuario(id(200), id(900), TOE, new AmbitoReserva(Set.of(id(101)), Set.of(), Set.of()),
                directorio(), new RegistroCambio(DIRECTOR, BASE.plusSeconds(120), Optional.of("Conflicto sobrevenido")));
        assertThrows(SecurityException.class, () -> salida.descargar(SalidaEstadistica.Formato.CSV, TOE,
                actual, politica(actual), BASE.plusSeconds(130)));
    }
}
