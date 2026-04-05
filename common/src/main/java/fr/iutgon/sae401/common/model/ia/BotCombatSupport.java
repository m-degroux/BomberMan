package fr.iutgon.sae401.common.model.ia;

import fr.iutgon.sae401.common.model.entity.Bomb;
import fr.iutgon.sae401.common.model.entity.Direction;
import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.gameplay.GameState;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.Tile;
import fr.iutgon.sae401.common.model.map.TileType;

import java.util.ArrayList;
import java.util.List;

final class BotCombatSupport {
    private BotCombatSupport() {
    }

    static int threatCount(GameState state, Position position) {
        if (state == null || position == null) {
            return Integer.MAX_VALUE / 4;
        }
        int count = 0;
        for (var explosion : state.getExplosions()) {
            if (explosion != null && explosion.contains(position)) {
                count += 4;
            }
        }
        for (Bomb bomb : state.getBombs()) {
            if (bomb != null && isInBlast(state.getMap(), bomb.getPosition(), bomb.getRange(), position)) {
                count++;
            }
        }
        return count;
    }

    static boolean isThreatened(GameState state, Position position) {
        return threatCount(state, position) > 0;
    }

    static boolean wouldHitEnemy(GameState state, Position origin, int range, String selfId) {
        if (state == null || origin == null) {
            return false;
        }
        for (IPlayer player : state.getAlivePlayers()) {
            if (player == null || player.getPosition() == null) {
                continue;
            }
            if (selfId != null && selfId.equals(player.getId())) {
                continue;
            }
            if (isInBlast(state.getMap(), origin, range, player.getPosition())) {
                return true;
            }
        }
        return false;
    }

    static int destructiblesHitByBomb(GameState state, Position origin, int range) {
        return destructibleTilesHitByBomb(state, origin, range).size();
    }

    static List<Position> destructibleTilesHitByBomb(GameState state, Position origin, int range) {
        List<Position> hits = new ArrayList<>();
        if (state == null || origin == null) {
            return hits;
        }
        GameMap map = state.getMap();
        if (map == null) {
            return hits;
        }
        for (Direction direction : BotStrategySupport.DIRECTIONS) {
            Position current = origin;
            for (int step = 1; step <= range; step++) {
                current = current.add(direction);
                if (!map.isInside(current)) {
                    break;
                }
                Tile tile = map.getTile(current);
                if (tile == null) {
                    break;
                }
                if (tile.getType() == TileType.WALL) {
                    break;
                }
                if (tile.getType() == TileType.DESTRUCTIBLE) {
                    hits.add(current);
                    break;
                }
            }
        }
        return hits;
    }

    static boolean isInBlast(GameMap map, Position origin, int range, Position target) {
        if (map == null || origin == null || target == null || range < 0) {
            return false;
        }
        if (origin.equals(target)) {
            return true;
        }
        for (Direction direction : BotStrategySupport.DIRECTIONS) {
            Position current = origin;
            for (int step = 1; step <= range; step++) {
                current = current.add(direction);
                if (!map.isInside(current)) {
                    break;
                }
                Tile tile = map.getTile(current);
                if (tile == null) {
                    break;
                }
                TileType type = tile.getType();
                if (type == TileType.WALL) {
                    break;
                }
                if (current.equals(target)) {
                    return true;
                }
                if (type == TileType.DESTRUCTIBLE) {
                    break;
                }
            }
        }
        return false;
    }

    static int distanceToNearestEnemy(GameState state, Position position, String selfId) {
        if (state == null || position == null) {
            return 0;
        }
        int best = Integer.MAX_VALUE;
        for (IPlayer player : state.getAlivePlayers()) {
            if (player == null || player.getPosition() == null) {
                continue;
            }
            if (selfId != null && selfId.equals(player.getId())) {
                continue;
            }
            best = Math.min(best, position.distance(player.getPosition()));
        }
        return best == Integer.MAX_VALUE ? 99 : best;
    }

    static int distanceToNearestBomb(GameState state, Position position) {
        if (state == null || position == null) {
            return 0;
        }
        int best = Integer.MAX_VALUE;
        for (Bomb bomb : state.getBombs()) {
            if (bomb == null || bomb.getPosition() == null) {
                continue;
            }
            best = Math.min(best, position.distance(bomb.getPosition()));
        }
        return best == Integer.MAX_VALUE ? 99 : best;
    }

    static int survivalScore(GameState state, Position position, String selfId) {
        int threatPenalty = threatCount(state, position) * 1000;
        int enemyDistance = distanceToNearestEnemy(state, position, selfId);
        int bombDistance = distanceToNearestBomb(state, position);
        int mobility = mobilityScore(state, position);
        return (enemyDistance * 20) + (bombDistance * 10) + (mobility * 12) - threatPenalty;
    }

    static int mobilityScore(GameState state, Position position) {
        if (state == null || position == null) {
            return 0;
        }
        int score = 0;
        for (BotStrategySupport.ReachableNode node : BotStrategySupport.reachableNodes(state, position)) {
            if (node.distance() <= 4) {
                score++;
            }
        }
        return score;
    }

    static boolean isExplosionTile(GameState state, Position position) {
        if (state == null || position == null) {
            return false;
        }
        for (var explosion : state.getExplosions()) {
            if (explosion != null && explosion.contains(position)) {
                return true;
            }
        }
        return false;
    }
}
