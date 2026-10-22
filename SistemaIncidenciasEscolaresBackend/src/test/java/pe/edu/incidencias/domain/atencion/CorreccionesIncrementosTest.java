package pe.edu.incidencias.domain.atencion;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.alerta.*;
import pe.edu.incidencias.domain.documento.*;
import pe.edu.incidencias.domain.incidencia.*;
import pe.edu.incidencias.domain.institucion.DatosInstitucion;
import pe.edu.incidencias.domain.catalogo.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.atencion.DatosPruebaAtencion.*;

class CorreccionesIncrementosTest {
    private RegistroAtenciones dosCasos() { return abrir(abrir(base(), 200, Set.of(id(100)), 100), 201, Set.of(id(101)), 110); }
    private RegistroCambio director(int t) { return new RegistroCambio(DIRECTOR, reloj(t), Optional.empty()); }
    private RegistroAlertas alertas(RegistroAtenciones registro) {
        var resultado = RegistroAlertas.vacio();
        for (int n : List.of(100, 101)) resultado = resultado.evaluarHecho(registro.hechoVigente(id(n)), registro.contextoDeHechos(Set.of(id(n))),
                new PoliticaAcceso(directorio(), registro.restricciones()), cambio(120));
        return resultado;
    }
    private GeneradorInformeCaso generador(RegistroAtenciones r, RegistroActas actas) {
        return new GeneradorInformeCaso(r, actas, ReferenciasOficiales.vacias(), directorio(), Map.of());
    }
    private InformeCaso informe(GeneradorInformeCaso g, int t) {
        return g.previsualizar(id(200), new DatosInstitucion("Colegio ficticio", Optional.empty(), Optional.empty(), Optional.empty()),
                "Atención institucional", DestinoInforme.interno(), Optional.empty(), Map.of(), TOE, reloj(t), reloj(t));
    }

    @Test void dosAtencionesExigenElegirContinuidadSinMutarNinguna() {
        var original = dosCasos();
        assertThrows(IllegalArgumentException.class, () -> original.relacionarDuplicados(id(100), id(101), Optional.empty(), directorio(), cambio(130)));
        assertEquals(1, original.hechoVigente(id(100)).reportes().size());
        assertEquals(Set.of(id(101)), original.caso(id(201)).orElseThrow().hechosActuales());
    }

    @Test void duplicadosAtendidosConservanFuentesActuacionesCompromisosYUnaSolaAtencionVigente() {
        var compromiso = Compromiso.acordar(id(400), "Acuerdo propio del caso anterior", estudiante(), Optional.empty(), true, cambio(120));
        var anterior = dosCasos().registrarActuacion(id(201), realizada(301, List.of(compromiso), 120), directorio(), cambio(120));
        var nuevo = anterior.relacionarDuplicados(id(100), id(101), Optional.of(id(200)), directorio(), cambio(130));
        assertEquals(id(100), nuevo.hechoCanonico(id(101)));
        assertEquals(2, nuevo.hechoVigente(id(100)).reportes().size());
        assertSame(anterior.caso(id(201)).orElseThrow().actuaciones().getFirst(), nuevo.caso(id(201)).orElseThrow().actuaciones().getFirst());
        assertEquals(Compromiso.Estado.ACORDADO, nuevo.caso(id(201)).orElseThrow().actuaciones().getFirst().compromisos().getFirst().estado());
        assertTrue(nuevo.caso(id(200)).orElseThrow().actuaciones().isEmpty());
        assertEquals(CasoAtencion.Estado.PENDIENTE, nuevo.caso(id(201)).orElseThrow().estado());
        assertTrue(nuevo.caso(id(201)).orElseThrow().hechosActuales().isEmpty());
        assertEquals(1, nuevo.hechosDelCaso(id(201), TOE, directorio(), reloj(140)).size());
        assertThrows(IllegalStateException.class, () -> abrir(nuevo, 202, Set.of(id(101)), 150));
        assertEquals(id(101), nuevo.duplicidades().getFirst().origen().id());
    }

