package pe.edu.incidencias.domain.documento;

import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.atencion.RegistroAtenciones;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class RegistroActas {
    private final Map<UUID, ActaAdjunta> actas;
    private RegistroActas(Map<UUID, ActaAdjunta> actas) { this.actas = Map.copyOf(actas); }
    public static RegistroActas vacio() { return new RegistroActas(Map.of()); }
    public RegistroActas registrar(ActaAdjunta acta, RegistroAtenciones atenciones, DirectorioUsuarios directorio,
                                   Optional<String> motivoSustitucionDirector) {
        var registro = acta.vigente().registro();
        exigirRecursos(acta, atenciones, registro.fecha());
        atenciones.consultarCaso(acta.casoId(), registro.autorId(), directorio, registro.fecha());
        var contexto = atenciones.contexto(acta.casoId());
        if (motivoSustitucionDirector.isPresent()) contexto = contexto.conMotivoSustitucion(motivoSustitucionDirector.get());
        var decision = new PoliticaAcceso(directorio, atenciones.restricciones(), atenciones.proteccion()).evaluar(registro.autorId(),
                Permiso.REGISTRAR_ACTO_COMITE, contexto, registro.fecha());
        PoliticaAcceso.exigir(decision);
        var anterior = actas.get(acta.id());
        var base = anterior == null ? acta : acta.conPrefijoIncorporado(anterior);
        if (anterior != null && anterior.revisiones().equals(base.revisiones())) return this;
        var incorporada = base.conReserva(decision.motivoSustitucion(), contexto.ambito());
        if (anterior != null && !incorporada.esContinuacionDe(anterior)) throw new IllegalArgumentException("La incorporación perdería historia del acta");
        int previas = anterior == null ? 0 : anterior.revisiones().size();
        if (acta.revisiones().size() != previas + 1) throw new IllegalArgumentException("Cada incorporación autoriza una única versión nueva del acta");
        if (actas.values().stream().filter(a -> !a.id().equals(acta.id())).flatMap(a -> a.revisiones().stream())
                .anyMatch(r -> r.archivo().id().equals(acta.vigente().archivo().id()))) throw new IllegalArgumentException("El archivo ya pertenece a otra acta");
        var nuevas = new LinkedHashMap<>(actas);
        nuevas.put(acta.id(), incorporada);
        return new RegistroActas(nuevas);
    }
    public List<ActaAdjunta> consultar(UUID casoId, UUID usuario, Instant ahora, RegistroAtenciones atenciones,
                                      DirectorioUsuarios directorio) {
        atenciones.consultarCaso(casoId, usuario, directorio, ahora);
        PoliticaAcceso.exigir(new PoliticaAcceso(directorio, atenciones.restricciones(), atenciones.proteccion()).evaluar(usuario,
                Permiso.CONSULTAR_DOCUMENTO_RESERVADO, atenciones.contexto(casoId), ahora));
        var resultado = actas.values().stream().filter(a -> a.casoId().equals(casoId)).sorted(java.util.Comparator.comparing(ActaAdjunta::id)).toList();
        resultado.forEach(a -> exigirConsulta(a.id(), usuario, ahora, atenciones, directorio));
        return resultado;
    }
    public ArchivoAdjunto archivoParaDescarga(UUID actaId, UUID archivoSolicitadoId, UUID usuario, Instant ahora,
                                               RegistroAtenciones actuales, DirectorioUsuarios directorio) {
        var acta = exigirConsulta(actaId, usuario, ahora, actuales, directorio);
        if (!acta.vigente().archivo().id().equals(archivoSolicitadoId)) throw new SecurityException("El archivo fue sustituido y no es la descarga vigente");
        return acta.vigente().archivo();
    }
    /** Autoriza también las versiones históricas que devuelve el agregado y las citadas en informes. */
    ActaAdjunta exigirConsulta(UUID actaId, UUID usuario, Instant ahora, RegistroAtenciones atenciones,
                               DirectorioUsuarios directorio) {
        var acta = Optional.ofNullable(actas.get(actaId)).orElseThrow(() -> new IllegalArgumentException("El acta no existe"));
        atenciones.consultarCaso(acta.casoId(), usuario, directorio, ahora);
        exigirRecursos(acta, atenciones, ahora);
        var politica = new PoliticaAcceso(directorio, atenciones.restricciones(), atenciones.proteccion());
        for (var revision : acta.revisiones()) {
            if (revision.registro().fecha().isAfter(ahora)) throw new IllegalArgumentException("El acta contiene una revisión futura");
            PoliticaAcceso.exigir(politica.evaluar(usuario, Permiso.CONSULTAR_DOCUMENTO_RESERVADO,
                    atenciones.contextoDocumento(acta.casoId(), revision.ambitoIncorporacion().orElseThrow()), ahora));
        }
        return acta;
    }
    private static void exigirRecursos(ActaAdjunta acta, RegistroAtenciones atenciones, Instant ahora) {
        if (!atenciones.proteccion().permiteRecurso(acta.id(), ahora)
                || acta.revisiones().stream().anyMatch(r -> !atenciones.proteccion().permiteRecurso(r.archivo().id(), ahora)))
            throw new SecurityException("El acta o una versión de su archivo tiene bloqueo jurídico");
    }
}
