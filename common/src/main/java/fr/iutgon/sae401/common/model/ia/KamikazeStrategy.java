package fr.iutgon.sae401.common.model.ia;

import fr.iutgon.sae401.common.model.entity.Direction;
import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.gameplay.ActionType;
import fr.iutgon.sae401.common.model.gameplay.GameState;
import fr.iutgon.sae401.common.model.gameplay.PlayerAction;

import java.util.List;

public class KamikazeStrategy extends AbstractMemoryBotStrategy {
    private static final int FAR_TARGET_BREAK_DISTANCE = 5;

    @Override
    public PlayerAction decideNextMove(GameState state, IPlayer bot) {
        decayRecentDanger();
        rememberCurrentDangers(state);
        if (state == null || bot == null || bot.getPosition() == null || !bot.isAlive()) {
            return new PlayerAction(bot == null ? null : bot.getId(), ActionType.NONE, null);
        }

        PlayerAction escapeAction = continuePlannedEscape(state, bot);
        if (escapeAction != null) {
            return saferMoveOrNone(state, bot, escapeAction);
        }

        Position botPos = bot.getPosition();
        IPlayer target = BotStrategySupport.nearestEnemy(state, bot);

        if (target == null) {
            return new PlayerAction(bot.getId(), ActionType.NONE, null);
        }

        Position targetPos = target.getPosition();
        int bombRange = Math.max(1, bot.getBombRange());
        boolean canBomb = BotStrategySupport.canSafelyPlantBomb(state, bot)
                && BotStrategySupport.hasReliableEscapePlan(state, bot, 3, 4);
        boolean bombWouldHitEnemy = BotStrategySupport.wouldHitEnemy(state, botPos, bombRange, bot.getId());

        if (BotStrategySupport.isThreatened(state, botPos)) {
            BotStrategySupport.ReachableNode safePath = BotStrategySupport.pathToSafestPosition(state, bot);
            if (safePath != null && safePath.firstStep() != null) {
                return saferMoveOrNone(state, bot, new PlayerAction(bot.getId(), ActionType.MOVE, safePath.firstStep()));
            }
            return new PlayerAction(bot.getId(), ActionType.NONE, null);
        }

        if (canBomb && bombWouldHitEnemy) {
            return rememberBombIfNeeded(state, bot, new PlayerAction(bot.getId(), ActionType.PLACE_BOMB, null));
        }

        BotStrategySupport.ReachableNode attackPath = BotStrategySupport.pathToAttackPosition(state, bot);
        if (attackPath != null && attackPath.firstStep() != null) {
            return saferMoveOrNone(state, bot, new PlayerAction(bot.getId(), ActionType.MOVE, attackPath.firstStep()));
        }

        PlayerAction isolatedBreak = BotStrategySupport.isolatedBreakAction(state, bot);
        if (isolatedBreak != null && isolatedBreak.getType() != ActionType.NONE) {
            if (isolatedBreak.getType() == ActionType.PLACE_BOMB) {
                boolean nearEnoughTarget = botPos.distance(targetPos) <= FAR_TARGET_BREAK_DISTANCE;
                boolean reliableBreakEscape = BotStrategySupport.hasReliableEscapePlan(state, bot, 4, 3);
                if (nearEnoughTarget && reliableBreakEscape) {
                    return rememberBombIfNeeded(state, bot, isolatedBreak);
                }
            } else {
                return saferMoveOrNone(state, bot, isolatedBreak);
            }
        }

        List<Direction> validMoves = BotStrategySupport.validMoves(state, botPos);
        if (validMoves.isEmpty()) {
            return new PlayerAction(bot.getId(), ActionType.NONE, null);
        }

        Direction moveDir = validMoves.stream()
                .min(java.util.Comparator.comparingInt(direction -> botPos.add(direction).distance(targetPos)))
                .orElse(null);
        return moveDir == null
                ? new PlayerAction(bot.getId(), ActionType.NONE, null)
                : saferMoveOrNone(state, bot, new PlayerAction(bot.getId(), ActionType.MOVE, moveDir));
    }
}
