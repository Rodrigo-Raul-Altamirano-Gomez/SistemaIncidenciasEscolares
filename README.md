# Sistema de incidencias escolares

Proyecto para registrar y dar seguimiento a incidencias escolares, organizar la atención institucional y conservar la trazabilidad de reportes, actuaciones y documentos. Su alcance funcional está definido en la Especificación de Requisitos de Software (ERS v1.2), orientada inicialmente a secundaria en Perú.

## Estado de desarrollo

**En desarrollo — núcleo de dominio, versión `0.1.0-SNAPSHOT`.** Estado documentado al 22 de octubre de 2026.

La carpeta entregada contiene una biblioteca Java con los incrementos de dominio 1 a 7 y sus revisiones correctivas. Todavía no es una aplicación desplegable ni está preparada para operar con datos reales. No se declara completada la totalidad de la ERS.

| Área | Implementación actual |
| --- | --- |
| Estudiantes | Identidad estable, documento opcional, matrícula anual, grupos históricos y búsquedas en memoria. |
| Catálogo | Categorías, clasificación histórica, gravedad y parámetros de reiteración. |
| Incidencias | Hechos, reportes, participantes, fuentes vinculadas, correcciones y duplicidad. |
| Acceso | Usuarios, roles, vigencias, permisos y exclusiones por conflicto como reglas de dominio. |
| Atención | Casos, actuaciones, compromisos, derivaciones, cierre local, comité y continuidad. |
| Alertas | Reglas de gravedad y reiteración, notificaciones y bandeja de atención en el dominio. |
| Documentos | Modelo y composición de informes, actas adjuntas, reservas y referencias oficiales. |

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

Actualmente hay 65 archivos Java productivos y 29 archivos Java de pruebas, organizados en 28 suites y un auxiliar de datos. Las capas `application` y `adapter` no tienen implementación; Git no conserva directorios vacíos.

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

Con Maven y sus dependencias ya descargados se puede añadir `-o` para trabajar sin conexión. Los resultados se generan en `target/surefire-reports/`. Verificación del 22 de octubre de 2026: `mvnw.cmd -B -o test`, con JDK 27 y compilación `release 25`, terminó con **BUILD SUCCESS: 414 pruebas, sin fallos, errores ni omisiones**. Los informes y las clases compiladas se regeneran localmente y no se versionan.

No hay todavía un servidor o interfaz que iniciar: el JAR corresponde al núcleo de dominio.

## Trabajo pendiente

1. Completar las reglas de importación selectiva y conciliación de matrícula, contactos y contexto con procedencia.
2. Incorporar consultas históricas, estadísticas y reglas de exportación reservada.
3. Completar configuración institucional, años/grupos y decisiones de conservación y rectificación.
4. Implementar casos de uso, puertos y adaptadores, persistencia, transacciones y concurrencia.
5. Integrar autenticación, API, interfaz, parser Excel, almacenamiento de archivos, auditoría persistente y respaldos.
6. Validar la aceptación integral de requisitos y preparar el despliegue.

El punto de continuación propuesto es la importación selectiva, conciliación y contexto.

## Archivos del repositorio

Se versionan el código fuente, las pruebas, el Wrapper de Maven y el README principal. Los `.gitignore` excluyen compilados, configuración local del IDE, registros y credenciales. Los archivos de matrícula con datos personales deben mantenerse fuera del repositorio; los ejemplos de prueba son ficticios.
