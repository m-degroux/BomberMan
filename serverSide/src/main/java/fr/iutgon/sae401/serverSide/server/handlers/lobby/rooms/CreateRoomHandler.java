package fr.iutgon.sae401.serverSide.server.handlers.lobby.rooms;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.gameplay.GameConfig;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.LobbyServices;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.game.StartGameHandler;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;
import fr.iutgon.sae401.serverSide.server.rooms.RoomIdNaming;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Create a new room (match instance) and join it.
 * <p>
 * Request:  type = "create_room" payload = { "roomId": string|null } (optional)
 * Response: type = "room_created" payload = { "roomId": string }
 * OR type = "room_create_failed" payload = { "reason": string, "roomId": string|null }
 */
public final class CreateRoomHandler implements MessageHandler {
	private final LobbyServices lobby;

	public CreateRoomHandler(LobbyServices lobby) {
		this.lobby = Objects.requireNonNull(lobby, "lobby");
	}

	private static RoomId generateRoomId() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		return new RoomId("room-" + suffix);
	}

	private static RoomId generateUniqueRoomId(String requestedBaseName, java.util.function.Predicate<RoomId> isTaken) {
		String base = RoomIdNaming.baseNameFrom(requestedBaseName);
		if (base.isBlank()) {
			return generateRoomId();
		}
		RoomId candidate = RoomIdNaming.roomIdFromBaseName(base);
		if (!isTaken.test(candidate)) {
			return candidate;
		}
		String suffix = UUID.randomUUID().toString().substring(0, 4);
		return new RoomId(candidate.value() + "-" + suffix);
	}

	private static int normalizeToNextOdd(int value) {
		return (value & 1) == 0 ? value + 1 : value;
	}

	@Override
	public String messageType() {
		return "create_room";
	}

	@Override
	public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
		var engine = lobby.engine();
		var ready = lobby.ready().orElseThrow(() -> new IllegalStateException("ready manager is required"));

		Json payload = request.getPayload() == null ? Json.nullValue() : request.getPayload();
		String requested = payload.isObject() ? payload.value("roomId", (String) null) : null;

		RoomId roomId;
		try {
			if (requested == null || requested.isBlank()) {
				roomId = generateRoomId();
			} else {
				roomId = generateUniqueRoomId(requested, rid -> !engine.rooms().members(rid).isEmpty());
			}
		} catch (IllegalArgumentException e) {
			Json fail = Json.object(java.util.Map.of(
					"reason", Json.of("invalid_room_id"),
					"roomId", requested == null ? Json.nullValue() : Json.of(requested)
			));
			return Optional.of(new MessageEnvelope("room_create_failed", request.getRequestId(), fail));
		}

		// If the room already has members, consider it taken.
		if (!engine.rooms().members(roomId).isEmpty()) {
			Json fail = Json.object(java.util.Map.of(
					"reason", Json.of("room_already_exists"),
					"roomId", Json.of(roomId.value())
			));
			return Optional.of(new MessageEnvelope("room_create_failed", request.getRequestId(), fail));
		}

		// Extract gameplay config from payload if present
		GameConfig gameplayConfig = null;
		if (payload.isObject()) {
			Json cfg = payload.at("gameConfig");
			int bombTimer = 2000;
			int bombCooldownMs = cfg.value("bombCooldownMs", 2000);
			int width = normalizeToNextOdd(cfg.value("width", 15));
			int height = normalizeToNextOdd(cfg.value("height", 13));
			gameplayConfig = new GameConfig(
					width,
					height,
					cfg.value("initialHealth", 1),
					cfg.value("maxBombs", 1),
					cfg.value("bombRange", 2),
					bombTimer,
					bombCooldownMs,
					cfg.value("destructibleDensity", 60)
			);
		}
		if (gameplayConfig != null) {
			engine.setGameplayConfig(roomId, gameplayConfig);
		}

		// Map theme per room (optional)
		MapTheme theme = MapTheme.CLASSIC;
		if (payload.isObject()) {
			String themeRaw = payload.value("mapTheme", MapTheme.CLASSIC.name());
			try {
				theme = MapTheme.valueOf(themeRaw.trim().toUpperCase());
			} catch (Exception ignored) {
				theme = MapTheme.CLASSIC;
			}
		}
		engine.setMapTheme(roomId, theme);

		// Extract and store password if provided (optional)
		String password = "";
		if (payload.isObject()) {
			String rawPassword = payload.value("password", "");
			password = rawPassword == null ? "" : rawPassword.trim();
		}
		engine.setRoomPassword(roomId, password);

		ready.clear(client.clientId());
		Optional<RoomId> previous = engine.rooms().roomOf(client.clientId());
		previous.ifPresent(StartGameHandler::cancelAutoStart);
		StartGameHandler.cancelAutoStart(roomId);
		engine.joinRoom(roomId, client.clientId());
		lobby.broadcastRooms();
		lobby.broadcastLobbyStateMove(previous, roomId);

		Json ok = Json.object(java.util.Map.of("roomId", Json.of(roomId.value())));
		return Optional.of(new MessageEnvelope("room_created", request.getRequestId(), ok));
	}
}
