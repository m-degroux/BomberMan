package fr.iutgon.sae401.serverSide.server.runtime;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.common.protocol.MessageEnvelopeBinary;
import fr.iutgon.sae401.serverSide.game.ServerGameConfig;
import fr.iutgon.sae401.serverSide.game.modules.LobbyRoomsModule;
import fr.iutgon.sae401.serverSide.server.tcp.ServerConfig;
import fr.iutgon.sae401.serverSide.testsupport.TestClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UdpGamePositionsIntegrationTest {
    private static int findFreeTcpPort() throws IOException {
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        }
    }

    private static int findFreeUdpPort() throws IOException {
        try (DatagramSocket ds = new DatagramSocket(0)) {
            return ds.getLocalPort();
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

    private static void sendUdpEnvelope(DatagramSocket socket, int port, MessageEnvelope message) throws IOException {
        byte[] bytes = MessageEnvelopeBinary.toBytes(message);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        packet.setSocketAddress(new java.net.InetSocketAddress("127.0.0.1", port));
        socket.send(packet);
    }

    private static MessageEnvelope awaitUdpEnvelope(DatagramSocket socket, Duration timeout) throws IOException {
        socket.setSoTimeout((int) Math.max(1L, timeout.toMillis()));
        byte[] buffer = new byte[65_507];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return MessageEnvelopeBinary.fromBytes(java.util.Arrays.copyOf(packet.getData(), packet.getLength()));
    }

    @Test
    void readyMatchBroadcastsFreshPlayerSnapshotsOverUdp() throws Exception {
        int tcpPort = findFreeTcpPort();
        int udpPort = findFreeUdpPort();
        GameServer server = GameServer.create(
                new ServerConfig(tcpPort, udpPort, 8),
                new ServerGameConfig(60, 30),
                new LobbyRoomsModule("test-server", "0.1")
        );
        server.start();
        try {
            awaitServerUp(tcpPort, Duration.ofSeconds(5));

            try (TestClient c1 = new TestClient("127.0.0.1", tcpPort);
                 TestClient c2 = new TestClient("127.0.0.1", tcpPort);
                 DatagramSocket u1 = new DatagramSocket();
                 DatagramSocket u2 = new DatagramSocket()) {

                MessageEnvelope welcome1 = c1.awaitMessage(m -> "welcome".equals(m.getType()), Duration.ofSeconds(5));
                MessageEnvelope welcome2 = c2.awaitMessage(m -> "welcome".equals(m.getType()), Duration.ofSeconds(5));

                sendUdpEnvelope(u1, udpPort, MessageEnvelope.of(
                        "udp_hello",
                        Json.object(Map.of("udpToken", Json.of(welcome1.getPayload().value("udpToken", ""))))
                ));
                sendUdpEnvelope(u2, udpPort, MessageEnvelope.of(
                        "udp_hello",
                        Json.object(Map.of("udpToken", Json.of(welcome2.getPayload().value("udpToken", ""))))
                ));

                assertEquals("udp_ready", awaitUdpEnvelope(u1, Duration.ofSeconds(5)).getType());
                assertEquals("udp_ready", awaitUdpEnvelope(u2, Duration.ofSeconds(5)).getType());

                String createId = UUID.randomUUID().toString();
                Map<String, Json> gameCfg = new LinkedHashMap<>();
                gameCfg.put("width", Json.of(15));
                gameCfg.put("height", Json.of(13));
                gameCfg.put("initialHealth", Json.of(3));
                gameCfg.put("maxBombs", Json.of(1));
                gameCfg.put("bombRange", Json.of(2));
                gameCfg.put("bombCooldownMs", Json.of(300));
                gameCfg.put("destructibleDensity", Json.of(60));

                Json createPayload = Json.object(Map.of(
                        "roomId", Json.nullValue(),
                        "gameConfig", Json.object(gameCfg)
                ));
                c1.sendEnvelope(new MessageEnvelope("create_room", createId, createPayload));
                MessageEnvelope created = c1.awaitMessage(
                        m -> "room_created".equals(m.getType()) && createId.equals(m.getRequestId()),
                        Duration.ofSeconds(5)
                );
                String roomId = created.getPayload().at("roomId").getString();

                String joinId = UUID.randomUUID().toString();
                c2.sendEnvelope(new MessageEnvelope(
                        "join_room",
                        joinId,
                        Json.object(Map.of("roomId", Json.of(roomId)))
                ));
                c2.awaitMessage(m -> "room_joined".equals(m.getType()) && joinId.equals(m.getRequestId()), Duration.ofSeconds(5));

                String ready1 = UUID.randomUUID().toString();
                c1.sendEnvelope(new MessageEnvelope("ready", ready1, Json.object(Map.of("ready", Json.of(true)))));
                c1.awaitMessage(m -> "ready".equals(m.getType()) && ready1.equals(m.getRequestId()), Duration.ofSeconds(5));

                String ready2 = UUID.randomUUID().toString();
                c2.sendEnvelope(new MessageEnvelope("ready", ready2, Json.object(Map.of("ready", Json.of(true)))));
                c2.awaitMessage(m -> "ready".equals(m.getType()) && ready2.equals(m.getRequestId()), Duration.ofSeconds(5));

                MessageEnvelope init1 = c1.awaitMessage(m -> "game_init".equals(m.getType()), Duration.ofSeconds(8));
                c2.awaitMessage(m -> "game_init".equals(m.getType()), Duration.ofSeconds(8));
                String matchRoomId = init1.getPayload().value("roomId", roomId);

                MessageEnvelope positions1 = waitForPositions(u1, matchRoomId, Duration.ofSeconds(8));
                MessageEnvelope positions2 = waitForPositions(u2, matchRoomId, Duration.ofSeconds(8));
                MessageEnvelope positions1Next = waitForPositions(u1, matchRoomId, Duration.ofSeconds(8));

                assertEquals("game_positions", positions1.getType());
                assertEquals("game_positions", positions2.getType());
                assertTrue(positions1.getPayload().at("players").asArray().size() >= 2);
                assertTrue(positions2.getPayload().at("players").asArray().size() >= 2);
                assertTrue(positions1Next.getPayload().value("seq", -1L) > positions1.getPayload().value("seq", -1L));
            }
        } finally {
            server.stop();
        }
    }

    private static MessageEnvelope waitForPositions(DatagramSocket socket, String roomId, Duration timeout) throws IOException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            try {
                MessageEnvelope message = awaitUdpEnvelope(socket, Duration.between(Instant.now(), deadline));
                if ("game_positions".equals(message.getType()) && roomId.equals(message.getPayload().value("roomId", ""))) {
                    return message;
                }
            } catch (SocketTimeoutException ignored) {
                // retry until deadline
            }
        }
        throw new SocketTimeoutException("Timed out waiting for UDP positions");
    }
}
