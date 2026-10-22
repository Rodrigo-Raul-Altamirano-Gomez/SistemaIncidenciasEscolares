package pe.edu.incidencias.domain.incidencia;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.catalogo.ClasificacionCategoria;
import pe.edu.incidencias.domain.catalogo.Gravedad;
import pe.edu.incidencias.domain.catalogo.NaturalezaCategoria;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IntegridadHechoTest {
    private static final Instant REGISTRO = Instant.parse("2026-10-20T15:00:00Z");
    private static final UUID PERSONA = id(1);
    private static final UUID CUENTA_A = id(2);
    private static final UUID CUENTA_B = id(3);
    private static final UUID INSTITUCIONAL = id(4);

    @Test
    void unaIdentificacionInicialSinCuentaNoPermiteDosCuentasPosterioresParaLaMismaPersona() {
        var principal = reporte(10, personal(PERSONA, Optional.empty()));
        var identificacionA = reporte(11, personal(PERSONA, Optional.of(CUENTA_A)));
        var identificacionB = reporte(12, personal(PERSONA, Optional.of(CUENTA_B)));
        var hecho = Hecho.de(principal).vincularDuplicado(identificacionA, cambio(1));

        assertThrows(IllegalArgumentException.class,
                () -> hecho.vincularDuplicado(identificacionB, cambio(2)));
        assertEquals(2, hecho.reportes().size());
    }

    @Test
    void unReporteNoDuplicaAlMismoTrabajadorConDosIdentidadesQueCompartenCuenta() {
        var personaA = personal(id(20), Optional.of(CUENTA_A));
        var personaB = personal(id(21), Optional.of(CUENTA_A));

        assertThrows(IllegalArgumentException.class, () -> reporte(22, personaA, personaB));
    }

    @Test
    void unaAclaracionInstitucionalBloqueaLaCorreccionOrdinariaDeTodosLosAportesRelacionados() {
        var estudiante = PersonaInvolucrada.estudiante(PERSONA, "Estudiante ficticio");
        var principal = reporte(30, estudiante);
        var duplicado = reporte(31, estudiante);
        var relacionado = Hecho.de(principal).vincularDuplicado(duplicado, cambio(1));

        var aclarado = relacionado.aclararParticipacion(PERSONA,
                PapelParticipacion.ATRIBUCION_CORROBORADA,
                "Aclaración institucional ficticia durante la atención", cambio(2));

        assertAll(
                () -> assertTrue(principal.inicioAtencion().isEmpty()),
                () -> assertTrue(duplicado.inicioAtencion().isEmpty()),
                () -> assertEquals(PapelParticipacion.ATRIBUCION_CORROBORADA,
                        aclarado.participaciones().getFirst().papelVigente().orElseThrow()));
        for (var aporte : aclarado.reportes()) {
            assertEquals(INSTITUCIONAL, aporte.inicioAtencion().orElseThrow().autorId());
            assertThrows(IllegalStateException.class,
                    () -> aporte.corregirPorAutor(aporte.datos(),
                            new RegistroCambio(aporte.autorId(), REGISTRO.plusSeconds(3), Optional.empty())));
            assertEquals(PapelParticipacion.PRESUNTA_RESPONSABILIDAD,
                    aporte.datos().participaciones().getFirst().papel());
        }
    }

    @Test
    void laCuentaIdentificadaSoloEnDuplicadoSeRecuperaSinReescribirLaFuenteOriginal() {
        var principal = reporte(40, personal(PERSONA, Optional.empty()));
        var duplicado = reporte(41, personal(PERSONA, Optional.of(CUENTA_A)));
        var hecho = Hecho.de(principal).vincularDuplicado(duplicado, cambio(1));

        var consolidada = hecho.participaciones().getFirst();

        assertAll(
                () -> assertEquals(Optional.of(CUENTA_A), consolidada.persona().usuarioId()),
                () -> assertEquals(PERSONA, consolidada.personaId()),
                () -> assertEquals(2, consolidada.aportes().size()),
                () -> assertTrue(principal.datos().participaciones().getFirst().persona().usuarioId().isEmpty()),
                () -> assertEquals(Optional.of(CUENTA_A),
                        duplicado.datos().participaciones().getFirst().persona().usuarioId()));
    }

    @Test
    void nuevoDuplicadoDeUnHechoYaAtendidoTambienBloqueaLaEdicionOrdinaria() {
        var estudiante = PersonaInvolucrada.estudiante(PERSONA, "Estudiante ficticio");
        var atendido = Hecho.de(reporte(50, estudiante)).aclararParticipacion(PERSONA,
                PapelParticipacion.ATRIBUCION_CORROBORADA, "Intervención ficticia", cambio(1));
        var ampliado = atendido.vincularDuplicado(reporte(51, estudiante), cambio(2));
        var nuevoAporte = ampliado.reportes().getLast();
        assertTrue(nuevoAporte.inicioAtencion().isPresent());
        assertThrows(IllegalStateException.class, () -> nuevoAporte.corregirPorAutor(nuevoAporte.datos(),
                new RegistroCambio(nuevoAporte.autorId(), REGISTRO.plusSeconds(3), Optional.empty())));
    }

    @Test
    void corregirInstitucionalmenteEsAtencionAunqueNoExistaCasoAbierto() {
        var aporte = reporte(60, PersonaInvolucrada.estudiante(PERSONA, "Estudiante ficticio"));
        var corregido = aporte.corregirInstitucionalmente(aporte.datos(), cambio(1));
        assertTrue(corregido.inicioAtencion().isPresent());
        assertThrows(IllegalStateException.class, () -> corregido.corregirPorAutor(corregido.datos(),
                new RegistroCambio(aporte.autorId(), REGISTRO.plusSeconds(2), Optional.empty())));
    }

    @Test
    void actualizarUnReporteAtendidoPropagaLaRestriccionAlRestoDelHecho() {
        var estudiante = PersonaInvolucrada.estudiante(PERSONA, "Estudiante ficticio");
        var principal = reporte(70, estudiante);
        var hecho = Hecho.de(principal).vincularDuplicado(reporte(71, estudiante), cambio(1));
        var corregido = principal.corregirInstitucionalmente(principal.datos(), cambio(2));
        var actualizado = hecho.actualizarReporte(corregido);
        for (var aporte : actualizado.reportes()) {
            assertTrue(aporte.inicioAtencion().isPresent());
            assertThrows(IllegalStateException.class, () -> aporte.corregirPorAutor(aporte.datos(),
                    new RegistroCambio(aporte.autorId(), REGISTRO.plusSeconds(3), Optional.empty())));
        }
    }

    @Test
    void correccionRutinariaDeTextoOFechaNoObligaAJustificarDeNuevo() {
        var aporte = reporte(80, PersonaInvolucrada.estudiante(PERSONA, "Estudiante ficticio"))
                .marcarAtencionInstitucional(cambio(1));
        var datos = aporte.datos();
        var errataCorregida = new DatosReporte(datos.fechaHecho(), datos.anioEscolar(), datos.clasificacion(),
                "Relato ficticio con una errata corregida", datos.participaciones(), datos.solicitaAtencion(),
                datos.hora(), datos.lugar(), datos.comunicante(), datos.canal());
        var corregido = aporte.corregirInstitucionalmente(errataCorregida, cambio(2),
                ReporteIncidencia.TipoCorreccion.RUTINARIA);
        assertEquals(2, corregido.revisiones().size());
        assertEquals(Optional.empty(), corregido.revisiones().getLast().cambio().motivo());

        var personasCambiadas = new DatosReporte(datos.fechaHecho(), datos.anioEscolar(), datos.clasificacion(),
                datos.descripcion(), List.of(new Participacion(datos.participaciones().getFirst().persona(),
                PapelParticipacion.ATRIBUCION_CORROBORADA, Optional.empty())), false,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> aporte.corregirInstitucionalmente(personasCambiadas,
                cambio(2), ReporteIncidencia.TipoCorreccion.RUTINARIA));
    }

    private static PersonaInvolucrada personal(UUID personaId, Optional<UUID> cuenta) {
        return new PersonaInvolucrada(personaId, TipoPersona.PERSONAL, "Trabajador ficticio",
                cuenta, Optional.of("Función ficticia"));
    }

    private static ReporteIncidencia reporte(int numero, PersonaInvolucrada... personas) {
        var participaciones = List.of(personas).stream()
                .map(persona -> new Participacion(persona,
                        PapelParticipacion.PRESUNTA_RESPONSABILIDAD, Optional.empty()))
                .toList();
        var clasificacion = new ClasificacionCategoria(id(100), "Protección ficticia",
                NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.GRAVE, true);
        var datos = new DatosReporte(FechaHecho.exacta(LocalDate.of(2026, 10, 19)), 2026,
                clasificacion, "Relato de un hecho ficticio para comprobar integridad", participaciones,
                false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return ReporteIncidencia.registrar(id(numero), datos, id(numero + 1000), REGISTRO);
    }

    private static RegistroCambio cambio(int segundos) {
        return new RegistroCambio(INSTITUCIONAL, REGISTRO.plusSeconds(segundos), Optional.empty());
    }

    private static UUID id(long numero) {
        return new UUID(0, numero);
    }
}
