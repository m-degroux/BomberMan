package fr.iutgon.sae401.serverSide.server.tcp;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.common.protocol.MessageEnvelopeBinary;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageRouter;
import fr.iutgon.sae401.serverSide.server.NetworkServer;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import fr.iutgon.sae401.serverSide.server.clients.ClientLifecycleListener;
import fr.iutgon.sae401.serverSide.server.clients.ClientRegistry;
import fr.iutgon.sae401.serverSide.server.clients.InMemoryClientRegistry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @brief Minimal length-prefixed TCP server.
 * <p>
 * Frame format: [int32 length][binary envelope bytes].
 */

public final class BlockingTcpServer implements NetworkServer {
	private final ServerConfig config;
	private final MessageRouter router;
	private final ClientRegistry clients;
	private final ClientLifecycleListener lifecycle;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ServerSocket serverSocket;
    private volatile ExecutorService clientPool;
    private volatile Thread acceptThread;

    /**
     * @param config server configuration (port, max clients)
     * @param router router used to dispatch requests to handlers
     * @brief Create a blocking TCP server.
     * <p>
     * The server implements a simple length-prefixed framing:
     * [int32 length][binary envelope bytes].
     */
    public BlockingTcpServer(ServerConfig config, MessageRouter router) {
        this(config, router, new InMemoryClientRegistry(), ClientLifecycleListener.noop());
    }

    /**
     * @param config  server configuration (port, max clients)
     * @param router  router used to dispatch requests to handlers
     * @param clients registry used to keep track of active connections
     * @brief Create a blocking TCP server with an external {@link ClientRegistry}.
     */
    public BlockingTcpServer(ServerConfig config, MessageRouter router, ClientRegistry clients) {
        this(config, router, clients, ClientLifecycleListener.noop());
    }

    /**
     * @brief Create a blocking TCP server with external registry + lifecycle callbacks.
     */
    public BlockingTcpServer(ServerConfig config, MessageRouter router, ClientRegistry clients, ClientLifecycleListener lifecycle) {
        this.config = Objects.requireNonNull(config, "config");
        this.router = Objects.requireNonNull(router, "router");
        this.clients = Objects.requireNonNull(clients, "clients");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    }

    /**
     * @brief Start accepting client connections.
     * <p>
     * Idempotent: calling start() multiple times has no effect after the first call.
     */
    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        clientPool = Executors.newFixedThreadPool(config.maxClients());
        acceptThread = new Thread(this::acceptLoop, "tcp-accept");
        acceptThread.start();
    }

    /**
     * @brief Accept loop: accepts sockets and dispatches them to the client thread pool.
     */
    private void acceptLoop() {
        try (ServerSocket ss = new ServerSocket(config.port())) {
            serverSocket = ss;
            while (running.get()) {
                Socket socket = ss.accept();
                clientPool.submit(() -> handleClient(socket));
            }
        } catch (IOException e) {
            if (running.get()) {
                throw new RuntimeException("Server error", e);
            }
        }
    }

    /**
     * @brief Client loop: reads requests, routes them, and optionally sends a response.
     */
    private void handleClient(Socket socket) {
        try (socket;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            ClientId clientId = ClientId.random();
            ClientContext client = new SocketClientContext(clientId, socket, out);
            clients.register(client);
            lifecycle.onConnected(client);
            try {
                while (running.get() && !socket.isClosed()) {
                    MessageEnvelope request = readMessage(in);
                    Optional<MessageEnvelope> response = router.route(request, client);
                    if (response.isPresent()) {
                        client.send(response.get());
                    }
                }
            } finally {
                lifecycle.onDisconnected(client);
                clients.unregister(clientId);
            }
        } catch (EOFException ignored) {
            // Client disconnected.
        } catch (IOException e) {
            // Keep server alive; connection errors are expected.
        }
    }

    /**
     * @param in data input stream
     * @return parsed envelope
     * @throws IOException if the frame is invalid or the stream errors
     * @brief Read a single framed request message.
     */
    private MessageEnvelope readMessage(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > 10_000_000) {
            throw new IOException("Invalid frame length: " + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return MessageEnvelopeBinary.fromBytes(bytes);
    }

    /**
     * @brief Stop the server and close active resources.
     * <p>
     * Idempotent: calling stop() multiple times has no effect after the first call.
     */
    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        try {
            ServerSocket ss = serverSocket;
            if (ss != null) {
                ss.close();
            }
        } catch (IOException ignored) {
        }
        ExecutorService pool = clientPool;
        if (pool != null) {
            pool.shutdownNow();
        }
    }
}
