package fr.iutgon.sae401.serverSide.server;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MessageRouterTest {

    @Test
    void registerDuplicateHandler_throws() {
        MessageHandler handlerA = new TestHandler("hello");
        MessageHandler handlerB = new TestHandler("hello");
        MessageRouter router = new MessageRouter(java.util.List.of(handlerA));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> router.register(handlerB));
        assertTrue(error.getMessage().contains("Handler already registered"));
    }

    @Test
    void route_returnsEmptyWhenTypeNotFound() {
        MessageRouter router = new MessageRouter(java.util.List.of());
        MessageEnvelope request = new MessageEnvelope("missing", "req-1", null);
        Optional<MessageEnvelope> result = router.route(request, new DummyClientContext());

        assertTrue(result.isEmpty());
    }

    @Test
    void route_dispatchesToRegisteredHandler() {
        MessageHandler handler = new TestHandler("echo");
        MessageRouter router = new MessageRouter(java.util.List.of(handler));
        MessageEnvelope request = new MessageEnvelope("echo", "req-2", null);

        Optional<MessageEnvelope> result = router.route(request, new DummyClientContext());

        assertTrue(result.isPresent());
        assertEquals("echo", result.get().getType());
        assertEquals("req-2", result.get().getRequestId());
    }

    private static final class TestHandler implements MessageHandler {
        private final String type;

        private TestHandler(String type) {
            this.type = type;
        }

        @Override
        public String messageType() {
            return type;
        }

        @Override
        public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
            return Optional.of(new MessageEnvelope(type, request.getRequestId(), request.getPayload()));
        }
    }

    private static final class DummyClientContext implements ClientContext {
        @Override
        public SocketAddress remoteAddress() {
            return new InetSocketAddress(0);
        }

        @Override
        public void send(MessageEnvelope message) {
            // no-op
        }
    }
}
