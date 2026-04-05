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
 * Lobby leave room handler.
 * <p>
 * Request:  type = "leave_room" payload = ignored
 * Response: type = "room_left"  payload = { "previousRoom": string|null }
 */
public final class LeaveRoomHandler implements MessageHandler {
	private final LobbyServices lobby;

	public LeaveRoomHandler(LobbyServices lobby) {
		this.lobby = Objects.requireNonNull(lobby, "lobby");
	}

	@Override
	public String messageType() {
		return "leave_room";
	}

	@Override
	public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
		var engine = lobby.engine();

		Optional<RoomId> previous = engine.rooms().roomOf(client.clientId());
		previous.ifPresent(StartGameHandler::cancelAutoStart);
		engine.leaveRoom(client.clientId());
		engine.joinRoom(engine.defaultRoom(), client.clientId());
		lobby.ready().ifPresent(rm -> rm.clear(client.clientId()));
		lobby.broadcastRooms();
		previous.ifPresent(lobby::broadcastLobbyState);
		lobby.broadcastLobbyState(engine.defaultRoom());

		Json payload = Json.object(java.util.Map.of(
				"previousRoom", previous.<Json>map(r -> Json.of(r.value())).orElse(Json.nullValue())
		));
		return Optional.of(new MessageEnvelope("room_left", request.getRequestId(), payload));
	}
}
