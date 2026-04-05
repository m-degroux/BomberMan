package fr.iutgon.sae401.serverSide.server.runtime;

import fr.iutgon.sae401.serverSide.game.GameEngine;

import java.util.Objects;

/**
 * @brief Game loop that only calls engine start/stop.
 * <p>
 * Used for engines that manage their own threads/loops.
 */
public final class NoopGameLoop implements GameLoop {
    private final GameEngine engine;

    public NoopGameLoop(GameEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public void start() {
        engine.start();
    }

    @Override
    public void stop() {
        engine.stop();
    }
}
