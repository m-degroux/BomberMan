package fr.iutgon.sae401.common.model.entity;

import fr.iutgon.sae401.common.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BombTest {

    @Test
    void tick_reducesRemainingMilliseconds() {
        Bomb bomb = new Bomb("bomb-1", new Position(1, 1), "p1", 1000, 2);

        bomb.tick(0.25);

        assertTrue(bomb.getRemainingMsCeil() < 1000);
        assertFalse(bomb.isExploded());
    }

    @Test
    void isExploded_returnsTrueAfterTimerExpires() {
        Bomb bomb = new Bomb("bomb-2", new Position(0, 0), "p2", 100, 1);

        bomb.tick(0.2);

        assertTrue(bomb.isExploded());
    }

    @Test
    void createUnique_generatesNonNullId() {
        Bomb bomb = Bomb.createUnique(new Position(2, 2), "p3", 1200, 3);

        assertNotNull(bomb.getBombId());
        assertEquals(new Position(2, 2), bomb.getPosition());
        assertEquals("p3", bomb.getOwnerId());
    }

    @Test
    void fromJson_acceptsLegacyAndModernFormats() {
        Json nested = Json.object(java.util.Map.of(
                "id", Json.of("b-1"),
                "position", Json.object(java.util.Map.of("x", Json.of(3), "y", Json.of(4))),
                "owner", Json.of("p1"),
                "timerMs", Json.of(1500),
                "range", Json.of(2)
        ));

        Bomb nestedBomb = Bomb.fromJson(nested);
        assertEquals("b-1", nestedBomb.getBombId());
        assertEquals(new Position(3, 4), nestedBomb.getPosition());

        Json flat = Json.object(java.util.Map.of(
                "id", Json.of("b-2"),
                "x", Json.of(5),
                "y", Json.of(6),
                "timerMs", Json.of(900),
                "range", Json.of(3)
        ));

        Bomb flatBomb = Bomb.fromJson(flat);
        assertEquals(new Position(5, 6), flatBomb.getPosition());
        assertEquals(3, flatBomb.getRange());
    }

    @Test
    void toJson_containsExpectedFields() {
        Bomb bomb = new Bomb("bomb-3", new Position(1, 2), "p4", 1000, 4);
        Json json = bomb.toJson();

        assertEquals("bomb-3", json.at("id").getString());
        assertEquals(4, json.at("range").getInt());
        assertEquals(1, json.at("position").at("x").getInt());
    }
}
