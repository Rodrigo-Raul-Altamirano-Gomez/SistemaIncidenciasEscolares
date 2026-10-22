package pe.edu.incidencias.domain.documento;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.incidencia.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.documento.DatosPruebaDocumentos.*;

class InformeCasoTest {
    @Test void consolidadoConservaDuplicadosPersonasSoloEnDuplicadoYDiscrepanciasResueltas() {
        var h = hecho().vincularDuplicado(reporte(101, otro(), PapelParticipacion.TESTIGO, HOY.minusDays(2), "Otro aporte original"), cambio(TOE, 20));
        var registro = abrir(h);
        var actual = registro.hechosDelCaso(CASO, TOE, directorio(), reloj(105)).getFirst();
        var preciso = actual.precisarDatos(FechaHecho.exacta(HOY.minusDays(3)), clasificacion(), motivo(TOE, 110));
        registro = registro.actualizarHecho(preciso, directorio(), cambio(TOE, 110));
        var informe = vista(generador(registro));
        assertEquals(1, informe.hechos().size());
        var contenido = informe.hechos().getFirst();
        assertEquals(2, contenido.reportes().size());
        assertEquals(2, contenido.personas().size());
        assertEquals(FechaHecho.exacta(HOY.minusDays(3)), contenido.fechaVigente().orElseThrow());
        assertTrue(contenido.datosDiscrepantes());
        assertEquals(HOY.minusDays(1), contenido.reportes().getFirst().fechaComunicada().fecha());
        assertEquals(OTRO, contenido.personas().getLast().referenciaInterna());
        assertEquals(id(101), contenido.personas().getLast().fuentes().getFirst().reporteId());
        assertEquals(1, contenido.precisiones().size());
    }

    @Test void cadaPapelConservaSuFuenteSinAtribuirResponsabilidadPorDefecto() {
        var h = hecho().vincularDuplicado(reporte(101, estudiante(), PapelParticipacion.PRESUNTA_RESPONSABILIDAD, HOY.minusDays(1), "Atribución diferente"), cambio(TOE, 20));
        var persona = vista(generador(abrir(h))).hechos().getFirst().personas().getFirst();
        assertTrue(persona.discrepancia());
        assertTrue(persona.papelVigente().isEmpty());
        assertEquals(List.of(PapelParticipacion.AFECTADO, PapelParticipacion.PRESUNTA_RESPONSABILIDAD), persona.fuentes().stream().map(InformeCaso.PapelFuente::papel).toList());
    }

