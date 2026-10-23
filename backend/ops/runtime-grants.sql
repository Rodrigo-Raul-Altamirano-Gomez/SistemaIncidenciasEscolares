-- Aplicar como propietario después de migrar y, al restaurar, después de revisar decisiones vigentes.
GRANT CONNECT ON DATABASE :"DBNAME" TO i01_app;
GRANT USAGE ON SCHEMA public TO i01_app;
GRANT SELECT ON app_user,user_role,school_year,student,enrollment,category TO i01_app;
GRANT SELECT,INSERT,UPDATE,DELETE ON auth_session,login_throttle,note,note_participant,note_subject,note_revision,request_receipt TO i01_app;
GRANT INSERT ON audit_event TO i01_app;
GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA public TO i01_app;
