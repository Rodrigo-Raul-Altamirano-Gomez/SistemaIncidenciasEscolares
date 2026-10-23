package pe.edu.incidencias.adapter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SeguridadHttp extends OncePerRequestFilter {
    private final Acceso acceso; private final String origin; private final boolean secure;
    public SeguridadHttp(Acceso acceso,@Value("${app.origin}") String origin,@Value("${app.cookie-secure}") boolean secure) { this.acceso=acceso; this.origin=origin; this.secure=secure; }
    public static String token(HttpServletRequest r) { if(r.getCookies()!=null) for(Cookie c:r.getCookies()) if(c.getName().equals("I01_SESSION")) return c.getValue(); return null; }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        res.setHeader("Cache-Control","no-store, max-age=0"); res.setHeader("Pragma","no-cache");
        res.setHeader("X-Content-Type-Options","nosniff"); res.setHeader("X-Frame-Options","DENY");
        res.setHeader("Referrer-Policy","no-referrer"); res.setHeader("Permissions-Policy","camera=(), microphone=(), geolocation=()");
        res.setHeader("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
        if(req.isSecure()) res.setHeader("Strict-Transport-Security","max-age=31536000");
        if(secure && !req.isSecure()) { error(res,400,"Se requiere una conexión HTTPS."); return; }
        // ServletPath es la ruta decodificada del contenedor, también para segmentos escapados.
        String path=req.getServletPath();
        if(!path.startsWith("/api/")) { chain.doFilter(req,res); return; }
        try {
            boolean mutate=!Set.of("GET","HEAD").contains(req.getMethod());
            if(req.getContentLengthLong()>32768) { error(res,413,"La solicitud supera el límite de 32 KB."); return; }
            if(mutate && (!"true".equals(req.getHeader("X-I01-Client")) || req.getHeader("Origin")!=null && !origin.equals(req.getHeader("Origin")))) {
                error(res,403,"Origen de la solicitud no permitido."); return;
            }
            if(path.equals("/api/acceso/login")) { chain.doFilter(new SolicitudLimitada(req),res); return; }
            Acceso.Sesion session=acceso.autenticar(token(req),!path.equals("/api/acceso/sesion"));
            if(session==null) { error(res,401,"Inicia sesión para continuar."); return; }
            req.setAttribute("sesion",session); res.setHeader("X-Session-Expires",session.expira().toString());
            if(mutate && (req.getHeader("X-CSRF-Token")==null || !MessageDigest.isEqual(session.csrf().getBytes(StandardCharsets.UTF_8),req.getHeader("X-CSRF-Token").getBytes(StandardCharsets.UTF_8)))) {
                acceso.denegado(session.actor().id(),403); error(res,403,"La sesión de seguridad cambió. Vuelve a autenticarte."); return;
            }
            chain.doFilter(new SolicitudLimitada(req),res);
        } catch(Exception ex) {
            org.slf4j.LoggerFactory.getLogger(SeguridadHttp.class).warn("I01 solicitud no confirmada; tipo={}",ex.getClass().getSimpleName());
            if(!res.isCommitted()) error(res,503,"No se pudo confirmar la operación. Conserva tu texto y vuelve a intentarlo.");
        }
    }
    /** También limita JSON enviado sin Content-Length (transferencia por bloques). */
    private static class SolicitudLimitada extends HttpServletRequestWrapper {
        SolicitudLimitada(HttpServletRequest req) { super(req); }
        @Override public ServletInputStream getInputStream() throws IOException {
            ServletInputStream original=super.getInputStream();
            return new ServletInputStream() {
                int read;
                @Override public int read() throws IOException { int b=original.read(); if(b!=-1 && ++read>32768) throw new IOException("Límite de solicitud excedido."); return b; }
                @Override public boolean isFinished() { return original.isFinished(); }
                @Override public boolean isReady() { return original.isReady(); }
                @Override public void setReadListener(ReadListener listener) { original.setReadListener(listener); }
            };
        }
    }
    public static void error(HttpServletResponse res,int status,String message) throws IOException {
        res.setStatus(status); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write(Json.write(Map.of("mensaje",message)));
    }
}
