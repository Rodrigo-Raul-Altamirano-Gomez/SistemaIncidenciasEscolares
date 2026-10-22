package pe.edu.incidencias.domain.atencion;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.catalogo.*;
import pe.edu.incidencias.domain.incidencia.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.incidencias.domain.atencion.DatosPruebaAtencion.*;

class RegistroAtencionesTest {
    @Test
    void casoAgrupaVariosHechosSinDuplicarPersonasNiAtribuirUnMismoPapel() {
        var principal = hecho(100, PapelParticipacion.AFECTADO);
        var duplicado = reporte(110, PapelParticipacion.TESTIGO, otraPersona());
        principal = principal.vincularDuplicado(duplicado, cambio(20));
        var registro = RegistroAtenciones.iniciar(List.of(principal, hecho(101, PapelParticipacion.PRESUNTA_RESPONSABILIDAD)), RestriccionesAmbito.vacias());
        var nuevo = abrir(registro, 200, Set.of(id(100), id(101)), 100);
        var consulta = nuevo.hechosDelCaso(id(200), TOE, directorio(), reloj(110));
        assertEquals(2, consulta.size());
        assertEquals(2, consulta.getFirst().participaciones().size());
        assertEquals(PapelParticipacion.AFECTADO, consulta.getFirst().participaciones().getFirst().papelVigente().orElseThrow());
        assertEquals(1, nuevo.casos().size());
        assertTrue(registro.casos().isEmpty());
    }

