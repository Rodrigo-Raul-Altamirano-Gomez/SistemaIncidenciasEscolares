package pe.edu.incidencias.domain.acceso;

/** Operaciones de negocio; los derivados requieren el mismo ámbito que la consulta directa. */
public enum Permiso {
    BUSCAR_ESTUDIANTE(false), REGISTRAR_INCIDENCIA(false), IMPORTAR_NOMINA(false),
    ADMINISTRAR_USUARIOS(false), DEFINIR_CONFIGURACION(false),
    CONSULTAR_APORTE_PROPIO(true), CORREGIR_APORTE_PROPIO(true), ACLARAR_APORTE_PROPIO(true),
    REGISTRAR_ACCION_INMEDIATA(true), CONSULTAR_HECHO(true), ATENDER(true),
    ACLARAR_PARTICIPACION(true), CORREGIR_HECHO(true), RELACIONAR_REPORTES(true),
    CONSULTAR_CONTEXTO_INSTITUCIONAL(true), CONSULTAR_DOCUMENTO_RESERVADO(true),
    GENERAR_INFORME(true), CONSULTAR_ESTADISTICAS(true), EXPORTAR_ATENCION(true),
    CONVOCAR_COMITE(true), REGISTRAR_ACTO_COMITE(true), EXCLUIR_USUARIO(true), RETIRAR_EXCLUSION(true);

    private final boolean requiereAmbito;
    Permiso(boolean requiereAmbito) { this.requiereAmbito = requiereAmbito; }
    public boolean requiereAmbito() { return requiereAmbito; }
}
