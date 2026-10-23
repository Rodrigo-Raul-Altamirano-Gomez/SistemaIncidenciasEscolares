# Implementación del dominio — reglas completadas y contratos de integración

Estado consolidado al 22-10-2026: implementadas las continuaciones de importación/contexto, consultas/estadísticas, configuración y protección descritas en este documento. Las secciones de incrementos 1–7 y 8A conservan su estado histórico; sus pendientes se resuelven en las continuaciones y la matriz de cierre, no se declaran pendientes actuales de dominio.
El apartado «Incremento 8A» describe la conciliación inicial sobre filas tipadas. Las continuaciones
posteriores completan contactos/contexto, rectificaciones, asociación de encabezados y el resto de
reglas pendientes. La lectura física de Excel continúa siendo responsabilidad de un adaptador.
Fuente normativa: `ERS_Incidencias_Escolares.md`, versión 1.2.
Auditoría posterior de los diez paquetes: véase «Auditoría integral del dominio — 22 de octubre
de 2026», al final, con correcciones, simplificaciones y verificación que actualizan esta base.
Revisión posterior: precisiones institucionales de fecha/clasificación y corrección del principal,
verificadas el 21 de octubre de 2026.
Incremento 2: cuentas, designaciones, política de permisos y exclusiones por conflicto.
Los incrementos 3 y 4 incorporaron atención compartida, actuaciones, compromisos, derivaciones y cierre local.
Los tres últimos bloques fueron: incremento 5, comité y continuidad; incremento 6, alertas y bandeja;
incremento 7, informes, actas y referencias oficiales. La verificación conjunta se detalla al final.

## Alcance y forma de trabajo de la base 1–7

Los incrementos implementan el núcleo de identidad escolar, clasificación, comunicaciones de hechos,
participaciones, reiteración, autorización, atención local, comité, avisos y documentos. Es una biblioteca Java dentro del monolito
hexagonal; no incorpora casos de uso, puertos de persistencia, controladores, base de datos ni interfaz.
No representa todavía el dominio completo ni una aplicación apta para datos reales.

Los paquetes se separan por concepto del negocio, dentro de `pe.edu.incidencias.domain`.
Los valores, revisiones y agregados son inmutables. Las operaciones devuelven una nueva instancia,
reciben explícitamente autor y tiempo cuando corresponde y conservan las fuentes originales.
No se introdujeron Spring, JPA, Lombok ni dependencias productivas externas.

## Organización de la revisión

Este documento reúne los detalles de estudiantes, catálogo, reportes, duplicados, reiteración, acceso y atención,
además de decisiones, cobertura de requisitos, pruebas y pendientes para la revisión de otro agente.

La base de los incrementos 1–7 contiene 65 archivos Java productivos, 29 archivos Java de pruebas
(28 suites y un auxiliar de datos). Al cierre de 8A eran 69 productivos y 31 de pruebas (30 suites y
un auxiliar). Este documento sigue siendo el detalle local único de implementación; solo el
README principal se versionaba al cerrar esos incrementos. La auditoría integral posterior
versiona también la ERS y este documento para mantener juntas reglas, código y regresiones.
El incremento 2 añade 10 archivos productivos y 4 suites, manteniendo los archivos anteriores.
Los incrementos 3 y 4 añadieron 6 archivos productivos y 6 suites.
Los incrementos 5, 6 y 7 añaden **27 archivos productivos**: 7 de comité/continuidad,
8 de alertas/bandeja, 11 de documentos/institución y 1 coordinador de atención/avisos.
También se amplían `CasoAtencion` y `RegistroAtenciones`. La carga se agrupa en tres bloques
por sus reglas y verificaciones, manteniendo como referencia los 22 archivos del primer incremento.
Se mantuvo el `pom.xml` existente (Java 25 y JUnit 5). `application` y `adapter` permanecen sin código.

## Estudiantes: primer incremento de dominio

Fecha: 21 de octubre de 2026. Fuente: ERS v1.2, especialmente RF-04, RF-06, RF-09, RF-10, RF-11, RN-17, RN-18 y CU-01. Este bloque no implementa el importador de matrícula ni representa la aceptación completa de esos requisitos.

### Modelo y decisiones

Paquete `pe.edu.incidencias.domain.estudiante`, Java puro, sin dependencias de infraestructura.

| Tipo | Responsabilidad y API principal |
|---|---|
| `Estudiante` | Identidad interna UUID independiente del año; `crear`, `incorporarIdentidad`, `vincularMatricula`, `incorporarGrupo`, `incorporarSituacion`, `grupoEn`. |
| `DocumentoIdentidad` | Tipo y número textuales. DNI admite exactamente ocho dígitos ASCII, preserva ceros y no completa cifras. Otros tipos conservan el identificador declarado, sin inventar formatos. |
| `GrupoEscolar` | Grado y sección opcionales individualmente; exige al menos uno informado. El grupo totalmente desconocido es `Optional.empty()`. |
| `MatriculaAnual` | Año explícito, situación informada opcional e historial ordenado de grupos por fecha efectiva. |
| `BusquedaEstudiantes` | Funciones puras por nombre, documento, año y grupo histórico; devuelve solamente UUID y nombre en `Coincidencia`. |

Las instancias y colecciones son inmutables. Las modificaciones producen una nueva instantánea y conservan el UUID; el llamador debe guardar el resultado. No se generan identificadores ni fechas internamente. El nombre completo se conserva sin separación artificial ni normalización de tildes o espacios internos. La normalización se aplica exclusivamente al comparar búsquedas y tolera mayúsculas, tildes y espacios Unicode.

Documento, nacimiento y sexo pueden quedar desconocidos. El sexo informado y la situación anual se conservan como textos declarados, sin inferirlos ni crear un catálogo institucional no definido por la ERS. No se usa el nombre como clave única. La búsqueda por documento devuelve todas las coincidencias y no decide una fusión ni escoge el primer candidato.

`incorporarIdentidad`, `incorporarGrupo` e `incorporarSituacion` interpretan `Optional.empty()` como omisión: no borran datos. El borrado deliberado debe implementarse en un incremento de rectificación/autorización. No existe operación que infiera retiro, promoción o nueva matrícula por ausencia en una carga. Vincular de nuevo un año con contenido equivalente es idempotente; contenido contradictorio no sustituye el historial existente de forma silenciosa.

Cada fecha efectiva de grupo pertenece al año escolar explícito. Antes de la primera fecha conocida se devuelve grupo desconocido. Una fecha inicial desconocida puede completarse y los cambios posteriores se conservan. Se pueden incorporar fechas históricas conocidas fuera de orden. Dos grupos diferentes en la misma fecha son una contradicción que se rechaza; la futura operación de rectificación debe resolverla con autorización y trazabilidad. Cambiar al grupo actual no rellena fechas anteriores con información supuesta. La incidencia debe guardar su propia instantánea del grupo conocido al registrar el hecho: el historial de matrícula no modifica automáticamente hechos guardados.

Los criterios de búsqueda de grado/sección pueden ser parciales: los componentes omitidos no filtran, y un componente desconocido no satisface un valor solicitado. Los resultados no incluyen DNI completo, sexo, nacimiento, contexto, contactos, señales ni historia de atención. El paquete de estudiantes no autoriza consultas: quien lo invoque debe aplicar la política del paquete `acceso` descrita más adelante; la autenticación e integración de esas decisiones siguen pendientes.

### Cobertura y pendientes

| ERS | Cobertura de este incremento |
|---|---|
| RF-04 | Comparación tolerante de nombres, búsqueda por documento/año/grupo histórico y resultados mínimos. Pendiente consulta persistente, índices, autorización y presentación para distinguir homónimos mediante datos escolares autorizados. |
| RF-06 | Identidad, nombre completo, documento opcional, nacimiento/sexo informado, vínculo anual, situación y grupo por fechas. Pendientes contactos, domicilio y datos sensibles pertinentes. |
| RF-09 y RN-18 | Validación textual de DNI y desconocidos; sin parser Excel, columnas, filas, previsualización o validación de lotes. |
| RF-10 | Semántica no destructiva e idempotencia local del vínculo anual. Pendientes transacción/reintento del lote, confirmación, procedencia, resultado y borrado expreso. |
| RF-11 y RN-17 | Identidad estable y continuidad anual. Pendiente conciliador de candidatos y unicidad global de documentos: no puede garantizarse mirando un solo agregado. |
| CU-01 | Se prueban invariantes del modelo usadas por el caso de uso. No se ejecuta la aceptación del archivo de 91 estudiantes. |

No se implementan contactos familiares, antecedentes contextuales, importador, catálogo de grupos admitidos, derechos/conservación, repositorios ni puertos. El incremento 7 añade `DatosInstitucion` como valor para informes; el mantenimiento autorizado de configuración institucional sigue pendiente. Tampoco se garantiza auditoría persistente: la aplicación futura deberá aportar autor/fecha/procedencia y transacciones al incorporar cambios legítimos. El constructor del agregado es privado para que las actualizaciones no puedan reemplazar inadvertidamente todos sus vínculos.

### Pruebas de revisión

`EstudianteTest` cubre creación sin datos opcionales; añadir documento/corregir nombre sin romper identidad; omisión no destructiva; grupo desconocido y completado; fechas históricas y cambio efectivo; nuevo año sin promoción/retiro; repetición idempotente y contradicciones; colecciones inmutables; fechas/vínculos inconsistentes; DNI con ceros y formatos inválidos; rechazo de blancos; homónimos separados; normalización de búsqueda sin alterar el nombre; búsqueda por documento con resultado mínimo; y búsqueda histórica por año/grado/sección.

Los datos de prueba son ficticios. Ejecutar desde la raíz: `./mvnw.cmd test -Dtest=EstudianteTest`. El resultado de la verificación conjunta figura en la sección «Verificación reproducible» de este documento.

## Catálogo y parámetros de reiteración: incremento 1

Base: `ERS_Incidencias_Escolares.md` v1.2, RF-03, RN-03, RN-09, RN-28 y apartado 1.10. Código en `pe.edu.incidencias.domain.catalogo`; no contiene infraestructura, repositorios, autorización ni un motor configurable de reglas.

### Requisitos implementados

| Requisito | Capacidad y evidencia |
|---|---|
| RF-03 / RN-03 | `Categoria` conserva identidad, nombre, descripción, naturaleza, gravedad, aviso, admisión de reiteración y umbral opcional. `ClasificacionCategoria` conserva una copia histórica independiente de los cambios de catálogo. |
| RF-03 / RN-03 | `ClasificacionCategoria.requiereAvisoInmediato()` obliga aviso para violencia/protección y gravedad grave o muy grave. El indicador configurable nunca desactiva esas condiciones. Una categoría por clasificar admite aviso explícito. |
| RF-03 | Desactivar prohíbe `clasificacionParaNuevoHecho()` y mantiene revisiones, definiciones anteriores y clasificaciones ya emitidas. Activar permite volver a usar la misma identidad. |
| RF-03 / RN-09 / 1.10 | Cada categoría tiene umbral entero positivo opcional; una sola instancia institucional de `ParametrosReiteracion` contiene la ventana común opcional, expresada en días calendario positivos. No existen valores predeterminados. |
| RF-03 / RN-09 | `evaluar(Categoria)` devuelve `EstadoReiteracion`: habilitación, valores y conjunto inmutable de parámetros faltantes. Se habilita solo si la categoría admite reiteración y están presentes ventana y umbral. |
| RF-03 | Toda creación, modificación y activación/desactivación de categoría, así como cada cambio de ventana, conserva autor UUID, instante y estado completo de esa versión. |
| RN-28 | Las naturalezas y gravedades son enumeraciones cerradas. No se añaden sanciones, fórmulas, culpabilidad ni flujos configurables. |

### APIs y decisiones de diseño

- `DefinicionCategoria` agrupa los valores editables. Nombre y descripción son obligatorios; se eliminan espacios en los extremos y se rechazan cadenas vacías. No se fijan longitudes institucionales arbitrarias.
- `Categoria.crear(id, definicion, autorId, fecha)` crea una categoría activa. `actualizar`, `desactivar` y `activar` devuelven un nuevo agregado, sin mutar el anterior. Las revisiones completas permiten reconstruir el estado y la autoría de cada cambio. Los instantes se reciben explícitamente y no pueden preceder a la revisión vigente.
- `clasificacionParaNuevoHecho()` comprueba que la categoría esté activa y entrega el record `ClasificacionCategoria(categoriaId, nombre, naturaleza, gravedad, avisoInmediato)`. Es una copia estable: cambiar o desactivar la categoría no modifica la clasificación que conserva el hecho.
- Una categoría que no admite reiteración rechaza un umbral configurado, para evitar una combinación ambigua. Una categoría que sí la admite puede carecer de umbral: es un estado institucional pendiente legítimo y visible.
- `ParametrosReiteracion.sinConfigurar(autorId, fecha)` registra la ausencia explícita de ventana. `configurarVentana` y `retirarVentana` devuelven versiones nuevas con historia. `evaluar` genera un valor inmutable con los parámetros actuales; los valores usados en una señal pueden conservarse sin quedar referidos a un objeto mutable.
- La desactivación no elimina el umbral ni impide consultar la configuración histórica. Impedir nuevos usos se aplica en la obtención de una clasificación para un hecho nuevo.
- Se usan `UUID`, `Instant`, `OptionalInt`, records y colecciones inmutables de Java. No hay dependencias de ejecución fuera del JDK ni lectura del reloj global.

### Límites que debe respetar la siguiente implementación

La aplicación futura debe mantener **una ventana común institucional**, persistir las nuevas versiones y validar autorización antes de invocar modificaciones. `PoliticaAcceso` permite definir configuración a TOE/director; la carga delegada al administrador todavía requiere su modelo explícito y no se concede automáticamente. Pasar un `autorId` registra autoría, pero no comprueba su permiso. Tampoco puede tratarse el constructor público de `ClasificacionCategoria` como comprobación de que una categoría esté activa: la operación normal debe obtenerla desde el agregado.

Este módulo no emite notificaciones ni implementa el cálculo de reiteración, persistencia de señales, idempotencia o destinatarios. La señal deberá conservar los parámetros y las revisiones utilizados cuando se emita; guardar únicamente el identificador de categoría y volver a leer el catálogo reescribiría de hecho su fundamento histórico. Este módulo deja el fundamento disponible como valor inmutable, pero no afirma satisfacer por sí solo toda RF-03.

No se proporciona un catálogo institucional precargado, umbral de tres, plazo universal, gravedad jurídica o política real. Los valores de las pruebas son ficticios.

### Pruebas y revisión

`CatalogoTest` verifica obligatoriedad del aviso en todas las gravedades de violencia y en grave/muy grave; comportamiento leve sin aviso y por clasificar con aviso; cambio de catálogo sin reclasificación histórica; autorías y estados de revisiones; desactivación/reactivación; inmutabilidad; rechazo de umbrales y ventanas no positivos; ausencia explícita de cada parámetro; umbrales distintos con la misma ventana; categoría que no admite reiteración; cambio y retirada de ventana sin alterar versiones previas; y rechazo de revisiones anteriores en el tiempo.

El resultado de la ejecución conjunta de Maven, con las pruebas de todos los subdominios, figura en la sección «Verificación reproducible» de este documento.

## Modelo de hechos y comunicaciones

Paquete `pe.edu.incidencias.domain.incidencia`:

| Tipo | Responsabilidad |
|---|---|
| `TipoPersona`, `PersonaInvolucrada` | Identidad inequívoca cuando existe, referencia provisional cuando no. No crean cuentas ni estudiantes ficticios. |
| `PapelParticipacion`, `Participacion` | Papel individual explícito y grupo escolar histórico opcional; no se transmite el papel de otra persona. |
| `FechaHecho` | Fecha exacta o referencia aproximada expresamente diferenciadas. |
| `DatosReporte` | Fecha, año escolar, copia de clasificación, descripción, participantes y petición de atención; hora, lugar, comunicante y canal opcionales. |
| `RegistroCambio` | Autor UUID, instante y causa opcional, obligatoria en las transiciones que la necesitan. |
| `ReporteIncidencia` | Comunicación con ID, autor y fecha originales, revisiones, aclaraciones propias, marcador de atención y anulación por error. |
| `Hecho` | Identidad estable, principal corregible y comunicaciones duplicadas; vínculos reversibles, fuentes de participaciones y precisiones institucionales. |

`ReporteIncidencia.registrar` no requiere un caso ni confirmación de TOE. Rechaza fechas y horas
efectivas futuras respecto del instante de registro convertido a `America/Lima`. Admite registro
tardío sin motivo obligatorio. No inventa hora, lugar o identificación. Una fecha aproximada no
admite hora exacta y queda fuera de reiteración automática.

Un estudiante identificado utiliza su UUID persistente. Para un estudiante todavía desconocido se
usa `ESTUDIANTE_NO_IDENTIFICADO` y una referencia descriptiva con ID propio: ese ID **no** es una
matrícula ni un `Estudiante`. Para personal, familiares y externos se conserva el mínimo conocido;
no se exige DNI. El vínculo con una cuenta solo es válido para personal. El mismo UUID no puede
cambiar de tipo de persona entre fuentes ni asociarse con dos cuentas conocidas. Una misma cuenta
de personal no puede usarse con dos identidades dentro del hecho. Identificaciones parciales con
IDs distintos no se unifican por nombre o parentesco.

La participación cotidiana exige estudiante identificado y categoría disciplinaria leve. Se eligió
`LEVE` como condición de no gravedad conocida: `POR_CLASIFICAR` no se presume leve. Las denuncias
pueden empezar con presunta responsabilidad o papel desconocido; la corroboración y el descarte
no se aceptan como estado inicial del registro ordinario. No hay sanción ni decisión de culpabilidad.
El grupo es una copia histórica suministrada al reporte; un cambio de matrícula no lo modifica.

### Correcciones, aclaraciones y anulación

- `corregirPorAutor` valida autoría y solo permite corregir antes de la atención institucional.
  Conserva el contenido anterior en `revisiones`; una corrección ordinaria no exige motivo.
- `corregirInstitucionalmente` conserva la nueva revisión y marca atención. Para un hecho previamente
  atendido, un cambio sustancial exige causa. La sobrecarga con `TipoCorreccion.RUTINARIA` permite
  corregir escritura/fecha sin causa redundante; cambios de participantes, clasificación o año
  siguen requiriéndola. La distinción semántica de una errata y un cambio de hechos corresponde al
  operador institucional autorizado; el modelo no intenta deducirla del texto.
- `aclararPorAutor` añade el texto propio sin alterar las atribuciones institucionales ni reescribir
  el reporte original, incluso después de la atención.
- `anularPorError` exige causa breve, mantiene el contenido y lo excluye de cómputos. No representa
  «denuncia no probada» ni eliminación por derechos. La justificación fáctica de un error la aporta
  el operador; el modelo no investiga hechos.
- Los cambios con fecha anterior a la última modificación del reporte se rechazan. La inmutabilidad
  evita alteraciones de instancias previas; no sustituye el control de concurrencia persistente.

### Duplicados y participaciones consolidadas

`Hecho.de(principal).vincularDuplicado(reporte, cambio)` expresa una decisión humana explícita,
sin concatenar relatos ni copiar participantes al principal. Cada comunicación conserva ID,
texto, autor, fecha, participantes y revisiones. `retirarDuplicado` exige causa y conserva el
historial del vínculo; el reporte retirado sigue siendo un agregado independiente. No se permite
retirar el principal como si fuera duplicado ni añadir nuevos reportes a un hecho anulado.

`participaciones()` agrupa por identidad inequívoca, conserva todas las fuentes mediante
`AporteParticipacion` y cuenta una participación por persona. Incluye participantes que existan
solo en el duplicado. `persona()` recupera una cuenta identificada en una fuente complementaria;
los nombres/relaciones originales y sus diferencias siguen disponibles en `aportes`. No resuelve
automáticamente diferencias de papeles o de corroboración.

Si los papeles difieren, `papelVigente` queda vacío hasta que una aclaración institucional permita
determinarlo. La persona sigue involucrada. `aclararParticipacion` conserva fundamento, autor,
fecha y una firma de fuentes (ID de reporte → papel original); no modifica esos papeles. Una
nueva fuente o un cambio de papel invalida la aclaración respecto del conjunto nuevo sin borrar
su historia. La última aclaración solo se utiliza cuando corresponde a las fuentes vigentes.
No se propaga a otro hecho ni a otra persona.

