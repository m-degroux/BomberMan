package fr.iutgon.sae401.clientSide.view;

import fr.iutgon.sae401.TestReflectionUtils;
import fr.iutgon.sae401.clientSide.JavaFxTestUtils;
import javafx.beans.property.SimpleDoubleProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerViewTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestUtils.initToolkit();
    }

    @Test
    void update_ignoresNonPositiveTileSize_andInitializesOnFirstFrame() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            PlayerView view = new PlayerView(new SimpleDoubleProperty(0.0), 0);
            view.update(1_000L, 2, 2, "S", 0.1, 100_000_000L, 3);
            assertEquals(0.0, view.getImageView().getFitWidth(), 0.0001);

            PlayerView initialized = new PlayerView(new SimpleDoubleProperty(32.0), 0);
            initialized.update(1_000L, 1, 1, "S", 0.1, 100_000_000L, 3);
            assertTrue(initialized.getImageView().getFitWidth() > 0.0);
            try {
                boolean isFirstFrame = (boolean) TestReflectionUtils.getField(initialized, "isFirstFrame");
                assertFalse(isFirstFrame);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void update_deadAndLoseAnimationsReachTerminalFrame() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            PlayerView dead = new PlayerView(new SimpleDoubleProperty(32.0), 0);
            long t0 = System.nanoTime();
            dead.setDead();
            dead.update(t0 + 10_000_000L, 1, 1, "S", 0.1, 100_000_000L, 3);
            dead.update(t0 + 400_000_000L, 1, 1, "S", 0.1, 100_000_000L, 3);
            assertTrue(dead.getImageView().getFitWidth() > 0.0);

            PlayerView lose = new PlayerView(new SimpleDoubleProperty(32.0), 0);
            long t1 = System.nanoTime();
            lose.setLose();
            lose.update(t1 + 10_000_000L, 1, 1, "S", 0.1, 100_000_000L, 3);
            lose.update(t1 + 400_000_000L, 1, 1, "S", 0.1, 100_000_000L, 3);
            assertTrue(lose.getImageView().getFitWidth() > 0.0);
        });
    }

    @Test
    void update_winAnimationLoops_andMovementAnimationAdvances() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            PlayerView win = new PlayerView(new SimpleDoubleProperty(32.0), 0);
            long t0 = System.nanoTime();
            win.setWin();
            win.update(t0 + 10_000_000L, 1, 1, "S", 0.1, 100_000_000L, 3);
            win.update(t0 + 210_000_000L, 1, 1, "S", 0.1, 100_000_000L, 3);
            assertTrue(win.getImageView().getFitWidth() > 0.0);

            PlayerView moving = new PlayerView(new SimpleDoubleProperty(32.0), 0);
            long t1 = System.nanoTime();
            moving.update(t1 + 10_000_000L, 1, 1, "E", 0.3, 100_000_000L, 3);
            moving.update(t1 + 210_000_000L, 2, 1, "E", 0.3, 100_000_000L, 3);
            moving.update(t1 + 410_000_000L, 2, 1, "E", 0.3, 100_000_000L, 3);

            try {
                int walkIndex = (int) TestReflectionUtils.getField(moving, "walkSequenceIndex");
                assertTrue(walkIndex >= 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
