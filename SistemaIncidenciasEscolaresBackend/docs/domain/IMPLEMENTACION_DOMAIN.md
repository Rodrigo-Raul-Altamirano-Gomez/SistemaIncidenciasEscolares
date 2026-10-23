# Implementación del dominio — reglas vigentes y contratos de integración

Estado consolidado al 22-10-2026. Fuente de requisitos: `ERS_Incidencias_Escolares.md`, versión 1.2.
Este documento describe el dominio implementado actualmente: responsabilidades de sus tipos,
reglas, transiciones, autorizaciones, resultados y límites. Las descripciones de cada apartado
incorporan las correcciones vigentes; no requieren aplicar notas posteriores ni reconstruir
la secuencia de desarrollo para entender el comportamiento.

## Alcance y forma de trabajo

Es una biblioteca Java del monolito hexagonal, dentro de `pe.edu.incidencias.domain`. Implementa
identidad y matrícula escolar, importación selectiva y contexto, clasificación, comunicaciones de
hechos, participaciones, reiteración, autorización, atención local, comité, alertas, documentos,
consultas/estadísticas, configuración y protección de datos. No incorpora casos de uso de
aplicación, puertos, persistencia, controladores, autenticación, interfaz ni adaptadores.

Los valores, revisiones, agregados y colecciones son inmutables. Una modificación devuelve otra
instantánea que el llamador debe conservar. Autor e instante de operación se reciben de fuentes
confiables de aplicación; las fechas escolares se interpretan en `America/Lima`. El dominio
no realiza I/O ni consulta el reloj. Los IDs de operaciones se reciben explícitamente; señales,
avisos y restricciones preventivas derivan identificadores estables de sus fuentes para deduplicar.
La inmutabilidad no sustituye transacción, versión esperada o idempotencia durable.

Se compila con `release 25`. No hay Spring, JPA, Lombok ni dependencias productivas externas.
JUnit Jupiter y Maven Surefire se utilizan únicamente para pruebas. Hay 90 archivos Java
productivos y 45 de pruebas: 43 suites y dos auxiliares. ERS, este documento y README se versionan
junto al código; datos reales de matrícula, credenciales, compilados y configuración local
permanecen excluidos.

## Organización del dominio

| Paquete | Responsabilidad |
|---|---|
| `estudiante` | Identidad, matrícula, búsquedas mínimas, conciliación, lotes, contactos y contexto. |
| `catalogo` | Categorías, clasificaciones históricas y ventana/umbrales de reiteración. |
| `incidencia` | Hechos, reportes, personas, participaciones, correcciones y duplicidad. |
| `acceso` | Cuentas, designaciones, permisos, ámbitos y exclusiones por conflicto. |
| `atencion` | Casos, actuaciones, compromisos, derivación, cierre, comité y continuidad. |
| `alerta` | Reiteración, fundamentos, señales, avisos y proyección de bandeja. |
| `documento` | Informes, revisión externa, actas, archivos y referencias oficiales. |
| `consulta` | Historial, medidas estadísticas, cobertura, proporciones y salidas tabulares. |
| `institucion` | Configuración versionada, delegación exacta, años/grupos y aviso de privacidad. |
| `proteccion` | Bloqueo jurídico, disposición delimitada y evidencia. |

Los accesores de estado completo y métodos de carga son contratos de colaboración interna y
persistencia confiable. No son respuestas autorizadas para usuarios. Cada entrada que entregue
datos debe aplicar la política vigente y usar la proyección correspondiente.


## Estudiantes, identidad y matrícula anual

Base: ERS v1.2, RF-04, RF-06, RF-09–11, RN-17–18 y CU-01. El paquete `estudiante` contiene identidad, matrícula anual, conciliación, importación selectiva y contexto. La lectura física de Excel pertenece al adaptador.

### Modelo y decisiones

El nacimiento conocido no puede ser posterior a ninguna fecha inicial de matrícula guardada. La comprobación se aplica también al incorporar el nacimiento después o añadir un año histórico. Los blancos Unicode no permiten nombres o grupos vacíos; el tipo y número documental se recortan reconociendo esos espacios para que no eludan la regla de ocho dígitos del DNI.

`GrupoEscolar.equivalenteA` compara grado/sección con la misma normalización usada por configuración y búsquedas. El grupo equivalente en la misma fecha no crea una revisión. Una observación de fecha posterior se conserva aunque repita el grupo: permite rectificar el pasado sin reescribir lo conocido después.

Paquete `pe.edu.incidencias.domain.estudiante`, Java puro, sin dependencias de infraestructura.

| Tipo | Responsabilidad y API principal |
|---|---|
| `Estudiante` | Identidad interna UUID independiente del año; `crear`, `incorporarIdentidad`, `vincularMatricula`, `incorporarGrupo`, `incorporarSituacion`, `grupoEn`. |
| `DocumentoIdentidad` | Tipo y número textuales. DNI admite exactamente ocho dígitos ASCII, preserva ceros y no completa cifras. Otros tipos conservan el identificador declarado, sin inventar formatos. |
| `GrupoEscolar` | Grado y sección opcionales individualmente; exige al menos uno informado. El grupo totalmente desconocido es `Optional.empty()`. |
| `MatriculaAnual` | Año explícito, situación informada opcional e historial ordenado de grupos por fecha efectiva. |
| `BusquedaEstudiantes` | Funciones puras por nombre, documento, año y grupo histórico; devuelve `Coincidencia` con UUID, nombre y referencias escolares mínimas inmutables. Nombre/documento muestran la matrícula más reciente; año/grupo muestran la referencia consultada. Sin matrícula, la lista es vacía. |

Las instancias y colecciones son inmutables. Las modificaciones producen una nueva instantánea y conservan el UUID; el llamador debe guardar el resultado. No se generan identificadores ni fechas internamente. El nombre completo se conserva sin separación artificial ni normalización de tildes o espacios internos. La normalización se aplica exclusivamente al comparar búsquedas y tolera mayúsculas, tildes y espacios Unicode.

Documento, nacimiento y sexo pueden quedar desconocidos. El sexo informado y la situación anual se conservan como textos declarados, sin inferirlos ni crear un catálogo institucional no definido por la ERS. No se usa el nombre como clave única. La búsqueda por documento devuelve todas las coincidencias y no decide una fusión ni escoge el primer candidato.

`incorporarIdentidad`, `incorporarGrupo` e `incorporarSituacion` interpretan `Optional.empty()` como omisión: no borran datos. El borrado deliberado y la sustitución de grupo contradictorio se expresan mediante `RectificacionMatricula` en el lote autorizado; no son el significado de omitir un campo. No existe operación que infiera retiro, promoción o nueva matrícula por ausencia en una carga. Vincular de nuevo un año con contenido equivalente es idempotente; contenido contradictorio no sustituye el historial existente de forma silenciosa.

Cada fecha efectiva de grupo pertenece al año escolar explícito. Antes de la primera fecha conocida se devuelve grupo desconocido. Una fecha inicial desconocida puede completarse y los cambios posteriores se conservan. Se pueden incorporar fechas históricas conocidas fuera de orden. Dos grupos diferentes en la misma fecha son una contradicción que se rechaza; la rectificación expresa del lote la resuelve con autorización y trazabilidad. Cambiar al grupo actual no rellena fechas anteriores con información supuesta. La incidencia debe guardar su propia instantánea del grupo conocido al registrar el hecho: el historial de matrícula no modifica automáticamente hechos guardados.

