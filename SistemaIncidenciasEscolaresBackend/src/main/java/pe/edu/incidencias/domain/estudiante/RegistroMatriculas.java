package pe.edu.incidencias.domain.estudiante;

import pe.edu.incidencias.domain.acceso.Permiso;
import pe.edu.incidencias.domain.acceso.PoliticaAcceso;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Coordinación pura de identidad/matrícula. El llamador guarda el resultado completo en una transacción. */
public final class RegistroMatriculas {
    public enum Estado { ALTA, ACTUALIZACION, SIN_CAMBIOS, REQUIERE_CONCILIACION, ERROR, EXCLUIDA }
    public enum Campo { NOMBRE, DOCUMENTO, NACIMIENTO, SEXO, MATRICULA, GRUPO, SITUACION }
    public record ResultadoFila(int fila, Estado estado, Optional<UUID> estudianteId,
                                List<ConciliacionMatricula.Candidato> candidatos,
                                Set<Campo> cambios, List<String> avisos) {
        public ResultadoFila {
            candidatos = List.copyOf(candidatos);
            cambios = Set.copyOf(cambios);
            avisos = List.copyOf(avisos);
        }
        public boolean aceptable() {
            return estado == Estado.ALTA || estado == Estado.ACTUALIZACION || estado == Estado.SIN_CAMBIOS;
        }
    }
    /** Evidencia de lote sin archivo original, filas descartadas ni instantáneas anteriores del estudiante. */
    public record Confirmacion(UUID loteId, String huella, String procedencia, String hoja, int anio,
                               LocalDate fechaEfectiva, RegistroCambio cambio, List<ResultadoFila> filas) {
        public Confirmacion { filas = List.copyOf(filas); }
        public long cantidad(Estado estado) { return filas.stream().filter(f -> f.estado() == estado).count(); }
    }
    /** Solo este registro puede emitir una propuesta confirmable; no se acepta una vista fabricada por el cliente. */
    public static final class Previsualizacion {
        private final RegistroMatriculas base;
        private final LoteMatricula lote;
        private final UUID autorId;
        private final Instant fecha;
        private final List<ResultadoFila> filas;
        private final Map<UUID, Estudiante> incorporaciones;

        private Previsualizacion(RegistroMatriculas base, LoteMatricula lote, UUID autorId, Instant fecha,
                                 List<ResultadoFila> filas, Map<UUID, Estudiante> incorporaciones) {
            this.base = base;
            this.lote = lote;
            this.autorId = autorId;
            this.fecha = fecha;
            this.filas = List.copyOf(filas);
            this.incorporaciones = Map.copyOf(incorporaciones);
        }
        public LoteMatricula lote() { return lote; }
        public List<ResultadoFila> filas() { return filas; }
        public long cantidad(Estado estado) { return filas.stream().filter(f -> f.estado() == estado).count(); }
    }

    private final Map<UUID, Estudiante> estudiantes;
    private final Map<UUID, Confirmacion> confirmaciones;

    private RegistroMatriculas(Map<UUID, Estudiante> estudiantes, Map<UUID, Confirmacion> confirmaciones) {
        this.estudiantes = Map.copyOf(estudiantes);
        this.confirmaciones = Map.copyOf(confirmaciones);
    }

    /** Reconstitución confiable, no consulta AIP. Requiere índice completo de identidades y documentos. */
    public static RegistroMatriculas cargar(List<Estudiante> estudiantes, List<Confirmacion> confirmaciones) {
        var identidades = new HashMap<UUID, Estudiante>();
        for (var estudiante : List.copyOf(estudiantes)) {
            if (identidades.putIfAbsent(estudiante.id(), estudiante) != null)
                throw new IllegalArgumentException("Identidad de estudiante repetida");
        }
        var historia = new HashMap<UUID, Confirmacion>();
        for (var confirmacion : List.copyOf(confirmaciones)) {
            if (historia.putIfAbsent(confirmacion.loteId(), confirmacion) != null)
                throw new IllegalArgumentException("Confirmación de lote repetida");
        }
        return new RegistroMatriculas(identidades, historia);
    }

    /** Estado para persistencia e integración autorizada; no debe serializarse como respuesta al operador AIP. */
    public Map<UUID, Estudiante> estudiantes() { return estudiantes; }
    public Map<UUID, Confirmacion> confirmaciones() { return confirmaciones; }

