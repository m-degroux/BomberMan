package fr.iutgon.sae401.serverSide.server.clients;

import fr.iutgon.sae401.serverSide.server.ClientContext;

/**
 * @brief Callback interface for connection lifecycle events.
 * <p>
 * This allows higher-level layers (game engine, session manager, etc.)
 * to react to new connections and disconnections.
 */
public interface ClientLifecycleListener {
    static ClientLifecycleListener noop() {
        return new ClientLifecycleListener() {
            @Override
            public void onConnected(ClientContext client) {
            }

            @Override
            public void onDisconnected(ClientContext client) {
            }
        };
    }

    void onConnected(ClientContext client);

    void onDisconnected(ClientContext client);
}
