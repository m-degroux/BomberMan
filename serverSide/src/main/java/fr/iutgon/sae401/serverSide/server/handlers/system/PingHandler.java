package fr.iutgon.sae401.serverSide.server.handlers.system;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;

import java.time.Instant;
import java.util.Optional;

/**
 * Application-level ping handler.
 * <p>
 * Request:  type = "ping"   payload = any JSON (optional)
 * Response: type = "pong"   payload = { "serverTime": string, "echo": <payload> }
 */
public final class PingHandler implements MessageHandler {
    @Override
    public String messageType() {
        return "ping";
    }

    @Override
    public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
        Json payload = Json.object(java.util.Map.of(
                "serverTime", Json.of(Instant.now().toString()),
                "echo", request.getPayload() == null ? Json.nullValue() : request.getPayload()
        ));
        return Optional.of(new MessageEnvelope(
                "pong",
                request.getRequestId(),
                payload
        ));
    }
}
