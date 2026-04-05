package fr.iutgon.sae401.common.model.ia;

import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.gameplay.ActionType;
import fr.iutgon.sae401.common.model.gameplay.GameState;
import fr.iutgon.sae401.common.model.gameplay.PlayerAction;

public class SurvivorStrategy extends AbstractMemoryBotStrategy {

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

        Position myPos = bot.getPosition();
        int bombRange = Math.max(1, bot.getBombRange());
        boolean canBomb = BotStrategySupport.canSafelyPlantBomb(state, bot)
                && BotStrategySupport.hasReliableEscapePlan(state, bot, 3, 4);
        boolean canHitEnemy = BotStrategySupport.wouldHitEnemy(state, myPos, bombRange, bot.getId());
        boolean threatened = BotStrategySupport.isThreatened(state, myPos);

        if (threatened) {
            BotStrategySupport.ReachableNode safePath = BotStrategySupport.pathToSafestPosition(state, bot);
            if (safePath != null && safePath.firstStep() != null) {
                return saferMoveOrNone(state, bot, new PlayerAction(bot.getId(), ActionType.MOVE, safePath.firstStep()));
            }
            if (canBomb && canHitEnemy) {
                return rememberBombIfNeeded(state, bot, new PlayerAction(bot.getId(), ActionType.PLACE_BOMB, null));
            }
            return new PlayerAction(bot.getId(), ActionType.NONE, null);
        }

        if (canBomb && canHitEnemy) {
            return rememberBombIfNeeded(state, bot, new PlayerAction(bot.getId(), ActionType.PLACE_BOMB, null));
        }

        BotStrategySupport.ReachableNode attackPath = BotStrategySupport.pathToAttackPosition(state, bot);
        if (attackPath != null) {
            if (attackPath.distance() == 0 && canBomb) {
                return rememberBombIfNeeded(state, bot, new PlayerAction(bot.getId(), ActionType.PLACE_BOMB, null));
            }
            if (attackPath.firstStep() != null) {
                return saferMoveOrNone(state, bot, new PlayerAction(bot.getId(), ActionType.MOVE, attackPath.firstStep()));
            }
        }

        PlayerAction isolatedBreak = BotStrategySupport.isolatedBreakAction(state, bot);
        if (isolatedBreak != null && isolatedBreak.getType() != ActionType.NONE) {
            if (isolatedBreak.getType() == ActionType.PLACE_BOMB
                    && !BotStrategySupport.hasReliableEscapePlan(state, bot, 4, 3)) {
                isolatedBreak = null;
            } else {
                return rememberBombIfNeeded(state, bot, isolatedBreak);
            }
        }

        BotStrategySupport.ReachableNode safePath = BotStrategySupport.pathToSafestPosition(state, bot);
        if (safePath != null && safePath.firstStep() != null) {
            return saferMoveOrNone(state, bot, new PlayerAction(bot.getId(), ActionType.MOVE, safePath.firstStep()));
        }

        return new PlayerAction(bot.getId(), ActionType.NONE, null);
    }
}
