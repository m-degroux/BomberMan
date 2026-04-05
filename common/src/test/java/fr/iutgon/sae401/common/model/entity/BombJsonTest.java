package fr.iutgon.sae401.common.model.entity;

import fr.iutgon.sae401.common.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BombJsonTest {

    @Test
    void fromJson_acceptsNestedPositionFormat() {
        Json j = Json.object(java.util.Map.of(
                "id", Json.of("b-1"),
                "position", Json.object(java.util.Map.of("x", Json.of(3), "y", Json.of(4))),
                "owner", Json.of("p1"),
                "timerMs", Json.of(1500),
                "range", Json.of(2)
        ));

        Bomb b = Bomb.fromJson(j);
        assertEquals("b-1", b.getBombId());
        assertEquals("p1", b.getOwnerId());
        assertEquals(2, b.getRange());
        assertEquals(new Position(3, 4), b.getPosition());
        assertTrue(b.getRemainingMsCeil() <= 1500);
    }

    @Test
    void fromJson_acceptsFlatWireFormat() {
        Json j = Json.object(java.util.Map.of(
                "id", Json.of("b-2"),
                "x", Json.of(5),
                "y", Json.of(6),
                "timerMs", Json.of(900),
                "range", Json.of(3)
        ));

        Bomb b = Bomb.fromJson(j);
        assertEquals("b-2", b.getBombId());
        assertEquals(new Position(5, 6), b.getPosition());
        assertEquals(3, b.getRange());
        assertNotNull(b.getOwnerId());
    }

    @Test
    void toJson_containsIdAndTimerMs() {
        Bomb b = new Bomb("b-3", new Position(1, 2), "p2", 1000, 1);
        Json j = b.toJson();
        assertEquals("b-3", j.at("id").getString());
        assertTrue(j.at("timerMs").getInt() > 0);
        assertTrue(j.at("position").isObject());
    }
}
