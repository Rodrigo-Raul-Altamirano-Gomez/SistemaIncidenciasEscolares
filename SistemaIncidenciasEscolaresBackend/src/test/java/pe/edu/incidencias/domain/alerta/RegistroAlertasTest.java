package pe.edu.incidencias.domain.alerta;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.catalogo.*;
import pe.edu.incidencias.domain.incidencia.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.alerta.DatosPruebaAlertas.*;

class RegistroAlertasTest {
    @Test
    void gravedadDesdePrimeraComunicacionAunqueElPapelSeaDesconocido() {
        var hecho = grave(100);
        var registro = evaluar(RegistroAlertas.vacio(), hecho, 20);
        assertEquals(Set.of(MotivoAlerta.GRAVEDAD), unica(registro).motivos());
        assertEquals(Set.of(hecho.id()), unica(registro).hechosVigentes());
        assertEquals(1, registro.notificaciones().size());
        assertTrue(registro.notificaciones().stream().noneMatch(n -> n.destinatarioId().equals(DOCENTE)));
        assertEquals(1, hecho.principal().revisiones().size());
        assertFalse(registro.requiereDesignacion());
    }

    @Test
    void solicitudManualNoConvierteNotaLeveEnGraveNiDependeDeParametros() {
        var hecho = hecho(100, cotidiana(), PapelParticipacion.DESCONOCIDO, true);
        var registro = evaluar(RegistroAlertas.vacio(), hecho, 20);
        assertEquals(Set.of(MotivoAlerta.RIESGO_COMUNICADO), unica(registro).motivos());
        assertEquals(Gravedad.LEVE, hecho.clasificacionVigente().orElseThrow().gravedad());
        registro = registro.evaluarReiteracion(ESTUDIANTE, cotidiana(), ParametrosReiteracion.sinConfigurar(TOE, BASE),
                contexto(List.of(hecho)), politica(), cambio(30));
        assertTrue(unica(registro).pendiente());
        assertEquals(1, registro.notificaciones().size());
    }

    @Test
    void notaSinSenalNoCreaPendiente() {
        var registro = evaluar(RegistroAlertas.vacio(), leve(100), 20);
        assertTrue(registro.alertas().isEmpty());
        assertTrue(registro.notificaciones().isEmpty());
    }

    @Test
    void discrepanciaNoRetrasaGravedadYLaPrecisionInstitucionalRecalculaSinBorrarFuentes() {
        var hecho = grave(100).vincularDuplicado(leve(101).principal(), motivo(20));
        var registro = evaluar(RegistroAlertas.vacio(), hecho, 30);
        var emitido = unica(registro).revisiones().getFirst();
        assertEquals(2, emitido.fundamentos().values().iterator().next().clasificaciones().size());
        var preciso = hecho.precisarDatos(FechaHecho.exacta(HOY), cotidiana().clasificacionParaNuevoHecho(), motivo(40));
        var recalculado = evaluar(registro, preciso, 50);
        assertFalse(unica(recalculado).pendiente());
        assertEquals(emitido, unica(recalculado).revisiones().getFirst());
        assertEquals(2, preciso.reportes().size());
        assertEquals(Gravedad.GRAVE, preciso.reportes().getFirst().datos().clasificacion().gravedad());
    }

    @Test
    void principalAnuladoNoRetiraSenalSostenidaPorOtroReporteValido() {
        var principal = grave(100);
        var unido = principal.vincularDuplicado(grave(101).principal(), motivo(20));
        var registro = evaluar(RegistroAlertas.vacio(), unido, 30);
        var anulado = unido.actualizarReporte(unido.principal().anularPorError(motivo(40)));
        var nuevo = evaluar(registro, anulado, 50);
        assertTrue(unica(nuevo).pendiente());
        assertEquals(unica(registro).id(), unica(nuevo).id());
        assertEquals(1, nuevo.notificaciones().size());
    }

