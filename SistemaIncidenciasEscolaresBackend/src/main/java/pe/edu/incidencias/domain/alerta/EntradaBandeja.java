package pe.edu.incidencias.domain.alerta;

import pe.edu.incidencias.domain.catalogo.Gravedad;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Proyección autorizada de un asunto: conserva las tareas reales sin convertir cada motivo en otra atención. */
public record EntradaBandeja(UUID asuntoId, Optional<UUID> casoId, Set<UUID> alertasIds,
                              Set<MotivoAlerta> motivos, Set<Gravedad> gravedadesDeclaradas,
                              Set<UUID> hechosIds, Instant desde, Optional<UUID> responsableId,
                              Optional<LocalDate> vencimiento, boolean atencionPendiente,
                              Set<UUID> actuacionesPrevistas, Set<UUID> compromisosPendientes,
                              Set<UUID> derivacionesPendientes) {
    public EntradaBandeja {
        Objects.requireNonNull(asuntoId, "asuntoId");
        casoId = Objects.requireNonNull(casoId, "casoId");
        alertasIds = Set.copyOf(alertasIds);
        motivos = Set.copyOf(motivos);
        gravedadesDeclaradas = Set.copyOf(gravedadesDeclaradas);
        hechosIds = Set.copyOf(hechosIds);
        Objects.requireNonNull(desde, "desde");
        responsableId = Objects.requireNonNull(responsableId, "responsableId");
        vencimiento = Objects.requireNonNull(vencimiento, "vencimiento");
        actuacionesPrevistas = Set.copyOf(actuacionesPrevistas);
        compromisosPendientes = Set.copyOf(compromisosPendientes);
        derivacionesPendientes = Set.copyOf(derivacionesPendientes);
        if (alertasIds.isEmpty() && !atencionPendiente && actuacionesPrevistas.isEmpty()
                && compromisosPendientes.isEmpty() && derivacionesPendientes.isEmpty())
            throw new IllegalArgumentException("La bandeja solo contiene trabajo realmente pendiente");
    }
    public boolean situacionNueva() { return !alertasIds.isEmpty(); }
    public boolean seguimientoPendiente() {
        return !actuacionesPrevistas.isEmpty() || !compromisosPendientes.isEmpty() || !derivacionesPendientes.isEmpty();
    }
}