La aclaración marca atención de todos los reportes activos del hecho. La vinculación y
`actualizarReporte` propagan ese marcador cuando un aporte ya tenía atención, para evitar que
otro autor mantenga edición ordinaria sobre el mismo hecho atendido. El marcador no abre un
caso ni registra una actuación contable; representa la restricción de edición de RF-17.
`actualizarReporte` exige continuación de las revisiones y rechaza versiones anteriores o
incompatibles que perderían historia. El llamador debe conservar y sustituir el agregado completo.

Anular un reporte, incluso el principal, solo excluye esa fuente de los datos y participaciones
computables. `anulado()` es verdadero únicamente cuando **todos** los reportes del hecho están
anulados. Mientras exista una fuente válida, el hecho conserva sus participaciones, su posible
aviso y su cómputo. Sin fuentes válidas, las consultas de fecha/clasificación vigente devuelven
vacío; ninguna precisión institucional resucita por sí sola un hecho sin reportes válidos.

### Fecha y clasificación institucionales vigentes

`fechaVigente()` y `clasificacionVigente()` son las consultas del hecho que deben usar los cálculos
y las futuras vistas institucionales. Sin precisión expresa, cada consulta devuelve el valor
coincidente entre reportes **activos**; si ese dato difiere, devuelve vacío. No utiliza por defecto
los datos del principal ni los de un reporte anulado. Se compara la clasificación completa,
incluidas naturaleza, gravedad y aviso, para no elegir silenciosamente entre versiones de una
misma categoría. `requierePrecisionDatos()` indica si queda fecha o clasificación sin resolver.

`precisarDatos(fecha, clasificacion, cambio)` permite fijar conjuntamente los dos datos del hecho
por decisión institucional. Exige causa breve, autor e instante, valida que la fecha no sea futura
en Perú y marca atención en las fuentes activas. Conserva cada `PrecisionDatos` en `precisiones()`;
una rectificación posterior añade otra entrada y la última determina los valores vigentes.
Los reportes, sus relatos, fechas, categorías y revisiones originales no se modifican.
`discrepanciaFechaOCategoria()` sigue mostrando las diferencias de las fuentes aunque estén
resueltas institucionalmente; no equivale a `requierePrecisionDatos()`.

La precisión del hecho permanece vigente al vincular, retirar, actualizar o anular una fuente,
aclarar una participación o cambiar el principal; solo otra precisión institucional la sustituye.
Las nuevas fuentes no revocan automáticamente una decisión institucional, y sus diferencias
siguen disponibles para revisión. No se fabrica precisión temporal: una fecha institucional
aproximada se conserva, pero no se incluye en el contador automático.

`requiereAvisoInmediato()` usa la clasificación vigente. Si aún no puede determinarse, mantiene
el aviso preventivo cuando cualquiera de las fuentes activas lo exige por gravedad/protección.
Una solicitud expresa de atención de una fuente activa sigue generando el motivo, incluso si
la clasificación institucional no es grave. Se calcula el motivo actual; las señales históricas
emitidas se conservan como valores históricos en el incremento 6; su almacenamiento durable corresponde a infraestructura futura.

### Corrección del reporte principal

`cambiarPrincipal(reporteId, cambio)` selecciona otro reporte ya vinculado y válido; exige causa,
autor y fecha coherente. Cada `CambioPrincipal` conserva las referencias anterior/nueva y el
registro de la decisión en `cambiosPrincipal()`. No restaura un reporte anulado, cambia su texto,
mueve participaciones ni fusiona contenidos. También permite corregir una selección entre dos
reportes válidos o volver a una referencia anterior válida, con su nueva constancia.

La identidad `Hecho.id()` se fija al crear el agregado y no cambia al corregir el principal;
`principal().id()` identifica la referencia seleccionada y puede ser distinto del ID del hecho.
`reportes()` conserva su orden y contenido: ya no debe interpretarse su primera posición como
la selección vigente. Si el principal se anula, sigue visible como referencia histórica hasta
que un autorizado lo cambie; eso no bloquea el uso de las demás fuentes válidas. No hay selección
automática de otro principal. Solo el principal actual está protegido frente a retirada como
duplicado. La calculadora rechaza dos versiones del mismo ID de hecho, además de reportes
repetidos, para impedir que el cambio de principal duplique ocurrencias.

Estas operaciones agregan trazabilidad en el mismo dominio inmutable. La política del incremento 2
permite decidir la autorización del operador antes de invocarlas. `RegistroAtenciones` coordina versiones
posteriores y ahora también duplicidad/separación de hechos ya atendidos; `SeguimientoInstitucional`
coordina los avisos de esa transición. La persistencia transaccional sigue fuera del dominio.

## Cálculo de reiteración y motivos de atención

`pe.edu.incidencias.domain.alerta.CalculadoraReiteracion` es una función pura. Recibe estudiante,
categoría, parámetros comunes, fecha local actual y hechos consolidados. Devuelve configuración
explícita y, cuando está habilitada, un `Fundamento` inmutable con ventana inclusiva, valores
utilizados y IDs de hechos con el carácter de cada atribución.

- Ventana de W días: `[hoy − (W − 1), hoy]`. Se utiliza la fecha efectiva exacta del hecho,
  independientemente de cuándo se registró. Las cargas tardías no simulan notificaciones pasadas.
- Cuenta una vez por estudiante/hecho/categoría, incluso si la atribución elegible solo está en
  el duplicado. La evaluación rechaza datos de entrada que repitan un reporte en dos hechos.
- Distingue cotidiana atribuida, presunta responsabilidad y atribución corroborada; el total
  se acompaña del desglose, sin llamar «corroboradas» a todas las ocurrencias.
- No cuenta afectados, testigos, otros, desconocidos, descartados, referencias provisionales,
  familiares o externos como responsabilidades del estudiante. Los antecedentes contextuales
  no forman parte de los datos de entrada del contador.
- Excluye hechos anulados, fechas aproximadas y discrepancias de atribución no aclaradas.
- Si duplicados contradicen fecha o clasificación sin una precisión institucional vigente, conserva los datos y devuelve el ID en
  `hechosConDatosDiscrepantes` al evaluar cualquiera de las categorías de sus fuentes activas.
  Una precisión resuelve ese bloqueo; el contador utiliza `fechaVigente()` y `clasificacionVigente()`,
  aunque difieran de todos los aportes originales o del principal seleccionado. Los datos
  corregidos se computan bajo la categoría vigente y dentro de su ventana efectiva.
- Si una precisión cambia la clasificación a violencia/protección o a una conducta no leve,
  la atribución cotidiana original no se transforma automáticamente en una atribución elegible
  de esa nueva naturaleza. Se conserva su papel original; una aclaración institucional puede
  expresar presunta responsabilidad o corroboración cuando corresponda. La validación de una
  aclaración cotidiana también utiliza la clasificación vigente, no la del reporte principal.
- Sin ventana/umbral aprobados no calcula un fundamento; devuelve los parámetros faltantes.
  No se usan tres ocurrencias ni diez días como valores reales: aparecen únicamente en pruebas.

`Hecho.requiereAvisoInmediato()` evalúa la clasificación vigente y las peticiones expresas activas;
si la clasificación sigue en discrepancia, preserva el aviso preventivo de las fuentes activas,
sin esperar confirmación de papeles, resolución de duplicados o umbral.
Es el **motivo de dominio**. El incremento 6 lo utiliza para conservar señales, destinatarios,
lectura, deduplicación, gestión por atención y bandeja; no realiza entrega ni almacenamiento externo.

## Cuentas, designaciones y autorización: incremento 2

Base: capítulo 2; RF-01, RF-15, RF-27–28; RN-06–08, RN-23, RN-26; escenarios de CU-08,
CU-09 y CU-14. Es un bloque de dominio puro, no una implementación de autenticación ni de casos.
Todos sus tipos están en `pe.edu.incidencias.domain.acceso`.

| Tipo | Responsabilidad y API de revisión |
|---|---|
| `RolUsuario` | Roles ordinarios, TOE, miembro del comité, director, AIP y administrador operativo. El directivo ordinario es distinto del director. |
| `Vigencia` | Intervalo de instantes: inicio inclusivo y fin exclusivo opcional. |
| `DesignacionRol` | Identidad de designación, rol, vigencia original, otorgamiento y cese eventual con autoría/fecha. |
| `Usuario` | Cuenta nominal, identificador de acceso y estados/designaciones inmutables; `activaEn` y `rolesVigentes`. |
| `DirectorioUsuarios` | Alta, asignación/cese de roles, baja/reactivación y listado; cada operación verifica administrador vigente. |
| `AmbitoReserva` | Referencias a hechos, reportes y casos que revelan el mismo asunto; unión y cruce por tipo de ID. |
| `RestriccionesAmbito` | Historial inmutable de exclusiones y retiros. Mutaciones internas del paquete, protegidas públicamente por la política. |
| `Permiso` | Operaciones cerradas y necesidad de ámbito; la matriz se decide expresamente en la política. |
| `ContextoAcceso` | Ámbito completo suministrado, hechos que lo revelan, aporte solicitado y motivo de sustitución si corresponde. |
| `PoliticaAcceso` | Decisión de permiso, vista del aporte propio, filtros, destinatarios y registro/retiro de restricciones. |

### Cuentas y designaciones

`DirectorioUsuarios.inicializar` es la frontera excepcional de aprovisionamiento del primer
administrador. Exige una constancia institucional motivada; no autentica al otorgante ni permite
suponer que cualquier solicitante puede usarla. La futura aplicación no debe publicarla como
autoalta ni reinicialización. Esta es la única entrada inicial sin un administrador previo.

`registrarUsuario`, `asignarRol`, `finalizarDesignacion`, `desactivarUsuario` y `activarUsuario`
devuelven una nueva instantánea. Exigen que el autor exista, esté activo y tenga designación
de administrador operativo vigente en el instante del cambio. Se prohíbe toda autoasignación
de roles, incluso si el actor es administrador. No se exigen motivos ni adjuntos en el
mantenimiento rutinario; los movimientos conservan automáticamente operador, fecha y referencias.

El identificador de acceso es único en el directorio, comparado sin mayúsculas y sin espacios
exteriores; se rechazan espacios internos. No se usa el nombre laboral como identidad única.
Las designaciones tienen IDs únicos y se acumulan sobre la misma cuenta, sin crear otra cuenta
por pertenecer al comité. El listado conserva roles y vigencias para una revisión de privilegios;
no implementa un recordatorio semestral ni certifica que la institución haya hecho la revisión.

Una designación no habilita permisos antes de su registro, aunque declare un comienzo anterior.
El fin exclusivo y la baja retiran permisos en la siguiente evaluación. El cese puede ser actual
o futuro, incluida la cancelación de una designación todavía no iniciada; no altera la vigencia
original ni produce una baja retroactiva. Se conservan estados históricos, designaciones y
autorías tras la baja. Una reactivación no fabrica nuevos roles: evalúa las designaciones vigentes.
No hay contraseñas, credenciales compartidas, recuperación de acceso ni sesiones en este bloque.

### Permisos y responsabilidades específicas

`evaluar(usuario, permiso, contexto, ahora)` consulta cuenta, roles vigentes y restricciones en
cada llamada. Las operaciones que necesitan ámbito se rechazan sin él. Una exclusión prevalece
antes de examinar autoría, rol administrativo o membresía. No hay un permiso especial de
administrador que permita leer el contenido. `PoliticaAcceso.exigir(decision)` lanza
`SecurityException` cuando la decisión está denegada; las entradas/cronologías inválidas se
rechazan mediante excepciones de validación.

| Capacidad | Decisión de dominio |
|---|---|
| Buscar identidad mínima | Registradores institucionales/ordinarios y AIP; no administrador por su solo rol. |
| Registrar hechos | Docente, auxiliar, directivo ordinario, TOE, comité y director. |
| Importar nómina | AIP; los roles institucionales no lo adquieren automáticamente. |
| Consultar/rectificar/aclarar aporte propio | Autor con capacidad funcional; no habilita aporte ajeno ni hecho consolidado. Rectificar y acción cotidiana quedan limitadas por la atención iniciada. |
| Acción cotidiana inmediata | Aporte propio activo, todavía no atendido y clasificación vigente disciplinaria leve. Una clasificación discrepante o reclasificada grave no reutiliza la copia leve del reporte. |
| Consulta institucional, atención, aclaración, informes, documentos y estadísticas | TOE, miembro del comité y director, con la misma amplitud institucional y sin conflicto. La membresía no transfiere recepción cotidiana automáticamente. |
| Definir configuración | TOE/director; no miembro por su sola membresía ni administrador por su solo rol. Carga delegada pendiente. |
| Administrar cuentas | Administrador designado y vigente; no da acceso funcional a expedientes. |
| Convocar comité | TOE ordinariamente. Comité/director sustituyen solo si hay TOE vigente y todos los TOE vigentes están excluidos en el asunto, con motivo. La mera ausencia o falta de designación no se considera automáticamente conflicto. |
| Registrar actos formales del comité | Director vigente y no excluido; si no hay director elegible por exclusión o falta de vigencia, miembro del comité con motivo de sustitución. TOE sin membresía no obtiene esa facultad. |

Las decisiones por sustitución devuelven `porSustitucion` y el motivo. No registran una convocatoria,
un acuerdo o un caso: el incremento 5 guarda esos datos con autoría real y la sustitución aplicada.
La política comprueba quién podría actuar, no si un caso pendiente es especialmente grave/no resuelto.

`consultarAportePropio` devuelve una proyección `AportePropio`, nunca `Hecho`. Incluye solo
revisiones cuyo autor es el reportante y sus aclaraciones propias; omite revisiones hechas por
otros operadores institucionales, reportes ajenos, participantes agregados por otras fuentes,
motivos de exclusión, precisiones del hecho y contenidos de atención. Permite conocer el marcador
de atención y la condición de anulado sin revelar la causa institucional. No sirve como descarga
del informe ni de historia completa. Una baja o exclusión también deniega esta consulta propia.

`filtrarHechosConsultables` solo sirve para hechos autónomos sin relaciones de caso omitidas.
`filtrarContextosConsultables` conserva el cierre suministrado de cada asunto y debe preferirse
al integrar casos/historiales. La salida no debe acompañarse de conteos de elementos excluidos.
Las estadísticas futuras deberán filtrar antes de calcular totales y derivados identificables.

### Ámbitos y exclusiones expresas

`AmbitoReserva.deHecho` incluye el ID estable y **todos** los reportes, incluidos anulados;
no depende del principal elegido. `ContextoAcceso` une automáticamente el ámbito suministrado
con esos IDs de todos los hechos entregados. Distingue IDs de hecho/reporte/caso, rechaza
versiones repetidas de un hecho y exige que un aporte solicitado pertenezca al contexto.
No se extiende la exclusión por compartir estudiante. Los datos de otro asunto autorizado del
mismo estudiante siguen disponibles.

La construcción del contexto **no descubre relaciones no suministradas**. El futuro caso deberá
aportar todos los hechos/reportes/casos que revelen la denuncia; no basta presentar un ID aislado
ni agrupar hechos ajenos por estudiante. Un permiso de documento, exportación o estadística exige
el mismo contexto reservado que el acceso directo. El paquete no implementa consultas SQL ni
puede recuperar por sí mismo vínculos omitidos.

`PoliticaAcceso.excluir` exige TOE/director/comité vigente, activo y no excluido; el destinatario
debe existir y ser distinto del operador. Registra motivo, autor, fecha, ID y ámbito. La transición
interna permite atribuir una prevención al autor de su fuente, incluso un autoinforme, pero eso no
habilita una autoexclusión expresa por la API pública. Las mutaciones de `RestriccionesAmbito`
son internas al paquete; una llamada externa no puede saltarse la política usando ese agregado.

`retirarExclusion` exige un responsable institucional no excluido, causa y el ámbito completo
original. Nunca permite retirar la propia restricción. Retira únicamente esa fuente de exclusión,
sin borrar el registro ni otras restricciones coincidentes. Los comandos internos exactos tienen
idempotencia local; reutilizar un ID con otro contenido se rechaza. Reintentar un alta ya retirada
no la revive. Esto no sustituye la idempotencia transaccional de la futura aplicación.

### Prevención automática y retiro motivado

`restriccionesActualizadas(contexto, ahora)` examina fuentes históricas, no solo la participación
consolidada actual. Detecta personal denunciado/corroborado en revisiones de reportes y en
aclaraciones institucionales de `Hecho`, vinculándolo a su cuenta incluso si esta se identificó
después. La prevención se activa para cuentas con capacidad institucional actual o en el
momento conocido de atribución/identificación. Un papel de testigo no es conflicto automático.

Cada entrada a un papel denunciado de una fuente se identifica de forma determinista por su
origen, persona y cuenta; las aclaraciones se distinguen de las revisiones del reporte. No se usa
el nombre como identidad y no se interpreta el contenido del relato para determinar culpabilidad.
Una precisión de fecha/categoría no confirma responsabilidad de nadie.

El resultado es otro `RestriccionesAmbito` que debe persistirse junto con la operación. Cada
evaluación y selección de destinatarios lo calcula igualmente para que no se emita un aviso al
implicado aunque aún no se haya guardado esa proyección. La fecha de materialización es el
instante confiable de evaluación; el autor es el operador de la fuente que origina la prevención.
La fuente y episodio quedan vinculados por el identificador determinista y por las historias
inmutables del hecho/reporte.

La prevención no desaparece por anular el reporte, cambiar el principal, corregir su papel a
testigo o descartar después la atribución. Se conserva la evidencia original y el historial de
exclusión. Un responsable no implicado puede retirarla expresamente con motivo: ese mismo ID
queda retirado y no se reactiva al recalcular ni por repetir una aclaración sin nueva transición.
Otra fuente o nueva entrada a un papel denunciado puede producir otra prevención independiente.
Este criterio evita tanto la elusión por corrección como una prohibición imposible de levantar.

La política rechaza evaluar antes de la configuración vigente o con fuentes/decisiones del hecho
posteriores al instante recibido: revisiones, aclaraciones, vínculos, precisiones y cambios de
principal. El reloj debe suministrarlo la aplicación; no se aceptará la hora del cliente como prueba
de vigencia. No hay lectura del reloj global dentro del dominio.

### Destinatarios y continuidad

`destinatariosAtencion` prioriza TOE elegible y, si no queda, devuelve integrantes elegibles del
comité/director. Reevalúa conflicto y vigencia antes de incluir cada usuario, no convoca al comité
ni abre caso. Si no queda nadie, devuelve lista vacía y `requiereDesignacion=true`, sin relato,
identidades de participantes o causa de denuncia. El administrador no se convierte por ello en
receptor de contenido; la futura aplicación solo podrá mostrarle la necesidad operativa mínima.

### Integración pendiente y límite del incremento

La política es de dominio, no un interceptor de todos los métodos Java. Los agregados anteriores
siguen siendo objetos internos: pasarles un UUID de autor no acredita autorización. Las futuras
entradas del monolito deberán cargar directorio y relaciones actuales, formar el contexto,
sincronizar restricciones, exigir el permiso y persistir las nuevas instancias con versión esperada.
Solo después podrán entregar una vista autorizada o emitir avisos. Deben conservar los retiros
y no reconstruir el directorio/restricciones desde una copia antigua para recuperar permisos.

Quedan pendientes autenticación, contraseñas/recuperación, sesiones y enlaces de descarga,
bloqueo jurídico por disposición sobre datos, delegación de carga de configuración, alcance AIP
del lote/conciliación, auditoría persistente de accesos, resolución de relaciones en repositorios,
transacciones y concurrencia. No se implementa respaldo ni se concede a TOE/AIP por su rol.
No hay aceptación integral de CU-09 ni de seguridad operativa: se prueban las decisiones de dominio.

## Atención compartida, actuaciones y compromisos: incremento 3

Base: RF-14, RF-22–26; RN-05, RN-12–13 y RN-15; invariantes de CU-06 y CU-07.
Paquete `pe.edu.incidencias.domain.atencion`. **Caso** significa aquí la entidad de atención
local de la ERS, no un caso de uso de `application`. No se añadieron controladores, servicios
de aplicación, puertos ni adaptadores. Los métodos devuelven otro estado de dominio sin I/O.

