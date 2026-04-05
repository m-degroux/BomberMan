package fr.iutgon.sae401.clientSide.controllers;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.clientSide.utils.GamepadInputManager;
import fr.iutgon.sae401.clientSide.utils.KeyBindingsManager;
import fr.iutgon.sae401.clientSide.utils.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class SettingsController {
    private static final String NO_GAMEPAD_LABEL = "Aucune";
    @FXML private Label moveUpLabel;
    @FXML private Label moveDownLabel;
    @FXML private Label moveLeftLabel;
    @FXML private Label moveRightLabel;
    @FXML private Label dropBombLabel;
    @FXML private Label clientSlotLabel;
    @FXML private ComboBox<String> gamepadComboBox;
    
    @FXML private Button moveUpButton;
    @FXML private Button moveDownButton;
    @FXML private Button moveLeftButton;
    @FXML private Button moveRightButton;
    @FXML private Button dropBombButton;
    
    @FXML private VBox rootPane;
    
    private KeyBindingsManager manager;
    private Button currentlyListening = null;
    private KeyBindingsManager.Action currentAction = null;
    private final Map<KeyBindingsManager.Action, Label> labelMap = new HashMap<>();
    
    @FXML
    public void initialize() {
        manager = App.getKeyBindingsManager();
        
        if (clientSlotLabel != null) {
            clientSlotLabel.setText(manager.getClientSlotName());
        }
        
        labelMap.put(KeyBindingsManager.Action.MOVE_UP, moveUpLabel);
        labelMap.put(KeyBindingsManager.Action.MOVE_DOWN, moveDownLabel);
        labelMap.put(KeyBindingsManager.Action.MOVE_LEFT, moveLeftLabel);
        labelMap.put(KeyBindingsManager.Action.MOVE_RIGHT, moveRightLabel);
        labelMap.put(KeyBindingsManager.Action.DROP_BOMB, dropBombLabel);
        
        updateAllLabels();
        initGamepadSelection();
        
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
    }

    private void initGamepadSelection() {
        if (gamepadComboBox == null) {
            return;
        }
        gamepadComboBox.getItems().clear();
        gamepadComboBox.getItems().add(NO_GAMEPAD_LABEL);
        String[] pads = App.getGamepadInputManager().listGamepads();
        for (String pad : pads) {
            gamepadComboBox.getItems().add(pad);
        }

        String selected = manager.getSelectedGamepad();
        if (selected == null || selected.isBlank() || GamepadInputManager.NO_GAMEPAD.equals(selected)) {
            gamepadComboBox.setValue(NO_GAMEPAD_LABEL);
            return;
        }
        if (gamepadComboBox.getItems().contains(selected)) {
            gamepadComboBox.setValue(selected);
        } else {
            gamepadComboBox.setValue(NO_GAMEPAD_LABEL);
        }
    }
    
    private void updateAllLabels() {
        for (Map.Entry<KeyBindingsManager.Action, KeyCode> entry : manager.getAllBindings().entrySet()) {
            Label label = labelMap.get(entry.getKey());
            if (label != null) {
                label.setText(getKeyDisplayName(entry.getValue()));
            }
        }
    }
    
    private String getKeyDisplayName(KeyCode keyCode) {
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
    private void onChangeMoveUp() {
        startListening(KeyBindingsManager.Action.MOVE_UP, moveUpButton);
    }
    
    @FXML
    private void onChangeMoveDown() {
        startListening(KeyBindingsManager.Action.MOVE_DOWN, moveDownButton);
    }
    
    @FXML
    private void onChangeMoveLeft() {
        startListening(KeyBindingsManager.Action.MOVE_LEFT, moveLeftButton);
    }
    
    @FXML
    private void onChangeMoveRight() {
        startListening(KeyBindingsManager.Action.MOVE_RIGHT, moveRightButton);
    }
    
    @FXML
    private void onChangeDropBomb() {
        startListening(KeyBindingsManager.Action.DROP_BOMB, dropBombButton);
    }
    
    private void startListening(KeyBindingsManager.Action action, Button button) {
        if (currentlyListening != null) {
            currentlyListening.setText("Changer");
        }
        
        currentlyListening = button;
        currentAction = action;
        button.setText("Appuyez sur une touche...");
        button.requestFocus();
    }
    
    private void handleKeyPress(KeyEvent event) {
        if (currentlyListening != null && currentAction != null) {
            KeyCode newKey = event.getCode();
            
            if (newKey == KeyCode.ESCAPE) {
                cancelListening();
                event.consume();
                return;
            }
            
            if (manager.hasConflict(currentAction, newKey)) {
                showAlert("Conflit de touche", 
                    "Cette touche est déjà assignée à une autre action.\nVeuillez choisir une autre touche.");
                event.consume();
                return;
            }
            
            manager.setKeyBinding(currentAction, newKey);
            Label label = labelMap.get(currentAction);
            if (label != null) {
                label.setText(getKeyDisplayName(newKey));
            }
            
            cancelListening();
            event.consume();
        }
    }
    
    private void cancelListening() {
        if (currentlyListening != null) {
            currentlyListening.setText("Changer");
        }
        currentlyListening = null;
        currentAction = null;
    }
    
    @FXML
    private void onResetToDefault() {
        manager.resetToDefault();
        updateAllLabels();
        showInfo("Réinitialisation", "Les touches ont été réinitialisées aux valeurs par défaut.");
    }
    
    @FXML
    private void onSave(ActionEvent event) {
        String selectedPad = gamepadComboBox == null ? NO_GAMEPAD_LABEL : gamepadComboBox.getValue();
        if (selectedPad == null || NO_GAMEPAD_LABEL.equals(selectedPad)) {
            manager.setSelectedGamepad(GamepadInputManager.NO_GAMEPAD);
        } else {
            manager.setSelectedGamepad(selectedPad);
        }
        manager.saveToFile();
        showInfo("Sauvegarde", "Les paramètres ont été sauvegardés avec succès.");
        SceneManager.switchScene(event, "/fr/iutgon/sae401/fxml/MainMenu.fxml");
    }
    
    @FXML
    private void onCancel(ActionEvent event) {
        manager.loadFromFile();
        SceneManager.switchScene(event, "/fr/iutgon/sae401/fxml/MainMenu.fxml");
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // --- AJOUT DU STYLE ---
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/fr/iutgon/sae401/css/styleMainMenu.css").toExternalForm()
        );
        
        alert.showAndWait();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // --- AJOUT DU STYLE ---
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/fr/iutgon/sae401/css/styleMainMenu.css").toExternalForm()
        );
        
        alert.showAndWait();
    }
}
