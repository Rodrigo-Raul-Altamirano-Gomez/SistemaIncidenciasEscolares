# Implementación de I01 — revisión técnica independiente

Fecha de verificación: **23/10/2026, zona America/Lima**. Alcance: ficha I01 y controles proporcionales T01–T08 del plan v1.1, sobre la ERS v1.2. «Registro cotidiano» e «incidencia» son los términos de la interfaz; `Nota`/`note`/`/api/notas` son nombres internos del mismo aporte, no calificaciones académicas.

Fuentes conservadas sin modificación:

- `docs/ERS_Incidencias_Escolares.md`: SHA-256 `DD52FAC9091B3B7B42B447D884E13D712BF5497186BAF481A3DB23E7CFB95366`.
- `docs/PLAN_IMPLEMENTACION_INCREMENTOS_VERTICALES.md`: SHA-256 `A5FE28B88BDC4C40C99E3356876B67743179FD0800631B6B15A6FA36FCF3B001`.

La inspección inicial encontró el POM y wrapper del backend, sin fuentes Java, y el frontend inicial de Vite. Había compilados/informes antiguos en `backend/target/`; **no constituyen evidencia de esta entrega**. Se trabajó en ambas carpetas existentes. Las eliminaciones de las antiguas carpetas `SistemaIncidenciasEscolaresBackend/` y `untitled/` ya figuraban en Git al iniciar. No se restauró ni amplió aquel dominio de incrementos posteriores. El README se actualizó para retirar afirmaciones antiguas que ya no describían los proyectos presentes.

## 1. Cobertura y aceptación

**I/V** = implementado y verificado en el alcance indicado. **P** = solo esa parte del RF/CU; no significa completitud global. La evidencia de ejecución se conserva en `docs/evidencias/I01_RESULTADOS.json`; las clases y métodos de prueba citados son la comprobación reproducible. Las limitaciones de aceptación están en §6.

| Referencia | Alcance entregado y estado | Dónde comprobarlo |
|---|---|---|
| RF-01, P | Cuentas ficticias individuales, acceso, cierre, limitación y recuperación técnica. Sin administración escolar de cuentas. I/V. | `adapter/Acceso.java`, `Operaciones.seed/recuperar/revocar`; pruebas `logoutExpiracionYRevocacionEnDosSesiones`, `recuperacionRetiraAmbasSesionesYNoPermiteClaveVieja`, `limitaIntentosSinBloquearOtrosUsuarios`. |
| RF-02, P | Año explícito y matrícula histórica precargada; grupo del hecho separado de la referencia de búsqueda. No hay mantenimiento/promoción de años/grupos. I/V. | `JdbcRegistro.buscar/guardar`, `school_year`, `enrollment`, `Cuerpo.participantes`; `busquedaNormalizadaMinimaHistoricaYFiltros`. |
| RF-04, P | Fragmentos/tokens de nombre o código escolar, mayúsculas/espacios/tildes, año/grado/sección, homónimos y matrícula anterior. Sin DNI, contacto, antecedentes o señales en respuesta ordinaria. I/V. | `JdbcRegistro.normalizar/buscar`; `Estudiante` es el DTO mínimo. Pruebas de búsqueda y tres recorridos E2E. La búsqueda no usa el DNI reservado. |
| RF-06, P | Identidad UUID estable, nombre íntegro, documento textual opcional cifrado, matrícula y grupo conocido/ausente. I/V para la precarga y consulta mínima. No contactos/contexto ni importador. | `V1__i01.sql`, `Identidad`, `Operaciones.seed`; FIC-003 sin documento/grupo y FIC-004 de 2024. |
| RF-12, P | Fecha real/pasada/aproximada, categoría cotidiana o por clasificar, descripción, varios estudiantes, autor/registro automáticos; hora/lugar/comunicante/canal opcionales. I/V. No comunicaciones graves ni externos. | `domain/Nota.Entrada.validar`, `RegistroService.guardar`, `JdbcRegistro.guardar`; `NotaTest`, pruebas de contrato/integridad y E2E. |
| RF-13, P | Papel individual visible y corregible. Solo el primer estudiante de categoría disciplinaria propone ATRIBUIDA; los adicionales proponen DESCONOCIDO. Afectado/testigo no heredan atribución. Por clasificar no permite ATRIBUIDA. I/V. | `App.agregar`, selectores de papel, `Nota.validar`; `papelesIndependientesYDocumentoNoObligatorio`, `aproximadaNoInventaHoraYPorClasificarNoAtribuye`, recorridos E2E. No hay corroboración, presuntas responsabilidades ni cálculo de reiteración. |
| RF-14, P | Acción realizada, resultado y compromiso opcionales en el mismo guardado; contenido/persona/fecha pertinente, sin plazo obligatorio. I/V. Recuperación desde atención queda fuera de I01. | `Entrada.accion/compromiso`, una sola llamada `App.guardar`; `tresRolesGuardanCompuestoPersistenYRecuperanEnOtraSesion` y E2E de los tres roles. |
| RF-15, P | Listado propio por estudiante, intervalo de fechas del hecho, categoría y año; detalle y rectificaciones propias. I/V. No relaciones con atención/duplicados todavía. | `Api.listar/leer`, `JdbcRegistro.listar/cargar`; `busquedaNormalizadaMinimaHistoricaYFiltros`, `accesoAjenoAdministracionAipYSinSesionDenegados`. |
| RF-17, P | Corrección del aporte propio, versión esperada y diferencias automáticas, con autor/fecha, sin motivo rutinario. I/V. Todas las incidencias de I01 carecen de atención; aclaración tras atención/anulación institucional no se implementan aquí. | `JdbcRegistro.diff/guardar`, `note_revision`, `App.editar`; `correccionAutomaticaYDosEdicionesNoSePisan` y E2E. |
| RF-38, P | Evidencia automática de acceso/cierre, búsqueda/listado/lectura, creación/corrección/reintento y operaciones técnicas; extracción inmediata por propietario autorizado, sin panel. I/V. | `Auditoria.registrar`, `audit_event`, `Operaciones.evidencia`, CLI `evidence`; pruebas de fallo de evidencia y de protección de bitácora. Filtrado externo documentado en §5. |
| RF-39, P | Localización técnica sin relatos de terceros, corrección propia y rectificación técnica de identidad/copias, bloqueo de la incidencia y disposición por manifiesto autorizado sobre identidad, vínculos, cuerpo, rectificaciones y referencias actuales/históricas. I/V del ensayo delimitado. | `Operaciones.localizar/rectificarIdentidad/bloquear/disponer`, CLI `locate/rectify/block/unblock/dispose`; `rectificacionTecnicaDeIdentidadActualizaCopiasSinRomperTerceros`, `disposicionDelimitadaPreservaParticipacionDeTercero`, `validacionTextoInerteCifradoBloqueoYEvidenciaProtegida`. La institución decide el alcance y revisa los textos sustitutos; no hay motor jurídico. |
| RF-41, P | Copia cifrada con configuración/ejecutables y estado/fecha; comprobación de autenticidad y restauración aislada con conciliación de tablas. I/V del ensayo inicial. | `ops/backup.ps1`, `ops/test-backup.ps1`, resultados y procedimiento de §5. No se declara respaldo diario operativo ni entrega integral del producto futuro. |