| Tipo | Responsabilidad |
|---|---|
| `CasoAtencion` | Identidad, responsable, inicio efectivo, hechos actuales/históricos, antecedentes, actuaciones, derivaciones y revisiones de cierre. Sus únicos estados son `PENDIENTE` y `CERRADA`. |
| `RegistroAtenciones` | Colección inmutable que coordina la unicidad de atención pendiente, referencias a hechos/acciones y la política de acceso. No es un repositorio en memoria ni garantiza transacciones de base de datos. |
| `Actuacion` | Acción `PREVISTA`, `REALIZADA` o `CANCELADA`, responsable, alcance por hechos/personas, fechas pertinentes, relato suficiente y compromisos. |
| `Compromiso` | Contenido, persona que asume, objetivo opcional, observaciones de cumplimiento y continuidad explícita de seguimiento. |

### Apertura y agrupación

`RegistroAtenciones.iniciar(hechos, restricciones)` recibe hechos existentes y rechaza IDs de
hecho repetidos o el mismo reporte en dos hechos. No crea estudiantes ni comunicaciones nuevas.
`abrirCaso` recibe hechos seleccionados, fecha efectiva, responsable, actuaciones opcionales y
cierre opcional: permite guardar conversación, compromiso y conclusión en una sola transición.
No necesita otra descripción de hechos, motivo rutinario de apertura, programación, recepción,
confirmación de atribuciones ni una apertura guardada previamente.

Se exige permiso institucional de atención tanto al operador como al responsable inicial. No
se abre desde un conjunto vacío, hechos inexistentes o hechos sin reportes válidos. Cada hecho
puede pertenecer a una sola atención **pendiente** en la colección. No se impone un caso por
estudiante; varios hechos de la misma persona pueden recibir atenciones diferentes cuando
corresponda. Cerrar una atención permite otra posterior sobre un hecho por decisión explícita,
sin incrementar ocurrencias del hecho ni reabrir la anterior. Reutilizar el ID de una atención
se rechaza en vez de duplicar el guardado; el reintento transaccional por clave sigue pendiente.

`vincularHechos` añade relaciones ordinarias sin motivo obligatorio y conserva los datos del
hecho, incluidos participantes que figuren solo en duplicados y sus papeles/fuentes. No selecciona
otra vez participantes ni propaga una atribución por estudiante/caso. `hechosDelCaso` devuelve
las fuentes autorizadas, con sus precisiones de fecha/clasificación vigentes ya disponibles.
Guardar atención marca los reportes activos como atendidos para RF-17, sin corroborar sus papeles.

`retirarVinculo` requiere causa y conserva el hecho en `hechosHistoricos`, la relación retirada,
actuaciones y sus alcances. Si se retira el último vínculo de una apertura errónea, el caso se
conserva cerrado por `APERTURA_ERRONEA`; no queda una atención pendiente vacía ni se borran los
seguimientos reales que existían. `citarAntecedente` referencia un caso cerrado sin copiar sus
actuaciones. El incremento 5 añade continuidad explícita entre pendientes, conservando los datos en su caso de origen.

### Actuaciones y compromisos

`Actuacion.realizada` admite directamente fecha efectiva y un relato suficiente que exprese
acción/resultado. No exige dos textos, planificación previa o adjuntos. `prevista` registra solo
un trabajo realmente programado. Al `realizar` se conserva el ID y la fecha prevista histórica;
una prevista no se cuenta realizada y su ejecución no crea una segunda actuación. `cancelar`
exige causa y no marca realizada. Las fechas efectivas no son futuras según Perú; las previstas
sí pueden serlo. Se conservan revisiones con autor/fecha y se rechazan versiones que perderían
historia. Los alcances de hechos no pueden referirse a hechos ajenos al caso; los vacíos indican
la atención completa. Las personas de una actuación no reciben por ello papeles en la incidencia.

Un `Compromiso` requiere contenido y persona que asume, que puede ser una referencia mínima
sin cuenta ni documento. El objetivo solo existe si fue acordado. El plazo o un seguimiento
expreso mantienen una tarea real; sin ambos no aparece un pendiente inventado ni vencimiento.
`registrarSeguimiento` conserva fecha, observación, cumplimiento observado opcional y si continúa
el trabajo. Ausencia de observación no se vuelve cumplimiento; incluso un `NO_CUMPLIDO` puede
mantener seguimiento. Terminar un pendiente sin haber observado cumplimiento tampoco lo declara
cumplido. Cancelar o sustituir sustancialmente el acuerdo exige causa y conserva su historia.

`registrarActuacion` incorpora o actualiza descendientes de la misma actuación, con ID estable.
Se rechazan copias del mismo ID en otra atención y compromisos duplicados entre actuaciones,
también dentro de un único guardado inicial. El caso valida que la actuación efectiva no preceda
a su inicio; una acción cotidiana anterior conserva su propia procedencia como referencia.

### Acciones inmediatas sin caso

`registrarInmediata` vincula una actuación ya realizada al aporte propio cotidiano habilitado
por `PoliticaAcceso`; no crea un caso. Al abrir atención, `actuacionesDelCaso` recupera esas mismas
instancias por sus reportes de origen. No las copia al mapa de actuaciones del caso ni les da
otro ID. Un intento de incorporarlas de nuevo como nuevas actuaciones se rechaza.
`actualizarInmediata` conserva su historia y permite seguimiento institucional cuando el autor
ordinario ya no tiene edición tras atención. Los compromisos siguen perteneciendo a una sola
actuación. Los pendientes de estas referencias también se consideran al consultar trabajo real.

### Reserva del conjunto y evolución de los hechos

`contexto(casoId)` protege el contenido que presenta el caso y sus antecedentes consultados.
`hechosReservados` distingue hechos vigentes y fuentes que permanecen en actuaciones, derivaciones,
convocatorias o continuidad real de los simples vínculos erróneos retirados. Estos últimos conservan
su rastro en `hechosHistoricos`/`vinculos` sin arrastrar automáticamente el relato al caso independiente.
Los casos que referencian un hecho aportan sus IDs al ámbito de ese hecho, sin incorporar por transitividad
el contenido de otros asuntos. No se extiende por compartir estudiante. Añadir o corregir vínculos autoriza
antes de modificar; las exclusiones no se retiran al separar, se recalcula qué contenido revelan.

Las transiciones reciben directorio y operador/instante y usan la política existente. Guardan
también el resultado de sincronizar restricciones preventivas. `consultarCaso`, `hechosDelCaso`,
`actuacionesDelCaso` y `conTrabajoPendiente` aplican permisos; los getters estructurales `caso`,
`casos` y `contexto` son para colaboración interna del dominio/persistencia, no vistas que puedan
serializarse al cliente sin autorización. Las sesiones y descargas reales siguen pendientes.

`incorporarHecho` admite otra fuente existente sin abrir/reabrir atenciones. `actualizarHecho`
admite versiones descendientes, precisiones, cambios de principal, anulación, nuevos reportes
e identificación posterior. Verifica prefijos de historia, autoriza sobre la reserva anterior
y sincroniza prevención antes/después. Identificar después al personal denunciado retira acceso
al caso ya abierto en la siguiente consulta; no se mantiene una copia vieja de sus participantes.
`excluirUsuario` y `retirarExclusion` autorizan sobre el contexto pertinente. La exclusión se ancla al
caso declarado o a las fuentes expresamente indicadas, no a una copia de toda la propagación.
La prevención se ancla al reporte denunciado o a la aclaración del hecho que la origina.

`actualizarHecho` continúa rechazando la eliminación implícita de fuentes. Para hechos existentes se
utilizan las transiciones explícitas `relacionarDuplicados` y `separarDuplicado`, que coordinan fuentes,
atenciones, reserva e historia sin fusionar actuaciones. Su detalle figura en la revisión correctiva.

## Derivaciones y cierre local: incremento 4

Base: RF-29–30, RN-15–16 e invariantes de CU-07/CU-10. Valores nuevos `Derivacion` y `CierreLocal`,
integrados con `CasoAtencion` y `RegistroAtenciones`.

### Preparación y ejecución

`Derivacion.preparada` es opcional; `efectuada` admite ejecución directa sin guardar preparación.
Destino, supuesto y cobertura por IDs de hechos son explícitos. El relato puede bastar para
motivo/constancia, y la referencia es opcional y completable después; no se exige oficio, archivo,
número oficial ni identificación inventados. `efectuar` conserva el ID y la preparación previa.
`completarReferencia` añade revisión sin repetir la ejecución. Se comprueban fechas reales en
Perú, orden de registros, cobertura no vacía y que los hechos pertenezcan a la historia del caso.
Una derivación no se copia con el mismo ID a otro caso. Registrar no envía comunicaciones.

### Regla familiar de cierre

`registrarDerivacion` cierra automáticamente una atención pendiente únicamente si la derivación
está **efectuada**, declara el supuesto `VIOLENCIA_FAMILIAR`, su destino es `FISCALIA` y su cobertura
alcanza todos los hechos actuales del caso. Recupera fecha y resultado de la ejecución; no pide
motivo repetido. Preparar, otro destino, otro supuesto o cobertura parcial de un caso mixto
mantienen pendiente la atención. No se suman preparaciones o coberturas parciales para inferir
que una derivación cubre todo. El supuesto familiar/cobertura los declara el operador con los
datos institucionales; no se deducen del texto, nombre de entidad ni de toda categoría de violencia.

El cierre registra `DERIVACION_FAMILIAR_FISCALIA` y el ID de la derivación real. No puede invocarse
esa etiqueta manualmente sin origen efectivo, cobertura completa y fecha correspondiente. La
ejecución no puede preceder al inicio efectivo de atención. Nada declara cierre fiscal, de SíseVe,
solución de la violencia o cumplimiento del protocolo oficial.

### Conclusión, pendientes y corrección

`CierreLocal` conserva fecha efectiva, resultado suficiente, tipo, autor/registro y origen cuando
corresponde. Una conclusión ordinaria no exige otra causa. No puede preceder al inicio ni, en
su guardado inicial, a una conversación realizada que se esté usando para concluir la atención.
La derivación familiar puede haber ocurrido antes de otros acompañamientos ya documentados:
su fecha de cierre es la de ejecución, sin borrar la continuidad posterior.

Cerrar no modifica actuaciones/compromisos ni presume cumplimiento. `requiereTrabajo` distingue
estado del caso de seguimiento real: una cerrada con acciones previstas, acuerdos con seguimiento
o derivaciones preparadas permanece en la proyección de pendientes. Una cerrada sin pendientes
sale de ella y conserva su historia. El incremento 6 integra esta proyección en la bandeja y coordina avisos.
El seguimiento posterior se registra sobre la misma atención sin reabrirla ni cambiar su cierre.
Completar referencias o registrar otra derivación después tampoco reemplaza el cierre original.

`corregirCierre` exige causa y conserva la conclusión anterior en `cierres`. Puede sustituirla
o retirar un cierre equivocado para dejar pendiente; en este último caso se vuelve a verificar
la unicidad de atención pendiente por hecho y se rechaza conflicto con otra atención vigente.
No existe reapertura automática ni un procedimiento administrativo obligatorio adicional.

### Límite de los dos bloques

La colección comprueba sus invariantes con el contenido relevante y un índice autoritativo de reserva
y atención pendiente. `cargarAmbito` permite reconstituir una parte consistente sin cargar el contenido
de todos los casos del colegio. Deben cargarse los antecedentes cuyo contenido se consulta y las
atenciones cuya continuidad se decide. El índice no sustituye transacción, versión esperada ni unicidad
persistente; no es válido omitir del índice una atención para aparentar que el hecho está libre.
No hay repositorio, autenticación, API, interfaz, persistencia durable de notificaciones ni comprobación de
envíos externos. El registro no descubre información almacenada fuera de sus entradas.
Los incrementos siguientes de este documento completan comité, reasignación, continuidad, actas,
informes y referencias en el dominio, manteniendo esos límites de infraestructura.

## Comité y continuidad: incremento 5

Base: RF-18, RF-22–23, RF-27–30; RN-06, RN-08, RN-12, RN-14–16; escenarios de CU-05,
CU-06, CU-08 y CU-09. Se amplían `CasoAtencion` y `RegistroAtenciones`; no se crea un expediente
separado del comité ni una capa de aplicación.

| Tipo nuevo | Responsabilidad |
|---|---|
| `EncargadoAtencion` | Encargado actual TOE/COMITE, independiente de los dos estados pendiente/cerrada. |
| `ComposicionComite` | Instantánea de cuentas/designaciones institucionales vigentes: director y miembros designados, sin plantilla fija de tres personas. |
| `ConvocatoriaComite` | Convocatoria expresa, fecha, motivo especial, composición y elegibilidad del ámbito; conserva autor y sustitución. |
| `IntervencionComite` | Participantes efectivos y su instante real de intervención, separados de composición y fecha de registro. |
| `DecisionComite` | Contenido de decisión ligado a una intervención efectiva, con operador real y sustitución. |
| `ActoComite` | Procedencia colectiva de actuación, derivación, decisión o cierre; referencia el dato original en vez de copiarlo. |
| `CambioResponsable` | Responsable anterior/nuevo y constancia trazable de reasignación. |

### Convocatoria, participación y registro formal

`convocarComite` opera sobre el mismo caso pendiente y exige motivo explícito; una categoría grave
o un contador no lo convoca automáticamente. Mantiene el estado pendiente y cambia el encargado
a COMITE. La composición procede del directorio vigente; los elegibles se filtran por la reserva
completa del caso. La instantánea de una convocatoria no reemplaza las designaciones institucionales.

Se distinguen integrantes vigentes, elegibles en el ámbito y personas que realmente intervinieron.
`registrarIntervencionComite` comprueba designación/cuenta y exclusiones en el instante efectivo
informado. Un cese posterior no borra una participación histórica válida ni concede acceso actual.
El operador que documenta se autoriza con las condiciones actuales. Las fechas efectivas no pueden
ser futuras y deben respetar el inicio/convocatoria conocidos, sin equiparar reunión a su registro tardío.

El director elegible registra ordinariamente decisiones y actos colectivos. Si falta director elegible,
un miembro restante habilitado puede sustituirlo con motivo conservado. TOE por su solo rol no
obtiene registro formal. Si TOE está excluido, la convocatoria puede efectuarse por sustitución
motivada conforme a la política existente. No se exigen firmas, aprobación individual ni acta adjunta.

Las operaciones `registrarDecisionComite`, `registrarActuacionComite`, `registrarDerivacionComite`,
`cerrarCasoComite` y `corregirCierreComite` enlazan una intervención real y conservan la autoría de
la decisión de acceso. Una revisión de actuación ya formal no puede entrar por el método ordinario
para eludir la sustitución. Derivación y conclusión de un caso encargado al comité exigen la vía formal.
Las acciones individuales de atención siguen siendo distinguibles de actos colectivos.

`reasignarResponsable` exige un operador y un nuevo responsable habilitados en el ámbito completo;
conserva la secuencia de cambios y no cambia el encargado o el estado del caso por sí sola.

### Continuidad sin fusión

`continuarAtencion(origenId, destinoId, hechosIds, directorio, cambio)` elige expresamente dónde
continúan los hechos entre dos atenciones pendientes. El destino incorpora vínculos y referencia
al origen; el origen retiene hechos históricos, actuaciones, compromisos y derivaciones, con la
referencia de continuidad. Los contenidos no se trasladan ni se copian y no aparecen dos atenciones
pendientes del mismo hecho. Los dos ámbitos se cierran antes de autorizar: una restricción del origen
también protege el destino y los derivados que revelen esa relación.

Si el origen conserva otros hechos o trabajo propio real, continúa pendiente por ellos. Si entrega
todos sus hechos pero conserva seguimiento propio, puede permanecer pendiente como atención
anterior relacionada, sin reclamar la atención vigente de esos hechos. Al agotar sus trabajos, se
cierra por DUPLICIDAD. Este cierre estructural expresa la elección de continuidad; no se presenta
como una conclusión colectiva sobre el fondo. La retirada ordinaria del último vínculo erróneo
mantiene su regla distinta de cierre por APERTURA_ERRONEA.

El ámbito distingue fuentes del contenido consultado y referencias de reserva; no hace equivalentes
todas las consultas del componente conectado. La revisión correctiva incorpora duplicados con
atenciones existentes y su separación motivada, conservando las atenciones anteriores relacionadas.

## Alertas, notificaciones y bandeja: incremento 6

Base: RF-19–21, RF-22, RF-28; RN-09–11, RN-23; escenarios de CU-03–05 y CU-07.
Son estados y proyecciones de dominio, sin correo, SMS, interfaz, planificador ni repositorio.

| Tipo nuevo | Responsabilidad |
|---|---|
| `MotivoAlerta` | Distingue gravedad, riesgo comunicado y reiteración. |
| `FundamentoAlerta` | Fuentes, motivos, clasificación y evaluación de reiteración emitidos, con instante explícito. |
| `Alerta` | Asunto con revisiones del fundamento y disposición separada; conserva hechos históricos. |
| `DisposicionAlerta` | Atención, intervención sin caso, duplicidad o descarte fundamentado, con resultado y referencia pertinente. |
| `Notificacion` | Destinatario y lectura, sin confundirla con gestión del asunto. |
| `RegistroAlertas` | Emisión/reevaluación, relación de motivos, destinatarios y sincronización con atención. |
| `EntradaBandeja` | Vista de trabajo: motivos, antigüedad, responsable, gravedad y vencimientos conocidos. |
| `BandejaAtencion` | Proyección autorizada de señales nuevas, atenciones pendientes y seguimientos reales. |
| `SeguimientoInstitucional` | Resultado conjunto inmutable de atención actualizada y avisos sincronizados, en el paquete `atencion`. |

### Señales conservadas y novedades

`evaluarHecho` avisa desde la primera comunicación grave/protección o solicitud expresa, aunque
no exista responsabilidad corroborada, parámetros de reiteración o caso. Usa clasificación vigente;
si sigue discrepante, conserva la precaución de las fuentes válidas. `evaluarReiteracion` utiliza la
calculadora existente y guarda su ventana, parámetros, caracteres de atribución y fuentes como
fundamento de esa emisión. No reconstruye una emisión histórica con parámetros nuevos.

Los motivos relacionados por sus hechos se reúnen en un asunto pendiente. Las señales absorbidas
se disponen por duplicidad, conservando referencia e historia. Reevaluar sin cambio material no
crea tareas ni notificaciones adicionales. Correcciones, anulación, duplicidad y variaciones de
parámetros actualizan el fundamento vigente sin borrar las versiones emitidas.

Una señal ya gestionada no vuelve a pendiente por repetir el cálculo o corregir sus fuentes
conocidas. Un nuevo hecho elegible puede originar otro episodio; los hechos que lo activan se
distinguen de los antecedentes que también sustentan la ventana. El algoritmo no reabre casos.

`leerNotificacion` registra lectura para su destinatario autorizado y deja la señal pendiente.
Una disposición sin caso requiere resultado suficiente; descartar gravedad no puede ser una
operación sin fundamento. No se infiere una intervención realizada a partir de la lectura.

### Atención y reserva conjunta

`SeguimientoInstitucional.actualizar(nuevasAtenciones, alertasAnteriores, directorio, cambio)`
devuelve ambos estados coordinados: decide la gestión pertinente y aplica las transiciones de avisos
pertinentes al abrir/vincular/documentar atención. Permite documentar conversación y cierre en la
misma transición, sin un segundo trámite de recepción. Es coordinación pura del dominio; el llamador
debe persistir ambos resultados en una transacción con versión esperada. No garantiza por sí misma
una transacción de base de datos ni obliga a crear un caso para toda señal.
El overload con `hechosModificados` limita la coordinación. `RegistroAlertas` ya no conoce
`RegistroAtenciones` ni recorre casos; conserva exclusivamente estados/fundamentos/avisos.

La sincronización comprueba novedad temporal y pertinencia de la atención a los hechos activadores;
una atención vieja o un seguimiento ajeno no consume automáticamente una señal nueva. Los contextos
se obtienen del cierre de relaciones de `RegistroAtenciones.contextoDeHechos`, incluidos los hechos
que pertenecen a casos distintos y las fuentes históricas relacionadas.

Destinatarios y consultas se revalidan con directorio y restricciones actuales. Se prefiere TOE
elegible; se usan miembros/director elegibles si corresponde. Si no hay destinatario, se conserva
la señal y solo se expone metadato de necesidad de designación, sin trasladar contenido al implicado.
Los avisos y la bandeja también pierden visibilidad tras exclusión o baja.

La bandeja recupera los pendientes reales del caso: programación sin realizar, compromisos con
seguimiento y derivaciones preparadas. Una atención cerrada con pendiente continúa visible; una
cerrada sin trabajo queda en historia. Las anotaciones cotidianas sin señal no crean trabajo.
No se agrupa por estudiante como sustituto de caso/hecho.

