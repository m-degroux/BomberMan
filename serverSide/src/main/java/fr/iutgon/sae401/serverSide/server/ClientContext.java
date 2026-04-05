package fr.iutgon.sae401.serverSide.server;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;

import java.io.IOException;
import java.net.SocketAddress;

public interface ClientContext {
    /**
     * @brief Server-assigned identifier for this client.
     */
    default ClientId clientId() {
        return ClientId.UNKNOWN;
    }

    /**
     * @brief Remote address of the connected client.
     */
    SocketAddress remoteAddress();

    /**
     * @brief Send an envelope to this client using the server's transport framing.
     */
    void send(MessageEnvelope message) throws IOException;

}
