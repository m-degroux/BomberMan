package fr.iutgon.sae401.serverSide.game.modules;

import fr.iutgon.sae401.serverSide.game.GameEngine;
import fr.iutgon.sae401.serverSide.game.GameModule;
import fr.iutgon.sae401.serverSide.game.ServerGameConfig;
import fr.iutgon.sae401.serverSide.server.MessageHandler;
import fr.iutgon.sae401.serverSide.server.clients.ClientRegistry;
import fr.iutgon.sae401.serverSide.server.handlers.game.InputHandler;
import fr.iutgon.sae401.serverSide.server.handlers.system.EchoHandler;
import fr.iutgon.sae401.serverSide.server.handlers.system.HealthHandler;
import fr.iutgon.sae401.serverSide.server.handlers.system.PingHandler;
import fr.iutgon.sae401.serverSide.server.handlers.system.ServerInfoHandler;

import java.util.List;

/**
 * @brief Demo module that exposes the built-in generic handlers.
 * <p>
 * This keeps existing integration tests and manual usage working while the
 * real game module is developed.
 */
public final class BuiltinHandlersModule implements GameModule {
    private final String serverName;
    private final String protocolVersion;

    public BuiltinHandlersModule(String serverName, String protocolVersion) {
        this.serverName = serverName;
        this.protocolVersion = protocolVersion;
    }

    @Override
    public String name() {
        return "builtin-handlers";
    }

    @Override
    public GameEngine createEngine(ServerGameConfig config, ClientRegistry clients) {
        return dtSeconds -> {
            // No simulation for the demo module.
        };
    }

    @Override
    public Iterable<MessageHandler> createHandlers(GameEngine engine, ClientRegistry clients) {
        return List.of(
                new PingHandler(),
                new EchoHandler(),
                new HealthHandler(),
                new InputHandler(engine),
                new ServerInfoHandler(serverName, protocolVersion)
        );
    }
}
