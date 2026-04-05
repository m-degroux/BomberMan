package fr.iutgon.sae401.common.model.entity;

import fr.iutgon.sae401.common.json.Json;

import java.util.Objects;

public class Position {
    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Position fromJson(String json) {
		return fromJson(Json.parse(json));
    }

	public static Position fromJson(Json json) {
		if (json == null || !json.isObject()) {
			throw new IllegalArgumentException("Position JSON must be an object");
		}
		int x = json.at("x").getInt();
		int y = json.at("y").getInt();
		return new Position(x, y);
	}

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Position add(Direction dir) {
        return new Position(x + dir.dx(), y + dir.dy());
    }

    public int distance(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Position p))
            return false;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public Json toJson() {
        return Json.object(java.util.Map.of(
                "x", Json.of(x),
                "y", Json.of(y)
        ));
    }
}
