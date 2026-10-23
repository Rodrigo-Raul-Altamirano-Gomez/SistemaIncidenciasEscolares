# Plan de implementación por incrementos verticales

**Producto:** gestión de incidencias escolares.  
**Versión del plan:** 1.0 · **Fecha:** 23 de octubre de 2026.  
**Fuente normativa del producto:** [ERS de incidencias escolares, versión 1.2](ERS_Incidencias_Escolares.md), con fecha y corte normativo del 20 de octubre de 2026.  
**Huella SHA-256 del archivo analizado:** `DD52FAC9091B3B7B42B447D884E13D712BF5497186BAF481A3DB23E7CFB95366`.  
**Punto de partida:** implementación nueva, sin compromisos con el dominio, código, pruebas o numeración de incrementos anteriores del repositorio.  
**Destino:** documento de referencia para implementación y aceptación; no acredita que las capacidades estén implementadas.

## 1. Alcance, interpretación y uso

La ERS prevalece sobre este plan. Se analizaron sus seis capítulos: descripción y límites, permisos, los 41 RF, los 25 RNF, las 28 RN y los 17 CU, incluidos alternativas y criterios de aceptación. Las relaciones de la sección 6.2 de la ERS se contrastaron con el contenido de los requisitos; no sustituyeron su lectura. Las referencias legales se utilizan con el alcance que la propia ERS les atribuye, sin introducir obligaciones o políticas adicionales.

Cada incremento se entrega como una versión ejecutable de React con TypeScript, Spring Boot y PostgreSQL, con las partes de dominio, aplicación, puertos, adaptadores y pruebas que necesite su flujo. Los procedimientos de operación también son entregables cuando la ERS admite herramientas convencionales. No se construye una capa completa antes de probar una capacidad.

Un agente que reciba un incremento debe leer su ficha, los requisitos citados completos en la ERS, el capítulo 2, los controles comunes de la sección 3 y los escenarios globales relacionados. Debe conservar las capacidades aceptadas y actualizar documentación y evidencias. No puede interpretar un alcance diferido como autorización para omitir una regla necesaria en la funcionalidad que sí entrega.

**Convenciones de cobertura:** **P** = implementación parcial, indicando qué parte existe; **C** = se completa el alcance exigido; **V** = verificación o regresión concreta sobre comportamiento ya disponible. Una capacidad precargada con datos ficticios no satisface el requisito de administrarla o importarla. Las marcas describen el plan de entrega, no resultados de pruebas ejecutadas. Los CU se completan cuando funcionan también sus alternativas y criterios, no solo su camino principal.

## 2. Análisis funcional

### 2.1 Capacidades, valor y relaciones

| Capacidad | Resultado para sus actores | Alcance y dependencias funcionales | Entrega principal |
|---|---|---|---|
| Registro cotidiano y consulta propia | Docente, auxiliar y otro directivo documentan hechos y acciones inmediatas sin aprobación | Identidad mínima, categoría, papel individual, fechas, autoría y reserva. Una nota no exige caso ni validación de TOE. RF-04, RF-12–RF-15, RF-17; CU-02. | I01, ampliación I02 e I05 |
| Comunicación de riesgo y recepción segura | Un responsable elegible conoce una situación desde su primera comunicación | Personas parcialmente identificadas, solicitud manual, gravedad y conflicto antes del aviso. No necesita umbral, confirmación, acta ni apertura. RF-05, RF-13, RF-19, RF-21, RF-28; CU-03/CU-09. | I02 |
| Atención y continuidad | TOE/comité documenta intervención, compromisos y cierre con un guardado cuando basta | Reutiliza el asunto de origen; acciones previstas solo si son reales; el cierre no resuelve pendientes por sí solo. RF-22, RF-24–RF-26, RF-30; CU-07. | I03 |
| Historia compartida y calidad del hecho | Los operadores reconstruyen qué ocurrió sin duplicar hechos o casos por estudiante | Relación explícita de reportes, papeles y fuentes; agrupación de hechos; atención pendiente única por hecho. RF-16, RF-18, RF-23; CU-05/CU-06. | I04; consumidores I06 e I11 |
| Reiteración útil | Atención advierte patrones sin investigar cada nota cotidiana | Estudiante/categoría/fecha real/ventana; hechos únicos y atribuciones elegibles; parámetros expresos y avisos sin tareas repetidas. RF-03, RF-20–RF-21; CU-04. | I05 |
| Sustento documental, referencias y derivación | Un autorizado prepara informe y documenta comunicación externa real | Consolida el caso, minimiza contexto, separa preparación/ejecución y cierre local/oficial. RF-29–RF-31, RF-33; CU-10/CU-12 y parte de CU-11. | I06 |
| Intervención formal del comité | El órgano interviene en el mismo caso con autoría correcta y acta externa | Membresía vigente no depende de convocatoria para acceder. TOE convoca; director o sustituto documenta actos colectivos. RF-27, RF-32; CU-08/CU-09/CU-11. | Permisos I02; proceso I07 |
| Nómina e identidad entre años | AIP incorpora/actualiza estudiantes sin romper historia | Excel revisado, excepciones localizadas, confirmación única, conciliación segura, grupo desconocido permitido. RF-06, RF-08–RF-11; CU-01. | I08 |
| Contacto y contexto pertinente | Atención consulta información necesaria, con procedencia y reserva | Extiende la misma importación; alcance institucional admitido; AIP no consulta antecedentes anteriores. RF-06–RF-07, RF-40; CU-01/CU-15. | I09 |
| Autonomía de configuración | Operadores autorizados mantienen cuentas, designaciones, catálogo e institución | No concede lectura de expedientes; no promueve, cierra o elimina al crear año. RF-01–RF-03; CU-14. | Mínimos anteriores; gestión completa I10 |
| Análisis institucional | TOE y comité distinguen hechos, personas, atribuciones y carga real | Usa datos de hechos y atención; no necesita cada dato demográfico para mostrar cantidades. RF-34–RF-35; CU-13. | I11 |
| Comparaciones y salidas | Se comparan poblaciones válidas y se exporta según finalidad | Denominadores coherentes, cobertura, formatos y reserva; cifras internas pequeñas exactas. RF-36–RF-37; CU-13. | I12 |
| Derechos y conservación | La institución aplica una decisión concreta sin perjudicar terceros | Reutiliza búsqueda, corrección y extracción; bloqueo/disposición delimitados y verificados en derivados. RF-38–RF-40; CU-15. | Controles desde I01; recorrido completo I13 |
| Recuperación y transferencia | Un técnico autorizado recupera el servicio y entrega información interpretable | Incluye relaciones, archivos y restricciones actuales; operación protegida y autónoma. RF-41; CU-16. | Respaldo desde I01; entrega integral I14 |

Las capacidades principales son registrar, recibir, atender, reconstruir historia y sustentar la intervención. Nómina, configuración y operación las habilitan y mantienen; no justifican construir primero todos los módulos administrativos. Estadísticas, protección de datos y recuperación tienen resultados propios verificables y además imponen controles sobre las capacidades anteriores.

### 2.2 Distinciones que condicionan todos los incrementos

- **Hecho y reporte:** un suceso puede tener varios aportes. No se borran ni mezclan relatos al identificar duplicidad; se conserva cada fuente y una sola ocurrencia.
- **Persona y participación:** el papel pertenece a una persona en un hecho. Estudiantes, personal, familiares y externos no heredan papeles entre sí. Nombre o parentesco inciertos no bastan para unificar identidades.
- **Atribución cotidiana, presunta responsabilidad, corroboración y desconocimiento:** son condiciones diferentes. Una nota cotidiana no grave cuenta desde su guardado; violencia no recibe por defecto esa atribución. Afectados/testigos no suman responsabilidades. Contexto importado no es un hecho nuevo.
- **Atención y cierre:** el caso tiene solo estados pendiente/cerrado. TOE/comité identifica quién atiende, no otro estado. Una derivación preparada no está ejecutada; un cierre local no certifica cierre oficial ni cese de protección.
- **Rol y responsabilidad:** comité tiene capacidades institucionales equivalentes a TOE durante su vigencia, sin permiso adicional por caso; recepción habitual, convocatoria, configuración y registro colectivo mantienen responsables específicos.
- **Permiso y exclusión:** los roles vigentes se acumulan; conflicto y bloqueo jurídico prevalecen sobre autoría y administración. Relacionar datos nunca debe crear un camino de acceso indirecto.
- **Identidad e información anual:** cambiar año/grupo no crea otra persona ni reescribe grupos históricos conocidos. No informado, negación explícita y cero no son equivalentes.
- **Información actual y salida emitida:** las correcciones explican diferencias entre cortes. Una emisión conservada mantiene reserva y queda alcanzada por una disposición legítima; su fecha de corte no autoriza retener información que deba retirarse.

### 2.3 Decisiones pendientes y límites de producto

| Pendiente de ERS 1.10 | Comportamiento mientras falte | Punto de incorporación y condición real |
|---|---|---|
| Catálogo definitivo/gravedad | «Por clasificar» y solicitud manual inmediata; sin sanciones inventadas | I01/I02; edición completa I10. Dirección con TOE define valores. |
| Ventana común y umbral por categoría | Reiteración automática inactiva para categorías sin valores expresos, ausencia visible; gravedad/manual siguen operando | I05; mantenimiento I10. Valores de pruebas no se publican como aprobados. |
| Contexto y fundamento admitidos | Se omiten conceptos nuevos no admitidos, con aviso; salud/familia no se copian por defecto | I09; debe existir alcance institucional antes de tratar esos datos reales. |
| Grupo ausente | Identidad/año se incorporan con grupo no informado; sin tarea para TOE ni inferencia del archivo | I01 lo representa; I08 permite completarlo válidamente. |
| Conservación y suspensiones | Sin eliminación por año/cierre ni retención perpetua predeterminada; disposición solo por decisión expresa | Procedimiento limitado desde I01, completo I13/I14. Criterios fundados documentados antes de datos reales. |
| Designaciones nominales | Cuentas ficticias para desarrollo; ninguna precarga acredita designación real | Vigencias desde I01/I02; administración I10. El colegio designa antes de habilitar cuentas reales. |

Se mantienen todas las exclusiones de ERS 1.7: sin sanciones automáticas, motor de protocolos, integración SíseVe/SIAGIE, mensajería externa, cuentas de estudiantes/familias, generación de actas, gestión clínica, matrícula integral, BI libre o multiinstitución simultánea. Tampoco se crean plataformas de derechos/consentimientos, fusión general de identidades/casos, editor de conservación ni panel avanzado de respaldo. Una columna nueva o una norma citada no amplía por sí sola el producto.

## 3. Estrategia de desarrollo y controles comunes

### 3.1 Arquitectura fundamental y diseño aplazado

| Mantener desde el inicio | Restricciones conocidas que deben permitir evolucionar | Resolver cuando exista la necesidad |
|---|---|---|
| Monolito modular, Spring Boot, React/TypeScript y PostgreSQL; una instalación/ámbito para un colegio | Identidad independiente del año; hecho con varios reportes/personas; fuentes y papeles discrepantes | Límites internos definitivos de módulos, agregados y tablas |
| Dominio sin dependencia de React, HTTP, Spring o persistencia; aplicación coordina la tarea; adaptadores realizan I/O | Atención compartida y única pendiente por hecho; autoría histórica y fechas efectivas distintas | Contratos concretos, estructuras de clases, endpoints y consultas |
| Autorización efectiva en operaciones del servidor; interfaz coherente con el permiso | Exclusión que se extiende a relaciones/derivados; membresía vigente y sustituciones | Mecanismo de sesión, control de concurrencia y reintento que cumpla los ensayos |
| Cambios persistentes y migraciones incrementales verificables; límites transaccionales según la operación | Anexos e informes reservados y recuperables; disposición legítima también sobre copias operativas | Bibliotecas de Excel/PDF, disposición física de archivos, índices o procesos periódicos |
| Evidencia, privacidad y pruebas incorporadas al flujo, no añadidas al final | Conteos coherentes y cortes; lectura actualizada de permisos aun con sesiones/enlaces abiertos | Optimización de estadísticas, generación de documentos y evaluación al avanzar el día |

Los límites modulares se descubren alrededor de responsabilidades de negocio. No se crea un módulo por tabla, RF o CU, ni una red de servicios. Un puerto se introduce para una interacción necesaria, sin interfaces vacías o duplicadas por cada clase. Una colaboración dentro del monolito no necesita mensajería distribuida. La elección de versiones, bibliotecas y alojamiento se hará al implementar, revisando soporte, vulnerabilidades, licencias y costo entonces; este plan no los fija.

La interfaz inicia una intención de usuario; la aplicación comprueba identidad, permiso y estado actual, ejecuta las reglas necesarias y coordina persistencia y evidencia. El resultado vuelve a la interfaz como confirmado o fallido. Esa descripción fija responsabilidades, no un diseño anticipado de contratos o almacenamiento. Los límites transaccionales concretos se eligen al conocer la operación: una confirmación única de lote no obliga a una transacción gigante, pero nunca permite filas incompletas o resultados de éxito falsos.

### 3.2 Controles de aceptación aplicables desde su primera superficie

Los siguientes controles, **T01–T08**, forman parte de cada ficha. Se aplican a los datos y funciones que existan en ese incremento y se amplían con cada nueva superficie. No son un incremento técnico separado.

| Control | Obligación y comprobación mínima |
|---|---|
| T01. Identidad y autorización | Autenticación real, mínimo acceso en servidor y pruebas con todos los roles pertinentes, usuario ajeno y referencias manipuladas. Cierre de sesión, limitación de intentos, recuperación controlada y expiración tras 20 minutos con aviso y conservación segura del texto de la tarea. Revocación efectiva en la siguiente operación protegida, probada con dos sesiones. Membresía no equivale a configuración ni a autoría de actos colectivos. |
| T02. Reserva transversal | No revelar contenido por búsqueda, error, historial, totales, avisos, diagnósticos o enlaces. Desde I02: exclusión preventiva del denunciado institucional antes del aviso, explícita con causa cuando corresponda, retirada motivada no propia y propagación por relaciones. Desde la primera operación: respetar bloqueos jurídicos delimitados aplicables al recurso; cada superficie posterior los integra. Cachés y descargas revalidan acceso. No se promete retirar copias legítimas ya descargadas. |
| T03. Evidencia recuperable | Inicio/cierre de sesión, lecturas/listados y mutaciones de datos personales, importaciones/exportaciones/eliminaciones dejan usuario, fecha/hora, resultado y referencia; criterios de listado sin copiar relatos. Evidencia legible inmediatamente por operador específicamente autorizado, protegida de edición ordinaria y con retención mínima de dos años. Un fallo no declara exitosa una modificación sin evidencia recuperable. No otorga acceso general a datos de atención. |
| T04. Integridad y fechas | Probar doble envío, pérdida de respuesta, reintento y dos ediciones concurrentes: sin duplicidad ni sobrescritura silenciosa. Referencias válidas y participación computable única de persona identificada por hecho. Zona de Perú, año explícito, fechas efectivas no futuras y coherencia apertura/cierre; aproximación y ausencia visibles. Actualizaciones relacionadas y avisos quedan coherentes o se informa el fallo real. |
| T05. Privacidad y tratamiento de entradas | Datos mínimos, opcionales claros, transmisión y almacenamiento protegidos, secretos fuera de código y errores. Textos y archivos no ejecutables; temporales y generaciones fallidas retirados. Nada de telemetría de contenido, publicidad o entrenamiento con datos escolares. Toda nueva persistencia se incorpora al procedimiento autorizado de localización, rectificación, bloqueo y disposición, incluyendo derivados disponibles, sin eliminar datos legítimos de terceros. |
| T06. Interacción y accesibilidad | Español claro, errores corregibles que preservan entrada, aviso de salida con cambios, éxito solo confirmado. Un guardado para la tarea conjunta prevista; sin confirmaciones o motivos rutinarios redundantes. Teclado, foco, etiquetas, errores/estados textuales, contraste y ampliación al 200 %. Registro/consulta/atención desde 320 px y escritorio de 1 280 px; importación masiva puede priorizar escritorio con aviso. |
| T07. Operación y evolución | Versión ejecutable, migración/reversión o recuperación comprobadas según el cambio, guía de lo entregado, dependencias/licencias/costos inventariados. Desde I01: copia protegida de los datos/configuración existentes, comprobación de integridad, restauración básica ficticia y estado/fallo consultable por responsable; cada nuevo archivo o relación amplía el procedimiento. No se deja todo el respaldo para I14. Ninguna restauración abre acceso antes de aplicar restricciones y decisiones vigentes. |
| T08. Evidencia de extremo a extremo | UI real → aplicación/reglas → adaptadores → PostgreSQL/archivos → nueva consulta y efectos autorizados. Datos ficticios, pruebas positivas/negativas, integración y regresiones pertinentes; se registran entorno, versión, resultados y defectos. Las pruebas unitarias complementan, pero no aceptan el incremento por sí solas. |

