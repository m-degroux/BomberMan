package fr.iutgon.sae401.serverSide.game.engines;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.bonus.BombRangeBonus;
import fr.iutgon.sae401.common.model.bonus.BonusPickup;
import fr.iutgon.sae401.common.model.bonus.BonusType;
import fr.iutgon.sae401.common.model.bonus.MaxBombBonus;
import fr.iutgon.sae401.common.model.bonus.SpeedBonus;
import fr.iutgon.sae401.common.model.entity.Explosion;
import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.gameplay.GameConfig;
import fr.iutgon.sae401.common.model.gameplay.GameState;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.Tile;
import fr.iutgon.sae401.common.model.map.TileType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class BombermanEngineStateOps {
    private static final int EXPLOSION_FRAME_MS = 80;
    private static final int EXPLOSION_ARM_DELAY_FRAMES = 2;
    private static final int EXPLOSION_CENTER_FRAMES = 7;
    private static final int EXPLOSION_ARM_FRAMES = 6;

    private BombermanEngineStateOps() {
    }

    static Json snapshotJson(GameState state, GameConfig config, boolean includeMap) {
        var players = state.getPlayers().stream().map(p -> Json.object(java.util.Map.of(
                "id", Json.of(p.getId()),
                "x", Json.of(p.getPosition().getX()),
                "y", Json.of(p.getPosition().getY()),
                "alive", Json.of(p.isAlive()),
                "health", Json.of(p.getHealth()),
                "bombs", Json.of(p.getCurrentBombs()),
                "speed", Json.of(p.getSpeed()),
                "bombRange", Json.of(p.getBombRange()),
                "maxBombs", Json.of(p.getMaxBombs())
        ))).toList();
        var bombs = state.getBombs().stream().map(b -> Json.object(java.util.Map.of(
                "id", Json.of(b.getBombId()),
                "x", Json.of(b.getPosition().getX()),
                "y", Json.of(b.getPosition().getY()),
                "range", Json.of(b.getRange()),
                "timerMs", Json.of(b.getRemainingMsCeil())
        ))).toList();
        var explosions = state.getExplosions().stream().map(e -> {
            java.util.Map<String, Json> obj = new java.util.HashMap<>();
            Position origin = e.getOrigin();
            if (origin != null) {
                obj.put("origin", origin.toJson());
            }
            obj.put("tiles", Json.array(e.getAffectedTiles().stream().map(t -> Json.object(java.util.Map.of(
                    "x", Json.of(t.getX()),
                    "y", Json.of(t.getY())
            ))).toList()));
            return Json.object(obj);
        }).toList();
        var bonuses = state.getBonuses().stream().map(BonusPickup::toJson).toList();

        java.util.Map<String, Json> out = new java.util.HashMap<>();
        out.put("width", Json.of(state.getMap().getWidth()));
        out.put("height", Json.of(state.getMap().getHeight()));
        out.put("bombTimerMs", Json.of(config.getBombTimer()));
        out.put("players", Json.array(players));
        out.put("bombs", Json.array(bombs));
        out.put("explosions", Json.array(explosions));
        out.put("bonuses", Json.array(bonuses));
        if (includeMap) {
            out.put("map", Json.of(state.getMap().toJson().stringify()));
        }
        return Json.object(out);
    }

    static Position pickSpawnAvoidingPlayers(GameState state, int width, int height) {
        int minX = Math.max(0, 1);
        int minY = Math.max(0, 1);
        int maxX = Math.max(0, width - 2);
        int maxY = Math.max(0, height - 2);
        List<Position> candidates = List.of(
                new Position(minX, minY),
                new Position(maxX, minY),
                new Position(minX, maxY),
                new Position(maxX, maxY)
        );
        Set<String> occupied = new HashSet<>();
        for (IPlayer p : state.getPlayers()) {
            if (p == null || p.getPosition() == null) {
                continue;
            }
            Position pos = p.getPosition();
            occupied.add(pos.getX() + ":" + pos.getY());
        }
        for (Position c : candidates) {
            String key = c.getX() + ":" + c.getY();
            if (occupied.contains(key)) {
                continue;
            }
            if (state.getMap().isInside(c) && state.getMap().isWalkable(c)) {
                return c;
            }
        }
        for (int y = 0; y < state.getMap().getHeight(); y++) {
            for (int x = 0; x < state.getMap().getWidth(); x++) {
                Position p = new Position(x, y);
                String key = x + ":" + y;
                if (occupied.contains(key)) {
                    continue;
                }
                if (state.getMap().isWalkable(p)) {
                    return p;
                }
            }
        }
        return new Position(minX, minY);
    }

    static boolean spawnBonuses(GameState state, Random random, double bonusDropChance, List<Position> destroyedDestructibles) {
        boolean spawned = false;
        for (Position p : destroyedDestructibles) {
            if (p == null) {
                continue;
            }
            boolean already = state.getBonuses().stream().anyMatch(b -> p.equals(b.getPosition()));
            if (already || random.nextDouble() >= bonusDropChance) {
                continue;
            }
            BonusType type = switch (random.nextInt(3)) {
                case 0 -> BonusType.SPEED;
                case 1 -> BonusType.MAX_BOMBS;
                default -> BonusType.BOMB_RANGE;
            };
            state.addBonus(new BonusPickup(p, type));
            spawned = true;
        }
        return spawned;
    }

    static boolean applyBonusPickups(GameState state) {
        if (state.getBonuses().isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (IPlayer player : new ArrayList<>(state.getPlayers())) {
            if (player == null || !player.isAlive()) {
                continue;
            }
            Position pos = player.getPosition();
            if (pos == null) {
                continue;
            }
            BonusPickup hit = null;
            for (BonusPickup b : state.getBonuses()) {
                if (b != null && pos.equals(b.getPosition())) {
                    hit = b;
                    break;
                }
            }
            if (hit == null) {
                continue;
            }
            IPlayer upgraded = switch (hit.getType()) {
                case SPEED -> new SpeedBonus(player);
                case MAX_BOMBS -> new MaxBombBonus(player);
                case BOMB_RANGE -> new BombRangeBonus(player);
            };
            state.replacePlayer(player.getId(), upgraded);
            state.removeBonus(hit);
            changed = true;
        }
        return changed;
    }

    static boolean applyExplosionDamageOnce(GameState state, Map<Explosion, Set<String>> damagedByExplosion) {
        boolean damaged = false;
        for (Explosion e : state.getExplosions()) {
            Set<String> already = damagedByExplosion.computeIfAbsent(e, _k -> new HashSet<>());
            for (IPlayer p : state.getPlayers()) {
                if (!p.isAlive()) {
                    continue;
                }
                Position pos = p.getPosition();
                if (pos == null || already.contains(p.getId())) {
                    continue;
                }
                if (!isTileInContactWithExplosionAnimation(e, pos)) {
                    continue;
                }
                already.add(p.getId());
                p.takeDamage();
                damaged = true;
            }
        }
        return damaged;
    }

    private static boolean isTileInContactWithExplosionAnimation(Explosion explosion, Position pos) {
        if (explosion == null || pos == null || !explosion.contains(pos)) {
            return false;
        }

        Position origin = explosion.getOrigin();
        if (origin == null) {
            // Fallback for legacy payloads without origin: keep previous behavior.
            return true;
        }

        int dx = Math.abs(pos.getX() - origin.getX());
        int dy = Math.abs(pos.getY() - origin.getY());
        if (dx != 0 && dy != 0) {
            return false;
        }
        int distance = dx + dy;

        int currentGlobalFrame = (int) Math.floor(explosion.getElapsedMs() / EXPLOSION_FRAME_MS);
        if (currentGlobalFrame < 1) {
            return false;
        }

        int localFrame = currentGlobalFrame - (distance * EXPLOSION_ARM_DELAY_FRAMES);
        int maxFrames = distance == 0 ? EXPLOSION_CENTER_FRAMES : EXPLOSION_ARM_FRAMES;
        return localFrame >= 1 && localFrame <= maxFrames;
    }

    static int explosionDurationMs(int range, int explosionArmFrames, int frameMs) {
        int r = Math.max(0, range);
        return (2 * r + explosionArmFrames) * frameMs;
    }

    static List<Position> computeExplosionTiles(
            GameState state,
            Position center,
            int range,
            List<Position> destroyedDestructiblesOut,
            Map<String, TileType> tileDeltas
    ) {
        List<Position> out = new ArrayList<>();
        if (center == null) {
            return out;
        }
        out.add(center);
        GameMap map = state.getMap();
        int cx = center.getX();
        int cy = center.getY();
        int[][] dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int dx = d[0];
            int dy = d[1];
            for (int step = 1; step <= range; step++) {
                Position p = new Position(cx + dx * step, cy + dy * step);
                if (!map.isInside(p)) {
                    break;
                }
                Tile tile = map.getTile(p);
                if (tile == null || tile.getType() == TileType.WALL) {
                    break;
                }
                if (tile.getType() == TileType.DESTRUCTIBLE) {
                    if (destroyedDestructiblesOut != null) {
                        destroyedDestructiblesOut.add(p);
                    }
                    out.add(p);
                    tile.destroy();
                    tileDeltas.put(p.getX() + ":" + p.getY(), TileType.GROUND);
                    break;
                }
                out.add(p);
            }
        }
        return out;
    }
}
