package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.map.Tile;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameInitDTOTest {

    @Test
    void toJson_andFromJson_roundTrip() {
        GameMap map = new GameMap(2, 2, MapTheme.CLASSIC);
        map.setTile(new Position(0, 0), Tile.fromJson(Json.of("GROUND")));
        map.setTile(new Position(1, 1), Tile.fromJson(Json.of("WALL")));

        GameInitDTO dto = new GameInitDTO(map, List.of(new PlayerDTO("p1", 0, 0, true, 3, 1, "A", 1.0f, 2, 2)));
        Json json = dto.toJson();
        GameInitDTO restored = GameInitDTO.fromJson(json.stringify());

        assertNotNull(restored.map);
        assertEquals(MapTheme.CLASSIC, restored.map.getTheme());
        assertEquals(2, restored.map.getWidth());
        assertEquals(2, restored.map.getHeight());
        assertEquals(1, restored.players.size());
        assertEquals("p1", restored.players.get(0).id);
    }

    @Test
    void toBytes_andFromBytes_roundTrip() {
        GameMap map = new GameMap(1, 1, MapTheme.CLASSIC);
        map.setTile(new Position(0, 0), Tile.fromJson(Json.of("GROUND")));

        GameInitDTO dto = new GameInitDTO(map, List.of(new PlayerDTO("p2", 1, 1, false, 1, 0, "B", 1.0f, 1, 1)));
        byte[] bytes = dto.toBytes();
        GameInitDTO restored = GameInitDTO.fromBytes(ByteBuffer.wrap(bytes));

        assertNotNull(restored.map);
        assertEquals(1, restored.players.size());
        assertEquals("p2", restored.players.get(0).id);
    }
}
