package fr.iutgon.sae401.common.model.bonus;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Position;

import java.util.Objects;

public final class BonusPickup {
	private final Position position;
	private final BonusType type;

	public BonusPickup(Position position, BonusType type) {
		this.position = Objects.requireNonNull(position, "position");
		this.type = Objects.requireNonNull(type, "type");
	}

	public Position getPosition() {
		return position;
	}

	public BonusType getType() {
		return type;
	}

	public Json toJson() {
		return Json.object(java.util.Map.of(
				"x", Json.of(position.getX()),
				"y", Json.of(position.getY()),
				"type", Json.of(type.name())
		));
	}
}
