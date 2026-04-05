package fr.iutgon.sae401.clientSide.controllers;

import fr.iutgon.sae401.TestReflectionUtils;
import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.clientSide.JavaFxTestUtils;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntroControllerTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestUtils.initToolkit();
        TestReflectionUtils.setStaticField(App.class, "scene", new Scene(new Group()));
    }

    @Test
    void skipIntro_setsOpacityToZeroAndStopsDelay() throws Exception {
        IntroController controller = new IntroController();
        ImageView imageView = new ImageView();
        PauseTransition delay = new PauseTransition(Duration.millis(1000));
        delay.play();

        TestReflectionUtils.setField(controller, "imageView", imageView);
        TestReflectionUtils.setField(controller, "delay", delay);

        JavaFxTestUtils.runAndWait(() -> {
            try {
                TestReflectionUtils.invokeMethod(controller, "skipIntro", new Class<?>[0]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals(0.0, imageView.getOpacity());
        assertNotEquals(Animation.Status.RUNNING, delay.getStatus());
    }
}
