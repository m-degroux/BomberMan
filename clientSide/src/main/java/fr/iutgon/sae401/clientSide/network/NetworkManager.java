package fr.iutgon.sae401.clientSide.network;

import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.common.protocol.MessageEnvelopeBinary;
import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class NetworkManager implements AutoCloseable {
	private static final Logger LOGGER = Logger.getLogger();
    private static final int MAX_UDP_PACKET_SIZE = 65_507;

	private static NetworkManager manager;
	private Socket socket;
	private DataOutputStream out;
	private DataInputStream in;
    private DatagramSocket udpSocket;
    private String serverHost;
	private List<NetworkObserver> observers;
	private boolean running = true;
    private boolean udpRunning = false;

	public boolean isClosed() {
		return socket == null || socket.isClosed() || !running;
	}

	private NetworkManager() throws IOException {
		this.observers = new LinkedList<>();
	}

	private void listen() {
		try {
			while (running && !socket.isClosed()) {
				int length = in.readInt();
				if (length <= 0 || length > 10_000_000) {
					throw new IOException("Invalid frame length: " + length);
				}
				byte[] bytes = new byte[length];
				in.readFully(bytes);
				MessageEnvelope msg = MessageEnvelopeBinary.fromBytes(bytes);
                handleInboundMessage(msg);
			}
		} catch (EOFException ignored) {
			// Normal when server closes the socket.
		} catch (IOException e) {
			if (running) {
				LOGGER.log(new LogMessage("[NETWORK] Erreur de lecture socket: " + e.getMessage(), LogLevel.ERROR));
			}
		} finally {
			running = false;
			try {
				socket.close();
			} catch (IOException ignored) {
			}
		}
	}

    private void listenUdp() {
        byte[] buffer = new byte[MAX_UDP_PACKET_SIZE];
        try {
            while (udpRunning && udpSocket != null && !udpSocket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                byte[] bytes = java.util.Arrays.copyOf(packet.getData(), packet.getLength());
                MessageEnvelope msg = MessageEnvelopeBinary.fromBytes(bytes);
                handleInboundMessage(msg);
            }
        } catch (IOException e) {
            if (udpRunning) {
                LOGGER.log(new LogMessage("[NETWORK] Erreur de lecture UDP: " + e.getMessage(), LogLevel.WARNING));
            }
        }
    }

    private void handleInboundMessage(MessageEnvelope msg) {
        if (msg == null) {
            return;
        }
        bootstrapUdpIfWelcome(msg);
        Platform.runLater(() -> notifyObservers(msg));
    }

    private void bootstrapUdpIfWelcome(MessageEnvelope msg) {
        if (!"welcome".equals(msg.getType())) {
            return;
        }
        var payload = msg.getPayload();
        if (payload == null) {
            return;
        }
        String udpToken = payload.value("udpToken", "");
        int udpPort = payload.value("udpPort", -1);
        if (udpToken.isBlank() || udpPort <= 0 || udpSocket == null || udpSocket.isClosed()) {
            return;
        }
        try {
            byte[] bytes = MessageEnvelopeBinary.toBytes(MessageEnvelope.of(
                    "udp_hello",
                    fr.iutgon.sae401.common.json.Json.object(java.util.Map.of(
                            "udpToken", fr.iutgon.sae401.common.json.Json.of(udpToken)
                    ))
            ));
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            packet.setSocketAddress(new InetSocketAddress(serverHost, udpPort));
            udpSocket.send(packet);
        } catch (IOException e) {
            LOGGER.log(new LogMessage("[NETWORK] Erreur de handshake UDP: " + e.getMessage(), LogLevel.WARNING));
        }
    }

	public SocketAddress remoteAddress() {
		return socket.getRemoteSocketAddress();
	}

	public void send(MessageEnvelope message) {
		try {
			if (socket.isClosed()) {
				throw new IOException("Socket closed");
			}
			byte[] bytes = MessageEnvelopeBinary.toBytes(message);
			synchronized (out) {
				out.writeInt(bytes.length);
				out.write(bytes);
				out.flush();
			}
		} catch (IOException e) {
			LOGGER.log(new LogMessage("[NETWORK] Erreur d'envoi socket: " + e.getMessage(), LogLevel.ERROR));
			close();
		}
	}

	@Override
	public void close() {
		running = false;
        udpRunning = false;
		try {
            if (socket != null) {
			    socket.close();
            }
			LOGGER.log(new LogMessage("[NETWORK] Socket client fermé.", LogLevel.INFO));
		} catch (IOException e) {
			LOGGER.log(new LogMessage("[NETWORK] Erreur de fermeture de socket: " + e.getMessage(), LogLevel.ERROR));
		}
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
	}

	public void addObserver(NetworkObserver obs) {
		if (obs == null) {
			return;
		}
		if (observers.contains(obs)) {
			return;
		}
		observers.add(obs);
	}
	
	public void removeObserver(NetworkObserver obs) {
		observers.remove(obs);
	}

	public void notifyObservers(MessageEnvelope msg) {
		LOGGER.log(new LogMessage("[NETWORK] Message reçu du serveur: " + msg.getType(), LogLevel.INFO));
		observers.forEach(obs -> obs.onMessage(msg));
	}

	public static synchronized NetworkManager getManager() {
		if (manager == null)
			try {
				manager = new NetworkManager();
			} catch (IOException e) {
				LOGGER.log(new LogMessage("[NETWORK] Erreur de réseau: " + e.getMessage(), LogLevel.ERROR));
			}
		return manager;
	}

	public void connect(String host, int port) throws IOException {
		if (!isClosed())
			return;

        this.serverHost = Objects.requireNonNull(host, "host");
		this.socket = new Socket(host, port);
		this.out = new DataOutputStream(socket.getOutputStream());
		this.in = new DataInputStream(socket.getInputStream());
        this.udpSocket = new DatagramSocket();

		Thread t = new Thread(this::listen);
		t.setDaemon(true);
		running = true;
		t.start();

        Thread udpThread = new Thread(this::listenUdp);
        udpThread.setDaemon(true);
        udpRunning = true;
        udpThread.start();
	}
}
