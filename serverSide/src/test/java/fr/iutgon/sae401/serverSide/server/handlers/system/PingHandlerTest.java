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

class PingHandlerTest {

    @Test
    void handle_returnsPongWithEchoedPayloadAndSameRequestId() {
        PingHandler handler = new PingHandler();
        MessageEnvelope request = new MessageEnvelope("ping", "r1", Json.of("hello"));

        Optional<MessageEnvelope> response = handler.handle(request, new DummyClientContext());

        assertTrue(response.isPresent());
        assertEquals("pong", response.get().getType());
        assertEquals("r1", response.get().getRequestId());
        assertEquals("hello", response.get().getPayload().at("echo").getString());
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
