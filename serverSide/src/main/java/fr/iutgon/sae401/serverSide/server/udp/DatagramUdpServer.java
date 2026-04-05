package fr.iutgon.sae401.serverSide.server.udp;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.common.protocol.MessageEnvelopeBinary;
import fr.iutgon.sae401.serverSide.server.NetworkServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal UDP transport used only for the gameplay freshness channel.
 */
public final class DatagramUdpServer implements NetworkServer {
    private static final Logger LOGGER = Logger.getLogger();
    private static final int MAX_PACKET_SIZE = 65_507;

    private final int port;
    private final HybridClientRegistry clients;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile DatagramSocket socket;
    private volatile Thread readerThread;

    public DatagramUdpServer(int port, HybridClientRegistry clients) {
        this.port = port;
        this.clients = java.util.Objects.requireNonNull(clients, "clients");
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            DatagramSocket udpSocket = new DatagramSocket(port);
            this.socket = udpSocket;
            this.clients.attachUdpSocket(udpSocket);
            readerThread = new Thread(this::readLoop, "udp-server-" + port);
            readerThread.setDaemon(true);
            readerThread.start();
        } catch (IOException e) {
            running.set(false);
            throw new RuntimeException("Failed to start UDP server on port " + port, e);
        }
    }

    private void readLoop() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] payload = java.util.Arrays.copyOf(packet.getData(), packet.getLength());
                MessageEnvelope envelope = MessageEnvelopeBinary.fromBytes(payload);
                if (!"udp_hello".equals(envelope.getType())) {
                    continue;
                }
                Json body = envelope.getPayload() == null ? Json.emptyObject() : envelope.getPayload();
                String token = body.value("udpToken", "");
                if (token.isBlank()) {
                    continue;
                }
                boolean bound = clients.bindUdpEndpoint(token, packet.getSocketAddress());
                if (bound) {
                    byte[] ack = MessageEnvelopeBinary.toBytes(MessageEnvelope.of(
                            "udp_ready",
                            Json.object(java.util.Map.of("ok", Json.of(true)))
                    ));
                    DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
                    ackPacket.setSocketAddress(packet.getSocketAddress());
                    socket.send(ackPacket);
                }
            } catch (IOException e) {
                if (running.get()) {
                    LOGGER.log(new LogMessage("[UDP] Erreur de lecture datagramme: " + e.getMessage(), LogLevel.WARNING));
                }
            } catch (RuntimeException e) {
                LOGGER.log(new LogMessage("[UDP] Paquet ignoré: " + e.getMessage(), LogLevel.WARNING));
            }
        }
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        DatagramSocket udpSocket = socket;
        socket = null;
        if (udpSocket != null) {
            udpSocket.close();
        }
        Thread reader = readerThread;
        readerThread = null;
        if (reader != null) {
            reader.interrupt();
        }
        clients.attachUdpSocket(null);
    }
}