Los criterios de búsqueda de grado/sección pueden ser parciales: los componentes omitidos no filtran, y un componente desconocido no satisface un valor solicitado. Los resultados no incluyen DNI completo, sexo, nacimiento, contexto, contactos, señales ni historia de atención. Las funciones puras de `BusquedaEstudiantes` no autorizan por sí solas: el llamador debe aplicar la política del paquete `acceso`. Los registros de importación sí comprueban sus permisos en las operaciones públicas; la autenticación y la entrega de resultados por API siguen siendo integración.

### Cobertura y límites

| ERS | Regla implementada | Integración pendiente |
|---|---|---|
| RF-04 | Búsqueda por nombre/documento/año/grupo, tolerancia textual y referencias escolares para homónimos; sin DNI completo ni datos de atención. | Aplicar autorización, consultar almacenamiento e implementar selección de resultados. |
| RF-06 | Identidad estable, datos opcionales, nacimiento coherente, matrícula anual y grupos por fecha; contactos/contexto en el registro de importación. | Persistencia y vistas autorizadas. |
| RF-09–11; RN-17–18 | Conciliación segura, lotes revisados, confirmación/reintento, omisiones no destructivas y rectificación expresa. | Parser Excel y transacción concurrente sobre el padrón completo. |
| CU-01 | Invariantes con filas tipadas ficticias, incluida una muestra de 91 identidades. | Aceptación del libro físico y de la operación completa. |

### Pruebas de revisión

`EstudianteTest` comprueba identidad estable, datos desconocidos, coherencia de nacimiento, grupos históricos, observaciones posteriores, equivalencia textual, DNI y blancos Unicode, homónimos distinguibles sin datos reservados, búsquedas e inmutabilidad. `ConciliacionMatriculaTest`, `RegistroMatriculasTest` y `RegistroImportacionTest` comprueban la coordinación descrita en las secciones de importación. Los resultados actuales de todas las suites figuran en «Verificación reproducible».

## Catálogo y parámetros de reiteración

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

Reaplicar la misma definición y estado activo de una categoría, o la misma ventana común, devuelve la instancia existente sin inflar su historia. Se siguen rechazando instantes anteriores al estado vigente. Las pruebas de catálogo incluyen estas operaciones sin cambio material.

- `DefinicionCategoria` agrupa los valores editables. Nombre y descripción son obligatorios; se eliminan espacios en los extremos y se rechazan cadenas vacías. No se fijan longitudes institucionales arbitrarias.
- `Categoria.crear(id, definicion, autorId, fecha)` crea una categoría activa. `actualizar`, `desactivar` y `activar` devuelven un nuevo agregado, sin mutar el anterior. Las revisiones completas permiten reconstruir el estado y la autoría de cada cambio. Los instantes se reciben explícitamente y no pueden preceder a la revisión vigente.
- `clasificacionParaNuevoHecho()` comprueba que la categoría esté activa y entrega el record `ClasificacionCategoria(categoriaId, nombre, naturaleza, gravedad, avisoInmediato)`. Es una copia estable: cambiar o desactivar la categoría no modifica la clasificación que conserva el hecho.
- Una categoría que no admite reiteración rechaza un umbral configurado, para evitar una combinación ambigua. Una categoría que sí la admite puede carecer de umbral: es un estado institucional pendiente legítimo y visible.
- `ParametrosReiteracion.sinConfigurar(autorId, fecha)` registra la ausencia explícita de ventana. `configurarVentana` y `retirarVentana` devuelven versiones nuevas con historia. `evaluar` genera un valor inmutable con los parámetros actuales; los valores usados en una señal pueden conservarse sin quedar referidos a un objeto mutable.
- La desactivación no elimina el umbral ni impide consultar la configuración histórica. Impedir nuevos usos se aplica en la obtención de una clasificación para un hecho nuevo.
- Se usan `UUID`, `Instant`, `OptionalInt`, records y colecciones inmutables de Java. No hay dependencias de ejecución fuera del JDK ni lectura del reloj global.

### Límites que debe respetar la siguiente implementación

La aplicación futura debe mantener **una ventana común institucional**, persistir las nuevas versiones y validar autorización antes de invocar modificaciones. `PoliticaAcceso` permite definir configuración a TOE/director; `ConfiguracionInstitucional` permite delegar al administrador operativo la carga exacta de una definición, sin concederle capacidad de decidir otros valores. Pasar un `autorId` registra autoría, pero no comprueba su permiso. Tampoco puede tratarse el constructor público de `ClasificacionCategoria` como comprobación de que una categoría esté activa: la operación normal debe obtenerla desde el agregado.

El paquete `catalogo` define valores y revisiones; `CalculadoraReiteracion` calcula el fundamento y `RegistroAlertas` conserva las señales y sus parámetros emitidos. Guardar solo el ID de categoría y volver a leer su configuración no reconstruye una señal histórica. La persistencia durable sigue correspondiendo a la siguiente capa.

No se proporciona un catálogo institucional precargado, umbral de tres, plazo universal, gravedad jurídica o política real. Los valores de las pruebas son ficticios.

### Pruebas y revisión

`CatalogoTest` verifica obligatoriedad del aviso en todas las gravedades de violencia y en grave/muy grave; comportamiento leve sin aviso y por clasificar con aviso; cambio de catálogo sin reclasificación histórica; autorías y estados de revisiones; desactivación/reactivación; inmutabilidad; rechazo de umbrales y ventanas no positivos; ausencia explícita de cada parámetro; umbrales distintos con la misma ventana; categoría que no admite reiteración; cambio y retirada de ventana sin alterar versiones previas; y rechazo de revisiones anteriores en el tiempo.

El resultado de la ejecución conjunta de Maven, con las pruebas de todos los subdominios, figura en la sección «Verificación reproducible» de este documento.

## Modelo de hechos y comunicaciones

### Personas identificadas y datos externos

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

### Hechos, reportes y participaciones

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
y las vistas institucionales. Sin precisión expresa, cada consulta devuelve el valor
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
emitidas se conservan como valores históricos en el módulo de alertas; su almacenamiento durable corresponde a infraestructura futura.

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

Estas operaciones agregan trazabilidad en el mismo dominio inmutable. La política del paquete de acceso
permite decidir la autorización del operador antes de invocarlas. `RegistroAtenciones` coordina versiones
y duplicidad/separación de hechos ya atendidos; `SeguimientoInstitucional`
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
Es el **motivo de dominio**. El módulo de alertas lo utiliza para conservar señales, destinatarios,
lectura, deduplicación, gestión por atención y bandeja; no realiza entrega ni almacenamiento externo.

## Cuentas, designaciones y autorización

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

Una baja o cese explícitos no pueden dejar la instalación sin administrador operativo vigente en su fecha efectiva. Antes de retirar el último acceso debe designarse un reemplazo. Esta regla no evita el vencimiento natural futuro de designaciones; su revisión sigue siendo una responsabilidad operativa.

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

