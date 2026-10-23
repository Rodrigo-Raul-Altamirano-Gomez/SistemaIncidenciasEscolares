package pe.edu.incidencias.adapter;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Auditoria {
    private final JdbcTemplate db;
    public Auditoria(JdbcTemplate db) { this.db = db; }
    public void registrar(UUID actor, String operacion, String resultado, UUID referencia, Object metadata) {
        db.update("INSERT INTO audit_event(actor_id,operation,result,resource,metadata) VALUES (?,?,?,?,?::jsonb)",actor,operacion,resultado,referencia,Json.write(metadata));
    }
}
