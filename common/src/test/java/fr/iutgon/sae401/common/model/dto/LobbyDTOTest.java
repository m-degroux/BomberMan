package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.gameplay.Lobby;
import fr.iutgon.sae401.common.model.entity.Player;
import fr.iutgon.sae401.common.model.entity.Position;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class LobbyDTOTest {

    @Test
    void fromLobby_andJsonRoundTrip() {
        Lobby lobby = new Lobby("lobby-1", 4);
        lobby.addPlayer(new Player("p1", new Position(0, 0), 3, 1));
        LobbyDTO dto = LobbyDTO.from(lobby);

        Json json = dto.toJson();
        LobbyDTO restored = LobbyDTO.fromJson(json);

        assertEquals("lobby-1", restored.id);
        assertEquals(1, restored.playerCount);
        assertEquals(4, restored.maxPlayers);
        assertFalse(restored.inGame);
    }

    @Test
    void binaryRoundTrip() {
        LobbyDTO dto = new LobbyDTO("lobby-2", 2, 6, false, false);
        byte[] bytes = dto.toBytes();
        LobbyDTO restored = LobbyDTO.fromBytes(ByteBuffer.wrap(bytes));

        assertEquals(dto.id, restored.id);
        assertEquals(dto.playerCount, restored.playerCount);
        assertEquals(dto.maxPlayers, restored.maxPlayers);
        assertEquals(dto.inGame, restored.inGame);
    }
}