    @Test void duplicidadSinOtrosPendientesCierraAnteriorYActualizaAvisosSinOtroTramite() {
        var original = dosCasos();
        var avisos = alertas(original);
        var conjunto = SeguimientoInstitucional.relacionarDuplicados(original, avisos, id(100), id(101), Optional.of(id(200)), directorio(), cambio(130));
        assertEquals(CierreLocal.TipoCierre.DUPLICIDAD, conjunto.atenciones().caso(id(201)).orElseThrow().cierre().orElseThrow().tipo());
        assertTrue(conjunto.alertas().alertas().stream().noneMatch(Alerta::pendiente));
        assertTrue(conjunto.alertas().alertas().stream().anyMatch(a -> a.disposicion().filter(d -> d.tipo() == DisposicionAlerta.Tipo.DUPLICIDAD).isPresent()));
        assertEquals(avisos.notificaciones().size(), conjunto.alertas().notificaciones().size());
        assertEquals(2, avisos.alertas().stream().filter(Alerta::pendiente).count());
    }

    @Test void puedeElegirseLaAtencionDelReporteDuplicadoYMantenerOtrosHechosEnElAnterior() {
        var original = abrir(abrir(base(), 200, Set.of(id(100), id(102)), 100), 201, Set.of(id(101)), 110);
        var nuevo = original.relacionarDuplicados(id(100), id(101), Optional.of(id(201)), directorio(), cambio(130));
        assertEquals(Set.of(id(100)), nuevo.caso(id(201)).orElseThrow().hechosActuales());
        assertEquals(Set.of(id(102)), nuevo.caso(id(200)).orElseThrow().hechosActuales());
        assertEquals(CasoAtencion.Estado.PENDIENTE, nuevo.caso(id(200)).orElseThrow().estado());
    }

    @Test void atencionesCerradasSeRelacionanSinReabrirNiCopiarActuaciones() {
        var original = dosCasos().cerrarCaso(id(200), conclusion(120), directorio(), cambio(120))
                .cerrarCaso(id(201), conclusion(125), directorio(), cambio(125));
        var nuevo = original.relacionarDuplicados(id(100), id(101), Optional.empty(), directorio(), cambio(130));
        assertTrue(nuevo.casos().stream().allMatch(c -> c.estado() == CasoAtencion.Estado.CERRADA));
        assertEquals(2, nuevo.hechosDelCaso(id(201), TOE, directorio(), reloj(140)).getFirst().reportes().size());
        assertEquals(original.caso(id(201)).orElseThrow().cierre(), nuevo.caso(id(201)).orElseThrow().cierre());
    }

    @Test void corregirDuplicidadExigeCausaYConservaFuentesSinReaperturaAutomatica() {
        var unido = dosCasos().relacionarDuplicados(id(100), id(101), Optional.of(id(200)), directorio(), cambio(130));
        assertThrows(IllegalArgumentException.class, () -> unido.separarDuplicado(id(101), Optional.empty(), directorio(), cambio(140)));
        var separado = unido.separarDuplicado(id(101), Optional.empty(), directorio(), motivo(140));
        assertEquals(id(101), separado.hechoCanonico(id(101)));
        assertEquals(1, separado.hechoVigente(id(100)).reportes().size());
        assertEquals(1, separado.hechoVigente(id(101)).reportes().size());
        assertTrue(separado.duplicidades().getFirst().correccion().isPresent());
        assertEquals(CasoAtencion.Estado.CERRADA, separado.caso(id(201)).orElseThrow().estado());
        assertFalse(separado.caso(id(200)).orElseThrow().antecedentes().contains(id(201)));
        var historia = generador(separado, RegistroActas.vacio()).previsualizar(id(201),
                new DatosInstitucion("Colegio ficticio", Optional.empty(), Optional.empty(), Optional.empty()), "Historia de atención", DestinoInforme.interno(),
                Optional.empty(), Map.of(), TOE, reloj(150), reloj(150));
        assertEquals(Optional.of(reloj(140)), historia.gobierno().continuidades().getFirst().corregidaEl());
    }

