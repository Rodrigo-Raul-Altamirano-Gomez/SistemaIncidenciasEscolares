package pe.edu.incidencias.adapter;

import java.sql.*;
import java.text.Normalizer;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.edu.incidencias.application.RegistroPort;
import pe.edu.incidencias.application.RegistroService.Rechazo;
import pe.edu.incidencias.domain.Nota.*;
import tools.jackson.core.type.TypeReference;

@Repository
public class JdbcRegistro implements RegistroPort {
    private final JdbcTemplate db;
    private final Crypto crypto;
    private final Auditoria audit;
    private final Clock clock;
    public JdbcRegistro(JdbcTemplate db,Crypto crypto,Auditoria audit,Clock clock) { this.db=db; this.crypto=crypto; this.audit=audit; this.clock=clock; }
    public record Identidad(String nombre, String documentoTipo, String documento) {}
    public static String normalizar(String s) { return Normalizer.normalize(s.strip().toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("\\s+"," "); }
    @Override public List<Estudiante> buscar(Actor actor,String texto,Integer anio,String grado,String seccion) {
        String query = normalizar(texto);
        List<Estudiante> all = db.query("""
            SELECT s.id,s.school_code,s.identity_enc,e.year,e.grade,e.section FROM student s
            LEFT JOIN enrollment e ON e.student_id=s.id
            WHERE NOT s.blocked AND (?::integer IS NULL OR e.year=?)
              AND (?::text IS NULL OR e.grade=?) AND (?::text IS NULL OR e.section=?)
            ORDER BY s.school_code,e.year DESC,e.effective_from DESC
            """, (rs,n)-> {
                UUID id=rs.getObject("id",UUID.class);
                Identidad identity=Json.read(crypto.open(rs.getString("identity_enc"),"student:"+id),Identidad.class);
                return new Estudiante(id,identity.nombre(),rs.getString("school_code"),(Integer)rs.getObject("year"),rs.getString("grade"),rs.getString("section"));
            },anio,anio,blank(grado),blank(grado),blank(seccion),blank(seccion));
        Set<String> tokens = new LinkedHashSet<>(Arrays.asList(query.split(" ")));
        List<Estudiante> result = all.stream().filter(e -> tokens.stream().allMatch(t -> normalizar(e.nombre()+" "+e.codigo()).contains(t))).limit(51).toList();
        // Solo un resumen HMAC del texto libre, jamás un nombre/documento introducido en la bitácora.
        audit.registrar(actor.id(),"BUSCAR_ESTUDIANTE","OK",null,Map.of("consultaHmac",crypto.fingerprint(query),"anio",anio==null?"todos":anio,"grado",grado==null?"":grado,"seccion",seccion==null?"":seccion,"limite",50));
        return result;
    }
    @Override public List<Categoria> categorias() { return db.query("SELECT * FROM category WHERE active ORDER BY id",(r,n)->new Categoria(r.getString("id"),r.getString("name"),r.getString("nature"),r.getString("severity"))); }
    @Override public List<Integer> anios() { return db.queryForList("SELECT year FROM school_year ORDER BY year DESC",Integer.class); }
    @Override public Vista guardar(Actor actor, UUID requestId, UUID id, Integer version, Entrada entrada) {
        if (entrada==null) throw new IllegalArgumentException("Completa los datos de la nota.");
        // Un bloqueo transaccional serializa el mismo reintento incluso entre conexiones distintas.
        db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",actor.id()+":"+requestId);
        String fingerprint=crypto.fingerprint(Json.write(Arrays.asList(id,version,entrada)));
        var receipts=db.queryForList("SELECT fingerprint,note_id FROM request_receipt WHERE actor_id=? AND request_id=?",actor.id(),requestId);
        if (!receipts.isEmpty()) {
            if (!fingerprint.equals(receipts.getFirst().get("fingerprint"))) throw new Rechazo(409,"Este reintento contiene otros datos. Revisa el resultado antes de guardar de nuevo.");
            Vista previous = cargar(actor,(UUID)receipts.getFirst().get("note_id"),false);
            audit.registrar(actor.id(),"REINTENTO_NOTA","OK",previous.id(),Map.of()); return previous;
        }
        Categoria categoria=categorias().stream().filter(c->c.id().equals(entrada.categoriaId())).findFirst().orElseThrow(()->new IllegalArgumentException("Selecciona una categoría cotidiana disponible."));
        boolean disciplinaria=categoria.naturaleza().equals("DISCIPLINARIA") && categoria.gravedad().equals("LEVE");
        if(!entrada.participantes().isEmpty()) entrada.validar(clock,disciplinaria);
        if (!anios().contains(entrada.anio())) throw new IllegalArgumentException("Selecciona un año escolar disponible.");
        // Orden común con las operaciones técnicas: estudiantes (UUID ordenado), después notas.
        // Se bloquea antes de leer la identidad y antes de tomar FOR UPDATE sobre la nota.
        for(UUID studentId:entrada.participantes().stream().map(Participante::estudianteId).sorted().toList())
            db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,2))",studentId.toString());
        Vista old=null;
        if(id!=null) {
            old=cargar(actor,id,true);
            if(version==null || version!=old.version()) throw new Rechazo(409,"Otra sesión corrigió esta nota. Conserva tu texto y abre la versión actual antes de decidir.");
        }
        if(entrada.participantes().isEmpty()) {
            if(old!=null && old.cuerpo().entrada().participantes().isEmpty()) entrada.validarConParticipacionesRetiradas(clock,disciplinaria);
            else entrada.validar(clock,disciplinaria);
        }
        UUID noteId=id==null?UUID.randomUUID():id;
        List<Participacion> participants=new ArrayList<>();
        for(Participante p:entrada.participantes()) {
            // El bloqueo asesor evita conceder UPDATE sobre la nómina al usuario ordinario del servidor.
            var students=db.queryForList("SELECT * FROM student WHERE id=? AND NOT blocked",p.estudianteId());
            if(students.isEmpty()) throw new Rechazo(422,"Un estudiante seleccionado no está disponible. Revisa la selección.");
            var row=students.getFirst();
            Identidad identity=Json.read(crypto.open((String)row.get("identity_enc"),"student:"+p.estudianteId()),Identidad.class);
            String grade=null,section=null;
            // Si la fecha es aproximada solo se toma grupo si todo el año está cubierto por un único vínculo.
            var groups=db.queryForList("SELECT grade,section FROM enrollment WHERE student_id=? AND year=? AND effective_from<=? AND (effective_to IS NULL OR effective_to>=?) ORDER BY effective_from DESC LIMIT 1",p.estudianteId(),entrada.anio(),entrada.aproximada()?LocalDate.of(entrada.anio(),1,1):entrada.fechaHecho(),entrada.aproximada()?LocalDate.of(entrada.anio(),12,31):entrada.fechaHecho());
            if(!groups.isEmpty()) { grade=(String)groups.getFirst().get("grade"); section=(String)groups.getFirst().get("section"); }
            if(old!=null && entrada.fechaHecho().equals(old.cuerpo().entrada().fechaHecho()) && entrada.aproximada()==old.cuerpo().entrada().aproximada()) {
                var historical=old.cuerpo().participantes().stream().filter(x->x.estudianteId().equals(p.estudianteId())).findFirst();
                if(historical.isPresent()) { grade=historical.get().grado(); section=historical.get().seccion(); }
            }
            participants.add(new Participacion(p.estudianteId(),identity.nombre(),(String)row.get("school_code"),grade,section,p.papel().name()));
        }
        Cuerpo body=new Cuerpo(entrada,categoria.nombre(),categoria.naturaleza(),categoria.gravedad(),participants);
        Instant now=clock.instant(); int next=old==null?1:old.version()+1;
        String enc=crypto.seal(Json.write(body),"note:"+noteId);
        if(old==null) db.update("INSERT INTO note(id,author_id,school_year,category_id,event_date,created_at,updated_at,version,body_enc) VALUES (?,?,?,?,?,?,?,?,?)",noteId,actor.id(),entrada.anio(),entrada.categoriaId(),entrada.fechaHecho(),Timestamp.from(now),Timestamp.from(now),next,enc);
        else {
            List<Cambio> changes=diff(old.cuerpo(),body);
            if(changes.isEmpty()) throw new IllegalArgumentException("No hay cambios que guardar.");
            db.update("UPDATE note SET school_year=?,category_id=?,event_date=?,updated_at=?,version=?,body_enc=? WHERE id=?",entrada.anio(),entrada.categoriaId(),entrada.fechaHecho(),Timestamp.from(now),next,enc,noteId);
            db.update("INSERT INTO note_revision(note_id,version,actor_id,at,changes_enc) VALUES (?,?,?,?,?)",noteId,next,actor.id(),Timestamp.from(now),crypto.seal(Json.write(changes),"revision:"+noteId+":"+next));
            db.update("DELETE FROM note_participant WHERE note_id=?",noteId);
        }
        for(Participante p:entrada.participantes()) {
            db.update("INSERT INTO note_participant(note_id,student_id,role) VALUES (?,?,?)",noteId,p.estudianteId(),p.papel().name());
            db.update("INSERT INTO note_subject VALUES (?,?) ON CONFLICT DO NOTHING",noteId,p.estudianteId());
        }
        db.update("INSERT INTO request_receipt(actor_id,request_id,fingerprint,note_id) VALUES (?,?,?,?)",actor.id(),requestId,fingerprint,noteId);
        audit.registrar(actor.id(),old==null?"CREAR_NOTA":"CORREGIR_NOTA","OK",noteId,Map.of("version",next));
        return cargar(actor,noteId,false);
    }
    public static List<Cambio> diff(Cuerpo old,Cuerpo current) {
        var a=Json.MAPPER.valueToTree(old.entrada()); var b=Json.MAPPER.valueToTree(current.entrada());
        List<Cambio> changes=new ArrayList<>();
        a.properties().forEach(e->{
            if(!e.getValue().equals(b.get(e.getKey()))) {
                if(e.getKey().equals("participantes")) changes.add(new Cambio("participantes",old.participantes(),current.participantes()));
                else if(e.getKey().equals("categoriaId")) changes.add(new Cambio("categoriaId",old.categoriaNombre(),current.categoriaNombre()));
                else changes.add(new Cambio(e.getKey(),e.getValue(),b.get(e.getKey())));
            }
        });
        return changes;
    }
    @Override public Vista leer(Actor actor,UUID id) { Vista v=cargar(actor,id,false); audit.registrar(actor.id(),"LEER_NOTA","OK",id,Map.of()); return v; }
    private Vista cargar(Actor actor,UUID id,boolean lock) {
        var rows=db.queryForList("""
            SELECT n.*,u.name AS author_name FROM note n JOIN app_user u ON u.id=n.author_id
            WHERE n.id=? AND n.author_id=? AND NOT n.blocked
            AND NOT EXISTS (SELECT 1 FROM note_subject p JOIN student s ON s.id=p.student_id WHERE p.note_id=n.id AND s.blocked)
            """+(lock?" FOR UPDATE OF n":""),id,actor.id());
        if(rows.isEmpty()) throw new Rechazo(404,"Registro no disponible.");
        var row=rows.getFirst();
        var revisions=db.query("SELECT r.*,u.name FROM note_revision r JOIN app_user u ON u.id=r.actor_id WHERE note_id=? ORDER BY version",(r,n)->new Revision(r.getInt("version"),r.getString("name"),r.getTimestamp("at").toInstant(),Json.read(crypto.open(r.getString("changes_enc"),"revision:"+id+":"+r.getInt("version")),new TypeReference<List<Cambio>>(){})),id);
        return new Vista(id,(UUID)row.get("author_id"),(String)row.get("author_name"),(Integer)row.get("version"),((Timestamp)row.get("created_at")).toInstant(),((Timestamp)row.get("updated_at")).toInstant(),Json.read(crypto.open((String)row.get("body_enc"),"note:"+id),Cuerpo.class),revisions);
    }
    @Override public Pagina listar(Actor actor,Filtros f) {
        var ids=db.queryForList("""
            SELECT n.id FROM note n WHERE author_id=? AND NOT blocked
            AND NOT EXISTS(SELECT 1 FROM note_subject p JOIN student s ON s.id=p.student_id WHERE p.note_id=n.id AND s.blocked)
            AND (?::uuid IS NULL OR EXISTS(SELECT 1 FROM note_participant p WHERE p.note_id=n.id AND p.student_id=?))
            AND (?::date IS NULL OR event_date>=?) AND (?::date IS NULL OR event_date<=?)
            AND (?::text IS NULL OR category_id=?) AND (?::integer IS NULL OR school_year=?)
            ORDER BY event_date DESC,created_at DESC,id LIMIT 21 OFFSET ?
            """,UUID.class,actor.id(),f.estudianteId(),f.estudianteId(),f.desde(),f.desde(),f.hasta(),f.hasta(),blank(f.categoriaId()),blank(f.categoriaId()),f.anio(),f.anio(),20L*f.pagina());
        List<Vista> data=ids.stream().limit(20).map(id->cargar(actor,id,false)).toList();
        audit.registrar(actor.id(),"LISTAR_PROPIOS","OK",null,f);
        return new Pagina(data,ids.size()>20);
    }
    private static String blank(String s) { return s==null || s.isBlank()?null:s.strip(); }
}
