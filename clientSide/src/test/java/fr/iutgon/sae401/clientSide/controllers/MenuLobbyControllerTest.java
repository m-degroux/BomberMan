package fr.iutgon.sae401.clientSide.controllers;

import fr.iutgon.sae401.TestReflectionUtils;
import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.clientSide.JavaFxTestUtils;
import fr.iutgon.sae401.clientSide.network.NetworkManager;
import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MenuLobbyControllerTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestUtils.initToolkit();
        TestReflectionUtils.setStaticField(App.class, "scene", new Scene(new Pane()));
    }

    @Test
    void initialize_bindsJoinButtonToSelection() throws Exception {
        MenuLobbyController controller = new MenuLobbyController();
        TableView<MenuLobbyController.RoomRow> arenaTable = new TableView<>();
        TableColumn<MenuLobbyController.RoomRow, String> nameColumn = new TableColumn<>();
        TableColumn<MenuLobbyController.RoomRow, String> statusColumn = new TableColumn<>();
        TableColumn<MenuLobbyController.RoomRow, String> playersColumn = new TableColumn<>();
        Button joinButton = new Button();
        Button createButton = new Button();

        TestReflectionUtils.setField(controller, "arenaTable", arenaTable);
        TestReflectionUtils.setField(controller, "nameColumn", nameColumn);
        TestReflectionUtils.setField(controller, "statusColumn", statusColumn);
        TestReflectionUtils.setField(controller, "playersColumn", playersColumn);
        TestReflectionUtils.setField(controller, "joinButton", joinButton);
        TestReflectionUtils.setField(controller, "createButton", createButton);

        JavaFxTestUtils.runAndWait(controller::initialize);

        MenuLobbyController.RoomRow room = new MenuLobbyController.RoomRow("room-1", "Libre", "1/4", false);
        JavaFxTestUtils.runAndWait(() -> {
            arenaTable.getItems().add(room);
            arenaTable.getSelectionModel().select(room);
        });

        assertFalse(joinButton.isDisable());

        NetworkManager.getManager().removeObserver(controller);
    }

    @Test
    void onMessage_roomsPopulatesTable() throws Exception {
        MenuLobbyController controller = new MenuLobbyController();
        TableView<MenuLobbyController.RoomRow> arenaTable = new TableView<>();
        TableColumn<MenuLobbyController.RoomRow, String> nameColumn = new TableColumn<>();
        TableColumn<MenuLobbyController.RoomRow, String> statusColumn = new TableColumn<>();
        TableColumn<MenuLobbyController.RoomRow, String> playersColumn = new TableColumn<>();
        Button joinButton = new Button();
        Button createButton = new Button();

        TestReflectionUtils.setField(controller, "arenaTable", arenaTable);
        TestReflectionUtils.setField(controller, "nameColumn", nameColumn);
        TestReflectionUtils.setField(controller, "statusColumn", statusColumn);
        TestReflectionUtils.setField(controller, "playersColumn", playersColumn);
        TestReflectionUtils.setField(controller, "joinButton", joinButton);
        TestReflectionUtils.setField(controller, "createButton", createButton);

        JavaFxTestUtils.runAndWait(controller::initialize);

        MessageEnvelope roomsMessage = new MessageEnvelope(
                "rooms",
                null,
                Json.object(Map.of("rooms", Json.array(java.util.List.of(
                        Json.object(Map.of("id", Json.of("room-1"), "members", Json.of(2)))
                ))))
        );

        JavaFxTestUtils.runAndWait(() -> {
            try {
                controller.onMessage(roomsMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        JavaFxTestUtils.runAndWait(() -> {
        });

        assertEquals(1, arenaTable.getItems().size());
        MenuLobbyController.RoomRow row = arenaTable.getItems().get(0);
        assertEquals("room-1", row.getName());
        assertEquals("Libre", row.getStatus());
        assertEquals("2/4", row.getPlayers());

        NetworkManager.getManager().removeObserver(controller);
    }

    @Test
    void handleJoinServer_setsPendingJoinRequestIdAndJoinInFlight() throws Exception {
        MenuLobbyController controller = new MenuLobbyController();
        TableView<MenuLobbyController.RoomRow> arenaTable = new TableView<>();
        TestReflectionUtils.setField(controller, "arenaTable", arenaTable);

        MenuLobbyController.RoomRow room = new MenuLobbyController.RoomRow("room-1", "Libre", "1/4", false);

        JavaFxTestUtils.runAndWait(() -> {
            arenaTable.getItems().add(room);
            arenaTable.getSelectionModel().select(room);
        });

        JavaFxTestUtils.runAndWait(() -> {
            try {
                TestReflectionUtils.invokeMethod(controller, "handleJoinServer", new Class<?>[]{ActionEvent.class}, new ActionEvent());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        String pendingJoin = (String) TestReflectionUtils.getField(controller, "pendingJoinRequestId");
        assertNotNull(pendingJoin);
        assertFalse(pendingJoin.isBlank());

        var joinInFlight = (javafx.beans.property.BooleanProperty) TestReflectionUtils.getField(controller, "joinInFlight");
        assertTrue(joinInFlight.get());
    }

    @Test
    void onMessage_roomJoinedSwitchesToChatLobby() throws Exception {
        MenuLobbyController controller = new MenuLobbyController();
        TableView<MenuLobbyController.RoomRow> arenaTable = new TableView<>();
        TestReflectionUtils.setField(controller, "arenaTable", arenaTable);
        TestReflectionUtils.setField(controller, "pendingJoinRequestId", "request-123");

        Scene scene = new Scene(new Pane());
        TestReflectionUtils.setStaticField(App.class, "scene", scene);
        var initialRoot = scene.getRoot();

        MessageEnvelope joinedMessage = new MessageEnvelope(
                "room_joined",
                "request-123",
                Json.emptyObject()
        );

        JavaFxTestUtils.runAndWait(() -> {
            try {
                controller.onMessage(joinedMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        JavaFxTestUtils.runAndWait(() -> {
        });

        assertNotSame(initialRoot, scene.getRoot());
    }

    @Test
    void roomRow_getters_returnExpectedValues() {
        MenuLobbyController.RoomRow row = new MenuLobbyController.RoomRow("room-1", "Libre", "1/4", false);
        assertEquals("room-1", row.getName());
        assertEquals("Libre", row.getStatus());
        assertEquals("1/4", row.getPlayers());
    }
}
