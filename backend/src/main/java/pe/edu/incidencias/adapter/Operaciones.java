package pe.edu.incidencias.adapter;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.incidencias.adapter.JdbcRegistro.Identidad;
import pe.edu.incidencias.application.RegistroPort.*;

/** Herramientas locales bajo credenciales de propietario; no son endpoints ni roles escolares. */
@Service
public class Operaciones {
    private final JdbcTemplate db; private final Crypto crypto; private final Acceso acceso; private final Auditoria audit; private final Clock clock;
    public Operaciones(JdbcTemplate db,Crypto crypto,Acceso acceso,Auditoria audit,Clock clock) { this.db=db; this.crypto=crypto; this.acceso=acceso; this.audit=audit; this.clock=clock; }
    public static UUID id(String value) { return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)); }
    @Transactional public void seed(String password) {
        String hash=acceso.hashPassword(password);
        String[] roles={"DOCENTE","AUXILIAR","DIRECTIVO","DOCENTE","ADMIN","AIP"};
        String[] users={"docente","auxiliar","directivo","ajeno","admin","aip"};
        for(int i=0;i<users.length;i++) {
            UUID uid=id("user:"+users[i]);
            db.update("INSERT INTO app_user(id,login,name,password_hash) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",uid,users[i],"Operador ficticio "+users[i],hash);
            db.update("INSERT INTO user_role VALUES (?,?,?,null) ON CONFLICT DO NOTHING",uid,roles[i],LocalDate.of(2020,1,1));
        }
        for(int year:List.of(2024,2025,2026)) db.update("INSERT INTO school_year VALUES (?) ON CONFLICT DO NOTHING",year);
        db.update("INSERT INTO category VALUES ('COTIDIANA','Conducta cotidiana de prueba','DISCIPLINARIA','LEVE',true),('POR_CLASIFICAR','Por clasificar','OTRA','POR_CLASIFICAR',true) ON CONFLICT DO NOTHING");
        String[] names={"Samira Torres Luna","Samira Tórres Luna","Álex Muñoz del Río","Estudiante Histórico Ficticio"};
        for(int i=0;i<names.length;i++) {
            UUID sid=id("student:"+i); String document=i==0?"00000001":null;
            db.update("INSERT INTO student(id,school_code,identity_enc) VALUES (?,?,?) ON CONFLICT DO NOTHING",sid,"FIC-00"+(i+1),crypto.seal(Json.write(new Identidad(names[i],document==null?null:"DNI",document)),"student:"+sid));
            int year=i==3?2024:2026;
            db.update("INSERT INTO enrollment VALUES (?,?,?,?,?,null) ON CONFLICT DO NOTHING",sid,year,i==2?null:i==1?"2":"1",i==2?null:i==1?"B":"A",LocalDate.of(year,1,1));
        }
        audit.registrar(null,"PRECARGA_FICTICIA","OK",null,Map.of("identidades",4));
    }
    @Transactional public void recuperar(String login,String password,String authorization) {
        var id=db.queryForObject("SELECT id FROM app_user WHERE login=?",UUID.class,login);
        db.update("UPDATE app_user SET password_hash=?,access_version=access_version+1 WHERE id=?",acceso.hashPassword(password),id);
        db.update("DELETE FROM auth_session WHERE user_id=?",id);
        audit.registrar(null,"RECUPERAR_ACCESO","OK",id,Map.of("constancia",authorization));
    }
    @Transactional public void revocar(String login,String authorization) {
        UUID uid=db.queryForObject("SELECT id FROM app_user WHERE login=?",UUID.class,login);
        db.update("UPDATE app_user SET active=false,access_version=access_version+1 WHERE id=?",uid);
        db.update("DELETE FROM auth_session WHERE user_id=?",uid);
        audit.registrar(null,"REVOCAR_ACCESO","OK",uid,Map.of("constancia",authorization));
    }
    @Transactional public String evidencia(String authorization) {
        audit.registrar(null,"EXTRAER_EVIDENCIA","OK",null,Map.of("constancia",authorization));
        return Json.write(db.queryForList("SELECT id,actor_id,at,operation,result,resource,metadata::text FROM audit_event ORDER BY id"));
    }
    @Transactional public String localizar(UUID student,String authorization) {
        var rows=db.queryForList("SELECT id,school_code,identity_enc FROM student WHERE id=?",student);
        if(rows.isEmpty()) throw new IllegalArgumentException("Identidad no disponible.");
        var refs=db.queryForList("SELECT note_id FROM note_subject WHERE student_id=?",UUID.class,student);
        audit.registrar(null,"LOCALIZAR_DATOS","OK",student,Map.of("constancia",authorization));
        // Referencias para revisión delimitada: no entrega relatos ni identidades de terceros.
        return Json.write(Map.of("id",student,"codigo",rows.getFirst().get("school_code"),"identidad",Json.read(crypto.open((String)rows.getFirst().get("identity_enc"),"student:"+student),Identidad.class),"notasRelacionadas",refs));
    }
    @Transactional public void bloquear(UUID note,boolean blocked,String authorization) {
        if(db.update("UPDATE note SET blocked=? WHERE id=?",blocked,note)!=1) throw new IllegalArgumentException("Nota no disponible.");
        audit.registrar(null,blocked?"BLOQUEAR_NOTA":"DESBLOQUEAR_NOTA","OK",note,Map.of("constancia",authorization));
    }
    public record Sustitucion(UUID notaId,int versionEsperada,Cuerpo cuerpo,Map<Integer,List<Cambio>> rectificaciones) {}
    public record Disposicion(UUID estudianteId,List<Sustitucion> sustituciones) {}
    public record RectificacionIdentidad(UUID estudianteId,Identidad identidad,List<Sustitucion> sustituciones) {}
    @Transactional public void rectificarIdentidad(RectificacionIdentidad manifest,String authorization) {
        db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,3))","rectificacion-identidad");
        Identidad identity=manifest.identidad();
        if(identity==null) throw new IllegalArgumentException("Indica la identidad corregida.");
        pe.edu.incidencias.domain.Nota.texto(identity.nombre(),160,true,"nombre");
        pe.edu.incidencias.domain.Nota.texto(identity.documentoTipo(),24,false,"tipo de documento");
        pe.edu.incidencias.domain.Nota.texto(identity.documento(),32,false,"documento");
        if((identity.documento()!=null && !identity.documento().isBlank()) != (identity.documentoTipo()!=null && !identity.documentoTipo().isBlank())) throw new IllegalArgumentException("Documento y tipo deben informarse juntos o quedar ausentes.");
        if("DNI".equals(identity.documentoTipo()) && !identity.documento().matches("[0-9]{8}")) throw new IllegalArgumentException("El DNI debe conservar ocho cifras textuales, sin rellenarlas.");
        for(var row:db.queryForList("SELECT id,identity_enc FROM student WHERE id<>?",manifest.estudianteId())) {
            Identidad other=Json.read(crypto.open((String)row.get("identity_enc"),"student:"+row.get("id")),Identidad.class);
            if(identity.documento()!=null && !identity.documento().isBlank() && identity.documento().equals(other.documento()) && Objects.equals(identity.documentoTipo(),other.documentoTipo())) throw new IllegalArgumentException("El documento ya corresponde a otra identidad. No se unen personas por esta operación.");
        }
        transformar(manifest.estudianteId(),manifest.sustituciones(),identity,authorization);
    }
    /** La constancia incluye la redacción aprobada de cada copia operativa compartida. Se exige cobertura exacta. */
    @Transactional public void disponer(Disposicion manifest,String authorization) {
        transformar(manifest.estudianteId(),manifest.sustituciones(),null,authorization);
    }
    private void transformar(UUID sid,List<Sustitucion> substitutions,Identidad corrected,String authorization) {
        db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,2))",sid.toString());
        if(db.queryForList("SELECT id FROM student WHERE id=? FOR UPDATE",sid).isEmpty()) throw new IllegalArgumentException("Identidad no disponible para disposición.");
        Set<UUID> actual=new HashSet<>(db.queryForList("SELECT note_id FROM note_subject WHERE student_id=?",UUID.class,sid));
        Set<UUID> supplied=new HashSet<>();
        for(var s:substitutions) if(!supplied.add(s.notaId())) throw new IllegalArgumentException("Nota repetida en el manifiesto.");
        if(!actual.equals(supplied)) throw new IllegalArgumentException("El manifiesto debe cubrir exactamente todas las notas relacionadas.");
        // Igual que guardar: identidad antes que notas; varias notas siempre en orden de UUID.
        for(var s:substitutions.stream().sorted(Comparator.comparing(Sustitucion::notaId)).toList()) {
            int version=db.queryForObject("SELECT version FROM note WHERE id=? FOR UPDATE",Integer.class,s.notaId());
            if(version!=s.versionEsperada()) throw new IllegalArgumentException("Una nota cambió después de preparar la disposición.");
            Cuerpo previous=Json.read(crypto.open(db.queryForObject("SELECT body_enc FROM note WHERE id=?",String.class,s.notaId()),"note:"+s.notaId()),Cuerpo.class);
            var before=previous.entrada(); var after=s.cuerpo().entrada();
            if(before.anio()!=after.anio() || !Objects.equals(before.fechaHecho(),after.fechaHecho()) || before.aproximada()!=after.aproximada() || !Objects.equals(before.hora(),after.hora()) || !Objects.equals(before.categoriaId(),after.categoriaId()) || !Objects.equals(previous.categoriaNombre(),s.cuerpo().categoriaNombre()) || !Objects.equals(previous.naturaleza(),s.cuerpo().naturaleza()) || !Objects.equals(previous.gravedad(),s.cuerpo().gravedad()))
                throw new IllegalArgumentException("Una disposición de identidad no puede cambiar fechas o clasificación del hecho compartido.");
            List<Participacion> expected=previous.participantes().stream().filter(p->corrected!=null || !p.estudianteId().equals(sid)).map(p->p.estudianteId().equals(sid)?new Participacion(p.estudianteId(),corrected.nombre(),p.codigo(),p.grado(),p.seccion(),p.papel()):p).toList();
            if(!expected.equals(s.cuerpo().participantes()) || !before.participantes().stream().filter(p->corrected!=null || !p.estudianteId().equals(sid)).toList().equals(after.participantes()))
                throw new IllegalArgumentException("Deben conservarse íntegros los papeles e identidades de terceros.");
            Set<UUID> remaining=new HashSet<>(db.queryForList("SELECT student_id FROM note_participant WHERE note_id=? AND student_id<>?",UUID.class,s.notaId(),sid));
            if(corrected!=null && before.participantes().stream().anyMatch(p->p.estudianteId().equals(sid))) remaining.add(sid);
            if(!remaining.equals(new HashSet<>(s.cuerpo().participantes().stream().map(Participacion::estudianteId).toList())) || !remaining.equals(new HashSet<>(s.cuerpo().entrada().participantes().stream().map(p->p.estudianteId()).toList()))) throw new IllegalArgumentException("La disposición debe conservar exactamente las participaciones de terceros.");
            boolean disciplinaria=previous.naturaleza().equals("DISCIPLINARIA") && previous.gravedad().equals("LEVE");
            // No se borra el hecho ni se inventa otra persona: el remanente aprobado puede ser anónimo.
            // La excepción al mínimo solo existe aquí y al corregir un remanente ya existente.
            if(remaining.isEmpty()) after.validarConParticipacionesRetiradas(clock,disciplinaria);
            else after.validar(clock,disciplinaria);
            Set<Integer> revisions=new HashSet<>(db.queryForList("SELECT version FROM note_revision WHERE note_id=?",Integer.class,s.notaId()));
            if(!revisions.equals(s.rectificaciones().keySet())) throw new IllegalArgumentException("Falta la sustitución aprobada de una rectificación.");
            db.update("UPDATE note SET body_enc=?,version=version+1,updated_at=now() WHERE id=?",crypto.seal(Json.write(s.cuerpo()),"note:"+s.notaId()),s.notaId());
            if(corrected==null) {
                db.update("DELETE FROM note_participant WHERE note_id=? AND student_id=?",s.notaId(),sid);
                db.update("DELETE FROM note_subject WHERE note_id=? AND student_id=?",s.notaId(),sid);
            }
            for(var rev:s.rectificaciones().entrySet()) db.update("UPDATE note_revision SET changes_enc=? WHERE note_id=? AND version=?",crypto.seal(Json.write(rev.getValue()),"revision:"+s.notaId()+":"+rev.getKey()),s.notaId(),rev.getKey());
            // Rectificar conserva el recibo: un reintento devuelve la copia corregida y no crea otro hecho.
            // La disposición puede retirarlo porque la identidad suprimida ya no admite un nuevo registro.
            if(corrected==null) db.update("DELETE FROM request_receipt WHERE note_id=?",s.notaId());
        }
        if(corrected==null) { db.update("DELETE FROM enrollment WHERE student_id=?",sid); db.update("DELETE FROM student WHERE id=?",sid); }
        else db.update("UPDATE student SET identity_enc=? WHERE id=?",crypto.seal(Json.write(corrected),"student:"+sid),sid);
        audit.registrar(null,corrected==null?"DISPONER_DATOS":"RECTIFICAR_IDENTIDAD","OK",sid,Map.of("constancia",authorization,"notas",actual.size()));
    }
}

