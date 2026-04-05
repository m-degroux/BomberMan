package fr.iutgon.sae401.serverSide.server.udp;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.common.protocol.MessageEnvelopeBinary;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combined TCP/UDP registry.
 *
 * TCP remains authoritative for connection lifecycle and reliable messages.
 * UDP is bound later through a private token exchanged on the TCP welcome message.
 */
public final class HybridClientRegistry implements UdpCapableClientRegistry {
    private final ConcurrentHashMap<ClientId, ClientContext> clientsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ClientId, String> udpTokensByClient = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ClientId> clientByUdpToken = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ClientId, SocketAddress> udpEndpointsByClient = new ConcurrentHashMap<>();

    private volatile DatagramSocket udpSocket;

    void attachUdpSocket(DatagramSocket socket) {
        this.udpSocket = socket;
    }

    @Override
    public void register(ClientContext client) {
        Objects.requireNonNull(client, "client");
        ClientId id = Objects.requireNonNull(client.clientId(), "client.clientId()");
        ClientContext previous = clientsById.putIfAbsent(id, client);
        if (previous != null) {
            throw new IllegalArgumentException("Client already registered: " + id.value());
        }
    }

    @Override
    public void unregister(ClientId id) {
        Objects.requireNonNull(id, "id");
        clientsById.remove(id);
        udpEndpointsByClient.remove(id);
        String token = udpTokensByClient.remove(id);
        if (token != null) {
            clientByUdpToken.remove(token, id);
        }
    }

    @Override
    public java.util.Optional<ClientContext> get(ClientId id) {
        Objects.requireNonNull(id, "id");
        return java.util.Optional.ofNullable(clientsById.get(id));
    }

    @Override
    public Collection<ClientId> ids() {
        return List.copyOf(clientsById.keySet());
    }

    @Override
    public boolean send(ClientId id, MessageEnvelope message) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(message, "message");
        ClientContext client = clientsById.get(id);
        if (client == null) {
            return false;
        }
        try {
            client.send(message);
            return true;
        } catch (IOException e) {
            unregister(id);
            return false;
        }
    }

    @Override
    public int broadcast(MessageEnvelope message) {
        Objects.requireNonNull(message, "message");
        int ok = 0;
        for (ClientId id : ids()) {
            if (send(id, message)) {
                ok++;
            }
        }
        return ok;
    }

    @Override
    public String ensureUdpToken(ClientId clientId) {
        Objects.requireNonNull(clientId, "clientId");
        if (!clientsById.containsKey(clientId)) {
            throw new IllegalArgumentException("Unknown client: " + clientId.value());
        }
        return udpTokensByClient.computeIfAbsent(clientId, id -> {
            String token = UUID.randomUUID().toString();
            clientByUdpToken.put(token, id);
            return token;
        });
    }

    @Override
    public boolean bindUdpEndpoint(String token, SocketAddress address) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(address, "address");
        ClientId clientId = clientByUdpToken.get(token);
        if (clientId == null || !clientsById.containsKey(clientId)) {
            return false;
        }
        udpEndpointsByClient.put(clientId, address);
        return true;
    }

    @Override
    public boolean hasUdpEndpoint(ClientId clientId) {
        return clientId != null && udpEndpointsByClient.containsKey(clientId);
    }

    @Override
    public boolean sendUdp(ClientId clientId, MessageEnvelope message) {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(message, "message");
        DatagramSocket socket = udpSocket;
        SocketAddress remote = udpEndpointsByClient.get(clientId);
        if (socket == null || socket.isClosed() || remote == null) {
            return false;
        }
        try {
            byte[] bytes = MessageEnvelopeBinary.toBytes(message);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            packet.setSocketAddress(remote);
            socket.send(packet);
            return true;
        } catch (IOException e) {
            udpEndpointsByClient.remove(clientId);
            return false;
        }
    }
}