La seguridad verificable de RNF-09 se revisa con cada entrega y constituye además una puerta antes de datos reales. Si un mecanismo de bloqueo o disposición se resuelve con soporte técnico autorizado, se entrega el procedimiento ejecutable y verificable para los datos ya existentes; no basta mencionarlo como futuro. I13 completa el recorrido institucional sobre el producto entero y reduce las lagunas de integración, sin inventar un módulo de solicitudes.

### 3.3 Datos ficticios, entrega y reordenación

I01–I07 pueden utilizar cuentas con vigencias, estudiantes, años/grupos y categorías ficticias precargadas. La carga solo prepara el ensayo; no cumple RF-01–RF-03 ni RF-08–RF-11. I05 incorpora la edición de parámetros necesaria para probar reiteración; I08 incorpora la nómina operable y I10 completa la administración. Los valores ausentes se muestran como tales y las opciones aún no implementadas no aparentan éxito.

Los incrementos previos a la aceptación integral se ensayan con datos ficticios. No se autoriza una explotación real por haber aceptado una ficha. Una implantación real parcial exigiría un alcance expresamente acordado, sus obligaciones transversales completas y una comprobación específica de aptitud; este plan no la presupone. La puerta normal de uso real se define en la sección 7.

Se proponen **catorce incrementos**: cinco para el ciclo central y calidad del hecho, dos para sustento/actuación institucional, dos para nómina/contexto, uno para autonomía administrativa, dos para análisis/salidas y dos para disposición/recuperación completas. La división responde al tamaño y a resultados observables, no al número de entidades o CU.

Se divide una ficha si contiene más de un resultado independiente que pueda aceptarse sin fingir el otro; cada parte conserva UI, reglas, persistencia y controles. Se agrupan partes cuando separarlas obligaría a transcribir, usar guardados administrativos ficticios o entregar una operación insegura. Se reordena por valor o disponibilidad de usuarios/datos solo respetando dependencias de la sección 6 y actualizando matrices. No se separa «hacer backend» de «hacer frontend». Las refactorizaciones se incluyen en la capacidad que las justifique y conservan regresiones existentes. No se asignan duraciones sin conocer equipo, restricciones operativas y resultados de las primeras entregas.

## 4. Plan de incrementos

### I01. Registrar y recuperar una nota cotidiana propia

**1. Identificador y nombre:** I01 — Registro cotidiano autónomo.

**2. Objetivo funcional:** que un docente, auxiliar u otro directivo ordinario deje una constancia útil, persistida y reservada, sin pasar por TOE.

**3. Actores beneficiados:** registradores ordinarios; operador autorizado que verifica evidencia y recuperación de esta primera capacidad.

**4. Funcionalidades:** inicio/cierre y recuperación controlada de acceso; búsqueda mínima de estudiantes actuales/anteriores sin datos reservados; registro no grave con uno o varios estudiantes y papel propio; acción/compromiso inmediato opcional en el mismo guardado; consulta propia con filtros por estudiante, fecha, categoría/año; corrección propia antes de atención, con diferencias automáticas. Grupo desconocido y fecha pasada/aproximada se representan sin inferirlos. La atribución cotidiana es visible y corregible y queda disponible para el cálculo posterior.

**5. Flujo E2E:** autenticarse → buscar/distinguir estudiante → elegir categoría cotidiana y describir → añadir otros papeles/acción/compromiso si procede → guardar una vez → volver a consultar desde otra sesión autorizada → corregir escritura → comprobar diferencia y autoría. No se genera caso ni tarea para TOE.

**6. RF y CU:** P RF-01 (acceso sobre cuentas de prueba, sin administración), RF-02 (año/grupo), RF-04, RF-06 (identidad/vínculo mínimos), RF-12–RF-15, RF-17 y RF-38–RF-41 sobre lo existente. P CU-02 (reiteración se añade en I05), CU-14–CU-17. RF-14 se completa aquí para su flujo cotidiano; los nuevos consumidores lo reutilizan.

**7. RN/RNF:** RN-02, RN-04–RN-05, RN-07, RN-13, RN-17, RN-24–RN-28; RNF-01–RNF-15, RNF-17–RNF-18 y RNF-20–RNF-25 en las superficies entregadas; RNF-16 búsqueda/guardado y RNF-19 consulta coherente. Aplican T01–T08; no hay dispensa por usar datos ficticios.

**8. Dependencias:** ninguna capacidad previa. Se crea solo el arranque necesario para este recorrido.

**9. Condiciones/datos:** cuentas individuales ficticias de los tres roles ordinarios y una ajena; estudiantes homónimos, con/sin grupo/documento, matrícula pasada y nombre con tildes; catálogo cotidiano y «por clasificar» de prueba. Sin valores de reiteración aprobados, se informa que el cálculo está inactivo.

**10. Excluido/diferido:** comunicación grave y personas externas I02; atención I03; repetición automática I05; importación I08/I09; mantenimiento de cuentas/catálogo I10. Una precarga no se presenta como importador ni panel administrativo.

**11. Aceptación verificable:** una decisión de guardado conserva hecho, papeles y constancia inmediata; afectados/testigos no heredan atribución. Búsqueda tolera espacios/mayúsculas/tildes sin alterar el nombre; DNI completo/contacto no aparecen en búsqueda ordinaria. El autor solo ve lo suyo. No se exige documento/compromiso/adjunto. Las fechas reales futuras se rechazan. Sesión expira a los 20 minutos con aviso, sin pérdida silenciosa de texto. Registrar y localizar se ensayan hacia 90 s/30 s, con guion de CU-17.

**12. Pruebas:** reglas de papel/fechas; integración real de guardado conjunto y lectura; E2E con cada rol; acceso directo a aporte ajeno; fuerza de intentos fallidos; cierre y reutilización de sesión; fallo de persistencia/evidencia, pérdida de respuesta, doble envío y conflicto de edición; texto no confiable, teclado/320 px/200 %; copia y restauración básica ficticia. Consulta y rectificación/disposición autorizadas verifican los datos que ya existen.

**13. Arquitectura indispensable:** flujo de aplicación y reglas independientes de HTTP/persistencia; adaptadores solo para interacciones presentes. Persistir identidad estable, fuente de autoría y datos históricos conocidos sin inventar el modelo de atención. No fijar «una incidencia = un alumno» por conveniencia de esta primera pantalla.

### I02. Comunicar un riesgo y recibirlo sin exponer al implicado

**1. Identificador y nombre:** I02 — Recepción institucional segura.

**2. Objetivo funcional:** permitir una comunicación urgente incompleta y hacerla visible al receptor elegible desde el primer registro.

**3. Actores beneficiados:** todos los registradores; TOE y miembros vigentes del comité no excluidos; administrador que conoce únicamente una necesidad de designación.

**4. Funcionalidades:** categorías graves/muy graves/violencia y petición manual por riesgo; participantes estudiantes, personal, familiares/externos con identificación mínima y papel individual; referencia provisional de estudiante sin crear identidad ficticia. Personal se relaciona inequívocamente con su cuenta cuando se conoce. Aviso persistente y bandeja de situaciones nuevas con motivos/gravedad/antigüedad/responsable y filtros; lectura no resuelve. Consulta institucional de hechos con filtros de RF-16 y trayectoria de hechos disponibles. Exclusión preventiva y explícita, revocación motivada no propia, continuidad por elegibles y tratamiento de ausencia total de receptor. Puede registrarse disposición breve sin caso; descartar gravedad sin actuación exige fundamento.

**5. Flujo E2E:** comunicar violencia con familiar desconocido o personal denunciado → aplicar restricción antes de notificar → receptor elegible actualiza bandeja → abre hecho reservado → lee o documenta disposición pertinente. Ensayo alternativo: identificar después la cuenta denunciada y bloquear su siguiente lectura, manteniendo otros asuntos permitidos.

**6. RF y CU:** completa RF-04, RF-05, RF-12–RF-13 y RF-19 para registro/búsqueda; P RF-15–RF-16, RF-17, RF-21, RF-28 y RF-38. P CU-03/CU-09; amplía CU-02 y CU-17. La atención formal de CU-03 se completa en I03; informes/actas/estadísticas de CU-09 aún no existen.

**7. RN/RNF:** RN-02–RN-08, RN-11, RN-19, RN-24, RN-26–RN-28; RNF-01–RNF-10, RNF-12–RNF-16, RNF-19–RNF-20 y RNF-25; además T01–T08 para continuidad/operación. No se usa un rol «superior» para eludir reserva.

**8. Dependencias:** I01; membresías, años y categorías de prueba precargados. La administración completa de esas vigencias no es prerrequisito.

**9. Condiciones/datos:** TOE/director/docente y auxiliar de comité vigentes, otros registradores y administrador sin rol funcional; denuncias contra cada miembro por separado, trabajador testigo, familiar parcial, estudiante aún no identificado y todos los receptores impedidos. Dos sesiones y enlaces abiertos antes de la exclusión.

**10. Excluido/diferido:** apertura formal I03; agrupación/duplicidad I04; reiteración I05; convocatoria y registro colectivo I07. No se concede registro colectivo al miembro solo por tener lectura institucional.

**11. Aceptación verificable:** primera gravedad/manual avisa sin confirmación/umbral/caso; disponible en siguiente consulta de bandeja en ≤5 s. Roles desconocidos no bloquean protección. Familiar presunto no atribuye falta al estudiante afectado. Testigo no queda excluido automáticamente. TOE implicado no recibe detalle y el resto asume sin su permiso. Exclusión prevalece aun siendo autor/administrador; retirada propia denegada. Sin elegibles se conserva protegido y solo se comunica necesidad de designación. Un miembro vigente accede institucionalmente sin convocatoria, uno no vigente no.

**12. Pruebas:** E2E de CU-03/CU-09 en el alcance disponible; avisos y exclusión concurrentes; reconocimiento tardío de cuenta; reintentos sin avisos duplicados; lector no autorizado, identificación manipulada, navegación por otro estudiante, historial y errores sin filtraciones; roles acumulados y fin de vigencia con sesiones abiertas; disposición grave sin fundamento rechazada y lectura que deja pendiente.

**13. Arquitectura indispensable:** decidir elegibilidad/restricción antes de exponer avisos; registrar hecho y disponibilidad coherente de su señal. No exige tiempo real permanente. Mantener el vínculo personal-cuenta sin crear recursos humanos, padrón externo o cuentas familiares. El alcance de exclusión debe poder ampliarse con las relaciones de I04.

### I03. Documentar atención, compromisos y cierre con continuidad real

**1. Identificador y nombre:** I03 — Intervención directa desde el asunto.

**2. Objetivo funcional:** que TOE o un miembro habilitado documente una intervención sencilla completa sin guardados administrativos previos.

**3. Actores beneficiados:** TOE y comité elegible en atención ordinaria; registrador que conserva acceso únicamente a su aporte.

**4. Funcionalidades:** atención desde incidencia/señal, responsable y fecha propuestos; pendiente/cerrada; conversación realizada, resultado, compromiso y cierre en un solo guardado. Programación opcional, completar la misma actuación o cancelarla con causa; compromisos con/sin plazo y seguimiento real. Aclaración institucional de participaciones con fuente/fecha, sin modificar el original ni confirmar todas las notas. Corrección posterior del autor como aclaración, corrección institucional sustancial con fundamento y anulación por error. Bandeja distingue situaciones nuevas, atenciones pendientes y seguimiento real, con motivo, gravedad declarada, antigüedad, responsable y vencimiento conocido, ordenables/filtrables. Continúa protección tras cierre sin reabrirlo; corrección motivada de cierre equivocado.

**5. Flujo E2E:** desde aviso o hecho → registrar conversación realizada y resultado → compromiso opcional y cierre → un guardado → aviso gestionado e historia cerrada. Alternativa: programar acción/seguimiento → realizar o cancelar → actualizar la misma constancia; un pendiente real continúa visible aun con caso cerrado.

**6. RF y CU:** completa RF-17, RF-24–RF-26; P RF-15–RF-16, RF-21–RF-22 y RF-30 (sin regla de derivación todavía). C CU-03; P CU-07 (informe I06 y facultades colectivas I07), CU-06/CU-17; amplía CU-09 en atención ordinaria y sustitución del receptor.

**7. RN/RNF:** RN-02, RN-04–RN-08, RN-11–RN-13, RN-15, RN-26–RN-28; RNF-01–RNF-13, RNF-14–RNF-20, RNF-22 y RNF-25 en el alcance; T01–T08. La atribución descartada recalculará también las señales de I05 cuando existan.

**8. Dependencias:** I01/I02. No requiere importación, acta, comité formal ni numeración de oficio.

**9. Condiciones/datos:** hecho cotidiano y grave; atención resuelta al registrarla; acción prevista y realizada tardíamente; compromiso sin fecha, con fecha y con seguimiento explícito; cierre con pendiente, atribución desconocida y denuncia no corroborada; dos operadores sobre el mismo contenido.

**10. Excluido/diferido:** agrupación de varios hechos I04; reiteración I05; PDF/derivación I06; actos formales de comité I07. No se fuerza abrir un caso para las acciones cotidianas de I01.

**11. Aceptación verificable:** conversación+resultado+compromiso+cierre tienen una confirmación final, sin motivos repetidos. Un hecho no recibe dos atenciones locales pendientes por carrera concurrente. Abrir/documentar/gestionar actualiza avisos automáticamente. Cierre no confirma roles ni compromisos; sin plazo no hay vencimiento inventado. Una prevista solo cuenta realizada al ejecutarse; cancelar exige causa. Anular requiere error y causa, no falta de corroboración. Seguimiento posterior conserva fecha de cierre. El autor ordinario no corrige libremente tras atención ni lee sus actuaciones. Ensayo TOE de CU-17: ≤3 min.

**12. Pruebas:** flujo directo y programado, cancelación, compromisos y cierre con pendientes; aclaración no propagada a otros hechos/personas; anulación vs. denuncia no corroborada; conflicto concurrente al abrir/editar, reintento del guardado compuesto, fallo de evidencia; reserva de actuaciones frente a autor original; teclado/móvil y ausencia de pasos redundantes.

**13. Arquitectura indispensable:** coordinar caso/actuación/compromiso/cierre y avisos como una intención consistente. No crear estados «recibido», «aprobado» o «en comité». Relacionar la acción inmediata original sin copiarla. Reutilizar la validación de papeles al aclararlos; no implantar un proceso de investigación obligatorio.

### I04. Reconstruir un hecho y una atención compartida sin duplicaciones

**1. Identificador y nombre:** I04 — Reportes vinculados, agrupación e historia compartida.

**2. Objetivo funcional:** conservar todos los aportes y participantes pertinentes y atender conjuntamente hechos relacionados sin doble conteo ni nuevos permisos.

**3. Actores beneficiados:** TOE y comité elegible; cada reportante conserva su propia comunicación; estudiantes indirectamente por continuidad de historia.

**4. Funcionalidades:** elección explícita de principal y duplicado, consulta de fuentes/papeles/diferencias, participación única computable de persona identificada y participantes presentes solo en duplicados; no unificar externos inciertos. Retirar/cambiar duplicidad con causa. Agrupar hechos distintos en caso existente, conservar atenciones previas sin fusionar ni copiar actuaciones, decidir una continuación vigente; corregir vínculos y cerrar apertura vacía errónea. Historia cronológica y consultas por personas incluyen todos los datos disponibles y grupos históricos; aporta filtros completos de hechos/atención de RF-16. Propagar reserva por duplicidad y agrupación.

**5. Flujo E2E:** dos reportantes comunican el mismo hecho con personas diferentes → institucional elige principal → ve participantes/fuentes sin transcribir → continúa en atención elegida → incorpora otros hechos relacionados → consulta desde cada estudiante → corrige un vínculo equivocado y comprueba historia/pendientes. Cada autor vuelve a su aporte aislado.

**6. RF y CU:** C RF-15, RF-22–RF-23; P RF-16 (contexto I09), RF-18 (reiteración I05, informe I06 y estadísticas I11 completan sus consumidores) y RF-28 (nuevas relaciones). P CU-05/CU-06/CU-12; V CU-02/CU-03 y atención de CU-07.

**7. RN/RNF:** RN-01–RN-04, RN-06–RN-08, RN-10–RN-12, RN-15, RN-17, RN-20–RN-23; RNF-02, RNF-06–RNF-08, RNF-10–RNF-16 y RNF-19; T01–T08. El cálculo de responsabilidades con discrepancia queda pendiente de aclaración, sin ocultar involucramiento ni aviso grave.

**8. Dependencias:** I01–I03. No depende de estadísticas; sus reglas de unidad y procedencia ya se comprueban en consulta y atención.

**9. Condiciones/datos:** ejemplo Alfa afectado en principal/duplicado, Beta testigo y Familiar F presunto solo en duplicado; estudiante atribuido solo allí; contradicción de papel sin aclaración y con aclaración; externos homónimos inciertos; dos atenciones previas, una con otros hechos propios pendientes; tres hechos y grupos diferentes; exclusión en cualquiera de los registros.

