package pe.edu.incidencias.domain.estudiante;

import java.util.Objects;

/** Identificador textual; nunca completa cifras perdidas ni usa datos del familiar. */
public record DocumentoIdentidad(String tipo, String numero) {
    public DocumentoIdentidad {
        tipo = textoObligatorio(tipo, "tipo");
        numero = textoObligatorio(numero, "numero");
        if (tipo.equalsIgnoreCase("DNI")) {
            tipo = "DNI";
            if (!numero.matches("[0-9]{8}")) {
                throw new IllegalArgumentException("El DNI debe contener exactamente ocho dígitos.");
            }
        }
    }

    private static String textoObligatorio(String valor, String campo) {
        String texto = Objects.requireNonNull(valor, campo).replaceAll("(?U)^\\s+|\\s+$", "");
        if (texto.isBlank()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío.");
        }
        return texto;
    }
}
