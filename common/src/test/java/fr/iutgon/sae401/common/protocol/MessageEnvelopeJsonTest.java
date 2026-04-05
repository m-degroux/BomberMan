package fr.iutgon.sae401.common.protocol;

import fr.iutgon.sae401.common.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageEnvelopeJsonTest {
    @Test
    void roundTripEnvelope() {
        MessageEnvelope msg = new MessageEnvelope(
                "ping",
                "r1",
                Json.object(java.util.Map.of("x", Json.of(1)))
        );

        String json = MessageEnvelopeJson.stringify(msg);
        MessageEnvelope decoded = MessageEnvelopeJson.parse(json);

        assertEquals("ping", decoded.getType());
        assertEquals("r1", decoded.getRequestId());
        assertEquals(1, decoded.getPayload().at("x").getInt());
    }
}
