package fr.iutgon.sae401.serverSide.server.handlers.lobby.rooms;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.game.rooms.RoomPerThreadEngine;
import fr.iutgon.sae401.serverSide.server.clients.ClientRegistry;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @brief Push "rooms" snapshots to all connected clients.
 *
 * This is used to keep lobby screens updated without polling.
 */
public final class RoomsBroadcaster {
	private RoomsBroadcaster() {
	}

	public static void broadcast(RoomPerThreadEngine engine, ClientRegistry clients) {
		Objects.requireNonNull(engine, "engine");
		Objects.requireNonNull(clients, "clients");

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

		Json payload = Json.object(java.util.Map.of(
				"rooms", Json.array(rooms),
				"yourRoom", Json.nullValue()
		));
		clients.broadcast(new MessageEnvelope("rooms", null, payload));
	}
}
