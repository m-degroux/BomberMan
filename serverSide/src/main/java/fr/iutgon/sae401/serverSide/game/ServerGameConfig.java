package fr.iutgon.sae401.serverSide.game;

/**
 * @param tickRateHz simulation tick rate (authoritative game state updates)
 * @param netRateHz  network update rate (how often to push state to clients)
 * @brief Game loop configuration.
 */
public record ServerGameConfig(int tickRateHz, int netRateHz) {
    public ServerGameConfig {
        if (tickRateHz <= 0) {
            throw new IllegalArgumentException("tickRateHz must be > 0");
        }
        if (netRateHz <= 0) {
            throw new IllegalArgumentException("netRateHz must be > 0");
        }
    }
}
