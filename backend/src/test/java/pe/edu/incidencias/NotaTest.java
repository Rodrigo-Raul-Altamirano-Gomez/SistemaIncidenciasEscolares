package pe.edu.incidencias;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import pe.edu.incidencias.domain.Nota.*;
import pe.edu.incidencias.adapter.JdbcRegistro;
import pe.edu.incidencias.application.RegistroPort.*;

class NotaTest {
    private final Clock clock=Clock.fixed(Instant.parse("2026-10-23T15:00:00Z"),ZoneId.of("America/Lima"));
    private Entrada entrada(LocalDate date,boolean approx,LocalTime time,List<Participante> ps) { return new Entrada(2026,date,approx,time,"COTIDIANA","Relato ficticio",null,null,null,ps,null,null); }
    @Test void papelesIndependientesYDocumentoNoObligatorio() {
        var ps=List.of(new Participante(UUID.randomUUID(),Papel.ATRIBUIDA),new Participante(UUID.randomUUID(),Papel.TESTIGO),new Participante(UUID.randomUUID(),Papel.AFECTADO));
        assertDoesNotThrow(()->entrada(LocalDate.of(2026,10,1),false,null,ps).validar(clock,true));
        assertEquals(Papel.TESTIGO,ps.get(1).papel());
    }
    @Test void personaUnicaPorHecho() {
        UUID id=UUID.randomUUID(); var p=new Participante(id,Papel.DESCONOCIDO);
        assertThrows(IllegalArgumentException.class,()->entrada(LocalDate.of(2026,10,1),false,null,List.of(p,p)).validar(clock,true));
    }
    @Test void fechaYHoraFuturaRechazadas() {
        var ps=List.of(new Participante(UUID.randomUUID(),Papel.ATRIBUIDA));
        assertThrows(IllegalArgumentException.class,()->entrada(LocalDate.of(2026,10,24),false,null,ps).validar(clock,true));
        assertThrows(IllegalArgumentException.class,()->entrada(LocalDate.of(2026,10,23),false,LocalTime.of(11,0),ps).validar(clock,true));
    }
    @Test void aproximadaNoInventaHoraYPorClasificarNoAtribuye() {
        var ps=List.of(new Participante(UUID.randomUUID(),Papel.ATRIBUIDA));
        assertThrows(IllegalArgumentException.class,()->entrada(LocalDate.of(2026,10,1),true,LocalTime.NOON,ps).validar(clock,true));
        assertThrows(IllegalArgumentException.class,()->entrada(LocalDate.of(2026,10,1),false,null,ps).validar(clock,false));
    }
    @Test void normalizaSinAlterarOriginal() { assertEquals("alex munoz del rio",JdbcRegistro.normalizar("  ÁLEX   Muñoz del RÍO  ")); }
    @Test void fechaPasadaYGrupoNoObligatorio() {
        assertDoesNotThrow(()->entrada(LocalDate.of(2026,1,1),true,null,List.of(new Participante(UUID.randomUUID(),Papel.DESCONOCIDO))).validar(clock,false));
    }
    @Test void diferenciaDePapelConservaNombreYCodigoLegibles() {
        UUID id=UUID.randomUUID(); var date=LocalDate.of(2026,10,1);
        var before=entrada(date,false,null,List.of(new Participante(id,Papel.AFECTADO)));
        var after=entrada(date,false,null,List.of(new Participante(id,Papel.TESTIGO)));
        var oldBody=new Cuerpo(before,"Categoría ficticia","DISCIPLINARIA","LEVE",List.of(new Participacion(id,"Persona ficticia","FIC-TEST",null,null,"AFECTADO")));
        var newBody=new Cuerpo(after,"Categoría ficticia","DISCIPLINARIA","LEVE",List.of(new Participacion(id,"Persona ficticia","FIC-TEST",null,null,"TESTIGO")));
        var diff=JdbcRegistro.diff(oldBody,newBody).getFirst();
        assertEquals("participantes",diff.campo()); assertEquals(oldBody.participantes(),diff.antes()); assertEquals(newBody.participantes(),diff.despues());
    }
    @Test void compromisoConYSinAccionYPlazoOpcional() {
        var e=entrada(LocalDate.of(2026,10,1),false,null,List.of(new Participante(UUID.randomUUID(),Papel.ATRIBUIDA)));
        for(Accion action:Arrays.asList(null,new Accion("Conversación ficticia",LocalDate.of(2026,10,2),null))) {
            for(LocalDate due:Arrays.asList(null,LocalDate.of(2026,10,3))) {
                var input=new Entrada(e.anio(),e.fechaHecho(),false,null,e.categoriaId(),e.descripcion(),null,null,null,e.participantes(),action,new Compromiso("Acuerdo ficticio","Persona ficticia",due));
                assertDoesNotThrow(()->input.validar(clock,true));
            }
        }
        var invalid=new Entrada(e.anio(),e.fechaHecho(),false,null,e.categoriaId(),e.descripcion(),null,null,null,e.participantes(),null,new Compromiso("Acuerdo","Persona",LocalDate.of(2026,9,30)));
        assertThrows(IllegalArgumentException.class,()->invalid.validar(clock,true));
    }
    @Test void remanenteNoAutorizaAltaOrdinariaSinPersonasNiOmiteOtrasValidaciones() {
        var e=entrada(LocalDate.of(2026,10,1),false,null,List.of());
        assertThrows(IllegalArgumentException.class,()->e.validar(clock,true));
        assertDoesNotThrow(()->e.validarConParticipacionesRetiradas(clock,true));
        assertThrows(IllegalArgumentException.class,()->entrada(LocalDate.of(2026,10,24),false,null,List.of()).validarConParticipacionesRetiradas(clock,true));
    }
}
