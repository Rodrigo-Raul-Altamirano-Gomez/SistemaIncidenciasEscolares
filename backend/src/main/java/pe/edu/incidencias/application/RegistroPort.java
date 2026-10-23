package pe.edu.incidencias.application;

import java.time.*;
import java.util.*;
import pe.edu.incidencias.domain.Nota.Entrada;

public interface RegistroPort {
    record Actor(UUID id, String nombre, Set<String> roles) {
        public boolean registra() { return roles.stream().anyMatch(Set.of("DOCENTE","AUXILIAR","DIRECTIVO","TOE","COMITE","DIRECTOR")::contains); }
    }
    record Filtros(UUID estudianteId, LocalDate desde, LocalDate hasta, String categoriaId, Integer anio, int pagina) {}
    record Estudiante(UUID id, String nombre, String codigo, Integer anio, String grado, String seccion) {}
    record Categoria(String id, String nombre, String naturaleza, String gravedad) {}
    record Participacion(UUID estudianteId, String nombre, String codigo, String grado, String seccion, String papel) {}
    record Cuerpo(Entrada entrada, String categoriaNombre, String naturaleza, String gravedad, List<Participacion> participantes) {}
    record Cambio(String campo, Object antes, Object despues) {}
    record Revision(int version, String autor, Instant fecha, List<Cambio> cambios) {}
    record Vista(UUID id, UUID autorId, String autor, int version, Instant registrada, Instant actualizada, Cuerpo cuerpo, List<Revision> rectificaciones) {}
    record Pagina(List<Vista> registros, boolean hayMas) {}
    List<Estudiante> buscar(Actor actor, String texto, Integer anio, String grado, String seccion);
    List<Categoria> categorias();
    List<Integer> anios();
    Vista guardar(Actor actor, UUID requestId, UUID id, Integer version, Entrada entrada);
    Vista leer(Actor actor, UUID id);
    Pagina listar(Actor actor, Filtros filtros);
}
