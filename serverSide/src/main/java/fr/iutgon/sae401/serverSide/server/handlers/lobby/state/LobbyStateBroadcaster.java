package fr.iutgon.sae401.serverSide.server.handlers.lobby.state;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.game.rooms.RoomPerThreadEngine;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import fr.iutgon.sae401.serverSide.server.clients.ClientRegistry;
import fr.iutgon.sae401.serverSide.server.rooms.ReadyManager;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Push "lobby_state" snapshots to all members of a room.
 */
public final class LobbyStateBroadcaster {
	private LobbyStateBroadcaster() {
	}

	public static void broadcastRoom(RoomPerThreadEngine engine, ReadyManager ready, ClientRegistry clients, fr.iutgon.sae401.serverSide.server.clients.NicknameRegistry nicknames, RoomId roomId) {
		Objects.requireNonNull(engine, "engine");
		Objects.requireNonNull(ready, "ready");
		Objects.requireNonNull(clients, "clients");
		Objects.requireNonNull(nicknames, "nicknames");
		Objects.requireNonNull(roomId, "roomId");

		java.util.Set<ClientId> idsSet = new java.util.HashSet<>(engine.rooms().members(roomId));
		java.util.Set<ClientId> matchIds = java.util.Set.of();
		if (roomId.value().startsWith("room-")) {
			String base = fr.iutgon.sae401.serverSide.server.rooms.RoomIdNaming.baseNameFrom(roomId.value());
			if (!base.isBlank()) {
				RoomId matchRoom = fr.iutgon.sae401.serverSide.server.rooms.RoomIdNaming.matchIdFromBaseName(base);
				matchIds = engine.rooms().members(matchRoom);
				idsSet.addAll(matchIds);
			}
		} else if (roomId.value().startsWith("match-")) {
			matchIds = engine.rooms().members(roomId);
		}

		List<ClientId> ids = new ArrayList<>(idsSet);
		ids.sort(Comparator.comparing(ClientId::value));

		List<Json> members = new ArrayList<>(ids.size());
		for (ClientId id : ids) {
			boolean inGame = matchIds.contains(id);
			members.add(Json.object(java.util.Map.of(
					"clientId", Json.of(id.value()),
					"nickname", Json.of(nicknames.getNickname(id)),
					"ready", Json.of(!inGame && ready.isReady(id)),
					"inGame", Json.of(inGame)
			)));
		}

		Json payload = Json.object(java.util.Map.of(
				"roomId", Json.of(roomId.value()),
				"members", Json.array(members)
		));

		MessageEnvelope msg = new MessageEnvelope("lobby_state", null, payload);
		for (ClientId id : ids) {
			clients.send(id, msg);
		}
	}
}
