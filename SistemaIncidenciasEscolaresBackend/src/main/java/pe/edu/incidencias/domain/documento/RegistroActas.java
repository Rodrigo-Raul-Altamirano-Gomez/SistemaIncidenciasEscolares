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
        atenciones.consultarCaso(acta.casoId(), registro.autorId(), directorio, registro.fecha());
        var contexto = atenciones.contexto(acta.casoId());
        if (motivoSustitucionDirector.isPresent()) contexto = contexto.conMotivoSustitucion(motivoSustitucionDirector.get());
        var decision = new PoliticaAcceso(directorio, atenciones.restricciones()).evaluar(registro.autorId(),
                Permiso.REGISTRAR_ACTO_COMITE, contexto, registro.fecha());
        PoliticaAcceso.exigir(decision);
        var anterior = actas.get(acta.id());
        var base = anterior == null ? acta : acta.conPrefijoIncorporado(anterior);
        if (anterior != null && anterior.revisiones().equals(base.revisiones())) return this;
        var incorporada = base.conReserva(decision.motivoSustitucion(), contexto.ambito());
        if (anterior != null && !incorporada.esContinuacionDe(anterior)) throw new IllegalArgumentException("La incorporación perdería historia del acta");
        int previas = anterior == null ? 0 : anterior.revisiones().size();
        if (anterior != null && anterior.revisiones().equals(incorporada.revisiones())) return this;
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
        PoliticaAcceso.exigir(new PoliticaAcceso(directorio, atenciones.restricciones()).evaluar(usuario,
                Permiso.CONSULTAR_DOCUMENTO_RESERVADO, atenciones.contexto(casoId), ahora));
        var resultado = actas.values().stream().filter(a -> a.casoId().equals(casoId)).sorted(java.util.Comparator.comparing(ActaAdjunta::id)).toList();
        for (var acta : resultado) PoliticaAcceso.exigir(new PoliticaAcceso(directorio, atenciones.restricciones()).evaluar(usuario,
                Permiso.CONSULTAR_DOCUMENTO_RESERVADO, atenciones.contextoDocumento(casoId,
                        acta.vigente().ambitoIncorporacion().orElseThrow()), ahora));
        if (resultado.stream().anyMatch(a -> a.vigente().registro().fecha().isAfter(ahora))) throw new IllegalArgumentException("El acta contiene una revisión futura");
        return resultado;
    }
    public ArchivoAdjunto archivoParaDescarga(UUID actaId, UUID archivoSolicitadoId, UUID usuario, Instant ahora,
                                               RegistroAtenciones actuales, DirectorioUsuarios directorio) {
        var acta = java.util.Optional.ofNullable(actas.get(actaId)).orElseThrow(() -> new IllegalArgumentException("El acta no existe"));
        consultar(acta.casoId(), usuario, ahora, actuales, directorio);
        if (!acta.vigente().archivo().id().equals(archivoSolicitadoId)) throw new SecurityException("El archivo fue sustituido y no es la descarga vigente");
        return acta.vigente().archivo();
    }
}
