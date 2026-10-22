package pe.edu.incidencias.domain.estudiante;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AsociacionColumnasTest {
    private final AlcanceImportacion alcance = new AlcanceImportacion(UUID.randomUUID(), Map.of(
            "familiar", new AlcanceImportacion.Concepto("familiar", "Contacto familiar", AlcanceImportacion.Tipo.CONTACTO_FAMILIAR, "Finalidad institucional"),
            "bullying", new AlcanceImportacion.Concepto("bullying", "Bullying previo informado", AlcanceImportacion.Tipo.ANTECEDENTE, "Protección")), "Constancia ficticia");

    @Test
    void reutilizaEncabezadosCompletosMovidosYDistingueDniEstudianteYFamiliar() {
        var alumno = new AsociacionColumnas.Destino(AsociacionColumnas.Campo.DOCUMENTO_ESTUDIANTE, Optional.empty());
        var familiar = new AsociacionColumnas.Destino(AsociacionColumnas.Campo.DOCUMENTO_FAMILIAR, Optional.of("familiar"));
        var perfil = new AsociacionColumnas(alcance, Map.of(enc("Estudiante", "DNI"), alumno, enc("Padre/Madre/Apoderado", "DNI"), familiar));
        var resultado = perfil.reutilizar(Map.of(15, enc("ESTUDIÁNTE", " dni "), 3, enc("Padre/Madre/Apoderado", "DNI"),
                4, enc("Hermanos", "P/S"), 5, enc("Orden")), alcance);
        assertEquals(alumno, resultado.seleccionadas().get(15));
        assertEquals(familiar, resultado.seleccionadas().get(3));
        assertTrue(resultado.omitidas().containsAll(List.of(4, 5)));
        assertTrue(resultado.revisar().isEmpty());
    }

    @Test
    void duplicadoDeEncabezadoConocidoExigeRevisionYNoEligePrimero() {
        var perfil = new AsociacionColumnas(alcance, Map.of(enc("Alumno", "Nombre"),
                new AsociacionColumnas.Destino(AsociacionColumnas.Campo.NOMBRE_ESTUDIANTE, Optional.empty())));
        var resultado = perfil.reutilizar(Map.of(1, enc("Alumno", "Nombre"), 3, enc("Alumno", "Nombre")), alcance);
        assertTrue(resultado.seleccionadas().isEmpty());
        assertEquals(2, resultado.revisar().size());
    }

    @Test
    void antecedenteFuturoAdmitidoSeAsociaSinEsquemaNuevoYNuevosDesconocidosSeOmiten() {
        var destino = new AsociacionColumnas.Destino(AsociacionColumnas.Campo.ANTECEDENTE, Optional.of("bullying"));
        var perfil = new AsociacionColumnas(alcance, Map.of(enc("Bullying previo informado"), destino));
        var resultado = perfil.reutilizar(Map.of(2, enc("Bullying previo informado"), 5, enc("Nueva observación heterogénea")), alcance);
        assertEquals(destino, resultado.seleccionadas().get(2));
        assertTrue(resultado.omitidas().contains(5));
    }

    @Test
    void contextoNoAdmitidoODestinoIncompatibleSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> new AsociacionColumnas(alcance, Map.of(enc("Diagnóstico"),
                new AsociacionColumnas.Destino(AsociacionColumnas.Campo.ANTECEDENTE, Optional.of("no_admitido")))));
        assertThrows(IllegalArgumentException.class, () -> new AsociacionColumnas(alcance, Map.of(enc("Familiar"),
                new AsociacionColumnas.Destino(AsociacionColumnas.Campo.NOMBRE_FAMILIAR, Optional.of("bullying")))));
    }

    @Test
    void perfilRevisadoNoUsaDosColumnasParaUnDatoNiGrupoCombinadoYSeparado() {
        var nombre = new AsociacionColumnas.Destino(AsociacionColumnas.Campo.NOMBRE_ESTUDIANTE, Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> new AsociacionColumnas(alcance, Map.of(enc("Nombre"), nombre, enc("Alumno"), nombre)));
        assertThrows(IllegalArgumentException.class, () -> new AsociacionColumnas(alcance, Map.of(enc("Grado y sección"),
                new AsociacionColumnas.Destino(AsociacionColumnas.Campo.GRUPO, Optional.empty()), enc("Grado"),
                new AsociacionColumnas.Destino(AsociacionColumnas.Campo.GRADO, Optional.empty()))));
    }

    @Test
    void cambioDeAlcanceRequiereRevisarPerfilYResultadosSonInmutables() {
        var perfil = new AsociacionColumnas(alcance, Map.of());
        assertThrows(IllegalArgumentException.class, () -> perfil.reutilizar(Map.of(),
                new AlcanceImportacion(UUID.randomUUID(), alcance.conceptos(), "Nueva decisión")));
        var resultado = perfil.reutilizar(Map.of(1, enc("Desconocida")), alcance);
        assertThrows(UnsupportedOperationException.class, () -> resultado.omitidas().clear());
        assertThrows(IllegalArgumentException.class, () -> perfil.reutilizar(Map.of(0, enc("Desconocida")), alcance));
    }

    private static AsociacionColumnas.Encabezado enc(String... niveles) {
        return new AsociacionColumnas.Encabezado(List.of(niveles));
    }
}