## Informes, actas y referencias: incremento 7

Base: RF-02 parcial, RF-31–33, RF-28; RN-20–21, RN-23; escenarios de CU-11 y CU-12.
Paquetes `documento` e `institucion`, sin lectura/escritura de archivos, renderizado PDF o envío.

| Tipo nuevo | Responsabilidad |
|---|---|
| `DatosInstitucion` | Valores institucionales conocidos, sin colegio o código fijados en Java. |
| `InstrumentoOficial` | Instrumentos/orígenes externos explícitos; no es un motor de protocolos. |
| `ReferenciaOficial` | Ámbito, instrumento/origen, código/fecha opcionales y observaciones/protocolo disponibles. |
| `ReferenciasOficiales` | Revisiones autorizadas de referencias y consulta por ámbito reservado. |
| `ArchivoAdjunto` | Identificador opaco, nombre, MIME y tamaño declarado del PDF/DOCX. |
| `ActaAdjunta` | Acta externa del caso y sustituciones motivadas que conservan referencias anteriores. |
| `RegistroActas` | Registro por operador formal y consulta/descarga con permiso actual. |
| `DestinoInforme` | Uso interno o destinatario/finalidad concretos para terceros. |
| `RevisionSalida` | Selección de personas/referencias/contexto y textos revisados para entrega externa. |
| `InformeCaso` | Instantánea consolidada de datos proyectados; distingue vista previa y emisión identificada. |
| `GeneradorInformeCaso` | Construcción pura del consolidado y revalidación de consulta/emisión. |

### Consolidado y emisión

El generador recibe registros de atención, actas, referencias, directorio e identidades escolares
necesarias. Produce un informe por caso que conserva cada hecho una vez, con sus datos vigentes,
fuentes originales, diferencias y participaciones procedentes de todos los reportes vinculados.
No convierte un afectado en responsable ni resuelve discrepancias por elegir el reporte principal.
La aclaración institucional se distingue de los aportes comunicados.

Incluye actuaciones y compromisos —también las acciones inmediatas referenciadas, una sola vez—,
derivaciones, estado/cierre local y seguimiento, intervención/decisiones del comité, actas y
referencias oficiales disponibles. Las continuidades se identifican sin copiar actuaciones del
otro caso. El contexto se incorpora mediante selección explícita, no desde una ficha completa.
Los antecedentes de atención muestran referencia, estado y trabajo pendiente propio, incluida
la fuente de una continuidad; no se ocultan sus pendientes ni se copian sus actuaciones al destino.
El módulo de antecedentes contextuales y su procedencia aún está pendiente; el mapa seleccionado
es un límite de entrada, no una implementación de esa ficha ni su importador.

La vista previa no tiene identificador de emisión. Emitir asigna el ID y conserva quién/cuándo,
institución, motivo, destinatario/finalidad y corte. Una nueva actuación no reescribe la instantánea
ya emitida. El corte describe los datos proporcionados al generador; no es una consulta temporal
arbitraria a una base de datos ni permite usar un registro con datos posteriores al corte.
Emitir no acredita firma, envío, registro oficial ni cierre de un protocolo.

`consultarEmitido` exige construir el generador con el registro/directorio actuales y vuelve a
comprobar la reserva del caso; devuelve la misma instantánea, sin regenerarla. Una exclusión nueva
deniega nuevas lecturas aun si el informe fue emitido o consultado antes. Los getters estructurales
de valores/agregados son internos: no deben convertirse directamente en endpoints de descarga.

### Salida a terceros

El destino externo exige destinatario, finalidad y `RevisionSalida` del caso, con base de comunicación
declarada y evaluación del riesgo de identificación conservadas en la instantánea. Registrar esa base
no comprueba automáticamente su validez jurídica ni crea un trámite adicional de aprobación.
La revisión cubre la salida seleccionada completa. `textosRevisados` contiene únicamente las
adecuaciones necesarias; un fragmento existente seguro se reutiliza sin exigir transcribirlo ni
una confirmación individual por campo. Si el texto incluye una identificación protegida conocida,
se exige adecuar ese fragmento. No se intenta adivinar o reemplazar semánticamente identidades.
La selección permite omitir personas, referencias y contexto ajenos a la comunicación.
La revisión conserva operador y fecha y se autoriza en el mismo ámbito.

En documentos sobre violencia dirigidos a terceros los estudiantes se identifican mediante iniciales
y DNI conocido; si falta,
se indica su ausencia junto con referencia interna. No se inventa DNI ni se bloquea por ese solo
desconocimiento. Esto no promete anonimato: los textos revisados, la selección y el destinatario
siguen bajo responsabilidad institucional. Para documentos no violentos esa regla específica no
se impone automáticamente: se mantiene la identificación seleccionada y su documento conocido,
con la misma revisión/minimización. No hay publicación ni entrega automática.

### Actas y constancias externas

Se admiten varias actas del caso. `ArchivoAdjunto` limita formato declarado a PDF/DOCX y tamaño a
10 000 000 bytes inclusive (interpretación decimal de 10 MB); rechaza archivos vacíos, exceso,
extensión/MIME discordantes o rutas como nombre. Son metadatos: comprobar firma real del archivo,
contenido malicioso, almacenamiento reservado y entrega de bytes corresponde al adaptador futuro.

Sustituir un acta exige motivo y otro identificador de archivo, conserva fecha/autor y la referencia
sustituida. Cada incorporación conserva además el motivo de sustitución del director devuelto por
la política, cuando actúa un miembro sustituto; las versiones anteriores mantienen su propia autoría
y causa. `archivoParaDescarga` revalida el caso y exige la versión vigente; una referencia de
descarga obtenida antes no permite leer después de una exclusión ni obtener la versión reemplazada.
Cada registro incorpora una revisión nueva autorizada, o reconoce el reintento de la misma historia;
no permite introducir varias revisiones intermedias sin comprobar su incorporación.
Las referencias históricas se conservan para trazabilidad, sujetas a la política de conservación pendiente.

Las referencias oficiales pueden estar incompletas y completarse después; corregir un dato conocido
sustancial deja causa. Origen SíseVe no significa nuevo registro, envío efectuado ni cierre externo.
Estas relaciones no crean hechos, casos o sanciones, no alteran el cierre local y no son requisito
para documentar protección urgente. No se migra retrospectivamente un protocolo por cambiar su versión.

## Revisión correctiva de los siete incrementos

Esta revisión atiende los cinco puntos solicitados sin crear otro incremento ni nuevas clases Java
productivas. Se mantienen `application`, `adapter`, POM y dependencias sin cambios. Se añadió la suite
`CorreccionesIncrementosTest` y se ampliaron las pruebas externas; se migraron los llamadores de
sincronización de avisos al coordinador. Las reglas de esta sección reemplazan las limitaciones
anteriores sobre duplicados atendidos y propagación histórica indiscriminada.

### 1. Reutilización e identificación en informes externos

`RevisionSalida` sigue siendo la revisión institucional de la salida completa para finalidad,
destinatario, base declarada, selección y riesgo. Su mapa `textosRevisados` es ahora un conjunto
de sustituciones opcionales: `texto(clave, original)` reutiliza el dato existente cuando no necesita
adecuación. No se exige un texto nuevo por relato, actuación, autor, motivo, finalidad o destinatario.

`GeneradorInformeCaso` comprueba que un fragmento reutilizado o sustituido no contenga una
descripción/nombre completo conocido de una persona excluida de la selección ni de un estudiante
que deba aparecer con iniciales. Solo el fragmento afectado debe adecuarse. La comprobación también
se aplica al contexto seleccionado. No es anonimización semántica, detección universal de nombres,
ni sustitución por conjetura: la revisión humana de necesidad/riesgo se conserva.

Iniciales más DNI se aplican a documentos sobre violencia dirigidos a terceros. Para determinarlo
se utiliza la clasificación institucional vigente; si no está precisada, las fuentes activas que
comuniquen violencia; también el supuesto registrado de derivación por violencia familiar. En
informes externos no violentos no se impone esa transformación ni se descarta un documento distinto
del DNI por esa regla específica. La selección/minimización sigue siendo obligatoria para ambos.
La ausencia de DNI en violencia conserva indicación y referencia interna sin bloquear urgencia.

### 2. Duplicados con atenciones existentes

`RegistroAtenciones.relacionarDuplicados(principalId, origenId, casoContinuidad, directorio, cambio)`
autoriza el conjunto antes de relacionar. Si ambos hechos tienen atenciones pendientes diferentes,
exige elegir una de ellas; si existe una sola se utiliza esa. El hecho principal conserva su UUID y
recibe las referencias a los reportes íntegros del origen, sin mezclar textos o revisiones. El origen
sale del conjunto de hechos computables y su UUID se resuelve al canónico mediante `hechoCanonico`.
No se crea un hecho nuevo ni se cuenta el origen como otra ocurrencia.

El registro `Duplicidad` conserva el hecho de origen completo —incluidas precisiones, aclaraciones,
relaciones y selección previas—, el principal, la atención elegida, referencias generadas, operador,
fecha y eventual corrección. Las decisiones institucionales del origen conservan su procedencia;
no se convierten automáticamente en nuevas decisiones del principal ni se resuelven diferencias
por mezclar historiales. Para precisar el hecho consolidado se usa la operación institucional existente.

El caso elegido mantiene una sola atención vigente del hecho. El otro conserva sus actuaciones,
compromisos, derivaciones y referencias a acciones inmediatas, sin copiarlas. Si conserva otros
hechos/trabajos continúa por ellos; si no, cierra técnicamente por duplicidad. Las atenciones ya
cerradas conservan su cierre y no se reabren. Los reportes y el histórico de reservas se conservan.
Consultas, unicidad y cobertura de derivación usan identidad canónica; una derivación preparada
con el ID anterior puede completarse con ese mismo ID y cubrir el hecho único, sin reescribirla.

`separarDuplicado(origenId, casoDelSeparado, directorio, cambio)` corrige con causa una relación
equivocada. Recupera la identidad del origen y las versiones actuales de sus reportes, incluidas
rectificaciones recibidas durante la relación. La historia de duplicidad se marca corregida, no
se elimina. No reabre atenciones cerradas; una fuente que seguía pendiente recupera su hecho, y
puede elegirse expresamente otra atención pendiente para continuarlo sin copiar actuaciones.
Las referencias generadas solo por el error se retiran cuando no conservan contenido compartido;
las continuidades rectificadas conservan fecha/causa y se identifican como tales en el informe,
sin presentarlas como vigentes. Las actuaciones/documentos que sí contienen información relacionada
mantienen su reserva.

No se destruye un principal vigente: si el reporte seleccionado pertenece al origen que se separa,
debe elegirse antes otro reporte válido que permanezca. Si el principal fue absorbido después por
otra duplicidad, se corrige primero esa relación posterior. Son comprobaciones referenciales,
no un procedimiento de reconciliación de contenidos o versiones.

`SeguimientoInstitucional.relacionarDuplicados` y `separarDuplicado` devuelven atención y alertas
coordinadas en la misma operación de dominio. Los avisos exclusivos del duplicado se disponen por
duplicidad; se conserva su notificación e historia. La reiteración se recalcula con hechos únicos
y la última configuración conservada en los fundamentos afectados, manteniendo todas las emisiones
anteriores. Los cambios institucionales de parámetros siguen entrando por `evaluarReiteracion` con
la configuración aprobada actual; no se inventan parámetros al identificar un duplicado.
Corregir una disposición de duplicidad equivocada conserva esa disposición en la revisión y puede
devolver la señal a pendiente desde la rectificación. Un cierre técnico anterior no la consume como
atención nueva; la corrección no reabre un caso ni inventa una intervención.

### 3. Reserva y corrección de vínculos

La fuente de la restricción y su propagación se separan. Una prevención por reporte se ancla a su
ID; una prevención por aclaración, al hecho. La exclusión manual se ancla al caso declarado o al
ámbito de hechos/reportes indicado mediante el overload de `excluirUsuario`/`PoliticaAcceso.excluir`.
Este origen debe pertenecer al contexto autorizado. Se conserva la prohibición de autoexclusión,
autoría, fecha, motivo, retiro no propio e independencia de cada fuente de restricción.

`hechosHistoricos` y `vinculos` mantienen auditoría; `hechosReservados` identifica el contenido que
todavía revela el asunto. Retirar un vínculo erróneo sin contenido compartido no arrastra el relato
retirado a las consultas futuras ni transmite permanentemente su exclusión a otros hechos. Si una
actuación del caso, derivación, convocatoria o continuidad real lo conserva, se mantiene protegido.
`contextoDeHechos` incorpora IDs de casos de reserva sin importar automáticamente todos sus otros
hechos: consultar un hecho independiente no equivale a descargar el caso que también lo mencionó.

Un informe emitido conserva las fuentes que contenía, aunque hoy se haya corregido el vínculo.
Su consulta/emisión usa `contextoDocumento`, que añade esas fuentes y sus propietarios actuales.
Cada revisión de acta conserva `ambitoIncorporacion`; consultar/descargar revalida ese ámbito.
Así puede permitirse el caso independiente y seguir denegando un informe o acta anterior que contiene
la denuncia. Una sustitución conserva los metadatos de dominio de las revisiones incorporadas,
incluso si la entrada trae solo su contenido original; no puede modificar ese prefijo conocido.

Se corrigieron tres expectativas anteriores: la prevención se ancla al reporte denunciante, el
contexto de un hecho no copia todos los hechos de la cadena de casos y un vínculo retirado sin
contenido compartido no mantiene una exclusión ajena sobre el asunto independiente. No se elimina
ninguna de esas pruebas: comprueban ahora la regla correcta y se acompañan de negativos sobre fuga.

### 4. Intervención completa del comité

El overload de `registrarIntervencionComite` recibe decisiones (`Map<UUID,String>`), actuaciones
con compromisos y cierre opcional junto con reunión, convocatoria, participantes y fecha efectiva.
Autoriza una vez, valida participantes/fechas/identidades y devuelve un solo registro nuevo. Conserva
operador real, sustitución y procedencia formal de cada decisión/actuación/cierre. No requiere guardar
primero una reunión vacía. Si falla cualquier parte, la instancia anterior permanece íntegra.
Las operaciones individuales existentes se mantienen para completar información posteriormente.

### 5. Responsabilidades y carga acotada

`CasoAtencion` decide la pertinencia temporal y material de su propia gestión. `RegistroAtenciones`
coordina identidades, vínculos, casos y reserva. `RegistroAlertas` conserva señales, fundamentos,
disposiciones y avisos; se retiró su dependencia de `RegistroAtenciones` y su antiguo método
`sincronizarAtenciones`. `SeguimientoInstitucional` es el único coordinador entre ambos estados.
El overload `actualizar(..., hechosModificados, ...)` omite asuntos independientes y comprueba
fechas únicamente de las señales y atenciones pertinentes. No entrega mensajes ni realiza I/O.

`RegistroAtenciones.cargarAmbito` recibe hechos/casos relevantes, acciones inmediatas, restricciones,
relaciones de duplicidad y un `Indice` autoritativo de reserva/unicidad (IDs). Mantiene referencias
a casos no cargados para comprobar conflictos y rechazar otra atención pendiente sin necesitar
sus actuaciones completas. Recalcula las entradas de los casos modificados y conserva las demás.
Para decidir continuidad sí exige cargar la atención que debe modificarse; para consultar un
antecedente, su contenido. `RegistroAlertas.cargarAmbito` recibe solo episodios/avisos afectados,
incluyendo la historia pertinente de esos orígenes, no todas las señales del colegio.

Estos constructores reconstituyen estados confiables; no son entradas de cliente ni autorizaciones.
La futura persistencia debe aportar un índice completo para los IDs afectados, fuente actual de
restricciones/designaciones, versión consistente, unicidad y transacción. Debe guardar los cambios
por identidad y no reemplazar datos no cargados con una colección parcial. No se introduce un
repositorio, una base de datos ni casos de uso para suplir ese contrato.

## Incremento 8A: conciliación y confirmación de matrícula

Fecha: 22 de octubre de 2026. ERS v1.2: RF-06, RF-08–11, RN-17–18 y parte de CU-01.
Es un bloque de dominio del monolito hexagonal: no incorpora aplicación, puertos, persistencia,
parser Excel, interfaz, contactos ni antecedentes contextuales. No declara completado el bloque
entero de importación ni la aceptación de CU-01. Se conservan intactas las APIs productivas anteriores.

### Tipos y operaciones

Todos los archivos nuevos están en `pe.edu.incidencias.domain.estudiante`.

| Tipo | Contrato |
|---|---|
| `FilaMatricula` | Fila seleccionada y tipada: número físico de fila, UUID estable propuesto para alta, nombre, documento de estudiante, nacimiento, sexo, grupo y situación opcionales. No recibe documento/teléfono familiar ni columnas libres. |
| `LoteMatricula` | ID, referencia de procedencia, hoja, año confirmado, fecha efectiva explícita, grupo común opcional, filas y resoluciones por fila. Copia las colecciones y rechaza fila repetida, fecha de otro año y resolución de fila ajena. |
| `LoteMatricula.Resolucion` | Elección explícita de vincular un UUID existente, crear una identidad distinta o excluir la fila. Solo `VINCULAR` admite un UUID destino. |
| `ConciliacionMatricula` | Función pura de propuesta por documento/nombre, con candidatos mínimos ordenados por UUID. Nunca escoge el primero de una ambigüedad. |
| `RegistroMatriculas` | Estado inmutable de estudiantes y confirmaciones. `cargar` reconstituye desde estado confiable; `previsualizar` produce propuesta sin cambios; `confirmar` devuelve otra instantánea completa. |
| `Previsualizacion` | Solo el registro la construye. Conserva lote, estado base, autor/fecha de revisión y propuestas internas; al operador entrega resultados por fila, campos afectados y avisos. No entrega los agregados históricos de candidatos. |
| `Confirmacion` | ID y huella del lote tipado, procedencia/hoja/año/fecha efectiva, autor/fecha y resultado de cada fila. No conserva archivo Excel, campos omitidos ni datos de candidatos descartados. |

`ResultadoFila` distingue `ALTA`, `ACTUALIZACION`, `SIN_CAMBIOS`, `REQUIERE_CONCILIACION`,
`ERROR` y `EXCLUIDA`. Incluye fila física, destino cuando es aceptable, campos cambiados y avisos.
Hoja y año pertenecen al lote visible. La confirmación incorpora únicamente filas aceptables y
conserva el recuento de las pendientes/erróneas/excluidas; no interpreta «confirmado» como éxito
de todas las filas. Las filas pendientes pueden resolverse en otro lote después de confirmar este.

### Identidad segura y contradicciones

1. Documento válido y único del estudiante permite coincidencia automática, incluso entre años,
   cuando nombre normalizado coincide y no hay nacimiento conocido contradictorio.
2. Se reutiliza la normalización existente de búsqueda (mayúsculas, espacios Unicode y tildes).
   El nombre suministrado se conserva en su forma declarada; no se divide en apellidos artificiales.
3. Nombre distinto con el mismo documento, o nacimientos conocidos diferentes, constituyen la
   política conservadora de contradicción de este bloque. No se rechaza definitivamente una
   corrección legítima: AIP puede vincular expresamente la identidad y revisar los campos a cambiar.
4. Sin coincidencia documental, el nombre exacto normalizado propone candidatos, pero nunca une
   personas automáticamente, incluso si existe uno solo o pertenece al mismo año. Si no hay ningún
   candidato se propone alta, con documento opcional y UUID suministrado desde fuera del dominio.
   La búsqueda aproximada y la selección adicional de candidatos corresponden a la aplicación futura;
   AIP puede vincular un UUID existente obtenido legítimamente por información institucional.
5. La proyección de candidato contiene únicamente UUID, nombre y documento, necesarios para esta
   conciliación AIP. No contiene vínculos anuales completos, nacimiento, sexo, contactos ni atención.
   No sustituye la proyección de búsqueda ordinaria de registradores, que no expone DNI completo.
6. Dos filas no excluidas con el mismo documento se marcan como error, aunque una sea contradictoria
   o tenga otro error. AIP debe corregirlas o excluir una y volver a revisar. No gana la primera ni
   la última del archivo. Varias propuestas dirigidas al mismo UUID o a un mismo documento final se
   rechazan todas. Un UUID de alta ya existente también se rechaza.
