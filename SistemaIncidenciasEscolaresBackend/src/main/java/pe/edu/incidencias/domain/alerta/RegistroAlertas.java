package pe.edu.incidencias.domain.alerta;

import pe.edu.incidencias.domain.acceso.*;



import pe.edu.incidencias.domain.catalogo.Categoria;
import pe.edu.incidencias.domain.catalogo.ClasificacionCategoria;
import pe.edu.incidencias.domain.catalogo.ParametrosReiteracion;
import pe.edu.incidencias.domain.incidencia.Hecho;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.incidencia.ReporteIncidencia;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Estado de señales y avisos internos; no crea atención ni realiza entrega fuera de la aplicación. */
public final class RegistroAlertas {
    private final Map<UUID, Alerta> alertas;
    private final Map<UUID, Notificacion> notificaciones;
    private final Set<UUID> sinDestinatario;

    private RegistroAlertas(Map<UUID, Alerta> alertas, Map<UUID, Notificacion> notificaciones, Set<UUID> sinDestinatario) {
        this.alertas = Map.copyOf(alertas);
        this.notificaciones = Map.copyOf(notificaciones);
        this.sinDestinatario = Set.copyOf(sinDestinatario);
    }
    public static RegistroAlertas vacio() { return new RegistroAlertas(Map.of(), Map.of(), Set.of()); }
    /** Carga únicamente los episodios/fuentes afectados y sus avisos; no requiere las señales del colegio completo. */
    public static RegistroAlertas cargarAmbito(List<Alerta> alertas, List<Notificacion> notificaciones, Set<UUID> sinDestinatario) {
        var porId = new LinkedHashMap<UUID, Alerta>();
        for (var alerta : alertas) if (porId.putIfAbsent(alerta.id(), alerta) != null) throw new IllegalArgumentException("Señal repetida");
        var avisos = new LinkedHashMap<UUID, Notificacion>();
        for (var aviso : notificaciones) {
            if (!porId.containsKey(aviso.alertaId()) || avisos.putIfAbsent(aviso.id(), aviso) != null) throw new IllegalArgumentException("Aviso ajeno o repetido");
        }
        if (!porId.keySet().containsAll(sinDestinatario)) throw new IllegalArgumentException("Metadato de señal ajena al ámbito");
        return new RegistroAlertas(porId, avisos, sinDestinatario);
    }
    /** Estructura interna para persistencia; usar consultar/pendientes para vistas autorizadas. */
    public List<Alerta> alertas() { return alertas.values().stream().sorted(Comparator.comparing(a -> a.id().toString())).toList(); }
    public List<Notificacion> notificaciones() { return notificaciones.values().stream().sorted(Comparator.comparing(n -> n.id().toString())).toList(); }
    /** Solo metadato operativo, sin IDs de hechos, relatos ni participantes. */
    public boolean requiereDesignacion() { return !sinDestinatario.isEmpty(); }

    public RegistroAlertas evaluarHecho(Hecho hecho, ContextoAcceso contexto, PoliticaAcceso politica, RegistroCambio cambio) {
        validarContexto(Set.of(hecho.id()), contexto);
        exigirRegistrador(politica, cambio);
        politica.restriccionesActualizadas(contexto, cambio.fecha());
        var motivos = new HashSet<MotivoAlerta>();
        var clasificaciones = new LinkedHashMap<UUID, ClasificacionCategoria>();
        if (!hecho.anulado()) {
            if (hecho.clasificacionVigente().isPresent()) {
                var clasificacion = hecho.clasificacionVigente().orElseThrow();
                clasificaciones.put(hecho.id(), clasificacion);
                if (clasificacion.requiereAvisoInmediato()) motivos.add(MotivoAlerta.GRAVEDAD);
            } else if (hecho.reportes().stream().filter(r -> !r.anulado()).anyMatch(r -> r.datos().clasificacion().requiereAvisoInmediato())) {
                motivos.add(MotivoAlerta.GRAVEDAD);
                hecho.reportes().stream().filter(r -> !r.anulado()).forEach(r -> clasificaciones.put(r.id(), r.datos().clasificacion()));
            }
            if (hecho.reportes().stream().filter(r -> !r.anulado()).anyMatch(r -> r.datos().solicitaAtencion())) motivos.add(MotivoAlerta.RIESGO_COMUNICADO);
        }
        var fundamento = new FundamentoAlerta("hecho:" + hecho.id(), motivos.isEmpty() ? Set.of() : Set.of(hecho.id()),
                motivos, clasificaciones, Optional.empty(), cambio.fecha());
        var alias = hecho.reportes().stream().map(ReporteIncidencia::id).collect(Collectors.toCollection(HashSet::new));
        alias.add(hecho.id());
        return registrar(fundamento, alias, contexto, politica, cambio, Optional.of(hecho.id()));
    }

