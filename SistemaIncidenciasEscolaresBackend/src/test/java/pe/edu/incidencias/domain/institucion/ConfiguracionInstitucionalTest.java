package pe.edu.incidencias.domain.institucion;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.catalogo.*;
import pe.edu.incidencias.domain.estudiante.GrupoEscolar;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguracionInstitucionalTest {
    private static final Instant T = Instant.parse("2026-10-22T15:00:00Z");
    private final UUID admin = UUID.randomUUID();
    private final UUID toe = UUID.randomUUID();
    private final UUID director = UUID.randomUUID();
    private final UUID docente = UUID.randomUUID();
    private final DirectorioUsuarios directorio = directorio();
    private final PoliticaAcceso politica = new PoliticaAcceso(directorio, RestriccionesAmbito.vacias());

    @Test void creaSinInventarVentanaAvisoNiAnio() {
        var c = crear();
        assertEquals("Colegio de prueba", c.estado().institucion().nombre());
        assertTrue(c.estado().reiteracion().ventanaDias().isEmpty());
        assertTrue(c.estado().anioTrabajo().isEmpty());
        assertTrue(c.estado().aviso().isEmpty());
        assertTrue(c.estado().alcanceImportacion().isEmpty());
    }
    @Test void configuracionNoEsCapacidadDeAdministradorODocente() {
        for (var id : List.of(admin, docente)) assertThrows(SecurityException.class,
                () -> ConfiguracionInstitucional.crear(valores("Colegio"), politica, cambio(id)));
        assertDoesNotThrow(() -> ConfiguracionInstitucional.crear(valores("Colegio"), politica, cambio(director)));
    }
    @Test void toeNoAdministraAniosPorSuRol() {
        assertThrows(SecurityException.class, () -> crear().registrarAnio(2026, politica, cambio(toe)));
        assertThrows(SecurityException.class, () -> crear().registrarAnio(2026, politica, cambio(director)));
    }
    @Test void aniosHistoricosSinCierresNiPromocionesYSeleccionExplicita() {
        var anterior = crear();
        var actual = anterior.registrarAnio(2025, politica, cambio(admin)).registrarAnio(2027, politica, cambio(admin))
                .seleccionarAnioTrabajo(2025, politica, cambio(admin));
        assertEquals(Set.of(2025, 2027), actual.estado().gruposPorAnio().keySet());
        assertEquals(2025, actual.estado().anioTrabajo().orElseThrow());
        assertTrue(anterior.estado().gruposPorAnio().isEmpty());
        assertSame(actual, actual.registrarAnio(2025, politica, cambio(admin)));
        assertThrows(IllegalArgumentException.class, () -> actual.seleccionarAnioTrabajo(2026, politica, cambio(admin)));
        assertThrows(IllegalArgumentException.class, () -> actual.registrarAnio(0, politica, cambio(admin)));
    }
    @Test void gruposDesconocidosAdmitidosEInformadosContraCatalogo() {
        var grupo = new GrupoEscolar("Primero", "A");
        var c = crear().registrarAnio(2026, politica, cambio(admin)).definirGrupos(2026, Set.of(grupo), politica, cambio(admin));
        assertTrue(c.admiteGrupo(2026, Optional.empty()));
        assertTrue(c.admiteGrupo(2026, Optional.of(new GrupoEscolar("PRIMERO", "a"))));
        assertFalse(c.admiteGrupo(2026, Optional.of(new GrupoEscolar("Segundo", "B"))));
        assertThrows(IllegalArgumentException.class, () -> c.definirGrupos(2026,
                Set.of(grupo, new GrupoEscolar("PRIMERO", "a")), politica, cambio(admin)));
        assertThrows(IllegalArgumentException.class, () -> c.definirGrupos(2025, Set.of(grupo), politica, cambio(admin)));
    }
    @Test void delegacionCongelaValoresRegistraDecisorYCargador() {
        var original = crear();
        var id = UUID.randomUUID();
        var delegada = original.delegar(id, admin, valores("Nuevo nombre"), politica, cambio(toe));
        assertEquals("Colegio de prueba", delegada.estado().institucion().nombre());
        var cargada = delegada.cargarDelegacion(id, politica, cambio(admin));
        assertEquals("Nuevo nombre", cargada.estado().institucion().nombre());
        var evidencia = cargada.delegaciones().get(id);
        assertEquals(toe, evidencia.definicion().autorId());
        assertEquals(admin, evidencia.carga().orElseThrow().autorId());
        assertSame(cargada, cargada.cargarDelegacion(id, politica, cambio(admin)));
    }
    @Test void noDelegaAUnUsuarioSinAdministracionNiCargaOtroOperador() {
        var c = crear();
        assertThrows(SecurityException.class, () -> c.delegar(UUID.randomUUID(), docente, valores("X"), politica, cambio(toe)));
        var id = UUID.randomUUID();
        var d = c.delegar(id, admin, valores("X"), politica, cambio(toe));
        assertThrows(SecurityException.class, () -> d.cargarDelegacion(id, politica, cambio(toe)));
        assertThrows(IllegalArgumentException.class, () -> d.delegar(id, admin, valores("Y"), politica, cambio(toe)));
    }
    @Test void delegacionNoPisaDecisionPosteriorAunqueCoincidanInstantes() {
        var id = UUID.randomUUID();
        var c = crear().delegar(id, admin, valores("Antiguo"), politica, cambio(toe))
                .definir(valores("Más reciente"), politica, cambio(toe));
        assertThrows(IllegalStateException.class, () -> c.cargarDelegacion(id, politica, cambio(admin)));
        assertEquals("Más reciente", c.estado().institucion().nombre());
    }
    @Test void cambiarAnioNoInvalidaDelegacionInstitucional() {
        var id = UUID.randomUUID();
        var c = crear().delegar(id, admin, valores("Nuevo"), politica, cambio(toe))
                .registrarAnio(2027, politica, cambio(admin)).cargarDelegacion(id, politica, cambio(admin));
        assertTrue(c.estado().gruposPorAnio().containsKey(2027));
        assertEquals("Nuevo", c.estado().institucion().nombre());
    }
    @Test void bajaDelAdministradorRevocaCargaDelegada() {
        var id = UUID.randomUUID();
        var c = crear().delegar(id, admin, valores("Nuevo"), politica, cambio(toe));
        var suplente = UUID.randomUUID();
        var conSuplente = directorio.registrarUsuario(suplente, "Administrador suplente", "admin-suplente", cambio(admin))
                .asignarRol(suplente, UUID.randomUUID(), RolUsuario.ADMINISTRADOR_OPERATIVO,
                        new Vigencia(T, Optional.empty()), cambio(admin));
        var baja = conSuplente.desactivarUsuario(admin, cambio(admin));
        assertThrows(SecurityException.class, () -> c.cargarDelegacion(id,
                new PoliticaAcceso(baja, RestriccionesAmbito.vacias()), cambio(admin)));
    }
    @Test void catalogoYVentanaVersionadosSinReclasificacionRetroactiva() {
        var id = UUID.randomUUID();
        var definicion = categoria("Convivencia");
        var valores = new DefinicionInstitucional(datos("Colegio"), Optional.empty(), Optional.empty(),
                Map.of(id, definicion), Set.of(id), OptionalInt.of(30));
        var original = ConfiguracionInstitucional.crear(valores, politica, cambio(toe));
        var instantanea = original.estado().categorias().get(id).clasificacionParaNuevoHecho();
        var siguientes = new DefinicionInstitucional(datos("Colegio"), Optional.empty(), Optional.empty(),
                Map.of(id, categoria("Nombre nuevo")), Set.of(), OptionalInt.of(60));
        var actualizado = original.definir(siguientes, politica, cambio(toe));
        assertEquals("Convivencia", instantanea.nombre());
        assertFalse(actualizado.estado().categorias().get(id).activa());
        assertEquals(60, actualizado.estado().reiteracion().ventanaDias().orElseThrow());
        assertEquals(30, original.estado().reiteracion().ventanaDias().orElseThrow());
        assertThrows(IllegalStateException.class, () -> actualizado.estado().categorias().get(id).clasificacionParaNuevoHecho());
        var conserva = actualizado.definir(valores("Colegio"), politica, cambio(toe));
        assertTrue(conserva.estado().categorias().containsKey(id));
        assertTrue(conserva.estado().reiteracion().ventanaDias().isEmpty());
    }
    @Test void avisoContieneInformacionMinimaYSeVersiona() {
        var aviso = new AvisoPrivacidad("Banco", "Responsable", "Domicilio", "Contacto", List.of("Encargado"),
                "Finalidad", "Destinatarios", "Opcionales y consecuencias", "Criterio institucional", "Canal derechos");
        var c = crear();
        var valores = new DefinicionInstitucional(datos("Colegio"), Optional.of(aviso), Optional.empty(), Map.of(), Set.of(), OptionalInt.empty());
        var actualizado = c.definir(valores, politica, cambio(toe));
        assertEquals(aviso, actualizado.estado().aviso().orElseThrow());
        assertTrue(c.estado().aviso().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new AvisoPrivacidad("", "R", "D", "C", List.of(), "F", "T", "O", "P", "M"));
    }
    @Test void cambiarSoloDatosInstitucionalesNoDuplicaVersionesDeCategoriasSinCambios() {
        var id = UUID.randomUUID();
        var categorias = Map.of(id, categoria("Convivencia"));
        var inicial = new DefinicionInstitucional(datos("Nombre inicial"), Optional.empty(), Optional.empty(),
                categorias, Set.of(id), OptionalInt.of(30));
        var original = ConfiguracionInstitucional.crear(inicial, politica, cambio(toe));
        var cambioNombre = new DefinicionInstitucional(datos("Nombre actualizado"), Optional.empty(), Optional.empty(),
                categorias, Set.of(id), OptionalInt.of(30));
        var actual = original.definir(cambioNombre, politica, cambio(toe));
        assertEquals(2, actual.revisiones().size());
        assertSame(original.estado().categorias().get(id), actual.estado().categorias().get(id));
        assertSame(original.estado().reiteracion(), actual.estado().reiteracion());
    }
    @Test void noPermiteVersionesPasadasNiMutarColecciones() {
        var c = crear().registrarAnio(2026, politica, cambio(admin));
        assertThrows(IllegalArgumentException.class, () -> c.definir(valores("X"), politica,
                new RegistroCambio(toe, T.minusSeconds(1), Optional.empty())));
        assertThrows(UnsupportedOperationException.class, () -> c.revisiones().clear());
        assertThrows(UnsupportedOperationException.class, () -> c.estado().gruposPorAnio().clear());
        assertThrows(UnsupportedOperationException.class, () -> c.estado().gruposPorAnio().get(2026).clear());
    }
    private ConfiguracionInstitucional crear() { return ConfiguracionInstitucional.crear(valores("Colegio de prueba"), politica, cambio(toe)); }
    private DefinicionInstitucional valores(String nombre) {
        return new DefinicionInstitucional(datos(nombre), Optional.empty(), Optional.empty(), Map.of(), Set.of(), OptionalInt.empty());
    }
    private DatosInstitucion datos(String nombre) { return new DatosInstitucion(nombre, Optional.empty(), Optional.empty(), Optional.empty()); }
    private DefinicionCategoria categoria(String nombre) {
        return new DefinicionCategoria(nombre, "Descripción", NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE, false, true, OptionalInt.of(3));
    }
    private RegistroCambio cambio(UUID id) { return new RegistroCambio(id, T, Optional.empty()); }
    private DirectorioUsuarios directorio() {
        var c = new RegistroCambio(admin, T, Optional.of("Designación ficticia"));
        var d = DirectorioUsuarios.inicializar(admin, "Administrador", "admin", c);
        for (var entrada : Map.of(toe, RolUsuario.TOE, director, RolUsuario.DIRECTOR, docente, RolUsuario.DOCENTE).entrySet()) {
            d = d.registrarUsuario(entrada.getKey(), entrada.getValue().name(), entrada.getValue().name(), c)
                    .asignarRol(entrada.getKey(), UUID.randomUUID(), entrada.getValue(), new Vigencia(T, Optional.empty()), c);
        }
        return d;
    }
}
