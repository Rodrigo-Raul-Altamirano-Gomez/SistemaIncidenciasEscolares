package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.acceso.DirectorioUsuarios;
import pe.edu.incidencias.domain.acceso.RolUsuario;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Instantánea de designaciones institucionales, no un comité creado para cada caso. */
public record ComposicionComite(Instant consultadaEn, List<Miembro> miembros) {
    public record Miembro(UUID usuarioId, String nombreLaboral, Set<RolUsuario> roles, Set<UUID> designacionesIds) {
        public Miembro {
            Objects.requireNonNull(usuarioId, "usuarioId");
            Objects.requireNonNull(nombreLaboral, "nombreLaboral");
            roles = Set.copyOf(roles);
            designacionesIds = Set.copyOf(designacionesIds);
        }
    }
    public ComposicionComite {
        Objects.requireNonNull(consultadaEn, "consultadaEn");
        miembros = List.copyOf(miembros);
    }
    public static ComposicionComite vigente(DirectorioUsuarios directorio, Instant instante) {
        Objects.requireNonNull(directorio, "directorio");
        if (directorio.movimientos().getLast().cambio().fecha().isAfter(instante))
            throw new IllegalArgumentException("La consulta precede al directorio vigente");
        var miembros = directorio.usuarios().stream()
                .filter(u -> u.rolesVigentes(instante).contains(RolUsuario.MIEMBRO_COMITE)
                        || u.rolesVigentes(instante).contains(RolUsuario.DIRECTOR))
                .map(u -> new Miembro(u.id(), u.nombreLaboral(), u.rolesVigentes(instante),
                        u.designaciones().stream().filter(d -> d.vigenteEn(instante) && !d.otorgamiento().fecha().isAfter(instante))
                                .filter(d -> d.rol() == RolUsuario.MIEMBRO_COMITE || d.rol() == RolUsuario.DIRECTOR)
                                .map(d -> d.id()).collect(Collectors.toUnmodifiableSet())))
                .toList();
        return new ComposicionComite(instante, miembros);
    }
    public Set<UUID> usuariosIds() { return miembros.stream().map(Miembro::usuarioId).collect(Collectors.toUnmodifiableSet()); }
}
