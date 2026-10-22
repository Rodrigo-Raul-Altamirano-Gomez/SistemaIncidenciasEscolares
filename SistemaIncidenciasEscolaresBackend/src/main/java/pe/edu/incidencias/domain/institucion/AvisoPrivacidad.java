package pe.edu.incidencias.domain.institucion;

import java.util.List;
import java.util.Objects;

/** Contenido institucional declarado; no verifica una base jurídica ni registra consentimientos. */
public record AvisoPrivacidad(String banco, String responsable, String domicilio, String contacto,
                              List<String> encargados, String finalidades, String destinatariosTransferencias,
                              String obligatoriedadConsecuencias, String conservacion, String mediosDerechos) {
    public AvisoPrivacidad {
        banco = texto(banco); responsable = texto(responsable); domicilio = texto(domicilio);
        contacto = texto(contacto); encargados = List.copyOf(encargados).stream().map(AvisoPrivacidad::texto).toList();
        finalidades = texto(finalidades); destinatariosTransferencias = texto(destinatariosTransferencias);
        obligatoriedadConsecuencias = texto(obligatoriedadConsecuencias);
        conservacion = texto(conservacion); mediosDerechos = texto(mediosDerechos);
    }
    private static String texto(String valor) {
        if (Objects.requireNonNull(valor, "texto").isBlank()) throw new IllegalArgumentException("Aviso incompleto");
        return valor.strip();
    }
}
