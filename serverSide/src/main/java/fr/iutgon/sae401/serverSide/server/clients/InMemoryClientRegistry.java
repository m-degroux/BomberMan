package fr.iutgon.sae401.serverSide.server.clients;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @brief Default in-memory {@link ClientRegistry} implementation.
 */
public final class InMemoryClientRegistry implements ClientRegistry {
    private final ConcurrentHashMap<ClientId, ClientContext> clientsById = new ConcurrentHashMap<>();

    @Override
    public void register(ClientContext client) {
        Objects.requireNonNull(client, "client");
        ClientId id = Objects.requireNonNull(client.clientId(), "client.clientId()");
        ClientContext previous = clientsById.putIfAbsent(id, client);
        if (previous != null) {
            throw new IllegalArgumentException("Client already registered: " + id.value());
        }
    }

    @Override
    public void unregister(ClientId id) {
        Objects.requireNonNull(id, "id");
        clientsById.remove(id);
    }

    @Override
    public Optional<ClientContext> get(ClientId id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(clientsById.get(id));
    }

    @Override
    public Collection<ClientId> ids() {
        return List.copyOf(clientsById.keySet());
    }

    @Override
    public boolean send(ClientId id, MessageEnvelope message) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(message, "message");
        ClientContext client = clientsById.get(id);
        if (client == null) {
            return false;
        }
        try {
            client.send(message);
            return true;
        } catch (IOException e) {
            clientsById.remove(id);
            return false;
        }
    }

    @Override
    public int broadcast(MessageEnvelope message) {
        Objects.requireNonNull(message, "message");
        int ok = 0;
        for (var entry : clientsById.entrySet()) {
            ClientId id = entry.getKey();
            ClientContext client = entry.getValue();
            try {
                client.send(message);
                ok++;
            } catch (IOException e) {
                clientsById.remove(id);
            }
        }
        return ok;
    }
}
