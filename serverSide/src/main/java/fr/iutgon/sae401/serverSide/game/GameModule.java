package fr.iutgon.sae401.serverSide.game;

import fr.iutgon.sae401.serverSide.server.MessageHandler;
import fr.iutgon.sae401.serverSide.server.clients.ClientRegistry;

/**
 * @brief Plug-in entry point for a server-hosted game.
 * <p>
 * A module is responsible for creating:
 * - a {@link GameEngine} (state + simulation)
 * - the set of {@link MessageHandler} used to receive client messages
 */
public interface GameModule {
    /**
     * @brief Human-readable module name.
     */
    String name();

    /**
     * @brief Create the authoritative engine for this module.
     */
    GameEngine createEngine(ServerGameConfig config, ClientRegistry clients);

    /**
     * @brief Create network handlers bound to this module.
     */
    Iterable<MessageHandler> createHandlers(GameEngine engine, ClientRegistry clients);
}
