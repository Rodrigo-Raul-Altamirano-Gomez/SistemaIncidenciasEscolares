package pe.edu.incidencias.domain.proteccion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.proteccion.DisposicionDatos.*;

class ProteccionDatosTest {
    static final Instant T = Instant.parse("2026-10-21T15:00:00Z");
    static final UUID ADMIN = id(1), PERSONA = id(20), RECURSO = id(30), COPIA = id(31);
    static UUID id(long i) { return new UUID(0, i); }
    static RegistroCambio cambio(int segundos) { return new RegistroCambio(ADMIN, T.plusSeconds(segundos), Optional.of("Decisión institucional delimitada")); }
    static DirectorioUsuarios directorio() { return DirectorioUsuarios.inicializar(ADMIN, "Operador", "operador", cambio(0)); }
    static AlcanceProteccion alcance() { return new AlcanceProteccion(Optional.empty(), Set.of(PERSONA), Set.of(RECURSO, COPIA)); }
    static AutorizacionProteccion autorizacion(AlcanceProteccion alcance) {
        return new AutorizacionProteccion(id(2), ADMIN, Set.of(AutorizacionProteccion.Operacion.values()), alcance,
                new Vigencia(T, Optional.of(T.plusSeconds(1000))), "Responsable institucional", "Constancia externa 1",
                "Fundamento institucional revisado", T);
    }
    static Plan plan() {
        return new Plan(id(40), Decision.SUPRESION, alcance(), List.of(
                new CambioRecurso(RECURSO, TipoRecurso.REPORTE, Tratamiento.SUSTITUIR_CAMPOS, Set.of("documento"),
                        Map.of("relato", "Relato pertinente restante"), Set.of("tercero")),
                new CambioRecurso(COPIA, TipoRecurso.COPIA_OPERATIVA, Tratamiento.ELIMINAR_RECURSO,
                        Set.of(), Map.of(), Set.of())),
                "Inventario revisado: original, revisiones, informes, adjuntos y copias operativas", 
                "Respaldos aislados; retención institucional; reaplicar decisiones antes de acceso", "", cambio(10), "Decisión 2");
    }
    static List<Representacion> originales() {
        return List.of(new Representacion(RECURSO, TipoRecurso.REPORTE, Map.of("documento", "12345678",
                "relato", "Dato personal que debe retirarse", "tercero", "Información necesaria de otra persona")),
                new Representacion(COPIA, TipoRecurso.COPIA_OPERATIVA, Map.of("contenido", "Copia identificada")));
    }
    static Preparacion preparada() { return preparar(plan(), originales(), List.of(), autorizacion(alcance()), directorio()); }

