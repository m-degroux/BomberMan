package fr.iutgon.sae401.common.model.map;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameMapTest {

    @Test
    void setTile_andGetTile_workForInsideCoordinates() {
        GameMap map = new GameMap(3, 3, MapTheme.CLASSIC);
        Position pos = new Position(1, 1);
        Tile tile = new Tile(TileType.DESTRUCTIBLE);

        map.setTile(pos, tile);

        assertEquals(tile, map.getTile(pos));
        assertTrue(map.isInside(pos));
        assertFalse(map.isInside(new Position(3, 3)));
    }

    @Test
    void isWalkable_returnsFalseForNullOrNonGroundTile() {
        GameMap map = new GameMap(2, 2, MapTheme.CLASSIC);
        map.setTile(new Position(0, 0), new Tile(TileType.WALL));

        assertFalse(map.isWalkable(new Position(0, 0)));
        assertFalse(map.isWalkable(new Position(10, 0)));
    }

    @Test
    void toJson_andFromJson_roundTripWithTheme() {
        GameMap map = new GameMap(2, 1, MapTheme.PERFECT);
        map.setTile(new Position(0, 0), new Tile(TileType.GROUND));
        map.setTile(new Position(1, 0), new Tile(TileType.DESTRUCTIBLE));

        Json json = map.toJson();
        assertEquals("PERFECT", json.at("theme").getString());

        GameMap restored = GameMap.fromJson(json.stringify(), MapTheme.CLASSIC);
        assertEquals(MapTheme.PERFECT, restored.getTheme());
        assertEquals(TileType.GROUND, restored.getTile(new Position(0, 0)).getType());
        assertEquals(TileType.DESTRUCTIBLE, restored.getTile(new Position(1, 0)).getType());
    }

    @Test
    void fromJson_acceptsLegacyArrayFormat() {
        Json legacy = Json.array(java.util.List.of(
                Json.array(java.util.List.of(Json.of("GROUND"), Json.of("WALL")))
        ));

        GameMap map = GameMap.fromJson(legacy.stringify(), MapTheme.CLASSIC);
        assertEquals(2, map.getWidth());
        assertEquals(1, map.getHeight());
        assertEquals(TileType.GROUND, map.getTile(new Position(0, 0)).getType());
    }
}
