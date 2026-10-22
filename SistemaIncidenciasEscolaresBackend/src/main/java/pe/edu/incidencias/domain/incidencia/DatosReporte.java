package pe.edu.incidencias.domain.incidencia;

import pe.edu.incidencias.domain.catalogo.ClasificacionCategoria;
import pe.edu.incidencias.domain.catalogo.Gravedad;
import pe.edu.incidencias.domain.catalogo.NaturalezaCategoria;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record DatosReporte(FechaHecho fechaHecho, int anioEscolar, ClasificacionCategoria clasificacion,
                           String descripcion, List<Participacion> participaciones,
                           boolean solicitaAtencion, Optional<LocalTime> hora,
                           Optional<String> lugar, Optional<String> comunicante, Optional<String> canal) {
    public DatosReporte {
        Objects.requireNonNull(fechaHecho, "fechaHecho");
        if (anioEscolar < 1 || anioEscolar > 9999) throw new IllegalArgumentException("Año escolar inválido");
        Objects.requireNonNull(clasificacion, "clasificacion");
        Objects.requireNonNull(descripcion, "descripcion");
        if (descripcion.isBlank()) throw new IllegalArgumentException("Se necesita la descripción del hecho");
        participaciones = List.copyOf(participaciones);
        if (participaciones.isEmpty()) throw new IllegalArgumentException("Se necesita al menos una persona o referencia provisional");
        var personas = new HashSet<UUID>();
        var cuentas = new HashSet<UUID>();
        for (var participacion : participaciones) {
            if (!personas.add(participacion.persona().id()))
                throw new IllegalArgumentException("Una persona no puede repetirse dentro del reporte");
            validarPapel(participacion.papel(), participacion.persona(), clasificacion);
            if (participacion.persona().usuarioId().isPresent()
                    && !cuentas.add(participacion.persona().usuarioId().get()))
                throw new IllegalArgumentException("Una cuenta de personal identifica una sola persona por reporte");
        }
        hora = Objects.requireNonNull(hora, "hora");
        if (fechaHecho.aproximada() && hora.isPresent())
            throw new IllegalArgumentException("Una fecha aproximada no admite una hora exacta");
        lugar = textoOpcional(lugar);
        comunicante = textoOpcional(comunicante);
        canal = textoOpcional(canal);
    }

    static void validarPapel(PapelParticipacion papel, PersonaInvolucrada persona,
                            ClasificacionCategoria clasificacion) {
        if (papel == PapelParticipacion.CONDUCTA_COTIDIANA_ATRIBUIDA
                && (persona.tipo() != TipoPersona.ESTUDIANTE
                || clasificacion.naturaleza() != NaturalezaCategoria.CONDUCTA_DISCIPLINARIA
                || clasificacion.gravedad() != Gravedad.LEVE))
            throw new IllegalArgumentException("La atribución cotidiana requiere conducta disciplinaria leve");
    }

    private static Optional<String> textoOpcional(Optional<String> texto) {
        Objects.requireNonNull(texto, "texto opcional");
        if (texto.filter(String::isBlank).isPresent()) throw new IllegalArgumentException("Texto opcional vacío");
        return texto;
    }
}
