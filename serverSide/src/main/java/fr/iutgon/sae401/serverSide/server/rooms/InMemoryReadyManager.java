package fr.iutgon.sae401.serverSide.server.rooms;

import fr.iutgon.sae401.serverSide.server.clients.ClientId;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @brief In-memory {@link ReadyManager} implementation.
 */
public final class InMemoryReadyManager implements ReadyManager {
    private final ConcurrentHashMap<ClientId, Boolean> readyByClient = new ConcurrentHashMap<>();

    @Override
    public void setReady(ClientId clientId, boolean ready) {
        Objects.requireNonNull(clientId, "clientId");
        readyByClient.put(clientId, ready);
    }

    @Override
    public boolean isReady(ClientId clientId) {
        Objects.requireNonNull(clientId, "clientId");
        return Boolean.TRUE.equals(readyByClient.get(clientId));
    }

    @Override
    public void clear(ClientId clientId) {
        Objects.requireNonNull(clientId, "clientId");
        readyByClient.remove(clientId);
    }
}
