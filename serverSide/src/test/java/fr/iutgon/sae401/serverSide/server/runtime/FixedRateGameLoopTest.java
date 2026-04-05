package fr.iutgon.sae401.serverSide.server.runtime;

import fr.iutgon.sae401.serverSide.game.GameEngine;
import fr.iutgon.sae401.serverSide.game.ServerGameConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedRateGameLoopTest {
    @Test
    void callsTickAndNetTick() throws Exception {
        var tickLatch = new CountDownLatch(3);
        var netLatch = new CountDownLatch(2);
        var ticks = new AtomicInteger(0);
        var nets = new AtomicInteger(0);

        GameEngine engine = new GameEngine() {
            @Override
            public void tick(double dtSeconds) {
                ticks.incrementAndGet();
                tickLatch.countDown();
            }

            @Override
            public void netTick() {
                nets.incrementAndGet();
                netLatch.countDown();
            }
        };

        var loop = new FixedRateGameLoop(new ServerGameConfig(60, 30), engine);
        loop.start();
        try {
            assertTrue(tickLatch.await(1500, TimeUnit.MILLISECONDS), "tick not called enough");
            assertTrue(netLatch.await(1500, TimeUnit.MILLISECONDS), "netTick not called enough");
            assertTrue(ticks.get() >= 3);
            assertTrue(nets.get() >= 2);
        } finally {
            loop.stop();
        }
    }

    @Test
    void startIsIdempotentAndStopIsIdempotent() {
        GameEngine engine = dtSeconds -> {
        };
        var loop = new FixedRateGameLoop(new ServerGameConfig(60, 20), engine);
        loop.start();
        loop.start();
        loop.stop();
        loop.stop();
    }
}
