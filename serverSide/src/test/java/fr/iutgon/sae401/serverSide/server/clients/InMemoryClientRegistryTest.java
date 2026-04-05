package fr.iutgon.sae401.serverSide.server.clients;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryClientRegistryTest {
    @Test
    void registerAndLookupById() {
        var registry = new InMemoryClientRegistry();
        var clientId = new ClientId("c1");
        ClientContext client = new FakeClient(clientId);

        registry.register(client);
        assertTrue(registry.get(clientId).isPresent());
        assertEquals(1, registry.ids().size());

        registry.unregister(clientId);
        assertTrue(registry.get(clientId).isEmpty());
        assertEquals(0, registry.ids().size());
    }

    @Test
    void broadcastSendsToAllClients() {
        var registry = new InMemoryClientRegistry();
        var c1 = new FakeClient(new ClientId("c1"));
        var c2 = new FakeClient(new ClientId("c2"));
        registry.register(c1);
        registry.register(c2);

        int ok = registry.broadcast(MessageEnvelope.of("echo", null));
        assertEquals(2, ok);
        assertEquals(1, c1.sentCount.get());
        assertEquals(1, c2.sentCount.get());
    }

    @Test
    void sendRemovesClientOnIOException() {
        var registry = new InMemoryClientRegistry();
        var id = new ClientId("c1");
        var client = new FakeClient(id);
        client.failSends = true;
        registry.register(client);

        boolean ok = registry.send(id, MessageEnvelope.of("x", null));
        assertFalse(ok);
        assertTrue(registry.get(id).isEmpty());
    }

    private static final class FakeClient implements ClientContext {
        private final ClientId id;
        private final SocketAddress addr = new InetSocketAddress("127.0.0.1", 0);
        private final AtomicInteger sentCount = new AtomicInteger(0);
        private volatile boolean failSends;

        private FakeClient(ClientId id) {
            this.id = id;
        }

        @Override
        public ClientId clientId() {
            return id;
        }

        @Override
        public SocketAddress remoteAddress() {
            return addr;
        }

        @Override
        public void send(MessageEnvelope message) throws IOException {
            if (failSends) {
                throw new IOException("boom");
            }
            sentCount.incrementAndGet();
        }
    }
}
