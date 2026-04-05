package fr.iutgon.sae401.serverSide.server.tcp;

/**
 * @param port       TCP port to bind to
 * @param udpPort    UDP port to bind to
 * @param maxClients maximum number of concurrent client handler threads
 * @brief TCP server configuration.
 */
public record ServerConfig(int port, int udpPort, int maxClients) {
    public ServerConfig(int port, int maxClients) {
        this(port, port, maxClients);
    }

    public ServerConfig {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be in 1..65535");
        }
        if (udpPort <= 0 || udpPort > 65535) {
            throw new IllegalArgumentException("udpPort must be in 1..65535");
        }
        if (maxClients <= 0) {
            throw new IllegalArgumentException("maxClients must be > 0");
        }
    }
}
