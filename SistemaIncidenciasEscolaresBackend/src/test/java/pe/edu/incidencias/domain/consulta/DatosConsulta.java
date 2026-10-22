package pe.edu.incidencias.domain.consulta;

import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.alerta.RegistroAlertas;
import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.catalogo.*;
import pe.edu.incidencias.domain.estudiante.*;
import pe.edu.incidencias.domain.incidencia.*;
import java.time.*;
import java.util.*;

final class DatosConsulta {
    static final Instant BASE = Instant.parse("2026-10-22T12:00:00Z");
    static final LocalDate DIA = LocalDate.of(2026, 10, 20);
    static final UUID ADMIN = id(1), TOE = id(2), DOCENTE = id(3), DIRECTOR = id(4), ESTUDIANTE = id(50);
    static UUID id(long n) { return new UUID(0, n); }
    static RegistroCambio cambio(int n) { return new RegistroCambio(TOE, BASE.plusSeconds(n), Optional.of("Decisión ficticia")); }
    static DirectorioUsuarios directorio() {
        var d = DirectorioUsuarios.inicializar(ADMIN, "Admin ficticio", "admin", new RegistroCambio(id(99), BASE, Optional.of("Alta inicial")));
        for (var u : List.of(TOE, DOCENTE, DIRECTOR)) d = d.registrarUsuario(u, "Usuario ficticio", "u" + u,
                new RegistroCambio(ADMIN, BASE, Optional.empty())).asignarRol(u, id(1000 + u.getLeastSignificantBits()),
                u.equals(TOE) ? RolUsuario.TOE : u.equals(DIRECTOR) ? RolUsuario.DIRECTOR : RolUsuario.DOCENTE, new Vigencia(BASE, Optional.empty()),
                new RegistroCambio(ADMIN, BASE, Optional.empty()));
        return d;
    }
    static PersonaInvolucrada persona(long n) { return PersonaInvolucrada.estudiante(id(n), "Estudiante ficticio " + n); }
    static Hecho hecho(long n, long estudiante, PapelParticipacion papel) {
        return Hecho.de(reporte(n, List.of(new Participacion(persona(estudiante), papel,
                estudiante == 50 ? Optional.of(new GrupoEscolar("1", "A")) : Optional.empty())), FechaHecho.exacta(DIA), Optional.of(LocalTime.of(10, 0))));
    }
    static ReporteIncidencia reporte(long n, List<Participacion> personas, FechaHecho fecha, Optional<LocalTime> hora) {
        return ReporteIncidencia.registrar(id(n), new DatosReporte(fecha, 2026,
                new ClasificacionCategoria(id(80), "Categoría ficticia", NaturalezaCategoria.CONDUCTA_DISCIPLINARIA, Gravedad.LEVE, false),
                "Relato reservado que nunca debe salir en estadísticas", personas, false, hora, Optional.empty(), Optional.empty(), Optional.empty()), DOCENTE, BASE.plusSeconds(1));
    }
    static RegistroMatriculas matriculas() {
        var a = Estudiante.crear(ESTUDIANTE, "Nombre secreto").incorporarIdentidad(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("F"))
                .vincularMatricula(MatriculaAnual.crear(2026, LocalDate.of(2026, 1, 1), Optional.of(new GrupoEscolar("1", "A")), Optional.empty()))
                .incorporarGrupo(2026, LocalDate.of(2026, 10, 21), Optional.of(new GrupoEscolar("2", "B")));
        var b = Estudiante.crear(id(51), "Otro nombre secreto").vincularMatricula(MatriculaAnual.crear(2026,
                LocalDate.of(2026, 1, 1), Optional.empty(), Optional.empty()));
        return RegistroMatriculas.cargar(List.of(a, b), List.of());
    }
    static RegistroAtenciones registro(Hecho... hechos) { return RegistroAtenciones.iniciar(List.of(hechos), RestriccionesAmbito.vacias()); }
    static PoliticaAcceso politica(RegistroAtenciones r) { return new PoliticaAcceso(directorio(), r.restricciones(), r.proteccion()); }
    static MotorEstadisticas motor(RegistroAtenciones r, long... ids) {
        var conjunto = Arrays.stream(ids).mapToObj(DatosConsulta::id).collect(java.util.stream.Collectors.toSet());
        return new MotorEstadisticas(r, RegistroAlertas.vacio(), matriculas(), conjunto, politica(r));
    }
    static ConsultaEstadistica.Periodo periodo() { return new ConsultaEstadistica.Periodo(DIA.minusMonths(1), DIA.plusMonths(1), true); }
    static ConsultaEstadistica consulta(ConsultaEstadistica.Medida m) { return ConsultaEstadistica.de(m, periodo()); }
    static long contar(MotorEstadisticas m, ConsultaEstadistica.Medida medida) { return m.calcular(consulta(medida), TOE, BASE.plusSeconds(200)).total(); }
}
