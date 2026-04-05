package fr.iutgon.sae401.serverSide.server.handlers.system;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;

import java.util.Optional;

/**
 * Generic debugging handler.
 * <p>
 * Request:  type = "echo"   payload = any JSON
 * Response: type = "echo"   payload = exactly the same JSON
 */
public final class EchoHandler implements MessageHandler {
    @Override
    public String messageType() {
        return "echo";
    }

    @Override
    public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
        Json payload = request.getPayload() == null ? Json.nullValue() : request.getPayload();
        return Optional.of(new MessageEnvelope("echo", request.getRequestId(), payload));
    }
}
