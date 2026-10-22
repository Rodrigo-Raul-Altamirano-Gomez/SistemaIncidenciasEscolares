package pe.edu.incidencias.domain.atencion;

import pe.edu.incidencias.domain.incidencia.RegistroCambio;
import pe.edu.incidencias.domain.incidencia.ReporteIncidencia;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record ConvocatoriaComite(UUID id, LocalDate fecha, String motivoEspecial,
                                 ComposicionComite composicion, Set<UUID> miembrosElegibles,
                                 RegistroCambio registro, Optional<String> motivoSustitucion) {
    public ConvocatoriaComite {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(composicion, "composicion");
        Objects.requireNonNull(registro, "registro");
        if (motivoEspecial == null || motivoEspecial.isBlank()) throw new IllegalArgumentException("Se requiere motivo de especial gravedad/no resolución");
        miembrosElegibles = Set.copyOf(miembrosElegibles);
        if (miembrosElegibles.isEmpty() || !composicion.usuariosIds().containsAll(miembrosElegibles))
            throw new IllegalArgumentException("La convocatoria necesita integrantes institucionales elegibles");
        motivoSustitucion = Objects.requireNonNull(motivoSustitucion, "motivoSustitucion");
        if (motivoSustitucion.filter(String::isBlank).isPresent()) throw new IllegalArgumentException("Motivo de sustitución vacío");
        if (fecha.isAfter(registro.fecha().atZone(ReporteIncidencia.ZONA_ESCOLAR).toLocalDate())
                || composicion.consultadaEn().isAfter(registro.fecha()))
            throw new IllegalArgumentException("Una convocatoria registrada no puede ser futura");
    }
}
