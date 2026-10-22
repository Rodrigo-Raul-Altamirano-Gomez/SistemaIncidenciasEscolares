package pe.edu.incidencias.domain.institucion;

import pe.edu.incidencias.domain.acceso.Permiso;
import pe.edu.incidencias.domain.acceso.PoliticaAcceso;
import pe.edu.incidencias.domain.catalogo.Categoria;
import pe.edu.incidencias.domain.catalogo.ParametrosReiteracion;
import pe.edu.incidencias.domain.estudiante.AlcanceImportacion;
import pe.edu.incidencias.domain.estudiante.BusquedaEstudiantes;
import pe.edu.incidencias.domain.estudiante.GrupoEscolar;
import pe.edu.incidencias.domain.incidencia.RegistroCambio;

import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/** Configuración de una instalación. Definir valores no confiere permisos de administración de años. */
public final class ConfiguracionInstitucional {
    public enum Operacion { DEFINICION, DELEGACION, CARGA_DELEGADA, ANIO, GRUPOS, ANIO_TRABAJO }
    public record Estado(DatosInstitucion institucion, Optional<AvisoPrivacidad> aviso,
                          Optional<AlcanceImportacion> alcanceImportacion, Map<UUID, Categoria> categorias,
                          ParametrosReiteracion reiteracion, Map<Integer, Set<GrupoEscolar>> gruposPorAnio,
                          OptionalInt anioTrabajo) {
        public Estado {
            categorias = Map.copyOf(categorias);
            var copia = new HashMap<Integer, Set<GrupoEscolar>>();
            gruposPorAnio.forEach((anio, grupos) -> copia.put(anio, Set.copyOf(grupos)));
            gruposPorAnio = Map.copyOf(copia);
        }
    }
    /** La delegación congela valores exactos; no es un permiso de decidir otros valores. */
    public record Delegacion(UUID id, UUID operadorId, DefinicionInstitucional valores, RegistroCambio definicion,
                             Optional<RegistroCambio> carga) { }
    public record Revision(Operacion operacion, Estado estado, RegistroCambio cambio, Optional<UUID> delegacionId) { }

    private final Estado estado;
    private final Map<UUID, Delegacion> delegaciones;
    private final List<Revision> revisiones;

    private ConfiguracionInstitucional(Estado estado, Map<UUID, Delegacion> delegaciones, List<Revision> revisiones) {
        this.estado = estado;
        this.delegaciones = Map.copyOf(delegaciones);
        this.revisiones = List.copyOf(revisiones);
    }

    public static ConfiguracionInstitucional crear(DefinicionInstitucional valores, PoliticaAcceso politica,
                                                    RegistroCambio cambio) {
        exigir(politica, Permiso.DEFINIR_CONFIGURACION, cambio);
        var base = new Estado(valores.institucion(), valores.aviso(), valores.alcanceImportacion(), Map.of(),
                ParametrosReiteracion.sinConfigurar(cambio.autorId(), cambio.fecha()), Map.of(), OptionalInt.empty());
        var estado = aplicar(base, valores, cambio);
        return new ConfiguracionInstitucional(estado, Map.of(), List.of(new Revision(Operacion.DEFINICION, estado, cambio, Optional.empty())));
    }

    /** Estado confiable para persistencia; no debe serializarse para usuarios sin la capacidad pertinente. */
    public Estado estado() { return estado; }
    public List<Revision> revisiones() { return revisiones; }
    public Map<UUID, Delegacion> delegaciones() { return delegaciones; }

    public ConfiguracionInstitucional definir(DefinicionInstitucional valores, PoliticaAcceso politica, RegistroCambio cambio) {
        validarFecha(cambio);
        exigir(politica, Permiso.DEFINIR_CONFIGURACION, cambio);
        return guardar(Operacion.DEFINICION, aplicar(estado, valores, cambio), delegaciones, cambio, Optional.empty());
    }

