package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Explosion;
import fr.iutgon.sae401.common.model.entity.Position;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExplosionDTOTest {

    @Test
    void fromExplosion_andJsonRoundTrip() {
        Explosion explosion = new Explosion(List.of(new Position(4, 5), new Position(5, 5)), 1000);
        ExplosionDTO dto = ExplosionDTO.from(explosion);

        Json json = dto.toJson();
        ExplosionDTO restored = ExplosionDTO.fromJson(json);

        assertEquals(2, restored.tiles.size());
        assertEquals(4, restored.tiles.get(0).getX());
        assertEquals(5, restored.tiles.get(0).getY());
    }

    @Test
    void binarySerialization_roundTrip() {
        ExplosionDTO dto = new ExplosionDTO(List.of(new Position(7, 8)));
        byte[] bytes = dto.toBytes();
        ExplosionDTO restored = ExplosionDTO.fromBytes(ByteBuffer.wrap(bytes));

        assertEquals(dto.tiles.size(), restored.tiles.size());
        assertEquals(dto.tiles.get(0).getX(), restored.tiles.get(0).getX());
        assertEquals(dto.tiles.get(0).getY(), restored.tiles.get(0).getY());
    }
}
