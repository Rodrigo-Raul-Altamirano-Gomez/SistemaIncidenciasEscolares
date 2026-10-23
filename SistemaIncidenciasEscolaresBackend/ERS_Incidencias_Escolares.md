**Especificación de Requisitos de Software — Gestión de incidencias escolares**\
**Versión:** 1.2, correcciones puntuales · **Fecha y corte normativo:** 20 de octubre de 2026 · **Idioma:** español\
**Estado:** propuesta completa para revisión de aprobación previa al desarrollo.\
**Naturaleza:** especificación funcional y de calidad para implementar y aceptar el producto. Los ejemplos son ficticios.

# Capítulo 1. Descripción general

## 1.1 Propósito y criterio de interpretación

Esta ERS define íntegramente el comportamiento, la información, las autorizaciones y las condiciones de calidad de una aplicación web interna de incidencias escolares. Es el único documento de requisitos necesario para implementar el producto. Las fuentes sirven para justificar los requisitos; no es necesario consultarlas para descubrir campos, permisos o flujos omitidos.

«Debe» expresa una condición obligatoria. RF identifica capacidades funcionales; RNF, condiciones de calidad y operación; RN, reglas del dominio; CU, procesos y pruebas de aceptación. Las referencias entre estos identificadores son normativas dentro de esta ERS. Las enumeraciones de campos son necesidades de información, no diseños de almacenamiento ni de interfaz.

Se distinguen tres procedencias: decisiones confirmadas de la transferencia; decisiones de análisis que completan comportamientos convencionales; y restricciones derivadas de normas verificadas. Los valores de calidad formulados aquí son objetivos de aceptación de ingeniería, no cifras atribuidas al colegio ni a la ley. Las decisiones institucionales realmente pendientes se delimitan en 1.10.

## 1.2 Contexto, objetivos y alcance

Los registros actuales se dispersan en cuadernos de docentes y auxiliares. Esto dificulta reconstruir antecedentes, advertir reiteraciones, coordinar intervenciones y analizar la convivencia. Se requiere centralizar los hechos sin convertir toda anotación cotidiana en un expediente o una tarea de aprobación.

La aplicación debe permitir registrar un hecho con varias personas, consultar su historia escolar a lo largo de los años, advertir situaciones graves y reiteraciones, documentar atención institucional y generar información útil para decisiones humanas. Debe distinguir hechos comunicados, presuntas responsabilidades, afectación y protección. No determina culpabilidad ni sanciones.

Comprende usuarios internos, nómina e información contextual pertinente, incidencias, alertas internas, casos que agrupan hechos relacionados, actuaciones y compromisos, intervención del comité, derivaciones registradas, informes consolidados, actas adjuntas, referencias oficiales y estadísticas. Todo ello forma un proceso integrado; no se requieren sistemas independientes de expedientes, comité, derivaciones o documentos.

La unidad de utilización es un colegio por instalación o ámbito operativo. Debe poder adaptarse a otro colegio cambiando sus datos institucionales, años, grados/secciones, usuarios, categorías y parámetros simples. No se exige operación simultánea de múltiples instituciones ni administración empresarial.

## 1.3 Responsabilidades y límites

El colegio determina la finalidad y base jurídica del tratamiento, la exactitud de sus comunicaciones, las actuaciones de protección, las responsabilidades legales y los destinatarios de información. Confirma sus autoridades, comité y operadores. Ejecuta las comunicaciones urgentes, denuncias, registros oficiales y seguimiento que correspondan; atiende solicitudes de derechos y aprueba conservación y eliminación.

El software conserva y relaciona información, controla accesos, facilita el trabajo autorizado, muestra pendientes, produce salidas y aporta trazabilidad y recuperación. No sustituye la investigación de hechos, los servicios de salud, las decisiones disciplinarias, el Ministerio Público, la Policía, el CEM, la UGEL ni SíseVe. Tampoco verifica automáticamente que una declaración o una actuación registrada haya ocurrido.

El cierre de una **atención local** representa el término de la intervención local documentada. No certifica el cierre de un protocolo oficial ni de un procedimiento externo. La obligación institucional de protección puede continuar y debe poder documentarse sin alterar el cierre histórico.

## 1.4 Fuentes revisadas y formato de matrícula comprobado

La revisión 1.1 corrigió la ERS 1.0 existente, conservando sus requisitos válidos; esta versión 1.2 mantiene ese trabajo e incorpora las precisiones puntuales de participantes y reportes vinculados. En aquella revisión se leyeron íntegramente la ERS de origen, `Transferencia_Insumos_ERS_Incidencias_Escolares.md`, compilada el 20-10-2026, y el archivo correcto `MATRICULA 1°-2026 (1).xlsx`. El importador se fundamenta exclusivamente en esta matrícula y en sus variaciones razonables. No se utilizan modelos técnicos anteriores. El RI 2025 y el PDF de proyecto mencionados por la transferencia no se entregaron para esta revisión; sus contenidos ausentes no se presumen confirmados.

**Identidad y estructura comprobadas:** el libro de matrícula contiene una única hoja, `Hoja1`, de 121 filas y 17 columnas, sin fórmulas. Las filas 1–2 contienen el título; las filas 3–5, encabezados agrupados y combinados. Hay una etiqueta de etapa en la fila 6, un registro de estudiante en la 7, filas vacías 8–14 y noventa registros en las filas 15–104. Las filas 105–121 tienen solo numeración. Se identifican **91 registros de estudiantes con nombre y DNI**, sin DNI de estudiante repetidos en la muestra. La numeración se reinicia entre bloques y no identifica personas. La muestra es parcial y no permite deducir el tamaño total del colegio.

| Columnas y significado observado | Tratamiento requerido |
|---|---|
| A: número de orden | Omitir como identificador. Las etiquetas, filas vacías y filas con solo numeración no crean estudiantes. |
| B: grado/sección | Existe, pero está vacía en todos los registros de la muestra. Admitir columna combinada o columnas separadas en archivos futuros. AIP puede indicar un grupo común o por bloque si realmente lo conoce; lo no informado permanece desconocido y se completa después, sin adivinarlo por el archivo. |
| C: apellidos y nombres; D: DNI del estudiante | Identidad y conciliación. Los 91 DNI están almacenados como números de ocho cifras; deben conservarse como identificadores textuales. El nombre completo no debe dividirse por conjetura ni utilizarse como identificador único universal. |
| E: celular del estudiante | Opcional y escasamente informado. Incorporarlo solo si se justifica para contacto; no confundirlo con el teléfono del familiar. |
| F–G: hermanos en la institución, subencabezados P/S | Omitir: no se requiere gestión de hermanos. No son sexo del estudiante ni su grado/sección. |
| H: domicilio | Dato opcional y reservado, utilizable para contacto/atención si es necesario. |
| I: enfermedad; J: discapacidad | Contexto de salud especialmente protegido. Importación selectiva y justificada; no crear historias clínicas ni exigir documentos médicos adjuntos. «No» declarado y celda vacía tienen significados distintos. |
| K: con quién vive | Contexto familiar opcional cuando sea pertinente para protección. No permite inferir por sí solo quién es el apoderado ni una situación de riesgo. |
| L: nombre de padre/madre/apoderado; M: su DNI; N: su celular | L y N permiten disponer de un contacto familiar. La relación exacta queda no informada si la fuente no la precisa; el DNI del familiar se omite por defecto salvo necesidad documentada. Un mismo contacto/documento familiar para dos estudiantes no duplica al estudiante ni justifica unirlos. |
| O: grado de instrucción del familiar; P: ocupación | Omitir por no existir una necesidad confirmada de esos datos para incidencias. |
| Q: observación | Contenido libre y heterogéneo, con texto y valores numéricos. No convertirlo automáticamente en antecedente sensible, teléfono, falta o estadística. AIP incorpora solo información pertinente reconocida y asociada expresamente; una inconsistencia se corrige en la fuente o se omite. |

No hay columnas de sexo, fecha de nacimiento o bullying previo en esta muestra. El sistema admite esos datos pertinentes en nóminas futuras, sin inferirlos de nombres, hermanos o contexto familiar. Los antecedentes de bullying sufrido deben poder llegar desde columnas futuras admitidas que importe AIP. La presencia de salud, familiares y observaciones en el Excel no autoriza copiar todo su contenido.

El año se confirma para cada importación; el título puede ayudar a proponerlo, pero no sustituye la confirmación. La falta de grupo no debe impedir incorporar identidades válidas: se conserva el año y se identifica el grupo no informado, sin tareas para TOE ni datos inventados. Las estadísticas por grupo deben declarar su cobertura. Los límites de filas, letras de columna y cantidades anteriores describen la muestra para validarla; no son un formato rígido para archivos futuros.

## 1.5 Conceptos y estados del dominio

| Término | Significado obligatorio |
|---|---|
| Estudiante | Persona con identidad persistente, independiente de su año, grado, sección o continuidad de matrícula. |
| Matrícula o vínculo anual | Información de pertenencia a un año escolar y grupo; conserva los cambios fechados de grupo que se conozcan. No es un sistema integral de matrícula. |
| Incidencia o hecho | Suceso o situación comunicada, identificable una sola vez, aunque afecte a varias personas o llegue por varios canales. No supone falta probada. |
| Reporte o aporte | Comunicación de un usuario sobre el hecho. Varias comunicaciones pueden corresponder a una única incidencia; conservan autoría y visibilidad propias. |
| Participación | Papel individual de un estudiante, trabajador, familiar u otra persona externa pertinente en un hecho. Distingue conducta cotidiana atribuida por quien registra, presunta responsabilidad de una denuncia por confirmar, atribución corroborada durante atención, afectado, testigo, otro o papel desconocido. Atribución del autor y corroboración institucional no son equivalentes. |
| Antecedente contextual | Información previa pertinente a protección o atención, con procedencia y fecha disponible; no es una incidencia nueva ni una falta del estudiante. |
| Caso / atención local | Intervención que reúne una o más incidencias relacionadas. Sus únicos estados operativos son pendiente y cerrada. El encargado actual —TOE o comité— no constituye otro estado. |
| Actuación | Acción prevista o realizada, con autor, responsable y fechas pertinentes. Una prevista sigue pendiente hasta que se registre realización o motivo de no ejecución. |
| Compromiso | Acuerdo con contenido y persona que lo asume, vinculado a una actuación. Puede tener fecha objetivo y seguimiento cuando proceda. |
| Alerta | Señal por gravedad o reiteración de conductas/atribuciones registradas. Distingue la naturaleza de sus antecedentes; no es sanción ni atención abierta automáticamente. |
| Notificación | Aviso persistente dentro de la aplicación a usuarios autorizados; leerlo no acredita haber atendido el asunto. |
| Bandeja | Situaciones nuevas, atenciones pendientes y seguimientos realmente pendientes. Un asunto se representa una vez, con sus motivos relacionados; la historia cerrada sin trabajo pendiente queda fuera. |
| Informe | Salida consolidada generada a partir de los datos de un caso. |
| Acta | Documento del comité elaborado fuera del sistema y adjuntado al caso. |
| TOE / comité | Denominaciones funcionales del levantamiento, con las competencias descritas aquí. Su correspondencia con los cargos y órganos formalmente designados por el colegio debe documentarse institucionalmente. |

Registrar una falta cotidiana atribuida a un estudiante constituye la constancia de su autor y permite detectar reiteración sin corroboración posterior obligatoria de TOE. No equivale a una sanción ni a una confirmación investigada. En denuncias de violencia se mantienen las presuntas responsabilidades por confirmar, sin aplicarles esa atribución cotidiana por defecto. Un papel completamente desconocido es diferente de una atribución no corroborada. La representación precisa se define en RF-13.

## 1.6 Entorno, perspectiva y dependencias

El uso inicial es en secundaria peruana, por personal con experiencia digital heterogénea. Se prevén computadoras de escritorio y portátiles, y registro/consulta frecuente desde teléfonos; la importación masiva puede realizarse en computadora. Se requiere conectividad con el servicio; no se exige operación sin conexión ni una aplicación móvil instalada.

La información y fechas de trabajo se interpretan en la zona horaria de Perú, con año escolar explícito y fechas mostradas sin ambigüedad. El producto depende de que el colegio provea nóminas válidas, cuentas responsables, parámetros aprobados y medios operativos de respaldo. Una indisponibilidad no suspende la protección de estudiantes: el procedimiento institucional debe admitir registro posterior de lo actuado fuera de la aplicación.

## 1.7 Exclusiones

Se excluyen notas, asistencia general, matrícula administrativa completa, pagos, recursos humanos, atención clínica, gestión de procedimientos externos, sanciones automáticas, valoración automatizada de culpabilidad, un motor de protocolos o reglas arbitrarias, BI de consultas libres, mensajería externa, integración automática con SíseVe/SIAGIE, cuentas de estudiantes o familias, generación/edición de actas, firma digital obligatoria y videovigilancia. No se exige una tecnología, arquitectura, infraestructura o proveedor concreto.

La edición manual libre de antecedentes importados no forma parte del alcance confirmado. La rectificación debe poder resolverse mediante importación controlada o el procedimiento autorizado de RF-39, sin un módulo general de derechos. No se requieren fusión/reversión de expedientes, editor de políticas de conservación, registro electrónico de consentimientos ni panel avanzado de respaldo. Las ampliaciones futuras requieren una decisión de alcance, no se activan por aparecer una columna nueva.

## 1.8 Referencias normativas y estado verificado

Se contrastaron publicaciones oficiales con corte al 20-10-2026. Las referencias normativas siguientes sostienen únicamente las restricciones expresamente trasladadas a esta ERS.

