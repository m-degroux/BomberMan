package fr.iutgon.sae401.serverSide.server.clients;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;

import java.util.Collection;
import java.util.Optional;

/**
 * @brief Thread-safe registry of currently connected clients.
 * <p>
 * This is the central place to:
 * - map a {@link ClientId} to a {@link ClientContext}
 * - send messages to a specific client
 * - broadcast messages to all clients
 */
public interface ClientRegistry {
    /**
     * @brief Register a client context.
     * <p>
     * If a client with the same id already exists, implementations should throw.
     */
    void register(ClientContext client);

    /**
     * @brief Unregister a client id.
     */
    void unregister(ClientId id);

    /**
     * @brief Lookup a connected client.
     */
    Optional<ClientContext> get(ClientId id);

    /**
     * @brief @return snapshot of all currently registered client ids.
     */
    Collection<ClientId> ids();

    /**
     * @return true if the message was sent; false if the client is missing or send failed.
     * @brief Send a message to a specific client.
     */
    boolean send(ClientId id, MessageEnvelope message);

    /**
     * @return number of clients the message was successfully sent to.
     * @brief Broadcast a message to all currently connected clients.
     */
    int broadcast(MessageEnvelope message);
}
