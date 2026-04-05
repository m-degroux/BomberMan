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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Restart flow: create a new waiting room (non match-*) with the same parameters
 * and move all members of the current match room into it.
 *
 * This allows players to go back to the lobby UI, toggle ready, and start a new match.
 *
 * Request:  type = "restart_game" payload = ignored
 * Response: broadcast type = "room_restarted" payload = { "oldRoom": string, "newRoom": string, "members": [string] }
 * OR type = "restart_failed" payload = { "reason": string, "roomId": string|null }
 */
public final class RestartGameHandler implements MessageHandler {
	private static final int DEFAULT_MIN_PLAYERS = 1;
	private static final int DEFAULT_MAX_PLAYERS = 4;

	private final LobbyServices lobby;

	public RestartGameHandler(LobbyServices lobby) {
		this.lobby = Objects.requireNonNull(lobby, "lobby");
	}

	@Override
	public String messageType() {
		return "restart_game";
	}

	@Override
	public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
		var engine = lobby.engine();
		var clients = lobby.clients();
		var readyOpt = lobby.ready();

		Optional<RoomId> roomOpt = engine.rooms().roomOf(client.clientId());
		if (roomOpt.isEmpty()) {
			return Optional.of(new MessageEnvelope("restart_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("not_in_room"),
							"roomId", Json.nullValue()
					))));
		}
		RoomId oldRoom = roomOpt.get();
		if (!oldRoom.value().startsWith("match-")) {
			return Optional.of(new MessageEnvelope("restart_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("not_in_match_room"),
							"roomId", Json.of(oldRoom.value())
					))));
		}

		if (!engine.isMatchFinished(oldRoom)) {
			return Optional.of(new MessageEnvelope("restart_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("match_not_finished"),
							"roomId", Json.of(oldRoom.value())
					))));
		}

		Set<ClientId> membersSet = engine.rooms().members(oldRoom);
		if (membersSet.isEmpty()) {
			return Optional.of(new MessageEnvelope("restart_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("empty_room"),
							"roomId", Json.of(oldRoom.value())
					))));
		}
		if (membersSet.size() < DEFAULT_MIN_PLAYERS) {
			return Optional.of(new MessageEnvelope("restart_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("not_enough_players"),
							"roomId", Json.of(oldRoom.value()),
							"minPlayers", Json.of(DEFAULT_MIN_PLAYERS)
					))));
		}
		if (membersSet.size() > DEFAULT_MAX_PLAYERS) {
			return Optional.of(new MessageEnvelope("restart_failed", request.getRequestId(),
					Json.object(Map.of(
							"reason", Json.of("too_many_players"),
							"roomId", Json.of(oldRoom.value()),
							"maxPlayers", Json.of(DEFAULT_MAX_PLAYERS)
					))));
		}

		List<ClientId> members = new ArrayList<>(membersSet);
		String base = RoomIdNaming.baseNameFrom(oldRoom.value());
		if (base.isBlank()) {
			base = UUID.randomUUID().toString().substring(0, 8);
		}
		RoomId newRoom = RoomIdNaming.roomIdFromBaseName(base);
		if (!engine.rooms().members(newRoom).isEmpty()) {
			String suffix = UUID.randomUUID().toString().substring(0, 4);
			newRoom = new RoomId(newRoom.value() + "-" + suffix);
		}
		GameConfig cfg = engine.getGameplayConfig(oldRoom);
		engine.setGameplayConfig(newRoom, cfg != null ? cfg : new GameConfig());
		MapTheme theme = engine.getMapTheme(oldRoom);
		engine.setMapTheme(newRoom, theme != null ? theme : MapTheme.CLASSIC);

		for (ClientId id : members) {
			readyOpt.ifPresent(r -> r.clear(id));
			engine.joinRoom(newRoom, id);
		}

		List<String> memberIds = members.stream().map(ClientId::value).collect(Collectors.toList());
		Json payload = Json.object(Map.of(
				"oldRoom", Json.of(oldRoom.value()),
				"newRoom", Json.of(newRoom.value()),
				"members", Json.array(memberIds.stream().map(Json::of).collect(Collectors.toList()))
		));
		MessageEnvelope restarted = new MessageEnvelope("room_restarted", request.getRequestId(), payload);
		for (ClientId id : members) {
			clients.send(id, restarted);
		}
		lobby.broadcastRooms();
		lobby.broadcastLobbyState(oldRoom);
		lobby.broadcastLobbyState(newRoom);
		return Optional.empty();
	}
}