**10. Excluido/diferido:** fusionar relatos/casos/identidades o reconstruir versiones; revisión sistemática de participaciones coherentes. PDF I06 y análisis I11 reutilizarán la composición de fuentes sin nuevas selecciones manuales.

**11. Aceptación verificable:** el ejemplo produce un hecho, tres participantes pertinentes y dos estudiantes; Alfa no se repite y nadie hereda papel de Familiar F. Beta accede a la misma historia institucional por su participación en el duplicado. Discrepancia conserva versiones y aclaración vigente separada. Dos atenciones eligen continuidad del hecho, preservando las actuaciones anteriores; la otra sigue por sus propios hechos. Retirar vínculo sustancial exige causa; retirar último hecho de apertura errónea no deja caso pendiente vacío. Avisos duplicados dejan de ser tarea independiente. Retirar duplicidad errónea recupera dos hechos sin reconstruir contenidos. Reportantes y excluidos no ganan acceso indirecto.

**12. Pruebas:** CU-05/CU-06 con fuentes distintas, papeles coherentes/discrepantes y cambio de principal; concurrencia al vincular, doble petición, relaciones inconsistentes o circulares rechazadas cuando impidan un principal inequívoco; dos atenciones y corrección del último vínculo; consultas por cada persona, identificaciones parciales y roles acumulados; exclusión propagada antes de una nueva lectura; regresión de acciones inmediatas/compromisos sin copias.

**13. Arquitectura indispensable:** evolucionar la representación inicial para consultar el hecho y sus aportes con procedencia. Evitar que cada consumidor concatene personas de modo distinto; la solución común concreta se decide aquí. Mantener referencias a actuaciones previas y una atención pendiente por hecho mediante garantías persistentes frente a concurrencia. No introducir un subsistema de fusiones.

### I05. Advertir reiteración sin validar notas cotidianas

**1. Identificador y nombre:** I05 — Señales por categoría y gestión unificada del asunto.

**2. Objetivo funcional:** detectar patrones con parámetros expresos y actualizar una sola tarea útil por asunto.

**3. Actores beneficiados:** TOE y comité elegible; registradores cuya nota cotidiana ya tiene efecto sin revisión individual.

**4. Funcionalidades:** TOE/director introduce ventana común y umbral por categoría de prueba/aprobado cuando exista; visualiza categorías inactivas por falta de parámetros. Cálculo por estudiante/categoría/hecho único, con distinción de atribución cotidiana, no corroborada y corroborada. Señal sustentada en fuentes/ventana/parámetros, actualización con novedades y combinación de motivos con gravedad. Recalcular por paso de fecha local, nuevos hechos, cambios de atribución, anulación, duplicidad y parámetros; conservar referencia de señales emitidas. Gestionar por atención/disposición sin otra recepción; hecho nuevo tras cierre permite señal nueva sin reabrir automáticamente.

**5. Flujo E2E:** configurar valores expresos → guardar conductas cotidianas en distintos días → consultar señal al alcanzar umbral → revisar fundamento → atender/cerrar en un acto → registrar hecho nuevo → observar nueva novedad. Corregir duplicidad/atribución y comprobar fundamento y bandeja actualizados.

**6. RF y CU:** C RF-20–RF-21; P RF-03 (parámetros, edición completa de catálogo I10), RF-18 y RF-34 (fundamento/recuentos de señales, análisis completo I11). C CU-02; P CU-04 (análisis I11), CU-14 y CU-17; V CU-03/CU-05/CU-07.

**7. RN/RNF:** RN-01–RN-05, RN-07–RN-13, RN-15, RN-22, RN-28; RNF-02, RNF-06–RNF-08, RNF-10–RNF-14, RNF-16, RNF-19 y RNF-22; T01–T08. Los ejemplos de umbral/ventana no son política escolar.

**8. Dependencias:** I01–I04. I10 no es dependencia: se incorpora aquí solo el mantenimiento de parámetros necesario para esta capacidad.

**9. Condiciones/datos:** reloj de prueba al 20-10-2026; W=10 y umbral=3 exclusivamente ficticios; hechos del 10/11/15/20; atribución solo en duplicado y discrepante; víctima, testigo, desconocido, descartado, contexto y anulado; carga tardía y fecha aproximada; categoría sin valores.

**10. Excluido/diferido:** sanción/corroboración/caso automáticos, reglas de «consecutivas», umbral universal, motor configurable. Estadísticas variadas I11/I12; catálogo completo I10.

**11. Aceptación verificable:** ventana inclusiva 11–20, hecho del 10 fuera; tres atribuciones cotidianas cuentan sin TOE. Reportes presuntos se rotulan separados; el familiar denunciado no incrementa faltas del estudiante afectado. Una participación elegible solo en duplicado cuenta una vez; contradicción impeditiva no cuenta responsabilidad hasta aclararse. Carga tardía usa fecha del hecho y ventana actual, sin aviso retroactivo simulado; aproximación insuficiente queda fuera. Sin parámetros se informa ausencia y funcionan gravedad/manual. Leer/recalcular sin novedad no repite avisos; cambios de día eliminan fundamentos caducados sin perder referencia histórica.

**12. Pruebas:** límites de W y zona Perú, avance de día sin nuevos guardados, guardado tardío, cambios de ventana/umbral, autoría cotidiana vs. corroborada, toda exclusión del contador; actualización concurrente/reintentos; alerta ya atendida y novedad posterior; coherencia señal/bandeja/historial tras duplicidad/anulación/aclaración, sin exponer casos excluidos. Regresión del flujo de una confirmación final.

**13. Arquitectura indispensable:** obtener hora local de forma controlable para pruebas y reevaluar al cambiar el día; elegir el mecanismo más sencillo que mantenga resultados actuales. Compartir definiciones de hecho/atribución con I04, preservar parámetros de la señal emitida y evitar crear avisos por cada ejecución técnica. No exige cola, motor de reglas ni infraestructura distribuida.

### I06. Sustentar y registrar una derivación externa

**1. Identificador y nombre:** I06 — Informe consolidado, derivación y referencias oficiales.

**2. Objetivo funcional:** preparar una comunicación institucional útil y documentar su ejecución real, separando cierre local y proceso externo.

**3. Actores beneficiados:** TOE y comité en atención ordinaria autorizada; autoridades externas como destinatarias sin cuentas. El registro de actos colectivos se habilita en I07 para director/sustituto.

**4. Funcionalidades:** informe único del caso legible, imprimible y PDF descargable durante atención, antes de derivar y al cierre, también para casos no graves; seleccionar contexto pertinente ya disponible, sin transcribir. Incluye todos los campos mínimos de RF-31 y fuentes/papeles de duplicados; datos aún desconocidos quedan expresos. Versión interna y dirigida a terceros con finalidad/destinatario y reserva de violencia. Previsualización sin archivar obligatoriamente; emisión efectiva de cierre/derivación identificada y conservada. Derivación preparada o efectuada en una operación si ya ocurrió, con entidad/motivo/fecha y referencia disponible; se puede añadir oficio, cargo o reporte cuando se conozca, sin adjunto/número obligatorio ni relatos redundantes. Regla de Fiscalía para violencia familiar completa; caso mixto permanece pendiente por lo no abarcado. Referencias manuales de Libro/SíseVe/otros y protocolo/versión, completables después.

**5. Flujo E2E:** abrir caso familiar → preparar derivación → revisar PDF consolidado dirigido a Fiscalía → emitir/descargar → realizar comunicación fuera del sistema → registrar ejecución efectiva → cierre local cuando abarca todo el objeto → documentar seguimiento posterior sin cambiar cierre. Alternativa: incorporar un origen SíseVe al hecho existente sin crear otra ocurrencia.

**6. RF y CU:** C RF-33; P RF-29–RF-30 (actos formales de comité I07), RF-31 (comité/acta I07 y contexto importado I09), RF-18 (nuevo consumidor), RF-28 (informes/descargas) y RF-38. P CU-07 (facultades colectivas I07), CU-10–CU-12 y CU-05/CU-09. La generación no acredita que se haya enviado nada.

**7. RN/RNF:** RN-01–RN-04, RN-07–RN-08, RN-11–RN-13, RN-15–RN-16, RN-19–RN-21, RN-24–RN-28; RNF-02–RNF-09, RNF-10, RNF-12–RNF-13, RNF-16, RNF-18–RNF-20 y RNF-23; T01–T08.

**8. Dependencias:** I03/I04 para atención y composición de fuentes; I02 para reserva. I05 aporta señales al informe si existen, pero no es dependencia funcional de una derivación.

**9. Condiciones/datos:** caso con tres hechos, familiares/externos y participantes solo en duplicado, roles discrepantes/aclarados; estudiante sin DNI; derivación preparada/efectiva; Policía frente a Fiscalía; caso mixto; códigos oficiales ausentes y protocolo histórico. Informe de hasta 50 incidencias para rendimiento.

**10. Excluido/diferido:** envío, firma, oficio inventado, registro automático oficial y motor de protocolos; actas/actos colectivos I07; selección de antecedentes importados I09. No se exige plantilla institucional para generar el contenido mínimo.

**11. Aceptación verificable:** tres hechos producen un consolidado que conserva fuentes y todas las personas pertinentes sin copiar al principal. En violencia a terceros: iniciales y DNI disponible, sin nombres completos de estudiantes; sin DNI se indica ausencia/referencia interna segura y no se bloquea protección. Preparación a Fiscalía no cierra; ejecución familiar completa sí; otro destino, otro supuesto o caso mixto no cierran automáticamente. Se muestran estado local y constancias externas separados; código ausente no bloquea. Emisión mantiene autor, fecha/corte/motivo y puede actualizarse con otra referencia; previsualización no genera versiones innecesarias. PDF de 50 incidencias se prepara en ≤15 s bajo ERS 4.1.

**12. Pruebas:** PDF real, legibilidad e impresión, contenido contra datos persistidos, procedencia y discrepancias; selección/omisión de contexto; cuatro variantes de derivación y reintento sin duplicar; enlace anterior tras exclusión, autor ordinario y referencias manipuladas; temporales tras fallo; bloqueo/disposición autorizada sobre PDF emitido y respaldo; constancias oficiales tardías y nuevo corte sin alterar silenciosamente el anterior.

**13. Arquitectura indispensable:** separar preparación documental de comunicación externa. Producir el informe desde información autorizada del caso y fuentes, sin lógica de papeles divergente del historial. Elegir aquí formato de conservación y adaptador PDF; reservar almacenamiento/descarga, incluirlo en copias y disposición. No crear un gestor documental de versiones.

### I07. Intervenir como comité y adjuntar el acta externa

**1. Identificador y nombre:** I07 — Intervención formal del comité en el mismo caso.

**2. Objetivo funcional:** que un caso especialmente grave no resuelto reciba intervención colectiva con responsabilidad y documentación correctas.

**3. Actores beneficiados:** TOE, director, docente/auxiliar miembros vigentes; sustitutos elegibles cuando TOE o director están excluidos o el director no está vigente.

**4. Funcionalidades:** convocatoria habitual por TOE, con fecha/motivo y miembros elegibles; asunción por restantes si TOE está excluido; registro de asistentes efectivos. El caso sigue siendo el mismo y su estado solo pendiente/cerrado. Director registra acuerdos/actuaciones/derivación colectivos; sustituto deja autoría real y motivo. Adjuntar varias actas PDF/.docx con datos mínimos, consulta/descarga y sustitución motivada con referencia anterior. Informe integra intervención y referencia de actas. No se espera acta/firma para proteger.

**5. Flujo E2E:** TOE convoca en caso pendiente → miembros consultan → director registra intervención/acuerdos/derivación o cierre → adjunta acta externa cuando esté disponible → autorizado consulta informe/acta. Repetir con director excluido y con TOE excluido, usando sustitución sin alterar comité estable.

**6. RF y CU:** C RF-27, RF-29–RF-30 y RF-32; C CU-07 en facultades colectivas. P RF-01 (vigencias ya efectivas, gestión I10), RF-28 (actas) y RF-31 (contexto I09). P CU-08 (renovación operable I10 y acceso efectivo a estadísticas I11), CU-09 (análisis/salida I12), CU-11 (contexto I09); amplía CU-10 en facultades colectivas.

**7. RN/RNF:** RN-06–RN-08, RN-12–RN-16, RN-20, RN-24–RN-28; RNF-01–RNF-09, RNF-10, RNF-12–RNF-13, RNF-16, RNF-18–RNF-20 y RNF-23; T01–T08.

**8. Dependencias:** I03/I04/I06 y controles de I02. Vigencias de prueba conocidas; I10 completará su mantenimiento escolar.

**9. Condiciones/datos:** comité estable y luego otra vigencia; integrantes no implicados y sustituto, director excluido/no vigente y TOE excluido; actas válidas PDF/.docx, límite de 10 MB y superior, formato falso/malformado, nombre de archivo que intenta cambiar ruta y contenido activo inocuo de prueba.

**10. Excluido/diferido:** generación/edición de actas, firmas colectivas, aprobación por integrante, nuevo comité o expediente por caso. Una gravedad o reiteración no dispara convocatoria automática.

**11. Aceptación verificable:** el miembro vigente ya consulta/atiende institucionalmente sin convocatoria, pero los actos colectivos se guardan por director o sustituto habilitado. TOE sin otra habilitación no registra automáticamente acuerdos colectivos. Si director está excluido/no vigente, miembro restante registra sustitución; si TOE está excluido, no se espera su convocatoria. Varias actas se guardan una vez por caso; se aceptan hasta 10 MB, límite visible y rechazo claro por exceso/formato, sin dañar otros datos. Sustituir deja causa/referencia. Fin de vigencia retira siguiente operación y conserva autorías. Excluido no descarga actas ni PDF por enlace previo.

**12. Pruebas:** CU-08/CU-09 en intervención formal; miembro frente a cargo ordinario y TOE convocante frente a autor colectivo; sustituciones y ausencia de elegibles; carga/descarga real, archivos no confiables sin ejecución, colisión de nombres, permiso revocado durante operación, fallo sin adjunto huérfano anunciado como válido; restauración de acta y referencia sustituida; regresión del informe y derivación sin acta obligatoria.

**13. Arquitectura indispensable:** reutilizar atención y autorizaciones existentes; distinguir vigencia estable, elegibilidad del caso y participación efectiva. Adaptador de archivos privados sin enlaces que sustituyan la autorización. Metadatos/contenido deben quedar coherentes y recuperables; la técnica concreta se decide en este incremento.

### I08. Importar identidades y matrícula anual sin romper la historia

**1. Identificador y nombre:** I08 — Nómina operable y conciliación entre años.

**2. Objetivo funcional:** que AIP incorpore/actualice la nómina con revisión de excepciones y una confirmación, sin limpieza rutinaria del formato conocido.

**3. Actores beneficiados:** AIP; registradores y atención que buscan identidades estables. Administrador aporta años/grupos válidos, precargados hasta I10.

**4. Funcionalidades:** elegir .xlsx/hoja, confirmar año, proponer y revisar asociaciones reutilizables con encabezados agrupados, columnas movidas, títulos, huecos y filas auxiliares. Grado/sección combinado/separado y grupo común/por bloque conocido. Previsualizar alcance, cantidades y diferencias, localizar errores hoja/fila/campo, excluir/cancelar/revalidar. Confirmar filas aceptables con resultado fiel y procedencia; reintentar sin duplicar. Conciliación por documento válido único sin contradicción; candidatos sin documento requieren confirmación segura, no unión por nombre/grupo. Admite nombre/año sin DNI, fecha de nacimiento/sexo/situación/cambio de grupo legítimamente informados; identificadores textuales. Rectificar identidad/grupo dentro de alcance autorizado y borrado explícito de campos con revisión.

**5. Flujo E2E:** AIP elige réplica de matrícula y año → revisa asociación/cobertura/excepciones → resuelve u omite fila insegura → confirma una vez → consulta resultado del lote → registrador busca estudiante y atención recupera historia → repetir/cargar año siguiente → completar grupo conocido sin nueva identidad.

**6. RF y CU:** C RF-11; P RF-02, RF-06 (contactos I09), RF-08–RF-10 (contexto/contactos I09) y RF-39 (rectificación por lote). C CU-06; P CU-01/CU-14/CU-15/CU-17. V RF-04, RF-16 y CU-02/CU-05 en identidad/historia. El lote identifica las columnas contextuales excluidas de esta entrega.

**7. RN/RNF:** RN-07–RN-08, RN-17–RN-19, RN-24–RN-28; RNF-01–RNF-09, RNF-11–RNF-16, RNF-18–RNF-20, RNF-22–RNF-25; T01–T08. La relación y actualización de contexto se añade a la misma operación en I09.

**8. Dependencias:** I01/I02 para identidad/acceso; I04 aporta comprobación de historia ya compartida. No depende de que se haya concluido el catálogo administrativo, las estadísticas o los documentos.

