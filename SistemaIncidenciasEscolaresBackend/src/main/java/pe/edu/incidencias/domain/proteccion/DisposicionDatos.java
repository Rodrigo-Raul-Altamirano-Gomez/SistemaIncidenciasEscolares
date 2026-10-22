package pe.edu.incidencias.domain.proteccion;

import pe.edu.incidencias.domain.acceso.DirectorioUsuarios;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Transformación delimitada y comprobación de un manifiesto operativo. Nunca marca éxito al planificar. */
public final class DisposicionDatos {
    public enum Decision { SUPRESION, ANONIMIZACION }
    public enum TipoRecurso { IDENTIDAD, CONTEXTO, REPORTE, ACTUACION, COMPROMISO, CASO,
        INFORME, ACTA, COPIA_OPERATIVA, INDICE, EXPORTACION }
    public enum Tratamiento { ELIMINAR_RECURSO, RETIRAR_CAMPOS, SUSTITUIR_CAMPOS }
    /** Representación de intercambio del adaptador: campos explícitos, sin interpretación automática de relatos. */
    public record Representacion(UUID recursoId, TipoRecurso tipo, Map<String, String> campos) {
        public Representacion {
            Objects.requireNonNull(recursoId); Objects.requireNonNull(tipo); campos = Map.copyOf(campos);
            if (campos.keySet().stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Campo sin referencia");
        }
    }
    public record CambioRecurso(UUID recursoId, TipoRecurso tipo, Tratamiento tratamiento,
                                 Set<String> camposRetirados, Map<String, String> sustituciones,
                                 Set<String> camposDeTerceros) {
        public CambioRecurso {
            Objects.requireNonNull(recursoId); Objects.requireNonNull(tipo); Objects.requireNonNull(tratamiento);
            camposRetirados = Set.copyOf(camposRetirados); sustituciones = Map.copyOf(sustituciones);
            camposDeTerceros = Set.copyOf(camposDeTerceros);
            if (!java.util.Collections.disjoint(camposRetirados, sustituciones.keySet())
                    || !java.util.Collections.disjoint(camposDeTerceros, camposRetirados)
                    || !java.util.Collections.disjoint(camposDeTerceros, sustituciones.keySet()))
                throw new IllegalArgumentException("La disposición no puede afectar campos conservados de terceros");
            switch (tratamiento) {
                case ELIMINAR_RECURSO -> {
                    if (!camposDeTerceros.isEmpty() || !camposRetirados.isEmpty() || !sustituciones.isEmpty())
                        throw new IllegalArgumentException("No se elimina íntegramente un recurso necesario para terceros");
                }
                case RETIRAR_CAMPOS -> {
                    if (camposRetirados.isEmpty() || !sustituciones.isEmpty()) throw new IllegalArgumentException("Faltan campos a retirar");
                }
                case SUSTITUIR_CAMPOS -> {
                    if (sustituciones.isEmpty()) throw new IllegalArgumentException("Faltan sustituciones explícitas revisadas");
                }
            }
        }
    }
    public record Retencion(Set<UUID> recursos, String fundamento, String constancia, Optional<Instant> hasta) {
        public Retencion {
            recursos = Set.copyOf(recursos); fundamento = texto(fundamento); constancia = texto(constancia);
            Objects.requireNonNull(hasta);
            if (recursos.isEmpty()) throw new IllegalArgumentException("La retención requiere recursos identificados");
        }
        boolean vigente(Instant ahora) { return hasta.map(ahora::isBefore).orElse(true); }
    }
    public record Plan(UUID id, Decision decision, AlcanceProteccion alcance, List<CambioRecurso> cambios,
                        String inventarioCompleto, String tratamientoRespaldos, String verificacionAnonimizacion,
                        RegistroCambio registro, String constancia) {
        public Plan {
            Objects.requireNonNull(id); Objects.requireNonNull(decision); Objects.requireNonNull(alcance);
            cambios = List.copyOf(cambios); inventarioCompleto = texto(inventarioCompleto);
            tratamientoRespaldos = texto(tratamientoRespaldos); Objects.requireNonNull(registro);
            constancia = texto(constancia); Objects.requireNonNull(verificacionAnonimizacion);
            if (decision == Decision.ANONIMIZACION) verificacionAnonimizacion = texto(verificacionAnonimizacion);
            if (cambios.isEmpty() || cambios.stream().map(CambioRecurso::recursoId).distinct().count() != cambios.size())
                throw new IllegalArgumentException("El inventario debe contener recursos únicos");
            var ids = cambios.stream().map(CambioRecurso::recursoId).collect(Collectors.toSet());
            if (!alcance.recursos().equals(ids)) throw new IllegalArgumentException("El alcance de recursos debe coincidir con el manifiesto completo");
            registro.exigirMotivo();
        }
    }
    public record Comprobacion(UUID recursoId, Optional<Representacion> vigente) {
        public Comprobacion {
            Objects.requireNonNull(recursoId); Objects.requireNonNull(vigente);
            if (vigente.filter(r -> !r.recursoId().equals(recursoId)).isPresent())
                throw new IllegalArgumentException("La comprobación pertenece a otro recurso");
        }
    }
    /** No contiene los valores retirados; el adaptador aplica el resultado de manera atómica y relee. */
    public static final class Preparacion {
        private final Plan plan;
        private final List<Comprobacion> resultadoEsperado;
        private Preparacion(Plan plan, List<Comprobacion> resultadoEsperado) {
            this.plan = Objects.requireNonNull(plan); this.resultadoEsperado = List.copyOf(resultadoEsperado);
        }
        public Plan plan() { return plan; }
        public List<Comprobacion> resultadoEsperado() { return resultadoEsperado; }
    }
    public record Ejecucion(UUID planId, Set<UUID> recursosVerificados, RegistroCambio registro,
                             String constanciaVerificacion, String constanciaAutorizacion) {
        public Ejecucion {
            Objects.requireNonNull(planId); recursosVerificados = Set.copyOf(recursosVerificados);
            Objects.requireNonNull(registro); constanciaVerificacion = texto(constanciaVerificacion);
            constanciaAutorizacion = texto(constanciaAutorizacion);
        }
    }
    private DisposicionDatos() { }

