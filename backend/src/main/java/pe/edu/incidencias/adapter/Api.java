package pe.edu.incidencias.adapter;

import java.time.*;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.incidencias.application.*;
import pe.edu.incidencias.application.RegistroPort.*;
import pe.edu.incidencias.domain.Nota.Entrada;

@RestController
@RequestMapping("/api")
public class Api {
    private final RegistroService registro; private final Acceso acceso; private final boolean secure;
    public Api(RegistroService registro,Acceso acceso,@Value("${app.cookie-secure}") boolean secure) { this.registro=registro; this.acceso=acceso; this.secure=secure; }
    public record Login(String usuario,String clave) {}
    public record Guardado(UUID solicitudId,Integer version,Entrada entrada) {}
    private static Acceso.Sesion session(HttpServletRequest req) { return (Acceso.Sesion)req.getAttribute("sesion"); }
    private ResponseCookie cookie(String token,long age) { return ResponseCookie.from("I01_SESSION",token).httpOnly(true).secure(secure).sameSite("Strict").path("/api").maxAge(age).build(); }
    @PostMapping("/acceso/login") public ResponseEntity<?> login(@RequestBody Login body,HttpServletRequest req) {
        var r=acceso.login(body.usuario(),body.clave(),req.getRemoteAddr());
        if(r.sesion()==null) return ResponseEntity.status(r.status()).headers(h->{if(r.status()==429)h.set("Retry-After",String.valueOf(r.reintentoSegundos()));}).body(Map.of("mensaje",r.status()==429?(r.reintentoSegundos()==1?"Servicio de acceso ocupado. Reintenta en un momento.":"Demasiados intentos. Espera quince minutos y vuelve a intentarlo."):"No se pudo iniciar sesión. Revisa tus credenciales."));
        // Evita mantener inadvertidamente una sesión anterior en el mismo navegador.
        String old=SeguridadHttp.token(req); if(old!=null) { var previous=acceso.autenticar(old,false); if(previous!=null) acceso.logout(old,previous.actor().id()); }
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie(r.sesion().token(),-1).toString()).body(info(r.sesion()));
    }
    @GetMapping("/acceso/sesion") public Object me(HttpServletRequest req) { return info(session(req)); }
    private Object info(Acceso.Sesion s) { return Map.of("usuario",s.actor(),"csrf",s.csrf(),"expira",s.expira()); }
    @PostMapping("/acceso/logout") public ResponseEntity<?> logout(HttpServletRequest req) { acceso.logout(SeguridadHttp.token(req),session(req).actor().id()); return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie("",0).toString()).body(Map.of("mensaje","Sesión cerrada.")); }
    @GetMapping("/opciones") public Object opciones(HttpServletRequest r) { return registro.opciones(session(r).actor()); }
    @PostMapping("/estudiantes/buscar") public Object estudiantes(@RequestBody Busqueda b,HttpServletRequest r) { return registro.buscar(session(r).actor(),b.texto()==null?"":b.texto(),b.anio(),b.grado(),b.seccion()); }
    public record Busqueda(String texto,Integer anio,String grado,String seccion) {}
    // POST evita nombres/criterios personales en URL e historial de red del navegador.
    @PostMapping("/notas/consultar") public Object listar(@RequestBody Filtros f,HttpServletRequest r) { return registro.listar(session(r).actor(),f); }
    @GetMapping("/notas/{id}") public Object leer(@PathVariable UUID id,HttpServletRequest r) { return registro.leer(session(r).actor(),id); }
    @PostMapping("/notas") public Object crear(@RequestBody Guardado b,HttpServletRequest r) { validateKey(b); return registro.guardar(session(r).actor(),b.solicitudId(),null,null,b.entrada()); }
    @PutMapping("/notas/{id}") public Object corregir(@PathVariable UUID id,@RequestBody Guardado b,HttpServletRequest r) { validateKey(b); return registro.guardar(session(r).actor(),b.solicitudId(),id,b.version(),b.entrada()); }
    private void validateKey(Guardado b) { if(b.solicitudId()==null) throw new IllegalArgumentException("Falta la referencia del guardado. Conserva el texto y vuelve a abrir la tarea."); }
}