@Component
class ComandoOperativo implements ApplicationRunner {
    private final Operaciones ops; private final ConfigurableApplicationContext context;
    @Value("${i01.command:}") private String command;
    @Value("${i01.authorization:}") private String authorization;
    @Value("${i01.login:}") private String login;
    @Value("${i01.resource:00000000-0000-0000-0000-000000000000}") private UUID resource;
    @Value("${i01.file:}") private String file;
    ComandoOperativo(Operaciones ops,ConfigurableApplicationContext context) { this.ops=ops; this.context=context; }
    @Override public void run(ApplicationArguments args) throws Exception {
        if(command.isBlank()) return;
        if(authorization.isBlank() || authorization.length()>120) throw new IllegalArgumentException("Indica la referencia externa de autorización, sin datos personales (máximo 120 caracteres).");
        switch(command) {
            case "seed" -> ops.seed(System.getenv("I01_PASSWORD"));
            case "recover" -> ops.recuperar(login,System.getenv("I01_PASSWORD"),authorization);
            case "revoke" -> ops.revocar(login,authorization);
            case "evidence" -> Files.writeString(Path.of(file),ops.evidencia(authorization));
            case "locate" -> Files.writeString(Path.of(file),ops.localizar(resource,authorization));
            case "block" -> ops.bloquear(resource,true,authorization);
            case "unblock" -> ops.bloquear(resource,false,authorization);
            case "dispose" -> ops.disponer(Json.read(Files.readString(Path.of(file)),Operaciones.Disposicion.class),authorization);
            case "rectify" -> ops.rectificarIdentidad(Json.read(Files.readString(Path.of(file)),Operaciones.RectificacionIdentidad.class),authorization);
            default -> throw new IllegalArgumentException("Operación técnica desconocida.");
        }
        context.close();
    }
}
