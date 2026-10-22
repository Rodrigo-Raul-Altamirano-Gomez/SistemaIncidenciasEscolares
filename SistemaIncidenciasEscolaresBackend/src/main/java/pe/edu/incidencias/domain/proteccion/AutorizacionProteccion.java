package pe.edu.incidencias.domain.proteccion;

import pe.edu.incidencias.domain.acceso.DirectorioUsuarios;
import pe.edu.incidencias.domain.acceso.Vigencia;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Mandato comprobado por el canal institucional; no es un rol ni una autoautorización del operador. */
public record AutorizacionProteccion(UUID id, UUID operadorId, Set<Operacion> operaciones,
                                     AlcanceProteccion alcance, Vigencia vigencia,
                                     String autoridad, String constancia, String fundamento, Instant otorgadaEn) {
    public enum Operacion { BLOQUEAR, LEVANTAR_BLOQUEO, DISPONER, EXTRAER_EVIDENCIA }
    public AutorizacionProteccion {
        Objects.requireNonNull(id); Objects.requireNonNull(operadorId); Objects.requireNonNull(alcance);
        Objects.requireNonNull(vigencia); Objects.requireNonNull(otorgadaEn);
        operaciones = Set.copyOf(operaciones);
        if (operaciones.isEmpty()) throw new IllegalArgumentException("La autorización requiere operaciones expresas");
        autoridad = texto(autoridad); constancia = texto(constancia); fundamento = texto(fundamento);
    }
    public void exigir(UUID operador, Operacion operacion, AlcanceProteccion solicitado,
                        Instant ahora, DirectorioUsuarios directorio) {
        if (!operadorId.equals(operador) || !operaciones.contains(operacion)
                || otorgadaEn.isAfter(ahora) || !vigencia.contiene(ahora) || !alcance.contiene(solicitado))
            throw new SecurityException("La operación excede la autorización institucional específica");
        if (directorio.usuario(operador).filter(u -> u.activaEn(ahora)).isEmpty())
            throw new SecurityException("El operador autorizado debe tener cuenta vigente");
    }
    private static String texto(String valor) {
        if (Objects.requireNonNull(valor).isBlank()) throw new IllegalArgumentException("Falta constancia, autoridad o fundamento");
        return valor.strip();
    }
}