`REGISTRAR_ACCION_INMEDIATA` permite al autor de un aporte documentar una acción realizada antes de su atención, con cuenta/rol vigentes y sin conflicto ni bloqueo. No exige clasificación leve para consignar protección urgente. El registro de atención limita los compromisos cotidianos a conductas disciplinarias leves; este permiso no concede decisión formal de atención. La lectura posterior de lo propio usa `CONSULTAR_APORTE_PROPIO`, no el permiso de edición.

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
| Acción inmediata propia | Aporte propio activo y no atendido. Puede documentarse protección urgente aunque sea grave, por clasificar o discrepante; el registro admite compromisos cotidianos solo en clasificación disciplinaria leve. |
| Consulta institucional, atención, aclaración, informes, documentos y estadísticas | TOE, miembro del comité y director, con la misma amplitud institucional y sin conflicto. La membresía no transfiere recepción cotidiana automáticamente. |
| Definir configuración | TOE/director; el miembro no obtiene esta facultad por su sola membresía. El administrador operativo solo puede cargar una definición exacta delegada mediante `ConfiguracionInstitucional`. |
| Administrar cuentas | Administrador designado y vigente; no da acceso funcional a expedientes. |
| Convocar comité | TOE ordinariamente. Comité/director sustituyen solo si hay TOE vigente y todos los TOE vigentes están excluidos en el asunto, con motivo. La mera ausencia o falta de designación no se considera automáticamente conflicto. |
| Registrar actos formales del comité | Director vigente y no excluido; si no hay director elegible por exclusión o falta de vigencia, miembro del comité con motivo de sustitución. TOE sin membresía no obtiene esa facultad. |

Las decisiones por sustitución devuelven `porSustitucion` y el motivo. No registran una convocatoria,
un acuerdo o un caso: el módulo de comité guarda esos datos con autoría real y la sustitución aplicada.
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
`MotorEstadisticas` filtra las fuentes antes de calcular totales y derivados identificables.

### Ámbitos y exclusiones expresas

`AmbitoReserva.deHecho` incluye el ID estable y **todos** los reportes, incluidos anulados;
no depende del principal elegido. `ContextoAcceso` une automáticamente el ámbito suministrado
con esos IDs de todos los hechos entregados. Distingue IDs de hecho/reporte/caso, rechaza
versiones repetidas de un hecho y exige que un aporte solicitado pertenezca al contexto.
No se extiende la exclusión por compartir estudiante. Los datos de otro asunto autorizado del
mismo estudiante siguen disponibles.

La construcción del contexto **no descubre relaciones no suministradas**. `RegistroAtenciones` debe recibir y cerrar los hechos/reportes/casos que revelen la denuncia; no basta presentar un ID aislado
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

### Integración y límites de autorización

La política es de dominio, no un interceptor de todos los métodos Java. Los agregados son objetos internos: pasarles un UUID de autor no acredita autorización. Las futuras
entradas del monolito deberán cargar directorio y relaciones actuales, formar el contexto,
sincronizar restricciones, exigir el permiso y persistir las nuevas instancias con versión esperada.
Solo después podrán entregar una vista autorizada o emitir avisos. Deben conservar los retiros
y no reconstruir el directorio/restricciones desde una copia antigua para recuperar permisos.

El bloqueo jurídico se evalúa mediante `ControlProteccion`; la delegación exacta de configuración y el alcance AIP del lote están modelados en sus registros. Quedan pendientes autenticación, contraseñas/recuperación, sesiones, enlaces de descarga, evidencia persistente de accesos, resolución de relaciones en repositorios, transacciones y concurrencia. No se implementa respaldo ni se concede a TOE/AIP por su rol.
No hay aceptación integral de CU-09 ni de seguridad operativa: se prueban las decisiones de dominio.

## Atención compartida, actuaciones y compromisos

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
actuaciones. `continuarAtencion` permite continuidad explícita entre pendientes, conservando los datos en su caso de origen.

### Actuaciones y compromisos

El guardado común de `RegistroAtenciones` valida autor y fecha de todas las revisiones nuevas de actuaciones, compromisos anidados y derivaciones contra el operador y el instante confiables del guardado. No reatribuye las revisiones ya almacenadas cuando interviene otro responsable. Las comprobaciones se comparten entre operaciones ordinarias y formales.

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

`inmediatasDelAutor(reporteId, usuarioId, directorio, ahora)` exige `CONSULTAR_APORTE_PROPIO` y conserva la lectura tras iniciar atención. Devuelve `AccionInmediataPropia`, `RevisionPropia` y `CompromisoPropio`, valores anidados con colecciones inmutables: solo revisiones del propio autor, también dentro de cada compromiso. No entrega seguimiento institucional ajeno, los datos reservados del caso ni actos colectivos. Conflicto, bloqueo o datos posteriores al corte impiden la consulta.

`registrarInmediata` vincula una actuación ya realizada a un único hecho de un aporte propio habilitado por `REGISTRAR_ACCION_INMEDIATA`, antes de atención institucional; no crea un caso. Admite constancia urgente de protección en hechos graves, por clasificar o discrepantes, sin esperar diagnóstico. Los compromisos inmediatos solo se admiten para clasificación disciplinaria leve; no se añaden acuerdos formales a una constancia urgente. Al abrir atención, `actuacionesDelCaso` recupera esas mismas
instancias por sus reportes de origen. No las copia al mapa de actuaciones del caso ni les da
otro ID. Un intento de incorporarlas de nuevo como nuevas actuaciones se rechaza.
`actualizarInmediata` conserva su historia y permite seguimiento institucional cuando el autor
ordinario ya no tiene edición tras atención. Los compromisos siguen perteneciendo a una sola
actuación. Los pendientes de estas referencias también se consideran al consultar trabajo real.

### Reserva del conjunto y evolución de los hechos

Las sobrecargas de `actuacionesDelCaso` e `inmediatasDelHecho` que reciben una política externa aplican además las exclusiones y el control propios del registro. Una política incompleta no los retira. Se rechazan actuaciones inmediatas posteriores al instante consultado, aunque el caso sea anterior.

`proteccionEfectiva` se calcula una vez al construir cada instantánea, después del índice de reserva. Relaciona actuaciones, compromisos de todas sus revisiones, derivaciones, acciones inmediatas, convocatorias, intervenciones y decisiones del comité con los ámbitos y personas que revelan. Las copias conservan el control recibido y recalculan las relaciones de su nuevo estado. No se guardan decisiones de autorización: usuario, tiempo, vigencia y bloqueos se evalúan en cada operación.

`contexto(casoId)` protege el contenido que presenta el caso y sus antecedentes consultados.
`hechosReservados` distingue hechos vigentes y fuentes que permanecen en actuaciones, derivaciones,
convocatorias, conclusiones reales de cierre o continuidad real de los simples vínculos erróneos retirados. Estos últimos conservan
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
atenciones, reserva e historia sin fusionar actuaciones. Su contrato se describe en «Duplicados con atenciones existentes».

### Duplicados con atenciones existentes

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

### Reserva al corregir vínculos

La fuente de la restricción y su propagación se separan. Una prevención por reporte se ancla a su
ID; una prevención por aclaración, al hecho. La exclusión manual se ancla al caso declarado o al
ámbito de hechos/reportes indicado mediante el overload de `excluirUsuario`/`PoliticaAcceso.excluir`.
Este origen debe pertenecer al contexto autorizado. Se conserva la prohibición de autoexclusión,
autoría, fecha, motivo, retiro no propio e independencia de cada fuente de restricción.

