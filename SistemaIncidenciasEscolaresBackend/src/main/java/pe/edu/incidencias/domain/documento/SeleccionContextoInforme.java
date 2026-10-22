package pe.edu.incidencias.domain.documento;

import pe.edu.incidencias.domain.acceso.ContextoAcceso;
import pe.edu.incidencias.domain.acceso.PoliticaAcceso;
import pe.edu.incidencias.domain.estudiante.ContextoEstudiante;
import pe.edu.incidencias.domain.estudiante.RegistroImportacion;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Recupera solo los campos pertinentes seleccionados; el generador aplica después la revisión externa. */
public final class SeleccionContextoInforme {
    public enum Campo { FAMILIAR_NOMBRE, FAMILIAR_RELACION, FAMILIAR_TELEFONO, FAMILIAR_DOCUMENTO,
        TELEFONO_ESTUDIANTE, DOMICILIO }
    private SeleccionContextoInforme() { }

    public static Map<String, String> recuperar(UUID estudianteId, Set<Campo> campos, Set<String> antecedentes,
                                                RegistroImportacion registro, ContextoAcceso contexto,
                                                PoliticaAcceso politica, UUID usuario, Instant corte, Instant ahora) {
        Objects.requireNonNull(corte);
        if (corte.isAfter(ahora)) throw new IllegalArgumentException("Corte futuro");
        campos = Set.copyOf(campos);
        antecedentes = Set.copyOf(antecedentes);
        var ficha = registro.consultarContexto(estudianteId, contexto, politica, usuario, ahora);
        var historia = registro.consultarHistoriaContexto(estudianteId, contexto, politica, usuario, ahora);
        if (!historia.isEmpty()) {
            ficha = historia.getFirst().anterior();
            for (var revision : historia) {
                if (revision.cambio().fecha().isAfter(corte)) break;
                ficha = revision.resultado();
            }
        }
        var resultado = new HashMap<String, String>();
        for (var campo : campos) {
            String clave = estudianteId + ":" + campo.name();
            switch (campo) {
                case TELEFONO_ESTUDIANTE -> incorporar(resultado, clave, ficha.telefonoEstudiante(), corte);
                case DOMICILIO -> incorporar(resultado, clave, ficha.domicilio(), corte);
                default -> {
                    if (ficha.contacto().isEmpty()) {
                        resultado.put(clave, "No informado");
                        break;
                    }
                    var dato = ficha.contacto().orElseThrow();
                    exigirCorte(dato.procedencia(), corte);
                    var familiar = dato.valor();
                    Optional<String> texto = switch (campo) {
                        case FAMILIAR_NOMBRE -> familiar.nombre();
                        case FAMILIAR_RELACION -> familiar.relacionInformada();
                        case FAMILIAR_TELEFONO -> familiar.telefono();
                        case FAMILIAR_DOCUMENTO -> familiar.documento().map(d -> d.tipo() + " " + d.numero());
                        default -> throw new IllegalStateException("Campo no familiar");
                    };
                    resultado.put(clave, texto.orElse("No informado"));
                }
            }
        }
        for (var clave : antecedentes) {
            var dato = ficha.antecedentes().get(clave);
            if (dato == null) throw new IllegalArgumentException("Antecedente seleccionado no disponible: " + clave);
            exigirCorte(dato.procedencia(), corte);
            var antecedente = dato.valor();
            resultado.put(estudianteId + ":antecedente:" + clave, antecedente.etiqueta() + ": " + antecedente.contenido()
                    + " [" + antecedente.calidad() + "; fecha: " + antecedente.fechaAntecedente().map(Object::toString).orElse("no informada")
                    + "; período: " + antecedente.periodo().orElse("no informado") + "]");
        }
        return Map.copyOf(resultado);
    }

    private static void incorporar(Map<String, String> resultado, String clave,
                                   Optional<ContextoEstudiante.Dato<String>> dato, Instant corte) {
        dato.ifPresent(d -> exigirCorte(d.procedencia(), corte));
        resultado.put(clave, dato.map(ContextoEstudiante.Dato::valor).orElse("No informado"));
    }
    private static void exigirCorte(ContextoEstudiante.Procedencia procedencia, Instant corte) {
        if (procedencia.incorporacion().fecha().isAfter(corte))
            throw new IllegalArgumentException("El contexto es posterior al corte; use la versión histórica pertinente");
    }
}
