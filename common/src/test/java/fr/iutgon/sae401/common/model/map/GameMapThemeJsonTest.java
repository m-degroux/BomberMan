package fr.iutgon.sae401.common.model.map;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameMapThemeJsonTest {

    @Test
    void toJson_includesTheme_andFromJsonRestoresIt() {
        GameMap map = new GameMap(3, 2, MapTheme.PERFECT);
        map.setTile(new Position(0, 0), new Tile(TileType.GROUND));
        map.setTile(new Position(1, 0), new Tile(TileType.WALL));
        map.setTile(new Position(2, 0), new Tile(TileType.DESTRUCTIBLE));

        String json = map.toJson().stringify();
        GameMap parsed = GameMap.fromJson(json);

        assertEquals(MapTheme.PERFECT, parsed.getTheme());
        assertEquals(3, parsed.getWidth());
        assertEquals(2, parsed.getHeight());
        assertEquals(TileType.GROUND, parsed.getTile(new Position(0, 0)).getType());
    }

    @Test
    void fromJson_acceptsLegacyArrayFormat() {
        // legacy format: [["GROUND","WALL"],["DESTRUCTIBLE","GROUND"]]
        Json legacy = Json.array(java.util.List.of(
                Json.array(java.util.List.of(Json.of("GROUND"), Json.of("WALL"))),
                Json.array(java.util.List.of(Json.of("DESTRUCTIBLE"), Json.of("GROUND")))
        ));

        GameMap parsed = GameMap.fromJson(legacy.stringify());
        assertEquals(MapTheme.CLASSIC, parsed.getTheme());
        assertEquals(2, parsed.getWidth());
        assertEquals(2, parsed.getHeight());
        assertEquals(TileType.WALL, parsed.getTile(new Position(1, 0)).getType());
    }
}