**9. Condiciones/datos:** réplica totalmente ficticia de Hoja1, 121×17: estudiante fila 7 y 90 filas 15–104; etiqueta fila 6, vacías 8–14 y solo numeración 105–121. Columnas C/D son estudiante; L/M/N son familiar y no criterio de identidad del alumno. B vacía. Variantes de encabezados, grado «1° A»/«1 A»/«Primero A», documentos contradictorios, homónimos, sin DNI y datos opcionales. Otra muestra de 1 000 filas/30 columnas asociadas.

**10. Excluido/diferido:** contactos/antecedentes selectivos I09; alta de años en UI I10. Sin OCR, ejecución/reparación de fórmulas o formato universal. Sin promoción automática, baja por ausencia, fusión universal o división artificial del nombre.

**11. Aceptación verificable:** 91 identidades válidas con documentos de ocho cifras textuales; repetir no duplica identidad/vínculo. Grupo ausente permite incorporación sin tarea TOE; defecto común no pisa grupo conocido. Sin documento hay identidad interna; homónimos ambiguos no se unen automáticamente. DNI incompleto no se rellena; familiar compartido no une estudiantes. Ausencia del lote no elimina/retira. Vacíos/omitidos no borran; borrado explícito muestra alcance. Fecha Excel/texto inequívoco se admite; fórmula solo usa resultado guardado válido sin ejecutar. Fallo no deja fila parcialmente aplicada ni éxito falso; cada fase de validación/confirmación ≤2 min bajo 4.1, sin revisión humana/transferencia.

**12. Pruebas:** adaptador .xlsx real con todos los formatos descritos, archivo malformado/falso, hoja múltiple y columnas reordenadas; normalización de identificadores sin inferencia, conciliación anual/contradicciones/sin documento, corrección delimitada de identidad y preservación histórica; cancelación sin cambios, reintento tras respuesta perdida, concurrencia entre previsualizar/confirmar y dos lotes; AIP sin acceso a casos; revisión de temporales/columnas omitidas/diagnósticos; representante AIP con guía sin ayuda del autor.

**13. Arquitectura indispensable:** interpretar Excel en adaptador y separar esa lectura de validaciones/conciliación de aplicación y reglas. Revalidar estado y autorización al confirmar; conservar fila coherente con procedencia/resultado. Evolucionar identidad/año según los datos realmente admitidos, no construir un subsistema integral de matrícula. El archivo original innecesario no se conserva por defecto.

### I09. Incorporar contactos y contexto admitido con reserva longitudinal

**1. Identificador y nombre:** I09 — Importación selectiva de contexto y aviso institucional.

**2. Objetivo funcional:** poner información pertinente de protección a disposición de atención, sin otorgar a AIP lectura de antecedentes previos ni copiar columnas indiscriminadamente.

**3. Actores beneficiados:** AIP para su lote; TOE/comité no excluidos para atención; responsable institucional define alcance/aviso, y un operador con autorización delimitada los carga sin crear un rol de acceso universal.

**4. Funcionalidades:** ampliar la misma asociación/previsualización/confirmación de I08 con contactos y conceptos contextuales admitidos. Nombre/relación conocida/teléfono familiar y domicilio pertinente, teléfono del estudiante separado y documento familiar solo con necesidad documentada. Antecedente etiquetado con contenido, fuente de lote, incorporación y fecha/período disponible; negación distinta de ausencia. Nueva columna contextual admitida asociable sin código. Actualización/borrado explícitos muestran alcance; si AIP no ve el anterior conoce sustitución/procedencia sin contenido previo. Historia reservada y selección del contexto pertinente para informe. Poner a disposición aviso vigente con todo RF-40 y mantener alcance admitido sin aprobación por celda ni edición de código.

**5. Flujo E2E:** cargar aviso/alcance institucional autorizado → AIP asocia contacto/contexto permitido y ve lo omitido → revisa/confirma lote → TOE consulta procedencia en historia → selecciona antecedente pertinente al informe externo → AIP rectifica por nuevo lote sin leer historial anterior → comprobar derivados afectados según la decisión aplicable.

**6. RF y CU:** C RF-06–RF-10, RF-16, RF-31 y RF-40; P RF-39 en rectificación y minimización. C CU-01/CU-10/CU-11; P CU-14/CU-15/CU-17; V CU-05/CU-06/CU-12 en fuentes y protección.

**7. RN/RNF:** RN-02, RN-07–RN-08, RN-17–RN-21, RN-24–RN-28; RNF-02–RNF-09, RNF-11–RNF-14, RNF-16, RNF-18–RNF-20 y RNF-22–RNF-25; T01–T08.

**8. Dependencias:** I08 para importación; I04/I06 para consulta e informe. No se difiere la selección de campos hasta después de incorporar datos.

**9. Condiciones/datos:** réplica ficticia con enfermedad/discapacidad, convivencia, contacto compartido, «no» y vacío; observaciones heterogéneas con texto/número; futura columna «Bullying previo informado» admitida y otra no admitida; antecedentes previos inaccesibles para AIP; contacto sin relación exacta, DNI familiar no justificado; decisiones ficticias de rectificación/bloqueo.

**10. Excluido/diferido:** edición libre de antecedentes, historias clínicas, datos de hermanos/instrucción/ocupación, deducción de sexo/parentesco, captura universal de consentimientos. La institución tramita fundamento e información a titulares por sus canales. Recorrido completo de disposición I13.

**11. Aceptación verificable:** se omiten datos no admitidos y no se guardan en temporales permanentes. «No» y desconocido permanecen distintos. Bullying previo no crea hecho/reiteración; columna nueva no admitida se omite con aviso, la admitida se asocia sin programar campos a medida. Cada antecedente conserva fuente/fechas. AIP ve solo lo necesario de lote/conciliación, tampoco recupera contenido anterior mediante historial del lote o error. Vacío no borra; borrado/sustitución explícitos tienen alcance visible. Identidad/matrícula/contexto de una fila se confirman coherentemente. Informe incorpora contexto elegido sin transcribir ni copiar toda la ficha. Aviso y alcance se actualizan sin código y sin cuentas para familias.

**12. Pruebas:** CU-01 completo, actualización selectiva y reintento conjunto; antecedente negativo/vacío, concepto admitido/no admitido, observación numérica, contactos compartidos y campos alumno/familiar separados; autorización al confirmar y antecedente previo oculto en toda respuesta AIP; rectificación y bloqueo en historia/PDF; inspección de temporales/diagnósticos/copias operativas; AIP resuelve columna movida/fila errónea con guía; selección contextual de CU-10/CU-11.

**13. Arquitectura indispensable:** ampliar el importador existente y su unidad coherente de confirmación, sin crear importadores inconexos. Antecedentes etiquetados con procedencia no implican motor de esquemas jurídicos. Respuestas de AIP y consultas de atención tienen alcances distintos; no exponer el objeto persistido completo como respuesta. Incorporar contextos y aviso al respaldo y a los procedimientos vigentes de tratamiento.

### I10. Mantener cuentas y configuración sin dependencia del desarrollador

**1. Identificador y nombre:** I10 — Autonomía operativa escolar.

**2. Objetivo funcional:** sustituir la preparación ficticia por mantenimiento autorizado de cuentas, institución, designaciones, categorías y años.

**3. Actores beneficiados:** administrador operativo designado; TOE/director para valores definidos institucionalmente; AIP que selecciona años/grupos disponibles. Administrar no concede lectura de atención.

**4. Funcionalidades:** altas individuales, desactivación, recuperación y vigencias TOE/comité, listado de roles/vigencias para revisión semestral; conservar autorías. Datos institucionales para informes, año de trabajo y años/grupos históricos. Catálogo con nombre/descripción, naturaleza/gravedad/aviso, admisión o no de reiteración, umbral propio y ventana común, activación/desactivación de categoría; delegación de carga de valores definidos sin aprobación electrónica por cambio. Autor/fecha de mantenimiento; sin autoatribución de roles. Categoría desactivada no admite nuevos usos y sigue en historia; cambios no reclasifican hechos ni reescriben señales emitidas. Acceso al aviso/alcance de I09 y a estado operativo sin lectura de expedientes.

**5. Flujo E2E:** administrador mantiene una designación → miembro entra con capacidades vigentes → TOE/director define/carga categoría o administrador delegado carga valores exactos → crear año/grupos → AIP importa/rectifica año seleccionado → comprobar historia/pendientes y revisión de privilegios → terminar membresía con sesión abierta y verificar revocación.

**6. RF y CU:** C RF-01–RF-03 y CU-14; P RF-41 (estado/entrega completa I14) y CU-08 (estadísticas efectivas I11). V CU-01/CU-04/CU-09/CU-10 y RF-11/RF-28/RF-40 en cambios de configuración.

**7. RN/RNF:** RN-03, RN-06–RN-09, RN-14, RN-17–RN-18, RN-24–RN-28; RNF-01–RNF-03, RNF-06–RNF-09, RNF-12–RNF-13, RNF-20–RNF-25; T01–T08.

**8. Dependencias:** I01/I02/I05/I08/I09 para verificar efectos reales. La parte de cuentas/años puede adelantarse tras I02 sin esperar documentos, si se conservan estos ensayos al integrarla.

**9. Condiciones/datos:** decisiones y delegaciones ficticias, administrador sin otro rol, usuario con roles acumulados y otro excluido; membresía expirada/renovada; categorías usadas y nuevas; parámetros ausentes y modificados; año histórico con pendientes; instalación ficticia con otro nombre institucional. Cuenta inicial bajo procedimiento institucional protegido documentado, sin cuentas compartidas.

**10. Excluido/diferido:** redefinir reglas estables, sanciones o flujos arbitrarios; aprobación por nota/cambio; justificar rutinariamente cada mantenimiento o subir archivos de designación; editar datos internos para tareas escolares. Ningún nombre de rol crea facultades no previstas en capítulo 2.

**11. Aceptación verificable:** baja/fin de designación impide siguiente operación en dos sesiones y conserva autoría. Administrador no lee casos ni se concede privilegios; no retira exclusión propia. Miembro mantiene atención equivalente aunque no configure. Carga delegada deja autor real sin decidir políticas. Año nuevo no promueve/cierra/borra; rectificar nómina histórica no exige desbloqueo anual. Ausencia de ventana/umbral es visible; categoría inactiva preserva historia, cambio de naturaleza/gravedad no reescribe hechos. Operadores completan tareas con guía sin código ni ayuda del autor.

**12. Pruebas:** CU-14 completo y renovación de CU-08; permisos por operación de mantenimiento, acumulación/separación de capacidades, autoría delegada y no autoatribución; recuperación de cuenta y sesiones comprometidas; concurrencia de vigencias/catálogo con operaciones abiertas; cambio de año e informe institucional; revisión semestral sobre listado y prueba de instalación/configuración de otro colegio; regresión de reiteración/restricción y asociación de columnas sin código.

**13. Arquitectura indispensable:** ampliar los mismos datos/vigencias/parámetros utilizados desde el inicio. Reemplazar precargas de ensayo por operaciones administrativas sin redefinir el núcleo. Separar valores institucionales variables de invariantes no desactivables. Documentar provisión inicial de acceso y actualización de versión sin depender de cuentas personales del desarrollador.

### I11. Analizar hechos, participaciones y carga de atención con cifras coherentes

**1. Identificador y nombre:** I11 — Análisis institucional por unidades y dimensiones.

**2. Objetivo funcional:** que TOE/comité comprenda convivencia y trabajo real mediante tablas/gráficos y filtros combinables, sin doble conteo.

**3. Actores beneficiados:** TOE, director y miembros vigentes del comité no excluidos. Roles ordinarios, AIP y administrador sin rol funcional son pruebas negativas.

**4. Funcionalidades:** todas las medidas de RF-34: hechos, participaciones por papel/carácter, estudiantes distintos y atribuciones separadas, afectados/testigos, casos, señales por motivo/condición, actuaciones previstas/realizadas, compromisos/seguimientos pendientes/comprobados y derivaciones efectivas. Filtros pertinentes combinables por período, año, categoría, naturaleza, gravedad, grado/sección, sexo informado, papel/carácter de atribución, estado de atención, motivo de alerta, entidad de derivación e intervención del comité. Cruces de dos o más dimensiones admitidas, sin máximo artificial de dos filtros. Frecuencias, distribución, evolución mensual/anual, comparación de períodos, reiteración y carga; tablas/gráficos, mapa semanal por fecha real del hecho, análisis horario solo con hora fiable. Cobertura, desconocimiento, criterio de fecha/universo y subtotales solapados visibles. Cifras exactas en grupos internos pequeños.

**5. Flujo E2E:** seleccionar medida/período/dimensiones y filtros → revisar tabla/gráfico/mapa → contrastar con hechos/atención autorizados → corregir atribución/duplicidad en su flujo → volver al análisis y explicar nueva cifra mediante trazabilidad. Miembro excluido repite consulta sin obtener indicios del caso restringido.

**6. RF y CU:** C RF-18, RF-34–RF-35; P RF-36 (cantidades/sexo disponible, denominadores I12), RF-37 (presentación interna sin descargas todavía) y RF-28 (reserva estadística, salida I12). C CU-04/CU-05/CU-08/CU-12; P CU-13/CU-09/CU-17. CU-08 requiere además la renovación operable de I10 para declararse completo, aunque el análisis puede desarrollarse con vigencias ficticias antes.

**7. RN/RNF:** RN-01–RN-04, RN-07–RN-13, RN-15–RN-19, RN-22–RN-23, RN-26, RN-28; RNF-02, RNF-05–RNF-09, RNF-12–RNF-16, RNF-19–RNF-20 y RNF-25; T01–T08.

**8. Dependencias:** I04/I05 para unidades/señales, I06/I07 para derivación/comité e I09 para todas las dimensiones disponibles. No necesita una nómina completa para frecuencias absolutas; debe declarar cobertura.

**9. Condiciones/datos:** base de CU-13 con dos hechos, cuatro participaciones, tres estudiantes y un caso, duplicado y atribuciones de distinto carácter; lunes/miércoles; prueba adicional de CU-05 con persona solo en duplicado, externos y discrepancias; datos sin sexo/grupo/hora; cierre por error/duplicidad y actuaciones reales; diferentes fechas efectivas/de registro; caso excluido y grupo de dos estudiantes.

**10. Excluido/diferido:** porcentajes/tasas y exportación I12; consultas SQL libres, análisis de relatos, BI general, supresión automática por menos de cinco o deducción de horas/sexo/grupo. No se fabrica un indicador porque su dimensión no aplique a la medida.

**11. Aceptación verificable:** base CU-13 da 2 hechos/4 participaciones/3 estudiantes/1 caso; corroborar denuncia solo cambia carácter, sin corroborar nota cotidiana ni aumentar participaciones. Se incluyen estudiantes solo en duplicados y no se cuentan familiares como estudiantes. Cierre erróneo/duplicidad no suma atención efectiva resuelta. Mapa marca lunes/miércoles y no hora de registro. Se producen categoría/grado y sexo/período con filtros adicionales pertinentes; tabla y gráfico coinciden. Subtotales por grupos advierten solapamiento. Sin dato aparece desconocido, no cero. Interno autorizado ve dos; excluido no puede reconstruir su caso mediante filtros/totales particulares. Resumen ordinario ≤10 s bajo 4.1.

**12. Pruebas:** resultados conocidos reconciliados en detalle/bandeja/informe/análisis; cada medida y dimensión aplicable, combinaciones de filtros y fechas, corte actual tras corrección; no sumar duplicado/anulado ni perder participante solo en duplicado; accesos directos y ataques de inferencia con desgloses/totalizaciones; tablas equivalentes a gráficos, teclado/lector y contraste; integración de consultas con volumen 4.1.

**13. Arquitectura indispensable:** lectura analítica con definiciones compartidas de unidad/atribución/reserva. Elegir consultas/índices según medición, sin almacén BI o copia completa de información sensible por anticipado. Cualquier caché queda subordinada a permiso/restricción y correcciones actuales; la optimización no autoriza cifras incoherentes.

### I12. Comparar poblaciones válidas y descargar resultados reservados

**1. Identificador y nombre:** I12 — Denominadores, comparaciones y exportación por finalidad.

**2. Objetivo funcional:** producir comparaciones interpretables y salidas que la institución pueda custodiar y preparar para comunicación externa.

**3. Actores beneficiados:** TOE/director/comité elegibles; responsable institucional que decide comunicación externa por sus canales.

**4. Funcionalidades:** comparación por sexo/grupo informados con cobertura, cantidades y proporciones de estudiantes distintos de la misma población anual/período/grupo/sexo; frecuencia de hechos por cien estudiantes separada de porcentaje y de tasa de exposición. No calcular si no hay denominador válido; declarar referencia anual cuando el período de hechos es menor. Imprimir/descargar resumen PDF y tablas CSV o .xlsx, con fecha/corte, medidas, filtros, cobertura y redondeo. Agregados sin nombres/DNI/relatos/columnas ocultas. Preparar agrupación más amplia/omisión para entrega externa bajo decisión institucional; no envío/publicación automáticos. Revalidar reserva al descargar, incluso resultados preparados antes de exclusión.