7. Nombres iguales dentro del lote con documento ausente en alguna fila requieren resolución expresa
   para cada fila afectada. Dos `CREAR` permiten homónimos confirmados distintos: el nombre no es
   clave única. Si ambas tienen documentos diferentes no se las une por nombre.
8. Una elección expresa no permite apropiarse de un documento que conserva otro UUID del padrón.
   Un documento duplicado preexistente debe corregirse antes de usarlo. No se intercambian documentos
   ni se fusionan identidades mediante el orden de las filas del mismo lote.

### Actualización no destructiva y matrícula histórica

La propuesta usa `incorporarIdentidad`, `vincularMatricula`, `incorporarGrupo` e
`incorporarSituacion` ya existentes. Si el año existe, no lo sustituye con una nueva matrícula.
Las omisiones conservan documento/nacimiento/sexo/grupo/situación conocidos. La ausencia en el lote
no toca al estudiante, no retira matrícula ni promueve al año siguiente.

La fecha efectiva pertenece expresamente al año seleccionado, también histórico. El grupo
informado en una fila es expreso; el grupo común es un valor por defecto que completa lo desconocido,
pero conserva el grupo ya conocido en esa fecha y lo avisa. Grupo ausente produce aviso de cobertura,
no impide identidad/año ni crea trabajo para TOE. La resolución de variantes textuales de grado/sección
y la validación contra el catálogo institucional siguen fuera de este bloque.

Un cambio posterior de grupo conserva fechas anteriores. Grupo contradictorio en la misma fecha
continúa requiriendo rectificación explícita y queda como error: no se sortea la regla reemplazando
todo el historial. La rectificación de este caso y los borrados expresos quedan pendientes. Tampoco
se modifica el grupo histórico que ya guardó una incidencia. Nacimiento posterior a fecha efectiva
de matrícula se señala como error localizable.

### Confirmación, permisos y reintentos

Tanto revisar como confirmar requieren `IMPORTAR_NOMINA` con `PoliticaAcceso` actual, limitado
al AIP vigente. La confirmación revalida el rol/cuenta, exige al mismo AIP que revisó y una fecha
no anterior a la revisión ni al estado confirmado. Una baja o fin de designación entre operaciones
impide confirmar. Otro AIP no puede recuperar el resultado histórico de ese lote. No se exige
motivo rutinario para confirmar cada cambio de matrícula.

La propuesta vincula la instancia base del registro. Si se confirmó otro lote entre revisión y
confirmación, se rechaza como obsoleta y se exige nueva revisión. Cancelar equivale a no confirmar:
la previsualización no ha alterado ningún agregado. Todas las operaciones se preparan antes de
publicar la nueva instantánea; un error de grupo no deja nombre/documento parcialmente actualizados.

Cada confirmación se identifica por ID externo estable y huella SHA-256 de los campos tipados,
incluidos filas, IDs propuestos, año, fecha efectiva, grupo común y resoluciones. La codificación
usa presencia y prefijos de longitud UTF-8; el orden del mapa de resoluciones no influye. No se
conserva el archivo original ni una copia de los datos candidatos. Mismo ID y contenido devuelve
el resultado original sin repetir altas, también tras reconstituir desde estudiantes/confirmaciones.
Mismo ID con otro contenido se rechaza. La huella sirve para detectar reutilización accidental;
no es firma, prueba de autenticidad ni control de acceso.

El adaptador debe conservar los UUID propuestos y el ID del lote durante sus reintentos. Un nuevo
lote con documentos ya seguros produce `SIN_CAMBIOS` cuando no aporta variaciones. Para filas sin
documento de otro lote vuelve a exigirse conciliación; no se deduce identidad por nombre para
simular idempotencia. No se usa reloj global, generador de UUID ni I/O externo en el dominio.

### Contrato de integración y cobertura pendiente

`RegistroMatriculas.cargar` recibe un padrón/índice **completo y confiable** para la conciliación
global de identidad/documento, más las confirmaciones necesarias para idempotencia. No admite
afirmar unicidad con una lista parcial. Rechaza UUIDs y IDs de lote duplicados en esa carga, pero
permite detectar documentos ambiguos heredados sin elegirlos automáticamente. Los accesores
`estudiantes()` y `confirmaciones()` son estado de integración/persistencia, no endpoints AIP.
`ConciliacionMatricula` es una regla pura sin autorización propia; la entrada autorizada del flujo
es el registro, y la futura aplicación debe controlar la obtención de otras identidades seleccionadas.

La aplicación futura debe obtener política/directorio actuales, comprobar concurrencia mediante
versión esperada y guardar estudiantes y confirmación juntos en una transacción. El rechazo de
propuesta obsoleta en memoria no reemplaza la transacción ni impide dos escrituras concurrentes
en una base de datos. No serializar los agregados históricos internos como respuesta al AIP.
El código conserva estado/autor/fecha/procedencia, pero todavía no garantiza guardado durable.

| ERS | Cobertura 8A | Pendiente |
|---|---|---|
| RF-06; RN-17 | Identidad estable, año explícito, omisiones, historial y grupo común no destructivo | Contactos/contexto y rectificación contradictoria en la misma fecha. |
| RF-08–09; RN-18 | Lote de campos tipados, hoja/fila, año/fecha efectiva, resultados, cobertura de grupo y conflictos de identidad | Parser `.xlsx`, asociación/reutilización de encabezados y rangos; títulos/etapas, filas de orden, celdas combinadas, fórmulas y errores de formato por celda. |
| RF-10 | Previsualización sin cambios, confirmación AIP, resultados, autor/fecha/procedencia, reintento por ID/huella, sin actualización parcial en memoria | Transacción durable, borrados expresos y actualización selectiva de contactos/contexto. |
| RF-11 | Documento seguro interanual, homónimos/contradicciones resueltos expresamente, unicidad contra padrón completo | Conciliación persistente concurrente, rectificación excepcional RF-39; no fusión universal. |
| CU-01 | 91 filas tipadas ficticias, altas sin grupo y repetición sin duplicados, más casos negativos | No se reproduce aún el libro de 121 filas/17 columnas ni se acepta el caso de uso completo. |

El constructor de `DocumentoIdentidad` sigue validando ocho dígitos de DNI. El adaptador futuro
debe convertir valores numéricos completos a texto y reportar por celda los inválidos **antes** de
crear filas tipadas, permitiendo omitir opcionales expresamente. No se rellenan dígitos perdidos.
Un nombre vacío seleccionado se presenta como error de fila; no se lo convierte en identidad.
Las filas de título/etapa deben descartarse en el adaptador: este modelo no pretende reconocerlas
a partir de texto libre ni declara que toda cadena no vacía represente a un estudiante.

### Pruebas y estado estable de entrega

Se añaden dos suites: `ConciliacionMatriculaTest` (11 pruebas) y `RegistroMatriculasTest`
(30 pruebas). Cubren normalización, documento seguro/ambiguo/contradictorio, nacimiento,
homónimos, UUID elegido/suministrado, duplicados con filas inválidas y orden inverso, exclusión,
grupo ausente/común/histórico, omisiones, continuidad anual, altas/actualizaciones/sin cambios,
errores sin efectos parciales, previsualización obsoleta, reintento después de reconstitución,
ID de lote alterado, revocación AIP, cuenta desactivada, autor ajeno, fechas y copias inmutables.
Los datos son ficticios; la prueba de 91 identidades no utiliza matrícula real.

