package pe.edu.incidencias.domain.documento;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.estudiante.DocumentoIdentidad;
import pe.edu.incidencias.domain.incidencia.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.documento.DatosPruebaDocumentos.*;

class PersonaExternaDatosTest {
    private PersonaInvolucrada externa(String telefono, String documento) {
        return new PersonaInvolucrada(id(60), TipoPersona.FAMILIAR, "Persona familiar referida", Optional.empty(),
                Optional.empty(), Optional.of(telefono), Optional.of(new DocumentoIdentidad("DNI", documento)));
    }
    private Hecho hechoCon(PersonaInvolucrada familiar) {
        return Hecho.de(reporte(100, familiar, PapelParticipacion.TESTIGO, HOY, "Información pertinente comunicada"));
    }
    @Test void contactoYDocumentoSonOpcionalesYNoCreanCuentaNiRepresentacionLegal() {
        var minima = new PersonaInvolucrada(id(60), TipoPersona.EXTERNA, "Persona no identificada por completo", Optional.empty(), Optional.empty());
        assertTrue(minima.contacto().isEmpty()); assertTrue(minima.documento().isEmpty());
        var familiar = externa("001 555 0123", "00123456");
        assertEquals("001 555 0123", familiar.contacto().orElseThrow());
        assertTrue(familiar.usuarioId().isEmpty()); assertTrue(familiar.funcionORelacion().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new PersonaInvolucrada(ESTUDIANTE, TipoPersona.ESTUDIANTE, "Estudiante",
                Optional.empty(), Optional.empty(), Optional.empty(), familiar.documento()));
    }
    @Test void datosSoloDeDuplicadoSeConservanSinTranscripcionAlPrincipal() {
        var minima = new PersonaInvolucrada(id(60), TipoPersona.FAMILIAR, "Persona familiar referida", Optional.empty(), Optional.empty());
        var h = hechoCon(minima).vincularDuplicado(reporte(101, externa("001 555 0123", "00123456"), PapelParticipacion.TESTIGO,
                HOY, "Dato complementario"), cambio(TOE, 20));
        var p = h.participaciones().getFirst();
        assertEquals(2, p.aportes().size()); assertEquals("001 555 0123", p.persona().contacto().orElseThrow());
        assertEquals("00123456", p.persona().documento().orElseThrow().numero());
        assertTrue(h.principal().datos().participaciones().getFirst().persona().documento().isEmpty());
        assertFalse(p.datosPersonalesDiscrepantes());
    }
    @Test void datosContradictoriosConservanFuentesYNoEligenPrimerDocumento() {
        var h = hechoCon(externa("001 555 0123", "00123456")).vincularDuplicado(reporte(101, externa("001 555 9876", "00876543"),
                PapelParticipacion.TESTIGO, HOY, "Dato discrepante"), cambio(TOE, 20));
        var p = h.participaciones().getFirst();
        assertTrue(p.datosPersonalesDiscrepantes()); assertTrue(p.persona().documento().isEmpty());
        assertTrue(p.persona().contacto().isEmpty()); assertEquals(2, p.aportes().size());
        var informe = vista(generador(abrir(h)));
        assertTrue(informe.hechos().getFirst().personas().getFirst().discrepancia());
        assertEquals("00123456", informe.hechos().getFirst().reportes().getFirst().versiones().getFirst().personas().getFirst().documento().orElseThrow().numero());
    }
    @Test void informeInternoConservaDatosYExternoExigeSeleccionIndividualExplicita() {
        var familiar = externa("001 555 0123", "00123456");
        var g = generador(abrir(hechoCon(familiar)));
        assertEquals(familiar.contacto(), vista(g).hechos().getFirst().personas().getFirst().contacto());
        var revision = new RevisionSalida(CASO, Set.of(familiar.id()), Set.of(), Map.of(), Map.of(), "Comunicación institucional",
                "Necesidad y riesgo revisados", cambio(TOE, 120));
        var informe = g.previsualizar(CASO, institucion(), "Información pertinente", DestinoInforme.tercero("Autoridad", "Protección"),
                Optional.of(revision), Map.of(), TOE, reloj(120), reloj(120));
        assertTrue(informe.hechos().getFirst().personas().getFirst().documento().isEmpty());
        assertTrue(informe.hechos().getFirst().personas().getFirst().contacto().isEmpty());
        assertFalse(informe.toString().contains("00123456")); assertFalse(informe.toString().contains("001 555 0123"));
        var seleccion = new RevisionSalida(CASO, revision.personasAdmitidas(), Set.of(), Map.of(), Map.of(), revision.baseComunicacion(),
                revision.evaluacionRiesgo(), cambio(TOE, 120), Set.of(familiar.id()), Set.of(familiar.id()));
        var admitido = g.previsualizar(CASO, institucion(), "Información pertinente", DestinoInforme.tercero("Autoridad", "Protección"),
                Optional.of(seleccion), Map.of(), TOE, reloj(120), reloj(120));
        assertEquals(familiar.documento(), admitido.hechos().getFirst().personas().getFirst().documento());
        assertEquals(familiar.contacto(), admitido.hechos().getFirst().personas().getFirst().contacto());
    }
}
