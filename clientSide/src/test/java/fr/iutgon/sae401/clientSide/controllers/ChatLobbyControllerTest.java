package fr.iutgon.sae401.clientSide.controllers;

import fr.iutgon.sae401.TestReflectionUtils;
import fr.iutgon.sae401.clientSide.JavaFxTestUtils;
import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChatLobbyControllerTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestUtils.initToolkit();
    }

    @Test
    void sendMessage_clearsInputWhenTextIsNotEmpty() throws Exception {
        ChatLobbyController controller = new ChatLobbyController();
        TextField messageInput = new TextField(" Bonjour ");
        TextArea chatHistory = new TextArea();
        TestReflectionUtils.setField(controller, "messageInputField", messageInput);
        TestReflectionUtils.setField(controller, "chatHistoryArea", chatHistory);

        JavaFxTestUtils.runAndWait(() -> {
            try {
                TestReflectionUtils.invokeMethod(controller, "sendMessage", new Class<?>[0]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("", messageInput.getText());
    }

    @Test
    void onMessage_chatAppendsChatLine() throws Exception {
        ChatLobbyController controller = new ChatLobbyController();
        TextArea chatHistory = new TextArea();
        TestReflectionUtils.setField(controller, "chatHistoryArea", chatHistory);

        MessageEnvelope message = new MessageEnvelope(
                "chat",
                null,
                Json.object(Map.of("from", Json.of("Alice"), "message", Json.of("salut")))
        );

        JavaFxTestUtils.runAndWait(() -> {
            try {
                controller.onMessage(message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("Alice: salut\n", chatHistory.getText());
    }

    @Test
    void onMessage_lobbyStateUpdatesPlayersAndLabel() throws Exception {
        ChatLobbyController controller = new ChatLobbyController();
        Label arenaNameLabel = new Label();
        ListView<String> playerListView = new ListView<>();
        TestReflectionUtils.setField(controller, "arenaNameLabel", arenaNameLabel);
        TestReflectionUtils.setField(controller, "playerListView", playerListView);

        Json member = Json.object(Map.of(
                "clientId", Json.of("c1"),
                "nickname", Json.of("Bob"),
                "ready", Json.of(true)
        ));

        MessageEnvelope message = new MessageEnvelope(
                "lobby_state",
                null,
                Json.object(Map.of("roomId", Json.of("lobby-123"), "members", Json.array(java.util.List.of(member))))
        );

        JavaFxTestUtils.runAndWait(() -> {
            try {
                controller.onMessage(message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("lobby-123", arenaNameLabel.getText());
        assertEquals(1, playerListView.getItems().size());
        assertEquals("Bob (Prêt)", playerListView.getItems().get(0));
    }

    @Test
    void toggleReady_updatesButtonTextAndStyle() throws Exception {
        ChatLobbyController controller = new ChatLobbyController();
        Button readyButton = new Button();
        TestReflectionUtils.setField(controller, "readyButton", readyButton);

        JavaFxTestUtils.runAndWait(() -> {
            try {
                TestReflectionUtils.invokeMethod(controller, "toggleReady", new Class<?>[0]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("Annuler (Prêt)", readyButton.getText());
        assertTrue(readyButton.getStyle().contains("#90ee90"));

        JavaFxTestUtils.runAndWait(() -> {
            try {
                TestReflectionUtils.invokeMethod(controller, "toggleReady", new Class<?>[0]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("Prêt", readyButton.getText());
        assertEquals("", readyButton.getStyle());
    }

    @Test
    void changePseudo_clearsFieldAfterSending() throws Exception {
        ChatLobbyController controller = new ChatLobbyController();
        TextField pseudoField = new TextField("Jean");
        TestReflectionUtils.setField(controller, "pseudoField", pseudoField);

        JavaFxTestUtils.runAndWait(() -> {
            try {
                TestReflectionUtils.invokeMethod(controller, "changePseudo", new Class<?>[0]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("", pseudoField.getText());
    }
}
