package fr.iutgon.sae401.serverSide.app;

import fr.iutgon.sae401.serverSide.game.modules.LobbyRoomsModule;
import fr.iutgon.sae401.serverSide.server.runtime.GameServer;
import fr.iutgon.sae401.serverSide.server.runtime.config.ArgsConfigLoader;
import fr.iutgon.sae401.serverSide.server.runtime.config.ServerRuntimeConfig;

/**
 * @brief Main entry point for the reusable server runtime.
 */
public final class GameServerMain {
    public static void main(String[] args) {
        ServerRuntimeConfig cfg = ArgsConfigLoader.load(args);
        var module = new LobbyRoomsModule("json-tcp-server-java", "0.1");
        GameServer server = GameServer.create(cfg.server(), cfg.game(), module);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}
