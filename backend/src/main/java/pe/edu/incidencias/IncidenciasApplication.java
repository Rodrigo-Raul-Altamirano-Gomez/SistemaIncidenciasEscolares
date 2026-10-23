package pe.edu.incidencias;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class IncidenciasApplication {
    public static void main(String[] args) { SpringApplication.run(IncidenciasApplication.class, args); }
    @Bean Clock clock() { return Clock.system(ZoneId.of("America/Lima")); }
}
