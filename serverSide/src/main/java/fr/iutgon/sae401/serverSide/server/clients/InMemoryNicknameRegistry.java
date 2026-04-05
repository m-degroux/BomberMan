package fr.iutgon.sae401.serverSide.server.clients;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory implementation of NicknameRegistry.
 */
public class InMemoryNicknameRegistry implements NicknameRegistry {
    private final Map<ClientId, String> nicknames = new HashMap<>();
    private final AtomicInteger playerCounter = new AtomicInteger(0);

    @Override
    public synchronized void setNickname(ClientId id, String nickname) {
        Objects.requireNonNull(id, "id");
        if (nickname != null && !nickname.isBlank()) {
            nicknames.put(id, nickname);
        }
    }

    @Override
    public synchronized String getNickname(ClientId id) {
        Objects.requireNonNull(id, "id");
        return nicknames.getOrDefault(id, "Joueur " + (playerCounter.get() + 1));
    }

    @Override
    public synchronized void remove(ClientId id) {
        Objects.requireNonNull(id, "id");
        nicknames.remove(id);
    }

    /**
     * Increment the player counter for the next player to get a unique default nickname.
     */
    public void incrementPlayerCounter() {
        playerCounter.incrementAndGet();
    }

    /**
     * Get the current player counter value.
     */
    public int getPlayerCount() {
        return playerCounter.get();
    }
}