**5. Flujo E2E:** elegir participación/población/período → revisar numerador/denominador y cobertura → comparar grupos/sexos → descargar PDF y tabla reservados → preparar agregado externo más amplio y revisar identificación/finalidad/destinatario institucionalmente. Excluir después a quien preparó una salida y probar su intento de descarga anterior.

**6. RF y CU:** C RF-28, RF-36–RF-37; C CU-09/CU-13; P CU-15/CU-17; V CU-04/CU-05/CU-11 en fuentes, reserva y cortes. RF-38 se amplía con todas las salidas y se completa operativamente en I13.

**7. RN/RNF:** RN-01–RN-04, RN-07–RN-08, RN-10, RN-17–RN-19, RN-22–RN-26, RN-28; RNF-02–RNF-09, RNF-12–RNF-16, RNF-19–RNF-20, RNF-23 y RNF-25; T01–T08.

**8. Dependencias:** I11 para análisis e I08/I09 para poblaciones/datos; reserva de I02/I04. No se exige recopilar exposición individual adicional.

**9. Condiciones/datos:** 20 estudiantes de población válida, dos involucrados y tres hechos; estudiante repetido en varios hechos, sin matrícula vigente, grupo/sexo desconocidos y nómina parcial; períodos/poblaciones distintos; agregado pequeño y caso excluido; texto inocuo con prefijo de fórmula en campos exportables.

**10. Excluido/diferido:** motor universal de anonimización, publicación web, mensajería o cifras externas consideradas seguras solo por no tener nombres. La preparación institucional puede usar filtros/agrupaciones y omitir datos sin crear un módulo legal o un flujo de aprobación.

**11. Aceptación verificable:** ejemplo produce 10 % de estudiantes y 15 hechos por cien, con rótulos diferentes; no se afirma 15 % ni riesgo individual/tasa exacta. Numerador y denominador son de la misma población; sin denominador válido no calcula ni presenta cero. Sexo/grupo desconocidos se separan con cobertura y comparaciones explican tamaños. PDF y CSV o .xlsx representan tabla/filtros/corte sin identificadores ni contenido activo. Interno ve cifras exactas pequeñas; exterior requiere revisión institucional de identificación. Reserva se aplica a datos fuente y cobertura, incluyendo filtros capaces de reconstruir excluidos; un enlace antiguo no supera restricción vigente.

**12. Pruebas:** cálculo y redondeo con resultados conocidos, poblaciones incompatibles y denominador ausente/parcial; reconciliar pantalla/PDF/tabla descargada y cortes posteriores; fórmula no ejecutable al abrir exportación; permiso revocado entre preparar/descargar, dato fuente excluido/bloqueado y caché antigua; distintos roles y ámbitos; lectura accesible de gráficos/valores y prueba externa de agrupación/omisión sin enviar datos.

**13. Arquitectura indispensable:** declarar universo y referencia poblacional en el resultado, sin ligar identidad al año. Generación/exportación es adaptador de un resultado autorizado, no acceso libre al almacenamiento. El formato puede ser CSV o .xlsx; se implementa y prueba al menos uno, además del PDF. No se necesita un sistema de publicación o anonimización automática.

### I13. Ejecutar una decisión delimitada sobre datos y comprobar sus efectos

**1. Identificador y nombre:** I13 — Derechos, conservación y evidencia utilizable de todo el producto.

**2. Objetivo funcional:** que la institución obtenga una respuesta procedente y ejecute/verifique rectificación, bloqueo o disposición válida sin revelar terceros ni depender del autor original.

**3. Actores beneficiados:** responsable institucional que decide fuera del sistema; operador con autorización específica; AIP para nuevo lote y soporte técnico autorizado para disposición excepcional. No se crea un rol de lectura universal.

**4. Funcionalidades:** completar un procedimiento utilizable que combina búsqueda, extracción delimitada, corrección/importación y los controles de bloqueo/disposición ya introducidos. Alcance/fundamento/fecha de decisión pueden estar en constancia externa; ejecución deja evidencia sin reproducir contenido retirado. Localizar los datos de una persona en hechos compartidos, contexto, fuentes duplicadas, informes emitidos, actas y otras copias operativas identificadas; aplicar disposición/anonimización solo cuando esa sea la decisión válida. Verificar resultados en consultas, derivados, archivos y accesos, preservando terceros/retenciones. Recuperación legible e inmediata de evidencia por período/usuario/operación/referencia, usando medios básicos documentados. Gestionar destinatarios previos y respuesta fuera del sistema según procedimiento.

**5. Flujo E2E:** recibir decisión institucional delimitada → autorizado localiza y prepara información procedente → AIP rectifica por lote o soporte ejecuta disposición/bloqueo autorizado → otro control de lectura/descarga verifica efectos y conservación de terceros → recuperar evidencia de resultado → registrar constancias necesarias y tratamiento de copias antiguas aisladas. Ensayo adicional sin bloqueo por excepción aplicable documentada.

**6. RF y CU:** C RF-38–RF-39; P RF-41 en entrega/copia posterior a decisiones; C CU-15 en operación/disposición, con V de restauración conjunta CU-15/CU-16 en I14. V CU-01/CU-05/CU-09/CU-11/CU-13; CU-16 parcial hasta ensayo integral.

**7. RN/RNF:** RN-07–RN-08, RN-17–RN-20, RN-23–RN-28; RNF-01–RNF-09, RNF-12, RNF-18–RNF-23 y RNF-25; T01–T08. No se aplica bloqueo indiscriminado por recibir una solicitud: se siguen condiciones/excepciones de la ERS.

**8. Dependencias:** I09 para contexto/lotes, I12 para todas las salidas e incrementos anteriores para localizar el conjunto persistido. No habilita a los incrementos previos a ignorar T02/T05.

**9. Condiciones/datos:** decisión ficticia con identidad/representación comprobada, dato sensible incorrecto, persona en hecho compartido/duplicado, informe emitido y acta, retención/suspensión válida y copias operativas identificadas; respaldo anterior y constancias posteriores de baja/exclusión/disposición. Operador autorizado delimitado y técnico sin autorización como negativo.

**10. Excluido/diferido:** portal/expediente de solicitudes, consentimientos, plazos automáticos, políticas universales, eliminación por año/cierre, borrado de cada respaldo histórico, reconstrucción de información suprimida. Ensayo completo de recuperación/transferencia I14.

**11. Aceptación verificable:** se prepara información procedente sin entregar automáticamente expediente/actas/relatos de terceros; violencia conserva reserva específica. Rectificación ordinaria no depende del desarrollador. Decisión y ejecución identifican alcance/fundamento/fecha, sin copiar información retirada a evidencia. Los datos alcanzados dejan de revelarse en persistencia y copias operativas identificadas, no solo en un botón; terceros legítimos se conservan. Retención impide disposición improcedente. Un bloqueo afecta toda lectura/tratamiento alcanzados y respeta excepciones decididas. Evidencia se recupera inmediatamente por medios documentados y no permite al implicado investigar un caso. Copias anteriores permanecen aisladas y con vencimiento documentado.

**12. Pruebas:** CU-15 completo con decisiones de alcance distinto, bloqueo y excepción, terceros compartidos y autorización específica; disposición de filas/archivos/PDF emitidos/derivados y revisión de temporales/evidencia sin datos retirados; intento de ejecutar sin autorización y fallo parcial que no se anuncia completo; recuperar evidencia de lectura/importación/informe/exportación; reintento/control concurrente y resultado verificable por otro responsable; preparar ensayo de copia antigua para I14.

**13. Arquitectura indispensable:** completar el inventario real de datos/copias, no suponer un esquema futuro. Reutilizar operaciones existentes; intervención técnica excepcional protegida, documentada y contrastada con resultado observable. No exige plataforma de derechos, borrado distribuido ni cadenas inmutables. Una disposición jurídica puede alcanzar la salida emitida y sus datos personales pese a su corte histórico.

### I14. Recuperar y transferir la operación institucional con restricciones vigentes

**1. Identificador y nombre:** I14 — Recuperación completa y continuidad autónoma.

**2. Objetivo funcional:** que un técnico distinto recupere el servicio y entregue información interpretable sin revivir datos/accesos retirados, y que el personal reanude su trabajo.

**3. Actores beneficiados:** responsable operativo/técnico autorizado, institución que controla cuentas/medios y operadores escolares que vuelven a registrar/consultar. TOE/AIP no vigilan copias por su rol.

**4. Funcionalidades:** recorrido completo de consulta de estado/fecha/fallo de servicio y copias mediante herramientas o registros documentados; conjunto recuperable de datos/archivos/configuración/relaciones/restricciones y formatos interpretables. Ensayar pérdida principal y corrupción reciente, recuperar copia anterior, comprobar integridad, aplicar constancias vigentes posteriores y abrir servicio; demostrar registro/consulta/atención después de restaurar. Transferir puesta en marcha/actualización/diagnóstico y pruebas a otro técnico con inventario/licencias/cuentas/costos reproducibles. Procedimiento de incidentes con evidencia ficticia y responsables, sin comunicaciones reales.

**5. Flujo E2E:** operador conoce fallo → consulta copia y resultado → técnico autorizado restaura en entorno controlado → verifica datos/archivos/conteos y aplica bajas/exclusiones/disposiciones posteriores → prueba acceso permitido y denegado → habilita servicio → docente registra con fecha efectiva una acción ocurrida durante caída y TOE consulta → otro técnico interpreta entrega y repite comprobación crítica con la guía.

**6. RF y CU:** C RF-41 y CU-16; V RF-01/RF-28/RF-38–RF-40 y CU-15. C CU-17 se declara en la aceptación global acumulativa con sus representantes y métricas, no por el ensayo técnico solo. Verificación de todos los demás CU tras restauración en los recorridos críticos seleccionados.

**7. RN/RNF:** RN-07–RN-08, RN-17, RN-24–RN-28; RNF-01–RNF-09, RNF-16–RNF-25; T01–T08. Usabilidad/accesibilidad de las tareas reanudadas mantienen RNF-10–RNF-15.

**8. Dependencias:** I13 y el conjunto de capacidades que produce la información a recuperar. Puede prepararse operación desde I01; el ensayo completo necesita todos sus tipos de datos y restricciones, no todas las tareas de desarrollo en serie.

**9. Condiciones/datos:** conjunto ficticio integral con hecho compartido/duplicado, contexto, caso cerrado y pendiente, informe/acta, exportación y constancias posteriores a la copia; credenciales/medios protegidos bajo control autorizado; técnico distinto y entorno de restauración. Jornada escolar operativa documentada y registro de disponibilidad mensual.

**10. Excluido/diferido:** centro de monitoreo, panel escolar propio, reconciliación automática de historias, copia selectivamente reescrita, guardia permanente, garantía de ocho horas o infraestructura redundante obligatoria. La evaluación/comunicación legal de brechas queda en el procedimiento institucional.

**11. Aceptación verificable:** pérdida ≤24 h de cambios confirmados y recuperación ≤2 días hábiles desde disponer de medios e iniciar el procedimiento; una copia anterior utilizable sobrevive a pérdida/corrupción principal. Antes del acceso ordinario se aplican decisiones vigentes, sin revivir baja, exclusión o dato retirado. Hechos/vínculos/archivos/conteos se reconcilian y la entrega completa se interpreta fuera de un visor propietario inaccesible. Otro técnico opera solo con documentación/medios autorizados. Estado/fallo no revela relatos. Se documentan ensayo inicial, repetición anual escolar y cambios que afectan recuperación. Disponibilidad mensual ≥99 % en jornada documentada, incluido mantenimiento como caída, con evidencia efectiva. Exceso de objetivos es fallo, aunque esté explicado. Recursos/cuentas/costos y responsables están declarados; no hay dependencia de cuenta personal del autor.

**12. Pruebas:** CU-16 con copia corrupta/anterior y CU-15 tras restaurar; cuentas revocadas/excluidas y datos retirados posteriores a copia; lectura/descarga por rutas anteriores, integridad de archivos y relaciones, reintento de registro durante caída sin duplicidad; actualización fallida y recuperación de versión/información; transferencia de mantenimiento y salida completa contra muestra conocida; evidencia ficticia de incidente y comunicación al responsable según procedimiento, sin enviar mensajes; aceptación global de sección 7.

**13. Arquitectura indispensable:** recuperación de la instalación y sus adaptadores como capacidad operativa vertical, seguida de tareas UI reales; no es «construir infraestructura al final». Aprovechar herramientas convencionales y ampliar los procedimientos existentes. Documentar formatos y relaciones efectivamente construidos; mantener aislamiento de copias antiguas y revisión controlada de decisiones antes de exponer datos.

## 5. Matriz de trazabilidad completa

Cada fila tiene un punto de finalización y una comprobación observable. **C no elimina las verificaciones posteriores:** un nuevo consumidor debe conservar el requisito. En requisitos de alcance transversal, P significa que se implementa obligatoriamente en cada superficie ya entregada; C identifica la consolidación de todas las superficies del producto. No habilita a diferir un control necesario hasta ese incremento final. Los escenarios G01–G11 se especifican en la sección 7 y son aceptación global adicional, no sustitutos de las fichas.

### 5.1 Requisitos funcionales — 41 identificadores

