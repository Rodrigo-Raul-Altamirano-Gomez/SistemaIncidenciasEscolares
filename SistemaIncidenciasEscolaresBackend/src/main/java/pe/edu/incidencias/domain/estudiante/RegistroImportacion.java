package pe.edu.incidencias.domain.estudiante;

import pe.edu.incidencias.domain.acceso.ContextoAcceso;
import pe.edu.incidencias.domain.acceso.Permiso;
import pe.edu.incidencias.domain.acceso.PoliticaAcceso;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.institucion.ConfiguracionInstitucional;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Frontera transaccional pura de matrícula y contexto: se persiste la instantánea completa o ninguna. */
public final class RegistroImportacion {
    public enum Accion { INCORPORAR, SUSTITUIR, BORRAR, SIN_CAMBIOS }
    /** El AIP conoce el alcance y procedencia anteriores, sin recibir sus valores reservados. */
    public record CambioContexto(String concepto, Accion accion, Optional<ContextoEstudiante.Procedencia> anterior) {
        public CambioContexto {
            Objects.requireNonNull(concepto, "concepto");
            Objects.requireNonNull(accion, "accion");
            Objects.requireNonNull(anterior, "anterior");
        }
    }
    public record ResultadoFila(RegistroMatriculas.ResultadoFila matricula, List<CambioContexto> contexto,
                                List<String> erroresContexto, Optional<RectificacionMatricula> rectificacion) {
        public ResultadoFila {
            contexto = List.copyOf(contexto);
            erroresContexto = List.copyOf(erroresContexto);
            Objects.requireNonNull(rectificacion, "rectificacion");
        }
        public boolean aceptable() { return matricula.aceptable() && erroresContexto.isEmpty(); }
        public boolean modificaContexto() { return contexto.stream().anyMatch(c -> c.accion() != Accion.SIN_CAMBIOS); }
    }
    /** Sin contenido del lote descartado ni valores anteriores. Mantener protegida como metadatos internos. */
    public record Confirmacion(UUID loteId, String huella, RegistroCambio cambio, List<ResultadoFila> filas) {
        public Confirmacion { filas = List.copyOf(filas); }
    }
    /** Historia reservada de rectificaciones, no una vista de resultados de AIP. */
    public record RevisionContexto(UUID loteId, RegistroCambio cambio, ContextoEstudiante anterior,
                                   ContextoEstudiante resultado) {
        public RevisionContexto {
            Objects.requireNonNull(loteId, "loteId");
            Objects.requireNonNull(cambio, "cambio");
            Objects.requireNonNull(anterior, "anterior");
            Objects.requireNonNull(resultado, "resultado");
            if (!anterior.estudianteId().equals(resultado.estudianteId()))
                throw new IllegalArgumentException("Una revisión no cambia la identidad del contexto");
        }
    }
    public static final class Previsualizacion {
        private final RegistroImportacion base;
        private final LoteImportacion lote;
        private final UUID autor;
        private final Instant fecha;
        private final RegistroMatriculas.Previsualizacion matricula;
        private final List<ResultadoFila> filas;

        private Previsualizacion(RegistroImportacion base, LoteImportacion lote, UUID autor, Instant fecha,
                                 RegistroMatriculas.Previsualizacion matricula, List<ResultadoFila> filas) {
            this.base = base;
            this.lote = lote;
            this.autor = autor;
            this.fecha = fecha;
            this.matricula = matricula;
            this.filas = List.copyOf(filas);
        }
        public LoteImportacion lote() { return lote; }
        public List<ResultadoFila> filas() { return filas; }
        public long altas() { return filas.stream().filter(f -> f.aceptable()
                && f.matricula().estado() == RegistroMatriculas.Estado.ALTA).count(); }
        public long actualizaciones() { return filas.stream().filter(f -> f.aceptable()
                && f.matricula().estado() != RegistroMatriculas.Estado.ALTA
                && (f.matricula().estado() == RegistroMatriculas.Estado.ACTUALIZACION || f.modificaContexto())).count(); }
    }

    private final RegistroMatriculas matriculas;
    private final Map<UUID, ContextoEstudiante> contextos;
    private final Map<UUID, Confirmacion> confirmaciones;
    private final Map<UUID, List<RevisionContexto>> historias;

