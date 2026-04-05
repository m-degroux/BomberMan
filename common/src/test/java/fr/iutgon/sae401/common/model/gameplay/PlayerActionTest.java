package fr.iutgon.sae401.common.model.gameplay;

import fr.iutgon.sae401.common.model.entity.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerActionTest {

    @Test
    void getters_returnConstructorValues() {
        PlayerAction action = new PlayerAction("p1", ActionType.MOVE, Direction.LEFT);

        assertEquals("p1", action.getPlayerId());
        assertEquals(ActionType.MOVE, action.getType());
        assertEquals(Direction.LEFT, action.getDirection());
    }
}