| ID | Comportamiento concreto exigido | P: construcción/ampliación | C: alcance completo | V: comprobación adicional |
|---|---|---|---|---|
| RF-01 | Cuentas individuales, alta/baja/recuperación, vigencias y comité estable, autorías, listado semestral y no autoatribución | I01 acceso; I02 vigencias efectivas; I07 responsabilidad colectiva | I10 mantenimiento autónomo | I13/I14, G01/G11: bajas y privilegios tras disposición/restauración |
| RF-02 | Institución/años/grupos configurables, año explícito, hechos tardíos y nómina histórica sin promover/eliminar/cerrar | I01 año/grupo; I08 selección/actualización anual | I10 | I14, G03/G11: otro colegio/año conserva historia y pendientes |
| RF-03 | Naturaleza/gravedad/aviso por categoría, umbral propio y ventana común, faltantes visibles, desactivación e historia | I01/I02 clasificación; I05 parámetros y señales | I10 | I11/I14, G05: cambios no reclasifican hechos ni reescriben señales |
| RF-04 | Búsqueda tolerante por identidad/año/grupo, homónimos e históricos, mínimo visible y acceso institucional autorizado | I01 búsqueda para registrar | I02 | I08/I09/I10, G01/G03/G09: matrícula actualizada sin divulgar contexto |
| RF-05 | Personal inequívoco respecto de cuenta; externos/familiares mínimos, parciales, sin padrón ni cuentas exigidas | — | I02 | I04/I06/I09, G01/G04/G06: participante solo en duplicado llega a atención/informe |
| RF-06 | Identidad estable, documentos textuales, vínculo anual y cambios fechados, campos legítimos opcionales, contacto/familiar separados | I01 mínimos; I08 identidad/anualidad/demografía | I09 | I10/I12/I13, G03/G08: sin inferencia ni borrado accidental |
| RF-07 | Contexto selectivo etiquetado, procedencia/fechas, negación vs. ausencia, futuro bullying admitido, reserva AIP | — | I09 | I11/I13/I14, G03/G08: no cuenta como hecho y se rectifica/retira conforme a decisión |
| RF-08 | .xlsx/hoja/asociación reutilizable, encabezados agrupados/variantes, grupo común y omisión de columnas/rangos | I08 identidad/matrícula | I09 contexto/contacto | I10/I14, G03/G09: columnas movidas y nuevas admitidas sin código |
| RF-09 | Previsualizar y corregir excepciones hoja/fila/campo, documentos/fechas/grupos y fórmulas guardadas sin ejecución | I08 nómina | I09 todos los campos seleccionables | I13, G03/G09: cancelación, ambigüedad y comprensión por AIP |
| RF-10 | Confirmación única, resultados fieles, fila coherente, reintento sin duplicar, vacío no borra y borrado explícito | I08 identidades/vínculos | I09 contexto/contacto y procedencia reservada | I10/I13/I14, G03/G08: rectificación y fallo/reintento completos |
| RF-11 | Coincidencia segura por documento, excepciones sin unión por nombre/familiar, continuidad anual y ausencia sin baja | — | I08 | I09/I10/I13/I14, G03/G04: 91 identidades repetidas y cambios conservan historia |
| RF-12 | Registro oportuno con datos propuestos, varios papeles, comunicación incompleta/provisional y acción urgente, sin caso automático | I01 cotidiano | I02 todos los tipos/personas | I03/I05/I06, G02/G05/G06: tardío, incierto y grave sin burocracia |
| RF-13 | Papel por persona/hecho y carácter de atribución, no herencia, cotidiano visible, presunto no corroborado, afectados/testigos separados | I01 estudiantes cotidianos | I02 todas las personas/naturalezas | I03/I04/I05/I06/I11, G02/G04/G05/G07: aclaración y fuentes no mezclan papeles |
| RF-14 | Acción/compromiso inmediato opcional con hecho no grave, sin atención y recuperable sin copiar | — | I01 | I03/I04/I06/I11, G02/G04: misma constancia histórica sin doble actuación |
| RF-15 | Filtros/lectura de aporte propio; noticia de relación sin acceso a principal ajeno, actuaciones, informe o acta | I01 propio; I02/I03 recepción/atención | I04 relación de duplicado | I06/I07/I14, G01/G04: autor ordinario no descarga documentos del caso |
| RF-16 | Consulta institucional por filtros/personas y trayectoria cronológica completa, fuentes/roles/contexto con reserva por elemento | I02 hechos; I03 atención; I04 fuentes/historia; I08 continuidad | I09 contexto completo | I11/I13/I14, G01/G03/G04/G08: no revela ni fragmentos/conteos de excluidos |
| RF-17 | Corrección propia antes de atención, aclaración posterior, cambio institucional, causa sustancial y anulación por error | I01 corrección rutinaria | I03 | I04/I05/I13, G02/G04/G05/G08: no anular por no probar denuncia |
| RF-18 | Principal/duplicado sin fusión, personas solo en duplicado, fuentes/discrepancias, una ocurrencia, continuidad de atención y corrección de relación | I04 núcleo; I05 reiteración; I06/I09 informe completo | I11 último consumidor: estadísticas | I12/I13/I14, G04/G05/G06/G07: una misma composición en todos los resultados |
| RF-19 | Primer aviso por gravedad/protección/manual sin esperar confirmación, con receptores elegibles antes del detalle | — | I02 | I03/I05/I10, G01/G05/G09: acceso inmediato ≤5 s sin avisar al implicado |
| RF-20 | Ventana local inclusiva, hechos únicos elegibles por estudiante/categoría, distintos caracteres, recálculo y novedad sin sanción | — | I05 | I10/I11/I12/I14, G04/G05/G07: límites, tardíos, duplicados y discrepancias |
| RF-21 | Bandeja de nuevos/atenciones/seguimientos reales, ordenar/filtrar, lectura separada y gestión conjunta sin recepción duplicada | I02 nuevos; I03 pendientes; I04 duplicidad | I05 combinación de motivos/novedades | I06/I07/I11, G02/G05: cierre no oculta seguimiento ni crea tarea cotidiana |
| RF-22 | Atender desde origen sin transcribir, abierto/cerrado directo, vincular a caso existente y actualizar avisos | I03 atención directa | I04 vinculación compartida | I05/I06/I07/I14, G02/G04: conversación/compromiso/cierre con un guardado |
| RF-23 | Agrupar hechos/personas, única atención pendiente por hecho, referencias previas, corregir vínculos y apertura vacía errónea | — | I04 | I06/I07/I11/I14, G04: actuaciones intactas, sin fusión/caso por estudiante |
| RF-24 | Aclarar/corroborar/descartar con autor/fecha/fundamento de actuación, sin confirmar todo ni alterar originales | — | I03 | I04/I05/I06/I11, G04/G05/G07: cambia atribución sin heredar ni sumar hechos |
| RF-25 | Acción realizada directa, prevista opcional, completar la misma, cancelar con causa, fecha efectiva y alcance pertinente | — | I03 | I04/I06/I07/I11/I14, G02/G07: prevista no realizada y común cuenta una vez |
| RF-26 | Compromiso con persona/contenido, plazo solo acordado, seguimiento/cumplimiento observados, sin tarea/sanción automática | — | I03 | I06/I07/I11, G02/G07: sin plazo no vence ni se presume cumplido |
| RF-27 | Convocatoria TOE, mismo caso/comité, intervención efectiva, registro director/sustituto y asunción sin TOE implicado | — | I07 | I10/I13/I14, G01/G06: vigencias y sustituciones sin firmas ni convocatoria automática |
| RF-28 | Conflicto preventivo/expreso prevalente, propagación a relaciones/derivados, siguiente acceso revocado y retirada no propia | I02 hecho/avisos; I03/I04 atención/relaciones; I06/I07 documentos; I11 análisis | I12 salidas analíticas | I13/I14, G01/G08/G10: ningún rol, enlace, agregado o restauración lo burla |
| RF-29 | Derivación preparada/efectiva con destinatario/motivo/fecha/referencia disponible, informe previo y sin envío automático | I06 ordinaria | I07 acto colectivo con autor habilitado | I09/I11/I14, G06/G07: reintento no duplica y solo efectiva suma |
| RF-30 | Cierre con resultado/fecha y pendientes reales, regla familiar-Fiscalía completa, caso mixto y continuidad/corrección trazables | I03 cierre/seguimiento; I06 derivación y supuesto especial | I07 facultades colectivas completas | I09/I11/I14, G02/G06: cierre local distinto de externo |
| RF-31 | PDF único con mínimo completo, fuentes/personas de duplicados, contexto elegido, corte/emisión y reserva específica a terceros | I06 documento/derivación; I07 comité/acta | I09 contexto importado seleccionable | I11/I13/I14, G04/G06/G08: no transcribir, no inferir y disponer derivados alcanzados |
| RF-32 | Varias actas PDF/.docx por caso, datos mínimos, hasta 10 MB, sustitución motivada y descarga reservada | — | I07 | I09/I13/I14, G01/G06/G10: archivos no públicos y recuperables |
| RF-33 | Orígenes/referencias manuales de instrumentos y protocolo/versión, completables, sin certificar cumplimiento/cierre | — | I06 | I07/I09/I11/I14, G06/G07: nuevo origen no añade hecho si ya existe |
| RF-34 | Todas las unidades, fechas, universos y condiciones exigidas, sin duplicados/anulados ni pérdida de participantes | I05 fundamentos y recuentos de señales | I11 | I12/I13/I14, G04/G07: tablas/informe/bandeja reconciliados y cortes explicables |
| RF-35 | Filtros pertinentes combinables, cruces de dimensiones, frecuencias/evolución/carga, tabla/gráfico/mapa semanal y cobertura | — | I11 | I12/I14, G07/G09: cruces con filtros adicionales y hora solo fiable |
| RF-36 | Sexo informado/cobertura, proporciones de población coherente, hechos por cien distintos y no cálculo sin denominador | I11 cantidades y sexo disponible | I12 | I14, G07: 10 % frente a 15 hechos/cien; sin tasa de exposición inventada |
| RF-37 | PDF y CSV o .xlsx con criterios/corte, agregados sin identidad, cifras internas exactas y preparación externa revisada | I11 presentación y ámbito internos | I12 | I13/I14, G01/G07/G08: fórmula inerte y sin inferir caso excluido |
| RF-38 | Evidencia automática recuperable por período/usuario/operación/referencia, extracción autorizada y sin relatos indebidos | I01–I12 cada nueva operación | I13 procedimiento sobre producto completo | I14, G08/G10: reconstruir lectura, lote, cambios e informe sin acceso universal |
| RF-39 | Localizar/preparar, rectificar/bloquear según decisión y disponer datos/copias operativas protegiendo terceros | I01–I12 procedimiento/control por dato nuevo | I13 recorrido institucional completo | I14, G08/G10: decisiones válidas aplicadas antes de abrir copia antigua |
| RF-40 | Aviso completo accesible y alcance admitido actualizable sin código; nuevas columnas no autorizan tratamiento | — | I09 | I10/I13/I14, G03/G08/G11: carga autorizada sin consentimientos/permiso por celda |
| RF-41 | Estado/fecha/fallos de copias y ensayo, conjunto completo protegido/relacionado e interpretación por otro responsable | I01–I13 ampliación con lo persistido | I14 | G10/G11: recuperar datos/archivos/restricciones y transferir sin cuenta del autor |

### 5.2 Reglas de negocio — 28 identificadores

| ID | Regla materializada | P: construcción/ampliación | C | V: evidencia concreta |
|---|---|---|---|---|
| RN-01 | Hecho único con varios aportes/personas y procedencia, sin fusión ni ampliar acceso | I01–I03 soporte de varias personas | I04 | I05/I06/I11/I12, G04: participantes solo en duplicado y un hecho en todo consumidor |
| RN-02 | Registro/contexto no prueba culpabilidad, sanción o falta de víctima | — | I01 | I02–I14, G02/G06/G07: rótulos y cálculos no atribuyen responsabilidad por afectación |
| RN-03 | Clasificación declarada, por clasificar/manual, primer aviso y cambios no retroactivos | I01/I02 registro/avisos; I05 parámetros | I10 | G05/G06: desactivar/cambiar catálogo conserva historia |
| RN-04 | Papel por persona/hecho; cotidiano cuenta, violencia no corroborada y desconocido/afectado separados | I01 cotidiano | I02 | I03/I04/I05/I06/I11, G02/G04/G05/G07: no propagación de papel ni corroboración |
| RN-05 | Registro cotidiano sin TOE, acción opcional y sin caso/revisión automática | — | I01 | I03/I05/I14, G02/G05: nota cuenta aunque TOE nunca la abra |
| RN-06 | Equivalencia institucional de comité y responsabilidades específicas de recepción/convocatoria/registro/configuración | I02/I03 permisos; I07 proceso formal | I10 delegación/configuración completas | G01/G06/G11: facultades no dependen de aprobación por caso |
| RN-07 | Propio ordinario, institucional reservado, AIP delimitado y administración sin expedientes | I01 consulta propia | I02 | I03–I14, G01/G03/G08: toda nueva superficie conserva el ámbito |
| RN-08 | Conflicto prevalente, restantes atienden y sustituyen sin permiso del implicado | I02 restricción/asunción; I04 propagación | I07 actos colectivos/sustitución | I10/I12/I14, G01/G10: roles acumulados y restauración no restituyen acceso |
| RN-09 | Umbral entero por categoría/ventana común positiva expresos; sin universal ni consecutivas | — | I05 | I10/I11, G05: falta de parámetros no inventa valores |
| RN-10 | Una ocurrencia elegible estudiante/categoría, duplicados con todos sus participantes y discrepancia impeditiva excluida | — | I05 | I11/I12, G04/G05/G07: fuentes no suman ni pierden personas |
| RN-11 | Gravedad independiente, lectura no gestión, actualización conjunta, novedad sin reapertura automática | I02–I04 recepción/atención/duplicidad | I05 | I06/I11, G02/G05: una tarea por asunto y sin avisos por recálculo vacío |
| RN-12 | Caso compartido, única atención pendiente, previas referenciadas, sin copiar acciones ni exigir caso por persona | — | I04 | I06/I07/I09/I11/I14, G04: vínculo corregible e historia cerrada sin nuevo conteo |
| RN-13 | Actuación realizada directa, previstas/compromisos/seguimientos opcionales reales, sin sanciones | I01 acción cotidiana | I03 | I07/I11, G02/G07: ejecución completa la misma prevista; sin plazo no vence |
| RN-14 | Comité excepcional por decisión TOE, mismo caso, director/sustituto y acta externa | — | I07 | I10/I14, G06: gravedad/reiteración no convocan por algoritmo |
| RN-15 | Cierre local suficiente con pendientes visibles, seguimiento posterior y corrección motivada | — | I03 | I06/I07/I11/I14, G02/G06: cierre no cumple compromisos ni cambia por seguimiento |
| RN-16 | Preparada/efectiva distintas; solo familiar-Fiscalía con objeto completo cierra localmente | — | I06 | I07/I09/I11, G06: Policía/preparación/parcial no cierran todo |
| RN-17 | Identidad independiente de año/grupo, documento preferente, sin unión por familiar ni promoción/baja inferida | I01 representación estable | I08 | I09/I10/I11/I14, G03/G04: cambios conservan antecedentes/grupo conocido |
| RN-18 | Nómina selectiva y desconocidos, encabezados correctos, textos sin dígitos inventados y sin personas auxiliares | I08 identidad/matrícula | I09 contexto/contacto | I10/I13, G03: muestra 91, P/S omitido, vacío no borra |
| RN-19 | Contexto con fuente/calidad/fundamento, sin condena/diagnóstico/falta inferidos ni consentimiento universal | — | I09 | I11/I13/I14, G03/G08: selección admitida y bullying previo no contabilizado |
| RN-20 | Informe único distinto de acta, fuentes/papeles exactos y reserva de violencia exterior | I06 informe; I07 acta | I09 contexto pertinente | I11/I13/I14, G04/G06: consolidado sin transcripción y iniciales/DNI disponible |
| RN-21 | Local no sustituye instrumento/protocolo ni cierre oficial; referencia histórica conserva versión | — | I06 | I07/I09/I11/I14, G06: origen/código no certifica registro o cierre externo |
| RN-22 | Unidades/fechas/grupos/cobertura y población coherentes, sin falsa tasa o subtotales aditivos | I04 unidad; I05 señales; I11 medidas | I12 proporciones | I14, G07: resultados conocidos y referencias temporales visibles |
| RN-23 | Exactitud interna en grupos pequeños, revisión externa y conflicto también en agregados | I02 reserva; I06 externo nominativo; I11 agregados | I12 | I13/I14, G01/G07: dos visibles internamente y sin publicación automática |
| RN-24 | Institución decide derechos/base/excepciones, operador ejecuta alcance y protege terceros | I01–I12 controles/procedimientos crecientes | I13 | I14, G08: no bloqueo automático universal ni consentimiento del presunto agresor |
| RN-25 | Conservación fundada, disposición de copias operativas y recuperación sin revivir retirados | I01–I13 por persistencia/decisión | I14 copia antigua y producto completo | G08/G10: retención y aplicación previa a acceso, sin dos años universales de expediente |
| RN-26 | Confidencialidad/autoridad, retirada de acceso y evidencia; obligaciones organizativas sin rol universal | I01–I13 cada operación | I14 entrega/operación completas | G01/G08/G11: soporte delimitado y cuenta/ODP sin expedientes por defecto |
| RN-27 | Protección por canales competentes aun sin sistema y fechas reales posteriores | — | I02 | I03/I06/I14, G02/G06/G10: caída no exige esperar validación ni certifica cumplimiento |
| RN-28 | Solo parámetros razonables variables; invariantes de identidad/reserva/cierre no desactivables | I01–I09 invariantes y datos variables | I10 configuración completa | I11–I14, G01/G03/G11: otro colegio conserva comportamientos estables |

### 5.3 Requisitos no funcionales — 25 identificadores

La exigibilidad empieza con la primera función relacionada. Las consolidaciones C de esta tabla no sustituyen T01–T08 ni permiten usar inseguramente una entrega anterior.

