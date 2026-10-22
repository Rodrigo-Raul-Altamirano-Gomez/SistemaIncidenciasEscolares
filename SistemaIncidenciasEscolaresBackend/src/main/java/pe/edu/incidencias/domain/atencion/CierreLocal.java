package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Conclusión de la atención local; no certifica cierre fiscal, de SíseVe ni de un protocolo oficial. */
public record CierreLocal(LocalDate fecha, String resultado, TipoCierre tipo,
                          RegistroCambio registro, Optional<UUID> derivacionOrigenId) {
    public enum TipoCierre { CONCLUSION, DERIVACION_FAMILIAR_FISCALIA, APERTURA_ERRONEA, DUPLICIDAD }

    public CierreLocal {
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(resultado, "resultado");
        if (resultado.isBlank()) throw new IllegalArgumentException("El cierre necesita un resultado suficiente");
        resultado = resultado.strip();
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(registro, "registro");
        derivacionOrigenId = Objects.requireNonNull(derivacionOrigenId, "derivacionOrigenId");
        if (fecha.isAfter(registro.fecha().atZone(ZoneId.of("America/Lima")).toLocalDate()))
            throw new IllegalArgumentException("La fecha efectiva del cierre no puede ser futura");
        if ((tipo == TipoCierre.DERIVACION_FAMILIAR_FISCALIA) != derivacionOrigenId.isPresent())
            throw new IllegalArgumentException("Solo el cierre por derivación familiar a Fiscalía referencia la derivación de origen");
    }
}
