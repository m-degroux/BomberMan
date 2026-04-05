package fr.iutgon.sae401.common.protocol;

import fr.iutgon.sae401.common.json.Json;

import java.util.Objects;

/**
 * @brief Transport-level message wrapper.
 * <p>
 * The server routes messages using {@link #getType()}.
 * The {@link #getPayload()} is kept as a {@link Json} tree to stay dependency-free
 * and schema-agnostic.
 */
public final class MessageEnvelope {
    private final String type;
    private final String requestId;
    private final Json payload;

    public MessageEnvelope(String type, String requestId, Json payload) {
        this.type = Objects.requireNonNull(type, "type");
        this.requestId = requestId;
        this.payload = payload;
    }

    public static MessageEnvelope of(String type, Json payload) {
        return new MessageEnvelope(type, null, payload);
    }

    public String getType() {
        return type;
    }

    public String getRequestId() {
        return requestId;
    }

    public Json getPayload() {
        return payload;
    }
}