`hechosHistoricos` y `vinculos` mantienen auditoría; `hechosReservados` identifica el contenido que
todavía revela el asunto. Retirar un vínculo erróneo sin contenido compartido no arrastra el relato
retirado a las consultas futuras ni transmite permanentemente su exclusión a otros hechos. Si una
actuación del caso, derivación, convocatoria, conclusión real de cierre o continuidad real lo conserva, se mantiene protegido.
`contextoDeHechos` incorpora IDs de casos de reserva sin importar automáticamente todos sus otros
hechos: consultar un hecho independiente no equivale a descargar el caso que también lo mencionó.

Un informe emitido conserva las fuentes que contenía, aunque hoy se haya corregido el vínculo.
Su consulta/emisión usa `contextoDocumento`, que añade esas fuentes y sus propietarios actuales.
Cada revisión de acta conserva `ambitoIncorporacion`; consultar/descargar revalida ese ámbito.
Así puede permitirse el caso independiente y seguir denegando un informe o acta anterior que contiene
la denuncia. Una sustitución conserva los metadatos de dominio de las revisiones incorporadas,
incluso si la entrada trae solo su contenido original; no puede modificar ese prefijo conocido.

### Responsabilidades y carga acotada

`CasoAtencion` decide la pertinencia temporal y material de su propia gestión. `RegistroAtenciones`
coordina identidades, vínculos, casos y reserva. `RegistroAlertas` conserva señales, fundamentos,
disposiciones y avisos; no depende de `RegistroAtenciones`. `SeguimientoInstitucional` es el único coordinador entre ambos estados.
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

## Derivaciones y cierre local

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
sale de ella y conserva su historia. `BandejaAtencion` integra esta proyección y `SeguimientoInstitucional` coordina los avisos.
El seguimiento posterior se registra sobre la misma atención sin reabrirla ni cambiar su cierre.
Completar referencias o registrar otra derivación después tampoco reemplaza el cierre original.

`corregirCierre` exige causa y conserva la conclusión anterior en `cierres`. Puede sustituirla
o retirar un cierre equivocado para dejar pendiente; en este último caso se vuelve a verificar
la unicidad de atención pendiente por hecho y se rechaza conflicto con otra atención vigente.
No existe reapertura automática ni un procedimiento administrativo obligatorio adicional.

### Límites de la coordinación de atención

La colección comprueba sus invariantes con el contenido relevante y un índice autoritativo de reserva
y atención pendiente. `cargarAmbito` permite reconstituir una parte consistente sin cargar el contenido
de todos los casos del colegio. Deben cargarse los antecedentes cuyo contenido se consulta y las
atenciones cuya continuidad se decide. El índice no sustituye transacción, versión esperada ni unicidad
persistente; no es válido omitir del índice una atención para aparentar que el hecho está libre.
No hay repositorio, autenticación, API, interfaz, persistencia durable de notificaciones ni comprobación de
envíos externos. El registro no descubre información almacenada fuera de sus entradas.
Comité, reasignación, continuidad, actas, informes y referencias comparten estos límites de infraestructura.

## Comité y continuidad

Base: RF-18, RF-22–23, RF-27–30; RN-06, RN-08, RN-12, RN-14–16; escenarios de CU-05,
CU-06, CU-08 y CU-09. Se amplían `CasoAtencion` y `RegistroAtenciones`; no se crea un expediente
separado del comité ni una capa de aplicación.

| Tipo | Responsabilidad |
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
todas las consultas del componente conectado. Las operaciones de duplicidad y separación conservan las atenciones anteriores relacionadas conforme al contrato de atención.

### Registro conjunto de la intervención

El overload de `registrarIntervencionComite` recibe decisiones (`Map<UUID,String>`), actuaciones
con compromisos y cierre opcional junto con reunión, convocatoria, participantes y fecha efectiva.
Autoriza una vez, valida participantes/fechas/identidades y devuelve un solo registro nuevo. Conserva
operador real, sustitución y procedencia formal de cada decisión/actuación/cierre. No requiere guardar
primero una reunión vacía. Si falla cualquier parte, la instancia anterior permanece íntegra.
Las operaciones individuales existentes se mantienen para completar información posteriormente.

## Alertas, notificaciones y bandeja

Base: RF-19–21, RF-22, RF-28; RN-09–11, RN-23; escenarios de CU-03–05 y CU-07.
Son estados y proyecciones de dominio, sin correo, SMS, interfaz, planificador ni repositorio.

| Tipo | Responsabilidad |
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

`cargarAmbito` exige IDs únicos y un solo aviso por pareja señal/destinatario; el aviso no puede preceder a su señal. Las consultas de notificaciones rechazan emisión o lecturas posteriores al instante solicitado.

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

El bloqueo del UUID de una alerta se comprueba al consultar, gestionar, reevaluar o listar pendientes. El bloqueo del aviso se comprueba al leerlo o entregarlo en un listado. La bandeja omite también fundamentos históricos cuyo recurso esté bloqueado. Estos controles se suman al ámbito de hechos/casos; los accesores completos siguen siendo exclusivos de colaboración interna y persistencia confiable.

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

## Informes, actas y referencias

Base: RF-02 parcial, RF-31–33, RF-28; RN-20–21, RN-23; escenarios de CU-11 y CU-12.
Paquetes `documento` e `institucion`, sin lectura/escritura de archivos, renderizado PDF o envío.

| Tipo | Responsabilidad |
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

Al emitir o consultar una instantánea, `autorizarInstantanea` revalida el ID del informe, ámbito actual y fuentes originales, todas las actas citadas con sus revisiones/archivos, referencias, actuaciones, compromisos, derivaciones y actos de gobierno proyectados. El generador debe recibir el `RegistroActas` actual completo para las actas citadas; una fuente ausente falla cerrada, incluso si el informe externo oculta el ID del archivo. Si una fuente está bloqueada se deniega el informe completo.

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
`SeleccionContextoInforme.recuperar` obtiene los campos pertinentes de `RegistroImportacion` y su historia al corte. El mapa seleccionado es la entrada al generador, no una autorización para copiar toda la ficha contextual.

La vista previa no tiene identificador de emisión. Emitir asigna el ID y conserva quién/cuándo,
institución, motivo, destinatario/finalidad y corte. Una nueva actuación no reescribe la instantánea
ya emitida. El corte describe los datos proporcionados al generador; no es una consulta temporal
arbitraria a una base de datos ni permite usar un registro con datos posteriores al corte.
Emitir no acredita firma, envío, registro oficial ni cierre de un protocolo.

`consultarEmitido` exige construir el generador con el registro/directorio actuales y vuelve a
comprobar la reserva del caso y las fuentes conservadas; devuelve la misma instantánea, sin regenerarla. Una exclusión nueva
deniega nuevas lecturas aun si el informe fue emitido o consultado antes. Los getters estructurales
de valores/agregados son internos: no deben convertirse directamente en endpoints de descarga.

### Salida a terceros

