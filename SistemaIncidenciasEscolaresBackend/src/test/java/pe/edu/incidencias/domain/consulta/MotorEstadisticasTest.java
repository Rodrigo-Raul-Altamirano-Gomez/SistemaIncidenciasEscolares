package pe.edu.incidencias.domain.consulta;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.incidencia.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.consulta.DatosConsulta.*;
import static pe.edu.incidencias.domain.consulta.ConsultaEstadistica.Dimension.*;
import static pe.edu.incidencias.domain.consulta.ConsultaEstadistica.Medida.*;

class MotorEstadisticasTest {
    @Test void personasSoloEnDuplicadoSeConservanSinDuplicarHechoNiEstudiantesRepetidos() {
        var h = hecho(100, 50, PapelParticipacion.AFECTADO).vincularDuplicado(hecho(101, 51, PapelParticipacion.TESTIGO).principal(), cambio(10));
        var r = registro(h, hecho(102, 50, PapelParticipacion.AFECTADO));
        var m = motor(r, 100, 102);
        assertEquals(2, contar(m, HECHOS));
        assertEquals(3, contar(m, COMUNICACIONES));
        assertEquals(3, contar(m, PARTICIPACIONES));
        assertEquals(2, contar(m, ESTUDIANTES));
        assertEquals(1, contar(m, ESTUDIANTES_AFECTADOS));
        assertEquals(1, contar(m, ESTUDIANTES_TESTIGOS));
    }
    @Test void familiaresParticipanPeroNoSeConviertenEnEstudiantes() {
        var familiar = new PersonaInvolucrada(id(60), TipoPersona.FAMILIAR, "Familiar ficticio", Optional.empty(), Optional.empty());
        var r = registro(Hecho.de(reporte(100, List.of(new Participacion(familiar, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, Optional.empty())), FechaHecho.exacta(DIA), Optional.empty())));
        var m = motor(r, 100);
        assertEquals(1, contar(m, PARTICIPACIONES));
        assertEquals(0, contar(m, ESTUDIANTES_PRESUNTA));
    }
    @Test void discrepanciaNoEsResponsabilidadElegibleYSeCuentaPersonaUnaVez() {
        var h = hecho(100, 50, PapelParticipacion.PRESUNTA_RESPONSABILIDAD)
                .vincularDuplicado(hecho(101, 50, PapelParticipacion.AFECTADO).principal(), cambio(10));
        var m = motor(registro(h), 100);
        assertEquals(1, contar(m, PARTICIPACIONES));
        assertEquals(0, contar(m, ESTUDIANTES_PRESUNTA));
        var q = new ConsultaEstadistica(PARTICIPACIONES, periodo(), List.of(PAPEL, ATRIBUCION), Map.of());
        assertEquals(List.of("DISCREPANTE", "DISCREPANTE"), m.calcular(q, TOE, BASE.plusSeconds(200)).celdas().getFirst().coordenadas());
    }
    @Test void anulacionesNoAportanUnHechoNiSusPersonas() {
        var reporte = hecho(100, 50, PapelParticipacion.AFECTADO).principal().anularPorError(cambio(10));
        assertEquals(0, contar(motor(registro(Hecho.de(reporte)), 100), HECHOS));
    }
    @Test void cruceCombinaTresDimensionesConGrupoHistoricoYHoraInformada() {
        var m = motor(registro(hecho(100, 50, PapelParticipacion.AFECTADO)), 100);
        var q = new ConsultaEstadistica(HECHOS, periodo(), List.of(GRADO, SECCION, HORA), Map.of(SEXO, Set.of("F"), PAPEL, Set.of("AFECTADO")));
        var corte = m.calcular(q, TOE, BASE.plusSeconds(200));
        assertEquals(1, corte.total());
        assertEquals(List.of("1", "A", "10"), corte.celdas().getFirst().coordenadas());
        assertEquals(0, corte.cobertura().unidadesSinDato().get(GRADO));
        assertThrows(UnsupportedOperationException.class, () -> corte.celdas().clear());
    }
    @Test void fechaAproximadaNoInventaDiaSemanaNiHora() {
        var h = Hecho.de(reporte(100, List.of(new Participacion(persona(50), PapelParticipacion.TESTIGO, Optional.empty())), FechaHecho.aproximada(DIA), Optional.empty()));
        var q = new ConsultaEstadistica(HECHOS, periodo(), List.of(DIA_SEMANA, HORA), Map.of());
        var corte = motor(registro(h), 100).calcular(q, TOE, BASE.plusSeconds(200));
        assertEquals(List.of("NO_INFORMADO", "NO_INFORMADO"), corte.celdas().getFirst().coordenadas());
    }
    @Test void datosDiscrepantesSeSeparanDelCeroYPeriodoExcluyeSinFechaSiSePide() {
        var h = hecho(100, 50, PapelParticipacion.AFECTADO).vincularDuplicado(reporte(101,
                List.of(new Participacion(persona(51), PapelParticipacion.TESTIGO, Optional.empty())), FechaHecho.exacta(DIA.minusDays(1)), Optional.empty()), cambio(10));
        var m = motor(registro(h), 100);
        assertEquals(1, contar(m, HECHOS));
        var q = ConsultaEstadistica.de(HECHOS, new ConsultaEstadistica.Periodo(DIA, DIA, false));
        var corte = m.calcular(q, TOE, BASE.plusSeconds(200));
        assertEquals(0, corte.total()); assertEquals(1, corte.cobertura().unidadesSinFechaPrecisada());
    }
    @Test void totalCompartidoNoSumaSubtotalesDeGrupos() {
        var h = hecho(100, 50, PapelParticipacion.AFECTADO).vincularDuplicado(hecho(101, 51, PapelParticipacion.TESTIGO).principal(), cambio(10));
        var q = new ConsultaEstadistica(HECHOS, periodo(), List.of(GRADO), Map.of());
        var corte = motor(registro(h), 100).calcular(q, TOE, BASE.plusSeconds(200));
        assertEquals(1, corte.total()); assertEquals(2, corte.celdas().stream().mapToLong(ResultadoEstadistico.Celda::cantidad).sum());
    }
    @Test void proporcionCuentaEstudiantesUnaVezYUsaNominaAnualDeclarada() {
        var m = motor(registro(hecho(100, 50, PapelParticipacion.AFECTADO), hecho(101, 50, PapelParticipacion.TESTIGO)), 100, 101);
        var p = new MotorEstadisticas.Poblacion(2026, DIA, Optional.empty(), Optional.empty(), Optional.empty(), "Nómina parcial ficticia");
        var resultado = m.proporcionEstudiantes(consulta(ESTUDIANTES), p, TOE, BASE.plusSeconds(200));
        assertEquals(1, resultado.numerador()); assertEquals(2, resultado.denominador());
        assertEquals("50.00", resultado.valor().orElseThrow().toPlainString());
        assertEquals(1, resultado.sinGrupo()); assertEquals(1, resultado.sinSexo());
        assertEquals("100.00", m.frecuenciaPorCien(consulta(HECHOS), p, TOE, BASE.plusSeconds(200)).valor().orElseThrow().toPlainString());
    }
    @Test void denominadorInexistenteNoProduceCeroYFiltroAjenoSeRechaza() {
        var m = motor(registro(hecho(100, 50, PapelParticipacion.AFECTADO)), 100);
        var p = new MotorEstadisticas.Poblacion(2026, DIA, Optional.of("9"), Optional.empty(), Optional.empty(), "Parcial");
        var resultado = m.proporcionEstudiantes(consulta(ESTUDIANTES), p, TOE, BASE.plusSeconds(200));
        assertTrue(resultado.valor().isEmpty()); assertTrue(resultado.motivoNoCalculable().isPresent());
        var q = new ConsultaEstadistica(ESTUDIANTES, periodo(), List.of(), Map.of(GRADO, Set.of("1")));
        assertThrows(IllegalArgumentException.class, () -> m.proporcionEstudiantes(q, p, TOE, BASE.plusSeconds(200)));
    }
    @Test void rolOrdinarioNoConsultaEstadisticasAunqueUniversoEsteVacio() {
        var m = motor(registro(), new long[0]);
        assertThrows(SecurityException.class, () -> m.calcular(consulta(HECHOS), DOCENTE, BASE.plusSeconds(200)));
    }
    @Test void cierreTecnicoNoSePresentaComoAtencionResuelta() {
        var r = registro(hecho(100, 50, PapelParticipacion.AFECTADO));
        r = r.abrirCaso(id(200), Set.of(id(100)), DIA, TOE, List.of(), Optional.empty(), directorio(), cambio(20));
        assertEquals(1, contar(motor(r, 100), CASOS_PENDIENTES));
        r = r.retirarVinculo(id(200), id(100), directorio(), cambio(30));
        var m = motor(r, 100);
        assertEquals(1, contar(m, CASOS_CERRADOS)); assertEquals(0, contar(m, CASOS_RESUELTOS));
        var q = new ConsultaEstadistica(CASOS_CERRADOS, periodo(), List.of(TIPO_CIERRE), Map.of());
        assertEquals("APERTURA_ERRONEA", m.calcular(q, TOE, BASE.plusSeconds(200)).celdas().getFirst().coordenadas().getFirst());
    }
    @Test void accionesInmediatasSinCasoCuentanYNoSeDuplicanTrasApertura() {
        var r = registro(hecho(100, 50, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA));
        var c = new RegistroCambio(DOCENTE, BASE.plusSeconds(10), Optional.empty());
        var compromiso = Compromiso.acordar(id(400), "Acuerdo ficticio", persona(50), Optional.of(DIA.plusDays(1)), true, c);
        var a = Actuacion.realizada(id(300), "Conversación ficticia", DIA, DOCENTE, Set.of(id(100)), Set.of(ESTUDIANTE), List.of(compromiso), c);
        r = r.registrarInmediata(id(100), a, directorio(), c);
        assertEquals(1, contar(motor(r, 100), ACTUACIONES_REALIZADAS));
        r = r.abrirCaso(id(200), Set.of(id(100)), DIA, TOE, List.of(), Optional.empty(), directorio(), cambio(20));
        assertEquals(1, contar(motor(r, 100), ACTUACIONES_REALIZADAS));
        assertEquals(1, contar(motor(r, 100), COMPROMISOS_PENDIENTES));
    }
    @Test void previstasYSeguimientosMantienenFechasYEstadosPropios() {
        var compromiso = Compromiso.acordar(id(400), "Acuerdo ficticio", persona(50), Optional.of(DIA), true, cambio(10));
        compromiso = compromiso.registrarSeguimiento(DIA, "Comprobación explícita", Optional.of(false), false, cambio(20));
        var realizada = Actuacion.realizada(id(300), "Conversación ficticia", DIA, TOE, Set.of(), Set.of(), List.of(compromiso), cambio(30));
        var prevista = Actuacion.prevista(id(301), "Revisión futura", DIA.plusDays(5), TOE, Set.of(), Set.of(), cambio(30));
        var r = registro(hecho(100, 50, PapelParticipacion.AFECTADO)).abrirCaso(id(200), Set.of(id(100)), DIA, TOE,
                List.of(realizada, prevista), Optional.empty(), directorio(), cambio(40));
        var m = motor(r, 100);
        assertEquals(1, contar(m, ACTUACIONES_REALIZADAS)); assertEquals(1, contar(m, ACTUACIONES_PREVISTAS));
        assertEquals(0, contar(m, COMPROMISOS_PENDIENTES)); assertEquals(1, contar(m, COMPROMISOS_COMPROBADOS));
        assertEquals(1, contar(m, SEGUIMIENTOS_COMPROBADOS));
    }
    @Test void comparacionMantieneDefinicionesYCambiaSoloPeriodo() {
        var m = motor(registro(hecho(100, 50, PapelParticipacion.AFECTADO)), 100);
        var comparacion = m.comparar(consulta(HECHOS), new ConsultaEstadistica.Periodo(DIA.minusMonths(3), DIA.minusMonths(2), false), TOE, BASE.plusSeconds(200));
        assertEquals(1, comparacion.diferencia()); assertEquals(0, comparacion.anterior().total());
    }
    @Test void filtrosNoPertinentesNoSonIgnorados() {
        assertThrows(IllegalArgumentException.class, () -> new ConsultaEstadistica(HECHOS, periodo(), List.of(MOTIVO_ALERTA), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new ConsultaEstadistica(HECHOS, periodo(), List.of(SEXO, SEXO), Map.of()));
    }
}
