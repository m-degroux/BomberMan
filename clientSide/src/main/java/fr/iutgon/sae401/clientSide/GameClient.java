package fr.iutgon.sae401.clientSide;

import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.common.protocol.MessageEnvelopeBinary;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Minimal standalone socket client used by tests and legacy code.
 *
 * This class is intentionally independent from the JavaFX NetworkManager singleton
 * so tests can create isolated connections.
 */
public final class GameClient implements AutoCloseable {
	private static final Logger LOGGER = Logger.getLogger();

	private final String host;
	private final int port;
	private final String clientId;
	private final Consumer<MessageEnvelope> onMessage;

	private volatile boolean running;
	private Socket socket;
	private DataOutputStream out;
	private DataInputStream in;
	private Thread readerThread;

	public GameClient(String host, int port, String clientId, Consumer<MessageEnvelope> onMessage) throws IOException {
		this.host = Objects.requireNonNull(host, "host");
		this.port = port;
		this.clientId = (clientId == null || clientId.isBlank()) ? UUID.randomUUID().toString() : clientId;
		this.onMessage = onMessage == null ? msg -> {
		} : onMessage;
		connect();
	}

	public String clientId() {
		return clientId;
	}

	public boolean isClosed() {
		return socket == null || socket.isClosed() || !running;
	}

	private void connect() throws IOException {
		this.socket = new Socket(host, port);
		this.out = new DataOutputStream(socket.getOutputStream());
		this.in = new DataInputStream(socket.getInputStream());
		this.running = true;
		this.readerThread = new Thread(this::readLoop, "GameClient-reader");
		this.readerThread.setDaemon(true);
		this.readerThread.start();
	}

	private void readLoop() {
		try {
			while (running && socket != null && !socket.isClosed()) {
				int length = in.readInt();
				if (length <= 0 || length > 10_000_000) {
					throw new IOException("Invalid frame length: " + length);
				}
				byte[] bytes = new byte[length];
				in.readFully(bytes);
				MessageEnvelope msg = MessageEnvelopeBinary.fromBytes(bytes);
				onMessage.accept(msg);
			}
		} catch (EOFException ignored) {
			// Normal when server closes the socket.
		} catch (IOException e) {
			if (running) {
				LOGGER.log(new LogMessage("[GAMECLIENT] Erreur de lecture socket: " + e.getMessage(), LogLevel.ERROR));
			}
		} finally {
			running = false;
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (IOException ignored) {
			}
		}
	}

	public void send(MessageEnvelope message) {
		Objects.requireNonNull(message, "message");
		try {
			if (socket == null || socket.isClosed()) {
				throw new IOException("Socket closed");
			}
			byte[] bytes = MessageEnvelopeBinary.toBytes(message);
			synchronized (out) {
				out.writeInt(bytes.length);
				out.write(bytes);
				out.flush();
			}
		} catch (IOException e) {
			LOGGER.log(new LogMessage("[GAMECLIENT] Erreur d'envoi socket: " + e.getMessage(), LogLevel.ERROR));
			close();
		}
	}

	@Override
	public void close() {
		running = false;
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException ignored) {
		}
		try {
			if (readerThread != null && readerThread.isAlive()) {
				readerThread.interrupt();
			}
		} catch (Exception ignored) {
		}
	}
}
