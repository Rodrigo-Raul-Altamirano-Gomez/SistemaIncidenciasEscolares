package pe.edu.incidencias.adapter;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pe.edu.incidencias.application.RegistroService.Rechazo;

@RestControllerAdvice
public class Errores {
    private final Acceso acceso;
    public Errores(Acceso acceso) { this.acceso=acceso; }
    @ExceptionHandler(Rechazo.class) ResponseEntity<?> rechazo(Rechazo ex,HttpServletRequest req) {
        var session=(Acceso.Sesion)req.getAttribute("sesion"); if(session!=null) acceso.denegado(session.actor().id(),ex.status());
        return response(ex.status(),ex.getMessage());
    }
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> validation(IllegalArgumentException e) { return response(422,e.getMessage()); }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class}) ResponseEntity<?> malformed(Exception e) { return response(400,"Revisa el formato de los datos y las fechas."); }
    @ExceptionHandler(NoResourceFoundException.class) ResponseEntity<?> missing(NoResourceFoundException e) { return response(404,"Recurso no disponible."); }
    @ExceptionHandler(Exception.class) ResponseEntity<?> failure(Exception e) {
        org.slf4j.LoggerFactory.getLogger(Errores.class).warn("I01 operación no confirmada; tipo={}",e.getClass().getSimpleName());
        return response(503,"No se pudo confirmar la operación. Conserva tu texto y reintenta con los mismos datos.");
    }
    private ResponseEntity<?> response(int status,String message) { return ResponseEntity.status(status).body(Map.of("mensaje",message)); }
}
