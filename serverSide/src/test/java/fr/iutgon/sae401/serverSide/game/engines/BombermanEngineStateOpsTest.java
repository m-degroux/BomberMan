package fr.iutgon.sae401.serverSide.game.engines;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.bonus.BonusPickup;
import fr.iutgon.sae401.common.model.bonus.BonusType;
import fr.iutgon.sae401.common.model.entity.Bomb;
import fr.iutgon.sae401.common.model.entity.Explosion;
import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Player;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.gameplay.GameConfig;
import fr.iutgon.sae401.common.model.gameplay.GameState;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.model.map.Tile;
import fr.iutgon.sae401.common.model.map.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BombermanEngineStateOpsTest {

    @Test
    void snapshotJson_includesOrOmitsMapAndSerializesCollections() {
        GameState state = new GameState(openMap(7, 7));
        Player p = player("p1", 3, 3, 3);
        state.addPlayer(p);
        state.addBomb(Bomb.createUnique(new Position(3, 3), "p1", 1200, 2));
        state.addExplosion(new Explosion(new Position(3, 3), List.of(new Position(3, 3), new Position(4, 3)), 300));
        state.addBonus(new BonusPickup(new Position(2, 2), BonusType.SPEED));

        GameConfig config = new GameConfig(7, 7, 3, 2, 2, 1500, 0.3f);

        Json withMap = BombermanEngineStateOps.snapshotJson(state, config, true);
        assertTrue(withMap.contains("map"));
        assertTrue(withMap.at("map").isString());
        assertEquals(1, withMap.at("players").asArray().size());
        assertEquals(1, withMap.at("bombs").asArray().size());
        assertEquals(1, withMap.at("explosions").asArray().size());
        assertEquals(1, withMap.at("bonuses").asArray().size());
        assertEquals(1500, withMap.value("bombTimerMs", -1));

        Json withoutMap = BombermanEngineStateOps.snapshotJson(state, config, false);
        assertFalse(withoutMap.contains("map"));
    }

    @Test
    void pickSpawnAvoidingPlayers_prefersCornersThenWalkableFallbackThenDefault() {
        GameState state = new GameState(openMap(7, 7));
        Position spawn1 = BombermanEngineStateOps.pickSpawnAvoidingPlayers(state, 7, 7);
        assertEquals(new Position(1, 1), spawn1);

        state.addPlayer(player("a", 1, 1, 3));
        state.addPlayer(player("b", 5, 1, 3));
        state.addPlayer(player("c", 1, 5, 3));
        state.addPlayer(player("d", 5, 5, 3));
        Position spawn2 = BombermanEngineStateOps.pickSpawnAvoidingPlayers(state, 7, 7);
        assertTrue(state.getMap().isWalkable(spawn2));
        assertTrue(!Set.of(new Position(1, 1), new Position(5, 1), new Position(1, 5), new Position(5, 5)).contains(spawn2));

        GameMap allWalls = fillMap(5, 5, TileType.WALL);
        GameState blockedState = new GameState(allWalls);
        Position spawn3 = BombermanEngineStateOps.pickSpawnAvoidingPlayers(blockedState, 5, 5);
        assertEquals(new Position(1, 1), spawn3);
    }

    @Test
    void spawnBonuses_handlesExistingTilesProbabilityAndBonusTypeSwitch() {
        GameState state = new GameState(openMap(7, 7));
        Position existingPos = new Position(2, 2);
        Position p2 = new Position(3, 2);
        Position p3 = new Position(4, 2);
        Position p4 = new Position(5, 2);
        state.addBonus(new BonusPickup(existingPos, BonusType.SPEED));

        List<Position> destroyed = new ArrayList<>();
        destroyed.add(null);
        destroyed.add(existingPos);
        destroyed.add(p2);
        destroyed.add(p3);
        destroyed.add(p4);
        Random sequence = new SequenceRandom(
                new double[]{0.0, 0.0, 0.0, 0.0},
                new int[]{0, 1, 2}
        );

        boolean spawned = BombermanEngineStateOps.spawnBonuses(state, sequence, 1.0, destroyed);
        assertTrue(spawned);
        assertEquals(4, state.getBonuses().size());

        Map<Position, BonusType> byPos = new HashMap<>();
        for (BonusPickup b : state.getBonuses()) {
            byPos.put(b.getPosition(), b.getType());
        }
        assertEquals(BonusType.SPEED, byPos.get(existingPos));
        assertEquals(BonusType.SPEED, byPos.get(p2));
        assertEquals(BonusType.MAX_BOMBS, byPos.get(p3));
        assertEquals(BonusType.BOMB_RANGE, byPos.get(p4));
    }

    @Test
    void spawnBonuses_returnsFalseWhenChanceFails() {
        GameState state = new GameState(openMap(7, 7));
        boolean spawned = BombermanEngineStateOps.spawnBonuses(
                state,
                new SequenceRandom(new double[]{0.9}, new int[]{0}),
                0.2,
                List.of(new Position(2, 2))
        );
        assertFalse(spawned);
        assertTrue(state.getBonuses().isEmpty());
    }

    @Test
    void applyBonusPickups_handlesEmptyAndAppliesUpgradeOnlyForMatchingAlivePlayers() {
        GameState emptyState = new GameState(openMap(7, 7));
        assertFalse(BombermanEngineStateOps.applyBonusPickups(emptyState));

        GameState state = new GameState(openMap(7, 7));
        Player alive = player("alive", 2, 2, 3);
        Player dead = player("dead", 3, 3, 3);
        dead.die();
        Player noPos = player("nopos", 4, 4, 3);
        noPos.setPosition(null);
        state.addPlayer(alive);
        state.addPlayer(dead);
        state.addPlayer(noPos);
        state.addPlayer(null);
        state.addBonus(new BonusPickup(new Position(2, 2), BonusType.SPEED));
        state.addBonus(new BonusPickup(new Position(6, 6), BonusType.MAX_BOMBS));

        boolean changed = BombermanEngineStateOps.applyBonusPickups(state);
        assertTrue(changed);
        IPlayer upgraded = state.getPlayerById("alive");
        assertNotNull(upgraded);
        assertTrue(upgraded.getSpeed() > 1.0f);
        assertEquals(1, state.getBonuses().size());
        assertEquals(new Position(6, 6), state.getBonuses().getFirst().getPosition());
    }

    @Test
    void applyExplosionDamageOnce_damagesOnlyOncePerExplosionAndPlayer() {
        GameState state = new GameState(openMap(7, 7));
        Player target = player("target", 3, 3, 3);
        Player outside = player("outside", 1, 1, 2);
        Explosion explosion = new Explosion(new Position(3, 3), List.of(new Position(3, 3), new Position(4, 3)), 500);
        state.addPlayer(target);
        state.addPlayer(outside);
        state.addExplosion(explosion);

        Map<Explosion, Set<String>> damagedByExplosion = new HashMap<>();

        boolean beforeContact = BombermanEngineStateOps.applyExplosionDamageOnce(state, damagedByExplosion);
        assertFalse(beforeContact);
        assertEquals(3, target.getHealth());

        explosion.tick(0.08);
        boolean first = BombermanEngineStateOps.applyExplosionDamageOnce(state, damagedByExplosion);
        assertTrue(first);
        assertEquals(2, target.getHealth());
        assertEquals(2, outside.getHealth());

        boolean second = BombermanEngineStateOps.applyExplosionDamageOnce(state, damagedByExplosion);
        assertFalse(second);
        assertEquals(2, target.getHealth());
    }

    @Test
    void applyExplosionDamageOnce_delaysDamageOnArmsUntilWaveReachesTile() {
        GameState state = new GameState(openMap(7, 7));
        Player farTarget = player("far", 5, 3, 3);
        Explosion explosion = new Explosion(
                new Position(3, 3),
                List.of(new Position(3, 3), new Position(4, 3), new Position(5, 3)),
                1000
        );
        state.addPlayer(farTarget);
        state.addExplosion(explosion);

        Map<Explosion, Set<String>> damagedByExplosion = new HashMap<>();

        explosion.tick(0.39);
        assertFalse(BombermanEngineStateOps.applyExplosionDamageOnce(state, damagedByExplosion));
        assertEquals(3, farTarget.getHealth());

        explosion.tick(0.02);
        assertTrue(BombermanEngineStateOps.applyExplosionDamageOnce(state, damagedByExplosion));
        assertEquals(2, farTarget.getHealth());
    }

    @Test
    void explosionDurationMs_handlesNegativeRangeAndNormalRange() {
        assertEquals(560, BombermanEngineStateOps.explosionDurationMs(-3, 7, 80));
        assertEquals(880, BombermanEngineStateOps.explosionDurationMs(2, 7, 80));
    }

    @Test
    void computeExplosionTiles_handlesNullCenterWallsDestructiblesAndTileDeltas() {
        GameState state = new GameState(openMap(7, 7));
        List<Position> destroyed = new ArrayList<>();
        Map<String, TileType> deltas = new HashMap<>();

        List<Position> none = BombermanEngineStateOps.computeExplosionTiles(state, null, 3, destroyed, deltas);
        assertTrue(none.isEmpty());

        state.getMap().setTile(new Position(5, 3), new Tile(TileType.WALL));
        state.getMap().setTile(new Position(3, 1), new Tile(TileType.DESTRUCTIBLE));
        List<Position> tiles = BombermanEngineStateOps.computeExplosionTiles(
                state,
                new Position(3, 3),
                3,
                destroyed,
                deltas
        );

        assertTrue(tiles.contains(new Position(3, 3)));
        assertTrue(tiles.contains(new Position(4, 3)));
        assertTrue(!tiles.contains(new Position(5, 3)));
        assertTrue(tiles.contains(new Position(3, 2)));
        assertTrue(tiles.contains(new Position(3, 1)));
        assertTrue(destroyed.contains(new Position(3, 1)));
        assertEquals(TileType.GROUND, state.getMap().getTile(new Position(3, 1)).getType());
        assertEquals(TileType.GROUND, deltas.get("3:1"));
    }

    private static Player player(String id, int x, int y, int health) {
        return new Player(id, new Position(x, y), health, 2);
    }

    private static GameMap openMap(int width, int height) {
        GameMap map = new GameMap(width, height, MapTheme.CLASSIC);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                TileType type = (x == 0 || y == 0 || x == width - 1 || y == height - 1)
                        ? TileType.WALL
                        : TileType.GROUND;
                map.setTile(new Position(x, y), new Tile(type));
            }
        }
        return map;
    }

    private static GameMap fillMap(int width, int height, TileType type) {
        GameMap map = new GameMap(width, height, MapTheme.CLASSIC);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map.setTile(new Position(x, y), new Tile(type));
            }
        }
        return map;
    }

    private static final class SequenceRandom extends Random {
        private final double[] doubles;
        private final int[] ints;
        private int dIndex = 0;
        private int iIndex = 0;

        private SequenceRandom(double[] doubles, int[] ints) {
            this.doubles = doubles == null ? new double[0] : doubles;
            this.ints = ints == null ? new int[0] : ints;
        }

        @Override
        public double nextDouble() {
            if (doubles.length == 0) {
                return 0.0;
            }
            double value = doubles[Math.min(dIndex, doubles.length - 1)];
            dIndex++;
            return value;
        }

        @Override
        public int nextInt(int bound) {
            if (bound <= 0) {
                return 0;
            }
            if (ints.length == 0) {
                return 0;
            }
            int value = ints[Math.min(iIndex, ints.length - 1)];
            iIndex++;
            return Math.floorMod(value, bound);
        }
    }
}
