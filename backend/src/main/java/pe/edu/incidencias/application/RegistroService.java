package pe.edu.incidencias.application;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.incidencias.application.RegistroPort.*;
import pe.edu.incidencias.domain.Nota.Entrada;

@Service
@Transactional
public class RegistroService {
    private final RegistroPort port;
    public RegistroService(RegistroPort port) { this.port = port; }
    public void autorizar(Actor actor) { if (!actor.registra()) throw new Rechazo(403, "Tu cuenta no tiene permiso para esta operación."); }
    public List<Estudiante> buscar(Actor a, String q, Integer anio, String grado, String seccion) {
        autorizar(a); if (q.length() > 100) throw new IllegalArgumentException("La búsqueda admite hasta 100 caracteres.");
        return port.buscar(a, q, anio, grado, seccion);
    }
    public Map<String,Object> opciones(Actor a) { autorizar(a); return Map.of("categorias", port.categorias(), "anios",port.anios(),"reiteracion","Inactiva: sin parámetros institucionales aprobados."); }
    public Vista guardar(Actor a, UUID key, UUID id, Integer version, Entrada entrada) { autorizar(a); return port.guardar(a,key,id,version,entrada); }
    public Vista leer(Actor a, UUID id) { autorizar(a); return port.leer(a,id); }
    public Pagina listar(Actor a, Filtros filtros) {
        autorizar(a);
        if (filtros.pagina() < 0 || filtros.desde() != null && filtros.hasta() != null && filtros.desde().isAfter(filtros.hasta()))
            throw new IllegalArgumentException("Revisa el intervalo de fechas o la página solicitada.");
        if (filtros.categoriaId()!=null && !filtros.categoriaId().isBlank() && !filtros.categoriaId().matches("[A-Z_]{1,32}"))
            throw new IllegalArgumentException("Selecciona una categoría válida para filtrar.");
        return port.listar(a,filtros);
    }
    public static class Rechazo extends RuntimeException {
        private final int status;
        public Rechazo(int status,String mensaje) { super(mensaje); this.status = status; }
        public int status() { return status; }
    }
}
