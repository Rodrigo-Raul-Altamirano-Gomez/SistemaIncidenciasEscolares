package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.alerta.RegistroAlertas;
import pe.edu.incidencias.domain.alerta.DisposicionAlerta;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Resultado conjunto de atención y avisos relacionados. El llamador debe conservar ambos
 * estados en la misma operación; esta coordinación de dominio no realiza persistencia.
 */
public final class SeguimientoInstitucional {
    private final RegistroAtenciones atenciones;
    private final RegistroAlertas alertas;

    private SeguimientoInstitucional(RegistroAtenciones atenciones, RegistroAlertas alertas) {
        this.atenciones = atenciones;
        this.alertas = alertas;
    }

    public static SeguimientoInstitucional actualizar(RegistroAtenciones atencionesActualizadas,
                                                       RegistroAlertas alertasAnteriores,
                                                       DirectorioUsuarios directorio,
                                                       RegistroCambio cambio) {
        var hechos = alertasAnteriores.alertas().stream().filter(a -> a.pendiente()).flatMap(a -> a.hechosHistoricos().stream()).collect(Collectors.toSet());
        return actualizar(atencionesActualizadas, alertasAnteriores, hechos, directorio, cambio);
    }

    /** Una operación pequeña solo coordina señales de los hechos modificados de su ámbito. */
    public static SeguimientoInstitucional actualizar(RegistroAtenciones atencionesActualizadas, RegistroAlertas alertasAnteriores,
                                                       Set<UUID> hechosModificados, DirectorioUsuarios directorio, RegistroCambio cambio) {
        Objects.requireNonNull(atencionesActualizadas, "atencionesActualizadas");
        Objects.requireNonNull(alertasAnteriores, "alertasAnteriores");
        Objects.requireNonNull(directorio, "directorio");
        Objects.requireNonNull(cambio, "cambio");
        var modificados = hechosModificados.stream().map(atencionesActualizadas::hechoCanonico).collect(Collectors.toSet());
        var resultado = alertasAnteriores;
        for (var alerta : alertasAnteriores.alertas()) if (alerta.pendiente()
                && alerta.hechosHistoricos().stream().map(atencionesActualizadas::hechoCanonico).anyMatch(modificados::contains)) {
            if (alerta.ultimaModificacion().isAfter(cambio.fecha())) throw new IllegalArgumentException("La señal pertinente contiene cambios futuros");
            var contexto = atencionesActualizadas.contextoDeHechos(alerta.hechosHistoricos());
            var activadores = alerta.hechosActivadores().stream().map(atencionesActualizadas::hechoCanonico).collect(Collectors.toSet());
            var pertinentes = atencionesActualizadas.casos().stream().filter(c -> c.hechosReservados().stream()
                    .map(atencionesActualizadas::hechoCanonico).anyMatch(activadores::contains)).toList();
            if (pertinentes.stream().anyMatch(c -> c.ultimaModificacion().isAfter(cambio.fecha())))
                throw new IllegalArgumentException("La atención pertinente contiene cambios posteriores a la coordinación");
            var gestion = pertinentes.stream().filter(c -> c.gestionPertinente(activadores, alerta.pendienteDesde(), atencionesActualizadas::hechoCanonico))
                    .max(Comparator.comparing(CasoAtencion::ultimaModificacion));
            if (gestion.isEmpty()) continue;
            var caso = gestion.get();
            contexto = atencionesActualizadas.contextoDocumento(caso.id(), contexto.ambito());
            var politica = new PoliticaAcceso(directorio, atencionesActualizadas.restricciones(), atencionesActualizadas.proteccion());
            String disposicion = caso.cierre().map(CierreLocal::resultado).orElse("Atención documentada en el caso " + caso.id());
            resultado = resultado.disponer(alerta.id(), new DisposicionAlerta(DisposicionAlerta.Tipo.ATENCION,
                    disposicion, Optional.of(caso.id()), cambio), contexto, politica);
        }
        return new SeguimientoInstitucional(atencionesActualizadas, resultado);
    }

    public static SeguimientoInstitucional relacionarDuplicados(RegistroAtenciones atenciones, RegistroAlertas alertas,
                                                                  UUID principal, UUID origen, Optional<UUID> casoContinuidad,
                                                                  DirectorioUsuarios directorio, RegistroCambio cambio) {
        var nuevas = atenciones.relacionarDuplicados(principal, origen, casoContinuidad, directorio, cambio);
        var fuentes = new java.util.HashSet<UUID>(Set.of(principal, origen));
        alertas.alertas().stream().filter(a -> a.hechosHistoricos().stream().anyMatch(id -> id.equals(principal) || id.equals(origen)))
                .forEach(a -> fuentes.addAll(a.hechosHistoricos()));
        var contexto = nuevas.contextoDeHechos(fuentes);
        var politica = new PoliticaAcceso(directorio, nuevas.restricciones(), nuevas.proteccion());
        var ajustadas = alertas.relacionarDuplicados(nuevas.hechoVigente(principal), origen, contexto, politica, cambio);
        return actualizar(nuevas, ajustadas, Set.of(principal), directorio, cambio);
    }

    public static SeguimientoInstitucional separarDuplicado(RegistroAtenciones atenciones, RegistroAlertas alertas,
                                                              UUID origen, Optional<UUID> casoDelSeparado,
                                                              DirectorioUsuarios directorio, RegistroCambio cambio) {
        var principal = atenciones.hechoCanonico(origen);
        var nuevas = atenciones.separarDuplicado(origen, casoDelSeparado, directorio, cambio);
        var fuentes = new java.util.HashSet<UUID>(Set.of(principal, origen));
        alertas.alertas().stream().filter(a -> a.hechosHistoricos().stream().anyMatch(id -> id.equals(principal) || id.equals(origen)))
                .forEach(a -> fuentes.addAll(a.hechosHistoricos()));
        var contexto = nuevas.contextoDeHechos(fuentes);
        var ajustadas = alertas.corregirDuplicidad(nuevas.hechoVigente(principal), nuevas.hechoVigente(origen), contexto,
                new PoliticaAcceso(directorio, nuevas.restricciones(), nuevas.proteccion()), cambio);
        return actualizar(nuevas, ajustadas, Set.of(principal, origen), directorio, cambio);
    }

    public RegistroAtenciones atenciones() { return atenciones; }
    public RegistroAlertas alertas() { return alertas; }
}
