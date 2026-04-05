package fr.iutgon.sae401.serverSide.server.handlers.lobby.rooms;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.LobbyServices;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.game.StartGameHandler;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;

import java.util.Objects;
import java.util.Optional;

/**
 * Lobby join room handler.
 * <p>
 * Request:  type = "join_room" payload = { "roomId": string, "password": string (optional) }
 * Response: type = "room_joined" payload = { "roomId": string }
 * OR type = "room_join_failed" payload = { "reason": string, "roomId": string }
 */
public final class JoinRoomHandler implements MessageHandler {
	private static final int DEFAULT_MAX_PLAYERS = 4;

	private final LobbyServices lobby;

	public JoinRoomHandler(LobbyServices lobby) {
		this.lobby = Objects.requireNonNull(lobby, "lobby");
	}

	@Override
	public String messageType() {
		return "join_room";
	}

	@Override
	public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
		var engine = lobby.engine();
		Optional<RoomId> previousRoom = engine.rooms().roomOf(client.clientId());
		Json payload = request.getPayload() == null ? Json.nullValue() : request.getPayload();
		String roomIdRaw = payload.at("roomId").getString();
		RoomId roomId = new RoomId(roomIdRaw);
		// If a client tries to join a match room directly, redirect to the corresponding waiting room.
		// This ensures join-in-progress users stay in the lobby and do not spawn into the running game.
		if (roomId.value().startsWith("match-")) {
			String base = fr.iutgon.sae401.serverSide.server.rooms.RoomIdNaming.baseNameFrom(roomId.value());
			if (!base.isBlank()) {
				roomId = fr.iutgon.sae401.serverSide.server.rooms.RoomIdNaming.roomIdFromBaseName(base);
			}
		}
		final RoomId targetRoom = roomId;

		if (!engine.availableRooms().isEmpty() && !engine.availableRooms().contains(targetRoom)) {
			Json fail = Json.object(java.util.Map.of(
					"reason", Json.of("unknown_room"),
					"roomId", Json.of(targetRoom.value())
			));
			return Optional.of(new MessageEnvelope("room_join_failed", request.getRequestId(), fail));
		}

		// Check password if room has one
		if (engine.hasPassword(roomId)) {
			String providedPassword = payload.value("password", "");
			String correctPassword = engine.getRoomPassword(roomId);
			if (!providedPassword.equals(correctPassword)) {
				Json fail = Json.object(java.util.Map.of(
						"reason", Json.of("invalid_password"),
						"roomId", Json.of(roomId.value())
				));
				return Optional.of(new MessageEnvelope("room_join_failed", request.getRequestId(), fail));
			}
		}

		if (!roomId.equals(engine.defaultRoom())) {
			int currentMembers = engine.rooms().members(roomId).size();
			if (currentMembers >= DEFAULT_MAX_PLAYERS) {
				Json fail = Json.object(java.util.Map.of(
						"reason", Json.of("room_full"),
						"roomId", Json.of(targetRoom.value()),
						"maxPlayers", Json.of(DEFAULT_MAX_PLAYERS)
				));
				return Optional.of(new MessageEnvelope("room_join_failed", request.getRequestId(), fail));
			}
		}

		boolean changed = previousRoom.isEmpty() || !previousRoom.get().equals(targetRoom);
		if (changed) {
			previousRoom.ifPresent(StartGameHandler::cancelAutoStart);
			StartGameHandler.cancelAutoStart(targetRoom);
		}

		engine.joinRoom(targetRoom, client.clientId());
		lobby.ready().ifPresent(rm -> {
			if (changed) {
				rm.clear(client.clientId());
			}
		});
		lobby.broadcastRooms();
		lobby.broadcastLobbyStateMove(previousRoom, targetRoom);
		Json ok = Json.object(java.util.Map.of("roomId", Json.of(targetRoom.value())));
		return Optional.of(new MessageEnvelope("room_joined", request.getRequestId(), ok));
	}
}
