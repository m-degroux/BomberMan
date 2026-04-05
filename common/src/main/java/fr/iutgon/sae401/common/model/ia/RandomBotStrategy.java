package fr.iutgon.sae401.common.model.ia;

import fr.iutgon.sae401.common.model.entity.Direction;
import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.gameplay.ActionType;
import fr.iutgon.sae401.common.model.gameplay.GameState;
import fr.iutgon.sae401.common.model.gameplay.PlayerAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * IA imprévisible, mais qui évite les coups manifestement suicidaires.
 */
public class RandomBotStrategy extends AbstractMemoryBotStrategy {

    private final Random random = new Random();

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

        Position origin = bot.getPosition();
        List<Direction> validMoves = BotStrategySupport.validMoves(state, origin);
        List<Direction> safeMoves = validMoves.stream()
                .filter(direction -> !BotStrategySupport.isThreatened(state, origin.add(direction)))
                .toList();

        if (BotStrategySupport.isThreatened(state, origin) && !safeMoves.isEmpty()) {
            Direction escape = safeMoves.get(random.nextInt(safeMoves.size()));
            return saferMoveOrNone(state, bot, new PlayerAction(bot.getId(), ActionType.MOVE, escape));
        }

        List<PlayerAction> weightedChoices = new ArrayList<>();
        for (Direction direction : validMoves) {
            weightedChoices.add(new PlayerAction(bot.getId(), ActionType.MOVE, direction));
            if (!BotStrategySupport.isThreatened(state, origin.add(direction))) {
                weightedChoices.add(new PlayerAction(bot.getId(), ActionType.MOVE, direction));
            }
        }

        boolean canBomb = BotStrategySupport.canSafelyPlantBomb(state, bot);
        boolean bombHitsEnemy = BotStrategySupport.wouldHitEnemy(state, origin, bot.getBombRange(), bot.getId());
        if (canBomb && bombHitsEnemy) {
            weightedChoices.add(rememberBombIfNeeded(state, bot, new PlayerAction(bot.getId(), ActionType.PLACE_BOMB, null)));
            weightedChoices.add(rememberBombIfNeeded(state, bot, new PlayerAction(bot.getId(), ActionType.PLACE_BOMB, null)));
        }

        weightedChoices.add(new PlayerAction(bot.getId(), ActionType.NONE, null));
        if (!weightedChoices.isEmpty()) {
            return saferMoveOrNone(state, bot, weightedChoices.get(random.nextInt(weightedChoices.size())));
        }

        if (canBomb && bombHitsEnemy) {
            return rememberBombIfNeeded(state, bot, new PlayerAction(bot.getId(), ActionType.PLACE_BOMB, null));
        }
        return new PlayerAction(bot.getId(), ActionType.NONE, null);
    }
}