Las rutas Java de la tabla son relativas a `backend/src/main/java/pe/edu/incidencias/`; las pruebas Java están en `backend/src/test/java/pe/edu/incidencias/`.

### Criterios de la ficha y controles comunes

| Criterio / control | Resultado y evidencia verificable |
|---|---|
| Una confirmación conserva hecho, papeles, acción y compromiso; no crea caso/tarea | I/V: operación transaccional `RegistroService.guardar` → `JdbcRegistro.guardar`; prueba compuesta revisa filas y ausencia de tablas de caso/tarea/alerta. E2E guarda sin aprobación, documento ni adjunto. |
| Fechas no futuras, ausencia y aproximación visibles | I/V en dominio/API; pruebas `fechaYHoraFuturaRechazadas`, `fechaPasadaYGrupoNoObligatorio`, `validacion...`, `csrfYReferenciasInvalidasRechazados`. Fecha y hora de Perú se calculan con `Clock` inyectado. La fecha aproximada no admite hora; si el vínculo no cubre de forma inequívoca el período, grupo no informado. |
| T01/T02; RN-07; RNF-01/02/06 | I/V: identidad real, cookie opaca HttpOnly/Secure/SameSite Strict, CSRF, 20 min sin actividad con datos, aviso previo y borrador solo en memoria oculto tras expiración. Servidor verifica cuenta/roles vigentes en cada operación. GET de estado no renueva actividad. Usuario ajeno obtiene 404 sin contenido; ADMIN/AIP no adquieren registro/lectura por esos roles. La recuperación retira ambas sesiones. Las rutas escapadas también se autentican (prueba `csrfYReferenciasInvalidasRechazados`). La prueba de expiración del servidor altera `last_activity` en PostgreSQL; la prueba UI adelanta solo el reloj del navegador. |
| T03; RNF-07 | I/V: evidencia y modificación comparten transacción; fallo inyectado revierte filas y recibo. INSERT de evidencia es el único permiso del servidor sobre bitácora; trigger impide UPDATE/DELETE de eventos de menos de dos años incluso al propietario ordinario. El DBA con capacidad de desactivar triggers sigue siendo un custodio autorizado, no un operador escolar. No hay purga automática ni plazo de expediente inferido de esos dos años. |
| T04; RNF-08 | I/V: UUID de intención persistido, HMAC del contenido, bloqueo de reintento, persona única, claves foráneas, versión y transacciones. Doble envío concurrente, pérdida de respuesta, conflicto de dos ediciones, fallo de participante y fallo de evidencia probados. La UI inmoviliza el contenido cuyo guardado es incierto y reintenta la misma intención. |
| T05; RNF-03/04/05 | I/V local: cuerpos, identidades y diferencias con AES-256-GCM y ámbito autenticado; bcrypt 12 para contraseñas; solo hash de token en base; secretos por entorno; HTTP rechazado si cookie segura está activada; E2E por TLS. Texto se representa como texto React, CSP sin script/estilo inline, sin HTML aportado por usuario. JSON limitado a 32 KiB también sin Content-Length; límites por campo y 30 participantes. No hay telemetría ni cargas de archivos en I01. Copias cifradas y temporales con ACL restringida. Certificado local de ensayo; confianza PKI real no aceptada. |
| T06; RNF-10/12/13/14/15 | I/V de pantallas entregadas: mensajes en español, éxito solo tras respuesta, preservación ante error/red, aviso de abandono, foco/etiquetas, controles nativos, 320 px y 200 %. Prueba completa de teclado y comprobación visual. Papel/categoría seleccionados se repiten en texto ajustable para evitar perder su significado en selectores estrechos. Lector de pantalla, representantes y matriz completa de navegadores **no ejecutados**; no se afirma conformidad WCAG integral. |
| T07; RNF-17/18/20/21/22/23/24 | I/V de medidas iniciales: migraciones, compilación limpia, copia/restauración, estado de fallos, guía e inventario de §3/5. El ensayo recupera datos/configuración/ejecutable y elimina sesiones antiguas antes de habilitar acceso. Frecuencia operativa, copia externa al equipo, disponibilidad mensual y autonomía de otro técnico no se dan por demostradas. |
| T08; RNF-09/16/19/25 | I/V con datos ficticios: navegador → aplicación → JDBC → PostgreSQL → otra sesión y corrección; negativos y recuperación. Inventario Maven y consulta OSV; auditoría npm. Se midieron 20 búsquedas y 20 guardados en muestra local (p95: 92,20 ms y 100,17 ms respectivamente, hasta resultado visible); no la escala/latencia/concurrencia global de ERS 4.1. Consultas vuelven a datos persistidos y filtros de autor; no hay conteos de responsabilidades, estadísticas ni reiteración simulados. |

