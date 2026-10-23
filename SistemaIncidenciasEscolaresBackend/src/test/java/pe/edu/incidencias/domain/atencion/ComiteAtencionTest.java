package pe.edu.incidencias.domain.atencion;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.RolUsuario;
import pe.edu.incidencias.domain.acceso.Vigencia;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.atencion.DatosPruebaAtencion.*;

class ComiteAtencionTest {
    private static RegistroCambio autor(UUID id, int segundo) { return new RegistroCambio(id, reloj(segundo), Optional.empty()); }
    private static RegistroAtenciones convocado() {
        return abrir(base(), 200, Set.of(id(100)), 100).convocarComite(id(200), id(600), HOY,
                "Especial gravedad que requiere intervención conjunta", Optional.empty(), directorio(), cambio(110));
    }
    private static RegistroAtenciones intervenido() {
        return convocado().registrarIntervencionComite(id(200), id(601), id(600), reloj(115), Set.of(DIRECTOR, COMITE),
                Optional.empty(), directorio(), autor(DIRECTOR, 120));
    }

    @Test void laComposicionEsEstableYNoImplicaConvocatoriaAutomatica() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        assertEquals(Set.of(DIRECTOR, COMITE), registro.composicionComite(id(200), TOE, directorio(), reloj(105)).usuariosIds());
        assertEquals(EncargadoAtencion.TOE, registro.caso(id(200)).orElseThrow().encargado());
        assertTrue(registro.caso(id(200)).orElseThrow().convocatorias().isEmpty());
        assertThrows(IllegalStateException.class, () -> registro.registrarDecisionComite(id(200), id(610), id(601), "Acuerdo",
                Optional.empty(), directorio(), autor(DIRECTOR, 110)));
    }

    @Test void convocatoriaManualMotivadaConservaCasoEstadoYMiembrosElegibles() {
        var anterior = abrir(base(), 200, Set.of(id(100)), 100);
        assertThrows(IllegalArgumentException.class, () -> anterior.convocarComite(id(200), id(600), HOY, " ", Optional.empty(), directorio(), cambio(110)));
        var nuevo = convocado().caso(id(200)).orElseThrow();
        assertEquals(1, nuevo.convocatorias().size());
        assertEquals(Set.of(DIRECTOR, COMITE), nuevo.convocatorias().getFirst().miembrosElegibles());
        assertEquals(CasoAtencion.Estado.PENDIENTE, nuevo.estado());
        assertEquals(EncargadoAtencion.COMITE, nuevo.encargado());
        assertTrue(anterior.caso(id(200)).orElseThrow().convocatorias().isEmpty());
        assertEquals(1, convocado().casos().size());
    }

    @Test void miembroNoConvocaEnLugarDeToEQuePuedeAtender() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        assertThrows(SecurityException.class, () -> registro.convocarComite(id(200), id(600), HOY, "No resolución",
                Optional.of("Preferencia del miembro"), directorio(), autor(COMITE, 110)));
    }

    @Test void sustitucionPorToEExcluidoEsMotivadaYSoloIncluyeMiembrosNoExcluidos() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100)
                .excluirUsuario(id(200), id(700), TOE, directorio(), new RegistroCambio(DIRECTOR, reloj(105), Optional.of("Conflicto identificado")));
        assertThrows(SecurityException.class, () -> registro.convocarComite(id(200), id(600), HOY, "Especial gravedad", Optional.empty(), directorio(), autor(COMITE, 110)));
        var nuevo = registro.convocarComite(id(200), id(600), HOY, "Especial gravedad", Optional.of("TOE impedido por conflicto"), directorio(), autor(COMITE, 110));
        assertEquals(COMITE, nuevo.caso(id(200)).orElseThrow().convocatorias().getFirst().registro().autorId());
        assertEquals(Optional.of("TOE impedido por conflicto"), nuevo.caso(id(200)).orElseThrow().convocatorias().getFirst().motivoSustitucion());
        assertEquals(EncargadoAtencion.COMITE, nuevo.caso(id(200)).orElseThrow().encargado());
    }

    @Test void convocatoriaNoAdmiteMiembroExcluidoNiSeRealizaSobreCerrado() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100).excluirUsuario(id(200), id(700), COMITE, directorio(), motivo(105));
        var nuevo = registro.convocarComite(id(200), id(600), HOY, "No resolución", Optional.empty(), directorio(), cambio(110));
        assertEquals(Set.of(DIRECTOR), nuevo.caso(id(200)).orElseThrow().convocatorias().getFirst().miembrosElegibles());
        var cerrado = abrir(base(), 200, Set.of(id(100)), 100).cerrarCaso(id(200), conclusion(105), directorio(), cambio(105));
        assertThrows(IllegalStateException.class, () -> cerrado.convocarComite(id(200), id(600), HOY, "Gravedad", Optional.empty(), directorio(), cambio(110)));
    }

    @Test void participacionEfectivaNoSeDeduceDeLaConvocatoria() {
        assertTrue(convocado().caso(id(200)).orElseThrow().intervencionesComite().isEmpty());
        var registro = convocado().registrarIntervencionComite(id(200), id(601), id(600), reloj(115), Set.of(COMITE), Optional.empty(), directorio(), autor(DIRECTOR, 120));
        var intervencion = registro.caso(id(200)).orElseThrow().intervencionesComite().getFirst();
        assertEquals(Set.of(COMITE), intervencion.participantesIds());
        assertEquals(reloj(115), intervencion.ocurridaEn());
        assertEquals(DIRECTOR, intervencion.registro().autorId());
    }

    @Test void participacionRequiereMiembrosVigentesFechaRealYConvocatoriaDelMismoCaso() {
        var registro = convocado();
        assertThrows(IllegalArgumentException.class, () -> registro.registrarIntervencionComite(id(200), id(601), id(600), reloj(115), Set.of(TOE), Optional.empty(), directorio(), autor(DIRECTOR, 120)));
        assertThrows(IllegalArgumentException.class, () -> registro.registrarIntervencionComite(id(200), id(601), id(600), reloj(125), Set.of(COMITE), Optional.empty(), directorio(), autor(DIRECTOR, 120)));
        assertThrows(IllegalArgumentException.class, () -> registro.registrarIntervencionComite(id(200), id(601), id(999), reloj(115), Set.of(COMITE), Optional.empty(), directorio(), autor(DIRECTOR, 120)));
        assertThrows(IllegalArgumentException.class, () -> registro.registrarIntervencionComite(id(200), id(601), id(600), reloj(-86400), Set.of(COMITE), Optional.empty(), directorio(), autor(DIRECTOR, 120)));
    }

    @Test void convocatoriaEIntervencionSeDocumentanTardiamenteSinCambiarFechasEfectivas() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100).convocarComite(id(200), id(600), HOY,
                "Especial gravedad", Optional.empty(), directorio(), cambio(86510));
        var nuevo = registro.registrarIntervencionComite(id(200), id(601), id(600), reloj(115), Set.of(COMITE), Optional.empty(), directorio(), autor(DIRECTOR, 86520));
        assertEquals(HOY, nuevo.caso(id(200)).orElseThrow().convocatorias().getFirst().fecha());
        assertEquals(reloj(115), nuevo.caso(id(200)).orElseThrow().intervencionesComite().getFirst().ocurridaEn());
    }

    @Test void participacionTardiaConservaMiembroQueEraVigenteAunqueSuCuentaEsteDeBaja() {
        var directorio = directorio().desactivarUsuario(COMITE, new RegistroCambio(ADMIN, reloj(130), Optional.of("Baja institucional")));
        var registro = convocado().registrarIntervencionComite(id(200), id(601), id(600), reloj(115), Set.of(COMITE), Optional.empty(), directorio, autor(DIRECTOR, 140));
        assertEquals(Set.of(COMITE), registro.caso(id(200)).orElseThrow().intervencionesComite().getFirst().participantesIds());
        assertThrows(SecurityException.class, () -> registro.consultarCaso(id(200), COMITE, directorio, reloj(150)));
        assertThrows(IllegalArgumentException.class, () -> registro.registrarIntervencionComite(id(200), id(602), id(600), reloj(135), Set.of(COMITE), Optional.empty(), directorio, autor(DIRECTOR, 150)));
    }

    @Test void renovacionActualNoReescribeLaComposicionHistorica() {
        var registro = convocado();
        var directorio = directorio().finalizarDesignacion(COMITE, id(1002), reloj(130), new RegistroCambio(ADMIN, reloj(130), Optional.of("Fin designación")))
                .registrarUsuario(id(7), "Nueva integrante", "nueva", new RegistroCambio(ADMIN, reloj(130), Optional.empty()))
                .asignarRol(id(7), id(1007), RolUsuario.MIEMBRO_COMITE, new Vigencia(reloj(130), Optional.empty()), new RegistroCambio(ADMIN, reloj(130), Optional.empty()));
        assertEquals(Set.of(DIRECTOR, id(7)), registro.composicionComite(id(200), TOE, directorio, reloj(140)).usuariosIds());
        assertEquals(Set.of(DIRECTOR, COMITE), registro.caso(id(200)).orElseThrow().convocatorias().getFirst().composicion().usuariosIds());
    }

    @Test void directorRegistraDecisionYToESinMembresiaNoSustituyeAlDirector() {
        var registro = intervenido();
        assertThrows(SecurityException.class, () -> registro.registrarDecisionComite(id(200), id(610), id(601), "Acuerdo suficiente", Optional.empty(), directorio(), cambio(130)));
        var excluido = registro.excluirUsuario(id(200), id(700), DIRECTOR, directorio(), motivo(125));
        assertThrows(SecurityException.class, () -> excluido.registrarDecisionComite(id(200), id(610), id(601), "Acuerdo suficiente", Optional.of("Director impedido"), directorio(), cambio(130)));
        var nuevo = registro.registrarDecisionComite(id(200), id(610), id(601), "Acuerdo suficiente", Optional.empty(), directorio(), autor(DIRECTOR, 130));
        assertEquals(DIRECTOR, nuevo.caso(id(200)).orElseThrow().decisionesComite().getFirst().registro().autorId());
        assertEquals(ActoComite.Tipo.DECISION, nuevo.caso(id(200)).orElseThrow().actosComite().getFirst().tipo());
    }

    @Test void sustitutoFormalConservaAutorRealYMotivoYNoActuaSiDirectorElegible() {
        var registro = intervenido();
        assertThrows(SecurityException.class, () -> registro.registrarDecisionComite(id(200), id(610), id(601), "Acuerdo", Optional.of("Preferencia"), directorio(), autor(COMITE, 130)));
        var excluido = registro.excluirUsuario(id(200), id(700), DIRECTOR, directorio(), motivo(125));
        assertThrows(SecurityException.class, () -> excluido.registrarDecisionComite(id(200), id(610), id(601), "Acuerdo", Optional.empty(), directorio(), autor(COMITE, 130)));
        var nuevo = excluido.registrarDecisionComite(id(200), id(610), id(601), "Acuerdo de protección", Optional.of("Director excluido del asunto"), directorio(), autor(COMITE, 130));
        var caso = nuevo.caso(id(200)).orElseThrow();
        assertEquals(COMITE, caso.decisionesComite().getFirst().registro().autorId());
        assertEquals(Optional.of("Director excluido del asunto"), caso.actosComite().getFirst().motivoSustitucion());
        assertThrows(SecurityException.class, () -> nuevo.registrarDecisionComite(id(200), id(611), id(601), "Otro acuerdo", Optional.empty(), directorio(), autor(DIRECTOR, 140)));
    }

    @Test void atencionIndividualDelMiembroPermaneceEquivalenteYSeDistingueDelActoFormal() {
        var individual = Actuacion.realizada(id(300), "Atención individual del miembro", HOY, COMITE,
                Set.of(), Set.of(), List.of(), autor(COMITE, 130));
        var registro = intervenido().registrarActuacion(id(200), individual, directorio(), autor(COMITE, 130));
        assertEquals(1, registro.caso(id(200)).orElseThrow().actuaciones().size());
        assertTrue(registro.caso(id(200)).orElseThrow().actosComite().isEmpty());
        assertNotNull(registro.consultarCaso(id(200), COMITE, directorio(), reloj(140)));
    }

    @Test void actuacionFormalNoPuedeModificarsePorViaIndividual() {
        var prevista = Actuacion.prevista(id(300), "Actuación colectiva prevista", HOY, DIRECTOR, Set.of(id(100)), Set.of(), autor(DIRECTOR, 130));
        var registro = intervenido().registrarActuacionComite(id(200), prevista, id(601), Optional.empty(), directorio(), autor(DIRECTOR, 130));
        var completada = prevista.realizar(HOY, "Resultado colectivo documentado", autor(DIRECTOR, 140));
        assertThrows(SecurityException.class, () -> registro.registrarActuacion(id(200), completada, directorio(), autor(DIRECTOR, 140)));
        var nuevo = registro.registrarActuacionComite(id(200), completada, id(601), Optional.empty(), directorio(), autor(DIRECTOR, 140));
        assertEquals(2, nuevo.caso(id(200)).orElseThrow().actosComite().size());
        assertEquals(2, nuevo.caso(id(200)).orElseThrow().actuaciones().getFirst().revisiones().size());
    }

    @Test void derivacionYCierreDeComiteNoEludenRegistroFormal() {
        var derivacion = Derivacion.efectuada(id(620), Set.of(id(100)), Derivacion.Supuesto.VIOLENCIA_FAMILIAR,
                new Derivacion.Destino(Derivacion.TipoDestino.FISCALIA, "Fiscalía"), "Derivación real documentada", Optional.empty(), HOY, autor(DIRECTOR, 130));
        var registro = intervenido();
        assertThrows(SecurityException.class, () -> registro.registrarDerivacion(id(200), derivacion, directorio(), autor(DIRECTOR, 130)));
        assertThrows(SecurityException.class, () -> registro.cerrarCaso(id(200), conclusion(130), directorio(), cambio(130)));
        var nuevo = registro.registrarDerivacionComite(id(200), derivacion, id(601), Optional.empty(), directorio(), autor(DIRECTOR, 130));
        var caso = nuevo.caso(id(200)).orElseThrow();
        assertEquals(CierreLocal.TipoCierre.DERIVACION_FAMILIAR_FISCALIA, caso.cierre().orElseThrow().tipo());
        assertEquals(List.of(ActoComite.Tipo.DERIVACION, ActoComite.Tipo.CIERRE), caso.actosComite().stream().map(ActoComite::tipo).toList());
        assertThrows(SecurityException.class, () -> nuevo.corregirCierre(id(200), Optional.empty(), directorio(), motivo(140)));
    }

    @Test void cierreFormalSeCorrigeConMotivoYConservaHistoria() {
        var cierre = new CierreLocal(HOY, "Conclusión colectiva documentada", CierreLocal.TipoCierre.CONCLUSION, autor(DIRECTOR, 130), Optional.empty());
        var cerrado = intervenido().cerrarCasoComite(id(200), cierre, id(601), Optional.empty(), directorio(), autor(DIRECTOR, 130));
        assertThrows(IllegalArgumentException.class, () -> cerrado.corregirCierreComite(id(200), Optional.empty(), id(601), Optional.empty(), directorio(), autor(DIRECTOR, 140)));
        var reabierto = cerrado.corregirCierreComite(id(200), Optional.empty(), id(601), Optional.empty(), directorio(), new RegistroCambio(DIRECTOR, reloj(140), Optional.of("Corrección fundada del cierre")));
        assertEquals(CasoAtencion.Estado.PENDIENTE, reabierto.caso(id(200)).orElseThrow().estado());
        assertEquals(2, reabierto.caso(id(200)).orElseThrow().cierres().size());
        assertEquals(ActoComite.Tipo.CORRECCION_CIERRE, reabierto.caso(id(200)).orElseThrow().actosComite().getLast().tipo());
    }

    @Test void formalidadCompruebaOperadorYNoAdmiteHistoriaFutura() {
        var registro = intervenido();
        assertThrows(IllegalArgumentException.class, () -> registro.registrarActuacionComite(id(200), realizada(300, List.of(), 130), id(601), Optional.empty(), directorio(), autor(DIRECTOR, 130)));
        assertThrows(IllegalArgumentException.class, () -> registro.registrarDecisionComite(id(200), id(610), id(601), "Acuerdo", Optional.empty(), directorio(), autor(DIRECTOR, 110)));
        assertThrows(IllegalArgumentException.class, () -> registro.registrarDecisionComite(id(200), id(610), id(999), "Acuerdo", Optional.empty(), directorio(), autor(DIRECTOR, 130)));
        assertThrows(UnsupportedOperationException.class, () -> registro.caso(id(200)).orElseThrow().intervencionesComite().clear());
    }

    @Test void reasignacionExigeResponsableVigenteYConservaCadaCambio() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        assertThrows(SecurityException.class, () -> registro.reasignarResponsable(id(200), DOCENTE, directorio(), cambio(110)));
        var nuevo = registro.reasignarResponsable(id(200), COMITE, directorio(), cambio(110));
        var caso = nuevo.caso(id(200)).orElseThrow();
        assertEquals(COMITE, caso.responsableId());
        assertEquals(new CambioResponsable(TOE, COMITE, cambio(110)), caso.cambiosResponsable().getFirst());
        assertEquals(TOE, registro.caso(id(200)).orElseThrow().responsableId());
        var excluido = nuevo.excluirUsuario(id(200), id(700), DIRECTOR, directorio(), motivo(120));
        assertThrows(SecurityException.class, () -> excluido.reasignarResponsable(id(200), DIRECTOR, directorio(), cambio(130)));
        var baja = directorio().desactivarUsuario(COMITE, new RegistroCambio(ADMIN, reloj(120), Optional.of("Baja")));
        assertThrows(SecurityException.class, () -> registro.reasignarResponsable(id(200), COMITE, baja, cambio(130)));
    }
}
