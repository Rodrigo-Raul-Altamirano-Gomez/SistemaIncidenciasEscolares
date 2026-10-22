package pe.edu.incidencias.domain.acceso;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Historial de exclusiones expresas. Solo la política del mismo paquete expone mutaciones autorizadas. */
public final class RestriccionesAmbito {
    public enum Operacion { EXCLUIR, RETIRAR }

    public record Exclusion(UUID id, UUID usuarioId, AmbitoReserva ambito, RegistroCambio registro,
                             Optional<RegistroCambio> retiro) {
        public Exclusion {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(usuarioId, "usuarioId");
            Objects.requireNonNull(ambito, "ambito");
            Objects.requireNonNull(registro, "registro").exigirMotivo();
            retiro = Objects.requireNonNull(retiro, "retiro");
            retiro.ifPresent(cambio -> {
                cambio.exigirMotivo();
                if (cambio.autorId().equals(usuarioId))
                    throw new IllegalArgumentException("Ningún usuario puede retirar su propia exclusión");
                if (cambio.fecha().isBefore(registro.fecha()))
                    throw new IllegalArgumentException("El retiro precede a la exclusión");
            });
        }

        public boolean activa() { return retiro.isEmpty(); }
    }

    public record Evento(Operacion operacion, UUID exclusionId, RegistroCambio cambio) { }

    private final List<Exclusion> exclusiones;
    private final List<Evento> historial;

    private RestriccionesAmbito(List<Exclusion> exclusiones, List<Evento> historial) {
        this.exclusiones = List.copyOf(exclusiones);
        this.historial = List.copyOf(historial);
    }

    public static RestriccionesAmbito vacias() { return new RestriccionesAmbito(List.of(), List.of()); }
    public List<Exclusion> exclusiones() { return exclusiones; }
    public List<Evento> historial() { return historial; }

    public boolean estaExcluido(UUID usuarioId, AmbitoReserva ambito) {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(ambito, "ambito");
        return exclusiones.stream().anyMatch(e -> e.activa() && e.usuarioId().equals(usuarioId)
                && e.ambito().seCruzaCon(ambito));
    }

    /* La política comprueba roles vigentes y ausencia de conflicto antes de invocar esta transición. */
    RestriccionesAmbito excluir(UUID exclusionId, UUID usuarioId, AmbitoReserva ambito, RegistroCambio cambio) {
        var nueva = new Exclusion(exclusionId, usuarioId, ambito, cambio, Optional.empty());
        var existente = exclusiones.stream().filter(e -> e.id().equals(exclusionId)).findFirst();
        if (existente.isPresent()) {
            var anterior = existente.get();
            if (anterior.usuarioId().equals(usuarioId) && anterior.ambito().equals(ambito)
                    && anterior.registro().equals(cambio)) return this;
            throw new IllegalArgumentException("El identificador ya corresponde a otra exclusión");
        }
        validarFecha(cambio.fecha());
        var nuevas = new ArrayList<>(exclusiones);
        nuevas.add(nueva);
        var eventos = new ArrayList<>(historial);
        eventos.add(new Evento(Operacion.EXCLUIR, exclusionId, cambio));
        return new RestriccionesAmbito(nuevas, eventos);
    }

    /* Retirar una fuente de restricción no retira ninguna otra exclusión del mismo usuario. */
    RestriccionesAmbito retirar(UUID exclusionId, RegistroCambio cambio) {
        Objects.requireNonNull(exclusionId, "exclusionId");
        Objects.requireNonNull(cambio, "cambio").exigirMotivo();
        var anterior = exclusiones.stream().filter(e -> e.id().equals(exclusionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La exclusión no existe"));
        if (anterior.usuarioId().equals(cambio.autorId()))
            throw new IllegalArgumentException("Ningún usuario puede retirar su propia exclusión");
        if (anterior.retiro().isPresent()) {
            if (anterior.retiro().get().equals(cambio)) return this;
            throw new IllegalStateException("La exclusión ya fue retirada");
        }
        validarFecha(cambio.fecha());
        var retirada = new Exclusion(anterior.id(), anterior.usuarioId(), anterior.ambito(), anterior.registro(), Optional.of(cambio));
        var nuevas = exclusiones.stream().map(e -> e.id().equals(exclusionId) ? retirada : e).toList();
        var eventos = new ArrayList<>(historial);
        eventos.add(new Evento(Operacion.RETIRAR, exclusionId, cambio));
        return new RestriccionesAmbito(nuevas, eventos);
    }

    private void validarFecha(Instant fecha) {
        if (!historial.isEmpty() && fecha.isBefore(historial.getLast().cambio().fecha()))
            throw new IllegalArgumentException("El cambio precede a la última modificación de restricciones");
    }
}
