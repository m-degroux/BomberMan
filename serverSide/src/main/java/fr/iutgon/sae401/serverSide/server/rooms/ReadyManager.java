package fr.iutgon.sae401.serverSide.server.rooms;

import fr.iutgon.sae401.serverSide.server.clients.ClientId;

/**
 * @brief Thread-safe readiness store for lobby management.
 * <p>
 * This is intentionally minimal: readiness is tracked per client.
 * Higher-level code may interpret it per-room by combining with {@link RoomManager}.
 */
public interface ReadyManager {
    void setReady(ClientId clientId, boolean ready);

    boolean isReady(ClientId clientId);

    /**
     * @brief Remove readiness info for a client (e.g. on leave/disconnect).
     */
    void clear(ClientId clientId);
}
