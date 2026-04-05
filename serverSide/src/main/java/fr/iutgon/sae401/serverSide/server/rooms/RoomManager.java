package fr.iutgon.sae401.serverSide.server.rooms;

import fr.iutgon.sae401.serverSide.server.clients.ClientId;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * @brief Thread-safe room registry.
 * <p>
 * Minimal implementation for "session" grouping (lobbies, match instances, etc.).
 * <p>
 * Design choice: one room per client.
 */
public interface RoomManager {
    /**
     * @brief Join a room.
     * <p>
     * If the room doesn't exist, it is created.
     * If the client was already in another room, it is moved.
     */
    void join(RoomId roomId, ClientId clientId);

    /**
     * @brief Leave the current room, if any.
     */
    void leave(ClientId clientId);

    /**
     * @brief Called on disconnect to ensure cleanup.
     */
    default void onClientDisconnected(ClientId clientId) {
        leave(clientId);
    }

    /**
     * @brief @return the room a client currently belongs to.
     */
    Optional<RoomId> roomOf(ClientId clientId);

    /**
     * @brief @return a snapshot of room members.
     */
    Set<ClientId> members(RoomId roomId);

    /**
     * @brief @return snapshot of existing rooms.
     */
    Collection<RoomId> rooms();
}
