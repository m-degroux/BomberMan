package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Explosion;
import fr.iutgon.sae401.common.model.entity.Position;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO pour une explosion.
 */
public class ExplosionDTO {

    public Position origin;
    public List<Position> tiles;

    public ExplosionDTO(Position origin, List<Position> tiles) {
        this.origin = origin;
        this.tiles = tiles;
    }

    public ExplosionDTO(List<Position> tiles) {
        this(null, tiles);
    }

    public static ExplosionDTO from(Explosion e) {
        if (e == null) {
            return new ExplosionDTO(null, List.of());
        }
        return new ExplosionDTO(e.getOrigin(), e.getAffectedTiles());
    }

    public static ExplosionDTO fromJson(String json) {
        return fromJson(Json.parse(json));
    }

    public static ExplosionDTO fromJson(Json json) {
        if (json == null || !json.isObject()) {
            return new ExplosionDTO(null, List.of());
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
        List<Position> list = new ArrayList<>();
        Json tiles = json.value("tiles", Json.emptyArray());
        if (tiles.isArray()) {
            for (Json t : tiles.asArray()) {
                list.add(Position.fromJson(t));
            }
        }
        return new ExplosionDTO(origin, list);
    }

    public Json toJson() {
        List<Json> values = new ArrayList<>();
        for (Position p : tiles) {
            values.add(p == null ? Json.nullValue() : p.toJson());
        }
        java.util.Map<String, Json> out = new java.util.HashMap<>();
        out.put("tiles", Json.array(values));
        if (origin != null) {
            out.put("origin", origin.toJson());
        }
        return Json.object(out);
    }

    public static ExplosionDTO fromBytes(ByteBuffer buffer) {
        int size = buffer.getInt();
        List<Position> list = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            list.add(new Position(buffer.getInt(), buffer.getInt()));
        }

        return new ExplosionDTO(list);
    }

    public byte[] toBytes() {
        List<Position> safeTiles = tiles == null ? List.of() : tiles;
        ByteBuffer buffer = ByteBuffer.allocate(4 + safeTiles.size() * 8);

        buffer.putInt(safeTiles.size());
        for (Position p : safeTiles) {
            buffer.putInt(p.getX());
            buffer.putInt(p.getY());
        }

        return buffer.array();
    }
}
