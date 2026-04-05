package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Player;
import fr.iutgon.sae401.common.model.entity.Position;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDTOTest {

    @Test
    void fromPlayer_andToJson_roundTrip() {
        Player player = new Player("p1", new Position(2, 3), 4, 2);
        player.setNickname("Alice");
        player.useBomb(1.0);

        PlayerDTO dto = PlayerDTO.from(player);
        Json json = dto.toJson();
        PlayerDTO restored = PlayerDTO.fromJson(json);

        assertEquals("p1", restored.id);
        assertEquals(2, restored.x);
        assertEquals(3, restored.y);
        assertEquals(true, restored.alive);
        assertEquals(4, restored.health);
        assertEquals("Alice", restored.nickname);
        assertEquals(1, restored.bombRange);
    }

    @Test
    void fromBytes_roundTrip_preservesPayload() {
        PlayerDTO dto = new PlayerDTO("p2", 1, 1, true, 2, 1, "Bob", 1.5f, 2, 2);
        byte[] data = dto.toBytes();
        PlayerDTO restored = PlayerDTO.fromBytes(ByteBuffer.wrap(data));

        assertEquals(dto.id, restored.id);
        assertEquals(dto.x, restored.x);
        assertEquals(dto.y, restored.y);
        assertEquals(dto.bombs, restored.bombs);
        assertEquals(dto.nickname, restored.nickname);
        assertEquals(dto.speed, restored.speed);
        assertEquals(dto.bombRange, restored.bombRange);
    }

    @Test
    void fromJson_usesDefaultsWhenOptionalFieldsAreMissing() {
        Json json = Json.object(java.util.Map.of(
                "id", Json.of("p3"),
                "x", Json.of(0),
                "y", Json.of(0),
                "alive", Json.of(true),
                "health", Json.of(3),
                "bombs", Json.of(1)
        ));

        PlayerDTO dto = PlayerDTO.fromJson(json);

        assertEquals("p3", dto.id);
        assertEquals("p3", dto.nickname);
        assertEquals(1.0f, dto.speed);
        assertEquals(1, dto.bombRange);
        assertEquals(1, dto.maxBombs);
    }
}