El informe externo no copia automáticamente contactos/documentos de personas externas,
incluso si la persona está admitida. `RevisionSalida` agrega selecciones expresas por ID
(`contactosAdmitidos`, `documentosExternosAdmitidos`) y exige que esos IDs estén admitidos
como personas. El constructor anterior inicializa ambas selecciones vacías. Si un relato
repite un contacto/documento no seleccionado, exige adecuarlo antes de salir. Estas reglas
no modifican la regla previa de iniciales/DNI del estudiante para violencia externa.
`PersonaExternaDatosTest` comprueba mínimos, duplicados, discrepancias y selección interna/externa sin fuga por las fuentes del informe.

La detección de un nombre literal protegido utiliza límites Unicode de letra/número/marca: «Ana» no coincide dentro de «semana», pero «ANA,» independiente sí requiere adecuación. Esta comprobación no detecta toda identificación posible ni sustituye la revisión institucional.

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

`RegistroActas.exigirConsulta` autoriza el acta individual y los ámbitos de incorporación de todas sus revisiones, además de sus archivos históricos. Una sustitución no retira la reserva de versiones anteriores. `archivoParaDescarga` comprueba exclusivamente el acta solicitada y exige su archivo vigente: otra acta bloqueada no inutiliza una descarga permitida.

Se admiten varias actas del caso. `ArchivoAdjunto` limita formato declarado a PDF/DOCX y tamaño a
10 000 000 bytes inclusive (interpretación decimal de 10 MB); rechaza archivos vacíos, exceso,
extensión/MIME discordantes, controles (incluidos NUL y CR/LF), separadores de ruta o caracteres de nombre como `:` que permitan rutas o flujos alternativos. Son metadatos: comprobar firma real del archivo,
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

### Selección y reutilización de texto externo

`RevisionSalida` sigue siendo la revisión institucional de la salida completa para finalidad,
destinatario, base declarada, selección y riesgo. Su mapa `textosRevisados` es un conjunto
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

## Conciliación y confirmación de matrícula

Base: ERS v1.2, RF-06, RF-08–11, RN-17–18 y CU-01. `RegistroMatriculas` coordina identidad y matrícula sobre filas tipadas; `RegistroImportacion` añade contexto, contactos, configuración admitida e historia a la misma confirmación. No hay parser Excel, persistencia ni interfaz.

### Tipos y operaciones

Los tipos están en `pe.edu.incidencias.domain.estudiante`.

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
se aplica mediante `GrupoEscolar.equivalenteA`; la validación contra el catálogo institucional la realiza la entrada pública de `RegistroImportacion`.

Un cambio posterior de grupo conserva fechas anteriores. Grupo contradictorio en la misma fecha
continúa requiriendo rectificación explícita y queda como error: no se sortea la regla reemplazando
todo el historial. `RectificacionMatricula` permite esa sustitución y los borrados expresos revisados. Tampoco
se modifica el grupo histórico que ya guardó una incidencia. Nacimiento posterior a fecha efectiva
de matrícula se señala como error localizable.

### Confirmación, permisos y reintentos

El reintento exige también el autor de la confirmación original cuando se recibe una previsualización procedente de otra instantánea. Usar el mismo ID y contenido no permite a otro operador recuperar el resultado.

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

| ERS | Cobertura de dominio | Integración pendiente |
|---|---|---|
| RF-06; RN-17 | Identidad/año estables, omisiones no destructivas, grupo común, historia, contactos/contexto y rectificación expresa. | Persistencia y vistas según permisos. |
| RF-08–09; RN-18 | Campos tipados, asociación revisada de encabezados, hoja/fila, alcance, previsualización y errores localizados. | Leer el libro, rangos, títulos/etapas, celdas combinadas, fórmulas guardadas y formatos por celda. |
| RF-10 | Confirmación conjunta AIP, procedencia, resultados, reintento por ID/huella, rectificación/borrados y contexto selectivo. | Transacción durable y concurrencia. |
| RF-11 | Coincidencia documental interanual segura, homónimos/contradicciones expresos y unicidad contra padrón completo. | Coordinación concurrente de identidad en almacenamiento. |
| CU-01 | Escenarios con filas tipadas ficticias, incluida muestra de 91 identidades y pruebas negativas. | Aceptación del libro físico y del flujo de usuario completo. |

El constructor de `DocumentoIdentidad` sigue validando ocho dígitos de DNI. El adaptador futuro
debe convertir valores numéricos completos a texto y reportar por celda los inválidos **antes** de
crear filas tipadas, permitiendo omitir opcionales expresamente. No se rellenan dígitos perdidos.
Un nombre vacío seleccionado se presenta como error de fila; no se lo convierte en identidad.
Las filas de título/etapa deben descartarse en el adaptador: este modelo no pretende reconocerlas
a partir de texto libre ni declara que toda cadena no vacía represente a un estudiante.

### Pruebas y límites

`ConciliacionMatriculaTest` y `RegistroMatriculasTest` cubren documento seguro/ambiguo/contradictorio, nacimientos, homónimos, UUID propuesto/elegido, conflictos entre filas, exclusión, grupos ausentes/comunes/históricos, omisiones, rectificación, previsualización obsoleta, reintento tras reconstitución, reutilización de ID, revocación AIP, autor ajeno, fechas e inmutabilidad. La muestra de 91 identidades usa datos ficticios. La ejecución vigente figura en «Verificación reproducible».

## Importación selectiva, contactos y contexto

`RegistroImportacion` coordina matrícula y contexto conservando identidad, historia e inmutabilidad.
No hay infraestructura, lectura de Excel, I/O ni transacciones de base de datos en estas clases.

### Contratos y recorrido

Los errores de grupo/contexto se pasan a `RegistroMatriculas.previsualizar` sin construir un lote filtrado intermedio. Todas las filas originales no excluidas participan en la detección de documentos repetidos, aunque alguna tenga otro error; no gana la fila restante por haberse ocultado su contradicción. Una fila excluida expresamente no invalida el lote por su grupo. La confirmación comprueba que los grupos de las filas aceptadas sigan admitidos; si cambió la configuración, exige nueva revisión antes de devolver estado.

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
   vigente, configuración no futura, año conocido y grupos admitidos. Usa la conciliación de matrícula sobre todas las filas originales.
   Un contexto no admitido/mal asociado o un grupo de fila no admitido producen error localizado en **la fila completa** antes de confirmar matrícula;
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

El recuento separa altas y actualizaciones aunque el alta incluya contexto. Los homónimos con documentos diferentes permanecen separados: una identidad bloqueada no
impide actualizar otra con documento seguro ni confirmar un alta expresamente distinta. Si hace
falta conciliación por nombre, los candidatos bloqueados se sustituyen por un aviso sin ID,
nombre ni documento. Sin documento en el candidato bloqueado no se permite eludir el bloqueo
forzando un alta; requiere resolución institucional.

### Rectificaciones de matrícula

`LoteMatricula` incorpora una colección opcional de `RectificacionMatricula` por número de fila;
su constructor anterior sigue disponible. El alcance puede borrar documento, nacimiento, sexo,
situación o grupo, o sustituir expresamente un grupo contradictorio para la misma fecha efectiva.
No se puede aportar y borrar el mismo dato. No se sustituye grupo sin suministrar el nuevo.
La sustitución/borrado en una fecha no reescribe grupos posteriores ni instantáneas de incidencias.
Un grupo común del lote no repone silenciosamente un grupo borrado expresamente.