    public Previsualizacion previsualizar(LoteMatricula lote, PoliticaAcceso politica, UUID autorId, Instant fecha) {
        autorizar(politica, autorId, fecha);
        Objects.requireNonNull(lote, "lote");
        var anterior = confirmaciones.get(lote.id());
        if (anterior != null) {
            if (!anterior.cambio().autorId().equals(autorId))
                throw new SecurityException("El resultado corresponde al lote de otro operador");
            exigirMismoLote(lote, anterior);
            anterior.filas().stream().filter(ResultadoFila::aceptable).forEach(f ->
                    politica.exigirPersonaDisponible(f.estudianteId().orElseThrow(), fecha));
            return new Previsualizacion(this, lote, autorId, fecha, anterior.filas(), Map.of());
        }
        var resultados = new ArrayList<ResultadoFila>();
        var propuestas = new HashMap<Integer, Estudiante>();
        for (var fila : lote.filas()) {
            if (!excluida(lote, fila) && identidadNoDisponible(lote, fila, politica, fecha)) {
                resultados.add(error(fila.numero(), "La identidad requiere resolución institucional antes de importar"));
            } else if (!excluida(lote, fila) && documentoRepetido(lote, fila)) {
                resultados.add(error(fila.numero(), "Documento repetido en el lote; corrija o excluya las filas en conflicto"));
            } else if (!excluida(lote, fila) && !lote.resoluciones().containsKey(fila.numero())
                    && nombreAmbiguoEnLote(lote, fila)) {
                resultados.add(new ResultadoFila(fila.numero(), Estado.REQUIERE_CONCILIACION, Optional.empty(),
                        List.of(), Set.of(), List.of("Nombre repetido sin documento en el lote; confirme identidades distintas o excluya")));
            } else {
                resultados.add(reservarCandidatos(preparar(lote, fila, propuestas), politica, fecha));
            }
        }

        // Todas las filas que compiten se excluyen: ni el orden del archivo ni la última escritura deciden.
        var repetidas = new HashSet<Integer>();
        var destinos = new HashMap<UUID, List<Integer>>();
        var documentos = new HashMap<DocumentoIdentidad, List<Integer>>();
        for (var resultado : resultados) {
            if (resultado.aceptable()) {
                var propuesta = propuestas.get(resultado.fila());
                destinos.computeIfAbsent(propuesta.id(), k -> new ArrayList<>()).add(resultado.fila());
                propuesta.documento().ifPresent(d -> documentos.computeIfAbsent(d, k -> new ArrayList<>()).add(resultado.fila()));
            }
        }
        destinos.values().stream().filter(ids -> ids.size() > 1).forEach(repetidas::addAll);
        documentos.values().stream().filter(ids -> ids.size() > 1).forEach(repetidas::addAll);

        // Comprueba contra el padrón original: no permite apropiarse de otro documento mediante el lote.
        for (var resultado : resultados) {
            if (!resultado.aceptable()) continue;
            var propuesta = propuestas.get(resultado.fila());
            if (propuesta.documento().isPresent() && estudiantes.values().stream().anyMatch(e ->
                    !e.id().equals(propuesta.id()) && e.documento().equals(propuesta.documento())))
                repetidas.add(resultado.fila());
        }
        var incorporaciones = new LinkedHashMap<UUID, Estudiante>();
        for (int i = 0; i < resultados.size(); i++) {
            var resultado = resultados.get(i);
            if (repetidas.contains(resultado.fila())) {
                resultados.set(i, error(resultado.fila(), "Identidad o documento repetido; resuelva o excluya las filas en conflicto"));
            } else if (resultado.aceptable()) {
                var estudiante = propuestas.get(resultado.fila());
                incorporaciones.put(estudiante.id(), estudiante);
            }
        }
        return new Previsualizacion(this, lote, autorId, fecha, resultados, incorporaciones);
    }

