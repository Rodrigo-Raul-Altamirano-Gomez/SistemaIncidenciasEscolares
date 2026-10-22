package pe.edu.incidencias.domain.documento;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.atencion.CasoAtencion;
import pe.edu.incidencias.domain.institucion.DatosInstitucion;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.documento.DatosPruebaDocumentos.*;

class ReferenciasOficialesTest {
    private ReferenciaOficial referencia(Optional<String> codigo, int segundos) {
        return new ReferenciaOficial(id(600), abierto().contexto(CASO).ambito(), InstrumentoOficial.SISEVE,
                "La familia comunica un registro previo", codigo, Optional.empty(), Optional.of("Protocolo informado v1"),
                Optional.empty(), cambio(TOE, segundos));
    }

    @Test void origenSisEveSinCodigoNoRegistraOtroHechoNiCierraAtencion() {
        var actual = abierto();
        var referencias = ReferenciasOficiales.vacias().registrar(referencia(Optional.empty(), 110), actual.contexto(CASO), directorio(), actual.restricciones());
        assertEquals(1, referencias.consultar(actual.contexto(CASO), TOE, reloj(120), directorio(), actual.restricciones()).size());
        assertEquals(1, actual.casos().size());
        assertEquals(1, actual.hechosDelCaso(CASO, TOE, directorio(), reloj(120)).size());
        assertEquals(CasoAtencion.Estado.PENDIENTE, actual.caso(CASO).orElseThrow().estado());
        assertTrue(referencias.revisiones().get(id(600)).getFirst().codigoOAsiento().isEmpty());
    }

    @Test void completarDespuesYRectificarConMotivoConservaVersionDeProtocolo() {
        var actual = abierto();
        var contexto = actual.contexto(CASO);
        var r = ReferenciasOficiales.vacias().registrar(referencia(Optional.empty(), 110), contexto, directorio(), actual.restricciones())
                .registrar(referencia(Optional.of("S-1"), 120), contexto, directorio(), actual.restricciones());
        var previo = r;
        assertThrows(IllegalArgumentException.class, () -> previo.registrar(referencia(Optional.of("S-2"), 130), contexto, directorio(), actual.restricciones()));
        var correccion = new ReferenciaOficial(id(600), contexto.ambito(), InstrumentoOficial.SISEVE,
                "La familia comunica un registro previo", Optional.of("S-2"), Optional.of(HOY), Optional.of("Protocolo informado v2"), Optional.empty(), motivo(TOE, 130));
        r = r.registrar(correccion, contexto, directorio(), actual.restricciones());
        assertEquals(3, r.revisiones().get(id(600)).size());
        assertEquals("Protocolo informado v1", r.revisiones().get(id(600)).getFirst().protocoloYVersion().orElseThrow());
        assertEquals("S-2", r.consultar(contexto, TOE, reloj(140), directorio(), actual.restricciones()).getFirst().codigoOAsiento().orElseThrow());
        var finalR = r;
        assertThrows(UnsupportedOperationException.class, () -> finalR.revisiones().get(id(600)).clear());
    }

    @Test void noSeFiltraReferenciaDeAmbitoMasAmplioPorInterseccionParcial() {
        var actual = abierto();
        var amplio = new ContextoAcceso(actual.contexto(CASO).ambito().unir(new AmbitoReserva(Set.of(), Set.of(), Set.of(id(999)))),
                actual.contexto(CASO).hechos(), Optional.empty(), Optional.empty());
        var referencia = new ReferenciaOficial(id(600), amplio.ambito(), InstrumentoOficial.OTRO, "Origen conocido", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), cambio(TOE, 110));
        var r = ReferenciasOficiales.vacias().registrar(referencia, amplio, directorio(), actual.restricciones());
        assertThrows(IllegalArgumentException.class, () -> r.consultar(actual.contexto(CASO), TOE, reloj(120), directorio(), actual.restricciones()));
        assertThrows(SecurityException.class, () -> r.consultar(amplio, AIP, reloj(120), directorio(), actual.restricciones()));
    }

    @Test void datosInstitucionalesSonValoresSinCodificarUnaEscuela() {
        var datos = new DatosInstitucion(" Escuela elegida ", Optional.of(" 001 "), Optional.empty(), Optional.empty());
        assertEquals("Escuela elegida", datos.nombre());
        assertEquals("001", datos.codigo().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> new DatosInstitucion(" ", Optional.empty(), Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new DatosInstitucion("Escuela", Optional.of(" "), Optional.empty(), Optional.empty()));
    }
}
