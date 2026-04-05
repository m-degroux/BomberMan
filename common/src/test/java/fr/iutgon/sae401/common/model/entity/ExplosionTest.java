package fr.iutgon.sae401.common.model.entity;

import fr.iutgon.sae401.common.json.Json;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExplosionTest {

    @Test
    void tickReducesRemainingTimeAndMarksFinished() {
        Explosion explosion = new Explosion(List.of(new Position(1, 1)), 500);

        assertFalse(explosion.isFinished());
        explosion.tick(0.6);

        assertTrue(explosion.isFinished());
    }

    @Test
    void contains_returnsTrueWhenTileIsAffected() {
        Position tile = new Position(2, 3);
        Explosion explosion = new Explosion(List.of(tile), 1000);

        assertTrue(explosion.contains(tile));
        assertFalse(explosion.contains(new Position(0, 0)));
    }

    @Test
    void toJson_includesOriginAndTiles() {
        Position origin = new Position(5, 5);
        Explosion explosion = new Explosion(origin, List.of(new Position(5, 5), new Position(6, 5)), 750);
        Json json = explosion.toJson();

        assertEquals(2, json.at("tiles").asArray().size());
        assertEquals(origin.getX(), json.at("origin").at("x").getInt());
    }

    @Test
    void fromJson_acceptsOriginObjectAndTileList() {
        Json json = Json.object(java.util.Map.of(
                "origin", Json.object(java.util.Map.of("x", Json.of(3), "y", Json.of(4))),
                "tiles", Json.array(List.of(Json.object(java.util.Map.of("x", Json.of(3), "y", Json.of(4))))),
                "durationMs", Json.of(1000)
        ));

        Explosion explosion = Explosion.fromJson(json);

        assertEquals(new Position(3, 4), explosion.getOrigin());
        assertEquals(1, explosion.getAffectedTiles().size());
    }
}
