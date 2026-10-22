# Sistema de incidencias escolares

Proyecto para registrar y dar seguimiento a incidencias escolares, organizar la atención institucional y conservar la trazabilidad de reportes, actuaciones y documentos. Su alcance funcional está definido en la Especificación de Requisitos de Software (ERS v1.2), orientada inicialmente a secundaria en Perú.

## Estado de desarrollo

**En desarrollo — núcleo de dominio implementado, versión `0.1.0-SNAPSHOT`.** Estado documentado al 22 de octubre de 2026.

La carpeta contiene una biblioteca Java con los incrementos de dominio 1 a 7, sus revisiones correctivas, la conciliación de matrícula 8A y la continuación de importación/contexto, consultas/estadísticas, configuración y protección de datos. Todavía no es una aplicación desplegable ni está preparada para operar con datos reales. Implementar las reglas de dominio no equivale a aceptar la ERS de extremo a extremo.

| Área | Implementación actual |
| --- | --- |
| Estudiantes | Identidad estable, documento opcional, matrícula anual, grupos históricos y búsquedas en memoria. |
| Importación de matrícula | Conciliación, asociación revisada de encabezados, alcance institucional admitido, contactos/contexto, historial reservado, rectificación/borrado explícito y confirmación conjunta sin duplicados. |
| Catálogo | Categorías, clasificación histórica, gravedad y parámetros de reiteración. |
| Incidencias | Hechos, reportes, participantes, fuentes vinculadas, correcciones y duplicidad. |
| Acceso | Usuarios, roles, vigencias, permisos y exclusiones por conflicto como reglas de dominio. |
| Atención | Casos, actuaciones, compromisos, derivaciones, cierre local, comité y continuidad. |
| Alertas | Reglas de gravedad y reiteración, notificaciones y bandeja de atención en el dominio. |
| Documentos | Modelo y composición de informes, actas adjuntas, reservas y referencias oficiales. |
| Consultas y estadísticas | Trayectoria reservada, medidas diferenciadas, filtros/cruces, cobertura, proporciones y tablas exportables con revalidación de reserva. |
| Configuración | Institución/aviso/alcance, catálogo/ventana versionados, carga delegada de valores exactos y años/grupos sin promoción automática. |
| Protección y evidencia | Bloqueos jurídicos de personas/ámbitos/recursos, mandatos específicos, disposición verificable por manifiesto y trazabilidad sin relatos. |

El cierre local no equivale al cierre de un procedimiento oficial. Las reglas de acceso aún requieren integración con autenticación y con las operaciones de la futura aplicación. La composición de informes y el modelo de adjuntos no incluyen todavía almacenamiento ni descarga efectiva de archivos.

## Tecnología y organización

- Java con compilación `release 25`.
- Maven Wrapper 3.9.16.
- JUnit Jupiter 5.11.4 y Maven Surefire 3.5.4 para pruebas.
- Dominio independiente de infraestructura, sin dependencias productivas externas, Spring ni JPA.
- Agregados, valores y revisiones inmutables; las operaciones devuelven nuevas instancias.

```text
SistemaIncidenciasEscolares/
├── README.md
└── SistemaIncidenciasEscolaresBackend/
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    ├── .mvn/wrapper/
    └── src/
        ├── main/java/pe/edu/incidencias/domain/
        └── test/java/pe/edu/incidencias/domain/
```

Hay 90 archivos Java productivos y 42 archivos Java de pruebas (40 suites y dos auxiliares). Las capas `application` y `adapter` no tienen implementación; Git no conserva directorios vacíos. El dominio no realiza I/O ni genera UUID o fechas de operación por su cuenta.

El incremento 8A recibe filas ya seleccionadas, interpretadas y validadas por un futuro adaptador. `RegistroMatriculas.previsualizar` devuelve cambios, advertencias y conflictos sin modificar el estado; `confirmar` revalida el permiso AIP y devuelve otro registro completo con evidencia del lote. Un documento único sin contradicción permite reconocer la misma identidad entre años. Nombre/grupo nunca unen personas automáticamente. El grupo común completa desconocidos y conserva los grupos ya conocidos.

