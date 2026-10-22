package pe.edu.incidencias.domain.estudiante;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConciliacionMatriculaTest {
    private static final DocumentoIdentidad DNI = new DocumentoIdentidad("DNI", "01234567");
    private static final LocalDate NACIMIENTO = LocalDate.of(2013, 5, 2);

    @Test
    void documentoUnicoConNombreNormalizadoReconoceLaMismaIdentidad() {
        var estudiante = estudiante("García del Río Lucía", Optional.of(DNI), Optional.of(NACIMIENTO));

        var resultado = ConciliacionMatricula.evaluar(
                fila("GARCIA\u00a0 DEL   RIO LUCIA", Optional.of(DNI), Optional.of(NACIMIENTO)), List.of(estudiante));

        assertEquals(ConciliacionMatricula.Estado.COINCIDENCIA_SEGURA, resultado.estado());
        assertEquals(List.of(estudiante.id()), resultado.candidatos().stream().map(ConciliacionMatricula.Candidato::id).toList());
        assertEquals("García del Río Lucía", estudiante.nombreCompleto());
    }

    @Test
    void mismoDocumentoConNombreDistintoRequiereConciliacion() {
        var estudiante = estudiante("Lucía Pérez", Optional.of(DNI), Optional.empty());

        var resultado = ConciliacionMatricula.evaluar(fila("Ana Torres", Optional.of(DNI), Optional.empty()), List.of(estudiante));

        assertEquals(ConciliacionMatricula.Estado.REQUIERE_CONCILIACION, resultado.estado());
        assertEquals(estudiante.id(), resultado.candidatos().getFirst().id());
    }

    @Test
    void mismoDocumentoConNacimientoContradictorioRequiereConciliacion() {
        var estudiante = estudiante("Lucía Pérez", Optional.of(DNI), Optional.of(NACIMIENTO));

        var resultado = ConciliacionMatricula.evaluar(fila("Lucía Pérez", Optional.of(DNI),
                Optional.of(NACIMIENTO.plusDays(1))), List.of(estudiante));

        assertEquals(ConciliacionMatricula.Estado.REQUIERE_CONCILIACION, resultado.estado());
        assertEquals(Optional.of(NACIMIENTO), estudiante.fechaNacimiento());
    }

    @Test
    void nacimientoOmitidoNoContradiceElConocido() {
        var estudiante = estudiante("Lucía Pérez", Optional.of(DNI), Optional.of(NACIMIENTO));

        var resultado = ConciliacionMatricula.evaluar(fila("Lucía Pérez", Optional.of(DNI), Optional.empty()), List.of(estudiante));

        assertEquals(ConciliacionMatricula.Estado.COINCIDENCIA_SEGURA, resultado.estado());
        assertEquals(Optional.of(NACIMIENTO), estudiante.fechaNacimiento());
    }

    @Test
    void documentoCompartidoConservaTodosLosCandidatosSinElegirElPrimero() {
        var primero = estudiante("Lucía Pérez", Optional.of(DNI), Optional.empty());
        var segundo = estudiante("Lucía Pérez", Optional.of(DNI), Optional.empty());
        var fila = fila("Lucía Pérez", Optional.of(DNI), Optional.empty());

        var resultado = ConciliacionMatricula.evaluar(fila, List.of(primero, segundo));
        var invertido = ConciliacionMatricula.evaluar(fila, List.of(segundo, primero));

        assertEquals(ConciliacionMatricula.Estado.REQUIERE_CONCILIACION, resultado.estado());
        assertEquals(2, resultado.candidatos().size());
        assertEquals(resultado, invertido);
        assertTrue(resultado.candidatos().stream().anyMatch(c -> c.id().equals(primero.id())));
        assertTrue(resultado.candidatos().stream().anyMatch(c -> c.id().equals(segundo.id())));
    }

    @Test
    void candidatoUnicoPorNombreSinDniNoUneAutomaticamenteEntreAnios() {
        var anterior = estudiante("Lucía Pérez", Optional.empty(), Optional.empty())
                .vincularMatricula(MatriculaAnual.crear(2025, LocalDate.of(2025, 3, 1),
                        Optional.of(new GrupoEscolar("Primero", "A")), Optional.of("Matriculada")));

        var resultado = ConciliacionMatricula.evaluar(fila("LUCIA PEREZ", Optional.empty(), Optional.empty()), List.of(anterior));

        assertEquals(ConciliacionMatricula.Estado.REQUIERE_CONCILIACION, resultado.estado());
        assertEquals(anterior.id(), resultado.candidatos().getFirst().id());
        assertEquals(1, anterior.matriculas().size());
        assertTrue(anterior.matricula(2026).isEmpty());
    }

    @Test
    void identidadNuevaSinDocumentoNiCandidatosPuedeProponerseComoAlta() {
        var resultado = ConciliacionMatricula.evaluar(fila("Lucía Pérez", Optional.empty(), Optional.empty()), List.of());

        assertEquals(ConciliacionMatricula.Estado.ALTA_PROPUESTA, resultado.estado());
        assertTrue(resultado.candidatos().isEmpty());
    }

    @Test
    void mismoNombreConDocumentoDistintoNoSeUneNiSeCreaSinRevision() {
        var estudiante = estudiante("Lucía Pérez", Optional.of(DNI), Optional.empty());
        var otroDocumento = new DocumentoIdentidad("DNI", "07654321");

        var resultado = ConciliacionMatricula.evaluar(fila("Lucía Pérez", Optional.of(otroDocumento), Optional.empty()), List.of(estudiante));

        assertEquals(ConciliacionMatricula.Estado.REQUIERE_CONCILIACION, resultado.estado());
        assertEquals(Optional.of(DNI), resultado.candidatos().getFirst().documento());
        assertEquals(Optional.of(DNI), estudiante.documento());
    }

    @Test
    void documentoUnicoTienePrioridadSobreOtroHomonimo() {
        var identificado = estudiante("Lucía Pérez", Optional.of(DNI), Optional.empty());
        var homonimo = estudiante("Lucía Pérez", Optional.empty(), Optional.empty());

        var resultado = ConciliacionMatricula.evaluar(fila("Lucía Pérez", Optional.of(DNI), Optional.empty()), List.of(homonimo, identificado));

        assertEquals(ConciliacionMatricula.Estado.COINCIDENCIA_SEGURA, resultado.estado());
        assertEquals(List.of(identificado.id()), resultado.candidatos().stream().map(ConciliacionMatricula.Candidato::id).toList());
    }

    @Test
    void candidatoSoloProyectaIdentidadSinMatriculaNacimientoSexoOContexto() {
        var estudiante = estudiante("Lucía Pérez", Optional.of(DNI), Optional.of(NACIMIENTO))
                .incorporarIdentidad(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("Femenino"))
                .vincularMatricula(MatriculaAnual.crear(2025, LocalDate.of(2025, 3, 1),
                        Optional.of(new GrupoEscolar("Primero", "A")), Optional.of("Matriculada")));

        var candidato = ConciliacionMatricula.evaluar(fila("Lucía Pérez", Optional.empty(), Optional.empty()), List.of(estudiante))
                .candidatos().getFirst();

        assertEquals(estudiante.id(), candidato.id());
        assertEquals(estudiante.nombreCompleto(), candidato.nombreCompleto());
        assertEquals(estudiante.documento(), candidato.documento());
        assertEquals(List.of("id", "nombreCompleto", "documento"), Arrays.stream(candidato.getClass().getRecordComponents())
                .map(componente -> componente.getName()).toList());
    }

    @Test
    void evaluarNoModificaEntradaYElResultadoConservaUnaCopiaInmutable() {
        var estudiante = estudiante("Lucía Pérez", Optional.empty(), Optional.empty());
        var entrada = new ArrayList<>(List.of(estudiante));
        var resultado = ConciliacionMatricula.evaluar(fila("Lucía Pérez", Optional.empty(), Optional.empty()), entrada);

        assertEquals(List.of(estudiante), entrada);
        assertSame(estudiante, entrada.getFirst());
        entrada.clear();
        assertEquals(1, resultado.candidatos().size());
        assertThrows(UnsupportedOperationException.class, () -> resultado.candidatos().clear());

        var candidatos = new ArrayList<>(resultado.candidatos());
        var copia = new ConciliacionMatricula.Resultado(resultado.estado(), candidatos);
        candidatos.clear();
        assertEquals(resultado.candidatos(), copia.candidatos());
    }

    private static Estudiante estudiante(String nombre, Optional<DocumentoIdentidad> documento, Optional<LocalDate> nacimiento) {
        return Estudiante.crear(UUID.randomUUID(), nombre)
                .incorporarIdentidad(Optional.empty(), documento, nacimiento, Optional.empty());
    }

    private static FilaMatricula fila(String nombre, Optional<DocumentoIdentidad> documento, Optional<LocalDate> nacimiento) {
        return new FilaMatricula(7, UUID.randomUUID(), nombre, documento, nacimiento,
                Optional.empty(), Optional.empty(), Optional.empty());
    }
}
