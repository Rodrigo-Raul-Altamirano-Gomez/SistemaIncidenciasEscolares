package pe.edu.incidencias.adapter;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.type.TypeReference;

public final class Json {
    public static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();
    private Json() {}
    public static String write(Object o) { return MAPPER.writeValueAsString(o); }
    public static <T> T read(String s, Class<T> type) { return MAPPER.readValue(s,type); }
    public static <T> T read(String s, TypeReference<T> type) { return MAPPER.readValue(s,type); }
}
