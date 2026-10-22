package pe.edu.incidencias.domain.acceso;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** La vigencia aprobada original se conserva aunque se registre después su término anticipado. */
public record DesignacionRol(UUID id, RolUsuario rol, Vigencia vigencia, RegistroCambio otorgamiento,
                             Optional<Cese> cese) {
    public record Cese(Instant finEfectivo, RegistroCambio cambio) {
        public Cese {
            Objects.requireNonNull(finEfectivo, "finEfectivo");
            Objects.requireNonNull(cambio, "cambio");
            if (finEfectivo.isBefore(cambio.fecha()))
                throw new IllegalArgumentException("El término operativo no puede aplicarse retroactivamente");
        }
    }

    public DesignacionRol {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(vigencia, "vigencia");
        Objects.requireNonNull(otorgamiento, "otorgamiento");
        Objects.requireNonNull(cese, "cese");
        if (cese.filter(fin -> fin.cambio().fecha().isBefore(otorgamiento.fecha())).isPresent())
            throw new IllegalArgumentException("El cese no puede preceder al registro de la designación");
    }

    public boolean vigenteEn(Instant instante) {
        return vigencia.contiene(instante) && cese.map(fin -> instante.isBefore(fin.finEfectivo())).orElse(true);
    }

    DesignacionRol finalizar(Instant fin, RegistroCambio cambio) {
        if (cese.isPresent()) throw new IllegalStateException("La designación ya tiene término registrado");
        return new DesignacionRol(id, rol, vigencia, otorgamiento, Optional.of(new Cese(fin, cambio)));
    }
}
