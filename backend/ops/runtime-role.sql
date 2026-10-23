-- Ejecutar una vez como propietario. Contraseña solo desde variable de entorno.
\getenv runtime_password I01_DB_APP_PASSWORD
CREATE ROLE i01_app LOGIN PASSWORD :'runtime_password';
\ir runtime-grants.sql
