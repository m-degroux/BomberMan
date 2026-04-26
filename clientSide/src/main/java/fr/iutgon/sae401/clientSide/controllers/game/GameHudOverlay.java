package fr.iutgon.sae401.clientSide.controller.game;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.clientSide.service.SpriteManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.Map;
import java.util.function.Supplier;

public final class GameHudOverlay {
    private static final double HUD_ICON_PX = 46.0;
    private static final double HUD_PLAYER_SPRITE_PX = 64.0;
    private static final double HUD_COUNT_FONT_PX = 32.0;
    private static final double GAME_OVER_MESSAGE_FONT_PX = 140.0;
    private static final String REPLAY_BUTTON_ENABLED_STYLE =
            "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 10;";
    private static final String REPLAY_BUTTON_DISABLED_STYLE =
            "-fx-background-color: #7f8c8d; -fx-text-fill: #dfe6e9; -fx-background-radius: 10;";

    private final Pane gamePane;
    private final Pane hudLayer;
    private final Supplier<Map<String, RemotePlayer>> playersSupplier;
    private final Runnable quitAction;
    private final Runnable restartAction;

    private final HBox healthContainer = new HBox(5);
    private final HBox bombContainer = new HBox(10);
    private final VBox playerSpriteContainer = new VBox(4);
    private final ImageView playerSpriteView = new ImageView();
    private Image heartImage;
    private Image bombIconImage;
    private int displayedPlayerSkinId = Integer.MIN_VALUE;
    private Button inGameQuitButton;
    private Button replayButton;
    private VBox gameOverOverlay;
    private boolean gameEnded = false;

    public GameHudOverlay(
            Pane gamePane,
            Pane hudLayer,
            Supplier<Map<String, RemotePlayer>> playersSupplier,
            Runnable quitAction,
            Runnable restartAction
    ) {
        this.gamePane = gamePane;
        this.hudLayer = hudLayer;
        this.playersSupplier = playersSupplier;
        this.quitAction = quitAction;
        this.restartAction = restartAction;
    }

