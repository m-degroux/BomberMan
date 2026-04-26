package fr.iutgon.sae401.serverSide.server.runtime;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.game.ServerGameConfig;
import fr.iutgon.sae401.serverSide.game.modules.LobbyRoomsModule;
import fr.iutgon.sae401.serverSide.server.tcp.ServerConfig;
import fr.iutgon.sae401.serverSide.testsupport.TestClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GameInitMapConsistencyTest {
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
	void twoClientsReceiveSameMapInGameInit() throws Exception {
		int port = findFreePort();
		GameServer server = GameServer.create(
				new ServerConfig(port, 8),
				new ServerGameConfig(60, 30),
				new LobbyRoomsModule("test-server", "0.1")
		);
		server.start();
		try {
			awaitServerUp(port, Duration.ofSeconds(2));

			try (TestClient c1 = new TestClient("127.0.0.1", port);
				 TestClient c2 = new TestClient("127.0.0.1", port)) {

				String createId = UUID.randomUUID().toString();
				Map<String, Json> gameCfg = new LinkedHashMap<>();
				gameCfg.put("width", Json.of(15));
				gameCfg.put("height", Json.of(13));
				gameCfg.put("initialHealth", Json.of(1));
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
						Duration.ofSeconds(2)
				);
				String roomId = created.getPayload().at("roomId").getString();

				String joinId = UUID.randomUUID().toString();
				c2.sendEnvelope(new MessageEnvelope(
						"join_room",
						joinId,
						Json.object(Map.of("roomId", Json.of(roomId)))
				));
				c2.awaitMessage(m -> "room_joined".equals(m.getType()) && joinId.equals(m.getRequestId()), Duration.ofSeconds(2));

				String ready1 = UUID.randomUUID().toString();
				c1.sendEnvelope(new MessageEnvelope("ready", ready1, Json.object(Map.of("ready", Json.of(true)))));
				c1.awaitMessage(m -> "ready".equals(m.getType()) && ready1.equals(m.getRequestId()), Duration.ofSeconds(2));

				String ready2 = UUID.randomUUID().toString();
				c2.sendEnvelope(new MessageEnvelope("ready", ready2, Json.object(Map.of("ready", Json.of(true)))));
				c2.awaitMessage(m -> "ready".equals(m.getType()) && ready2.equals(m.getRequestId()), Duration.ofSeconds(2));

				MessageEnvelope init1 = c1.awaitMessage(m -> "game_init".equals(m.getType()), Duration.ofSeconds(3));
				MessageEnvelope init2 = c2.awaitMessage(m -> "game_init".equals(m.getType()), Duration.ofSeconds(3));

				String map1 = init1.getPayload().at("state").at("map").getString();
				String map2 = init2.getPayload().at("state").at("map").getString();
				assertFalse(map1 == null || map1.isBlank(), "map in game_init must not be blank");
				assertEquals(map1, map2, "Both clients should receive the exact same map payload in game_init");
			}
		} finally {
			server.stop();
		}
	}

	@Test
	void createRoomRoundsEvenMapDimensionsToNextOddInGameInit() throws Exception {
		int port = findFreePort();
		GameServer server = GameServer.create(
				new ServerConfig(port, 8),
				new ServerGameConfig(60, 30),
				new LobbyRoomsModule("test-server", "0.1")
		);
		server.start();
		try {
			awaitServerUp(port, Duration.ofSeconds(2));

			try (TestClient c1 = new TestClient("127.0.0.1", port);
				 TestClient c2 = new TestClient("127.0.0.1", port)) {

				String createId = UUID.randomUUID().toString();
				Map<String, Json> gameCfg = new LinkedHashMap<>();
				gameCfg.put("width", Json.of(14));
				gameCfg.put("height", Json.of(12));
				gameCfg.put("initialHealth", Json.of(1));
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
						Duration.ofSeconds(2)
				);
				String roomId = created.getPayload().at("roomId").getString();

				String joinId = UUID.randomUUID().toString();
				c2.sendEnvelope(new MessageEnvelope(
						"join_room",
						joinId,
						Json.object(Map.of("roomId", Json.of(roomId)))
				));
				c2.awaitMessage(m -> "room_joined".equals(m.getType()) && joinId.equals(m.getRequestId()), Duration.ofSeconds(2));

				String ready1 = UUID.randomUUID().toString();
				c1.sendEnvelope(new MessageEnvelope("ready", ready1, Json.object(Map.of("ready", Json.of(true)))));
				c1.awaitMessage(m -> "ready".equals(m.getType()) && ready1.equals(m.getRequestId()), Duration.ofSeconds(2));

				String ready2 = UUID.randomUUID().toString();
				c2.sendEnvelope(new MessageEnvelope("ready", ready2, Json.object(Map.of("ready", Json.of(true)))));
				c2.awaitMessage(m -> "ready".equals(m.getType()) && ready2.equals(m.getRequestId()), Duration.ofSeconds(2));

				MessageEnvelope init = c1.awaitMessage(m -> "game_init".equals(m.getType()), Duration.ofSeconds(3));
				Json state = init.getPayload().at("state");
				assertEquals(15, state.value("width", -1));
				assertEquals(13, state.value("height", -1));
			}
		} finally {
			server.stop();
		}
	}
}