    private RegistroImportacion(RegistroMatriculas matriculas, Map<UUID, ContextoEstudiante> contextos,
                                Map<UUID, Confirmacion> confirmaciones, Map<UUID, List<RevisionContexto>> historias) {
        this.matriculas = Objects.requireNonNull(matriculas, "matriculas");
        this.contextos = Map.copyOf(contextos);
        this.confirmaciones = Map.copyOf(confirmaciones);
        var copia = new HashMap<UUID, List<RevisionContexto>>();
        historias.forEach((id, revisiones) -> copia.put(id, List.copyOf(revisiones)));
        this.historias = Map.copyOf(copia);
    }

    /** Entrada de reconstitución confiable, sin acceso de usuario. Las tres partes son una unidad de persistencia. */
    public static RegistroImportacion cargar(RegistroMatriculas matriculas, List<ContextoEstudiante> contextos,
                                             List<Confirmacion> confirmaciones) {
        return cargar(matriculas, contextos, confirmaciones, Map.of());
    }

    public static RegistroImportacion cargar(RegistroMatriculas matriculas, List<ContextoEstudiante> contextos,
                                             List<Confirmacion> confirmaciones, Map<UUID, List<RevisionContexto>> historias) {
        var porPersona = new HashMap<UUID, ContextoEstudiante>();
        for (var contexto : List.copyOf(contextos)) {
            if (!matriculas.estudiantes().containsKey(contexto.estudianteId()))
                throw new IllegalArgumentException("Contexto sin estudiante");
            if (porPersona.putIfAbsent(contexto.estudianteId(), contexto) != null)
                throw new IllegalArgumentException("Contexto repetido");
        }
        var porLote = new HashMap<UUID, Confirmacion>();
        for (var confirmacion : List.copyOf(confirmaciones)) {
            if (!matriculas.confirmaciones().containsKey(confirmacion.loteId()))
                throw new IllegalArgumentException("Contexto confirmado sin confirmación de matrícula");
            if (porLote.putIfAbsent(confirmacion.loteId(), confirmacion) != null)
                throw new IllegalArgumentException("Lote contextual repetido");
        }
        historias.forEach((id, revisiones) -> {
            if (!porPersona.containsKey(id)) throw new IllegalArgumentException("Historia sin contexto vigente");
            Instant fechaAnterior = Instant.MIN;
            ContextoEstudiante estadoAnterior = null;
            for (var revision : revisiones) {
                if (!revision.resultado().estudianteId().equals(id) || !porLote.containsKey(revision.loteId()))
                    throw new IllegalArgumentException("Historia contextual con referencia ajena");
                if (revision.cambio().fecha().isBefore(fechaAnterior)
                        || estadoAnterior != null && !estadoAnterior.equals(revision.anterior()))
                    throw new IllegalArgumentException("Historia contextual incoherente");
                fechaAnterior = revision.cambio().fecha();
                estadoAnterior = revision.resultado();
            }
            if (estadoAnterior != null && !estadoAnterior.equals(porPersona.get(id)))
                throw new IllegalArgumentException("Historia distinta del contexto vigente");
        });
        return new RegistroImportacion(matriculas, porPersona, porLote, historias);
    }
    /** Para repositorio confiable; no serializar estos accesores como una respuesta a AIP. */
    public RegistroMatriculas matriculas() { return matriculas; }
    public Map<UUID, ContextoEstudiante> contextos() { return contextos; }
    public Map<UUID, Confirmacion> confirmaciones() { return confirmaciones; }
    public Map<UUID, List<RevisionContexto>> historias() { return historias; }

    public Previsualizacion previsualizar(LoteImportacion lote, ConfiguracionInstitucional configuracion,
                                          PoliticaAcceso politica, UUID autor, Instant fecha) {
        validarConfiguracion(lote, configuracion, fecha);
        return previsualizar(lote, politica, autor, fecha);
    }

