package pe.edu.incidencias.domain.atencion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CierreLocalTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);
    private static final RegistroCambio REGISTRO = new RegistroCambio(new UUID(0, 1),
            Instant.parse("2026-10-21T01:00:00Z"), Optional.empty());
    private static final UUID DERIVACION = new UUID(0, 2);

    @Test
    void conclusionLocalAdmiteResultadoSuficienteSinMotivoDuplicadoNiDerivacion() {
        var cierre = new CierreLocal(HOY, "Se concluyó la conversación y atención local", CierreLocal.TipoCierre.CONCLUSION,
                REGISTRO, Optional.empty());
        assertEquals(HOY, cierre.fecha());
        assertTrue(cierre.registro().motivo().isEmpty());
        assertTrue(cierre.derivacionOrigenId().isEmpty());
        assertEquals(CierreLocal.TipoCierre.CONCLUSION, cierre.tipo());
    }

    @Test
    void cierrePorDerivacionConservaReferenciaDeOrigenSinDeclararEstadoOficial() {
        var cierre = new CierreLocal(HOY.minusDays(2), "Derivación familiar efectuada a Fiscalía",
                CierreLocal.TipoCierre.DERIVACION_FAMILIAR_FISCALIA, REGISTRO, Optional.of(DERIVACION));
        assertEquals(Optional.of(DERIVACION), cierre.derivacionOrigenId());
        assertEquals(HOY.minusDays(2), cierre.fecha());
        assertEquals(REGISTRO, cierre.registro());
    }

    @Test
    void cierreNoPuedeSerFuturoSegunLaFechaLocalDePeru() {
        assertThrows(IllegalArgumentException.class, () -> new CierreLocal(HOY.plusDays(1), "Conclusión ficticia",
                CierreLocal.TipoCierre.CONCLUSION, REGISTRO, Optional.empty()));
    }

    @Test
    void resultadoNoPuedeEstarVacio() {
        assertThrows(IllegalArgumentException.class, () -> new CierreLocal(HOY, " ",
                CierreLocal.TipoCierre.CONCLUSION, REGISTRO, Optional.empty()));
    }

    @Test
    void tipoDerivacionExigeSuOrigen() {
        assertThrows(IllegalArgumentException.class, () -> new CierreLocal(HOY, "Derivación ficticia",
                CierreLocal.TipoCierre.DERIVACION_FAMILIAR_FISCALIA, REGISTRO, Optional.empty()));
    }

    @ParameterizedTest
    @EnumSource(value = CierreLocal.TipoCierre.class, names = {"CONCLUSION", "APERTURA_ERRONEA", "DUPLICIDAD"})
    void losOtrosTiposNoSePresentanComoCierrePorDerivacion(CierreLocal.TipoCierre tipo) {
        assertThrows(IllegalArgumentException.class, () -> new CierreLocal(HOY, "Resultado ficticio",
                tipo, REGISTRO, Optional.of(DERIVACION)));
    }

    @Test
    void registrarTardiamenteUnaConclusionNoExigeJustificarLaDemora() {
        var cierre = new CierreLocal(HOY.minusYears(1), "Atención realizada fuera de la aplicación",
                CierreLocal.TipoCierre.CONCLUSION, REGISTRO, Optional.empty());
        assertTrue(cierre.registro().motivo().isEmpty());
        assertEquals(HOY.minusYears(1), cierre.fecha());
    }
}
