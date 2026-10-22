package pe.edu.incidencias.domain.consulta;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Consulta declarativa limitada a variables registradas; nunca analiza relatos. */
public record ConsultaEstadistica(Medida medida, Periodo periodo, List<Dimension> agrupaciones,
                                  Map<Dimension, Set<String>> filtros) {
    public enum Dimension {
        ANIO, MES, DIA_SEMANA, HORA, ANIO_ESCOLAR, CATEGORIA, NATURALEZA, GRAVEDAD,
        GRADO, SECCION, SEXO, TIPO_PERSONA, PAPEL, ATRIBUCION, ESTADO_ATENCION,
        MOTIVO_ALERTA, ENTIDAD_DERIVACION, INTERVENCION_COMITE, PRECISION_FECHA, TIPO_CIERRE
    }
    public enum Medida {
        HECHOS("hechos únicos", "fecha del hecho"),
        COMUNICACIONES("comunicaciones válidas", "fecha de registro"),
        PARTICIPACIONES("personas por hecho; incluye estudiantes y otras personas", "fecha del hecho"),
        ESTUDIANTES("estudiantes distintos involucrados", "fecha del hecho"),
        ESTUDIANTES_COTIDIANA("estudiantes con conducta cotidiana atribuida", "fecha del hecho"),
        ESTUDIANTES_PRESUNTA("estudiantes con presunta responsabilidad", "fecha del hecho"),
        ESTUDIANTES_CORROBORADA("estudiantes con atribución corroborada", "fecha del hecho"),
        ESTUDIANTES_AFECTADOS("estudiantes afectados", "fecha del hecho"),
        ESTUDIANTES_TESTIGOS("estudiantes testigos", "fecha del hecho"),
        CASOS_PENDIENTES("casos pendientes", "apertura"),
        CASOS_CERRADOS("casos cerrados; incluye cierres administrativos identificados", "cierre"),
        CASOS_RESUELTOS("atenciones concluidas; excluye apertura errónea y duplicidad", "cierre"),
        ALERTAS_EMITIDAS("señales emitidas", "emisión"),
        ALERTAS_GESTIONADAS("señales gestionadas", "emisión"),
        ALERTAS_PENDIENTES("señales pendientes", "emisión"),
        ACTUACIONES_REALIZADAS("actuaciones realizadas", "fecha efectiva"),
        ACTUACIONES_PREVISTAS("actuaciones previstas", "fecha prevista"),
        COMPROMISOS("compromisos no cancelados", "acuerdo"),
        COMPROMISOS_PENDIENTES("compromisos con seguimiento pendiente", "acuerdo"),
        COMPROMISOS_COMPROBADOS("compromisos con cumplimiento observado", "última observación de cumplimiento"),
        SEGUIMIENTOS_PENDIENTES("seguimientos pendientes de compromisos", "acuerdo"),
        SEGUIMIENTOS_COMPROBADOS("observaciones de cumplimiento", "observación"),
        DERIVACIONES_EFECTIVAS("derivaciones efectuadas", "fecha efectiva");

        private final String unidad;
        private final String criterioTemporal;
        Medida(String unidad, String criterioTemporal) { this.unidad = unidad; this.criterioTemporal = criterioTemporal; }
        public String unidad() { return unidad; }
        public String criterioTemporal() { return criterioTemporal; }
        public boolean estudiantes() { return name().startsWith("ESTUDIANTES"); }
    }
    public record Periodo(LocalDate desde, LocalDate hasta, boolean incluirFechaNoPrecisada) {
        public Periodo {
            Objects.requireNonNull(desde, "desde");
            Objects.requireNonNull(hasta, "hasta");
            if (hasta.isBefore(desde)) throw new IllegalArgumentException("Período invertido");
        }
        public boolean contiene(LocalDate fecha) { return !fecha.isBefore(desde) && !fecha.isAfter(hasta); }
    }
    public ConsultaEstadistica {
        Objects.requireNonNull(medida, "medida");
        Objects.requireNonNull(periodo, "periodo");
        agrupaciones = List.copyOf(agrupaciones);
        if (agrupaciones.stream().distinct().count() != agrupaciones.size())
            throw new IllegalArgumentException("Dimensión repetida");
        var copia = new java.util.EnumMap<Dimension, Set<String>>(Dimension.class);
        filtros.forEach((dimension, valores) -> {
            Objects.requireNonNull(dimension, "dimension");
            if (valores.isEmpty() || valores.stream().anyMatch(v -> v == null || v.isBlank()))
                throw new IllegalArgumentException("Filtro vacío");
            copia.put(dimension, Set.copyOf(valores));
        });
        filtros = Map.copyOf(copia);
        for (var dimension : java.util.stream.Stream.concat(agrupaciones.stream(), filtros.keySet().stream()).toList())
            if (!pertinente(medida, dimension)) throw new IllegalArgumentException("Dimensión no pertinente: " + dimension);
    }
    public static ConsultaEstadistica de(Medida medida, Periodo periodo) {
        return new ConsultaEstadistica(medida, periodo, List.of(), Map.of());
    }
    public ConsultaEstadistica en(Periodo otro) { return new ConsultaEstadistica(medida, otro, agrupaciones, filtros); }
    private static boolean pertinente(Medida medida, Dimension dimension) {
        if (dimension == Dimension.MOTIVO_ALERTA) return medida.name().startsWith("ALERTAS");
        if (dimension == Dimension.ENTIDAD_DERIVACION) return medida == Medida.DERIVACIONES_EFECTIVAS;
        if (dimension == Dimension.TIPO_CIERRE) return medida.name().startsWith("CASOS");
        if (dimension == Dimension.HORA) return medida == Medida.HECHOS || medida == Medida.PARTICIPACIONES || medida.estudiantes();
        return true;
    }
}
