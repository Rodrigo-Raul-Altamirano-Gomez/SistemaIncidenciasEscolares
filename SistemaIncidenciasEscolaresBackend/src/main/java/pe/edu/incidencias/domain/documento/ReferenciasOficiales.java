package pe.edu.incidencias.domain.documento;

import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Relaciones de referencias sin alterar hechos, casos o sus estados. */
public final class ReferenciasOficiales {
    private final Map<UUID, List<ReferenciaOficial>> revisiones;
    private ReferenciasOficiales(Map<UUID, List<ReferenciaOficial>> revisiones) {
        var copia = new LinkedHashMap<UUID, List<ReferenciaOficial>>();
        revisiones.forEach((id, lista) -> copia.put(id, List.copyOf(lista)));
        this.revisiones = Map.copyOf(copia);
    }
    public static ReferenciasOficiales vacias() { return new ReferenciasOficiales(Map.of()); }
    public Map<UUID, List<ReferenciaOficial>> revisiones() { return revisiones; }
    public ReferenciasOficiales registrar(ReferenciaOficial referencia, ContextoAcceso contexto,
                                          DirectorioUsuarios directorio, RestriccionesAmbito restricciones) {
        return registrar(referencia, contexto, new PoliticaAcceso(directorio, restricciones));
    }
    public ReferenciasOficiales registrar(ReferenciaOficial referencia, ContextoAcceso contexto, PoliticaAcceso politica) {
        politica.exigirRecurso(referencia.id(), referencia.registro().fecha());
        PoliticaAcceso.exigir(politica.evaluar(referencia.registro().autorId(), Permiso.CORREGIR_HECHO, contexto, referencia.registro().fecha()));
        exigirContenido(referencia.ambito(), contexto.ambito());
        var anteriores = revisiones.getOrDefault(referencia.id(), List.of());
        if (!anteriores.isEmpty()) {
            var anterior = anteriores.getLast();
            if (anterior.equals(referencia)) return this;
            if (!anterior.ambito().equals(referencia.ambito()) || anterior.instrumento() != referencia.instrumento()
                    || !anterior.origenComunicado().equals(referencia.origenComunicado()))
                throw new IllegalArgumentException("La identidad, instrumento y origen no se sustituyen silenciosamente");
            if (referencia.registro().fecha().isBefore(anterior.registro().fecha())) throw new IllegalArgumentException("La revisión precede a la vigente");
            if ((anterior.codigoOAsiento().isPresent() && !anterior.codigoOAsiento().equals(referencia.codigoOAsiento()))
                    || (anterior.fechaRegistroExterno().isPresent() && !anterior.fechaRegistroExterno().equals(referencia.fechaRegistroExterno())))
                referencia.registro().exigirMotivo();
        }
        var historia = new ArrayList<>(anteriores);
        historia.add(referencia);
        var nuevas = new LinkedHashMap<>(revisiones);
        nuevas.put(referencia.id(), historia);
        return new ReferenciasOficiales(nuevas);
    }
    public List<ReferenciaOficial> consultar(ContextoAcceso contexto, UUID usuario, Instant ahora,
                                            DirectorioUsuarios directorio, RestriccionesAmbito restricciones) {
        return consultar(contexto, usuario, ahora, new PoliticaAcceso(directorio, restricciones));
    }
    public List<ReferenciaOficial> consultar(ContextoAcceso contexto, UUID usuario, Instant ahora, PoliticaAcceso politica) {
        PoliticaAcceso.exigir(politica.evaluar(usuario, Permiso.CONSULTAR_HECHO, contexto, ahora));
        var resultado = revisiones.values().stream().map(List::getLast)
                .filter(r -> r.ambito().seCruzaCon(contexto.ambito())).sorted(java.util.Comparator.comparing(ReferenciaOficial::id)).toList();
        if (resultado.stream().anyMatch(r -> r.registro().fecha().isAfter(ahora))) throw new IllegalArgumentException("La referencia contiene una revisión futura");
        resultado.forEach(r -> exigirContenido(r.ambito(), contexto.ambito()));
        resultado.forEach(r -> politica.exigirRecurso(r.id(), ahora));
        return resultado;
    }
    static void exigirContenido(AmbitoReserva menor, AmbitoReserva mayor) {
        if (!mayor.hechosIds().containsAll(menor.hechosIds()) || !mayor.reportesIds().containsAll(menor.reportesIds())
                || !mayor.casosIds().containsAll(menor.casosIds())) throw new IllegalArgumentException("Debe aportarse todo el ámbito reservado");
    }
}