    @Test void restriccionDelDuplicadoProtegeAntesYDespuesDeSepararSinContagiarElOtroHecho() {
        var original = dosCasos();
        var politica = new PoliticaAcceso(directorio(), original.restricciones());
        var ledger = politica.excluir(id(900), DIRECTOR, ContextoAcceso.deHecho(original.hechoVigente(id(101))), motivo(120));
        original = RegistroAtenciones.cargarAmbito(List.of(original.hechoVigente(id(100)), original.hechoVigente(id(101)), original.hechoVigente(id(102))),
                original.casos(), Map.of(), ledger, List.of(), original.indice());
        var unido = original.relacionarDuplicados(id(100), id(101), Optional.of(id(200)), directorio(), cambio(130));
        assertThrows(SecurityException.class, () -> unido.consultarCaso(id(200), DIRECTOR, directorio(), reloj(135)));
        var separado = unido.separarDuplicado(id(101), Optional.empty(), directorio(), motivo(140));
        assertNotNull(separado.consultarCaso(id(200), DIRECTOR, directorio(), reloj(150)));
        assertThrows(SecurityException.class, () -> separado.consultarCaso(id(201), DIRECTOR, directorio(), reloj(150)));
        assertEquals(1, separado.restricciones().exclusiones().size());
    }

