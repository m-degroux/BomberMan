package fr.iutgon.sae401.clientSide.controllers;

import fr.iutgon.sae401.clientSide.App;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.net.URL;

public class IntroController {

    @FXML
    private ImageView imageView;
    
    private PauseTransition delay;

    @FXML
    public void initialize() {
        try {
            URL imageUrl = getClass().getResource("/fr/iutgon/sae401/media/intro.gif");
            
            if (imageUrl != null) {
                Image gif = new Image(imageUrl.toExternalForm());
                imageView.setImage(gif);
            }

            delay = new PauseTransition(Duration.millis(3000)); 
            
            delay.setOnFinished(event -> fadeOutAndGoToMenu());
            delay.play();

        } catch (Exception e) {
            e.printStackTrace();
            goToMainMenu();
        }
    }

    @FXML
    private void skipIntro() {
        if (delay != null) {
            delay.stop();
        }
        imageView.setOpacity(0); 
        goToMainMenu();
    }

    private void fadeOutAndGoToMenu() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(850), imageView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> goToMainMenu()); 
        fadeOut.play();
    }

    private void goToMainMenu() {
        Platform.runLater(() -> {
            try {
                App.setRoot("MainMenu");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}