RN-02/04/05/13 se aplican mediante vocabulario, papeles y acción opcional sin sanción/caso/corroboración. RN-17 se representa por identidad independiente del vínculo anual y snapshot de grupo. RN-24/25/26 se aplican al procedimiento autorizado, evidencia, conservación y recuperación aislada; no se decide legitimación dentro del sistema. RN-27 se refleja en la ayuda para registro posterior con fecha efectiva y continuidad institucional durante caídas. RN-28: no existe un ajuste para desactivar reserva, integridad o papeles.

CU-02 es **parcial**: recorrido cotidiano probado; señal automática de reiteración diferida a I05. CU-14 solo tiene precarga/recuperación técnica; no mantenimiento escolar. CU-15 tiene el ensayo técnico delimitado, no aceptación de todos los derechos. CU-16 tiene restauración básica, no objetivos operativos completos. CU-17 tiene automatización/inspección inicial, sin aceptación con representantes. RNF-11 y la matriz global de RNF-15 están pendientes. RF-40 no se declara implementado.

## 2. Código, entradas y relaciones

| Ruta / símbolos relevantes | Responsabilidad y relación |
|---|---|
| `backend/src/main/java/pe/edu/incidencias/IncidenciasApplication.java` | Arranque Boot y `Clock` de America/Lima. `pom.xml` conserva identidad del proyecto y empaqueta `frontend/dist` en recursos estáticos; exige compilar antes el frontend. |
| `domain/Nota.java` — `Entrada`, `Participante`, `Accion`, `Compromiso`, `validar` | Reglas independientes de Spring/HTTP/JDBC. No consulta reloj global: recibe `Clock`. |
| `application/RegistroPort.java` | Puerto de persistencia para búsqueda, opciones, guardado, lectura/listado y DTO mínimos. `Vista` contiene la clasificación/grupo persistidos y rectificaciones autorizadas. |
| `application/RegistroService.java` | Entrada de aplicación; autoriza rol funcional, valida filtros y delimita transacciones. Delega I/O al puerto. No hay interfaz/capa por cada tabla. |
| `adapter/Api.java`, `SeguridadHttp.java`, `Errores.java` | Contrato HTTP, sesión/CSRF/origen, límites, no-store/CSP, errores corregibles sin SQL/datos personales. Cada ruta de datos pasa por filtro y servicio. |
| `adapter/Acceso.java` | Contraseñas, limitación persistente por cuenta/fuente, sesiones, vigencias actuales y retirada. El token presentado no es el valor almacenado. |
| `adapter/JdbcRegistro.java` — `guardar`, `cargar`, `listar`, `buscar`, `diff` | Implementación del puerto con SQL parametrizado; snapshots, cifrado, evidencia, reintentos, bloqueo y control concurrente. |
| `adapter/Crypto.java`, `Json.java`, `Auditoria.java` | AES-GCM con nonce aleatorio y ámbito por recurso, HMAC, serialización y evidencia sin relatos. El cifrado no sustituye permisos de base/host. |
| `adapter/Operaciones.java` — servicio y `ComandoOperativo` | Precarga ficticia y operaciones locales autorizadas. Sin endpoints técnicos ni rol escolar universal. `--i01.command` solo se usa con aplicación web desactivada y credenciales de propietario. |
| `frontend/src/main.tsx`, `App.tsx`, `api.ts` | Arranque React; acceso, búsqueda, formulario, consulta/detalle/corrección y ayuda. `guardar` mantiene una intención; `editar` usa versión; `expire` oculta vistas; borrador asociado a la cuenta. `api` ajusta la advertencia a tiempo del servidor y clasifica errores. No localStorage/sessionStorage. |
| `frontend/src/App.css`, `index.css`, `index.html` | Distribución adaptable, foco, contraste, etiquetas/idioma y tamaños. Sin dependencias visuales externas. |
| `backend/ops/runtime-role.sql`, `runtime-grants.sql`, `backup.ps1`, `test-backup.ps1` | Separación de credenciales, copia/restauración y ensayo reproducible. |

Contrato actual (mismo origen; JSON):

- `POST /api/acceso/login` `{usuario,clave}`; `GET /api/acceso/sesion`; `POST /api/acceso/logout`.
- `GET /api/opciones`; `POST /api/estudiantes/buscar` `{texto,anio?,grado?,seccion?}`. Se usa POST para evitar términos personales en URL.
- `POST /api/notas` y `PUT /api/notas/{uuid}` reciben `{solicitudId,version,entrada}`; el servidor obtiene autor/fechas/grupos. La corrección exige la versión leída.
- `POST /api/notas/consultar` recibe `Filtros` (campos opcionales más página desde 0); `GET /api/notas/{uuid}`. Página de 20, una fila extra determina `hayMas`; búsqueda devuelve hasta 51 y la UI avisa al superar 50.
- El login exige `X-I01-Client: true` y el origen configurado cuando el navegador lo envía; las demás operaciones POST/PUT requieren además `X-CSRF-Token` de la sesión. Sin CORS abierto. 400: formato; 401: sesión; 403: permiso/CSRF/origen; 404: aporte no disponible; 409: intención diferente o edición concurrente; 413/422: límites/reglas; 503: resultado no confirmado. No se reintenta automáticamente una intención con otro contenido.

## 3. Decisiones que afectan conformidad

**Persistencia y dominio.** Monolito Boot/JDBC, dominio Java puro, aplicación transaccional y un puerto para las interacciones existentes. Sin JPA, colas, microservicios, motor de reglas ni modelo anticipado de atención. El cuerpo cifrado representa el reporte cotidiano de un hecho con varios participantes; no se fijó una incidencia por alumno. Categoría/naturaleza/gravedad y grupo quedan guardados, no calculados al consultar según el estado actual de la matrícula. La corrección conserva grupo anterior cuando fecha/aproximación no cambian; si cambia la fecha, se vuelve a buscar vínculo efectivo. Sin vínculo inequívoco se conserva ausencia.

