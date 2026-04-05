package fr.iutgon.sae401.serverSide.input;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.serverSide.game.input.InputEvent;
import fr.iutgon.sae401.serverSide.game.input.JsonInputQueue;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonInputQueueTest {
    @Test
    void drainAllReturnsFifoAndEmptiesQueue() {
        var q = new JsonInputQueue();
        var c1 = new ClientId("c1");
        var c2 = new ClientId("c2");

        q.enqueue(c1, Json.of(1));
        q.enqueue(c2, Json.of(2));
        q.enqueue(c1, Json.of(3));

        List<InputEvent> batch = q.drainAll();
        assertEquals(3, batch.size());
        assertEquals("c1", batch.get(0).clientId().value());
        assertEquals(1, batch.get(0).payload().getInt());
        assertEquals("c2", batch.get(1).clientId().value());
        assertEquals(2, batch.get(1).payload().getInt());
        assertEquals("c1", batch.get(2).clientId().value());
        assertEquals(3, batch.get(2).payload().getInt());

        assertTrue(q.drainAll().isEmpty());
    }
}
