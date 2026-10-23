package pe.edu.incidencias.domain.documento;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.estudiante.*;
import pe.edu.incidencias.domain.incidencia.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.documento.DatosPruebaDocumentos.*;

class InformeExternoTest {
    private Map<String, String> textos() {
        return Map.of("reporte:" + id(100) + ":version:0", "M. L. P. S. relata el hecho pertinente",
                "autor:" + TOE, "Responsable de atención", "motivoInforme", "Atención solicitada",
                "finalidadInforme", "Protección del estudiante", "destinatarioInforme", "Entidad receptora");
    }
    private RevisionSalida revision(Set<UUID> personas, Map<String, String> textos, UUID autor) {
        return new RevisionSalida(CASO, personas, Set.of(), textos, Map.of("Contexto pertinente", "Información mínima seleccionada"),
                "Comunicación institucional declarada", "Revisados destinatario, necesidad y riesgo de identificación", cambio(autor, 120));
    }
    private InformeCaso externa(GeneradorInformeCaso g, RevisionSalida revision) {
        return g.previsualizar(CASO, institucion(), "Texto que debe revisarse", DestinoInforme.tercero("Entidad inicial", "Protección"),
                Optional.of(revision), Map.of("Secreto interno", "Nombre completo ajeno"), TOE, reloj(120), reloj(120));
    }

    @Test void salidaAtercerosUsaInicialesYDNIDisponibleSinCopiarNombresDelRelato() {
        var estudiante = Estudiante.crear(ESTUDIANTE, "María Lucía Pérez Soto").incorporarIdentidad(Optional.empty(),
                Optional.of(new DocumentoIdentidad("DNI", "00123456")), Optional.empty(), Optional.empty());
        var g = new GeneradorInformeCaso(abierto(), RegistroActas.vacio(), ReferenciasOficiales.vacias(), directorio(), Map.of(ESTUDIANTE, estudiante));
        var informe = externa(g, revision(Set.of(ESTUDIANTE), textos(), TOE));
        var persona = informe.hechos().getFirst().personas().getFirst();
        assertEquals("M. L. P. S. · DNI 00123456", persona.identificacion());
        assertEquals("00123456", persona.documento().orElseThrow().numero());
        assertFalse(informe.toString().contains("María Lucía Pérez Soto"));
        assertFalse(informe.toString().contains("Secreto interno"));
        assertFalse(informe.toString().contains("Nombre completo ajeno"));
        assertEquals("Atención solicitada", informe.motivo());
        assertEquals("Entidad receptora", informe.destino().destinatario().orElseThrow());
        assertEquals("Protección del estudiante", informe.destino().finalidad());
        assertEquals("Comunicación institucional declarada", informe.baseComunicacion().orElseThrow());
        assertTrue(informe.revisionExterna().isPresent());
        assertTrue(informe.hechos().getFirst().reportes().getFirst().autorId().isEmpty());
        assertTrue(informe.hechos().getFirst().reportes().getFirst().versiones().getFirst().autorId().isEmpty());
    }

    @Test void faltaDniNoImpideInformeUrgenteYConservaReferenciaSinInventarDocumento() {
        var informe = externa(generador(abierto()), revision(Set.of(ESTUDIANTE), textos(), TOE));
        var persona = informe.hechos().getFirst().personas().getFirst();
        assertTrue(persona.documento().isEmpty());
        assertTrue(persona.identificacion().contains("DNI no disponible; referencia " + ESTUDIANTE));
        assertFalse(persona.identificacion().contains("María"));
    }

    @Test void noSeConfundeOtroDocumentoConDniNiSePermiteAsignarloAOtraIdentidad() {
        var estudiante = Estudiante.crear(ESTUDIANTE, "María Lucía Pérez Soto").incorporarIdentidad(Optional.empty(),
                Optional.of(new DocumentoIdentidad("Pasaporte", "PAS123")), Optional.empty(), Optional.empty());
        var g = new GeneradorInformeCaso(abierto(), RegistroActas.vacio(), ReferenciasOficiales.vacias(), directorio(), Map.of(ESTUDIANTE, estudiante));
        assertTrue(externa(g, revision(Set.of(ESTUDIANTE), textos(), TOE)).hechos().getFirst().personas().getFirst().documento().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new GeneradorInformeCaso(abierto(), RegistroActas.vacio(), ReferenciasOficiales.vacias(), directorio(), Map.of(OTRO, estudiante)));
    }

