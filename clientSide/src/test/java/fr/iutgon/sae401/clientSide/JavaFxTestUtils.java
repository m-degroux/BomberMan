package fr.iutgon.sae401.clientSide;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class JavaFxTestUtils {

    private static final Object LOCK = new Object();
    private static boolean initialized = false;

    private JavaFxTestUtils() {
    }

    public static void initToolkit() throws Exception {
        synchronized (LOCK) {
            if (initialized) {
                return;
            }
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(latch::countDown);
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("JavaFX toolkit did not initialize in time");
                }
            } catch (IllegalStateException ex) {
                // Toolkit already initialized in another test run.
            }
            initialized = true;
        }
    }

    public static void runAndWait(Runnable action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX action did not complete in time");
        }
    }
}
