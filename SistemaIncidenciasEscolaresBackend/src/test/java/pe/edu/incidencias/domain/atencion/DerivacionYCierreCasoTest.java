package pe.edu.incidencias.domain.atencion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pe.edu.incidencias.domain.acceso.RestriccionesAmbito;
import pe.edu.incidencias.domain.incidencia.PapelParticipacion;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.atencion.DatosPruebaAtencion.*;

class DerivacionYCierreCasoTest {
    @Test
    void prepararDerivacionFamiliarNoCierraYEjecutarlaCierraConMismaIdentidad() {
        var preparada = Derivacion.preparada(id(600), Set.of(id(100)), Derivacion.Supuesto.VIOLENCIA_FAMILIAR,
                fiscalia(), "Preparación de comunicación", Optional.empty(), cambio(110));
        var registro = abrir(base(), 200, Set.of(id(100)), 100)
                .registrarDerivacion(id(200), preparada, directorio(), cambio(110));
        assertEquals(CasoAtencion.Estado.PENDIENTE, registro.caso(id(200)).orElseThrow().estado());
        var ejecutada = preparada.efectuar(HOY, "Comunicación entregada a Fiscalía; comprende el objeto completo", Optional.empty(), cambio(120));
        var cerrado = registro.registrarDerivacion(id(200), ejecutada, directorio(), cambio(120));
        var caso = cerrado.caso(id(200)).orElseThrow();
        assertEquals(CasoAtencion.Estado.CERRADA, caso.estado());
        assertEquals(CierreLocal.TipoCierre.DERIVACION_FAMILIAR_FISCALIA, caso.cierre().orElseThrow().tipo());
        assertEquals(Optional.of(ejecutada.id()), caso.cierre().orElseThrow().derivacionOrigenId());
        assertEquals(ejecutada.relato(), caso.cierre().orElseThrow().resultado());
        assertEquals(1, caso.derivaciones().size());
        assertEquals(2, caso.derivaciones().getFirst().revisiones().size());
        assertEquals(TOE, caso.cierre().orElseThrow().registro().autorId());
    }

