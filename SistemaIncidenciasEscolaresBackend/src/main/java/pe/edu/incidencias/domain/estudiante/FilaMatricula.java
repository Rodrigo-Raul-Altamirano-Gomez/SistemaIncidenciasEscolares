package pe.edu.incidencias.domain.estudiante;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Campos seleccionados y tipados por el adaptador. No representa una fila arbitraria de Excel. */
public record FilaMatricula(int numero, UUID identidadParaAlta, String nombreCompleto,
                            Optional<DocumentoIdentidad> documento, Optional<LocalDate> nacimiento,
                            Optional<String> sexo, Optional<GrupoEscolar> grupo, Optional<String> situacion) {
    public FilaMatricula {
        if (numero < 1) throw new IllegalArgumentException("La fila debe ser positiva");
        Objects.requireNonNull(identidadParaAlta, "identidadParaAlta");
        nombreCompleto = Objects.requireNonNull(nombreCompleto, "nombreCompleto").strip();
        Objects.requireNonNull(documento, "documento");
        Objects.requireNonNull(nacimiento, "nacimiento");
        sexo = textoOpcional(sexo);
        Objects.requireNonNull(grupo, "grupo");
        situacion = textoOpcional(situacion);
    }

    private static Optional<String> textoOpcional(Optional<String> valor) {
        return Objects.requireNonNull(valor, "valor").map(texto -> {
            if (texto.isBlank()) throw new IllegalArgumentException("Un campo asociado no puede ser blanco; omítalo expresamente");
            return texto.strip();
        });
    }
}