    @Test void corregirVinculoConActuacionQueContieneLaDenunciaConservaEsaReserva() {
        var original = abrir(base(), 200, Set.of(id(100), id(101)), 100)
                .registrarActuacion(id(200), realizada(300, List.of(), 110), directorio(), cambio(110));
        var ledger = new PoliticaAcceso(directorio(), original.restricciones()).excluir(id(900), DIRECTOR,
                ContextoAcceso.deHecho(original.hechoVigente(id(101))), motivo(120));
        var cargado = RegistroAtenciones.cargarAmbito(List.of(original.hechoVigente(id(100)), original.hechoVigente(id(101))), original.casos(),
                Map.of(), ledger, List.of(), original.indice());
        var corregido = cargado.retirarVinculo(id(200), id(101), directorio(), motivo(130));
        assertThrows(SecurityException.class, () -> corregido.consultarCaso(id(200), DIRECTOR, directorio(), reloj(140)));
        assertTrue(corregido.contexto(id(200)).ambito().hechosIds().contains(id(101)));
        assertFalse(new PoliticaAcceso(directorio(), corregido.restricciones()).evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO,
                corregido.contextoDeHechos(Set.of(id(100))), reloj(140)).resultado() == PoliticaAcceso.Resultado.EXCLUIDO);
    }

    @Test void informeYActaAnterioresConservanReservaAunqueSeCorrijaElVinculoSinActuaciones() {
        var original = abrir(base(), 200, Set.of(id(100), id(101)), 100);
        var acta = ActaAdjunta.incorporar(id(800), id(200), HOY, "Acta relacionada", new ArchivoAdjunto(id(801), "acta.pdf", "application/pdf", 100), director(110));
        var actas = RegistroActas.vacio().registrar(acta, original, directorio(), Optional.empty());
        var emitido = generador(original, actas).emitir(informe(generador(original, actas), 115), id(850), cambio(115));
        var ledger = new PoliticaAcceso(directorio(), original.restricciones()).excluir(id(900), DIRECTOR,
                ContextoAcceso.deHecho(original.hechoVigente(id(101))), motivo(120));
        var cargado = RegistroAtenciones.cargarAmbito(List.of(original.hechoVigente(id(100)), original.hechoVigente(id(101))), original.casos(),
                Map.of(), ledger, List.of(), original.indice());
        var corregido = cargado.retirarVinculo(id(200), id(101), directorio(), motivo(130));
        assertNotNull(corregido.consultarCaso(id(200), DIRECTOR, directorio(), reloj(140)));
        assertThrows(SecurityException.class, () -> generador(corregido, actas).consultarEmitido(emitido, DIRECTOR, reloj(140)));
        assertThrows(SecurityException.class, () -> actas.archivoParaDescarga(id(800), id(801), DIRECTOR, reloj(140), corregido, directorio()));
    }

    @Test void comiteGuardaReunionDecisionesActuacionCompromisoYCierreEnUnaOperacion() {
        var original = abrir(base(), 200, Set.of(id(100)), 100).convocarComite(id(200), id(600), HOY, "Especial gravedad", Optional.empty(), directorio(), cambio(110));
        var acuerdo = Compromiso.acordar(id(400), "Acuerdo de la intervención", estudiante(), Optional.empty(), false, director(130));
        var accion = Actuacion.realizada(id(300), "Conversación y resultado", HOY, DIRECTOR, Set.of(id(100)), Set.of(), List.of(acuerdo), director(130));
        var cierre = new CierreLocal(HOY, "Atención concluida", CierreLocal.TipoCierre.CONCLUSION, director(130), Optional.empty());
        var nuevo = original.registrarIntervencionComite(id(200), id(601), id(600), reloj(120), Set.of(DIRECTOR, COMITE), Optional.empty(),
                Map.of(id(602), "Decisión adoptada"), List.of(accion), Optional.of(cierre), directorio(), director(130));
        var caso = nuevo.caso(id(200)).orElseThrow();
        assertEquals(1, caso.intervencionesComite().size());
        assertEquals(1, caso.decisionesComite().size());
        assertEquals(3, caso.actosComite().size());
        assertEquals(CasoAtencion.Estado.CERRADA, caso.estado());
        assertEquals(Compromiso.Estado.ACORDADO, caso.actuaciones().getFirst().compromisos().getFirst().estado());
        assertTrue(original.caso(id(200)).orElseThrow().intervencionesComite().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> original.registrarIntervencionComite(id(200), id(601), id(600), reloj(120), Set.of(DIRECTOR), Optional.empty(),
                Map.of(id(602), "Decisión"), List.of(accion, accion), Optional.empty(), directorio(), director(130)));
        assertTrue(original.caso(id(200)).orElseThrow().decisionesComite().isEmpty());
    }

    @Test void indiceEvitaDobleAtencionYConservaExclusionSinCargarElOtroCaso() {
        var completo = abrir(base(), 200, Set.of(id(100)), 100).excluirUsuario(id(200), id(900), DIRECTOR, directorio(), motivo(110));
        var parcial = RegistroAtenciones.cargarAmbito(List.of(completo.hechoVigente(id(100)), completo.hechoVigente(id(101))), List.of(),
                Map.of(), completo.restricciones(), List.of(), completo.indice());
        assertTrue(parcial.casos().isEmpty());
        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO, new PoliticaAcceso(directorio(), parcial.restricciones()).evaluar(DIRECTOR,
                Permiso.CONSULTAR_HECHO, parcial.contextoDeHechos(Set.of(id(100))), reloj(120)).resultado());
        assertThrows(IllegalStateException.class, () -> abrir(parcial, 201, Set.of(id(100)), 120));
        var otro = abrir(parcial, 201, Set.of(id(101)), 120);
        assertEquals(1, otro.casos().size());
        assertEquals(id(200), otro.indice().pendientesPorHecho().get(id(100)));
    }

    @Test void coordinacionAcotadaNoCargaSenalesNiAtencionesIndependientes() {
        var completo = dosCasos();
        var avisos = alertas(completo);
        var h = completo.hechoVigente(id(100));
        var parcial = RegistroAtenciones.cargarAmbito(List.of(h), List.of(completo.caso(id(200)).orElseThrow()), Map.of(), completo.restricciones(), List.of(), completo.indice());
        var actualizado = parcial.registrarActuacion(id(200), realizada(300, List.of(), 130), directorio(), cambio(130));
        var resultado = SeguimientoInstitucional.actualizar(actualizado, avisos, Set.of(id(100)), directorio(), cambio(130));
        assertEquals(1, resultado.alertas().alertas().stream().filter(Alerta::pendiente).count());
        var pertinentes = avisos.alertas().stream().filter(a -> a.hechosHistoricos().contains(id(100))).toList();
        var ids = pertinentes.stream().map(Alerta::id).collect(java.util.stream.Collectors.toSet());
        var cargadas = RegistroAlertas.cargarAmbito(pertinentes, avisos.notificaciones().stream().filter(n -> ids.contains(n.alertaId())).toList(), Set.of());
        assertEquals(1, cargadas.alertas().size());
    }

    @Test void rectificarDuplicidadRestauraSenalSinReabrirCasoNiBorrarSuGestionAnterior() {
        var original = dosCasos();
        var avisos = alertas(original);
        var unificado = SeguimientoInstitucional.relacionarDuplicados(original, avisos, id(100), id(101), Optional.of(id(200)), directorio(), cambio(130));
        var corregido = SeguimientoInstitucional.separarDuplicado(unificado.atenciones(), unificado.alertas(), id(101), Optional.empty(), directorio(), motivo(140));
        var restaurada = corregido.alertas().alertas().stream().filter(a -> a.hechosVigentes().equals(Set.of(id(101)))).findFirst().orElseThrow();
        assertTrue(restaurada.pendiente());
        assertEquals(reloj(140), restaurada.pendienteDesde());
        assertTrue(restaurada.revisiones().stream().anyMatch(r -> r.disposicionRectificada().filter(d -> d.tipo() == DisposicionAlerta.Tipo.DUPLICIDAD).isPresent()));
        assertEquals(CasoAtencion.Estado.CERRADA, corregido.atenciones().caso(id(201)).orElseThrow().estado());
        assertEquals(avisos.notificaciones().size(), corregido.alertas().notificaciones().size());
    }

    @Test void recalculaReiteracionAlUnificarYSepararSinPerderVentanaNiFuentesEmitidas() {
        var registro = RegistroAtenciones.iniciar(List.of(hecho(100, PapelParticipacion.PRESUNTA_RESPONSABILIDAD),
                hecho(101, PapelParticipacion.PRESUNTA_RESPONSABILIDAD)), RestriccionesAmbito.vacias());
        var categoria = Categoria.crear(id(80), new DefinicionCategoria("Protección ficticia", "Categoría de prueba", NaturalezaCategoria.VIOLENCIA_PROTECCION,
                Gravedad.GRAVE, true, true, OptionalInt.of(2)), TOE, reloj(0));
        var parametros = ParametrosReiteracion.sinConfigurar(TOE, reloj(0)).configurarVentana(10, TOE, reloj(0));
        var contexto = registro.contextoDeHechos(Set.of(id(100), id(101)));
        var avisos = RegistroAlertas.vacio().evaluarReiteracion(ESTUDIANTE, categoria, parametros, contexto,
                new PoliticaAcceso(directorio(), registro.restricciones()), cambio(30));
        var unido = SeguimientoInstitucional.relacionarDuplicados(registro, avisos, id(100), id(101), Optional.empty(), directorio(), cambio(40));
        var reevaluada = unido.alertas().alertas().stream().flatMap(a -> a.fundamentosVigentes().values().stream()).filter(f -> f.reiteracion().isPresent()).findFirst().orElseThrow();
        assertEquals(1, reevaluada.reiteracion().get().cantidad());
        assertFalse(reevaluada.motivos().contains(MotivoAlerta.REITERACION));
        var separado = SeguimientoInstitucional.separarDuplicado(unido.atenciones(), unido.alertas(), id(101), Optional.empty(), directorio(), motivo(50));
        var actual = separado.alertas().alertas().stream().flatMap(a -> a.fundamentosVigentes().values().stream()).filter(f -> f.reiteracion().isPresent())
                .max(Comparator.comparing(FundamentoAlerta::evaluadoEn)).orElseThrow();
        assertEquals(2, actual.reiteracion().get().cantidad());
        assertEquals(10, actual.reiteracion().get().ventanaDias());
        assertTrue(separado.alertas().alertas().stream().flatMap(a -> a.revisiones().stream()).flatMap(r -> r.fundamentos().values().stream())
                .anyMatch(f -> f.reiteracion().filter(r -> r.cantidad() == 2 && r.evaluadoEl().equals(HOY)).isPresent()));
    }

    @Test void derivacionPreparadaAntesDeIdentificarDuplicadoConservaCoberturaDelMismoHecho() {
        var preparada = Derivacion.preparada(id(700), Set.of(id(101)), Derivacion.Supuesto.VIOLENCIA_FAMILIAR,
                new Derivacion.Destino(Derivacion.TipoDestino.FISCALIA, "Fiscalía"), "Protección", Optional.empty(), cambio(120));
        var original = dosCasos().registrarDerivacion(id(201), preparada, directorio(), cambio(120));
        var unido = original.relacionarDuplicados(id(100), id(101), Optional.of(id(201)), directorio(), cambio(130));
        var efectuada = preparada.efectuar(HOY, "Derivación efectuada", Optional.empty(), cambio(140));
        var nuevo = unido.registrarDerivacion(id(201), efectuada, directorio(), cambio(140));
        assertEquals(CierreLocal.TipoCierre.DERIVACION_FAMILIAR_FISCALIA, nuevo.caso(id(201)).orElseThrow().cierre().orElseThrow().tipo());
        assertEquals(Set.of(id(101)), nuevo.caso(id(201)).orElseThrow().derivaciones().getFirst().hechosIds());
    }

    @Test void denunciaPreventivaQuedaAncladaALaFuenteYNoAlAsuntoVinculadoPorError() {
        var personal = new PersonaInvolucrada(id(90), TipoPersona.PERSONAL, "Personal denunciado", Optional.of(DIRECTOR), Optional.of("Función"));
        var denunciado = Hecho.de(reporte(101, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, personal));
        var original = abrir(RegistroAtenciones.iniciar(List.of(hecho(100, PapelParticipacion.AFECTADO), denunciado), RestriccionesAmbito.vacias()),
                200, Set.of(id(100), id(101)), 100);
        assertThrows(SecurityException.class, () -> original.consultarCaso(id(200), DIRECTOR, directorio(), reloj(110)));
        var corregido = original.retirarVinculo(id(200), id(101), directorio(), motivo(120));
        assertNotNull(corregido.consultarCaso(id(200), DIRECTOR, directorio(), reloj(130)));
        assertEquals(PoliticaAcceso.Resultado.EXCLUIDO, new PoliticaAcceso(directorio(), corregido.restricciones()).evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO,
                corregido.contextoDeHechos(Set.of(id(101))), reloj(130)).resultado());
        assertEquals(Set.of(id(101)), corregido.restricciones().exclusiones().getFirst().ambito().reportesIds());
    }

    @Test void separarConservaCompromisoInmediatoPendienteYLaAtencionQueSeguIaAbierta() {
        var propio = new RegistroCambio(DOCENTE, reloj(50), Optional.empty());
        var acuerdo = Compromiso.acordar(id(400), "Acuerdo inmediato pendiente", estudiante(), Optional.empty(), true, propio);
        var accion = Actuacion.realizada(id(300), "Intervención inmediata", HOY, DOCENTE, Set.of(id(101)), Set.of(), List.of(acuerdo), propio);
        var registro = RegistroAtenciones.iniciar(List.of(hechoLeve(100), hechoLeve(101)), RestriccionesAmbito.vacias())
                .registrarInmediata(id(101), accion, directorio(), propio);
        registro = abrir(abrir(registro, 200, Set.of(id(100)), 100), 201, Set.of(id(101)), 110);
        var unido = registro.relacionarDuplicados(id(100), id(101), Optional.of(id(200)), directorio(), cambio(130));
        var separado = unido.separarDuplicado(id(101), Optional.empty(), directorio(), motivo(140));
        assertEquals(CasoAtencion.Estado.PENDIENTE, separado.caso(id(201)).orElseThrow().estado());
        assertEquals(Set.of(id(101)), separado.caso(id(201)).orElseThrow().hechosActuales());
        assertSame(accion, separado.actuacionesDelCaso(id(201), TOE, directorio(), reloj(150)).getFirst());
        assertTrue(separado.caso(id(201)).orElseThrow().actuaciones().isEmpty());
    }

    @Test void separarRetieneLasCorreccionesRecibidasMientrasLosReportesEstuvieronRelacionados() {
        var unido = dosCasos().relacionarDuplicados(id(100), id(101), Optional.of(id(200)), directorio(), cambio(130));
        var hecho = unido.hechoVigente(id(100));
        var reporte = hecho.reportes().stream().filter(r -> r.id().equals(id(101))).findFirst().orElseThrow();
        var d = reporte.datos();
        var corregido = new DatosReporte(d.fechaHecho(), d.anioEscolar(), d.clasificacion(), "Aporte rectificado sin perder su original", d.participaciones(),
                d.solicitaAtencion(), d.hora(), d.lugar(), d.comunicante(), d.canal());
        var actualizado = unido.actualizarHecho(hecho.actualizarReporte(reporte.corregirInstitucionalmente(corregido, motivo(140))), directorio(), cambio(140));
        var separado = actualizado.separarDuplicado(id(101), Optional.empty(), directorio(), motivo(150));
        assertEquals(2, separado.hechoVigente(id(101)).principal().revisiones().size());
        assertEquals(corregido.descripcion(), separado.hechoVigente(id(101)).principal().datos().descripcion());
        assertEquals(reporte.revisiones().getFirst(), separado.hechoVigente(id(101)).principal().revisiones().getFirst());
        assertEquals(1, separado.duplicidades().getFirst().origen().principal().revisiones().size());
    }

    @Test void quienEstaExcluidoDeUnaAtencionNoRelacionaSusReportesParaRecuperarAcceso() {
        var original = dosCasos().excluirUsuario(id(201), id(900), DIRECTOR, directorio(), motivo(120));
        assertThrows(SecurityException.class, () -> original.relacionarDuplicados(id(100), id(101), Optional.of(id(200)), directorio(), director(130)));
        assertTrue(original.duplicidades().isEmpty());
        assertEquals(id(101), original.hechoVigente(id(101)).id());
    }

    @Test void relacionarAutorizaTodoElCasoQueModificaAunqueElConflictoEsteEnOtroHechoDeEseCaso() {
        var personal = new PersonaInvolucrada(id(90), TipoPersona.PERSONAL, "Personal referido", Optional.of(DIRECTOR), Optional.of("Función"));
        var h = Hecho.de(reporte(102, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, personal));
        var original = RegistroAtenciones.iniciar(List.of(hecho(100, PapelParticipacion.AFECTADO), hecho(101, PapelParticipacion.AFECTADO), h), RestriccionesAmbito.vacias());
        original = abrir(abrir(original, 200, Set.of(id(100)), 100), 201, Set.of(id(101), id(102)), 110);
        var previo = original;
        assertThrows(SecurityException.class, () -> previo.relacionarDuplicados(id(100), id(101), Optional.of(id(200)), directorio(), director(130)));
        assertTrue(original.duplicidades().isEmpty());
        assertTrue(new PoliticaAcceso(directorio(), original.restricciones()).evaluar(DIRECTOR, Permiso.CONSULTAR_HECHO,
                original.contextoDeHechos(Set.of(id(100))), reloj(130)).permitida());
    }
}
