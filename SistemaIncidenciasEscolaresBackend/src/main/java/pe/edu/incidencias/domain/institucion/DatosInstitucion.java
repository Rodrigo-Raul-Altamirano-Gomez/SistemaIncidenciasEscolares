package pe.edu.incidencias.domain.institucion;

import java.util.Objects;
import java.util.Optional;

/** Datos institucionales de una instalación, sin colegio fijado en código. */
public record DatosInstitucion(String nombre, Optional<String> codigo, Optional<String> domicilio,
                               Optional<String> contacto) {
    public DatosInstitucion {
        nombre = obligatorio(nombre);
        codigo = opcional(codigo);
        domicilio = opcional(domicilio);
        contacto = opcional(contacto);
    }
    private static String obligatorio(String texto) {
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException("El nombre institucional es obligatorio");
        return texto.strip();
    }
    private static Optional<String> opcional(Optional<String> valor) {
        Objects.requireNonNull(valor);
        return valor.map(texto -> {
            if (texto.isBlank()) throw new IllegalArgumentException("Un dato informado no puede quedar vacío");
            return texto.strip();
        });
    }
}