El resumen de matrícula muestra los campos cambiados; el coordinador contextual conserva además
la solicitud revisada en su resultado por fila. La huella del lote incorpora también esas solicitudes.
La huella contextual usa SHA-256 con longitudes y marcadores de presencia, campos tipados y
ordenación de claves, sin conservar los datos descartados en las confirmaciones.

### Pruebas y límites

`RegistroImportacionTest` y `AsociacionColumnasTest` cubren confirmación sin filas parciales, alcance admitido, asociaciones, contactos separados/compartidos, omisiones y negaciones, procedencia, sustitución/borrado delimitados, historia reservada, huellas/reintentos, obsolescencia/autoría, configuración vigente, bloqueo posterior a previsualización y rectificaciones que preservan grupos posteriores. Se comprueban errores por fila sin ocultar conflictos documentales del lote original. El resultado actual está en «Verificación reproducible».

El dominio no lee `.xlsx`, no normaliza grupos textuales como «1° A» ni convierte números/fechas de
Excel, no calcula fórmulas y no detecta filas de títulos o numeración. Eso se implementará en el
adaptador Excel, entregando campos tipados y asociaciones revisadas a estas reglas. No hay controles
de sesión, base de datos, archivo temporal, persistencia, HTTP ni borrado físico. Las capacidades de
dominio existen; la aceptación integral de CU-01 exige probar ese adaptador y la transacción real.

## Consultas, historial y estadística

Paquete `pe.edu.incidencias.domain.consulta`; no depende de Spring, persistencia, generación de archivos ni reloj del sistema. Extiende RF-16, RF-34–37 y la consistencia RNF-19. No implementa un motor de consultas de texto libre, comunicación externa automática ni adaptación PDF/CSV/XLSX.

### Contratos y carga

`ConsultaHistorial(registro, hechosCargados, politicaActual)` no necesita un padrón: consulta identidades y grupos conservados en los hechos. Las fuentes de sus resultados se autorizan individualmente. Los casos relacionados se proyectan solo si se permite su ámbito completo, aplicando también las restricciones propias del registro. `MotorEstadisticas` usa el mismo criterio para estado de atención e intervención de comité; un hecho permitido no revela un caso restringido por otra fuente.

- `ConsultaEstadistica` declara medida, período inclusivo, agrupaciones y filtros. Las dimensiones se combinan con AND; los valores de una dimensión con OR. Las colecciones son copias inmutables. Rechaza agrupaciones duplicadas y dimensiones específicas sin pertinencia (motivo solo para señales, entidad solo derivaciones, tipo de cierre solo casos y hora solo hechos/participaciones/estudiantes).
- `MotorEstadisticas` recibe `RegistroAtenciones`, `RegistroAlertas`, `RegistroMatriculas`, IDs de **hechos** cargados y `PoliticaAcceso` actual. No recibe cifras calculadas por cliente. La aplicación debe cargar un universo completo para la consulta requerida y sus relaciones de reserva; el dominio no puede demostrar que una muestra sea toda la institución. Valida que las fuentes de casos/señales cargados estén presentes. Los IDs absorbidos por relaciones del registro se normalizan; un ID de reporte manualmente vinculado no es necesariamente un ID de hecho cargado.
- La política recibida debe ser actual, construida con directorio, restricciones y `registro.proteccion()`. Además se consulta la protección/restricción del propio registro. No se reinstancia una política vacía para autorizar lecturas. El rol se exige aun con universo vacío. Una exclusión o bloqueo no produce ni título ni cifra separada de lo omitido.

### Unidades y recuentos

Las medidas disponibles y su fecha de asignación al período son:

| Medida | Unidad y criterio temporal |
|---|---|
| `HECHOS` | Hechos únicos, por fecha del hecho. |
| `COMUNICACIONES` | Reportes válidos, por fecha de registro. |
| `PARTICIPACIONES` | Una persona por hecho, por fecha del hecho; incluye personas no estudiantes. |
| `ESTUDIANTES` | Identidades escolares distintas involucradas, por fecha del hecho. |
| `ESTUDIANTES_COTIDIANA`, `ESTUDIANTES_PRESUNTA`, `ESTUDIANTES_CORROBORADA`, `ESTUDIANTES_AFECTADOS`, `ESTUDIANTES_TESTIGOS` | Estudiantes distintos con el papel correspondiente, por fecha del hecho. |
| `CASOS_PENDIENTES` | Atenciones pendientes, por apertura. |
| `CASOS_CERRADOS`, `CASOS_RESUELTOS` | Atenciones cerradas, por fecha de cierre; resueltos excluye apertura errónea y duplicidad. |
| `ALERTAS_EMITIDAS`, `ALERTAS_GESTIONADAS`, `ALERTAS_PENDIENTES` | Señales según su condición, por emisión. |
| `ACTUACIONES_REALIZADAS`, `ACTUACIONES_PREVISTAS` | Actuaciones, por fecha efectiva o prevista respectivamente. |
| `COMPROMISOS` | Compromisos no cancelados, por acuerdo. |
| `COMPROMISOS_PENDIENTES`, `SEGUIMIENTOS_PENDIENTES` | Compromisos con seguimiento pendiente, por acuerdo; no son tareas adicionales independientes. |
| `COMPROMISOS_COMPROBADOS` | Compromisos con cumplimiento/no cumplimiento observado, por última observación. |
| `SEGUIMIENTOS_COMPROBADOS` | Revisiones con observación de cumplimiento/no cumplimiento, por fecha de observación. |
| `DERIVACIONES_EFECTIVAS` | Derivaciones efectuadas, por fecha efectiva. |

`ALERTAS_PENDIENTES` agrupa y filtra por motivos vigentes; `ALERTAS_EMITIDAS` y `ALERTAS_GESTIONADAS` conservan los motivos históricos de sus revisiones. Retirar gravedad del fundamento vigente no la presenta como motivo todavía pendiente.

Hay 23 medidas comprobadas con fixture común: hechos, comunicaciones, participaciones, estudiantes distintos y los cinco subconjuntos por papel (cotidiana, presunta, corroborada, afectados, testigos); casos pendientes/cerrados/resueltos; señales emitidas/gestionadas/pendientes; actuaciones realizadas/previstas; compromisos totales/pendientes/comprobados, seguimientos pendientes/comprobados y derivaciones efectivas.

Cada medida genera unidades procedentes de agregados y filas de contexto con variables conocidas. Los recuentos deduplican **identidad de unidad**, tanto en total como por celda. Un hecho compartido por varios grupos cuenta una sola vez institucionalmente; sus subtotales pueden no ser aditivos. Participaciones cuentan persona/hecho, estudiantes cuentan identidad persistente y excluyen familiares/personal/otras personas. Las personas presentes solo en un reporte vinculado también se incluyen. Reportes anulados no aportan hechos ni personas activas. La discrepancia de papel se identifica como `DISCREPANTE` y no se convierte en atribución elegible.