La integración debe aportar un padrón completo para conciliar documentos, una política de acceso actual y una transacción con versión esperada para guardar estudiantes y resultado juntos. Los accesores de estado del registro son para persistencia: no son una respuesta de consulta para AIP. Reintentar exige conservar el ID y contenido del lote; modificarlo tras confirmarlo requiere un ID nuevo.

`RegistroImportacion` añade contexto/contactos e historial a esa misma unidad de confirmación. Su entrada pública valida año/grupos y alcance contra `ConfiguracionInstitucional`. Los candidatos y resultados AIP no conceden lectura de antecedentes anteriores. Las reglas de asociación reciben encabezados interpretados: leer celdas `.xlsx`, fórmulas guardadas y formatos sigue siendo tarea del adaptador.

`MotorEstadisticas` y `ConsultaHistorial` derivan resultados de registros de hechos/atención/alertas y de la nómina autorizada. Los grupos del análisis proceden de las participaciones históricas del hecho; la población denominadora usa su referencia anual explícita. Se distinguen datos no informados, discrepancias, cero y proporciones no calculables. `SalidaEstadistica` prepara tablas para PDF/CSV/XLSX y revalida todas las fuentes del corte al descargar; no genera bytes ni publica información.

`ControlProteccion` se suministra actualizado a la política y registros protegidos. La autorización para disponer datos o extraer evidencia es institucional y específica, sin un rol de acceso universal. `DisposicionDatos` transforma representaciones delimitadas, protege campos de terceros y comprueba el manifiesto completo antes de registrar ejecución. El adaptador debe enumerar las copias operativas, aplicar cambios y releerlos; preparar un plan no acredita haber borrado archivos o datos persistidos. Las copias de recuperación requieren el procedimiento institucional documentado.

## Compilar y ejecutar las pruebas

Requisitos: un JDK compatible con Java 25, `JAVA_HOME` apuntando al JDK y acceso a Internet para descargar Maven y las dependencias en la primera ejecución.

Desde la raíz del repositorio, en Windows/PowerShell:

```powershell
cd SistemaIncidenciasEscolaresBackend
.\mvnw.cmd -B clean test
```

En Linux/macOS:

```sh
cd SistemaIncidenciasEscolaresBackend
sh mvnw -B clean test
```

Para generar el JAR de la biblioteca:

```powershell
.\mvnw.cmd -B package
```

Con Maven y sus dependencias ya descargados se puede añadir `-o` para trabajar sin conexión. Los resultados se generan en `target/surefire-reports/`. Verificación del 22 de octubre de 2026: `mvnw.cmd -B -o clean test`, con JDK 27 y compilación `release 25`, terminó con **BUILD SUCCESS: 610 pruebas, sin fallos, errores ni omisiones**. La continuación añade 155 ejecuciones a las 455 existentes después de 8A. Los informes y las clases compiladas se regeneran localmente y no se versionan.

No hay todavía un servidor o interfaz que iniciar: el JAR corresponde al núcleo de dominio.

## Trabajo pendiente

1. Implementar casos de uso y puertos del monolito hexagonal, con transacciones, versión esperada y reintentos persistentes.
2. Integrar autenticación/sesiones, API e interfaz; aplicar la política actual antes de entregar cualquier agregado o derivado.
3. Construir el adaptador Excel y validar el libro completo ficticio de aceptación, además de las filas ya tipadas.
4. Implementar almacenamiento/inspección de adjuntos, renderizado PDF/CSV/XLSX, registro durable de evidencia y disposición efectiva de copias operativas.
5. Integrar respaldo/restauración y decisiones vigentes de datos/restricciones antes de reabrir accesos tras recuperar una copia.
6. Ejecutar aceptación integral de la ERS, seguridad, concurrencia, rendimiento, accesibilidad y despliegue.

No se declara aceptado ningún caso de uso de aplicación de extremo a extremo por pasar pruebas de dominio.

## Archivos del repositorio

Se versionan el código fuente, las pruebas, el Wrapper de Maven y el README principal. Los `.gitignore` excluyen compilados, configuración local del IDE, registros y credenciales. Los archivos de matrícula con datos personales deben mantenerse fuera del repositorio; los ejemplos de prueba son ficticios.
