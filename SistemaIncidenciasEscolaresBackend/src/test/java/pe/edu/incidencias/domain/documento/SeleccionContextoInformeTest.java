package pe.edu.incidencias.domain.documento;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.estudiante.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.documento.DatosPruebaDocumentos.*;

class SeleccionContextoInformeTest {
    @Test void seleccionExplicitaNoCopiaTodaLaFichaNiRequiereTranscribir() {
        var resultado = SeleccionContextoInforme.recuperar(ESTUDIANTE,
                Set.of(SeleccionContextoInforme.Campo.FAMILIAR_TELEFONO), Set.of("salud"), registro(),
                ContextoAcceso.deHecho(hecho()), politica(), TOE, reloj(120), reloj(120));
        assertEquals(2, resultado.size());
        assertEquals("0012345", resultado.get(ESTUDIANTE + ":FAMILIAR_TELEFONO"));
        assertTrue(resultado.get(ESTUDIANTE + ":antecedente:salud").contains("NEGACION_DECLARADA"));
        assertFalse(resultado.values().stream().anyMatch(v -> v.contains("Domicilio reservado")));
        assertThrows(UnsupportedOperationException.class, resultado::clear);
    }
    @Test void seleccionVaciaNoIncorporaAntecedentesAutomaticamente() {
        assertTrue(SeleccionContextoInforme.recuperar(ESTUDIANTE, Set.of(), Set.of(), registro(),
                ContextoAcceso.deHecho(hecho()), politica(), TOE, reloj(120), reloj(120)).isEmpty());
    }
    @Test void noInventaRelacionDelContacto() {
        var r = SeleccionContextoInforme.recuperar(ESTUDIANTE, Set.of(SeleccionContextoInforme.Campo.FAMILIAR_RELACION),
                Set.of(), registro(), ContextoAcceso.deHecho(hecho()), politica(), TOE, reloj(120), reloj(120));
        assertEquals("No informado", r.get(ESTUDIANTE + ":FAMILIAR_RELACION"));
    }
    @Test void antecedenteNoDisponibleYCortePasadoSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> SeleccionContextoInforme.recuperar(ESTUDIANTE,
                Set.of(), Set.of("no-existe"), registro(), ContextoAcceso.deHecho(hecho()), politica(), TOE, reloj(120), reloj(120)));
        assertThrows(IllegalArgumentException.class, () -> SeleccionContextoInforme.recuperar(ESTUDIANTE,
                Set.of(), Set.of("salud"), registro(), ContextoAcceso.deHecho(hecho()), politica(), TOE, reloj(1), reloj(120)));
        assertThrows(IllegalArgumentException.class, () -> SeleccionContextoInforme.recuperar(ESTUDIANTE,
                Set.of(), Set.of(), registro(), ContextoAcceso.deHecho(hecho()), politica(), TOE, reloj(130), reloj(120)));
    }
    @Test void administradorNoRecibeFichaNiEstudianteAjenoAlAmbito() {
        assertThrows(SecurityException.class, () -> SeleccionContextoInforme.recuperar(ESTUDIANTE, Set.of(), Set.of(),
                registro(), ContextoAcceso.deHecho(hecho()), politica(), ADMIN, reloj(120), reloj(120)));
        assertThrows(IllegalArgumentException.class, () -> SeleccionContextoInforme.recuperar(OTRO, Set.of(), Set.of(),
                registro(), ContextoAcceso.deHecho(hecho()), politica(), TOE, reloj(120), reloj(120)));
    }
    @Test void borradoPosteriorNoOcultaContextoExistenteAlCorteHistorico() {
        var base = registro();
        var anterior = base.contextos().get(ESTUDIANTE);
        var vacio = ContextoEstudiante.vacio(ESTUDIANTE);
        var lote = id(2010);
        var borrado = cambio(TOE, 20);
        var matriculas = RegistroMatriculas.cargar(List.copyOf(base.matriculas().estudiantes().values()),
                List.of(new RegistroMatriculas.Confirmacion(lote, "huella-reconstituida", "Nómina ficticia", "Hoja1", 2026,
                        HOY, borrado, List.of())));
        var conHistoria = RegistroImportacion.cargar(matriculas, List.of(vacio),
                List.of(new RegistroImportacion.Confirmacion(lote, "huella-reconstituida", borrado, List.of())),
                Map.of(ESTUDIANTE, List.of(new RegistroImportacion.RevisionContexto(lote, borrado, anterior, vacio))));
        var campos = Set.of(SeleccionContextoInforme.Campo.DOMICILIO);
        var historico = SeleccionContextoInforme.recuperar(ESTUDIANTE, campos, Set.of(), conHistoria,
                ContextoAcceso.deHecho(hecho()), politica(), TOE, reloj(10), reloj(120));
        assertEquals("Domicilio reservado", historico.get(ESTUDIANTE + ":DOMICILIO"));
        var actual = SeleccionContextoInforme.recuperar(ESTUDIANTE, campos, Set.of(), conHistoria,
                ContextoAcceso.deHecho(hecho()), politica(), TOE, reloj(120), reloj(120));
        assertEquals("No informado", actual.get(ESTUDIANTE + ":DOMICILIO"));
    }
    private PoliticaAcceso politica() { return new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias()); }
    private RegistroImportacion registro() {
        var estudiantes = RegistroMatriculas.cargar(List.of(Estudiante.crear(ESTUDIANTE, "Estudiante ficticio"),
                Estudiante.crear(OTRO, "Otro ficticio")), List.of());
        var p = new ContextoEstudiante.Procedencia(id(2000), "Nómina ficticia", "Hoja1", 7, id(2001),
                "salud", "Alcance institucional de prueba", cambio(TOE, 5));
        var familiar = new ContactoFamiliar(Optional.of("Familiar de prueba"), Optional.empty(), Optional.of("0012345"), Optional.empty(), Optional.empty());
        var salud = new ContextoEstudiante.Antecedente("Salud declarada", "No", ContextoEstudiante.Calidad.NEGACION_DECLARADA,
                Optional.empty(), Optional.empty());
        var ficha = new ContextoEstudiante(ESTUDIANTE, Optional.of(new ContextoEstudiante.Dato<>(familiar, p)), Optional.empty(),
                Optional.of(new ContextoEstudiante.Dato<>("Domicilio reservado", p)), Map.of("salud", new ContextoEstudiante.Dato<>(salud, p)));
        return RegistroImportacion.cargar(estudiantes, List.of(ficha), List.of());
    }
}
