package fr.iutgon.sae401.common.json;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonStringifyTest {
    @Test
    void stringifyParsesBackToSameValue() {
        Json original = Json.object(Map.of(
                "n", Json.of(1),
                "s", Json.of("x"),
                "a", Json.array(List.of(Json.of(true), Json.nullValue()))
        ));

        String text = original.stringify();
        Json reparsed = Json.parse(text);

        assertEquals(1, reparsed.at("n").getInt());
        assertEquals("x", reparsed.at("s").getString());
        assertEquals(true, reparsed.at("a").at(0).getBoolean());
        assertEquals(true, reparsed.at("a").at(1).isNull());
    }

    @Test
    void stringifyEscapesStrings() {
        Json j = Json.of("a\n\t\\\"b");
        assertEquals("\"a\\n\\t\\\\\\\"b\"", j.stringify());
    }

    @Test
    void objectKeyOrderIsPreserved() {
        LinkedHashMap<String, Json> map = new LinkedHashMap<>();
        map.put("b", Json.of(2));
        map.put("a", Json.of(1));
        map.put("c", Json.of(3));

        Json j = Json.object(map);
        assertEquals("{\"b\":2,\"a\":1,\"c\":3}", j.stringify());
    }
}
