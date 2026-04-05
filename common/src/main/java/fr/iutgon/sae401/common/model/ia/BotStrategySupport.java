package fr.iutgon.sae401.common.model.ia;

import fr.iutgon.sae401.common.model.entity.Bomb;
import fr.iutgon.sae401.common.model.entity.Direction;
import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.gameplay.ActionType;
import fr.iutgon.sae401.common.model.gameplay.GameState;
import fr.iutgon.sae401.common.model.gameplay.PlayerAction;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.Tile;
import fr.iutgon.sae401.common.model.map.TileType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class BotStrategySupport {
    static final class EscapePlan {
        private final List<Direction> steps;
        private final Direction firstStep;
        private final int distance;
        private final int safeAreaSize;

        EscapePlan(List<Direction> steps, int distance, int safeAreaSize) {
            this.steps = List.copyOf(steps);
            this.firstStep = this.steps.isEmpty() ? null : this.steps.getFirst();
            this.distance = distance;
            this.safeAreaSize = safeAreaSize;
        }

        List<Direction> steps() {
            return steps;
        }

        Direction firstStep() {
            return firstStep;
        }

        int distance() {
            return distance;
        }

        int safeAreaSize() {
            return safeAreaSize;
        }
    }

    static final class ReachableNode {
        private final Position position;
        private final Direction firstStep;
        private final int distance;

        ReachableNode(Position position, Direction firstStep, int distance) {
            this.position = position;
            this.firstStep = firstStep;
            this.distance = distance;
        }

        Position position() {
            return position;
        }

        Direction firstStep() {
            return firstStep;
        }

        int distance() {
            return distance;
        }
    }

    static final List<Direction> DIRECTIONS = List.of(
            Direction.UP,
            Direction.DOWN,
            Direction.LEFT,
            Direction.RIGHT
    );

    private BotStrategySupport() {
    }

    static List<IPlayer> aliveEnemies(GameState state, IPlayer bot) {
        List<IPlayer> enemies = new ArrayList<>();
        if (state == null || bot == null) {
            return enemies;
        }
        for (IPlayer player : state.getAlivePlayers()) {
            if (player == null || !player.isAlive()) {
                continue;
            }
            if (bot.getId().equals(player.getId())) {
                continue;
            }
            if (player.getPosition() == null) {
                continue;
            }
            enemies.add(player);
        }
        return enemies;
    }

    static IPlayer nearestEnemy(GameState state, IPlayer bot) {
        Position origin = bot == null ? null : bot.getPosition();
        if (origin == null) {
            return null;
        }
        return aliveEnemies(state, bot).stream()
                .min(Comparator.comparingInt(enemy -> origin.distance(enemy.getPosition())))
                .orElse(null);
    }

    static Position nextPosition(Position position, Direction direction) {
        if (position == null || direction == null) {
            return null;
        }
        return position.add(direction);
    }

    static boolean isWalkableAndFree(GameState state, Position position) {
        if (state == null || position == null) {
            return false;
        }
        GameMap map = state.getMap();
        if (map == null || !map.isWalkable(position)) {
            return false;
        }
        return !hasBombAt(state, position);
    }

    static boolean hasBombAt(GameState state, Position position) {
        if (state == null || position == null) {
            return false;
        }
        for (Bomb bomb : state.getBombs()) {
            if (bomb != null && position.equals(bomb.getPosition())) {
                return true;
            }
        }
        return false;
    }

    static List<Direction> validMoves(GameState state, Position origin) {
        List<Direction> moves = new ArrayList<>();
        if (origin == null) {
            return moves;
        }
        for (Direction direction : DIRECTIONS) {
            Position next = nextPosition(origin, direction);
            if (isWalkableAndFree(state, next)) {
                moves.add(direction);
            }
        }
        return moves;
    }

    static List<ReachableNode> reachableNodes(GameState state, Position start) {
        List<ReachableNode> nodes = new ArrayList<>();
        if (state == null || start == null) {
            return nodes;
        }
        GameMap map = state.getMap();
        if (map == null || !map.isInside(start) || !map.isWalkable(start)) {
            return nodes;
        }

        ArrayDeque<ReachableNode> queue = new ArrayDeque<>();
        Set<Position> visited = new HashSet<>();
        ReachableNode root = new ReachableNode(start, null, 0);
        queue.add(root);
        visited.add(start);

        while (!queue.isEmpty()) {
            ReachableNode current = queue.removeFirst();
            nodes.add(current);
            for (Direction direction : DIRECTIONS) {
                Position next = current.position.add(direction);
                if (!visited.add(next)) {
                    continue;
                }
                if (!isWalkableAndFree(state, next)) {
                    continue;
                }
                Direction firstStep = current.firstStep == null ? direction : current.firstStep;
                queue.addLast(new ReachableNode(next, firstStep, current.distance + 1));
            }
        }
        return nodes;
    }

    static ReachableNode bestReachableNode(GameState state, Position start, Predicate<Position> goal, Comparator<ReachableNode> comparator) {
        ReachableNode best = null;
        for (ReachableNode node : reachableNodes(state, start)) {
            if (!goal.test(node.position())) {
                continue;
            }
            if (best == null || comparator.compare(node, best) < 0) {
                best = node;
            }
        }
        return best;
    }

    static boolean canPlaceBomb(GameState state, IPlayer bot) {
        return bot != null
                && bot.canPlaceBomb()
                && bot.getPosition() != null
                && isWalkableAndFreeIgnoringSelfBomb(state, bot.getPosition());
    }

    private static boolean isWalkableAndFreeIgnoringSelfBomb(GameState state, Position position) {
        if (state == null || position == null) {
            return false;
        }
        GameMap map = state.getMap();
        if (map == null || !map.isWalkable(position)) {
            return false;
        }
        return !hasBombAt(state, position);
    }

    static int threatCount(GameState state, Position position) {
        return BotCombatSupport.threatCount(state, position);
    }

    static boolean isThreatened(GameState state, Position position) {
        return BotCombatSupport.isThreatened(state, position);
    }

    static boolean wouldHitEnemy(GameState state, Position origin, int range, String selfId) {
        return BotCombatSupport.wouldHitEnemy(state, origin, range, selfId);
    }

    static int destructiblesHitByBomb(GameState state, Position origin, int range) {
        return BotCombatSupport.destructiblesHitByBomb(state, origin, range);
    }

    static List<Position> destructibleTilesHitByBomb(GameState state, Position origin, int range) {
        return BotCombatSupport.destructibleTilesHitByBomb(state, origin, range);
    }

    static boolean canEscapeOwnBomb(GameState state, Position origin, int range) {
        return findEscapePlanAfterOwnBomb(state, origin, range) != null;
    }

    static EscapePlan findEscapePlanAfterOwnBomb(GameState state, Position origin, int range) {
        if (state == null || origin == null) {
            return null;
        }
        GameMap map = state.getMap();
        if (map == null) {
            return null;
        }

        ArrayDeque<Position> queue = new ArrayDeque<>();
        Set<Position> visited = new HashSet<>();
        java.util.Map<Position, Position> parent = new java.util.HashMap<>();
        EscapePlan bestPlan = null;
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty()) {
            Position current = queue.removeFirst();
            if (!current.equals(origin) && !isThreatenedByExistingAndOwnBomb(state, current, origin, range)) {
                List<Direction> steps = pathDirections(origin, current, parent);
                Direction firstStep = steps.isEmpty() ? null : steps.getFirst();
                int distance = origin.distance(current);
                int safeArea = safeAreaSizeAfterOwnBomb(state, current, origin, range);
                EscapePlan plan = new EscapePlan(steps, distance, safeArea);
                if (firstStep != null && safeArea >= 2) {
                    if (bestPlan == null
                            || plan.distance() < bestPlan.distance()
                            || (plan.distance() == bestPlan.distance() && plan.safeAreaSize() > bestPlan.safeAreaSize())) {
                        bestPlan = plan;
                    }
                }
            }
            for (Direction direction : DIRECTIONS) {
                Position next = current.add(direction);
                if (!visited.add(next)) {
                    continue;
                }
                if (!map.isInside(next) || !map.isWalkable(next)) {
                    continue;
                }
                if (!next.equals(origin) && hasBombAt(state, next)) {
                    continue;
                }
                parent.put(next, current);
                queue.addLast(next);
            }
        }
        return bestPlan;
    }

    static boolean canSafelyPlantBomb(GameState state, IPlayer bot) {
        if (!canPlaceBomb(state, bot)) {
            return false;
        }
        int range = Math.max(1, bot.getBombRange());
        return findEscapePlanAfterOwnBomb(state, bot.getPosition(), range) != null;
    }

    static boolean hasReliableEscapePlan(GameState state, IPlayer bot, int minSafeArea, int maxDistance) {
        if (state == null || bot == null || bot.getPosition() == null) {
            return false;
        }
        int range = Math.max(1, bot.getBombRange());
        EscapePlan plan = findEscapePlanAfterOwnBomb(state, bot.getPosition(), range);
        if (plan == null || plan.firstStep() == null) {
            return false;
        }
        Position next = bot.getPosition().add(plan.firstStep());
        if (isExplosionTile(state, next)) {
            return false;
        }
        int currentThreat = threatCount(state, bot.getPosition());
        int nextThreat = threatCount(state, next);
        if (nextThreat > currentThreat) {
            return false;
        }
        if (maxDistance > 0 && plan.distance() > maxDistance) {
            return false;
        }
        return plan.safeAreaSize() >= minSafeArea;
    }

    static ReachableNode pathToAttackPosition(GameState state, IPlayer bot) {
        if (state == null || bot == null || bot.getPosition() == null) {
            return null;
        }
        int range = Math.max(1, bot.getBombRange());
        String selfId = bot.getId();
        return bestReachableNode(
                state,
                bot.getPosition(),
                position -> wouldHitEnemy(state, position, range, selfId),
                Comparator
                        .comparingInt((ReachableNode node) -> threatCount(state, node.position()) * 100)
                        .thenComparingInt(ReachableNode::distance)
                        .thenComparingInt(node -> distanceToNearestEnemy(state, node.position(), selfId))
        );
    }

    static boolean isIsolatedFromEnemies(GameState state, IPlayer bot) {
        if (state == null || bot == null || bot.getPosition() == null) {
            return false;
        }
        Set<Position> reachable = new HashSet<>();
        for (ReachableNode node : reachableNodes(state, bot.getPosition())) {
            reachable.add(node.position());
        }
        boolean hasEnemy = false;
        for (IPlayer enemy : aliveEnemies(state, bot)) {
            hasEnemy = true;
            if (reachable.contains(enemy.getPosition())) {
                return false;
            }
        }
        return hasEnemy;
    }

    static ReachableNode pathToWallBreakPosition(GameState state, IPlayer bot, boolean aggressive) {
        if (state == null || bot == null || bot.getPosition() == null) {
            return null;
        }
        int range = Math.max(1, bot.getBombRange());
        String selfId = bot.getId();
        return bestReachableNode(
                state,
                bot.getPosition(),
                position -> destructiblesHitByBomb(state, position, range) > 0 && canEscapeOwnBomb(state, position, range),
                Comparator.comparingInt((ReachableNode node) -> {
                            int destructibles = destructiblesHitByBomb(state, node.position(), range);
                            int threatPenalty = threatCount(state, node.position()) * 150;
                            int distancePenalty = node.distance() * 8;
                            int enemyBias = aggressive
                                    ? distanceToNearestEnemy(state, node.position(), selfId) * 6
                                    : -distanceToNearestEnemy(state, node.position(), selfId) * 3;
                            return threatPenalty + distancePenalty + enemyBias - (destructibles * 40);
                        })
                        .thenComparingInt(ReachableNode::distance)
        );
    }

    static ReachableNode pathToIsolationBreakPosition(GameState state, IPlayer bot) {
        if (state == null || bot == null || bot.getPosition() == null) {
            return null;
        }
        int range = Math.max(1, bot.getBombRange());
        return bestReachableNode(
                state,
                bot.getPosition(),
                position -> destructiblesHitByBomb(state, position, range) > 0 && canEscapeOwnBomb(state, position, range),
                Comparator.comparingInt((ReachableNode node) -> {
                            int destructibles = destructiblesHitByBomb(state, node.position(), range);
                            int threatPenalty = threatCount(state, node.position()) * 200;
                            int distancePenalty = node.distance() * 10;
                            return threatPenalty + distancePenalty - (destructibles * 60);
                        })
                        .thenComparingInt(ReachableNode::distance)
        );
    }

    static ReachableNode pathToSafestPosition(GameState state, IPlayer bot) {
        if (state == null || bot == null || bot.getPosition() == null) {
            return null;
        }
        String selfId = bot.getId();
        ReachableNode best = null;
        int bestScore = Integer.MIN_VALUE;
        for (ReachableNode node : reachableNodes(state, bot.getPosition())) {
            if (node.distance() == 0) {
                continue;
            }
            if (isThreatened(state, node.position())) {
                continue;
            }
            int score = survivalScore(state, node.position(), selfId) - (node.distance() * 4);
            if (best == null || score > bestScore) {
                best = node;
                bestScore = score;
            }
        }
        return best;
    }

    static int distanceToNearestEnemy(GameState state, Position position, String selfId) {
        return BotCombatSupport.distanceToNearestEnemy(state, position, selfId);
    }

    static int distanceToNearestBomb(GameState state, Position position) {
        return BotCombatSupport.distanceToNearestBomb(state, position);
    }

    static int survivalScore(GameState state, Position position, String selfId) {
        return BotCombatSupport.survivalScore(state, position, selfId);
    }

    static PlayerAction isolatedBreakAction(GameState state, IPlayer bot) {
        if (state == null || bot == null || bot.getPosition() == null || !isIsolatedFromEnemies(state, bot)) {
            return null;
        }
        int range = Math.max(1, bot.getBombRange());
        int destructiblesHere = destructiblesHitByBomb(state, bot.getPosition(), range);
        if (canSafelyPlantBomb(state, bot) && destructiblesHere > 0) {
            return new PlayerAction(bot.getId(), ActionType.PLACE_BOMB, null);
        }
        ReachableNode breakPath = pathToIsolationBreakPosition(state, bot);
        if (breakPath != null && breakPath.firstStep() != null) {
            return new PlayerAction(bot.getId(), ActionType.MOVE, breakPath.firstStep());
        }
        return new PlayerAction(bot.getId(), ActionType.NONE, null);
    }

    static boolean isInBlast(GameMap map, Position origin, int range, Position target) {
        return BotCombatSupport.isInBlast(map, origin, range, target);
    }

    private static boolean isThreatenedByExistingAndOwnBomb(GameState state, Position position, Position ownBombOrigin, int ownBombRange) {
        if (isThreatened(state, position)) {
            return true;
        }
        return isInBlast(state.getMap(), ownBombOrigin, ownBombRange, position);
    }

    private static int safeAreaSizeAfterOwnBomb(GameState state, Position start, Position ownBombOrigin, int ownBombRange) {
        ArrayDeque<Position> queue = new ArrayDeque<>();
        Set<Position> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        int count = 0;
        while (!queue.isEmpty()) {
            Position current = queue.removeFirst();
            if (isThreatenedByExistingAndOwnBomb(state, current, ownBombOrigin, ownBombRange)) {
                continue;
            }
            count++;
            for (Direction direction : DIRECTIONS) {
                Position next = current.add(direction);
                if (!visited.add(next)) {
                    continue;
                }
                if (!isWalkableAndFree(state, next)) {
                    continue;
                }
                queue.addLast(next);
            }
        }
        return count;
    }

    private static List<Direction> pathDirections(Position origin, Position target, java.util.Map<Position, Position> parent) {
        java.util.LinkedList<Direction> steps = new java.util.LinkedList<>();
        Position current = target;
        while (current != null && !current.equals(origin)) {
            Position prev = parent.get(current);
            if (prev == null) {
                return List.of();
            }
            steps.addFirst(directionBetween(prev, current));
            current = prev;
        }
        return steps;
    }

    private static Direction directionBetween(Position from, Position to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        for (Direction direction : DIRECTIONS) {
            if (direction.dx() == dx && direction.dy() == dy) {
                return direction;
            }
        }
        throw new IllegalArgumentException("No cardinal direction between positions");
    }

    static int mobilityScore(GameState state, Position position) {
        return BotCombatSupport.mobilityScore(state, position);
    }

    static boolean isExplosionTile(GameState state, Position position) {
        return BotCombatSupport.isExplosionTile(state, position);
    }
}
