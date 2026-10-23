package pe.edu.incidencias;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pe.edu.incidencias.adapter.Crypto;

class CryptoTest {
    @Test void cifradoAutenticadoNoRepiteNonceNiAdmiteOtroAmbito() {
        Crypto crypto=new Crypto(Base64.getEncoder().encodeToString(new byte[32]));
        String a=crypto.seal("Dato ficticio","note:uno"),b=crypto.seal("Dato ficticio","note:uno");
        assertNotEquals(a,b); assertEquals("Dato ficticio",crypto.open(a,"note:uno"));
        assertThrows(IllegalStateException.class,()->crypto.open(a,"note:otro"));
        byte[] tampered=Base64.getDecoder().decode(a); tampered[tampered.length-1]^=1;
        assertThrows(IllegalStateException.class,()->crypto.open(Base64.getEncoder().encodeToString(tampered),"note:uno"));
    }
}
