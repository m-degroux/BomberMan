package fr.iutgon.sae401.serverSide.server.handlers.lobby.rooms;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.game.rooms.RoomPerThreadEngine;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Lobby room listing handler.
 * <p>
 * Request:  type = "list_rooms" payload = ignored
 * Response: type = "rooms"      payload = { "rooms": [ {"id": string, "members": int}... ], "yourRoom": string|null }
 */
public final class ListRoomsHandler implements MessageHandler {
	private final RoomPerThreadEngine engine;

	public ListRoomsHandler(RoomPerThreadEngine engine) {
		this.engine = Objects.requireNonNull(engine, "engine");
	}

	@Override
	public String messageType() {
		return "list_rooms";
	}

	@Override
	public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
		List<Json> rooms = new ArrayList<>();
		for (RoomId roomId : engine.rooms().rooms()) {
			if (roomId.equals(engine.defaultRoom())) {
				continue;
			}
			// Matches are internal runtime rooms; users should join the waiting room instead.
			if (roomId.value().startsWith("match-")) {
				continue;
			}
			int members = engine.rooms().members(roomId).size();
			boolean hasPassword = engine.hasPassword(roomId);
			rooms.add(Json.object(java.util.Map.of(
					"id", Json.of(roomId.value()),
					"members", Json.of(members),
					"hasPassword", Json.of(hasPassword)
			)));
		}

		Json yourRoom = engine.rooms().roomOf(client.clientId())
				.<Json>map(r -> Json.of(r.value()))
				.orElse(Json.nullValue());

		Json payload = Json.object(java.util.Map.of(
				"rooms", Json.array(rooms),
				"yourRoom", yourRoom
		));
		return Optional.of(new MessageEnvelope("rooms", request.getRequestId(), payload));
	}
}
