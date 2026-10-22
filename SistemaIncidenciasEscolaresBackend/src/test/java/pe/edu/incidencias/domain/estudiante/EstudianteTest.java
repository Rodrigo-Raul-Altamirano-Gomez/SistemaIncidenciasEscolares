package pe.edu.incidencias.domain.estudiante;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EstudianteTest {
    private static final LocalDate INICIO = LocalDate.of(2026, 3, 1);
    private static final GrupoEscolar PRIMERO_A = new GrupoEscolar("Primero", "A");
    private static final GrupoEscolar PRIMERO_B = new GrupoEscolar("Primero", "B");

    @Test
    void creaIdentidadSinDocumentoSexoNacimientoOMatricula() {
        var estudiante = Estudiante.crear(UUID.randomUUID(), "García del Río María Isabel");

        assertEquals("García del Río María Isabel", estudiante.nombreCompleto());
        assertTrue(estudiante.documento().isEmpty());
        assertTrue(estudiante.fechaNacimiento().isEmpty());
        assertTrue(estudiante.sexoInformado().isEmpty());
        assertTrue(estudiante.matriculas().isEmpty());
    }

    @Test
    void agregarDocumentoYCorregirNombreConservaIdentidadYVinculosAnteriores() {
        var original = estudianteSinGrupo();
        var documento = new DocumentoIdentidad("DNI", "01234567");
        var actualizado = original.incorporarIdentidad(Optional.of("Pérez Lucía Elena"), Optional.of(documento),
                Optional.of(LocalDate.of(2013, 5, 2)), Optional.of("Femenino"));

        assertEquals(original.id(), actualizado.id());
        assertEquals(original.matriculas(), actualizado.matriculas());
        assertEquals("Pérez Lucía Elena", actualizado.nombreCompleto());
        assertEquals("01234567", actualizado.documento().orElseThrow().numero());
        assertEquals("Pérez Lucía", original.nombreCompleto());
        assertTrue(original.documento().isEmpty());
    }

    @Test
    void camposOmitidosNoBorranLosValoresConocidos() {
        var conocido = estudianteSinGrupo().incorporarIdentidad(Optional.empty(),
                Optional.of(new DocumentoIdentidad("DNI", "01234567")), Optional.of(LocalDate.of(2013, 5, 2)),
                Optional.of("Femenino"));
        var actualizado = conocido.incorporarIdentidad(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
                .incorporarGrupo(2026, INICIO, Optional.empty()).incorporarSituacion(2026, Optional.empty());

        assertEquals(conocido.documento(), actualizado.documento());
        assertEquals(conocido.fechaNacimiento(), actualizado.fechaNacimiento());
        assertEquals(conocido.sexoInformado(), actualizado.sexoInformado());
        assertEquals(conocido.matriculas(), actualizado.matriculas());
    }

    @Test
    void completarGrupoNoRecreaIdentidadNiReemplazaHistoriaPosterior() {
        var original = estudianteSinGrupo();
        var cambio = original.incorporarGrupo(2026, LocalDate.of(2026, 7, 1), Optional.of(PRIMERO_B));
        var completado = cambio.incorporarGrupo(2026, INICIO, Optional.of(PRIMERO_A));

        assertEquals(original.id(), completado.id());
        assertTrue(original.grupoEn(2026, INICIO).isEmpty());
        assertEquals(Optional.of(PRIMERO_A), completado.grupoEn(2026, LocalDate.of(2026, 6, 30)));
        assertEquals(Optional.of(PRIMERO_B), completado.grupoEn(2026, LocalDate.of(2026, 7, 1)));
        assertEquals(Optional.of(PRIMERO_B), completado.grupoEn(2026, LocalDate.of(2026, 8, 1)));
    }

    @Test
    void noAsignaElGrupoActualAUnaFechaAnteriorALaInformada() {
        var estudiante = Estudiante.crear(UUID.randomUUID(), "Pérez Lucía")
                .vincularMatricula(MatriculaAnual.crear(2026, LocalDate.of(2026, 7, 1),
                        Optional.of(PRIMERO_B), Optional.empty()));

        assertTrue(estudiante.grupoEn(2026, LocalDate.of(2026, 6, 30)).isEmpty());
    }

    @Test
    void agregarOtroAnioNoPromueveNiRetiraElVinculoPrevio() {
        var original = estudianteSinGrupo().incorporarGrupo(2026, INICIO, Optional.of(PRIMERO_A));
        var actualizado = original.vincularMatricula(MatriculaAnual.crear(2027, LocalDate.of(2027, 3, 1),
                Optional.empty(), Optional.empty()));

        assertEquals(original.id(), actualizado.id());
        assertEquals(original.matricula(2026), actualizado.matricula(2026));
        assertTrue(actualizado.grupoEn(2027, LocalDate.of(2027, 4, 1)).isEmpty());
        assertTrue(actualizado.matricula(2028).isEmpty());
        assertEquals(Optional.of("Matriculada"), actualizado.matricula(2026).orElseThrow().situacionInformada());
    }

    @Test
    void repetirVinculoEquivalenteEsIdempotenteYUnoContradictorioNoLoSobrescribe() {
        var original = estudianteSinGrupo();
        var equivalente = MatriculaAnual.crear(2026, INICIO, Optional.empty(), Optional.of("Matriculada"));
        var contradictorio = MatriculaAnual.crear(2026, INICIO, Optional.of(PRIMERO_A), Optional.empty());

        assertSame(original, original.vincularMatricula(equivalente));
        assertThrows(IllegalArgumentException.class, () -> original.vincularMatricula(contradictorio));
        assertTrue(original.grupoEn(2026, INICIO).isEmpty());
    }

    @Test
    void omitirGrupoNoBorraYUnCambioContradictorioEnLaMismaFechaRequiereRectificacion() {
        var estudiante = estudianteSinGrupo().incorporarGrupo(2026, INICIO, Optional.of(PRIMERO_A));

        assertSame(estudiante, estudiante.incorporarGrupo(2026, INICIO, Optional.empty()));
        assertSame(estudiante, estudiante.incorporarGrupo(2026, INICIO, Optional.of(PRIMERO_A)));
        assertThrows(IllegalArgumentException.class,
                () -> estudiante.incorporarGrupo(2026, INICIO, Optional.of(PRIMERO_B)));
        assertEquals(Optional.of(PRIMERO_A), estudiante.grupoEn(2026, INICIO));
    }

    @Test
    void coleccionesExpuestasSonInmutables() {
        var estudiante = estudianteSinGrupo();

        assertThrows(UnsupportedOperationException.class, () -> estudiante.matriculas().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> estudiante.matricula(2026).orElseThrow().historialGrupos().clear());
    }

    @Test
    void noCreaVinculosNiFechasDeGrupoInconsistentes() {
        var estudiante = estudianteSinGrupo();

        assertThrows(IllegalArgumentException.class,
                () -> MatriculaAnual.crear(2026, LocalDate.of(2025, 3, 1), Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> estudiante.incorporarGrupo(2027, LocalDate.of(2027, 3, 1), Optional.of(PRIMERO_A)));
        assertThrows(IllegalArgumentException.class,
                () -> estudiante.grupoEn(2026, LocalDate.of(2025, 3, 1)));
    }

    @Test
    void dniConservaCerosYRechazaCifrasPerdidasSinInventarlas() {
        assertEquals("01234567", new DocumentoIdentidad("dni", "01234567").numero());
        assertEquals("DNI", new DocumentoIdentidad("dni", "01234567").tipo());
        for (String invalido : List.of("1234567", "123456789", "1234567X", "12.345.678", "１２３４５６７８")) {
            assertThrows(IllegalArgumentException.class, () -> new DocumentoIdentidad("DNI", invalido));
        }
        assertEquals("AB-0123", new DocumentoIdentidad("Pasaporte", "AB-0123").numero());
    }

    @Test
    void rechazaValoresVaciosSinConvertirlosEnInformacionValida() {
        assertThrows(IllegalArgumentException.class, () -> Estudiante.crear(UUID.randomUUID(), " "));
        assertThrows(IllegalArgumentException.class, () -> new DocumentoIdentidad("", "12345678"));
        assertThrows(IllegalArgumentException.class, () -> new GrupoEscolar(Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new GrupoEscolar(Optional.of(" "), Optional.of("A")));
        assertThrows(IllegalArgumentException.class,
                () -> estudianteSinGrupo().incorporarIdentidad(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(" ")));
    }

    @Test
    void homonimosConservanIdentidadesDistintasYLaBusquedaNoLosFusiona() {
        var primero = Estudiante.crear(UUID.randomUUID(), "José Pérez");
        var segundo = Estudiante.crear(UUID.randomUUID(), "José Pérez");
        var resultados = BusquedaEstudiantes.porNombre(List.of(primero, segundo), "jose perez");

        assertEquals(2, resultados.size());
        assertNotEquals(resultados.getFirst().id(), resultados.getLast().id());
    }

    @Test
    void busquedaToleraTildesEspaciosYMayusculasSinAlterarElNombre() {
        var estudiante = Estudiante.crear(UUID.randomUUID(), "García   del Río María");
        var resultados = BusquedaEstudiantes.porNombre(List.of(estudiante), "  GARCIA\u00a0 DEL  RIO  ");

        assertEquals(List.of(new BusquedaEstudiantes.Coincidencia(estudiante.id(), estudiante.nombreCompleto())), resultados);
        assertEquals("García   del Río María", estudiante.nombreCompleto());
        assertThrows(UnsupportedOperationException.class, resultados::clear);
        assertThrows(IllegalArgumentException.class, () -> BusquedaEstudiantes.porNombre(List.of(estudiante), " "));
    }

    @Test
    void buscaDocumentoSinDevolverloYNoUsaNombreComoClaveUnica() {
        var documento = new DocumentoIdentidad("DNI", "01234567");
        var primero = Estudiante.crear(UUID.randomUUID(), "José Pérez")
                .incorporarIdentidad(Optional.empty(), Optional.of(documento), Optional.empty(), Optional.empty());
        var segundo = Estudiante.crear(UUID.randomUUID(), "José Pérez");

        var resultados = BusquedaEstudiantes.porDocumento(List.of(primero, segundo), documento);
        assertEquals(List.of(new BusquedaEstudiantes.Coincidencia(primero.id(), "José Pérez")), resultados);
    }

    @Test
    void buscaMatriculaHistoricaYPorGradoSinExigirSeccion() {
        var estudiante = estudianteSinGrupo().incorporarGrupo(2026, INICIO, Optional.of(PRIMERO_A))
                .incorporarGrupo(2026, LocalDate.of(2026, 7, 1), Optional.of(PRIMERO_B));
        var todos = List.of(estudiante, Estudiante.crear(UUID.randomUUID(), "Sin matrícula"));

        assertEquals(1, BusquedaEstudiantes.porAnio(todos, 2026).size());
        assertTrue(BusquedaEstudiantes.porAnio(todos, 2027).isEmpty());
        assertEquals(1, BusquedaEstudiantes.porGrupoEn(todos, 2026, INICIO, PRIMERO_A).size());
        assertTrue(BusquedaEstudiantes.porGrupoEn(todos, 2026, LocalDate.of(2026, 7, 1), PRIMERO_A).isEmpty());
        assertEquals(1, BusquedaEstudiantes.porGrupoEn(todos, 2026, LocalDate.of(2026, 7, 1),
                new GrupoEscolar(Optional.of("primero"), Optional.empty())).size());
    }

    private static Estudiante estudianteSinGrupo() {
        return Estudiante.crear(UUID.randomUUID(), "Pérez Lucía")
                .vincularMatricula(MatriculaAnual.crear(2026, INICIO, Optional.empty(), Optional.of("Matriculada")));
    }
}
