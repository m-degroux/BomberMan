package fr.iutgon.sae401.serverSide.server.handlers.system;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EchoHandlerTest {

    @Test
    void handle_returnsEchoResponseWithSamePayload() {
        EchoHandler handler = new EchoHandler();
        Json payload = Json.object(java.util.Map.of("value", Json.of(123)));
        MessageEnvelope request = new MessageEnvelope("echo", "r2", payload);

        Optional<MessageEnvelope> response = handler.handle(request, new DummyClientContext());

        assertTrue(response.isPresent());
        assertEquals("echo", response.get().getType());
        assertEquals(payload, response.get().getPayload());
    }

    private static final class DummyClientContext implements ClientContext {
        @Override
        public SocketAddress remoteAddress() {
            return new InetSocketAddress(0);
        }

        @Override
        public void send(MessageEnvelope message) throws IOException {
            // no-op
        }
    }
}
