package fr.iutgon.sae401.common.model.ia;

import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.gameplay.GameState;
import fr.iutgon.sae401.common.model.gameplay.PlayerAction;

public interface IBotStrategy {

    PlayerAction decideNextMove(GameState state, IPlayer botInfo);
}
