package pe.edu.incidencias.domain.consulta;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.alerta.*;
import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.incidencia.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.consulta.DatosConsulta.*;
import static pe.edu.incidencias.domain.consulta.ConsultaEstadistica.Medida.*;

class ReconciliacionMedidasTest {
    @ParameterizedTest @EnumSource(ConsultaEstadistica.Medida.class)
    void cadaMedidaSeReconciliaConConjuntoConocido(ConsultaEstadistica.Medida medida) {
        var h1 = conAviso(hecho(100, 50, PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA));
        var h2 = conAviso(hecho(101, 50, PapelParticipacion.PRESUNTA_RESPONSABILIDAD));
        var corroborado = hecho(102, 50, PapelParticipacion.PRESUNTA_RESPONSABILIDAD)
                .aclararParticipacion(ESTUDIANTE, PapelParticipacion.ATRIBUCION_CORROBORADA, "Comprobación institucional", cambio(5));
        var r = registro(h1, h2, corroborado,
                hecho(103, 50, PapelParticipacion.AFECTADO), hecho(104, 50, PapelParticipacion.TESTIGO), hecho(105, 50, PapelParticipacion.OTRO));
        var pendiente = Compromiso.acordar(id(400), "Acuerdo pendiente", persona(50), Optional.of(DIA), true, cambio(10));
        var comprobado = Compromiso.acordar(id(401), "Acuerdo comprobado", persona(50), Optional.of(DIA), true, cambio(10))
                .registrarSeguimiento(DIA, "Cumplimiento observado", Optional.of(true), false, cambio(20));
        var realizada = Actuacion.realizada(id(300), "Atención ficticia", DIA, TOE, Set.of(), Set.of(), List.of(pendiente, comprobado), cambio(30));
        var prevista = Actuacion.prevista(id(301), "Revisión prevista", DIA.plusDays(3), TOE, Set.of(), Set.of(), cambio(30));
        r = r.abrirCaso(id(200), Set.of(id(100), id(101), id(102), id(103), id(104)), DIA, TOE,
                List.of(realizada, prevista), Optional.empty(), directorio(), cambio(40));
        r = r.abrirCaso(id(201), Set.of(id(105)), DIA, TOE, List.of(),
                Optional.of(new CierreLocal(DIA, "Atención concluida", CierreLocal.TipoCierre.CONCLUSION, cambio(50), Optional.empty())), directorio(), cambio(50));
        var derivacion = Derivacion.efectuada(id(500), Set.of(id(100)), Derivacion.Supuesto.OTRO,
                new Derivacion.Destino(Derivacion.TipoDestino.OTRA_ENTIDAD, "Entidad ficticia"), "Envío realizado", Optional.empty(), DIA, cambio(60));
        r = r.registrarDerivacion(id(200), derivacion, directorio(), cambio(60));
        var a = RegistroAlertas.vacio().evaluarHecho(r.hechoVigente(id(100)), r.contextoDeHechos(Set.of(id(100))), politica(r), cambio(70));
        a = a.evaluarHecho(r.hechoVigente(id(101)), r.contextoDeHechos(Set.of(id(101))), politica(r), cambio(80));
        var primera = a.alertas().getFirst();
        a = a.disponer(primera.id(), new DisposicionAlerta(DisposicionAlerta.Tipo.INTERVENCION_SIN_CASO,
                "Se atendió la señal", Optional.empty(), cambio(90)), r.contextoDeHechos(primera.hechosHistoricos()), politica(r));
        var m = new MotorEstadisticas(r, a, matriculas(), Set.of(id(100), id(101), id(102), id(103), id(104), id(105)), politica(r));
        long esperado = switch (medida) {
            case HECHOS, COMUNICACIONES, PARTICIPACIONES -> 6;
            case ALERTAS_EMITIDAS, COMPROMISOS -> 2;
            default -> 1;
        };
        assertEquals(esperado, contar(m, medida), medida.name());
    }
    @Test void cierreAdministrativoYActuacionRealNoDesaparecenCuandoSeAnulaUltimaFuente() {
        var r = registro(hecho(100, 50, PapelParticipacion.AFECTADO));
        var actuacion = Actuacion.realizada(id(300), "Atención real histórica", DIA, TOE, Set.of(id(100)), Set.of(), List.of(), cambio(20));
        r = r.abrirCaso(id(200), Set.of(id(100)), DIA, TOE, List.of(actuacion), Optional.empty(), directorio(), cambio(20));
        r = r.retirarVinculo(id(200), id(100), directorio(), cambio(30));
        var h = r.hechoVigente(id(100));
        h = h.actualizarReporte(h.principal().anularPorError(cambio(40)));
        r = r.actualizarHecho(h, directorio(), cambio(40));
        var m = motor(r, 100);
        assertEquals(0, contar(m, HECHOS)); assertEquals(0, contar(m, ESTUDIANTES));
        assertEquals(1, contar(m, CASOS_CERRADOS)); assertEquals(0, contar(m, CASOS_RESUELTOS));
        assertEquals(1, contar(m, ACTUACIONES_REALIZADAS));
    }
    private static Hecho conAviso(Hecho hecho) {
        var d = hecho.principal().datos();
        return Hecho.de(ReporteIncidencia.registrar(hecho.id(), new DatosReporte(d.fechaHecho(), d.anioEscolar(), d.clasificacion(),
                d.descripcion(), d.participaciones(), true, d.hora(), d.lugar(), d.comunicante(), d.canal()), DOCENTE, BASE.plusSeconds(1)));
    }
}
