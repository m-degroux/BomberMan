package fr.iutgon.sae401.serverSide.server.handlers.lobby.game;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.gameplay.GameConfig;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.game.GameEngine;
import fr.iutgon.sae401.serverSide.game.ServerGameConfig;
import fr.iutgon.sae401.serverSide.game.rooms.RoomPerThreadEngine;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import fr.iutgon.sae401.serverSide.server.clients.InMemoryClientRegistry;
import fr.iutgon.sae401.serverSide.server.clients.InMemoryNicknameRegistry;
import fr.iutgon.sae401.serverSide.server.clients.InMemorySkinRegistry;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.LobbyServices;
import fr.iutgon.sae401.serverSide.server.rooms.InMemoryReadyManager;
import fr.iutgon.sae401.serverSide.server.rooms.InMemoryRoomManager;
import fr.iutgon.sae401.serverSide.server.rooms.ReadyManager;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StartGameHandlerTest {
	@Test
	void messageType_isStartGame() {
		assertEquals("start_game", fixture().handler.messageType());
	}

	@Test
	void handle_failsWhenClientIsNotInAnyRoom() {
		Fixture fixture = fixture();
		TestClientContext c1 = fixture.registerClient("c1");

		Optional<MessageEnvelope> resp = fixture.handler.handle(startRequest("r-not-in-room", Json.emptyObject()), c1);
		assertStartFailedReason(resp, "not_in_room");
	}

	@Test
	void handle_failsWhenStartRequestedFromLobby() {
		Fixture fixture = fixture();
		TestClientContext c1 = fixture.registerClient("c1");
		fixture.engine.joinRoom(fixture.lobbyRoom, c1.clientId());

		Optional<MessageEnvelope> resp = fixture.handler.handle(startRequest("r-lobby", Json.emptyObject()), c1);
		assertStartFailedReason(resp, "cannot_start_from_lobby");
	}

	@Test
	void handle_failsWhenAlreadyInMatchRoom() {
		Fixture fixture = fixture();
		TestClientContext c1 = fixture.registerClient("c1");
		fixture.engine.joinRoom(new RoomId("match-room-1"), c1.clientId());

		Optional<MessageEnvelope> resp = fixture.handler.handle(startRequest("r-match", Json.emptyObject()), c1);
		assertStartFailedReason(resp, "already_in_match");
	}

	@Test
	void handle_failsWhenNotEnoughPlayersForMulti() {
		Fixture fixture = fixture();
		TestClientContext c1 = fixture.registerClient("c1");
		RoomId waitingRoom = new RoomId("room-one");
		fixture.engine.joinRoom(waitingRoom, c1.clientId());
		fixture.ready.setReady(c1.clientId(), true);

		Optional<MessageEnvelope> resp = fixture.handler.handle(startRequest("r-not-enough", Json.emptyObject()), c1);
		assertStartFailedReason(resp, "not_enough_players");
		assertEquals(2, resp.orElseThrow().getPayload().value("minPlayers", -1));
	}

	@Test
	void handle_failsWhenNotAllPlayersAreReady() {
		Fixture fixture = fixture();
		TestClientContext c1 = fixture.registerClient("c1");
		TestClientContext c2 = fixture.registerClient("c2");
		RoomId waitingRoom = new RoomId("room-ready");
		fixture.engine.joinRoom(waitingRoom, c1.clientId());
		fixture.engine.joinRoom(waitingRoom, c2.clientId());
		fixture.ready.setReady(c1.clientId(), true);
		fixture.ready.setReady(c2.clientId(), false);

		Optional<MessageEnvelope> resp = fixture.handler.handle(startRequest("r-not-ready", Json.emptyObject()), c1);
		assertStartFailedReason(resp, "not_all_ready");
	}

	@Test
	void handle_failsWhenRoomHasTooManyPlayers() {
		Fixture fixture = fixture();
		RoomId waitingRoom = new RoomId("room-too-many");
		List<TestClientContext> contexts = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			TestClientContext ctx = fixture.registerClient("c" + i);
			contexts.add(ctx);
			fixture.engine.joinRoom(waitingRoom, ctx.clientId());
			fixture.ready.setReady(ctx.clientId(), true);
		}

		Optional<MessageEnvelope> resp = fixture.handler.handle(startRequest("r-too-many", Json.emptyObject()), contexts.getFirst());
		assertStartFailedReason(resp, "too_many_players");
		assertEquals(4, resp.orElseThrow().getPayload().value("maxPlayers", -1));
	}

	@Test
	void handle_failsWhenMatchForSameBaseIsInProgress() {
		Fixture fixture = fixture();
		TestClientContext c1 = fixture.registerClient("c1");
		TestClientContext c2 = fixture.registerClient("c2");
		TestClientContext c3 = fixture.registerClient("c3");
		RoomId waitingRoom = new RoomId("room-old");
		fixture.engine.joinRoom(waitingRoom, c1.clientId());
		fixture.engine.joinRoom(waitingRoom, c2.clientId());
		fixture.ready.setReady(c1.clientId(), true);
		fixture.ready.setReady(c2.clientId(), true);
		fixture.engine.joinRoom(new RoomId("match-old"), c3.clientId());

		Optional<MessageEnvelope> resp = fixture.handler.handle(startRequest("r-match-progress", Json.emptyObject()), c1);
		assertStartFailedReason(resp, "match_in_progress");
	}

	@Test
	void handle_allowsLocalSoloWhenAllowSoloAndLoopback() {
		Fixture fixture = fixture();
		TestClientContext c1 = fixture.registerClient("c1");
		RoomId waitingRoom = new RoomId("room-local");
		fixture.engine.joinRoom(waitingRoom, c1.clientId());
		fixture.ready.setReady(c1.clientId(), true);

		Optional<MessageEnvelope> resp = fixture.handler.handle(
				startRequest("r-solo", Json.object(Map.of("allowSolo", Json.of(true)))),
				c1
		);
		assertTrue(resp.isEmpty());
		MessageEnvelope started = c1.sent.stream().filter(m -> "game_started".equals(m.getType())).findFirst().orElseThrow();
		assertEquals("room-local", started.getPayload().value("oldRoom", ""));
	}

	@Test
	void handle_doesNotAllowSoloForNonLoopbackClient() {
		Fixture fixture = fixture();
		TestClientContext c1 = fixture.registerClient("c1", new InetSocketAddress("8.8.8.8", 9999));
		RoomId waitingRoom = new RoomId("room-remote");
		fixture.engine.joinRoom(waitingRoom, c1.clientId());
		fixture.ready.setReady(c1.clientId(), true);

		Optional<MessageEnvelope> resp = fixture.handler.handle(
				startRequest("r-remote", Json.object(Map.of("allowSolo", Json.of(true)))),
				c1
		);
		assertStartFailedReason(resp, "not_enough_players");
		assertEquals(2, resp.orElseThrow().getPayload().value("minPlayers", -1));
	}

	@Test
	void handle_usesDefaultConfigAndThemeWhenNoRoomSettingsProvided() {
		Fixture fixture = fixture();
		TestClientContext c1 = fixture.registerClient("c1");
		TestClientContext c2 = fixture.registerClient("c2");
		RoomId waitingRoom = new RoomId("room-defaults");
		fixture.engine.joinRoom(waitingRoom, c1.clientId());
		fixture.engine.joinRoom(waitingRoom, c2.clientId());
		fixture.ready.setReady(c1.clientId(), true);
		fixture.ready.setReady(c2.clientId(), true);

		Optional<MessageEnvelope> resp = fixture.handler.handle(startRequest("r-defaults", Json.emptyObject()), c1);
		assertTrue(resp.isEmpty());

		MessageEnvelope started = c1.sent.stream()
				.filter(m -> "game_started".equals(m.getType()))
				.findFirst()
				.orElseThrow();
		RoomId newRoom = new RoomId(started.getPayload().value("newRoom", ""));
		assertNotNull(fixture.engine.getGameplayConfig(newRoom));
		assertEquals(MapTheme.CLASSIC, fixture.engine.getMapTheme(newRoom));
	}

	@Test
	void propagatesMapThemeToMatchRoom() {
		Fixture fixture = fixture();
		TestClientContext c1 = fixture.registerClient("c1");
		TestClientContext c2 = fixture.registerClient("c2");
		RoomId oldRoom = new RoomId("room-test");
		fixture.engine.joinRoom(oldRoom, c1.clientId());
		fixture.engine.joinRoom(oldRoom, c2.clientId());
		fixture.engine.setGameplayConfig(oldRoom, new GameConfig());
		fixture.engine.setMapTheme(oldRoom, MapTheme.PERFECT);
		fixture.ready.setReady(c1.clientId(), true);
		fixture.ready.setReady(c2.clientId(), true);

		Optional<MessageEnvelope> resp = fixture.handler.handle(startRequest("r1", Json.nullValue()), c1);
		assertTrue(resp.isEmpty());

		MessageEnvelope started = c1.sent.stream()
				.filter(m -> "game_started".equals(m.getType()))
				.findFirst()
				.orElseThrow();
		String matchRoomValue = started.getPayload().value("newRoom", (String) null);
		assertNotNull(matchRoomValue);

		MapTheme matchTheme = fixture.engine.getMapTheme(new RoomId(matchRoomValue));
		assertEquals(MapTheme.PERFECT, matchTheme);
	}

	@Test
	void handle_appliesSelectedSkinsToPlayersInGame() {
		Fixture fixture = fixture();
		TestClientContext c1 = fixture.registerClient("c1");
		TestClientContext c2 = fixture.registerClient("c2");
		RoomId waitingRoom = new RoomId("room-skins");
		fixture.engine.joinRoom(waitingRoom, c1.clientId());
		fixture.engine.joinRoom(waitingRoom, c2.clientId());
		
		// Select skins in lobby
		fixture.skins.setSkin(c1.clientId(), 5);
		fixture.skins.setSkin(c2.clientId(), 10);
		
		// Verify skins are set in the registry before game starts
		assertEquals(5, fixture.skins.getSkin(c1.clientId()), "c1 should have skin 5 in registry");
		assertEquals(10, fixture.skins.getSkin(c2.clientId()), "c2 should have skin 10 in registry");
		
		// Mark ready and start game
		fixture.ready.setReady(c1.clientId(), true);
		fixture.ready.setReady(c2.clientId(), true);

		Optional<MessageEnvelope> resp = fixture.handler.handle(startRequest("r-skins", Json.emptyObject()), c1);
		assertTrue(resp.isEmpty());

		MessageEnvelope started = c1.sent.stream()
				.filter(m -> "game_started".equals(m.getType()))
				.findFirst()
				.orElseThrow();
		String matchRoomValue = started.getPayload().value("newRoom", (String) null);
		assertNotNull(matchRoomValue);
		
		// Verify both clients received game_started
		assertTrue(c1.sent.stream().anyMatch(m -> "game_started".equals(m.getType())), "c1 should receive game_started");
		assertTrue(c2.sent.stream().anyMatch(m -> "game_started".equals(m.getType())), "c2 should receive game_started");
	}

	private static void assertStartFailedReason(Optional<MessageEnvelope> response, String reason) {
		assertTrue(response.isPresent());
		MessageEnvelope envelope = response.orElseThrow();
		assertEquals("start_failed", envelope.getType());
		assertEquals(reason, envelope.getPayload().value("reason", ""));
	}

	private static MessageEnvelope startRequest(String requestId, Json payload) {
		return new MessageEnvelope("start_game", requestId, payload);
	}

	private static Fixture fixture() {
		return new Fixture();
	}

	private static final class Fixture {
		private final InMemoryClientRegistry clients = new InMemoryClientRegistry();
		private final ReadyManager ready = new InMemoryReadyManager();
		private final InMemoryRoomManager rooms = new InMemoryRoomManager();
		private final InMemoryNicknameRegistry nicknames = new InMemoryNicknameRegistry();
		private final InMemorySkinRegistry skins = new InMemorySkinRegistry();
		private final RoomId lobbyRoom = new RoomId("lobby");
		private final RoomPerThreadEngine engine = new RoomPerThreadEngine(
				new ServerGameConfig(10, 10),
				clients,
				rooms,
				List.of(),
				lobbyRoom,
				(roomId) -> (GameEngine) dtSeconds -> {
				},
				skins
		);
		private final StartGameHandler handler = new StartGameHandler(new LobbyServices(engine, ready, clients, nicknames, skins));

		private TestClientContext registerClient(String id) {
			return registerClient(id, new InetSocketAddress("127.0.0.1", 1));
		}

		private TestClientContext registerClient(String id, SocketAddress remoteAddress) {
			TestClientContext ctx = new TestClientContext(new ClientId(id), remoteAddress);
			clients.register(ctx);
			return ctx;
		}
	}

	private static final class TestClientContext implements ClientContext {
		private final ClientId id;
		private final SocketAddress remoteAddress;
		private final List<MessageEnvelope> sent = new ArrayList<>();

		private TestClientContext(ClientId id, SocketAddress remoteAddress) {
			this.id = id;
			this.remoteAddress = remoteAddress;
		}

		@Override
		public ClientId clientId() {
			return id;
		}

		@Override
		public SocketAddress remoteAddress() {
			return remoteAddress;
		}

		@Override
		public void send(MessageEnvelope message) {
			sent.add(message);
		}
	}
}
