package pe.edu.incidencias.domain.documento;

import pe.edu.incidencias.domain.acceso.*;
import pe.edu.incidencias.domain.atencion.*;
import pe.edu.incidencias.domain.catalogo.*;
import pe.edu.incidencias.domain.incidencia.*;
import pe.edu.incidencias.domain.institucion.DatosInstitucion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class DatosPruebaDocumentos {
    static final Instant INICIO = Instant.parse("2026-10-21T15:00:00Z");
    static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    static final UUID ADMIN = id(1), DIRECTOR = id(2), TOE = id(3), COMITE = id(4), DOCENTE = id(5), AIP = id(6);
    static final UUID CASO = id(200), ESTUDIANTE = id(50), OTRO = id(51);
    static UUID id(long numero) { return new UUID(0, numero); }
    static Instant reloj(int segundos) { return INICIO.plusSeconds(segundos); }
    static RegistroCambio cambio(UUID usuario, int segundos) { return new RegistroCambio(usuario, reloj(segundos), Optional.empty()); }
    static RegistroCambio motivo(UUID usuario, int segundos) { return new RegistroCambio(usuario, reloj(segundos), Optional.of("Corrección documentada")); }
    static DirectorioUsuarios directorio() {
        var d = DirectorioUsuarios.inicializar(ADMIN, "Administrador", "admin", motivo(id(99), 0));
        var usuarios = List.of(DIRECTOR, TOE, COMITE, DOCENTE, AIP);
        var roles = List.of(RolUsuario.DIRECTOR, RolUsuario.TOE, RolUsuario.MIEMBRO_COMITE, RolUsuario.DOCENTE, RolUsuario.AIP);
        for (int i = 0; i < usuarios.size(); i++) d = d.registrarUsuario(usuarios.get(i), "Personal institucional " + i, "usuario" + i, cambio(ADMIN, 0))
                .asignarRol(usuarios.get(i), id(1000 + i), roles.get(i), new Vigencia(INICIO, Optional.empty()), cambio(ADMIN, 0));
        return d;
    }
    static PersonaInvolucrada estudiante() { return PersonaInvolucrada.estudiante(ESTUDIANTE, "María Lucía Pérez Soto"); }
    static PersonaInvolucrada otro() { return PersonaInvolucrada.estudiante(OTRO, "José Luis Ramos Díaz"); }
    static ClasificacionCategoria clasificacion() {
        return new ClasificacionCategoria(id(80), "Protección", NaturalezaCategoria.VIOLENCIA_PROTECCION, Gravedad.GRAVE, true);
    }
    static ReporteIncidencia reporte(int id, PersonaInvolucrada persona, PapelParticipacion papel, LocalDate fecha, String texto) {
        return ReporteIncidencia.registrar(id(id), new DatosReporte(FechaHecho.exacta(fecha), 2026, clasificacion(), texto,
                List.of(new Participacion(persona, papel, Optional.empty())), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()), DOCENTE, reloj(10));
    }
    static Hecho hecho() { return Hecho.de(reporte(100, estudiante(), PapelParticipacion.AFECTADO, HOY.minusDays(1), "María Lucía Pérez Soto relata un hecho")); }
    static RegistroAtenciones abierto() { return abrir(hecho()); }
    static RegistroAtenciones abrir(Hecho h) {
        return RegistroAtenciones.iniciar(List.of(h), RestriccionesAmbito.vacias())
                .abrirCaso(CASO, Set.of(h.id()), HOY, TOE, List.of(), Optional.empty(), directorio(), cambio(TOE, 100));
    }
    static ArchivoAdjunto pdf(int numero) { return new ArchivoAdjunto(id(numero), "acta.pdf", "application/pdf", 100); }
    static ActaAdjunta acta() { return ActaAdjunta.incorporar(id(300), CASO, HOY, "Acta institucional", pdf(301), cambio(DIRECTOR, 110)); }
    static DatosInstitucion institucion() { return new DatosInstitucion("Institución educativa", Optional.of("001"), Optional.empty(), Optional.empty()); }
    static GeneradorInformeCaso generador(RegistroAtenciones registro) {
        return new GeneradorInformeCaso(registro, RegistroActas.vacio(), ReferenciasOficiales.vacias(), directorio(), java.util.Map.of());
    }
    static InformeCaso vista(GeneradorInformeCaso g) {
        return g.previsualizar(CASO, institucion(), "Atención institucional", DestinoInforme.interno(), Optional.empty(), java.util.Map.of(), TOE, reloj(120), reloj(120));
    }
}
