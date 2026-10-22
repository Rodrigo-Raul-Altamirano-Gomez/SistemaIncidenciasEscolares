package pe.edu.incidencias.domain.estudiante;

import java.util.Objects;
import java.util.Optional;

/** Contacto aportado por la fuente; no acredita filiación ni representación legal. */
public record ContactoFamiliar(Optional<String> nombre, Optional<String> relacionInformada,
                               Optional<String> telefono, Optional<DocumentoIdentidad> documento,
                               Optional<String> necesidadDocumento) {
    public ContactoFamiliar {
        nombre = limpiar(nombre);
        relacionInformada = limpiar(relacionInformada);
        telefono = limpiar(telefono);
        Objects.requireNonNull(documento, "documento");
        necesidadDocumento = limpiar(necesidadDocumento);
        if (nombre.isEmpty() && telefono.isEmpty()) throw new IllegalArgumentException("Contacto sin nombre ni teléfono");
        if (documento.isPresent() != necesidadDocumento.isPresent())
            throw new IllegalArgumentException("El documento familiar requiere necesidad documentada, y viceversa");
    }

    static Optional<String> limpiar(Optional<String> valor) {
        return Objects.requireNonNull(valor, "valor").map(AlcanceImportacion::texto);
    }

    /** Las celdas omitidas tampoco borran componentes de un contacto ya disponible. */
    ContactoFamiliar incorporar(ContactoFamiliar entrada) {
        return new ContactoFamiliar(entrada.nombre().or(() -> nombre),
                entrada.relacionInformada().or(() -> relacionInformada), entrada.telefono().or(() -> telefono),
                entrada.documento().or(() -> documento), entrada.necesidadDocumento().or(() -> necesidadDocumento));
    }
}