    @Test void bloqueoEsExpresoYNoSeDeduceDeRecibirSolicitud() {
        assertTrue(ControlProteccion.vacio().permitePersona(PERSONA, T));
        var control = ControlProteccion.vacio().bloquear(id(4), AlcanceProteccion.persona(PERSONA), autorizacion(alcance()), cambio(10), directorio());
        assertFalse(control.permitePersona(PERSONA, T.plusSeconds(11)));
        assertTrue(control.permitePersona(id(21), T.plusSeconds(11)));
    }
    @Test void mandatoNoConcedeRolNiPermisoFuncional() {
        var p = new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias());
        assertFalse(p.evaluarPersona(ADMIN, Permiso.CONSULTAR_ESTADISTICAS, PERSONA, T).permitida());
    }
    @Test void alcanceNoPuedeSerVacio() {
        assertThrows(IllegalArgumentException.class, () -> new AlcanceProteccion(Optional.empty(), Set.of(), Set.of()));
    }
    @Test void operadorNoPuedeExtenderAlcanceDelMandato() {
        assertThrows(SecurityException.class, () -> ControlProteccion.vacio().bloquear(id(4), AlcanceProteccion.persona(id(99)),
                autorizacion(alcance()), cambio(10), directorio()));
    }
    @Test void mandatoCompruebaOperadorVigenciaYCuentaActiva() {
        assertThrows(SecurityException.class, () -> autorizacion(alcance()).exigir(id(99), AutorizacionProteccion.Operacion.DISPONER,
                alcance(), T.plusSeconds(10), directorio()));
        assertThrows(SecurityException.class, () -> autorizacion(alcance()).exigir(ADMIN, AutorizacionProteccion.Operacion.DISPONER,
                alcance(), T.plusSeconds(1000), directorio()));
        var inactivo = directorio().registrarUsuario(id(99), "Administrador de respaldo", "respaldo", cambio(1))
                .asignarRol(id(99), id(199), RolUsuario.ADMINISTRADOR_OPERATIVO,
                        new Vigencia(T, Optional.empty()), cambio(1)).desactivarUsuario(ADMIN, cambio(2));
        assertThrows(SecurityException.class, () -> autorizacion(alcance()).exigir(ADMIN, AutorizacionProteccion.Operacion.DISPONER,
                alcance(), T.plusSeconds(10), inactivo));
    }
    @Test void levantarExigeOtraOperacionAutorizadaYConservaHistoria() {
        var a = autorizacion(alcance());
        var control = ControlProteccion.vacio().bloquear(id(4), alcance(), a, cambio(10), directorio());
        var limitado = new AutorizacionProteccion(id(2), ADMIN, Set.of(AutorizacionProteccion.Operacion.BLOQUEAR), alcance(),
                new Vigencia(T, Optional.empty()), "Autoridad", "Constancia", "Fundamento", T);
        assertThrows(SecurityException.class, () -> control.levantar(id(4), limitado, cambio(20), directorio()));
        var levantado = control.levantar(id(4), a, cambio(20), directorio());
        assertTrue(levantado.permitePersona(PERSONA, T.plusSeconds(21)));
        assertFalse(control.permitePersona(PERSONA, T.plusSeconds(21)));
        assertEquals(cambio(10), levantado.bloqueos().getFirst().registro());
    }
    @Test void controlActualRechazaConsultasQuePrecedenSusDecisiones() {
        var c = ControlProteccion.vacio().bloquear(id(4), alcance(), autorizacion(alcance()), cambio(10), directorio());
        assertThrows(IllegalArgumentException.class, () -> c.permitePersona(PERSONA, T));
    }
    @Test void transformacionRetiraDatoYCopiaConservandoExactamenteTerceros() {
        var p = preparada();
        var r = p.resultadoEsperado().getFirst().vigente().orElseThrow();
        assertFalse(r.campos().containsKey("documento"));
        assertEquals("Relato pertinente restante", r.campos().get("relato"));
        assertEquals(originales().getFirst().campos().get("tercero"), r.campos().get("tercero"));
        assertTrue(p.resultadoEsperado().getLast().vigente().isEmpty());
        assertTrue(originales().getFirst().campos().containsKey("documento"));
    }
    @Test void prepararNoEsEjecutarYConfirmarExigeRelecturaCompleta() {
        var p = preparada();
        assertThrows(IllegalStateException.class, () -> confirmar(p, List.of(), List.of(), autorizacion(alcance()), cambio(20), "Verificado", directorio()));
        var e = confirmar(p, p.resultadoEsperado(), List.of(), autorizacion(alcance()), cambio(20), "Relectura operativa comprobada", directorio());
        assertEquals(Set.of(RECURSO, COPIA), e.recursosVerificados());
    }
    @Test void copiaOperativaQueSigueExistiendoImpideExito() {
        var p = preparada();
        assertThrows(IllegalStateException.class, () -> confirmar(p, List.of(p.resultadoEsperado().getFirst(),
                new Comprobacion(COPIA, Optional.of(originales().getLast()))), List.of(), autorizacion(alcance()), cambio(20), "Verificado", directorio()));
    }
    @Test void perdidaDeDatoAjenoImpideExito() {
        var p = preparada();
        assertThrows(IllegalStateException.class, () -> confirmar(p, List.of(new Comprobacion(RECURSO,
                Optional.of(new Representacion(RECURSO, TipoRecurso.REPORTE, Map.of("relato", "Relato pertinente restante")))),
                p.resultadoEsperado().getLast()), List.of(), autorizacion(alcance()), cambio(20), "Verificado", directorio()));
    }
    @Test void retencionBloqueaPreparacionYEjecucionPosterior() {
        var retencion = new Retencion(Set.of(RECURSO), "Necesario conservar", "Constancia", Optional.empty());
        assertThrows(IllegalStateException.class, () -> preparar(plan(), originales(), List.of(retencion), autorizacion(alcance()), directorio()));
        assertThrows(IllegalStateException.class, () -> confirmar(preparada(), preparada().resultadoEsperado(), List.of(retencion),
                autorizacion(alcance()), cambio(20), "Verificado", directorio()));
    }
    @Test void noSePermiteEliminarRecursoNecesarioParaTerceros() {
        assertThrows(IllegalArgumentException.class, () -> new CambioRecurso(RECURSO, TipoRecurso.ACTA,
                Tratamiento.ELIMINAR_RECURSO, Set.of(), Map.of(), Set.of("tercero")));
    }
    @Test void camposDeTercerosNoSonObjetivosDeCambio() {
        assertThrows(IllegalArgumentException.class, () -> new CambioRecurso(RECURSO, TipoRecurso.REPORTE,
                Tratamiento.RETIRAR_CAMPOS, Set.of("tercero"), Map.of(), Set.of("tercero")));
    }
    @Test void manifiestoNoAdmiteRecursosNiCamposDesconocidos() {
        assertThrows(IllegalArgumentException.class, () -> preparar(plan(), List.of(originales().getFirst()), List.of(), autorizacion(alcance()), directorio()));
        assertThrows(IllegalArgumentException.class, () -> preparar(plan(), List.of(new Representacion(RECURSO, TipoRecurso.REPORTE,
                Map.of("documento", "12345678")), originales().getLast()), List.of(), autorizacion(alcance()), directorio()));
    }
    @Test void evidenciaTieneMinimoDosAniosYSoloMetadatosTipados() {
        var e = evento(1, T, RegistroEvidencia.Operacion.CONSULTA);
        assertEquals(Instant.parse("2028-10-21T15:00:00Z"), e.conservacionMinimaHasta());
        assertThrows(IllegalArgumentException.class, () -> new RegistroEvidencia.Criterios(Optional.of(T.plusSeconds(1)),
                Optional.of(T), Optional.empty(), Set.of(), Set.of()));
    }
    @Test void evidenciaSeExtraeConAutorizacionYRegistraSuPropiaExtraccion() {
        var r = RegistroEvidencia.vacio().registrar(evento(1, T.plusSeconds(1), RegistroEvidencia.Operacion.CONSULTA));
        var x = r.extraer(id(102), new RegistroEvidencia.Filtro(T, T.plusSeconds(10), Optional.of(ADMIN), Set.of()), alcance(),
                autorizacion(alcance()), cambio(20), directorio(), RestriccionesAmbito.vacias(), ControlProteccion.vacio());
        assertEquals(1, x.eventos().size());
        var x2 = x.actualizado().extraer(id(103), new RegistroEvidencia.Filtro(T, T.plusSeconds(21), Optional.empty(),
                Set.of(RegistroEvidencia.Operacion.EXTRACCION_EVIDENCIA)), alcance(), autorizacion(alcance()), cambio(30),
                directorio(), RestriccionesAmbito.vacias(), ControlProteccion.vacio());
        assertEquals(1, x2.eventos().size());
        assertEquals(id(102), x2.eventos().getFirst().id());
    }
    @Test void evidenciaBloqueadaNoSeFiltraMedianteExtraccion() {
        var r = RegistroEvidencia.vacio().registrar(evento(1, T.plusSeconds(1), RegistroEvidencia.Operacion.CONSULTA));
        var control = ControlProteccion.vacio().bloquear(id(4), AlcanceProteccion.persona(PERSONA), autorizacion(alcance()), cambio(10), directorio());
        var x = r.extraer(id(102), new RegistroEvidencia.Filtro(T, T.plusSeconds(10), Optional.empty(), Set.of()), alcance(),
                autorizacion(alcance()), cambio(20), directorio(), RestriccionesAmbito.vacias(), control);
        assertTrue(x.eventos().isEmpty());
    }
    @Test void bloqueoDelIdDeEvidenciaImpideSuExtraccionAunqueLasFuentesSeanDisponibles() {
        var evento = evento(1, T.plusSeconds(1), RegistroEvidencia.Operacion.CONSULTA);
        var r = RegistroEvidencia.vacio().registrar(evento);
        var alcanceEvento = new AlcanceProteccion(Optional.empty(), Set.of(), Set.of(evento.id()));
        var control = ControlProteccion.vacio().bloquear(id(4), alcanceEvento, autorizacion(alcanceEvento), cambio(10), directorio());
        var x = r.extraer(id(102), new RegistroEvidencia.Filtro(T, T.plusSeconds(10), Optional.empty(), Set.of()), alcance(),
                autorizacion(alcance()), cambio(20), directorio(), RestriccionesAmbito.vacias(), control);
        assertTrue(x.eventos().isEmpty());
    }
    @Test void criteriosNoPuedenOcultarPersonasFueraDelAlcanceAutorizable() {
        var criterios = new RegistroEvidencia.Criterios(Optional.empty(), Optional.empty(), Optional.empty(), Set.of(), Set.of(id(99)));
        assertThrows(IllegalArgumentException.class, () -> new RegistroEvidencia.Evento(id(101), ADMIN, T,
                RegistroEvidencia.Operacion.CONSULTA, RegistroEvidencia.Resultado.CORRECTO,
                alcance(), criterios, Optional.empty()));
    }
    @Test void evidenciaRechazaIdDuplicadoYRetrocesoTemporal() {
        var r = RegistroEvidencia.vacio().registrar(evento(1, T.plusSeconds(2), RegistroEvidencia.Operacion.CONSULTA));
        assertSame(r, r.registrar(evento(1, T.plusSeconds(2), RegistroEvidencia.Operacion.CONSULTA)));
        assertThrows(IllegalArgumentException.class, () -> r.registrar(evento(1, T.plusSeconds(3), RegistroEvidencia.Operacion.EXPORTACION)));
        assertThrows(IllegalArgumentException.class, () -> r.registrar(evento(2, T, RegistroEvidencia.Operacion.EXPORTACION)));
    }
    @Test void reconstitucionRechazaDuplicadosYConservaOrden() {
        var e = evento(1, T, RegistroEvidencia.Operacion.CONSULTA);
        assertThrows(IllegalArgumentException.class, () -> RegistroEvidencia.cargar(List.of(e, e)));
        assertThrows(IllegalArgumentException.class, () -> RegistroEvidencia.cargar(List.of(evento(2, T.plusSeconds(1), RegistroEvidencia.Operacion.CREACION), e)));
    }
    @Test void retiroEvidenciaEsExpresoYNoPuedeIgnorarMinimoDosAnios() {
        var e = evento(1, T, RegistroEvidencia.Operacion.CONSULTA);
        var r = RegistroEvidencia.cargar(List.of(e));
        var scope = new AlcanceProteccion(Optional.empty(), Set.of(), Set.of(e.id()));
        var fecha = Instant.parse("2028-10-21T15:00:00Z");
        var autorizacion = new AutorizacionProteccion(id(700), ADMIN, Set.of(AutorizacionProteccion.Operacion.DISPONER), scope,
                new Vigencia(T, Optional.empty()), "Responsable", "Constancia retiro evidencia", "Plazo institucional cumplido", T);
        assertThrows(IllegalStateException.class, () -> r.retirarVencidos(Set.of(e.id()), id(701), autorizacion,
                new RegistroCambio(ADMIN, fecha.minusSeconds(1), Optional.of("Retiro expresamente decidido")), directorio()));
        var actual = r.retirarVencidos(Set.of(e.id()), id(701), autorizacion,
                new RegistroCambio(ADMIN, fecha, Optional.of("Retiro expresamente decidido")), directorio());
        var grant = new AutorizacionProteccion(id(702), ADMIN, Set.of(AutorizacionProteccion.Operacion.EXTRAER_EVIDENCIA), alcance(),
                new Vigencia(T, Optional.empty()), "Responsable", "Constancia extracción", "Comprobación posterior", T);
        var salida = actual.extraer(id(703), new RegistroEvidencia.Filtro(T, fecha, Optional.empty(), Set.of(RegistroEvidencia.Operacion.CONSULTA)),
                alcance(), grant, new RegistroCambio(ADMIN, fecha.plusSeconds(1), Optional.empty()), directorio(),
                RestriccionesAmbito.vacias(), ControlProteccion.vacio());
        assertTrue(salida.eventos().isEmpty());
    }
    static RegistroEvidencia.Evento evento(int numero, Instant fecha, RegistroEvidencia.Operacion operacion) {
        return new RegistroEvidencia.Evento(id(100 + numero), ADMIN, fecha, operacion, RegistroEvidencia.Resultado.CORRECTO,
                alcance(), RegistroEvidencia.Criterios.ninguno(), Optional.empty());
    }
}
