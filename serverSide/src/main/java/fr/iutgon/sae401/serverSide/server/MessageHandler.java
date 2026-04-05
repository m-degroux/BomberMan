package fr.iutgon.sae401.serverSide.server;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;

import java.util.Optional;

public interface MessageHandler {
    /**
     * @brief Message type handled by this handler (e.g. "ping", "move", "join").
     */
    String messageType();

    /**
     * @param request incoming envelope
     * @param client  client context, allows sending out-of-band messages
     * @return response envelope if any
     * @brief Handle a request and optionally return a response.
     */
    Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client);
}