    @Test
    void recalculoYLecturaNoRepitenAvisosNiGestionanElAsunto() {
        var hecho = grave(100);
        var inicial = evaluar(RegistroAlertas.vacio(), hecho, 20);
        var aviso = inicial.notificaciones().stream().filter(n -> n.destinatarioId().equals(TOE)).findFirst().orElseThrow();
        var leido = inicial.leerNotificacion(aviso.id(), ContextoAcceso.deHecho(hecho), politica(), TOE, reloj(30));
        var repetido = evaluar(leido, hecho, 40);
        assertEquals(1, repetido.notificaciones().size());
        assertEquals(1, unica(repetido).revisiones().size());
        assertTrue(unica(repetido).pendiente());
        assertTrue(repetido.notificaciones().stream().filter(n -> n.id().equals(aviso.id())).findFirst().orElseThrow().leida());
        assertFalse(aviso.leida());
        assertThrows(SecurityException.class, () -> inicial.leerNotificacion(aviso.id(), ContextoAcceso.deHecho(hecho), politica(), DIRECTOR, reloj(30)));
    }

    @Test
    void motivosDeGravedadYReiteracionSeAcumulanEnUnSoloAsunto() {
        var cat = violencia();
        var hechos = List.of(hecho(100, cat, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, false),
                hecho(101, cat, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, false));
        var registro = evaluar(RegistroAlertas.vacio(), hechos.getFirst(), 20)
                .evaluarReiteracion(ESTUDIANTE, cat, parametros(), contexto(hechos), politica(), cambio(30));
        assertEquals(1, registro.alertas().size());
        assertEquals(Set.of(MotivoAlerta.GRAVEDAD, MotivoAlerta.REITERACION), unica(registro).motivos());
        assertEquals(1, registro.notificaciones().size());
        var fundamento = unica(registro).fundamentosVigentes().values().stream().flatMap(f -> f.reiteracion().stream()).findFirst().orElseThrow();
        assertEquals(2, fundamento.cantidad(PapelParticipacion.PRESUNTA_RESPONSABILIDAD));
        assertEquals(0, fundamento.cantidad(PapelParticipacion.ATRIBUCION_CORROBORADA));
    }

    @Test
    void umbralVentanaYHechosQuedanConservadosAlAnularOCambiarParametros() {
        var uno = leve(100);
        var dos = leve(101);
        var registro = reiterar(RegistroAlertas.vacio(), List.of(uno, dos), 20);
        var emitida = unica(registro).revisiones().getFirst();
        var anulado = dos.actualizarReporte(dos.principal().anularPorError(motivo(30)));
        var nuevo = reiterar(registro, List.of(uno, anulado), 40);
        assertFalse(unica(nuevo).pendiente());
        assertEquals(emitida, unica(nuevo).revisiones().getFirst());
        assertEquals(2, emitida.fundamentos().values().iterator().next().reiteracion().orElseThrow().umbral());
        var cambiado = nuevo.evaluarReiteracion(ESTUDIANTE, cotidiana(), parametros().retirarVentana(TOE, reloj(45)),
                contexto(List.of(uno, anulado)), politica(), cambio(50));
        assertFalse(unica(cambiado).pendiente());
        assertEquals(emitida, unica(cambiado).revisiones().getFirst());
    }

    @Test
    void novedadesMientrasEstaPendienteMantienenIdentidadYUnAvisoPorDestinatario() {
        var registro = reiterar(RegistroAlertas.vacio(), List.of(leve(100), leve(101)), 20);
        var ampliado = reiterar(registro, List.of(leve(100), leve(101), leve(102)), 30);
        assertEquals(unica(registro).id(), unica(ampliado).id());
        assertEquals(3, unica(ampliado).hechosVigentes().size());
        assertEquals(1, ampliado.notificaciones().size());
        assertEquals(2, unica(ampliado).revisiones().size());
    }