    @Test void seleccionExplicitaConservaPersonaSoloEnDuplicadoYNoExponeLasOtras() {
        var h = hecho().vincularDuplicado(reporte(101, otro(), PapelParticipacion.TESTIGO, HOY.minusDays(1), "José Luis Ramos Díaz también aporta"), cambio(TOE, 20));
        var textos = new HashMap<>(textos());
        textos.put("reporte:" + id(101) + ":version:0", "El testigo aporta información revisada");
        var informe = externa(generador(abrir(h)), revision(Set.of(OTRO), textos, TOE));
        assertEquals(2, informe.hechos().getFirst().reportes().size());
        assertEquals(1, informe.hechos().getFirst().personas().size());
        assertEquals(OTRO, informe.hechos().getFirst().personas().getFirst().referenciaInterna());
        assertFalse(informe.toString().contains("José Luis Ramos Díaz"));
        assertTrue(informe.hechos().getFirst().reportes().getFirst().versiones().getFirst().personas().isEmpty());
    }

    @Test void exigeRevisionYCorreccionDelFragmentoConIdentidadProtegida() {
        var g = generador(abierto());
        assertThrows(IllegalArgumentException.class, () -> g.previsualizar(CASO, institucion(), "Motivo", DestinoInforme.tercero("Entidad", "Finalidad"), Optional.empty(), Map.of(), TOE, reloj(120), reloj(120)));
        var incompleta = new HashMap<>(textos());
        incompleta.remove("reporte:" + id(100) + ":version:0");
        assertThrows(IllegalArgumentException.class, () -> externa(g, revision(Set.of(ESTUDIANTE), incompleta, TOE)));
        assertThrows(SecurityException.class, () -> externa(g, revision(Set.of(ESTUDIANTE), textos(), AIP)));
        assertThrows(IllegalArgumentException.class, () -> new RevisionSalida(CASO, Set.of(), Set.of(), textos(), Map.of(), " ", "Riesgo revisado", cambio(TOE, 120)));
        assertThrows(IllegalArgumentException.class, () -> new RevisionSalida(CASO, Set.of(), Set.of(), textos(), Map.of(), "Base", " ", cambio(TOE, 120)));
        assertThrows(IllegalArgumentException.class, () -> DestinoInforme.tercero(" ", "Finalidad"));
    }

    @Test void nombreCortoProtegidoNoObligaATranscribirPalabrasAjenasQueLoContienen() {
        var persona = PersonaInvolucrada.estudiante(ESTUDIANTE, "Ana");
        var h = Hecho.de(reporte(100, persona, PapelParticipacion.AFECTADO, HOY, "Se hará seguimiento durante la semana"));
        var g = generador(abrir(h));
        var revision = revision(Set.of(ESTUDIANTE), Map.of(), TOE);
        var informe = externa(g, revision);
        assertEquals("Se hará seguimiento durante la semana", informe.hechos().getFirst().reportes().getFirst().relato());
        var conNombre = Hecho.de(reporte(100, persona, PapelParticipacion.AFECTADO, HOY, "Se entrevistó a ANA, estudiante afectada"));
        assertThrows(IllegalArgumentException.class, () -> externa(generador(abrir(conNombre)), revision));
    }

    @Test void accionesYCompromisosConIdentidadesProtegidasTambienSeAdecuan() {
        var compromiso = Compromiso.acordar(id(401), "María Lucía Pérez Soto asumió un acuerdo", estudiante(), Optional.empty(), true, cambio(TOE, 110));
        var accion = Actuacion.realizada(id(400), "María Lucía Pérez Soto fue atendida", HOY, TOE, Set.of(), Set.of(), List.of(compromiso), cambio(TOE, 110));
        var registro = abierto().registrarActuacion(CASO, accion, directorio(), cambio(TOE, 110));
        var g = generador(registro);
        assertThrows(IllegalArgumentException.class, () -> externa(g, revision(Set.of(ESTUDIANTE), textos(), TOE)));
        var completos = new HashMap<>(textos());
        completos.put("actuacion:" + id(400), "Atención individual documentada");
        completos.put("compromiso:" + id(401), "Acuerdo pertinente revisado");
        var informe = externa(g, revision(Set.of(ESTUDIANTE), completos, TOE));
        assertFalse(informe.toString().contains("María Lucía Pérez Soto"));
        assertEquals("Acuerdo pertinente revisado", informe.actuaciones().getFirst().compromisos().getFirst().contenido());
        assertTrue(informe.seguimientoPendiente());
    }

    @Test void revisionDesactualizadaAjenaOFuturaNoAutorizaSalida() {
        var g = generador(abierto());
        for (var r : List.of(new RevisionSalida(CASO, Set.of(ESTUDIANTE), Set.of(), textos(), Map.of(), "Base", "Riesgo", cambio(TOE, 110)),
                new RevisionSalida(CASO, Set.of(ESTUDIANTE), Set.of(), textos(), Map.of(), "Base", "Riesgo", cambio(TOE, 130)),
                new RevisionSalida(id(201), Set.of(ESTUDIANTE), Set.of(), textos(), Map.of(), "Base", "Riesgo", cambio(TOE, 120))))
            assertThrows(IllegalArgumentException.class, () -> externa(g, r));
    }

