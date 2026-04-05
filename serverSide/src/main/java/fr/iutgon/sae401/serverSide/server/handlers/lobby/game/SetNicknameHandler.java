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
 * Nickname change handler.
 * <p>
 * Request:  type = "set_nickname" payload = { "nickname": string }
 * Response: type = "set_nickname" payload = { "nickname": string, "roomId": string|null }
 */
public final class SetNicknameHandler implements MessageHandler {
	private final LobbyServices lobby;

	public SetNicknameHandler(LobbyServices lobby) {
		this.lobby = Objects.requireNonNull(lobby, "lobby");
	}

	@Override
	public String messageType() {
		return "set_nickname";
	}

	@Override
	public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
		Json payload = request.getPayload() == null ? Json.nullValue() : request.getPayload();
		String nickname = payload.value("nickname", "").trim();

		if (nickname.isBlank()) {
			return Optional.of(new MessageEnvelope("set_nickname", request.getRequestId(),
					Json.object(java.util.Map.of("error", Json.of("nickname cannot be empty")))));
		}

		lobby.nicknames().setNickname(client.clientId(), nickname);

		Optional<RoomId> roomId = lobby.engine().rooms().roomOf(client.clientId());
		lobby.broadcastLobbyState(roomId);

		Json out = Json.object(java.util.Map.of(
				"nickname", Json.of(nickname),
				"roomId", roomId.<Json>map(r -> Json.of(r.value())).orElse(Json.nullValue())
		));
		return Optional.of(new MessageEnvelope("set_nickname", request.getRequestId(), out));
	}
}
