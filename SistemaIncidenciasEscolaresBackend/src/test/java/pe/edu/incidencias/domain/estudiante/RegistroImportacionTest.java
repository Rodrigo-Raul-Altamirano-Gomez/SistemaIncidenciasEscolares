package pe.edu.incidencias.domain.estudiante;

import org.junit.jupiter.api.Test;
import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.incidencia.*;
import pe.edu.incidencias.domain.catalogo.*;
import pe.edu.incidencias.domain.institucion.*;
import pe.edu.incidencias.domain.proteccion.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegistroImportacionTest {
    private static final Instant INICIO = Instant.parse("2025-01-01T12:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-10-22T15:00:00Z");
    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);
    private static final GrupoEscolar A = new GrupoEscolar("Primero", "A");
    private static final GrupoEscolar B = new GrupoEscolar("Primero", "B");
    private final UUID admin = UUID.randomUUID();
    private final UUID aip = UUID.randomUUID();
    private final UUID toe = UUID.randomUUID();
    private final DirectorioUsuarios directorio = directorio();
    private final PoliticaAcceso politica = new PoliticaAcceso(directorio, RestriccionesAmbito.vacias());
    private final AlcanceImportacion alcance = new AlcanceImportacion(UUID.randomUUID(), Map.of(
            "contacto", concepto("contacto", AlcanceImportacion.Tipo.CONTACTO_FAMILIAR),
            "telefono", concepto("telefono", AlcanceImportacion.Tipo.TELEFONO_ESTUDIANTE),
            "domicilio", concepto("domicilio", AlcanceImportacion.Tipo.DOMICILIO),
            "bullying", concepto("bullying", AlcanceImportacion.Tipo.ANTECEDENTE),
            "salud", concepto("salud", AlcanceImportacion.Tipo.ANTECEDENTE)), "Acuerdo institucional ficticio 01");

    @Test
    void guardaMatriculaContactoYAntecedenteComoUnaInstantaneaSinModificarLaBase() {
        var original = registro();
        var fila = fila(7, "Lucía", "01234567");
        var lote = lote(fila, Map.of("contacto", new LoteImportacion.Familiar(contacto()), "bullying", no()));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(1, vista.altas());
        assertEquals(0, vista.actualizaciones());
        assertTrue(original.matriculas().estudiantes().isEmpty());
        assertTrue(original.contextos().isEmpty());
        var confirmado = original.confirmar(vista, politica, cambio());
        assertEquals(1, confirmado.matriculas().estudiantes().size());
        var contexto = confirmado.contextos().get(fila.identidadParaAlta());
        assertEquals("Contacto familiar", contexto.contacto().orElseThrow().valor().nombre().orElseThrow());
        var antecedente = contexto.antecedentes().get("bullying");
        assertEquals(ContextoEstudiante.Calidad.NEGACION_DECLARADA, antecedente.valor().calidad());
        assertEquals("no", antecedente.valor().contenido());
        assertEquals(lote.matricula().id(), antecedente.procedencia().loteId());
        assertEquals(7, antecedente.procedencia().fila());
        assertEquals(AHORA, antecedente.procedencia().incorporacion().fecha());
        assertEquals("Fundamento ficticio", antecedente.procedencia().fundamento());
        assertEquals(Optional.of("año anterior"), antecedente.valor().periodo());
    }

    @Test
    void campoNoAdmitidoExcluyeFilaCompletaSinAltaParcialYPermiteOtrasFilas() {
        var primera = fila(7, "Primera", "12345678");
        var segunda = fila(8, "Segunda", "87654321");
        var m = matricula(List.of(primera, segunda));
        var lote = new LoteImportacion(m, alcance, Map.of(7,
                new LoteImportacion.FilaContexto(Map.of("ocupacion", new LoteImportacion.Texto("Dato ajeno")), Set.of())));
        var original = registro();
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertFalse(vista.filas().getFirst().aceptable());
        assertFalse(vista.filas().getFirst().erroresContexto().isEmpty());
        var guardado = original.confirmar(vista, politica, cambio());
        assertFalse(guardado.matriculas().estudiantes().containsKey(primera.identidadParaAlta()));
        assertTrue(guardado.matriculas().estudiantes().containsKey(segunda.identidadParaAlta()));
        assertFalse(guardado.confirmaciones().toString().contains("Dato ajeno"));
    }

    @Test
    void asociacionIncompatibleNoGuardaIdentidadNiContexto() {
        var original = registro();
        var lote = lote(fila(7, "Lucía", null), Map.of("salud", new LoteImportacion.Texto("no")));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertTrue(vista.filas().getFirst().erroresContexto().getFirst().contains("incompatible"));
        assertTrue(original.confirmar(vista, politica, cambio()).matriculas().estudiantes().isEmpty());
    }

    @Test
    void contactosCompartidosNoConciliarIdentidadesYTelefonoAlumnoEsIndependiente() {
        var primera = fila(7, "Primera", "12345678");
        var segunda = fila(8, "Segunda", "87654321");
        var datos = new LoteImportacion.FilaContexto(Map.of("contacto", new LoteImportacion.Familiar(contacto()),
                "telefono", new LoteImportacion.Texto("099999999")), Set.of());
        var lote = new LoteImportacion(matricula(List.of(primera, segunda)), alcance, Map.of(7, datos, 8, datos));
        var original = registro();
        var nuevo = original.confirmar(original.previsualizar(lote, politica, aip, AHORA), politica, cambio());
        assertEquals(2, nuevo.matriculas().estudiantes().size());
        var contexto = nuevo.contextos().get(primera.identidadParaAlta());
        assertEquals("099999999", contexto.telefonoEstudiante().orElseThrow().valor());
        assertEquals("012345678", contexto.contacto().orElseThrow().valor().telefono().orElseThrow());
        assertTrue(contexto.contacto().orElseThrow().valor().relacionInformada().isEmpty());
    }

    @Test
    void documentoFamiliarRequiereNecesidadYNoAcreditaRepresentacion() {
        assertThrows(IllegalArgumentException.class, () -> new ContactoFamiliar(Optional.of("Familiar"),
                Optional.empty(), Optional.empty(), Optional.of(new DocumentoIdentidad("DNI", "12345678")), Optional.empty()));
        var contacto = new ContactoFamiliar(Optional.of("Familiar"), Optional.of("Relación declarada"),
                Optional.empty(), Optional.of(new DocumentoIdentidad("DNI", "12345678")), Optional.of("Necesidad institucional delimitada"));
        assertEquals("Relación declarada", contacto.relacionInformada().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> new ContactoFamiliar(Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty()));
    }

    @Test
    void omisionesConservanContextoYReimportarValoresIgualesNoDuplicaNiCambiaProcedencia() {
        var fila = fila(7, "Lucía", "12345678");
        var primero = lote(fila, Map.of("bullying", no(), "contacto", new LoteImportacion.Familiar(contacto())));
        var original = registro();
        var base = original.confirmar(original.previsualizar(primero, politica, aip, AHORA), politica, cambio());
        var contexto = base.contextos().get(fila.identidadParaAlta());
        var segundo = lote(fila, Map.of());
        var omitido = base.confirmar(base.previsualizar(segundo, politica, aip, AHORA), politica, cambio());
        assertEquals(contexto, omitido.contextos().get(fila.identidadParaAlta()));
        var tercero = lote(fila, Map.of("bullying", no()));
        var vista = omitido.previsualizar(tercero, politica, aip, AHORA);
        assertEquals(RegistroImportacion.Accion.SIN_CAMBIOS, vista.filas().getFirst().contexto().getFirst().accion());
        var repetido = omitido.confirmar(vista, politica, cambio());
        assertEquals(contexto, repetido.contextos().get(fila.identidadParaAlta()));
    }

    @Test
    void vistaDeSustitucionMuestraProcedenciaYAlcanceSinContenidoAnterior() {
        var fila = fila(7, "Lucía", "12345678");
        var primero = lote(fila, Map.of("salud", antecedente("Contenido reservado anterior")));
        var original = registro();
        var base = original.confirmar(original.previsualizar(primero, politica, aip, AHORA), politica, cambio());
        var siguiente = lote(fila, Map.of("salud", antecedente("Contenido nuevo")));
        var vista = base.previsualizar(siguiente, politica, aip, AHORA);
        assertEquals(1, vista.actualizaciones());
        var detalle = vista.filas().getFirst().contexto().getFirst();
        assertEquals(RegistroImportacion.Accion.SUSTITUIR, detalle.accion());
        assertEquals(primero.matricula().id(), detalle.anterior().orElseThrow().loteId());
        assertFalse(vista.filas().toString().contains("Contenido reservado anterior"));
        var nuevo = base.confirmar(vista, politica, cambio());
        assertEquals("Contenido nuevo", nuevo.contextos().get(fila.identidadParaAlta()).antecedentes().get("salud").valor().contenido());
        assertFalse(nuevo.confirmaciones().toString().contains("Contenido reservado anterior"));
    }

    @Test
    void borradoExpresoMuestraAlcanceYNoBorraLosDemasCampos() {
        var fila = fila(7, "Lucía", "12345678");
        var primero = lote(fila, Map.of("salud", no(), "contacto", new LoteImportacion.Familiar(contacto())));
        var original = registro();
        var base = original.confirmar(original.previsualizar(primero, politica, aip, AHORA), politica, cambio());
        var borrar = new LoteImportacion(matricula(List.of(fila)), alcance,
                Map.of(7, new LoteImportacion.FilaContexto(Map.of(), Set.of("salud"))));
        var vista = base.previsualizar(borrar, politica, aip, AHORA);
        assertEquals("salud", vista.filas().getFirst().contexto().getFirst().concepto());
        assertEquals(RegistroImportacion.Accion.BORRAR, vista.filas().getFirst().contexto().getFirst().accion());
        var nuevo = base.confirmar(vista, politica, cambio());
        assertTrue(nuevo.contextos().get(fila.identidadParaAlta()).antecedentes().isEmpty());
        assertTrue(nuevo.contextos().get(fila.identidadParaAlta()).contacto().isPresent());
    }

    @Test
    void noPermiteBorrarConceptosNoAdmitidosNiBorrarYAportarElMismo() {
        assertThrows(IllegalArgumentException.class, () -> new LoteImportacion.FilaContexto(Map.of("salud", no()), Set.of("salud")));
        var lote = new LoteImportacion(matricula(List.of(fila(7, "Lucía", null))), alcance,
                Map.of(7, new LoteImportacion.FilaContexto(Map.of(), Set.of("ajeno"))));
        assertFalse(registro().previsualizar(lote, politica, aip, AHORA).filas().getFirst().aceptable());
    }

    @Test
    void reintentoNoDuplicaYNoAceptaOtroContextoConElMismoId() {
        var original = registro();
        var lote = lote(fila(7, "Lucía", "12345678"), Map.of("salud", no()));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        var nuevo = original.confirmar(vista, politica, cambio());
        assertSame(nuevo, nuevo.confirmar(vista, politica, cambio()));
        assertSame(nuevo, nuevo.confirmar(nuevo.previsualizar(lote, politica, aip, AHORA), politica, cambio()));
        var alterado = new LoteImportacion(lote.matricula(), alcance,
                Map.of(7, new LoteImportacion.FilaContexto(Map.of("salud", antecedente("Otro")), Set.of())));
        assertThrows(IllegalArgumentException.class, () -> nuevo.previsualizar(alterado, politica, aip, AHORA));
        assertEquals(1, nuevo.contextos().values().iterator().next().antecedentes().size());
    }

    @Test
    void noAceptaPrevisualizacionDeOtraInstantaneaNiConfirmacionAnterior() {
        var original = registro();
        var lote = lote(fila(7, "Lucía", null), Map.of());
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertThrows(IllegalArgumentException.class, () -> registro().confirmar(vista, politica, cambio()));
        assertThrows(IllegalArgumentException.class, () -> original.confirmar(vista, politica,
                new RegistroCambio(aip, AHORA.minusSeconds(1), Optional.empty())));
    }

    @Test
    void soloAipRevisaYElMismoOperadorConfirma() {
        var original = registro();
        var lote = lote(fila(7, "Lucía", null), Map.of());
        assertThrows(SecurityException.class, () -> original.previsualizar(lote, politica, toe, AHORA));
        var otro = UUID.randomUUID();
        var cambioAdmin = new RegistroCambio(admin, INICIO, Optional.empty());
        var d = directorio.registrarUsuario(otro, "Otro AIP", "otro", cambioAdmin)
                .asignarRol(otro, UUID.randomUUID(), RolUsuario.AIP, new Vigencia(INICIO, Optional.empty()), cambioAdmin);
        var p = new PoliticaAcceso(d, RestriccionesAmbito.vacias());
        var vista = original.previsualizar(lote, p, aip, AHORA);
        assertThrows(SecurityException.class, () -> original.confirmar(vista, p, new RegistroCambio(otro, AHORA, Optional.empty())));
        var nuevo = original.confirmar(vista, p, cambio());
        assertThrows(SecurityException.class, () -> nuevo.consultarResultadoLote(lote.matricula().id(), p, otro, AHORA));
        assertThrows(SecurityException.class, () -> nuevo.previsualizar(lote, p, otro, AHORA));
    }

    @Test
    void loteExcluidoNoValidaNiIncorporaContextoDescartado() {
        var fila = fila(7, "Lucía", null);
        var m = matricula(List.of(fila));
        m = new LoteMatricula(m.id(), m.procedencia(), m.hoja(), m.anio(), m.fechaEfectiva(), m.grupoComun(),
                m.filas(), Map.of(7, LoteMatricula.Resolucion.excluir()));
        var lote = new LoteImportacion(m, alcance, Map.of(7,
                new LoteImportacion.FilaContexto(Map.of("no_admitido", antecedente("No guardar")), Set.of())));
        var original = registro();
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertTrue(vista.filas().getFirst().erroresContexto().isEmpty());
        var guardado = original.confirmar(vista, politica, cambio());
        assertTrue(guardado.contextos().isEmpty());
        assertTrue(guardado.matriculas().estudiantes().isEmpty());
    }

    @Test
    void grupoContradictorioRequiereRectificacionExpresaYConservaGrupoPosterior() {
        var e = Estudiante.crear(UUID.randomUUID(), "Lucía").incorporarIdentidad(Optional.empty(),
                Optional.of(new DocumentoIdentidad("DNI", "12345678")), Optional.empty(), Optional.empty())
                .vincularMatricula(MatriculaAnual.crear(2026, FECHA, Optional.of(A), Optional.of("Matriculada")))
                .incorporarGrupo(2026, FECHA.plusMonths(1), Optional.of(A));
        var f = fila(7, "Lucía", "12345678");
        f = new FilaMatricula(7, f.identidadParaAlta(), f.nombreCompleto(), f.documento(), f.nacimiento(), f.sexo(), Optional.of(B), f.situacion());
        var m = matricula(List.of(f));
        var original = registro(e);
        assertFalse(original.previsualizar(new LoteImportacion(m, alcance, Map.of()), politica, aip, AHORA).filas().getFirst().aceptable());
        var solicitud = new RectificacionMatricula(Set.of(), true);
        var rectificado = new LoteMatricula(m.id(), m.procedencia(), m.hoja(), m.anio(), m.fechaEfectiva(), m.grupoComun(),
                m.filas(), m.resoluciones(), Map.of(7, solicitud));
        var lote = new LoteImportacion(rectificado, alcance, Map.of());
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertEquals(Optional.of(solicitud), vista.filas().getFirst().rectificacion());
        var nuevo = original.confirmar(vista, politica, cambio());
        assertEquals(Optional.of(B), nuevo.matriculas().estudiantes().get(e.id()).grupoEn(2026, FECHA));
        assertEquals(Optional.of(A), nuevo.matriculas().estudiantes().get(e.id()).grupoEn(2026, FECHA.plusMonths(1)));
        assertTrue(nuevo.matriculas().confirmaciones().get(m.id()).filas().getFirst().cambios().contains(RegistroMatriculas.Campo.GRUPO));
    }

    @Test
    void borradoDeGrupoYDocumentoSonExpresosYGrupoComunNoReponeLoBorrado() {
        var e = Estudiante.crear(UUID.randomUUID(), "Lucía").incorporarIdentidad(Optional.empty(),
                Optional.of(new DocumentoIdentidad("DNI", "12345678")), Optional.empty(), Optional.empty())
                .vincularMatricula(MatriculaAnual.crear(2026, FECHA, Optional.of(A), Optional.empty()));
        var fila = fila(7, "Lucía", null);
        var solicitud = new RectificacionMatricula(Set.of(RectificacionMatricula.Campo.DOCUMENTO, RectificacionMatricula.Campo.GRUPO), false);
        var m = new LoteMatricula(UUID.randomUUID(), "Fuente", "Hoja", 2026, FECHA, Optional.of(B), List.of(fila),
                Map.of(7, LoteMatricula.Resolucion.vincular(e.id())), Map.of(7, solicitud));
        var original = registro(e);
        var lote = new LoteImportacion(m, alcance, Map.of());
        var nuevo = original.confirmar(original.previsualizar(lote, politica, aip, AHORA), politica, cambio());
        var actualizado = nuevo.matriculas().estudiantes().get(e.id());
        assertTrue(actualizado.documento().isEmpty());
        assertTrue(actualizado.grupoEn(2026, FECHA).isEmpty());
        assertEquals(e.id(), actualizado.id());
    }

    @Test
    void erroresDeContextoEvitanTambienAplicarRectificacionDeIdentidad() {
        var e = Estudiante.crear(UUID.randomUUID(), "Lucía").incorporarIdentidad(Optional.empty(),
                Optional.of(new DocumentoIdentidad("DNI", "12345678")), Optional.empty(), Optional.empty());
        var fila = fila(7, "Lucía", null);
        var m = new LoteMatricula(UUID.randomUUID(), "Fuente", "Hoja", 2026, FECHA, Optional.empty(), List.of(fila),
                Map.of(7, LoteMatricula.Resolucion.vincular(e.id())),
                Map.of(7, new RectificacionMatricula(Set.of(RectificacionMatricula.Campo.DOCUMENTO), false)));
        var lote = new LoteImportacion(m, alcance, Map.of(7,
                new LoteImportacion.FilaContexto(Map.of("ajeno", no()), Set.of())));
        var original = registro(e);
        var nuevo = original.confirmar(original.previsualizar(lote, politica, aip, AHORA), politica, cambio());
        assertSame(e, nuevo.matriculas().estudiantes().get(e.id()));
    }

    @Test
    void alcanceNoAdmiteFundamentoVacioNiCamposDeContactoDuplicados() {
        assertThrows(IllegalArgumentException.class, () -> new AlcanceImportacion.Concepto("salud", "Salud", AlcanceImportacion.Tipo.ANTECEDENTE, " "));
        assertThrows(IllegalArgumentException.class, () -> new AlcanceImportacion(UUID.randomUUID(), Map.of(
                "uno", concepto("uno", AlcanceImportacion.Tipo.CONTACTO_FAMILIAR),
                "dos", concepto("dos", AlcanceImportacion.Tipo.CONTACTO_FAMILIAR)), "Referencia"));
        assertThrows(UnsupportedOperationException.class, () -> alcance.conceptos().clear());
    }

    @Test
    void configuracionInstitucionalObligaAlcanceVigenteAnioYGrupoAdmitidos() {
        var config = configuracion(alcance);
        var original = registro();
        var lote = lote(fila(7, "Lucía", "12345678"), Map.of("salud", no()));
        var vista = original.previsualizar(lote, config, politica, aip, AHORA);
        assertEquals(1, original.confirmar(vista, config, politica, cambio()).contextos().size());
        var otro = new AlcanceImportacion(UUID.randomUUID(), alcance.conceptos(), "Otra decisión");
        assertThrows(IllegalArgumentException.class, () -> original.previsualizar(new LoteImportacion(lote.matricula(), otro,
                lote.contexto()), config, politica, aip, AHORA));
        assertThrows(IllegalArgumentException.class, () -> original.confirmar(vista, configuracion(otro), politica, cambio()));
    }

    @Test
    void grupoNoAdmitidoEsErrorDeFilaYNoImpideConfirmarLasDemas() {
        var invalida = new FilaMatricula(7, UUID.randomUUID(), "Primera", Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(new GrupoEscolar("Décimo", "Z")), Optional.empty());
        var valida = fila(8, "Segunda", "87654321");
        var lote = new LoteImportacion(matricula(List.of(invalida, valida)), alcance,
                Map.of(7, new LoteImportacion.FilaContexto(Map.of("salud", no()), Set.of())));
        var original = registro();
        var config = configuracion(alcance);
        var vista = original.previsualizar(lote, config, politica, aip, AHORA);
        assertEquals(RegistroMatriculas.Estado.ERROR, vista.filas().getFirst().matricula().estado());
        assertTrue(vista.filas().getFirst().matricula().avisos().getFirst().contains("Grupo"));
        var guardado = original.confirmar(vista, config, politica, cambio());
        assertFalse(guardado.matriculas().estudiantes().containsKey(invalida.identidadParaAlta()));
        assertFalse(guardado.contextos().containsKey(invalida.identidadParaAlta()));
        assertTrue(guardado.matriculas().estudiantes().containsKey(valida.identidadParaAlta()));
    }

    @Test
    void filaExcluidaNoBloqueaLotePorGrupoNoAdmitido() {
        var f = new FilaMatricula(7, UUID.randomUUID(), "Excluida", Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(new GrupoEscolar("Décimo", "Z")), Optional.empty());
        var m = matricula(List.of(f, fila(8, "Segunda", "87654321")));
        var lote = new LoteImportacion(new LoteMatricula(m.id(), m.procedencia(), m.hoja(), m.anio(),
                m.fechaEfectiva(), m.grupoComun(), m.filas(), Map.of(7, LoteMatricula.Resolucion.excluir())), alcance, Map.of());
        var original = registro();
        var config = configuracion(alcance);
        var vista = original.previsualizar(lote, config, politica, aip, AHORA);
        assertEquals(RegistroMatriculas.Estado.EXCLUIDA, vista.filas().getFirst().matricula().estado());
        assertEquals(1, original.confirmar(vista, config, politica, cambio()).matriculas().estudiantes().size());
    }

    @Test
    void grupoRetiradoDespuesDePrevisualizarImpideConfirmarLaFilaAceptada() {
        var f = new FilaMatricula(7, UUID.randomUUID(), "Primera", Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(A), Optional.empty());
        var lote = lote(f, Map.of());
        var original = registro();
        var config = configuracion(alcance);
        var vista = original.previsualizar(lote, config, politica, aip, AHORA);
        var sinGrupo = config.definirGrupos(2026, Set.of(B), politica, new RegistroCambio(admin, AHORA, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> original.confirmar(vista, sinGrupo, politica, cambio()));
        assertTrue(original.matriculas().estudiantes().isEmpty());
    }

    @Test
    void contextoInvalidoNoOcultaDniRepetidoDeOtraFila() {
        var primera = fila(7, "Primera", "12345678");
        var segunda = fila(8, "Segunda", "12345678");
        var lote = new LoteImportacion(matricula(List.of(primera, segunda)), alcance,
                Map.of(7, new LoteImportacion.FilaContexto(Map.of("salud", new LoteImportacion.Texto("Incorrecto")), Set.of())));
        var original = registro();
        var vista = original.previsualizar(lote, configuracion(alcance), politica, aip, AHORA);
        assertTrue(vista.filas().stream().allMatch(f -> f.matricula().estado() == RegistroMatriculas.Estado.ERROR));
        assertTrue(original.confirmar(vista, configuracion(alcance), politica, cambio()).matriculas().estudiantes().isEmpty());
    }

    @Test
    void noPermiteReusarIdConfirmadoSoloComoMatriculaParaAgregarContexto() {
        var fila = fila(7, "Lucía", null);
        var lote = lote(fila, Map.of());
        var m = RegistroMatriculas.cargar(List.of(), List.of());
        m = m.confirmar(m.previsualizar(lote.matricula(), politica, aip, AHORA), politica, cambio());
        var registro = RegistroImportacion.cargar(m, List.of(), List.of());
        assertThrows(IllegalArgumentException.class, () -> registro.previsualizar(lote, politica, aip, AHORA));
    }

    @Test
    void copiasDeEntradaYResultadosSonInmutables() {
        var original = registro();
        var lote = lote(fila(7, "Lucía", null), Map.of("salud", no()));
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        assertThrows(UnsupportedOperationException.class, () -> vista.filas().clear());
        assertThrows(UnsupportedOperationException.class, () -> lote.contexto().clear());
        var nuevo = original.confirmar(vista, politica, cambio());
        assertThrows(UnsupportedOperationException.class, () -> nuevo.contextos().clear());
        assertThrows(UnsupportedOperationException.class, () -> nuevo.confirmaciones().clear());
    }

    @Test
    void actualizarNombreDelContactoNoBorraTelefonoRelacionODocumentoOmitidos() {
        var fila = fila(7, "Lucía", "12345678");
        var primero = lote(fila, Map.of("contacto", new LoteImportacion.Familiar(contacto())));
        var original = registro();
        var base = original.confirmar(original.previsualizar(primero, politica, aip, AHORA), politica, cambio());
        var parcial = new ContactoFamiliar(Optional.of("Nombre actualizado"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        var siguiente = lote(fila, Map.of("contacto", new LoteImportacion.Familiar(parcial)));
        var nuevo = base.confirmar(base.previsualizar(siguiente, politica, aip, AHORA), politica, cambio());
        var actual = nuevo.contextos().get(fila.identidadParaAlta()).contacto().orElseThrow().valor();
        assertEquals("Nombre actualizado", actual.nombre().orElseThrow());
        assertEquals("012345678", actual.telefono().orElseThrow());
    }

    @Test
    void historiaDeSustitucionYBorradoEsInstitucionalYNuncaSeExponeAlAip() {
        var fila = fila(7, "Lucía", "12345678");
        var primero = lote(fila, Map.of("salud", antecedente("Dato histórico uno")));
        var original = registro();
        var base = original.confirmar(original.previsualizar(primero, politica, aip, AHORA), politica, cambio());
        var segundo = lote(fila, Map.of("salud", antecedente("Dato histórico dos")));
        base = base.confirmar(base.previsualizar(segundo, politica, aip, AHORA), politica, cambio());
        var tercero = new LoteImportacion(matricula(List.of(fila)), alcance,
                Map.of(7, new LoteImportacion.FilaContexto(Map.of(), Set.of("salud"))));
        var nuevo = base.confirmar(base.previsualizar(tercero, politica, aip, AHORA), politica, cambio());
        var ambito = contexto(fila.identidadParaAlta());
        var historia = nuevo.consultarHistoriaContexto(fila.identidadParaAlta(), ambito, politica, toe, AHORA);
        assertEquals(3, historia.size());
        assertEquals("Dato histórico uno", historia.getFirst().resultado().antecedentes().get("salud").valor().contenido());
        assertEquals("Dato histórico dos", historia.getLast().anterior().antecedentes().get("salud").valor().contenido());
        assertTrue(historia.getLast().resultado().antecedentes().isEmpty());
        assertThrows(SecurityException.class, () -> nuevo.consultarHistoriaContexto(fila.identidadParaAlta(), ambito, politica, aip, AHORA));
        assertFalse(nuevo.consultarResultadoLote(tercero.matricula().id(), politica, aip, AHORA).toString().contains("Dato histórico"));
        var reconstituido = RegistroImportacion.cargar(nuevo.matriculas(), List.copyOf(nuevo.contextos().values()),
                List.copyOf(nuevo.confirmaciones().values()), nuevo.historias());
        assertEquals(historia, reconstituido.consultarHistoriaContexto(fila.identidadParaAlta(), ambito, politica, toe, AHORA));
        assertThrows(UnsupportedOperationException.class, () -> historia.clear());
    }

    @Test
    void ambitoDeOtroEstudianteNoAutorizaConsultarContextoNiHistoria() {
        var fila = fila(7, "Lucía", "12345678");
        var lote = lote(fila, Map.of("salud", no()));
        var original = registro();
        var nuevo = original.confirmar(original.previsualizar(lote, politica, aip, AHORA), politica, cambio());
        assertThrows(IllegalArgumentException.class, () -> nuevo.consultarContexto(fila.identidadParaAlta(), contexto(UUID.randomUUID()), politica, toe, AHORA));
        assertThrows(SecurityException.class, () -> nuevo.consultarContexto(fila.identidadParaAlta(), contexto(fila.identidadParaAlta()), politica, admin, AHORA));
    }

    @Test
    void exclusionInstitucionalImpideConsultaDelContextoAunqueTengaRolToe() {
        var fila = fila(7, "Lucía", "12345678");
        var lote = lote(fila, Map.of("salud", no()));
        var original = registro();
        var nuevo = original.confirmar(original.previsualizar(lote, politica, aip, AHORA), politica, cambio());
        var contexto = contexto(fila.identidadParaAlta());
        var director = UUID.randomUUID();
        var cambioAdmin = new RegistroCambio(admin, INICIO, Optional.empty());
        var d = directorio.registrarUsuario(director, "Director", "director", cambioAdmin)
                .asignarRol(director, UUID.randomUUID(), RolUsuario.DIRECTOR, new Vigencia(INICIO, Optional.empty()), cambioAdmin);
        var autoridad = new PoliticaAcceso(d, RestriccionesAmbito.vacias());
        var restringida = autoridad.excluir(UUID.randomUUID(), toe, contexto,
                new RegistroCambio(director, AHORA, Optional.of("Conflicto institucional ficticio")));
        var p = new PoliticaAcceso(d, restringida);
        assertThrows(SecurityException.class, () -> nuevo.consultarContexto(fila.identidadParaAlta(), contexto, p, toe, AHORA));
    }

    @Test
    void bloqueoPosteriorALaVistaImpideGuardarFilaYConsultarResultado() {
        var fila = fila(7, "Lucía", "12345678");
        var lote = lote(fila, Map.of("salud", no()));
        var original = registro();
        var vista = original.previsualizar(lote, politica, aip, AHORA);
        var alcance = AlcanceProteccion.persona(fila.identidadParaAlta());
        var mandato = new AutorizacionProteccion(UUID.randomUUID(), admin, Set.of(AutorizacionProteccion.Operacion.BLOQUEAR),
                alcance, new Vigencia(INICIO, Optional.empty()), "Responsable institucional", "Constancia 01", "Fundamento", INICIO);
        var control = ControlProteccion.vacio().bloquear(UUID.randomUUID(), alcance, mandato,
                new RegistroCambio(admin, AHORA, Optional.of("Bloqueo para rectificación")), directorio);
        var bloqueada = new PoliticaAcceso(directorio, RestriccionesAmbito.vacias(), control);
        assertThrows(SecurityException.class, () -> original.confirmar(vista, bloqueada, cambio()));
        assertFalse(original.previsualizar(lote, bloqueada, aip, AHORA).filas().getFirst().aceptable());
        var guardado = original.confirmar(vista, politica, cambio());
        assertThrows(SecurityException.class, () -> guardado.consultarResultadoLote(lote.matricula().id(), bloqueada, aip, AHORA));
        assertThrows(SecurityException.class, () -> guardado.confirmar(vista, bloqueada, cambio()));
        assertThrows(SecurityException.class, () -> guardado.consultarContexto(fila.identidadParaAlta(), contexto(fila.identidadParaAlta()), bloqueada, toe, AHORA));
    }

    private ContextoAcceso contexto(UUID estudiante) {
        var clasificacion = new ClasificacionCategoria(UUID.randomUUID(), "Protección", NaturalezaCategoria.VIOLENCIA_PROTECCION,
                Gravedad.GRAVE, true);
        var datos = new DatosReporte(FechaHecho.exacta(LocalDate.of(2026, 10, 1)), 2026, clasificacion, "Hecho ficticio para atención",
                List.of(new Participacion(PersonaInvolucrada.estudiante(estudiante, "Lucía"), PapelParticipacion.AFECTADO,
                        Optional.empty())), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return ContextoAcceso.deHecho(Hecho.de(ReporteIncidencia.registrar(UUID.randomUUID(), datos, toe, AHORA)));
    }

    @Test
    void homonimoBloqueadoConOtroDocumentoNoImpideIdentidadSeguraNiExponeCandidato() {
        var reservado = Estudiante.crear(UUID.randomUUID(), "Lucía").incorporarIdentidad(Optional.empty(),
                Optional.of(new DocumentoIdentidad("DNI", "87654321")), Optional.empty(), Optional.empty());
        var politicaActual = politicaBloqueada(reservado.id());
        var f = fila(7, "Lucía", "12345678");
        var lote = lote(f, Map.of("salud", no()));
        var original = registro(reservado);
        var vista = original.previsualizar(lote, politicaActual, aip, AHORA);
        var resultado = vista.filas().getFirst().matricula();
        assertEquals(RegistroMatriculas.Estado.REQUIERE_CONCILIACION, resultado.estado());
        assertTrue(resultado.candidatos().isEmpty());
        assertFalse(resultado.toString().contains("87654321"));
        assertFalse(resultado.toString().contains(reservado.id().toString()));
        var m = lote.matricula();
        var resuelto = new LoteImportacion(new LoteMatricula(m.id(), m.procedencia(), m.hoja(), m.anio(), m.fechaEfectiva(),
                m.grupoComun(), m.filas(), Map.of(7, LoteMatricula.Resolucion.crear())), alcance, lote.contexto());
        var nuevo = original.confirmar(original.previsualizar(resuelto, politicaActual, aip, AHORA), politicaActual, cambio());
        assertEquals(2, nuevo.matriculas().estudiantes().size());
        assertSame(reservado, nuevo.matriculas().estudiantes().get(reservado.id()));
        var siguiente = lote(f, Map.of("salud", antecedente("Actualización pertinente")));
        var segura = nuevo.previsualizar(siguiente, politicaActual, aip, AHORA);
        assertTrue(segura.filas().getFirst().aceptable());
        assertEquals(Optional.of(f.identidadParaAlta()), segura.filas().getFirst().matricula().estudianteId());
        assertEquals(0, segura.altas());
        assertEquals(1, segura.actualizaciones());
    }

    @Test
    void candidatoPorNombreBloqueadoSinDocumentoNoPuedeSortearseConAltaExpresa() {
        var reservado = Estudiante.crear(UUID.randomUUID(), "Lucía");
        var politicaActual = politicaBloqueada(reservado.id());
        var f = fila(7, "Lucía", "12345678");
        var m = matricula(List.of(f));
        var original = registro(reservado);
        var lote = new LoteImportacion(m, alcance, Map.of());
        var vista = original.previsualizar(lote, politicaActual, aip, AHORA);
        assertEquals(RegistroMatriculas.Estado.ERROR, vista.filas().getFirst().matricula().estado());
        assertTrue(vista.filas().getFirst().matricula().candidatos().isEmpty());
        var explicito = new LoteImportacion(new LoteMatricula(m.id(), m.procedencia(), m.hoja(), m.anio(), m.fechaEfectiva(),
                m.grupoComun(), m.filas(), Map.of(7, LoteMatricula.Resolucion.crear())), alcance, Map.of());
        var guardado = original.confirmar(original.previsualizar(explicito, politicaActual, aip, AHORA), politicaActual, cambio());
        assertEquals(1, guardado.matriculas().estudiantes().size());
        assertFalse(guardado.matriculas().estudiantes().containsKey(f.identidadParaAlta()));
    }

    @Test
    void documentoBloqueadoNoSeIncorporaCambiandoNombreOForzandoAlta() {
        var reservado = Estudiante.crear(UUID.randomUUID(), "Persona reservada").incorporarIdentidad(Optional.empty(),
                Optional.of(new DocumentoIdentidad("DNI", "12345678")), Optional.empty(), Optional.empty());
        var politicaActual = politicaBloqueada(reservado.id());
        var f = fila(7, "Nombre diferente", "12345678");
        var m = matricula(List.of(f));
        var lote = new LoteImportacion(new LoteMatricula(m.id(), m.procedencia(), m.hoja(), m.anio(), m.fechaEfectiva(),
                m.grupoComun(), m.filas(), Map.of(7, LoteMatricula.Resolucion.crear())), alcance, Map.of());
        var vista = registro(reservado).previsualizar(lote, politicaActual, aip, AHORA);
        assertFalse(vista.filas().getFirst().aceptable());
        assertFalse(vista.filas().toString().contains("Persona reservada"));
        assertFalse(vista.filas().toString().contains(reservado.id().toString()));
    }

    private PoliticaAcceso politicaBloqueada(UUID estudiante) {
        var alcance = AlcanceProteccion.persona(estudiante);
        var mandato = new AutorizacionProteccion(UUID.randomUUID(), admin, Set.of(AutorizacionProteccion.Operacion.BLOQUEAR),
                alcance, new Vigencia(INICIO, Optional.empty()), "Responsable institucional", "Constancia 01", "Fundamento", INICIO);
        var control = ControlProteccion.vacio().bloquear(UUID.randomUUID(), alcance, mandato,
                new RegistroCambio(admin, AHORA, Optional.of("Bloqueo para rectificación")), directorio);
        return new PoliticaAcceso(directorio, RestriccionesAmbito.vacias(), control);
    }

    private ConfiguracionInstitucional configuracion(AlcanceImportacion a) {
        var datos = new DatosInstitucion("Colegio ficticio", Optional.empty(), Optional.empty(), Optional.empty());
        var definicion = new DefinicionInstitucional(datos, Optional.empty(), Optional.of(a), Map.of(), Set.of(), OptionalInt.empty());
        return ConfiguracionInstitucional.crear(definicion, politica, new RegistroCambio(toe, INICIO, Optional.empty()))
                .registrarAnio(2026, politica, new RegistroCambio(admin, INICIO, Optional.empty()))
                .definirGrupos(2026, Set.of(A, B), politica, new RegistroCambio(admin, INICIO, Optional.empty()));
    }
    private static AlcanceImportacion.Concepto concepto(String clave, AlcanceImportacion.Tipo tipo) {
        return new AlcanceImportacion.Concepto(clave, "Etiqueta " + clave, tipo, "Fundamento ficticio");
    }
    private static ContactoFamiliar contacto() {
        return new ContactoFamiliar(Optional.of("Contacto familiar"), Optional.empty(), Optional.of("012345678"), Optional.empty(), Optional.empty());
    }
    private static LoteImportacion.Antecedente no() {
        return new LoteImportacion.Antecedente("no", ContextoEstudiante.Calidad.NEGACION_DECLARADA,
                Optional.empty(), Optional.of("año anterior"));
    }
    private static LoteImportacion.Antecedente antecedente(String contenido) {
        return new LoteImportacion.Antecedente(contenido, ContextoEstudiante.Calidad.DECLARADO, Optional.of(LocalDate.of(2025, 10, 1)), Optional.empty());
    }
    private DirectorioUsuarios directorio() {
        var cambio = new RegistroCambio(admin, INICIO, Optional.of("Designación institucional ficticia"));
        return DirectorioUsuarios.inicializar(admin, "Administrador", "admin", cambio)
                .registrarUsuario(aip, "Operador AIP", "aip", cambio)
                .asignarRol(aip, UUID.randomUUID(), RolUsuario.AIP, new Vigencia(INICIO, Optional.empty()), cambio)
                .registrarUsuario(toe, "Responsable TOE", "toe", cambio)
                .asignarRol(toe, UUID.randomUUID(), RolUsuario.TOE, new Vigencia(INICIO, Optional.empty()), cambio);
    }
    private static RegistroImportacion registro(Estudiante... estudiantes) {
        return RegistroImportacion.cargar(RegistroMatriculas.cargar(List.of(estudiantes), List.of()), List.of(), List.of());
    }
    private RegistroCambio cambio() { return new RegistroCambio(aip, AHORA, Optional.empty()); }
    private LoteImportacion lote(FilaMatricula fila, Map<String, LoteImportacion.Entrada> valores) {
        return new LoteImportacion(matricula(List.of(fila)), alcance, Map.of(fila.numero(), new LoteImportacion.FilaContexto(valores, Set.of())));
    }
    private static LoteMatricula matricula(List<FilaMatricula> filas) {
        return new LoteMatricula(UUID.randomUUID(), "Nómina ficticia", "Hoja1", 2026, FECHA, Optional.empty(), filas, Map.of());
    }
    private static FilaMatricula fila(int numero, String nombre, String documento) {
        return new FilaMatricula(numero, UUID.randomUUID(), nombre, Optional.ofNullable(documento).map(d -> new DocumentoIdentidad("DNI", d)),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
