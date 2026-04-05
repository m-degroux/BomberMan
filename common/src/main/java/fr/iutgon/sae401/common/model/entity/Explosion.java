package fr.iutgon.sae401.common.model.entity;

import fr.iutgon.sae401.common.json.Json;

import java.util.ArrayList;
import java.util.List;

public class Explosion {

    private final List<Position> affectedTiles;
    private final Position origin;
    private final double durationMs;
    private double remainingMs;

    public Explosion(List<Position> affectedTiles, int durationMs) {
        this(null, affectedTiles, durationMs);
    }

    public Explosion(Position origin, List<Position> affectedTiles, int durationMs) {
        this.origin = origin;
        this.affectedTiles = affectedTiles;
        this.durationMs = Math.max(0, durationMs);
        this.remainingMs = durationMs;
    }

    public static Explosion fromJson(String json) {
		return fromJson(Json.parse(json));
    }

	public static Explosion fromJson(Json json) {
		if (json == null || !json.isObject()) {
			throw new IllegalArgumentException("Explosion JSON must be an object");
		}
        Position origin = null;
        Json originJson = json.contains("origin") ? json.at("origin") : Json.nullValue();
        if (originJson.isObject()) {
            origin = Position.fromJson(originJson);
        } else {
            int ox = json.value("originX", Integer.MIN_VALUE);
            int oy = json.value("originY", Integer.MIN_VALUE);
            if (ox != Integer.MIN_VALUE && oy != Integer.MIN_VALUE) {
                origin = new Position(ox, oy);
            }
        }
		List<Position> positions = new ArrayList<>();
		Json tiles = json.value("tiles", Json.emptyArray());
		if (tiles.isArray()) {
			for (Json t : tiles.asArray()) {
				positions.add(Position.fromJson(t));
			}
		}
        int durationMs = json.value("durationMs", 1000);
        return new Explosion(origin, positions, durationMs);
	}

    public void tick() {
        // Legacy tick: treat as milliseconds.
        remainingMs -= 1.0;
    }

    public void tick(double dtSeconds) {
        remainingMs -= dtSeconds * 1000.0;
    }

    public boolean isFinished() {
        return remainingMs <= 0;
    }

    public double getDurationMs() {
        return durationMs;
    }

    public double getRemainingMs() {
        return remainingMs;
    }

    public double getElapsedMs() {
        double clampedRemaining = Math.max(0.0, remainingMs);
        return Math.max(0.0, durationMs - clampedRemaining);
    }

    public List<Position> getAffectedTiles() {
        return affectedTiles;
    }

	public Position getOrigin() {
		return origin;
	}

    public boolean contains(Position pos) {
        return affectedTiles.contains(pos);
    }

    public Json toJson() {
        List<Json> tiles = new ArrayList<>();
        for (Position p : affectedTiles) {
            tiles.add(p == null ? Json.nullValue() : p.toJson());
        }
        java.util.Map<String, Json> out = new java.util.HashMap<>();
        out.put("tiles", Json.array(tiles));
        out.put("durationMs", Json.of((int) Math.ceil(remainingMs)));
        if (origin != null) {
            out.put("origin", origin.toJson());
        }
        return Json.object(out);
    }
}
