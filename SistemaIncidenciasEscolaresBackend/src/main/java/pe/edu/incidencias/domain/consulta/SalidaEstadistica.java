package pe.edu.incidencias.domain.consulta;

import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.atencion.RegistroAtenciones;
import pe.edu.incidencias.domain.incidencia.Hecho;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.util.*;

/** Plan exportable, sin bytes ni envío. Aplicación audita emisión/descarga y adaptadores generan PDF/CSV/XLSX. */
public final class SalidaEstadistica {
    public enum Formato { PDF, CSV, XLSX }
    public record RevisionExterna(String finalidad, String baseComunicacion, String destinatario,
                                   String evaluacionRiesgo, boolean riesgoIdentificacionResuelto,
                                   RegistroCambio revision) {
        public RevisionExterna {
            for (var texto : List.of(finalidad, baseComunicacion, destinatario, evaluacionRiesgo))
                if (texto.isBlank()) throw new IllegalArgumentException("La revisión externa requiere finalidad, base, destinatario y riesgo");
            Objects.requireNonNull(revision);
            if (!riesgoIdentificacionResuelto) throw new IllegalArgumentException("No se emite una salida con riesgo identificable pendiente");
        }
    }
    public record Tabla(Formato formato, Instant corte, String unidad, String criterioTemporal,
                         ConsultaEstadistica consulta, ResultadoEstadistico.Cobertura cobertura,
                         List<String> advertencias, List<String> columnas, List<List<String>> filas,
                         Optional<RevisionExterna> revisionExterna) {
        public Tabla {
            advertencias = List.copyOf(advertencias); columnas = List.copyOf(columnas);
            filas = filas.stream().map(List::copyOf).toList();
            Objects.requireNonNull(revisionExterna);
        }
    }
    private final UUID id;
    private final ResultadoEstadistico resultado;
    private final RegistroCambio emision;
    private final Optional<RevisionExterna> externa;

    private SalidaEstadistica(UUID id, ResultadoEstadistico resultado, RegistroCambio emision,
                              Optional<RevisionExterna> externa) {
        this.id = Objects.requireNonNull(id);
        this.resultado = Objects.requireNonNull(resultado);
        this.emision = Objects.requireNonNull(emision);
        this.externa = Objects.requireNonNull(externa);
        if (emision.fecha().isBefore(resultado.corte())) throw new IllegalArgumentException("La emisión precede al corte");
        externa.ifPresent(r -> {
            if (!r.revision().autorId().equals(emision.autorId()) || !r.revision().fecha().equals(emision.fecha()))
                throw new IllegalArgumentException("La revisión debe corresponder a esta emisión y responsable");
        });
    }
    public static SalidaEstadistica interna(UUID id, ResultadoEstadistico resultado, RegistroAtenciones actuales,
                                            PoliticaAcceso politicaActual, RegistroCambio emision) {
        var salida = new SalidaEstadistica(id, resultado, emision, Optional.empty());
        salida.autorizar(actuales, politicaActual, emision.autorId(), emision.fecha());
        return salida;
    }
    public static SalidaEstadistica externa(UUID id, ResultadoEstadistico resultado, RevisionExterna revision,
                                            RegistroAtenciones actuales, PoliticaAcceso politicaActual) {
        var salida = new SalidaEstadistica(id, resultado, revision.revision(), Optional.of(revision));
        salida.autorizar(actuales, politicaActual, revision.revision().autorId(), revision.revision().fecha());
        return salida;
    }
    public UUID id() { return id; }
    public RegistroCambio emision() { return emision; }

    /** Cada descarga revalida todas las fuentes del corte. Nunca entrega un corte parcialmente filtrado. */
    public Tabla descargar(Formato formato, UUID usuario, RegistroAtenciones actuales,
                            PoliticaAcceso politicaActual, Instant ahora) {
        Objects.requireNonNull(formato);
        if (ahora.isBefore(emision.fecha())) throw new IllegalArgumentException("La descarga precede a la emisión");
        autorizar(actuales, politicaActual, usuario, ahora);
        var columnas = new ArrayList<String>();
        resultado.consulta().agrupaciones().forEach(d -> columnas.add(d.name())); columnas.add("CANTIDAD");
        var filas = new ArrayList<List<String>>();
        for (var celda : resultado.celdas()) {
            var fila = new ArrayList<String>();
            celda.coordenadas().forEach(v -> fila.add(textoSeguro(v)));
            fila.add(Long.toString(celda.cantidad())); filas.add(List.copyOf(fila));
        }
        return new Tabla(formato, resultado.corte(), resultado.consulta().medida().unidad(),
                resultado.consulta().medida().criterioTemporal(), resultado.consulta(), resultado.cobertura(),
                resultado.advertencias(), columnas, filas, externa);
    }
    private void autorizar(RegistroAtenciones actuales, PoliticaAcceso politica, UUID usuario, Instant ahora) {
        if (!actuales.proteccion().permiteRecurso(id, ahora)) throw new SecurityException("Salida con bloqueo jurídico vigente");
        politica.exigirRecurso(id, ahora);
        for (var recurso : resultado.recursos()) {
            politica.exigirRecurso(recurso, ahora);
            if (!actuales.proteccion().permiteRecurso(recurso, ahora)) throw new SecurityException("Recurso fuente actualmente bloqueado");
        }
        PoliticaAcceso.exigir(politica.evaluarPersona(usuario, Permiso.EXPORTAR_ATENCION, new UUID(0, 0), ahora));
        if (resultado.fuentes().isEmpty()) return;
        var fuentes = resultado.fuentes().orElseThrow();
        var hechosIds = new HashSet<>(fuentes.hechosIds()); hechosIds.addAll(fuentes.reportesIds());
        var hechos = new LinkedHashMap<UUID, Hecho>();
        AmbitoReserva ambito = fuentes;
        if (!hechosIds.isEmpty()) {
            var actual = actuales.contextoDeHechos(hechosIds);
            ambito = ambito.unir(actual.ambito()); actual.hechos().forEach(h -> hechos.put(h.id(), h));
        }
        for (var casoId : fuentes.casosIds()) {
            if (!actuales.casoDisponible(casoId, ahora)) throw new SecurityException("Atención fuente actualmente bloqueada");
            var contexto = actuales.contextoDocumento(casoId, fuentes);
            ambito = ambito.unir(contexto.ambito()); contexto.hechos().forEach(h -> hechos.put(h.id(), h));
        }
        if (!hechos.isEmpty() || !fuentes.casosIds().isEmpty()) {
            var contexto = new ContextoAcceso(ambito, List.copyOf(hechos.values()), Optional.empty(), Optional.empty());
            if (!actuales.proteccion().permite(contexto, ahora) || actuales.restricciones().estaExcluido(usuario, ambito))
                throw new SecurityException("Fuentes de la salida actualmente restringidas");
            PoliticaAcceso.exigir(politica.evaluar(usuario, Permiso.EXPORTAR_ATENCION, contexto, ahora));
        }
    }
    private static String textoSeguro(String valor) {
        // Defensa de contenido tabular ante fórmulas; el adaptador sigue siendo responsable del escape del formato.
        String inicial = valor.stripLeading();
        return !inicial.isEmpty() && "=+-@".indexOf(inicial.charAt(0)) >= 0 ? "'" + valor : valor;
    }
}
