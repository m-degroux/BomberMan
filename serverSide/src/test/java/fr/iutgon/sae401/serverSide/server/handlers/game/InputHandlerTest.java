package fr.iutgon.sae401.serverSide.server.handlers.game;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.game.GameEngine;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InputHandlerTest {

    @Test
    void handle_delegatesPayloadToEngineAndReturnsEmpty() {
        AtomicReference<ClientId> lastClient = new AtomicReference<>();
        AtomicReference<Json> lastPayload = new AtomicReference<>();
        GameEngine engine = new GameEngine() {
            @Override
            public void tick(double dtSeconds) {
            }

            @Override
            public void onInput(ClientId clientId, Json payload) {
                lastClient.set(clientId);
                lastPayload.set(payload);
            }
        };
        InputHandler handler = new InputHandler(engine);
        MessageEnvelope request = new MessageEnvelope("input", "req-3", Json.object(java.util.Map.of("x", Json.of(1), "y", Json.of(2))));

        Optional<fr.iutgon.sae401.common.protocol.MessageEnvelope> response = handler.handle(request, new DummyClientContext());

        assertTrue(response.isEmpty());
        assertEquals(new ClientId("client-1"), lastClient.get());
        assertEquals(1, lastPayload.get().at("x").getInt());
        assertEquals(2, lastPayload.get().at("y").getInt());
    }

    private static final class DummyClientContext implements ClientContext {
        @Override
        public fr.iutgon.sae401.serverSide.server.clients.ClientId clientId() {
            return new ClientId("client-1");
        }

        @Override
        public SocketAddress remoteAddress() {
            return new InetSocketAddress(0);
        }

        @Override
        public void send(fr.iutgon.sae401.common.protocol.MessageEnvelope message) throws IOException {
            // no-op
        }
    }
}
