package fr.iutgon.sae401.common.model.map;

import fr.iutgon.sae401.common.json.Json;

/**
 * Représente une case individuelle de la carte.
 * Une case possède un type qui détermine son comportement.
 */
public class Tile {

    private TileType type;

    /**
     * Constructeur d'une case.
     *
     * @param type type de la case
     */
    public Tile(TileType type) {
        this.type = type;
    }

    public static Tile fromJson(String json) {
        return fromJson(Json.parse(json));
    }

    public static Tile fromJson(Json json) {
        if (json == null || json.isNull()) {
            return null;
        }
        if (json.isString()) {
            String value = json.getString();
            TileType type = TileType.valueOf(value);
            return new Tile(type);
        }
        // Fallback permissif
        String value = json.stringify().replace("\"", "");
        TileType type = TileType.valueOf(value);
        return new Tile(type);
    }

    /**
     * Indique si la case est traversable par un joueur.
     *
     * @return true si la case est libre (GROUND)
     */
    public boolean isWalkable() {
        return type == TileType.GROUND;
    }

    /**
     * Indique si la case peut être détruite par une explosion.
     *
     * @return true si destructible
     */
    public boolean isDestructible() {
        return type == TileType.DESTRUCTIBLE;
    }

    /**
     * Détruit la case si elle est destructible.
     * La transforme en sol.
     */
    public void destroy() {
        if (isDestructible()) {
            type = TileType.GROUND;
        }
    }

    /**
     * Retourne le type de la case.
     *
     * @return type de la case
     */
    public TileType getType() {
        return type;
    }

    /**
     * Définit le type de la case.
     *
     * @param type nouveau type
     */
    public void setType(TileType type) {
        this.type = type;
    }

    /**
     * Convertit la case en JSON.
     */
    public Json toJson() {
        return Json.of(type.name());
    }
}
