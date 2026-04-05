package fr.iutgon.sae401.clientSide.utils;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.net.URL;

public class SceneManager {

	public static void switchScene(ActionEvent event, String fxmlFileName) {
        try {
            Scene oldScene = ((Node) event.getSource()).getScene();
            URL fxmlUrl = SceneManager.class.getResource(fxmlFileName);

            if (fxmlUrl == null) {
                App.LOGGER.log(new LogMessage("[GAME] Fichier FXML introuvable : " + fxmlFileName, LogLevel.ERROR));
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            oldScene.setRoot(root);

            App.LOGGER.log(new LogMessage("[GAME] Scène changée vers : " + fxmlFileName, LogLevel.INFO));

        } catch (IOException e) {
            App.LOGGER.log(new LogMessage("[GAME] Erreur lors du changement de scène (" + fxmlFileName + ") : " + e.getMessage(), LogLevel.ERROR));
        }
    }
}