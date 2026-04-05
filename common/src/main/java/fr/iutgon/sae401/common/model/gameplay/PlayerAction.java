package fr.iutgon.sae401.common.model.gameplay;

import fr.iutgon.sae401.common.model.entity.Direction;

/**
 * Représente une action effectuée par un joueur.
 * Utile pour la communication client → serveur.
 */
public class PlayerAction {

    private final String playerId;
    private final ActionType type;
    private final Direction direction;

    public PlayerAction(String playerId, ActionType type, Direction direction) {
        this.playerId = playerId;
        this.type = type;
        this.direction = direction;
    }

    public String getPlayerId() {
        return playerId;
    }

    public ActionType getType() {
        return type;
    }

    public Direction getDirection() {
        return direction;
    }
}
