package fr.iutgon.sae401.common.model.ia;

import fr.iutgon.sae401.common.model.entity.Direction;
import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.gameplay.ActionType;
import fr.iutgon.sae401.common.model.gameplay.GameState;
import fr.iutgon.sae401.common.model.gameplay.PlayerAction;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

abstract class AbstractMemoryBotStrategy implements IBotStrategy {
    private static final int RECENT_DANGER_TICKS = 6;

    private final ArrayDeque<Direction> plannedEscape = new ArrayDeque<>();
    private Position plannedBombOrigin;
    private final Map<Position, Integer> recentDangerTiles = new HashMap<>();

    protected final void rememberCurrentDangers(GameState state) {
        if (state == null) {
            return;
        }
        for (var bomb : state.getBombs()) {
            if (bomb == null || bomb.getPosition() == null) {
                continue;
            }
            recentDangerTiles.put(bomb.getPosition(), RECENT_DANGER_TICKS);
        }
        for (var explosion : state.getExplosions()) {
            if (explosion == null || explosion.getAffectedTiles() == null) {
                continue;
            }
            for (Position tile : explosion.getAffectedTiles()) {
                if (tile != null) {
                    recentDangerTiles.put(tile, RECENT_DANGER_TICKS);
                }
            }
        }
    }

    protected final void decayRecentDanger() {
        Iterator<Map.Entry<Position, Integer>> iterator = recentDangerTiles.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Position, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    protected final boolean isRecentlyDangerous(Position position) {
        return position != null && recentDangerTiles.containsKey(position);
    }

    protected final PlayerAction continuePlannedEscape(GameState state, IPlayer bot) {
        if (state == null || bot == null || bot.getPosition() == null) {
            clearEscapePlan();
            return null;
        }
        if (plannedEscape.isEmpty()) {
            if (plannedBombOrigin != null && !BotStrategySupport.hasBombAt(state, plannedBombOrigin)) {
                clearEscapePlan();
            }
            return null;
        }

        if (plannedBombOrigin == null || !BotStrategySupport.hasBombAt(state, plannedBombOrigin)) {
            clearEscapePlan();
            return null;
        }

        while (!plannedEscape.isEmpty()) {
            Direction next = plannedEscape.peekFirst();
            Position nextPos = bot.getPosition().add(next);
            if (!BotStrategySupport.isWalkableAndFree(state, nextPos) || BotStrategySupport.isExplosionTile(state, nextPos)) {
                clearEscapePlan();
                return null;
            }
            plannedEscape.removeFirst();
            if (plannedEscape.isEmpty() && !BotStrategySupport.isThreatened(state, nextPos)) {
                clearEscapePlan();
            }
            return new PlayerAction(bot.getId(), ActionType.MOVE, next);
        }
        clearEscapePlan();
        return null;
    }

    protected final PlayerAction rememberBombIfNeeded(GameState state, IPlayer bot, PlayerAction action) {
        if (action == null || action.getType() != ActionType.PLACE_BOMB || state == null || bot == null || bot.getPosition() == null) {
            return action;
        }
        BotStrategySupport.EscapePlan plan = BotStrategySupport.findEscapePlanAfterOwnBomb(state, bot.getPosition(), Math.max(1, bot.getBombRange()));
        if (plan == null || plan.steps().isEmpty()) {
            return new PlayerAction(bot.getId(), ActionType.NONE, null);
        }
        plannedEscape.clear();
        for (Direction step : plan.steps()) {
            plannedEscape.addLast(step);
        }
        plannedBombOrigin = bot.getPosition();
        return action;
    }

    protected final PlayerAction saferMoveOrNone(GameState state, IPlayer bot, PlayerAction candidate) {
        if (candidate == null || candidate.getType() != ActionType.MOVE || candidate.getDirection() == null || bot == null || bot.getPosition() == null) {
            return candidate;
        }
        Position next = bot.getPosition().add(candidate.getDirection());
        if (BotStrategySupport.isExplosionTile(state, next)) {
            return new PlayerAction(bot.getId(), ActionType.NONE, null);
        }
        if (!BotStrategySupport.isThreatened(state, bot.getPosition()) && isRecentlyDangerous(next)) {
            List<Direction> alternatives = BotStrategySupport.validMoves(state, bot.getPosition()).stream()
                    .filter(direction -> {
                        Position pos = bot.getPosition().add(direction);
                        return !isRecentlyDangerous(pos) && !BotStrategySupport.isExplosionTile(state, pos);
                    })
                    .toList();
            if (!alternatives.isEmpty()) {
                return new PlayerAction(bot.getId(), ActionType.MOVE, alternatives.getFirst());
            }
        }
        return candidate;
    }

    protected final void clearEscapePlan() {
        plannedEscape.clear();
        plannedBombOrigin = null;
    }

    protected final List<Direction> plannedEscapeSnapshot() {
        return List.copyOf(plannedEscape);
    }
}
