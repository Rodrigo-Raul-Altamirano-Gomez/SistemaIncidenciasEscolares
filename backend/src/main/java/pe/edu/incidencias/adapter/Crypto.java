package pe.edu.incidencias.adapter;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Crypto {
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();
    public Crypto(@Value("${app.data-key}") String encoded) {
        key = Base64.getDecoder().decode(encoded);
        if (key.length != 32) throw new IllegalArgumentException("DATA_KEY debe contener 32 bytes en Base64.");
    }
    public String token() { byte[] b = new byte[32]; random.nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    public String seal(String text, String scope) {
        try {
            byte[] iv = new byte[12]; random.nextBytes(iv);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, iv, scope);
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[12 + encrypted.length]; System.arraycopy(iv,0,result,0,12); System.arraycopy(encrypted,0,result,12,encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (GeneralSecurityException e) { throw new IllegalStateException("No se pudo proteger la información."); }
    }
    public String open(String encoded, String scope) {
        try { byte[] b = Base64.getDecoder().decode(encoded); return new String(cipher(Cipher.DECRYPT_MODE,Arrays.copyOf(b,12),scope).doFinal(Arrays.copyOfRange(b,12,b.length)),StandardCharsets.UTF_8); }
        catch (GeneralSecurityException | IllegalArgumentException e) { throw new IllegalStateException("No se pudo verificar la información protegida."); }
    }
    private Cipher cipher(int mode, byte[] iv, String scope) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(mode,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv)); c.updateAAD(scope.getBytes(StandardCharsets.UTF_8)); return c;
    }
    public String fingerprint(String value) {
        try { Mac m = Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(key,"HmacSHA256")); return HexFormat.of().formatHex(m.doFinal(value.getBytes(StandardCharsets.UTF_8))); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("No se pudo verificar la operación."); }
    }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
}
