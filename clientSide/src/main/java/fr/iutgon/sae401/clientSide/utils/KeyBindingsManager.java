package fr.iutgon.sae401.clientSide.utils;

import javafx.scene.input.KeyCode;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;

public class KeyBindingsManager {
    private static final String GAMEPAD_SELECTION_KEY = "GAMEPAD_SELECTION";
    private static final String NO_GAMEPAD = "__NONE__";
    private final String configFile;
    private final Map<String, KeyCode> keyBindings = new HashMap<>();
    private final ClientSlotManager slotManager;
    private String selectedGamepad = NO_GAMEPAD;
    
    
    public enum Action {
        MOVE_UP,
        MOVE_DOWN,
        MOVE_LEFT,
        MOVE_RIGHT,
        DROP_BOMB
    }
    
    public KeyBindingsManager() {
        this.slotManager = new ClientSlotManager();
        int slot = slotManager.getSlot();
        this.configFile = System.getProperty("user.home") + "/.bomberman_keybindings_slot_" + slot + ".properties";
        
        loadDefaultBindingsForSlot();
        loadFromFile();
        
        App.LOGGER.log(new LogMessage("[KeyBindings] " + slotManager.getSlotName() + " - Fichier: " + this.configFile, LogLevel.INFO));
    }
    
    private void loadDefaultBindingsForSlot() {
        Map<Action, KeyCode> defaultKeys = slotManager.getDefaultKeysForSlot();
        for (Map.Entry<Action, KeyCode> entry : defaultKeys.entrySet()) {
            keyBindings.put(entry.getKey().name(), entry.getValue());
        }
        selectedGamepad = NO_GAMEPAD;
    }
    
    public void resetToDefault() {
        loadDefaultBindingsForSlot();
        saveToFile();
    }
    
    public int getClientSlot() {
        return slotManager.getSlot();
    }
    
    public String getClientSlotName() {
        return slotManager.getSlotName();
    }
    
    public KeyCode getKeyBinding(Action action) {
        return keyBindings.get(action.name());
    }
    
    public void setKeyBinding(Action action, KeyCode keyCode) {
        keyBindings.put(action.name(), keyCode);
    }
    
    public Map<Action, KeyCode> getAllBindings() {
        Map<Action, KeyCode> result = new HashMap<>();
        for (Action action : Action.values()) {
            result.put(action, keyBindings.get(action.name()));
        }
        return result;
    }

    public String getSelectedGamepad() {
        return selectedGamepad;
    }

    public void setSelectedGamepad(String selectedGamepad) {
        if (selectedGamepad == null || selectedGamepad.isBlank()) {
            this.selectedGamepad = NO_GAMEPAD;
            return;
        }
        this.selectedGamepad = selectedGamepad;
    }
    
    public void saveToFile() {
        Properties props = new Properties();
        for (Map.Entry<String, KeyCode> entry : keyBindings.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue().name());
        }
        props.setProperty(GAMEPAD_SELECTION_KEY, selectedGamepad);
        
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "Bomberman Key Bindings - Instance Config");
        } catch (IOException e) {
            App.LOGGER.log(new LogMessage("Erreur lors de la sauvegarde des touches: " + e.getMessage(), LogLevel.ERROR));
        }
    }
    
    public void loadFromFile() {
        File file = new File(configFile);
        if (!file.exists()) {
            return;
        }
        
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
            for (String key : props.stringPropertyNames()) {
                if (GAMEPAD_SELECTION_KEY.equals(key)) {
                    selectedGamepad = props.getProperty(key, NO_GAMEPAD);
                    continue;
                }
                try {
                    KeyCode keyCode = KeyCode.valueOf(props.getProperty(key));
                    keyBindings.put(key, keyCode);
                } catch (IllegalArgumentException e) {
                    App.LOGGER.log(new LogMessage("Touche invalide ignorée: " + props.getProperty(key), LogLevel.INFO));
                }
            }
        } catch (IOException e) {
            App.LOGGER.log(new LogMessage("Erreur lors du chargement des touches: " + e.getMessage(), LogLevel.ERROR));
        }
    }
    
    public String getConfigFilePath() {
        return configFile;
    }
    
    public boolean hasConflict(Action currentAction, KeyCode newKey) {
        for (Map.Entry<String, KeyCode> entry : keyBindings.entrySet()) {
            if (!entry.getKey().equals(currentAction.name()) && entry.getValue().equals(newKey)) {
                return true;
            }
        }
        return false;
    }
}
