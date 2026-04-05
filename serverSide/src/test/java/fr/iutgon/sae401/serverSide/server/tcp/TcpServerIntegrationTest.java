package fr.iutgon.sae401.serverSide.server.tcp;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.MessageRouter;
import fr.iutgon.sae401.serverSide.server.handlers.system.EchoHandler;
import fr.iutgon.sae401.serverSide.server.handlers.system.HealthHandler;
import fr.iutgon.sae401.serverSide.server.handlers.system.PingHandler;
import fr.iutgon.sae401.serverSide.server.handlers.system.ServerInfoHandler;
import fr.iutgon.sae401.serverSide.testsupport.TestClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TcpServerIntegrationTest {
    private static int findFreePort() throws IOException {
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        }
    }

    private static void awaitServerUp(int port, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            try (Socket ignored = new Socket("127.0.0.1", port)) {
                return;
            } catch (IOException e) {
                Thread.sleep(25);
            }
        }
        throw new AssertionError("Server did not start within " + timeout);
    }

    @Test
    void canPingEchoHealthAndServerInfo() throws Exception {
        int port = findFreePort();
        var router = new MessageRouter(List.of(
                new PingHandler(),
                new EchoHandler(),
                new HealthHandler(),
                new ServerInfoHandler("begin-sae", "0.1")
        ));
        var server = new BlockingTcpServer(new ServerConfig(port, 8), router);
        server.start();
        try {
            awaitServerUp(port, Duration.ofSeconds(2));
            try (TestClient client = new TestClient("127.0.0.1", port)) {
                MessageEnvelope pong = client.request(new MessageEnvelope(
                        "ping",
                        "t1",
                        Json.object(java.util.Map.of("x", Json.of(1)))
                ));
                assertEquals("pong", pong.getType());
                assertEquals("t1", pong.getRequestId());
                assertEquals(1, pong.getPayload().at("echo").at("x").getInt());
                assertTrue(pong.getPayload().at("serverTime").getString().length() > 0);

                MessageEnvelope echo = client.request(new MessageEnvelope(
                        "echo",
                        "t2",
                        Json.object(java.util.Map.of("msg", Json.of("hello")))
                ));
                assertEquals("echo", echo.getType());
                assertEquals("hello", echo.getPayload().at("msg").getString());

                MessageEnvelope health = client.request(new MessageEnvelope(
                        "health",
                        "t3",
                        Json.nullValue()
                ));
                assertEquals("health", health.getType());
                assertEquals(true, health.getPayload().at("ok").getBoolean());

                MessageEnvelope info = client.request(new MessageEnvelope(
                        "server_info",
                        "t4",
                        Json.nullValue()
                ));
                assertEquals("server_info", info.getType());
                assertEquals("begin-sae", info.getPayload().at("serverName").getString());
                assertEquals("0.1", info.getPayload().at("protocolVersion").getString());
            }
        } finally {
            server.stop();
        }
    }
}