    Previsualizacion previsualizar(LoteImportacion lote, PoliticaAcceso politica, UUID autor, Instant fecha) {
        autorizar(politica, autor, fecha);
        Objects.requireNonNull(lote, "lote");
        var previa = confirmaciones.get(lote.matricula().id());
        if (previa != null) {
            exigirReintento(lote, previa, autor);
            previa.filas().stream().filter(ResultadoFila::aceptable).forEach(f ->
                    politica.exigirPersonaDisponible(f.matricula().estudianteId().orElseThrow(), fecha));
            return new Previsualizacion(this, lote, autor, fecha, null, previa.filas());
        }
        if (matriculas.confirmaciones().containsKey(lote.matricula().id()))
            throw new IllegalArgumentException("ID ya usado por matrícula sin contexto; utilice un nuevo lote");
        var errores = new HashMap<Integer, List<String>>();
        var resoluciones = new HashMap<>(lote.matricula().resoluciones());
        for (var fila : lote.contexto().entrySet()) {
            if (Optional.ofNullable(resoluciones.get(fila.getKey()))
                    .filter(r -> r.accion() == LoteMatricula.Accion.EXCLUIR).isPresent()) continue;
            var problemas = validar(lote.alcance(), fila.getValue());
            if (!problemas.isEmpty()) {
                errores.put(fila.getKey(), problemas);
                resoluciones.put(fila.getKey(), LoteMatricula.Resolucion.excluir());
            }
        }
        var m = lote.matricula();
        var filtrado = new LoteMatricula(m.id(), m.procedencia(), m.hoja(), m.anio(), m.fechaEfectiva(),
                m.grupoComun(), m.filas(), resoluciones, m.rectificaciones());
        var propuesta = matriculas.previsualizar(filtrado, politica, autor, fecha);
        var filas = new ArrayList<ResultadoFila>();
        for (var resultado : propuesta.filas()) {
            var cambios = new ArrayList<CambioContexto>();
            if (resultado.aceptable()) {
                var contexto = contextos.getOrDefault(resultado.estudianteId().orElseThrow(),
                        ContextoEstudiante.vacio(resultado.estudianteId().orElseThrow()));
                describir(lote, resultado.fila(), contexto, cambios);
            }
            filas.add(new ResultadoFila(resultado, cambios, errores.getOrDefault(resultado.fila(), List.of()),
                    Optional.ofNullable(m.rectificaciones().get(resultado.fila()))));
        }
        return new Previsualizacion(this, lote, autor, fecha, propuesta, filas);
    }

    public RegistroImportacion confirmar(Previsualizacion propuesta, ConfiguracionInstitucional configuracion,
                                          PoliticaAcceso politica, RegistroCambio cambio) {
        validarConfiguracion(propuesta.lote, configuracion, cambio.fecha());
        return confirmar(propuesta, politica, cambio);
    }

    RegistroImportacion confirmar(Previsualizacion propuesta, PoliticaAcceso politica, RegistroCambio cambio) {
        Objects.requireNonNull(propuesta, "propuesta");
        autorizar(politica, cambio.autorId(), cambio.fecha());
        if (!propuesta.autor.equals(cambio.autorId())) throw new SecurityException("Debe confirmar el operador que revisó el lote");
        if (cambio.fecha().isBefore(propuesta.fecha)) throw new IllegalArgumentException("Confirmación anterior a la revisión");
        var previa = confirmaciones.get(propuesta.lote.matricula().id());
        if (previa != null) {
            exigirReintento(propuesta.lote, previa, cambio.autorId());
            previa.filas().stream().filter(ResultadoFila::aceptable).forEach(f ->
                    politica.exigirPersonaDisponible(f.matricula().estudianteId().orElseThrow(), cambio.fecha()));
            return this;
        }
        if (propuesta.base != this) throw new IllegalArgumentException("Previsualización obsoleta; vuelva a revisar");
        var nuevasMatriculas = matriculas.confirmar(propuesta.matricula, politica, cambio);
        var nuevosContextos = new HashMap<>(contextos);
        var nuevasHistorias = new HashMap<>(historias);
        for (var fila : propuesta.filas) {
            if (!fila.aceptable()) continue;
            UUID persona = fila.matricula().estudianteId().orElseThrow();
            politica.exigirPersonaDisponible(persona, cambio.fecha());
            var anterior = contextos.getOrDefault(persona, ContextoEstudiante.vacio(persona));
            var resultado = aplicar(propuesta.lote, fila.matricula().fila(), anterior, cambio);
            nuevosContextos.put(persona, resultado);
            if (!resultado.equals(anterior)) {
                var revisiones = new ArrayList<>(historias.getOrDefault(persona, List.of()));
                revisiones.add(new RevisionContexto(propuesta.lote.matricula().id(), cambio, anterior, resultado));
                nuevasHistorias.put(persona, List.copyOf(revisiones));
            }
        }
        var historia = new HashMap<>(confirmaciones);
        // No conservar los candidatos de conciliación, ni los antecedentes aportados por filas rechazadas.
        var resumen = propuesta.filas.stream().map(f -> new ResultadoFila(new RegistroMatriculas.ResultadoFila(
                f.matricula().fila(), f.matricula().estado(), f.matricula().estudianteId(), List.of(),
                f.matricula().cambios(), f.matricula().avisos()), f.contexto(), f.erroresContexto(), f.rectificacion())).toList();
        historia.put(propuesta.lote.matricula().id(), new Confirmacion(propuesta.lote.matricula().id(),
                huella(propuesta.lote), cambio, resumen));
        return new RegistroImportacion(nuevasMatriculas, nuevosContextos, historia, nuevasHistorias);
    }

