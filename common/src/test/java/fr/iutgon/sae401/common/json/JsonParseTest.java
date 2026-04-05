package fr.iutgon.sae401.common.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonParseTest {
    @Test
    void parsesNullTrueFalse() {
        assertTrue(Json.parse("null").isNull());
        assertTrue(Json.parse("true").isBoolean());
        assertTrue(Json.parse("false").isBoolean());
        assertEquals(true, Json.parse("true").getBoolean());
        assertEquals(false, Json.parse("false").getBoolean());
    }

    @Test
    void parsesWhitespaceAroundValue() {
        Json j = Json.parse(" \n\t { \"a\" : 1 } \r ");
        assertEquals(1, j.at("a").getInt());
    }

    @Test
    void parsesStringWithEscapes() {
        Json j = Json.parse("\"a\\n\\t\\\\\\\"b\"");
        assertEquals("a\n\t\\\"b", j.getString());
    }

    @Test
    void parsesUnicodeEscape() {
        Json j = Json.parse("\"\\u263A\"");
        assertEquals("\u263A", j.getString());
    }

    @Test
    void parsesNumbers() {
        assertEquals(0, Json.parse("0").getInt());
        assertEquals(-12, Json.parse("-12").getInt());
        assertEquals(1200L, Json.parse("1.2e3").getLong());
        assertEquals(0.25, Json.parse("2.5e-1").getDouble(), 1e-12);
    }

    @Test
    void parsesArrayAndObject() {
        Json j = Json.parse("{\"arr\":[1,2,3],\"s\":\"x\"}");
        assertEquals(2, j.at("arr").at(1).getInt());
        assertEquals("x", j.at("s").getString());
    }

    @Test
    void rejectsTrailingCharacters() {
        assertThrows(JsonParseException.class, () -> Json.parse("true false"));
    }

    @Test
    void rejectsUnterminatedString() {
        assertThrows(JsonParseException.class, () -> Json.parse("\"abc"));
    }

    @Test
    void rejectsMissingCommaInArray() {
        assertThrows(JsonParseException.class, () -> Json.parse("[1 2]"));
    }

    @Test
    void rejectsInvalidNumber() {
        assertThrows(JsonParseException.class, () -> Json.parse("01"));
        assertThrows(JsonParseException.class, () -> Json.parse("-"));
        assertThrows(JsonParseException.class, () -> Json.parse("1."));
        assertThrows(JsonParseException.class, () -> Json.parse("1e"));
    }
}
