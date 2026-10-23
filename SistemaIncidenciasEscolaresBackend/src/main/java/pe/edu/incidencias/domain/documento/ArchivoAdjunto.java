package pe.edu.incidencias.domain.documento;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Referencia opaca al archivo. La lectura y validación real del contenido corresponden al adaptador. */
public record ArchivoAdjunto(UUID id, String nombre, String tipoMime, long bytes) {
    public static final long MAXIMO_BYTES = 10_000_000L;
    public ArchivoAdjunto {
        Objects.requireNonNull(id);
        if (nombre == null || nombre.isBlank() || nombre.codePoints().anyMatch(c -> Character.isISOControl(c)
                || "/\\:<>\"|?*".indexOf(c) >= 0))
            throw new IllegalArgumentException("Se necesita un nombre de archivo, no una ruta");
        Objects.requireNonNull(tipoMime);
        var inferior = nombre.toLowerCase(Locale.ROOT);
        boolean pdf = inferior.endsWith(".pdf") && tipoMime.equals("application/pdf");
        boolean docx = inferior.endsWith(".docx") && tipoMime.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        if (!pdf && !docx) throw new IllegalArgumentException("El acta admite PDF o DOCX con tipo concordante");
        if (bytes <= 0 || bytes > MAXIMO_BYTES) throw new IllegalArgumentException("El archivo debe tener hasta 10 MB (10 000 000 bytes)");
    }
}
