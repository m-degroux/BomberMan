package fr.iutgon.sae401.serverSide.server.handlers.lobby.game;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.gameplay.GameConfig;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.LobbyServices;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;
import fr.iutgon.sae401.serverSide.server.rooms.RoomIdNaming;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Handler to start a game if all members are ready.
 * <p>
 * Request:  type = "start_game" payload = optional { "allowSolo": boolean }
 * Response: type = "game_started" payload = { "oldRoom": string, "newRoom": string, "members": [string] }
 * OR type = "start_failed" payload = { "reason": string, "roomId": string|null }
 */
public final class StartGameHandler implements MessageHandler {
	private static final int MULTI_MIN_PLAYERS = 2;
	private static final int LOCAL_SOLO_MIN_PLAYERS = 1;
	private static final int DEFAULT_MAX_PLAYERS = 4;
	private static final int AUTO_START_DELAY_SECONDS = 3;
	private static final ScheduledExecutorService AUTO_START_EXECUTOR =
			Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "lobby-auto-start");
				t.setDaemon(true);
				return t;
			});
	private static final ConcurrentHashMap<String, ScheduledFuture<?>> AUTO_START_TASKS = new ConcurrentHashMap<>();

	private final LobbyServices lobby;

	public StartGameHandler(LobbyServices lobby) {
		this.lobby = Objects.requireNonNull(lobby, "lobby");
	}

	@Override
	public String messageType() {
		return "start_game";
	}

	@Override
	public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
		var engine = lobby.engine();
		var ready = lobby.ready().orElseThrow(() -> new IllegalStateException("ready manager is required"));
		var clients = lobby.clients();
		int requiredMinPlayers = requiredMinPlayersForRequest(request, client);

		Optional<RoomId> roomOpt = engine.rooms().roomOf(client.clientId());
		if (roomOpt.isEmpty()) {
			return Optional.of(new MessageEnvelope("start_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("not_in_room"),
							"roomId", Json.nullValue()
					))));
		}
		RoomId oldRoom = roomOpt.get();
		if (oldRoom.value().equals("lobby") || oldRoom.value().isBlank()) {
			return Optional.of(new MessageEnvelope("start_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("cannot_start_from_lobby"),
							"roomId", Json.of(oldRoom.value())
					))));
		}
		if (oldRoom.value().startsWith("match-")) {
			return Optional.of(new MessageEnvelope("start_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("already_in_match"),
							"roomId", Json.of(oldRoom.value())
					))));
		}

		// Prevent starting a new match while there are still players in an existing match for this arena.
		String base = RoomIdNaming.baseNameFrom(oldRoom.value());
		if (!base.isBlank() && hasPlayersInAnyMatchForBase(engine, base)) {
			return Optional.of(new MessageEnvelope("start_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("match_in_progress"),
							"roomId", Json.of(oldRoom.value())
					))));
		}
		Set<ClientId> members = engine.rooms().members(oldRoom);
		if (members.isEmpty()) {
			return Optional.of(new MessageEnvelope("start_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("empty_room"),
							"roomId", Json.of(oldRoom.value())
					))));
		}
		if (members.size() < requiredMinPlayers) {
			return Optional.of(new MessageEnvelope("start_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("not_enough_players"),
							"roomId", Json.of(oldRoom.value()),
							"minPlayers", Json.of(requiredMinPlayers)
					))));
		}
		if (members.size() > DEFAULT_MAX_PLAYERS) {
			return Optional.of(new MessageEnvelope("start_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("too_many_players"),
							"roomId", Json.of(oldRoom.value()),
							"maxPlayers", Json.of(DEFAULT_MAX_PLAYERS)
					))));
		}
		for (ClientId id : members) {
			if (!ready.isReady(id)) {
				return Optional.of(new MessageEnvelope("start_failed", request.getRequestId(),
						Json.object(Map.of(
								"reason", Json.of("not_all_ready"),
								"roomId", Json.of(oldRoom.value())
						))));
			}
		}
		// Tous prêts, on crée une nouvelle room "match-<nom>" basée sur la room d'attente
		if (base.isBlank()) {
			base = UUID.randomUUID().toString().substring(0, 8);
		}
		RoomId matchRoom = RoomIdNaming.matchIdFromBaseName(base);
		if (!engine.rooms().members(matchRoom).isEmpty()) {
			String suffix = UUID.randomUUID().toString().substring(0, 4);
			matchRoom = new RoomId(matchRoom.value() + "-" + suffix);
		}
		GameConfig cfg = engine.getGameplayConfig(oldRoom);
		engine.setGameplayConfig(matchRoom, cfg != null ? cfg : new GameConfig());
		MapTheme theme = engine.getMapTheme(oldRoom);
		engine.setMapTheme(matchRoom, theme != null ? theme : MapTheme.CLASSIC);
		List<String> memberIds = members.stream().map(ClientId::value).collect(Collectors.toList());
		cancelAutoStart(oldRoom);
		for (ClientId id : members) {
			ready.clear(id);
			engine.joinRoom(matchRoom, id);
		}
		Json payload = Json.object(Map.of(
				"oldRoom", Json.of(oldRoom.value()),
				"newRoom", Json.of(matchRoom.value()),
				"members", Json.array(memberIds.stream().map(Json::of).collect(Collectors.toList()))
		));

		MessageEnvelope started = new MessageEnvelope("game_started", request.getRequestId(), payload);
		for (ClientId id : members) {
			clients.send(id, started);
		}
		lobby.broadcastRooms();
		lobby.broadcastLobbyState(oldRoom);
		lobby.broadcastLobbyState(matchRoom);
		return Optional.empty();
	}

	private static boolean hasPlayersInAnyMatchForBase(fr.iutgon.sae401.serverSide.game.rooms.RoomPerThreadEngine engine, String baseName) {
		String prefix = "match-" + baseName;
		for (RoomId id : engine.rooms().rooms()) {
			String v = id == null ? "" : id.value();
			if (v.startsWith(prefix) && !engine.rooms().members(id).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Auto-start variant triggered on "ready" when everybody is ready.
	 * Returns true if a countdown is running or was started.
	 */
	public static boolean tryAutoStart(LobbyServices lobby, RoomId oldRoom, String requestId) {
		Objects.requireNonNull(lobby, "lobby");
		Objects.requireNonNull(oldRoom, "oldRoom");

		if (!isAutoStartEligible(lobby, oldRoom)) {
			cancelAutoStart(oldRoom);
			return false;
		}

		AUTO_START_TASKS.compute(oldRoom.value(), (roomKey, pending) -> {
			if (pending != null && !pending.isDone()) {
				return pending;
			}
			return AUTO_START_EXECUTOR.schedule(() -> {
				AUTO_START_TASKS.remove(roomKey);
				tryAutoStartNow(lobby, oldRoom, requestId);
			}, AUTO_START_DELAY_SECONDS, TimeUnit.SECONDS);
		});
		return true;
	}

	public static void cancelAutoStart(RoomId roomId) {
		if (roomId == null) {
			return;
		}
		ScheduledFuture<?> pending = AUTO_START_TASKS.remove(roomId.value());
		if (pending != null) {
			pending.cancel(false);
		}
	}

	private static boolean isAutoStartEligible(LobbyServices lobby, RoomId oldRoom) {
		var engine = lobby.engine();
		var ready = lobby.ready().orElseThrow(() -> new IllegalStateException("ready manager is required"));

		if (oldRoom.value().equals("lobby") || oldRoom.value().isBlank() || oldRoom.value().startsWith("match-")) {
			return false;
		}

		String base = RoomIdNaming.baseNameFrom(oldRoom.value());
		if (!base.isBlank() && hasPlayersInAnyMatchForBase(engine, base)) {
			return false;
		}

		Set<ClientId> members = engine.rooms().members(oldRoom);
		if (members.size() < MULTI_MIN_PLAYERS || members.size() > DEFAULT_MAX_PLAYERS) {
			return false;
		}
		for (ClientId id : members) {
			if (!ready.isReady(id)) {
				return false;
			}
		}
		return true;
	}

	private static boolean tryAutoStartNow(LobbyServices lobby, RoomId oldRoom, String requestId) {
		if (!isAutoStartEligible(lobby, oldRoom)) {
			return false;
		}

		var engine = lobby.engine();
		var ready = lobby.ready().orElseThrow(() -> new IllegalStateException("ready manager is required"));
		var clients = lobby.clients();

		String base = RoomIdNaming.baseNameFrom(oldRoom.value());
		if (base.isBlank()) {
			base = UUID.randomUUID().toString().substring(0, 8);
		}
		RoomId matchRoom = RoomIdNaming.matchIdFromBaseName(base);
		if (!engine.rooms().members(matchRoom).isEmpty()) {
			String suffix = UUID.randomUUID().toString().substring(0, 4);
			matchRoom = new RoomId(matchRoom.value() + "-" + suffix);
		}

		GameConfig cfg = engine.getGameplayConfig(oldRoom);
		engine.setGameplayConfig(matchRoom, cfg != null ? cfg : new GameConfig());
		MapTheme theme = engine.getMapTheme(oldRoom);
		engine.setMapTheme(matchRoom, theme != null ? theme : MapTheme.CLASSIC);

		Set<ClientId> members = engine.rooms().members(oldRoom);
		List<String> memberIds = members.stream().map(ClientId::value).collect(Collectors.toList());
		cancelAutoStart(oldRoom);
		for (ClientId id : members) {
			ready.clear(id);
			engine.joinRoom(matchRoom, id);
		}
		Json payload = Json.object(Map.of(
				"oldRoom", Json.of(oldRoom.value()),
				"newRoom", Json.of(matchRoom.value()),
				"members", Json.array(memberIds.stream().map(Json::of).collect(Collectors.toList()))
		));

		MessageEnvelope started = new MessageEnvelope("game_started", requestId, payload);
		for (ClientId id : members) {
			clients.send(id, started);
		}
		lobby.broadcastRooms();
		lobby.broadcastLobbyState(oldRoom);
		lobby.broadcastLobbyState(matchRoom);
		return true;
	}

	private static int requiredMinPlayersForRequest(MessageEnvelope request, ClientContext client) {
		Json payload = request == null ? Json.emptyObject() : request.getPayload();
		if (payload == null || !payload.isObject()) {
			payload = Json.emptyObject();
		}
		boolean allowSolo = payload.value("allowSolo", false);
		if (allowSolo && isLoopbackClient(client)) {
			return LOCAL_SOLO_MIN_PLAYERS;
		}
		return MULTI_MIN_PLAYERS;
	}

	private static boolean isLoopbackClient(ClientContext client) {
		if (client == null) {
			return false;
		}
		SocketAddress remote = client.remoteAddress();
		if (!(remote instanceof InetSocketAddress inet)) {
			return false;
		}
		if (inet.getAddress() != null) {
			return inet.getAddress().isLoopbackAddress() || inet.getAddress().isAnyLocalAddress();
		}
		String host = inet.getHostString();
		return host != null && "localhost".equalsIgnoreCase(host);
	}
}
