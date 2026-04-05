package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Bomb;
import fr.iutgon.sae401.common.model.entity.Position;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class BombDTOTest {

    @Test
    void fromBomb_andToJson_roundTrip() {
        Bomb bomb = new Bomb("bomb-1", new Position(1, 2), "p1", 1000, 3);
        BombDTO dto = BombDTO.from(bomb);

        Json json = dto.toJson();
        BombDTO restored = BombDTO.fromJson(json);

        assertEquals("bomb-1", restored.id);
        assertEquals(1, restored.x);
        assertEquals(2, restored.y);
        assertEquals(3, restored.range);
        assertEquals(1000, restored.timerMs);
    }

    @Test
    void fromBytes_roundTrip_preservesPayload() {
        BombDTO dto = new BombDTO("bomb-2", 0, 0, 2, 500);
        byte[] bytes = dto.toBytes();
        BombDTO restored = BombDTO.fromBytes(ByteBuffer.wrap(bytes));

        assertEquals(dto.id, restored.id);
        assertEquals(dto.range, restored.range);
        assertEquals(dto.timerMs, restored.timerMs);
    }
}
