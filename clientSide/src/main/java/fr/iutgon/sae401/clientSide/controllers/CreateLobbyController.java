package fr.iutgon.sae401.clientSide.controllers;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.clientSide.network.NetworkObserver;
import fr.iutgon.sae401.clientSide.service.LocalReplayContext;
import fr.iutgon.sae401.clientSide.service.LocalReplayContext.LocalGameSettings;
import fr.iutgon.sae401.clientSide.service.LocalServerManager;
import fr.iutgon.sae401.clientSide.utils.SceneManager;
import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CreateLobbyController implements NetworkObserver {
    

    private volatile String pendingCreateRequestId;
    private volatile boolean localQuickStart = false;

    @FXML private Button cancelButton;
    @FXML private Button createConfirmButton;
    @FXML private TextField serverNameField;
    @FXML private PasswordField passwordField;
    @FXML private Spinner<Integer> widthSpinner;
    @FXML private Spinner<Integer> heightSpinner;
    @FXML private Spinner<Integer> healthSpinner;
    @FXML private Spinner<Integer> maxBombsSpinner;
    @FXML private Spinner<Integer> bombRangeSpinner;
    @FXML private Spinner<Integer> bombCooldownSpinner;
    @FXML private Spinner<Integer> fillingSpinner;
	@FXML private ComboBox<MapTheme> mapComboBox;

    @FXML
    public void initialize() {
        networkManager.addObserver(this);
		if (mapComboBox != null) {
			mapComboBox.getItems().setAll(MapTheme.values());
			mapComboBox.setValue(MapTheme.CLASSIC);
		}
        enforceOddDimensionSpinner(widthSpinner);
        enforceOddDimensionSpinner(heightSpinner);
        if (LocalServerManager.isLocalModeEnabled() && createConfirmButton != null) {
            createConfirmButton.setText("Lancer la partie");
        }
        prefillFromLocalReplayIfRequested();
    }

    // Action du bouton "Annuler"
    @FXML
    private void handleCancel(ActionEvent event) {
        App.LOGGER.log(new LogMessage("Annulation de la création d'arène.", LogLevel.INFO));
        resetPendingFlow();
        networkManager.removeObserver(this);
        if (LocalServerManager.isLocalModeEnabled()) {
            LocalServerManager.exitLocalMode();
            SceneManager.switchScene(event, "/fr/iutgon/sae401/fxml/MainMenu.fxml");
            return;
        }
        SceneManager.switchScene(event, "/fr/iutgon/sae401/fxml/MenuLobby.fxml");
    }

    // Action du bouton "Créer le serveur"
    @FXML
    private void handleCreateConfirm(ActionEvent event) {
        if (!App.isConnected()) {
            App.LOGGER.log(new LogMessage("Impossible de créer une arène: client hors-ligne (socket fermée)", LogLevel.WARNING));
            showInfo("Hors-ligne", "Impossible de créer l'arène: vous n'êtes pas connecté au serveur.\nLance d'abord le serveur local puis reconnecte-toi.");
            return;
        }
        App.LOGGER.log(new LogMessage("[ROOM] Création de partie", LogLevel.INFO));
        String roomId = serverNameField != null && serverNameField.getText() != null ? serverNameField.getText().trim() : "";
        String password = passwordField != null && passwordField.getText() != null ? passwordField.getText().trim() : "";
        MapTheme selectedTheme = mapComboBox != null && mapComboBox.getValue() != null
                ? mapComboBox.getValue()
                : MapTheme.CLASSIC;
        int normalizedWidth = normalizeOddSpinnerValue(widthSpinner);
        int normalizedHeight = normalizeOddSpinnerValue(heightSpinner);

        Map<String, Json> gameCfg = new LinkedHashMap<>();
        gameCfg.put("width", Json.of(normalizedWidth));
        gameCfg.put("height", Json.of(normalizedHeight));
        gameCfg.put("initialHealth", Json.of(healthSpinner.getValue()));
        gameCfg.put("maxBombs", Json.of(maxBombsSpinner.getValue()));
        gameCfg.put("bombRange", Json.of(bombRangeSpinner.getValue()));
        if (bombCooldownSpinner != null && bombCooldownSpinner.getValue() != null) {
            gameCfg.put("bombCooldownMs", Json.of(bombCooldownSpinner.getValue()));
        }
        gameCfg.put("destructibleDensity", Json.of(fillingSpinner.getValue()));

        Map<String, Json> payloadMap = new LinkedHashMap<>();
        payloadMap.put("roomId", roomId.isBlank() ? Json.nullValue() : Json.of(roomId));
        payloadMap.put("gameConfig", Json.object(gameCfg));
		if (mapComboBox != null && mapComboBox.getValue() != null) {
			payloadMap.put("mapTheme", Json.of(selectedTheme.name()));
		}
        if (!password.isBlank()) {
            payloadMap.put("password", Json.of(password));
        }
        if (LocalServerManager.isLocalModeEnabled()) {
            LocalReplayContext.rememberLastLocalGameSettings(new LocalGameSettings(
                    roomId,
                    password,
                    normalizedWidth,
                    normalizedHeight,
                    healthSpinner.getValue(),
                    maxBombsSpinner.getValue(),
                    bombRangeSpinner.getValue(),
                    bombCooldownSpinner != null && bombCooldownSpinner.getValue() != null ? bombCooldownSpinner.getValue() : 2000,
                    fillingSpinner.getValue(),
                    selectedTheme
            ));
        }

        String requestId = UUID.randomUUID().toString();
        localQuickStart = LocalServerManager.isLocalModeEnabled();
        pendingCreateRequestId = requestId;
        createConfirmButton.setDisable(true);
        App.sendToServer(new MessageEnvelope("create_room", requestId, Json.object(payloadMap)));
    }

    private void resetPendingFlow() {
        pendingCreateRequestId = null;
        localQuickStart = false;
    }

    private void prefillFromLocalReplayIfRequested() {
        if (!LocalServerManager.isLocalModeEnabled()) {
            return;
        }
        LocalReplayContext.consumeRequestedSettings().ifPresent(settings -> {
            if (serverNameField != null) {
                serverNameField.setText(settings.roomName());
            }
            if (passwordField != null) {
                passwordField.setText(settings.password());
            }
            setOddSpinnerValue(widthSpinner, settings.width());
            setOddSpinnerValue(heightSpinner, settings.height());
            setSpinnerValue(healthSpinner, settings.initialHealth());
            setSpinnerValue(maxBombsSpinner, settings.maxBombs());
            setSpinnerValue(bombRangeSpinner, settings.bombRange());
            setSpinnerValue(bombCooldownSpinner, settings.bombCooldownMs());
            setSpinnerValue(fillingSpinner, settings.destructibleDensity());
            if (mapComboBox != null) {
                mapComboBox.setValue(settings.mapTheme());
            }
        });
    }

    private static void enforceOddDimensionSpinner(Spinner<Integer> spinner) {
        if (spinner == null || spinner.getValueFactory() == null) {
            return;
        }
        SpinnerValueFactory<Integer> factory = spinner.getValueFactory();
        if (factory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory intFactory) {
            intFactory.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    return;
                }
                int normalized = normalizeToNextOdd(newVal, intFactory.getMin(), intFactory.getMax());
                if (normalized != newVal) {
                    intFactory.setValue(normalized);
                }
            });
        }
        if (spinner.getEditor() != null) {
            spinner.getEditor().setOnAction(event -> normalizeOddSpinnerValue(spinner));
        }
        spinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                normalizeOddSpinnerValue(spinner);
            }
        });
        normalizeOddSpinnerValue(spinner);
    }

    private static int normalizeOddSpinnerValue(Spinner<Integer> spinner) {
        if (spinner == null || spinner.getValueFactory() == null) {
            return 1;
        }
        SpinnerValueFactory<Integer> factory = spinner.getValueFactory();
        if (!(factory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory intFactory)) {
            Integer raw = factory.getValue();
            int normalized = raw == null ? 1 : raw;
            if ((normalized & 1) == 0) {
                normalized++;
                factory.setValue(normalized);
            }
            return normalized;
        }

        int raw = intFactory.getValue() == null ? intFactory.getMin() : intFactory.getValue();
        if (spinner.isEditable() && spinner.getEditor() != null) {
            String text = spinner.getEditor().getText();
            if (text != null && !text.isBlank()) {
                try {
                    raw = Integer.parseInt(text.trim());
                } catch (NumberFormatException ignored) {
                    // Keep spinner value if the editor text is invalid.
                }
            }
        }

        int normalized = normalizeToNextOdd(raw, intFactory.getMin(), intFactory.getMax());
        intFactory.setValue(normalized);
        if (spinner.getEditor() != null) {
            spinner.getEditor().setText(Integer.toString(normalized));
        }
        return normalized;
    }

    private static int normalizeToNextOdd(int value, int min, int max) {
        int clamped = Math.max(min, Math.min(max, value));
        if ((clamped & 1) != 0) {
            return clamped;
        }
        if (clamped < max) {
            return clamped + 1;
        }
        if (clamped > min) {
            return clamped - 1;
        }
        return clamped;
    }

    private static void setOddSpinnerValue(Spinner<Integer> spinner, int value) {
        setSpinnerValue(spinner, value);
        normalizeOddSpinnerValue(spinner);
    }

    private static void setSpinnerValue(Spinner<Integer> spinner, int value) {
        if (spinner == null || spinner.getValueFactory() == null) {
            return;
        }
        SpinnerValueFactory<Integer> factory = spinner.getValueFactory();
        if (factory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory intFactory) {
            int clamped = Math.max(intFactory.getMin(), Math.min(intFactory.getMax(), value));
            intFactory.setValue(clamped);
            return;
        }
        factory.setValue(value);
    }

    private void startLocalQuickMatch() {
        Platform.runLater(() -> {
            try {
                App.setRoot("Game");
            } catch (Exception e) {
                App.LOGGER.log(new LogMessage("[GAME] Erreur lors du changement de scène (Game): " + e.getMessage(), LogLevel.ERROR));
                createConfirmButton.setDisable(false);
                resetPendingFlow();
                return;
            }

            // Important: send readiness/start only after GameController is initialized,
            // otherwise game_init can arrive while no game observer is listening.
            App.sendToServer(new MessageEnvelope(
                    "ready",
                    UUID.randomUUID().toString(),
                    Json.object(Map.of("ready", Json.of(true)))
            ));
            App.sendToServer(new MessageEnvelope(
                    "start_game",
                    UUID.randomUUID().toString(),
                    Json.object(Map.of("allowSolo", Json.of(true)))
            ));

            networkManager.removeObserver(this);
            resetPendingFlow();
        });
    }

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            
            alert.getDialogPane().getStylesheets().add(
                    getClass().getResource("/fr/iutgon/sae401/css/styleMainMenu.css").toExternalForm()
                );
            
            alert.showAndWait();
        });
    }

	@Override
	public void onMessage(MessageEnvelope message) {
		if (Objects.equals("room_created", message.getType())
                && pendingCreateRequestId != null
                && Objects.equals(pendingCreateRequestId, message.getRequestId())) {
            pendingCreateRequestId = null;

            if (!localQuickStart) {
                networkManager.removeObserver(this);
                Platform.runLater(() -> {
                    try {
                        App.setRoot("ChatLobby");
                    } catch (Exception e) {
                        App.LOGGER.log(new LogMessage("[GAME] Erreur lors du changement de scène (ChatLobby): " + e.getMessage(), LogLevel.ERROR));
                    }
                });
                return;
            }

            startLocalQuickMatch();
            return;
        }

        if (Objects.equals("room_create_failed", message.getType())
                && pendingCreateRequestId != null
                && Objects.equals(pendingCreateRequestId, message.getRequestId())) {
            Json payload = message.getPayload() == null ? Json.emptyObject() : message.getPayload();
            String reason = payload.value("reason", "unknown");
            String roomId = payload.value("roomId", (String) null);
            resetPendingFlow();
            App.LOGGER.log(new LogMessage("[ROOM] Create room échoué (roomId=" + roomId + ", reason=" + reason + ")", LogLevel.WARNING));
            Platform.runLater(() -> createConfirmButton.setDisable(false));
            return;
        }
	}
}
