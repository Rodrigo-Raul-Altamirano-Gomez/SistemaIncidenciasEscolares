package pe.edu.incidencias.domain.incidencia;

import pe.edu.incidencias.domain.estudiante.GrupoEscolar;

import java.util.Objects;
import java.util.Optional;

public record Participacion(PersonaInvolucrada persona, PapelParticipacion papel,
                            Optional<GrupoEscolar> grupoHistorico) {
    public Participacion {
        Objects.requireNonNull(persona, "persona");
        Objects.requireNonNull(papel, "papel");
        grupoHistorico = Objects.requireNonNull(grupoHistorico, "grupoHistorico");
        if (grupoHistorico.isPresent() && persona.tipo() != TipoPersona.ESTUDIANTE)
            throw new IllegalArgumentException("El grupo corresponde solo al estudiante identificado");
        if (papel == PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA
                && persona.tipo() != TipoPersona.ESTUDIANTE)
            throw new IllegalArgumentException("La atribución cotidiana requiere identificar al estudiante");
    }
}
