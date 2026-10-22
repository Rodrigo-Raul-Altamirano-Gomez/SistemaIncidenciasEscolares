package pe.edu.incidencias.domain.documento;

import java.util.Objects;
import java.util.Optional;

public record DestinoInforme(Tipo tipo, Optional<String> destinatario, String finalidad) {
    public enum Tipo { INTERNO, TERCERO }
    public DestinoInforme {
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(destinatario);
        if (finalidad == null || finalidad.isBlank() || destinatario.filter(String::isBlank).isPresent())
            throw new IllegalArgumentException("Finalidad/destinatario informados no pueden estar vacíos");
        if ((tipo == Tipo.TERCERO) != destinatario.isPresent()) throw new IllegalArgumentException("Solo la salida a tercero precisa destinatario");
    }
    public static DestinoInforme interno() {
        return new DestinoInforme(Tipo.INTERNO, Optional.empty(), "Atención institucional del caso");
    }
    public static DestinoInforme tercero(String destinatario, String finalidad) {
        return new DestinoInforme(Tipo.TERCERO, Optional.of(destinatario), finalidad);
    }
}
