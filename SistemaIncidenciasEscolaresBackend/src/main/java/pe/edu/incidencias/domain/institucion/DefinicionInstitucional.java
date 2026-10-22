package pe.edu.incidencias.domain.institucion;

import pe.edu.incidencias.domain.catalogo.DefinicionCategoria;
import pe.edu.incidencias.domain.estudiante.AlcanceImportacion;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/** Valores expresos aprobados. Categorías omitidas se conservan; desactivar exige incluir su definición. */
public record DefinicionInstitucional(DatosInstitucion institucion, Optional<AvisoPrivacidad> aviso,
                                      Optional<AlcanceImportacion> alcanceImportacion,
                                      Map<UUID, DefinicionCategoria> categorias, Set<UUID> categoriasActivas,
                                      OptionalInt ventanaDias) {
    public DefinicionInstitucional {
        Objects.requireNonNull(institucion, "institucion");
        Objects.requireNonNull(aviso, "aviso");
        Objects.requireNonNull(alcanceImportacion, "alcanceImportacion");
        categorias = Map.copyOf(categorias);
        categoriasActivas = Set.copyOf(categoriasActivas);
        if (!categorias.keySet().containsAll(categoriasActivas))
            throw new IllegalArgumentException("Activación sin definición de categoría");
        Objects.requireNonNull(ventanaDias, "ventanaDias");
        if (ventanaDias.isPresent() && ventanaDias.getAsInt() < 1)
            throw new IllegalArgumentException("La ventana común debe ser positiva");
    }
}
