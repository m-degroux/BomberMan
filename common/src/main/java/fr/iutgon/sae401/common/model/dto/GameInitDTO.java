package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.map.GameMap;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO envoyé au début de la partie.
 */

public class GameInitDTO {

    public GameMap map;
    public List<PlayerDTO> players;

    public GameInitDTO(GameMap map, List<PlayerDTO> players) {
        this.map = map;
        this.players = players;
    }

    public static GameInitDTO fromJson(String json) {
        Json j = Json.parse(json);
        Json mapJson = j.at("map");
        GameMap map;
        if (mapJson.isString()) {
            map = GameMap.fromJson(mapJson.getString());
        } else {
            map = GameMap.fromJson(mapJson.stringify());
        }
        Json playersJson = j.value("players", Json.emptyArray());
        List<PlayerDTO> players = new java.util.ArrayList<>();
        if (playersJson.isArray()) {
            for (Json p : playersJson.asArray()) {
                players.add(PlayerDTO.fromJson(p));
            }
        }
        return new GameInitDTO(map, players);
    }

    public static GameInitDTO fromBytes(ByteBuffer buffer) {

        int mapLen = buffer.getInt();
        byte[] mapBytes = new byte[mapLen];
        buffer.get(mapBytes);

    GameMap map = GameMap.fromJson(new String(mapBytes, StandardCharsets.UTF_8));

        int size = buffer.getInt();
        List<PlayerDTO> players = new java.util.ArrayList<>();

        for (int i = 0; i < size; i++) {
            players.add(PlayerDTO.fromBytes(buffer));
        }

        return new GameInitDTO(map, players);
    }

    public Json toJson() {
        List<Json> p = new ArrayList<>();
        for (PlayerDTO dto : players) {
            p.add(dto.toJson());
        }
		return Json.object(java.util.Map.of(
				"map", map == null ? Json.nullValue() : map.toJson(),
				"players", Json.array(p)
		));
    }

    public byte[] toBytes() {
        byte[] mapBytes = (map == null ? Json.nullValue() : map.toJson()).stringify().getBytes(StandardCharsets.UTF_8);
        int playersBytesLen = 0;
        byte[][] playersBytes = new byte[players.size()][];
        for (int i = 0; i < players.size(); i++) {
            byte[] pb = players.get(i).toBytes();
            playersBytes[i] = pb;
            playersBytesLen += pb.length;
        }

        int totalLen = 4 + mapBytes.length + 4 + playersBytesLen;
        ByteBuffer buffer = ByteBuffer.allocate(totalLen);

        buffer.putInt(mapBytes.length);
        buffer.put(mapBytes);
        buffer.putInt(players.size());
        for (byte[] pb : playersBytes) {
            buffer.put(pb);
        }
        return buffer.array();
    }
}