    public ConfiguracionInstitucional delegar(UUID id, UUID operadorId, DefinicionInstitucional valores,
                                               PoliticaAcceso politica, RegistroCambio cambio) {
        validarFecha(cambio);
        exigir(politica, Permiso.DEFINIR_CONFIGURACION, cambio);
        PoliticaAcceso.exigir(politica.evaluar(operadorId, Permiso.ADMINISTRAR_USUARIOS, cambio.fecha()));
        if (delegaciones.containsKey(Objects.requireNonNull(id))) throw new IllegalArgumentException("Delegación ya registrada");
        var nuevas = new HashMap<>(delegaciones);
        nuevas.put(id, new Delegacion(id, operadorId, Objects.requireNonNull(valores), cambio, Optional.empty()));
        return guardar(Operacion.DELEGACION, estado, nuevas, cambio, Optional.of(id));
    }

    public ConfiguracionInstitucional cargarDelegacion(UUID id, PoliticaAcceso politica, RegistroCambio cambio) {
        validarFecha(cambio);
        exigir(politica, Permiso.ADMINISTRAR_USUARIOS, cambio);
        var delegacion = Optional.ofNullable(delegaciones.get(id)).orElseThrow(() -> new IllegalArgumentException("Delegación desconocida"));
        if (!delegacion.operadorId().equals(cambio.autorId())) throw new SecurityException("Carga asignada a otro administrador");
        if (delegacion.carga().isPresent()) return this;
        // Una decisión antigua no puede pisar una definición institucional posterior.
        int origen = -1;
        for (int i = 0; i < revisiones.size(); i++) if (revisiones.get(i).operacion() == Operacion.DELEGACION
                && revisiones.get(i).delegacionId().equals(Optional.of(id))) origen = i;
        if (origen < 0 || revisiones.subList(origen + 1, revisiones.size()).stream().anyMatch(r ->
                r.operacion() == Operacion.DEFINICION || r.operacion() == Operacion.CARGA_DELEGADA))
            throw new IllegalStateException("Delegación obsoleta; los valores deben definirse nuevamente");
        var nuevas = new HashMap<>(delegaciones);
        nuevas.put(id, new Delegacion(id, delegacion.operadorId(), delegacion.valores(), delegacion.definicion(), Optional.of(cambio)));
        return guardar(Operacion.CARGA_DELEGADA, aplicar(estado, delegacion.valores(), cambio), nuevas, cambio, Optional.of(id));
    }

    public ConfiguracionInstitucional registrarAnio(int anio, PoliticaAcceso politica, RegistroCambio cambio) {
        validarFecha(cambio);
        exigir(politica, Permiso.ADMINISTRAR_USUARIOS, cambio);
        validarAnio(anio);
        if (estado.gruposPorAnio().containsKey(anio)) return this;
        var grupos = new HashMap<>(estado.gruposPorAnio());
        grupos.put(anio, Set.of());
        return guardar(Operacion.ANIO, conAnios(grupos, estado.anioTrabajo()), delegaciones, cambio, Optional.empty());
    }

    /** Catálogo de grupos permitidos, sin editar las matrículas ni las instantáneas de hechos. */
    public ConfiguracionInstitucional definirGrupos(int anio, Set<GrupoEscolar> grupos, PoliticaAcceso politica, RegistroCambio cambio) {
        validarFecha(cambio);
        exigir(politica, Permiso.ADMINISTRAR_USUARIOS, cambio);
        exigirAnio(anio);
        grupos = Set.copyOf(grupos);
        var claves = new HashSet<String>();
        for (var grupo : grupos) {
            String clave = BusquedaEstudiantes.normalizar(grupo.grado().orElse("")) + "\u0000"
                    + BusquedaEstudiantes.normalizar(grupo.seccion().orElse(""));
            if (!claves.add(clave)) throw new IllegalArgumentException("Grupo equivalente repetido");
        }
        var nuevos = new HashMap<>(estado.gruposPorAnio());
        nuevos.put(anio, grupos);
        return guardar(Operacion.GRUPOS, conAnios(nuevos, estado.anioTrabajo()), delegaciones, cambio, Optional.empty());
    }