**Esquema.** `V1__i01.sql` crea `app_user`, `user_role`, sesiones/limitación, `student`, `enrollment`, `school_year`, `category`, `note`, `note_participant`, `note_revision`, `request_receipt` y `audit_event`. `V2__referencias_historicas.sql` añade `note_subject`, con referencias de quienes participaron alguna vez: una corrección no hace desaparecer su localización ni permite ignorar su bloqueo. Relaciones por UUID/FK; unicidad por estudiante/hecho y por intención/autor. No hay borrado por cierre/año. Flyway registra y valida versiones. La reversión operativa es restaurar copia y ejecutables anteriores en base nueva aislada; no hay downgrade destructivo de tablas.

**Unidad de confirmación.** Creación/corrección, participantes, referencias históricas, diferencias, recibo y evidencia se confirman juntos. Bloqueo asesor serializa la misma intención y coordina identidad con disposición; bloqueo de fila y versión evitan sobrescritura. Un recibo repetido devuelve la vista actual autorizada de la misma incidencia; no archiva otra copia del contenido que pueda sobrevivir a una disposición. Una intención con contenido distinto recibe 409.

**Reserva y custodia.** La consulta SQL restringe autor y bloqueos antes de descifrar/responder, incluidos sujetos históricos; AIP y administrador sin rol funcional no leen. El rol PostgreSQL `i01_app` solo lee nómina/cuentas/catálogo, modifica tablas operativas y solo inserta evidencia. No puede cambiar claves/roles ni leer/editar bitácora. El propietario se usa fuera del servidor para migraciones/decisiones; sus credenciales requieren custodia institucional específica. Roles de prueba adicionales existen para negativos, no habilitan funcionalidades de incrementos posteriores.

**Cifrado/búsqueda.** `DATA_KEY` aleatoria de 32 bytes, sin valor por defecto; identidad/documento, cuerpo y diferencias se cifran con AES-GCM y ámbito UUID/versión. Búsqueda descifra solo identidad mínima en memoria y normaliza sin reescribir nombres. Evita un índice nominal en claro; su rendimiento a escala completa sigue pendiente. Códigos escolares, fechas, papeles, relaciones y nombres laborales de operadores no se cifran por campo: quedan bajo permisos de base y host. El despliegue real debe proteger el volumen/ACL, credenciales y transporte hacia una base remota. Los criterios libres de búsqueda se registran como HMAC; no se recupera su texto original por la bitácora. Año/grado/sección y alcance permiten contextualizarla; para contrastar una búsqueda concreta, un técnico autorizado calcula su misma huella. Es una decisión de minimización revisable por la institución.

**Sesión y texto pendiente.** Caducidad autoritativa del servidor de 1 200 segundos. La advertencia usa el plazo recibido, corregido respecto del reloj del servidor. No hay guardado offline ni persistencia del borrador en navegador: sigue solo en la memoria de la pestaña, oculto y asociado a la misma cuenta. Recargar/cerrar puede descartarlo después del aviso del navegador; una cuenta diferente lo borra. Logout confirmado limpia vistas, búsquedas y texto. El control del navegador no sustituye la autorización del servidor.