    @Test void correccionDeUnReporteConservaSuVersionOriginalEnElInforme() {
        var registro = abierto();
        var h = registro.hechosDelCaso(CASO, TOE, directorio(), reloj(105)).getFirst();
        var anterior = h.principal().datos();
        var datos = new DatosReporte(anterior.fechaHecho(), anterior.anioEscolar(), anterior.clasificacion(), "Relato rectificado",
                anterior.participaciones(), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        var actualizado = h.actualizarReporte(h.principal().corregirInstitucionalmente(datos, motivo(TOE, 110)));
        var nuevo = registro.actualizarHecho(actualizado, directorio(), cambio(TOE, 110));
        var reporte = vista(generador(nuevo)).hechos().getFirst().reportes().getFirst();
        assertEquals("Relato rectificado", reporte.relato());
        assertEquals(2, reporte.versiones().size());
        assertEquals(anterior.descripcion(), reporte.versiones().getFirst().relato());
        assertEquals(ESTUDIANTE, reporte.versiones().getFirst().personas().getFirst().referenciaInterna());
    }

    @Test void previsualizarNoEmiteYEmitirPreservaUnaInstantaneaInmutable() {
        var registro = abierto();
        var g = generador(registro);
        var previa = vista(g);
        assertFalse(previa.emitido());
        assertTrue(previa.id().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> g.consultarEmitido(previa, TOE, reloj(130)));
        var emitido = g.emitir(previa, id(700), cambio(TOE, 125));
        var posterior = registro.registrarActuacion(CASO, Actuacion.prevista(id(400), "Seguimiento posterior", HOY.plusDays(1), TOE, Set.of(), Set.of(), cambio(TOE, 130)), directorio(), cambio(TOE, 130));
        assertSame(emitido, generador(posterior).consultarEmitido(emitido, TOE, reloj(140)));
        assertEquals(reloj(120), emitido.corte());
        assertTrue(emitido.actuaciones().isEmpty());
        assertTrue(previa.emision().isEmpty());
        assertThrows(IllegalStateException.class, () -> g.emitir(emitido, id(701), cambio(TOE, 140)));
        assertThrows(UnsupportedOperationException.class, () -> emitido.hechos().clear());
    }

    @Test void noSeAtribuyeUnaVersionActualAUnCorteAnteriorNiSeAdmiteEmisionAnterior() {
        var g = generador(abierto());
        assertThrows(IllegalArgumentException.class, () -> g.previsualizar(CASO, institucion(), "Motivo", DestinoInforme.interno(), Optional.empty(), Map.of(), TOE, reloj(90), reloj(120)));
        assertThrows(IllegalArgumentException.class, () -> g.previsualizar(CASO, institucion(), "Motivo", DestinoInforme.interno(), Optional.empty(), Map.of(), TOE, reloj(130), reloj(120)));
        assertThrows(IllegalArgumentException.class, () -> g.emitir(vista(g), id(700), cambio(TOE, 110)));
    }

    @Test void informeEmitidoPierdeLecturaSiElLectorEsExcluidoOBajaODejaSuRol() {
        var registro = abierto();
        var g = generador(registro);
        var emitido = g.emitir(vista(g), id(700), cambio(TOE, 125));
        assertSame(emitido, g.consultarEmitido(emitido, DIRECTOR, reloj(126)));
        var excluido = registro.excluirUsuario(CASO, id(900), DIRECTOR, directorio(), motivo(TOE, 130));
        assertThrows(SecurityException.class, () -> generador(excluido).consultarEmitido(emitido, DIRECTOR, reloj(140)));
        var baja = directorio().desactivarUsuario(DIRECTOR, motivo(ADMIN, 130));
        var gBaja = new GeneradorInformeCaso(registro, RegistroActas.vacio(), ReferenciasOficiales.vacias(), baja, Map.of());
        assertThrows(SecurityException.class, () -> gBaja.consultarEmitido(emitido, DIRECTOR, reloj(140)));
        var fin = directorio().finalizarDesignacion(DIRECTOR, id(1000), reloj(130), motivo(ADMIN, 130));
        var gFin = new GeneradorInformeCaso(registro, RegistroActas.vacio(), ReferenciasOficiales.vacias(), fin, Map.of());
        assertThrows(SecurityException.class, () -> gFin.consultarEmitido(emitido, DIRECTOR, reloj(140)));
    }

    @Test void ningunRolTecnicoNiAutorOrdinarioObtieneInformePorSerAutorDelReporte() {
        var g = generador(abierto());
        for (var usuario : List.of(DOCENTE, AIP, ADMIN)) {
            assertThrows(SecurityException.class, () -> g.previsualizar(CASO, institucion(), "Motivo", DestinoInforme.interno(), Optional.empty(), Map.of(), usuario, reloj(120), reloj(120)));
            var emitido = g.emitir(vista(g), id(700), cambio(TOE, 125));
            assertThrows(SecurityException.class, () -> g.consultarEmitido(emitido, usuario, reloj(130)));
        }
    }

    @Test void cierreLocalConservaAccionesYCompromisosPendientes() {
        var compromiso = Compromiso.acordar(id(401), "Visita acordada", estudiante(), Optional.of(HOY.plusDays(1)), true, cambio(TOE, 110));
        var accion = Actuacion.realizada(id(400), "Conversación", HOY, TOE, Set.of(), Set.of(), List.of(compromiso), cambio(TOE, 110));
        var registro = abierto().registrarActuacion(CASO, accion, directorio(), cambio(TOE, 110));
        registro = registro.cerrarCaso(CASO, new CierreLocal(HOY, "Conclusión local", CierreLocal.TipoCierre.CONCLUSION, cambio(TOE, 115), Optional.empty()), directorio(), cambio(TOE, 115));
        var informe = vista(generador(registro));
        assertEquals(CasoAtencion.Estado.CERRADA, informe.estadoLocal());
        assertTrue(informe.seguimientoPendiente());
        assertEquals(Compromiso.Estado.ACORDADO, informe.actuaciones().getFirst().compromisos().getFirst().estado());
        assertTrue(informe.cierre().isPresent());
    }

    @Test void informeIncluyeGobiernoComiteYAutorRealDeSusDecisiones() {
        var registro = abierto().convocarComite(CASO, id(800), HOY, "Gravedad especial", Optional.empty(), directorio(), cambio(TOE, 110))
                .registrarIntervencionComite(CASO, id(801), id(800), reloj(115), Set.of(DIRECTOR, COMITE), Optional.empty(), directorio(), cambio(DIRECTOR, 120))
                .registrarDecisionComite(CASO, id(802), id(801), "Decisión colectiva", Optional.empty(), directorio(), cambio(DIRECTOR, 130));
        var informe = generador(registro).previsualizar(CASO, institucion(), "Motivo", DestinoInforme.interno(), Optional.empty(), Map.of(), TOE, reloj(140), reloj(140));
        assertEquals(EncargadoAtencion.COMITE, informe.gobierno().encargado());
        assertEquals(1, informe.gobierno().convocatorias().size());
        assertEquals(1, informe.gobierno().intervenciones().size());
        assertEquals("Decisión colectiva", informe.gobierno().decisiones().getFirst().contenido());
        assertEquals(directorio().usuario(DIRECTOR).orElseThrow().nombreLaboral(), informe.gobierno().decisiones().getFirst().autor());
        assertEquals(ActoComite.Tipo.DECISION, informe.gobierno().actos().getFirst().tipo());
        assertTrue(informe.gobierno().intervenciones().getFirst().motivoSustitucion().isEmpty());
    }

    @Test void continuidadCitaAtencionAnteriorConTrabajoPendienteSinCopiarSusAcciones() {
        var otroHecho = Hecho.de(reporte(101, otro(), PapelParticipacion.TESTIGO, HOY.minusDays(1), "Hecho distinto"));
        var prevista = Actuacion.prevista(id(400), "Seguimiento que permanece en fuente", HOY.plusDays(1), TOE, Set.of(id(100)), Set.of(), cambio(TOE, 100));
        var registro = RegistroAtenciones.iniciar(List.of(hecho(), otroHecho), RestriccionesAmbito.vacias())
                .abrirCaso(CASO, Set.of(id(100)), HOY, TOE, List.of(prevista), Optional.empty(), directorio(), cambio(TOE, 100))
                .abrirCaso(id(201), Set.of(id(101)), HOY, TOE, List.of(), Optional.empty(), directorio(), cambio(TOE, 110))
                .continuarAtencion(CASO, id(201), Set.of(id(100)), directorio(), cambio(TOE, 120));
        var informe = generador(registro).previsualizar(id(201), institucion(), "Motivo", DestinoInforme.interno(), Optional.empty(), Map.of(), TOE, reloj(130), reloj(130));
        assertEquals(2, informe.hechos().size());
        assertTrue(informe.actuaciones().isEmpty());
        assertEquals(CASO, informe.gobierno().antecedentes().getFirst().casoId());
        assertTrue(informe.gobierno().antecedentes().getFirst().trabajoPendiente());
        assertTrue(informe.gobierno().cambiosResponsable().isEmpty());
    }
}
