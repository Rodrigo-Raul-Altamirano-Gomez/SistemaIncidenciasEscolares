package pe.edu.incidencias.domain.acceso;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Cuenta individual sin credenciales. Su mantenimiento solo se expone por DirectorioUsuarios. */
public final class Usuario {
    public record EstadoCuenta(boolean activa, RegistroCambio cambio) {
        public EstadoCuenta { Objects.requireNonNull(cambio, "cambio"); }
    }

    private final UUID id;
    private final String nombreLaboral;
    private final String identificadorAcceso;
    private final List<DesignacionRol> designaciones;
    private final List<EstadoCuenta> estados;

    Usuario(UUID id, String nombreLaboral, String identificadorAcceso, List<DesignacionRol> designaciones,
            List<EstadoCuenta> estados) {
        this.id = Objects.requireNonNull(id, "id");
        this.nombreLaboral = textoObligatorio(nombreLaboral, "nombreLaboral");
        this.identificadorAcceso = textoObligatorio(identificadorAcceso, "identificadorAcceso");
        if (this.identificadorAcceso.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)))
            throw new IllegalArgumentException("El identificador de acceso no admite espacios internos");
        this.designaciones = List.copyOf(designaciones);
        this.estados = List.copyOf(estados);
        if (this.estados.isEmpty()) throw new IllegalArgumentException("La cuenta necesita constancia de alta");
    }

    public UUID id() { return id; }
    public String nombreLaboral() { return nombreLaboral; }
    public String identificadorAcceso() { return identificadorAcceso; }
    public boolean activa() { return estados.getLast().activa(); }
    public List<DesignacionRol> designaciones() { return designaciones; }
    public List<EstadoCuenta> estados() { return estados; }

    public boolean activaEn(Instant instante) {
        Objects.requireNonNull(instante, "instante");
        return estados.stream().filter(estado -> !estado.cambio().fecha().isAfter(instante))
                .reduce((anterior, siguiente) -> siguiente).map(EstadoCuenta::activa).orElse(false);
    }

    public Set<RolUsuario> rolesVigentes(Instant instante) {
        if (!activaEn(instante)) return Set.of();
        return designaciones.stream().filter(designacion -> !designacion.otorgamiento().fecha().isAfter(instante))
                .filter(designacion -> designacion.vigenteEn(instante)).map(DesignacionRol::rol)
                .collect(Collectors.toUnmodifiableSet());
    }

    Usuario asignar(DesignacionRol designacion) {
        var nuevas = new ArrayList<>(designaciones);
        nuevas.add(designacion);
        return new Usuario(id, nombreLaboral, identificadorAcceso, nuevas, estados);
    }

    Usuario finalizar(UUID designacionId, Instant fin, RegistroCambio cambio) {
        if (designaciones.stream().noneMatch(designacion -> designacion.id().equals(designacionId)))
            throw new IllegalArgumentException("La designación no pertenece a esta cuenta");
        var nuevas = designaciones.stream().map(designacion -> designacion.id().equals(designacionId)
                ? designacion.finalizar(fin, cambio) : designacion).toList();
        return new Usuario(id, nombreLaboral, identificadorAcceso, nuevas, estados);
    }

    Usuario cambiarEstado(boolean activa, RegistroCambio cambio) {
        if (activa == activa()) return this;
        var nuevos = new ArrayList<>(estados);
        nuevos.add(new EstadoCuenta(activa, cambio));
        return new Usuario(id, nombreLaboral, identificadorAcceso, designaciones, nuevos);
    }

    private static String textoObligatorio(String valor, String campo) {
        Objects.requireNonNull(valor, campo);
        if (valor.isBlank()) throw new IllegalArgumentException(campo + " no puede estar vacío");
        return valor.strip();
    }
}
