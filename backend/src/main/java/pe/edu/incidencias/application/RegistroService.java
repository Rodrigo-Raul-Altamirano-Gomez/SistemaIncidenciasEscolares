package pe.edu.incidencias.application;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.incidencias.application.RegistroPort.*;
import pe.edu.incidencias.domain.Nota.Entrada;

@Service
@Transactional
public class RegistroService {
    public static final int MAX_PAGINA=1000; // 20 020 aportes por consulta; acotar por fechas/estudiante si se supera.
    private final RegistroPort port;
    public RegistroService(RegistroPort port) { this.port = port; }
    public void autorizar(Actor actor) { if (!actor.registra()) throw new Rechazo(403, "Tu cuenta no tiene permiso para esta operación."); }
    public List<Estudiante> buscar(Actor a, String q, Integer anio, String grado, String seccion) {
        autorizar(a); if (q.length() > 100) throw new IllegalArgumentException("La búsqueda admite hasta 100 caracteres.");
        if(anio!=null && (anio<2000 || anio>2100) || grado!=null && grado.length()>32 || seccion!=null && seccion.length()>32)
            throw new IllegalArgumentException("Revisa año, grado y sección de la búsqueda.");
        return port.buscar(a, q, anio, grado, seccion);
    }
    public Map<String,Object> opciones(Actor a) { autorizar(a); return Map.of("categorias", port.categorias(), "anios",port.anios(),"reiteracion","Inactiva: sin parámetros institucionales aprobados."); }
    public Vista guardar(Actor a, UUID key, UUID id, Integer version, Entrada entrada) { autorizar(a); return port.guardar(a,key,id,version,entrada); }
    public Vista leer(Actor a, UUID id) { autorizar(a); return port.leer(a,id); }
    public Pagina listar(Actor a, Filtros filtros) {
        autorizar(a);
        if(filtros.pagina()<0 || filtros.pagina()>MAX_PAGINA) throw new IllegalArgumentException("La página debe estar entre 0 y 1000. Acota por fechas o estudiante para continuar.");
        if(filtros.desde()!=null && filtros.hasta()!=null && filtros.desde().isAfter(filtros.hasta())) throw new IllegalArgumentException("Revisa el intervalo de fechas.");
        if(filtros.anio()!=null && (filtros.anio()<2000 || filtros.anio()>2100)) throw new IllegalArgumentException("El año debe estar entre 2000 y 2100.");
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
