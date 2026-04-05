package fr.iutgon.sae401.serverSide.server.handlers.game;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.game.GameEngine;
import fr.iutgon.sae401.serverSide.server.ClientContext;
import fr.iutgon.sae401.serverSide.server.MessageHandler;

import java.util.Objects;
import java.util.Optional;

/**
 * @brief Generic input handler.
 * <p>
 * Request:  type = "input" payload = game-defined JSON
 * Response: none
 */
public final class InputHandler implements MessageHandler {
    private final GameEngine engine;

    public InputHandler(GameEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public String messageType() {
        return "input";
    }

    @Override
    public Optional<MessageEnvelope> handle(MessageEnvelope request, ClientContext client) {
        engine.onInput(client.clientId(), request.getPayload() == null ? fr.iutgon.sae401.common.json.Json.nullValue() : request.getPayload());
        return Optional.empty();
    }
}