    @Test void metadataDeRevisionYEmisionExternaNoCopiaMotivosAdministrativosCrudos() {
        var control = new RevisionSalida(CASO, Set.of(ESTUDIANTE), Set.of(), textos(), Map.of(), "Base declarada", "Riesgo revisado",
                new RegistroCambio(TOE, reloj(120), Optional.of("María Lucía Pérez Soto en motivo administrativo")));
        var g = generador(abierto());
        var emitido = g.emitir(externa(g, control), id(700), new RegistroCambio(TOE, reloj(125), Optional.of("María Lucía Pérez Soto en motivo de emisión")));
        assertTrue(emitido.emitido());
        assertTrue(emitido.revisionExterna().orElseThrow().motivo().isEmpty());
        assertTrue(emitido.emision().orElseThrow().motivo().isEmpty());
        assertFalse(emitido.toString().contains("María Lucía Pérez Soto"));
    }

    @Test void reutilizaRelatoYActuacionSegurosSinTranscribirCadaFragmento() {
        var h = Hecho.de(reporte(100, estudiante(), PapelParticipacion.AFECTADO, HOY.minusDays(1), "Se comunicó la situación de protección"));
        var accion = Actuacion.realizada(id(400), "Se efectuó acompañamiento y se comunicó el resultado", HOY, TOE, Set.of(), Set.of(), List.of(), cambio(TOE, 110));
        var registro = abrir(h).registrarActuacion(CASO, accion, directorio(), cambio(TOE, 110));
        var salida = generador(registro).previsualizar(CASO, institucion(), "Informe para protección", DestinoInforme.tercero("Entidad", "Protección"),
                Optional.of(revision(Set.of(ESTUDIANTE), Map.of(), TOE)), Map.of(), TOE, reloj(120), reloj(120));
        assertEquals("Se comunicó la situación de protección", salida.hechos().getFirst().reportes().getFirst().relato());
        assertEquals(accion.relato(), salida.actuaciones().getFirst().relato());
        assertEquals("Informe para protección", salida.motivo());
        assertTrue(salida.hechos().getFirst().personas().getFirst().identificacion().startsWith("M. L. P. S."));
    }

    @Test void soloElFragmentoQueExponeLaIdentidadNecesitaAdecuacion() {
        var control = revision(Set.of(ESTUDIANTE), Map.of("reporte:" + id(100) + ":version:0", "La estudiante comunicó el hecho"), TOE);
        var salida = externa(generador(abierto()), control);
        assertEquals("La estudiante comunicó el hecho", salida.hechos().getFirst().reportes().getFirst().relato());
        assertEquals("Texto que debe revisarse", salida.motivo());
        assertFalse(salida.toString().contains("María Lucía Pérez Soto"));
    }

    @Test void informeNoViolentoATerceroNoImponeInicialesNiLimitaElDocumentoADni() {
        var categoria = new pe.edu.incidencias.domain.catalogo.ClasificacionCategoria(id(90), "Convivencia ordinaria",
                pe.edu.incidencias.domain.catalogo.NaturalezaCategoria.OTRA_CONVIVENCIA,
                pe.edu.incidencias.domain.catalogo.Gravedad.LEVE, false);
        var reporte = ReporteIncidencia.registrar(id(100), new DatosReporte(FechaHecho.exacta(HOY), 2026, categoria,
                "María Lucía Pérez Soto participó en la actividad", List.of(new Participacion(estudiante(), PapelParticipacion.OTRO, Optional.empty())),
                false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()), DOCENTE, reloj(10));
        var estudiante = Estudiante.crear(ESTUDIANTE, "María Lucía Pérez Soto").incorporarIdentidad(Optional.empty(),
                Optional.of(new DocumentoIdentidad("Pasaporte", "PAS123")), Optional.empty(), Optional.empty());
        var g = new GeneradorInformeCaso(abrir(Hecho.de(reporte)), RegistroActas.vacio(), ReferenciasOficiales.vacias(), directorio(), Map.of(ESTUDIANTE, estudiante));
        var salida = externa(g, revision(Set.of(ESTUDIANTE), Map.of(), TOE));
        assertEquals("María Lucía Pérez Soto", salida.hechos().getFirst().personas().getFirst().identificacion());
        assertEquals("Pasaporte", salida.hechos().getFirst().personas().getFirst().documento().orElseThrow().tipo());
        assertEquals(reporte.datos().descripcion(), salida.hechos().getFirst().reportes().getFirst().relato());
    }

    @Test void seleccionDeContextoNoPermiteCopiarNombreProtegidoSinAdecuarlo() {
        var control = new RevisionSalida(CASO, Set.of(ESTUDIANTE), Set.of(), textos(), Map.of("Contexto", "María Lucía Pérez Soto consta en el antecedente"),
                "Base declarada", "Riesgo revisado", cambio(TOE, 120));
        assertThrows(IllegalArgumentException.class, () -> externa(generador(abierto()), control));
    }
}