    /** AIP puede recuperar el resultado sin navegar desde sus referencias al contexto histórico. */
    public Confirmacion consultarResultadoLote(UUID loteId, PoliticaAcceso politica, UUID autor, Instant fecha) {
        autorizar(politica, autor, fecha);
        var resultado = Optional.ofNullable(confirmaciones.get(loteId))
                .orElseThrow(() -> new IllegalArgumentException("Lote no disponible"));
        if (!resultado.cambio().autorId().equals(autor)) throw new SecurityException("Lote de otro operador");
        resultado.filas().stream().filter(ResultadoFila::aceptable).forEach(f ->
                politica.exigirPersonaDisponible(f.matricula().estudianteId().orElseThrow(), fecha));
        return resultado;
    }

    public ContextoEstudiante consultarContexto(UUID estudianteId, ContextoAcceso ambito,
                                                 PoliticaAcceso politica, UUID autor, Instant fecha) {
        politica.exigirPersonaDisponible(estudianteId, fecha);
        if (!politica.evaluar(autor, Permiso.CONSULTAR_CONTEXTO_INSTITUCIONAL, ambito, fecha).permitida())
            throw new SecurityException("Contexto reservado a atención institucional autorizada");
        if (!matriculas.estudiantes().containsKey(estudianteId)) throw new IllegalArgumentException("Estudiante inexistente");
        // El ámbito debe contener la persona consultada; no autoriza consultar otra identidad arbitraria.
        if (ambito.hechos().stream().flatMap(h -> h.reportes().stream()).flatMap(r -> r.datos().participaciones().stream())
                .noneMatch(p -> p.persona().id().equals(estudianteId)))
            throw new IllegalArgumentException("El estudiante no pertenece al ámbito de atención aportado");
        return contextos.getOrDefault(estudianteId, ContextoEstudiante.vacio(estudianteId));
    }

    /** Lectura longitudinal para atención autorizada; jamás se ofrece desde resultados de importación. */
    public List<RevisionContexto> consultarHistoriaContexto(UUID estudianteId, ContextoAcceso ambito,
                                                            PoliticaAcceso politica, UUID autor, Instant fecha) {
        consultarContexto(estudianteId, ambito, politica, autor, fecha);
        var historia = historias.getOrDefault(estudianteId, List.of());
        if (historia.stream().anyMatch(r -> r.cambio().fecha().isAfter(fecha)))
            throw new IllegalArgumentException("Historia posterior a la consulta");
        return historia;
    }

    private static void validarConfiguracion(LoteImportacion lote, ConfiguracionInstitucional configuracion, Instant fecha) {
        Objects.requireNonNull(configuracion, "configuracion");
        if (configuracion.revisiones().stream().anyMatch(r -> r.cambio().fecha().isAfter(fecha)))
            throw new IllegalArgumentException("Configuración posterior a la operación");
        if (!configuracion.estado().alcanceImportacion().equals(Optional.of(lote.alcance())))
            throw new IllegalArgumentException("El alcance del lote no es el institucional vigente; revise las asociaciones");
        if (!configuracion.admiteGrupo(lote.matricula().anio(), lote.matricula().grupoComun()))
            throw new IllegalArgumentException("Grupo común no admitido para el año");
        for (var fila : lote.matricula().filas()) {
            if (!configuracion.admiteGrupo(lote.matricula().anio(), fila.grupo()))
                throw new IllegalArgumentException("Grupo no admitido en fila " + fila.numero());
        }
    }

    private void autorizar(PoliticaAcceso politica, UUID autor, Instant fecha) {
        if (!politica.evaluar(autor, Permiso.IMPORTAR_NOMINA, fecha).permitida())
            throw new SecurityException("Se requiere AIP vigente");
        if (confirmaciones.values().stream().anyMatch(c -> c.cambio().fecha().isAfter(fecha)))
            throw new IllegalArgumentException("Operación anterior al estado vigente");
    }

