package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.gameplay.Lobby;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class LobbyDTO {

    public String id;
    public int playerCount;
    public int maxPlayers;
    public boolean inGame;
    public boolean hasPassword;

    public LobbyDTO(String id, int playerCount, int maxPlayers, boolean inGame, boolean hasPassword) {
        this.id = id;
        this.playerCount = playerCount;
        this.maxPlayers = maxPlayers;
        this.inGame = inGame;
        this.hasPassword = hasPassword;
    }

    public static LobbyDTO from(Lobby lobby) {
        return new LobbyDTO(
                lobby.getId(),
                lobby.getPlayers().size(),
                lobby.getMaxPlayers(),
                lobby.isInGame(),
                false
        );
    }

    /**
     * Reconstruit un LobbyDTO depuis JSON.
     */
    public static LobbyDTO fromJson(String json) {
        return fromJson(Json.parse(json));
    }

    public static LobbyDTO fromJson(Json json) {
        String id = json.at("id").getString();
        int playerCount = json.at("playerCount").getInt();
        int maxPlayers = json.at("maxPlayers").getInt();
        boolean inGame = json.at("inGame").getBoolean();
        boolean hasPassword = json.value("hasPassword", false);
        return new LobbyDTO(id, playerCount, maxPlayers, inGame, hasPassword);
    }

    public Json toJson() {
        return Json.object(java.util.Map.of(
                "id", Json.of(id),
                "playerCount", Json.of(playerCount),
                "maxPlayers", Json.of(maxPlayers),
                "inGame", Json.of(inGame),
                "hasPassword", Json.of(hasPassword)
        ));
    }

    /**
     * Reconstruit depuis binaire.
     */
    public static LobbyDTO fromBytes(ByteBuffer buffer) {

        int len = buffer.getInt();
        byte[] idBytes = new byte[len];
        buffer.get(idBytes);

        String id = new String(idBytes, StandardCharsets.UTF_8);
        int playerCount = buffer.getInt();
        int maxPlayers = buffer.getInt();
        boolean inGame = buffer.get() == 1;
        boolean hasPassword = buffer.get() == 1;

        return new LobbyDTO(id, playerCount, maxPlayers, inGame, hasPassword);
    }

    public byte[] toBytes() {
        byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + idBytes.length + 4 + 4 + 1 + 1);

        buffer.putInt(idBytes.length);
        buffer.put(idBytes);
        buffer.putInt(playerCount);
        buffer.putInt(maxPlayers);
        buffer.put((byte) (inGame ? 1 : 0));
        buffer.put((byte) (hasPassword ? 1 : 0));

        return buffer.array();
    }
}
