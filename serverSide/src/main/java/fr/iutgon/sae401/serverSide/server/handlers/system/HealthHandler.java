package fr.iutgon.sae401.serverSide.server.handlers.system;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;

import java.time.Instant;
import java.util.Optional;

/**
 * Basic health check handler.
 * <p>
 * Request:  type = "health" payload = ignored
 * Response: type = "health" payload = { "ok": true, "serverTime": string }
 */
public final class HealthHandler implements MessageHandler {
    @Override
    public String messageType() {
        return "health";
    }

    @Override
    public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
        Json payload = Json.object(java.util.Map.of(
                "ok", Json.of(true),
                "serverTime", Json.of(Instant.now().toString())
        ));
        return Optional.of(new MessageEnvelope("health", request.getRequestId(), payload));
    }
}
