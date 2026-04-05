package fr.iutgon.sae401.serverSide.server.runtime.config;

import fr.iutgon.sae401.serverSide.game.ServerGameConfig;

/**
 * @brief Aggregates runtime configuration for the transport layer and the game loop.
 */
public record ServerRuntimeConfig(fr.iutgon.sae401.serverSide.server.tcp.ServerConfig server, ServerGameConfig game) {
}
