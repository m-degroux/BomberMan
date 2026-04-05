package fr.iutgon.sae401.common.model.map;

/**
 * Représente les différents types de cases dans la carte du jeu.
 */
public enum TileType {
    /**
     * Case libre sur laquelle les joueurs peuvent se déplacer.
     */
    GROUND,

    /**
     * Mur indestructible bloquant le passage et les explosions.
     */
    WALL,

    /**
     * Mur destructible pouvant être détruit par une explosion.
     */
    DESTRUCTIBLE
}
