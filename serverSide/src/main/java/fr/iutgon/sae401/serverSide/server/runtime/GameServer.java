package fr.iutgon.sae401.serverSide.server.runtime;

import fr.iutgon.sae401.serverSide.game.GameEngine;
import fr.iutgon.sae401.serverSide.game.GameModule;
import fr.iutgon.sae401.serverSide.game.SelfRunningGameEngine;
import fr.iutgon.sae401.serverSide.game.ServerGameConfig;
import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.MessageRouter;
import fr.iutgon.sae401.serverSide.server.NetworkServer;
import fr.iutgon.sae401.serverSide.server.clients.ClientLifecycleListener;
import fr.iutgon.sae401.serverSide.server.tcp.BlockingTcpServer;
import fr.iutgon.sae401.serverSide.server.udp.DatagramUdpServer;
import fr.iutgon.sae401.serverSide.server.udp.HybridClientRegistry;

import java.util.Objects;

/**
 * @brief High-level bootstrap that assembles transport + routing + game loop.
 */
public final class GameServer {
    private final NetworkServer transport;
    private final GameLoop loop;

    private GameServer(NetworkServer transport, GameLoop loop) {
        this.transport = transport;
        this.loop = loop;
    }

    /**
     * @brief Build a server for a given {@link GameModule}.
     */
    public static GameServer create(fr.iutgon.sae401.serverSide.server.tcp.ServerConfig serverConfig, ServerGameConfig serverGameConfig, GameModule module) {
        Objects.requireNonNull(serverConfig, "serverConfig");
        Objects.requireNonNull(serverGameConfig, "gameConfig");
        Objects.requireNonNull(module, "module");

        HybridClientRegistry clients = new HybridClientRegistry();
        GameEngine engine = module.createEngine(serverGameConfig, clients);
        var router = new MessageRouter(module.createHandlers(engine, clients));

        ClientLifecycleListener lifecycle = new ClientLifecycleListener() {
            @Override
            public void onConnected(fr.iutgon.sae401.serverSide.server.ClientContext client) {
                String udpToken = clients.ensureUdpToken(client.clientId());
                // Tell the client its server-assigned id (the server generates it).
                try {
                    client.send(MessageEnvelope.of(
                        "welcome",
                        Json.object(java.util.Map.of(
                            "clientId", Json.of(client.clientId().value()),
                            "udpPort", Json.of(serverConfig.udpPort()),
                            "udpToken", Json.of(udpToken)
                        ))
                    ));
                } catch (java.io.IOException ignored) {
                    // Client might disconnect before we send the welcome.
                }
                engine.onClientConnected(client.clientId());
            }

            @Override
            public void onDisconnected(fr.iutgon.sae401.serverSide.server.ClientContext client) {
                engine.onClientDisconnected(client.clientId());
            }
        };

        NetworkServer tcp = new BlockingTcpServer(serverConfig, router, clients, lifecycle);
        NetworkServer udp = new DatagramUdpServer(serverConfig.udpPort(), clients);
        NetworkServer transport = new NetworkServer() {
            @Override
            public void start() {
                udp.start();
                tcp.start();
            }

            @Override
            public void stop() {
                tcp.stop();
                udp.stop();
            }
        };
        GameLoop loop = (engine instanceof SelfRunningGameEngine)
                ? new NoopGameLoop(engine)
                : new FixedRateGameLoop(serverGameConfig, engine);
        return new GameServer(transport, loop);
    }

    public void start() {
        loop.start();
        transport.start();
    }

    public void stop() {
        transport.stop();
        loop.stop();
    }
}
