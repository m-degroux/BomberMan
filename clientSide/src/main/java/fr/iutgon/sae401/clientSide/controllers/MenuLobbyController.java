package fr.iutgon.sae401.clientSide.controllers;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.clientSide.network.NetworkObserver;
import fr.iutgon.sae401.clientSide.service.LocalServerManager;
import fr.iutgon.sae401.clientSide.utils.SceneManager;
import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.net.URL;
import java.io.IOException;

public class MenuLobbyController implements NetworkObserver {
	private static final int MAX_PLAYERS = 4;
    private static final int ROOM_LIST_REFRESH_SECONDS = 5;

    @FXML
    private TableView<RoomRow> arenaTable;
    @FXML
    private TableColumn<RoomRow, String> nameColumn;
    @FXML
    private TableColumn<RoomRow, String> statusColumn;
    @FXML
    private TableColumn<RoomRow, String> playersColumn;
    @FXML
    private Button createButton;
    @FXML
    private Button joinButton;

    private volatile String pendingJoinRequestId;
    private final BooleanProperty joinInFlight = new SimpleBooleanProperty(false);
	private volatile boolean cancelAfterJoin = false;
    private volatile RoomRow selectedRoomBeforeJoin = null;
    private Timeline delayedRoomsApplyTimeline;
    private List<Json> pendingRoomsSnapshot = List.of();
    private long lastRoomsApplyEpochMs = 0L;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        playersColumn.setCellValueFactory(new PropertyValueFactory<>("players"));

        networkManager.addObserver(this);

        // Le bouton Rejoindre est désactivé TANT QUE rien n'est sélectionné dans le tableau
        joinButton.disableProperty().bind(
            arenaTable.getSelectionModel().selectedItemProperty().isNull().or(joinInFlight)
        );