| ID | Aplicación progresiva obligatoria | P: implementación/ampliación | C: consolidación | V: medición o ensayo de aceptación |
|---|---|---|---|---|
| RNF-01 | Autenticación, credenciales no legibles, recuperación/limitación, cierre, 20 min con aviso, revocación siguiente operación | I01 acceso; I02 conflicto; I10 administración | I10 | I13/I14, G01: dos sesiones, enlaces y cuenta/rol revocados |
| RNF-02 | Matriz en toda lectura/cambio/proceso; mínimos, membresías y exclusiones | I01–I13 cada operación y nuevo recurso | I14 recuperación/soporte completos | T01/T02 y G01/G10: llamadas directas, rol múltiple, fin de vigencia y administrativo |
| RNF-03 | Proteger transmisión/almacenamiento/secretos, adjuntos, copias y entrega completa | I01 datos; I06/I07 archivos; I08/I09 lotes; I13 disposición | I14 entrega recuperable | G01/G10/G11: tercero sin credenciales no interpreta tráfico ni accede a copias/rutas |
| RNF-04 | Texto/archivos no confiables; no ejecutar contenido; formato/límites/rutas y autorización por descarga | I01 textos; I06 PDF; I07 actas; I08/I09 Excel | I12 exportación tabular inerte | I13/I14, G03/G06/G07: archivos falsos/malformados, fórmula y vínculo externo inocuos |
| RNF-05 | Minimizar campos/diagnósticos, sin usos ajenos y retirar temporales/columnas descartadas | I01–I12 cada entrada/salida | I13 inventario y procedimiento completo | I14, G03/G08: inspección de lotes, temporales, logs y generaciones fallidas |
| RNF-06 | No revelar en avisos/errores/URL/sesión; descargas solicitadas/reservadas y custodia | I01/I02 vistas; I06/I07 documentos; I11 análisis | I12 todas las salidas | I13/I14, G01/G07: volver atrás tras sesión/permisos no recupera datos del servicio |
| RNF-07 | Trazabilidad de sesiones/lecturas/mutaciones/importación/exportación, inmediata/protegida ≥2 años | I01–I12 por operación | I13 recuperación integral de evidencia | I14, G08/G10: reconstruir caso/lote/informe y fallo de evidencia sin éxito silencioso |
| RNF-08 | Sin duplicidad/incompletos/sobrescritura, referencias válidas y fechas coherentes | I01–I12 cada intención compuesta | I13 disposición/control concurrente completo | I14, G02/G03/G04/G10: dos sesiones, pérdida de respuesta, filas coherentes y pasado/futuro |
| RNF-09 | Pruebas de seguridad y revisión de vulnerabilidades de componentes antes de datos reales | I01–I13 inventario/revisión y ensayos por superficie | I14 puerta de seguridad integral | G01/G08/G10: corregir o deshabilitar hallazgos que afecten confidencialidad/integridad |
| RNF-10 | Campos propuestos, cotidiano sin TOE y atención conjunta con una confirmación | I01 cotidiano; I02/I03 atención | I03 | I04–I14, G02/G09: sin transcripción ni motivos/guardados administrativos repetidos |
| RNF-11 | Prueba viable con tres representantes y uno AIP, orientación ≤30 min y tareas sin ayuda | I01/I03 ensayos registro/TOE; I08/I09 AIP | I14 aceptación acumulada | G09: 90 s, 30 s, 3 min y AIP comprende asociación/cambios/exclusiones |
| RNF-12 | Errores en español conservan entrada, estado real, abandono advertido y alcance previo a única confirmación | I01–I12 cada tarea y operación sensible | I13 disposición con alcance verificable | I14, G02/G03/G04/G08/G09: validación/red fallida y filtros conservados |
| RNF-13 | Términos y distinciones claras, navegación contextual sin repetición, ayuda y avisos actualizados | I01–I11 tareas y vocabulario progresivos | I12 producto funcional completo | I13/I14, G02/G04/G06/G09: local/oficial, prevista/realizada y atribuciones distinguibles |
| RNF-14 | Teclado/lector/foco/etiquetas/estados, 200 %, contraste y alternativas de gráficos | I01–I10 por pantalla; I11/I12 análisis | I12 tareas/resultados completos | I13/I14, G09: criterios pertinentes WCAG 2.2 con inspección manual además de herramientas |
| RNF-15 | 320/1 280 px, navegador Android y dos versiones estables de dos familias de escritorio | I01–I13 por flujo; importación puede priorizar escritorio con aviso | I14 versiones de aceptación documentadas | G09: sin pérdida de operaciones/desplazamiento horizontal ordinario ni software de pago por puesto |
| RNF-16 | p95 y volúmenes de ERS 4.1 medidos desde solicitud a resultado utilizable | I01 búsqueda/guardado; I02 aviso; I04 historial; I06 PDF; I08/I09 lote; I11/I12 resumen | I14 conjunto y entorno completos | G09: ≥20 mediciones/tarea y 10 usuarios concurrentes; sin confundir transferencia/preparación |
| RNF-17 | Disponibilidad mensual ≥99 % en jornada documentada y continuidad visible con reintento seguro | I01 fallos/red y registro tardío; I02/I03 continuidad; I10 jornada operativa | I14 medición y ensayo integral | G10: registro/consulta autenticados, mantenimiento cuenta, sin promesa de guardia/offline |
| RNF-18 | Copias protegidas desde primeros datos, pérdida ≤24 h, restauración ≤2 días hábiles y copia anterior | I01 copia/ensayo básicos; I02–I13 ampliación por cada recurso/decisión | I14 ensayo completo y constancias posteriores | G10: inicial/anual/tras cambio pertinente, integridad y decisiones antes de abrir acceso |
| RNF-19 | Definiciones compartidas, cifras/tabla/gráfico/corte reconciliados y ausencia no cero | I01–I11 por consultas y consumidores | I12 resultados y poblaciones completos | I13/I14, G04/G05/G06/G07/G08: correcciones y supresión no reconstruyen datos retirados |
| RNF-20 | Guías escolares y técnicas de lo existente, mantenimiento/puesta en marcha por otro técnico | I01–I13 guía y entrega incremental | I14 demostración de autonomía | G09/G11: tareas habituales sin código y copia de prueba solo con documentación/medios |
| RNF-21 | Estado/fallo, evidencia/recuperación y procedimiento institucional de incidente sin relatos | I01 estado básico; I06/I07/I13 evidencia de nuevos recursos | I14 recorrido institucional completo | G08/G10: proveedor avisa sin demora y responsable evalúa comunicación ANPD en 48 h en supuestos ERS, sin enviarla en prueba |
| RNF-22 | Configuración sin código y actualización recuperable con regresiones críticas reproducibles | I01–I09 migración/pruebas; I10 autonomía de parámetros | I14 ensayo de actualización y entrega | G03/G05/G10/G11: otro técnico detecta regresión de acceso/identidad/conteos/cierre/recuperación |
| RNF-23 | Entrega completa en formatos documentados, con relaciones/roles/fechas/archivos interpretables | I01–I13 inventario y extracción de lo existente | I14 transferencia completa | G11: reconciliar cantidades/relaciones/adjuntos sin visor propietario inaccesible |
| RNF-24 | Servicios/licencias/recursos/renovaciones/cuentas/costos reproducibles y tareas periódicas documentadas | I01–I13 actualizar inventario al elegir recursos | I14 evaluación completa de sostenibilidad | G11: costos fijos/variables, crecimiento/cambio de responsable, sin techo monetario inventado |
| RNF-25 | Identidades/relatos ficticios en desarrollo, pruebas/evidencias y entrega; real solo bajo acceso controlado autorizado | — | I01 condición permanente | I02–I14, G03/G09/G11: inspeccionar repositorio, capturas y materiales sin datos reales |

### 5.4 Casos de uso — 17 identificadores

| ID | Resultado y alternativas que deben aceptarse | P: recorrido parcial | C | V: aceptación global/regresión |
|---|---|---|---|---|
| CU-01 | Formato real replicado, 91 identidades, contexto permitido, excepciones, grupo desconocido, anualidad/reintento sin duplicar ni abrir historial a AIP | I08 nómina | I09 | I10/I13/I14, G03/G08/G09 |
| CU-02 | Cotidiano con acción opcional, consulta propia y atribución ya elegible sin TOE | I01 registro; I02/I03 reserva; I04 fuentes | I05 señal automática cuando corresponde | I10/I14, G02/G05/G09 |
| CU-03 | Primera gravedad incompleta, exclusión antes de aviso, atención/aclaración y protección sin prejuzgar | I02 comunicación/recepción | I03 atención | I04/I06/I07, G01/G02/G05 |
| CU-04 | Ventana 11–20 de prueba, papeles excluidos, duplicados/tardíos, señal/atención/novedad y recuentos | I05 señales y atención; I10 mantenimiento de parámetros | I11 medidas de comprobación | I12/I14, G05/G07 |
| CU-05 | Duplicado conserva fuentes/personas/reserva, una ocurrencia en historia/atención/informe/reiteración/estadística, dos atenciones y corrección | I04 relación; I05 señal; I06/I09 informe | I11 estadísticas completas | I12/I13/I14, G04/G07 |
| CU-06 | Tres hechos y varias personas en caso compartido, corrección de vínculos/previas y continuidad del grupo histórico | I03 atención; I04 agrupación/historia | I08 actualización real de identidad/grupo | I09/I14, G03/G04 |
| CU-07 | Conversación/compromiso/cierre en un guardado, prevista opcional, pendientes/seguimiento y cierre corregible, informe | I03 intervención y continuidad; I06 informe | I07 facultades colectivas completas | I09/I14, G02/G06/G09 |
| CU-08 | Convocatoria excepcional, capacidades equivalentes, autoría colectiva/acta posterior y renovación de membresía | I02 permisos; I07 intervención/acta; I10 renovación operable | I11 estadísticas accesibles a miembro vigente | I14, G01/G06/G07/G11 |
| CU-09 | Denunciar director/TOE/otro miembro, sustituir, negar cada camino indirecto y preservar otros casos, sin elegibles | I02/I03 hechos/atención; I04 relaciones; I06/I07 archivos; I10 vigencias; I11 análisis | I12 descarga estadística | I13/I14, G01/G07/G10 |
| CU-10 | Familiar presunto parcial y estudiante afectado, informe externo con contexto, Fiscalía efectiva completa vs. preparada/otro destino/mixto y continuidad | I02/I03 personas/atención; I06 derivación; I07 facultades | I09 contexto y documento completo | I11/I14, G06/G08 |
| CU-11 | Consolidado con tres hechos/fuentes/papeles, minimización externa, acta válida/sustituida, límite y acceso directo denegado | I06 informe; I07 actas | I09 selección contextual completa | I13/I14, G01/G04/G06/G08 |
| CU-12 | Origen SíseVe con/sin hecho previo, referencia tardía, un hecho y análisis local, estado/protocolo histórico separado | I04 duplicidad; I06 referencias/atención/informe | I11 análisis | I14, G04/G06/G07 |
| CU-13 | Medidas conocidas, cruces combinables, mapa, poblaciones/porcentajes, exactitud interna y revisión/reserva exterior | I11 medidas/filtros | I12 comparaciones/salidas | I13/I14, G07/G09 |
| CU-14 | Cuentas/designaciones, parámetros delegados, año e importación histórica, privilegios y autonomía | I01/I02 vigencias; I05 parámetros; I08/I09 importación/aviso | I10 mantenimiento | I14, G01/G03/G05/G11 |
| CU-15 | Información procedente, rectificación por lote, bloqueo según decisión/excepción, disposición compartida y copias aisladas | I01–I12 controles por dato/salida | I13 recorrido institucional | I14, G08/G10 con respaldo anterior y decisiones posteriores |
| CU-16 | Pérdida/corrupción, restauración íntegra dentro de objetivos, restricciones/decisiones vigentes y otro técnico/costos | I01–I13 copia/guías y ampliación de alcance | I14 | G10/G11, con CU-15 tras restauración |
| CU-17 | Representantes sin ayuda, tiempos, AIP, teclado/lector/teléfono/200 %, concurrencia/fallos y respuesta | I01/I03 registro/atención; I08/I09 AIP; I11/I12 resultados | I14 aceptación acumulativa global | G09; repetición de lo afectado si se corrige un obstáculo |

## 6. Dependencias y evolución del diseño

### 6.1 Dependencias reales y oportunidades de reordenación

La numeración expresa un orden recomendado. Las dependencias directas de esta tabla son de capacidades, no de la estructura de paquetes. Las dependencias transitivas no se repiten.

| Incremento | Depende directamente de | Razón / independencia útil |
|---|---|---|
| I01 | — | Primer recorrido cotidiano; datos de prueba precargados. |
| I02 | I01 | Reutiliza registro, identidad y seguridad básica. |
| I03 | I02 | Necesita asunto/recepción y elegibilidad segura; no Excel/documentos. |
| I04 | I03 | Agrupa hechos y preserva intervenciones ya existentes. |
| I05 | I04 | Cuenta hechos únicos con fuentes/papeles y gestión de atención. |
| I06 | I04 | Informe/derivación necesita composición del caso; puede adelantarse a I05. |
| I07 | I06 | Actos colectivos, derivación e informe/acta sobre la atención existente. |
| I08 | I04 | Nómina conserva identidad e historia compartida; puede realizarse antes de I05–I07. |
| I09 | I08, I06 | Extiende importación y permite consultar/seleccionar contexto en informe. |
| I10 | I05, I07, I09 | Aceptación completa verifica parámetros, membresía, año e importación. Cuentas/años pueden adelantarse tras I02; las pruebas dependientes se completan después. |
| I11 | I05, I07, I09 | Debe analizar todas las unidades y dimensiones disponibles; no requiere I10 si existen datos de prueba. |
| I12 | I11 | Añade denominadores y salida a resultados coherentes. |
| I13 | I12 | Verifica disposición/evidencia sobre todas las superficies operativas. |
| I14 | I13 | Recupera el conjunto e incorpora decisiones posteriores a la copia. |

No hay dependencia I13 → I14 → I13: I13 ejecuta y comprueba decisiones en el servicio actual y prepara constancias/copias; I14 verifica además su cumplimiento después de recuperar. Del mismo modo, disponer de una administración completa no es condición para ensayar roles/categorías ficticios; sí lo es para declarar autonomía de mantenimiento.

Tras I04 se pueden desarrollar de forma independiente reiteración, nómina de identidades y sustento de derivaciones, conservando sus controles comunes. Intervención formal y nómina tampoco requieren trabajar una sobre la otra. «Independiente» permite reordenar trabajo o asignarlo posteriormente a equipos distintos; no implica separar despliegues ni autoriza omitir sus pruebas de integración.

### 6.2 Refactorizaciones justificadas y riesgos de integración

| Momento | Evolución probable, sin imponer clases/tablas | Riesgo y comprobación que contiene la complejidad |
|---|---|---|
| I02 | Ampliar participantes y alcance de autorización del registro inicial | Confundir familiar/contacto o personal/cuenta; probar identificación parcial y restricción antes del aviso. |
| I03 | Coordinar una intención compuesta con datos ya registrados y pendientes reales | Convertir atención en trámites sucesivos o copiar acción inmediata; medir guardados y constancias únicas. |
| I04 | Incorporar fuentes vinculadas y composición común del hecho/personas | Perder participantes solo en duplicados, mezclar papeles o filtrar un excluido al agrupar; usar ejemplos completos de CU-05. |
| I05 | Compartir unidad de hecho/atribución y mantener señales ante correcciones/paso del tiempo | Contadores por reporte, reloj fijo o avisos repetidos; pruebas de ventana, novedad y recálculo. |
| I06/I07 | Añadir archivos/emisiones con permisos vigentes, recuperación y disposición | URL reutilizable tras exclusión, PDF inconsistente, archivo huérfano o retención de dato retirado; probar descarga actual y fallo/copia. |
| I08/I09 | Ampliar identidad/anualidad y confirmar lote/contexto coherentes | Previsualización obsoleta, unión de homónimos, campos ocultos a AIP revelados por diferencias; revalidar antes de confirmar y ensayar fallos parciales. |
| I10 | Sustituir precargas por mantenimiento con las mismas reglas | Bypass administrativo o configuraciones que alteran historia; probar otro colegio y fin de vigencia en sesión abierta. |
| I11/I12 | Resolver consultas analíticas y optimizar solo donde haya mediciones | Definiciones duplicadas de unidad/cobertura, caché con permiso antiguo o inferencia por desgloses; reconciliar todos los consumidores y revalidar fuentes. |
| I13 | Completar localización/disposición a partir del inventario real, incluidos derivados | «Borrado» solo visual, copia operativa olvidada o daño a terceros; comprobar datos/archivos alcanzados y fallos sin éxito falso. |
| I14 | Integrar recuperación y transferencia de la instalación completa | Copia sin archivos o restauración que reviva privilegios/datos; ensayo con constancias posteriores y técnico distinto. |

Los cambios internos son aceptables cuando simplifican esos recorridos o eliminan duplicación semántica. Se conservan pruebas de comportamiento y migraciones verificables. No se predeterminan agregados, herencia, eventos, repositorios genéricos, cachés o tablas de todos los incrementos para evitar una refactorización futura.

## 7. Verificación global y condiciones de producto terminado

### 7.1 Escenarios acumulativos de aceptación

Se ejecutan con datos ficticios y resultados conocidos, además de las pruebas de cada ficha. La aceptación registra versión, entorno, actor, datos/guion, resultado observado, evidencia y defectos; una falla explicada sigue siendo falla. Las evidencias no contienen datos reales. Un resultado preparado o una prueba unitaria no sustituyen la descarga/consulta posterior y su efecto persistido.

**G01 — Autenticación, permisos y conflicto en todo el recorrido.** Ejecutar capítulo 2 y CU-09 con docente, auxiliar, otro directivo, TOE, director, miembro docente/auxiliar, AIP, administrador, roles acumulados y usuario sin permiso. Probar inicio/cierre/recuperación, intentos repetidos frenados sin bloqueo global permanente, expiración de 20 min con advertencia y texto conservado sin exposición. Denunciar por separado director/TOE/otro miembro; identificar cuenta después; revocar membresía/cuenta con dos sesiones abiertas. Intentar lectura/cambio por referencia manipulada, duplicado, otro estudiante, historial, aviso, informe, acta, agregado, exportación y evidencia. Ningún permiso de autor o administrador supera exclusión/bloqueo. El resto atiende sin permiso del implicado; sin elegibles solo hay aviso de designación sin contenido. Testigo no se excluye por automatismo; retirar exclusión propia se deniega. Validar protección de tráfico, almacenamiento, rutas/secretos y controles de los componentes elegidos.

**G02 — Carga mínima de registro y atención.** Ejecutar CU-02/CU-03/CU-07 sobre hechos nuevos y ya atendidos: cotidiano con atribución visible/acción/compromiso opcional, sin TOE/caso/adjunto; intervención desde origen con conversación+resultado+compromiso+cierre y una confirmación final. Programación se prueba solo como alternativa real; completar la misma prevista, cancelar con causa, compromiso sin plazo sin vencimiento/cumplimiento inventados y seguimiento que sigue después del cierre. Probar corrección rutinaria, aclaración posterior, causa sustancial y anulación solo por error. Red caída, respuesta perdida, doble envío y dos ediciones no duplican ni sobrescriben. Fechas tardías no exigen trámite adicional y futuras reales se rechazan.

