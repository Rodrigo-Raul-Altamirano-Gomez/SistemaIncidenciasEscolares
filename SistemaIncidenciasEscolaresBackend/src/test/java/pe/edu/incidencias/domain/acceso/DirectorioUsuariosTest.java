package pe.edu.incidencias.domain.acceso;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DirectorioUsuariosTest {
    private static final Instant INICIO = Instant.parse("2026-10-21T15:00:00Z");
    private static final UUID ADMIN = id(1);
    private static final UUID DOCENTE = id(2);
    private static final UUID OTRO = id(3);

    @Test
    void inicializacionConservaRespaldoInstitucionalYSoloCreaAdministradorNominal() {
        var respaldo = new RegistroCambio(id(90), INICIO, Optional.of("Designación institucional inicial ficticia"));
        var directorio = DirectorioUsuarios.inicializar(ADMIN, "Operador inicial ficticio", " Admin.Inicial ", respaldo);
        var administrador = directorio.usuario(ADMIN).orElseThrow();
        assertEquals("Admin.Inicial", administrador.identificadorAcceso());
        assertEquals("Operador inicial ficticio", administrador.nombreLaboral());
        assertEquals(Set.of(RolUsuario.ADMINISTRADOR_OPERATIVO), administrador.rolesVigentes(INICIO));
        assertEquals(respaldo, directorio.movimientos().getFirst().cambio());
        assertEquals(DirectorioUsuarios.Operacion.INICIALIZACION, directorio.movimientos().getFirst().operacion());
        assertTrue(administrador.rolesVigentes(INICIO.minusSeconds(1)).isEmpty());
        assertFalse(administrador.activaEn(INICIO.minusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> DirectorioUsuarios.inicializar(ADMIN,
                "Operador", "admin", cambio(ADMIN, 0)));
    }

    @Test
    void altaIndividualNoConcedeRolesNiExigeTextoRutinario() {
        var previo = inicial();
        var directorio = previo.registrarUsuario(DOCENTE, "Docente ficticio", "docente", cambio(ADMIN, 1));
        var cuenta = directorio.usuario(DOCENTE).orElseThrow();
        assertTrue(cuenta.activa());
        assertEquals(DOCENTE, cuenta.id());
        assertTrue(cuenta.rolesVigentes(INICIO.plusSeconds(1)).isEmpty());
        assertEquals(cambio(ADMIN, 1), cuenta.estados().getFirst().cambio());
        assertTrue(previo.usuario(DOCENTE).isEmpty());
        assertEquals(2, directorio.usuarios().size());
        assertEquals(DirectorioUsuarios.Operacion.ALTA, directorio.movimientos().getLast().operacion());
    }

    @Test
    void noDuplicaIdentidadNiIdentificadorAunqueCambienMayusculasOExtremos() {
        var directorio = conDocente();
        assertThrows(IllegalArgumentException.class, () -> directorio.registrarUsuario(DOCENTE,
                "Otro nombre ficticio", "otro", cambio(ADMIN, 2)));
        assertThrows(IllegalArgumentException.class, () -> directorio.registrarUsuario(OTRO,
                "Otro ficticio", " DOCENTE ", cambio(ADMIN, 2)));
        assertThrows(IllegalArgumentException.class, () -> directorio.registrarUsuario(OTRO,
                " ", "otro", cambio(ADMIN, 2)));
        assertThrows(IllegalArgumentException.class, () -> directorio.registrarUsuario(OTRO,
                "Otro ficticio", "otro usuario", cambio(ADMIN, 2)));
    }

    @ParameterizedTest
    @EnumSource(RolUsuario.class)
    void asignacionConservaAutorYVigenciaSinInventarOtrosRoles(RolUsuario rol) {
        var hasta = INICIO.plusSeconds(10);
        var cambio = cambio(ADMIN, 2);
        var directorio = conDocente().asignarRol(DOCENTE, id(100), rol,
                new Vigencia(INICIO.plusSeconds(3), Optional.of(hasta)), cambio);
        var cuenta = directorio.usuario(DOCENTE).orElseThrow();
        assertTrue(cuenta.rolesVigentes(INICIO.plusSeconds(2)).isEmpty());
        assertEquals(Set.of(rol), cuenta.rolesVigentes(INICIO.plusSeconds(3)));
        assertEquals(Set.of(rol), cuenta.rolesVigentes(hasta.minusNanos(1)));
        assertTrue(cuenta.rolesVigentes(hasta).isEmpty());
        assertEquals(rol, cuenta.designaciones().getFirst().rol());
        assertEquals(cambio, cuenta.designaciones().getFirst().otorgamiento());
        assertEquals(Optional.empty(), cuenta.designaciones().getFirst().cese());
        assertEquals(Optional.of(id(100)), directorio.movimientos().getLast().designacionId());
    }

    @Test
    void rolesSeAcumulanSobreLaMismaIdentidadYLaMembresiaSeRetiraSinBorrarLosDemas() {
        var directorio = conDocente()
                .asignarRol(DOCENTE, id(100), RolUsuario.DOCENTE, sinFin(1), cambio(ADMIN, 2))
                .asignarRol(DOCENTE, id(101), RolUsuario.MIEMBRO_COMITE, hasta(1, 5), cambio(ADMIN, 3));
        var cuenta = directorio.usuario(DOCENTE).orElseThrow();
        assertEquals(Set.of(RolUsuario.DOCENTE, RolUsuario.MIEMBRO_COMITE), cuenta.rolesVigentes(INICIO.plusSeconds(4)));
        assertEquals(Set.of(RolUsuario.DOCENTE), cuenta.rolesVigentes(INICIO.plusSeconds(5)));
        assertEquals(2, cuenta.designaciones().size());
        assertEquals(DOCENTE, cuenta.id());
    }

    @ParameterizedTest
    @EnumSource(RolUsuario.class)
    void administradorNoPuedeAtribuirseNingunRol(RolUsuario rol) {
        var directorio = inicial();
        assertThrows(SecurityException.class, () -> directorio.asignarRol(ADMIN, id(100), rol,
                sinFin(0), cambio(ADMIN, 1)));
        assertEquals(1, directorio.usuario(ADMIN).orElseThrow().designaciones().size());
    }

    @Test
    void docenteDirectorOToeSinDesignacionAdministrativaNoGestionanCuentas() {
        var directorio = conDocente().asignarRol(DOCENTE, id(100), RolUsuario.DIRECTOR,
                sinFin(0), cambio(ADMIN, 2));
        assertThrows(SecurityException.class, () -> directorio.registrarUsuario(OTRO,
                "Otro ficticio", "otro", cambio(DOCENTE, 3)));
        assertThrows(SecurityException.class, () -> directorio.asignarRol(OTRO, id(101), RolUsuario.TOE,
                sinFin(0), cambio(DOCENTE, 3)));
        assertThrows(SecurityException.class, () -> directorio.desactivarUsuario(ADMIN, cambio(DOCENTE, 3)));
        assertThrows(IllegalArgumentException.class, () -> directorio.registrarUsuario(OTRO,
                "Otro ficticio", "otro", cambio(id(99), 3)));
    }

    @Test
    void administradorDesignadoPuedeActuarSoloMientrasSuDesignacionEsteVigente() {
        var directorio = conDocente().asignarRol(DOCENTE, id(100), RolUsuario.ADMINISTRADOR_OPERATIVO,
                hasta(2, 5), cambio(ADMIN, 2));
        var conOtraCuenta = directorio.registrarUsuario(OTRO, "Otro ficticio", "otro", cambio(DOCENTE, 4));
        assertTrue(conOtraCuenta.usuario(OTRO).isPresent());
        assertEquals(DOCENTE, conOtraCuenta.movimientos().getLast().cambio().autorId());
        assertThrows(SecurityException.class, () -> conOtraCuenta.desactivarUsuario(OTRO, cambio(DOCENTE, 5)));
    }

    @Test
    void bajaRetiraTodosLosRolesEnLaSiguienteOperacionYConservaLaHistoria() {
        var directorio = conDocente()
                .asignarRol(DOCENTE, id(100), RolUsuario.DOCENTE, sinFin(0), cambio(ADMIN, 2))
                .asignarRol(DOCENTE, id(101), RolUsuario.MIEMBRO_COMITE, sinFin(0), cambio(ADMIN, 3));
        var anterior = directorio.usuario(DOCENTE).orElseThrow();
        var dadoDeBaja = directorio.desactivarUsuario(DOCENTE, cambio(ADMIN, 4));
        var cuenta = dadoDeBaja.usuario(DOCENTE).orElseThrow();
        assertFalse(cuenta.activa());
        assertTrue(cuenta.rolesVigentes(INICIO.plusSeconds(4)).isEmpty());
        assertEquals(Set.of(RolUsuario.DOCENTE, RolUsuario.MIEMBRO_COMITE), cuenta.rolesVigentes(INICIO.plusSeconds(3)));
        assertEquals(anterior.designaciones(), cuenta.designaciones());
        assertEquals(2, cuenta.estados().size());
        assertEquals(cambio(ADMIN, 4), cuenta.estados().getLast().cambio());
        assertTrue(anterior.activa());
    }

    @Test
    void administradorDadoDeBajaNoPuedeContinuarAunqueConserveSuDesignacionHistorica() {
        var directorio = conDocente().asignarRol(DOCENTE, id(100), RolUsuario.ADMINISTRADOR_OPERATIVO,
                sinFin(0), cambio(ADMIN, 2));
        var sinPermiso = directorio.desactivarUsuario(DOCENTE, cambio(ADMIN, 3));
        assertThrows(SecurityException.class, () -> sinPermiso.registrarUsuario(OTRO,
                "Otro ficticio", "otro", cambio(DOCENTE, 4)));
    }

    @Test
    void reactivarNoRestauraMembresiasYaVencidasNiBorraLaBaja() {
        var directorio = conDocente()
                .asignarRol(DOCENTE, id(100), RolUsuario.DOCENTE, sinFin(0), cambio(ADMIN, 2))
                .asignarRol(DOCENTE, id(101), RolUsuario.MIEMBRO_COMITE, hasta(0, 5), cambio(ADMIN, 3))
                .desactivarUsuario(DOCENTE, cambio(ADMIN, 4))
                .activarUsuario(DOCENTE, cambio(ADMIN, 6));
        var cuenta = directorio.usuario(DOCENTE).orElseThrow();
        assertTrue(cuenta.activa());
        assertEquals(Set.of(RolUsuario.DOCENTE), cuenta.rolesVigentes(INICIO.plusSeconds(6)));
        assertEquals(3, cuenta.estados().size());
        assertFalse(cuenta.activaEn(INICIO.plusSeconds(5)));
    }

    @Test
    void ceseActualRetiraLaDesignacionSinModificarLaVigenciaAprobadaOriginal() {
        var vigencia = sinFin(0);
        var previo = conDocente().asignarRol(DOCENTE, id(100), RolUsuario.MIEMBRO_COMITE,
                vigencia, cambio(ADMIN, 2));
        var directorio = previo.finalizarDesignacion(DOCENTE, id(100), INICIO.plusSeconds(3), cambio(ADMIN, 3));
        var designacion = directorio.usuario(DOCENTE).orElseThrow().designaciones().getFirst();
        assertTrue(directorio.usuario(DOCENTE).orElseThrow().rolesVigentes(INICIO.plusSeconds(3)).isEmpty());
        assertEquals(Set.of(RolUsuario.MIEMBRO_COMITE), directorio.usuario(DOCENTE).orElseThrow().rolesVigentes(INICIO.plusSeconds(2)));
        assertEquals(vigencia, designacion.vigencia());
        assertEquals(cambio(ADMIN, 3), designacion.cese().orElseThrow().cambio());
        assertTrue(previo.usuario(DOCENTE).orElseThrow().designaciones().getFirst().cese().isEmpty());
        assertThrows(IllegalStateException.class, () -> directorio.finalizarDesignacion(DOCENTE,
                id(100), INICIO.plusSeconds(4), cambio(ADMIN, 4)));
    }

    @Test
    void permiteCancelarUnaDesignacionFuturaAntesDeQueComience() {
        var directorio = conDocente().asignarRol(DOCENTE, id(100), RolUsuario.TOE, sinFin(10), cambio(ADMIN, 2))
                .finalizarDesignacion(DOCENTE, id(100), INICIO.plusSeconds(3), cambio(ADMIN, 3));
        assertTrue(directorio.usuario(DOCENTE).orElseThrow().rolesVigentes(INICIO.plusSeconds(10)).isEmpty());
        assertEquals(INICIO.plusSeconds(10), directorio.usuario(DOCENTE).orElseThrow().designaciones().getFirst().vigencia().desde());
    }

    @Test
    void finProgramadoEsExclusivoYNoSeAdmiteRevocarRetroactivamenteElAcceso() {
        var previo = conDocente().asignarRol(DOCENTE, id(100), RolUsuario.TOE, sinFin(0), cambio(ADMIN, 2));
        var directorio = previo.finalizarDesignacion(DOCENTE, id(100), INICIO.plusSeconds(8), cambio(ADMIN, 3));
        assertEquals(Set.of(RolUsuario.TOE), directorio.usuario(DOCENTE).orElseThrow().rolesVigentes(INICIO.plusSeconds(7)));
        assertTrue(directorio.usuario(DOCENTE).orElseThrow().rolesVigentes(INICIO.plusSeconds(8)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> previo.finalizarDesignacion(DOCENTE, id(100),
                INICIO.plusSeconds(2), cambio(ADMIN, 3)));
    }

    @Test
    void noSePuedeRetirarElUltimoAccesoAdministrativoSinReemplazoVigente() {
        var directorio = inicial();
        assertThrows(IllegalStateException.class, () -> directorio.finalizarDesignacion(ADMIN, ADMIN,
                INICIO.plusSeconds(1), cambio(ADMIN, 1)));
        assertThrows(IllegalStateException.class, () -> directorio.desactivarUsuario(ADMIN, cambio(ADMIN, 1)));
        assertTrue(directorio.usuario(ADMIN).orElseThrow().activa());
    }

    @Test
    void reemplazoPermiteBajaOCeseSinConcederAutorecuperacionAlAdministradorSaliente() {
        var directorio = conDocente().asignarRol(DOCENTE, id(100), RolUsuario.ADMINISTRADOR_OPERATIVO,
                sinFin(2), cambio(ADMIN, 2));
        var baja = directorio.desactivarUsuario(ADMIN, cambio(ADMIN, 3));
        assertThrows(SecurityException.class, () -> baja.activarUsuario(ADMIN, cambio(ADMIN, 4)));
        assertTrue(baja.activarUsuario(ADMIN, cambio(DOCENTE, 4)).usuario(ADMIN).orElseThrow().activa());
        var cese = directorio.finalizarDesignacion(ADMIN, ADMIN, INICIO.plusSeconds(4), cambio(ADMIN, 3));
        assertTrue(cese.usuario(ADMIN).orElseThrow().rolesVigentes(INICIO.plusSeconds(4)).isEmpty());
        assertThrows(IllegalStateException.class, () -> cese.desactivarUsuario(DOCENTE, cambio(DOCENTE, 5)));
    }

    @Test
    void ceseProgramadoExigeReemplazoEnLaFechaEfectivaSinImponerDesignacionPerpetua() {
        var directorio = conDocente().asignarRol(DOCENTE, id(100), RolUsuario.ADMINISTRADOR_OPERATIVO,
                hasta(2, 5), cambio(ADMIN, 2));
        assertThrows(IllegalStateException.class, () -> directorio.finalizarDesignacion(ADMIN, ADMIN,
                INICIO.plusSeconds(5), cambio(ADMIN, 3)));
        assertNotNull(directorio.finalizarDesignacion(ADMIN, ADMIN, INICIO.plusSeconds(4), cambio(ADMIN, 3)));
    }

    @Test
    void identificadoresDeDesignacionNoSeReutilizanEntreCuentas() {
        var directorio = conDocente().registrarUsuario(OTRO, "Otro ficticio", "otro", cambio(ADMIN, 2))
                .asignarRol(DOCENTE, id(100), RolUsuario.TOE, sinFin(0), cambio(ADMIN, 3));
        assertThrows(IllegalArgumentException.class, () -> directorio.asignarRol(OTRO, id(100),
                RolUsuario.MIEMBRO_COMITE, sinFin(0), cambio(ADMIN, 4)));
        assertThrows(IllegalArgumentException.class, () -> directorio.finalizarDesignacion(OTRO, id(100),
                INICIO.plusSeconds(4), cambio(ADMIN, 4)));
    }

    @Test
    void noSePuedenSobrescribirEstadosConCambiosAnteriores() {
        var directorio = conDocente().asignarRol(DOCENTE, id(100), RolUsuario.TOE, sinFin(0), cambio(ADMIN, 3));
        assertThrows(IllegalArgumentException.class, () -> directorio.desactivarUsuario(DOCENTE, cambio(ADMIN, 2)));
        assertThrows(IllegalArgumentException.class, () -> new Vigencia(INICIO, Optional.of(INICIO)));
        assertThrows(IllegalArgumentException.class, () -> new Vigencia(INICIO, Optional.of(INICIO.minusSeconds(1))));
    }

    @Test
    void coleccionesYEstadosPreviosSonInmutablesYUnaReactivacionRepetidaNoDuplicaEventos() {
        var directorio = conDocente().asignarRol(DOCENTE, id(100), RolUsuario.DOCENTE, sinFin(0), cambio(ADMIN, 2));
        var cuenta = directorio.usuario(DOCENTE).orElseThrow();
        assertAll(
                () -> assertThrows(UnsupportedOperationException.class, () -> directorio.usuarios().clear()),
                () -> assertThrows(UnsupportedOperationException.class, () -> directorio.movimientos().clear()),
                () -> assertThrows(UnsupportedOperationException.class, () -> cuenta.designaciones().clear()),
                () -> assertThrows(UnsupportedOperationException.class, () -> cuenta.estados().clear()),
                () -> assertThrows(UnsupportedOperationException.class, () -> cuenta.rolesVigentes(INICIO.plusSeconds(2)).clear()));
        assertSame(directorio, directorio.activarUsuario(DOCENTE, cambio(ADMIN, 3)));
    }

    private static DirectorioUsuarios inicial() {
        return DirectorioUsuarios.inicializar(ADMIN, "Operador ficticio", "admin",
                new RegistroCambio(id(90), INICIO, Optional.of("Designación institucional ficticia")));
    }
    private static DirectorioUsuarios conDocente() {
        return inicial().registrarUsuario(DOCENTE, "Docente ficticio", "docente", cambio(ADMIN, 1));
    }
    private static RegistroCambio cambio(UUID autor, int segundos) {
        return new RegistroCambio(autor, INICIO.plusSeconds(segundos), Optional.empty());
    }
    private static Vigencia sinFin(int desde) { return new Vigencia(INICIO.plusSeconds(desde), Optional.empty()); }
    private static Vigencia hasta(int desde, int hasta) { return new Vigencia(INICIO.plusSeconds(desde), Optional.of(INICIO.plusSeconds(hasta))); }
    private static UUID id(long numero) { return new UUID(0, numero); }
}
