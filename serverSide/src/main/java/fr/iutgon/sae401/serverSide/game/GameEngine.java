package fr.iutgon.sae401.serverSide.game;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;

/**
 * @brief Authoritative game logic executed on the server.
 * <p>
 * The engine owns the game state and is driven by a {@link fr.iutgon.sae401.serverSide.server.runtime.GameLoop}.
 * <p>
 * Recommended concurrency model:
 * - network threads enqueue inputs
 * - the game loop thread calls {@link #tick(double)} and applies inputs
 */
public interface GameEngine {
    /**
     * @brief Called once before ticks start.
     */
    default void start() {
    }

    /**
     * @brief Called once after the loop stops.
     */
    default void stop() {
    }

    /**
     * @brief Called when a client connects.
     */
    default void onClientConnected(ClientId clientId) {
    }

    /**
     * @brief Called when a client disconnects.
     */
    default void onClientDisconnected(ClientId clientId) {
    }

    /**
     * @param clientId client identifier
     * @param payload  game-defined input payload
     * @brief Receive a client input message.
     * <p>
     * This method is typically invoked on a network thread. Prefer enqueueing the
     * input into a thread-safe queue and applying it during {@link #tick(double)}.
     */
    default void onInput(ClientId clientId, Json payload) {
    }

    /**
     * @param dtSeconds fixed delta time in seconds (typically 1/tickRateHz)
     * @brief Simulation tick.
     */
    void tick(double dtSeconds);

    /**
     * @brief Network tick.
     * <p>
     * This is where the engine may decide to push state updates to clients.
     * Default: no-op.
     */
    default void netTick() {
    }
}