    @Test
    void trasGestionSoloUnHechoElegibleNuevoCreaOtroEpisodio() {
        var hechos = List.of(leve(100), leve(101));
        var primero = reiterar(RegistroAlertas.vacio(), hechos, 20);
        var gestionado = primero.disponer(unica(primero).id(), disposicion(30), contexto(hechos), politica());
        var repetido = reiterar(gestionado, hechos, 40);
        assertEquals(1, repetido.alertas().size());
        assertFalse(unica(repetido).pendiente());
        var nuevo = reiterar(repetido, List.of(leve(100), leve(101), leve(102)), 50);
        assertEquals(2, nuevo.alertas().size());
        var pendiente = nuevo.alertas().stream().filter(Alerta::pendiente).findFirst().orElseThrow();
        assertEquals(Set.of(id(102)), pendiente.hechosActivadores());
        assertEquals(3, pendiente.hechosVigentes().size());
        assertEquals(2, nuevo.notificaciones().size());
    }

    @Test
    void volverABajarYSubirElUmbralConLosMismosHechosNoRepiteAtencionGestionada() {
        var hechos = List.of(leve(100), leve(101));
        var inicial = reiterar(RegistroAlertas.vacio(), hechos, 20);
        var gestionado = inicial.disponer(unica(inicial).id(), disposicion(30), contexto(hechos), politica());
        var retirado = gestionado.evaluarReiteracion(ESTUDIANTE, cotidiana(), parametros().retirarVentana(TOE, reloj(35)), contexto(hechos), politica(), cambio(40));
        var deNuevo = reiterar(retirado, hechos, 50);
        assertEquals(1, deNuevo.alertas().size());
        assertFalse(unica(deNuevo).pendiente());
        assertEquals(1, deNuevo.notificaciones().size());
    }

    @Test
    void duplicadosConSenalesSeparadasSeRelacionanSinPerderHistoriaNiRepetirTarea() {
        var uno = grave(100);
        var dos = grave(101);
        var primero = evaluar(RegistroAlertas.vacio(), uno, 20);
        var dosSenales = evaluar(primero, dos, 30);
        var unido = uno.vincularDuplicado(dos.principal(), motivo(40));
        var consolidado = evaluar(dosSenales, unido, 50);
        assertEquals(2, consolidado.alertas().size());
        assertEquals(1, consolidado.alertas().stream().filter(Alerta::pendiente).count());
        var absorbida = consolidado.alertas().stream().filter(a -> a.disposicion().isPresent()).findFirst().orElseThrow();
        assertEquals(DisposicionAlerta.Tipo.DUPLICIDAD, absorbida.disposicion().orElseThrow().tipo());
        assertEquals(1, absorbida.revisiones().size());
        var vigente = consolidado.alertas().stream().filter(Alerta::pendiente).findFirst().orElseThrow();
        assertEquals(Set.of(id(100)), vigente.hechosVigentes());
        assertEquals(1, consolidado.notificacionesConsultables(TOE, Map.of(vigente.id(), ContextoAcceso.deHecho(unido)), politica(), reloj(60)).size());
    }

    @Test
    void ausenciaDeDestinatariosProduceSoloNecesidadDeDesignacion() {
        var directorio = directorio().desactivarUsuario(TOE, admin(11)).desactivarUsuario(DIRECTOR, admin(12)).desactivarUsuario(COMITE, admin(13));
        var politica = new PoliticaAcceso(directorio, RestriccionesAmbito.vacias());
        var hecho = grave(100);
        var registro = RegistroAlertas.vacio().evaluarHecho(hecho, ContextoAcceso.deHecho(hecho), politica,
                new RegistroCambio(DOCENTE, reloj(20), Optional.empty()));
        assertTrue(registro.requiereDesignacion());
        assertTrue(registro.notificaciones().isEmpty());
        assertTrue(registro.pendientes(ADMIN, Map.of(unica(registro).id(), ContextoAcceso.deHecho(hecho)), politica, reloj(30)).isEmpty());
    }

