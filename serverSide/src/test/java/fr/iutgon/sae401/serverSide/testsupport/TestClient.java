package fr.iutgon.sae401.serverSide.testsupport;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.common.protocol.MessageEnvelopeBinary;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

/**
 * Small TCP client for integration tests.
 */
public final class TestClient implements AutoCloseable {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public TestClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public MessageEnvelope request(MessageEnvelope req) throws IOException {
        send(MessageEnvelopeBinary.toBytes(req));
        byte[] responseBytes = read();
        return MessageEnvelopeBinary.fromBytes(responseBytes);
    }

    public void sendEnvelope(MessageEnvelope req) throws IOException {
        send(MessageEnvelopeBinary.toBytes(req));
    }

    public MessageEnvelope readEnvelope(Duration timeout) throws IOException {
        int previous = socket.getSoTimeout();
        int ms = (timeout == null) ? 0 : (int) Math.max(1L, timeout.toMillis());
        socket.setSoTimeout(ms);
        try {
            byte[] bytes = read();
            return MessageEnvelopeBinary.fromBytes(bytes);
        } finally {
            socket.setSoTimeout(previous);
        }
    }

    public MessageEnvelope awaitMessage(java.util.function.Predicate<MessageEnvelope> predicate, Duration timeout) throws IOException {
        Instant deadline = Instant.now().plus(timeout == null ? Duration.ofSeconds(2) : timeout);
        while (Instant.now().isBefore(deadline)) {
            Duration remaining = Duration.between(Instant.now(), deadline);
            if (remaining.isNegative() || remaining.isZero()) {
                break;
            }
            try {
                MessageEnvelope msg = readEnvelope(remaining);
                if (predicate.test(msg)) {
                    return msg;
                }
            } catch (SocketTimeoutException ignored) {
                // retry until deadline
            }
        }
        throw new SocketTimeoutException("Timed out waiting for message");
    }

    private void send(byte[] bytes) throws IOException {
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
    }

    private byte[] read() throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > 10_000_000) {
            throw new IOException("Invalid frame length: " + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return bytes;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
