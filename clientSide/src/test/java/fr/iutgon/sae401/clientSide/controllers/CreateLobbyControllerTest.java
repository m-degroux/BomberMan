package fr.iutgon.sae401.clientSide.controllers;

import fr.iutgon.sae401.TestReflectionUtils;
import fr.iutgon.sae401.clientSide.JavaFxTestUtils;
import fr.iutgon.sae401.clientSide.service.LocalReplayContext;
import fr.iutgon.sae401.clientSide.service.LocalReplayContext.LocalGameSettings;
import fr.iutgon.sae401.clientSide.service.LocalServerManager;
import fr.iutgon.sae401.common.model.map.MapTheme;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateLobbyControllerTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestUtils.initToolkit();
    }

    @AfterEach
    void cleanupReplayContext() throws Exception {
        LocalReplayContext.clear();
        TestReflectionUtils.setStaticField(LocalServerManager.class, "localModeEnabled", false);
        TestReflectionUtils.setStaticField(LocalServerManager.class, "ownedServer", false);
        TestReflectionUtils.setStaticField(LocalServerManager.class, "server", null);
    }

    @Test
    void initialize_populatesMapComboBox() throws Exception {
        CreateLobbyController controller = new CreateLobbyController();
        ComboBox<MapTheme> mapComboBox = new ComboBox<>();
        TestReflectionUtils.setField(controller, "mapComboBox", mapComboBox);

        JavaFxTestUtils.runAndWait(controller::initialize);

        assertEquals(MapTheme.CLASSIC, mapComboBox.getValue());
        assertEquals(MapTheme.values().length, mapComboBox.getItems().size());
    }

    @Test
    void initialize_localReplayPrefillsFieldsWithPreviousSettings() throws Exception {
        LocalServerManager.enterLocalMode();
        LocalReplayContext.rememberLastLocalGameSettings(new LocalGameSettings(
                "partie-test",
                "secret",
                21,
                17,
                5,
                3,
                4,
                1450,
                65,
                MapTheme.PERFECT
        ));
        LocalReplayContext.requestRestoreOnNextCreateLobby();

        CreateLobbyController controller = new CreateLobbyController();
        TextField serverNameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Spinner<Integer> widthSpinner = intSpinner(5, 51, 15, 2);
        Spinner<Integer> heightSpinner = intSpinner(5, 51, 13, 2);
        Spinner<Integer> healthSpinner = intSpinner(1, 10, 3, 1);
        Spinner<Integer> maxBombsSpinner = intSpinner(1, 10, 1, 1);
        Spinner<Integer> bombRangeSpinner = intSpinner(1, 10, 1, 1);
        Spinner<Integer> bombCooldownSpinner = intSpinner(0, 10000, 2000, 10);
        Spinner<Integer> fillingSpinner = intSpinner(0, 100, 70, 5);
        ComboBox<MapTheme> mapComboBox = new ComboBox<>();
        Button createConfirmButton = new Button();

        TestReflectionUtils.setField(controller, "serverNameField", serverNameField);
        TestReflectionUtils.setField(controller, "passwordField", passwordField);
        TestReflectionUtils.setField(controller, "widthSpinner", widthSpinner);
        TestReflectionUtils.setField(controller, "heightSpinner", heightSpinner);
        TestReflectionUtils.setField(controller, "healthSpinner", healthSpinner);
        TestReflectionUtils.setField(controller, "maxBombsSpinner", maxBombsSpinner);
        TestReflectionUtils.setField(controller, "bombRangeSpinner", bombRangeSpinner);
        TestReflectionUtils.setField(controller, "bombCooldownSpinner", bombCooldownSpinner);
        TestReflectionUtils.setField(controller, "fillingSpinner", fillingSpinner);
        TestReflectionUtils.setField(controller, "mapComboBox", mapComboBox);
        TestReflectionUtils.setField(controller, "createConfirmButton", createConfirmButton);

        JavaFxTestUtils.runAndWait(controller::initialize);

        assertEquals("partie-test", serverNameField.getText());
        assertEquals("secret", passwordField.getText());
        assertEquals(21, widthSpinner.getValue());
        assertEquals(17, heightSpinner.getValue());
        assertEquals(5, healthSpinner.getValue());
        assertEquals(3, maxBombsSpinner.getValue());
        assertEquals(4, bombRangeSpinner.getValue());
        assertEquals(1450, bombCooldownSpinner.getValue());
        assertEquals(65, fillingSpinner.getValue());
        assertEquals(MapTheme.PERFECT, mapComboBox.getValue());
        assertEquals("Lancer la partie", createConfirmButton.getText());
    }

    @Test
    void initialize_localReplayRoundsEvenMapDimensionsToNextOdd() throws Exception {
        LocalServerManager.enterLocalMode();
        LocalReplayContext.rememberLastLocalGameSettings(new LocalGameSettings(
                "partie-test",
                "",
                20,
                12,
                3,
                1,
                2,
                2000,
                60,
                MapTheme.CLASSIC
        ));
        LocalReplayContext.requestRestoreOnNextCreateLobby();

        CreateLobbyController controller = new CreateLobbyController();
        Spinner<Integer> widthSpinner = intSpinner(5, 51, 15, 2);
        Spinner<Integer> heightSpinner = intSpinner(5, 51, 13, 2);
        Button createConfirmButton = new Button();

        TestReflectionUtils.setField(controller, "widthSpinner", widthSpinner);
        TestReflectionUtils.setField(controller, "heightSpinner", heightSpinner);
        TestReflectionUtils.setField(controller, "createConfirmButton", createConfirmButton);

        JavaFxTestUtils.runAndWait(controller::initialize);

        assertEquals(21, widthSpinner.getValue());
        assertEquals(13, heightSpinner.getValue());
    }

    @Test
    void initialize_forcesEvenSpinnerValuesToOdd() throws Exception {
        CreateLobbyController controller = new CreateLobbyController();
        Spinner<Integer> widthSpinner = intSpinner(5, 51, 15, 2);
        Spinner<Integer> heightSpinner = intSpinner(5, 51, 13, 2);

        TestReflectionUtils.setField(controller, "widthSpinner", widthSpinner);
        TestReflectionUtils.setField(controller, "heightSpinner", heightSpinner);

        JavaFxTestUtils.runAndWait(controller::initialize);
        JavaFxTestUtils.runAndWait(() -> {
            widthSpinner.getValueFactory().setValue(20);
            heightSpinner.getValueFactory().setValue(50);
        });

        assertEquals(21, widthSpinner.getValue());
        assertEquals(51, heightSpinner.getValue());
    }

    private static Spinner<Integer> intSpinner(int min, int max, int initial, int step) {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial, step));
        return spinner;
    }
}