    /** Confirmación explícita: aplica solo filas aceptables y conserva resultado de las demás. */
    public RegistroMatriculas confirmar(Previsualizacion propuesta, PoliticaAcceso politica, RegistroCambio cambio) {
        Objects.requireNonNull(propuesta, "propuesta");
        Objects.requireNonNull(cambio, "cambio");
        autorizar(politica, cambio.autorId(), cambio.fecha());
        if (!propuesta.autorId.equals(cambio.autorId())) throw new SecurityException("Debe confirmar el AIP que revisó el lote");
        if (cambio.fecha().isBefore(propuesta.fecha)) throw new IllegalArgumentException("Confirmación anterior a la revisión");
        var anterior = confirmaciones.get(propuesta.lote.id());
        if (anterior != null) {
            exigirMismoLote(propuesta.lote, anterior);
            anterior.filas().stream().filter(ResultadoFila::aceptable).forEach(f ->
                    politica.exigirPersonaDisponible(f.estudianteId().orElseThrow(), cambio.fecha()));
            return this;
        }
        if (propuesta.base != this) throw new IllegalArgumentException("La previsualización está obsoleta; vuelva a validar");
        propuesta.incorporaciones.keySet().forEach(id -> politica.exigirPersonaDisponible(id, cambio.fecha()));
        var nuevos = new HashMap<>(estudiantes);
        nuevos.putAll(propuesta.incorporaciones);
        // La historia no guarda candidatos (ni sus documentos/nombres) de las filas descartadas.
        var resumen = propuesta.filas.stream().map(f -> new ResultadoFila(f.fila(), f.estado(), f.estudianteId(),
                List.of(), f.cambios(), f.avisos())).toList();
        var evidencia = new Confirmacion(propuesta.lote.id(), huella(propuesta.lote), propuesta.lote.procedencia(),
                propuesta.lote.hoja(), propuesta.lote.anio(), propuesta.lote.fechaEfectiva(), cambio, resumen);
        var historia = new HashMap<>(confirmaciones);
        historia.put(evidencia.loteId(), evidencia);
        return new RegistroMatriculas(nuevos, historia);
    }

    private ResultadoFila preparar(LoteMatricula lote, FilaMatricula fila, Map<Integer, Estudiante> propuestas) {
        var resolucion = lote.resoluciones().get(fila.numero());
        if (resolucion != null && resolucion.accion() == LoteMatricula.Accion.EXCLUIR)
            return new ResultadoFila(fila.numero(), Estado.EXCLUIDA, Optional.empty(), List.of(), Set.of(), List.of("Excluida expresamente"));
        if (fila.nombreCompleto().isBlank()) return error(fila.numero(), "Nombre del estudiante obligatorio; corrija o excluya la fila");
        if (fila.nacimiento().filter(f -> f.isAfter(lote.fechaEfectiva())).isPresent())
            return error(fila.numero(), "Nacimiento posterior a la fecha efectiva de matrícula");
        var conciliacion = ConciliacionMatricula.evaluar(fila, List.copyOf(estudiantes.values()));
        Optional<Estudiante> existente = Optional.empty();
        if (resolucion != null && resolucion.accion() == LoteMatricula.Accion.VINCULAR) {
            existente = Optional.ofNullable(estudiantes.get(resolucion.estudianteId().orElseThrow()));
            if (existente.isEmpty()) return error(fila.numero(), "La identidad elegida no existe en el padrón");
        } else if (resolucion == null && conciliacion.estado() == ConciliacionMatricula.Estado.COINCIDENCIA_SEGURA) {
            existente = Optional.of(estudiantes.get(conciliacion.candidatos().getFirst().id()));
        } else if (resolucion == null && conciliacion.estado() == ConciliacionMatricula.Estado.REQUIERE_CONCILIACION) {
            return new ResultadoFila(fila.numero(), Estado.REQUIERE_CONCILIACION, Optional.empty(),
                    conciliacion.candidatos(), Set.of(), List.of("Seleccione identidad, confirme alta distinta o excluya; no se unirá por nombre"));
        }
        if (existente.isEmpty() && estudiantes.containsKey(fila.identidadParaAlta()))
            return error(fila.numero(), "El identificador suministrado para alta ya existe");
        try {
            var avisos = new ArrayList<String>();
            var anterior = existente.orElseGet(() -> Estudiante.crear(fila.identidadParaAlta(), fila.nombreCompleto()));
            var rectificacion = lote.rectificaciones().get(fila.numero());
            var base = rectificacion == null ? anterior : anterior.rectificar(lote.anio(), lote.fechaEfectiva(), fila, rectificacion);
            var actualizado = base.incorporarIdentidad(Optional.of(fila.nombreCompleto()), fila.documento(), fila.nacimiento(), fila.sexo());
            var grupo = fila.grupo();
            if (grupo.isEmpty() && lote.grupoComun().isPresent()
                    && (rectificacion == null || !rectificacion.borrar().contains(RectificacionMatricula.Campo.GRUPO))) {
                if (anterior.grupoEn(lote.anio(), lote.fechaEfectiva()).isEmpty()) grupo = lote.grupoComun();
                else avisos.add("El grupo común no sustituye el grupo conocido; se conserva este último");
            }
            if (anterior.matricula(lote.anio()).isEmpty()) {
                actualizado = actualizado.vincularMatricula(MatriculaAnual.crear(lote.anio(), lote.fechaEfectiva(), grupo, fila.situacion()));
            } else {
                actualizado = actualizado.incorporarGrupo(lote.anio(), lote.fechaEfectiva(), grupo)
                        .incorporarSituacion(lote.anio(), fila.situacion());
            }
            if (actualizado.grupoEn(lote.anio(), lote.fechaEfectiva()).isEmpty()) avisos.add("Grupo no informado; no impide incorporar identidad y año");
            var cambios = cambios(anterior, actualizado, lote.anio());
            var estado = existente.isEmpty() ? Estado.ALTA : cambios.isEmpty() ? Estado.SIN_CAMBIOS : Estado.ACTUALIZACION;
            propuestas.put(fila.numero(), actualizado);
            return new ResultadoFila(fila.numero(), estado, Optional.of(actualizado.id()), List.of(), cambios, avisos);
        } catch (IllegalArgumentException error) {
            return error(fila.numero(), error.getMessage());
        }
    }

