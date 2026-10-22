package pe.edu.incidencias.domain.estudiante;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Datos reservados vigentes. Ninguna entrada es un hecho ni una atribución de responsabilidad. */
public record ContextoEstudiante(UUID estudianteId, Optional<Dato<ContactoFamiliar>> contacto,
                                 Optional<Dato<String>> telefonoEstudiante, Optional<Dato<String>> domicilio,
                                 Map<String, Dato<Antecedente>> antecedentes) {
    public enum Calidad { DECLARADO, NEGACION_DECLARADA }
    public record Antecedente(String etiqueta, String contenido, Calidad calidad,
                              Optional<LocalDate> fechaAntecedente, Optional<String> periodo) {
        public Antecedente {
            etiqueta = AlcanceImportacion.texto(etiqueta);
            contenido = AlcanceImportacion.texto(contenido);
            Objects.requireNonNull(calidad, "calidad");
            Objects.requireNonNull(fechaAntecedente, "fechaAntecedente");
            periodo = ContactoFamiliar.limpiar(periodo);
        }
    }
    /** Incluye localización y fundamento de cada campo, sin conservar el archivo descartado. */
    public record Procedencia(UUID loteId, String fuente, String hoja, int fila, UUID alcanceId,
                              String concepto, String fundamento, RegistroCambio incorporacion) {
        public Procedencia {
            Objects.requireNonNull(loteId, "loteId");
            fuente = AlcanceImportacion.texto(fuente);
            hoja = AlcanceImportacion.texto(hoja);
            if (fila < 1) throw new IllegalArgumentException("Fila inválida");
            Objects.requireNonNull(alcanceId, "alcanceId");
            concepto = AlcanceImportacion.texto(concepto);
            fundamento = AlcanceImportacion.texto(fundamento);
            Objects.requireNonNull(incorporacion, "incorporacion");
        }
    }
    public record Dato<T>(T valor, Procedencia procedencia) {
        public Dato {
            Objects.requireNonNull(valor, "valor");
            Objects.requireNonNull(procedencia, "procedencia");
        }
    }
    public ContextoEstudiante {
        Objects.requireNonNull(estudianteId, "estudianteId");
        Objects.requireNonNull(contacto, "contacto");
        Objects.requireNonNull(telefonoEstudiante, "telefonoEstudiante");
        Objects.requireNonNull(domicilio, "domicilio");
        antecedentes = Map.copyOf(antecedentes);
    }
    public static ContextoEstudiante vacio(UUID estudianteId) {
        return new ContextoEstudiante(estudianteId, Optional.empty(), Optional.empty(), Optional.empty(), Map.of());
    }
}
