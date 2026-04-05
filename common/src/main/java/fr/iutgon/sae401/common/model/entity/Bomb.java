package fr.iutgon.sae401.common.model.entity;

import fr.iutgon.sae401.common.json.Json;


import java.util.UUID;

public class Bomb extends Entity {
    private final String ownerId;
    private double remainingMs;
    private final int range;
    private final String bombId;

    public Bomb(String bombId, Position position, String ownerId, double timerMs, int range) {
        super(bombId, position);
        this.bombId = bombId;
        this.ownerId = ownerId;
        this.remainingMs = timerMs;
        this.range = range;
    }

    /**
     * Crée une bombe avec un ID unique (UUID).
     */
    public static Bomb createUnique(Position position, String ownerId, double timerMs, int range) {
        String id = UUID.randomUUID().toString();
        return new Bomb(id, position, ownerId, timerMs, range);
    }

    public static Bomb fromJson(String json) {
        return fromJson(Json.parse(json));
    }

    public static Bomb fromJson(Json json) {
        if (json == null || !json.isObject()) {
            throw new IllegalArgumentException("Bomb JSON must be an object");
        }
        String bombId = json.value("id", UUID.randomUUID().toString());
        Position pos;
        if (json.contains("position") && json.at("position").isObject()) {
            pos = Position.fromJson(json.at("position"));
        } else {
            int x = json.value("x", Integer.MIN_VALUE);
            int y = json.value("y", Integer.MIN_VALUE);
            if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE) {
                throw new IllegalArgumentException("Bomb JSON must contain either 'position' or 'x'/'y'");
            }
            pos = new Position(x, y);
        }

        String owner = json.value("owner", json.value("ownerId", ""));
        double timerMs = json.value("timerMs", json.value("timer", 2000));
        int range = json.value("range", 1);
        return new Bomb(bombId, pos, owner, timerMs, range);
    }

    public void tick(double dtSeconds) {
        remainingMs -= dtSeconds * 1000.0;
    }

    public boolean isExploded() {
        return remainingMs <= 0;
    }

    public int getRemainingMsCeil() {
        return (int) Math.ceil(remainingMs);
    }

    public String getOwnerId() {
        return ownerId;
    }

    public int getRange() {
        return range;
    }

    public String getBombId() {
        return bombId;
    }

    public Json toJson() {
        return Json.object(java.util.Map.of(
                "id", Json.of(bombId),
                "position", position == null ? Json.nullValue() : position.toJson(),
                "owner", Json.of(ownerId),
                "timerMs", Json.of(getRemainingMsCeil()),
                "range", Json.of(range)
        ));
    }
}
