package fr.iutgon.sae401.common.model.ia;

import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.gameplay.ActionType;
import fr.iutgon.sae401.common.model.gameplay.GameState;
import fr.iutgon.sae401.common.model.gameplay.PlayerAction;

public class EliteBotStrategy extends AbstractMemoryBotStrategy {

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

        PlayerAction isolatedBreak = BotStrategySupport.isolatedBreakAction(state, bot);
        if (isolatedBreak != null && isolatedBreak.getType() != ActionType.NONE) {
            return rememberBombIfNeeded(state, bot, isolatedBreak);
        }

        Position position = bot.getPosition();
        int bombRange = Math.max(1, bot.getBombRange());
        boolean canSafelyBomb = BotStrategySupport.canSafelyPlantBomb(state, bot);
        boolean canHitEnemy = BotStrategySupport.wouldHitEnemy(state, position, bombRange, bot.getId());

        if (BotStrategySupport.isThreatened(state, position)) {
            BotStrategySupport.ReachableNode safest = BotStrategySupport.pathToSafestPosition(state, bot);
            if (safest != null && safest.firstStep() != null) {
                return saferMoveOrNone(state, bot, new PlayerAction(bot.getId(), ActionType.MOVE, safest.firstStep()));
            }
            return new PlayerAction(bot.getId(), ActionType.NONE, null);
        }

        if (canSafelyBomb && canHitEnemy) {
            return rememberBombIfNeeded(state, bot, new PlayerAction(bot.getId(), ActionType.PLACE_BOMB, null));
        }

        BotStrategySupport.ReachableNode attackPath = BotStrategySupport.pathToAttackPosition(state, bot);
        if (attackPath != null) {
            if (attackPath.distance() == 0 && canSafelyBomb && canHitEnemy) {
                return rememberBombIfNeeded(state, bot, new PlayerAction(bot.getId(), ActionType.PLACE_BOMB, null));
            }
            if (attackPath.firstStep() != null) {
                return saferMoveOrNone(state, bot, new PlayerAction(bot.getId(), ActionType.MOVE, attackPath.firstStep()));
            }
        }

        BotStrategySupport.ReachableNode safest = BotStrategySupport.pathToSafestPosition(state, bot);
        if (safest != null && safest.firstStep() != null) {
            return saferMoveOrNone(state, bot, new PlayerAction(bot.getId(), ActionType.MOVE, safest.firstStep()));
        }

        return new PlayerAction(bot.getId(), ActionType.NONE, null);
    }
}
