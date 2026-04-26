package fr.iutgon.sae401.serverSide.server.handlers.lobby.state;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.game.rooms.RoomPerThreadEngine;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import fr.iutgon.sae401.serverSide.server.clients.SkinRegistry;
import fr.iutgon.sae401.serverSide.server.rooms.ReadyManager;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;

import java.util.*;

/**
 * Lobby state snapshot handler.
 * <p>
 * Request:  type = "lobby_state" payload = ignored
 * Response: type = "lobby_state" payload = { "roomId": string|null, "members": [ {"clientId": string, "nickname": string, "skinId": int, "ready": boolean}... ] }
 */
public final class LobbyStateHandler implements MessageHandler {
	private final RoomPerThreadEngine engine;
	private final ReadyManager ready;
	private final fr.iutgon.sae401.serverSide.server.clients.NicknameRegistry nicknames;
	private final SkinRegistry skins;

	public LobbyStateHandler(RoomPerThreadEngine engine, ReadyManager ready, fr.iutgon.sae401.serverSide.server.clients.NicknameRegistry nicknames, SkinRegistry skins) {
		this.engine = Objects.requireNonNull(engine, "engine");
		this.ready = Objects.requireNonNull(ready, "ready");
		this.nicknames = Objects.requireNonNull(nicknames, "nicknames");
		this.skins = Objects.requireNonNull(skins, "skins");
	}

	@Override
	public String messageType() {
		return "lobby_state";
	}

	@Override
	public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
		Optional<RoomId> room = engine.rooms().roomOf(client.clientId());
		List<Json> members = new ArrayList<>();

		if (room.isPresent()) {
			List<ClientId> ids = new ArrayList<>(engine.rooms().members(room.get()));
			ids.sort(Comparator.comparing(ClientId::value));
			for (ClientId id : ids) {
				members.add(Json.object(java.util.Map.of(
						"clientId", Json.of(id.value()),
						"nickname", Json.of(nicknames.getNickname(id)),
						"skinId", Json.of(skins.getSkin(id)),
						"ready", Json.of(ready.isReady(id))
				)));
			}
		}

		Json payload = Json.object(java.util.Map.of(
				"roomId", room.<Json>map(r -> Json.of(r.value())).orElse(Json.nullValue()),
				"members", Json.array(members)
		));
		return Optional.of(new MessageEnvelope("lobby_state", request.getRequestId(), payload));
	}
}
