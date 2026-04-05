package fr.iutgon.sae401.clientSide.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.function.Consumer;

public class ChatOverlay extends VBox {
    
    public enum State {
        CLOSED,
        OPEN
    }
    
    private State currentState = State.CLOSED;
    
    private final VBox closedView;
    private final VBox openView;
    
    private final TextFlow chatHistoryFlow;
    private final ScrollPane chatScrollPane;
    private final TextField messageInputField;
    private final Button sendButton;
    
    private Consumer<String> onMessageSend;
    private Runnable onClose;
    
    public ChatOverlay() {
        this.setPickOnBounds(false);
        
        // Closed view: small icon/bar
        closedView = createClosedView();
        
        // Open view: full chat panel
        openView = createOpenView();
        
        chatHistoryFlow = new TextFlow();
        chatHistoryFlow.setStyle("-fx-padding: 5;");
        
        chatScrollPane = new ScrollPane(chatHistoryFlow);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        messageInputField = new TextField();
        messageInputField.setPromptText("Tapez votre message...");
        messageInputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });
        
        sendButton = new Button("Envoyer");
        sendButton.setOnAction(event -> sendMessage());
        
        HBox inputBox = new HBox(5, messageInputField, sendButton);
        inputBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(messageInputField, Priority.ALWAYS);
        
        VBox chatContent = new VBox(5);
        chatContent.getChildren().addAll(chatScrollPane, inputBox);
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);
        chatContent.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.75);" +
            "-fx-background-radius: 5;" +
            "-fx-padding: 10;" +
            "-fx-border-color: rgba(255, 255, 255, 0.3);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 5;"
        );
        
        openView.getChildren().add(chatContent);
        VBox.setVgrow(chatContent, Priority.ALWAYS);
        
        // Start in closed state
        this.getChildren().add(closedView);
    }
    
    private VBox createClosedView() {
        VBox box = new VBox(5);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPrefWidth(35);
        box.setPrefHeight(100);
        box.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.6);" +
            "-fx-background-radius: 5;" +
            "-fx-padding: 5;" +
            "-fx-border-color: rgba(255, 255, 255, 0.3);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 5;"
        );
        
        Label chatIcon = new Label("💬");
        chatIcon.setStyle("-fx-font-size: 20px;");
        
        Label chatLabel = new Label("C\nH\nA\nT");
        chatLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;");
        chatLabel.setAlignment(Pos.CENTER);
        
        box.getChildren().addAll(chatIcon, chatLabel);
        
        // Make it clickable to open
        box.setOnMouseClicked(event -> open());
        box.setStyle(box.getStyle() + "-fx-cursor: hand;");
        
        return box;
    }
    
    private VBox createOpenView() {
        VBox box = new VBox(5);
        box.setPrefWidth(280);
        box.setAlignment(Pos.TOP_LEFT);
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(5, 10, 5, 10));
        header.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.8);" +
            "-fx-background-radius: 5 5 0 0;"
        );
        
        Label titleLabel = new Label("💬 Chat");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        Button closeButton = new Button("✕");
        closeButton.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-cursor: hand;"
        );
        closeButton.setOnAction(event -> close());
        
        header.getChildren().addAll(titleLabel, closeButton);
        box.getChildren().add(header);
        
        return box;
    }
    
    public void open() {
        if (currentState == State.OPEN) {
            return;
        }
        
        currentState = State.OPEN;
        this.getChildren().clear();
        this.getChildren().add(openView);
        
        // Request focus on the text field
        messageInputField.requestFocus();
    }
    
    public void close() {
        if (currentState == State.CLOSED) {
            return;
        }
        
        currentState = State.CLOSED;
        this.getChildren().clear();
        this.getChildren().add(closedView);
        
        // Clear the input field
        messageInputField.clear();
        
        // Notify close callback
        if (onClose != null) {
            onClose.run();
        }
    }
    
    public void sendMessage() {
        String message = messageInputField.getText();
        if (message != null && !message.trim().isEmpty()) {
            if (onMessageSend != null) {
                onMessageSend.accept(message.trim());
            }
            messageInputField.clear();
            close();
        }
    }
    
    public void appendMessage(String from, String message) {
        Text fromText = new Text(from + ": ");
        fromText.setStyle("-fx-fill: #4CAF50; -fx-font-weight: bold;");
        
        Text messageText = new Text(message + "\n");
        messageText.setStyle("-fx-fill: white;");
        
        chatHistoryFlow.getChildren().addAll(fromText, messageText);
        
        // Auto-scroll to bottom
        chatScrollPane.setVvalue(1.0);
    }
    
    public void clearHistory() {
        chatHistoryFlow.getChildren().clear();
    }
    
    public State getState() {
        return currentState;
    }
    
    public boolean isOpen() {
        return currentState == State.OPEN;
    }
    
    public boolean isClosed() {
        return currentState == State.CLOSED;
    }
    
    public void setOnMessageSend(Consumer<String> callback) {
        this.onMessageSend = callback;
    }
    
    public void setOnClose(Runnable callback) {
        this.onClose = callback;
    }
    
    public TextField getMessageInputField() {
        return messageInputField;
    }
}
