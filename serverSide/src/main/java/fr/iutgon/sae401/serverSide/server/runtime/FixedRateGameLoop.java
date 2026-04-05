package fr.iutgon.sae401.serverSide.server.runtime;

import fr.iutgon.sae401.serverSide.game.GameEngine;
import fr.iutgon.sae401.serverSide.game.ServerGameConfig;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @brief Fixed-rate game loop implementation using {@link java.util.concurrent.ScheduledExecutorService}.
 */
public final class FixedRateGameLoop implements GameLoop {
    private final ServerGameConfig config;
    private final GameEngine engine;
    private final String threadName;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> tickTask;
    private volatile ScheduledFuture<?> netTask;

    public FixedRateGameLoop(ServerGameConfig config, GameEngine engine) {
        this(config, engine, "game-loop");
    }

    public FixedRateGameLoop(ServerGameConfig config, GameEngine engine, String threadName) {
        this.config = Objects.requireNonNull(config, "config");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.threadName = Objects.requireNonNull(threadName, "threadName");
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        engine.start();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, threadName));

        long tickPeriodNs = 1_000_000_000L / config.tickRateHz();
        double dtSeconds = 1.0 / config.tickRateHz();
        tickTask = scheduler.scheduleAtFixedRate(() -> engine.tick(dtSeconds), 0, tickPeriodNs, TimeUnit.NANOSECONDS);

        long netPeriodNs = 1_000_000_000L / config.netRateHz();
        netTask = scheduler.scheduleAtFixedRate(engine::netTick, 0, netPeriodNs, TimeUnit.NANOSECONDS);
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        ScheduledFuture<?> t = tickTask;
        if (t != null) {
            t.cancel(true);
        }
        ScheduledFuture<?> n = netTask;
        if (n != null) {
            n.cancel(true);
        }
        ScheduledExecutorService s = scheduler;
        if (s != null) {
            s.shutdownNow();
        }
        engine.stop();
    }
}