    @Test
    void exclusionPosteriorALecturaRetiraDetalleYNotificacionDeLaSiguienteConsulta() {
        var hecho = grave(100);
        var contexto = ContextoAcceso.deHecho(hecho);
        var registro = evaluar(RegistroAlertas.vacio(), hecho, 20);
        var aviso = registro.notificaciones().stream().filter(n -> n.destinatarioId().equals(TOE)).findFirst().orElseThrow();
        var leido = registro.leerNotificacion(aviso.id(), contexto, politica(), TOE, reloj(30));
        var restringidas = politica().excluir(id(600), TOE, contexto, new RegistroCambio(DIRECTOR, reloj(40), Optional.of("Conflicto documentado")));
        var politicaNueva = new PoliticaAcceso(directorio(), restringidas);
        assertTrue(leido.notificacionesConsultables(TOE, Map.of(unica(leido).id(), contexto), politicaNueva, reloj(50)).isEmpty());
        assertThrows(SecurityException.class, () -> leido.consultar(unica(leido).id(), contexto, politicaNueva, TOE, reloj(50)));
        assertTrue(unica(leido).pendiente());
    }

    @Test
    void contextoParcialNoPermiteConsultarSenalDeVariosHechos() {
        var uno = leve(100);
        var registro = reiterar(RegistroAlertas.vacio(), List.of(uno, leve(101)), 20);
        assertThrows(IllegalArgumentException.class, () -> registro.consultar(unica(registro).id(), ContextoAcceso.deHecho(uno), politica(), TOE, reloj(30)));
        assertThrows(IllegalArgumentException.class, () -> registro.pendientes(TOE, Map.of(), politica(), reloj(30)));
    }

    @Test
    void descarteGraveNecesitaResultadoYNoRequiereUnaRecepcionAdicional() {
        var hecho = grave(100);
        var inicial = evaluar(RegistroAlertas.vacio(), hecho, 20);
        assertThrows(IllegalArgumentException.class, () -> new DisposicionAlerta(DisposicionAlerta.Tipo.DESCARTE_FUNDAMENTADO, " ", Optional.empty(), cambio(30)));
        var descartado = inicial.disponer(unica(inicial).id(), new DisposicionAlerta(DisposicionAlerta.Tipo.DESCARTE_FUNDAMENTADO,
                "La verificación documentada constató un registro ajeno al hecho", Optional.empty(), cambio(30)), ContextoAcceso.deHecho(hecho), politica());
        assertFalse(unica(descartado).pendiente());
        assertEquals(1, descartado.notificaciones().size());
        assertEquals(1, unica(descartado).revisiones().size());
    }

    @Test
    void sincronizacionDeSenalMulticasoUsaTodosLosAmbitosYUnaGestionPertinente() {
        var hechos = List.of(leve(100), leve(101));
        var registro = RegistroAtenciones.iniciar(hechos, RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(), Optional.empty(), directorio(), cambio(20))
                .abrirCaso(id(201), Set.of(id(101)), HOY, TOE, List.of(), Optional.empty(), directorio(), cambio(21));
        var alertas = RegistroAlertas.vacio().evaluarReiteracion(ESTUDIANTE, cotidiana(), parametros(), registro.contextoDeHechos(Set.of(id(100), id(101))), politica(), cambio(30));
        var actuacion = Actuacion.realizada(id(300), "Intervención pertinente documentada", HOY, TOE, Set.of(id(100)), Set.of(), List.of(), cambio(40));
        var atendido = registro.registrarActuacion(id(200), actuacion, directorio(), cambio(40));
        var sincronizado = SeguimientoInstitucional.actualizar(atendido, alertas, directorio(), cambio(40)).alertas();
        assertFalse(unica(sincronizado).pendiente());
        assertEquals(Optional.of(id(200)), unica(sincronizado).disposicion().orElseThrow().referenciaId());
        assertEquals(2, atendido.casos().size());
    }

