package fr.iutgon.sae401.clientSide;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;

import fr.iutgon.sae401.clientSide.network.NetworkManager;
import fr.iutgon.sae401.clientSide.network.NetworkObserver;
import fr.iutgon.sae401.clientSide.service.LocalServerManager;
import fr.iutgon.sae401.clientSide.utils.GamepadInputManager;
import fr.iutgon.sae401.clientSide.utils.GlobalKeyCapture;
import fr.iutgon.sae401.clientSide.utils.KeyBindingsManager;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * JavaFX App
 */
public class App extends Application {

    private static final NetworkManager NETWORK_MANAGER = NetworkManager.getManager();
    private static final NetworkObserver WELCOME_OBSERVER = msg -> {
        if (msg != null && "welcome".equals(msg.getType())) {
            var payload = msg.getPayload();
            if (payload != null && payload.contains("clientId") && !payload.at("clientId").isNull()) {
                serverAssignedClientId = payload.at("clientId").getString();
            }
        }
    };
	private static final int SERVER_PORT = Integer.parseInt(System.getProperty("sae.server.port", "7777"));
	private static Scene scene;
	// Legacy hook for tests/older code paths (GameControllerTest injects a GameClient via reflection).
	@SuppressWarnings("unused")
	private static volatile GameClient gameClient;
	private static volatile String serverAssignedClientId;
	private static volatile String connectedServerHost;
	private static volatile String lastServerHost;
	private static final String localClientId = UUID.randomUUID().toString();
    private static KeyBindingsManager keyBindingsManager;
    private static final GamepadInputManager gamepadInputManager = new GamepadInputManager();
	private static final Map<Consumer<MessageEnvelope>, NetworkObserver> LISTENER_ADAPTERS = new ConcurrentHashMap<>();
	
	public static final Logger LOGGER = Logger.getLogger();

	public static boolean isConnected() {
		if (gameClient != null && !gameClient.isClosed()) {
			return true;
		}
		return !NETWORK_MANAGER.isClosed();
	}

	public static void disconnect() {
		serverAssignedClientId = null;
		connectedServerHost = null;
		try {
			if (gameClient != null) {
				gameClient.close();
			}
		} catch (Exception ignored) {
		} finally {
			gameClient = null;
		}
		try {
			NETWORK_MANAGER.close();
		} catch (Exception ignored) {
		}
	}

	public static void setRoot(String fxml) throws IOException {
		scene.setRoot(loadFXML(fxml));
	}

	private static Parent loadFXML(String fxml) throws IOException {
		String path = "/fr/iutgon/sae401/fxml/" + fxml + ".fxml";
		var resource = App.class.getResource(path);
		if (resource == null) {
			throw new IOException("Fichier FXML introuvable : " + path);
		}
		return new FXMLLoader(resource).load();
	}

	public static void main(String[] args) {
		launch();
	}

	public static void sendToServer(MessageEnvelope message) {
		if (gameClient != null && !gameClient.isClosed()) {
			gameClient.send(message);
			return;
		}
		if (!NETWORK_MANAGER.isClosed()) {
			NETWORK_MANAGER.send(message);
			return;
		}
		{
			LOGGER.log(
					new LogMessage("[CLIENT] Message non envoyé (hors-ligne): " + message.getType(), LogLevel.WARNING));
		}
	}

	/**
	 * Compatibility layer for legacy controllers/tests that used App.add/removeMessageListener.
	 * Under the hood this now uses the NetworkManager observer model.
	 */
	public static void addMessageListener(Consumer<MessageEnvelope> listener) {
		if (listener == null) {
			return;
		}
		LISTENER_ADAPTERS.computeIfAbsent(listener, l -> {
			NetworkObserver obs = l::accept;
			NETWORK_MANAGER.addObserver(obs);
			return obs;
		});
	}

	public static void removeMessageListener(Consumer<MessageEnvelope> listener) {
		if (listener == null) {
			return;
		}
		NetworkObserver obs = LISTENER_ADAPTERS.remove(listener);
		if (obs != null) {
			NETWORK_MANAGER.removeObserver(obs);
		}
	}
	public static String clientId() {
		if (isConnected() && serverAssignedClientId != null && !serverAssignedClientId.isBlank())
			return serverAssignedClientId;
		return localClientId;
	}

	public static String connectedServerHost() {
		return connectedServerHost;
	}

	public static String lastServerHost() {
		return lastServerHost;
	}

	public static KeyBindingsManager getKeyBindingsManager() {
		if (keyBindingsManager == null) {
			keyBindingsManager = new KeyBindingsManager();
			LOGGER.log(new LogMessage("[CLIENT] KeyBindingsManager initialisé pour cette instance", LogLevel.INFO));
		}
		return keyBindingsManager;
	}

	public static GamepadInputManager getGamepadInputManager() {
		return gamepadInputManager;
	}
    
	public static boolean connectToServer(String host) {
		try {
			if (isConnected())
				NETWORK_MANAGER.close();

			serverAssignedClientId = null;
			connectedServerHost = null;

			NETWORK_MANAGER.connect(host, SERVER_PORT);
			NETWORK_MANAGER.addObserver(WELCOME_OBSERVER);
			connectedServerHost = host;
			lastServerHost = host;

			LOGGER.log(new LogMessage("[CLIENT] Connecté au serveur : " + host, LogLevel.INFO));
			return true;
		} catch (ConnectException e) {
			serverAssignedClientId = null;
			connectedServerHost = null;
			LOGGER.log(new LogMessage("[CLIENT] Serveur injoignable sur " + host + ":" + SERVER_PORT + ".", LogLevel.WARNING));
			return false;
		} catch (Exception e) {
			serverAssignedClientId = null;
			connectedServerHost = null;
			LOGGER.log(
					new LogMessage("[CLIENT] Erreur inattendue lors de la connexion à " + host + "\n" + e.getMessage(),
							LogLevel.ERROR));
			return false;
		}
	}

	@Override
	public void start(Stage stage) throws IOException {
        keyBindingsManager = new KeyBindingsManager();
        App.LOGGER.log(new LogMessage("[CLIENT] Instance unique de KeyBindingsManager créée", LogLevel.INFO));

		scene = new Scene(loadFXML("Intro"), 960, 640);
		URL cssUrl = getClass().getResource("/fr/iutgon/sae401/css/styleMainMenu.css");
		if (cssUrl != null) {
			scene.getStylesheets().add(cssUrl.toExternalForm());
		}
		stage.setScene(scene);
		stage.setTitle("Bomberman");
		stage.show();

		LOGGER.log(new LogMessage("[CLIENT] Application démarrée (En attente de connexion).", LogLevel.INFO));
	}

	@Override
	public void stop() {
		disconnect();
		LOGGER.log(new LogMessage("[CLIENT] Client déconnecté.", LogLevel.INFO));
        try {
            GlobalKeyCapture.getInstance().stop();
        } catch (Exception e) {
            App.LOGGER.log(new LogMessage("[CLIENT] Erreur lors de l'arrêt de la capture globale: " + e.getMessage(), LogLevel.WARNING));
        }
		// If we started an embedded local server, stop it.
		try {
			LocalServerManager.exitLocalMode();
		} catch (Exception ignored) {
		}
	}
}