**Dependencias/costos.** Inventario exacto de 54 componentes Maven de ejecución y 74 paquetes del lock npm en `evidencias/I01_DEPENDENCIAS.json`, con licencias declaradas en sus POM/paquetes y URLs. Boot 4.1.2/Spring 7.0.10, Flyway 12.4.0, pgJDBC 42.7.14, React 19.3.1, Vite 8.3.5, TypeScript 6.0.3, Playwright 1.62.1 y PostgreSQL 17.11 fueron los componentes ensayados. Spring/Java libraries principales, Maven, TypeScript y Playwright: Apache-2.0; React/Vite/oxlint: MIT; PostgreSQL/driver: licencia PostgreSQL/BSD según componente. La elección de JDK debe conservar sus términos: el ensayo usó Oracle JDK 27 instalado; una distribución OpenJDK compatible evita depender de esa instalación concreta. Los transitivos incluyen Logback con EPL-2.0/LGPL-2.1-only y Jakarta Annotation con EPL-2.0/GPL2 con Classpath Exception; el lock npm también contiene MPL-2.0, ISC y BSD-3-Clause. El inventario conserva la declaración por componente, sin suponer una licencia única para todos. Edge solo fue navegador de prueba. Referencias consultadas: [requisitos oficiales Boot](https://docs.spring.io/spring-boot/system-requirements.html), [avisos Spring](https://spring.io/security), [binarios PostgreSQL para Windows](https://www.postgresql.org/download/windows/), [OSV API](https://google.github.io/osv.dev/api/).

No hay servicio SaaS, licencia por puesto, API pagada, correo externo ni cuenta personal de desarrollador exigidos. Recursos: un host Java, PostgreSQL, almacenamiento principal y copia separada, certificado TLS y responsable operativo. El ensayo local tiene costo marginal de software 0; **no hay cotización de alojamiento real** ni estimación de crecimiento validada. Estimación a completar por la institución: tarifa mensual host + base/volumen + almacenamiento de al menos dos copias + certificado/dominio si tienen costo + horas de revisión/recuperación. Esta falta impide declarar RNF-24 completo, sin inventar un presupuesto. Documentar responsables, medio/retención de copias, jornada y renovaciones antes de operación real.

## 4. Pruebas y evidencias reales

### Ejecución

- Windows 11 build 26300, AMD Ryzen 7 250 (16 procesadores lógicos), 32 GB de memoria; Java 27 compilando `release 25`, Maven wrapper 3.9.16, Node 24.22.1, PostgreSQL 17.11 portátil, Edge 155.0.4283.62/Playwright 1.62.1. Base `i01_test` exclusiva para Java; servidor ordinario con `i01_app`, HTTPS y certificado autofirmado local. Las pruebas de navegador ignoran únicamente la confianza de ese certificado de ensayo. La comprobación adicional registró TLS 1.3, cookie HttpOnly/Secure/Strict, 401 anónimo, 404 para el autor del aporte bloqueado y 404 al intentar una ruta previsible de respaldo. La [captura de 320 px](evidencias/I01_320.png) se inspeccionó visualmente; los pares CSS personalizados de texto/botón/foco/borde medidos superan 4,5:1, sin extender esa medida a conformidad WCAG integral.
- `npm ci`/instalación equivalente con lock, `npm run build` y `npm run lint`: compilación y revisión estática correctas, sin advertencias finales de lint. El entorno no tenía `npm` en PATH; se invocó `node .local/package/bin/npm-cli.js` (npm 11.13.0 descargado). El artefacto reproducible usa comandos npm estándar.
- `backend/mvnw.cmd -f backend/pom.xml -B -o clean package`, con variables de base de pruebas: **22 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**. `CryptoTest`: 1; `NotaTest`: 7; `I01IntegrationTest`: 14. La compilación limpia evita contabilizar clases antiguas. También se ejecutó `mvnw.cmd -B -o test` tras añadir `baseExclusiva`, que exige una URL de base con sufijo `_test` antes de preparar el contexto.
- `node frontend/node_modules/@playwright/test/cli.js test`, desde `frontend/` como `node node_modules/@playwright/test/cli.js test`: recorridos E2E, y repetición usando los ejecutables/configuración de la copia restaurada. **8 pruebas E2E aprobadas, 0 fallos, 0 omitidas**, en la ejecución final sobre el JAR recuperado, cuya huella coincide con el entregable. No se suman dos veces esas ocho pruebas por figurar como verificación del frontend y de recuperación. Resultado y métricas exactas en `evidencias/I01_RESULTADOS.json`.
- `backend/ops/test-backup.ps1`: dos copias cifradas, rechazo de alteración/clave distinta, restauración en una base nueva aislada y comparación de cantidades/huellas de las 11 tablas de datos/configuración. `auth_session` debe quedar vacía y los bloqueos persistir. El ensayo final conservó 53 incidencias, 62 participaciones y 6 rectificaciones; las 11 tablas coincidieron por cantidad/huella, quedó un aporte bloqueado y cero sesiones antiguas. Copia: 4,890 s; restauración aislada: 5,733 s. La comparación se realizó antes de repetir E2E; esas pruebas posteriores añaden registros ficticios a la copia restaurada. La recuperación del servicio se comprobó después con las ocho pruebas E2E. El ensayo y la habilitación posterior se distinguen en evidencias.
- `mvn dependency:list -DincludeScope=runtime` y OSV `querybatch`: **54 componentes Maven consultados, sin avisos devueltos** en esa consulta. `npm audit --json`: resultado archivado en evidencias; el informe no equivale a garantía de ausencia de vulnerabilidades o revisión de penetración. Las consultas envían coordenadas/versiones, ningún dato escolar.

La copia elegida de esta ejecución está en `.local/i01-delivery-actual.i01b`, con anterior `.local/i01-delivery-anterior.i01b`, estados adyacentes y ejecutables/configuración recuperados en `.local/i01-delivery-actual.i01b.recovered/`. La base de ensayo recuperada fue `i01_restore_delivery`. Estos medios locales, secretos y compilados no se versionan; la evidencia de revisión está en `docs/evidencias/`. Tras comprobarla se detuvieron las cinco instancias Java de ensayo y PostgreSQL portátil (`.local/pgdata`, puerto 55432); la guía de reproducción presupone que PostgreSQL está iniciado.

### Qué comprueban los archivos de prueba

| Archivo | Casos y fallos significativos |
|---|---|
| `NotaTest.java` | Diferencias de participantes con nombre/código legibles, papeles independientes, persona única, futuro/hora, fecha aproximada sin hora, atribución vedada fuera de disciplinaria leve, fecha pasada y normalización. |
| `CryptoTest.java` | Nonce distinto, descifrado correcto, rechazo por ámbito incorrecto y por alteración del contenido autenticado. |
| `I01IntegrationTest.java` | Tres roles/otra sesión; búsqueda mínima histórica y filtros; lectura/escritura ajena y ADMIN/AIP; logout, 20 min, baja y recuperación con dos sesiones; cinco fallos y limitación posterior; pérdida de respuesta/doble envío simultáneo; dos ediciones simultáneas con un 409; triggers de fallo de participante/evidencia con rollback completo; texto inerte/cifrado/bloqueo/bitácora protegida; CSRF/FK/futuro; campo extra/límite de cuerpo/fin de vigencia; localizar/disponer sin entregar relato/identidad de terceros y conservar participación ajena; rectificación técnica de identidad propagada a sus copias con UUID/matrícula conservados. Usa PostgreSQL real y HTTP real. Los triggers de fallo existen solo durante los tests en la base de ensayo. |
| `frontend/tests/i01.spec.ts` | Los tres roles guardan, recuperan en otro contexto y corrigen; intercepción tras respuesta del servidor simula pérdida de respuesta ya persistida y verifica una sola incidencia; HTML inocuo no se ejecuta; 320 px, 200 %, abandono y teclado; reloj UI de expiración y borrador de la misma cuenta, borrado para cuenta distinta; muestra de veinte búsquedas/guardados. El test de teclado recorre foco con Tab/Enter, sin clics durante el registro. |
| `ops/test-backup.ps1` | Comparación independiente de tablas originales/restauradas, retirada de sesiones, restricción persistente, estado/fallo visible y existencia de copia anterior. No sobrescribe la base original. |

Fallos encontrados y corregidos durante la verificación: el primer guardado con `i01_app` fallaba porque `SELECT ... FOR SHARE` requería UPDATE de estudiantes; se sustituyó por bloqueo asesor sin ampliar esos permisos. Los campos de texto poblados tenían etiquetas implícitas ambiguas para la herramienta/lectura; se separaron etiquetas explícitas y textarea. La prueba de ampliación inicialmente intentó insertar estilo inline y CSP lo rechazó: se cambió el mecanismo de ensayo sin debilitar CSP. Se ajustó la sincronización de la advertencia de sesión. Un intento de reempaquetar el JAR en ejecución falló por bloqueo de Windows: la guía exige detenerlo antes de actualizar. Ninguno de esos intentos fallidos cuenta como aceptación.

**No ejecutado:** ensayo humano con docente/auxiliar/TOE y AIP, lector de pantalla, cuatro combinaciones de versiones de dos familias de escritorio y Android; escala completa de ERS 4.1, diez usuarios concurrentes y enlace con 100 ms/10 Mb/s; disponibilidad mensual; frecuencia diaria y pérdida real ≤24 h; recuperación por un segundo técnico; copia en otro equipo; retención operativa a largo plazo y puerta de seguridad integral antes de datos reales. El rendimiento medido es una muestra de 4 estudiantes/1 usuario sobre loopback, hasta resultado visible, sin extrapolarlo a aceptación global.

## 5. Reproducir la verificación

### Configuración y arranque mínimo

Requiere JDK 25–27, Node 24/npm, PowerShell 7 y PostgreSQL con `psql`, `createdb`, `pg_dump`, `pg_restore` (Windows; otra plataforma necesita adaptar los scripts operativos). Comprobar la versión en las herramientas. Todos los comandos siguientes parten de la raíz del repositorio, en un entorno aislado con datos ficticios. Nunca apuntar los tests a una base real: `I01IntegrationTest.reset` trunca sus tablas de ensayo.

1. Preparar PostgreSQL bajo una cuenta autorizada; seleccionar `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`. Generar y custodiar `DATA_KEY` de 32 bytes Base64 y una contraseña para `i01_app`. Los secretos no se imprimen ni se versionan. `backend/.env.example` enumera variables; Spring no carga ese archivo automáticamente. Proteger los ficheros locales con ACL/volumen cifrado antes de guardar claves. Para PostgreSQL remoto, configurar TLS y validación de certificado en `DB_URL`.
2. Crear dos bases vacías: `createdb i01` y `createdb i01_test`. Con credenciales del propietario, definir `DB_URL=jdbc:postgresql://HOST:PUERTO/i01_test`, `DB_USER`, `DB_PASSWORD` y la misma `DATA_KEY` durante cada ensayo.

```powershell
# Generación para ensayo; conservar el valor en un medio local protegido.
$env:DATA_KEY = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
cd frontend
npm ci
npm run build
npm run lint
cd ../backend
.\mvnw.cmd -B clean package
cd ..
```

3. Cambiar `DB_URL` a `i01` manteniendo credenciales de propietario. Precargar y migrar (no abre servidor):

```powershell
$env:I01_PASSWORD = 'Ensayo-I01-Ficticio!' # Solo cuentas ficticias; nunca credencial real.
java -jar backend/target/SistemaIncidenciasEscolaresBackend-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --i01.command=seed --i01.authorization=ENSAYO-I01
$env:I01_DB_APP_PASSWORD = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
psql -d i01 -v ON_ERROR_STOP=1 -f backend/ops/runtime-role.sql
```

`runtime-role.sql` crea el rol una vez. En una instalación ya preparada o después de nuevas migraciones, usar solo `runtime-grants.sql`. El propietario migra por separado; el servidor funciona con Flyway desactivado. No ejecutar `seed` en datos reales ni presentarlo como importador/mantenimiento. La repetición no sobrescribe cuentas/identidades existentes ni restablece contraseñas; para eso está `recover`.

4. Preparar TLS. Para ensayo local puede usarse un PKCS12 de `keytool` con SAN `localhost`/`127.0.0.1`; fuera del ensayo se requiere certificado confiable. Conservar almacén/contraseña en un medio protegido. Definir `SERVER_SSL_KEY_STORE_PASSWORD`, `APP_ORIGIN=https://localhost:8443`, `COOKIE_SECURE=true`, `DB_USER=i01_app`, `DB_PASSWORD` con la contraseña del rol; retirar variables `DB_OWNER*` del proceso ordinario. Mantener `DATA_KEY` de la precarga.

```powershell
# La carpeta .local queda excluida del repositorio; proteger su ACL antes de guardar secretos.
keytool -genkeypair -alias i01 -keyalg RSA -keysize 3072 -validity 14 -storetype PKCS12 -keystore .local/i01.p12 -storepass:env SERVER_SSL_KEY_STORE_PASSWORD -dname 'CN=localhost' -ext 'SAN=dns:localhost,ip:127.0.0.1'
java -jar backend/target/SistemaIncidenciasEscolaresBackend-0.1.0-SNAPSHOT.jar --spring.flyway.enabled=false --server.port=8443 --server.ssl.enabled=true --server.ssl.key-store=.local/i01.p12 --server.ssl.key-store-type=PKCS12
```

El frontend ya está dentro del JAR. Abrir `https://localhost:8443`. Para desarrollo exclusivamente ficticio y loopback puede usarse Vite/proxy (`npm run dev`), backend HTTP 8080, `COOKIE_SECURE=false` y `APP_ORIGIN=http://127.0.0.1:5173`; ese modo no demuestra protección de transmisión.

### Datos y guion de revisión

Usuarios individuales `docente`, `auxiliar`, `directivo`, `ajeno`, `admin`, `aip`, todos ficticios, con la contraseña de precarga. Los tres primeros registran; `ajeno` permite verificar reserva entre autores; ADMIN/AIP sirven para denegaciones. Samira Torres Luna/FIC-001 y Samira Tórres Luna/FIC-002 son homónimos diferenciados por código/grupo; Álex Muñoz del Río/FIC-003 no tiene documento/grupo; Estudiante Histórico Ficticio/FIC-004 solo tiene vínculo 2024. Catálogo ficticio: Conducta cotidiana de prueba/leve y Por clasificar. No hay umbrales aprobados.

Entrar como auxiliar → Nuevo registro → buscar `  SAMIRA  TORRES ` → añadir FIC-001 y FIC-002 → comprobar que solo el primero propone atribución → cambiar el segundo a afectado/testigo → descripción ficticia → acción y compromiso opcionales → un guardado. En otra sesión del auxiliar, consultar por estudiante/fechas/categoría/año, abrir y corregir descripción: debe mostrar antes/después/autor/fecha. Entrar como `ajeno`: no debe aparecer; un enlace GET/PUT con ese UUID devuelve 404. Repetir con docente/directivo, sin acción/documento/compromiso; probar FIC-003 y el estudiante histórico seleccionando fecha/año 2024. Por clasificar obliga a papel distinto de atribución cotidiana. Intentar fecha futura y comprobar mensaje/entrada conservada.

En otra terminal con servidor iniciado:

```powershell
cd frontend
$env:I01_PASSWORD = 'Ensayo-I01-Ficticio!'
$env:I01_URL = 'https://localhost:8443'
npm run test:e2e
```

Por defecto las pruebas requieren Edge instalado. Para otra familia, añadir el proyecto correspondiente a `playwright.config.ts` e instalar su navegador de Playwright; no se da por probado solo por añadirlo. Resultados completos se regeneran en `frontend/test-results/results.json`; trazas/capturas solo de fallos se mantienen fuera de Git y contienen exclusivamente datos ficticios. Java regenera `backend/target/surefire-reports/`.

### Evidencia, recuperación de acceso y decisiones sobre datos

Estas operaciones usan **credenciales de propietario, aplicación web desactivada y referencia externa de autorización**, fuera de la cuenta escolar. Comprobar identidad del solicitante y alcance institucional por un canal protegido. No poner nombres, relato o fundamento jurídico íntegro en `--i01.authorization`: usar referencia a la constancia custodiada por el colegio. Registrar operador técnico real en esa constancia; los eventos técnicos identifican la ejecución y referencia, sin crear un rol universal.

```powershell
# DB_USER/DB_PASSWORD del propietario; DATA_KEY de la instalación.
java -jar backend/target/SistemaIncidenciasEscolaresBackend-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --i01.command=evidence --i01.authorization=CONSTANCIA-FICTICIA-01 --i01.file=.local/evidencia.json
# Filtrar la extracción en el medio protegido por período, usuario, operación y resource.
$eventos = Get-Content .local/evidencia.json -Raw | ConvertFrom-Json
$eventos | Where-Object { $_.operation -eq 'CORREGIR_NOTA' }
```

`recover --i01.login=auxiliar` lee la nueva contraseña de `I01_PASSWORD`, incrementa versión de acceso y retira todas las sesiones anteriores; entregar la nueva clave por canal protegido. `revoke --i01.login=...` retira cuenta/sesiones; no existe UI administrativa en I01. No modificar la contraseña directamente en claro ni compartir la cuenta técnica. Usar `locate --i01.resource=UUID_ESTUDIANTE --i01.file=RUTA_PROTEGIDA` para obtener identidad y referencias sin relatos/identidades de terceros. Revisar cada aporte en su alcance autorizado; una respuesta a derechos no es por defecto una exportación completa.

Para limitar temporalmente un aporte: `block --i01.resource=UUID_NOTA`; verificar GET/listado y reintento con el autor (no disponible), conservando otros aportes. `unblock` exige nueva autorización específica; no es automático al vencer un plazo. Se ejecutaron además los comandos CLI `locate`, `rectify` y `evidence` con datos ficticios: cambio y relectura de una identidad histórica, reposición del valor original y extracción con fecha ISO/resultado legibles. El resultado queda en `technicalCli` de las evidencias. La corrección cotidiana se hace por la UI; no requiere soporte. Para rectificar excepcionalmente identidad, `rectify --i01.file=MANIFIESTO` recibe `RectificacionIdentidad`: `estudianteId`, `identidad` con `nombre/documentoTipo/documento`, y las `sustituciones` descritas a continuación. Debe cubrir todas las referencias actuales/históricas, conservar UUID, matrícula, código, grupos y papeles; los cuerpos conservan a la persona con el nombre corregido. Documento/tipo van juntos o ausentes; DNI exige ocho cifras, sin rellenarlas, y un documento de otra identidad se rechaza sin fusionar. Revisar también texto libre y rectificaciones. La operación sustituye las copias aprobadas y la identidad cifrada en una transacción, sin habilitar mantenimiento escolar ni edición libre de antecedentes.

Disposición excepcional: obtener la localización completa (incluye sujetos históricos), bloquear los aportes afectados y preparar un JSON protegido conforme a `Operaciones.Disposicion`/`Sustitucion`: UUID del estudiante; por **cada** incidencia relacionada, UUID, versión esperada, `Cuerpo` íntegro sustituto aprobado y mapa de todas las rectificaciones por versión con sus diferencias sustitutas. Retirar al destinatario del cuerpo/entrada, conservar las participaciones de terceros y revisar descripción, acción, compromiso, resultado, comunicante y todas las diferencias para eliminar referencias alcanzadas sin perder información legítima ajena. No basta quitar una participación estructurada. El test de disposición proporciona un ejemplo ficticio tipado y comprueba rechazo del manifiesto incompleto.

Ejecutar `dispose --i01.file=MANIFIESTO` con autorización; operación transaccional exige cobertura exacta y versiones actuales, conserva terceros, reemplaza copias cifradas, retira recibos alcanzados, vínculos e identidad y deja evidencia sin relatos. Verificar mediante SQL/relectura y nueva localización, incluyendo diferencias y búsquedas; custodiar constancia de cantidades/resultado. Retirar de forma controlada extracciones/manifiestos temporales una vez cumplida su finalidad; la herramienta no elimina constancias externas. Comunicar rectificaciones a destinatarios previos por canales institucionales cuando proceda. No usar este procedimiento sin revisión humana del texto sustituto: no identifica automáticamente menciones personales libres ni decide una supresión de un tercero.

### Copia, restauración y actualización

Detener el servidor antes de actualizar el JAR o de realizar el corte estable del ensayo. Definir `PGHOST/PGPORT/PGUSER/PGPASSWORD` de propietario, `DATA_KEY`, `BACKUP_KEY` aleatoria **distinta**, `APP_ORIGIN`, `COOKIE_SECURE` y contraseña TLS. Conservar BACKUP_KEY por separado y bajo control institucional. Crear una carpeta protegida de salida. El script usa `pg_dump`/`pg_restore`, temporales con ACL y AES-GCM; incluye base, configuración y ejecutables. La copia histórica no se edita selectivamente.

```powershell
$env:BACKUP_KEY = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$ensayo = Get-Date -Format yyyyMMddHHmmss
backend/ops/test-backup.ps1 -PgBin RUTA_BIN_POSTGRES -BackupPrefix ".local/ensayo-$ensayo" -RestoreDatabase "i01_restore_$ensayo" -ApplicationJar backend/target/SistemaIncidenciasEscolaresBackend-0.1.0-SNAPSHOT.jar -FrontendDist frontend/dist -TlsKeystore .local/i01.p12
backend/ops/backup.ps1 -Action Status -BackupPath ".local/ensayo-$ensayo-actual.i01b"
```

El ensayo crea copia actual/anterior, rechaza una alterada y una clave distinta, restaura sin sobrescribir y concilia tablas. `*.Backup.status.json`, `*.Verify.status.json`, `*.Restore.status.json` permiten conocer fecha, resultado, duración y fallo; `*-results.json` contiene conciliación. No confundir Verify con restauración. Los artefactos completos recuperados y configuración sensible quedan con ACL restringida en `*.recovered/`; no son una descarga pública. Un fallo no declara éxito. La base restaurada queda sin acceso público y con sesiones eliminadas.

Antes de abrir servicio: revisar designaciones, bloqueos y decisiones válidas posteriores a la copia; aplicar rectificación/disposición pendiente con las herramientas anteriores; comprobar integridad y permisos. Solo entonces aplicar `psql -d BASE_RESTAURADA -f backend/ops/runtime-grants.sql`, configurar el servidor con la clave y ejecutable recuperados, certificado y credencial del rol operativo vigente. **No** basta restaurar una copia antigua y abrirla. El ensayo actual verifica restricciones existentes en la copia; la secuencia completa de decisiones posteriores con copia antigua pertenece a aceptación acumulativa y no se da por ejecutada aquí.

Programación, ubicación de copia independiente del equipo, pérdida máxima de 24 h, retención fundada, jornada/responsables y ensayo anual deben quedar establecidos y demostrados antes de datos reales. El script no es un programador ni un panel de respaldo. Conservar al menos una copia anterior utilizable y evitar que una sola pérdida destruya todas. Para actualizar: copia previa, detener versión, compilar frontend/JAR, migrar como propietario, aplicar grants pertinentes, arrancar usuario restringido y ejecutar regresiones. Si falla, recuperar versión y base previas aisladamente; no usar `flyway clean` en producción.

Durante una caída: la UI conserva texto en la pestaña y ofrece reintento seguro, sin prometer offline. Continuar protección por canales institucionales y registrar luego fecha real. Consultar respuesta autenticada y logs con fecha/clase de fallo (sin argumentos/relatos), y estado de copia/restauración. Ante incidente, preservar evidencia y avisar sin demora al responsable institucional; este evalúa comunicaciones y plazos de la ERS por sus canales. No hay comunicaciones externas automáticas ni se envió ninguna en los ensayos.

## 6. Problemas, decisiones revisables y límites

- No hay funcionalidad de I02 o posterior: riesgo/gravedad/externos, recepción institucional, importación, contexto, atención, duplicados, cálculo de reiteración, informes ni administración escolar. Es alcance diferido, no éxito simulado. La estructura guarda una atribución cotidiana utilizable por cálculo posterior; no demuestra que se cuente todavía.
- Los controles entregados y ensayos iniciales no acreditan preparación para datos reales. Permanecen los ensayos humanos, accesibilidad asistida, compatibilidad completa, escala/latencia/concurrencia, despliegue/costos y continuidad enumerados en §4. El bloqueo no se activa indiscriminadamente ante cualquier solicitud; requiere decisión institucional concreta.
- Búsqueda de identidades cifradas en memoria y listado de 20 aportes completos son soluciones de I01; deben evaluarse con el volumen global antes de aceptarlo. Las correcciones conservan el contenido necesario, mientras una disposición autorizada puede reemplazarlo. No hay editor libre de identidad ni fusión de personas.
- La disposición exige una revisión técnica/institucional del manifiesto para texto libre, revisiones y terceros. Se ha probado la ejecución delimitada, no todos los supuestos jurídicos, copias externas ni decisiones posteriores a un respaldo antiguo. La constancia/entrega a titulares y la autorización de soporte permanecen fuera de la aplicación.
- La clave de datos no tiene rotación automatizada en I01. Su pérdida impide interpretar los cuerpos; una clave de respaldo perdida impide recuperar el paquete. Custodia separada y recuperación por otro responsable son condiciones de operación, no capacidades acreditadas por las credenciales locales de ensayo.
- El entorno usa medios locales de prueba y certificado autofirmado; no se demuestra confianza de certificado real, cifrado de disco del equipo, copia fuera del host, disponibilidad o pérdida efectiva de cambios en operación. La retención mínima de bitácora se protege por permisos/trigger, pero un superusuario puede deshabilitarlos y necesita controles de custodia externos.
- No se completan RF/RNF/CU enteros por pasar las pruebas de este incremento. Los valores pendientes están visibles y las evidencias distinguen controles implementados, ensayos ejecutados y aceptación no realizada.