    @Test
    void seguimientoDeCasoAnteriorNoConsumeNuevaSenalSustentadaEnHechoNuevo() {
        var hechos = List.of(leve(100), leve(101));
        var primero = reiterar(RegistroAlertas.vacio(), hechos, 20);
        var registro = RegistroAtenciones.iniciar(hechos, RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(id(100), id(101)), HOY, TOE, List.of(), Optional.of(conclusion(30)), directorio(), cambio(30));
        var gestionado = SeguimientoInstitucional.actualizar(registro, primero, directorio(), cambio(30)).alertas();
        var conNuevoHecho = registro.incorporarHecho(leve(102), directorio(), cambio(40));
        var nuevo = reiterar(gestionado, List.of(leve(100), leve(101), leve(102)), 50);
        var seguimiento = Actuacion.realizada(id(300), "Seguimiento del caso anterior", HOY, TOE, Set.of(), Set.of(), List.of(), cambio(60));
        var anteriorConSeguimiento = conNuevoHecho.registrarActuacion(id(200), seguimiento, directorio(), cambio(60));
        var sincronizado = SeguimientoInstitucional.actualizar(anteriorConSeguimiento, nuevo, directorio(), cambio(60)).alertas();
        assertEquals(1, sincronizado.alertas().stream().filter(Alerta::pendiente).count());
        assertEquals(CasoAtencion.Estado.CERRADA, anteriorConSeguimiento.caso(id(200)).orElseThrow().estado());
    }

    @Test
    void primeraReiteracionCombinadaConGravedadNuevaNoReactivaLosHechosGravesYaGestionados() {
        var cat = violencia();
        var uno = hecho(100, cat, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, false);
        var dos = hecho(101, cat, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, false);
        var primera = evaluar(RegistroAlertas.vacio(), uno, 20);
        var atenciones = RegistroAtenciones.iniciar(List.of(uno, dos), RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(), Optional.of(conclusion(30)), directorio(), cambio(30));
        var gestionada = SeguimientoInstitucional.actualizar(atenciones, primera, directorio(), cambio(30)).alertas();
        var nueva = evaluar(gestionada, dos, 40).evaluarReiteracion(ESTUDIANTE, cat, parametros(),
                atenciones.contextoDeHechos(Set.of(id(100), id(101))), politica(), cambio(50));
        var pendiente = nueva.alertas().stream().filter(Alerta::pendiente).findFirst().orElseThrow();
        assertEquals(Set.of(id(101)), pendiente.hechosActivadores());
        assertEquals(Set.of(id(100), id(101)), pendiente.hechosVigentes());
        var seguimiento = Actuacion.realizada(id(300), "Seguimiento del hecho anterior", HOY, TOE, Set.of(id(100)), Set.of(), List.of(), cambio(60));
        var conSeguimiento = atenciones.registrarActuacion(id(200), seguimiento, directorio(), cambio(60));
        var sincronizada = SeguimientoInstitucional.actualizar(conSeguimiento, nueva, directorio(), cambio(60)).alertas();
        assertEquals(1, sincronizada.alertas().stream().filter(Alerta::pendiente).count());
        var recalculada = sincronizada.evaluarReiteracion(ESTUDIANTE, cat, parametros(), conSeguimiento.contextoDeHechos(Set.of(id(100), id(101))), politica(), cambio(70));
        assertEquals(Set.of(id(101)), recalculada.alertas().stream().filter(Alerta::pendiente).findFirst().orElseThrow().hechosActivadores());
    }

