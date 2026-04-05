package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.bonus.BonusPickup;
import fr.iutgon.sae401.common.model.bonus.BonusType;
import fr.iutgon.sae401.common.model.entity.Position;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * DTO representing a bonus pickup on the map.
 */
public final class BonusPickupDTO {
	public int x;
	public int y;
	public BonusType type;

	public BonusPickupDTO(int x, int y, BonusType type) {
		this.x = x;
		this.y = y;
		this.type = type;
	}

	public static BonusPickupDTO from(BonusPickup pickup) {
		Position p = pickup.getPosition();
		return new BonusPickupDTO(p.getX(), p.getY(), pickup.getType());
	}

	public static BonusPickupDTO fromJson(Json json) {
		int x = json.at("x").getInt();
		int y = json.at("y").getInt();
		String typeStr = json.at("type").getString();
		return new BonusPickupDTO(x, y, BonusType.valueOf(typeStr));
	}

	public Json toJson() {
		return Json.object(java.util.Map.of(
				"x", Json.of(x),
				"y", Json.of(y),
				"type", Json.of(type == null ? BonusType.SPEED.name() : type.name())
		));
	}

	public static BonusPickupDTO fromBytes(ByteBuffer buffer) {
		int x = buffer.getInt();
		int y = buffer.getInt();
		int len = buffer.getInt();
		byte[] typeBytes = new byte[len];
		buffer.get(typeBytes);
		String typeStr = new String(typeBytes, StandardCharsets.UTF_8);
		return new BonusPickupDTO(x, y, BonusType.valueOf(typeStr));
	}

	public byte[] toBytes() {
		byte[] typeBytes = (type == null ? BonusType.SPEED.name() : type.name()).getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 4 + typeBytes.length);
		buffer.putInt(x);
		buffer.putInt(y);
		buffer.putInt(typeBytes.length);
		buffer.put(typeBytes);
		return buffer.array();
	}
}
