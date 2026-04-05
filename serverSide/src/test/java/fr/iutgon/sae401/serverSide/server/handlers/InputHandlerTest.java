package fr.iutgon.sae401.serverSide.server.handlers;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.game.GameEngine;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import fr.iutgon.sae401.serverSide.server.handlers.game.InputHandler;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InputHandlerTest {
    @Test
    void forwardsPayloadToEngineAndReturnsEmptyResponse() {
        var captured = new Holder();
        GameEngine engine = new GameEngine() {
            @Override
            public void tick(double dtSeconds) {
                // not used
            }

            @Override
            public void onInput(ClientId clientId, Json payload) {
                captured.clientId = clientId;
                captured.payload = payload;
            }
        };

        var handler = new InputHandler(engine);
        ClientContext client = new ClientContext() {
            @Override
            public ClientId clientId() {
                return new ClientId("c1");
            }

            @Override
            public SocketAddress remoteAddress() {
                return new InetSocketAddress("127.0.0.1", 1);
            }

            @Override
            public void send(MessageEnvelope message) {
                throw new UnsupportedOperationException();
            }
        };

        Optional<MessageEnvelope> resp = handler.handle(MessageEnvelope.of("input", Json.object(java.util.Map.of("dx", Json.of(1)))), client);
        assertTrue(resp.isEmpty());
        assertNotNull(captured.clientId);
        assertEquals("c1", captured.clientId.value());
        assertEquals(1, captured.payload.at("dx").getInt());
    }

    private static final class Holder {
        ClientId clientId;
        Json payload;
    }
}
