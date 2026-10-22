package pe.edu.incidencias.domain.atencion;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.documento.*;
import pe.edu.incidencias.domain.institucion.DatosInstitucion;
import pe.edu.incidencias.domain.incidencia.PapelParticipacion;
import pe.edu.incidencias.domain.incidencia.PersonaInvolucrada;
import pe.edu.incidencias.domain.incidencia.TipoPersona;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.atencion.DatosPruebaAtencion.*;

class ReservaDocumentosIntegracionTest {
    private GeneradorInformeCaso generador(RegistroAtenciones registro) {
        return new GeneradorInformeCaso(registro, RegistroActas.vacio(), ReferenciasOficiales.vacias(), directorio(), Map.of());
    }

    @Test
    void informeEmitidoConservaSuCortePeroSeRevocaSuLecturaTrasExclusionPosterior() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        var generador = generador(registro);
        var vista = generador.previsualizar(id(200), new DatosInstitucion("Colegio ficticio", Optional.empty(), Optional.empty(), Optional.empty()),
                "Atención institucional", DestinoInforme.interno(), Optional.empty(), Map.of(), TOE, reloj(100), reloj(110));
        var emitido = generador.emitir(vista, id(900), cambio(120));
        assertSame(emitido, generador.consultarEmitido(emitido, DIRECTOR, reloj(125)));

        var restringido = registro.excluirUsuario(id(200), id(500), DIRECTOR, directorio(), motivo(130));
        var actual = generador(restringido);
        assertThrows(SecurityException.class, () -> actual.consultarEmitido(emitido, DIRECTOR, reloj(140)));
        assertSame(emitido, actual.consultarEmitido(emitido, TOE, reloj(140)));
        assertEquals(reloj(100), emitido.corte());
    }

    @Test
    void unaActuacionPosteriorNoReescribeLaInstantaneaEmitida() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        var generador = generador(registro);
        var vista = generador.previsualizar(id(200), new DatosInstitucion("Colegio ficticio", Optional.empty(), Optional.empty(), Optional.empty()),
                "Atención institucional", DestinoInforme.interno(), Optional.empty(), Map.of(), TOE, reloj(100), reloj(110));
        var emitido = generador.emitir(vista, id(900), cambio(120));
        var posterior = registro.registrarActuacion(id(200), realizada(300, List.of(), 130), directorio(), cambio(130));
        var actual = generador(posterior);
        var nuevaVista = actual.previsualizar(id(200), vista.institucion(), "Seguimiento posterior", DestinoInforme.interno(),
                Optional.empty(), Map.of(), TOE, reloj(130), reloj(140));
        assertTrue(emitido.actuaciones().isEmpty());
        assertEquals(1, nuevaVista.actuaciones().size());
        assertSame(emitido, actual.consultarEmitido(emitido, DIRECTOR, reloj(140)));
        assertFalse(nuevaVista.emitido());
    }

    @Test
    void denunciadoIdentificadoSoloEnNuevoDuplicadoPierdeAccesoAlInformeAnterior() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        var generador = generador(registro);
        var vista = generador.previsualizar(id(200), new DatosInstitucion("Colegio ficticio", Optional.empty(), Optional.empty(), Optional.empty()),
                "Atención institucional", DestinoInforme.interno(), Optional.empty(), Map.of(), TOE, reloj(100), reloj(110));
        var emitido = generador.emitir(vista, id(900), cambio(120));
        assertNotNull(generador.consultarEmitido(emitido, DIRECTOR, reloj(125)));

        var directorReferido = new PersonaInvolucrada(id(52), TipoPersona.PERSONAL, "Personal referido",
                Optional.of(DIRECTOR), Optional.of("Función institucional"));
        var hecho = registro.hechosDelCaso(id(200), TOE, directorio(), reloj(125)).getFirst();
        var actualizado = hecho.vincularDuplicado(reporte(110, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, directorReferido), motivo(130));
        var actuales = registro.actualizarHecho(actualizado, directorio(), cambio(130));

        assertThrows(SecurityException.class, () -> generador(actuales).consultarEmitido(emitido, DIRECTOR, reloj(140)));
        assertSame(emitido, generador(actuales).consultarEmitido(emitido, TOE, reloj(140)));
        assertEquals(1, emitido.hechos().getFirst().reportes().size());
    }
}
