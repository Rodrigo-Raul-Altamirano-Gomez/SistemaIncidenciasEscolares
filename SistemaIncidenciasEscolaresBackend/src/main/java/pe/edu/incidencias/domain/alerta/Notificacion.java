package pe.edu.incidencias.domain.alerta;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

/** Aviso interno persistible. Una lectura no cambia ni gestiona su alerta. */
public record Notificacion(UUID id, UUID alertaId, UUID destinatarioId, Instant emitidaEn, List<Instant> lecturas) {
    public Notificacion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(alertaId, "alertaId");
        Objects.requireNonNull(destinatarioId, "destinatarioId");
        Objects.requireNonNull(emitidaEn, "emitidaEn");
        lecturas = List.copyOf(lecturas);
        Instant anterior = emitidaEn;
        for (var lectura : lecturas) {
            if (lectura.isBefore(anterior)) throw new IllegalArgumentException("Las lecturas deben conservar su orden");
            anterior = lectura;
        }
    }
    public boolean leida() { return !lecturas.isEmpty(); }
    Notificacion leer(Instant cuando) {
        var ultima = lecturas.isEmpty() ? emitidaEn : lecturas.getLast();
        if (cuando.isBefore(ultima)) throw new IllegalArgumentException("La lectura precede al aviso");
        if (leida()) return this;
        var nuevas = new ArrayList<>(lecturas);
        nuevas.add(cuando);
        return new Notificacion(id, alertaId, destinatarioId, emitidaEn, nuevas);
    }
}
