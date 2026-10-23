package pe.edu.incidencias.adapter;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.incidencias.application.RegistroPort.Actor;
import pe.edu.incidencias.application.RegistroService.Rechazo;

@Service
public class Acceso {
    public record Sesion(Actor actor, String csrf, Instant expira, String token) {}
    private final JdbcTemplate db; private final Crypto crypto; private final Auditoria audit; private final Clock clock;
    private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(12);
    private final Semaphore verificaciones=new Semaphore(4);
    private final String dummy=passwords.encode(UUID.randomUUID().toString());
    public Acceso(JdbcTemplate db,Crypto crypto,Auditoria audit,Clock clock) { this.db=db; this.crypto=crypto; this.audit=audit; this.clock=clock; }
    // Los fallos deben confirmar su contador/evidencia: se retorna la denegación en vez de lanzar dentro de la transacción.
    public record LoginResultado(Sesion sesion, int status, int reintentoSegundos) {
        public LoginResultado(Sesion sesion,int status) { this(sesion,status,status==429?900:0); }
    }
    @Transactional public LoginResultado login(String login,String password,String ip) {
        if(!verificaciones.tryAcquire()) { audit.registrar(null,"LOGIN","SATURADO",null,Map.of()); return new LoginResultado(null,429,1); }
        try { return comprobarLogin(login,password,ip); }
        finally { verificaciones.release(); }
    }
    private LoginResultado comprobarLogin(String login,String password,String ip) {
        String normalized=login==null?"":login.strip().toLowerCase(Locale.ROOT);
        if(normalized.length()>80 || password==null || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) {
            audit.registrar(null,"LOGIN","DENEGADO",null,Map.of()); return new LoginResultado(null,401);
        }
        String accountKey=crypto.fingerprint("login:"+normalized),ipKey=crypto.fingerprint("ip:"+ip);
        db.update("DELETE FROM login_throttle WHERE key IN (SELECT key FROM login_throttle WHERE started_at<=? LIMIT 500 FOR UPDATE SKIP LOCKED)",Timestamp.from(clock.instant().minusSeconds(900)));
        // Una IP compartida no serializa todas las contraseñas ni bloquea cuentas válidas.
        // La cuenta sigue limitada entre IP distintas; el semáforo acota el trabajo BCrypt.
        db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,1))",accountKey);
        if(limited(accountKey,5)) { audit.registrar(null,"LOGIN","LIMITADO",null,Map.of("cuentaHmac",accountKey)); return new LoginResultado(null,429); }
        boolean ipLimited=limited(ipKey,50);
        var rows=db.queryForList("SELECT * FROM app_user WHERE login=?",normalized);
        var user=rows.isEmpty()?null:rows.getFirst();
        boolean matches=passwords.matches(password,user==null?dummy:(String)user.get("password_hash"));
        if(user==null || !matches || !(Boolean)user.get("active")) {
            failed(accountKey); failed(ipKey); audit.registrar(null,"LOGIN",ipLimited?"LIMITADO":"DENEGADO",null,Map.of("cuentaHmac",accountKey)); return new LoginResultado(null,ipLimited?429:401);
        }
        UUID id=(UUID)user.get("id"); Instant now=clock.instant();
        String token=crypto.token(),csrf=crypto.token();
        db.update("DELETE FROM login_throttle WHERE key=?",accountKey);
        db.update("DELETE FROM auth_session WHERE token_hash IN (SELECT token_hash FROM auth_session WHERE last_activity<=? LIMIT 500 FOR UPDATE SKIP LOCKED)",Timestamp.from(now.minusSeconds(1200)));
        db.update("INSERT INTO auth_session VALUES (?,?,?,?,?,?)",Crypto.hash(token),id,csrf,user.get("access_version"),Timestamp.from(now),Timestamp.from(now));
        audit.registrar(id,"LOGIN","OK",null,Map.of());
        return new LoginResultado(new Sesion(actor(id,(String)user.get("name")),csrf,now.plusSeconds(1200),token),200);
    }
    private boolean limited(String key,int limit) { return Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM login_throttle WHERE key=? AND attempts>=? AND started_at>?)",Boolean.class,key,limit,Timestamp.from(clock.instant().minusSeconds(900)))); }
    private void failed(String key) { db.update("""
        INSERT INTO login_throttle VALUES (?,1,?) ON CONFLICT(key) DO UPDATE SET
        attempts=CASE WHEN login_throttle.started_at<=? THEN 1 ELSE login_throttle.attempts+1 END,
        started_at=CASE WHEN login_throttle.started_at<=? THEN EXCLUDED.started_at ELSE login_throttle.started_at END
        """,key,Timestamp.from(clock.instant()),Timestamp.from(clock.instant().minusSeconds(900)),Timestamp.from(clock.instant().minusSeconds(900))); }
    @Transactional public Sesion autenticar(String token,boolean actividad) {
        if(token==null || token.length()>128) return null;
        var rows=db.queryForList("SELECT s.*,u.name,u.active,u.access_version AS current_version FROM auth_session s JOIN app_user u ON u.id=s.user_id WHERE token_hash=? FOR UPDATE OF s",Crypto.hash(token));
        if(rows.isEmpty()) return null;
        var r=rows.getFirst(); UUID id=(UUID)r.get("user_id"); Instant last=((Timestamp)r.get("last_activity")).toInstant(), now=clock.instant();
        if(!(Boolean)r.get("active") || !r.get("access_version").equals(r.get("current_version")) || !last.plusSeconds(1200).isAfter(now)) {
            db.update("DELETE FROM auth_session WHERE token_hash=?",Crypto.hash(token)); audit.registrar(id,"SESION","EXPIRADA_O_REVOCADA",null,Map.of()); return null;
        }
        if(actividad) { db.update("UPDATE auth_session SET last_activity=? WHERE token_hash=?",Timestamp.from(now),Crypto.hash(token)); last=now; }
        return new Sesion(actor(id,(String)r.get("name")),(String)r.get("csrf_token"),last.plusSeconds(1200),null);
    }
    private Actor actor(UUID id,String name) {
        var roles=db.queryForList("SELECT role FROM user_role WHERE user_id=? AND valid_from<=? AND (valid_to IS NULL OR valid_to>=?)",String.class,id,LocalDate.now(clock),LocalDate.now(clock));
        return new Actor(id,name,Set.copyOf(roles));
    }
    @Transactional public void logout(String token,UUID actor) { db.update("DELETE FROM auth_session WHERE token_hash=?",Crypto.hash(token)); audit.registrar(actor,"LOGOUT","OK",null,Map.of()); }
    @Transactional public void denegado(UUID actor,int status) { audit.registrar(actor,"OPERACION","DENEGADO",null,Map.of("status",status)); }
    public String hashPassword(String password) { if(password==null || password.length()<12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new Rechazo(422,"La contraseña debe tener al menos 12 caracteres y un máximo de 72 bytes UTF-8."); return passwords.encode(password); }
}
