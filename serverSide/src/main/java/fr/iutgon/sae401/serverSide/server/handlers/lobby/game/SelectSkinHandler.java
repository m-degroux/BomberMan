package fr.iutgon.sae401.serverSide.server.handlers.lobby.game;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.LobbyServices;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;

import java.util.Objects;
import java.util.Optional;

/**
 * Skin selection handler.
 * <p>
 * Request:  type = "select_skin" payload = { "skinId": int }
 * Response: type = "select_skin" payload = { "skinId": int, "roomId": string|null } or { "error": string }
 */
public final class SelectSkinHandler implements MessageHandler {
	private final LobbyServices lobby;

	public SelectSkinHandler(LobbyServices lobby) {
		this.lobby = Objects.requireNonNull(lobby, "lobby");
	}

	@Override
	public String messageType() {
		return "select_skin";
	}

	@Override
	public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
		Json payload = request.getPayload() == null ? Json.nullValue() : request.getPayload();
		int skinId = payload.value("skinId", 0);

		// Validate skin ID (0-34, excluding 6, 9, 30, 32)
		if (!isValidSkinId(skinId)) {
			return Optional.of(new MessageEnvelope("select_skin", request.getRequestId(),
					Json.object(java.util.Map.of("error", Json.of("invalid skin id")))));
		}

		// Check if skin is already taken by another player
		if (lobby.skins().isSkinTaken(skinId, client.clientId())) {
			return Optional.of(new MessageEnvelope("select_skin", request.getRequestId(),
					Json.object(java.util.Map.of("error", Json.of("skin already taken")))));
		}

		// Assign the skin
		boolean assigned = lobby.skins().setSkin(client.clientId(), skinId);
		if (!assigned) {
			return Optional.of(new MessageEnvelope("select_skin", request.getRequestId(),
					Json.object(java.util.Map.of("error", Json.of("failed to assign skin")))));
		}

		Optional<RoomId> roomId = lobby.engine().rooms().roomOf(client.clientId());
		lobby.broadcastLobbyState(roomId);

		Json out = Json.object(java.util.Map.of(
				"skinId", Json.of(skinId),
				"roomId", roomId.<Json>map(r -> Json.of(r.value())).orElse(Json.nullValue())
		));
		return Optional.of(new MessageEnvelope("select_skin", request.getRequestId(), out));
	}

	private boolean isValidSkinId(int skinId) {
		if (skinId < 0 || skinId > 34) {
			return false;
		}
		// Exclude specific invalid skins
		return skinId != 6 && skinId != 9 && skinId != 30 && skinId != 32;
	}
}