    @Test
    void sincronizacionNoUtilizaIntervencionesRegistradasDespuesDelInstanteSolicitado() {
        var hecho = grave(100);
        var atenciones = RegistroAtenciones.iniciar(List.of(hecho), RestriccionesAmbito.vacias())
                .abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(), Optional.empty(), directorio(), cambio(20));
        var contexto = atenciones.contexto(id(200));
        var alertas = RegistroAlertas.vacio().evaluarHecho(contexto.hechos().getFirst(), contexto, politica(), cambio(30));
        var futura = Actuacion.realizada(id(300), "Intervención registrada después", HOY, TOE, Set.of(id(100)), Set.of(), List.of(), cambio(100));
        var futuro = atenciones.registrarActuacion(id(200), futura, directorio(), cambio(100));
        assertThrows(IllegalArgumentException.class, () -> SeguimientoInstitucional.actualizar(futuro, alertas, directorio(), cambio(40)).alertas());
        assertTrue(unica(alertas).pendiente());
        assertFalse(unica(SeguimientoInstitucional.actualizar(futuro, alertas, directorio(), cambio(100)).alertas()).pendiente());
    }

    @Test
    void absorberReporteYaGestionadoBajoOtroPrincipalNoRepresentaHechoElegibleNuevo() {
        var anterior = grave(100);
        var primero = evaluar(RegistroAlertas.vacio(), anterior, 20);
        var gestionado = primero.disponer(unica(primero).id(), disposicion(30), ContextoAcceso.deHecho(anterior), politica());
        var unido = grave(101).vincularDuplicado(anterior.principal(), motivo(40));
        var actualizado = evaluar(gestionado, unido, 50);
        assertEquals(1, actualizado.alertas().size());
        assertFalse(unica(actualizado).pendiente());
        assertEquals(1, actualizado.notificaciones().size());
        assertEquals(Set.of(id(101)), unica(actualizado).hechosVigentes());
        assertTrue(unica(actualizado).hechosHistoricos().containsAll(Set.of(id(100), id(101))));
        assertEquals(unica(primero).revisiones().getFirst(), unica(actualizado).revisiones().getFirst());
    }

    @Test
    void toeImplicadoEsExcluidoYElAvisoSeDirigeAlComiteNoExcluido() {
        var persona = new PersonaInvolucrada(id(60), TipoPersona.PERSONAL, "TOE referido", Optional.of(TOE), Optional.of("TOE"));
        var datos = new DatosReporte(FechaHecho.exacta(HOY), 2026, violencia().clasificacionParaNuevoHecho(), "Relato reservado suficiente",
                List.of(new Participacion(persona, PapelParticipacion.PRESUNTA_RESPONSABILIDAD, Optional.empty())), false,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        var hecho = Hecho.de(ReporteIncidencia.registrar(id(100), datos, DOCENTE, reloj(10)));
        var registro = evaluar(RegistroAlertas.vacio(), hecho, 20);
        assertEquals(Set.of(DIRECTOR, COMITE), registro.notificaciones().stream().map(Notificacion::destinatarioId).collect(java.util.stream.Collectors.toSet()));
        assertThrows(SecurityException.class, () -> registro.consultar(unica(registro).id(), ContextoAcceso.deHecho(hecho), politica(), TOE, reloj(30)));
        assertFalse(registro.requiereDesignacion());
    }

    @Test
    void historiaAvisosYEstadoAnteriorSonInmutablesYSeRechazanCambiosRetroactivos() {
        var hecho = grave(100);
        var inicial = evaluar(RegistroAlertas.vacio(), hecho, 20);
        assertThrows(UnsupportedOperationException.class, () -> inicial.alertas().clear());
        assertThrows(UnsupportedOperationException.class, () -> unica(inicial).hechosVigentes().clear());
        assertThrows(UnsupportedOperationException.class, () -> unica(inicial).revisiones().getFirst().fundamentos().clear());
        assertThrows(IllegalArgumentException.class, () -> evaluar(inicial, hecho, 19));
        var gestionado = inicial.disponer(unica(inicial).id(), disposicion(30), ContextoAcceso.deHecho(hecho), politica());
        assertTrue(unica(inicial).pendiente());
        assertFalse(unica(gestionado).pendiente());
    }
}

