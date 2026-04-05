package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.IPlayer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DTO représentant un joueur pour le réseau.
 */
public class PlayerDTO {

    public String id;
    public int x;
    public int y;
    public boolean alive;
    public int health;
    public int bombs;
    public String nickname;
    public float speed;
    public int bombRange;
    public int maxBombs;
    public int skinId;

    public PlayerDTO(String id, int x, int y, boolean alive, int health, int bombs, String nickname, float speed, int bombRange, int maxBombs) {
        this(id, x, y, alive, health, bombs, nickname, speed, bombRange, maxBombs, -1);
    }

    public PlayerDTO(String id, int x, int y, boolean alive, int health, int bombs, String nickname, float speed, int bombRange, int maxBombs, int skinId) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.alive = alive;
        this.health = health;
        this.bombs = bombs;
        this.nickname = nickname;
        this.speed = speed;
        this.bombRange = bombRange;
        this.maxBombs = maxBombs;
        this.skinId = skinId;
    }

    /**
     * Conversion depuis Player.
     */
    public static PlayerDTO from(IPlayer ip) {
        return new PlayerDTO(
                ip.getId(),
                ip.getPosition().getX(),
                ip.getPosition().getY(),
                ip.isAlive(),
                ip.getHealth(),
                ip.getCurrentBombs(),
                ip.getNickname(),
                ip.getSpeed(),
                ip.getBombRange(),
                ip.getMaxBombs()
        );
    }

    public static PlayerDTO fromJson(String json) {
        return fromJson(Json.parse(json));
    }

    public static PlayerDTO fromJson(Json json) {
        String id = json.at("id").getString();
        int x = json.at("x").getInt();
        int y = json.at("y").getInt();
        boolean alive = json.at("alive").getBoolean();
        int health = json.value("health", 0);
        int bombs = json.value("bombs", 0);
        String nickname = json.value("nickname", id);
        float speed = 1.0f;
		if (json.contains("speed") && json.at("speed").isNumber()) {
			speed = (float) json.at("speed").getDouble();
		}
        int bombRange = json.value("bombRange", 1);
        int maxBombs = json.value("maxBombs", 1);
        int skinId = json.value("skinId", -1);
        return new PlayerDTO(id, x, y, alive, health, bombs, nickname, speed, bombRange, maxBombs, skinId);
    }

    public Json toJson() {
        Map<String, Json> out = new LinkedHashMap<>();
        out.put("id", Json.of(id));
        out.put("x", Json.of(x));
        out.put("y", Json.of(y));
        out.put("alive", Json.of(alive));
        out.put("health", Json.of(health));
        out.put("bombs", Json.of(bombs));
        out.put("nickname", Json.of(nickname));
        out.put("speed", Json.of(speed));
        out.put("bombRange", Json.of(bombRange));
        out.put("maxBombs", Json.of(maxBombs));
        if (skinId >= 0) {
            out.put("skinId", Json.of(skinId));
        }
        return Json.object(out);
    }

    public static PlayerDTO fromBytes(ByteBuffer buffer) {
        int len = buffer.getInt();
        byte[] idBytes = new byte[len];
        buffer.get(idBytes);

        String id = new String(idBytes, StandardCharsets.UTF_8);
        int x = buffer.getInt();
        int y = buffer.getInt();
        boolean alive = buffer.get() == 1;
		int health = buffer.getInt();
		int bombs = buffer.getInt();
        
        int nickLen = buffer.getInt();
        byte[] nickBytes = new byte[nickLen];
        buffer.get(nickBytes);
        String nickname = new String(nickBytes, StandardCharsets.UTF_8);
        
        float speed = buffer.getFloat();
		int bombRange = buffer.getInt();
		int maxBombs = buffer.getInt();
		int skinId = buffer.remaining() >= Integer.BYTES ? buffer.getInt() : -1;

        return new PlayerDTO(id, x, y, alive, health, bombs, nickname, speed, bombRange, maxBombs, skinId);
    }

    public byte[] toBytes() {
        byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
        byte[] nickBytes = nickname.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + idBytes.length + 4 + 4 + 1 + 4 + 4 + 4 + nickBytes.length + 4 + 4 + 4 + 4);

        buffer.putInt(idBytes.length);
        buffer.put(idBytes);
        buffer.putInt(x);
        buffer.putInt(y);
        buffer.put((byte) (alive ? 1 : 0));
		buffer.putInt(health);
		buffer.putInt(bombs);
        buffer.putInt(nickBytes.length);
        buffer.put(nickBytes);
        buffer.putFloat(speed);
		buffer.putInt(bombRange);
		buffer.putInt(maxBombs);
		buffer.putInt(skinId);

        return buffer.array();
    }
}
