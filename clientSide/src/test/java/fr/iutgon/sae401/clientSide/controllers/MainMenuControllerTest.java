package fr.iutgon.sae401.clientSide.controllers;

import fr.iutgon.sae401.clientSide.JavaFxTestUtils;
import fr.iutgon.sae401.TestReflectionUtils;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainMenuControllerTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestUtils.initToolkit();
    }

    @Test
    void initialize_enablesLocalButton() throws Exception {
        MainMenuController controller = new MainMenuController();
        Button localButton = new Button();
        localButton.setDisable(true);
        TestReflectionUtils.setField(controller, "localButton", localButton);

        JavaFxTestUtils.runAndWait(() -> {
            try {
                TestReflectionUtils.invokeMethod(controller, "initialize", new Class<?>[0]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertFalse(localButton.isDisable());
    }
}