Verificación limpia del 22-10-2026 desde la carpeta backend, Windows/PowerShell, Oracle JDK 27,
Maven Wrapper 3.9.16, compilación `release 25`, POM sin cambios:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-27'
.\mvnw.cmd -B -o clean test
```

**BUILD SUCCESS: 455 pruebas, 0 fallos, 0 errores y 0 omitidas**, incluidas las 414 anteriores.
Se compilaron desde limpio 69 archivos productivos y 31 de pruebas, 30 suites más un auxiliar.
Las pruebas no certifican persistencia, parser, rendimiento ni aceptación integral de importación.
La rama de entrega es `feature/domain-importacion-matricula`; el documento se actualiza localmente
y el README versionado resume el contrato para revisión remota.

## Entrega de dominio: importación selectiva y contexto

Se amplía la base 8A, conservando el modelo inmutable y las reglas de identidad/matrícula.
No hay infraestructura, lectura de Excel, I/O ni transacciones de base de datos en estas clases.

### Contratos y recorrido

1. La institución define `AlcanceImportacion`: versión identificada, referencia institucional y
   conceptos admitidos con clave estable, etiqueta comprensible, tipo y fundamento documentado.
   Hay conceptos dinámicos de antecedentes; los campos de contacto tienen un único concepto por tipo.
   La configuración institucional vigente guarda ese alcance. No se presume consentimiento por matrícula.
2. `AsociacionColumnas` conserva asociaciones ya revisadas de encabezados jerárquicos normalizados.
   Reubica columnas por encabezado completo; distingue Estudiante/DNI de Familiar/DNI. Un encabezado
   desconocido queda omitido, uno repetido y conocido exige revisión de ambas columnas. No se toma
   automáticamente la primera coincidencia ni se asocia P/S de hermanos con sexo. Los destinos
   reservados exigen el concepto institucional del tipo correspondiente. Un cambio de alcance exige
   revisar el perfil. El adaptador extraerá títulos/encabezados combinados y aplicará el perfil.
3. El adaptador entrega `LoteImportacion`: `LoteMatricula`, alcance y contexto exclusivamente de las
   columnas seleccionadas. `FilaContexto` distingue valores aportados y claves de borrado expreso.
   `Familiar`, `Texto` y `Antecedente` evitan intercambiar arbitrariamente contactos y antecedentes.
4. `RegistroImportacion.previsualizar(lote, configuracion, politica, autor, fecha)` comprueba alcance
   vigente, configuración no futura, año conocido y grupos admitidos. Luego usa la conciliación 8A.
   Un contexto no admitido o mal asociado excluye **la fila completa** antes de confirmar matrícula;
   no modifica identidad/grupo/documento aunque esas partes fueran válidas. Las demás filas aceptables
   pueden incorporarse. La propuesta es privada, ligada a la instantánea y al operador que revisó.
5. `confirmar(vista, configuracion, politica, cambio)` vuelve a validar configuración y permisos y
   devuelve un registro nuevo que contiene matrícula, contexto, historia y confirmaciones. El llamador
   debe persistir todo en una sola transacción con control de concurrencia; una falla no publica éxito.
   Los overloads sin configuración son de paquete para pruebas y coordinación interna, no APIs públicas.

### Contactos, desconocidos y procedencia

`ContactoFamiliar` conserva nombre, relación declarada opcional, teléfono y documento opcional.
No representa filiación probada o representación legal. El documento familiar requiere necesidad
documentada; compartirlo o compartir teléfono nunca concilia estudiantes. El teléfono del estudiante
está separado del familiar y ambos se conservan como texto sin reconstruir dígitos.

Omitir un dato no lo borra. Esto incluye componentes de un contacto: aportar solo un nombre nuevo
conserva teléfono/relación/documento existentes. Para borrar el contacto completo se solicita su
concepto expresamente. No se ha agregado un editor libre de componentes de contacto.

`ContextoEstudiante` mantiene los valores vigentes y cada `Dato` lleva lote, hoja/fila, fuente,
alcance/concepto/fundamento y autor/fecha de incorporación. Los antecedentes añaden etiqueta,
contenido declarado, calidad (`DECLARADO`/`NEGACION_DECLARADA`), fecha y período opcionales.
Una negación como «no» sigue presente; una celda ausente no crea un antecedente negativo. No se
interpreta texto como diagnóstico, responsabilidad, incidencia ni entrada de estadísticas/reiteración.

Reimportar el mismo valor no duplica antecedentes ni cambia su procedencia inicial. Sustituir un
valor produce una revisión contextual reservada con estado anterior y resultado, lote y autor/fecha.
También el borrado ordinario queda en esa historia. `consultarHistoriaContexto` permite reconstruir
los antecedentes a atención institucional autorizada; AIP no puede navegar esta historia.
La disposición jurídica efectiva debe incluir **todos los valores e historias contextuales**, no solo
la instantánea vigente. El dominio de protección planifica esa disposición; su ejecución física es externa.

### Reserva y resultados

La previsualización muestra cambios `INCORPORAR`, `SUSTITUIR`, `BORRAR`, `SIN_CAMBIOS`, concepto y
procedencia anterior. No devuelve el contenido reservado anterior. AIP puede recuperar únicamente
el resultado de sus propios lotes. Las confirmaciones no guardan archivo, celdas descartadas,
candidatos de conciliación ni valores anteriores.

`consultarContexto` y `consultarHistoriaContexto` exigen permiso institucional sobre un contexto de
atención completo que contenga al estudiante, además de no estar jurídicamente bloqueado. Un contexto
de otro estudiante no habilita una lectura arbitraria. Un ámbito debe aportar el cierre real de
relaciones de atención/exclusión, conforme al contrato general de `ContextoAcceso`.
Los accesores de estado, historias y carga son de persistencia confiable, nunca respuestas a usuario.

La política actual comprueba bloqueo jurídico por persona al conciliar, confirmar y reintentar;
también al consultar resultados y contexto. Una propuesta producida antes de un bloqueo no autoriza
guardar después. Para confirmar luego de retirada del rol se vuelve a evaluar AIP vigente.

La revisión final corrige el recuento para separar altas y actualizaciones aun si el alta trae
contexto. También distingue homónimos con documentos diferentes: una identidad bloqueada no
impide actualizar otra con documento seguro ni confirmar un alta expresamente distinta. Si hace
falta conciliación por nombre, los candidatos bloqueados se sustituyen por un aviso sin ID,
nombre ni documento. Sin documento en el candidato bloqueado no se permite eludir el bloqueo
forzando un alta; requiere resolución institucional. Se agregan tres regresiones de estas variantes
y una aserción sobre cantidades. No se ejecutó Maven en paralelo; la verificación final conjunta
debe recogerlas, por lo que la suite RegistroImportacion pasa de 25 a 28 métodos de prueba.

### Rectificaciones de matrícula

`LoteMatricula` incorpora una colección opcional de `RectificacionMatricula` por número de fila;
su constructor anterior sigue disponible. El alcance puede borrar documento, nacimiento, sexo,
situación o grupo, o sustituir expresamente un grupo contradictorio para la misma fecha efectiva.
No se puede aportar y borrar el mismo dato. No se sustituye grupo sin suministrar el nuevo.
La sustitución/borrado en una fecha no reescribe grupos posteriores ni instantáneas de incidencias.
Un grupo común del lote no repone silenciosamente un grupo borrado expresamente.

El resumen de cambios 8A muestra los campos cambiados; el coordinador contextual conserva además
la solicitud revisada en su resultado por fila. La huella del lote incorpora también esas solicitudes.
La huella contextual usa SHA-256 con longitudes y marcadores de presencia, campos tipados y
ordenación de claves, sin conservar los datos descartados en las confirmaciones.

### Verificación y límites

Suites: `RegistroImportacionTest`, `AsociacionColumnasTest`, más regresión completa de 8A y estudiante.
Ejecución verificada con JDK 27 y compilación `release 25`: **88 pruebas, 0 fallos, 0 errores,
0 omitidas**. Corresponden a 25 de RegistroImportacion, 6 de AsociacionColumnas, 30 de
RegistroMatriculas, 11 de ConciliacionMatricula y 16 de Estudiante. Comando desde backend:
`./mvnw.cmd -B -o -Dtest=RegistroImportacionTest,AsociacionColumnasTest,RegistroMatriculasTest,ConciliacionMatriculaTest,EstudianteTest test`.
Se cubren transacción pura sin filas parciales, contexto admitido, compatibilidad de asociaciones,
contactos compartidos/separados, omisiones, negaciones, procedencia, sustitución sin filtrar datos,
borrado delimitado, historia reservada, reintento/huella, obsolescencia y autorías, configuración
vigente, exclusión institucional, bloqueo posterior a previsualización y rectificación de grupos.

Esta entrega no lee `.xlsx`, no normaliza grupos textuales como «1° A» ni convierte números/fechas de
Excel, no calcula fórmulas y no detecta filas de títulos o numeración. Eso se implementará en el
adaptador Excel, entregando campos tipados y asociaciones revisadas a estas reglas. No hay controles
de sesión, base de datos, archivo temporal, persistencia, HTTP ni borrado físico. Las capacidades de
dominio existen; la aceptación integral de CU-01 exige probar ese adaptador y la transacción real.


## Entrega del dominio de consultas y estadística — 22 de octubre de 2026

Paquete `pe.edu.incidencias.domain.consulta`; no depende de Spring, persistencia, generación de archivos ni reloj del sistema. Extiende RF-16, RF-34–37 y la consistencia RNF-19. No implementa un motor de consultas de texto libre, comunicación externa automática ni adaptación PDF/CSV/XLSX.

### Contratos y carga

- `ConsultaEstadistica` declara medida, período inclusivo, agrupaciones y filtros. Las dimensiones se combinan con AND; los valores de una dimensión con OR. Las colecciones son copias inmutables. Rechaza agrupaciones duplicadas y dimensiones específicas sin pertinencia (motivo solo para señales, entidad solo derivaciones, tipo de cierre solo casos y hora solo hechos/participaciones/estudiantes).
- `MotorEstadisticas` recibe `RegistroAtenciones`, `RegistroAlertas`, `RegistroMatriculas`, IDs de **hechos** cargados y `PoliticaAcceso` actual. No recibe cifras calculadas por cliente. La aplicación debe cargar un universo completo para la consulta requerida y sus relaciones de reserva; el dominio no puede demostrar que una muestra sea toda la institución. Valida que las fuentes de casos/señales cargados estén presentes. Los IDs absorbidos por relaciones del registro se normalizan; un ID de reporte manualmente vinculado no es necesariamente un ID de hecho cargado.
- La política recibida debe ser actual, construida con directorio, restricciones y `registro.proteccion()`. Además se consulta la protección/restricción del propio registro. No se reinstancia una política vacía para autorizar lecturas. El rol se exige aun con universo vacío. Una exclusión o bloqueo no produce ni título ni cifra separada de lo omitido.

### Unidades y recuentos

Hay 23 medidas comprobadas con fixture común: hechos, comunicaciones, participaciones, estudiantes distintos y los cinco subconjuntos por papel (cotidiana, presunta, corroborada, afectados, testigos); casos pendientes/cerrados/resueltos; señales emitidas/gestionadas/pendientes; actuaciones realizadas/previstas; compromisos totales/pendientes/comprobados, seguimientos pendientes/comprobados y derivaciones efectivas.

Cada medida genera unidades procedentes de agregados y filas de contexto con variables conocidas. Los recuentos deduplican **identidad de unidad**, tanto en total como por celda. Un hecho compartido por varios grupos cuenta una sola vez institucionalmente; sus subtotales pueden no ser aditivos. Participaciones cuentan persona/hecho, estudiantes cuentan identidad persistente y excluyen familiares/personal/otras personas. Las personas presentes solo en un reporte vinculado también se incluyen. Reportes anulados no aportan hechos ni personas activas. La discrepancia de papel se identifica como `DISCREPANTE` y no se convierte en atribución elegible.

Los casos cerrados incluyen cierre técnico identificado con `TIPO_CIERRE`; `CASOS_RESUELTOS` excluye apertura errónea/duplicidad. Una base sintética permite conservar casos y actuaciones reales aunque no queden participaciones activas, sin reactivar sus estudiantes para cifras. Las acciones inmediatas cuentan sin necesidad de caso y conservan el mismo ID cuando se referencian desde uno. Los compromisos cancelados no se cuentan. Seguimientos comprobados representan revisiones con observación y estado de cumplimiento/no cumplimiento registrado; el agregado actual no conserva un booleano independiente sobre si cada revisión repitió o cambió esa conclusión.

### Tiempo, grupos y falta de datos

El criterio temporal está declarado en `Medida.criterioTemporal`: hecho para hechos/participaciones/estudiantes, registro para comunicaciones, apertura o cierre para casos, emisión para señales, fecha efectiva/prevista para actuaciones, acuerdo para compromisos y observación para comprobaciones, fecha efectiva para derivaciones. Los grupos provienen del **snapshot `Participacion.grupoHistorico`**, no se rellenan ni reescriben desde el padrón actual. Aportes contradictorios se separan como `DISCREPANTE`; falta de información como `NO_INFORMADO`. Sexo solo procede de dato explícito de la identidad al corte y no se infiere del nombre. Una fecha aproximada permite año/mes de referencia pero no día de semana ni hora exacta. Hora solo se publica si las fuentes activas coinciden y la fecha es exacta; no se deriva de fecha de registro.

Las celdas sustentan cruces, frecuencias, ordenación por frecuencia, comparación entre períodos, gráficos de evolución y mapa de calor por día. La representación gráfica pertenece al adaptador. El resultado conserva criterio, período, corte, cobertura de unidades evaluadas y unidades sin dato, diferenciados de cero. `comparar` mantiene medida/dimensiones/filtros y cambia exclusivamente el período.

### Proporciones

`Poblacion` exige año, fecha de referencia dentro de ese año, filtros opcionales de grado/sección/sexo y declaración de cobertura de la nómina (incluida parcial). `proporcionEstudiantes` deduplica estudiantes y restringe a la misma cohorte anual autorizada del denominador; no duplica una persona con varios hechos. Los filtros poblacionales del numerador no pueden contradecir la cohorte. El grupo del denominador sí se obtiene de la matrícula a la fecha de referencia explícita; el grupo del hecho conserva su snapshot.

`frecuenciaPorCien` cuenta hechos únicos que involucran miembros de esa cohorte y se rotula como hechos por cien estudiantes, no porcentaje de personas/probabilidad. Se retorna ausencia de valor y motivo cuando no hay denominador válido, junto con numerador/denominador, faltantes de grupo/sexo y referencia anual. Se redondea HALF_UP a dos decimales. No se inventa exposición individual para una supuesta tasa exacta.

### Historial y salidas

`ConsultaHistorial` consulta por identidad conocida o descripción conocida de persona, fechas de hecho/registro, clasificación, gravedad, año/grupo snapshot y estado de atención. No fusiona nombres similares ni crea padrón externo. Ordena cronológicamente, ofrece tramos de 1 a 500 y deduplica hechos/casos/actuaciones/derivaciones; incluye procedencia desde el agregado y participaciones consolidadas. Cada fuente y caso se autoriza antes de proyectar. El acceso ordinario a un aporte propio usa la proyección reducida existente de `PoliticaAcceso`.

`ResultadoEstadistico` es un corte inmutable creado por el motor, con fuentes de reserva y recursos internos para autorización; las tablas no publican dichos metadatos. `SalidaEstadistica` conserva corte y autor/fecha de emisión. Preparar salida interna exige autorización y mantiene cifras exactas, incluso uno; no aplica umbral arbitrario. La externa exige finalidad, base institucional declarada, destinatario, evaluación de riesgo resuelta y responsable/instante de revisión coherentes con emisión. Una salida identificable pendiente se rechaza; el responsable puede recalcular con agrupaciones más amplias antes de revisarla. El software no determina legitimación jurídica.

Descargar en cualquiera de los tres formatos revalida política/controles vigentes sobre salida, recursos y todas sus fuentes y casos, incluidos cierres actuales de relaciones de reserva y referencias conservadas. Si falla una fuente, se rechaza la salida completa: no se entrega silenciosamente un corte viejo parcial. Las tablas contienen solo dimensiones solicitadas y cantidad, sin relatos ni nombres/DNI ni columnas ocultas. Los valores con prefijos de fórmula se neutralizan; el adaptador debe escapar correctamente CSV/XLSX y generar los bytes. Persistir emisión/descarga y evidencia de acceso corresponde a aplicación/adaptadores y su contrato transaccional.

### Verificación

`mvnw.cmd -B -o test -Dtest=pe.edu.incidencias.domain.consulta.*Test` con JDK 27 / release 25: **50 ejecuciones, 0 fallos, 0 errores, 0 omitidas**. Incluye 23 casos parametrizados (todas las medidas), deduplicación y personas de duplicados, familiares, discrepancia, anulaciones, tres dimensiones/filtros combinados, snapshots, precisión temporal, denominador inválido, no duplicación de estudiantes, tres formatos, revisión externa, acceso propio, exclusión sobrevenida y regresión de actuación real tras anulación de la última fuente. Las pruebas son ficticias. La corrida completa del proyecto la coordina el agente principal.

El historial también localiza hechos anulados mediante revisiones históricas de la persona: devuelve `hecho.anulado() == true`, participaciones vigentes vacías y actuaciones reales conservadas. No reactiva personas ni hechos para estadísticas. `buscarAportesPropios` (RF-15) permite al autor filtrar y paginar estudiante, fechas, categoría, año/grupo y relación de atención usando únicamente sus revisiones visibles; no utiliza correcciones institucionales ajenas como canal lateral.


## Entrega local: protección, disposición y evidencia

Fecha: 22 de octubre de 2026. Fuente de implementación: ERS v1.2, RF-28, RF-38–39,
RN-24–26 y RNF-02, RNF-07, RNF-18. Este documento es local y no se publica.

### Modelo y autorizaciones

El paquete `pe.edu.incidencias.domain.proteccion` es Java puro, inmutable y sin reloj global.
No agrega un rol con lectura irrestricta ni tramita solicitudes de titulares. El colegio decide
por sus canales si procede rectificar, bloquear, disponer o extraer evidencia. La recepción
de una solicitud por sí sola no dispara ninguna de esas operaciones.

`AlcanceProteccion` identifica un ámbito de hechos/reportes/casos, personas y/o recursos
concretos. Se rechaza el alcance vacío. `contiene` exige inclusión completa de todas las
dimensiones: autorizar una persona no autoriza implícitamente todos los recursos.

`AutorizacionProteccion` es la representación de un mandato institucional comprobado por
el adaptador/canal autorizado: operador individual, operaciones expresas, alcance, vigencia,
autoridad, constancia, fundamento y fecha de otorgamiento. Su constructor público representa
esa frontera de datos confiables; no debe exponerse como autoautorización desde una petición.
`exigir` comprueba operador, operación, inclusión del alcance, fecha, vigencia y cuenta activa.
El rol administrador por sí solo no permite ejecutar estas operaciones ni consultar casos.
La validación de autenticidad/competencia de la constancia externa corresponde al canal
institucional; el dominio no inventa una comprobación jurídica electrónica.

### Bloqueo jurídico aplicado a derivados

`ControlProteccion.vacio().bloquear(...)` exige mandato para BLOQUEAR, motivo y un ID único.
`levantar` exige otra capacidad expresa, fecha consistente y motivo, conservando ambas
constancias y registros. Las instancias anteriores permanecen inmutables; retirar un bloqueo
no retira otros controles. El control rechaza consultas anteriores a sus decisiones vigentes.

`PoliticaAcceso(directorio, restricciones, control)` deniega BLOQUEO_JURIDICO en toda operación
con contexto antes de conceder permiso por rol, incluidos aportes propios. El contexto se
contrasta con el ámbito, los recursos y las personas de TODAS las revisiones de reportes;
retirar una persona de la versión actual no permite consultar su referencia histórica bloqueada.
Las exclusiones por conflicto y vigencias de roles siguen aplicándose independientemente.

La política aporta `puedeConsultarPersona` / `exigirPersonaDisponible` para búsqueda,
importación y denominadores, y `exigirRecurso` para archivos/salidas almacenadas. Esos filtros
jurídicos no conceden permisos funcionales. `evaluarPersona` valida además rol/cuenta en
consultas de estudiante, contexto y universo estadístico, sin inventar un caso o hecho para
autorizar una población; los elementos de atención siempre requieren su contexto concreto.

`RegistroAtenciones.iniciar` y `cargarAmbito` tienen sobrecargas con el control; `conProteccion`
permite incorporar su instantánea vigente al reconstruir datos. Las transiciones, copias,
duplicidades y separaciones conservan el control. `proteccion()` amplía su cierre mediante
`conRelaciones`: IDs de actuaciones, compromisos de todas sus revisiones, derivaciones y
acciones inmediatas, y personas referidas en ellas, se relacionan con su ámbito. Un bloqueo
de un recurso embebido protege el conjunto que lo revela. `casoDisponible` permite filtrar
sin exponer contenido; no concede autorización. Las listas de actuaciones aceptan también
una política actual aportada por consultas y comprueban el control propio del registro.

La cadena de informes, actas, bandeja y seguimiento utiliza el control vigente de atenciones.
Consultar un informe emitido vuelve a comprobar ámbito actual y fuentes de su instantánea,
además de su propio ID. Las actas comprueban ID y archivos de todas sus versiones; un enlace
directo al archivo no evita el bloqueo. `ReferenciasOficiales` incorpora sobrecargas que
reciben la política actual y comprueban el recurso, usadas por el generador de informes.

Compatibilidad: los constructores/sobrecargas antiguos inicializan un control vacío. Son
útiles para datos iniciales sin decisiones, pero NO sustituyen la obligación de cargar el
control actual antes de exponer operaciones. El dominio conserva getters internos para
persistencia y composición; serializar agregados/getters internos como respuestas eludiría
la arquitectura. Los adaptadores deben usar las APIs protegidas, no esos getters.

### Disposición comprobable, diferente de un flag

`DisposicionDatos` separa una decisión expresa, la transformación preparada y la confirmación
de una relectura operativa. SUPRESION y ANONIMIZACION no son una anulación por error ni
una transición automática por cierre/año. El plan requiere:

- ID, decisión, alcance, causa/registro y constancia institucional.
- Manifiesto completo y único de recursos, con alcance de recursos exactamente coincidente.
- Revisión de inventario que incluya versiones, informes/actas y copias operativas pertinentes.
- Tratamiento de respaldos: aislamiento, retención fundada y aplicación de decisiones antes
  de volver a exponer datos restaurados. No se exige modificar cada respaldo histórico.
- Para anonimización, constancia expresa de la verificación institucional del resultado.

Los tipos de recurso incluyen identidad, contexto, reportes, actuaciones, compromisos, casos,
informes, actas, copias operativas, índices y exportaciones. Las revisiones de contexto/matrícula
y sus resultados de importación también deben inventariarse si contienen datos alcanzados.
No se presupone que el original sea la única copia ni se retiene automáticamente el Excel.

Cada cambio declara eliminar el recurso completo, retirar campos o sustituirlos por valores
revisados explícitos. No existe borrado textual por conjetura ni motor de anonimización.
Los campos necesarios de terceros se identifican y NO pueden ser objetivos de disposición;
un recurso completo con datos necesarios de terceros no puede eliminarse. La transformación
conserva exactamente los campos no seleccionados. Los relatos compartidos requieren una
versión sustitutiva institucionalmente revisada; no se pierden automáticamente.

`preparar` exige mandato DISPONER, representaciones de TODO el inventario, tipos/campos
consistentes y ausencia de retención vigente. Produce una `Preparacion` con constructor
privado que solo esta operación puede emitir, y resultados esperados sin valores retirados.
No declara ejecución ni modifica repositorios. Las retenciones son decisiones expresas por
recurso, con fundamento/constancia y término opcional, sin plazos escolares inventados.

El adaptador aplica el resultado al almacenamiento real, metadatos, archivos y copias
operativas identificadas, en una operación recuperable, y relee el manifiesto completo.
`confirmar` exige autorización vigente, fecha posterior al plan, ausencia de nuevas
retenciones y coincidencia EXACTA entre relectura y resultado esperado: no acepta recursos
faltantes, duplicados, copias que sigan existiendo, datos retirados presentes ni pérdida de
campos ajenos. Solo entonces emite `Ejecucion` con IDs, registro y constancias, sin duplicar
contenido personal. Si aplicar/releer falla, no hay ejecución exitosa.

Este contrato transforma una representación completa aportada por el adaptador; no descubre
almacenes desconocidos, analiza bytes binarios ni prueba por sí solo que un operador haya
aportado realmente la relectura. Esas garantías necesitan repositorios/storage y su prueba
de integración. La frontera debe construir las representaciones y comprobaciones desde
datos reales, nunca reenviar como evidencia un resultado esperado recibido de una petición.
No se afirma aquí que ya exista borrado físico de una base de datos o de PDF/DOCX.

### Evidencia mínima y extracción restringida

`RegistroEvidencia` conserva `Evento` con usuario, fecha, operación, resultado, ámbito,
criterios tipados e ID relacionado. Admite sesiones, consulta, creación, modificación,
anulación, eliminación, importación, exportación, extracción de evidencia y recuperación.
No acepta un campo de relato, adjuntos, contraseña o descripción libre de la consulta.
Los criterios conservan intervalo, año y referencias seleccionadas sin copiar filas/relatos.

`registrar` devuelve un nuevo registro disponible inmediatamente; mismo ID/contenido es
idempotente, ID reutilizado con contenido distinto se rechaza. `cargar` reconstituye una
instantánea validando unicidad y orden cronológico. La evidencia no debe quedar sin ámbito:
una sesión puede referirse al recurso/cuenta de la operación; una operación de caso debe
conservar sus referencias efectivas, también al exportar o ejecutar disposición.

`extraer` exige autorización EXTRAER_EVIDENCIA, vigencia/cuenta activa y alcance delimitado.
Filtra por período, usuario, operación y alcance, vuelve a aplicar exclusión por conflicto
y control jurídico, y devuelve evidencia junto con el registro actualizado de su extracción.
La autorización de soporte no concede acceso a relatos, actas o expediente; estos no forman
parte del modelo de evento. La aplicación persiste el evento de extracción antes de entregar.

`conservacionMinimaHasta` calcula dos años calendario UTC por evento, exclusivamente para
trazabilidad. `retirarVencidos` requiere selección explícita, mandato DISPONER que incluya
los IDs de evidencia, motivo y cumplimiento del mínimo de CADA evento; conserva un nuevo
evento de eliminación. No hay purga automática, conservación perpetua predeterminada ni
plazo universal de dos años para expedientes.

La evidencia continua de cada caso de uso no se fabrica automáticamente invocando getters
puros. La capa de aplicación deberá persistir resultado de negocio y evidencia en la misma
unidad recuperable (o mecanismo equivalente), incluyendo rechazos/fallos de operación sin
relatos. Un fallo no puede responder éxito dejando un cambio sin evidencia recuperable.
El dominio aporta el contrato y sus invariantes; persistencia, autenticación de sesiones,
protección contra alteración del almacenamiento y recuperación se verifican al integrar.

### Procedimiento operativo mínimo para la siguiente capa

1. Comprobar la decisión/constancia institucional y autenticar al operador específicamente
   habilitado; cargar permisos, exclusiones, controles y retenciones actuales.
2. Localizar datos por APIs reservadas; preparar solo la información procedente de la
   persona, protegiendo terceros. Seleccionar contactos/contexto para informes mediante
   `SeleccionContextoInforme`; la institución revisa y comunica fuera de este núcleo.
3. Rectificar por importación/corrección autorizada. Si se decidió bloqueo, incorporarlo
   antes de exponer lecturas y derivados. No imponer bloqueos por cualquier solicitud.
4. Para disposición, inventariar datos y copias operativas, identificar campos necesarios
   de terceros y retenciones, preparar el plan, ejecutar en storage, releer y confirmar.
5. Conservar evidencia recuperable; registrar destinatarios/constancias pertinentes para
   que la institución gestione las comunicaciones de rectificación que correspondan.
6. Mantener respaldos antiguos aislados hasta su vencimiento; antes de restaurar acceso
   ordinario, reponer designaciones/restricciones y todas las decisiones posteriores válidas.

### Verificación

Las suites propias son `ProteccionDatosTest` y `BloqueoJuridicoIntegracionTest`. Cubren:
autorizaciones delimitadas, cuenta/vigencia, no autoatribución funcional, bloqueo explícito,
levantamiento, decisiones futuras, recursos y versiones, transformación selectiva sin pérdida
de terceros, copia operativa persistente, inventario incompleto, retenciones, comprobación
de resultado, evidencia idempotente, extracción trazada/restringida, reconstitución y retiro
autorizado tras el mínimo. La integración prueba todos los permisos con ámbito, revocación
de informe y descarga directa de acta ya existentes, revisión histórica, propagación de
controles en transiciones y bloqueo de compromiso embebido también en bandeja.

Comando desde backend (PowerShell):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-27'
.\mvnw.cmd -B -o '-Dtest=ProteccionDatosTest,BloqueoJuridicoIntegracionTest' test
```

La primera ejecución verificó 49 pruebas sin fallos ni errores. Después se añadieron
reconstitución/retiro de evidencia y recurso anidado, y se retiraron cinco ejecuciones
parametrizadas sin ámbito que no ejercían el bloqueo. El resultado definitivo se integra
en la verificación conjunta del documento principal; no se presupone por compilar.

### Complemento RF-05 y comprobación del contenido candidato

`PersonaInvolucrada` admite contacto textual y documento opcionales para personal, familiares
y externos, mediante constructor de siete parámetros. El constructor anterior de cinco
parámetros conserva compatibilidad y no inventa datos, cuenta ni representación legal.
Los datos escolares del estudiante siguen en su agregado: no se duplican documento/contacto
en cada referencia de participación. La validación de `DocumentoIdentidad` conserva ceros.

La consolidación de una misma persona recupera un contacto/documento solo cuando existe un
único valor conocido entre sus fuentes. Si discrepan no escoge el primero: los aportes
conservan cada valor y `datosPersonalesDiscrepantes` señala la diferencia, sin convertirla
en discrepancia de responsabilidad para reiteración. El informe interno conserva esos
datos en cada fuente y en la persona consolidada cuando son inequívocos.

El informe externo no copia automáticamente contactos/documentos de personas externas,
incluso si la persona está admitida. `RevisionSalida` agrega selecciones expresas por ID
(`contactosAdmitidos`, `documentosExternosAdmitidos`) y exige que esos IDs estén admitidos
como personas. El constructor anterior inicializa ambas selecciones vacías. Si un relato
repite un contacto/documento no seleccionado, exige adecuarlo antes de salir. Estas reglas
no modifican la regla previa de iniciales/DNI del estudiante para violencia externa.
`PersonaExternaDatosTest` añade cuatro pruebas de mínimos, duplicados, discrepancias y
selección interna/externa sin fuga por las fuentes del informe.

Las actualizaciones de hechos, casos y acciones inmediatas comprueban también el control
del estado candidato antes de devolverlo. Autorizar solamente el estado anterior habría
permitido introducir una persona/recurso ya bloqueado en un ámbito antes disponible. La
regresión prueba el rechazo del hecho y del compromiso nuevos, conservando el registro previo.

Conteo propio esperado tras estos complementos: 52 ejecuciones (21 protección, 27 integración
de bloqueo y cuatro RF-05). Pendiente la ejecución limpia conjunta del agente principal.


## Configuración institucional y selección contextual para informes: continuación final

Fecha: 22 de octubre de 2026. Este bloque completa reglas de RF-02–03, RF-31 y RF-40
que faltaban al cerrar 8A. No introduce una aplicación, puertos o adaptadores.

### Configuración versionada

`ConfiguracionInstitucional` es un agregado inmutable de una instalación. `Estado` conserva
datos institucionales, aviso, alcance de importación, categorías versionadas, ventana común,
años/grupos y año de trabajo opcional. `Revision` conserva el estado completo, operación,
autor/fecha e ID de delegación cuando corresponde. No se fijan colegio, año ni ventana de ejemplo.
Los accesores completos son para persistencia confiable; la aplicación no debe serializarlos a
roles sin la capacidad correspondiente.

`DefinicionInstitucional` expresa valores decididos, no columnas detectadas en una fuente.
TOE/director con `DEFINIR_CONFIGURACION` pueden crear o definir directamente. Las categorías
incluidas se crean/actualizan/activan/desactivan con sus APIs anteriores; las omitidas permanecen,
sin perder historia ni reclasificar hechos. La ventana puede retirarse expresamente y se conserva
su historia. Aviso y alcance opcionales representan ausencia explícita; sin alcance admitido
no se habilita la importación contextual pública. No hay sanciones ni reglas arbitrarias.

La carga delegada es alternativa al mantenimiento directo, no una aprobación electrónica obligatoria
por cambio. `delegar` requiere TOE/director y un administrador operativo vigente como cargador.
La delegación congela exactamente `DefinicionInstitucional`, decisor y fecha. `cargarDelegacion`
revalida al administrador asignado, aplica esos valores y conserva separadamente decisor y cargador.
No concede capacidad de cambiar otros valores ni lectura de atención. Una definición/carga posterior
invalida la delegación pendiente anterior, incluso con el mismo instante (se utiliza el orden de
revisiones). Crear un año/grupo no invalida por sí solo la decisión institucional. Repetir una carga
ya aplicada devuelve el estado actual sin volver a pisar valores posteriores.