    public static Preparacion preparar(Plan plan, List<Representacion> originales, List<Retencion> retenciones,
                                         AutorizacionProteccion autorizacion, DirectorioUsuarios directorio) {
        autorizacion.exigir(plan.registro().autorId(), AutorizacionProteccion.Operacion.DISPONER,
                plan.alcance(), plan.registro().fecha(), directorio);
        exigirSinRetenciones(plan, retenciones, plan.registro().fecha());
        var porId = originales.stream().collect(Collectors.toMap(Representacion::recursoId, r -> r));
        if (!porId.keySet().equals(plan.alcance().recursos())) throw new IllegalArgumentException("Debe aportarse todo el inventario, sin recursos ajenos");
        var esperado = new ArrayList<Comprobacion>();
        for (var cambio : plan.cambios()) {
            var original = porId.get(cambio.recursoId());
            if (original.tipo() != cambio.tipo()) throw new IllegalArgumentException("El tipo no corresponde al recurso");
            if (!original.campos().keySet().containsAll(cambio.camposRetirados())
                    || !original.campos().keySet().containsAll(cambio.sustituciones().keySet())
                    || !original.campos().keySet().containsAll(cambio.camposDeTerceros()))
                throw new IllegalArgumentException("El manifiesto señala campos ausentes o una versión incorrecta");
            if (cambio.tratamiento() == Tratamiento.ELIMINAR_RECURSO) {
                esperado.add(new Comprobacion(original.recursoId(), Optional.empty()));
            } else {
                var campos = new HashMap<>(original.campos());
                cambio.camposRetirados().forEach(campos::remove); campos.putAll(cambio.sustituciones());
                esperado.add(new Comprobacion(original.recursoId(), Optional.of(new Representacion(original.recursoId(), original.tipo(), campos))));
            }
        }
        return new Preparacion(plan, esperado);
    }

    /** Solo tras aplicar y releer TODOS los recursos/campos; la entrada proviene del adaptador autorizado. */
    public static Ejecucion confirmar(Preparacion preparacion, List<Comprobacion> relectura,
                                       List<Retencion> retenciones, AutorizacionProteccion autorizacion,
                                       RegistroCambio cambio, String constanciaVerificacion, DirectorioUsuarios directorio) {
        var plan = preparacion.plan();
        if (cambio.fecha().isBefore(plan.registro().fecha())) throw new IllegalArgumentException("La ejecución precede al plan");
        autorizacion.exigir(cambio.autorId(), AutorizacionProteccion.Operacion.DISPONER, plan.alcance(), cambio.fecha(), directorio);
        exigirSinRetenciones(plan, retenciones, cambio.fecha());
        var actual = relectura.stream().collect(Collectors.toMap(Comprobacion::recursoId, r -> r));
        var esperado = preparacion.resultadoEsperado().stream().collect(Collectors.toMap(Comprobacion::recursoId, r -> r));
        if (!actual.equals(esperado)) throw new IllegalStateException("La disposición no está comprobada: faltan recursos o difieren datos/campos de terceros");
        return new Ejecucion(plan.id(), actual.keySet(), cambio, constanciaVerificacion, autorizacion.constancia());
    }
    private static void exigirSinRetenciones(Plan plan, List<Retencion> retenciones, Instant ahora) {
        if (retenciones.stream().anyMatch(r -> r.vigente(ahora) && r.recursos().stream().anyMatch(plan.alcance().recursos()::contains)))
            throw new IllegalStateException("Una retención vigente impide esta disposición");
    }
    private static String texto(String valor) {
        if (Objects.requireNonNull(valor).isBlank()) throw new IllegalArgumentException("La decisión requiere constancia explícita");
        return valor.strip();
    }
}
