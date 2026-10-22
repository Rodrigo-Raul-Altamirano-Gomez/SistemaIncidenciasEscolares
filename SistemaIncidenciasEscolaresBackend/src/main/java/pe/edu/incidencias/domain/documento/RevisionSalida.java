package pe.edu.incidencias.domain.documento;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Revisión de la salida completa; el mapa contiene solo los fragmentos que necesitan adecuación. */
public record RevisionSalida(UUID casoId, Set<UUID> personasAdmitidas, Set<UUID> referenciasAdmitidas,
                              Map<String, String> textosRevisados, Map<String, String> contextoSeleccionado,
                              String baseComunicacion, String evaluacionRiesgo, RegistroCambio revision,
                              Set<UUID> contactosAdmitidos, Set<UUID> documentosExternosAdmitidos) {
    public RevisionSalida(UUID casoId, Set<UUID> personasAdmitidas, Set<UUID> referenciasAdmitidas,
                           Map<String, String> textosRevisados, Map<String, String> contextoSeleccionado,
                           String baseComunicacion, String evaluacionRiesgo, RegistroCambio revision) {
        this(casoId, personasAdmitidas, referenciasAdmitidas, textosRevisados, contextoSeleccionado,
                baseComunicacion, evaluacionRiesgo, revision, Set.of(), Set.of());
    }
    public RevisionSalida {
        Objects.requireNonNull(casoId);
        personasAdmitidas = Set.copyOf(personasAdmitidas);
        referenciasAdmitidas = Set.copyOf(referenciasAdmitidas);
        contactosAdmitidos = Set.copyOf(contactosAdmitidos); documentosExternosAdmitidos = Set.copyOf(documentosExternosAdmitidos);
        if (!personasAdmitidas.containsAll(contactosAdmitidos) || !personasAdmitidas.containsAll(documentosExternosAdmitidos))
            throw new IllegalArgumentException("Los datos externos seleccionados requieren admitir a la persona");
        textosRevisados = Map.copyOf(textosRevisados);
        contextoSeleccionado = Map.copyOf(contextoSeleccionado);
        Objects.requireNonNull(revision);
        if (baseComunicacion == null || baseComunicacion.isBlank()) throw new IllegalArgumentException("La revisión requiere la base institucional declarada para comunicar");
        if (evaluacionRiesgo == null || evaluacionRiesgo.isBlank()) throw new IllegalArgumentException("La revisión debe documentar el riesgo de identificación");
        textosRevisados.forEach((clave, texto) -> validar(clave, texto));
        contextoSeleccionado.forEach((clave, texto) -> validar(clave, texto));
    }
    public String texto(String clave, String original) {
        return textosRevisados.getOrDefault(clave, original);
    }
    private static void validar(String clave, String texto) {
        if (clave.isBlank() || texto.isBlank()) throw new IllegalArgumentException("La revisión no admite campos vacíos");
    }
}
