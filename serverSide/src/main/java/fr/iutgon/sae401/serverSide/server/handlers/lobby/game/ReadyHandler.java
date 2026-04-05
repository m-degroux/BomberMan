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
 * Lobby ready toggle handler.
 * <p>
 * Request:  type = "ready" payload = { "ready": boolean }
 * Response: type = "ready" payload = { "ready": boolean, "roomId": string|null }
 */
public final class ReadyHandler implements MessageHandler {
	private final LobbyServices lobby;

	public ReadyHandler(LobbyServices lobby) {
		this.lobby = Objects.requireNonNull(lobby, "lobby");
	}

	@Override
	public String messageType() {
		return "ready";
	}

	@Override
	public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
		var engine = lobby.engine();
		var ready = lobby.ready().orElseThrow(() -> new IllegalStateException("ready manager is required"));

		Json payload = request.getPayload() == null ? Json.nullValue() : request.getPayload();
		boolean isReady = payload.value("ready", false);
		ready.setReady(client.clientId(), isReady);

		Optional<RoomId> roomId = engine.rooms().roomOf(client.clientId());
		lobby.broadcastLobbyState(roomId);
		// Auto-start when everyone in the waiting room is ready.
		roomId.ifPresent(r -> StartGameHandler.tryAutoStart(lobby, r, request.getRequestId()));
		Json out = Json.object(java.util.Map.of(
				"ready", Json.of(isReady),
				"roomId", roomId.<Json>map(r -> Json.of(r.value())).orElse(Json.nullValue())
		));
		return Optional.of(new MessageEnvelope("ready", request.getRequestId(), out));
	}
}
