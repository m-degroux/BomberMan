package fr.iutgon.sae401.serverSide.server.handlers.lobby.chat;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import fr.iutgon.sae401.serverSide.server.clients.ClientRegistry;
import fr.iutgon.sae401.serverSide.server.clients.NicknameRegistry;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;
import fr.iutgon.sae401.serverSide.server.rooms.RoomManager;

import java.util.Objects;
import java.util.Optional;

/**
 * Handler for chat messages. Broadcasts a chat message to all clients in the sender's room.
 */
public final class ChatHandler implements MessageHandler {
	private final RoomManager roomManager;
	private final ClientRegistry clientRegistry;
	private final NicknameRegistry nicknameRegistry;

	public ChatHandler(RoomManager roomManager, ClientRegistry clientRegistry, NicknameRegistry nicknameRegistry) {
		this.roomManager = Objects.requireNonNull(roomManager);
		this.clientRegistry = Objects.requireNonNull(clientRegistry);
		this.nicknameRegistry = Objects.requireNonNull(nicknameRegistry);
	}

	@Override
	public String messageType() {
		return "chat";
	}

	@Override
	public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
		ClientId senderId = client.clientId();
		Optional<RoomId> roomOpt = roomManager.roomOf(senderId);
		if (roomOpt.isEmpty()) {
			return Optional.of(new MessageEnvelope("chat_error", request.getRequestId(),
					Json.object(java.util.Map.of(
							"error", Json.of("Not in a room")
					))
			));
		}
		RoomId roomId = roomOpt.get();
		Json payload = request.getPayload();
		String message = payload != null ? payload.at("message").getString() : null;
		if (message == null || message.isBlank()) {
			return Optional.of(new MessageEnvelope("chat_error", request.getRequestId(),
					Json.object(java.util.Map.of(
							"error", Json.of("Empty chat message")
					))
			));
		}

		// Get the sender's nickname
		String senderNickname = nicknameRegistry.getNickname(senderId);

		// Broadcast to all clients in the room
		for (ClientId member : roomManager.members(roomId)) {
			clientRegistry.send(member, new MessageEnvelope(
					"chat",
					request.getRequestId(),
					Json.object(java.util.Map.of(
							"from", Json.of(senderNickname),
							"roomId", Json.of(roomId.value()),
							"message", Json.of(message)
					))
			));
		}
		return Optional.empty();
	}
}
