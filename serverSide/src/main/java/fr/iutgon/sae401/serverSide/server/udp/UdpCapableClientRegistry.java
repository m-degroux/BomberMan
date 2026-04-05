package fr.iutgon.sae401.serverSide.server.udp;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import fr.iutgon.sae401.serverSide.server.clients.ClientRegistry;

import java.net.SocketAddress;

/**
 * Registry extension used to associate a private UDP token and a datagram endpoint with a TCP client.
 */
public interface UdpCapableClientRegistry extends ClientRegistry {
    String ensureUdpToken(ClientId clientId);

    boolean bindUdpEndpoint(String token, SocketAddress address);

    boolean hasUdpEndpoint(ClientId clientId);

    boolean sendUdp(ClientId clientId, MessageEnvelope message);
}
