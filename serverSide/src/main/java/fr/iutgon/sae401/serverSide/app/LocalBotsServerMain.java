package fr.iutgon.sae401.serverSide.app;

import fr.iutgon.sae401.serverSide.game.modules.LobbyRoomsModule;
import fr.iutgon.sae401.serverSide.server.runtime.GameServer;
import fr.iutgon.sae401.serverSide.server.runtime.config.ArgsConfigLoader;
import fr.iutgon.sae401.serverSide.server.runtime.config.ServerRuntimeConfig;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;

import java.util.List;

/**
 * Local dev entry point: starts a server with a pre-created match room filled with server-side bots.
 *
 * Usage:
 * - java ... LocalBotsServerMain --port 7777
 * Then from the client, join room "match-local".
 */
public final class LocalBotsServerMain {
	public static void main(String[] args) {
		ServerRuntimeConfig cfg = ArgsConfigLoader.load(args);
		var module = new LobbyRoomsModule("local-bots-server", "0.1", new RoomId("lobby"), List.of(new RoomId("match-local")));
		GameServer server = GameServer.create(cfg.server(), cfg.game(), module);

		Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
		server.start();
	}
}
