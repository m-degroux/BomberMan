package fr.iutgon.sae401.serverSide.server.handlers.system;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;

import java.util.Optional;

/**
 * Minimal server info handler.
 * <p>
 * Request:  type = "server_info" payload = ignored
 * Response: type = "server_info" payload = { "serverName": string, "protocolVersion": string }
 */
public final class ServerInfoHandler implements MessageHandler {
    private final String serverName;
    private final String protocolVersion;

    public ServerInfoHandler(String serverName, String protocolVersion) {
        this.serverName = serverName;
        this.protocolVersion = protocolVersion;
    }

    @Override
    public String messageType() {
        return "server_info";
    }

    @Override
    public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
        Json payload = Json.object(java.util.Map.of(
                "serverName", Json.of(serverName),
                "protocolVersion", Json.of(protocolVersion)
        ));
        return Optional.of(new MessageEnvelope("server_info", request.getRequestId(), payload));
    }
}
