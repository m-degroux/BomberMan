package fr.iutgon.sae401.common.model.gameplay;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Player;
import fr.iutgon.sae401.common.model.entity.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LobbyTest {

    @Test
    void addPlayer_limitsByMaxPlayers_andPreventsJoinAfterStart() {
        Lobby lobby = new Lobby("room", 2);
        Player p1 = new Player("p1", new Position(0, 0), 1, 1);
        Player p2 = new Player("p2", new Position(1, 0), 1, 1);
        Player p3 = new Player("p3", new Position(2, 0), 1, 1);

        assertTrue(lobby.addPlayer(p1));
        assertTrue(lobby.addPlayer(p2));
        assertFalse(lobby.addPlayer(p3));

        lobby.startGame();
        assertFalse(lobby.addPlayer(p3));
        assertTrue(lobby.isInGame());
    }

    @Test
    void setReady_andAllReady_workWhenAllPlayersAreReady() {
        Lobby lobby = new Lobby("room2", 2);
        Player p1 = new Player("p1", new Position(0, 0), 1, 1);
        Player p2 = new Player("p2", new Position(1, 1), 1, 1);

        lobby.addPlayer(p1);
        lobby.addPlayer(p2);
        lobby.setReady("p1", true);
        lobby.setReady("p2", true);

        assertTrue(lobby.allReady());

        lobby.setReady("p2", false);
        assertFalse(lobby.allReady());
    }

    @Test
    void toJson_andFromJson_roundTripPreservesPlayersAndReadyState() {
        Lobby lobby = new Lobby("lobby1", 3);
        Player p1 = new Player("p1", new Position(0, 0), 1, 1);
        Player p2 = new Player("p2", new Position(1, 1), 1, 1);
        lobby.addPlayer(p1);
        lobby.addPlayer(p2);
        lobby.setReady("p1", true);
        lobby.startGame();

        Json json = lobby.toJson();
        Lobby restored = Lobby.fromJson(json);

        assertEquals("lobby1", restored.getId());
        assertEquals(2, restored.getPlayers().size());
        assertTrue(restored.isInGame());
        assertTrue(restored.getReadyPlayers().contains("p1"));
    }
}