    public RegistroAlertas evaluarReiteracion(UUID estudianteId, Categoria categoria, ParametrosReiteracion parametros,
                                             ContextoAcceso contexto, PoliticaAcceso politica, RegistroCambio cambio) {
        exigirRegistrador(politica, cambio);
        politica.restriccionesActualizadas(contexto, cambio.fecha());
        var evaluacion = CalculadoraReiteracion.evaluar(estudianteId, categoria, parametros,
                cambio.fecha().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate(), contexto.hechos());
        return registrarReiteracion(estudianteId, categoria.id(), evaluacion, contexto, politica, cambio);
    }

    private RegistroAlertas registrarReiteracion(UUID estudianteId, UUID categoriaId, CalculadoraReiteracion.Evaluacion evaluacion,
                                                 ContextoAcceso contexto, PoliticaAcceso politica, RegistroCambio cambio) {
        var fundamentoReiteracion = evaluacion.fundamento();
        boolean alcanza = evaluacion.alcanzaUmbral();
        var hechos = alcanza ? fundamentoReiteracion.orElseThrow().atribuciones().stream()
                .map(CalculadoraReiteracion.Atribucion::hechoId).collect(Collectors.toUnmodifiableSet()) : Set.<UUID>of();
        var clasificaciones = new LinkedHashMap<UUID, ClasificacionCategoria>();
        contexto.hechos().stream().filter(h -> hechos.contains(h.id())).forEach(h -> h.clasificacionVigente().ifPresent(c -> clasificaciones.put(h.id(), c)));
        var fundamento = new FundamentoAlerta("reiteracion:" + estudianteId + ":" + categoriaId, hechos,
                alcanza ? Set.of(MotivoAlerta.REITERACION) : Set.of(), clasificaciones, fundamentoReiteracion, cambio.fecha());
        return registrar(fundamento, hechos, contexto, politica, cambio, Optional.empty());
    }

