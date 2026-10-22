package pe.edu.incidencias.domain.estudiante;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Versión institucional documentada. Una columna encontrada no amplía el alcance autorizado. */
public record AlcanceImportacion(UUID id, Map<String, Concepto> conceptos, String referenciaInstitucional) {
    public enum Tipo { CONTACTO_FAMILIAR, TELEFONO_ESTUDIANTE, DOMICILIO, ANTECEDENTE }

    public record Concepto(String clave, String etiqueta, Tipo tipo, String fundamento) {
        public Concepto {
            clave = texto(clave);
            etiqueta = texto(etiqueta);
            Objects.requireNonNull(tipo, "tipo");
            fundamento = texto(fundamento);
        }
    }

    public AlcanceImportacion {
        Objects.requireNonNull(id, "id");
        conceptos = Map.copyOf(conceptos);
        referenciaInstitucional = texto(referenciaInstitucional);
        conceptos.forEach((clave, concepto) -> {
            if (!clave.equals(concepto.clave())) throw new IllegalArgumentException("Clave de concepto inconsistente");
        });
        for (var tipo : Tipo.values()) {
            if (tipo != Tipo.ANTECEDENTE && conceptos.values().stream().filter(c -> c.tipo() == tipo).count() > 1)
                throw new IllegalArgumentException("Un solo concepto para cada campo de contacto");
        }
    }

    public Concepto exigir(String clave, Tipo tipo) {
        var concepto = conceptos.get(clave);
        if (concepto == null || concepto.tipo() != tipo)
            throw new IllegalArgumentException("Concepto no admitido para ese campo: " + clave);
        return concepto;
    }

    static String texto(String valor) {
        if (Objects.requireNonNull(valor, "texto").isBlank()) throw new IllegalArgumentException("Texto obligatorio vacío");
        return valor.strip();
    }
}
