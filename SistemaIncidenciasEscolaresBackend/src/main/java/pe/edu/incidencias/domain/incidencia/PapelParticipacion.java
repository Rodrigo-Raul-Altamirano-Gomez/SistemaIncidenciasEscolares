package pe.edu.incidencias.domain.incidencia;

public enum PapelParticipacion {
    CONDUCTA_COTIDIANA_ATRIBUIDA, PRESUNTA_RESPONSABILIDAD, ATRIBUCION_CORROBORADA,
    AFECTADO, TESTIGO, OTRO, DESCONOCIDO, ATRIBUCION_DESCARTADA;

    public boolean elegibleParaReiteracion() {
        return this == CONDUCTA_COTIDIANA_ATRIBUIDA || this == PRESUNTA_RESPONSABILIDAD
                || this == ATRIBUCION_CORROBORADA;
    }
}
