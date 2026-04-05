package fr.iutgon.sae401.common.protocol;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.json.JsonTypeException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @brief Pure helper for mapping {@link MessageEnvelope} <-> JSON text.
 * <p>
 * Wire format (JSON object):
 * - type: string (required)
 * - requestId: string|null (optional)
 * - payload: any JSON (optional)
 */
public final class MessageEnvelopeJson {
    private MessageEnvelopeJson() {
    }

    public static String stringify(MessageEnvelope envelope) {
        return toJson(envelope).stringify();
    }

    public static MessageEnvelope parse(String jsonText) {
        return fromJson(Json.parse(jsonText));
    }

    public static Json toJson(MessageEnvelope envelope) {
        Map<String, Json> obj = new LinkedHashMap<>();
        obj.put("type", Json.of(envelope.getType()));
        obj.put("requestId", envelope.getRequestId() == null ? Json.nullValue() : Json.of(envelope.getRequestId()));
        obj.put("payload", envelope.getPayload() == null ? Json.nullValue() : envelope.getPayload());
        return Json.object(obj);
    }

    public static MessageEnvelope fromJson(Json json) {
        if (!json.isObject()) {
            throw new JsonTypeException("Envelope must be a JSON object");
        }

        String type = json.at("type").getString();
        String requestId = null;
        if (json.contains("requestId")) {
            Json req = json.at("requestId");
            requestId = req.isNull() ? null : req.getString();
        }
        Json payload = json.contains("payload") ? json.at("payload") : Json.nullValue();
        return new MessageEnvelope(type, requestId, payload);
    }
}