    private static Set<Campo> cambios(Estudiante anterior, Estudiante nuevo, int anio) {
        var cambios = new HashSet<Campo>();
        if (!anterior.nombreCompleto().equals(nuevo.nombreCompleto())) cambios.add(Campo.NOMBRE);
        if (!anterior.documento().equals(nuevo.documento())) cambios.add(Campo.DOCUMENTO);
        if (!anterior.fechaNacimiento().equals(nuevo.fechaNacimiento())) cambios.add(Campo.NACIMIENTO);
        if (!anterior.sexoInformado().equals(nuevo.sexoInformado())) cambios.add(Campo.SEXO);
        var matricula = anterior.matricula(anio);
        if (matricula.isEmpty()) cambios.add(Campo.MATRICULA);
        else {
            var actual = nuevo.matricula(anio).orElseThrow();
            if (!matricula.get().historialGrupos().equals(actual.historialGrupos())) cambios.add(Campo.GRUPO);
            if (!matricula.get().situacionInformada().equals(actual.situacionInformada())) cambios.add(Campo.SITUACION);
        }
        return Set.copyOf(cambios);
    }

    private static ResultadoFila error(int fila, String mensaje) {
        return new ResultadoFila(fila, Estado.ERROR, Optional.empty(), List.of(), Set.of(), List.of(mensaje));
    }

    private static boolean excluida(LoteMatricula lote, FilaMatricula fila) {
        var resolucion = lote.resoluciones().get(fila.numero());
        return resolucion != null && resolucion.accion() == LoteMatricula.Accion.EXCLUIR;
    }

    private boolean identidadNoDisponible(LoteMatricula lote, FilaMatricula fila, PoliticaAcceso politica, Instant fecha) {
        var resolucion = lote.resoluciones().get(fila.numero());
        if (!politica.puedeConsultarPersona(fila.identidadParaAlta(), fecha)
                || resolucion != null && resolucion.estudianteId().filter(id -> !politica.puedeConsultarPersona(id, fecha)).isPresent())
            return true;
        var porDocumento = estudiantes.values().stream().filter(e -> fila.documento().isPresent()
                && fila.documento().equals(e.documento())).toList();
        if (!porDocumento.isEmpty()) return porDocumento.stream().anyMatch(e -> !politica.puedeConsultarPersona(e.id(), fecha));
        // Un documento diferente distingue al homónimo. Sin documento en cualquiera de las partes,
        // no se puede eludir la resolución institucional creando otra identidad para el dato bloqueado.
        return estudiantes.values().stream().filter(e -> (fila.documento().isEmpty() || e.documento().isEmpty())
                        && BusquedaEstudiantes.normalizar(fila.nombreCompleto()).equals(BusquedaEstudiantes.normalizar(e.nombreCompleto())))
                .anyMatch(e -> !politica.puedeConsultarPersona(e.id(), fecha));
    }

    private static ResultadoFila reservarCandidatos(ResultadoFila resultado, PoliticaAcceso politica, Instant fecha) {
        var visibles = resultado.candidatos().stream().filter(c -> politica.puedeConsultarPersona(c.id(), fecha)).toList();
        if (visibles.size() == resultado.candidatos().size()) return resultado;
        return new ResultadoFila(resultado.fila(), resultado.estado(), resultado.estudianteId(), visibles,
                resultado.cambios(), List.of("Existen coincidencias reservadas; confirme una identidad distinta con información institucional o excluya"));
    }