**G03 — Nómina, identidad y contexto del formato comprobado.** Ejecutar CU-01/CU-14 con réplica ficticia 121×17 y exactamente 91 estudiantes en las posiciones descritas. Verificar documentos textuales, encabezados estudiante/familiar, grupo B desconocido, omisión de orden/hermanos/instrucción/ocupación y no deducción de sexo/parentesco. Repetir, cambiar año, completar grupo, resolver homónimos/documento contradictorio y mantener identidad/historia. Otra hoja/columnas movidas, grado separado/variantes, fechas y fórmula guardada sin ejecución. Incorporar solo contexto admitido —incluido futuro bullying—, diferenciar «no»/vacío y localizar errores. Cancelación no cambia datos; vacío no borra, borrado explícito muestra alcance. Fallo/reintento/concurrencia no dejan fila incompleta. AIP nunca adquiere antecedentes previos ni acceso a casos por el lote. Inspeccionar temporales/logs/resultado para datos omitidos.

**G04 — Hecho único, fuentes y atención compartida.** Ejecutar todas las alternativas de CU-05/CU-06: Alfa en ambos aportes, Beta y Familiar F solo en duplicado, estudiante elegible solo en duplicado, discrepancia impeditiva y aclaración vigente, externos de identificación incierta. Comprobar fuentes/roles y un hecho en historial de cada estudiante, atención, reiteración, informe y estadísticas; no transcribir al principal, no elegir automáticamente papel ni unir externos ambiguos. Dos atenciones eligen continuación sin mover/copiar acciones; otra sigue por hechos propios. Corregir relación equivocada recupera dos hechos; corregir agrupación/último vínculo conserva historia y cierre por error sin contar atención efectiva resuelta. La reserva se propaga y reportantes conservan solo aportes propios.

**G05 — Señales, parámetros y tiempo.** Ejecutar CU-04 con fecha de prueba 20-10-2026, ventana de diez días/umbral tres ficticios: 11/15/20 cuentan y 10 queda fuera; avance de día sin nuevo registro, carga tardía y aproximación insuficiente. Separar cotidiana, presunta y corroborada; excluir afectado/testigo/desconocido/descartado/contexto/anulado y discrepancia impeditiva. Duplicado no añade hecho ni pierde estudiante presente solo allí. Gravedad/manual avisan aun sin parámetros/confirmaciones. Registrar atención/disposición/duplicidad gestiona el aviso en esa operación; leer o recalcular sin novedad no lo hace ni genera avisos. Nuevo hecho tras cierre puede producir novedad sin reabrir. Cambiar catálogo/umbral/ventana no altera historia/parametrización de emisiones conservadas.

**G06 — Comité, derivación, documento y referencias oficiales.** Ejecutar CU-08/CU-10/CU-11/CU-12 en caso de tres hechos y personas con papeles distintos. Convocatoria humana excepcional, miembros con facultades equivalentes y registro colectivo por director/sustituto real; acta externa posterior sin firmas obligatorias. Consolidado único con todos los campos RF-31, fuentes/participantes solo en duplicado, incertidumbre/contexto pertinente y emisión/corte. PDF de violencia a terceros solo iniciales y DNI disponible para estudiantes; ausencia explícita y referencia segura, sin nombres completos ni datos innecesarios. Acta PDF/.docx hasta 10 MB y sustitución motivada; formatos/excesos/contenido activo rechazados sin daño. Derivación preparada no ejecutada, Fiscalía familiar efectiva completa cierra localmente, Policía u otro supuesto no, mixto parcial no cierra todo. Seguimiento posterior conserva cierre. Origen/código SíseVe/Libro/protocolo histórico puede completarse sin certificar registro/cierre externo ni duplicar hecho. Verificar descarga/emisión contra datos persistidos y sin envío real.

**G07 — Exactitud analítica y salidas por finalidad.** Ejecutar CU-13 completo: 2 hechos/4 participaciones/3 estudiantes/1 caso; cambio de corroboración solo en denuncia; mapa lunes/miércoles; grupo/sexo/hora desconocidos y subtotales solapados. Incluir todas las medidas RF-34 y filtros RF-35 pertinentes, cruces categoría/grado y sexo/período con filtros adicionales. Comparar pantalla/tabla/gráfico/PDF/CSV o .xlsx contra el conjunto conocido. Base adicional de 20 estudiantes/2 distintos/3 hechos: 10 % y 15 hechos por cien, denominadores/período/redondeo visibles; sin denominador válido no calcula. Diferenciar anualidad poblacional y subperíodo de hechos. Dos personas siguen siendo cifra exacta interna; exterior exige finalidad/destinatario/legitimación y revisión institucional, sin regla de cinco, publicación o envío. Probar fórmula tabular inerte, corrección posterior y diferencia de cortes, ámbito excluido y descarga preparada antes de perder permiso, incluso fuentes que aporten cobertura.

**G08 — Trazabilidad, decisiones sobre datos y terceros.** Ejecutar CU-15 y reconstruir accesos/creaciones/cambios/lote/descargas/supresión de un caso ficticio inmediatamente mediante RF-38. Evidencia ≥2 años, protegida y sin copiar relatos/contenidos retirados; el implicado no consulta su caso por la bitácora. Una modificación sin evidencia recuperable no se declara exitosa. Preparar respuesta procedente de persona sin entregar por defecto terceros/actas/expediente y conservando reserva de violencia. Rectificar por importación/corrección, aplicar bloqueo según decisión y excepción, ejecutar disposición autorizada sobre todos los datos/copias operativas identificados, informes y archivos alcanzados; preservar terceros/retenciones. Verificar directamente el resultado, no solo UI. Conservar constancias y tratar destinatarios previos fuera de la aplicación según procedimiento. Copias de recuperación antiguas aisladas hasta vencer, sin edición selectiva; G10 prueba decisiones tras restaurar.

**G09 — Uso realista, accesibilidad, compatibilidad y rendimiento.** Ejecutar CU-17 con un docente, un auxiliar y un representante TOE, más un representante AIP que puede coincidir. Orientación ≤30 min; tareas propias sin ayuda, con relato breve proporcionado: nota ≤90 s, localizar estudiante/aporte ≤30 s, TOE conversación+compromiso+cierre desde alerta ≤3 min. AIP importa réplica/columnas movidas y resuelve u omite fila errónea con guía, comprende cambios/grupo/exclusiones, sin código ni ayuda del autor. Teclado, lector pertinente, foco, orden, etiquetas, errores/estados textuales, texto al 200 %, contraste ≥4,5:1 ordinario y ≥3:1 grande/interacción esencial; gráficos con tabla/valores, sin depender del color. Comprobar los criterios WCAG 2.2 enumerados por RNF-14, sin afirmar conformidad integral solo por automatización. Ensayar 320/1 280 px, dos versiones estables recientes de al menos dos familias de escritorio y navegador Android actual al aceptar; registrar versiones, sin extensiones/Excel/pago por puesto exigidos.

Para rendimiento, preparar 1 000 estudiantes activos, diez años, 10 000 vínculos, 20 000 incidencias y 5 000 casos, diez usuarios concurrentes, equipo de oficina, conexión ≥10 Mb/s y latencia ≤100 ms; si el colegio declara mayor volumen, probar también ese volumen. Al menos 20 mediciones por tarea frecuente, p95 de solicitud a resultado utilizable: búsqueda ≤2 s; guardado e historial por tramos ≤3 s; aviso en siguiente consulta ≤5 s; resumen ≤10 s; informe de hasta 50 incidencias ≤15 s; validar y confirmar cada fase de lote ≤2 min con 1 000 filas/30 columnas asociadas, excluyendo revisión humana/transferencia. Operaciones largas informan estado/límites sin truncar ni inventar porcentaje de avance. Registrar obstáculos/fallos, corregir y repetir lo afectado; ningún éxito con divulgación o sin persistencia se acepta.

**G10 — Continuidad, recuperación e incidentes.** Ejecutar CU-16 con copia anterior a baja/exclusión/disposición: pérdida principal/corrupción reciente, copia anterior íntegra, recuperación completa datos/archivos/relaciones/conteos. Pérdida ≤24 h de cambios confirmados; restauración ≤2 días hábiles desde medios disponibles e inicio técnico. Registrar copia, fecha, operador, integridad, resultado y tiempo; demostrar ensayo antes de uso real y programar por procedimiento repetición anual escolar/tras cambios que afecten recuperación. Antes de abrir acceso revisar y aplicar constancias posteriores, probando que no reviven cuentas/datos retirados. Verificar fracaso visible y continuidad de registro/consulta después de caída, con fechas efectivas y sin duplicidad. Medir registro/consulta autenticados ≥99 % mensual en jornada documentada; mantenimiento dentro cuenta como caída. La evidencia mensual debe ser efectiva, en ensayo operativo con datos ficticios o período de observación documentado: una estimación de disponibilidad no acredita el porcentaje. Procedimiento informa fallos al responsable operativo, contempla proveedor y evaluación/comunicaciones legales institucionales de incidentes según ERS, con evidencia ficticia, sin enviar comunicaciones de prueba reales ni asignar vigilancia a TOE/AIP.

**G11 — Autonomía, mantenibilidad, salida y sostenibilidad.** Ejecutar CU-14 y transferencia de CU-16: personal capacitado mantiene cuentas/recuperación/catálogo/años/columnas/aviso/alcance sin editar código ni depender del desarrollador; revisión semestral de privilegios con listado disponible. Otro técnico levanta copia ficticia, instala actualización, verifica datos/permisos, ensaya recuperar versión/información si falla, diagnostica y restaura usando documentación/medios autorizados. Entrega completa interpreta identidades/vínculos/roles/fechas/archivos en formatos documentados y reconcilia muestra, sin visor propietario inaccesible. Incluir requisitos de ejecución/puesta en marcha, variables, datos/relaciones efectivas, pruebas críticas reproducibles, limitaciones, copias/retenciones/responsables y credenciales por canal protegido. Inventario completo de servicios/licencias/recursos/renovaciones, control de cuentas y costos recurrentes estimados para 4.1, fijos/variables y efectos de crecimiento/cambio de responsable, sin techo inventado ni servicios ocultos. Inspeccionar materiales sin datos reales o secretos.

### 7.2 Definición de terminado de un incremento

Un incremento está terminado cuando su flujo funciona por UI con adaptadores reales y PostgreSQL, se vuelve a consultar lo guardado, sus criterios y pruebas positivas/negativas/de integración pasan, y los CU parciales muestran exactamente su alcance. Las superficies nuevas cumplen T01–T08; sus datos/archivos están cubiertos por evidencia, bloqueo/disposición y respaldo aplicables. Se conserva la regresión relevante, la actualización no destruye datos/permisos y la guía refleja lo que realmente se entrega. No quedan defectos que invaliden esos criterios ni operaciones simuladas declaradas completas. Se conserva una evidencia breve de aceptación funcional, distinta del informe unitario.

### 7.3 Puerta para declarar completo y apto para el uso previsto

El producto completo requiere simultáneamente:

1. Los catorce resultados aceptados; los 111 identificadores con cobertura completa y las verificaciones pertinentes de G01–G11 y los 17 CU aprobadas, incluidas alternativas y pruebas negativas.
2. Evidencia de seguridad revisable por otro técnico: sin defecto reproducible de acceso indebido, pérdida/alteración silenciosa de historia o exposición masiva; dependencias inventariadas/revisadas y hallazgos que afecten confidencialidad/integridad corregidos o funciones afectadas deshabilitadas antes de uso real. Deshabilitar una función requerida obliga a resolverla antes de declarar completo el producto.
3. Aceptación de usabilidad/accesibilidad/compatibilidad y todas las métricas de 4.1/RNF-16; restauración dentro de objetivos, disponibilidad medida y procedimientos/medios sostenibles, sin convertir fallos informados en aceptación.
4. Designaciones reales, responsables operativos/técnicos y medios de recuperación bajo control institucional; accesos mínimos verificados y cuentas de prueba fuera de la operación real. Credenciales reales por canal protegido.
5. Aviso institucional y alcance legítimo de datos disponibles; conservación/retenciones/suspensiones fundadas documentadas antes de datos reales; procedimiento de derechos/rectificación/disposición, custodia de exportaciones, incidentes y protección durante caídas. Inscripción del banco, documento de seguridad, ODP y demás deberes organizativos se asignan/verifican por la entidad cuando correspondan, sin presumir que el software los ejecuta o concede lectura general.
6. Catálogo/valores institucionales confirmados o comportamiento provisional de ERS 1.10 explícitamente aceptado por la institución: «por clasificar» y solicitud manual; reiteración inactiva donde falten parámetros; grupo/sexo no informados con cobertura. No se inventan datos/políticas para obtener una marca de completitud.
7. Entrega y mantenimiento demostrados por otro responsable, inventario de costos/recursos completo y salida recuperable reconciliada. Formación/guías disponibles y ensayo inicial de recuperación antes de tratar datos reales.

**Completo** significa que la capacidad especificada existe y ha sido aceptada; no que el colegio haya decidido todos los valores opcionales ni que cada estudiante tenga sexo/grupo. **Apto para datos reales** exige además las condiciones institucionales y operativas anteriores. Los valores de prueba, el pase de pruebas unitarias y la generación de un JAR no acreditan ninguna de esas dos declaraciones.

## 8. Auditoría final del plan

Esta auditoría revisa el documento, no declara implementado ni probado el producto. La comprobación documental automatizada contrastó los identificadores con los encabezados de la ERS: 111 filas, sin faltantes, duplicados ni identificadores desconocidos; 14 fichas con sus 13 campos; 14 entradas de dependencias sin referencias a incrementos posteriores y 11 escenarios globales. También verificó la consistencia de columnas de las tablas y la huella de la fuente. La revisión semántica retrasó la completitud de CU-07 hasta las facultades colectivas de I07 y de CU-08 hasta el acceso estadístico efectivo de I11, además de exigir su renovación operable en I10.

| Comprobación | Resultado y resolución incorporada |
|---|---|
| Inventario contra ERS | 41 RF + 28 RN + 25 RNF + 17 CU = **111 identificadores**; cada uno tiene fila individual con finalización y evidencia concreta. Ninguno termina solo con una mención o marca parcial. |
| Seis capítulos, no solo CU | Se incorporan contexto/matrícula/estados/límites y pendientes de capítulo 1; matriz y conflictos de capítulo 2; RF/RNF/RN completos y alternativas/criterios de capítulo 6. |
| Dependencias circulares | La tabla de dependencias solo referencia capacidades anteriores. Se separa ejecución de disposición I13 de su verificación restaurada I14; administración y datos precargados no crean dependencia circular. |
| Verticalidad | Cada ficha tiene actor, recorrido, UI o herramienta operativa pertinente, aplicación/reglas, adaptadores/persistencia, permisos y pruebas. I13 produce una decisión aplicada y comprobable; I14 recupera y reanuda tareas escolares reales. No son bloques de «todas las tablas», «todo el dominio» o «hacer pruebas al final». |
| Alcance razonable | Se separaron nómina identitaria I08/contexto I09 y análisis absoluto I11/denominadores-salidas I12. Atención sencilla conserva guardado conjunto; no se divide en apertura/programación/cierre forzados. |
| Duplicaciones evitadas | Un mismo importador se amplía; un caso contiene hechos y actas; informe consolidado recupera fuentes; atención referencia acciones previas; membresía y parámetros se mantienen una vez. Las verificaciones posteriores son regresión necesaria, no funcionalidades repetidas. |
| Particularidades de ERS 1.2 | Familiares/externos con papel propio y participantes solo en duplicados llegan automáticamente a historia/atención/informe/reiteración/análisis. Discrepancias no resueltas no crean responsabilidad elegible; reserva original y fuentes se mantienen. |
| Seguridad/integridad tempranas | T01–T08 son obligatorios por nueva superficie; conflicto comienza antes de la primera consulta institucional/aviso, y respaldo/evidencia/bloqueo/disposición cubren desde primeros datos. Consolidar en I13/I14 no autoriza diferir esos controles. |
| Diseño evolutivo | Se fijan tecnologías y dirección de dependencias, identidad/fuentes/estados/reserva; no hay modelo completo de dominio, esquema anticipado, catálogo de clases/interfaces/endpoints ni infraestructura distribuida. Refactorizaciones se justifican al aparecer consumidores/datos. |
| Falsas completitudes corregidas | Precarga no equivale a administración/importación; relacionar duplicados no completa consumidores aún ausentes; PDF no acredita envío; origen/cierre local no acredita cumplimiento oficial; copia creada no demuestra restauración; cifra de disponibilidad estimada no cumple medición. |
| Política institucional y exclusiones | Umbrales/ventana/catálogo/contexto/grupos/conservación/designaciones conservan responsables y comportamientos provisionales. Sin regla de tres, umbral de cinco, sanción automática, consentimiento universal, conservación perpetua o dos años para todo expediente. |
| Aceptación global | Los RNF cuantitativos, permisos entre superficies, consistencia de fuentes/conteos, derechos sobre derivados, recuperación posterior a decisiones y autonomía tienen ensayos G01–G11 adicionales a las pruebas por ficha. |

Si la ERS cambia, se registra su nueva versión/huella, se revisan capacidades y dependencias afectadas y se actualizan fichas, matrices y guiones antes de asignar el siguiente incremento. La evolución conserva lo ya aceptado salvo cambio de requisito explícito; no reactiva diseños anteriores como obligación implícita.
