package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.bonus.BonusType;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameStateUpdateDTOTest {

    @Test
    void toJson_andFromJson_roundTrip() {
        GameStateUpdateDTO dto = new GameStateUpdateDTO(
                List.of(new PlayerDTO("p1", 1, 1, true, 3, 1, "A", 1.0f, 2, 2)),
                List.of(new BombDTO("b1", 0, 0, 2, 1000)),
                List.of(new ExplosionDTO(List.of(new fr.iutgon.sae401.common.model.entity.Position(2, 2)))),
                List.of(new BonusPickupDTO(3, 3, BonusType.BOMB_RANGE))
        );

        Json json = dto.toJson();
        GameStateUpdateDTO restored = GameStateUpdateDTO.fromJson(json.stringify());

        assertEquals(1, restored.players.size());
        assertEquals("p1", restored.players.get(0).id);
        assertEquals(1, restored.bombs.size());
        assertEquals("b1", restored.bombs.get(0).id);
        assertEquals(1, restored.explosions.size());
        assertEquals(2, restored.explosions.get(0).tiles.get(0).getX());
        assertEquals(1, restored.bonuses.size());
        assertEquals(BonusType.BOMB_RANGE, restored.bonuses.get(0).type);
    }

    @Test
    void toBytes_andFromBytes_roundTrip() {
        GameStateUpdateDTO dto = new GameStateUpdateDTO(
                List.of(new PlayerDTO("p2", 4, 5, false, 1, 0, "B", 0.5f, 1, 1)),
                List.of(new BombDTO("b2", 1, 1, 1, 500)),
                List.of(new ExplosionDTO(List.of(new fr.iutgon.sae401.common.model.entity.Position(3, 3)))),
                List.of(new BonusPickupDTO(4, 4, BonusType.SPEED))
        );

        byte[] bytes = dto.toBytes();
        GameStateUpdateDTO restored = GameStateUpdateDTO.fromBytes(ByteBuffer.wrap(bytes));

        assertEquals(1, restored.players.size());
        assertEquals("p2", restored.players.get(0).id);
        assertEquals(1, restored.bombs.size());
        assertEquals("b2", restored.bombs.get(0).id);
        assertEquals(1, restored.explosions.size());
        assertEquals(3, restored.explosions.get(0).tiles.get(0).getX());
        assertEquals(1, restored.bonuses.size());
        assertEquals(BonusType.SPEED, restored.bonuses.get(0).type);
    }

    @Test
    void fromJson_missingArrays_returnsEmptyLists() {
        Json json = Json.object(java.util.Map.of(
                "players", Json.emptyArray(),
                "bombs", Json.emptyArray(),
                "explosions", Json.emptyArray(),
                "bonuses", Json.emptyArray()
        ));

        GameStateUpdateDTO restored = GameStateUpdateDTO.fromJson(json.stringify());

        assertNotNull(restored.players);
        assertTrue(restored.players.isEmpty());
        assertNotNull(restored.bombs);
        assertTrue(restored.bombs.isEmpty());
        assertNotNull(restored.explosions);
        assertTrue(restored.explosions.isEmpty());
        assertNotNull(restored.bonuses);
        assertTrue(restored.bonuses.isEmpty());
    }
}
