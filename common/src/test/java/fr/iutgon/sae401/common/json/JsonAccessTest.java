package fr.iutgon.sae401.common.json;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class JsonAccessTest {
    @Test
    void atThrowsWhenMissingKey() {
        Json j = Json.parse("{}");
        assertThrows(NoSuchElementException.class, () -> j.at("missing"));
    }

    @Test
    void atThrowsOnWrongContainerType() {
        Json j = Json.parse("[]");
        assertThrows(JsonTypeException.class, () -> j.at("x"));
    }

    @Test
    void valueReturnsDefaultWhenMissingOrNull() {
        Json j = Json.parse("{\"a\":null}");
        assertEquals("d", j.value("a", "d"));
        assertEquals(42, j.value("missing", 42));
        assertEquals(false, j.value("missing", false));
    }

    @Test
    void valueThrowsWhenPresentButWrongType() {
        Json j = Json.parse("{\"a\":1,\"b\":true,\"c\":\"x\"}");
        assertThrows(JsonTypeException.class, () -> j.value("a", "d"));
        assertThrows(JsonTypeException.class, () -> j.value("b", 0));
        assertThrows(JsonTypeException.class, () -> j.value("c", true));
    }

    @Test
    void containsAndGet() {
        Json j = Json.parse("{\"a\":1}");
        assertTrue(j.contains("a"));
        assertFalse(j.contains("b"));
        assertTrue(j.get("a").isPresent());
        assertTrue(j.get("b").isEmpty());
    }

    @Test
    void intLongExactness() {
        Json j = Json.parse("{\"x\":1.5}");
        assertThrows(JsonTypeException.class, () -> j.at("x").getInt());
        assertThrows(JsonTypeException.class, () -> j.at("x").getLong());
    }
}