Los casos cerrados incluyen cierre técnico identificado con `TIPO_CIERRE`; `CASOS_RESUELTOS` excluye apertura errónea/duplicidad. Una base sintética permite conservar casos y actuaciones reales aunque no queden participaciones activas, sin reactivar sus estudiantes para cifras. Las acciones inmediatas cuentan sin necesidad de caso y conservan el mismo ID cuando se referencian desde uno. Los compromisos cancelados no se cuentan. Seguimientos comprobados representan revisiones con observación y estado de cumplimiento/no cumplimiento registrado; el agregado actual no conserva un booleano independiente sobre si cada revisión repitió o cambió esa conclusión.

### Tiempo, grupos y falta de datos

`ConsultaEstadistica` admite agrupaciones y filtros sobre `ANIO`, `MES`, `DIA_SEMANA`, `HORA`,
`ANIO_ESCOLAR`, `CATEGORIA`, `NATURALEZA`, `GRAVEDAD`, `GRADO`, `SECCION`, `SEXO`, `TIPO_PERSONA`,
`PAPEL`, `ATRIBUCION`, `ESTADO_ATENCION`, `MOTIVO_ALERTA`, `ENTIDAD_DERIVACION`,
`INTERVENCION_COMITE`, `PRECISION_FECHA` y `TIPO_CIERRE`. Los filtros entre dimensiones se combinan
con AND y los valores dentro de una dimensión con OR. Motivo solo corresponde a señales, entidad
a derivaciones, tipo de cierre a casos y hora a hechos/participaciones/estudiantes. Una dimensión
específica no pertinente o una agrupación repetida se rechazan; no se ignoran silenciosamente.

El criterio temporal está declarado en `Medida.criterioTemporal`: hecho para hechos/participaciones/estudiantes, registro para comunicaciones, apertura o cierre para casos, emisión para señales, fecha efectiva/prevista para actuaciones, acuerdo para compromisos y observación para comprobaciones, fecha efectiva para derivaciones. Los grupos provienen del **snapshot `Participacion.grupoHistorico`**, no se rellenan ni reescriben desde el padrón actual. Los datos conocidos complementarios se combinan: omitir un grupo u hora en un duplicado no contradice el valor aportado por otra fuente. Variantes de grupo equivalentes por tildes/mayúsculas/espacios conservan la primera forma declarada. Valores conocidos realmente distintos se separan como `DISCREPANTE`; ausencia de valores como `NO_INFORMADO`. Sexo solo procede de dato explícito de la identidad al corte y no se infiere del nombre. Una fecha aproximada permite año/mes de referencia pero no día de semana ni hora exacta. Hora solo se publica si los valores conocidos de las fuentes activas coinciden y la fecha es exacta; no se deriva de fecha de registro.

Las celdas sustentan cruces, frecuencias, ordenación por frecuencia, comparación entre períodos, gráficos de evolución y mapa de calor por día. La representación gráfica pertenece al adaptador. El resultado conserva criterio, período, corte, cobertura de unidades evaluadas y unidades sin dato, diferenciados de cero. `comparar` mantiene medida/dimensiones/filtros y cambia exclusivamente el período.

### Proporciones

`Poblacion` exige año, fecha de referencia dentro de ese año, filtros opcionales de grado/sección/sexo y declaración de cobertura de la nómina (incluida parcial). `proporcionEstudiantes` deduplica estudiantes y restringe a la misma cohorte anual autorizada del denominador; no duplica una persona con varios hechos. Los filtros poblacionales del numerador no pueden contradecir la cohorte. El grupo del denominador sí se obtiene de la matrícula a la fecha de referencia explícita; el grupo del hecho conserva su snapshot.

`frecuenciaPorCien` cuenta hechos únicos que involucran miembros de esa cohorte y se rotula como hechos por cien estudiantes, no porcentaje de personas/probabilidad. Se retorna ausencia de valor y motivo cuando no hay denominador válido, junto con numerador/denominador, faltantes de grupo/sexo y referencia anual. Se redondea HALF_UP a dos decimales. No se inventa exposición individual para una supuesta tasa exacta.

### Historial y salidas

La búsqueda de persona usa la misma normalización de tildes, mayúsculas y espacios Unicode que la búsqueda escolar, tanto en historial institucional como en aportes propios. Para hechos anulados, persona, fecha del hecho y criterios deben coincidir en la misma revisión histórica y el período de registro en su reporte; no se combinan datos de versiones distintas. Se usa el estado actual autorizado del caso (`PENDIENTE`, `CERRADA`, `SIN_ATENCION`), sin crear participaciones vigentes ni reincorporar anulaciones a las estadísticas.

`ConsultaHistorial` consulta por identidad conocida o descripción conocida de persona, fechas de hecho/registro, clasificación, gravedad, año/grupo snapshot y estado de atención. No fusiona nombres similares ni crea padrón externo. Ordena cronológicamente, ofrece tramos de 1 a 500 y deduplica hechos/casos/actuaciones/derivaciones; incluye procedencia desde el agregado y participaciones consolidadas. Cada fuente y caso se autoriza antes de proyectar. El acceso ordinario a un aporte propio usa la proyección reducida existente de `PoliticaAcceso`.

`ResultadoEstadistico` es un corte inmutable creado por el motor, con fuentes de reserva y recursos internos de todas las unidades evaluadas, incluidas las que aportan solo cobertura aunque no pasen el filtro/período; las tablas no publican dichos metadatos. `SalidaEstadistica` conserva corte y autor/fecha de emisión. Preparar salida interna exige autorización y mantiene cifras exactas, incluso uno; no aplica umbral arbitrario. La externa exige finalidad, base institucional declarada, destinatario, evaluación de riesgo resuelta y responsable/instante de revisión coherentes con emisión. Una salida identificable pendiente se rechaza; el responsable puede recalcular con agrupaciones más amplias antes de revisarla. El software no determina legitimación jurídica.

Descargar en cualquiera de los tres formatos revalida política/controles vigentes sobre salida, recursos y todas sus fuentes y casos, incluidos cierres actuales de relaciones de reserva y referencias conservadas. Si falla una fuente, se rechaza la salida completa: no se entrega silenciosamente un corte viejo parcial. Las tablas contienen solo dimensiones solicitadas y cantidad, sin relatos ni nombres/DNI ni columnas ocultas. Los valores con prefijos de fórmula se neutralizan; el adaptador debe escapar correctamente CSV/XLSX y generar los bytes. Persistir emisión/descarga y evidencia de acceso corresponde a aplicación/adaptadores y su contrato transaccional.

### Pruebas y límites

`MotorEstadisticasTest`, `ReconciliacionMedidasTest`, `HistorialYSalidaTest` y `AuditoriaConsultasTest` comprueban las 23 medidas con un conjunto conocido, filtros/cruces, deduplicación, familiares/personas de duplicados, discrepancias y anulaciones, grupos históricos, horas fiables, cohorte/denominador, tres formatos, revisión externa, acceso propio, exclusión y bloqueo sobrevenidos. Se comprueba la reserva de cobertura fuera del período y de casos compartidos, y la conjunción de filtros por revisión anulada.

El historial devuelve las anulaciones con `hecho.anulado() == true`, participaciones vigentes vacías y actuaciones reales conservadas. `buscarAportesPropios` filtra/pagina usando únicamente las revisiones visibles del autor; las correcciones institucionales ajenas no sirven como canal lateral. Los límites de persistencia, generación de bytes y aceptación de interfaz se detallan en «Contratos obligatorios de integración».

