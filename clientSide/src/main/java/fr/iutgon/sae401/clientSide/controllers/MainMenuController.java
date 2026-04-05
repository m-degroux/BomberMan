package fr.iutgon.sae401.clientSide.controllers;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.clientSide.service.LocalServerManager;
import fr.iutgon.sae401.clientSide.utils.KeyBindingsManager;
import fr.iutgon.sae401.clientSide.utils.SceneManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainMenuController {
    @FXML private Label keysInfoLabel;
    @FXML private Label clientSlotLabel;
    @FXML private Button localButton;
    @FXML private Button multiplayerButton;
    @FXML private TextField ipTextField;
    @FXML private Button connectButton;

    @FXML
    private void initialize() {
        applyConnectionUiState();
        updateKeysDisplay();
    }

    private void applyConnectionUiState() {
        if (localButton != null) {
            localButton.setDisable(false);
        }
        String rememberedHost = App.connectedServerHost();
        if (rememberedHost == null || rememberedHost.isBlank()) {
            rememberedHost = App.lastServerHost();
        }
        if (ipTextField != null && rememberedHost != null && !rememberedHost.isBlank()) {
            ipTextField.setText(rememberedHost);
        }

        boolean connected = App.isConnected();
        if (multiplayerButton != null) {
            multiplayerButton.setDisable(!connected);
        }
        if (ipTextField != null) {
            ipTextField.setDisable(connected);
        }
        if (connectButton != null) {
            connectButton.setDisable(connected);
            connectButton.setText(connected ? "Connecté !" : "Connecter");
        }
    }

    private void updateKeysDisplay() {
        KeyBindingsManager manager = App.getKeyBindingsManager();
        String up = getKeyDisplayName(manager.getKeyBinding(KeyBindingsManager.Action.MOVE_UP));
        String down = getKeyDisplayName(manager.getKeyBinding(KeyBindingsManager.Action.MOVE_DOWN));
        String left = getKeyDisplayName(manager.getKeyBinding(KeyBindingsManager.Action.MOVE_LEFT));
        String right = getKeyDisplayName(manager.getKeyBinding(KeyBindingsManager.Action.MOVE_RIGHT));
        String bomb = getKeyDisplayName(manager.getKeyBinding(KeyBindingsManager.Action.DROP_BOMB));

        if (keysInfoLabel != null) {
            keysInfoLabel.setText(up + " " + down + " " + left + " " + right + " POUR BOUGER  |  " + bomb + " POUR POSER UNE BOMBE");
        }
        if (clientSlotLabel != null) {
            clientSlotLabel.setText("Vous êtes : " + manager.getClientSlotName());
        }
    }

    private String getKeyDisplayName(KeyCode keyCode) {
        if (keyCode == null) return "?";
        return switch (keyCode) {
            case UP -> "↑";
            case DOWN -> "↓";
            case LEFT -> "←";
            case RIGHT -> "→";
            case SPACE -> "ESPACE";
            case SHIFT -> "SHIFT";
            default -> keyCode.getName().toUpperCase();
        };
    }

    @FXML
    private void onConnectClicked() {
        LocalServerManager.exitLocalMode();
        String ipAddress = ipTextField.getText().trim();

        if (ipAddress.isEmpty()) {
            showInfo("Erreur", "Veuillez entrer une adresse IP.");
            return;
        }

        connectButton.setDisable(true);
        connectButton.setText("Connexion...");

        CompletableFuture.supplyAsync(() -> App.connectToServer(ipAddress))
                .orTimeout(3, TimeUnit.SECONDS)
                .whenComplete((connected, error) -> Platform.runLater(() -> {
                    if (error instanceof TimeoutException) {
                        showInfo("Délai d'attente dépassé", "Le serveur à l'adresse " + ipAddress + " n'a pas répondu assez vite (3s).");
                        connectButton.setDisable(false);
                        connectButton.setText("Connecter");
                    } else if (error != null || (connected != null && !connected)) {
                        showInfo("Échec de la connexion", "Impossible de joindre le serveur : " + ipAddress);
                        connectButton.setDisable(false);
                        connectButton.setText("Connecter");
                    } else {
                        applyConnectionUiState();
                    }
                }));
    }

    @FXML
    private void onLocalClicked(ActionEvent event) {
        LocalServerManager.enterLocalMode();
        localButton.setDisable(true);
        localButton.setText("Connexion... ");

        final int port = Integer.parseInt(System.getProperty("sae.server.port", "7777"));
        CompletableFuture.supplyAsync(() -> {
            boolean ok = LocalServerManager.ensureLocalServerRunning(port);
            if (!ok) {
                return false;
            }
            LocalServerManager.waitForLocalServer(port, Duration.ofSeconds(3));
            return App.connectToServer("localhost");
        }).orTimeout(5, TimeUnit.SECONDS)
                .whenComplete((connected, error) -> Platform.runLater(() -> {
                    localButton.setText("JOUER EN LOCAL");
                    localButton.setDisable(false);
                    if (error instanceof TimeoutException) {
                        LocalServerManager.exitLocalMode();
                        showInfo("Délai d'attente dépassé", "Le serveur local n'a pas répondu assez vite. Réessaie.");
                        return;
                    }
                    if (error != null || (connected != null && !connected)) {
                        LocalServerManager.exitLocalMode();
                        showInfo("Échec", "Impossible de démarrer ou joindre le serveur local (localhost:" + port + ").");
                        return;
                    }
                    SceneManager.switchScene(event, "/fr/iutgon/sae401/fxml/CreateLobby.fxml");
                }));
    }

    @FXML
    private void onMultiplayerClicked(ActionEvent event) {
        LocalServerManager.exitLocalMode();
        SceneManager.switchScene(event, "/fr/iutgon/sae401/fxml/MenuLobby.fxml");
    }

    @FXML
    private void onSettingsClicked(ActionEvent event) {
        SceneManager.switchScene(event, "/fr/iutgon/sae401/fxml/Settings.fxml");
    }

    @FXML
    private void onQuitClicked() {
        LocalServerManager.exitLocalMode();
        Platform.exit();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/fr/iutgon/sae401/css/styleMainMenu.css").toExternalForm()
            );
        
        alert.showAndWait();
    }
}
