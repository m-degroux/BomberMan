package fr.iutgon.sae401.common.model.gameplay;

/**
 * Représente la configuration d'une partie. Contient tous les paramètres
 * nécessaires à l'initialisation du jeu.
 */
public class GameConfig {

    private final int width;
    private final int height;
    private final int initialHealth;
    private final int maxBombs;
    private final int bombRange;
    private final int bombTimer;
    private final int bombCooldownMs;
    private final float destructibleDensity;

    /**
     * Initialise une configuration de partie.
     *
     * @param width               largeur de la carte
     * @param height              hauteur de la carte
     * @param initialHealth       points de vie initiaux
     * @param maxBombs            nombre maximum de bombes
     * @param bombRange           portée des explosions
     * @param bombTimer           temps avant explosion
     * @param destructibleDensity probabilité de murs destructibles (0.0 à 1.0)
     */
    public GameConfig(int width, int height, int initialHealth, int maxBombs, int bombRange, int bombTimer,
                      float destructibleDensity) {

        if (destructibleDensity < 0f || destructibleDensity > 1f) {
            throw new IllegalArgumentException("La densité doit être entre 0 et 1");
        }

        this.width = width;
        this.height = height;
        this.initialHealth = initialHealth;
        this.maxBombs = maxBombs;
        this.bombRange = bombRange;
        this.bombTimer = bombTimer;
        this.bombCooldownMs = bombTimer;
        this.destructibleDensity = destructibleDensity;
    }

    /**
     * Variante avec cooldown explicite.
     *
     * @param bombCooldownMs temps de recharge entre deux poses de bombe (ms)
     */
    public GameConfig(int width, int height, int initialHealth, int maxBombs, int bombRange, int bombTimer,
                      int bombCooldownMs, float destructibleDensity) {

        if (destructibleDensity < 0f || destructibleDensity > 1f) {
            throw new IllegalArgumentException("La densité doit être entre 0 et 1");
        }

        this.width = width;
        this.height = height;
        this.initialHealth = initialHealth;
        this.maxBombs = maxBombs;
        this.bombRange = bombRange;
        this.bombTimer = bombTimer;
        this.bombCooldownMs = bombCooldownMs;
        this.destructibleDensity = destructibleDensity;
    }

    /**
     * Initialise une configuration de partie.
     *
     * @param width               largeur de la carte
     * @param height              hauteur de la carte
     * @param initialHealth       points de vie initiaux
     * @param maxBombs            nombre maximum de bombes
     * @param bombRange           portée des explosions
     * @param bombTimer           temps avant explosion
     * @param destructibleDensity probabilité de murs destructibles (0% à 100%)
     */
    public GameConfig(int width, int height, int initialHealth, int maxBombs, int bombRange, int bombTimer,
                      int destructibleDensity) {

        this.width = width;
        this.height = height;
        this.initialHealth = initialHealth;
        this.maxBombs = maxBombs;
        this.bombRange = bombRange;
        this.bombTimer = bombTimer;
        this.bombCooldownMs = bombTimer;
        this.destructibleDensity = destructibleDensity / 100f;
    }

    /**
     * Variante avec cooldown explicite.
     *
     * @param bombCooldownMs temps de recharge entre deux poses de bombe (ms)
     */
    public GameConfig(int width, int height, int initialHealth, int maxBombs, int bombRange, int bombTimer,
                      int bombCooldownMs, int destructibleDensity) {

        this.width = width;
        this.height = height;
        this.initialHealth = initialHealth;
        this.maxBombs = maxBombs;
        this.bombRange = bombRange;
        this.bombTimer = bombTimer;
        this.bombCooldownMs = bombCooldownMs;
        this.destructibleDensity = destructibleDensity / 100f;
    }

    /**
     * Initialise une configuration de partie avec des paramètres par défaut.
     */
    public GameConfig() {
        this.width = 15;
        this.height = 13;
        this.initialHealth = 3;
        this.maxBombs = 1;
        this.bombRange = 2;
		this.bombTimer = 2000;
        this.bombCooldownMs = 2000;
        this.destructibleDensity = 0.6f;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getInitialHealth() {
        return initialHealth;
    }

    public int getMaxBombs() {
        return maxBombs;
    }

    public int getBombRange() {
        return bombRange;
    }

    public int getBombTimer() {
        return bombTimer;
    }

    public int getBombCooldownMs() {
        return bombCooldownMs;
    }

    public float getDestructibleDensity() {
        return destructibleDensity;
    }
}