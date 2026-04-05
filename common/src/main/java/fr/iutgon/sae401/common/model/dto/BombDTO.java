package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Bomb;

import java.nio.ByteBuffer;

/**
 * DTO représentant une bombe.
 */
public class BombDTO {

    public String id;
    public int x;
    public int y;
    public int range;
    public int timerMs;

    public BombDTO(String id, int x, int y, int range, int timerMs) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.range = range;
        this.timerMs = timerMs;
    }

    public static BombDTO from(Bomb b) {
        return new BombDTO(b.getBombId(), b.getPosition().getX(), b.getPosition().getY(), b.getRange(), b.getRemainingMsCeil());
    }

    public static BombDTO fromJson(String json) {
        return fromJson(Json.parse(json));
    }

    public static BombDTO fromJson(Json json) {
        String id = json.value("id", "");
        int x = json.at("x").getInt();
        int y = json.at("y").getInt();
        int range = json.at("range").getInt();
		int timerMs = json.value("timerMs", 0);
        return new BombDTO(id, x, y, range, timerMs);
    }

    public Json toJson() {
        return Json.object(java.util.Map.of(
                "id", Json.of(id),
                "x", Json.of(x),
                "y", Json.of(y),
                "range", Json.of(range),
				"timerMs", Json.of(timerMs)
        ));
    }

    public static BombDTO fromBytes(ByteBuffer buffer) {
		int len = buffer.getInt();
		byte[] idBytes = new byte[len];
		buffer.get(idBytes);
		String id = new String(idBytes, java.nio.charset.StandardCharsets.UTF_8);
		int x = buffer.getInt();
		int y = buffer.getInt();
		int range = buffer.getInt();
		int timerMs = buffer.getInt();
        return new BombDTO(id, x, y, range, timerMs);
    }

    public byte[] toBytes() {
        byte[] idBytes = (id == null ? "" : id).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + idBytes.length + 4 + 4 + 4 + 4);
		buffer.putInt(idBytes.length);
		buffer.put(idBytes);
        buffer.putInt(x);
        buffer.putInt(y);
        buffer.putInt(range);
		buffer.putInt(timerMs);
        return buffer.array();
    }
}