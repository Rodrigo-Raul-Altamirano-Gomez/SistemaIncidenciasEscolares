package pe.edu.incidencias.domain.documento;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.documento.DatosPruebaDocumentos.*;

class ActasTest {
    @Test void aceptaLosDosFormatosYElLimiteExactoDeDiezMegabytes() {
        assertEquals(10_000_000, new ArchivoAdjunto(id(1), "ACTA.PDF", "application/pdf", 10_000_000).bytes());
        assertEquals("acta.docx", new ArchivoAdjunto(id(2), "acta.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 1).nombre());
        assertThrows(IllegalArgumentException.class, () -> new ArchivoAdjunto(id(1), "acta.pdf", "application/pdf", 10_000_001));
        assertThrows(IllegalArgumentException.class, () -> new ArchivoAdjunto(id(1), "acta.pdf", "application/pdf", 0));
        assertThrows(IllegalArgumentException.class, () -> new ArchivoAdjunto(id(1), "acta.doc", "application/msword", 1));
        assertThrows(IllegalArgumentException.class, () -> new ArchivoAdjunto(id(1), "acta.pdf", "image/png", 1));
        assertThrows(IllegalArgumentException.class, () -> new ArchivoAdjunto(id(1), "../acta.pdf", "application/pdf", 1));
    }

    @Test void sustituirConservaReferenciaAnteriorSinPermitirPerderHistoria() {
        var original = acta();
        assertThrows(IllegalArgumentException.class, () -> original.sustituir(HOY, "Corregida", pdf(302), cambio(DIRECTOR, 120)));
        var corregida = original.sustituir(HOY, "Corregida", pdf(302), motivo(DIRECTOR, 120));
        assertEquals(1, original.revisiones().size());
        assertEquals(2, corregida.revisiones().size());
        assertEquals(id(301), corregida.vigente().sustituyeArchivoId().orElseThrow());
        var registro = RegistroActas.vacio().registrar(original, abierto(), directorio(), Optional.empty())
                .registrar(corregida, abierto(), directorio(), Optional.empty());
        assertEquals(id(302), registro.archivoParaDescarga(id(300), id(302), TOE, reloj(130), abierto(), directorio()).id());
        assertThrows(SecurityException.class, () -> registro.archivoParaDescarga(id(300), id(301), TOE, reloj(130), abierto(), directorio()));
        assertThrows(IllegalArgumentException.class, () -> registro.registrar(original, abierto(), directorio(), Optional.empty()));
        assertThrows(UnsupportedOperationException.class, () -> corregida.revisiones().clear());
    }

    @Test void reservaNoSeConcedeAlAutorOrdinarioNiAipNiAdministrador() {
        var registro = RegistroActas.vacio().registrar(acta(), abierto(), directorio(), Optional.empty());
        for (var usuario : List.of(DOCENTE, AIP, ADMIN)) {
            assertThrows(SecurityException.class, () -> registro.consultar(CASO, usuario, reloj(120), abierto(), directorio()));
            assertThrows(SecurityException.class, () -> registro.archivoParaDescarga(id(300), id(301), usuario, reloj(120), abierto(), directorio()));
        }
        assertEquals(1, registro.consultar(CASO, COMITE, reloj(120), abierto(), directorio()).size());
    }

    @Test void actaFormalExigeDirectorOSustitucionJustificadaYNoSoloToe() {
        var toe = ActaAdjunta.incorporar(id(300), CASO, HOY, "Acta", pdf(301), cambio(TOE, 110));
        assertThrows(SecurityException.class, () -> RegistroActas.vacio().registrar(toe, abierto(), directorio(), Optional.empty()));
        var sustituto = ActaAdjunta.incorporar(id(300), CASO, HOY, "Acta", pdf(301), cambio(COMITE, 130));
        assertThrows(SecurityException.class, () -> RegistroActas.vacio().registrar(sustituto, abierto(), directorio(), Optional.of("Sustitución")));
        var sinDirector = directorio().desactivarUsuario(DIRECTOR, motivo(ADMIN, 120));
        assertThrows(SecurityException.class, () -> RegistroActas.vacio().registrar(sustituto, abierto(), sinDirector, Optional.empty()));
        assertNotNull(RegistroActas.vacio().registrar(sustituto, abierto(), sinDirector, Optional.of("Director sin cuenta vigente")));
    }

    @Test void enlaceAnteriorSeRevalidaTrasExclusionBajaOFinDeDesignacion() {
        var actual = abierto();
        var registro = RegistroActas.vacio().registrar(acta(), actual, directorio(), Optional.empty());
        assertNotNull(registro.archivoParaDescarga(id(300), id(301), DIRECTOR, reloj(120), actual, directorio()));
        var excluido = actual.excluirUsuario(CASO, id(900), DIRECTOR, directorio(), motivo(TOE, 130));
        assertThrows(SecurityException.class, () -> registro.archivoParaDescarga(id(300), id(301), DIRECTOR, reloj(140), excluido, directorio()));
        var baja = directorio().desactivarUsuario(DIRECTOR, motivo(ADMIN, 130));
        assertThrows(SecurityException.class, () -> registro.archivoParaDescarga(id(300), id(301), DIRECTOR, reloj(140), actual, baja));
        var fin = directorio().finalizarDesignacion(DIRECTOR, id(1000), reloj(130), motivo(ADMIN, 130));
        assertThrows(SecurityException.class, () -> registro.archivoParaDescarga(id(300), id(301), DIRECTOR, reloj(140), actual, fin));
    }

    @Test void mismoArchivoNoSeIncorporaATresIdentidadesNiSeAceptaActaFutura() {
        var registro = RegistroActas.vacio().registrar(acta(), abierto(), directorio(), Optional.empty());
        var copia = ActaAdjunta.incorporar(id(303), CASO, HOY, "Otra", pdf(301), cambio(DIRECTOR, 120));
        assertThrows(IllegalArgumentException.class, () -> registro.registrar(copia, abierto(), directorio(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> ActaAdjunta.incorporar(id(300), CASO, HOY.plusDays(1), "Otra", pdf(302), cambio(DIRECTOR, 120)));
    }

    @Test void sustitucionDelDirectorConservaCausaAutorRealYVersionesPreviasDelArchivo() {
        var atenciones = abierto();
        var registro = RegistroActas.vacio().registrar(acta(), atenciones, directorio(), Optional.empty());
        var sinDirector = directorio().desactivarUsuario(DIRECTOR, motivo(ADMIN, 130));
        var anterior = registro.consultar(CASO, COMITE, reloj(135), atenciones, sinDirector).getFirst();
        var nueva = anterior.sustituir(HOY, "Acta rectificada por sustituto", pdf(302), motivo(COMITE, 140));
        registro = registro.registrar(nueva, atenciones, sinDirector, Optional.of("Director sin cuenta vigente"));
        var guardada = registro.consultar(CASO, COMITE, reloj(145), atenciones, sinDirector).getFirst();
        assertEquals(COMITE, guardada.vigente().registro().autorId());
        assertEquals("Director sin cuenta vigente", guardada.vigente().motivoSustitucion().orElseThrow());
        assertEquals(DIRECTOR, guardada.revisiones().getFirst().registro().autorId());
        assertTrue(guardada.revisiones().getFirst().motivoSustitucion().isEmpty());
        assertEquals(id(301), guardada.revisiones().getFirst().archivo().id());
        assertEquals(id(301), guardada.vigente().sustituyeArchivoId().orElseThrow());
        var posterior = guardada.sustituir(HOY, "Segunda rectificación", pdf(303), motivo(COMITE, 150));
        var finalRegistro = registro.registrar(posterior, atenciones, sinDirector, Optional.of("Director sin cuenta vigente"));
        var finalActa = finalRegistro.consultar(CASO, COMITE, reloj(155), atenciones, sinDirector).getFirst();
        assertEquals(guardada.revisiones(), finalActa.revisiones().subList(0, 2));
        assertEquals("Director sin cuenta vigente", finalActa.revisiones().get(1).motivoSustitucion().orElseThrow());
    }
}
