package fr.iutgon.sae401.common.model.entity;

import fr.iutgon.sae401.common.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {

    @Test
    void move_changesPosition() {
        Player player = new Player("p1", new Position(0, 0), 3, 2);
        player.move(Direction.RIGHT);

        assertEquals(new Position(1, 0), player.getPosition());
    }

    @Test
    void takeDamage_onlyKillsWhenHealthReachesZero() {
        Player player = new Player("p2", new Position(1, 1), 2, 1);
        player.takeDamage();

        assertTrue(player.isAlive());
        assertEquals(1, player.getHealth());

        player.takeDamage();

        assertFalse(player.isAlive());
        assertEquals(0, player.getHealth());
    }

    @Test
    void useBomb_reducesBombCountAndRestoresAfterCooldown() {
        Player player = new Player("p3", new Position(2, 2), 3, 2);

        assertTrue(player.canPlaceBomb());
        player.useBomb(1.0);

        assertEquals(1, player.getCurrentBombs());
        player.tickBombCooldowns(1.0);

        assertEquals(2, player.getCurrentBombs());
    }

    @Test
    void setNickname_ignoresBlankAndAcceptsValidValue() {
        Player player = new Player("p4", new Position(0, 0), 3, 1);
        player.setNickname("   ");

        assertEquals("p4", player.getNickname());

        player.setNickname("Bomber");
        assertEquals("Bomber", player.getNickname());
    }

    @Test
    void setBombRange_onlyAcceptsPositiveValues() {
        Player player = new Player("p5", new Position(0, 0), 3, 1);
        player.setBombRange(3);

        assertEquals(3, player.getBombRange());

        player.setBombRange(0);
        assertEquals(3, player.getBombRange());
    }

    @Test
    void toJson_andFromJson_preservesPlayerState() {
        Player original = new Player("p6", new Position(4, 4), 2, 2);
        original.setNickname("Alpha");
        original.useBomb(1.0);
        Json json = original.toJson();

        Player restored = Player.fromJson(json);

        assertEquals("p6", restored.getId());
        assertEquals("Alpha", restored.getNickname());
        assertEquals(original.getPosition(), restored.getPosition());
        assertEquals(original.getHealth(), restored.getHealth());
        assertEquals(original.isAlive(), restored.isAlive());
        assertEquals(original.getCurrentBombs(), restored.getCurrentBombs());
    }

    @Test
    void fromJson_marksDeadPlayerWhenAliveIsFalse() {
        Json j = Json.object(java.util.Map.of(
                "id", Json.of("p7"),
                "position", Json.object(java.util.Map.of("x", Json.of(0), "y", Json.of(0))),
                "health", Json.of(1),
                "alive", Json.of(false),
                "bombs", Json.of(1)
        ));

        Player player = Player.fromJson(j);

        assertFalse(player.isAlive());
    }
}