    @Test
    void unaSolaAtencionPendientePorHechoNoPorEstudiante() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        assertThrows(IllegalStateException.class, () -> abrir(registro, 201, Set.of(id(100)), 110));
        var otro = abrir(registro, 201, Set.of(id(101)), 110);
        assertEquals(2, otro.casos().size());
        assertEquals(2, otro.conTrabajoPendiente(TOE, directorio(), reloj(120)).size());
        assertThrows(IllegalArgumentException.class, () -> abrir(registro, 200, Set.of(id(101)), 110));
    }

    @Test
    void aperturaNoExigeConfirmarPapelesNiMotivoYMarcaAtencionDeFuentes() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        var caso = registro.caso(id(200)).orElseThrow();
        assertEquals(CasoAtencion.Estado.PENDIENTE, caso.estado());
        assertTrue(caso.apertura().motivo().isEmpty());
        var reporte = registro.hechosDelCaso(id(200), TOE, directorio(), reloj(110)).getFirst().principal();
        assertTrue(reporte.inicioAtencion().isPresent());
        assertEquals(PapelParticipacion.PRESUNTA_RESPONSABILIDAD, reporte.datos().participaciones().getFirst().papel());
        assertThrows(IllegalStateException.class, () -> reporte.corregirPorAutor(reporte.datos(),
                new RegistroCambio(DOCENTE, reloj(120), Optional.empty())));
    }

    @Test
    void conversacionCompromisoYCierreSeGuardanEnUnaSolaTransicion() {
        var compromiso = Compromiso.acordar(id(400), "Acuerdo ficticio", estudiante(), Optional.empty(), false, cambio(100));
        var actuacion = realizada(300, List.of(compromiso), 100);
        var cierre = conclusion(100);
        var registro = base().abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(actuacion),
                Optional.of(cierre), directorio(), cambio(100));
        var caso = registro.caso(id(200)).orElseThrow();
        assertEquals(CasoAtencion.Estado.CERRADA, caso.estado());
        assertEquals(1, caso.cantidadActuacionesRealizadas());
        assertEquals(Compromiso.Estado.ACORDADO, caso.actuaciones().getFirst().compromisos().getFirst().estado());
        assertFalse(caso.requiereTrabajo());
        assertTrue(registro.conTrabajoPendiente(TOE, directorio(), reloj(110)).isEmpty());
    }

    @Test
    void cerrarConProgramacionYCompromisoPendienteNoLosMarcaCumplidosNiLosOculta() {
        var acuerdo = Compromiso.acordar(id(400), "Seguimiento ficticio", estudiante(), Optional.of(HOY.plusDays(3)), false, cambio(100));
        var prevista = Actuacion.prevista(id(301), "Conversación prevista", HOY.plusDays(1), TOE, Set.of(), Set.of(), cambio(100));
        var registro = base().abrirCaso(id(200), Set.of(id(100)), HOY, TOE,
                List.of(realizada(300, List.of(acuerdo), 100), prevista), Optional.of(conclusion(100)), directorio(), cambio(100));
        var caso = registro.caso(id(200)).orElseThrow();
        assertEquals(CasoAtencion.Estado.CERRADA, caso.estado());
        assertTrue(caso.tieneSeguimientoPendiente());
        assertEquals(1, caso.cantidadActuacionesRealizadas());
        assertEquals(List.of(caso), registro.conTrabajoPendiente(TOE, directorio(), reloj(110)));
    }

    @Test
    void seguimientoPosteriorNoReabreNiReemplazaCierreHistorico() {
        var registro = base().abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(), Optional.of(conclusion(100)), directorio(), cambio(100));
        var original = registro.caso(id(200)).orElseThrow().cierre();
        var posterior = registro.registrarActuacion(id(200), realizada(300, List.of(), 120), directorio(), cambio(120));
        assertEquals(original, posterior.caso(id(200)).orElseThrow().cierre());
        assertEquals(1, posterior.caso(id(200)).orElseThrow().cierres().size());
        assertThrows(IllegalStateException.class, () -> posterior.vincularHechos(id(200), Set.of(id(101)), directorio(), cambio(130)));
    }

    @Test
    void previstaSeCompletaConMismoIdYCuentaUnaVez() {
        var prevista = Actuacion.prevista(id(300), "Atención programada", HOY, TOE, Set.of(), Set.of(), cambio(100));
        var registro = base().abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(prevista), Optional.empty(), directorio(), cambio(100));
        assertEquals(0, registro.caso(id(200)).orElseThrow().cantidadActuacionesRealizadas());
        var nueva = prevista.realizar(HOY, "Conversación y resultado documentados", cambio(110));
        var actualizado = registro.registrarActuacion(id(200), nueva, directorio(), cambio(110));
        assertEquals(1, actualizado.caso(id(200)).orElseThrow().cantidadActuacionesRealizadas());
        assertEquals(1, actualizado.caso(id(200)).orElseThrow().actuaciones().size());
        assertThrows(IllegalArgumentException.class, () -> actualizado.registrarActuacion(id(200), prevista, directorio(), cambio(120)));
    }

    @Test
    void vincularNoPideCausaPeroRetirarLaExigeYConservaActuaciones() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100)
                .vincularHechos(id(200), Set.of(id(101)), directorio(), cambio(110))
                .registrarActuacion(id(200), realizada(300, List.of(), 120), directorio(), cambio(120));
        assertThrows(IllegalArgumentException.class, () -> registro.retirarVinculo(id(200), id(101), directorio(), cambio(130)));
        var corregido = registro.retirarVinculo(id(200), id(101), directorio(), motivo(130));
        var caso = corregido.caso(id(200)).orElseThrow();
        assertEquals(Set.of(id(100)), caso.hechosActuales());
        assertEquals(Set.of(id(100), id(101)), caso.hechosHistoricos());
        assertEquals(1, caso.actuaciones().size());
        assertEquals(2, corregido.hechosDelCaso(id(200), TOE, directorio(), reloj(140)).size());
    }

    @Test
    void retirarUltimoVinculoCierraPorErrorSinBorrarHistoriaNiPendientes() {
        var prevista = Actuacion.prevista(id(300), "Seguimiento real", HOY.plusDays(1), TOE, Set.of(id(100)), Set.of(), cambio(100));
        var registro = base().abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(prevista), Optional.empty(), directorio(), cambio(100));
        var corregido = registro.retirarVinculo(id(200), id(100), directorio(), motivo(110));
        var caso = corregido.caso(id(200)).orElseThrow();
        assertTrue(caso.hechosActuales().isEmpty());
        assertEquals(CierreLocal.TipoCierre.APERTURA_ERRONEA, caso.cierre().orElseThrow().tipo());
        assertEquals(Set.of(id(100)), caso.hechosHistoricos());
        assertTrue(caso.requiereTrabajo());
        assertEquals(1, caso.actuaciones().size());
    }

    @Test
    void corregirCierreEsMotivadoYRevalidaUnicidadSiVuelveAPendiente() {
        var cerrado = base().abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(), Optional.of(conclusion(100)), directorio(), cambio(100));
        assertThrows(IllegalArgumentException.class, () -> cerrado.corregirCierre(id(200), Optional.empty(), directorio(), cambio(110)));
        var reabierto = cerrado.corregirCierre(id(200), Optional.empty(), directorio(), motivo(110));
        assertEquals(CasoAtencion.Estado.PENDIENTE, reabierto.caso(id(200)).orElseThrow().estado());
        assertEquals(2, reabierto.caso(id(200)).orElseThrow().cierres().size());
        var otraAtencion = abrir(cerrado, 201, Set.of(id(100)), 110);
        assertThrows(IllegalStateException.class, () -> otraAtencion.corregirCierre(id(200), Optional.empty(), directorio(), motivo(120)));
        assertEquals(CasoAtencion.Estado.CERRADA, otraAtencion.caso(id(200)).orElseThrow().estado());
    }

    @Test
    void citarHistoriaCerradaNoCopiaActuacionesNiReabreAtencionPrevia() {
        var registro = base().abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(realizada(300, List.of(), 100)),
                Optional.of(conclusion(100)), directorio(), cambio(100));
        registro = abrir(registro, 201, Set.of(id(101)), 110);
        var nuevo = registro.citarAntecedente(id(201), id(200), directorio(), cambio(120));
        assertEquals(Set.of(id(200)), nuevo.caso(id(201)).orElseThrow().antecedentes());
        assertTrue(nuevo.caso(id(201)).orElseThrow().actuaciones().isEmpty());
        assertEquals(CasoAtencion.Estado.CERRADA, nuevo.caso(id(200)).orElseThrow().estado());
        assertEquals(Set.of(id(200), id(201)), nuevo.contexto(id(201)).ambito().casosIds());
    }

    @Test
    void docenteAipYAdministradorNoAbrenNiConsultanCasos() {
        for (var usuario : List.of(DOCENTE, ADMIN, AIP)) {
            assertThrows(SecurityException.class, () -> base().abrirCaso(id(200), Set.of(id(100)), HOY, TOE,
                    List.of(), Optional.empty(), directorio(), new RegistroCambio(usuario, reloj(100), Optional.empty())));
            var abierto = abrir(base(), 200, Set.of(id(100)), 100);
            assertThrows(SecurityException.class, () -> abierto.consultarCaso(id(200), usuario, directorio(), reloj(110)));
        }
    }

    @Test
    void corregirVinculoSinContenidoCompartidoNoRestringeElAsuntoIndependiente() {
        var h1 = hecho(100, PapelParticipacion.PRESUNTA_RESPONSABILIDAD);
        var h2 = hecho(101, PapelParticipacion.TESTIGO);
        var politica = new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias());
        var ledger = politica.excluir(id(500), DIRECTOR, ContextoAcceso.deHecho(h1), motivo(50));
        var registro = RegistroAtenciones.iniciar(List.of(h1, h2), ledger);
        registro = abrir(registro, 200, Set.of(id(100), id(101)), 100);
        var corregido = registro.retirarVinculo(id(200), id(100), directorio(), motivo(110));
        assertNotNull(corregido.consultarCaso(id(200), DIRECTOR, directorio(), reloj(120)));
        assertEquals(1, corregido.conTrabajoPendiente(DIRECTOR, directorio(), reloj(120)).size());
        assertFalse(corregido.contexto(id(200)).ambito().hechosIds().contains(id(100)));
        assertTrue(corregido.caso(id(200)).orElseThrow().hechosHistoricos().contains(id(100)));
        assertTrue(corregido.restricciones().estaExcluido(DIRECTOR, ContextoAcceso.deHecho(h1).ambito()));
    }

    @Test
    void agruparUnHechoRestringidoNoPermiteAlImplicadoMoverloParaConseguirAcceso() {
        var h1 = hecho(100, PapelParticipacion.PRESUNTA_RESPONSABILIDAD);
        var h2 = hecho(101, PapelParticipacion.TESTIGO);
        var ledger = new PoliticaAcceso(directorio(), RestriccionesAmbito.vacias())
                .excluir(id(500), DIRECTOR, ContextoAcceso.deHecho(h1), motivo(50));
        var registro = abrir(RegistroAtenciones.iniciar(List.of(h1, h2), ledger), 200, Set.of(id(101)), 100);
        assertThrows(SecurityException.class, () -> registro.vincularHechos(id(200), Set.of(id(100)), directorio(),
                new RegistroCambio(DIRECTOR, reloj(110), Optional.empty())));
        assertEquals(Set.of(id(101)), registro.caso(id(200)).orElseThrow().hechosActuales());
    }

    @Test
    void casoNoAdmiteActuacionesDeHechosAjenosNiCopiasDeActuaciones() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        var ajena = Actuacion.realizada(id(300), "Acción ficticia", HOY, TOE, Set.of(id(101)), Set.of(), List.of(), cambio(110));
        assertThrows(IllegalArgumentException.class, () -> registro.registrarActuacion(id(200), ajena, directorio(), cambio(110)));
        var conActuacion = registro.registrarActuacion(id(200), realizada(300, List.of(), 110), directorio(), cambio(110));
        var dos = abrir(conActuacion, 201, Set.of(id(101)), 120);
        assertThrows(IllegalArgumentException.class, () -> dos.registrarActuacion(id(201), realizada(300, List.of(), 130), directorio(), cambio(130)));
    }

    @Test
    void accionesInmediatasSeReferencianEnCasoSinCopiarlasNiObligarAAbrirlo() {
        var hecho = hechoLeve(100);
        var registro = RegistroAtenciones.iniciar(List.of(hecho), RestriccionesAmbito.vacias());
        var acuerdo = Compromiso.acordar(id(400), "Compromiso inmediato", estudiante(), Optional.empty(), false,
                new RegistroCambio(DOCENTE, reloj(50), Optional.empty()));
        var accion = Actuacion.realizada(id(300), "Conversación inmediata", HOY, DOCENTE, Set.of(id(100)), Set.of(ESTUDIANTE),
                List.of(acuerdo), new RegistroCambio(DOCENTE, reloj(50), Optional.empty()));
        var inmediato = registro.registrarInmediata(id(100), accion, directorio(), new RegistroCambio(DOCENTE, reloj(50), Optional.empty()));
        assertTrue(inmediato.casos().isEmpty());
        var abierto = abrir(inmediato, 200, Set.of(id(100)), 100);
        assertTrue(abierto.caso(id(200)).orElseThrow().actuaciones().isEmpty());
        assertSame(accion, abierto.actuacionesDelCaso(id(200), TOE, directorio(), reloj(110)).getFirst());
        assertThrows(IllegalArgumentException.class, () -> abierto.registrarActuacion(id(200), accion, directorio(), cambio(120)));
        assertThrows(SecurityException.class, () -> abierto.registrarInmediata(id(100), accion, directorio(),
                new RegistroCambio(DOCENTE, reloj(120), Optional.empty())));
    }

    @Test
    void identificarDenunciadoDespuesDeAbrirCasoRetiraAccesoDelImplicado() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        assertNotNull(registro.consultarCaso(id(200), DIRECTOR, directorio(), reloj(105)));
        var hecho = registro.hechosDelCaso(id(200), TOE, directorio(), reloj(105)).getFirst();
        var anteriores = hecho.principal().datos();
        var personas = new java.util.ArrayList<>(anteriores.participaciones());
        personas.add(new Participacion(new PersonaInvolucrada(id(52), TipoPersona.PERSONAL, "Personal identificado",
                Optional.of(DIRECTOR), Optional.of("Función laboral")), PapelParticipacion.PRESUNTA_RESPONSABILIDAD, Optional.empty()));
        var datos = new DatosReporte(anteriores.fechaHecho(), anteriores.anioEscolar(), anteriores.clasificacion(), anteriores.descripcion(),
                personas, anteriores.solicitaAtencion(), anteriores.hora(), anteriores.lugar(), anteriores.comunicante(), anteriores.canal());
        var actualizado = hecho.actualizarReporte(hecho.principal().corregirInstitucionalmente(datos, motivo(110)));
        var nuevo = registro.actualizarHecho(actualizado, directorio(), cambio(110));
        assertThrows(SecurityException.class, () -> nuevo.consultarCaso(id(200), DIRECTOR, directorio(), reloj(120)));
        assertTrue(nuevo.restricciones().estaExcluido(DIRECTOR, nuevo.contexto(id(200)).ambito()));
        assertEquals(2, nuevo.hechosDelCaso(id(200), TOE, directorio(), reloj(120)).getFirst().participaciones().size());
        assertThrows(IllegalArgumentException.class, () -> nuevo.actualizarHecho(hecho, directorio(), cambio(130)));
    }

    @Test
    void exclusionesDeCasoSeRegistranYRetiranConLaPoliticaYSuAmbitoCompleto() {
        var registro = abrir(base(), 200, Set.of(id(100)), 100);
        var restringido = registro.excluirUsuario(id(200), id(500), DIRECTOR, directorio(), motivo(110));
        assertThrows(SecurityException.class, () -> restringido.consultarCaso(id(200), DIRECTOR, directorio(), reloj(120)));
        assertThrows(SecurityException.class, () -> restringido.retirarExclusion(id(200), id(500), directorio(),
                new RegistroCambio(DIRECTOR, reloj(120), Optional.of("Intento propio"))));
        var retirado = restringido.retirarExclusion(id(200), id(500), directorio(), motivo(130));
        assertNotNull(retirado.consultarCaso(id(200), DIRECTOR, directorio(), reloj(140)));
        assertTrue(retirado.restricciones().exclusiones().getFirst().retiro().isPresent());
    }

    @Test
    void hechosNuevosSeIncorporanSinPromocionNiReaperturaAutomatica() {
        var registro = base().abrirCaso(id(200), Set.of(id(100)), HOY, TOE, List.of(), Optional.of(conclusion(100)), directorio(), cambio(100));
        var nuevo = registro.incorporarHecho(hecho(103, PapelParticipacion.PRESUNTA_RESPONSABILIDAD), directorio(), cambio(110));
        assertEquals(1, nuevo.casos().size());
        assertEquals(CasoAtencion.Estado.CERRADA, nuevo.caso(id(200)).orElseThrow().estado());
        var abierto = abrir(nuevo, 201, Set.of(id(103)), 120);
        assertEquals(2, abierto.casos().size());
        assertThrows(IllegalArgumentException.class, () -> abierto.incorporarHecho(hecho(103, PapelParticipacion.AFECTADO), directorio(), cambio(130)));
    }

    @Test
    void unSoloGuardadoNoDuplicaCompromisoEnDosActuaciones() {
        var compromiso = Compromiso.acordar(id(400), "Acuerdo compartido", estudiante(), Optional.empty(), false, cambio(100));
        assertThrows(IllegalArgumentException.class, () -> base().abrirCaso(id(200), Set.of(id(100)), HOY, TOE,
                List.of(realizada(300, List.of(compromiso), 100), realizada(301, List.of(compromiso), 100)), Optional.empty(), directorio(), cambio(100)));
    }

    @Test
    void coleccionesYCasosPreviosNoSeMutanYEntradasInvalidasSeRechazan() {
        var registro = base();
        assertThrows(IllegalArgumentException.class, () -> abrir(registro, 200, Set.of(), 100));
        assertThrows(IllegalArgumentException.class, () -> abrir(registro, 200, Set.of(id(999)), 100));
        var abierto = abrir(registro, 200, Set.of(id(100)), 100);
        assertTrue(registro.casos().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> abierto.casos().clear());
        assertThrows(UnsupportedOperationException.class, () -> abierto.caso(id(200)).orElseThrow().hechosActuales().clear());
        assertThrows(IllegalArgumentException.class, () -> abierto.registrarActuacion(id(200), realizada(300, List.of(), 90), directorio(), cambio(90)));
        assertThrows(IllegalArgumentException.class, () -> abierto.abrirCaso(id(201), Set.of(id(101)), HOY.plusDays(1), TOE,
                List.of(), Optional.empty(), directorio(), cambio(110)));
    }
}