    private static boolean documentoRepetido(LoteMatricula lote, FilaMatricula fila) {
        return fila.documento().isPresent() && lote.filas().stream().anyMatch(otra -> otra.numero() != fila.numero()
                && !excluida(lote, otra) && otra.documento().equals(fila.documento()));
    }

    private static boolean nombreAmbiguoEnLote(LoteMatricula lote, FilaMatricula fila) {
        return !fila.nombreCompleto().isBlank() && lote.filas().stream().anyMatch(otra -> otra.numero() != fila.numero()
                && !excluida(lote, otra) && (fila.documento().isEmpty() || otra.documento().isEmpty())
                && BusquedaEstudiantes.normalizar(fila.nombreCompleto()).equals(BusquedaEstudiantes.normalizar(otra.nombreCompleto())));
    }

    private void autorizar(PoliticaAcceso politica, UUID autorId, Instant fecha) {
        Objects.requireNonNull(politica, "politica");
        Objects.requireNonNull(autorId, "autorId");
        Objects.requireNonNull(fecha, "fecha");
        if (confirmaciones.values().stream().anyMatch(c -> c.cambio().fecha().isAfter(fecha)))
            throw new IllegalArgumentException("La operación precede al estado vigente del registro");
        if (!politica.evaluar(autorId, Permiso.IMPORTAR_NOMINA, fecha).permitida())
            throw new SecurityException("Se requiere AIP vigente para revisar y confirmar matrícula");
    }

    private static void exigirMismoLote(LoteMatricula lote, Confirmacion confirmacion) {
        if (!huella(lote).equals(confirmacion.huella()))
            throw new IllegalArgumentException("ID de lote ya confirmado con otro contenido; prepare un nuevo lote");
    }

    /** Huella de campos tipados, no del archivo. Prefijos de longitud y presencia evitan concatenaciones ambiguas. */
    static String huella(LoteMatricula lote) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            agregar(digest, List.of(lote.id().toString(), lote.procedencia(), lote.hoja(), Integer.toString(lote.anio()),
                    lote.fechaEfectiva().toString()));
            agregarGrupo(digest, lote.grupoComun());
            agregar(digest, List.of(Integer.toString(lote.filas().size())));
            for (var fila : lote.filas()) {
                agregar(digest, List.of(Integer.toString(fila.numero()), fila.identidadParaAlta().toString(), fila.nombreCompleto()));
                agregarOpcional(digest, fila.documento().map(DocumentoIdentidad::tipo));
                agregarOpcional(digest, fila.documento().map(DocumentoIdentidad::numero));
                agregarOpcional(digest, fila.nacimiento().map(LocalDate::toString));
                agregarOpcional(digest, fila.sexo());
                agregarGrupo(digest, fila.grupo());
                agregarOpcional(digest, fila.situacion());
                var resolucion = Optional.ofNullable(lote.resoluciones().get(fila.numero()));
                agregarOpcional(digest, resolucion.map(r -> r.accion().name()));
                agregarOpcional(digest, resolucion.flatMap(LoteMatricula.Resolucion::estudianteId).map(UUID::toString));
                var rectificacion = Optional.ofNullable(lote.rectificaciones().get(fila.numero()));
                agregarOpcional(digest, rectificacion.map(r -> r.borrar().stream().map(Enum::name).sorted()
                        .collect(java.util.stream.Collectors.joining(",")) + ":" + r.sustituirGrupo()));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no disponible", error);
        }
    }

    private static void agregarGrupo(MessageDigest digest, Optional<GrupoEscolar> grupo) {
        agregarOpcional(digest, grupo.flatMap(GrupoEscolar::grado));
        agregarOpcional(digest, grupo.flatMap(GrupoEscolar::seccion));
    }
    private static void agregarOpcional(MessageDigest digest, Optional<String> valor) {
        digest.update((byte) (valor.isPresent() ? 1 : 0));
        valor.ifPresent(texto -> agregar(digest, List.of(texto)));
    }
    private static void agregar(MessageDigest digest, List<String> valores) {
        for (var valor : valores) {
            var bytes = valor.getBytes(StandardCharsets.UTF_8);
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
            digest.update(bytes);
        }
    }
}