    public ConfiguracionInstitucional seleccionarAnioTrabajo(int anio, PoliticaAcceso politica, RegistroCambio cambio) {
        validarFecha(cambio);
        exigir(politica, Permiso.ADMINISTRAR_USUARIOS, cambio);
        exigirAnio(anio);
        if (estado.anioTrabajo().equals(OptionalInt.of(anio))) return this;
        return guardar(Operacion.ANIO_TRABAJO, conAnios(estado.gruposPorAnio(), OptionalInt.of(anio)), delegaciones, cambio, Optional.empty());
    }

    public boolean admiteGrupo(int anio, Optional<GrupoEscolar> grupo) {
        exigirAnio(anio);
        return grupo.isEmpty() || estado.gruposPorAnio().get(anio).stream().anyMatch(g ->
                g.grado().map(BusquedaEstudiantes::normalizar).equals(grupo.get().grado().map(BusquedaEstudiantes::normalizar))
                && g.seccion().map(BusquedaEstudiantes::normalizar).equals(grupo.get().seccion().map(BusquedaEstudiantes::normalizar)));
    }

    private Estado conAnios(Map<Integer, Set<GrupoEscolar>> grupos, OptionalInt anioTrabajo) {
        return new Estado(estado.institucion(), estado.aviso(), estado.alcanceImportacion(), estado.categorias(), estado.reiteracion(), grupos, anioTrabajo);
    }
    private static Estado aplicar(Estado anterior, DefinicionInstitucional valores, RegistroCambio cambio) {
        Objects.requireNonNull(valores, "valores");
        var categorias = new HashMap<>(anterior.categorias());
        valores.categorias().forEach((id, definicion) -> {
            var categoria = categorias.get(id);
            categoria = categoria == null ? Categoria.crear(id, definicion, cambio.autorId(), cambio.fecha())
                    : categoria.actualizar(definicion, cambio.autorId(), cambio.fecha());
            boolean activa = valores.categoriasActivas().contains(id);
            if (categoria.activa() != activa) categoria = activa ? categoria.activar(cambio.autorId(), cambio.fecha())
                    : categoria.desactivar(cambio.autorId(), cambio.fecha());
            categorias.put(id, categoria);
        });
        var parametros = anterior.reiteracion();
        if (!parametros.ventanaDias().equals(valores.ventanaDias())) parametros = valores.ventanaDias().isPresent()
                ? parametros.configurarVentana(valores.ventanaDias().getAsInt(), cambio.autorId(), cambio.fecha())
                : parametros.retirarVentana(cambio.autorId(), cambio.fecha());
        return new Estado(valores.institucion(), valores.aviso(), valores.alcanceImportacion(), categorias, parametros,
                anterior.gruposPorAnio(), anterior.anioTrabajo());
    }
    private ConfiguracionInstitucional guardar(Operacion operacion, Estado nuevo, Map<UUID, Delegacion> delegadas,
                                                RegistroCambio cambio, Optional<UUID> delegacionId) {
        var historia = new ArrayList<>(revisiones);
        historia.add(new Revision(operacion, nuevo, cambio, delegacionId));
        return new ConfiguracionInstitucional(nuevo, delegadas, historia);
    }
    private static void exigir(PoliticaAcceso politica, Permiso permiso, RegistroCambio cambio) {
        Objects.requireNonNull(cambio, "cambio");
        PoliticaAcceso.exigir(Objects.requireNonNull(politica).evaluar(cambio.autorId(), permiso, cambio.fecha()));
    }
    private void validarFecha(RegistroCambio cambio) {
        if (Objects.requireNonNull(cambio).fecha().isBefore(revisiones.getLast().cambio().fecha()))
            throw new IllegalArgumentException("El cambio precede a la configuración vigente");
    }
    private void exigirAnio(int anio) {
        validarAnio(anio);
        if (!estado.gruposPorAnio().containsKey(anio)) throw new IllegalArgumentException("Año no registrado");
    }
    private static void validarAnio(int anio) {
        Year.of(anio);
        if (anio < 1) throw new IllegalArgumentException("El año debe ser positivo");
    }
}
