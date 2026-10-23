# Sistema de incidencias escolares — I01

Registro cotidiano autónomo implementado sobre los proyectos existentes `backend/` y `frontend/`: acceso individual, búsqueda mínima, registro con varios estudiantes y papeles independientes, acción/compromiso opcionales, consulta y corrección propias. No crea atención ni alertas.

El alcance normativo está en `docs/ERS_Incidencias_Escolares.md` (v1.2) y `docs/PLAN_IMPLEMENTACION_INCREMENTOS_VERTICALES.md` (v1.1). Las pruebas usan exclusivamente datos ficticios.

La [revisión técnica de I01](docs/IMPLEMENTACION_I01.md) contiene cobertura, puntos de entrada, decisiones, resultados, configuración, reproducción y limitaciones. Es la referencia de puesta en marcha de esta entrega. No se declara aptitud para datos reales ni aceptación global de los requisitos no funcionales.

Con Java 25–27, Node.js 24 con npm y PostgreSQL disponible:

```powershell
cd frontend
npm ci
npm run build
npm run lint
cd ../backend
# Definir DB_URL, DB_USER, DB_PASSWORD y DATA_KEY; usar una base exclusiva de pruebas.
.\mvnw.cmd -B clean package
```

El JAR incorpora la distribución del frontend previamente compilada. La precarga y los permisos mínimos se preparan como indica el informe. El servidor ordinario usa `i01_app`, migraciones desactivadas y HTTPS; el propietario se reserva para migración y operaciones técnicas autorizadas.

Pruebas de navegador: con el servidor de ensayo iniciado, definir `I01_PASSWORD` y ejecutar `npm run test:e2e` en `frontend/`. `I01_URL` permite seleccionar otra dirección. Por defecto usa Edge y `https://localhost:8443`, con certificado local de ensayo.