    private static List<String> validar(AlcanceImportacion alcance, LoteImportacion.FilaContexto fila) {
        var errores = new ArrayList<String>();
        fila.valores().forEach((clave, entrada) -> {
            var concepto = alcance.conceptos().get(clave);
            if (concepto == null) errores.add("Campo no admitido: " + clave + "; omita o corrija su asociación");
            else if (!compatible(concepto.tipo(), entrada)) errores.add("Asociación incompatible para " + clave);
        });
        fila.borrar().forEach(clave -> {
            if (!alcance.conceptos().containsKey(clave)) errores.add("Borrado fuera del alcance admitido: " + clave);
        });
        return List.copyOf(errores);
    }
    private static boolean compatible(AlcanceImportacion.Tipo tipo, LoteImportacion.Entrada entrada) {
        return switch (tipo) {
            case CONTACTO_FAMILIAR -> entrada instanceof LoteImportacion.Familiar;
            case DOMICILIO, TELEFONO_ESTUDIANTE -> entrada instanceof LoteImportacion.Texto;
            case ANTECEDENTE -> entrada instanceof LoteImportacion.Antecedente;
        };
    }
    private static void describir(LoteImportacion lote, int numero, ContextoEstudiante contexto, List<CambioContexto> cambios) {
        var fila = lote.contexto().getOrDefault(numero, LoteImportacion.FilaContexto.vacia());
        fila.valores().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            var concepto = lote.alcance().conceptos().get(e.getKey());
            var anterior = anterior(contexto, concepto);
            Object valor = valor(concepto, e.getValue(), anterior);
            var accion = anterior.isEmpty() ? Accion.INCORPORAR : anterior.get().valor().equals(valor)
                    ? Accion.SIN_CAMBIOS : Accion.SUSTITUIR;
            cambios.add(new CambioContexto(e.getKey(), accion, anterior.map(ContextoEstudiante.Dato::procedencia)));
        });
        fila.borrar().stream().sorted().forEach(clave -> {
            var anterior = anterior(contexto, lote.alcance().conceptos().get(clave));
            cambios.add(new CambioContexto(clave, anterior.isPresent() ? Accion.BORRAR : Accion.SIN_CAMBIOS,
                    anterior.map(ContextoEstudiante.Dato::procedencia)));
        });
    }
    private static Optional<? extends ContextoEstudiante.Dato<?>> anterior(ContextoEstudiante contexto,
                                                                            AlcanceImportacion.Concepto concepto) {
        return switch (concepto.tipo()) {
            case CONTACTO_FAMILIAR -> contexto.contacto();
            case TELEFONO_ESTUDIANTE -> contexto.telefonoEstudiante();
            case DOMICILIO -> contexto.domicilio();
            case ANTECEDENTE -> Optional.ofNullable(contexto.antecedentes().get(concepto.clave()));
        };
    }
    private static Object valor(AlcanceImportacion.Concepto concepto, LoteImportacion.Entrada entrada,
                                Optional<? extends ContextoEstudiante.Dato<?>> previo) {
        return switch (entrada) {
            case LoteImportacion.Familiar familiar -> previo.map(ContextoEstudiante.Dato::valor)
                    .filter(ContactoFamiliar.class::isInstance).map(ContactoFamiliar.class::cast)
                    .map(c -> c.incorporar(familiar.contacto())).orElse(familiar.contacto());
            case LoteImportacion.Texto texto -> texto.valor();
            case LoteImportacion.Antecedente a -> new ContextoEstudiante.Antecedente(concepto.etiqueta(),
                    a.contenido(), a.calidad(), a.fecha(), a.periodo());
        };
    }
    private static ContextoEstudiante aplicar(LoteImportacion lote, int numero, ContextoEstudiante contexto, RegistroCambio cambio) {
        var fila = lote.contexto().getOrDefault(numero, LoteImportacion.FilaContexto.vacia());
        var contacto = contexto.contacto();
        var telefono = contexto.telefonoEstudiante();
        var domicilio = contexto.domicilio();
        var antecedentes = new HashMap<>(contexto.antecedentes());
        for (var e : fila.valores().entrySet()) {
            var concepto = lote.alcance().conceptos().get(e.getKey());
            var previo = anterior(contexto, concepto);
            var valor = valor(concepto, e.getValue(), previo);
            if (previo.filter(d -> d.valor().equals(valor)).isPresent()) continue;
            var procedencia = new ContextoEstudiante.Procedencia(lote.matricula().id(), lote.matricula().procedencia(),
                    lote.matricula().hoja(), numero, lote.alcance().id(), concepto.clave(), concepto.fundamento(), cambio);
            switch (concepto.tipo()) {
                case CONTACTO_FAMILIAR -> contacto = Optional.of(new ContextoEstudiante.Dato<>((ContactoFamiliar) valor, procedencia));
                case TELEFONO_ESTUDIANTE -> telefono = Optional.of(new ContextoEstudiante.Dato<>((String) valor, procedencia));
                case DOMICILIO -> domicilio = Optional.of(new ContextoEstudiante.Dato<>((String) valor, procedencia));
                case ANTECEDENTE -> antecedentes.put(e.getKey(), new ContextoEstudiante.Dato<>((ContextoEstudiante.Antecedente) valor, procedencia));
            }
        }
        for (var clave : fila.borrar()) switch (lote.alcance().conceptos().get(clave).tipo()) {
            case CONTACTO_FAMILIAR -> contacto = Optional.empty();
            case TELEFONO_ESTUDIANTE -> telefono = Optional.empty();
            case DOMICILIO -> domicilio = Optional.empty();
            case ANTECEDENTE -> antecedentes.remove(clave);
        }
        return new ContextoEstudiante(contexto.estudianteId(), contacto, telefono, domicilio, antecedentes);
    }

    private static void exigirReintento(LoteImportacion lote, Confirmacion previa, UUID autor) {
        if (!previa.cambio().autorId().equals(autor)) throw new SecurityException("Lote de otro operador");
        if (!previa.huella().equals(huella(lote))) throw new IllegalArgumentException("ID de lote reutilizado con otro contenido");
    }
    private static String huella(LoteImportacion lote) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            agregar(digest, RegistroMatriculas.huella(lote.matricula()));
            agregar(digest, lote.alcance().id().toString());
            agregar(digest, lote.alcance().referenciaInstitucional());
            agregar(digest, Integer.toString(lote.alcance().conceptos().size()));
            lote.alcance().conceptos().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
                agregar(digest, e.getKey());
                agregar(digest, e.getValue().etiqueta());
                agregar(digest, e.getValue().tipo().name());
                agregar(digest, e.getValue().fundamento());
            });
            agregar(digest, Integer.toString(lote.contexto().size()));
            lote.contexto().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
                agregar(digest, e.getKey().toString());
                agregar(digest, Integer.toString(e.getValue().valores().size()));
                e.getValue().valores().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(v -> {
                    agregar(digest, v.getKey());
                    agregarEntrada(digest, v.getValue());
                });
                agregar(digest, Integer.toString(e.getValue().borrar().size()));
                e.getValue().borrar().stream().sorted().forEach(c -> agregar(digest, c));
            });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    private static void agregarEntrada(MessageDigest digest, LoteImportacion.Entrada entrada) {
        switch (entrada) {
            case LoteImportacion.Texto texto -> {
                agregar(digest, "texto");
                agregar(digest, texto.valor());
            }
            case LoteImportacion.Familiar familiar -> {
                agregar(digest, "familiar");
                var f = familiar.contacto();
                agregarOpcional(digest, f.nombre());
                agregarOpcional(digest, f.relacionInformada());
                agregarOpcional(digest, f.telefono());
                agregarOpcional(digest, f.documento().map(DocumentoIdentidad::tipo));
                agregarOpcional(digest, f.documento().map(DocumentoIdentidad::numero));
                agregarOpcional(digest, f.necesidadDocumento());
            }
            case LoteImportacion.Antecedente a -> {
                agregar(digest, "antecedente");
                agregar(digest, a.contenido());
                agregar(digest, a.calidad().name());
                agregarOpcional(digest, a.fecha().map(Object::toString));
                agregarOpcional(digest, a.periodo());
            }
        }
    }
    private static void agregarOpcional(MessageDigest digest, Optional<String> texto) {
        digest.update((byte) (texto.isPresent() ? 1 : 0));
        texto.ifPresent(t -> agregar(digest, t));
    }
    private static void agregar(MessageDigest digest, String texto) {
        var bytes = texto.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
