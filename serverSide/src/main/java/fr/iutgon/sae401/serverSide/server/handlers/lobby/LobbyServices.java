package fr.iutgon.sae401.serverSide.server.handlers.lobby;

import fr.iutgon.sae401.serverSide.game.rooms.RoomPerThreadEngine;
import fr.iutgon.sae401.serverSide.server.clients.ClientRegistry;
import fr.iutgon.sae401.serverSide.server.clients.NicknameRegistry;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.rooms.RoomsBroadcaster;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.state.LobbyStateBroadcaster;
import fr.iutgon.sae401.serverSide.server.rooms.ReadyManager;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;

import java.util.Objects;
import java.util.Optional;

/**
 * Small façade used by lobby handlers to:
 * - access engine/ready/clients
 * - centralize broadcasts (rooms + lobby_state)
 */
public final class LobbyServices {
	private final RoomPerThreadEngine engine;
	private final ReadyManager ready; // may be null in some configurations
	private final ClientRegistry clients;
	private final NicknameRegistry nicknames;

	public LobbyServices(RoomPerThreadEngine engine, ReadyManager ready, ClientRegistry clients, NicknameRegistry nicknames) {
		this.engine = Objects.requireNonNull(engine, "engine");
		this.ready = ready;
		this.clients = Objects.requireNonNull(clients, "clients");
		this.nicknames = Objects.requireNonNull(nicknames, "nicknames");
	}

	public RoomPerThreadEngine engine() {
		return engine;
	}

	public ClientRegistry clients() {
		return clients;
	}

	public Optional<ReadyManager> ready() {
		return Optional.ofNullable(ready);
	}

	public NicknameRegistry nicknames() {
		return nicknames;
	}

	public void broadcastRooms() {
		RoomsBroadcaster.broadcast(engine, clients);
	}

	public void broadcastLobbyState(RoomId roomId) {
		if (ready == null) {
			return;
		}
		LobbyStateBroadcaster.broadcastRoom(engine, ready, clients, nicknames, roomId);
	}

	public void broadcastLobbyState(Optional<RoomId> roomId) {
		roomId.ifPresent(this::broadcastLobbyState);
	}

	public void broadcastLobbyStateMove(Optional<RoomId> previous, RoomId current) {
		if (ready == null) {
			return;
		}
		previous.filter(r -> !r.equals(current)).ifPresent(this::broadcastLobbyState);
		broadcastLobbyState(current);
	}
}
