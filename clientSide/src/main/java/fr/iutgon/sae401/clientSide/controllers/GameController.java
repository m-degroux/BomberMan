package fr.iutgon.sae401.clientSide.controller;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.clientSide.controller.game.GameHudOverlay;
import fr.iutgon.sae401.clientSide.controller.game.GameMapRenderer;
import fr.iutgon.sae401.clientSide.controller.game.GameStateSynchronizer;
import fr.iutgon.sae401.clientSide.controller.game.RemotePlayer;
import fr.iutgon.sae401.clientSide.network.NetworkManager;
import fr.iutgon.sae401.clientSide.network.NetworkObserver;
import fr.iutgon.sae401.clientSide.service.LocalReplayContext;
import fr.iutgon.sae401.clientSide.service.LocalServerManager;
import fr.iutgon.sae401.clientSide.utils.GlobalKeyCapture;
import fr.iutgon.sae401.clientSide.utils.KeyBindingsManager;
import fr.iutgon.sae401.clientSide.view.ChatOverlay;
import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class GameController implements NetworkObserver {
    private static final Logger LOGGER = Logger.getLogger();
    private static final double MOVE_SPEED = 0.098;
    private static final double MAX_RENDER_SPEED_MULTIPLIER = 1.20;
    private static final long PLAYER_ANIM_HZ = 350_000_000L;
    private static final NetworkManager networkManager = NetworkManager.getManager();

    @FXML
    private Pane gamePane;

    public enum InputState {
        GAME_PLAYING,
        CHAT_TYPING
    }

    private InputState currentInputState = InputState.GAME_PLAYING;
    private final Pane worldLayer = new Pane();
    private final Pane floorLayer = new Pane();
    private final Pane objectLayer = new Pane();
    private final Pane bonusLayer = new Pane();
    private final Pane bombLayer = new Pane();
    private final Pane explosionLayer = new Pane();
    private final Pane entityLayer = new Pane();
    private final Pane hudLayer = new Pane();
    private final DoubleProperty tileSize = new SimpleDoubleProperty();
    private final javafx.event.EventHandler<KeyEvent> keyFilter = this::handleInput;
    private final Consumer<javafx.scene.input.KeyCode> globalKeyListener = this::handleGlobalKey;

    // Enabled by default to support multi-client play on the same machine.
    // Can be disabled with: -Dsae.client.globalCapture=false
    private final boolean useGlobalCapture =
            Boolean.parseBoolean(System.getProperty("sae.client.globalCapture", "true"));
    private boolean cleanedUp = false;
    private AnimationTimer animationTimer;
    private ChatOverlay chatOverlay;

    private GameMapRenderer mapRenderer;
    private GameStateSynchronizer stateSynchronizer;
    private GameHudOverlay hudOverlay;

    @FXML
    public void initialize() {
        networkManager.addObserver(this);

        gamePane.setStyle("-fx-background-color: #0f1021;");
        worldLayer.getChildren().addAll(floorLayer, objectLayer, bonusLayer, bombLayer, explosionLayer, entityLayer);
        gamePane.getChildren().addAll(worldLayer, hudLayer);
        gamePane.setFocusTraversable(true);
        Platform.runLater(gamePane::requestFocus);

        GameMap placeholder = GameMapRenderer.createPlaceholderMap(15, 13);
        mapRenderer = new GameMapRenderer(gamePane, worldLayer, floorLayer, objectLayer, tileSize, () -> {
            if (stateSynchronizer == null || stateSynchronizer.gameMap() == null) {
                return placeholder;
            }
            return stateSynchronizer.gameMap();
        });
        stateSynchronizer = new GameStateSynchronizer(
                tileSize,
                entityLayer,
                bombLayer,
                bonusLayer,
                explosionLayer,
                App::clientId,
                () -> cleanedUp,
                this::isGameEnded,
                this::showGameOver,
                this::updateHealthDisplay,
                this::updateBombDisplay,
                this::redrawMap
        );
        stateSynchronizer.setInitialMap(placeholder);
        mapRenderer.setupResponsiveGrid();
        redrawMap();

        hudOverlay = new GameHudOverlay(
                gamePane,
                hudLayer,
                stateSynchronizer::players,
                this::quitCurrentGame,
                this::requestRestart
        );
        hudOverlay.initialize();
        if (!LocalServerManager.isLocalModeEnabled()) {
            setupChat();
        } else {
            App.LOGGER.log(new LogMessage("[GAME] Chat overlay disabled in local mode", LogLevel.INFO));
        }

        var gamepadManager = App.getGamepadInputManager();
        gamepadManager.setSelectedController(App.getKeyBindingsManager().getSelectedGamepad());

        if (useGlobalCapture) {
            GlobalKeyCapture.getInstance().start();
            GlobalKeyCapture.getInstance().addKeyListener(globalKeyListener);
            App.LOGGER.log(new LogMessage("[GAME] Capture globale des touches activée", LogLevel.INFO));
        }

        gamePane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (!useGlobalCapture && oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            }
            if (newScene != null) {
                if (!useGlobalCapture) {
                    newScene.addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
                }
                Platform.runLater(gamePane::requestFocus);
            } else {
                cleanupLocalCapture();
                cleanup(false);
            }
        });
        Platform.runLater(() -> {
            var scene = gamePane.getScene();
            if (!useGlobalCapture && scene != null) {
                scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
                scene.addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
                gamePane.requestFocus();
            }
        });

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                for (RemotePlayer p : stateSynchronizer.players().values()) {
                    double renderSpeedMultiplier = Math.max(0.1, Math.min(MAX_RENDER_SPEED_MULTIPLIER, p.speed));
                    double moveSpeed = MOVE_SPEED * renderSpeedMultiplier;
                    p.view.update(now, p.x, p.y, p.dir, moveSpeed, PLAYER_ANIM_HZ, 3);
                }
                stateSynchronizer.updateBombAnimations(now);
                handleGamepadInput(now);
            }
        };
        animationTimer.start();
    }

    private void redrawMap() {
        mapRenderer.drawMap(stateSynchronizer.gameMap());
    }

    private boolean isGameEnded() {
        return hudOverlay != null && hudOverlay.isGameEnded();
    }

    private void showGameOver(boolean victory) {
        if (hudOverlay != null) {
            hudOverlay.showGameOver(victory);
        }
    }

    private void updateHealthDisplay(int health) {
        if (hudOverlay != null) {
            hudOverlay.updateHealthDisplay(health);
        }
    }

    private void updateBombDisplay(int bombCount) {
        if (hudOverlay != null) {
            hudOverlay.updateBombDisplay(bombCount);
        }
    }

    private void handleGamepadInput(long nowNs) {
        if (isGameEnded()) {
            return;
        }
        var action = App.getGamepadInputManager().poll(nowNs);
        if (action == fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.NONE) {
            return;
        }
        applyAction(action);
    }

    private void cleanup(boolean sendLeaveRoom) {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;

        try {
            if (sendLeaveRoom) {
                App.sendToServer(new MessageEnvelope("leave_room", UUID.randomUUID().toString(), Json.emptyObject()));
            }
        } catch (Exception ex) {
            App.LOGGER.log(new LogMessage("[GAME] Failed to send leave_room: " + ex.getMessage(), LogLevel.WARNING));
        }

        try {
            networkManager.removeObserver(this);
        } catch (Exception ignored) {
        }

        try {
            var scene = gamePane.getScene();
            if (scene != null) {
                scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            }
        } catch (Exception ignored) {
        }

        try {
            if (animationTimer != null) {
                animationTimer.stop();
            }
        } catch (Exception ignored) {
        }
    }

    private void cleanupLocalCapture() {
        if (useGlobalCapture) {
            GlobalKeyCapture.getInstance().removeKeyListener(globalKeyListener);
            App.LOGGER.log(new LogMessage("[GAME] Listener de capture globale retiré", LogLevel.INFO));
        }
    }

    public void handleInput(KeyEvent event) {
        if (!stateSynchronizer.isMapReady()) {
            return;
        }

        if (chatOverlay != null) {
            if (event.getCode() == KeyCode.ENTER) {
                if (currentInputState == InputState.GAME_PLAYING) {
                    setInputState(InputState.CHAT_TYPING);
                    chatOverlay.open();
                    event.consume();
                    return;
                }
                return;
            }
            if (event.getCode() == KeyCode.ESCAPE) {
                if (currentInputState == InputState.CHAT_TYPING) {
                    chatOverlay.close();
                    event.consume();
                    return;
                }
                return;
            }
            if (currentInputState == InputState.CHAT_TYPING) {
                return;
            }
        }

        KeyBindingsManager keyBindings = App.getKeyBindingsManager();
        if (event.getCode() == keyBindings.getKeyBinding(KeyBindingsManager.Action.MOVE_UP)) {
            applyAction(fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.MOVE_UP);
        } else if (event.getCode() == keyBindings.getKeyBinding(KeyBindingsManager.Action.MOVE_DOWN)) {
            applyAction(fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.MOVE_DOWN);
        } else if (event.getCode() == keyBindings.getKeyBinding(KeyBindingsManager.Action.MOVE_LEFT)) {
            applyAction(fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.MOVE_LEFT);
        } else if (event.getCode() == keyBindings.getKeyBinding(KeyBindingsManager.Action.MOVE_RIGHT)) {
            applyAction(fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.MOVE_RIGHT);
        } else if (event.getCode() == keyBindings.getKeyBinding(KeyBindingsManager.Action.DROP_BOMB)) {
            applyAction(fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.DROP_BOMB);
        }
    }

    private void handleGlobalKey(KeyCode keyCode) {
        if (!stateSynchronizer.isMapReady()) {
            return;
        }

        if (chatOverlay != null) {
            if (keyCode == KeyCode.ENTER) {
                if (currentInputState == InputState.GAME_PLAYING) {
                    Platform.runLater(() -> {
                        setInputState(InputState.CHAT_TYPING);
                        chatOverlay.open();
                    });
                    return;
                }
                return;
            }
            if (keyCode == KeyCode.ESCAPE) {
                if (currentInputState == InputState.CHAT_TYPING) {
                    Platform.runLater(chatOverlay::close);
                    return;
                }
                return;
            }
            if (currentInputState == InputState.CHAT_TYPING) {
                return;
            }
        }

        KeyBindingsManager keyBindings = App.getKeyBindingsManager();
        if (keyCode == keyBindings.getKeyBinding(KeyBindingsManager.Action.MOVE_UP)) {
            applyAction(fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.MOVE_UP);
        } else if (keyCode == keyBindings.getKeyBinding(KeyBindingsManager.Action.MOVE_DOWN)) {
            applyAction(fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.MOVE_DOWN);
        } else if (keyCode == keyBindings.getKeyBinding(KeyBindingsManager.Action.MOVE_LEFT)) {
            applyAction(fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.MOVE_LEFT);
        } else if (keyCode == keyBindings.getKeyBinding(KeyBindingsManager.Action.MOVE_RIGHT)) {
            applyAction(fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.MOVE_RIGHT);
        } else if (keyCode == keyBindings.getKeyBinding(KeyBindingsManager.Action.DROP_BOMB)) {
            applyAction(fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.DROP_BOMB);
        }
    }

    private void applyAction(fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action action) {
        if (action == null || action == fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.NONE || isGameEnded()) {
            return;
        }
        if (!stateSynchronizer.isMapReady()) {
            return;
        }
        String me = App.clientId();
        if (me == null || me.isBlank()) {
            return;
        }
        Map<String, RemotePlayer> players = stateSynchronizer.players();
        RemotePlayer self = players.get(me);
        if (self == null) {
            return;
        }

        if (action == fr.iutgon.sae401.clientSide.utils.GamepadInputManager.Action.DROP_BOMB) {
            dropBomb();
            return;
        }

        int nX = self.x;
        int nY = self.y;
        switch (action) {
            case MOVE_UP -> {
                nY--;
                self.dir = "N";
            }
            case MOVE_DOWN -> {
                nY++;
                self.dir = "S";
            }
            case MOVE_LEFT -> {
                nX--;
                self.dir = "W";
            }
            case MOVE_RIGHT -> {
                nX++;
                self.dir = "E";
            }
            default -> {
                return;
            }
        }

        GameMap map = stateSynchronizer.gameMap();
        if (map == null) {
            return;
        }
        Position next = new Position(nX, nY);
        if (!map.isInside(next) || !map.isWalkable(next)) {
            return;
        }

        Json payload = Json.object(Map.of(
                "x", Json.of(nX),
                "y", Json.of(nY)
        ));
        App.sendToServer(new MessageEnvelope("input", UUID.randomUUID().toString(), payload));
    }

    private void dropBomb() {
        if (!stateSynchronizer.isMapReady()) {
            return;
        }
        String me = App.clientId();
        if (me == null || me.isBlank()) {
            return;
        }
        RemotePlayer self = stateSynchronizer.players().get(me);
        if (self == null) {
            return;
        }
        Json payload = Json.object(Map.of(
                "x", Json.of(self.x),
                "y", Json.of(self.y),
                "bomb", Json.of(true)
        ));
        App.LOGGER.log(new LogMessage("[GAME] send input bomb -> (" + self.x + "," + self.y + ")", LogLevel.INFO));
        App.sendToServer(new MessageEnvelope("input", UUID.randomUUID().toString(), payload));
    }

    private void setupChat() {
        chatOverlay = new ChatOverlay();
        chatOverlay.setLayoutX(10);
        chatOverlay.layoutYProperty().bind(
                gamePane.heightProperty()
                        .subtract(chatOverlay.heightProperty())
                        .subtract(10)
        );
        chatOverlay.maxHeightProperty().bind(gamePane.heightProperty().multiply(0.7));
        chatOverlay.setOnMessageSend(message -> {
            Json payload = Json.object(Map.of("message", Json.of(message)));
            App.sendToServer(new MessageEnvelope("chat", UUID.randomUUID().toString(), payload));
            App.LOGGER.log(new LogMessage("[GAME] Chat message sent: " + message, LogLevel.INFO));
        });
        chatOverlay.setOnClose(() -> {
            setInputState(InputState.GAME_PLAYING);
            Platform.runLater(gamePane::requestFocus);
        });
        hudLayer.getChildren().add(chatOverlay);
        App.LOGGER.log(new LogMessage("[GAME] Chat overlay initialized", LogLevel.INFO));
    }

    private void quitCurrentGame() {
        try {
            cleanup(true);
            if (LocalServerManager.isLocalModeEnabled()) {
                LocalServerManager.exitLocalMode();
            }
            App.setRoot("MainMenu");
        } catch (Exception ex) {
            App.LOGGER.log(new LogMessage("[GAME] Failed to return to menu: " + ex.getMessage(), LogLevel.ERROR));
        }
    }

    private void requestRestart() {
        if (hudOverlay != null) {
            hudOverlay.setReplayButtonDisabled(true);
        }
        try {
            App.sendToServer(new MessageEnvelope("restart_game", UUID.randomUUID().toString(), Json.nullValue()));
        } catch (Exception ex) {
            App.LOGGER.log(new LogMessage("[GAME] Failed to request restart: " + ex.getMessage(), LogLevel.ERROR));
            if (hudOverlay != null) {
                hudOverlay.setReplayButtonDisabled(false);
            }
        }
    }

    @Override
    public void onMessage(MessageEnvelope message) {
        if (message == null || message.getType() == null) {
            return;
        }
        switch (message.getType()) {
            case "chat" -> {
                Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
                String from = payload.value("from", "?");
                String txt = payload.value("message", "");
                if (!txt.isBlank() && chatOverlay != null) {
                    Platform.runLater(() -> chatOverlay.appendMessage(from, txt));
                }
            }
            case "game_init" -> {
                Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
                if (!payload.contains("state") || payload.at("state").isNull() || !payload.at("state").isObject()) {
                    return;
                }
                String roomId = payload.value("roomId", "");
                stateSynchronizer.clearLatestUdpSeqForRoom(roomId);
                Json state = payload.at("state");
                Platform.runLater(() -> stateSynchronizer.applyState(state));
            }
            case "game_delta" -> {
                Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
                if (!payload.contains("delta") || payload.at("delta").isNull() || !payload.at("delta").isObject()) {
                    return;
                }
                Json delta = payload.at("delta");
                Platform.runLater(() -> stateSynchronizer.applyDelta(delta));
            }
            case "game_positions" -> {
                Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
                if (!payload.contains("players") || !payload.at("players").isArray()) {
                    return;
                }
                String roomId = payload.value("roomId", "");
                long seq = payload.value("seq", -1L);
                if (seq < 0L) {
                    return;
                }
                Json players = payload.at("players");
                Platform.runLater(() -> stateSynchronizer.applyUdpPlayerSnapshot(roomId, seq, players));
            }
            case "game_state" -> {
                Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
                if (!payload.contains("state") || payload.at("state").isNull() || !payload.at("state").isObject()) {
                    return;
                }
                Json state = payload.at("state");
                if (state.contains("players") && state.at("players").isArray()) {
                    LOGGER.log(new LogMessage("[GAME] game_state received: players=" + state.at("players").asArray().size(), LogLevel.INFO));
                } else {
                    LOGGER.log(new LogMessage("[GAME] game_state received", LogLevel.INFO));
                }
                Platform.runLater(() -> stateSynchronizer.applyState(state));
            }
            case "room_restarted" -> Platform.runLater(() -> {
                try {
                    stateSynchronizer.clearLatestUdpSeqAll();
                    cleanup(false);
                    if (LocalServerManager.isLocalModeEnabled()) {
                        LocalReplayContext.requestRestoreOnNextCreateLobby();
                        App.setRoot("CreateLobby");
                    } else {
                        App.setRoot("ChatLobby");
                    }
                } catch (Exception ex) {
                    LOGGER.log(new LogMessage("[GAME] Failed to process room restart transition: " + ex.getMessage(), LogLevel.ERROR));
                }
            });
            case "restart_failed" -> {
                Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
                String reason = payload.value("reason", "unknown");
                LOGGER.log(new LogMessage("[GAME] restart_failed: " + reason, LogLevel.WARNING));
                Platform.runLater(() -> {
                    if (hudOverlay != null) {
                        hudOverlay.setReplayButtonDisabled(false);
                    }
                });
            }
            default -> {
            }
        }
    }

    private void setInputState(InputState newState) {
        this.currentInputState = newState;
    }
}