    public RegistroAlertas relacionarDuplicados(Hecho principal, UUID origenId, ContextoAcceso contexto,
                                                PoliticaAcceso politica, RegistroCambio cambio) {
        PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), Permiso.RELACIONAR_REPORTES, contexto, cambio.fecha()));
        var resultado = this;
        for (var alerta : alertas()) if (alerta.pendiente() && alerta.hechosVigentes().equals(Set.of(origenId)))
            resultado = resultado.disponer(alerta.id(), new DisposicionAlerta(DisposicionAlerta.Tipo.DUPLICIDAD,
                    "Reporte relacionado con el hecho principal", Optional.of(principal.id()), cambio), contexto, politica);
        resultado = resultado.evaluarHecho(principal, contexto, politica, cambio);
        return resultado.recalcularReiteraciones(Set.of(principal.id(), origenId), contexto, politica, cambio);
    }

    public RegistroAlertas corregirDuplicidad(Hecho principal, Hecho separado, ContextoAcceso contexto,
                                               PoliticaAcceso politica, RegistroCambio cambio) {
        cambio.exigirMotivo();
        PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), Permiso.RELACIONAR_REPORTES, contexto, cambio.fecha()));
        var nuevas = new LinkedHashMap<>(alertas);
        for (var alerta : alertas()) if (alerta.hechosHistoricos().contains(separado.id()) && alerta.disposicion()
                .filter(d -> d.tipo() == DisposicionAlerta.Tipo.DUPLICIDAD && d.referenciaId().filter(principal.id()::equals).isPresent()).isPresent())
            nuevas.put(alerta.id(), alerta.rectificarDuplicidad(cambio));
        var resultado = new RegistroAlertas(nuevas, notificaciones, sinDestinatario)
                .evaluarHecho(separado, contexto, politica, cambio).evaluarHecho(principal, contexto, politica, cambio);
        return resultado.recalcularReiteraciones(Set.of(principal.id(), separado.id()), contexto, politica, cambio);
    }

    private RegistroAlertas recalcularReiteraciones(Set<UUID> afectados, ContextoAcceso contexto, PoliticaAcceso politica, RegistroCambio cambio) {
        var resultado = this;
        var fuentes = new LinkedHashMap<String, FundamentoAlerta>();
        for (var alerta : alertas()) if (alerta.hechosHistoricos().stream().anyMatch(afectados::contains))
            for (var fuente : alerta.fundamentosVigentes().values()) if (fuente.reiteracion().isPresent())
                fuentes.merge(fuente.origen(), fuente, (a, b) -> b.evaluadoEn().isBefore(a.evaluadoEn()) ? a : b);
        for (var fuente : fuentes.values()) {
            var anterior = fuente.reiteracion().get();
            var configuracion = new ParametrosReiteracion.EstadoReiteracion(true, java.util.OptionalInt.of(anterior.umbral()), java.util.OptionalInt.of(anterior.ventanaDias()));
            var nueva = CalculadoraReiteracion.evaluar(anterior.estudianteId(), anterior.categoriaId(), configuracion,
                    cambio.fecha().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate(), contexto.hechos());
            resultado = resultado.registrarReiteracion(anterior.estudianteId(), anterior.categoriaId(), nueva, contexto, politica, cambio);
        }
        return resultado;
    }

    private RegistroAlertas registrar(FundamentoAlerta fundamento, Set<UUID> coincidencias, ContextoAcceso contexto,
                                      PoliticaAcceso politica, RegistroCambio cambio, Optional<UUID> hechoPrincipal) {
        var nuevas = new LinkedHashMap<>(alertas);
        var gestionados = alertas().stream().filter(a -> a.disposicion().filter(d -> d.tipo() != DisposicionAlerta.Tipo.DUPLICIDAD).isPresent())
                .flatMap(a -> a.hechosGestionados().stream()).collect(Collectors.toSet());
        gestionados = normalizarReferencias(gestionados, contexto);
        var candidatas = alertas().stream().filter(a -> a.disposicion().isEmpty())
                .filter(a -> a.fundamentosVigentes().containsKey(fundamento.origen())
                        || a.hechosVigentes().stream().anyMatch(coincidencias::contains))
                .sorted(Comparator.comparing(Alerta::creadaEn).thenComparing(a -> a.id().toString())).toList();
        Alerta destino;
        if (!candidatas.isEmpty()) {
            destino = candidatas.getFirst();
            validarContexto(destino.hechosHistoricos(), contexto);
            for (int i = 1; i < candidatas.size(); i++) {
                var otra = candidatas.get(i);
                validarContexto(otra.hechosHistoricos(), contexto);
                destino = destino.absorber(otra, cambio);
                nuevas.put(otra.id(), otra.gestionar(new DisposicionAlerta(DisposicionAlerta.Tipo.DUPLICIDAD,
                        "Motivos relacionados en un mismo asunto", Optional.of(destino.id()), cambio)));
            }
            destino = destino.registrar(fundamento, cambio);
        } else {
            var anteriores = alertas().stream().filter(a -> a.fundamentosVigentes().containsKey(fundamento.origen())
                    || hechoPrincipal.isPresent() && a.hechosHistoricos().stream().anyMatch(coincidencias::contains)).toList();
            var conocidos = new HashSet<>(gestionados);
            anteriores.stream().flatMap(a -> a.hechosHistoricos().stream()).forEach(conocidos::add);
            var ultima = anteriores.stream().max(Comparator.comparing(Alerta::creadaEn).thenComparing(a -> a.id().toString()));
            if (ultima.isPresent()) {
                validarContexto(ultima.get().hechosHistoricos(), contexto);
                var actualizada = ultima.get().registrar(fundamento, cambio);
                if (hechoPrincipal.isPresent()) actualizada = retirarFuentesAbsorbidas(actualizada, fundamento.origen(), coincidencias, cambio);
                nuevas.put(actualizada.id(), actualizada);
            }
            if (!fundamento.vigente() || conocidos.containsAll(fundamento.hechosIds()))
                return new RegistroAlertas(nuevas, notificaciones, sinDestinatario);
            UUID id = uuid("alerta:" + fundamento.origen() + ":" + anteriores.size());
            destino = Alerta.crear(id, fundamento, cambio, conocidos);
        }
        destino = destino.conHechosYaGestionados(gestionados);
        if (hechoPrincipal.isPresent()) destino = retirarFuentesAbsorbidas(destino, fundamento.origen(), coincidencias, cambio);
        nuevas.put(destino.id(), destino);
        var avisos = new LinkedHashMap<>(notificaciones);
        var faltantes = new HashSet<>(sinDestinatario);
        candidatas.stream().skip(1).forEach(a -> faltantes.remove(a.id()));
        if (destino.pendiente()) {
            var destinatarios = politica.destinatariosAtencion(contexto, cambio.fecha());
            if (destinatarios.requiereDesignacion()) faltantes.add(destino.id()); else faltantes.remove(destino.id());
            for (var destinatario : destinatarios.usuarios()) {
                var idAviso = uuid("notificacion:" + destino.id() + ":" + destinatario);
                avisos.putIfAbsent(idAviso, new Notificacion(idAviso, destino.id(), destinatario, cambio.fecha(), List.of()));
            }
        } else faltantes.remove(destino.id());
        return new RegistroAlertas(nuevas, avisos, faltantes);
    }

    private static Set<UUID> normalizarReferencias(Set<UUID> referencias, ContextoAcceso contexto) {
        var normalizadas = new HashSet<>(referencias);
        contexto.hechos().stream().filter(h -> referencias.contains(h.id()) || h.reportes().stream().anyMatch(r -> referencias.contains(r.id())))
                .forEach(h -> normalizadas.add(h.id()));
        return normalizadas;
    }
    private static Alerta retirarFuentesAbsorbidas(Alerta alerta, String origenActual, Set<UUID> coincidencias, RegistroCambio cambio) {
        var actualizada = alerta;
        for (var fuente : alerta.fundamentosVigentes().values())
            if (fuente.origen().startsWith("hecho:") && !fuente.origen().equals(origenActual)
                    && fuente.hechosIds().stream().anyMatch(coincidencias::contains))
                actualizada = actualizada.registrar(new FundamentoAlerta(fuente.origen(), Set.of(), Set.of(), Map.of(), Optional.empty(), cambio.fecha()), cambio);
        return actualizada;
    }

    public Alerta consultar(UUID alertaId, ContextoAcceso contexto, PoliticaAcceso politica, UUID usuarioId, Instant ahora) {
        var alerta = requerirAlerta(alertaId);
        validarContexto(alerta.hechosHistoricos(), contexto);
        if (alerta.ultimaModificacion().isAfter(ahora)) throw new IllegalArgumentException("La señal contiene datos futuros");
        PoliticaAcceso.exigir(politica.evaluar(usuarioId, Permiso.CONSULTAR_HECHO, contexto, ahora));
        return alerta;
    }
    public List<Alerta> pendientes(UUID usuarioId, Map<UUID, ContextoAcceso> contextos, PoliticaAcceso politica, Instant ahora) {
        var resultado = new ArrayList<Alerta>();
        for (var alerta : alertas()) if (alerta.pendiente()) {
            var contexto = Optional.ofNullable(contextos.get(alerta.id())).orElseThrow(() -> new IllegalArgumentException("Falta el contexto completo de una señal"));
            validarContexto(alerta.hechosHistoricos(), contexto);
            if (alerta.ultimaModificacion().isAfter(ahora)) throw new IllegalArgumentException("La señal contiene datos futuros");
            if (politica.evaluar(usuarioId, Permiso.CONSULTAR_HECHO, contexto, ahora).permitida()) resultado.add(alerta);
        }
        return List.copyOf(resultado);
    }
    public RegistroAlertas leerNotificacion(UUID notificacionId, ContextoAcceso contexto, PoliticaAcceso politica,
                                           UUID usuarioId, Instant ahora) {
        var aviso = Optional.ofNullable(notificaciones.get(notificacionId)).orElseThrow(() -> new IllegalArgumentException("El aviso no existe"));
        if (!aviso.destinatarioId().equals(usuarioId)) throw new SecurityException("El aviso corresponde a otro usuario");
        consultar(aviso.alertaId(), contexto, politica, usuarioId, ahora);
        var nuevas = new LinkedHashMap<>(notificaciones);
        nuevas.put(aviso.id(), aviso.leer(ahora));
        return new RegistroAlertas(alertas, nuevas, sinDestinatario);
    }
    public List<Notificacion> notificacionesConsultables(UUID usuarioId, Map<UUID, ContextoAcceso> contextos,
                                                         PoliticaAcceso politica, Instant ahora) {
        var autorizadas = pendientes(usuarioId, contextos, politica, ahora).stream().map(Alerta::id).collect(Collectors.toSet());
        return notificaciones().stream().filter(n -> n.destinatarioId().equals(usuarioId) && autorizadas.contains(n.alertaId())).toList();
    }
    public RegistroAlertas disponer(UUID alertaId, DisposicionAlerta disposicion, ContextoAcceso contexto, PoliticaAcceso politica) {
        var alerta = consultar(alertaId, contexto, politica, disposicion.registro().autorId(), disposicion.registro().fecha());
        PoliticaAcceso.exigir(politica.evaluar(disposicion.registro().autorId(), Permiso.ATENDER, contexto, disposicion.registro().fecha()));
        var nuevas = new LinkedHashMap<>(alertas);
        nuevas.put(alerta.id(), alerta.gestionar(disposicion));
        var faltantes = new HashSet<>(sinDestinatario);
        faltantes.remove(alerta.id());
        return new RegistroAlertas(nuevas, notificaciones, faltantes);
    }

    private Alerta requerirAlerta(UUID id) { return Optional.ofNullable(alertas.get(id)).orElseThrow(() -> new IllegalArgumentException("La señal no existe")); }
    private static void exigirRegistrador(PoliticaAcceso politica, RegistroCambio cambio) {
        PoliticaAcceso.exigir(politica.evaluar(cambio.autorId(), Permiso.REGISTRAR_INCIDENCIA, cambio.fecha()));
    }
    private static Set<UUID> referencias(ContextoAcceso contexto) {
        var referencias = new HashSet<>(contexto.ambito().hechosIds());
        referencias.addAll(contexto.ambito().reportesIds());
        return referencias;
    }
    private static void validarContexto(Set<UUID> hechosIds, ContextoAcceso contexto) {
        if (!referencias(contexto).containsAll(hechosIds)) throw new IllegalArgumentException("El contexto omite hechos o fuentes de la señal");
    }
    private static UUID uuid(String origen) { return UUID.nameUUIDFromBytes(origen.getBytes(StandardCharsets.UTF_8)); }
}
