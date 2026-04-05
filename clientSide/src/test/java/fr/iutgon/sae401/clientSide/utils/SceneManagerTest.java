package fr.iutgon.sae401.clientSide.utils;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneManagerTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
    }

    @Test
    void switchScene_missingFxml_doesNotThrow() {
        Pane pane = new Pane();
        Scene scene = new Scene(pane);
        ActionEvent event = new ActionEvent(pane, null);

        assertDoesNotThrow(() -> SceneManager.switchScene(event, "/does-not-exist.fxml"));
        assertSame(scene, pane.getScene());
    }
}
