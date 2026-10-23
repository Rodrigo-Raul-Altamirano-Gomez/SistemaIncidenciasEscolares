package pe.edu.incidencias.domain;

import java.time.*;
import java.util.*;

/** Una comunicación cotidiana con papeles individuales; no crea una atención. */
public final class Nota {
    private Nota() {}
    public enum Papel { ATRIBUIDA, AFECTADO, TESTIGO, OTRO, DESCONOCIDO }
    public record Participante(UUID estudianteId, Papel papel) {}
    public record Accion(String contenido, LocalDate fechaRealizada, String resultado) {}
    public record Compromiso(String contenido, String asumidoPor, LocalDate fechaObjetivo) {}
    public record Entrada(int anio, LocalDate fechaHecho, boolean aproximada, LocalTime hora,
                          String categoriaId, String descripcion, String lugar, String comunicante,
                          String canal, List<Participante> participantes, Accion accion, Compromiso compromiso) {
        public Entrada {
            participantes = participantes == null ? List.of() : List.copyOf(participantes);
        }
        public void validar(Clock clock, boolean disciplinaria) {
            LocalDate hoy = LocalDate.now(clock);
            if (fechaHecho == null || fechaHecho.isAfter(hoy)) fail("La fecha del hecho es obligatoria y no puede ser futura.");
            if (anio != fechaHecho.getYear()) fail("El año escolar debe corresponder a la fecha del hecho.");
            if (aproximada && hora != null) fail("Una fecha aproximada no debe inventar una hora.");
            if (hora != null && fechaHecho.equals(hoy) && hora.isAfter(LocalTime.now(clock))) fail("La hora real no puede ser futura.");
            texto(descripcion, 4000, true, "descripción");
            texto(lugar, 160, false, "lugar"); texto(comunicante, 160, false, "comunicante"); texto(canal, 80, false, "canal");
            if (participantes.isEmpty() || participantes.size() > 30) fail("Selecciona entre uno y treinta estudiantes.");
            Set<UUID> ids = new HashSet<>();
            for (Participante p : participantes) {
                if (p == null || p.estudianteId() == null || p.papel() == null) fail("Cada estudiante necesita un papel individual.");
                if (!ids.add(p.estudianteId())) fail("Un estudiante no puede aparecer dos veces en el mismo hecho.");
                if (p.papel() == Papel.ATRIBUIDA && !disciplinaria) fail("La atribución cotidiana solo corresponde a una categoría disciplinaria leve.");
            }
            if (accion != null) {
                texto(accion.contenido(), 2000, true, "acción realizada"); texto(accion.resultado(), 1000, false, "resultado");
                if (accion.fechaRealizada() == null || accion.fechaRealizada().isAfter(hoy) || accion.fechaRealizada().isBefore(fechaHecho))
                    fail("La fecha realizada debe estar entre el hecho y hoy.");
            }
            if (compromiso != null) {
                if (accion == null) fail("Vincula el compromiso a la acción inmediata en que se acordó.");
                texto(compromiso.contenido(), 2000, true, "compromiso"); texto(compromiso.asumidoPor(), 160, true, "persona que asume el compromiso");
                if (compromiso.fechaObjetivo() != null && compromiso.fechaObjetivo().isBefore(accion.fechaRealizada()))
                    fail("La fecha objetivo no puede ser anterior al acuerdo.");
            }
        }
    }
    public static void texto(String value, int max, boolean required, String label) {
        if (required && (value == null || value.isBlank()) || value != null && (value.length() > max || value.chars().anyMatch(c -> c < 32 && c != 10 && c != 13 && c != 9)))
            fail("Revisa " + label + ": " + (required ? "obligatorio, " : "") + "máximo " + max + " caracteres.");
    }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
