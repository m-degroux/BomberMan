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
import fr.iutgon.sae401.serverSide.server.clients.NicknameRegistry;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RestartGameHandlerTest {
	@Test
	void restartsFromMatchRoomAndKeepsParameters() {
		InMemoryClientRegistry clients = new InMemoryClientRegistry();
		ReadyManager ready = new InMemoryReadyManager();
		NicknameRegistry nicknames = new InMemoryNicknameRegistry();
		InMemorySkinRegistry skins = new InMemorySkinRegistry();
		var rooms = new InMemoryRoomManager();

		RoomId lobbyRoom = new RoomId("lobby");
		RoomPerThreadEngine engine = new RoomPerThreadEngine(
				new ServerGameConfig(10, 10),
				clients,
				rooms,
				List.of(),
				lobbyRoom,
				(roomId) -> (GameEngine) dtSeconds -> {
				},
				skins
		);

		ClientId c1 = new ClientId("c1");
		ClientId c2 = new ClientId("c2");
		TestClientContext ctx1 = new TestClientContext(c1);
		TestClientContext ctx2 = new TestClientContext(c2);
		clients.register(ctx1);
		clients.register(ctx2);

		RoomId oldMatch = new RoomId("match-old");
		engine.joinRoom(oldMatch, c1);
		engine.joinRoom(oldMatch, c2);
		engine.setGameplayConfig(oldMatch, new GameConfig(17, 15, 3, 2, 3, 2500, 0.2f));
		engine.setMapTheme(oldMatch, MapTheme.PERFECT);

		RestartGameHandler handler = new RestartGameHandler(new LobbyServices(engine, ready, clients, nicknames, skins));
		Optional<MessageEnvelope> resp = handler.handle(new MessageEnvelope("restart_game", "r1", Json.nullValue()), ctx1);
		assertTrue(resp.isEmpty());

		MessageEnvelope restarted = ctx1.sent.stream().filter(m -> "room_restarted".equals(m.getType())).findFirst().orElseThrow();
		String newRoomValue = restarted.getPayload().value("newRoom", (String) null);
		assertNotNull(newRoomValue);
		assertTrue(newRoomValue.startsWith("room-"));
		assertTrue(newRoomValue.startsWith("room-old"), "Expected restarted room to be based on match name (room-old*), got: " + newRoomValue);
		assertNotEquals(oldMatch.value(), newRoomValue);

		RoomId newRoom = new RoomId(newRoomValue);
		assertEquals(MapTheme.PERFECT, engine.getMapTheme(newRoom));
		assertNotNull(engine.getGameplayConfig(newRoom));
		assertEquals(17, engine.getGameplayConfig(newRoom).getWidth());

		assertEquals(newRoom, engine.rooms().roomOf(c1).orElseThrow());
		assertEquals(newRoom, engine.rooms().roomOf(c2).orElseThrow());
		assertTrue(engine.rooms().members(oldMatch).isEmpty());
		assertEquals(2, engine.rooms().members(newRoom).size());
	}

	private static final class TestClientContext implements ClientContext {
		private final ClientId id;
		private final List<MessageEnvelope> sent = new ArrayList<>();

		private TestClientContext(ClientId id) {
			this.id = id;
		}

		@Override
		public ClientId clientId() {
			return id;
		}

		@Override
		public SocketAddress remoteAddress() {
			return new InetSocketAddress("127.0.0.1", 1);
		}

		@Override
		public void send(MessageEnvelope message) {
			sent.add(message);
		}
	}
}
