package pe.edu.incidencias;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.incidencias.adapter.*;
import pe.edu.incidencias.application.RegistroPort.*;
import pe.edu.incidencias.domain.Nota.*;
import tools.jackson.databind.JsonNode;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"app.cookie-secure=false","app.origin=http://localhost"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class I01IntegrationTest {
    @DynamicPropertySource static void baseExclusiva(DynamicPropertyRegistry properties) {
        String url=System.getenv("DB_URL");
        if(url==null || !url.matches("jdbc:postgresql://[^/]+/[A-Za-z0-9_]+_test(?:\\?.*)?"))
            throw new IllegalStateException("Las pruebas destructivas exigen DB_URL de una base exclusiva cuyo nombre termine en _test.");
        properties.add("spring.datasource.url",()->url); properties.add("spring.flyway.url",()->url);
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate db;
    @Autowired Operaciones ops;
    @Autowired Crypto crypto;
    @Autowired PlatformTransactionManager transactions;
    private static final String PASSWORD="Ensayo-I01-Ficticio!";
    private final HttpClient client=HttpClient.newHttpClient();
    record Login(String cookie,String csrf) {}
    record Payload(UUID solicitudId,Integer version,Entrada entrada) {}
    @BeforeAll void provision() { ops.seed(PASSWORD); }
    @BeforeEach void reset() {
        db.execute("DROP TRIGGER IF EXISTS fail_audit_test ON audit_event");
        db.execute("DROP TRIGGER IF EXISTS fail_participant_test ON note_participant");
        db.execute("DROP TRIGGER IF EXISTS slow_note_test ON note");
        db.execute("TRUNCATE note,note_revision,note_participant,request_receipt,audit_event,auth_session,login_throttle RESTART IDENTITY CASCADE");
        db.update("UPDATE app_user SET active=true");
    }
    HttpResponse<String> call(String method,String path,Object body,Login session) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api"+path)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").header("X-I01-Client","true");
        if(session!=null) b.header("Cookie",session.cookie()).header("X-CSRF-Token",session.csrf());
        b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(Json.write(body)));
        return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
    }
    Login login(String user) throws Exception {
        var r=call("POST","/acceso/login",Map.of("usuario",user,"clave",PASSWORD),null); assertEquals(200,r.statusCode(),r.body());
        return new Login(r.headers().firstValue("set-cookie").orElseThrow().split(";")[0],Json.MAPPER.readTree(r.body()).get("csrf").asText());
    }
    Entrada input(String text) { return new Entrada(2026,LocalDate.of(2026,10,1),false,null,"COTIDIANA",text,null,null,null,List.of(new Participante(Operaciones.id("student:0"),Papel.ATRIBUIDA),new Participante(Operaciones.id("student:2"),Papel.TESTIGO)),new Accion("Conversación ficticia",LocalDate.of(2026,10,1),null),new Compromiso("Escuchar el turno","Estudiante ficticio",null)); }
    Vista create(Login s,String text) throws Exception { var r=call("POST","/notas",new Payload(UUID.randomUUID(),null,input(text)),s); assertEquals(200,r.statusCode(),r.body()); return Json.read(r.body(),Vista.class); }
    @Test void tresRolesGuardanCompuestoPersistenYRecuperanEnOtraSesion() throws Exception {
        for(String role:List.of("docente","auxiliar","directivo")) {
            Login first=login(role); Vista v=create(first,"Relato ficticio de "+role);
            assertEquals(2,v.cuerpo().participantes().size()); assertNull(v.cuerpo().participantes().get(1).grado());
            assertEquals("TESTIGO",v.cuerpo().participantes().get(1).papel()); assertNotNull(v.cuerpo().entrada().compromiso());
            var read=call("GET","/notas/"+v.id(),null,login(role)); assertEquals(200,read.statusCode());
            assertEquals(v,Json.read(read.body(),Vista.class));
        }
        assertEquals(3,db.queryForObject("SELECT count(*) FROM note",Integer.class));
        assertEquals(6,db.queryForObject("SELECT count(*) FROM note_participant",Integer.class));
        assertEquals(0,db.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_name IN ('caso','alerta','case','task')",Integer.class));
    }
    @Test void busquedaNormalizadaMinimaHistoricaYFiltros() throws Exception {
        Login s=login("docente"); var r=call("POST","/estudiantes/buscar",Map.of("texto","  sAMIRA   torRES "),s);
        assertEquals(200,r.statusCode(),r.body()); JsonNode data=Json.MAPPER.readTree(r.body()); assertEquals(2,data.size());
        assertFalse(r.body().contains("00000001")); assertFalse(r.body().contains("documento")); assertFalse(r.body().contains("contacto"));
        assertTrue(r.body().contains("Tórres"));
        var historic=call("POST","/estudiantes/buscar",Map.of("texto","histórico","anio",2024),s); assertTrue(historic.body().contains("FIC-004"));
        Vista v=create(s,"Filtro ficticio");
        var filtered=call("POST","/notas/consultar",new Filtros(Operaciones.id("student:0"),LocalDate.of(2026,10,1),LocalDate.of(2026,10,2),"COTIDIANA",2026,0),s);
        assertEquals(200,filtered.statusCode(),filtered.body()); assertTrue(filtered.body().contains(v.id().toString()));
        assertTrue(call("POST","/notas/consultar",new Filtros(null,null,null,null,2024,0),s).body().contains("\"registros\":[]"));
    }
    @Test void accesoAjenoAdministracionAipYSinSesionDenegados() throws Exception {
        Vista v=create(login("docente"),"Contenido reservado ficticio"); Login other=login("ajeno");
        assertEquals(404,call("GET","/notas/"+v.id(),null,other).statusCode());
        assertEquals(404,call("PUT","/notas/"+v.id(),new Payload(UUID.randomUUID(),1,input("Intrusión")),other).statusCode());
        assertFalse(call("POST","/notas/consultar",new Filtros(null,null,null,null,null,0),other).body().contains(v.id().toString()));
        for(String user:List.of("admin","aip")) { Login a=login(user); assertEquals(403,call("GET","/notas/"+v.id(),null,a).statusCode()); assertEquals(403,call("POST","/estudiantes/buscar",Map.of("texto",""),a).statusCode()); }
        assertEquals(401,call("GET","/notas/"+v.id(),null,null).statusCode());
    }
    @Test void logoutExpiracionYRevocacionEnDosSesiones() throws Exception {
        Login a=login("docente"),b=login("docente");
        assertEquals(200,call("POST","/acceso/logout",Map.of(),a).statusCode()); assertEquals(401,call("GET","/opciones",null,a).statusCode());
        db.update("UPDATE auth_session SET last_activity=now()-interval '20 minutes 1 second'"); assertEquals(401,call("GET","/opciones",null,b).statusCode());
        a=login("docente"); b=login("docente"); ops.revocar("docente","TEST-REVOCACION");
        assertEquals(401,call("GET","/opciones",null,a).statusCode()); assertEquals(401,call("GET","/opciones",null,b).statusCode());
    }
    @Test void recuperacionRetiraAmbasSesionesYNoPermiteClaveVieja() throws Exception {
        Login a=login("auxiliar"),b=login("auxiliar");
        try {
            ops.recuperar("auxiliar","Nueva-clave-ficticia!","TEST-RECUPERACION");
            assertEquals(401,call("GET","/opciones",null,a).statusCode()); assertEquals(401,call("GET","/opciones",null,b).statusCode());
            assertEquals(401,call("POST","/acceso/login",Map.of("usuario","auxiliar","clave",PASSWORD),null).statusCode());
            assertEquals(200,call("POST","/acceso/login",Map.of("usuario","auxiliar","clave","Nueva-clave-ficticia!"),null).statusCode());
        } finally { ops.recuperar("auxiliar",PASSWORD,"TEST-REPOSICION"); }
    }
    @Test void limitaIntentosSinBloquearOtrosUsuarios() throws Exception {
        for(int i=0;i<5;i++) assertEquals(401,call("POST","/acceso/login",Map.of("usuario","docente","clave","incorrecta"),null).statusCode());
        assertEquals(429,call("POST","/acceso/login",Map.of("usuario","docente","clave",PASSWORD),null).statusCode());
        assertNotNull(login("auxiliar"));
    }
    @Test void dobleEnvioConcurrentePerdidaRespuestaYClaveReutilizada() throws Exception {
        Login s=login("docente"); Payload p=new Payload(UUID.randomUUID(),null,input("Reintento ficticio"));
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var x=pool.submit(()->call("POST","/notas",p,s)); var y=pool.submit(()->call("POST","/notas",p,s));
            var a=x.get(); var b=y.get(); assertEquals(200,a.statusCode(),a.body()); assertEquals(200,b.statusCode(),b.body()); assertEquals(Json.read(a.body(),Vista.class).id(),Json.read(b.body(),Vista.class).id());
        }
        assertEquals(200,call("POST","/notas",p,s).statusCode());
        assertEquals(1,db.queryForObject("SELECT count(*) FROM note",Integer.class));
        assertEquals(409,call("POST","/notas",new Payload(p.solicitudId(),null,input("Otros datos")),s).statusCode());
    }
    @Test void correccionAutomaticaYDosEdicionesNoSePisan() throws Exception {
        Login s=login("docente"),second=login("docente"); Vista v=create(s,"Original ficticio");
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var a=pool.submit(()->call("PUT","/notas/"+v.id(),new Payload(UUID.randomUUID(),1,input("Corrección A")),s));
            var b=pool.submit(()->call("PUT","/notas/"+v.id(),new Payload(UUID.randomUUID(),1,input("Corrección B")),second));
            assertEquals(List.of(200,409),List.of(a.get().statusCode(),b.get().statusCode()).stream().sorted().toList());
        }
        Vista updated=Json.read(call("GET","/notas/"+v.id(),null,s).body(),Vista.class);
        assertEquals(2,updated.version()); assertEquals(1,updated.rectificaciones().size()); assertEquals("descripcion",updated.rectificaciones().getFirst().cambios().getFirst().campo());
        assertTrue(ops.evidencia("TEST-EVIDENCIA").contains("CORREGIR_NOTA"));
    }
    @Test void falloDeEvidenciaYDePersistenciaRevierteTodo() throws Exception {
        Login s=login("docente");
        for(String table:List.of("audit_event","note_participant")) {
            String trigger=table.equals("audit_event")?"fail_audit_test":"fail_participant_test";
            db.execute("CREATE OR REPLACE FUNCTION fail_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Injected failure'; END $$");
            db.execute("CREATE TRIGGER "+trigger+" BEFORE INSERT ON "+table+" FOR EACH ROW EXECUTE FUNCTION fail_test()");
            Payload p=new Payload(UUID.randomUUID(),null,input("Fallo ficticio"));
            try { var r=call("POST","/notas",p,s); assertEquals(503,r.statusCode(),r.body()); assertEquals(0,db.queryForObject("SELECT count(*) FROM note",Integer.class)); assertEquals(0,db.queryForObject("SELECT count(*) FROM request_receipt",Integer.class)); }
            finally { db.execute("DROP TRIGGER "+trigger+" ON "+table); }
            assertEquals(200,call("POST","/notas",p,s).statusCode());
            db.execute("TRUNCATE note CASCADE");
        }
    }
    @Test void validacionTextoInerteCifradoBloqueoYEvidenciaProtegida() throws Exception {
        Login s=login("docente"); String script="<script>window.__xss=1</script> Relato ficticio"; Vista v=create(s,script);
        assertEquals(script,Json.read(call("GET","/notas/"+v.id(),null,s).body(),Vista.class).cuerpo().entrada().descripcion());
        assertFalse(db.queryForObject("SELECT body_enc FROM note WHERE id=?",String.class,v.id()).contains("script"));
        ops.bloquear(v.id(),true,"TEST-BLOQUEO"); assertEquals(404,call("GET","/notas/"+v.id(),null,s).statusCode());
        assertFalse(call("POST","/notas/consultar",new Filtros(null,null,null,null,null,0),s).body().contains(v.id().toString()));
        assertThrows(Exception.class,()->db.update("UPDATE audit_event SET result='ALTERADO'"));
        assertFalse(ops.evidencia("TEST-EXTRACCION").contains("Relato ficticio"));
    }
    @Test void csrfYReferenciasInvalidasRechazados() throws Exception {
        var encoded=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/%61pi/opciones")).GET().build();
        assertEquals(401,client.send(encoded,HttpResponse.BodyHandlers.ofString()).statusCode());
        Login s=login("docente"); assertEquals(403,call("POST","/notas",new Payload(UUID.randomUUID(),null,input("Ficticio")),new Login(s.cookie(),"incorrecto")).statusCode());
        var e=input("Ficticio"); var missing=new Entrada(e.anio(),e.fechaHecho(),false,null,e.categoriaId(),e.descripcion(),null,null,null,List.of(new Participante(UUID.randomUUID(),Papel.ATRIBUIDA)),null,null);
        assertEquals(422,call("POST","/notas",new Payload(UUID.randomUUID(),null,missing),s).statusCode());
        var future=new Entrada(2099,LocalDate.of(2099,1,1),false,null,e.categoriaId(),e.descripcion(),null,null,null,e.participantes(),null,null);
        assertEquals(422,call("POST","/notas",new Payload(UUID.randomUUID(),null,future),s).statusCode());
    }
    @Test void revocarRolConSesionAbiertaYRechazarDatosFueraDeContrato() throws Exception {
        Login s=login("directivo");
        var extra=new LinkedHashMap<String,Object>(); extra.put("solicitudId",UUID.randomUUID()); extra.put("entrada",input("Ficticio")); extra.put("autorId",Operaciones.id("user:docente"));
        assertEquals(400,call("POST","/notas",extra,s).statusCode());
        assertEquals(413,call("POST","/estudiantes/buscar",Map.of("texto","x".repeat(34000)),s).statusCode());
        UUID uid=Operaciones.id("user:directivo");
        try {
            db.update("UPDATE user_role SET valid_to=? WHERE user_id=?",LocalDate.of(2020,1,2),uid);
            assertEquals(403,call("GET","/opciones",null,s).statusCode());
        } finally { db.update("UPDATE user_role SET valid_to=null WHERE user_id=?",uid); }
    }
    @Test void disposicionDelimitadaPreservaParticipacionDeTercero() throws Exception {
        Login s=login("docente"); Vista v=create(s,"Descripción revisada y ficticia"); UUID target=Operaciones.id("student:2");
        String location=ops.localizar(target,"TEST-LOCALIZACION");
        assertTrue(location.contains(v.id().toString())); assertFalse(location.contains("Descripción revisada")); assertFalse(location.contains("Samira"));
        var old=v.cuerpo(); var entry=old.entrada();
        Entrada replacement=new Entrada(entry.anio(),entry.fechaHecho(),entry.aproximada(),entry.hora(),entry.categoriaId(),"Descripción aprobada sin la persona retirada",null,null,null,List.of(entry.participantes().getFirst()),entry.accion(),entry.compromiso());
        Cuerpo body=new Cuerpo(replacement,old.categoriaNombre(),old.naturaleza(),old.gravedad(),List.of(old.participantes().getFirst()));
        assertThrows(Exception.class,()->ops.disponer(new Operaciones.Disposicion(target,List.of()),"TEST-INCOMPLETO"));
        Cuerpo altered=new Cuerpo(replacement,"Clasificación alterada",old.naturaleza(),old.gravedad(),body.participantes());
        assertThrows(Exception.class,()->ops.disponer(new Operaciones.Disposicion(target,List.of(new Operaciones.Sustitucion(v.id(),1,altered,Map.of()))),"TEST-TERCEROS"));
        Entrada empty=new Entrada(entry.anio(),entry.fechaHecho(),entry.aproximada(),entry.hora(),entry.categoriaId(),"No autoriza retirar al tercero",null,null,null,List.of(),null,null);
        Cuerpo noThirdParty=new Cuerpo(empty,old.categoriaNombre(),old.naturaleza(),old.gravedad(),List.of());
        assertThrows(IllegalArgumentException.class,()->ops.disponer(new Operaciones.Disposicion(target,List.of(new Operaciones.Sustitucion(v.id(),1,noThirdParty,Map.of()))),"TEST-NO-RETIRAR-TERCERO"));
        assertEquals(2,db.queryForObject("SELECT count(*) FROM note_participant WHERE note_id=?",Integer.class,v.id()));
        ops.disponer(new Operaciones.Disposicion(target,List.of(new Operaciones.Sustitucion(v.id(),1,body,Map.of()))),"TEST-DISPOSICION");
        assertEquals(0,db.queryForObject("SELECT count(*) FROM student WHERE id=?",Integer.class,target));
        Vista after=Json.read(call("GET","/notas/"+v.id(),null,s).body(),Vista.class);
        assertEquals(List.of(old.participantes().getFirst()),after.cuerpo().participantes());
        assertEquals(entry.accion(),after.cuerpo().entrada().accion()); assertEquals(entry.compromiso(),after.cuerpo().entrada().compromiso());
        // Repone solo la identidad ficticia para los demás ensayos independientes.
        ops.seed(PASSWORD);
    }
    @Test void rectificacionTecnicaDeIdentidadActualizaCopiasSinRomperTerceros() throws Exception {
        Login s=login("docente"); Payload intent=new Payload(UUID.randomUUID(),null,input("Relato ficticio sin nombres en texto libre"));
        var response=call("POST","/notas",intent,s); assertEquals(200,response.statusCode(),response.body());
        Vista v=Json.read(response.body(),Vista.class); UUID target=Operaciones.id("student:2");
        String original=db.queryForObject("SELECT identity_enc FROM student WHERE id=?",String.class,target);
        try {
            var identity=new JdbcRegistro.Identidad("Álex Ficticio Rectificado","DNI","00000999");
            var old=v.cuerpo();
            var participants=old.participantes().stream().map(p->p.estudianteId().equals(target)?new Participacion(target,identity.nombre(),p.codigo(),p.grado(),p.seccion(),p.papel()):p).toList();
            var body=new Cuerpo(old.entrada(),old.categoriaNombre(),old.naturaleza(),old.gravedad(),participants);
            assertThrows(Exception.class,()->ops.rectificarIdentidad(new Operaciones.RectificacionIdentidad(target,identity,List.of()),"TEST-INCOMPLETO"));
            ops.rectificarIdentidad(new Operaciones.RectificacionIdentidad(target,identity,List.of(new Operaciones.Sustitucion(v.id(),1,body,Map.of()))),"TEST-RECTIFICACION");
            Vista after=Json.read(call("GET","/notas/"+v.id(),null,s).body(),Vista.class);
            assertEquals(v.cuerpo().participantes().getFirst(),after.cuerpo().participantes().getFirst());
            assertEquals(identity.nombre(),after.cuerpo().participantes().get(1).nombre());
            assertTrue(ops.localizar(target,"TEST-RELECTURA").contains("00000999"));
            var search=call("POST","/estudiantes/buscar",Map.of("texto","Alex Ficticio Rectificado"),s);
            assertTrue(search.body().contains(identity.nombre())); assertFalse(search.body().contains("00000999"));
            assertEquals(2,db.queryForObject("SELECT count(*) FROM note_participant WHERE note_id=?",Integer.class,v.id()));
            var retry=call("POST","/notas",intent,s); assertEquals(200,retry.statusCode(),retry.body());
            assertEquals(v.id(),Json.read(retry.body(),Vista.class).id());
            assertEquals(1,db.queryForObject("SELECT count(*) FROM note",Integer.class));
        } finally { db.update("UPDATE student SET identity_enc=? WHERE id=?",original,target); }
    }
    Entrada with(Entrada e,List<Participante> participants,Accion action,Compromiso commitment,String text) {
        return new Entrada(e.anio(),e.fechaHecho(),e.aproximada(),e.hora(),e.categoriaId(),text,e.lugar(),e.comunicante(),e.canal(),participants,action,commitment);
    }
    Operaciones.Sustitucion withdrawal(Vista v,UUID sid) {
        var b=v.cuerpo(); var e=b.entrada();
        var after=with(e,e.participantes().stream().filter(p->!p.estudianteId().equals(sid)).toList(),null,null,"Remanente ficticio aprobado sin datos de la persona retirada");
        var revisions=new HashMap<Integer,List<Cambio>>();
        for(var revision:v.rectificaciones()) revisions.put(revision.version(),List.of(new Cambio("descripcion","Texto previo revisado","Texto posterior revisado")));
        return new Operaciones.Sustitucion(v.id(),v.version(),new Cuerpo(after,b.categoriaNombre(),b.naturaleza(),b.gravedad(),b.participantes().stream().filter(p->!p.estudianteId().equals(sid)).toList()),revisions);
    }
    @Test void compromisoIndependientePersisteSeLeeYSeCorrigeSinInventarAccion() throws Exception {
        Login session=login("docente"); var e=input("Compromiso ficticio independiente");
        var entry=with(e,e.participantes(),null,e.compromiso(),e.descripcion());
        var response=call("POST","/notas",new Payload(UUID.randomUUID(),null,entry),session);
        assertEquals(200,response.statusCode(),response.body()); Vista v=Json.read(response.body(),Vista.class);
        Vista read=Json.read(call("GET","/notas/"+v.id(),null,login("docente")).body(),Vista.class);
        assertNull(read.cuerpo().entrada().accion()); assertEquals(entry.compromiso(),read.cuerpo().entrada().compromiso());
        var changed=with(entry,entry.participantes(),e.accion(),entry.compromiso(),entry.descripcion());
        assertEquals(200,call("PUT","/notas/"+v.id(),new Payload(UUID.randomUUID(),1,changed),session).statusCode());
        assertEquals(200,call("PUT","/notas/"+v.id(),new Payload(UUID.randomUUID(),2,entry),session).statusCode());
        assertEquals(1,db.queryForObject("SELECT count(*) FROM note",Integer.class));
    }
    @Test void disposicionUnicoParticipanteConservaRemanenteYPermiteCorreccionSinFabricarIdentidad() throws Exception {
        Login session=login("auxiliar"); UUID sid=Operaciones.id("student:2"); var base=input("Relato ficticio aprobado");
        var entry=with(base,List.of(new Participante(sid,Papel.TESTIGO)),null,null,base.descripcion());
        var response=call("POST","/notas",new Payload(UUID.randomUUID(),null,entry),session); assertEquals(200,response.statusCode());
        Vista v=Json.read(response.body(),Vista.class);
        var corrected=with(entry,entry.participantes(),null,null,"Corrección ficticia antes de disposición");
        response=call("PUT","/notas/"+v.id(),new Payload(UUID.randomUUID(),1,corrected),session); assertEquals(200,response.statusCode()); v=Json.read(response.body(),Vista.class);
        try {
            var replacement=withdrawal(v,sid);
            ops.disponer(new Operaciones.Disposicion(sid,List.of(replacement)),"TEST-UNICO-PARTICIPANTE");
            Vista after=Json.read(call("GET","/notas/"+v.id(),null,session).body(),Vista.class);
            assertTrue(after.cuerpo().participantes().isEmpty()); assertTrue(after.cuerpo().entrada().participantes().isEmpty());
            assertEquals(v.registrada(),after.registrada()); assertEquals(v.autorId(),after.autorId()); assertEquals(v.cuerpo().entrada().fechaHecho(),after.cuerpo().entrada().fechaHecho());
            assertEquals(0,db.queryForObject("SELECT count(*) FROM note_subject WHERE student_id=?",Integer.class,sid));
            assertEquals(0,db.queryForObject("SELECT count(*) FROM student WHERE id=?",Integer.class,sid));
            assertFalse(Json.write(after).contains(sid.toString())); assertFalse(Json.write(after).contains("Álex"));
            assertTrue(call("POST","/notas/consultar",new Filtros(null,null,null,null,null,0),session).body().contains(v.id().toString()));
            var empty=with(after.cuerpo().entrada(),List.of(),null,null,"Corrección legítima del remanente aprobado");
            assertEquals(422,call("POST","/notas",new Payload(UUID.randomUUID(),null,empty),session).statusCode());
            assertEquals(200,call("PUT","/notas/"+v.id(),new Payload(UUID.randomUUID(),after.version(),empty),session).statusCode());
            assertEquals(404,call("GET","/notas/"+v.id(),null,login("ajeno")).statusCode());
            assertEquals(1,db.queryForObject("SELECT count(*) FROM note",Integer.class));
            assertEquals(1,db.queryForObject("SELECT count(*) FROM audit_event WHERE operation='DISPONER_DATOS'",Integer.class));
        } finally { ops.seed(PASSWORD); }
    }
    void awaitStudentWait() {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<until) {
            if(db.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE pid<>pg_backend_pid() AND pg_backend_pid()=ANY(pg_blocking_pids(pid)) AND wait_event_type='Lock' AND wait_event='advisory'",Integer.class)>0) return;
            try { Thread.sleep(10); } catch(InterruptedException ex) { Thread.currentThread().interrupt(); throw new AssertionError(ex); }
        }
        fail("La operación HTTP no llegó al bloqueo de estudiante en otra conexión.");
    }
    @Test @Timeout(30) void correccionYDisposicionSerializanSinInversionDeBloqueos() throws Exception {
        Login session=login("docente"); Vista v=create(session,"Concurrencia ficticia"); UUID sid=Operaciones.id("student:2");
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            @SuppressWarnings("unchecked") Future<HttpResponse<String>>[] request=new Future[1];
            new TransactionTemplate(transactions).executeWithoutResult(status->{
                db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,2))",sid.toString());
                request[0]=pool.submit(()->call("PUT","/notas/"+v.id(),new Payload(UUID.randomUUID(),1,input("Corrección concurrente")),session));
                awaitStudentWait();
                ops.disponer(new Operaciones.Disposicion(sid,List.of(withdrawal(v,sid))),"TEST-CONCURRENCIA");
            });
            var response=request[0].get(15,TimeUnit.SECONDS); assertEquals(409,response.statusCode(),response.body());
            assertEquals(1,db.queryForObject("SELECT count(*) FROM note_participant WHERE note_id=?",Integer.class,v.id()));
        } finally { ops.seed(PASSWORD); }
    }
    @Test @Timeout(30) void altaYDisposicionNoReintroducenIdentidadRetirada() throws Exception {
        Login session=login("docente"); UUID sid=Operaciones.id("student:2");
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            @SuppressWarnings("unchecked") Future<HttpResponse<String>>[] request=new Future[1];
            new TransactionTemplate(transactions).executeWithoutResult(status->{
                db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,2))",sid.toString());
                request[0]=pool.submit(()->call("POST","/notas",new Payload(UUID.randomUUID(),null,input("Alta concurrente")),session));
                awaitStudentWait(); ops.disponer(new Operaciones.Disposicion(sid,List.of()),"TEST-ALTA-CONCURRENTE");
            });
            var response=request[0].get(15,TimeUnit.SECONDS); assertEquals(422,response.statusCode(),response.body());
            assertEquals(0,db.queryForObject("SELECT count(*) FROM note",Integer.class));
            assertEquals(0,db.queryForObject("SELECT count(*) FROM request_receipt",Integer.class));
        } finally { ops.seed(PASSWORD); }
    }
    @Test @Timeout(30) void correccionYRectificacionIdentidadNoSeInterbloquean() throws Exception {
        Login session=login("docente"); Vista v=create(session,"Corrección e identidad ficticias"); UUID sid=Operaciones.id("student:2");
        String original=db.queryForObject("SELECT identity_enc FROM student WHERE id=?",String.class,sid);
        var identity=new JdbcRegistro.Identidad("Identidad ficticia revisada",null,null); var body=v.cuerpo();
        var replacement=new Cuerpo(body.entrada(),body.categoriaNombre(),body.naturaleza(),body.gravedad(),body.participantes().stream().map(p->p.estudianteId().equals(sid)?new Participacion(sid,identity.nombre(),p.codigo(),p.grado(),p.seccion(),p.papel()):p).toList());
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            @SuppressWarnings("unchecked") Future<HttpResponse<String>>[] request=new Future[1];
            new TransactionTemplate(transactions).executeWithoutResult(status->{
                db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,2))",sid.toString());
                request[0]=pool.submit(()->call("PUT","/notas/"+v.id(),new Payload(UUID.randomUUID(),1,input("Edición concurrente a identidad")),session));
                awaitStudentWait(); ops.rectificarIdentidad(new Operaciones.RectificacionIdentidad(sid,identity,List.of(new Operaciones.Sustitucion(v.id(),1,replacement,Map.of()))),"TEST-IDENTIDAD-CONCURRENTE");
            });
            var response=request[0].get(15,TimeUnit.SECONDS); assertEquals(409,response.statusCode(),response.body());
            assertTrue(call("GET","/notas/"+v.id(),null,session).body().contains(identity.nombre()));
        } finally { db.update("UPDATE student SET identity_enc=? WHERE id=?",original,sid); }
    }
    @Test @Timeout(30) void dosDisposicionesSobreNotasCompartidasOrdenanBloqueosYRechazanManifiestoObsoleto() throws Exception {
        Login session=login("docente"); Vista a=create(session,"Hecho compartido A"),b=create(session,"Hecho compartido B");
        UUID first=Operaciones.id("student:0"),second=Operaciones.id("student:2"); var start=new CyclicBarrier(2);
        db.execute("CREATE OR REPLACE FUNCTION slow_note_test_fn() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN PERFORM pg_sleep(0.15); RETURN NEW; END $$");
        db.execute("CREATE TRIGGER slow_note_test BEFORE UPDATE ON note FOR EACH ROW EXECUTE FUNCTION slow_note_test_fn()");
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Boolean> x=()->{start.await(); try {ops.disponer(new Operaciones.Disposicion(first,List.of(withdrawal(a,first),withdrawal(b,first))),"TEST-DOS-A"); return true;} catch(IllegalArgumentException ex) {assertTrue(ex.getMessage().contains("cambió"),ex.getMessage()); return false;}};
            Callable<Boolean> y=()->{start.await(); try {ops.disponer(new Operaciones.Disposicion(second,List.of(withdrawal(b,second),withdrawal(a,second))),"TEST-DOS-B"); return true;} catch(IllegalArgumentException ex) {assertTrue(ex.getMessage().contains("cambió"),ex.getMessage()); return false;}};
            var one=pool.submit(x); var two=pool.submit(y); assertNotEquals(one.get(15,TimeUnit.SECONDS),two.get(15,TimeUnit.SECONDS));
            assertEquals(2,db.queryForObject("SELECT count(*) FROM note_participant",Integer.class));
            assertEquals(1,db.queryForObject("SELECT count(*) FROM audit_event WHERE operation='DISPONER_DATOS'",Integer.class));
        } finally { db.execute("DROP TRIGGER slow_note_test ON note"); ops.seed(PASSWORD); }
    }
    @Test void paginacionAcotadaYEntradasMalformadasNoLleganAlListado() throws Exception {
        Login session=login("docente");
        for(int page:List.of(-1,1001,Integer.MAX_VALUE)) assertEquals(422,call("POST","/notas/consultar",Map.of("pagina",page),session).statusCode());
        for(Object page:List.of(1.5,"abc",2147483648L)) assertEquals(400,call("POST","/notas/consultar",Map.of("pagina",page),session).statusCode());
        var nullPage=new HashMap<String,Object>(); nullPage.put("pagina",null);
        assertEquals(400,call("POST","/notas/consultar",nullPage,session).statusCode());
        assertEquals(422,call("POST","/notas/consultar",Map.of("pagina",0,"anio",Integer.MAX_VALUE),session).statusCode());
        assertEquals(422,call("POST","/notas/consultar",Map.of("pagina",0,"categoriaId","x".repeat(1000)),session).statusCode());
        assertEquals(422,call("POST","/estudiantes/buscar",Map.of("texto","","grado","x".repeat(33)),session).statusCode());
        assertEquals(0,db.queryForObject("SELECT count(*) FROM audit_event WHERE operation='LISTAR_PROPIOS'",Integer.class));
        for(int page:List.of(0,1000)) assertEquals(200,call("POST","/notas/consultar",Map.of("pagina",page),session).statusCode());
        assertEquals(2,db.queryForObject("SELECT count(*) FROM audit_event WHERE operation='LISTAR_PROPIOS'",Integer.class));
    }
    @Test void sesionDeVistaImpideLeerConCookieDeOtraCuentaYMetodosInvalidosNoSonCaidas() throws Exception {
        Login first=login("docente"),second=login("ajeno"); Vista v=create(first,"Reservado de otra identidad");
        assertEquals(403,call("GET","/notas/"+v.id(),null,new Login(second.cookie(),first.csrf())).statusCode());
        assertEquals(200,call("GET","/acceso/sesion",null,new Login(second.cookie(),first.csrf())).statusCode());
        assertEquals(404,call("GET","/notas/"+v.id(),null,second).statusCode());
        assertEquals(405,call("DELETE","/notas/"+v.id(),null,first).statusCode());
    }
    @Test void ipCompartidaNoBloqueaCredencialesValidasNiConfiaEnCabecerasDeOrigen() throws Exception {
        String ip=crypto.fingerprint("ip:127.0.0.1");
        db.update("INSERT INTO login_throttle VALUES (?,50,now())",ip);
        for(String user:List.of("docente","auxiliar","directivo")) assertNotNull(login(user));
        assertEquals(429,call("POST","/acceso/login",Map.of("usuario","no-existe","clave","incorrecta"),null).statusCode());
        var spoof=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/acceso/login")).header("Content-Type","application/json").header("X-I01-Client","true").header("X-Forwarded-For","203.0.113.8").header("Forwarded","for=203.0.113.8;proto=https").POST(HttpRequest.BodyPublishers.ofString(Json.write(Map.of("usuario","otra-inexistente","clave","incorrecta")))).build();
        assertEquals(429,client.send(spoof,HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(52,db.queryForObject("SELECT attempts FROM login_throttle WHERE key=?",Integer.class,ip));
        assertEquals(0,db.queryForObject("SELECT count(*) FROM login_throttle WHERE key=?",Integer.class,crypto.fingerprint("ip:203.0.113.8")));
        db.update("UPDATE login_throttle SET started_at=now()-interval '16 minutes'");
        Login expired=login("ajeno"); db.update("UPDATE auth_session SET last_activity=now()-interval '21 minutes' WHERE token_hash=?",Crypto.hash(expired.cookie().substring("I01_SESSION=".length())));
        assertNotNull(login("docente")); assertEquals(401,call("GET","/opciones",null,expired).statusCode());
        assertEquals(0,db.queryForObject("SELECT count(*) FROM login_throttle",Integer.class));
    }
    @Test @Timeout(30) void saturacionDeLoginEsBreveAcotadaYTrazable() throws Exception {
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var requests=new ArrayList<Future<HttpResponse<String>>>();
            new TransactionTemplate(transactions).executeWithoutResult(status->{
                db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,1))",crypto.fingerprint("login:docente"));
                for(int i=0;i<4;i++) requests.add(pool.submit(()->call("POST","/acceso/login",Map.of("usuario","docente","clave",PASSWORD),null)));
                long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
                while(db.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE pid<>pg_backend_pid() AND pg_backend_pid()=ANY(pg_blocking_pids(pid)) AND wait_event='advisory'",Integer.class)<4 && System.nanoTime()<until) {
                    try {Thread.sleep(10);} catch(InterruptedException ex){throw new AssertionError(ex);}
                }
                try { var response=call("POST","/acceso/login",Map.of("usuario","auxiliar","clave",PASSWORD),null); assertEquals(429,response.statusCode()); assertEquals("1",response.headers().firstValue("Retry-After").orElseThrow()); }
                catch(Exception ex) {throw new AssertionError(ex);}
            });
            for(var request:requests) assertEquals(200,request.get(15,TimeUnit.SECONDS).statusCode());
            assertNotNull(login("auxiliar"));
            assertEquals(1,db.queryForObject("SELECT count(*) FROM audit_event WHERE result='SATURADO'",Integer.class));
        }
    }
    @Test void disposicionRevierteTodoSiNoPuedeGuardarEvidencia() throws Exception {
        Vista v=create(login("docente"),"Relato ficticio para reversión técnica"); UUID sid=Operaciones.id("student:2");
        db.execute("CREATE OR REPLACE FUNCTION fail_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Injected failure'; END $$");
        db.execute("CREATE TRIGGER fail_audit_test BEFORE INSERT ON audit_event FOR EACH ROW EXECUTE FUNCTION fail_test()");
        try {
            assertThrows(Exception.class,()->ops.disponer(new Operaciones.Disposicion(sid,List.of(withdrawal(v,sid))),"TEST-ROLLBACK-TECNICO"));
            assertEquals(1,db.queryForObject("SELECT count(*) FROM student WHERE id=?",Integer.class,sid));
            assertEquals(2,db.queryForObject("SELECT count(*) FROM note_participant WHERE note_id=?",Integer.class,v.id()));
            assertEquals(1,db.queryForObject("SELECT version FROM note WHERE id=?",Integer.class,v.id()));
            assertEquals(1,db.queryForObject("SELECT count(*) FROM request_receipt WHERE note_id=?",Integer.class,v.id()));
            Cuerpo body=Json.read(crypto.open(db.queryForObject("SELECT body_enc FROM note WHERE id=?",String.class,v.id()),"note:"+v.id()),Cuerpo.class);
            assertEquals(v.cuerpo(),body);
        } finally { db.execute("DROP TRIGGER fail_audit_test ON audit_event"); }
    }
    @Test void contenidoCorruptoNoSeEntregaYEntradasHttpIncorrectasNoSeConfundenConCaida() throws Exception {
        Login session=login("docente"); Vista v=create(session,"Relato ficticio reservado");
        db.update("UPDATE note SET body_enc='alterado-ficticio' WHERE id=?",v.id());
        var response=call("GET","/notas/"+v.id(),null,session); assertEquals(503,response.statusCode()); assertFalse(response.body().contains("Relato ficticio")); assertFalse(response.body().contains("alterado-ficticio"));
        var chunked=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/estudiantes/buscar")).header("Content-Type","application/json").header("X-I01-Client","true").header("Cookie",session.cookie()).header("X-CSRF-Token",session.csrf()).POST(HttpRequest.BodyPublishers.ofInputStream(()->new java.io.ByteArrayInputStream(Json.write(Map.of("texto","x".repeat(34000))).getBytes(java.nio.charset.StandardCharsets.UTF_8)))).build();
        assertEquals(413,client.send(chunked,HttpResponse.BodyHandlers.ofString()).statusCode());
        var wrongMedia=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/notas")).header("Content-Type","text/plain").header("X-I01-Client","true").header("Cookie",session.cookie()).header("X-CSRF-Token",session.csrf()).POST(HttpRequest.BodyPublishers.ofString("texto ficticio")).build();
        assertEquals(415,client.send(wrongMedia,HttpResponse.BodyHandlers.ofString()).statusCode());
    }
}