`registrarAnio`, `definirGrupos` y `seleccionarAnioTrabajo` requieren administración operativa;
ser TOE/director por sí solo no confiere esa capacidad. Admiten años históricos sin un flujo de
cierre/desbloqueo. Año desconocido o grupo equivalente repetido se rechazan; la comparación de
grupos tolera tildes/mayúsculas/espacios según la normalización existente. Un grupo no informado
es admisible; uno suministrado debe pertenecer al catálogo del año. Estas operaciones no tocan
identidades, matrículas, incidencias, pendientes ni cierres y no infieren promoción o retiro.

`AvisoPrivacidad` reúne banco, responsable/domicilio/contacto, encargados aplicables, finalidades,
destinatarios/transferencias, obligatoriedad/consecuencias, conservación y canales de derechos.
No permite publicar un aviso incompleto como valor válido. El contenido es la declaración
institucional: no comprueba legitimación, consentimiento ni entrega individual del aviso.

### Contexto pertinente sin transcripción

`SeleccionContextoInforme.recuperar` consulta `RegistroImportacion` con política actual, persona
y contexto real de atención. Solo recupera campos expresamente seleccionados: nombre/relación/
teléfono/documento familiar por separado, teléfono del estudiante, domicilio y claves concretas
de antecedentes. No copia la ficha completa ni infiere representación familiar. Los campos ausentes
seleccionados se expresan como no informados; antecedente inexistente se rechaza. Se conserva calidad
declarada/negación, fecha/período conocidos y claves estables por persona/concepto.

El resultado inmutable se utiliza como `contextoInterno` del generador existente. Se reconstruye
el contexto al corte con la historia reservada; un borrado/sustitución posterior no oculta ni
reescribe un dato que existía en ese corte. El corte no puede ser futuro ni atribuir una
incorporación posterior a una fecha anterior cuando no hay una versión histórica válida. La revisión de texto y
selección externa de `RevisionSalida` sigue siendo obligatoria para terceros; recuperar un campo
institucional no autoriza automáticamente comunicarlo fuera. El generador mantiene sus reglas de
iniciales/DNI para violencia y revalidación de documentos ya emitidos.

### Pruebas y contratos

`ConfiguracionInstitucionalTest` cubre separación de roles, valores ausentes, años históricos,
grupos conocidos/no informados, duplicidad normalizada, delegación exacta, autorías, reintento,
baja del cargador, delegación obsoleta en el mismo instante, versiones de catálogo/ventana,
aviso mínimo, inmutabilidad y fechas. `SeleccionContextoInformeTest` cubre selección mínima,
ausencias sin inferencia, cortes, antecedentes inexistentes y denegación al administrador o
a una persona ajena al ámbito. La verificación conjunta final se detalla al final de este documento.

Los registros de aplicación deben conservar configuración/directorio/control actuales y usar
versión esperada al guardar. La evidencia técnica continua, mostrar el aviso, interfaces de
mantenimiento y la publicación de versiones son integración de aplicación/adaptadores.

## Matriz para revisión de requisitos (base 1–7; ampliar con las continuaciones)

«Parcial» significa que se implementaron invariantes de dominio, no la aceptación completa del RF/CU.

| ERS | Implementación y evidencia | Alcance pendiente |
|---|---|---|
| RF-01; RN-06, RN-26 | Cuentas, vigencias, altas/bajas y prohibición de autoatribución; `DirectorioUsuariosTest` | Autenticación, recuperación y mantenimiento persistente. |
| RF-02 | `DatosInstitucion` recuperable en instantáneas de informe | Mantenimiento autorizado de institución/años/grupos; el valor no completa el RF. |
| RF-03; RN-03, RN-09 | `Categoria`, parámetros y permiso de definición TOE/director; `CatalogoTest`, `PoliticaAccesoTest` | Carga delegada y persistencia de versiones; el incremento 6 ya conserva fundamentos de señales. |
| RF-04, RF-06, RF-11; RN-17–18 | Identidad/matrícula/grupos y búsqueda pura; `EstudianteTest` | Conciliación global, importación, rectificación contradictoria, contactos y contexto. |
| RF-05, RF-12–13; RN-02, RN-04–05 | Personas mínimas, roles individuales y capacidad de registro por rol; `ReporteIncidenciaTest`, `PoliticaAccesoTest` | Resolución contra identidades persistidas e integración de permisos. |
| RF-15; RN-07 | Proyección propia sin aportes/revisiones institucionales ajenas; `PoliticaAccesoTest` | Consulta persistente y vistas de usuario. |
| RF-14; RN-05 | Acción inmediata con compromiso sin caso y referencia sin copia al atender; `RegistroAtencionesTest` | Guardado transaccional conjunto con comunicación e interfaz. |
| RF-22–23; RN-12 | Caso compartido, unicidad, corrección, antecedentes y continuidad; duplicados atendidos con elección explícita; `RegistroAtencionesTest`, `ContinuidadAtencionTest`, `CorreccionesIncrementosTest` | Persistencia de contenido/índices actuales y unicidad transaccional. |
| RF-25–26; RN-13 | Realización/programación, acuerdos y seguimiento explícito; `ActuacionTest`, `CompromisoTest` | Presentación y recuperación persistente. |
| RF-29–30; RN-15–16 | Derivación preparada/efectuada, cobertura familiar completa, cierre con pendientes y autoría formal; `DerivacionTest`, `CierreLocalTest`, `DerivacionYCierreCasoTest`, `ComiteAtencionTest` | Envíos externos institucionales y almacenamiento transaccional. |
| RF-17 | Revisiones, aclaración propia, precisiones del hecho y anulación de cada fuente; `ReporteIncidenciaTest`, `IntegridadHechoTest`, `CorreccionesHechoTest` | Trazabilidad persistente y disposición jurídica de datos. |
| RF-18; RN-01, RN-10 | Duplicados de hechos atendidos y separación motivada, origen/canónico trazables, continuidad y avisos sin copia; `HechoTest`, `CorreccionesHechoTest`, `CorreccionesIncrementosTest` | Guardado atómico de fuentes, relaciones, índices, casos y avisos. |
| RF-19; RN-03, RN-08 | Señal inmediata, destinatarios elegibles y falta de designación sin divulgar contenido; `RegistroAlertasTest`, `ConflictosPreventivosTest` | Integración transaccional al guardar el reporte y entrega interna por interfaz. |
| RF-20; RN-04, RN-09–10 | Contador y fundamento histórico por ventana/categoría/carácter, reevaluaciones y episodios por hechos nuevos; `CalculadoraReiteracionTest`, `RegistroAlertasTest` | Persistencia durable y activación desde entradas futuras. |
| RF-21; RN-11 | Lectura separada, disposición, bandeja de trabajo real y coordinación con atención; `BandejaAtencionTest`, `SeguimientoInstitucionalTest` | Guardado transaccional de ambos estados y presentación/ordenación interactiva. |
| RF-24 | Aclaraciones con originales intactos y permiso institucional; `HechoTest`, `PoliticaAccesoTest` | Integración del permiso y vínculo con actuaciones reales. |
| RF-27; RN-06, RN-14 | Convocatoria, composición, intervención/decisiones/actuación/cierre en una operación y sustitución; `ComiteAtencionTest`, `CorreccionesIncrementosTest` | Interfaz y persistencia; no firma ni aprobación electrónica. |
| RF-28; RN-08, RN-23 | Restricción anclada a origen, propagación según contenido, corrección sin contagio y reserva de documentos anteriores; suites de acceso, `CorreccionesIncrementosTest`, `ReservaDocumentosIntegracionTest` | Índices/restricciones actuales desde persistencia, sesiones y entrega efectiva de bytes. |
| RF-31; RN-20, RN-23 | Consolidado, emisión estable y revisión completa con reutilización segura; iniciales/DNI específicas para violencia externa; `InformeCasoTest`, `InformeExternoTest`, `ReservaDocumentosIntegracionTest` | PDF, archivo durable y ficha contextual completa. |
| RF-32; RN-20 | Actas PDF/DOCX hasta 10 MB, sustitución motivada y consulta/descarga autorizada; `ActasTest` | Inspección real de contenido, almacenamiento y descarga de archivos. |
| RF-33; RN-21 | Referencias incompletas/completables y protocolo histórico, sin nuevos hechos/cierre; `ReferenciasOficialesTest` | Entrada/persistencia y medios externos institucionales. |
| RNF-02 | Matriz explícita, roles acumulables y revalidación por operación; `PoliticaAccesoTest` | Aplicación uniforme en futuras entradas/adaptadores. |
| RNF-08 | Fechas Perú, participaciones no duplicadas, copias inmutables | Integridad referencial global, transacciones, versión esperada y reintentos. |
| CU-01–05 | Escenarios de dominio derivados, incluidos negativos y bordes | Ningún CU se declara aceptado de extremo a extremo. |

RF-15–16 (consulta persistente completa) y RF-34–41 continúan pendientes,
junto con importación/contexto/configuración y obligaciones transversales de seguridad. Los modelos
y pruebas de RF-14, RF-19–33 no equivalen a aceptación completa de esos requisitos en una aplicación.

## Contratos que faltan para integrar el monolito

1. Autorizar antes de invocar operaciones institucionales y antes de entregar cualquier agregado.
   Un UUID de autor registra autoría; **no comprueba permisos**. Usar la política del incremento 2
   con directorio y restricciones actuales; el bloqueo jurídico todavía está pendiente.
   El acceso al agregado no equivale a una vista permitida al reportante. Para consulta propia
   usar su proyección limitada, no serializar `Hecho` ni `ReporteIncidencia` completos.
2. Aplicar la exclusión por conflicto al conjunto de fuentes antes de avisar o consultar. Debe
   revisarse todo `aportes`, incluida la cuenta de personal identificada solo en un duplicado.
3. Resolver estudiantes y personal contra identidades existentes; validar el grupo histórico
   conocido y obtener la clasificación de una categoría activa. El record de clasificación por sí
   solo no prueba que el catálogo estuviera activo ni que el operador pudiera seleccionarlo.
4. Mantener un solo ámbito institucional de ventana común. La unicidad global de documentos,
   de relación reporte→hecho y de atención pendiente requiere coordinación entre agregados.
5. Persistir reportes como fuentes independientes, relaciones y revisiones, incluidos reportes
   desvinculados/anulados, precisiones institucionales y cambios de principal. No borrar un reporte
   porque ya no aparezca en la relación actual. Conservar el ID estable del hecho y seleccionar
   datos vigentes mediante las consultas del hecho, no leyendo `principal().datos()`.
6. Guardar las nuevas instancias con versión esperada y transacción. Un llamador no puede reutilizar
   una instantánea antigua sin detectar concurrencia. Los IDs los suministrará la operación
   idempotente; `UUID.randomUUID()` solo se utiliza en pruebas, no dentro del dominio productivo.
7. La fecha de evaluación debe venir de un reloj de aplicación convertido a Perú. Las fechas
   de registro/cambio deben venir de una fuente confiable; el dominio no lee el reloj global.
8. Un fundamento calculado no debe recalcularse para reconstruir una señal histórica: conservar
   el valor emitido y enlazar las reevaluaciones sin modificar aquella evidencia.
9. Persistir movimientos de cuentas/designaciones y restricciones, incluidos los retiros.
   No reconstruir autorizaciones con versiones antiguas ni usar un contexto que omita relaciones.
10. Ante cambios de clasificación, atribución, fuentes, anulación o parámetros, reevaluar los
    fundamentos pertinentes con las fuentes actuales. Al guardar atención, conservar el resultado
    conjunto de `SeguimientoInstitucional`; sus métodos no escriben una base de datos ni se activan
    mediante un reloj/planificador oculto. Consultar documentos con un generador construido desde
    registros actuales; un objeto emitido previamente no concede autorización futura.

## Verificación reproducible de la base 1–7

Entorno verificado: Windows/PowerShell, Microsoft OpenJDK 25.0.4.1, Maven Wrapper 3.9.16,
compilación `release 25`, JUnit Jupiter 5.11.4 y Surefire 3.5.4 existentes en el proyecto.

Comando desde la raíz, con `JAVA_HOME` apuntando a un JDK 25:

```powershell
.\mvnw.cmd -B -o clean test
```

La última verificación se hizo desde una compilación limpia para descartar clases o informes
generados anteriormente. Se ejecutó sin red, usando dependencias ya presentes. En otra máquina sin caché, quitar `-o`
para permitir que Maven resuelva las dependencias. No se modificó el wrapper ni el POM.

| Suite | Pruebas ejecutadas |
|---|---:|
| `CatalogoTest` | 20 |
| `EstudianteTest` | 16 |
| `ReporteIncidenciaTest` | 18 |
| `HechoTest` | 13 |
| `IntegridadHechoTest` | 8 |
| `CalculadoraReiteracionTest` | 20 |
| `CorreccionesHechoTest` | 14 |
| `DirectorioUsuariosTest` | 32 |
| `RestriccionesAmbitoTest` | 15 |
| `PoliticaAccesoTest` | 33 |
| `ConflictosPreventivosTest` | 16 |
| `ActuacionTest` | 15 |
| `CompromisoTest` | 16 |
| `DerivacionTest` | 14 |
| `CierreLocalTest` | 9 |
| `RegistroAtencionesTest` | 21 |
| `DerivacionYCierreCasoTest` | 13 |
| `ComiteAtencionTest` | 18 |
| `ContinuidadAtencionTest` | 9 |
| `RegistroAlertasTest` | 23 |
| `BandejaAtencionTest` | 12 |
| `ActasTest` | 7 |
| `ReferenciasOficialesTest` | 4 |
| `InformeCasoTest` | 10 |
| `InformeExternoTest` | 12 |
| `SeguimientoInstitucionalTest` | 3 |
| `ReservaDocumentosIntegracionTest` | 3 |
| `CorreccionesIncrementosTest` | 20 |
| **Total** | **414** |

Resultado final: **BUILD SUCCESS; 414 pruebas, 0 fallos, 0 errores y 0 omitidas**.
El bloque anterior conservaba 109 pruebas; el incremento 2 añade 96 ejecuciones sin retirar las anteriores.
Los incrementos 3 y 4 conservan esas 205 pruebas y añaden 88 ejecuciones de dominio.
Los incrementos 5–7 conservaron las 293 anteriores y añadieron 97: 27 de comité/continuidad, 35 de alertas/bandeja,
29 de documentos/referencias y 6 de integración. La revisión correctiva conserva las 390 ejecuciones y
añade 24: 20 regresiones integradas y 4 de salida externa. Se compilaron desde limpio los 65 archivos productivos
y 29 archivos Java de pruebas; se ejecutaron 28 suites. Comprobado además que `application` y
`adapter` permanecen vacíos y que no hay importaciones de infraestructura en el dominio.
Los informes generados por Maven están en `target/surefire-reports`; se regeneran con el comando.
Todos los ejemplos son ficticios. La revisión adicional detectó y corrigió la propagación del
marcador de atención y las asociaciones contradictorias de cuentas de personal; sus regresiones
se conservaron en `IntegridadHechoTest`.

Las correcciones posteriores añaden `CorreccionesHechoTest`: precisión trazable sin reescribir
fuentes; consultas y cómputo por fecha/categoría vigentes; fechas aproximadas/futuras y orden
temporal; avisos y solicitudes expresas; naturaleza/gravedad; conservación de atribuciones;
principal anulado con otro válido; cambio de principal sin cambiar ID ni contar dos veces;
causas, referencias válidas e historia inmutable; retirada del anterior principal; anulación
de todas las fuentes; y conservación de las decisiones al modificar las relaciones.
Se sustituyó la expectativa incorrecta de `HechoTest` que excluía el hecho completo al anular
solo su principal. Las otras pruebas del incremento se conservan y pasan.

El incremento 2 verifica la matriz por rol, acumulación de roles sin ampliar configuración,
vigencias/bajas, anti autoatribución, fuentes de exclusión independientes y retiros motivados,
aportes propios frente a ajenos, reservas de caso en filtros por contexto, sustituciones y
destinatarios. También reproduce denuncia en duplicado, identificación posterior, correcciones,
anulación y cambio de principal, aclaración que introduce una atribución, descarte posterior,
retiro que no revive al recalcular, nueva fuente/transición y contextos con decisiones futuras.
Las pruebas de acceso usan personas, cuentas, relaciones y decisiones ficticias.

Las seis suites de atención verifican además guardado único de conversación/compromiso/cierre,
unicidad por hecho, actos previstos que no cuentan realizados, continuidad explícita de acuerdos,
vínculos/antecedentes sin copia, cierre por último vínculo erróneo, acciones inmediatas por referencia,
propagación de reserva, identificación posterior de personal, restricciones del caso, nuevos hechos
sin reapertura, corrección de cierre con revalidación y derivación familiar total frente a parcial.
También prueban ejecución directa, referencia opcional, fuentes desconocidas, versiones anteriores,
IDs duplicados, causa requerida, fechas futuras/incoherentes y colecciones inmutables.

Las suites del incremento 5 verifican convocatoria expresa sin cambio de estado, composición de
designaciones, elegibilidad, sustituciones y operador real; intervención histórica documentada tarde,
cese posterior, fechas, inmutabilidad, vía formal y rechazo de revisión por método individual.
La continuidad prueba conservación de actuaciones, fuente con otros hechos o trabajo pendiente,
cierre técnico al agotarlos, reserva transitiva, unicidad y reasignación elegible sin motivo añadido.

Las del incremento 6 verifican gravedad inmediata aun con papel desconocido, fuentes discrepantes,
riesgo expreso, parámetros ausentes, reiteración y caracteres conservados, lectura separada,
recalcular sin duplicar avisos y episodios por hechos nuevos. Incluyen combinación de motivos y
gestiones anteriores, cambios de principal/duplicidad, rechazo de evidencia futura, destinatarios
TOE/sustitutos, ausencia de elegibles, exclusión posterior y bandeja con pendientes reales.

Las del incremento 7 cubren informes con fuentes/rectificaciones y principal vigente, participantes
solo del duplicado, discrepancias, comité, reasignación y continuidad sin motivo rutinario adicional;
actas PDF/DOCX en el límite y exceso, sustitución trazable, causa de sustitución del director y
versión de descarga; referencias
incompletas/completables sin nuevo hecho ni cierre. La salida externa prueba selección, base y riesgo,
revisión institucional y adecuación selectiva, reutilización de textos seguros e iniciales/DNI para
violencia externa; también identificación autorizada sin esa transformación en documentos no violentos.

`SeguimientoInstitucionalTest` recorre lectura y atención con conversación/cierre en el mismo
resultado, no gestión de un hecho ajeno y repetición sin duplicados. `ReservaDocumentosIntegracionTest`
comprueba inmutabilidad del informe emitido y revocación por exclusión explícita o por identificar
después al denunciado únicamente en un reporte duplicado.

Para revisar esta entrega, comenzar por la revisión correctiva, contrastar la matriz y los contratos
de los incrementos 1–7 y ejecutar las
suites y comprobar también sus escenarios negativos. Pasar las pruebas no certifica persistencia,
rendimiento, autenticación, descarga efectiva ni aceptación integral de la ERS.

## Cierre consolidado y siguiente capa del monolito

Las continuaciones incorporan las reglas pendientes identificadas después de 8A. El criterio de
cierre es disponer de reglas de negocio, estados inmutables, proyecciones autorizadas y pruebas
de invariantes; no que una aplicación funcional haya aceptado todos los requisitos. Los pendientes
indicados en apartados históricos 1–7/8A deben leerse con esta matriz y las continuaciones anteriores.