## Protección, disposición de datos y evidencia

Base: ERS v1.2, RF-28, RF-38–39, RN-24–26 y RNF-02, RNF-07, RNF-18. El modelo expresa decisiones institucionales delimitadas; no añade un rol con acceso general.

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
duplicidades y separaciones conservan el control. Cada nueva instantánea calcula `proteccionEfectiva` mediante `conRelaciones`: IDs de actuaciones, compromisos de todas sus revisiones, derivaciones, convocatorias, intervenciones/decisiones del comité y acciones inmediatas, y personas referidas en ellas, se relacionan con su ámbito. Un bloqueo
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

Las personas seleccionadas en los criterios deben estar incluidas en el alcance declarado del evento. `extraer` comprueba también el bloqueo del ID del propio evento, además de sus personas, recursos y ámbito; un criterio no amplía una autorización institucional.

`RegistroEvidencia` conserva `Evento` con usuario, fecha, operación, resultado, ámbito,
criterios tipados e ID relacionado. Admite sesiones, consulta, creación, modificación,
anulación, eliminación, importación, exportación, extracción de evidencia y recuperación.
No acepta un campo de relato, adjuntos, contraseña o descripción libre de la consulta.
Los criterios conservan intervalo, año y referencias seleccionadas sin copiar filas/relatos.

`registrar` devuelve un nuevo registro disponible inmediatamente; mismo ID/contenido es
idempotente, ID reutilizado con contenido distinto se rechaza. `cargar` reconstituye una
instantánea validando unicidad y orden cronológico en una sola pasada de coste lineal; no reconstruye ni copia acumulativamente cada prefijo de la historia. La evidencia no debe quedar sin ámbito:
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

### Pruebas y límites

`ProteccionDatosTest` y `BloqueoJuridicoIntegracionTest` comprueban mandatos delimitados, vigencias, bloqueo/levantamiento, fuentes/recursos históricos y anidados, decisiones futuras, transformación selectiva y terceros, manifiesto incompleto, retenciones, relectura exacta, evidencia idempotente, extracción restringida y retiro tras el mínimo. Se comprueba revocación de informes/actas ya existentes y de recursos proyectados en ellos. El resultado vigente está en «Verificación reproducible».

### Validación del contenido candidato

Las actualizaciones de hechos, casos y acciones inmediatas comprueban el control del estado
candidato antes de devolverlo. Autorizar el estado anterior no permite incorporar una persona
o recurso ya bloqueado en un ámbito antes disponible. Si el nuevo contenido no está permitido,
se rechaza el cambio y la instantánea anterior permanece íntegra.

## Configuración institucional y selección contextual para informes

Base: RF-02–03, RF-31 y RF-40. No introduce una aplicación, puertos o adaptadores.

### Configuración versionada

`ConfiguracionInstitucional` es un agregado inmutable de una instalación. `Estado` conserva
datos institucionales, aviso, alcance de importación, categorías versionadas, ventana común,
años/grupos y año de trabajo opcional. `Revision` conserva el estado completo, operación,
autor/fecha e ID de delegación cuando corresponde. No se fijan colegio, año ni ventana de ejemplo.
Los accesores completos son para persistencia confiable; la aplicación no debe serializarlos a
roles sin la capacidad correspondiente.

`DefinicionInstitucional` expresa valores decididos, no columnas detectadas en una fuente.
TOE/director con `DEFINIR_CONFIGURACION` pueden crear o definir directamente. Las categorías
incluidas se crean/actualizan/activan/desactivan con sus APIs; las omitidas permanecen,
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
cierre/desbloqueo. Registrar de nuevo un año existente o seleccionar el año de trabajo ya elegido
devuelve la instancia actual. Definir grupos o seleccionar año de trabajo exige un año registrado;
una lista con grupos equivalentes repetidos se rechaza. La comparación de grupos tolera
tildes/mayúsculas/espacios según la normalización existente. Un grupo no informado
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
a una persona ajena al ámbito. El resultado vigente se detalla en «Verificación reproducible».

Los registros de aplicación deben conservar configuración/directorio/control actuales y usar
versión esperada al guardar. La evidencia técnica continua, mostrar el aviso, interfaces de
mantenimiento y la publicación de versiones son integración de aplicación/adaptadores.

## Cobertura de requisitos

La tabla delimita las reglas disponibles en el núcleo y la integración necesaria para completar el producto.

| ERS | Reglas de dominio implementadas | Integración posterior |
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


## Contratos obligatorios de integración

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

8. Resolver personas contra identidades existentes y obtener clasificaciones para nuevos hechos desde categorías activas. Un UUID de autor o un record de clasificación construido por el cliente no acreditan permiso ni vigencia del catálogo.
9. Conciliar matrícula con padrón/índice completo y guardar identidad, matrícula, contexto, confirmación y evidencia en una misma operación recuperable. Conservar IDs y contenido del lote durante reintentos; no reemplazar datos no cargados con una colección parcial.
10. Para emitir o consultar un informe, aportar el registro actual de todas sus actas citadas y sus revisiones. Para una salida estadística, revalidar también las fuentes que solo aportaron cobertura, y los casos que aportaron estado/intervención. No entregar bytes almacenados por una ruta que evite estos controles.

## Verificación reproducible

Comando desde `SistemaIncidenciasEscolaresBackend`, con Oracle JDK 27 y compilación
`release 25`. `JAVA_HOME` debe apuntar al JDK disponible en la instalación:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-27'
.\mvnw.cmd -B -o clean test
```

Sin dependencias descargadas previamente se omite `-o`. Resultado comprobado el 22-10-2026:
**BUILD SUCCESS; 651 pruebas, 0 fallos, 0 errores y 0 omitidas**. Se recompilaron desde limpio
90 archivos productivos y 45 de pruebas. No se modificó el POM ni se añadieron dependencias.

| Paquete | Ejecuciones |
|---|---:|
| Acceso | 98 |
| Alertas | 59 |
| Atención | 148 |
| Catálogo | 21 |
| Consultas | 58 |
| Documentos | 76 |
| Estudiantes e importación | 101 |
| Incidencias | 53 |
| Institución | 14 |
| Protección | 23 |
| **Total** | **651** |

La suite comprueba escenarios positivos y negativos con datos ficticios: permisos/conflictos,
revocación de recursos y documentos anteriores, identidad/importación por fila, reintentos,
grupos históricos, conteos reconciliados, atención/cierre, autorías y disposición verificable.
`AuditoriaAlertasTest`, `AuditoriaConsultasTest` y `AuditoriaAtencionTest` comprueban regresiones
de alertas, proyecciones, atención, autorías y reserva junto con las suites de cada concepto.

`target/surefire-reports/` contiene los resultados regenerables y no se versiona. Las pruebas de
dominio no acreditan autenticación/sesiones, concurrencia entre dos sesiones, almacenamiento,
parser Excel, inspección de archivos, generación efectiva PDF/CSV/XLSX, respaldo/restauración ni
accesibilidad, rapidez o facilidad de uso de una interfaz todavía inexistente. La aceptación
integral de la ERS requiere implementar y ensayar esas capas y sus procedimientos operativos.
