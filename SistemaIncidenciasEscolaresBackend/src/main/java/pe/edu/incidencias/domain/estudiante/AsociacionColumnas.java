package pe.edu.incidencias.domain.estudiante;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Asociación revisada reutilizable de encabezados semánticos; extraer Excel corresponde al adaptador. */
public record AsociacionColumnas(AlcanceImportacion alcance, Map<Encabezado, Destino> asociaciones) {
    public enum Campo {
        NOMBRE_ESTUDIANTE, DOCUMENTO_ESTUDIANTE, NACIMIENTO, SEXO, SITUACION, GRADO, SECCION, GRUPO,
        NOMBRE_FAMILIAR, RELACION_FAMILIAR, DOCUMENTO_FAMILIAR, TELEFONO_FAMILIAR,
        TELEFONO_ESTUDIANTE, DOMICILIO, ANTECEDENTE
    }
    /** Los niveles contienen el contexto de encabezados combinados/multifila, nunca solo el subencabezado. */
    public record Encabezado(List<String> niveles) {
        public Encabezado {
            niveles = List.copyOf(niveles).stream().map(AlcanceImportacion::texto)
                    .map(BusquedaEstudiantes::normalizar).toList();
            if (niveles.isEmpty()) throw new IllegalArgumentException("Encabezado sin contexto");
        }
    }
    public record Destino(Campo campo, Optional<String> concepto) {
        public Destino {
            Objects.requireNonNull(campo, "campo");
            concepto = ContactoFamiliar.limpiar(concepto);
            if (tipoReservado(campo).isPresent() != concepto.isPresent())
                throw new IllegalArgumentException("Solo campos reservados requieren concepto institucional admitido");
        }
    }
    public record Resultado(Map<Integer, Destino> seleccionadas, Set<Integer> omitidas,
                             Map<Integer, String> revisar) {
        public Resultado {
            seleccionadas = Map.copyOf(seleccionadas);
            omitidas = Set.copyOf(omitidas);
            revisar = Map.copyOf(revisar);
        }
    }

    public AsociacionColumnas {
        Objects.requireNonNull(alcance, "alcance");
        asociaciones = Map.copyOf(asociaciones);
        var destinos = new HashSet<Destino>();
        for (var destino : asociaciones.values()) {
            tipoReservado(destino.campo()).ifPresent(tipo -> alcance.exigir(destino.concepto().orElseThrow(), tipo));
            if (!destinos.add(destino)) throw new IllegalArgumentException("Dos encabezados asociados al mismo dato");
        }
        boolean combinado = destinos.stream().anyMatch(d -> d.campo() == Campo.GRUPO);
        boolean separado = destinos.stream().anyMatch(d -> d.campo() == Campo.GRADO || d.campo() == Campo.SECCION);
        if (combinado && separado) throw new IllegalArgumentException("Revise la asociación contradictoria del grupo");
    }

    /** Reubica por encabezado completo, sin adivinar por letras de columna ni ejecutar contenido de celdas. */
    public Resultado reutilizar(Map<Integer, Encabezado> columnas, AlcanceImportacion vigente) {
        if (!alcance.equals(vigente)) throw new IllegalArgumentException("Revise el perfil ante un cambio de alcance institucional");
        var seleccionadas = new HashMap<Integer, Destino>();
        var omitidas = new HashSet<Integer>();
        var revisar = new HashMap<Integer, String>();
        var ubicaciones = new HashMap<Encabezado, List<Integer>>();
        Map.copyOf(columnas).forEach((columna, encabezado) -> {
            if (columna < 1) throw new IllegalArgumentException("Columna debe ser positiva");
            ubicaciones.computeIfAbsent(encabezado, _ -> new ArrayList<>()).add(columna);
        });
        ubicaciones.forEach((encabezado, posiciones) -> {
            var destino = asociaciones.get(encabezado);
            if (destino == null) omitidas.addAll(posiciones);
            else if (posiciones.size() > 1) posiciones.forEach(columna ->
                    revisar.put(columna, "Encabezado repetido; revise la asociación o deje la columna omitida"));
            else seleccionadas.put(posiciones.getFirst(), destino);
        });
        return new Resultado(seleccionadas, omitidas, revisar);
    }

    private static Optional<AlcanceImportacion.Tipo> tipoReservado(Campo campo) {
        return switch (campo) {
            case NOMBRE_FAMILIAR, RELACION_FAMILIAR, DOCUMENTO_FAMILIAR, TELEFONO_FAMILIAR ->
                    Optional.of(AlcanceImportacion.Tipo.CONTACTO_FAMILIAR);
            case TELEFONO_ESTUDIANTE -> Optional.of(AlcanceImportacion.Tipo.TELEFONO_ESTUDIANTE);
            case DOMICILIO -> Optional.of(AlcanceImportacion.Tipo.DOMICILIO);
            case ANTECEDENTE -> Optional.of(AlcanceImportacion.Tipo.ANTECEDENTE);
            default -> Optional.empty();
        };
    }
}
