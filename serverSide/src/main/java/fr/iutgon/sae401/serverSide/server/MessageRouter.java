package fr.iutgon.sae401.serverSide.server;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @brief Routes {@link MessageEnvelope} to a registered {@link MessageHandler}.
 */
public final class MessageRouter {
    private final Map<String, MessageHandler> handlersByType;

    /**
     * @param handlers handlers to register (iteration order does not matter)
     * @throws IllegalArgumentException if two handlers share the same message type
     * @brief Create a router and pre-register a set of handlers.
     */
    public MessageRouter(Iterable<MessageHandler> handlers) {
        Objects.requireNonNull(handlers, "handlers");
        this.handlersByType = new HashMap<>();
        for (MessageHandler handler : handlers) {
            register(handler);
        }
    }

    /**
     * @param handler handler instance
     * @throws IllegalArgumentException if a handler is already registered for the same type
     * @brief Register a handler for its {@link MessageHandler#messageType()}.
     */
    public void register(MessageHandler handler) {
        Objects.requireNonNull(handler, "handler");
        String type = Objects.requireNonNull(handler.messageType(), "handler.messageType()");
        MessageHandler previous = handlersByType.putIfAbsent(type, handler);
        if (previous != null) {
            throw new IllegalArgumentException("Handler already registered for type: " + type);
        }
    }

    /**
     * @param request incoming request
     * @param client  client context
     * @return handler response, or empty if no handler is registered for this type
     * @brief Route a request to the appropriate handler.
     */
    public Optional<MessageEnvelope> route(MessageEnvelope request, ClientContext client) {
        MessageHandler handler = handlersByType.get(request.getType());
        if (handler == null) {
            return Optional.empty();
        }
        return handler.handle(request, client);
    }
}
