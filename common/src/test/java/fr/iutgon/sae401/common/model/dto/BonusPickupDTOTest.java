package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.bonus.BonusType;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class BonusPickupDTOTest {

    @Test
    void fromJson_andJsonRoundTrip() {
        BonusPickupDTO dto = new BonusPickupDTO(3, 3, BonusType.BOMB_RANGE);

        Json json = dto.toJson();
        BonusPickupDTO restored = BonusPickupDTO.fromJson(json);

        assertEquals(3, restored.x);
        assertEquals(3, restored.y);
        assertEquals(BonusType.BOMB_RANGE, restored.type);
    }

    @Test
    void byteSerialization_roundTrip() {
        BonusPickupDTO dto = new BonusPickupDTO(2, 1, BonusType.SPEED);
        byte[] bytes = dto.toBytes();
        BonusPickupDTO restored = BonusPickupDTO.fromBytes(ByteBuffer.wrap(bytes));

        assertEquals(dto.x, restored.x);
        assertEquals(dto.y, restored.y);
        assertEquals(dto.type, restored.type);
    }
}
