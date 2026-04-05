package fr.iutgon.sae401.common.model.map;

import fr.iutgon.sae401.common.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TileTest {

    @Test
    void walkable_isOnlyGround() {
        assertTrue(new Tile(TileType.GROUND).isWalkable());
        assertFalse(new Tile(TileType.DESTRUCTIBLE).isWalkable());
        assertFalse(new Tile(TileType.WALL).isWalkable());
    }

    @Test
    void destructible_isOnlyDestructibleAndDestroyConvertsToGround() {
        Tile tile = new Tile(TileType.DESTRUCTIBLE);

        assertTrue(tile.isDestructible());
        tile.destroy();
        assertEquals(TileType.GROUND, tile.getType());
    }

    @Test
    void fromJson_acceptsStringValueAndFallbackPermissiveValue() {
        Tile fromString = Tile.fromJson(Json.of("WALL"));
        assertEquals(TileType.WALL, fromString.getType());

        Tile fromRawJson = Tile.fromJson(Json.parse("\"DESTRUCTIBLE\""));
        assertEquals(TileType.DESTRUCTIBLE, fromRawJson.getType());
    }

    @Test
    void toJson_returnsStringRepresentation() {
        Json json = new Tile(TileType.GROUND).toJson();

        assertTrue(json.isString());
        assertEquals("GROUND", json.getString());
    }
}