| ERS | Estado de dominio al cierre | Integración posterior |
|---|---|---|
| RF-01–03 | Cuentas/designaciones y política existentes; institución/aviso/alcance/catálogo/ventana versionados, carga exacta delegada y años/grupos explícitos | Sesiones/recuperación, persistencia y pantallas de mantenimiento. |
| RF-04–07 | Identidad y búsquedas mínimas, personas participantes, contactos/contexto con procedencia e historial reservado y selección pertinente para informes | Resolución persistente de identidades y entrega de vistas mínimas. |
| RF-08–11 | Conciliación, asociaciones revisadas reutilizables, configuración admitida, previsualización, confirmación conjunta, rectificación/borrado y reintento | Lectura/validación de celdas `.xlsx`, formatos numéricos/fechas, filas/encabezados combinados y transacción real. |
| RF-12–18 | Hechos/reportes/participaciones, acciones inmediatas, consulta propia filtrada, historial institucional y correcciones/duplicidad con trazabilidad | Casos de uso y repositorios; coherencia referencial, concurrencia y versión esperada. |
| RF-19–30 | Señales/reiteración, bandeja, atención/comité, exclusiones, derivación/cierre/continuidad; bloqueo jurídico incorporado a la cadena | Guardado transaccional, interfaz y notificaciones internas durables. |
| RF-31–33 | Consolidado y selección de contexto, revisión externa, emisiones/actas/referencias con permiso actual y control de recurso | Renderizado PDF, inspección/almacenamiento/descarga de bytes y comunicaciones institucionales. |
| RF-34–37 | Unidades diferenciadas, filtros/cruces, series/día/hora fiable, grupos históricos, cobertura, proporciones y exportaciones reservadas inmutables | Gráficos/tablas de interfaz y generación efectiva PDF/CSV/XLSX; aceptación con datos representativos. |
| RF-38–40 | Evidencia tipada, consulta/extracción restringida, mínimo de conservación de evidencia, mandato específico, bloqueo/disposición verificable, aviso y alcance admitido | Evidencia automática junto a cada operación, persistencia protegida, inventario exhaustivo de copias y ejecución física/relectura; gestión institucional externa de derechos. |
| RF-41; RNF operativos | Contratos de preservación/restauración de decisiones y reserva; no se modela un panel o scheduler ajeno al dominio | Herramientas de respaldo, ensayo de restauración, entrega recuperable, monitoreo y procedimientos de operación. |

### Contratos obligatorios de integración

1. Cargar versiones actuales coherentes de registros, directorio, exclusiones, control jurídico y
   configuración. Los constructores compatibles sin control jurídico representan instalaciones
   sin decisiones registradas, no una forma de ignorar controles existentes. Los overloads de reglas
   puras no constituyen por sí solos endpoints autorizados.
2. Aportar el cierre completo de relaciones/índices de reserva y documentos para los IDs afectados.
   Compartir estudiante no extiende una exclusión por conflicto a todos sus casos; un bloqueo
   jurídico expreso de persona sí tiene el alcance decidido institucionalmente. Recurso anidado
   bloqueado protege los derivados que lo revelan, incluso si cambió su versión.
3. Guardar agregado nuevo, evidencia y resultados asociados con transacción y versión esperada.
   No entregar éxito antes de que sean recuperables. La inmutabilidad en memoria y el rechazo de
   propuesta obsoleta no sustituyen controles de concurrencia de la base de datos.
4. Las APIs de `cargar` y accesores de estado completo son contratos de persistencia confiable,
   no vistas de cliente. Un UUID/mandato aportado por cliente no prueba permiso ni legitimación.
   Los mandatos de protección se obtienen de decisiones verificadas por el canal institucional.
5. Al exportar/descargar un corte ya emitido, revalidar los permisos, fuentes originales y recursos
   actuales. El renderizador no puede entregar bytes guardados por una ruta que eluda este control.
   No filtrar una parte de un corte viejo y presentarlo como el mismo documento original.
6. El adaptador de disposición enumera versiones e historias (incluido contexto previo), informes,
   actas, índices, exportaciones y copias operativas. Aplica las transformaciones delimitadas,
   preserva campos necesarios de terceros y relee todo el manifiesto. Un plan preparado no equivale
   a supresión ejecutada. Las copias de recuperación permanecen aisladas y se sujetan al procedimiento
   de retención; antes de restaurar accesos se reponen decisiones y restricciones vigentes.
7. Las fechas de operación e IDs provienen de fuentes confiables de aplicación. Las fechas de
   trabajo se interpretan en Perú; datos desconocidos no se completan por conjetura. No publicar
   relatos, documentos o datos contextuales como parte de una respuesta estadística o un log técnico.

### Punto de continuación

Implementar la capa de aplicación por casos de uso y sus puertos, seguida de adaptadores, conservando
estas reglas. No introducir reglas de negocio implícitas en controladores/parser/repositorios para
suplirlas. La siguiente validación de producto debe cubrir aceptación de los CU de extremo a extremo,
incluyendo fallos/reintentos y dos sesiones concurrentes, no solo repetir las suites unitarias.

## Verificación conjunta final — 22 de octubre de 2026

Comando desde `SistemaIncidenciasEscolaresBackend`, con Oracle JDK 27 y compilación `release 25`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-27'
.\mvnw.cmd -B -o clean test
```

Resultado real: **BUILD SUCCESS; 610 pruebas, 0 fallos, 0 errores, 0 omitidas**.
Se compilaron desde limpio 90 archivos Java productivos y 42 de pruebas (40 suites y dos auxiliares).
No se modificó el POM ni se añadieron dependencias productivas. Las 455 ejecuciones anteriores
permanecen y se incorporan 155 nuevas:

| Suite nueva | Ejecuciones |
|---|---:|
| `RegistroImportacionTest` | 28 |
| `AsociacionColumnasTest` | 6 |
| `MotorEstadisticasTest` | 16 |
| `ReconciliacionMedidasTest` | 24 |
| `HistorialYSalidaTest` | 10 |
| `ConfiguracionInstitucionalTest` | 13 |
| `SeleccionContextoInformeTest` | 6 |
| `ProteccionDatosTest` | 21 |
| `BloqueoJuridicoIntegracionTest` | 27 |
| `PersonaExternaDatosTest` | 4 |
| **Nuevas** | **155** |

La revisión cruzada corrigió antes del cierre: doble recuento de altas con contexto; homónimos
jurídicamente bloqueados que no deben revelar candidatos ni afectar identidades seguras diferentes;
grupos estadísticos tomados de matrícula en lugar de la participación histórica; omisión de actos
efectivos tras anular todas las fuentes; lectura de contexto al corte anterior a un borrado;
revalidación de recursos embebidos y del contenido candidato antes de guardar bajo bloqueo.
Los datos de prueba son ficticios. No se modificó el contenido de la ERS.

Entrega en `feature/domain-importacion-matricula`, sin fusionar a `main`. Al cerrar esta entrega
solo se versionaba el README principal; la auditoría posterior incorpora también la ERS y este
documento al repositorio. No deben confundirse los contratos puros de disposición/descarga con
ejecución física de eliminación, entrega de bytes o aceptación integral del producto.

## Auditoría integral del dominio — 22 de octubre de 2026

Fuente: ERS v1.2, sin modificar sus requisitos. Se revisaron los diez paquetes productivos y sus
pruebas, contrastando invariantes, reserva de datos, flujos ordinarios, reintentos, mantenimiento
y límites entre dominio e integración. Las 610 pruebas anteriores pasaban antes de corregir los
hallazgos; ese resultado no se tomó como prueba de que los contratos estuvieran completos.
Este apartado actualiza los contratos anteriores cuando describe una corrección de la auditoría.

### Identidad, importación y configuración

| Hallazgo | Corrección y contrato vigente | ERS |
|---|---|---|
| Un grupo inválido abortaba el lote, incluso si su fila se había excluido expresamente. | `RegistroImportacion.previsualizar` localiza errores por fila, conserva las válidas e ignora la fila excluida. `confirmar` comprueba que los grupos aceptados siguen admitidos por la configuración actual. | RF-09–10; RNF-08, RNF-12. |
| Filtrar primero las filas con contexto inválido ocultaba sus conflictos de identidad con las demás. | Se elimina el lote intermedio filtrado: `RegistroMatriculas.previsualizar` recibe los errores de la fila completa y comprueba documentos repetidos sobre el lote original. No se guarda una identidad por haber desaparecido su fila contradictoria del análisis. | RF-10–11; RN-17–18. |
| Diferencias de tildes/mayúsculas/espacios generaban contradicciones de grupo. | `GrupoEscolar.equivalenteA` comparte la comparación entre matrícula y configuración. Reincorporar el grupo equivalente en la misma fecha conserva la instancia. Las observaciones de fechas posteriores se mantienen: son necesarias para que una rectificación anterior no reescriba lo conocido después. | RF-02, RF-10–11; RNF-19. |
| Una previsualización de otra instantánea permitía reintentar el lote de otro operador. | `RegistroMatriculas.confirmar` exige también el autor de la confirmación original antes de devolver el resultado existente. | RF-10; RNF-02, RNF-08. |
| El nacimiento podía incorporarse después con fecha posterior a una matrícula ya guardada. | El agregado `Estudiante` comprueba nacimiento contra todas sus matrículas, tanto al incorporar identidad como al añadir un año histórico. El error de importación sigue localizado en su fila. | RF-06, RF-09; RNF-08. |
| Dos homónimos se presentaban únicamente con nombre y UUID. | `Coincidencia` añade referencias escolares mínimas con año, fecha y grupo conocido. Nombre/documento muestran solo la matrícula más reciente; año/grupo muestran la referencia expresamente consultada. No devuelve situación, contactos, nacimiento, contexto o historia de atención. | RF-04; RNF-05, RNF-12. |
| Un espacio de no separación podía eludir la validación de tipo DNI o admitir un nombre/grupo vacío. | Se reconocen blancos Unicode al validar identidad y grupo; el DNI sigue exigiendo ocho dígitos ASCII y conserva sus ceros. | RF-09; RN-18. |
| Reaplicar los mismos valores de catálogo/ventana inflaba las revisiones sin cambiar reglas. | `Categoria` y `ParametrosReiteracion` devuelven la instancia existente cuando no cambia su contenido. Se mantienen validaciones de autor/tiempo. | RF-03; RNF-22. |

### Reserva, documentos y evidencia

| Hallazgo | Corrección y contrato vigente | ERS |
|---|---|---|
| Un informe emitido podía conservar información de un acta/archivo/referencia bloqueados después de su corte. | `GeneradorInformeCaso.autorizarInstantanea` revalida las actas citadas y los recursos proyectados: archivos, referencias, actuaciones/compromisos, derivaciones y actos de gobierno. Se rechaza la instantánea completa si alguna fuente está bloqueada. | RF-28, RF-31–32, RF-39; RNF-02. |
| Sustituir un acta protegía solo el ámbito de su versión vigente, aunque el agregado devolvía las anteriores. | `RegistroActas.exigirConsulta` comprueba todas las revisiones, archivos y ámbitos originales conservados. La descarga individual autoriza el acta solicitada; un acta ajena bloqueada no impide descargar otra permitida. | RF-17, RF-28, RF-32; RNF-02. |
| Los nombres de adjuntos admitían controles y caracteres de rutas o flujos alternativos. | `ArchivoAdjunto` rechaza controles y caracteres de ruta, incluidos dos puntos. Mantiene formatos PDF/DOCX y límite existente. La inspección del archivo real continúa en el adaptador. | RNF-04. |
| Un nombre corto protegido, como «Ana», bloqueaba textos inocuos como «semana» en informes externos. | La detección de nombres literales respeta límites Unicode de palabra. El nombre independiente sigue requiriendo adecuación; no se presume que el software anonimice un relato por sí solo. | RF-31; RNF-05, RNF-12. |
| Los criterios de evidencia podían contener personas fuera del alcance declarado; un evento bloqueado por ID seguía en extracciones. | `RegistroEvidencia.Evento` exige que las personas de criterios pertenezcan al alcance. La extracción comprueba también el bloqueo del ID del evento. | RF-38–39; RNF-02, RNF-07. |
| Reconstituir evidencia copiaba y recorría toda la historia por cada evento. | `RegistroEvidencia.cargar` valida orden y unicidad en una sola pasada y construye una instantánea: coste lineal, sin infraestructura adicional. | RNF-16, RNF-22. |
| Una baja o cese explícito podía dejar la instalación sin administrador para recuperar sus cuentas. | `DirectorioUsuarios` rechaza esas transiciones cuando no queda otro administrador vigente en su fecha efectiva. Se designa el reemplazo antes de retirar al último. Esto no sustituye revisar vencimientos futuros. | RF-01, RF-41; RNF-17, RNF-20. |

### Hechos y atención

| Hallazgo | Corrección y contrato vigente | ERS |
|---|---|---|
| Se podían guardar actuaciones, compromisos o derivaciones con nuevas revisiones atribuidas a otro operador. | El guardado común de `RegistroAtenciones` verifica autor y tiempo de las revisiones nuevas, incluidas las anidadas. Las revisiones ya almacenadas conservan su autor original al intervenir otro responsable. | RF-25–26, RF-29, RF-38; RNF-07–08. |
| Una política externa sin las exclusiones guardadas permitía leer actuaciones restringidas. | Las sobrecargas de consulta aplican también las restricciones y protección propias del registro; una política incompleta no las retira. Las actuaciones inmediatas posteriores al corte tampoco se entregan desde un caso anterior. | RF-16, RF-28; RNF-02, RNF-08. |
| La constancia de protección urgente de un autor ordinario se rechazaba por tratarse de un hecho grave. | Se usa un único permiso `REGISTRAR_ACCION_INMEDIATA`: autor propio, cuenta vigente y ausencia de atención, conflicto o bloqueo. `registrarInmediata` permite documentar la protección ya realizada sin abrir caso; los compromisos cotidianos siguen limitados a la clasificación disciplinaria leve. | RF-12–14; RNF-10; RN-27. |
| Faltaba una consulta autorizada de las acciones inmediatas propias, sin entregar el agregado completo. | `inmediatasDelAutor` usa `CONSULTAR_APORTE_PROPIO` y devuelve `AccionInmediataPropia`, `RevisionPropia` y `CompromisoPropio` como valores anidados. Conserva lectura tras abrir atención y filtra las revisiones institucionales ajenas, incluidas las del compromiso. Conflicto y bloqueo siguen revocándola. | RF-14–15; RNF-02; RN-07. |
| Retirar un vínculo podía retirar también su reserva aunque el hecho permaneciera descrito en una conclusión. | `CasoAtencion.hechosReservados` y la detección de contenido incorporan conclusiones reales conservadas en el historial de cierre. Corregir el vínculo no libera el contenido histórico que sigue revelando la denuncia. | RF-17, RF-23, RF-28, RF-30; RNF-02. |
| Bloquear convocatoria/intervención/decisión del comité no protegía siempre su contenido embebido en el caso. | `RegistroAtenciones.proteccion` relaciona esos UUID con el ámbito completo del caso, igual que actuaciones, compromisos y derivaciones. | RF-27–28, RF-39; RNF-02. |

### Alertas, consultas y resultados

| Hallazgo | Corrección y contrato vigente | ERS |
|---|---|---|
| Bloquear el UUID de una señal o aviso no revocaba todas sus lecturas/listados. | `RegistroAlertas` comprueba recursos en detalle, gestión, reevaluación, lectura y listados. La bandeja omite fundamentos históricos de señales bloqueadas. Los accesores completos siguen reservados a persistencia confiable. | RF-21, RF-28, RF-39; RNF-02, RNF-06. |
| La carga admitía dos avisos distintos para el mismo destinatario/señal y consultas con lecturas posteriores al corte. | `cargarAmbito` exige un aviso por pareja y emisión posterior o igual a la señal. La consulta rechaza metadatos posteriores al instante solicitado. | RF-21; RNF-08, RNF-19. |
| Un grupo/hora omitidos en un reporte duplicado se confundían con contradicción de datos conocidos. | Las consultas combinan información conocida complementaria sin completar desde el padrón actual. Variantes equivalentes de grupo conservan la primera forma declarada; valores conocidos realmente distintos continúan discrepantes. | RF-16, RF-18, RF-34–35; RNF-19. |
| Las estadísticas de señales pendientes incluían motivos que ya se habían retirado. | `ALERTAS_PENDIENTES` usa motivos vigentes. Emisiones/gestiones conservan los motivos de su historia, sin reescribir la señal original. | RF-19–21, RF-34. |
| Una salida vacía por filtro/período conservaba cobertura de fuentes que no revalidaba al descargar. | `ResultadoEstadistico` conserva recursos y ámbitos de todas las unidades evaluadas, también las que solo contribuyen a cobertura. `SalidaEstadistica` revalida el corte completo. | RF-28, RF-37, RF-39; RNF-02. |
| El estado/comité de un caso compartido podía revelarse por un hecho permitido aunque otra fuente del caso estuviera excluida. | Historial y estadísticas autorizan el caso completo y aplican también las restricciones propias del registro antes de proyectarlo. No se publica un caso restringido mediante una política externa incompleta. | RF-16, RF-28, RF-35; RNF-02. |
| El historial confundía estado institucional de hechos anulados y combinaba criterios de revisiones distintas. | Los anulados usan estado real del caso y conjunción de persona, fecha y criterios sobre la misma revisión histórica, sin crear participaciones vigentes ni volver a contarlos. | RF-16–17; RNF-19. |
| Buscar persona en el historial era menos tolerante que buscar estudiante. | Se reutiliza `BusquedaEstudiantes.normalizar` para tildes, mayúsculas y espacios Unicode en búsquedas institucionales y propias. | RF-04, RF-15–16; RNF-12. |

### Simplificación y contratos de integración actualizados

Se conservaron los paquetes por concepto y las instantáneas inmutables. No se añadieron frameworks,
capas, dependencias productivas ni un motor genérico de validación. Las simplificaciones concretas
son eliminar el lote filtrado intermedio, compartir comparación/normalización, centralizar la
autorización individual de actas y cargar evidencia en una pasada. No se fusionaron conceptos
independientes solo para reducir la cantidad de archivos.

`RegistroAtenciones` calcula una vez sus relaciones de protección al construir cada instantánea,
después del índice de reserva, y las conserva en `proteccionEfectiva`. Las copias internas conservan
el control recibido y recalculan las relaciones para su nuevo estado. No se guardan decisiones de
permiso: cada operación sigue evaluando usuario, vigencia y bloqueos actuales. Esto evita reconstruir
todos los ámbitos por cada autorización de una consulta o estadística.

`ConsultaHistorial(registro, hechosCargados, politicaActual)` deja de pedir un `RegistroMatriculas`
que no utilizaba. Evita cargar datos personales innecesarios y mantiene los grupos del hecho.
`Coincidencia.referenciasEscolares` es inmutable; una persona sin matrícula devuelve la lista vacía.
Los adaptadores deberán actualizar estas firmas/proyecciones al implementarse.

El generador de informes debe recibir el `RegistroActas` actual con todas las actas citadas y sus
versiones históricas. Una fuente ausente se rechaza; no equivale a permiso para omitir su bloqueo.
Las políticas y registros se cargan coherentes con las restricciones actuales. Revalidar fuentes
en memoria no revoca copias que un usuario ya descargó legítimamente.

La ERS y este documento pasan a versionarse junto al código. Se mantiene la exclusión de datos
reales de matrícula, compilados, credenciales y configuración local. Los apartados históricos
conservan sus cifras y fechas; la comprobación posterior de esta auditoría se registra a continuación.

### Verificación y límites de la auditoría

La referencia inicial fue `mvnw.cmd -B -o clean test`: 610 ejecuciones correctas, con JDK 27 y
compilación `release 25`. Las regresiones nuevas usan datos ficticios y escenarios negativos,
incluida revocación posterior de documentos, errores parciales de lote y reintentos.

Comprobación integral posterior a todas las correcciones, desde
`SistemaIncidenciasEscolaresBackend`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-27'
.\mvnw.cmd -B -o clean test
```

Resultado real del 22-10-2026: **BUILD SUCCESS; 651 pruebas, 0 fallos, 0 errores, 0 omitidas**.
Se recompilaron desde limpio 90 archivos productivos y 45 archivos de pruebas (43 suites y dos
auxiliares). Se añadieron 41 ejecuciones respecto de la base de 610; no se añadieron dependencias
ni se modificó el POM. `git diff --check` no detectó errores de espacios.

| Bloque verificado | Ejecuciones |
|---|---:|
| Acceso, documentos y protección | 197 |
| Estudiantes, importación, catálogo e institución | 136 |
| Hechos y atención | 201 |
| Alertas y consultas | 117 |
| **Total integral** | **651** |

Las tres suites nuevas son `AuditoriaAlertasTest` (4), `AuditoriaConsultasTest` (8) y
`AuditoriaAtencionTest` (7). Las 22 ejecuciones nuevas restantes amplían suites existentes.
Se ajustaron fixtures que retiraban al único administrador o atribuían una actuación al operador
equivocado; se conservó su objetivo original de comprobar revocación y atención autorizada.

La auditoría se limita al dominio implementado. Autenticación/sesiones, transacciones durables,
concurrencia entre dos sesiones, parser Excel, inspección/almacenamiento de adjuntos, generación
de bytes, respaldo/restauración y pruebas de accesibilidad o tareas cronometradas requieren las
capas de aplicación/adaptadores e interfaz. No se declara cumplimiento de extremo a extremo ni
aptitud para datos reales por superar estas pruebas.