final class DatosPruebaAtencion {
    static final Instant INICIO = Instant.parse("2026-10-21T15:00:00Z");
    static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    static final UUID ADMIN = id(1), DIRECTOR = id(2), TOE = id(3), COMITE = id(4), DOCENTE = id(5), AIP = id(6), ESTUDIANTE = id(50);
    static UUID id(long numero) { return new UUID(0, numero); }
    static Instant reloj(int segundos) { return INICIO.plusSeconds(segundos); }
    static RegistroCambio cambio(int segundos) { return new RegistroCambio(TOE, reloj(segundos), Optional.empty()); }
    static RegistroCambio motivo(int segundos) { return new RegistroCambio(TOE, reloj(segundos), Optional.of("Corrección institucional ficticia")); }
    static PersonaInvolucrada estudiante() { return PersonaInvolucrada.estudiante(ESTUDIANTE, "Estudiante ficticio"); }
    static PersonaInvolucrada otraPersona() { return new PersonaInvolucrada(id(51), TipoPersona.FAMILIAR, "Familiar referido", Optional.empty(), Optional.empty()); }
    static DirectorioUsuarios directorio() {
        var resultado = DirectorioUsuarios.inicializar(ADMIN, "Administrador ficticio", "admin", new RegistroCambio(id(99), INICIO, Optional.of("Designación institucional")));
        var usuarios = List.of(DIRECTOR, TOE, COMITE, DOCENTE, AIP);
        var roles = List.of(RolUsuario.DIRECTOR, RolUsuario.TOE, RolUsuario.MIEMBRO_COMITE, RolUsuario.DOCENTE, RolUsuario.AIP);
        for (int i = 0; i < usuarios.size(); i++) resultado = resultado.registrarUsuario(usuarios.get(i), "Persona ficticia", "usuario" + i,
                new RegistroCambio(ADMIN, INICIO, Optional.empty())).asignarRol(usuarios.get(i), id(1000 + i), roles.get(i),
                new Vigencia(INICIO, Optional.empty()), new RegistroCambio(ADMIN, INICIO, Optional.empty()));
        return resultado;
    }
    static ReporteIncidencia reporte(int numero, PapelParticipacion papel, PersonaInvolucrada persona) {
        return ReporteIncidencia.registrar(id(numero), new DatosReporte(FechaHecho.exacta(HOY.minusDays(1)), 2026,
                new ClasificacionCategoria(id(80), "Protección ficticia", NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.GRAVE, true),
                "Relato ficticio de un hecho", List.of(new Participacion(persona, papel, Optional.empty())), false,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()), DOCENTE, reloj(10));
    }
    static Hecho hecho(int numero, PapelParticipacion papel) { return Hecho.de(reporte(numero, papel, estudiante())); }
    static Hecho hechoLeve(int numero) {
        return Hecho.de(ReporteIncidencia.registrar(id(numero), new DatosReporte(FechaHecho.exacta(HOY), 2026,
                new ClasificacionCategoria(id(81), "Cotidiana ficticia", NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE, false),
                "Anotación cotidiana ficticia", List.of(new Participacion(estudiante(), PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA, Optional.empty())),
                false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()), DOCENTE, reloj(10)));
    }
    static RegistroAtenciones base() {
        return RegistroAtenciones.iniciar(List.of(hecho(100, PapelParticipacion.PRESUNTA_RESPONSABILIDAD), hecho(101, PapelParticipacion.TESTIGO),
                hecho(102, PapelParticipacion.AFECTADO)), RestriccionesAmbito.vacias());
    }
    static RegistroAtenciones abrir(RegistroAtenciones registro, int caso, Set<UUID> hechos, int segundos) {
        return registro.abrirCaso(id(caso), hechos, HOY, TOE, List.of(), Optional.empty(), directorio(), cambio(segundos));
    }
    static Actuacion realizada(int numero, List<Compromiso> compromisos, int segundos) {
        return Actuacion.realizada(id(numero), "Conversación realizada con resultado suficiente", HOY, TOE, Set.of(), Set.of(), compromisos, cambio(segundos));
    }
    static CierreLocal conclusion(int segundos) {
        return new CierreLocal(HOY, "La conversación documentada concluye la atención local", CierreLocal.TipoCierre.CONCLUSION, cambio(segundos), Optional.empty());
    }
}
