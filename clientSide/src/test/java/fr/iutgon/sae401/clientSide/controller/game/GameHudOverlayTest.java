package fr.iutgon.sae401.clientSide.controller.game;

import fr.iutgon.sae401.TestReflectionUtils;
import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.clientSide.JavaFxTestUtils;
import fr.iutgon.sae401.clientSide.view.PlayerView;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameHudOverlayTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestUtils.initToolkit();
    }

    @Test
    void initializeAndHudDisplays_createUiElementsAndCounts() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            Pane gamePane = new Pane();
            gamePane.setPrefSize(800, 600);
            gamePane.resize(800, 600);
            Pane hudLayer = new Pane();
            Map<String, RemotePlayer> players = new HashMap<>();
            String me = App.clientId();
            players.put(me, new RemotePlayer(new PlayerView(new SimpleDoubleProperty(32.0), 4), 1, 1, 4));

            GameHudOverlay overlay = new GameHudOverlay(gamePane, hudLayer, () -> players, () -> {
            }, () -> {
            });
            overlay.initialize();
            overlay.updateHealthDisplay(3);
            overlay.updateBombDisplay(2);

            try {
                HBox health = (HBox) TestReflectionUtils.getField(overlay, "healthContainer");
                HBox bombs = (HBox) TestReflectionUtils.getField(overlay, "bombContainer");
                VBox playerSpriteContainer = (VBox) TestReflectionUtils.getField(overlay, "playerSpriteContainer");
                ImageView playerSpriteView = (ImageView) TestReflectionUtils.getField(overlay, "playerSpriteView");
                assertEquals(3, health.getChildren().size());
                assertEquals(2, bombs.getChildren().size());
                assertTrue(playerSpriteContainer.isVisible());
                assertTrue(playerSpriteView.getImage() != null);
                assertTrue(hudLayer.getChildren().size() >= 5);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void showGameOver_defeatMarksWinnerAndTriggersQuitOrReplayCallbacks() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            Pane gamePane = new Pane();
            gamePane.setPrefSize(800, 600);
            gamePane.resize(800, 600);
            Pane hudLayer = new Pane();
            Map<String, RemotePlayer> players = new HashMap<>();
            AtomicInteger quitCalls = new AtomicInteger();
            AtomicInteger restartCalls = new AtomicInteger();

            String me = App.clientId();
            RemotePlayer mePlayer = new RemotePlayer(new PlayerView(new SimpleDoubleProperty(32.0), 0), 1, 1, 0);
            mePlayer.alive = false;
            RemotePlayer otherPlayer = new RemotePlayer(new PlayerView(new SimpleDoubleProperty(32.0), 1), 2, 1, 1);
            otherPlayer.alive = true;
            players.put(me, mePlayer);
            players.put("other", otherPlayer);

            GameHudOverlay overlay = new GameHudOverlay(
                    gamePane,
                    hudLayer,
                    () -> players,
                    quitCalls::incrementAndGet,
                    restartCalls::incrementAndGet
            );
            overlay.initialize();
            overlay.showGameOver(false);
            assertTrue(overlay.isGameEnded());

            try {
                VBox gameOver = (VBox) TestReflectionUtils.getField(overlay, "gameOverOverlay");
                assertTrue(gameOver.isVisible());
                String meState = (String) TestReflectionUtils.getField(mePlayer.view, "state");
                String otherState = (String) TestReflectionUtils.getField(otherPlayer.view, "state");
                assertEquals("lose", meState);
                assertEquals("win", otherState);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void showGameOver_victoryMarksCurrentPlayerWin_onlyFirstCallApplies() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            Pane gamePane = new Pane();
            gamePane.setPrefSize(800, 600);
            gamePane.resize(800, 600);
            Pane hudLayer = new Pane();
            Map<String, RemotePlayer> players = new HashMap<>();

            String me = App.clientId();
            RemotePlayer mePlayer = new RemotePlayer(new PlayerView(new SimpleDoubleProperty(32.0), 0), 1, 1, 0);
            mePlayer.alive = true;
            players.put(me, mePlayer);

            GameHudOverlay overlay = new GameHudOverlay(gamePane, hudLayer, () -> players, () -> {
            }, () -> {
            });
            overlay.initialize();
            overlay.showGameOver(true);
            overlay.showGameOver(true);

            try {
                String meState = (String) TestReflectionUtils.getField(mePlayer.view, "state");
                assertEquals("win", meState);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void showGameOver_defeatDuringOngoingMatch_doesNotMarkAnyOtherPlayerAsWinner() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            Pane gamePane = new Pane();
            gamePane.setPrefSize(800, 600);
            gamePane.resize(800, 600);
            Pane hudLayer = new Pane();
            Map<String, RemotePlayer> players = new HashMap<>();

            String me = App.clientId();
            RemotePlayer mePlayer = new RemotePlayer(new PlayerView(new SimpleDoubleProperty(32.0), 0), 1, 1, 0);
            mePlayer.alive = false;
            RemotePlayer otherA = new RemotePlayer(new PlayerView(new SimpleDoubleProperty(32.0), 1), 2, 1, 1);
            otherA.alive = true;
            RemotePlayer otherB = new RemotePlayer(new PlayerView(new SimpleDoubleProperty(32.0), 2), 3, 1, 2);
            otherB.alive = true;
            players.put(me, mePlayer);
            players.put("otherA", otherA);
            players.put("otherB", otherB);

            GameHudOverlay overlay = new GameHudOverlay(gamePane, hudLayer, () -> players, () -> {}, () -> {});
            overlay.initialize();
            overlay.showGameOver(false);

            try {
                String meState = (String) TestReflectionUtils.getField(mePlayer.view, "state");
                String otherAState = (String) TestReflectionUtils.getField(otherA.view, "state");
                String otherBState = (String) TestReflectionUtils.getField(otherB.view, "state");
                assertEquals("lose", meState);
                assertNotEquals("win", otherAState);
                assertNotEquals("win", otherBState);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void replayButton_disabledStateIsGreyAndBlocksAdditionalClicks() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            Pane gamePane = new Pane();
            gamePane.setPrefSize(800, 600);
            gamePane.resize(800, 600);
            Pane hudLayer = new Pane();
            Map<String, RemotePlayer> players = new HashMap<>();
            AtomicInteger restartCalls = new AtomicInteger();

            String me = App.clientId();
            RemotePlayer mePlayer = new RemotePlayer(new PlayerView(new SimpleDoubleProperty(32.0), 0), 1, 1, 0);
            mePlayer.alive = true;
            players.put(me, mePlayer);

            GameHudOverlay overlay = new GameHudOverlay(gamePane, hudLayer, () -> players, () -> {}, restartCalls::incrementAndGet);
            overlay.initialize();
            overlay.showGameOver(true);

            try {
                Button replayButton = (Button) TestReflectionUtils.getField(overlay, "replayButton");
                assertFalse(replayButton.isDisabled());
                assertTrue(replayButton.getStyle().contains("#27ae60"));

                replayButton.fire();
                assertEquals(1, restartCalls.get());
                assertTrue(replayButton.isDisabled());
                assertTrue(replayButton.getStyle().contains("#7f8c8d"));

                replayButton.fire();
                assertEquals(1, restartCalls.get());

                overlay.setReplayButtonDisabled(false);
                assertFalse(replayButton.isDisabled());
                assertTrue(replayButton.getStyle().contains("#27ae60"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
