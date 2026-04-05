package fr.iutgon.sae401.serverSide.game.input;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @brief Thread-safe input queue (network threads -> game loop thread).
 */
public final class JsonInputQueue {
    private final ConcurrentLinkedQueue<InputEvent> queue = new ConcurrentLinkedQueue<>();

    /**
     * @brief Enqueue one input event.
     */
    public void enqueue(ClientId clientId, Json payload) {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(payload, "payload");
        queue.add(new InputEvent(clientId, payload));
    }

    /**
     * @brief Drain all currently queued inputs.
     * <p>
     * The returned list is in FIFO order.
     */
    public List<InputEvent> drainAll() {
        ArrayList<InputEvent> out = new ArrayList<>();
        while (true) {
            InputEvent e = queue.poll();
            if (e == null) {
                return out;
            }
            out.add(e);
        }
    }
}