    public void initialize() {
        try {
            heartImage = new Image(getClass().getResourceAsStream("/fr/iutgon/sae401/sprites/output/hud/coeur.png"));
            bombIconImage = new Image(getClass().getResourceAsStream("/fr/iutgon/sae401/sprites/output/hud/bomb.png"));
        } catch (Exception e) {
            App.LOGGER.log(new fr.iutgon.sae401.common.logger.LogMessage("[GAME] Failed to load HUD images: " + e.getMessage(), fr.iutgon.sae401.common.logger.LogLevel.ERROR));
            return;
        }

        healthContainer.setAlignment(Pos.CENTER_LEFT);
        healthContainer.setPadding(new Insets(10));
        healthContainer.setLayoutX(10);
        healthContainer.setLayoutY(10);
        healthContainer.setVisible(false);

        bombContainer.setAlignment(Pos.CENTER_LEFT);
        bombContainer.setPadding(new Insets(10));
        bombContainer.setLayoutX(10);
        bombContainer.setLayoutY(50);

        playerSpriteContainer.setAlignment(Pos.CENTER_LEFT);
        playerSpriteContainer.setPadding(new Insets(10));
        playerSpriteContainer.setLayoutX(10);
        playerSpriteContainer.setLayoutY(96);
        playerSpriteContainer.setVisible(false);

        playerSpriteView.setFitWidth(HUD_PLAYER_SPRITE_PX);
        playerSpriteView.setFitHeight(HUD_PLAYER_SPRITE_PX);
        playerSpriteView.setPreserveRatio(true);
        playerSpriteContainer.getChildren().add(playerSpriteView);

        setupInGameQuitButton();
        setupGameOverOverlay();
        hudLayer.getChildren().addAll(healthContainer, bombContainer, playerSpriteContainer, inGameQuitButton, gameOverOverlay);
        refreshPlayerSpriteDisplay();
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    public void showGameOver(boolean victory) {
        if (gameEnded) {
            // Still apply animations to players even if gameEnded is true
            Map<String, RemotePlayer> playersSnapshot = playersSupplier.get();
            String currentPlayerId = App.clientId();
            String winnerId = null;
            if (!victory) {
                int aliveOpponents = 0;
                String candidateWinner = null;
                for (var entry : playersSnapshot.entrySet()) {
                    RemotePlayer p = entry.getValue();
                    if (p == null || !p.alive) {
                        continue;
                    }
                    if (currentPlayerId != null && currentPlayerId.equals(entry.getKey())) {
                        continue;
                    }
                    aliveOpponents++;
                    candidateWinner = entry.getKey();
                    if (aliveOpponents > 1) {
                        break;
                    }
                }
                if (aliveOpponents == 1) {
                    winnerId = candidateWinner;
                }
            }

            for (var entry : playersSnapshot.entrySet()) {
                String playerId = entry.getKey();
                RemotePlayer player = entry.getValue();
                if (player == null) {
                    continue;
                }
                if (victory && player.alive && currentPlayerId != null && currentPlayerId.equals(playerId)) {
                    player.view.setWin();
                } else if (!victory && currentPlayerId != null && currentPlayerId.equals(playerId)) {
                    player.view.setLose();
                } else if (!victory && winnerId != null && playerId.equals(winnerId)) {
                    player.view.setWin();
                }
            }
            return;
        }
        gameEnded = true;

        Map<String, RemotePlayer> playersSnapshot = playersSupplier.get();
        String currentPlayerId = App.clientId();
        String winnerId = null;
        if (!victory) {
            int aliveOpponents = 0;
            String candidateWinner = null;
            for (var entry : playersSnapshot.entrySet()) {
                RemotePlayer p = entry.getValue();
                if (p == null || !p.alive) {
                    continue;
                }
                if (currentPlayerId != null && currentPlayerId.equals(entry.getKey())) {
                    continue;
                }
                aliveOpponents++;
                candidateWinner = entry.getKey();
                if (aliveOpponents > 1) {
                    break;
                }
            }
            if (aliveOpponents == 1) {
                winnerId = candidateWinner;
            }
        }

        for (var entry : playersSnapshot.entrySet()) {
            String playerId = entry.getKey();
            RemotePlayer player = entry.getValue();
            if (player == null) {
                continue;
            }
            if (victory && player.alive && currentPlayerId != null && currentPlayerId.equals(playerId)) {
                player.view.setWin();
            } else if (!victory && currentPlayerId != null && currentPlayerId.equals(playerId)) {
                player.view.setLose();
            } else if (!victory && winnerId != null && playerId.equals(winnerId)) {
                player.view.setWin();
            }
        }

        gameOverOverlay.getChildren().clear();

        Text messageText = new Text(victory ? "BIEN JOUE !" : "VOUS AVEZ PERDU");
        messageText.setFont(Font.font("Arial", FontWeight.BLACK, GAME_OVER_MESSAGE_FONT_PX));
        messageText.setFill(victory ? Color.GOLD : Color.RED);
        messageText.setStroke(Color.BLACK);
        messageText.setStrokeWidth(3.0);
        messageText.setTextAlignment(TextAlignment.CENTER);
        messageText.setStyle("-fx-effect: dropshadow(gaussian, black, 18, 0.55, 0, 0);");

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button quitButton = new Button("Quitter");
        quitButton.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        quitButton.setPrefSize(150, 50);
        quitButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 10;");
        quitButton.setOnAction(e -> quitAction.run());

        replayButton = new Button("Relancer");
        replayButton.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        replayButton.setPrefSize(150, 50);
        replayButton.disableProperty().addListener((obs, oldDisabled, newDisabled) -> applyReplayButtonStyle(newDisabled));
        setReplayButtonDisabled(false);
        replayButton.setOnAction(e -> {
            if (replayButton.isDisabled()) {
                return;
            }
            setReplayButtonDisabled(true);
            restartAction.run();
        });

        buttonBox.getChildren().addAll(quitButton, replayButton);
        gameOverOverlay.getChildren().addAll(messageText, buttonBox);
        gameOverOverlay.setVisible(true);
    }

    public void setReplayButtonDisabled(boolean disabled) {
        if (replayButton == null) {
            return;
        }
        replayButton.setDisable(disabled);
        applyReplayButtonStyle(disabled);
    }

    public void updateHealthDisplay(int health) {
        healthContainer.getChildren().clear();
        for (int i = 0; i < health; i++) {
            ImageView heartView = new ImageView(heartImage);
            heartView.setFitWidth(HUD_ICON_PX);
            heartView.setFitHeight(HUD_ICON_PX);
            heartView.setPreserveRatio(true);
            healthContainer.getChildren().add(heartView);
        }
        refreshPlayerSpriteDisplay();
    }

    public void updateBombDisplay(int bombCount) {
        bombContainer.getChildren().clear();
        ImageView bombIconView = new ImageView(bombIconImage);
        bombIconView.setFitWidth(HUD_ICON_PX);
        bombIconView.setFitHeight(HUD_ICON_PX);
        bombIconView.setPreserveRatio(true);

        Label bombLabel = new Label("x " + bombCount);
        bombLabel.setFont(Font.font("Arial", FontWeight.BOLD, HUD_COUNT_FONT_PX));
        bombLabel.setTextFill(Color.WHITE);
        bombLabel.setStyle("-fx-effect: dropshadow(gaussian, black, 2, 1.0, 0, 0);");

        bombContainer.getChildren().addAll(bombIconView, bombLabel);
        refreshPlayerSpriteDisplay();
    }

    private void refreshPlayerSpriteDisplay() {
        String currentPlayerId = App.clientId();
        Map<String, RemotePlayer> players = playersSupplier.get();
        if (currentPlayerId == null || currentPlayerId.isBlank() || players == null) {
            playerSpriteContainer.setVisible(false);
            return;
        }

        RemotePlayer me = players.get(currentPlayerId);
        if (me == null) {
            playerSpriteContainer.setVisible(false);
            return;
        }

        if (displayedPlayerSkinId != me.skinId || playerSpriteView.getImage() == null) {
            Image sprite = SpriteManager.getPlayerFrame(me.skinId, "S", 0);
            if (sprite == null) {
                playerSpriteContainer.setVisible(false);
                return;
            }
            displayedPlayerSkinId = me.skinId;
            playerSpriteView.setImage(sprite);
        }
        playerSpriteContainer.setVisible(true);
    }

    private void setupInGameQuitButton() {
        inGameQuitButton = new Button("Quitter");
        inGameQuitButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        inGameQuitButton.setPrefSize(120, 40);
        inGameQuitButton.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 8;");
        inGameQuitButton.setFocusTraversable(false);
        inGameQuitButton.layoutXProperty().bind(
                gamePane.widthProperty()
                        .subtract(inGameQuitButton.widthProperty())
                        .subtract(10)
        );
        inGameQuitButton.setLayoutY(10);
        inGameQuitButton.setOnAction(e -> quitAction.run());
    }

    private void setupGameOverOverlay() {
        gameOverOverlay = new VBox(20);
        gameOverOverlay.setAlignment(Pos.CENTER);
        gameOverOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.45);");
        gameOverOverlay.setPrefSize(gamePane.getPrefWidth(), gamePane.getPrefHeight());
        gameOverOverlay.setVisible(false);
        gameOverOverlay.prefWidthProperty().bind(gamePane.widthProperty());
        gameOverOverlay.prefHeightProperty().bind(gamePane.heightProperty());
    }

    private void applyReplayButtonStyle(boolean disabled) {
        if (replayButton == null) {
            return;
        }
        replayButton.setStyle(disabled ? REPLAY_BUTTON_DISABLED_STYLE : REPLAY_BUTTON_ENABLED_STYLE);
    }
}