| Referencia y enlace oficial | Estado y disposiciones pertinentes |
|---|---|
| [Ley 29733, texto consolidado difundido por SMV](https://www.smv.gob.pe/Uploads/Ley_29733_vigente_2025.pdf) | Vigente, con modificaciones, entre ellas las del D. Leg. 1353. Arts. 6–9, 13–14, 16–22 y 28: finalidad, proporcionalidad, calidad, seguridad, legitimación, confidencialidad y derechos. |
| [DS 016-2024-JUS, publicación oficial](https://www3.congreso.gob.pe/Docs/DGP/DIDP/files/ds_016-2024-jus.pdf) | Reglamento vigente desde el 31-03-2025; deroga DS 003-2013-JUS. Arts. 22–25: menores; 34–39: incidentes y ODP; 46–52: seguridad; 67–69 y 75–77: derechos. [Confirmación de entrada en vigor por MINJUSDH](https://www.gob.pe/institucion/minjus/noticias/1099347-minjusdh-realizo-conferencia-virtual-para-explicar-aspectos-de-la-ley-de-proteccion-de-datos-personales-y-su-nuevo-reglamento). |
| [Ley 29719](https://leyes.congreso.gob.pe/documentos/leyes/29719.pdf), con [Ley 31902](https://busquedas.elperuano.pe/dispositivo/NL/2226137-5) | Vigentes. Art. 11 de Ley 29719: Libro de Registro de Incidencias. Las modificaciones de Ley 31902 no justifican añadir psicología clínica ni cámaras al producto. |
| [DS 004-2018-MINEDU](https://busquedas.elperuano.pe/dispositivo/NL/1647264-6) | Vigente con modificaciones. Numerales 8.3.2–8.3.4 y anexo 05: atención, Libro confidencial y SíseVe. El anexo 03 aplicable a nuevas atenciones es el actualizado en 2025. |
| [DS 006-2021-MINEDU](https://busquedas.elperuano.pe/dispositivo/NL/1936151-2) | Segunda y tercera disposiciones complementarias modificatorias y cuarta final: adecuación de responsabilidades al Comité de Gestión del Bienestar y responsable de convivencia en instituciones públicas. No crean automáticamente cuentas o permisos para toda persona de ese órgano. |
| [RM 383-2025-MINEDU](https://busquedas.elperuano.pe/dispositivo/NL/2433909-1) y [anexo oficial MINEDU](https://cdn.www.gob.pe/uploads/document/file/8586343/7106134-rm-n-383-2025-minedu.pdf?v=1756831984), contrastado con [copia de UGEL Paruro](https://web.ugelparuro.gob.pe/wp-content/uploads/2025/09/RM-383-2025-MINUDU.pdf) | Vigente. Arts. 1–3: actualiza protocolos y deroga RM 274-2020, con continuidad de la norma anterior para casos SíseVe ya en trámite al entrar en vigor. Anexo 03, III.3.8: confidencialidad; IV.4.2: distinción conflicto/violencia; protocolo 7: entorno familiar/comunitario, seguimiento y cierre oficial. |
| [RD 100-2025-JUS/DGTAIPD](https://www.gob.pe/institucion/anpd/normas-legales/7574631-100-2025-jus-dgtaipd) | Directiva vigente sobre el Oficial de Datos Personales. Su designación y ámbito son organizativos; no impone un módulo de atención ni acceso indiscriminado a expedientes. |
| [RM 613-2026-MINEDU](https://busquedas.elperuano.pe/dispositivo/EX/2563534-1/pdf), publicada el 12-10-2026 | Publica un **proyecto** de lineamientos de disciplina para consulta. El proyecto no es regulación sustantiva vigente y no sustenta sanciones, umbrales ni flujos de esta ERS. |

La [cartilla ANPD de protección de datos durante las clases, marzo de 2026](https://www.gob.pe/institucion/anpd/informes-publicaciones/7913680-proteccion-de-datos-personales-durante-las-clases-escolares) es orientación. El RI 2025 mencionado en la transferencia es una referencia institucional provisional pendiente de actualización, no una ley. [WCAG 2.2](https://www.w3.org/TR/WCAG22/) se utiliza como referencia de aceptación de accesibilidad seleccionada por esta ERS, sin atribuirle por ello carácter de obligación legal peruana integral.

## 1.9 Traslado mínimo de obligaciones al producto

| Obligación y responsable | Consecuencia mínima especificada |
|---|---|
| El responsable institucional determina legitimación, información a titulares y pertinencia de datos; Ley 29733, arts. 6–8, 13–14 y 18. | Datos mínimos, campos opcionales controlados, aviso institucional y referencia al fundamento de campos contextuales; RF-06–RF-10, RF-40, RNF-05. No se exige consentimiento parental universal para registrar protección amparada por ley. |
| El colegio y su encargado protegen confidencialidad y derechos; Ley 29733, arts. 16–22 y 28. | Acceso restringido, datos localizables y corregibles, extracción necesaria y disposición autorizada; RF-28, RF-39, RNF-01–RNF-06. Solicitudes, decisiones y comunicaciones se tramitan institucionalmente fuera de la aplicación; no se exige un módulo de derechos. |
| El responsable gestiona privilegios y evidencias; DS 016-2024-JUS, art. 46. | Altas/bajas, revisión semestral y trazabilidad continua disponible inmediatamente, conservada al menos dos años; RF-01, RF-38, RNF-07. |
| El responsable asegura recuperación; DS 016-2024-JUS, arts. 48 y 51. | Respaldo protegido, integridad comprobada y recuperación demostrada; el mínimo legal es semanal con cambios. RNF-18 mantiene un objetivo diario para evitar perder una semana de registros, con ensayo anual y después de cambios que afecten recuperación, sin infraestructura o panel predeterminados. |
| La entidad gestiona brechas, ODP, inscripción del banco y documento de seguridad; DS 016-2024-JUS, arts. 34–39 y 42–47. | Documentación operativa y evidencias suficientes; RF-38, RF-41, RNF-20–RNF-21. Evaluar y efectuar comunicaciones legales corresponde a la entidad, sin integraciones obligatorias. |
| Dirección/responsable de convivencia cumplen Libro, SíseVe y protocolos; Ley 29719 art. 11; DS 004-2018, 8.3; RM 383-2025. | Datos y referencias que apoyan el registro y atención; RF-25–RF-33. Guardar aquí no acredita registro oficial ni cierra por sí solo un protocolo. |

No se prescribe certificación ISO, evaluación de impacto obligatoria para todo colegio, conservación perpetua, alojamiento exclusivamente nacional ni consentimiento por cada incidencia. El colegio debe evaluar proveedores, transferencias y la asignación del responsable/encargado según su organización. La aplicación debe permitir cumplir las decisiones válidas sin trasladar al implementador la definición de políticas escolares.

## 1.10 Decisiones institucionales pendientes y comportamiento mientras falten

| Decisión indispensable | Responsable de definirla y comportamiento exigido |
|---|---|
| Catálogo definitivo y gravedad, conforme al RI actualizado | Dirección con TOE. RF-03 admite su carga sin código. Mientras tanto se permite categoría provisional «por clasificar» y aviso manual inmediato por protección, sin inventar sanciones. |
| Duración común de ventana y umbral de cada categoría | Dirección con TOE. La reincidencia automática de una categoría no se activa sin parámetros expresos; se muestra esa ausencia a responsables. La gravedad y la derivación manual a atención siguen funcionando. |
| Campos contextuales sensibles concretos y fundamento de tratamiento | Responsable institucional. Solo se importan campos admitidos y pertinentes; columnas nuevas no aprobadas se omiten con aviso. Salud/discapacidad no se incorporan indiscriminadamente. |
| Información de grado/sección ausente en la muestra | AIP la obtiene de la nómina institucional cuando esté disponible; puede asignarla por lote/bloque o actualizarla después. Se conserva «no informado» mientras falte. No bloquea la importación de identidades válidas ni crea una tarea para TOE. |
| Plazos o criterios fundados de conservación por clase de información, restricciones archivísticas y suspensiones de eliminación | Responsable institucional, con asesoría competente. Debe documentarse antes de operación con datos reales. El producto no elimina por cambio de año ni aplica conservación indefinida predeterminada; RF-39 ejecuta decisiones expresas. |
| Designación nominal de operadores, comité y responsables de protección de datos | Colegio/entidad competente. Se registran antes de habilitar cuentas y tratamiento real. No son nuevos cargos creados por el software. |

No bloquean la especificación ni requieren nuevas decisiones institucionales: método de presentación del informe, validaciones de entrada, manejo de duplicados, asociación de columnas, recuperación ante fallos o forma técnica de aplicar permisos. Sus resultados se definen a continuación. No se requiere una plantilla institucional de informe para implementar su contenido mínimo.

# Capítulo 2. Actores y permisos

## 2.1 Actores y responsabilidades

**Docente y auxiliar:** registran hechos y actuaciones/compromisos cotidianos inmediatos. Consultan sus propios aportes; no reciben acceso a la historia institucional ni a actuaciones reservadas por haber reportado un hecho. No aprueban ni atienden por cuenta propia situaciones graves bajo su rol ordinario.

**Directivo distinto del director:** tiene la misma capacidad ordinaria de registro y consulta de aportes propios. El rótulo «directivo» no concede recepción general, atención o estadísticas. Una persona puede recibir otro rol por designación formal.

**TOE:** receptor y responsable habitual de las situaciones que requieren intervención. Consulta institucionalmente, aclara participaciones que requieren investigación, relaciona incidencias, documenta atención, convoca al comité, deriva y cierra. No aprueba ni confirma individualmente cada nota cotidiana. Puede registrar una intervención realizada y su cierre en una misma operación.

**Integrante estable del comité:** en el colegio de referencia son director, un docente y un auxiliar. La pertenencia añade capacidades institucionales al menos equivalentes a TOE en consulta, atención y estadísticas; no está limitada a leer únicamente casos convocados. La obligación de intervenir en un caso concreto comienza por convocatoria o por sustitución ante conflicto. La pertenencia por sí sola no transfiere la atención ordinaria de TOE.

**Director:** tiene las capacidades institucionales anteriores y registra ordinariamente decisiones, acuerdos, actuaciones y derivaciones cuando interviene formalmente el comité. Esto no lo convierte en aprobador universal. Los demás integrantes ejercen las facultades ampliadas por su membresía vigente; el sistema conoce esa condición y no exige declararla en cada operación. TOE sigue siendo receptor habitual. La equivalencia de consulta y atención no elimina las responsabilidades específicas de convocatoria, registro de acuerdos y configuración indicadas en la matriz.

**AIP:** importa/actualiza la nómina, incluidos antecedentes admitidos, y resuelve errores del lote. Puede ver datos necesarios de los registros a conciliar y los antecedentes aportados por ese lote. No obtiene acceso general a casos, historial ni antecedentes anteriores por operar la importación.

**Administrador operativo designado:** mantiene cuentas, designaciones, comité y años/grupos; puede recaer en personal existente, sin crear un nuevo cargo. No recibe por ello lectura de expedientes. La configuración institucional aprobada corresponde a TOE/director; su carga material puede delegarse al administrador, con autoría registrada, sin un flujo de aprobación por cambio. La operación técnica de recuperación puede recaer en un técnico autorizado y no se asigna por defecto a TOE o a AIP.

**Responsable institucional del tratamiento:** autoridad real que decide solicitudes de datos, conservación y autorizaciones de soporte. Puede coincidir con Dirección o estar en la entidad competente. Sus procedimientos administrativos y constancias pueden mantenerse fuera de la aplicación. No se exige crearle otro rol con lectura irrestricta; autoriza las operaciones delimitadas de RF-39 y las revisiones pertinentes.

Estudiantes, familiares, trabajadores y otras personas externas pueden ser participantes individuales de una incidencia, incluso presuntos responsables por confirmar, sin convertirse en usuarios. Las entidades externas y SíseVe pueden ser destinos/orígenes del proceso; no necesitan cuentas. Registrar a una persona externa utiliza los mismos permisos de registro de incidencias y reserva de consulta, sin concederle acceso al sistema ni crear otro rol o padrón.

## 2.2 Matriz de permisos

Sí = capacidad autorizada en el ámbito indicado. No = no disponible por ese rol. «Propio» se refiere a aporte del usuario, no a todo el expediente que posteriormente lo incluye. Todas las celdas se subordinan a exclusión por conflicto y a bloqueo jurídico.

| Operación | Docente / auxiliar / otro directivo ordinario | TOE | Miembro del comité distinto del director | Director | AIP | Administrador operativo sin otro rol |
|---|---|---|---|---|---|---|
| Buscar estudiante para registrar | Sí, identidad y grupo mínimos | Sí | Sí | Sí | Sí, para nómina/lote | No por defecto |
| Registrar incidencia con estudiantes, personal, familiares u otras personas externas | Sí | Sí | Sí | Sí | No | No |
| Documentar acción cotidiana inmediata | Sí, en aporte propio no grave | Sí | Sí | Sí | No | No |
| Consultar aporte propio | Sí | Sí | Sí | Sí | No | No |
| Consultar incidencias institucionales e historial | No | Sí | Sí | Sí | No | No |
| Consultar antecedentes contextuales y contactos pertinentes | No | Sí | Sí | Sí | Solo lote y conciliación autorizada | No |
| Aclarar roles, corregir hechos e identificar duplicados | Puede corregir aporte propio según RF-17; no confirma denuncias institucionalmente | Sí | Sí | Sí | No | No |
| Bandeja institucional, abrir/agrupar/atender/cerrar | No | Sí, responsable ordinario | Sí; sin asumir automáticamente casos de TOE | Sí; sin aprobación universal | No | No |
| Convocatoria ordinaria del comité | No | Sí | Solo sustitución por conflicto de TOE | Solo sustitución por conflicto de TOE | No | No |
| Leer actuaciones reservadas, informes y actas | No | Sí | Sí | Sí | No | No |
| Registrar acuerdos/actuaciones/derivación de intervención formal del comité | No | No por el solo rol TOE | Solo sustitución de director excluido o no vigente | Sí, autor ordinario | No | No |
| Documentar atención ordinaria y generar informe | No | Sí | Sí | Sí | No | No |
| Consultar estadísticas institucionales | No | Sí | Sí | Sí | No | No |
| Importar nómina y contexto admitido | No | No por el solo rol | No por el solo rol | No por el solo rol | Sí | No por el solo rol |
| Definir catálogo/parámetros institucionales y registrar sus valores | No | Sí, conforme a decisión institucional | Consulta los vigentes; la configuración no es capacidad de atención | Sí, conforme a decisión institucional | No por el solo rol | Solo carga delegada de valores definidos; no decide políticas |
| Excluir usuario por conflicto de un hecho/caso | No; comunica el conflicto | Sí, si no está excluido | Sí, si no está excluido | Sí, si no está excluido | No | No puede retirar/burlar exclusión |
| Administrar cuentas, designaciones y años/grupos | No | Solo si designado administrador | Solo si designado administrador | Solo si designado administrador | Solo si designado administrador | Sí, con evidencia de designación |
| Extraer datos nominativos de atención | No | Sí, finalidad institucional y alcance propio | Sí, misma restricción | Sí, misma restricción | Solo resultados de su importación | No |
| Ejecutar respaldo/restauración o una decisión excepcional sobre datos | No por el rol | No es tarea ordinaria de TOE | No por la membresía | Autoriza si es responsable competente; no debe ejecutarlo personalmente | No por el rol AIP | Según autorización específica y procedimiento; puede hacerlo soporte técnico sin acceso general |

## 2.3 Alcance, acumulación y conflictos

Los permisos de roles vigentes se acumulan. Una membresía docente/auxiliar en el comité debe habilitar las capacidades institucionales anteriores durante su vigencia, sin crear otra identidad de usuario. Terminar la designación retira esas capacidades y conserva autorías históricas.

La exclusión por conflicto prevalece sobre todo rol, autoría, permiso administrativo y acceso por estudiante relacionado. Afecta al hecho/caso restringido, sus aportes, actuaciones, alertas, informes, adjuntos y salidas derivadas. Un usuario excluido puede consultar otros casos del mismo estudiante si tiene permiso y no revelan el caso restringido. No se expondrán sus totales particulares mediante filtros que permitan reconstruirlo.

Registrar como presunto denunciado a un usuario con acceso institucional debe activar la restricción preventiva del hecho antes de emitir notificaciones. Un papel de testigo no equivale automáticamente a conflicto; el responsable no implicado puede registrar el conflicto explícito con motivo. Identificar al personal de forma inequívoca respecto de sus cuentas es necesario para este control. Si la vinculación se aclara después, la restricción se aplica desde esa identificación.

Cuando se excluye a director o TOE, los integrantes restantes no implicados asumen la atención sin consentimiento del excluido. Si el director queda excluido, uno de los integrantes restantes documenta como sustituto, con autoría real y motivo. No se modifica la composición estable del comité para atender el caso. Si no queda ningún operador elegible, el sistema conserva el registro protegido y comunica al administrador únicamente la necesidad de una designación institucional habilitante, sin revelar contenido; la decisión y actuaciones urgentes corresponden al colegio.

La revocación de una exclusión requiere una decisión motivada de un responsable institucional no implicado. Ningún usuario puede retirarse su propia exclusión, concederse un rol institucional o usar una restauración como forma de eludirla. Los derechos de defensa del denunciado se atienden por los canales competentes, no mediante acceso automático al expediente reservado.

Las facultades equivalentes del comité comprenden lectura institucional, atención de cualquier gravedad, aclaración de participaciones, actuaciones, derivación, cierre, informes y estadísticas, siempre sin conflicto. TOE mantiene la responsabilidad habitual de recepción y agrupación y la convocatoria ordinaria. El director mantiene la autoría de los actos colectivos. Esas asignaciones de responsabilidad y la configuración no constituyen una jerarquía de acceso superior a los demás miembros; las sustituciones se aplican solo en los supuestos expresamente previstos. No se requiere autorización adicional por caso para consultar en virtud de una membresía vigente.

## 2.4 Reserva y administración proporcionada

Todos los datos de atención son internos. Las actuaciones posteriores de TOE/comité y los antecedentes contextuales son reservados para actores institucionales autorizados. No se requiere que el operador clasifique manualmente cada nota como «pública» o «privada»: se aplica la reserva de su contexto y la matriz. Los campos de identidad visibles en búsquedas ordinarias se limitan a nombre, identificador escolar no sensible cuando exista y grupo; no se exponen contactos, DNI completo o señales de violencia.

Las exportaciones, vistas de historial, búsqueda, notificaciones, adjuntos y estadísticas deben aplicar los mismos permisos que la consulta directa. AIP no podrá navegar a antecedentes anteriores mediante el historial del lote. La administración técnica debe operar con información de estado y metadatos suficientes; cualquier soporte con datos reales requiere alcance y autorización institucional documentados y trazables.

# Capítulo 3. Requisitos funcionales

## 3.1 Acceso, institución y clasificación

### RF-01. Cuentas y designaciones

El sistema debe permitir al administrador designado dar de alta usuarios individuales, registrar nombre laboral, identificador de acceso y roles autorizados; desactivar cuentas; gestionar recuperación de acceso; y registrar vigencia de TOE y miembros del comité. Debe conservar autorías tras una baja y retirar permisos institucionales al terminar una designación. La composición vigente del comité se mantiene por período institucional, sin volver a crearla por caso. Las designaciones corresponden al colegio: el sistema registra su vigencia, operador y fecha, sin exigir archivos o un texto justificativo por cada mantenimiento ordinario. Debe ofrecer un listado de cuentas, roles y vigencias para revisión semestral de privilegios. No debe permitir autoatribución de roles ni cuentas compartidas.

### RF-02. Datos institucionales y años escolares

Debe permitir mantener nombre y datos institucionales necesarios para informes, año de trabajo, grados/secciones y años históricos, sin fijarlos al colegio de referencia. Crear el año siguiente no promueve estudiantes, borra pendientes ni cierra casos. AIP puede actualizar o corregir una nómina histórica seleccionando expresamente su año y revisando el resultado, sin un procedimiento adicional de cierre/desbloqueo anual. Debe permitir registrar tardíamente un hecho y conservar su fecha y grupo histórico. El año de trabajo debe estar identificado en operaciones y filtros que dependan de él.

### RF-03. Catálogo y parámetros simples

Debe mantenerse un catálogo sencillo de categorías con nombre, descripción breve, naturaleza —conducta disciplinaria, violencia/protección u otra convivencia—, nivel leve/grave/muy grave o por clasificar, aviso inmediato y umbral de reiteración cuando corresponda. La categoría recupera su naturaleza/gravedad y comportamiento habitual sin pedir al registrador que los clasifique de nuevo. Las de violencia/protección y las graves/muy graves avisan desde el primer registro. Cada categoría puede admitir o no reiteración y tiene su propio umbral; todas comparten una ventana de días. TOE/director registran los valores institucionales o delegan su carga conforme al capítulo 2. No se exige aprobar cada cambio dentro de la aplicación.

Se conservan autor/fecha de cambios y parámetros de señales emitidas. Desactivar una categoría impide nuevos usos sin ocultar historia; los cambios no reclasifican retroactivamente hechos. No se admiten sanciones, fórmulas arbitrarias ni flujos configurables. Debe mostrarse si falta un parámetro de 1.10, sin suplirlo por un valor inventado.

## 3.2 Personas, nómina y contexto

### RF-04. Localización e identificación de estudiantes

Los registradores deben poder buscar estudiantes por fragmentos de nombres/apellidos, por identificador disponible y por año/grado/sección. La búsqueda debe tolerar diferencias de mayúsculas, espacios y tildes sin cambiar el nombre conservado. Debe distinguir homónimos mediante datos escolares y los identificadores que el usuario esté autorizado a ver. Debe permitir localizar estudiantes de años anteriores o sin matrícula vigente cuando resulte necesario para un hecho. No debe mostrar sus antecedentes, historial, contactos ni señales de atención en la búsqueda ordinaria. Los operadores institucionales pueden acceder desde la selección al historial autorizado.

### RF-05. Personal y otras personas pertinentes

Debe permitir identificar personal del colegio involucrado mediante nombre y función laboral, con relación inequívoca con su cuenta si la tiene. No se exige una nómina de recursos humanos ni DNI de todo trabajador. El registrador podrá indicar una persona todavía no identificada por completo con la información conocida, sin inventar una cuenta. TOE/comité podrá completar su identificación. También debe permitir registrar como participantes del hecho a familiares o cualquier otra persona externa pertinente, como afectada, testigo, presunta responsable por confirmar u otro papel individual según RF-13, no solo como comunicante o contacto. Basta el nombre, referencia o descripción mínima conocida y, si se conoce y resulta pertinente, su relación con el estudiante; contacto y documento son opcionales. No se exige DNI, cuenta de usuario o identificación completa para registrar oportunamente, ni se inventa lo desconocido. La identificación puede precisarse después en la misma incidencia. Estos datos quedan vinculados al hecho, atención e informe, con su reserva. No se exige registrar a todos los familiares ni mantener un padrón general externo, módulo de familiares o gestión adicional de denuncias.

### RF-06. Información escolar suficiente

Debe conservar identidad persistente del estudiante, nombre completo sin separación artificial de apellidos, documento de identidad con su tipo cuando exista y vínculo con año escolar y grado/sección conocidos. Un grupo no informado se distingue del actual o histórico conocido; puede completarse por AIP sin volver a crear al estudiante. Debe admitir fecha de nacimiento, sexo informado, situación de matrícula y fecha efectiva de cambio de grupo cuando se suministren legítimamente, sin hacerlos obligatorios. Orden del archivo y grupo no identifican a la persona.

Para atención debe admitir nombre del contacto familiar/apoderado, relación si se conoce, teléfono y domicilio pertinente. El teléfono del estudiante es opcional y distinto del familiar. Un nombre en la columna «padre/madre/apoderado» no acredita una relación específica ni representación legal; si no se precisa, se conserva como contacto familiar de relación no informada. El DNI del familiar solo se incorpora con necesidad documentada; no es necesario para disponer del contacto. No se exige registrar ambos progenitores, instrucción, ocupación, ingresos o hermanos. Los datos adicionales siguen RF-40. Identificadores y teléfonos se conservan como texto; no se inventan ceros/dígitos perdidos. Ausente significa desconocido y no impide registrar protección.

### RF-07. Antecedentes contextuales importados

Debe permitir que AIP importe contexto pertinente desde columnas expresamente seleccionadas: salud/discapacidad o convivencia familiar cuando estén justificados y, en futuras nóminas, bullying sufrido u otros antecedentes admitidos. Cada antecedente conserva estudiante, etiqueta comprensible, contenido, procedencia del lote, fecha de incorporación y fecha/período del antecedente si se conoce. Se distingue una negación declarada —por ejemplo «no»— de ausencia o desconocimiento. No se convierte una expresión libre en diagnóstico ni hecho probado.

Una columna contextual nueva puede asociarse a un antecedente etiquetado sin programar un módulo nuevo ni definir un esquema de campos a medida. El alcance permitido se documenta institucionalmente conforme a RF-40; no se exige una aprobación electrónica por celda. Observaciones heterogéneas no se importan indiscriminadamente ni se transforman automáticamente en teléfono, responsabilidad o variable estadística. La consulta longitudinal corresponde a TOE/comité no excluidos; AIP ve su lote y lo necesario para conciliarlo, sin acceso a atenciones previas. Estos antecedentes no cuentan como incidencias nuevas ni reincidencia disciplinaria.

### RF-08. Selección y asociación de un Excel

AIP debe poder importar `.xlsx`, elegir la hoja si es necesario y revisar una asociación propuesta de encabezados a RF-06/RF-07. Debe soportar títulos/celdas combinadas, encabezados en varias filas, columnas reordenadas/añadidas, filas vacías y etiquetas o filas con solo numeración, sin exigir limpiar rutinariamente el archivo. En el formato entregado debe reconocer el contexto de los encabezados para distinguir DNI/nombre/celular del estudiante de los del familiar. Los subencabezados P/S de hermanos no son sexo. Una asociación ya revisada se reutiliza en archivos equivalentes; AIP solo interviene en diferencias o ambigüedades.

El año se confirma por lote. Debe admitirse grado/sección en una columna combinada o dos separadas, con reconocimiento de variantes ordinarias como «1° A», «1 A» o «Primero A» y revisión de cualquier ambigüedad. AIP puede proporcionar un grupo común para un lote/bloque cuando corresponda. Si la fuente y AIP no lo informan, se cargan identidad y año con grupo no informado, sin inventarlo ni convertir la carga en trabajo de TOE. Un valor por defecto no sustituye silenciosamente un grupo ya conocido.

Columnas de orden, hermanos, instrucción/ocupación del familiar y datos no admitidos se omiten. Las desconocidas se muestran como omitidas hasta asociación pertinente. AIP puede ajustar rangos/asociaciones o excluir filas cuando haga falta; no debe configurarlos nuevamente para cada archivo del mismo formato. No se exige OCR, interpretar formatos arbitrarios ni ejecutar fórmulas/macros.

### RF-09. Previsualización y validación de importación

Antes de una única confirmación del lote debe mostrar hoja, año, asociaciones, cantidades a crear/actualizar, filas omitidas/erróneas y los cambios relevantes. Los mensajes localizan hoja/fila/campo y ofrecen corrección comprensible, sin exponer datos ajenos al lote. Para crear identidad son necesarios nombre de estudiante y año; el documento es preferente para conciliar, pero no condición universal. Grupo ausente queda informado como falta de cobertura, sin rechazar por ello a los estudiantes de esta muestra.

Debe validar formato de documentos suministrados —DNI: ocho dígitos—, grupos informados, repeticiones de estudiantes y coincidencias contradictorias. Los DNI numéricos completos de la muestra se convierten a texto sin alterar cifras; si faltan dígitos no se completan por conjetura. Un documento de familiar o teléfono compartidos no se consideran duplicados del estudiante. Las celdas desconocidas o datos opcionales no válidos pueden omitirse de manera explícita sin sustituir información válida previa; si falta identidad segura para actualizar, la fila queda excluida hasta resolverla.

En futuras columnas de fecha se admiten valores Excel o texto inequívoco. Si aparece una fórmula seleccionada, solo se admite resultado guardado válido, sin ejecutarla; un error se localiza. No se obliga a calcular o reparar el archivo. AIP puede excluir filas, corregir fuente/asociación y volver a validar, o cancelar sin cambios. No se importan una etiqueta de etapa ni filas de orden vacías como personas.

### RF-10. Confirmación, resultados y rectificación del lote

Solo una confirmación explícita de AIP debe incorporar las filas aceptables. El resultado debe indicar altas, actualizaciones, registros sin cambios, filas excluidas y errores, con autor y fecha. Un fallo no debe dejar una fila con identidad actualizada y contexto/matrícula incompletos ni mostrar éxito de operaciones no guardadas. Debe poder reintentarse el lote sin duplicar estudiantes, matrículas o antecedentes ya incorporados.

Las celdas vacías y columnas omitidas no borran valores existentes. Los valores nuevos válidos pueden actualizar los campos asociados al confirmar el lote, sin motivo separado por cada cambio. Un borrado debe solicitarse expresamente y mostrar su alcance antes de confirmar. Si AIP no puede consultar un antecedente previo, conoce que se sustituirá y su procedencia, sin ver su contenido. Rectificaciones por derechos/conservación siguen RF-39. Se conservan autor, fecha, resultado y procedencia del lote; no se exige conservar el archivo original con columnas omitidas o datos innecesarios.

### RF-11. Conciliación de identidad y continuidad anual

Un documento de estudiante válido y único, sin contradicción relevante, permite actualizar la misma identidad al confirmar el lote, también entre años, sin revisar manualmente cada coincidencia correcta. Si hay contradicción o varios candidatos, se requiere conciliación solo de esas filas. Sin documento, nombre/grupo pueden proponer candidatos, pero no unir automáticamente personas entre años: AIP confirma usando información institucional o deja la fila sin incorporar hasta aclararla. No se elige el primer candidato ni se usan documento/teléfono del familiar como identidad del estudiante.

La carga de la muestra permite crear 91 estudiantes por sus DNI, conservando el grupo no informado o el que AIP asigne válidamente. Repetirla no duplica identidades/vínculos/contexto. Un estudiante sin documento puede tener identidad interna y ser relacionado después con el documento correcto. Cambiar nombre, documento o grupo no rompe su historia; las incidencias conservan el grupo del momento conocido. La ausencia en un lote no produce baja, retiro ni eliminación. Un error de identificación se corrige por operación autorizada y delimitada, preservando los registros correspondientes; no se exige una herramienta universal de fusión/descomposición de identidades.

## 3.3 Registro, consulta y corrección de hechos

### RF-12. Registro oportuno de una incidencia

Docentes, auxiliares, directivos, TOE y comité deben poder guardar un hecho con fecha del hecho, categoría o «por clasificar», descripción objetiva suficiente y personas involucradas. Autor, identificador, fecha/hora de registro y grupo conocido se recuperan automáticamente; la fecha del hecho puede proponerse como hoy y corregirse. La categoría aporta naturaleza/gravedad habituales. Para una conducta cotidiana, identificar al estudiante al que se atribuye forma parte del registro y no exige confirmación posterior de TOE; se aplica RF-13. Hora, lugar, comunicante y canal son opcionales si se conocen. Fecha de conocimiento/registro puede diferir del hecho; se admite fecha aproximada explícita sin inventar hora.

Debe admitir comunicación no verificada y personas presuntamente involucradas, incluidos trabajadores, familiares y otras personas externas, sin esperar identificación completa o confirmación de todos los roles. Se registran con los datos mínimos de RF-05 y papel individual de RF-13; un familiar presuntamente responsable no convierte al estudiante afectado en autor de una falta. Si una comunicación urgente aún no permite identificar al estudiante, debe poder registrarse una referencia provisional descriptiva y completarse después por atención institucional; nunca se crea un estudiante ficticio para sortear una validación. Para situaciones graves se puede consignar una acción urgente de protección realmente ejecutada o comunicada; esto no autoriza al rol ordinario a decidir la atención formal. Guardar crea el hecho y las alertas aplicables, sin abrir un caso automáticamente.

### RF-13. Participaciones individuales

Debe admitir en una incidencia varios estudiantes, trabajadores, familiares y otras personas externas pertinentes, cada uno con su papel individual; el grupo escolar solo corresponde al estudiante y, si se conoce, se conserva el del momento del hecho separado del actual. La identificación incompleta de una persona externa no impide registrar su participación ni la convierte en otra persona ya conocida por mera coincidencia de nombre o parentesco. Debe distinguir, con términos comprensibles, estas situaciones:

- **Conducta cotidiana atribuida por quien registra:** solo en una anotación disciplinaria cotidiana no grave, al registrar la conducta de un estudiante, esa selección y guardado ya constituyen atribución del autor. Puede proponerse ese papel para el estudiante objeto de la anotación disciplinaria, siempre visible y corregible antes de guardar. No se etiqueta como corroborada por TOE ni genera tarea de confirmación. Cuenta para reiteración de esa categoría.
- **Presunta responsabilidad por confirmar:** se identifica a quien se atribuye una denuncia o hecho aún por corroborar. En violencia/protección no se propone responsabilidad cotidiana ni corroboración automática. Puede sustentar una señal de reportes reiterados de su categoría, rotulada como no corroborada; la gravedad avisa independientemente.
- **Atribución corroborada durante la atención:** TOE/comité registra la aclaración cuando realmente la efectúe. No equivale a sanción o culpabilidad judicial.
- **Afectado, testigo, otro interviniente o papel desconocido/por confirmar:** no se incluye en el contador de responsabilidades por la mera participación. Se puede señalar después que una atribución fue descartada.

No se requiere que el usuario mantenga dos campos de confirmación en cada nota cotidiana: basta que el resultado conserve esas distinciones. Añadir otro involucrado no le atribuye automáticamente la misma conducta; debe identificarse su papel o dejarlo desconocido. Ninguna confirmación se propaga por estudiante o caso a otros hechos. No se duplica una persona identificada en un hecho aunque aparezca en varios reportes vinculados: se aplican la conservación de papeles y procedencia de RF-18. Un papel comunicado respecto de una persona nunca se hereda por otra por compartir hecho, parentesco o reporte. Los controles de conflicto se aplican a personal denunciado identificado según RF-28.

### RF-14. Actuación o compromiso inmediato sin caso

En un reporte cotidiano no grave, su autor debe poder añadir, dentro de la misma operación, una decisión o actuación realizada y un compromiso inmediato si lo hubo. Registra contenido, persona que asume el compromiso y fecha realizada; autor y registro se obtienen automáticamente. Fecha objetivo y resultado adicional se piden solo si son pertinentes. No son obligatorios para guardar una nota cotidiana y no exigen abrir atención, adjuntar documentos ni aprobar por TOE. Si después se abre un caso, estas constancias deben estar relacionadas y visibles para atención sin copiarlas como nuevas actuaciones.

### RF-15. Consulta del propio registro

El rol ordinario debe poder localizar sus aportes por estudiante, intervalo de fechas, categoría y año, y leer lo que registró y sus rectificaciones permitidas. Puede conocer que su reporte fue recibido o relacionado para atención, sin exponer contenido reservado, otros hechos del estudiante ni los acuerdos posteriores. Si su aporte se relacionó como duplicado de otro, sigue viendo solo su comunicación y datos propios, sin recibir acceso al aporte principal ajeno. La autoría de un aporte no habilita descarga del informe del caso, sus actas ni acceso por enlaces indirectos.

### RF-16. Consulta institucional y trayectoria escolar

TOE/comité no excluidos deben poder consultar incidencias por fechas de hecho y registro, categoría, gravedad, estudiante u otra persona involucrada con los datos conocidos, año/grupo histórico y existencia de atención. La consulta de participación de familiares/personas externas se realiza sobre los hechos y sus atenciones, sin requerir una ficha o historial externo independiente ni agrupar automáticamente personas de identificación incierta. Desde el historial de un estudiante deben poder revisar cronológicamente hechos, su rol y confirmación, atenciones relacionadas, actuaciones, compromisos, derivaciones y antecedentes contextuales, con su procedencia.

La misma incidencia compartida debe aparecer una sola vez en el historial escolar de cada estudiante involucrado, aunque su participación conste únicamente en un reporte identificado como duplicado; la consulta del hecho y de su atención debe recuperar también los demás participantes pertinentes de todos los reportes vinculados, sin copiarlos ni mezclar sus papeles. Se conserva la procedencia de cada información según RF-18. Las consultas por otras personas muestran sus participaciones conocidas con la misma reserva, sin crear un padrón externo. Los cambios de grado, sección, año o matrícula no deben ocultar antecedentes. La consulta debe distinguir notas sin atención, casos pendientes y cerrados, y permitir filtrar períodos. La reserva se aplica a cada elemento; si falta permiso, no se muestran título, tipo, conteo particular o texto que revele el contenido excluido.

### RF-17. Corrección y anulación trazables

El autor ordinario puede corregir su aporte mientras no haya atención institucional sobre él; después puede añadir una aclaración propia y TOE/comité corrige los datos institucionales si procede. Las correcciones conservan automáticamente autor, fecha y diferencia relevante. No se exige texto justificativo para una corrección rutinaria de escritura o fecha; un cambio sustancial de personas, atribución o hechos ya atendidos debe conservar una explicación breve, que puede ser la misma actuación/aclaración y no otro texto repetido.

No se elimina un hecho por una corrección habitual. TOE/comité puede anular un registro creado por error, conservando una causa breve y excluyéndolo de conteos/alertas. Una denuncia no corroborada se conserva con esa condición, no se anula por no haberse probado. Las decisiones legítimas de supresión siguen RF-39. No se exige reconstrucción general de versiones de expedientes: basta identificar cambios relevantes y mantener legibles el aporte y su rectificación autorizada.

### RF-18. Identificación simple de reportes duplicados

Si se detecta que dos registros describen el mismo suceso, TOE/comité debe poder elegir un registro principal y marcar el otro como reporte duplicado de aquel. La relación es explícita, no una fusión automática por nombre/fecha. Se conservan ambos relatos, autores y fechas; el conjunto de reportes vinculados al principal cuenta como un solo hecho en estadísticas/reiteración y concentra su atención vigente, considerando también las participaciones pertinentes que consten solo en los reportes duplicados. Los avisos del duplicado se consideran gestionados al relacionarlo con el principal. Una relación equivocada se corrige retirándola/cambiándola, con causa breve y recálculo, sin restaurar o descomponer contenidos fusionados porque no se mezclan ni destruyen.

Al vincular reportes, todas las personas y la información pertinente de su participación quedan disponibles automáticamente para la consulta del mismo hecho, su atención, los historiales escolares y el informe consolidado, aunque no estén en el registro principal. No se exige copiar esos datos al principal, volver a seleccionarlos ni transcribirlos por TOE. Cada reporte original conserva su texto, participantes, papeles, autor y fecha; la consulta conjunta identifica la procedencia de los datos y mantiene visibles las diferencias.

Una persona inequívocamente identificada tiene una participación computable por hecho; los datos complementarios y papeles se conservan respecto de esa misma persona, sin atribuirle los de otra. Una identificación externa incompleta no se equipara automáticamente a otra por nombre o parentesco. Si los reportes discrepan sobre una participación, se muestran las versiones y su fuente sin elegir automáticamente un papel o corroboración; la aclaración vigente registrada por TOE/comité conforme a RF-24 se distingue de los aportes originales. Si no existe esa aclaración y la discrepancia impide determinar responsabilidad elegible, se cuenta a la persona como involucrada y se aplica la exclusión de responsabilidad/reiteración de RF-20 hasta aclararla. Esto no impide aviso por gravedad ni atención. Solo se pide aclarar la diferencia cuando sea necesario, no revisar o confirmar sistemáticamente cada participación coherente.

El reportante ordinario mantiene acceso solo a su aporte, incluso si es el duplicado; no obtiene los nuevos participantes, papeles o relatos ajenos por la vinculación. Toda exclusión que revele la denuncia se aplica al conjunto relacionado.

Si ya existen dos atenciones, se elige dónde continuará ese hecho y se conserva la otra como atención anterior relacionada; si no tiene otros asuntos pendientes puede cerrarse por duplicidad. Sus actuaciones no se trasladan ni copian obligatoriamente. Si comprende otros hechos, continúa por ellos. No se requiere un subsistema de fusión, reversión, reconciliación de versiones ni revisión preventiva de todas las notas por TOE.

## 3.4 Alertas y recepción

### RF-19. Aviso inmediato y solicitud de atención

Guardar una incidencia grave/muy grave, de violencia/protección o marcada expresamente como riesgo que requiere atención debe crear un aviso interno y entrada en bandeja desde la primera comunicación, aunque los roles sean desconocidos. El usuario ordinario puede solicitar atención ante una situación preocupante sin reclasificarla como falta grave probada. El aviso identifica el hecho y motivo; solo destinatarios autorizados pueden acceder al detalle. Si TOE está implicado, se dirige a integrantes no excluidos conforme a RF-28. No debe esperar confirmación, contador de reiteración, apertura de caso ni aprobación de un superior.

### RF-20. Reiteración por categoría sin validación cotidiana

Con parámetros válidos, el sistema evalúa por estudiante y categoría los hechos únicos de una ventana móvil de W días calendario, hasta la fecha local actual inclusive; el inicio es esa fecha menos W−1 días. Una carga tardía recalcula la ventana actual con la fecha real del hecho, sin simular un aviso pasado. Fechas aproximadas sin precisión suficiente quedan fuera del cálculo automático, sin impedir solicitar atención humana.

Las conductas cotidianas atribuidas por su autor cuentan desde el guardado, sin confirmación de TOE. En categorías de denuncias/violencia, las presuntas responsabilidades por confirmar se representan y, si tienen umbral configurado, se cuentan como **reportes de presunta responsabilidad**, no como faltas corroboradas. La señal distingue conductas cotidianas registradas, atribuciones no corroboradas y corroboradas. Gravedad avisa desde el primer hecho sin depender de esta cuenta.

Se excluyen afectados, testigos, otros sin atribución de conducta, papel desconocido, atribución descartada, antecedentes y hechos anulados. Los reportes duplicados no son ocurrencias adicionales. Un estudiante cuenta una vez por hecho, considerando sus participaciones en todos los reportes vinculados según RF-18; excluir el reporte duplicado como nueva ocurrencia no descarta al estudiante que figure solo en él. Si existe una discrepancia sin aclaración vigente que impida determinar una atribución elegible, no se la computa como responsabilidad/reiteración hasta aclararla; conserva su condición de involucrado y no se retrasa el aviso por gravedad. La presunta responsabilidad de un familiar u otra persona externa no se cuenta como falta del estudiante afectado. Al alcanzar el umbral se genera/actualiza una señal con estudiante, categoría, ventana, parámetros y hechos que la sustentan. Una señal pendiente agrega nueva información sin duplicar tareas por el mismo asunto; leerla o recalcular sin novedades no produce más avisos. Gestionarla mediante atención actualiza su condición automáticamente según RF-21. Tras una atención cerrada, un hecho elegible nuevo puede activar nueva señal, sin reabrir el caso anterior. Correcciones de atribución, duplicidad, anulación o parámetros recalculan el fundamento y conservan la referencia de la señal emitida, sin exigir revisión del resto de notas cotidianas.

### RF-21. Bandeja y notificaciones sin trámites duplicados

La bandeja debe distinguir situaciones nuevas, atenciones pendientes y compromisos/actuaciones con seguimiento realmente pendiente. Debe identificar motivo, gravedad declarada, antigüedad, responsable y vencimiento conocido, permitiendo ordenar/filtrar por ellos. Un asunto con varios motivos aparece sin duplicar su atención; su historia cerrada sin pendientes no forma parte del trabajo pendiente. Las notas cotidianas sin señal quedan fuera. No se requiere correo, SMS o servicios externos.

Leer solo marca lectura; no resuelve la situación. Abrir/vincular una atención, registrar su intervención o identificar un duplicado debe actualizar los avisos relacionados en esa misma operación, sin un segundo trámite de recepción o retirada del aviso. Una actuación que resuelve el asunto y cierra su atención puede guardarse conjuntamente. Si no se necesita abrir atención, basta una disposición breve —por ejemplo, intervención inmediata ya documentada—, sin reescribir su resultado. Descartar una señal grave sin actuación requiere fundamento pertinente; los motivos rutinarios pueden derivarse de los datos ya registrados. Autor/fecha son automáticos. Nuevos hechos relevantes pueden volver a destacar el asunto, sin repetir avisos carentes de novedad ni abrir un caso por algoritmo.

## 3.5 Atención, comité y conflictos

### RF-22. Atención directa desde el asunto de origen

TOE debe poder iniciar atención desde una señal o incidencia y vincularla a un caso existente si corresponde, sin volver a seleccionar/transcribir sus datos. Comité conserva la capacidad conforme a sus facultades. Se registran hechos relacionados, fecha y responsable —propuestos desde el contexto— y estado pendiente o cerrado si la intervención ya concluyó. El motivo puede quedar expresado en hechos y actuación; no se exige una justificación adicional de apertura.

Debe poder documentarse una conversación realizada, su resultado, compromiso opcional y cierre en una misma operación, sin guardar primero apertura, programación o recepción por separado. La atención continua permite conservar trabajo pendiente real, sin crearlo obligatoriamente. Iniciar atención no confirma roles ni exige concluir verificaciones; conserva acciones inmediatas y referencias previas. El guardado actualiza el aviso pertinente según RF-21.

### RF-23. Agrupación de incidencias y corrección sencilla de vínculos

TOE determina habitualmente qué incidencias relacionadas se atienden juntas; los miembros del comité conservan esa capacidad al ejercer su función. Seleccionar los hechos relacionados basta para agruparlos: no se exige otra justificación escrita por cada vínculo. El caso puede reunir hechos y personas distintas —estudiantes, trabajadores, familiares u otras externas— con papeles propios por hecho; sus datos se recuperan de los registros, incluidos todos los reportes vinculados como duplicados según RF-18, sin volver a introducir participantes ni crear un caso por persona.

Un hecho sostiene una sola atención local pendiente. Si ya está atendido, se utiliza esa atención o se decide expresamente dónde continuará, conservando referencia a la anterior y sus actuaciones. No se exige fusionar casos, trasladar automáticamente actuaciones ni reconstruir versiones. Para continuar conjuntamente puede elegirse un caso y relacionar los antecedentes de los otros, preservando su consulta autorizada y cierre cuando ya no tengan trabajo pendiente propio.

Un vínculo equivocado se puede corregir sin borrar el hecho o sus actuaciones; basta una explicación breve del cambio sustancial, no un procedimiento de conciliación. Si se retira el último hecho de un caso abierto por error, se conserva cerrado por ese motivo con la referencia histórica necesaria. Casos cerrados pueden citarse como antecedentes sin volver a contar sus hechos ni reabrirse automáticamente. Agrupar no amplía acceso del reportante y mantiene restricciones por conflicto.

### RF-24. Aclaración de participaciones cuando corresponde

TOE/comité debe poder precisar una participación, corroborar una atribución cuando efectivamente la haya esclarecido o dejar constancia de que no se corroboró/descartó. Su autor y fecha se conservan automáticamente y el fundamento puede ser la propia actuación de atención, sin un texto separado por persona. No existe una lista obligatoria de roles cotidianos a confirmar: las atribuciones directas del registrador ya cuentan según RF-13/RF-20. Tampoco se exige confirmar todos los roles para proteger, derivar, registrar una actuación o cerrar localmente. Afectados/testigos nunca se vuelven responsables por confirmar el hecho. Los cambios ajustan señales y resultados sin ocultar que inicialmente fueron informados de otro modo.

### RF-25. Actuaciones realizadas y programación opcional

Debe permitir documentar una acción ya realizada directamente, con descripción/resultado, fecha efectiva y personas pertinentes si participaron, recuperando autor y responsable del contexto y proponiendo la fecha actual. Un relato suficiente puede expresar acción y resultado sin duplicarse en textos obligatorios. La programación es opcional: si realmente se prevé trabajo posterior, se registra fecha prevista y responsable; después se completa la misma actuación con ejecución/resultado, sin crear otra contable. Cancelarla requiere una causa breve y no la presenta como realizada.

La acción puede referirse al caso completo o a personas/hechos concretos y puede guardar compromisos y cierre en el mismo acto. No exige adjuntos, aprobaciones ni planificación previa. Las actuaciones fuera de la aplicación se documentan con su fecha real, distinta de registro si corresponde. Solo trabajo pendiente real aparece en bandeja. Se permite seguimiento posterior al cierre local según RF-30 sin administrar el protocolo oficial como otro expediente.

### RF-26. Compromisos sin programación administrativa obligatoria

Debe permitir registrar compromisos junto con una actuación, con contenido y persona que los asume; fecha objetivo solo si se acordó. Pueden documentarse seguimiento y cumplimiento/no cumplimiento observados, con autor/fecha automáticos. No se exige crear una tarea adicional por cada compromiso ni programar comprobación: solo habrá pendiente de seguimiento si el responsable realmente lo indica o existe una fecha objetivo aún por atender. Un compromiso sin plazo no se marca vencido ni se presume verificado. Una sustitución/cancelación importante deja causa breve. Vencimiento o incumplimiento no genera sanción o nueva responsabilidad automáticamente. Su contenido y resultados se conservan para atenciones futuras.

### RF-27. Intervención del comité

TOE debe poder registrar convocatoria del comité sobre el mismo caso pendiente, con fecha, motivo de especial gravedad/no resolución y miembros vigentes elegibles. No hay convocatoria automática por gravedad o reiteración. El caso conserva su historia, identifica que su atención actual corresponde al comité y permite registrar quién intervino efectivamente y cuándo, sin sustituir la composición estable por un comité distinto en cada caso.

Durante esta intervención, el director registra decisiones, actuaciones, acuerdos, resultados y derivaciones del órgano; los demás elegibles consultan y participan conforme a su función. Si el director está excluido o su designación no está vigente, registra un miembro restante habilitado, dejando constancia de sustitución. Si TOE está excluido, los miembros restantes pueden asumir directamente, registrar el motivo y ejercer las capacidades necesarias sin convocatoria del implicado. El sistema no exigirá firmas o aprobación electrónica de cada integrante para guardar lo actuado ni un acta para iniciar protección. El acta externa se trata en RF-32.

### RF-28. Restricción por conflicto de interés

Debe permitir registrar la exclusión de una persona usuaria respecto de un hecho/caso, motivo, fecha y responsable no implicado. Debe vincular el conflicto con todos los hechos y atenciones que revelen la denuncia, también al relacionar duplicados o vincularlos a una atención. Las restricciones preventivas por presunto denunciado con acceso institucional se aplican antes de notificar; si el rol denunciado se informa con posterioridad, revocan el acceso desde ese momento. Se deben revalidar sesiones y accesos ya abiertos para impedir nuevas lecturas/descargas.

TOE/director/comité no excluidos pueden aplicar o corregir la restricción; retirarla requiere decisión motivada no propia según 2.3. Quedan inaccesibles también informes, actas, notificaciones y fragmentos en historial o estadísticas que permitan reconstruir el caso. Ser autor, administrador o miembro estable no la supera. La cuenta puede seguir trabajando en los demás ámbitos autorizados. Si no hay destinatario elegible, se conserva el registro y la necesidad de designación sin comunicar el contenido al implicado. El software no decide sustituciones legales del cargo ni obliga a esperar su resolución para actuar fuera de la aplicación.

## 3.6 Derivación, cierre y documentos

### RF-29. Derivación externa registrada

TOE o el registrador habilitado del comité debe poder registrar una derivación con entidad/destinatario, motivo, fecha efectiva y referencia disponible. La preparación previa es opcional; si se registra, se distingue de la ejecutada y no obliga a guardarlas en operaciones separadas. La actuación puede expresar su motivo y constancia de envío/entrega sin dos relatos redundantes. Puede añadirse oficio, cargo o reporte cuando se conozca, sin exigir un archivo ni número inexistente para registrar la actuación.

Debe poder generarse el informe consolidado antes de ejecutar la derivación. Registrar destinatario no envía comunicaciones ni inicia un procedimiento externo. El colegio comunica por los medios competentes y documenta su referencia. Derivar no cierra automáticamente salvo RF-30 y no se computa como incidencia nueva.

### RF-30. Cierre local sin validaciones redundantes

TOE o el registrador habilitado del comité debe poder cerrar la atención con fecha efectiva y resultado que explique su conclusión, recuperando autor/fecha y sin repetir un motivo ya expresado. Si una conversación y compromiso bastan, se registran actuación, resultado y cierre mediante un único guardado; no se obliga a programar acciones, confirmar roles cotidianos ni realizar una aprobación de cierre. Los pendientes reales se muestran antes de cerrar y siguen visibles después, salvo que el operador los complete o cancele con causa; no debe justificar individualmente su mera continuidad. Un compromiso acordado no obliga por sí solo a mantener el caso abierto.

En atención local por violencia familiar, registrar una derivación efectivamente ejecutada a Fiscalía cierra esa atención por derivación cuando abarca su objeto completo, con fecha/fundamento recuperados de la propia actuación. Preparar la derivación, otro destinatario u otro supuesto no activa la regla. Un caso mixto continúa pendiente si conserva atención por hechos no comprendidos en la derivación. El cierre es **local**: no afirma solución de la violencia ni cierre fiscal, de SíseVe o del protocolo oficial.

La historia permanece consultable y permite documentar protección/seguimiento posterior con su fecha, sin reabrir automáticamente ni cambiar el cierre original. Solo los seguimientos realmente pendientes continúan en bandeja. Nuevos hechos pueden dar lugar a otra atención por decisión humana y citar antecedentes. Un cierre equivocado se puede corregir motivadamente con trazabilidad; no se exige un procedimiento general de reapertura. Los informes emitidos conservan su fecha/corte y puede generarse otro actualizado según RF-31.

### RF-31. Informe consolidado del caso

El sistema debe generar un informe legible, imprimible y descargable en PDF de los casos graves, antes de derivar y al cierre; también debe permitir generarlo cuando el responsable lo necesite durante la atención. La capacidad puede usarse para casos no graves sin crear otro formato/proceso. El responsable no debe tener que volver a transcribir los hechos. Es un informe consolidado de todas las incidencias del caso, aunque haya varias personas; la minimización de contexto para terceros no divide el caso en informes obligatorios por hecho.

Debe incluir institución, identificador del caso, fecha de emisión y corte, estado local y motivo de emisión; hechos con fecha, naturaleza/gravedad, descripción y fuente; involucrados y papel/carácter de atribución por hecho; contexto estrictamente pertinente; actuaciones programadas/realizadas, decisiones, resultados, compromisos y seguimientos; intervención del comité y referencia del acta; derivaciones preparadas/efectuadas y referencias oficiales disponibles; cierre y continuidad pendiente. Debe recuperar también las personas y participaciones pertinentes que consten solo en reportes vinculados como duplicados, incluidos familiares y otras personas externas, sin exigir transcripción o alta previa en el principal. El consolidado presenta el hecho una vez e identifica la procedencia y las diferencias pertinentes de participación, sin ocultarlas ni resolverlas automáticamente; distingue la aclaración institucional vigente si existe. Debe indicar lo desconocido o no corroborado sin completar datos por inferencia. No copia automáticamente toda la ficha contextual del estudiante: permite incorporar el antecedente pertinente ya disponible sin volver a transcribirlo. El informe interno puede identificar a las personas para el trabajo autorizado.

Antes de descargar una versión dirigida a terceros debe permitir seleccionar destinatario/finalidad y excluir contexto ajeno a esa comunicación. Para documentos sobre violencia dirigidos a terceros, las identidades de estudiantes deben expresarse mediante iniciales y DNI disponible, sin nombres completos, conforme al anexo 03, III.3.8 de RM 383-2025. Si no se dispone del documento, se indica esa ausencia y una referencia interna segura; no se inventa ni se bloquea la actuación urgente. Esto no habilita divulgar otros datos identificadores innecesarios.

Autor, fecha/corte, institución y datos conocidos se recuperan automáticamente. La finalidad ordinaria se obtiene del contexto; destinatario/finalidad se precisa para entrega externa, sin reiterarlos para cada consulta interna. El informe efectivamente emitido al cierre o para derivación queda identificado y conservado con los permisos del caso mientras corresponda; una vista previa no obliga a archivar otra versión. No se exige un gestor documental de versiones. Emitir un informe no acredita envío, firma institucional ni registro oficial. No se exige plantilla visual, firma electrónica o numeración de oficio no proporcionada.

### RF-32. Actas externas

El registrador habilitado del comité debe poder adjuntar actas institucionales en PDF o Word `.docx` al mismo caso, con fecha del acta, descripción breve y autor/fecha de incorporación. TOE/comité no excluidos pueden consultarlas o descargarlas; no se duplican en cada incidencia. Puede haber varias actas de distintas reuniones. Sustituir un archivo incorrecto exige motivo y conservar la referencia de sustitución; la conservación de versiones personales innecesarias se rige por RF-39. Deben admitirse actas de hasta 10 MB por archivo, objetivo de ingeniería suficiente para documentos ofimáticos ordinarios, mostrando el límite antes de adjuntar y un error claro si se supera. No se exige un editor, generador de actas, firma ni clasificación documental adicional.

### RF-33. SíseVe, Libro y otras referencias oficiales

Debe permitir registrar manualmente que un hecho se conoció por SíseVe, comunicación interna u otro canal, y asociar referencias externas disponibles a la incidencia/caso: sistema o instrumento, código o asiento, fecha de registro si se conoce y observación pertinente. Debe permitir completar después una referencia que faltaba. La mera selección de origen SíseVe no implica que el colegio lo registró nuevamente ni confirma cierre externo.

Un hecho informado primero en SíseVe debe poder incorporarse al registro local con sus personas y atención; si ya existe se añade su referencia o aporte al mismo hecho, con RF-18. Puede conservarse referencia al protocolo aplicable y su versión para explicar actuaciones históricas, sin motor de ejecución ni migración retrospectiva de protocolos. Debe mostrar diferenciados estado local y constancias externas efectivamente informadas. La aplicación debe recordar en su ayuda que no sustituye Libro/SíseVe ni acredita su cumplimiento por guardar aquí. No se exigirá completar una referencia oficial para guardar una comunicación urgente.

## 3.7 Consultas, análisis y salidas

### RF-34. Medidas y recuentos definidos

Las estadísticas deben distinguir: hechos únicos; participaciones por papel y carácter de atribución; estudiantes distintos involucrados; estudiantes con conductas cotidianas atribuidas, con presuntas responsabilidades por confirmar y con atribuciones corroboradas; afectados y testigos; casos pendientes/cerrados; señales emitidas/gestionadas/pendientes por motivo; actuaciones realizadas/previstas; compromisos y seguimientos pendientes/comprobados; y derivaciones efectivas. Se excluyen duplicados y anulaciones del conteo de hechos, sin omitir personas que consten únicamente en reportes vinculados según RF-18. Una persona identificada cuenta una participación por hecho, conservando los papeles y discrepancias que correspondan; estas no se convierten automáticamente en responsabilidad elegible. Las medidas referidas a estudiantes no incluyen familiares, trabajadores u otras personas externas; las de participaciones identifican su universo. No se denomina culpable a nadie ni se suman víctimas/testigos como infractores.

Hechos se asignan por fecha del hecho; comunicaciones por fecha de registro si esa es la medida. Casos se agrupan por apertura/cierre, actuaciones por fecha prevista/efectiva y señales por emisión, con criterio visible. Un cierre por apertura errónea o duplicidad se identifica y no se presenta como otra atención efectiva resuelta; sus actuaciones reales permanecen en historia. Un hecho compartido cuenta una vez en el total institucional aunque aparezca en varios grupos afectados; se advierten subtotales no aditivos. Grado/sección se toman del momento del hecho conocido, separando no informado. Identificar duplicado o corregir datos actualiza cifras actuales; no altera silenciosamente una salida emitida con otro corte.

### RF-35. Análisis y filtros combinables

TOE/director/comité elegibles deben poder seleccionar medidas, agrupaciones y filtros combinables sobre período, año, categoría, naturaleza, gravedad, grado, sección, sexo informado, papel/carácter de atribución, estado de atención, motivo de alerta, entidad de derivación e intervención del comité, cuando la medida disponga de esos datos. El sistema debe facilitar al menos frecuencias y tipos más frecuentes, distribución por grupos, evolución mensual/anual, comparación entre períodos, reiteraciones, carga de casos, acciones/compromisos pendientes y realizados y derivaciones. Son una cobertura mínima, no una lista cerrada de informes fijos.

Se deben poder combinar los filtros pertinentes y obtener cruces de al menos dos dimensiones —por ejemplo categoría/grado o sexo/período— sin escribir consultas ni solicitar un desarrollo por cada combinación disponible. No se establece un máximo de dos filtros ni se exige un motor universal de consultas, expresiones o datos externos. Las agrupaciones se limitan a variables registradas y pertinentes, sin analizar libremente relatos sensibles.

Debe ofrecer tablas y gráficos comprensibles, incluido mapa de calor por día de semana de la fecha del hecho. El análisis por hora/franja solo usa horas fiables informadas; no las deduce del registro. Cada salida identifica unidad, criterios, período, cobertura y falta de datos, distinguida de cero. Las tablas permiten comprender los valores de sus gráficos.

### RF-36. Comparaciones por sexo, cantidades, proporciones y tasas

Debe comparar por sexo cuando se disponga legítimamente de ese dato, sin inferirlo del nombre o de columnas de hermanos. Los valores no informados se separan y se muestra su cobertura; la matrícula entregada carece de ese campo. Lo mismo se aplica a grupo no informado.

Debe diferenciar cantidades absolutas y proporciones. Para proporción de estudiantes involucrados en una nómina anual/grupo/sexo, se divide el número de estudiantes distintos con la participación filtrada entre estudiantes distintos de esa misma población con datos válidos, expresándolo como porcentaje. Numerador y denominador deben referirse a la misma población; no se duplican estudiantes por varios hechos. Si se usa nómina anual con hechos de un subperíodo, se indica expresamente esa referencia y su cobertura. Si falta denominador válido no se calcula y no se presenta un dato incompleto como cero.

Si se presenta frecuencia de hechos por cada cien estudiantes, debe rotularse así, indicando período y denominador; no es porcentaje de estudiantes ni probabilidad de sufrir un hecho. Una tasa que necesite población/tiempo de exposición solo se calcula con información suficiente y lo declara. No se exige recopilar exposición individual adicional ni llamar tasa exacta a una comparación basada en una nómina parcial. Las comparaciones entre grupos/períodos deben mantener definiciones y mostrar diferencias de tamaño/cobertura.

### RF-37. Exportación según finalidad y destinatario

TOE/director/comité autorizados deben poder descargar/imprimir resúmenes estadísticos en PDF y tablas filtradas en CSV o .xlsx con fecha/corte, medidas, filtros y cobertura. Las exportaciones internas son reservadas y respetan permisos, sin columnas ocultas, relatos o identificadores ajenos al resultado. Los agregados no incluyen nombres/DNI; los listados nominativos necesarios para atención son consultas reservadas distintas.

**Consulta y descarga internas autorizadas:** deben conservar cifras exactas, incluidos grupos pequeños, cuando sean necesarias para atención/análisis institucional. No se ocultan automáticamente valores por ser menores que cinco ni se impone otro umbral numérico sin decisión fundada. La reserva y el conflicto se aplican también a las salidas: se calculan dentro del ámbito permitido y no se revela un caso excluido mediante desgloses o referencias particulares.

**Entrega externa o difusión:** la institución decide finalidad, legitimación y destinatario antes de comunicarla fuera del sistema. Puede preparar agregados más amplios utilizando filtros/agrupaciones y omitir campos/grupos identificables en su procedimiento de preparación, sin requerir un motor automático de anonimización. Un resumen de pocas personas puede seguir identificando aunque no tenga nombres; el responsable debe revisar ese riesgo y no divulgarlo así. No hay publicación pública ni envío automático. Las comunicaciones nominativas a autoridades competentes siguen RF-31 y su reserva específica, no una supresión ciega de cifras.

## 3.8 Trazabilidad, datos personales y operación

### RF-38. Trazabilidad utilizable sin auditoría administrativa cotidiana

Debe generarse automáticamente la evidencia exigida en RNF-07 y poder recuperarse de forma legible e inmediata por operador autorizado, localizando período, usuario, operación y referencia afectada. Puede obtenerse con consultas básicas o medios de operación documentados: no se exige un panel avanzado, buscador forense o revisión manual cotidiana por TOE. Los usuarios de atención pueden identificar autor/fecha y rectificaciones relevantes en el dato que ya están autorizados a consultar.

La evidencia no concede acceso a relatos/adjuntos ni permite al implicado investigar el caso excluido. No se deben copiar indiscriminadamente contenidos sensibles en registros técnicos. La extracción de evidencia es restringida y trazable. No se requieren firmas por evento, cadenas inmutables ni un subsistema de investigación independiente.

### RF-39. Resultados mínimos para derechos y conservación

La tramitación de solicitudes, comprobación de identidad/representación, decisión jurídica, plazos y comunicaciones corresponde al responsable institucional y puede hacerse fuera de la aplicación. No se requiere portal de titulares, expediente de derechos, calendario de conservación configurable o herramienta general de anonimización. Debe existir un procedimiento operativo documentado que use las consultas, exportaciones, correcciones y controles existentes y, cuando sea excepcionalmente necesario, intervención técnica autorizada, para obtener estos resultados:

- Localizar los datos de la persona y preparar la información procedente para una respuesta. No entregar por defecto actas, expediente o relatos completos de terceros; aplicar también reserva de violencia. La institución prepara y comunica la respuesta, incluyendo comunicaciones de rectificaciones a destinatarios previos cuando correspondan según Ley 29733 art. 20.
- Rectificar información inexacta mediante importación o corrección pertinente y, si corresponde, limitar temporalmente su acceso/tratamiento. El bloqueo durante actualización/rectificación/supresión se aplica según art. 20 de Ley 29733, con las excepciones de competencias públicas y comunicación del trámite; no es una reacción automática indiscriminada a cualquier solicitud.
- Ejecutar una disposición legítima de conservación/supresión sobre datos y copias operativas identificadas, o anonimizar cuando esa sea la decisión válida, sin destruir información necesaria de terceros en un hecho compartido. Los registros e informes alcanzados no deben seguir revelando aquello que legalmente deba retirarse. No basta ocultar un botón.

La decisión queda documentada con alcance, fecha y fundamento, admitiéndose una constancia institucional externa; no exige cargar un expediente electrónico aquí. Solo un operador con autorización específica la ejecuta. El procedimiento define cómo verificar su resultado y tratar respaldos conforme a RNF-18, sin editar selectivamente cada copia histórica ni construir un motor de coordinación. Los plazos/retenciones son los institucionales fundados de RN-25, no un borrado automático al cerrar o cambiar de año. Las operaciones ordinarias de corrección no deben depender del desarrollador original; las disposiciones excepcionales pueden requerir soporte técnico autorizado.

### RF-40. Aviso de privacidad y alcance de datos admitidos

Debe poder ponerse a disposición el aviso institucional vigente de privacidad, por texto o documento accesible, identificando banco, titular/responsable y domicilio, contacto, encargados aplicables, finalidades, destinatarios/transferencias, obligatoriedad y consecuencias de proporcionar/no proporcionar datos, tiempo o criterios de conservación y medios de derechos, conforme a Ley 29733 art. 18. La institución informa a titulares por sus canales; no necesita darles cuentas ni registrar electrónicamente cada entrega en esta aplicación.

La institución documenta fuera o dentro de sus medios habituales qué campos contextuales son necesarios y su fundamento, incluidos consentimientos cuando esa sea la base. La importación solo permite seleccionar los campos/conceptos admitidos de ese alcance; no crea una aprobación por celda, catálogo jurídico, registro de consentimientos/revocaciones o verificación legal por incidencia. Una columna nueva no habilita por sí sola tratarla. No se presume que matrícula equivalga a consentimiento universal ni que toda actuación de protección deba esperar consentimiento parental. Actualizar aviso o alcance de importación no obliga a modificar código ni contratar al desarrollador original.

### RF-41. Comprobación operativa y entrega recuperable

Debe existir un procedimiento sencillo para que el responsable operativo designado consulte el resultado/fecha de respaldos y del último ensayo de restauración y conozca fallos que requieren acción. Puede utilizar registros o informes de las herramientas elegidas; no es obligatorio desarrollar un panel específico. TOE, docentes y AIP no tienen que vigilar respaldos durante su trabajo escolar.

Un operador técnico autorizado debe poder obtener un conjunto completo recuperable de información y adjuntos, con sus relaciones, catálogos y restricciones, mediante medios documentados, para recuperación o cambio de responsable. No basta una exportación estadística. La operación y restauración son protegidas y dejan evidencia; la designación y medios de acceso deben estar disponibles para la institución, sin depender de una cuenta personal del desarrollador. Las condiciones verificables se fijan en RNF-18 y RNF-20–RNF-24.

# Capítulo 4. Requisitos no funcionales

## 4.1 Criterios de medición

Las cifras siguientes son objetivos de aceptación de ingeniería, no decisiones cuantitativas del colegio ni exigencias legales. La matrícula entregada es una muestra parcial de 91 estudiantes y no permite deducir la población institucional. Para comprobar escala de un colegio y continuidad durante años se adopta un conjunto ficticio de 1 000 estudiantes activos, diez años, 10 000 vínculos anuales, 20 000 incidencias y 5 000 casos, con diez usuarios concurrentes. Si al instalar se conoce un volumen institucional mayor, las mismas tareas deben verificarse con él; no se presume una plataforma empresarial.

Los tiempos se ensayan con un equipo de oficina, navegador compatible y conectividad de al menos 10 Mb/s y hasta 100 ms de latencia al servicio, documentando el entorno. Se toman al menos veinte mediciones por tarea frecuente y se verifica el percentil 95; no se requiere una plataforma de pruebas específica. La medición va de solicitud a resultado utilizable; la transferencia de archivos se distingue de su preparación. La inspección de consistencia y seguridad usa datos ficticios. Una falla se registra como tal y no se convierte en aceptación por haber sido explicada.

## 4.2 Seguridad, privacidad e integridad

### RNF-01. Autenticación y control de sesiones

Toda operación con datos personales debe exigir identidad autenticada. Las credenciales no deben poder recuperarse en texto legible ni aparecer en registros operativos o exportaciones. La recuperación de acceso debe comprobar control del mecanismo autorizado y retirar accesos anteriores comprometidos; no se exige correo externo para lograrlo. Debe limitar intentos repetidos de acceso y probar que una secuencia automatizada de intentos fallidos es frenada sin bloquear permanentemente a todos los usuarios. Las respuestas de autenticación no deben revelar datos de estudiantes o detalles de cuentas ajenas.

El cierre de sesión debe impedir reutilizarla. Tras veinte minutos sin actividad con datos personales debe solicitarse autenticación de nuevo, con advertencia previa y sin perder silenciosamente texto no guardado. Veinte minutos es un compromiso de ingeniería para puestos compartidos; no es un plazo legal. La revocación de cuenta, rol o exclusión por conflicto debe impedir la siguiente operación protegida, incluso desde una sesión ya abierta. Se verificará con dos sesiones simultáneas y reutilización de enlaces anteriores.

### RNF-02. Autorización uniforme y mínimo acceso

La matriz del capítulo 2 debe aplicarse a toda lectura, cambio, búsqueda, descarga, resumen y operación automatizada. Ocultar una opción visual no es prueba suficiente: un usuario sin permiso tampoco obtiene datos solicitándolos directamente o cambiando una referencia. Los nuevos roles no deben recibir permisos por defecto fuera de los definidos. Las pruebas deben cubrir acumulación de roles, término de membresía, usuario implicado y restricción mantenida al relacionar duplicados o casos. La cuenta administrativa sin rol funcional no debe leer expedientes. El colegio debe poder revisar privilegios al menos semestralmente usando RF-01, conforme al art. 46.1 del DS 016-2024-JUS.

### RNF-03. Protección de comunicaciones y almacenamiento

Los datos personales deben estar protegidos contra lectura, alteración y extracción no autorizadas durante transmisión y almacenamiento, incluidos adjuntos, respaldos y entregas completas. Debe verificarse que un tercero sin credenciales ni autorización no interpreta contenido transmitido ni accede a copias almacenadas mediante rutas o enlaces previsibles. Las claves o credenciales de operación no deben incluirse en documentos públicos, código entregado como ejemplo ni mensajes de error. No se exige un algoritmo, proveedor o implementación específica; el responsable técnico debe aportar evidencia de los controles aplicados al riesgo y de recuperación segura de sus medios de acceso.

### RNF-04. Archivos y contenido no confiable

Importaciones, textos y adjuntos deben tratarse como entradas no confiables. No se ejecutan fórmulas, macros, vínculos externos ni contenido activo de archivos cargados como parte de la importación/consulta. Deben rechazarse formatos no admitidos, archivos malformados o que excedan límites documentados, con mensajes corregibles. Un nombre de archivo no puede modificar rutas o permisos. Los archivos no deben ser públicos por conocer su dirección; cada consulta vuelve a comprobar autorización. Las exportaciones tabulares deben impedir que texto aportado por usuarios se ejecute como fórmula al abrirse. Se verificará con archivos y textos de prueba inocuos que representen esas condiciones.

### RNF-05. Privacidad desde la operación ordinaria

La aplicación debe solicitar solo los campos imprescindibles de la tarea, dejar claramente opcionales los restantes y reservar por defecto contexto, contactos y atención. No debe usar datos reales para publicidad, entrenamiento de modelos, telemetría de contenido ni analítica ajena a la finalidad escolar. Los diagnósticos deben evitar nombres, relatos, documentos y credenciales salvo evidencia delimitada y autorizada. Los archivos de importación temporal y de generación fallida deben retirarse al terminar su uso; no se conservará automáticamente una copia de todas las columnas omitidas. La prueba inspeccionará resultados, temporales y diagnósticos de un lote ficticio con columnas descartadas.

### RNF-06. Reserva de salidas y espacios compartidos

Notificaciones y mensajes de error no deben exponer denuncias o antecedentes a quien no pueda abrirlos. No se incluirán relatos o documentos de identidad en direcciones de acceso, títulos públicos o mensajes de sesión expirada. Las descargas deben ser solicitadas por el usuario autorizado y rotuladas como información reservada, sin sustituir los controles de acceso. Después de cerrar sesión o retirar un permiso, volver a una vista anterior no debe volver a mostrar datos recuperados del servicio. El procedimiento de usuario debe explicar la custodia de copias ya descargadas; el software no garantiza retirar una copia legítimamente exportada de un equipo ajeno.

### RNF-07. Trazabilidad automática disponible y protegida

Debe generarse trazabilidad continua de inicio/cierre de sesión, consulta/visualización, creación, modificación, anulación, eliminación, importación y exportación de datos personales. Cada evento identifica usuario, fecha/hora, operación, resultado y referencias al ámbito afectado; una consulta de listado conserva criterios/alcance sin copiar todos sus relatos. Los cambios relevantes se explican con la información de RF-17; solo se exige causa cuando el cambio la necesita, no un motivo narrativo para toda edición.

Los eventos deben ser legibles y disponibles inmediatamente tras la operación, protegidos contra alteración por operadores ordinarios y conservados al menos dos años conforme al art. 46.3 del DS 016-2024-JUS. Ese mínimo no fija conservación de expedientes. Se prueba reconstruyendo accesos/cambios de un caso ficticio, importación de su antecedente y exportación de su informe con los medios documentados de RF-38. Una falla de evidencia no puede informar éxito silencioso de una modificación sin trazabilidad recuperable. No se exige a TOE revisar estos registros durante la atención cotidiana.

### RNF-08. Integridad, concurrencia y fechas

No deben existir hechos sin autoría, participaciones duplicadas de una persona en el mismo hecho ni vínculos a personas inexistentes. Una fila importada no se declara guardada con identidad/contexto parcialmente aplicados. Dos cambios concurrentes del mismo contenido no se sobrescriben silenciosamente: se avisa y se permite conservar la información correcta. Un doble envío/reintento de guardado no crea una incidencia, actuación o derivación adicional; se verifica con pérdida de respuesta y dos sesiones.

Las fechas de hechos, realizaciones y derivaciones efectivas no pueden estar en el futuro; las acciones previstas sí. Fechas de apertura/cierre reales deben ser coherentes. Se permite registrar tardíamente hechos/actuaciones con su fecha efectiva, sin exigir una justificación adicional por esa sola demora. Lo aproximado/desconocido se conserva como tal; no se fabrica una hora ni se transforma ausencia en cero. La validación no obliga a introducir una secuencia administrativa previa que no ocurrió.

### RNF-09. Seguridad verificable antes de uso real

La aceptación debe incluir pruebas de autenticación, permisos por rol/caso, conflicto, acceso directo a informes/actas, entradas maliciosas, sesiones y recuperación, con datos ficticios. No debe quedar un defecto reproducible que permita acceso no autorizado, pérdida/alteración silenciosa de historia o exposición masiva de datos. Las dependencias y componentes escogidos deben contar con inventario y revisión de vulnerabilidades conocidas; los hallazgos con posibilidad de afectar confidencialidad/integridad deben corregirse o deshabilitarse antes de uso real. No se exige certificación ni un centro de seguridad permanente. La evidencia y correcciones deben poder revisarlas otro responsable técnico.

## 4.3 Usabilidad y accesibilidad prioritarias

### RNF-10. Bajo esfuerzo en registro y atención frecuente

Una nota cotidiana con estudiante existente debe requerir solo identificar al estudiante al que se atribuye la conducta, elegir categoría y escribir el hecho; fecha actual, autor, registro, grupo y clasificación habitual se recuperan/proponen. La atribución cotidiana de RF-13 es visible y corregible, sin una segunda confirmación ni tarea de validación para TOE. Otros participantes conservan su papel propio. Una decisión de guardado basta, con actuación/compromiso inmediato opcional y sin caso, aprobación o adjunto obligatorios.

Desde una alerta, TOE debe poder usar los hechos ya seleccionados y documentar una intervención sencilla y su cierre con una sola confirmación final, sin guardar recepción/apertura/programación por separado. Registrar actuación y compromiso conjuntamente no debe repetir estudiante, caso, autor, fecha o relato ya disponibles. La programación y los pendientes se utilizan cuando realmente existen. Apertura, agrupación rutinaria y cierre no exigen textos justificativos adicionales a hechos/resultado suficientes; conflictos, anulaciones y cambios sustanciales conservan causa según su requisito.

### RNF-11. Aprendizaje y eficiencia comprobables con una prueba viable

La aceptación debe ensayarse con tres representantes de tareas escolares —un docente, un auxiliar y quien represente atención TOE— tras una orientación de hasta treinta minutos. Cada participante completa las tareas propias de su función sin ayuda: registrar una nota cotidiana breve en hasta noventa segundos y localizar estudiante/aporte autorizado en hasta treinta segundos; quien representa TOE documenta una conversación realizada, compromiso y cierre desde una alerta en hasta tres minutos. Se proporciona el contenido breve del relato para medir interacción, sin omitir participantes ni datos necesarios. Si un obstáculo del producto impide la tarea, se corrige y repite el escenario afectado; el tamaño de prueba no pretende una inferencia estadística universal.

Un representante de AIP, que puede coincidir con un participante anterior, debe poder importar la muestra ficticia de matrícula, revisar columnas movidas y resolver/omitir una fila errónea usando la guía, sin código ni ayuda del desarrollador. No se exige localizar dos operadores AIP o un equipo especializado de evaluación. Se registra si comprende lo que se actualizará, el grupo no informado y las filas excluidas. Ninguna prueba acepta una divulgación indebida o una operación declarada exitosa sin estar guardada.

### RNF-12. Prevención de errores sin confirmaciones repetidas

Los mensajes deben explicar en español corriente qué impide continuar y cómo corregirlo. Una validación fallida conserva los datos introducidos. Se advierte al abandonar cambios sin guardar y se distingue operación en curso, éxito confirmado y fallo; una pérdida de conexión no aparenta éxito. Importación, identificación de duplicado, anulación o disposición de datos muestran alcance antes de una confirmación, sin repetirla en cada campo.

El cierre ordinario puede confirmarse junto a actuación/resultado, sin diálogo o autorización adicional obligatorios. La continuidad de pendientes no requiere justificarlos otra vez. Los filtros/criterios se conservan identificables al consultar y volver del detalle. Se verifica el registro habitual, atención resuelta en un acto, errores de importación y corrección de duplicado; las precauciones no deben multiplicar guardados rutinarios.

### RNF-13. Claridad de significado y continuidad de tarea

Las operaciones equivalentes usan términos consistentes. Deben distinguirse fecha del hecho/registro, conducta cotidiana atribuida/denuncia por confirmar/atribución corroborada, papel desconocido, incidencia/caso, prevista/realizada y cierre local/estado externo. No se pide comprender estructuras internas o algoritmos para registrar y atender. Una nota cotidiana no se presenta como investigación pendiente de TOE.

Desde alerta o seguimiento se accede al asunto autorizado conservando su contexto, sin buscar o transcribirlo de nuevo; las alertas atendidas se actualizan con la acción que las gestiona. Desde historial se reconoce el caso y las referencias de reportes duplicados sin confundirlos con hechos nuevos. La ayuda explica atribuciones, importación, reiteración, reserva y registros oficiales, sin pedir que el usuario clasifique manualmente cada dato en niveles de confidencialidad.

### RNF-14. Accesibilidad pertinente

Las tareas de registro, búsqueda, atención y consulta de resultados deben poder completarse por teclado, con foco visible y orden coherente, etiquetas/nombres accesibles, identificación textual de errores y estados, sin depender solo del color. Deben admitir ampliación de texto al 200 % sin pérdida de contenido o controles y contraste mínimo de 4,5:1 para texto ordinario y 3:1 para texto grande y elementos de interacción esenciales. Los gráficos deben disponer de valores/tablas equivalentes y no requerir interpretar únicamente colores.

Se verifican los criterios pertinentes de [WCAG 2.2](https://www.w3.org/TR/WCAG22/), especialmente 1.3.1, 1.4.1, 1.4.3, 1.4.4, 1.4.10, 1.4.11, 2.1.1, 2.1.2, 2.4.3, 2.4.7, 3.3.1–3.3.3 y 4.1.2–4.1.3, con inspección y uso de teclado/lector de pantalla. No se afirma conformidad integral por superar solo pruebas automáticas.

### RNF-15. Dispositivos y compatibilidad

Registro, búsqueda, consulta del aporte y bandeja/atención deben ser utilizables desde 320 píxeles CSS de ancho y en computadora desde 1 280, sin pérdida de operaciones esenciales ni desplazamiento horizontal para leer y completar textos ordinarios. Tablas o gráficos bidimensionales pueden tener desplazamiento propio y alternativa comprensible. La importación y administración masiva pueden priorizar computadora, indicándolo antes de iniciar.

Debe funcionar en las dos versiones estables más recientes al momento de aceptación de al menos dos familias de navegadores de escritorio de uso común y en un navegador actual de Android. Se documentan las versiones ensayadas. No debe requerir extensiones de navegador, Microsoft Excel instalado ni software de pago en cada puesto para registrar/consultar y generar informes. No se exige compatibilidad con navegadores sin soporte de seguridad.

## 4.4 Rendimiento, confiabilidad y recuperación

### RNF-16. Respuesta y capacidad proporcionadas

Bajo 4.1, el percentil 95 debe ser de hasta dos segundos para buscar estudiante y hasta tres para guardar una incidencia o consultar historial por tramos. Un aviso inmediato debe estar disponible en la siguiente consulta/actualización de bandeja en hasta cinco segundos desde el guardado; no se prescribe conexión permanente o una tecnología de entrega. Un resumen estadístico ordinario se prepara en hasta diez segundos y un informe de hasta cincuenta incidencias en hasta quince segundos.

Una importación de hasta 1 000 filas y treinta columnas asociadas debe validar y confirmar cada fase en hasta dos minutos, excluyendo revisión humana y transferencia del archivo. La prueba del formato entregado usa 91 estudiantes, encabezados agrupados, huecos y filas auxiliares. Las operaciones largas informan estado/resultado y evitan doble envío; no se exige porcentaje de avance si no se puede calcular. Los límites deben ser explícitos, sin truncar o perder datos silenciosamente.

### RNF-17. Disponibilidad y continuidad escolar

Como objetivo de ingeniería para operación habitual, el servicio debe alcanzar al menos 99 % de disponibilidad mensual durante la jornada escolar documentada para su utilización. La jornada se establece operativamente al instalar, sin crear una configuración horaria por usuario. Se mide si registro y consulta autenticada funcionan; mantenimiento dentro de ese horario cuenta como indisponibilidad. La evidencia puede proceder de registros/mediciones convencionales, sin desarrollar monitoreo empresarial ni encargar vigilancia permanente al personal escolar. El objetivo no exige soporte continuo, guardias o una infraestructura redundante específica.

Las interrupciones conservan lo ya confirmado y evitan duplicados al reintentar. La guía explica registrar posteriormente acciones atendidas durante una caída con sus fechas efectivas y continuar protección por canales institucionales. No se exige almacenamiento sin conexión de datos personales. El ensayo de aceptación debe demostrar tanto el fallo visible como la continuidad después de recuperar, no limitarse a mostrar una página disponible.

### RNF-18. Respaldo y restauración mediante operación sencilla

Debe existir respaldo protegido y recuperable de datos, adjuntos y configuración necesaria, con una pérdida objetivo máxima de veinticuatro horas de cambios confirmados cuando los haya. Se mantiene el objetivo diario porque evita reconstruir una semana de actuaciones; es una meta de ingeniería, no el mínimo legal semanal de los arts. 48 y 51 del DS 016-2024-JUS. Puede satisfacerse con los medios de respaldo elegidos por el implementador, sin un módulo escolar específico.

La recuperación de ensayo del servicio y sus datos debe completarse en hasta dos días hábiles desde que un operador técnico autorizado dispone de medios/credenciales y comienza el procedimiento documentado. Este objetivo sustituye una exigencia de respuesta de ocho horas sin soporte permanente; no supone guardia técnica ni trabajo de TOE. La pérdida del almacenamiento principal no debe destruir todas las copias y debe existir una copia anterior utilizable frente a corrupción reciente. Se documentan frecuencia efectiva, retención, ubicación de responsabilidades, protección y comprobación de integridad; no una arquitectura concreta.

Se debe demostrar restauración antes de uso real, al menos una vez por año escolar y después de cambios que afecten el procedimiento de recuperación. La integridad de las copias se comprueba con medios convencionales según su generación; no se requiere ensayo completo trimestral. El ensayo verifica hechos, vínculos, adjuntos, conteos y accesos, registrando copia, fecha, operador, resultado y tiempo.

Después de restaurar y antes de acceso ordinario, el operador comprueba las designaciones/restricciones vigentes y las decisiones de rectificación, bloqueo o supresión posteriores a la copia, aplicándolas en el alcance necesario. Puede hacerlo con una revisión controlada de sus constancias y medios documentados; no se exige reproducción automática de una bitácora ni reconciliar todos los historiales. Las copias antiguas con información ya retirada permanecen aisladas para recuperación, sin uso ordinario, y vencen según retención documentada; no deben editarse selectivamente una por una. Si se restaura una, se aplican las decisiones válidas antes de volver a exponer datos. No se revive información suprimida ni acceso de un excluido.

Los medios de recuperación deben estar bajo control institucional/autorizado, sin depender exclusivamente del desarrollador original. El ensayo que exceda pérdida o tiempo objetivo no supera aceptación por haberse informado del fallo. Se verifica con CU-15 y CU-16.

### RNF-19. Exactitud y consistencia de resultados

Consultas, bandeja, estadísticas e informes deben usar las mismas definiciones de hecho, rol, período y reserva. Las cifras se deben reconciliar con un conjunto de hechos ficticios de resultado conocido, incluidos duplicados y varios grupos. Una tabla y su gráfico deben coincidir; porcentajes identifican denominador y redondeo y no convierten datos ausentes en cero. Un informe emitido queda referido a su fecha de corte; una consulta actual puede diferir por correcciones posteriores y debe permitir explicarlas con trazabilidad. Se verifica con CU-05 y CU-13, sin exigir reproducir datos históricos personales innecesarios tras una supresión válida.

## 4.5 Mantenibilidad, operación y sostenibilidad

### RNF-20. Documentación y autonomía escolar

La entrega debe incluir instrucciones breves en español para registro/atención, nómina anual, errores de importación, designaciones, parámetros, custodia de exportaciones y procedimientos de interrupción, respaldo y restauración. Un operador escolar capacitado debe poder ejecutar las operaciones habituales y actualizar un catálogo o año sin editar código, acceder al almacenamiento interno ni depender del desarrollador original.

La documentación técnica de mantenimiento debe describir requisitos de ejecución, puesta en marcha, actualización, variables operativas, inventario de dependencias/licencias, datos y relaciones relevantes, exportación/restauración, diagnóstico, pruebas y limitaciones. Se documenta lo efectivamente implementado; esta ERS no elige su diseño. Un técnico distinto debe poder levantar una copia de prueba con datos ficticios y restaurar el respaldo de ensayo usando solo esa entrega y medios autorizados. Las credenciales reales se entregan por un canal protegido, no dentro de ejemplos públicos.

### RNF-21. Operación y respuesta institucional a incidentes

Debe poder conocerse si servicio o respaldos fallan, con fecha/resultado y orientación para contactar al responsable operativo, sin exponer relatos. Pueden utilizarse registros o informes de los medios de operación; no se exige un panel, centro de monitoreo ni gestión de incidentes de seguridad a cargo de TOE. RF-38/RF-41 y la guía deben permitir obtener evidencia y recuperar el servicio.

Corresponde al responsable institucional evaluar y comunicar incidentes comprendidos en arts. 34–36 del DS 016-2024-JUS, incluida la comunicación a ANPD en cuarenta y ocho horas desde conocimiento en los supuestos aplicables. El proveedor comunica sin demora al responsable los incidentes conocidos que afecten sus datos. Su trámite y constancias pueden estar fuera de esta aplicación. Se verifica con evidencia ficticia y procedimiento documentado, sin enviar comunicaciones reales ni desarrollar otro sistema de expedientes.

### RNF-22. Cambios mantenibles y comprobación de regresiones

La solución entregada debe permitir cambiar nombre institucional, año/grupos, catálogo, ventana/umbrales y asociación de columnas sin modificar código. Una nueva versión debe poder instalarse con procedimiento documentado, verificación de conservación de datos/permisos y forma de recuperar la versión o información anterior si falla. El implementador debe entregar pruebas reproducibles de los procesos críticos: permisos/conflicto, identidad/importación, conteos, atención/cierre e integridad de recuperación. No se exige un porcentaje artificial de cobertura ni una tecnología de pruebas. El criterio es que otro responsable pueda detectar una regresión en esos resultados sin conocer al autor original.

### RNF-23. Portabilidad y salida sostenible

Debe poder recuperarse la información completa de RF-41 en formatos documentados que permitan a otro responsable interpretar identidades, vínculos, fechas, roles y archivos sin depender de un visor propietario inaccesible o de la permanencia del proveedor. Los formatos de intercambio pueden diferir de los de operación; no se impone un diseño de almacenamiento. Una prueba de entrega debe reconciliar cantidades, relaciones y adjuntos de una muestra con el sistema. No basta exportar tablas estadísticas o capturas de pantalla.

### RNF-24. Costos y dependencia proporcionados

La entrega debe identificar todos los servicios, licencias, recursos y renovaciones necesarios, quién controla sus cuentas y el costo recurrente estimado para el volumen de 4.1; distinguir costos fijos y variables y los efectos de crecer o cambiar de responsable. No deben existir servicios obligatorios ocultos, cuentas personales irremplazables del desarrollador ni tareas periódicas indocumentadas que solo él pueda ejecutar. La operación ordinaria no puede depender de contratación continua para importar nóminas, recuperar accesos o cambiar categorías.

El colegio no ha fijado un techo monetario; no se inventa una cifra. La verificación consiste en inventario completo, estimación reproducible y demostración de autonomía con los recursos declarados. Este requisito no prohíbe herramientas pagadas ni prescribe alojamiento; exige que sus costos y condiciones puedan evaluarse y sostenerse antes de operar.

### RNF-25. Datos de desarrollo y prueba

Desarrollo, demostraciones, pruebas de aceptación y materiales compartidos deben utilizar identidades y relatos ficticios. No se deben copiar nombres, documentos, teléfonos, direcciones ni relatos reales del Excel/atenciones a ejemplos, repositorios, capturas o entornos públicos. Para comprobar la importación real se utiliza acceso controlado al archivo autorizado, sin trasladar su contenido personal al entregable. Los datos de prueba deben representar homónimos, campos ausentes y violencia sin identificar a menores reales. La inspección de la entrega y evidencias debe confirmar esta separación.

# Capítulo 5. Reglas de negocio

## 5.1 Hechos, personas y autoridad

### RN-01. Un hecho y múltiples comunicaciones

Un suceso cuenta como una incidencia aunque lo comuniquen distintas personas/canales o tenga varios involucrados. Los reportes repetidos se relacionan con un principal sin borrar aportes o autores y no incrementan ocurrencias. Las personas y participaciones pertinentes de todos los reportes vinculados pertenecen a la consulta del mismo hecho, su atención, historiales e informe, sin transcripción al principal; su procedencia y diferencias se conservan según RF-18. La identificación es una decisión autorizada, no coincidencia automática. No se exige fusionar contenidos/casos y la relación no amplía el permiso de los reportantes.

### RN-02. Incidencia no equivale a infracción

Una comunicación puede representar conducta disciplinaria, victimización, riesgo, violencia o convivencia. Registrar o importar no acredita verdad, culpabilidad, delito o sanción. La naturaleza se refiere al hecho; el rol y su confirmación, a cada persona. Los antecedentes de violencia sufrida nunca se consideran faltas cometidas por esa persona.

### RN-03. Clasificación y cambios

Se usa un catálogo institucional breve. Leve/grave/muy grave expresa clasificación escolar declarada, no gravedad jurídica probada. Si falta certeza se registra por clasificar y puede pedirse atención inmediata. Las categorías graves/muy graves y de violencia/protección avisan desde el primer hecho. Los cambios de catálogo rigen para nuevos usos y no reescriben automáticamente la historia.

### RN-04. Atribución cotidiana y responsabilidad por confirmar

Cada persona conserva su papel por hecho, sea estudiante, trabajador, familiar u otra persona externa. La identificación parcial no impide registrar oportunamente su participación ni autoriza atribuirle el papel de otra persona. Una conducta cotidiana disciplinaria no grave atribuida por el autor se cuenta como registrada desde que se guarda, sin corroboración obligatoria de TOE. Una denuncia de violencia conserva presunta responsabilidad por confirmar y nunca recibe por defecto la condición cotidiana o corroborada. Papel desconocido no equivale a presunta responsabilidad; afectados/testigos no son infractores. TOE/comité aclara lo que efectivamente investiga, sin una tarea por cada nota. Ninguna atribución equivale por sí sola a culpabilidad o sanción.

### RN-05. Registro cotidiano autónomo

Docente, auxiliar y directivo registran sin aprobación o validación individual de TOE. La conducta cotidiana atribuida no se queda fuera de la reiteración por no ser investigada. Una actuación/compromiso inmediato puede guardarse junto al hecho sin atención formal; el hecho permanece en historia. La confirmación del guardado no crea automáticamente caso ni tarea de revisión.

### RN-06. Capacidades institucionales y responsabilidades específicas

TOE recibe y atiende habitualmente; el comité tiene capacidades institucionales equivalentes para consulta, atención e informes/estadísticas, añadidas al rol ordinario durante su membresía vigente y siempre sin conflicto. Esto no transfiere automáticamente a todos los miembros la recepción cotidiana. TOE convoca ordinariamente y determina habitualmente agrupaciones; el director registra los actos colectivos. Los demás miembros mantienen capacidad de atención cuando ejercen su función y sustituyen conforme a RN-08. Definir/cargar parámetros es una responsabilidad de configuración asignada a TOE/director, con carga delegable al administrador; no un privilegio de lectura/atención superior o una nueva jerarquía.

### RN-07. Reserva ordinaria y privilegios

El reportante ordinario consulta sus propios aportes y acciones inmediatas. No consulta hechos ajenos del estudiante ni actuaciones institucionales posteriores. TOE/comité tiene el alcance institucional necesario y reservado. AIP importa contexto admitido, pero no adquiere por ello lectura longitudinal o de expedientes. Los permisos administrativos no equivalen a acceso funcional.

### RN-08. Conflicto prevalente y sustitución

Un miembro denunciado o con conflicto registrado no interviene ni consulta el caso afectado. Si se denuncia a TOE o director, actúa el resto del comité no implicado; no se necesita autorización del denunciado. Si el director está excluido, un miembro restante documenta excepcionalmente. La exclusión es por caso/hechos relacionados y prevalece sobre rol, autoría o administración; no renueva el comité ni inhabilita a la persona para todos los asuntos. No puede retirarse a sí misma esa restricción.

## 5.2 Reiteración, atención y comité

### RN-09. Parámetros institucionales mínimos

Cada categoría que use reiteración tiene su umbral entero aprobado y todas comparten una ventana positiva de días calendario. No existe un número universal, una regla aprobada de tres, ni una definición vigente de «consecutivas». Sin valor aprobado no se activa ese cálculo. Los ejemplos numéricos de CU son solo pruebas ficticias.

### RN-10. Unidad de reiteración

Se cuenta un hecho único por estudiante/categoría dentro de la ventana, utilizando la participación pertinente del estudiante en cualquier reporte vinculado y sin sumar otra ocurrencia por duplicidad. Una discrepancia que impida determinar atribución elegible, sin aclaración vigente, queda fuera del contador de responsabilidad/reiteración hasta aclararse; no se elimina a la persona como involucrada ni se retrasa el aviso por gravedad. La presunta responsabilidad de un familiar o externo no se transfiere al estudiante afectado. Las conductas cotidianas atribuidas por su autor cuentan sin validación de TOE; las presuntas responsabilidades por confirmar se identifican como reportes no corroborados de su categoría, separadas de atribuciones corroboradas. Afectados, testigos, otros sin atribución, papel desconocido, atribución descartada, contexto y hechos anulados no incrementan el contador de responsabilidad. La duplicidad de reporte no añade ocurrencias ni descarta participaciones elegibles presentes solo en él. La señal informa y no sanciona, confirma culpabilidad o cambia gravedad automáticamente.

### RN-11. Avisos útiles sin recepción duplicada

Gravedad y reiteración son motivos distintos que pueden concurrir en un solo asunto; gravedad no espera umbral. Leer un aviso no atiende. Abrir/vincular/documentar la atención, cerrar en el mismo acto o marcar duplicidad actualiza los avisos relacionados sin otro trámite. No abrir caso puede dejarse con una disposición breve, utilizando el resultado ya documentado; descartar gravedad sin acción requiere fundamento. Hechos nuevos relevantes pueden producir otra señal, sin reapertura automática o avisos repetidos por un recálculo sin novedad.

### RN-12. Caso compartido y referencias históricas

TOE agrupa habitualmente hechos relacionados y comité conserva la capacidad conforme a su función. No se exige un caso por estudiante ni varias atenciones locales pendientes del mismo hecho. Puede continuarse en un caso existente con referencias a atenciones previas, preservando actuaciones sin copiarlas o fusionarlas. Las participaciones de familiares, otras personas externas y reportes duplicados se consultan en ese mismo caso según su hecho y procedencia, sin otro expediente o selección repetida de participantes. Agregar vínculos ordinarios no exige textos adicionales; corregir un vínculo sustancial deja causa breve. Hechos nuevos pueden originar otra atención y citar historia cerrada sin contarla de nuevo ni reabrirla automáticamente.

### RN-13. Documentar la intervención real en un solo acto

Se puede registrar directamente una actuación realizada, resultado, compromiso y cierre sin programarla primero. Programación y seguimiento solo se crean cuando exista trabajo real. Una prevista no suma como realizada hasta ejecutarse; una cancelación tiene causa breve. El compromiso requiere contenido y quien lo asume, plazo solo si se acordó. No exige tarea o comprobación adicional por defecto, ni se presume cumplido por cerrar el caso. Vencimiento/incumplimiento no sanciona automáticamente. Actuaciones, compromisos y derivaciones son datos, sin documentos obligatorios por cada acción.

### RN-14. Comité como intervención excepcional

TOE convoca en un caso especialmente grave no resuelto, particularmente violencia, con motivo. No se convoca por toda gravedad ni reincidencia. La composición estable y la elegibilidad del caso son distintas. La intervención conserva el mismo caso; el acta se elabora fuera y se adjunta, mientras el director o sustituto habilitado registra sus acuerdos y actos.

### RN-15. Cierre local con historia y continuidad pertinente

Una conclusión local se registra con fecha y resultado suficiente, sin repetir motivo ni verificar administrativamente cada rol. El cierre puede guardarse junto a la intervención. Las acciones/seguimientos realmente pendientes siguen visibles sin justificar de nuevo su continuidad ni marcarlos cumplidos. Lo cerrado sin trabajo pendiente queda solo en historia. Se pueden documentar seguimientos de protección posteriores sin reabrir ni alterar el cierre; un cierre equivocado se corrige con causa y trazabilidad.

### RN-16. Derivación y supuesto de violencia familiar

Preparar no equivale a efectuar una derivación. La derivación externa no causa cierre general. En el supuesto institucional confirmado de violencia familiar derivada efectivamente a Fiscalía, se cierra la atención **local** por derivación cuando esta abarca su objeto completo; un caso mixto con otros hechos pendientes conserva su atención pendiente. Se conserva consulta, continuidad de protección y seguimiento que corresponda; no se declara cerrado el procedimiento fiscal ni el protocolo escolar oficial. Esta distinción armoniza el insumo con el protocolo 7 de RM 383-2025, que comprende el entorno familiar/comunitario y mantiene obligaciones de acompañamiento y cierre propias.

## 5.3 Información escolar, informes y estadísticas

### RN-17. Identidad y año independientes

La identidad del estudiante y su historia permanecen al cambiar año, grupo o matrícula. El documento válido de estudiante es criterio preferente de coincidencia; nombre, orden del Excel, grupo o documento/teléfono familiar no son sustitutos de identidad única. Un grupo ausente puede completarse después, sin inventarlo ni modificar el grupo histórico conocido de los hechos. Ausencia en un lote no significa retiro/eliminación. No se infiere promoción del nombre de archivo, edad o grado previo.

### RN-18. Matrícula selectiva y datos desconocidos

AIP importa campos asociados y pertinentes; una columna existente no autoriza su uso, especialmente salud/familia. Vacío no es negación ni permiso para borrar. Nombres, DNI y teléfonos de estudiante/familiar se distinguen por sus encabezados agrupados. Hermanos P/S no son sexo. Filas de título, etapa u orden sin estudiante no crean personas. Identidad válida puede cargarse con año y grupo no informado. Un DNI numérico completo se conserva textual; no se inventan dígitos ni se unen ambiguos automáticamente. La importación no exige diseñar un formato universal.

### RN-19. Contexto y protección

Un antecedente conserva procedencia y calidad declarada, sin convertirse en una falta o hecho nuevo. Solo se tratan datos necesarios para la finalidad y base válidas. Si el fundamento es consentimiento, deben observarse las condiciones de la Ley 29733 art. 13.6 y del reglamento, arts. 22–25, sobre menores y datos familiares; no se presume consentimiento universal por matricularse. No se crea un banco de antecedentes penales ni se registra una acusación como condena, conforme al límite de Ley 29733 art. 13.8.

### RN-20. Informe y acta diferentes

El informe reúne hechos y atención relacionados; el acta externa documenta la reunión institucional en su formato propio. No se genera un informe aislado por cada incidencia de un mismo caso. Las salidas distinguen atribuciones pendientes y papeles individuales por hecho, incluidos familiares/personas externas y participantes que consten solo en reportes duplicados, con su procedencia y discrepancias pertinentes según RF-18/RF-31. No se repite el hecho ni se transcribe lo ya registrado para incorporarlos. Los documentos de violencia dirigidos a terceros usan iniciales y DNI disponible según III.3.8 del anexo 03 actualizado; la identificación completa interna no se publica por ello.

### RN-21. Referencias oficiales sin sustitución

Registrar localmente no equivale a reportar al Libro/SíseVe ni a cumplir sus plazos o cierre. El colegio determina y ejecuta el instrumento y protocolo que correspondan. No todo conflicto es violencia ni todo hecho familiar exige registro SíseVe. Para violencia familiar/comunitaria, el protocolo 7 prevé su registro institucional en Libro o cuaderno de actas y actuaciones propias; la aplicación puede conservar su referencia sin imponer un portal incorrecto. Los casos históricos mantienen la referencia al protocolo bajo el que se atendieron; no se recalifican retrospectivamente solo por una nueva norma.

### RN-22. Unidades, cobertura y medidas estadísticas

Hechos, participaciones, estudiantes, atribuciones, casos, señales y actuaciones son unidades distintas. La atribución cotidiana por autor no se presenta como denuncia corroborada, y víctimas/testigos no suman responsables. Se usan fecha/grupo históricos conocidos y se separan datos no informados; no se fabrican sexo u horas. Los subtotales por grupos afectados pueden solaparse. Un porcentaje de estudiantes distintos usa la misma población en numerador/denominador; frecuencia de hechos por cien escolares se rotula de otro modo, con período. No se presenta tasa de exposición exacta sin datos que la sustenten.

### RN-23. Reserva proporcional al uso de resultados

TOE/director/comité autorizados necesitan cifras internas exactas y pueden consultar o descargar grupos pequeños dentro de su ámbito. Las normas citadas no establecen una obligación general de ocultar grupos de menos de cinco estudiantes; esta ERS no impone una regla automática por ese número. Listados nominativos son reservados, distintos de agregados. La entrega externa requiere finalidad/base/destinatario y revisión institucional del riesgo de identificación, con agregación u omisión necesarias; quitar nombres no garantiza anonimato. Un conflicto limita también consultas y salidas relacionadas. No hay publicación o envío externo automáticos.

## 5.4 Protección y conservación normativas

### RN-24. Responsabilidad institucional sobre derechos

El responsable institucional determina finalidad, base aplicable y excepciones al consentimiento de Ley 29733 art. 14. Tramita derechos por sus canales, comprueba identidad/representación y decide entrega, rectificación, limitación o disposición procedentes protegiendo terceros. La aplicación aporta los resultados de RF-39 mediante capacidades existentes y medios operativos documentados, sin administrar ese procedimiento como un módulo. El bloqueo se aplica con las condiciones/excepciones del art. 20, no por automatismo universal. No se pide permiso al presunto agresor para registrar protección legalmente amparada.

### RN-25. Conservación fundada y recuperación segura

Cambiar de año o cerrar no elimina la información necesaria; tampoco autoriza conservarla identificable para siempre. El responsable define plazos/criterios y retenciones archivísticas o legales fuera del sistema de reglas de la aplicación. La disposición autorizada alcanza datos y copias operativas pertinentes y protege a terceros. Las copias de recuperación permanecen aisladas, vencen según retención documentada y no se usan para recuperar datos/accesos retirados sin aplicar antes las decisiones válidas. No se exige editar cada respaldo o automatizar reconciliación histórica. Ley 29733 arts. 8 y 28.7 sustentan conservación limitada; los dos años mínimos de DS 016-2024-JUS art. 46.3 son de trazabilidad, no plazo universal de expediente escolar.

### RN-26. Confidencialidad y evidencia

Acceso y transmisión requieren autorización y finalidad. Los operadores conservan deber de confidencialidad incluso al terminar sus funciones, según Ley 29733 art. 17; el software retira accesos y conserva evidencia necesaria. El documento de seguridad, inscripción del banco, revisión de privilegios y designación de ODP que correspondan son responsabilidades organizativas. No se les asigna automáticamente acceso irrestricto a casos.

### RN-27. Protección no supeditada al sistema

Las comunicaciones, medidas urgentes y seguimiento exigibles se realizan por el personal competente aunque la aplicación esté caída, sin esperar confirmación de roles ni aprobación electrónica. El registro posterior debe reflejar fechas y acciones efectivas. Cerrar localmente no certifica que se hayan ejecutado todas las acciones normativas: ese cumplimiento permanece bajo responsabilidad institucional.

### RN-28. Parámetros variables y comportamientos estables

Solo varían razonablemente datos del colegio, años/grupos, designaciones, catálogo y gravedad, umbrales/ventana, asociación de columnas y conceptos contextuales admitidos. Integridad de identidad, hecho único, no culpabilidad automática, reserva, exclusión por conflicto y distinción entre cierre local/oficial son comportamientos estables y no configuraciones opcionales. No pueden desactivarse para simplificar la operación.

# Capítulo 6. Casos de uso y criterios de aceptación

## 6.1 Convenciones de aceptación

Todos los estudiantes, usuarios y relatos utilizados a continuación son ficticios. Las cifras de ventana/umbral de pruebas no son parámetros institucionales aprobados. Los criterios son acumulativos con el capítulo 2 y los RNF relacionados. En cada prueba se verifica también que la operación produce trazabilidad, conserva integridad y no amplía permisos por relaciones indirectas. Un flujo no puede darse por aceptado solo porque funciona con la cuenta de mayor privilegio.

### CU-01. Importar matrícula, contexto pertinente y actualización anual

**Objetivo:** cargar la matrícula real con mínimo trabajo, identidad estable y selección proporcionada de datos.

**Actores:** AIP; administrador para año/grupos; responsable institucional que determina datos admitidos, sin aprobar cada fila.

**Precondiciones:** AIP autenticado, año identificado y alcance de datos admitidos conocido. La prueba reproduce la estructura de la matrícula con información ficticia.

**Flujo principal:**

1. AIP selecciona archivo/año y revisa asociaciones propuestas a partir de los encabezados agrupados; la asociación del formato conocido se reutiliza.
2. Revisa campos omitidos, registros válidos, cobertura de grupo y excepciones de identidad; puede aportar un grupo común o por bloque si lo conoce.
3. Resuelve únicamente excepciones o excluye sus filas. Confirma el lote una vez.
4. El sistema crea/actualiza identidades y vínculos anuales y muestra resultado/procedencia sin duplicados.
5. En otro año, el documento de estudiante válido permite reconocer automáticamente las coincidencias seguras y conservar historia; AIP revisa solo contradicciones.

**Alternativas significativas:** grupo no informado, títulos/etapas y filas vacías, columnas movidas, contacto familiar compartido, estudiante sin documento, DNI contradictorio, columna de contexto futura, cancelación o fallo/reintento.

**Postcondiciones:** identidades y vínculos confirmados completos, campos pertinentes incorporados, excepciones identificadas, historia y permisos preservados.

**Requisitos relacionados:** RF-02, RF-04, RF-06–RF-11, RF-40; RNF-02, RNF-04–RNF-05, RNF-08, RNF-11–RNF-12, RNF-16, RNF-25; RN-17–RN-19, RN-24–RN-25, RN-28.

**Criterios de aceptación:**

- Una réplica ficticia de Hoja1 de 121 filas y 17 columnas reconoce 91 estudiantes de la fila 7 y las filas 15–104, no la etiqueta de etapa, huecos o filas con solo orden. La numeración repetida no produce un duplicado. C identifica nombre y D documento del estudiante; L/M/N identifican contacto familiar/documento/teléfono sin intercambiarlos. Las letras son referencia de la muestra, no formato obligatorio.
- Se importan los 91 documentos numéricos completos como texto de ocho cifras sin alterarlos. Una repetición posterior del lote no duplica personas/vínculos/contexto. El mismo documento o teléfono familiar para dos estudiantes no los une.
- B está vacía: sin información adicional se crean identidades y año con grupo no informado. Si AIP asigna un grupo conocido al lote o bloque, se presenta antes de confirmar. No se deduce el grado/sección del nombre de archivo ni de hermanos P/S. Completar grupo después mantiene identidad e historia.
- Se omiten orden, hermanos e instrucción/ocupación familiar. Salud/discapacidad/convivencia y observaciones se incorporan solo en el alcance pertinente seleccionado. Una celda «no» se distingue de vacía; un número en observación no se convierte en diagnóstico o falta.
- Otro archivo mueve columnas, separa grado y sección y añade «Bullying previo informado»: se asocia sin código y se incorpora como antecedente solo si está admitido. No exige otra aprobación por estudiante. Si falta sexo, queda desconocido.
- Un estudiante sin DNI puede cargarse con identidad interna; dos homónimos ambiguos no se unen automáticamente. Un DNI contradictorio se señala y excluye de actualización hasta resolverlo.
- Una celda vacía no elimina el contacto/grupo anterior; un borrado se pide expresamente y muestra alcance. Fallo/reintento no deja filas parcialmente actualizadas ni altas duplicadas. AIP no obtiene por el lote acceso a casos o antecedentes históricos ajenos a la conciliación autorizada.

### CU-02. Registrar una conducta cotidiana sin validación de TOE

**Objetivo:** guardar un hecho atribuible a un estudiante y su acción inmediata en una operación breve que ya sirva para detectar reiteración.

**Actores:** docente, auxiliar u otro directivo ordinario.

**Precondiciones:** cuenta activa, estudiante localizable y categoría disciplinaria cotidiana no grave.

**Flujo principal:**

1. El autor selecciona al estudiante al que atribuye la conducta, categoría y descripción; se proponen fecha y datos conocidos.
2. El registro expresa esa atribución cotidiana por el autor, visible/corregible sin otro trámite de confirmación. Puede añadir afectados, testigos u otros con su propio papel.
3. Si hubo conversación/compromiso inmediato, lo documenta en el mismo acto.
4. Guarda una vez; el sistema conserva hecho, autoría y señal de reiteración si corresponde, sin atención automática.
5. El autor consulta y puede corregir su aporte conforme a RF-17.

**Alternativas significativas:** ningún compromiso, grupo desconocido, fecha pasada, varios involucrados, papel desconocido explícito, fallo de conexión o atención institucional posterior reservada.

**Postcondiciones:** una nota cotidiana computable por su atribución, sin tarea de corroboración, caso o aprobación obligatoria para TOE.

**Requisitos relacionados:** RF-04, RF-12–RF-15, RF-17, RF-20; RNF-01–RNF-02, RNF-08, RNF-10–RNF-15; RN-02, RN-04–RN-07, RN-10, RN-13.

**Criterios de aceptación:**

- El auxiliar registra una conducta cotidiana del estudiante ficticio Samira Torres Luna y una conversación realizada sin abrir caso, adjuntar acta ni esperar a TOE. Docente/directivo ordinario tienen la misma capacidad.
- El estudiante objeto de la anotación figura con conducta atribuida por su autor, sin marcarse corroboración de TOE ni culpabilidad. La nota cuenta en su categoría aunque TOE nunca la revise.
- Añadir una persona como afectada/testigo o dejar papel desconocido no la añade al contador de responsabilidad. Todos comparten un hecho, sin repetir relato/fecha.
- Autor, registro, clasificación y grupo conocido no se transcriben. Una acción y compromiso se guardan opcionalmente con el hecho. No se pide motivo extra para una corrección rutinaria anterior a atención.
- El autor consulta solo su aporte; no obtiene otras notas o actuaciones reservadas posteriores. Error/caída conserva texto y no declara éxito falso ni duplica por reintento.

### CU-03. Recibir una incidencia grave no corroborada

**Objetivo:** advertir a los responsables desde la primera comunicación sin prejuzgar a involucrados.

**Actores:** registrador ordinario; TOE o sustitutos elegibles; estudiantes, personal, familiares u otras personas externas como participantes del hecho, sin necesitar cuentas.

**Precondiciones:** categoría grave/protección o solicitud expresa de atención; destinatario institucional vigente o necesidad de designación.

**Flujo principal:**

1. El registrador comunica el hecho con datos conocidos y papeles individuales de los involucrados, que pueden incluir personal, familiares y otras personas externas con identificación parcial.
2. El sistema conserva roles informados/no confirmados, aplica restricciones preventivas pertinentes y genera aviso/bandeja.
3. TOE consulta el asunto, inicia atención o lo vincula a una existente.
4. Registra protección/actuaciones y precisa roles conforme averigua, sin reescribir la comunicación original.

**Alternativas significativas:** estudiante todavía no identificado, docente denunciado que tiene cuenta, fecha aproximada, TOE implicado, o aviso leído pero aún no atendido.

**Postcondiciones:** comunicación y señal conservadas; receptor autorizado puede actuar; la creación de atención y confirmación de roles siguen siendo decisiones humanas.

**Requisitos relacionados:** RF-05, RF-12–RF-13, RF-19, RF-21–RF-22, RF-24, RF-28; RNF-02, RNF-06–RNF-08, RNF-13, RNF-16; RN-02–RN-04, RN-08, RN-11, RN-27.

**Criterios de aceptación:**

- Una primera comunicación de agresión genera señal sin esperar otra incidencia ni confirmar quién fue responsable. Leerla no resuelve el asunto; documentar su atención actualiza el aviso sin un trámite adicional de recepción.
- «Afectado / por confirmar», «presunto responsable / por confirmar» y «papel desconocido» se conservan distinguibles; ningún participante queda marcado culpable por defecto.
- Un trabajador sin cuenta puede figurar como presunto responsable. Si después se identifica su cuenta privilegiada, se aplica la exclusión a ese ámbito.
- Si TOE es el denunciado identificado, el aviso se dirige solo a miembros restantes elegibles y TOE no puede abrirlo mediante su bandeja ni el historial del estudiante.
- Un reporte urgente sin identificación completa se guarda como tal y después puede precisarse, sin crear un alumno ficticio. Un familiar u otra persona externa puede constar como presunto responsable por confirmar, afectado o testigo con nombre/referencia conocidos, sin DNI, identificación completa o alta en un padrón. Esto no concede una cuenta ni acceso al expediente.

### CU-04. Detectar reiteración sin confirmar notas cotidianas

**Objetivo:** avisar por patrones de una categoría distinguiendo atribución cotidiana de denuncias no corroboradas.

**Actores:** registradores; TOE/comité elegible.

**Precondiciones:** solo para prueba, categoría cotidiana con umbral tres y ventana diez días. Fecha ficticia de evaluación: 20-10-2026; ventana 11–20 inclusive. No son parámetros institucionales aprobados.

**Flujo principal:**

1. Se guardan conductas cotidianas atribuidas al mismo estudiante y categoría, sin revisión posterior de TOE.
2. Al alcanzar umbral, el sistema emite una señal sustentada en esos hechos y su carácter de atribución.
3. TOE consulta y documenta atención o disposición pertinente usando los datos existentes.
4. Esa intervención actualiza avisos en la misma operación; si queda trabajo, continúa como atención pendiente.

**Alternativas significativas:** víctima/testigo, papel desconocido, antecedente, duplicado, límite de ventana, denuncia de violencia por confirmar, registro tardío, corrección o falta de parámetros.

**Postcondiciones:** señal útil y trazable sin sanciones, aprobación cotidiana ni duplicidad de tareas.

**Requisitos relacionados:** RF-03, RF-12–RF-13, RF-17–RF-24, RF-34; RNF-07–RNF-08, RNF-10–RNF-13, RNF-19; RN-04–RN-05, RN-09–RN-12.

**Criterios de aceptación:**

- Tres conductas cotidianas de fechas 11, 15 y 20 activan reiteración sin que TOE confirme ninguna. La señal dice que son atribuciones del registro cotidiano, sin presentarlas como investigación de TOE. Un hecho del 10 queda fuera.
- Tres participaciones como víctima/testigo/papel desconocido y un antecedente de bullying sufrido no incrementan el contador de responsabilidades. Un reporte marcado duplicado del mismo hecho no suma otra ocurrencia.
- Una primera denuncia grave avisa aunque todos sus papeles estén por confirmar. Si su categoría evalúa reiteración, la señal identifica presuntas responsabilidades reportadas, separadas de conductas cotidianas o atribuciones corroboradas.
- Registrar atención o vincular la señal a un caso actualiza el aviso sin otra confirmación de recepción. Consultar/recalcular sin novedad no agrega avisos. Un pendiente real no desaparece por leerlo.
- Descartar una atribución o corregir duplicidad recalcula. Un hecho nuevo tras cierre puede generar otra señal, sin reapertura automática. Sin umbrales/ventana aprobados se indica cálculo inactivo y permanecen gravedad y solicitud manual.

### CU-05. Identificar un duplicado conservando aportes y reserva

**Objetivo:** evitar doble conteo con una referencia sencilla, sin fusionar expedientes.

**Actores:** dos reportantes; TOE/comité elegible.

**Precondiciones:** dos registros del mismo suceso con aportes distintos, uno con referencia externa; el duplicado contiene además un participante o dato de participación que no figura en el principal.

**Flujo principal:**

1. El responsable comprueba que es el mismo suceso y elige el registro principal.
2. Marca el otro como reporte duplicado del principal, conservando relatos, autores y fechas.
3. El sistema recupera para el mismo hecho y atención todos los participantes y papeles de los reportes vinculados, con su procedencia, sin transcripción o alta repetida en el principal. El responsable solo aclara una discrepancia cuando haga falta y continúa la atención con los antecedentes existentes.
4. El sistema computa un hecho y actualiza sus avisos y referencias de consulta, respetando los permisos de cada aporte.

**Alternativas significativas:** papeles discrepantes de una misma persona, identificación externa parcial, hechos parecidos pero distintos, relación de duplicidad equivocada, dos atenciones ya documentadas o restricción por conflicto en cualquiera de los aportes.

**Postcondiciones:** un hecho computable con todas sus participaciones pertinentes disponibles según permisos; originales, autores, diferencias e historia intactos, sin fusión/reversión de contenidos o tareas de reconstrucción.

**Requisitos relacionados:** RF-05, RF-12–RF-13, RF-15–RF-18, RF-20–RF-24, RF-28, RF-31, RF-33–RF-34; RNF-02, RNF-07–RNF-08, RNF-12, RNF-19; RN-01, RN-04, RN-07–RN-08, RN-10–RN-12, RN-20–RN-22.

**Criterios de aceptación:**

- Dos reportes pasan a representar un hecho computable, conservando dos autores y sus textos, participantes, papeles y referencias originales. No se combinan automáticamente relatos o papeles contradictorios.
- En un ejemplo ficticio, el principal contiene a Estudiante Alfa como afectado; el duplicado lo repite con el mismo papel y añade a Estudiante Beta como testigo y a Familiar F como presunto responsable por confirmar, sin DNI. Al vincular, el hecho tiene tres participantes pertinentes y dos estudiantes distintos; Alfa cuenta una sola participación y ninguno recibe la responsabilidad informada de Familiar F. El total sigue siendo un hecho.
- El hecho aparece una vez en el historial autorizado de Alfa y Beta con sus papeles correctos, aunque Beta esté solo en el duplicado. La atención y el informe consolidado recuperan también a Familiar F y la procedencia de las participaciones sin que TOE vuelva a introducirlas o complete obligatoriamente el principal.
- En otra prueba, una atribución elegible de un estudiante consta solo en el duplicado: cuenta una vez en reiteración según su carácter, sin validación adicional de una nota cotidiana. Si dos aportes discrepan de modo que no puede determinarse la atribución elegible y no existe aclaración vigente, las versiones permanecen visibles como discrepantes, la persona sigue como involucrada y no se le computa responsabilidad/reiteración hasta aclararla. La gravedad sigue avisando. La aclaración de RF-24 no modifica los aportes originales.
- Referencias externas parciales que podrían ser de personas distintas no se unifican solo por nombre o parentesco. Toda información queda consultable con su procedencia, sin exigir conciliación exhaustiva de identidades.
- Cada reportante sigue viendo solo su aporte, también si es duplicado; no ve el del otro, los participantes añadidos por otro reporte o el expediente. Los operadores autorizados acceden al principal y comunicaciones relacionadas dentro de sus permisos y exclusiones.
- Una exclusión se mantiene al relacionarlos. Los avisos del duplicado dejan de ser tarea independiente al guardar la relación.
- Si ya había dos atenciones, se elige dónde continúa ese hecho y se conserva la anterior con sus acciones como referencia; no se exige fusionar o copiar actuaciones. Otra atención puede seguir por hechos diferentes que conserve. Un cierre por apertura duplicada no aumenta el indicador de atenciones efectivas resueltas.
- Si se descubre que eran sucesos distintos, retirar la marca/referencia con causa breve restablece su conteo como dos sin reconstruir datos, porque ninguno fue borrado o mezclado.

### CU-06. Agrupar hechos y consultar su historia compartida

**Objetivo:** atender conjuntamente hechos relacionados de distintas personas sin expedientes por estudiante o fusión de casos.

**Actores:** TOE en recepción habitual; miembros del comité con sus facultades y sin conflicto.

**Precondiciones:** tres hechos relacionados con varios estudiantes y papeles distintos según el hecho.

**Flujo principal:**

1. El responsable utiliza un caso existente o inicia una atención desde uno de los hechos.
2. Selecciona las otras incidencias relacionadas; sus datos/personas se recuperan sin transcripción ni justificación rutinaria de cada vínculo.
3. Registra acción común o dirigida a personas/hechos concretos y consulta antecedentes autorizados.
4. Si un vínculo era erróneo, lo corrige con explicación breve sin borrar actuaciones.
5. Consulta el mismo caso desde las historias de las personas pertinentes.

**Alternativas significativas:** hecho ya en atención pendiente, atención anterior cerrada, grupo no informado/completado después o caso que contiene una exclusión.

**Postcondiciones:** atención coherente, hechos únicos y roles por hecho, antecedentes y reserva preservados.

**Requisitos relacionados:** RF-11, RF-13, RF-16, RF-18, RF-22–RF-25, RF-28; RNF-02, RNF-08, RNF-10–RNF-13, RNF-19; RN-04, RN-06, RN-08, RN-12, RN-17, RN-22.

**Criterios de aceptación:**

- Un caso reúne tres hechos y distintas personas sin atribuir a todas el mismo papel. Puede incluir familiares/personas externas y participantes registrados solo en reportes duplicados: se recuperan automáticamente en el contexto del caso, con su papel por hecho y procedencia. Una actuación común cuenta una vez.
- La agrupación no exige otra descripción de los hechos o un motivo por cada selección. Si hay atención previa se conserva como antecedente sin fusionar automáticamente sus acciones.
- Corregir un vínculo no elimina el hecho ni actuaciones. Retirar el último hecho de una apertura errónea conserva el caso cerrado por error y la referencia necesaria, sin caso pendiente vacío.
- El cambio de segundo A a tercero B conserva identidad y grupo histórico; un grupo inicialmente no informado solo se completa con dato válido, sin inventar retroactivamente todos los grupos de sus hechos.
- Un reportante ordinario o miembro excluido no obtiene otro aporte/caso por buscar a otro estudiante relacionado. Una historia cerrada se cita sin sumar una ocurrencia nueva.

### CU-07. Documentar una intervención y cerrar en un solo acto

**Objetivo:** resolver una atención sencilla sin planificación o validaciones administrativas ficticias, conservando lo realizado y compromisos.

**Actores:** TOE; director/sustituto habilitado si interviene formalmente el comité.

**Precondiciones:** incidencia o alerta consultable y operador autorizado.

**Flujo principal:**

1. El responsable accede al asunto conservando hechos y personas de origen.
2. Registra una conversación ya realizada, su resultado y compromiso si lo hubo, con datos conocidos propuestos.
3. Indica que esa atención concluyó y guarda todo con una confirmación final.
4. El sistema conserva la atención cerrada y su historia y actualiza los avisos relacionados sin otra recepción o retirada.

**Alternativas significativas:** atención que sí necesita continuidad; programación opcional de conversación; compromiso con plazo o sin él; seguimiento posterior de protección; corrección de un cierre equivocado.

**Postcondiciones:** trabajo real documentado sin expediente/programación previos obligatorios; solo pendientes pertinentes en bandeja.

**Requisitos relacionados:** RF-14, RF-21–RF-22, RF-24–RF-26, RF-30–RF-31; RNF-07–RNF-08, RNF-10–RNF-13; RN-04–RN-05, RN-11–RN-15, RN-27.

**Criterios de aceptación:**

- Desde una alerta se guardan conversación, resultado, compromiso y cierre sin guardados separados de recepción, apertura, programación, confirmación de roles cotidianos y cierre. El resultado suficiente no se copia en un motivo adicional.
- Un compromiso sin plazo se conserva, no figura vencido ni se presume cumplido. No crea una tarea de comprobación si no se acordó seguimiento; si existe plazo o seguimiento explícito pendiente, permanece visible.
- En la alternativa programada, una prevista no cuenta realizada; al ejecutarse se completa la misma actuación. Cancelar deja causa breve sin sumarla como hecha.
- Cerrar no exige justificar cada pendiente que continúa ni lo oculta. Un seguimiento posterior mantiene fecha de cierre local. Un nuevo hecho puede originar otra atención por decisión humana.
- Corregir un cierre erróneo deja causa/autor/fecha, sin eliminar la evidencia original.

### CU-08. Convocar al comité y documentar su intervención

**Objetivo:** atender un caso especialmente grave no resuelto por TOE con continuidad y responsabilidad clara.

**Actores:** TOE; director; docente y auxiliar miembros estables del comité.

**Precondiciones:** comité vigente; caso pendiente; ausencia de conflicto para los participantes de este escenario.

**Flujo principal:**

1. TOE registra convocatoria y motivo en el caso existente.
2. Los miembros elegibles consultan lo pertinente y se registra quién interviene.
3. El director documenta decisiones, acciones, acuerdos y derivaciones.
4. Adjunta el acta elaborada externamente cuando esté disponible y continúa/cierra según resultados.

**Alternativas significativas:** caso grave que resuelve TOE sin comité, composición renovada, acta disponible posteriormente y sustitución del director conforme a CU-09.

**Postcondiciones:** mismo caso e historia; autoría real de decisiones del órgano y acta vinculada sin generación interna.

**Requisitos relacionados:** RF-01, RF-22–RF-23, RF-25–RF-27, RF-29–RF-32; RNF-02, RNF-07, RNF-10, RNF-13; RN-06, RN-12–RN-16, RN-20.

**Criterios de aceptación:**

- Una reincidencia cotidiana o primera gravedad no convoca automáticamente. TOE puede atenderla ordinariamente.
- El docente/auxiliar miembro vigente puede consultar información institucional, actuaciones reservadas y estadísticas por esa responsabilidad, con capacidad equivalente a TOE; el usuario con el mismo cargo ordinario sin membresía no puede.
- La convocatoria no crea un comité distinto ni otra atención. Las decisiones de la intervención formal tienen como autor al director, no automáticamente a TOE.
- El sistema acepta compromisos y derivaciones como datos y permite añadir el acta después. No solicita un Word para cada acuerdo ni firmas electrónicas de todos para guardar.
- La renovación de membresía retira capacidades futuras sin borrar autoría ni composición histórica.

### CU-09. Atender una denuncia con conflicto de interés

**Objetivo:** impedir intervención/acceso del implicado y permitir atención del resto autorizado.

**Actores:** registrador; integrantes no implicados; director o TOE denunciado; administrador como función operativa.

**Precondiciones:** personas de personal vinculadas a sus cuentas, roles vigentes y hecho comunicado. Se ensayan por separado denuncias al director, a TOE y a otro miembro.

**Flujo principal:**

1. Se registra al denunciado como presunto responsable o se identifica después un conflicto relevante.
2. El sistema restringe el ámbito y dirige avisos a responsables no excluidos.
3. Los miembros restantes asumen y documentan la atención; si falta director elegible, uno registra como sustituto con autoría real.
4. Se realizan actuaciones, informe, acta y eventual derivación sin aprobación del excluido.
5. Se intenta acceso por todos los caminos habituales para comprobar la restricción.

**Alternativas significativas:** denunciado con varios roles y sesión abierta; vinculación de reportes/casos; exclusión comunicada después; todos los responsables impedidos; retiro motivado de una exclusión por autoridad no implicada.

**Postcondiciones:** atención posible por elegibles, mismo comité estable y confidencialidad transversal del caso.

**Requisitos relacionados:** RF-01, RF-13, RF-18–RF-19, RF-21, RF-23, RF-27–RF-28, RF-31–RF-32, RF-37–RF-38; RNF-01–RNF-03, RNF-06–RNF-09; RN-06–RN-08, RN-12, RN-14, RN-23, RN-26–RN-27.

**Criterios de aceptación:**

- Si se denuncia al director, el docente y auxiliar miembros restantes pueden atender y uno registrar acuerdos como sustituto. Si se denuncia a TOE, el resto del comité puede asumir sin convocatoria del implicado.
- Un excluido con rol adicional de administrador, autor del aporte o docente miembro no accede al caso, relato, adjunto, informe, aviso o fragmento de historial. No se retira su propia exclusión ni obtiene contenido mediante la consulta de seguridad.
- Un enlace de descarga obtenido antes de la exclusión falla en el siguiente acceso protegido. La sesión abierta pierde ese ámbito sin bloquear casos ajenos autorizados.
- Relacionar un duplicado, vincular una atención o buscar por otro estudiante no revela contenido. Los desgloses estadísticos no permiten recuperar el caso excluido.
- Si no queda ningún operador elegible se conserva la información, se informa únicamente la necesidad de designación y no se la entrega al implicado. No se simula que la aplicación resolvió la sustitución legal.

### CU-10. Derivar violencia familiar y preservar protección

**Objetivo:** sustentar una derivación externa y diferenciar cierre local de obligaciones continuadas.

**Actores:** TOE o sustituto elegible; Fiscalía como destinatario externo sin cuenta.

**Precondiciones:** caso de violencia familiar, hechos/contexto conocidos y no necesariamente confirmación completa de roles.

**Flujo principal:**

1. El responsable documenta hechos, protección y acciones pertinentes, y prepara derivación a Fiscalía.
2. Genera el informe consolidado dirigido al destinatario, con reserva aplicable.
3. Realiza la comunicación fuera de la aplicación.
4. Registra fecha efectiva, constancia y referencia disponible de la derivación.
5. El sistema cierra localmente por ese motivo cuando la derivación abarca el objeto completo de la atención; conserva seguimientos que continúan.
6. El colegio prosigue sus obligaciones oficiales; registra seguimiento posterior cuando lo utilice para documentarlo.

**Alternativas significativas:** derivación solo preparada, otro destinatario, otro tipo de caso o derivación parcial dentro de un caso mixto.

**Postcondiciones:** derivación efectiva distinguible; atención local cerrada en el supuesto confirmado; ninguna certificación de cierre fiscal, SíseVe o protocolo oficial.

**Requisitos relacionados:** RF-05, RF-07, RF-12–RF-13, RF-16, RF-21, RF-25–RF-26, RF-29–RF-31, RF-33; RNF-02, RNF-06–RNF-08, RNF-13; RN-02, RN-04, RN-13, RN-15–RN-16, RN-19–RN-21, RN-27.

**Criterios de aceptación:**

- El estudiante afectado no incrementa un contador de faltas por sufrir violencia. En un ejemplo ficticio, se registra al estudiante como afectado y a un familiar descrito como «familiar referido por el estudiante, nombre no conocido» como presunto responsable por confirmar, sin DNI ni identificación completa. Ambos constan con papeles propios en el hecho, atención e informe; la referencia puede precisarse después sin crear un padrón. El informe conserva incertidumbre y reserva y no atribuye culpabilidad automáticamente.
- Guardar «derivación preparada a Fiscalía» no cierra; registrar su ejecución efectiva sí cierra localmente el supuesto completo y deja resultado/motivo.
- Derivar una agresión escolar a Policía no la cierra automáticamente. Derivar solo la parte familiar de un caso que continúa por otros hechos tampoco lo cierra todo.
- Una actuación de acompañamiento posterior permanece visible pendiente con responsable y luego registra resultado sin reabrir automáticamente el caso ni alterar su fecha de cierre.
- No se exige referencia SíseVe para guardar/derivar violencia familiar, no se envía una denuncia automáticamente y no se declara terminado el seguimiento normativo.

### CU-11. Generar informe y consultar acta con reserva

**Objetivo:** producir un documento consolidado útil sin duplicar transcripción ni confundirlo con el acta.

**Actores:** TOE/comité autorizado; director o sustituto para acta y decisiones del órgano.

**Precondiciones:** caso con varios hechos, estudiantes con roles distintos, una atribución no corroborada, actuaciones/compromisos y derivación preparada o efectuada.

**Flujo principal:**

1. El operador solicita informe interno o dirigido a un tercero y declara su finalidad/destinatario.
2. Revisa pertinencia del contexto a incluir y genera el consolidado.
3. Descarga/imprime la salida; queda emisión con fecha y autoría.
4. El registrador del comité adjunta el acta externa con sus datos mínimos.
5. Un autorizado consulta la emisión y acta; una emisión actualizada conserva su referencia distinta.

**Alternativas significativas:** DNI desconocido, acta pendiente o malformada, archivo excesivo, usuario ordinario que reportó uno de los hechos o exclusión posterior.

**Postcondiciones:** informe consolidado e identificación de emisión; acta adjunta independiente con la misma reserva del caso.

**Requisitos relacionados:** RF-05, RF-13, RF-16, RF-18, RF-24–RF-26, RF-29–RF-33, RF-38; RNF-02–RNF-07, RNF-13, RNF-16, RNF-19; RN-04, RN-07–RN-08, RN-13, RN-20–RN-21, RN-26.

**Criterios de aceptación:**

- Tres hechos del caso producen un informe conjunto, no tres documentos obligatorios. Sus papeles y carácter de atribución se conservan por persona y hecho y la incertidumbre aparece expresamente. Incluye automáticamente la información pertinente de familiares/personas externas y de participantes presentes solo en reportes duplicados, con sus fuentes y diferencias relevantes, sin repetir el hecho ni transcribir datos al principal. Un papel discrepante no se presenta automáticamente como corroborado y ningún participante hereda el de otra persona.
- La versión dirigida a una autoridad externa sobre violencia usa iniciales y DNI disponible sin nombres completos de estudiantes, no incluye automáticamente todos los antecedentes y declara el documento desconocido cuando falta.
- El PDF muestra institución, caso, fecha/corte, acciones, resultados, compromisos, derivaciones y cierre/continuidad pertinentes; no añade una firma o envío inexistentes.
- El acta `.docx` o PDF se adjunta una vez al caso. Sustituir un archivo deja motivo. Un formato no admitido o más de 10 MB se rechaza claramente sin afectar lo demás.
- El reportante ordinario y el miembro excluido no descargan informe o acta mediante un enlace directo. Una respuesta de derechos tampoco entrega automáticamente el expediente completo.

### CU-12. Incorporar un hecho conocido por SíseVe

**Objetivo:** centralizar historia sin duplicar el hecho ni sustituir registros oficiales.

**Actores:** TOE/comité; registrador con permiso de comunicación; SíseVe como origen externo.

**Precondiciones:** comunicación o referencia externa disponible; se ensaya con y sin un hecho local previo.

**Flujo principal:**

1. Se busca un hecho local coincidente en el ámbito autorizado o se registra la comunicación conocida.
2. El responsable incorpora la referencia oficial al hecho existente o conserva la incidencia nueva si no existía.
3. Si hay dos registros locales del mismo suceso, identifica uno como duplicado del principal mediante CU-05.
4. Documenta su atención y referencias externas conocidas, manteniendo estado local separado.

**Alternativas significativas:** aún no hay código externo, caso histórico bajo protocolo anterior, instrumento aplicable distinto de SíseVe.

**Postcondiciones:** un hecho local computable y trazabilidad de sus orígenes, sin integración ni certificación automática de cumplimiento oficial.

**Requisitos relacionados:** RF-12, RF-18–RF-19, RF-22, RF-30–RF-34; RNF-02, RNF-07, RNF-13, RNF-19; RN-01–RN-03, RN-11–RN-12, RN-16, RN-20–RN-22, RN-27.

**Criterios de aceptación:**

- Un hecho externo nuevo aparece en historial y análisis locales. Si ya existía, añadir su referencia mantiene un solo hecho.
- Falta de código no impide registro urgente. Registrar un código no marca automáticamente reporte completado ni cierre externo.
- Una atención local cerrada no se presenta como caso SíseVe cerrado salvo que exista constancia externa explícita, conservada como referencia sin automatismo.
- Un caso histórico puede conservar la versión del protocolo aplicado y sus actuaciones; actualizar catálogo o normativa no las reescribe.

### CU-13. Analizar convivencia con unidades, cobertura y reserva

**Objetivo:** permitir análisis variados y comprensibles sin doble conteo, medidas engañosas u ocultación interna arbitraria.

**Actores:** TOE, director y miembros del comité elegibles; usuario ordinario como prueba negativa.

**Precondiciones:** conjunto ficticio: dos hechos únicos, tres estudiantes distintos y cuatro participaciones porque uno figura en ambos; una conducta cotidiana atribuida, una presunta responsabilidad por confirmar, una víctima y un testigo; un reporte duplicado del primer hecho; un caso con ambos. Un hecho ocurrió lunes y otro miércoles. Se ensaya con grupo conocido/no informado y con/sin sexo válido.

**Flujo principal:**

1. El usuario elige medida, agrupaciones y filtros combinados de los datos disponibles.
2. Revisa frecuencias, evolución, cruces, reiteraciones y atención, con unidades y cobertura explícitas.
3. Contrasta tamaños de población antes de obtener porcentajes o frecuencias por cien estudiantes.
4. Descarga un resultado interno reservado o prepara mediante agrupación/omisión el resumen cuya eventual comunicación externa decide la institución.

**Alternativas significativas:** grupo/sexo no informados, pocos estudiantes, subtotales solapados, denominador parcial, fecha/hora no fiable o caso excluido por conflicto.

**Postcondiciones:** resultados reproducibles con acceso proporcionado; sin publicación o comunicación externa automática.

**Requisitos relacionados:** RF-06, RF-13, RF-16, RF-18, RF-20–RF-21, RF-24–RF-26, RF-29–RF-30, RF-34–RF-38; RNF-02, RNF-05–RNF-07, RNF-14, RNF-16, RNF-19; RN-01–RN-04, RN-07–RN-11, RN-17–RN-19, RN-22–RN-23.

**Criterios de aceptación:**

- El conjunto produce dos hechos, cuatro participaciones, tres estudiantes distintos y un caso. La conducta cotidiana y la presunta responsabilidad son dos atribuciones de distinto carácter; inicialmente ninguna tiene corroboración de TOE. Víctima/testigo no suman responsables ni el reporte duplicado suma un hecho.
- Corroborar posteriormente la atribución de la denuncia cambia su condición a una corroborada sin aumentar hechos/participaciones ni convertir en corroborada la nota cotidiana. Los listados y gráficos lo distinguen.
- El mapa semanal muestra un hecho lunes y otro miércoles según fecha del hecho. Los subtotales por grupos relacionados advierten que pueden solaparse. Sin hora fiable no se muestra una distribución horaria inventada.
- Se obtiene, al menos, un cruce categoría/grado y otro sexo/período con filtros adicionales de gravedad/estado cuando sean aplicables, sin lenguaje de consultas. Otros filtros no quedan limitados a dos.
- En una prueba adicional con veinte estudiantes de población válida, dos distintos involucrados y tres hechos, se muestra 10 % de estudiantes y, separadamente, quince hechos por cien estudiantes durante el período. No se interpreta como 15 % de estudiantes afectados ni tasa de exposición exacta. Sin denominador válido no se calcula.
- La réplica de matrícula carece de sexo y puede carecer de grupo: ambos se declaran no informados con cobertura, sin inferirlos. Un conjunto adicional con datos válidos permite comparar por sexo/grupo manteniendo denominadores coherentes.
- El usuario interno autorizado ve y descarga cifras exactas aunque un agregado tenga dos estudiantes. Para una entrega externa, puede preparar una agrupación más amplia sin identificadores; la institución revisa riesgo/finalidad y no se publica por automatismo. No hay umbral fijo de cinco.
- Docente/auxiliar ordinario no accede a estadísticas. Un miembro del comité sí; si está excluido, el ámbito de consulta/exportación no revela ese caso por su detalle o referencia.

### CU-14. Mantener designaciones, parámetros y años sin trámites extra

**Objetivo:** permitir mantenimiento habitual por personal capacitado, sin editar código, aprobaciones por nota o dependencia del desarrollador.

**Actores:** administrador; TOE/director para valores institucionales, con carga delegable.

**Precondiciones:** designaciones institucionales y capacidades delimitadas, sin conflicto en el ámbito.

**Flujo principal:**

1. El administrador registra cuentas/designaciones y comité vigente, con autor/fecha automáticos.
2. TOE/director o administrador delegado registra valores ya definidos institucionalmente, sin aprobación electrónica por cambio.
3. Se habilita un año/grupos nuevos y AIP importa o actualiza los vínculos correspondientes.
4. Se utiliza el listado de roles para revisión semestral y se retiran accesos no vigentes.

**Alternativas significativas:** usuario administrativo intenta leer casos, sesión de cuenta desactivada, categoría histórica desactivada, parámetros ausentes o corrección de una nómina anterior.

**Postcondiciones:** operación vigente con historia, autorías y restricciones conservadas.

**Requisitos relacionados:** RF-01–RF-03, RF-10–RF-11, RF-28, RF-38, RF-40–RF-41; RNF-01–RNF-02, RNF-07, RNF-20–RNF-24; RN-03, RN-06–RN-09, RN-17–RN-18, RN-25–RN-28.

**Criterios de aceptación:**

- Baja/término de membresía impide la siguiente operación institucional correspondiente sin borrar autoría. No se vuelve a crear comité por caso.
- El administrador sin rol funcional no consulta relatos/contexto y no se concede facultades para eludir una designación o su propia exclusión.
- La configuración aprobada puede cargarla el administrador delegado; un miembro del comité conserva atención/consulta equivalentes aunque no tenga esa responsabilidad de configuración. No hay jerarquía adicional de recepción.
- Un cambio de umbral no reescribe las señales emitidas; la ausencia de valores no se sustituye por ejemplos. Una categoría desactivada conserva usos históricos.
- Crear año no promueve, elimina o cierra. Corregir una nómina histórica selecciona el año y revisa cambios sin flujo de desbloqueo. Un operador completa estas tareas con la guía, sin código o ayuda del autor original.

### CU-15. Atender una decisión sobre datos por medios convencionales

**Objetivo:** cumplir derechos y conservación con resultados comprobables, sin un módulo general de gestión de solicitudes.

**Actores:** responsable institucional que tramita/decide fuera del sistema; operador autorizado; AIP para rectificación por importación y soporte técnico si una disposición excepcional lo requiere.

**Precondiciones:** decisión/solicitud en el procedimiento institucional, identidad/representación comprobadas cuando corresponda, alcance y retenciones válidas identificados.

**Flujo principal:**

1. La institución tramita y decide lo procedente por sus canales, sin crear un expediente nuevo obligatorio en esta aplicación.
2. Un autorizado usa consultas/exportaciones y correcciones existentes para localizar/preparar datos propios y corregir errores.
3. Si se requiere bloqueo o disposición excepcional, ejecuta el procedimiento operativo documentado con autorización delimitada.
4. Comprueba que la decisión se cumple en consultas/copias operativas afectadas y que no se divulga información de terceros.
5. Conserva constancia de la decisión y ejecución por medios institucionales y gestiona los destinatarios de la respuesta fuera de la aplicación.

**Alternativas significativas:** retención legal, competencias públicas que exceptúan bloqueo, incidente compartido, dato de salud importado incorrecto o copia de respaldo anterior a la decisión.

**Postcondiciones:** resultado jurídico autorizado aplicado y verificable, sin reconstruir datos retirados ni eliminar innecesariamente información legítima de otros.

**Requisitos relacionados:** RF-07, RF-10–RF-11, RF-17, RF-28, RF-31, RF-38–RF-41; RNF-02–RNF-07, RNF-18, RNF-20–RNF-21, RNF-23, RNF-25; RN-07–RN-08, RN-17–RN-20, RN-24–RN-26.

**Criterios de aceptación:**

- La tramitación no exige portal, registro electrónico de consentimiento, contador legal o expediente de derechos dentro de la aplicación. La mera recepción no elimina ni bloquea todo: se demuestra el resultado conforme al art. 20 y sus excepciones/información al solicitante.
- Con consultas y exportación delimitadas se prepara información procedente de la persona sin entregar automáticamente actas, expediente, relatos o datos sensibles de terceros. No se habilita cuenta de familia.
- AIP rectifica un dato importado por un nuevo lote sin herramienta de edición libre. Los valores omitidos/vacíos no borran por accidente; la rectificación queda trazable.
- Una disposición válida sobre una persona en un hecho compartido deja inaccesibles los datos alcanzados en consultas y derivados, conservando información legítima de las demás. La constancia no reproduce el contenido retirado.
- Cambio de año/cierre no elimina; tampoco se aplica conservación perpetua o dos años universales a expedientes. Las retenciones se respetan y sus decisiones pueden documentarse fuera del sistema.
- Un respaldo anterior permanece aislado hasta vencer; no se exige editarlo. Si se restaura en ensayo, un autorizado verifica/aplica las decisiones vigentes antes de habilitar acceso, sin requerir un motor automático de reconciliación.

### CU-16. Restaurar y transferir mantenimiento con procedimiento sencillo

**Objetivo:** demostrar recuperación y autonomía de mantenimiento mediante medios convencionales documentados.

**Actores:** operador técnico autorizado —interno o soporte designado— y responsable institucional; TOE/AIP no son operadores de respaldo por su rol funcional.

**Precondiciones:** respaldo de ensayo y medios disponibles; datos ficticios con hechos compartidos, contexto, acta/informe, caso cerrado y constancias de una baja, exclusión y disposición de datos posteriores a la copia.

**Flujo principal:**

1. El operador consulta resultado/fecha de copias y simula pérdida del servicio principal.
2. Ejecuta la recuperación documentada y comprueba integridad de registros, vínculos y archivos.
3. Revisa constancias de designaciones/restricciones/disposiciones vigentes y aplica lo necesario antes de abrir acceso ordinario.
4. Comprueba consultas y tareas autorizadas y registra resultado/tiempo del ensayo.
5. Otro técnico interpreta la entrega recuperable y ejecuta una comprobación crítica con la documentación disponible.

**Alternativas significativas:** copia reciente corrupta con anterior utilizable, fallo de respaldo, necesidad de soporte o indicio de brecha gestionado por canales institucionales.

**Postcondiciones:** recuperación íntegra y restringida dentro de los objetivos; un fallo explícito sigue siendo incumplimiento y debe corregirse para aceptar.

**Requisitos relacionados:** RF-01, RF-28, RF-38–RF-41; RNF-01–RNF-09, RNF-16–RNF-25; RN-07–RN-08, RN-24–RN-27.

**Criterios de aceptación:**

- La pérdida de ensayo no supera veinticuatro horas de cambios confirmados y la recuperación se completa en hasta dos días hábiles desde disponibilidad de medios e inicio del procedimiento técnico. Documentar el exceso no lo convierte en aceptación.
- Perder el medio principal no destruye todas las copias; una copia anterior permite ensayar corrupción reciente y se informa la pérdida real. Se verifica la integridad con las herramientas elegidas, sin exigir un panel escolar.
- Se recuperan hechos, casos, vínculos y archivos. Una comprobación controlada de las constancias y configuración vigente evita que una cuenta dada de baja, un excluido o datos retirados reaparezcan accesibles. Puede hacerse mediante revisión/aplicación autorizada, sin reproducción automática de bitácoras.
- La guía identifica responsable, frecuencia/retención de copias, accesos y recuperación; se demuestra ensayo inicial, repetición por año escolar y después de cambios que afecten la recuperación, no una obligación trimestral.
- Otro técnico puede ejecutar recuperación/interpretar la entrega sin cuenta personal del autor original. El inventario identifica costos/renovaciones. La consulta de estado y evidencia no habilita lectura general de casos.
- La evidencia ficticia permite delimitar operaciones/ámbitos de una posible brecha para el procedimiento institucional, sin un módulo ni comunicaciones automáticas a autoridades.

### CU-17. Comprobar rapidez, accesibilidad y baja carga de trabajo

**Objetivo:** verificar que el producto facilita registro y atención con una prueba pequeña y reproducible.

**Actores:** tres representantes de registro/atención y un representante AIP, que puede coincidir con uno de ellos; responsable de aceptación.

**Precondiciones:** datos ficticios, entorno de 4.1, guiones breves y orientación máxima de RNF-11; versiones/dispositivos registrados.

**Flujo principal:**

1. Cada representante realiza las tareas frecuentes de su función: registro/búsqueda y atención directa desde alerta, según corresponda.
2. Se comprueba guardado conjunto de actuación, compromiso y cierre y actualización de aviso sin operaciones administrativas separadas.
3. AIP importa la réplica de matrícula y una variación moderada y comprende excepciones y grupo no informado.
4. Se ensayan teclado, ampliación, lector de pantalla pertinente y teléfono para tareas previstas, y respuesta bajo concurrencia.
5. Se registra el obstáculo/fallo real, se corrige y se repite solo lo afectado.

**Alternativas significativas:** texto no guardado, homónimos, fallo de validación/red, cambio de permiso, gráfico con alternativa tabular y operación larga.

**Postcondiciones:** evidencia verificable de rapidez, claridad, reserva y accesibilidad, sin aceptar tareas imposibles por explicarlas.

**Requisitos relacionados:** RF-04, RF-08–RF-09, RF-12–RF-16, RF-19–RF-26, RF-30–RF-31, RF-35; RNF-01, RNF-08, RNF-10–RNF-17, RNF-19–RNF-20, RNF-25; RN-04–RN-05, RN-11–RN-13, RN-15, RN-18, RN-22, RN-27.

**Criterios de aceptación:**

- Los representantes de registro guardan nota breve en noventa segundos y localizan estudiante/aporte en treinta, con contenido proporcionado, sin ayuda, omisiones o aprobación de TOE. La atribución cotidiana queda correctamente expresada y computable.
- Quien representa TOE documenta desde una alerta conversación realizada, compromiso y cierre en hasta tres minutos y una confirmación final, sin guardar apertura/recepción/programación aparte ni justificar datos rutinarios redundantes.
- El representante AIP importa/asocia el formato y columnas movidas usando la guía, sin código o ayuda del desarrollador; comprende qué se omite, actualiza o mantiene no informado. No necesita otros operadores de AIP para probarlo.
- Las tareas esenciales son realizables por teclado, con foco/etiquetas/errores reconocibles y ampliación al 200 %; a los anchos previstos no se pierden operaciones. Los gráficos disponen de valores/tablas equivalentes.
- RNF-16 se cumple en el entorno documentado. Operación larga indica estado y no duplica por reintento; falla de red no presenta éxito falso. La evidencia usa datos ficticios y permite repetir la tarea.

## 6.2 Matriz de trazabilidad y cobertura

Las relaciones detalladas de cada CU son la referencia de ejecución. Esta matriz agrupa la cobertura completa; los intervalos incluyen todos los identificadores intermedios.

| Necesidad o control | Requisitos funcionales | Calidad | Reglas | Casos de uso de comprobación |
|---|---|---|---|---|
| Cuentas, comité estable, año y configuración mínima | RF-01–RF-03, RF-40–RF-41 | RNF-01–RNF-02, RNF-07, RNF-20–RNF-24 | RN-03, RN-06–RN-09, RN-17, RN-26, RN-28 | CU-08, CU-09, CU-14, CU-16 |
| Estudiantes, contexto, Excel variable e identidad entre años | RF-04–RF-11, RF-39–RF-40 | RNF-02, RNF-04–RNF-05, RNF-08, RNF-11–RNF-12, RNF-16, RNF-25 | RN-17–RN-19, RN-24–RN-25, RN-28 | CU-01, CU-06, CU-14, CU-15, CU-17 |
| Hechos compartidos, registro cotidiano, atribuciones y consulta propia | RF-05, RF-12–RF-17, RF-20, RF-24 | RNF-02, RNF-07–RNF-15, RNF-19 | RN-01–RN-07, RN-13, RN-17 | CU-02, CU-03, CU-05, CU-06, CU-17 |
| Duplicados, participantes conservados y origen oficial sin doble conteo | RF-05, RF-13, RF-16, RF-18, RF-20, RF-23, RF-31, RF-33–RF-34 | RNF-02, RNF-07–RNF-08, RNF-19 | RN-01, RN-04, RN-10, RN-12, RN-20–RN-22 | CU-03, CU-05, CU-06, CU-10–CU-13 |
| Gravedad, reiteración, avisos y recepción sin aprobación universal | RF-03, RF-19–RF-22, RF-24 | RNF-02, RNF-06–RNF-08, RNF-13, RNF-16 | RN-03–RN-05, RN-09–RN-11, RN-27 | CU-03, CU-04, CU-13, CU-14, CU-17 |
| Atención agrupada, actuaciones, compromisos e historia cerrada | RF-14, RF-16, RF-22–RF-26, RF-30 | RNF-07–RNF-08, RNF-10–RNF-13, RNF-19 | RN-05, RN-12–RN-15, RN-17 | CU-02, CU-06, CU-07, CU-10 |
| Comité, director y exclusión transversal por conflicto | RF-01, RF-13, RF-18–RF-19, RF-23, RF-27–RF-28, RF-37–RF-38 | RNF-01–RNF-03, RNF-06–RNF-09 | RN-06–RN-08, RN-14, RN-23, RN-26 | CU-03, CU-05, CU-08, CU-09, CU-14 |
| Derivaciones, cierre familiar local, informe y acta | RF-25–RF-33 | RNF-02–RNF-08, RNF-13, RNF-16, RNF-19 | RN-13–RN-16, RN-20–RN-21, RN-27 | CU-07, CU-08, CU-10–CU-12 |
| Estadística variada, sexo/grupo informados, denominadores y reserva por destinatario | RF-06, RF-20, RF-34–RF-37 | RNF-02, RNF-05–RNF-07, RNF-14, RNF-16, RNF-19 | RN-01–RN-04, RN-10, RN-17–RN-19, RN-22–RN-23 | CU-04, CU-05, CU-13, CU-17 |
| Protección de datos, trazabilidad, derechos y conservación | RF-07, RF-10, RF-17, RF-28, RF-31, RF-37–RF-41 | RNF-01–RNF-09, RNF-18, RNF-20–RNF-21, RNF-25 | RN-07–RN-08, RN-19–RN-20, RN-23–RN-27 | CU-01, CU-09, CU-11, CU-15, CU-16 |
| Respaldo, continuidad, portabilidad y mantenimiento sostenible | RF-01–RF-03, RF-38–RF-41 | RNF-16–RNF-25 | RN-17, RN-24–RN-28 | CU-14–CU-17 |
| Usabilidad, accesibilidad y baja carga cotidiana | RF-04, RF-08–RF-09, RF-12–RF-16, RF-21, RF-25–RF-26, RF-31, RF-35 | RNF-10–RNF-17, RNF-20 | RN-04–RN-05, RN-11–RN-13, RN-22, RN-27 | CU-01, CU-02, CU-07, CU-17 |

## 6.3 Comprobación global y revisión de carga administrativa

La versión 1.2 conserva la especificación 1.1 y sus identificadores, con ajustes puntuales para personas externas involucradas y conservación de participaciones al vincular reportes duplicados, junto con sus referencias y pruebas afectadas. Se mantiene la revisión integral de los seis capítulos, las catorce aclaraciones de la transferencia y la matrícula correcta realizada para la versión 1.1. La aceptación requiere ejecutar los escenarios pertinentes y sus pruebas negativas; el cumplimiento no se presume por funcionar con una cuenta privilegiada. Las cifras de ensayo son metas de ingeniería y no políticas institucionales confirmadas.

| Aspecto revisado | Resultado de requisitos y criterio de simplicidad |
|---|---|
| Registro y reiteración cotidiana | Atribución por el autor desde el guardado, sin corroboración de TOE; denuncia por confirmar, víctima/testigo y papel desconocido se distinguen. La nota sin señal no es trabajo pendiente. |
| Atención breve | Puede registrarse conversación, resultado, compromiso y cierre con una confirmación final; no se obliga a apertura guardada, recepción, programación o motivo redundante. |
| Bandeja | Una gestión actualiza sus avisos. Solo nuevas situaciones, atenciones pendientes y seguimiento real requieren trabajo; historia cerrada no es una cola de revisión. |
| Duplicados | Se conservan principal, reportes originales y todas sus participaciones pertinentes, con procedencia/diferencias y un hecho computable. Historiales, atención e informe las recuperan sin transcripción, manteniendo papeles y reserva. No se exigen fusión/reversión de expedientes o reconstrucción de contenidos. |
| Casos compartidos | Se agrupan hechos/personas con sus papeles exactos; las actuaciones se conservan y referencian sin copiar o crear un caso por estudiante. Correcciones sustanciales dejan causa breve. |
| Comité | Capacidades institucionales equivalentes, recepción/convocatoria habituales de TOE y registro colectivo por director; configuración y sustituciones son responsabilidades explícitas, sin nuevas jerarquías o aprobación cotidiana. |
| Conflictos | Exclusión prevalente por ámbito, también para duplicados, informes, actas e historia; el resto elegible atiende sin permiso del implicado, conservando comité estable. |
| Matrícula | Formato real con encabezados agrupados, DNI de estudiante/familiar diferenciados, 91 registros y grupo no informado; asociación reutilizable, excepciones localizadas y datos mínimos sin copiar columnas sensibles por defecto. |
| Contexto | AIP importa antecedentes pertinentes, incluidos futuros campos de protección, sin acceso general a casos. La admisión se documenta por medios institucionales, sin aprobación por celda ni motor de campos jurídicos. |
| Informes y derivación | Un consolidado por caso; acta externa adjunta. Derivación familiar efectiva puede cerrar localmente sin certificar cierre oficial o extinguir protección. No se administra todo el proceso externo. |
| Estadísticas | Medidas diferenciadas y filtros/cruces combinables, datos válidos y cobertura explícita. Se mantienen cifras internas útiles sin umbral fijo de cinco; la entrega externa tiene revisión institucional de finalidad e identificación. |
| Datos personales | El colegio tramita derechos/conservación fuera del software si conviene. Consultas, corrección y procedimientos autorizados producen resultados legales, sin plataforma de solicitudes, consentimientos o políticas automáticas. |
| Respaldo y mantenimiento | Objetivo diario de pérdida, restauración de ensayo en dos días hábiles, comprobación anual y tras cambios pertinentes; medios convencionales y verificación de restricciones vigentes, sin panel propio o reconciliación automática. |
| Usabilidad y autonomía | Prueba pequeña con tareas cronometradas, un representante AIP, accesibilidad y guía. Las operaciones habituales son escolares; las excepcionales técnicas se documentan para cualquier responsable autorizado, sin dependencia personal del autor. |

Las cuestiones de 1.10 pendientes son institucionales o datos no suministrados: catálogo definitivo, parámetros de reiteración, grupo de estudiantes, alcance legítimo de contexto, conservación y designaciones. No impiden implementar las capacidades especificadas ni justifican llenar vacíos con reglas inventadas. Su definición para operación real utiliza esas capacidades y procedimientos existentes. No se añaden funcionalidades hipotéticas para compensar las exigencias retiradas.
