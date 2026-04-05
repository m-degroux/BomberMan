package fr.iutgon.sae401.serverSide.server.tcp;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.common.protocol.MessageEnvelopeBinary;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * @brief Socket-backed client context, used by handlers to send envelopes.
 */
final class SocketClientContext implements ClientContext {
    private final ClientId id;
    private final Socket socket;
    private final DataOutputStream out;

    SocketClientContext(ClientId id, Socket socket, DataOutputStream out) {
        this.id = id;
        this.socket = socket;
        this.out = out;
    }

    @Override
    public ClientId clientId() {
        return id;
    }

    @Override
    public SocketAddress remoteAddress() {
        return socket.getRemoteSocketAddress();
    }

    @Override
    public void send(MessageEnvelope message) throws IOException {
        byte[] bytes = MessageEnvelopeBinary.toBytes(message);
        synchronized (out) {
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();
        }
    }
}
