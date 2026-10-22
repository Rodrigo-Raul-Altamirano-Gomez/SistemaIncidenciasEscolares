package pe.edu.incidencias.domain.documento;

import pe.edu.incidencias.domain.acceso.AmbitoReserva;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.incidencia.ReporteIncidencia;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Constancia declarada de origen/instrumento; nunca acredita envío, registro nuevo ni cierre externo. */
public record ReferenciaOficial(UUID id, AmbitoReserva ambito, InstrumentoOficial instrumento,
                                String origenComunicado, Optional<String> codigoOAsiento,
                                Optional<LocalDate> fechaRegistroExterno, Optional<String> protocoloYVersion,
                                Optional<String> observacion, RegistroCambio registro) {
    public ReferenciaOficial {
        Objects.requireNonNull(id);
        Objects.requireNonNull(ambito);
        Objects.requireNonNull(instrumento);
        if (origenComunicado == null || origenComunicado.isBlank()) throw new IllegalArgumentException("Se conserva el origen comunicado");
        codigoOAsiento = textoOpcional(codigoOAsiento);
        protocoloYVersion = textoOpcional(protocoloYVersion);
        observacion = textoOpcional(observacion);
        Objects.requireNonNull(fechaRegistroExterno);
        Objects.requireNonNull(registro);
        if (fechaRegistroExterno.filter(f -> f.isAfter(registro.fecha().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate())).isPresent())
            throw new IllegalArgumentException("El registro externo efectivo no puede ser futuro");
    }
    private static Optional<String> textoOpcional(Optional<String> valor) {
        Objects.requireNonNull(valor);
        if (valor.filter(String::isBlank).isPresent()) throw new IllegalArgumentException("Dato opcional vacío");
        return valor;
    }
}