        refreshRooms();
    }

    // Action du bouton "Créer un serveur"
    @FXML
    private void handleCreateServer(ActionEvent event) {
        // On redirige vers la page de création (Assurez-vous que le nom du FXML est correct)
        SceneManager.switchScene(event, "/fr/iutgon/sae401/fxml/CreateLobby.fxml");
    }

    // Action du bouton "Rejoindre"
    @FXML
    private void handleJoinServer(ActionEvent event) {
        RoomRow selected = arenaTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        // If room has password, show password dialog
        if (selected.hasPassword()) {
            Dialog<String> passwordDialog = new Dialog<>();
            passwordDialog.setTitle("Mot de passe requis");
            passwordDialog.setHeaderText("Cette salle est protégée par un mot de passe");
            passwordDialog.setContentText("Entrez le mot de passe :");

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Mot de passe");
            passwordDialog.getDialogPane().setContent(passwordField);
            
            passwordDialog.getDialogPane().getStylesheets().add(
                    getClass().getResource("/fr/iutgon/sae401/css/styleMainMenu.css").toExternalForm()
                );

            javafx.scene.control.ButtonType okButton = javafx.scene.control.ButtonType.OK;
            javafx.scene.control.ButtonType cancelButton = javafx.scene.control.ButtonType.CANCEL;
            passwordDialog.getDialogPane().getButtonTypes().setAll(okButton, cancelButton);

            passwordDialog.setResultConverter(dialogButton -> {
                if (dialogButton == okButton) {
                    return passwordField.getText();
                }
                return null;
            });

            java.util.Optional<String> result = passwordDialog.showAndWait();
            if (result.isPresent()) {
                selectedRoomBeforeJoin = selected;
                sendJoinRequest(selected.getName(), result.get());
            }
        } else {
            // No password, join directly
            sendJoinRequest(selected.getName(), "");
        }
    }

    private void sendJoinRequest(String roomName, String password) {
        java.util.Map<String, Json> payloadMap = new java.util.LinkedHashMap<>();
        payloadMap.put("roomId", Json.of(roomName));
        if (!password.isEmpty()) {
            payloadMap.put("password", Json.of(password));
        }
        Json payload = Json.object(payloadMap);

        String requestId = java.util.UUID.randomUUID().toString();
        pendingJoinRequestId = requestId;
        joinInFlight.set(true);
        App.sendToServer(new MessageEnvelope("join_room", requestId, payload));
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        if (joinInFlight.get()) {
            // Avoid leaving the screen while the server may still accept the join.
            // We'll navigate once we receive room_joined/room_join_failed.
            cancelAfterJoin = true;
            createButton.setDisable(true);
            joinButton.setDisable(true);
            return;
        }

        stopPendingRoomsApply();
        networkManager.removeObserver(this);
        switchToMainMenu();
    }

    private void switchToMainMenu() {
        // If we are in local mode, exiting the lobby should stop the embedded server we own.
        LocalServerManager.exitLocalMode();
        try {
            if (arenaTable == null || arenaTable.getScene() == null) {
                return;
            }
            URL fxmlUrl = SceneManager.class.getResource("/fr/iutgon/sae401/fxml/MainMenu.fxml");
            if (fxmlUrl == null) {
                App.LOGGER.log(new LogMessage("Fichier FXML introuvable : /fr/iutgon/sae401/fxml/MainMenu.fxml", LogLevel.ERROR));
                return;
            }
            Parent root = FXMLLoader.load(fxmlUrl);
            arenaTable.getScene().setRoot(root);
        } catch (IOException e) {
            App.LOGGER.log(new LogMessage("Erreur lors du changement de scène (/fr/iutgon/sae401/fxml/MainMenu.fxml) : " + e.getMessage(), LogLevel.ERROR));
        }
    }

    private void refreshRooms() {
        App.sendToServer(new MessageEnvelope("list_rooms", UUID.randomUUID().toString(), Json.emptyObject()));
    }

    private void queueRoomsUpdate(List<Json> rooms) {
        pendingRoomsSnapshot = List.copyOf(rooms);

        long now = System.currentTimeMillis();
        long periodMs = ROOM_LIST_REFRESH_SECONDS * 1000L;
        long elapsedMs = now - lastRoomsApplyEpochMs;

        if (lastRoomsApplyEpochMs == 0L || elapsedMs >= periodMs || arenaTable.getItems().isEmpty()) {
            applyPendingRoomsNow();
            return;
        }

        if (delayedRoomsApplyTimeline != null) {
            return;
        }

        long remainingMs = periodMs - elapsedMs;
        delayedRoomsApplyTimeline = new Timeline(new KeyFrame(Duration.millis(remainingMs), _event -> applyPendingRoomsNow()));
        delayedRoomsApplyTimeline.setCycleCount(1);
        delayedRoomsApplyTimeline.playFromStart();
    }

    private void applyPendingRoomsNow() {
        if (delayedRoomsApplyTimeline != null) {
            delayedRoomsApplyTimeline.stop();
            delayedRoomsApplyTimeline = null;
        }
        applyRooms(pendingRoomsSnapshot);
        lastRoomsApplyEpochMs = System.currentTimeMillis();
    }

    private void stopPendingRoomsApply() {
        if (delayedRoomsApplyTimeline != null) {
            delayedRoomsApplyTimeline.stop();
            delayedRoomsApplyTimeline = null;
        }
    }

    private void applyRooms(List<Json> rooms) {
        RoomRow selected = arenaTable.getSelectionModel().getSelectedItem();
        String selectedRoomId = selected == null ? null : selected.getName();
        RoomRow toReselect = null;

        arenaTable.getItems().clear();
        for (Json room : rooms) {
            String id = room.value("id", "");
            int members = room.value("members", 0);
            boolean hasPassword = room.value("hasPassword", false);
            String status;
            if (members >= MAX_PLAYERS) {
                status = "Pleine";
            } else if (hasPassword) {
                status = "Privée";
            } else {
                status = "Libre";
            }
			String count = members + "/" + MAX_PLAYERS;
            RoomRow row = new RoomRow(id, status, count, hasPassword);
            arenaTable.getItems().add(row);
            if (selectedRoomId != null && selectedRoomId.equals(id)) {
                toReselect = row;
            }
        }

        if (toReselect != null) {
            arenaTable.getSelectionModel().select(toReselect);
        }
    }
    
    @Override
    public void onMessage(MessageEnvelope message) {
        if (Objects.equals("rooms", message.getType())) {
            Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
            List<Json> rooms = payload.contains("rooms") && payload.at("rooms").isArray()
                    ? payload.at("rooms").asArray()
                    : List.of();

            Platform.runLater(() -> queueRoomsUpdate(rooms));
            return;
        }

        // Navigation vers le lobby uniquement après confirmation serveur
        if (Objects.equals("room_joined", message.getType()) && pendingJoinRequestId != null
                && Objects.equals(pendingJoinRequestId, message.getRequestId())) {
            pendingJoinRequestId = null;
            Platform.runLater(() -> {
                joinInFlight.set(false);
                if (cancelAfterJoin) {
                    cancelAfterJoin = false;
                    try {
                        App.sendToServer(new MessageEnvelope("leave_room", UUID.randomUUID().toString(), Json.emptyObject()));
                    } catch (Exception ignored) {
                    }
                    stopPendingRoomsApply();
                    networkManager.removeObserver(this);
                    switchToMainMenu();
                    return;
                }
                stopPendingRoomsApply();
                networkManager.removeObserver(this);
                try {
                    App.setRoot("ChatLobby");
                } catch (Exception e) {
                    App.LOGGER.log(new LogMessage("Erreur lors du changement de scène (ChatLobby): " + e.getMessage(), LogLevel.ERROR));
                }
            });
            return;
        }

        if (Objects.equals("room_join_failed", message.getType()) && pendingJoinRequestId != null
                && Objects.equals(pendingJoinRequestId, message.getRequestId())) {
            Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
            String reason = payload.value("reason", "unknown");
            String roomId = payload.value("roomId", "");
            pendingJoinRequestId = null;
            App.LOGGER.log(new LogMessage("Join room échoué (roomId=" + roomId + ", reason=" + reason + ")", LogLevel.WARNING));
            Platform.runLater(() -> {
                joinInFlight.set(false);
                if (cancelAfterJoin) {
                    cancelAfterJoin = false;
                    stopPendingRoomsApply();
                    networkManager.removeObserver(this);
                    switchToMainMenu();
                } else if ("invalid_password".equals(reason)) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Mot de passe incorrect");
                    alert.setHeaderText("Accès refusé");
                    alert.setContentText("Le mot de passe que vous avez entré est incorrect.");
                    
                    alert.getDialogPane().getStylesheets().add(
                            getClass().getResource("/fr/iutgon/sae401/css/styleMainMenu.css").toExternalForm()
                        );
                    
                    alert.showAndWait();
                }
            });
            refreshRooms();
        }
    }

    public static final class RoomRow {
        private final String name;
        private final String status;
        private final String players;
        private final boolean hasPassword;

        public RoomRow(String name, String status, String players, boolean hasPassword) {
            this.name = name;
            this.status = status;
            this.players = players;
            this.hasPassword = hasPassword;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }

        public String getPlayers() {
            return players;
        }

        public boolean hasPassword() {
            return hasPassword;
        }
    }
}