    @Test
    void ejecucionDirectaNoExigePreparacionNiArchivoNiReferenciaOficial() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        var derivacion = efectuada(600, Set.of(id(100)), Derivacion.Supuesto.VIOLENCIA_FAMILIAR, fiscalia(), 110);
        var cerrado = registro.registrarDerivacion(id(200), derivacion, directorio(), cambio(110));
        assertEquals(1, cerrado.caso(id(200)).orElseThrow().derivaciones().getFirst().revisiones().size());
        assertTrue(derivacion.referencia().isEmpty());
        assertEquals(CasoAtencion.Estado.CERRADA, cerrado.caso(id(200)).orElseThrow().estado());
    }

    @ParameterizedTest
    @EnumSource(Derivacion.Supuesto.class)
    void otroDestinatarioNoCierraAutomaticamente(Derivacion.Supuesto supuesto) {
        var derivacion = efectuada(600, Set.of(id(100)), supuesto,
                new Derivacion.Destino(Derivacion.TipoDestino.OTRA_ENTIDAD, "Policía ficticia"), 110);
        var registro = abrir(base(), 200, Set.of(id(100)), 100).registrarDerivacion(id(200), derivacion, directorio(), cambio(110));
        assertEquals(CasoAtencion.Estado.PENDIENTE, registro.caso(id(200)).orElseThrow().estado());
    }

    @Test
    void fiscaliaConOtroSupuestoNoCierraAutomaticamente() {
        var derivacion = efectuada(600, Set.of(id(100)), Derivacion.Supuesto.OTRO, fiscalia(), 110);
        assertEquals(CasoAtencion.Estado.PENDIENTE, abrir(base(), 200, Set.of(id(100)), 100)
                .registrarDerivacion(id(200), derivacion, directorio(), cambio(110)).caso(id(200)).orElseThrow().estado());
    }

    @Test
    void casoMixtoConCoberturaParcialMantieneAtencionPendiente() {
        var derivacion = efectuada(600, Set.of(id(100)), Derivacion.Supuesto.VIOLENCIA_FAMILIAR, fiscalia(), 110);
        var registro = abrir(base(), 200, Set.of(id(100), id(101)), 100)
                .registrarDerivacion(id(200), derivacion, directorio(), cambio(110));
        assertEquals(CasoAtencion.Estado.PENDIENTE, registro.caso(id(200)).orElseThrow().estado());
        assertEquals(Set.of(id(100), id(101)), registro.caso(id(200)).orElseThrow().hechosActuales());
    }

    @Test
    void coberturaCompletaFamiliarCierraSinExigirCorroboracionDePapeles() {
        var registro = abrir(base(), 200, Set.of(id(100), id(101)), 100)
                .registrarDerivacion(id(200), efectuada(600, Set.of(id(100), id(101)),
                        Derivacion.Supuesto.VIOLENCIA_FAMILIAR, fiscalia(), 110), directorio(), cambio(110));
        assertEquals(CasoAtencion.Estado.CERRADA, registro.caso(id(200)).orElseThrow().estado());
        assertEquals(PapelParticipacion.PRESUNTA_RESPONSABILIDAD,
                registro.hechosDelCaso(id(200), TOE, directorio(), reloj(120)).getFirst().principal().datos().participaciones().getFirst().papel());
    }

    @Test
    void continuidadDeProteccionYCompromisosSobrevivenAlCierrePorDerivacion() {
        var acuerdo = Compromiso.acordar(id(400), "Acompañamiento acordado", estudiante(), Optional.empty(), true, cambio(100));
        var actuacion = realizada(300, List.of(acuerdo), 100);
        var registro = base().abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(actuacion), Optional.empty(), directorio(), cambio(100))
                .registrarDerivacion(id(200), efectuada(600, Set.of(id(100)), Derivacion.Supuesto.VIOLENCIA_FAMILIAR, fiscalia(), 110), directorio(), cambio(110));
        var cierre = registro.caso(id(200)).orElseThrow().cierre();
        assertEquals(1, registro.conTrabajoPendiente(TOE, directorio(), reloj(120)).size());
        assertEquals(Compromiso.Estado.ACORDADO, registro.caso(id(200)).orElseThrow().actuaciones().getFirst().compromisos().getFirst().estado());
        var seguido = acuerdo.registrarSeguimiento(HOY, "Protección revisada; termina este pendiente", Optional.empty(), false, cambio(130));
        var nueva = actuacion.actualizarCompromiso(seguido, cambio(130));
        var actualizado = registro.registrarActuacion(id(200), nueva, directorio(), cambio(130));
        assertEquals(cierre, actualizado.caso(id(200)).orElseThrow().cierre());
        assertTrue(actualizado.conTrabajoPendiente(TOE, directorio(), reloj(140)).isEmpty());
        assertEquals(Compromiso.Estado.ACORDADO, seguido.estado());
    }

    @Test
    void derivacionPosteriorYCompletarReferenciaNoReescribenCierreHistorico() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        var d = efectuada(600, Set.of(id(100)), Derivacion.Supuesto.VIOLENCIA_FAMILIAR, fiscalia(), 110);
        var cerrado = registro.registrarDerivacion(id(200), d, directorio(), cambio(110));
        var cierre = cerrado.caso(id(200)).orElseThrow().cierre();
        var actualizado = cerrado.registrarDerivacion(id(200), d.completarReferencia("Cargo ficticio", cambio(120)), directorio(), cambio(120));
        actualizado = actualizado.registrarDerivacion(id(200), efectuada(601, Set.of(id(100)),
                Derivacion.Supuesto.VIOLENCIA_FAMILIAR, fiscalia(), 130), directorio(), cambio(130));
        assertEquals(cierre, actualizado.caso(id(200)).orElseThrow().cierre());
        assertEquals(1, actualizado.caso(id(200)).orElseThrow().cierres().size());
        assertEquals(2, actualizado.caso(id(200)).orElseThrow().derivaciones().size());
    }

    @Test
    void cierreDerivadoNoPuedeInventarseSinOrigenEfectivoYCoberturaCompleta() {
        var abierto = abrir(base(), 200, Set.of(id(100), id(101)), 100);
        var falso = new CierreLocal(HOY, "Se afirma derivación", CierreLocal.TipoCierre.DERIVACION_FAMILIAR_FISCALIA,
                cambio(110), Optional.of(id(600)));
        assertThrows(IllegalArgumentException.class, () -> abierto.cerrarCaso(id(200), falso, directorio(), cambio(110)));
        var parcial = abierto.registrarDerivacion(id(200), efectuada(600, Set.of(id(100)),
                Derivacion.Supuesto.VIOLENCIA_FAMILIAR, fiscalia(), 110), directorio(), cambio(110));
        assertThrows(IllegalArgumentException.class, () -> parcial.cerrarCaso(id(200), falso, directorio(), cambio(120)));
    }

    @Test
    void noSeAdmitenDerivacionesAHechosAjenosOVersionesQuePierdanHistoria() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        assertThrows(IllegalArgumentException.class, () -> registro.registrarDerivacion(id(200),
                efectuada(600, Set.of(id(101)), Derivacion.Supuesto.OTRO, fiscalia(), 110), directorio(), cambio(110)));
        var preparada = Derivacion.preparada(id(600), Set.of(id(100)), Derivacion.Supuesto.OTRO, fiscalia(), "Preparación", Optional.empty(), cambio(110));
        var ejecutada = preparada.efectuar(HOY, "Comunicación efectuada", Optional.empty(), cambio(120));
        var actualizado = registro.registrarDerivacion(id(200), ejecutada, directorio(), cambio(120));
        assertThrows(IllegalArgumentException.class, () -> actualizado.registrarDerivacion(id(200), preparada, directorio(), cambio(130)));
        var otroCaso = abrir(actualizado, 201, Set.of(id(101)), 130);
        assertThrows(IllegalArgumentException.class, () -> otroCaso.registrarDerivacion(id(201), ejecutada, directorio(), cambio(140)));
    }

    @Test
    void fechasDeCierreYDerivacionNoPuedenPrecederInicioEfectivo() {
        var abierto = abrir(base(), 200, Set.of(id(100)), 100);
        var anterior = new CierreLocal(HOY.minusDays(1), "Conclusión documentada", CierreLocal.TipoCierre.CONCLUSION, cambio(110), Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> abierto.cerrarCaso(id(200), anterior, directorio(), cambio(110)));
        var derivacion = Derivacion.efectuada(id(600), Set.of(id(100)), Derivacion.Supuesto.VIOLENCIA_FAMILIAR,
                fiscalia(), "Comunicación efectuada", Optional.empty(), HOY.minusDays(1), cambio(110));
        assertThrows(IllegalArgumentException.class, () -> abierto.registrarDerivacion(id(200), derivacion, directorio(), cambio(110)));
    }

    @Test
    void conclusionInicialNoPrecedeConversacionRealizadaEnElMismoGuardado() {
        var cierre = new CierreLocal(HOY.minusDays(1), "Resultado ficticio", CierreLocal.TipoCierre.CONCLUSION, cambio(100), Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> base().abrirCaso(id(200), Set.of(id(100)), HOY.minusDays(1), TOE,
                List.of(realizada(300, List.of(), 100)), Optional.of(cierre), directorio(), cambio(100)));
    }

    private static Derivacion.Destino fiscalia() {
        return new Derivacion.Destino(Derivacion.TipoDestino.FISCALIA, "Fiscalía competente ficticia");
    }
    private static Derivacion efectuada(int numero, Set<java.util.UUID> hechos, Derivacion.Supuesto supuesto,
                                       Derivacion.Destino destino, int segundos) {
        return Derivacion.efectuada(id(numero), hechos, supuesto, destino, "Comunicación externa efectuada y documentada",
                Optional.empty(), HOY, cambio(segundos));
    }
}
