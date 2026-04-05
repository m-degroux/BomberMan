package fr.iutgon.sae401.common.model.bonus;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BonusPickupTest {

    @Test
    void getPositionAndType_returnAssignedValues() {
        BonusPickup pickup = new BonusPickup(new Position(1, 2), BonusType.MAX_BOMBS);

        assertEquals(new Position(1, 2), pickup.getPosition());
        assertEquals(BonusType.MAX_BOMBS, pickup.getType());
    }

    @Test
    void toJson_containsExpectedFields() {
        BonusPickup pickup = new BonusPickup(new Position(2, 3), BonusType.SPEED);
        Json json = pickup.toJson();

        assertEquals(2, json.at("x").getInt());
        assertEquals(3, json.at("y").getInt());
        assertEquals("SPEED", json.at("type").getString());
    }
}