final class DatosPruebaAlertas {
    static final Instant BASE = Instant.parse("2026-10-21T15:00:00Z");
    static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    static final UUID ADMIN = id(1), DIRECTOR = id(2), TOE = id(3), COMITE = id(4), DOCENTE = id(5), ESTUDIANTE = id(50);
    static UUID id(long valor) { return new UUID(0, valor); }
    static Instant reloj(int segundos) { return BASE.plusSeconds(segundos); }
    static RegistroCambio cambio(int segundos) { return new RegistroCambio(TOE, reloj(segundos), Optional.empty()); }
    static RegistroCambio motivo(int segundos) { return new RegistroCambio(TOE, reloj(segundos), Optional.of("Corrección documentada")); }
    static RegistroCambio admin(int segundos) { return new RegistroCambio(ADMIN, reloj(segundos), Optional.empty()); }
    static DirectorioUsuarios directorio() {
        var resultado = DirectorioUsuarios.inicializar(ADMIN, "Administrador ficticio", "admin", new RegistroCambio(id(90), BASE, Optional.of("Designación institucional")));
        var ids = List.of(DIRECTOR, TOE, COMITE, DOCENTE);
        var roles = List.of(RolUsuario.DIRECTOR, RolUsuario.TOE, RolUsuario.MIEMBRO_COMITE, RolUsuario.DOCENTE);
        for (int i = 0; i < ids.size(); i++) resultado = resultado.registrarUsuario(ids.get(i), "Usuario ficticio", "usuario" + i, admin(0))
                .asignarRol(ids.get(i), id(1000 + i), roles.get(i), new Vigencia(BASE, Optional.empty()), admin(0));
        return resultado;
    }
    static PoliticaAcceso politica() { return new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias()); }
    static Categoria cotidiana() { return categoria(80, NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE); }
    static Categoria violencia() { return categoria(81, NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.GRAVE); }
    static Categoria categoria(int numero, NaturalezaCategoria naturaleza, Gravedad gravedad) {
        return Categoria.crear(id(numero), new DefinicionCategoria("Categoría ficticia", "Descripción suficiente", naturaleza, gravedad,
                false, true, OptionalInt.of(2)), TOE, BASE);
    }
    static ParametrosReiteracion parametros() { return ParametrosReiteracion.sinConfigurar(TOE, BASE).configurarVentana(10, TOE, BASE); }
    static Hecho hecho(int numero, Categoria cat, PapelParticipacion papel, boolean solicitud) {
        return Hecho.de(ReporteIncidencia.registrar(id(numero), new DatosReporte(FechaHecho.exacta(HOY), 2026,
                cat.clasificacionParaNuevoHecho(), "Relato ficticio suficiente", List.of(new Participacion(estudiante(), papel, Optional.empty())),
                solicitud, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()), DOCENTE, reloj(10)));
    }
    static PersonaInvolucrada estudiante() { return PersonaInvolucrada.estudiante(ESTUDIANTE, "Estudiante ficticio"); }
    static Hecho grave(int numero) { return hecho(numero, violencia(), PapelParticipacion.DESCONOCIDO, false); }
    static Hecho leve(int numero) { return hecho(numero, cotidiana(), PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA, false); }
    static ContextoAcceso contexto(List<Hecho> hechos) { return new ContextoAcceso(AmbitoReserva.deHecho(hechos.getFirst()), hechos, Optional.empty(), Optional.empty()); }
    static RegistroAlertas evaluar(RegistroAlertas registro, Hecho hecho, int segundos) {
        return registro.evaluarHecho(hecho, ContextoAcceso.deHecho(hecho), politica(), cambio(segundos));
    }
    static RegistroAlertas reiterar(RegistroAlertas registro, List<Hecho> hechos, int segundos) {
        return registro.evaluarReiteracion(ESTUDIANTE, cotidiana(), parametros(), contexto(hechos), politica(), cambio(segundos));
    }
    static Alerta unica(RegistroAlertas registro) { assertEquals(1, registro.alertas().size()); return registro.alertas().getFirst(); }
    static DisposicionAlerta disposicion(int segundos) {
        return new DisposicionAlerta(DisposicionAlerta.Tipo.INTERVENCION_SIN_CASO, "Intervención inmediata ya documentada", Optional.of(id(300)), cambio(segundos));
    }
    static CierreLocal conclusion(int segundos) {
        return new CierreLocal(HOY, "Atención concluida según resultado documentado", CierreLocal.TipoCierre.CONCLUSION, cambio(segundos), Optional.empty());
    }
}
