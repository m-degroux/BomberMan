package fr.iutgon.sae401.serverSide.server.rooms;

import fr.iutgon.sae401.serverSide.server.clients.ClientId;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @brief In-memory {@link RoomManager} implementation.
 */
public final class InMemoryRoomManager implements RoomManager {
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<RoomId, Set<ClientId>> membersByRoom = new HashMap<>();
    private final Map<ClientId, RoomId> roomByClient = new HashMap<>();

    @Override
    public void join(RoomId roomId, ClientId clientId) {
        Objects.requireNonNull(roomId, "roomId");
        Objects.requireNonNull(clientId, "clientId");
        lock.lock();
        try {
            RoomId previous = roomByClient.get(clientId);
            if (previous != null && previous.equals(roomId)) {
                return;
            }
            if (previous != null) {
                Set<ClientId> prevMembers = membersByRoom.get(previous);
                if (prevMembers != null) {
                    prevMembers.remove(clientId);
                    if (prevMembers.isEmpty()) {
                        membersByRoom.remove(previous);
                    }
                }
            }

            membersByRoom.computeIfAbsent(roomId, k -> new HashSet<>()).add(clientId);
            roomByClient.put(clientId, roomId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void leave(ClientId clientId) {
        Objects.requireNonNull(clientId, "clientId");
        lock.lock();
        try {
            RoomId previous = roomByClient.remove(clientId);
            if (previous == null) {
                return;
            }
            Set<ClientId> prevMembers = membersByRoom.get(previous);
            if (prevMembers != null) {
                prevMembers.remove(clientId);
                if (prevMembers.isEmpty()) {
                    membersByRoom.remove(previous);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<RoomId> roomOf(ClientId clientId) {
        Objects.requireNonNull(clientId, "clientId");
        lock.lock();
        try {
            return Optional.ofNullable(roomByClient.get(clientId));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Set<ClientId> members(RoomId roomId) {
        Objects.requireNonNull(roomId, "roomId");
        lock.lock();
        try {
            Set<ClientId> members = membersByRoom.get(roomId);
            if (members == null) {
                return Set.of();
            }
            return Set.copyOf(members);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Collection<RoomId> rooms() {
        lock.lock();
        try {
            return List.copyOf(membersByRoom.keySet());
        } finally {
            lock.unlock();
        }
    }
}
