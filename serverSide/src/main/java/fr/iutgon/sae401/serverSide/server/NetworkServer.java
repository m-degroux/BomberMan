package fr.iutgon.sae401.serverSide.server;

public interface NetworkServer {
    /**
     * @brief Start accepting client connections.
     */
    void start();

    /**
     * @brief Stop the server and close active resources.
     */
    void stop();
}
