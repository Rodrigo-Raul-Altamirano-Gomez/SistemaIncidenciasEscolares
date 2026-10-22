package pe.edu.incidencias.domain.acceso;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Registro institucional inmutable. El mantenimiento rutinario valida al administrador en cada operación. */
public final class DirectorioUsuarios {
    public enum Operacion { INICIALIZACION, ALTA, ASIGNACION, FIN_DESIGNACION, BAJA, REACTIVACION }
    public record Movimiento(Operacion operacion, UUID usuarioId, Optional<UUID> designacionId, RegistroCambio cambio) { }

    private final Map<UUID, Usuario> usuarios;
    private final List<Movimiento> movimientos;

    private DirectorioUsuarios(Map<UUID, Usuario> usuarios, List<Movimiento> movimientos) {
        this.usuarios = Map.copyOf(usuarios);
        this.movimientos = List.copyOf(movimientos);
    }

    /** Frontera inicial de aprovisionamiento autorizado; no debe exponerse como autoalta de la aplicación. */
    public static DirectorioUsuarios inicializar(UUID administradorId, String nombre, String identificador,
                                                RegistroCambio respaldoInstitucional) {
        Objects.requireNonNull(respaldoInstitucional, "respaldoInstitucional").exigirMotivo();
        var designacion = new DesignacionRol(administradorId, RolUsuario.ADMINISTRADOR_OPERATIVO,
                new Vigencia(respaldoInstitucional.fecha(), Optional.empty()), respaldoInstitucional, Optional.empty());
        var cuenta = new Usuario(administradorId, nombre, identificador, List.of(designacion),
                List.of(new Usuario.EstadoCuenta(true, respaldoInstitucional)));
        return new DirectorioUsuarios(Map.of(administradorId, cuenta), List.of(new Movimiento(
                Operacion.INICIALIZACION, administradorId, Optional.of(designacion.id()), respaldoInstitucional)));
    }

    public Optional<Usuario> usuario(UUID id) { return Optional.ofNullable(usuarios.get(Objects.requireNonNull(id, "id"))); }
    public List<Usuario> usuarios() {
        return usuarios.values().stream().sorted(Comparator.comparing(Usuario::identificadorAcceso)).toList();
    }
    public List<Movimiento> movimientos() { return movimientos; }

    public DirectorioUsuarios registrarUsuario(UUID id, String nombre, String identificador, RegistroCambio cambio) {
        validarAdministrador(cambio);
        if (usuarios.containsKey(id)) throw new IllegalArgumentException("La identidad de usuario ya está registrada");
        var nuevo = new Usuario(id, nombre, identificador, List.of(), List.of(new Usuario.EstadoCuenta(true, cambio)));
        if (usuarios.values().stream().anyMatch(cuenta -> normalizar(cuenta.identificadorAcceso()).equals(normalizar(nuevo.identificadorAcceso()))))
            throw new IllegalArgumentException("El identificador de acceso ya pertenece a otra cuenta");
        return guardar(nuevo, Operacion.ALTA, Optional.empty(), cambio);
    }

    public DirectorioUsuarios asignarRol(UUID usuarioId, UUID designacionId, RolUsuario rol,
                                        Vigencia vigencia, RegistroCambio cambio) {
        validarAdministrador(cambio);
        if (usuarioId.equals(cambio.autorId())) throw new SecurityException("Nadie puede atribuirse sus propios roles");
        var destinatario = requerirUsuario(usuarioId);
        if (usuarios.values().stream().flatMap(cuenta -> cuenta.designaciones().stream())
                .anyMatch(designacion -> designacion.id().equals(designacionId)))
            throw new IllegalArgumentException("El identificador de designación ya está registrado");
        var designacion = new DesignacionRol(designacionId, rol, vigencia, cambio, Optional.empty());
        return guardar(destinatario.asignar(designacion), Operacion.ASIGNACION, Optional.of(designacionId), cambio);
    }

    public DirectorioUsuarios finalizarDesignacion(UUID usuarioId, UUID designacionId, Instant fin,
                                                   RegistroCambio cambio) {
        validarAdministrador(cambio);
        var cuenta = requerirUsuario(usuarioId).finalizar(designacionId, fin, cambio);
        return guardar(cuenta, Operacion.FIN_DESIGNACION, Optional.of(designacionId), cambio);
    }

    public DirectorioUsuarios desactivarUsuario(UUID usuarioId, RegistroCambio cambio) {
        return cambiarEstado(usuarioId, false, cambio);
    }

    public DirectorioUsuarios activarUsuario(UUID usuarioId, RegistroCambio cambio) {
        return cambiarEstado(usuarioId, true, cambio);
    }

    private DirectorioUsuarios cambiarEstado(UUID usuarioId, boolean activa, RegistroCambio cambio) {
        validarAdministrador(cambio);
        var actual = requerirUsuario(usuarioId);
        var nueva = actual.cambiarEstado(activa, cambio);
        return nueva == actual ? this : guardar(nueva, activa ? Operacion.REACTIVACION : Operacion.BAJA,
                Optional.empty(), cambio);
    }

    private void validarAdministrador(RegistroCambio cambio) {
        Objects.requireNonNull(cambio, "cambio");
        if (cambio.fecha().isBefore(movimientos.getLast().cambio().fecha()))
            throw new IllegalArgumentException("El cambio precede al estado vigente del directorio");
        if (!requerirUsuario(cambio.autorId()).rolesVigentes(cambio.fecha()).contains(RolUsuario.ADMINISTRADOR_OPERATIVO))
            throw new SecurityException("Se requiere administrador operativo vigente y cuenta activa");
    }

    private Usuario requerirUsuario(UUID usuarioId) {
        return usuario(usuarioId).orElseThrow(() -> new IllegalArgumentException("La cuenta no está registrada"));
    }

    private DirectorioUsuarios guardar(Usuario cuenta, Operacion operacion, Optional<UUID> designacionId, RegistroCambio cambio) {
        var nuevas = new LinkedHashMap<>(usuarios);
        nuevas.put(cuenta.id(), cuenta);
        var historia = new ArrayList<>(movimientos);
        historia.add(new Movimiento(operacion, cuenta.id(), designacionId, cambio));
        return new DirectorioUsuarios(nuevas, historia);
    }

    private static String normalizar(String identificador) { return identificador.strip().toLowerCase(Locale.ROOT); }
}
