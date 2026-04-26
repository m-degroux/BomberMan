package fr.iutgon.sae401.clientSide.controllers;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.clientSide.network.NetworkObserver;
import fr.iutgon.sae401.clientSide.utils.SceneManager;
import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.logger.Logger;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

import java.util.UUID;

public class ChatLobbyController implements NetworkObserver{

    
    @FXML
    private Label arenaNameLabel;
    @FXML
    private Label countdownLabel;

    @FXML
    private ListView<String> playerListView;
    @FXML
    private TextArea chatHistoryArea;
    @FXML
    private Button leaveButton;
    @FXML
    private TextField messageInputField;
    @FXML
    private Button sendButton;
    @FXML
    private Button readyButton;
    @FXML
    private Button chooseSkinButton;
	@FXML
	private TextField pseudoField;
    private boolean isReady = false;
    private int currentSkinId = 0;
    private static final int[] AVAILABLE_SKINS = {0, 1, 2, 3, 4, 5, 7, 8, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 31, 33, 34};
    private static final int START_COUNTDOWN_SECONDS = 3;
    private static final int MIN_PLAYERS_TO_START = 2;
    private Timeline startCountdownTimeline;
    private int countdownRemaining = 0;
    private boolean countdownActive = false;

    @FXML
    public void initialize() {
		networkManager.addObserver(this);
        playerListView.getItems().clear();
        leaveButton.setOnAction(this::handleLeaveLobby);

        sendButton.setOnAction(event -> sendMessage());
        chooseSkinButton.setOnAction(event -> openSkinSelector());

        // Permet d'envoyer le message en appuyant sur la touche "Entrée" du clavier
        messageInputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });
        readyButton.setOnAction(event -> toggleReady());
		
		// Handle pseudo changes
		if (pseudoField != null) {
			pseudoField.setOnKeyPressed(event -> {
				if (event.getCode() == KeyCode.ENTER) {
					changePseudo();
				}
			});
		}
		
        setCountdownVisible(false);
        // Snapshot initial pour remplir la liste des joueurs.
        App.sendToServer(new MessageEnvelope("lobby_state", UUID.randomUUID().toString(), Json.emptyObject()));
    }

    /**
     * Gère le retour au menu principal.
     */
    private void handleLeaveLobby(ActionEvent event) {
        App.LOGGER.log(new LogMessage("Le joueur quitte le lobby.", LogLevel.INFO));
        stopCountdownDisplay();
		networkManager.removeObserver(this);

		// Important: prévenir le serveur pour libérer la room et éviter des rooms "fantômes" dans list_rooms.
		App.sendToServer(new MessageEnvelope("leave_room", UUID.randomUUID().toString(), Json.emptyObject()));

        SceneManager.switchScene(event, "/fr/iutgon/sae401/fxml/MenuLobby.fxml");
    }

    /**
     * Récupère le texte, l'affiche dans le chat et vide le champ de saisie.
     */
    private void sendMessage() {
        String message = messageInputField.getText();
        if (message != null && !message.trim().isEmpty()) {
            App.LOGGER.log(new LogMessage("[CLIENT] Envoi message chat : " + message.trim(), LogLevel.INFO));
			Json payload = Json.object(java.util.Map.of("message", Json.of(message.trim())));
			App.sendToServer(new MessageEnvelope("chat", UUID.randomUUID().toString(), payload));
            messageInputField.clear();
        }
    }

    /**
     * Changes the player's pseudo/nickname.
     */
    private void changePseudo() {
        if (pseudoField == null) {
            return;
        }
        String pseudo = pseudoField.getText();
        if (pseudo != null && !pseudo.trim().isEmpty()) {
            App.LOGGER.log(new LogMessage("[CLIENT] Changement de pseudo : " + pseudo.trim(), LogLevel.INFO));
            Json payload = Json.object(java.util.Map.of("nickname", Json.of(pseudo.trim())));
            App.sendToServer(new MessageEnvelope("set_nickname", UUID.randomUUID().toString(), payload));
            pseudoField.clear();
        }
    }

    private void openSkinSelector() {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Sélectionner un skin");
        dialog.setHeaderText("Parcourez et sélectionnez votre skin (Flèches: naviguer, R_0: confirmer)");
        dialog.setResizable(true);
        
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/fr/iutgon/sae401/css/styleMainMenu.css").toExternalForm()
        );

        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-border-color: #333; -fx-border-width: 1;");

        HBox previewBox = new HBox();
        previewBox.setPrefHeight(150);
        previewBox.setStyle("-fx-border-color: #666; -fx-border-width: 1; -fx-alignment: center;");
        ImageView previewImage = new ImageView();
        previewImage.setFitHeight(120);
        previewImage.setFitWidth(80);
        previewImage.setPreserveRatio(true);
        previewBox.getChildren().add(previewImage);
        previewBox.setAlignment(Pos.CENTER);

        Label skinLabel = new Label("Skin ID: " + currentSkinId);
        skinLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        HBox navigationBox = new HBox(10);
        navigationBox.setAlignment(Pos.CENTER);
        Button prevButton = new Button("← Précédent");
        Button randomButton = new Button("Aléatoire");
        Button nextButton = new Button("Suivant →");
        
        prevButton.setStyle("-fx-padding: 10px 20px; -fx-font-size: 12px;");
        randomButton.setStyle("-fx-padding: 10px 20px; -fx-font-size: 12px;");
        nextButton.setStyle("-fx-padding: 10px 20px; -fx-font-size: 12px;");

        Runnable updatePreview = () -> {
            skinLabel.setText("Skin ID: " + currentSkinId);
            previewImage.setImage(fr.iutgon.sae401.clientSide.service.SpriteManager.getPlayerFrame(currentSkinId, "S", 0));
        };

        prevButton.setOnAction(e -> {
            int currentIndex = java.util.Arrays.binarySearch(AVAILABLE_SKINS, currentSkinId);
            if (currentIndex > 0) {
                currentSkinId = AVAILABLE_SKINS[currentIndex - 1];
                updatePreview.run();
            }
        });

        nextButton.setOnAction(e -> {
            int currentIndex = java.util.Arrays.binarySearch(AVAILABLE_SKINS, currentSkinId);
            if (currentIndex < AVAILABLE_SKINS.length - 1) {
                currentSkinId = AVAILABLE_SKINS[currentIndex + 1];
                updatePreview.run();
            }
        });

        randomButton.setOnAction(e -> {
            java.util.Random random = new java.util.Random();
            currentSkinId = AVAILABLE_SKINS[random.nextInt(AVAILABLE_SKINS.length)];
            updatePreview.run();
        });

        navigationBox.getChildren().addAll(prevButton, randomButton, nextButton);
        content.getChildren().addAll(
                new Label("Navigation du skin:"),
                previewBox,
                skinLabel,
                navigationBox
        );

        dialog.getDialogPane().setContent(content);
        
        javafx.scene.control.ButtonType confirmButton = new javafx.scene.control.ButtonType("Confirmer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType cancelButton = javafx.scene.control.ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().setAll(confirmButton, cancelButton);

        updatePreview.run();

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButton) {
                return currentSkinId;
            }
            return null;
        });

        java.util.Optional<Integer> result = dialog.showAndWait();
        if (result.isPresent()) {
            sendSkinSelection(result.get());
        }
    }

    private void sendSkinSelection(int skinId) {
        App.LOGGER.log(new LogMessage("[CLIENT] Sélection du skin : " + skinId, LogLevel.INFO));
        Json payload = Json.object(java.util.Map.of("skinId", Json.of(skinId)));
        App.sendToServer(new MessageEnvelope("select_skin", UUID.randomUUID().toString(), payload));
    }

    /**
     * Change l'état du joueur (Prêt / Pas prêt) de manière visuelle.
     */
    private void toggleReady() {
        isReady = !isReady;
        String status = isReady ? "Prêt" : "En attente";
        App.LOGGER.log(new LogMessage("[STATUS] Changement de statut : " + status, LogLevel.INFO));

		Json payload = Json.object(java.util.Map.of("ready", Json.of(isReady)));
		App.sendToServer(new MessageEnvelope("ready", UUID.randomUUID().toString(), payload));

        if (isReady) {
            readyButton.setText("Annuler (Prêt)");
            readyButton.setStyle("-fx-base: #90ee90;");
        } else {
            readyButton.setText("Prêt");
            readyButton.setStyle("");
        }
    }

    private void updateStartCountdown(boolean allReady, int memberCount) {
        boolean shouldShowCountdown = allReady && memberCount >= MIN_PLAYERS_TO_START;
        if (!shouldShowCountdown) {
            stopCountdownDisplay();
            return;
        }

        if (countdownActive) {
            return;
        }

        countdownActive = true;
        countdownRemaining = START_COUNTDOWN_SECONDS;
        setCountdownVisible(true);
        countdownLabel.setText("Démarrage dans " + countdownRemaining + "...");

        startCountdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), _event -> {
            if (countdownRemaining > 1) {
                countdownRemaining--;
                countdownLabel.setText("Démarrage dans " + countdownRemaining + "...");
                return;
            }

            countdownRemaining = 0;
            if (startCountdownTimeline != null) {
                startCountdownTimeline.stop();
                startCountdownTimeline = null;
            }
            countdownLabel.setText("Démarrage...");
        }));
        startCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
        startCountdownTimeline.playFromStart();
    }

    private void stopCountdownDisplay() {
        countdownActive = false;
        countdownRemaining = 0;
        if (startCountdownTimeline != null) {
            startCountdownTimeline.stop();
            startCountdownTimeline = null;
        }
        setCountdownVisible(false);
    }

    private void setCountdownVisible(boolean visible) {
        if (countdownLabel == null) {
            return;
        }
        countdownLabel.setVisible(visible);
        countdownLabel.setManaged(visible);
        if (!visible) {
            countdownLabel.setText("");
        }
    }

    @Override
    public void onMessage(MessageEnvelope message) {
        if (message == null || message.getType() == null) {
            return;
        }
        if ("chat".equals(message.getType())) {
            Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
            String from = payload.value("from", "?");
            String txt = payload.value("message", "");
            if (!txt.isBlank()) {
                chatHistoryArea.appendText(from + ": " + txt + "\n");
            }
            return;
        }
        if ("chat_error".equals(message.getType())) {
            Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
            String err = payload.value("error", "unknown");
            chatHistoryArea.appendText("Chat error: " + err + "\n");
            return;
        }
        if ("set_nickname".equals(message.getType())) {
            Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
            if (payload.contains("error")) {
                String err = payload.value("error", "unknown");
                chatHistoryArea.appendText("Nickname error: " + err + "\n");
            } else {
                String nickname = payload.value("nickname", "?");
                chatHistoryArea.appendText("Pseudo changé en: " + nickname + "\n");
            }
            return;
        }
        if ("lobby_state".equals(message.getType())) {
            Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
            String roomId = payload.contains("roomId") && !payload.at("roomId").isNull()
                    ? payload.at("roomId").getString()
                    : "";
            if (arenaNameLabel != null) {
                arenaNameLabel.setText(roomId.isBlank() ? "Lobby" : roomId);
            }
            playerListView.getItems().clear();
            boolean allReady = true;
            int memberCount = 0;
            if (payload.contains("members") && payload.at("members").isArray()) {
                for (Json m : payload.at("members").asArray()) {
                    String clientId = m.value("clientId", "?");
                    String nickname = m.value("nickname", clientId);
                    boolean ready = m.value("ready", false);
                    boolean inGame = m.value("inGame", false);
                    memberCount++;
                    allReady &= ready;
                    String suffix = inGame ? " (en partie)" : (ready ? " (Prêt)" : "");
                    playerListView.getItems().add(nickname + suffix);
                }
            }
            updateStartCountdown(allReady, memberCount);
            return;
        }
        if ("start_failed".equals(message.getType())) {
            Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
            String reason = payload.value("reason", "unknown");
            chatHistoryArea.appendText("Impossible de lancer: " + reason + "\n");
            stopCountdownDisplay();
            return;
        }
        if ("game_started".equals(message.getType())) {
            Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
            String oldRoom = payload.value("oldRoom", "");
            String newRoom = payload.value("newRoom", "");
            App.LOGGER.log(new LogMessage("[GAME] Partie démarrée (" + oldRoom + " -> " + newRoom + ")", LogLevel.INFO));
            chatHistoryArea.appendText("[GAME] Partie démarrée : " + newRoom + "\n");
            stopCountdownDisplay();

			// We are leaving the lobby screen now.
            networkManager.removeObserver(this);
			try {
				App.setRoot("Game");
			} catch (IOException e) {
				App.LOGGER.log(new LogMessage("[GAME] Impossible d'ouvrir l'écran de jeu: " + e.getMessage(), LogLevel.ERROR));
			}
        }
    }
}
