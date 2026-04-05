package fr.iutgon.sae401.common.protocol;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.json.JsonBinary;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Binary wire codec for {@link MessageEnvelope}.
 * <p>
 * This replaces JSON text on the network while keeping the rest of the codebase
 * working with {@link Json} trees.
 *
 * Format (versioned):
 * - [u8 version=1]
 * - [string type]
 * - [u8 hasRequestId]
 * - [string requestId] (if present)
 * - [json payload] (always present; null payload is encoded as JSON null)
 */
public final class MessageEnvelopeBinary {
	private static final byte VERSION = 1;
	private static final int MAX_STRING_LEN = 1_000_000;

	private MessageEnvelopeBinary() {
	}

	public static byte[] toBytes(MessageEnvelope envelope) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(256);
		out.write(VERSION);
		writeString(out, envelope.getType());
		String requestId = envelope.getRequestId();
		if (requestId == null) {
			out.write(0);
		} else {
			out.write(1);
			writeString(out, requestId);
		}
		Json payload = envelope.getPayload();
		out.writeBytes(JsonBinary.toBytes(payload == null ? Json.nullValue() : payload));
		return out.toByteArray();
	}

	public static MessageEnvelope fromBytes(byte[] bytes) {
		return fromBuffer(ByteBuffer.wrap(bytes));
	}

	public static MessageEnvelope fromBuffer(ByteBuffer buffer) {
		byte version = buffer.get();
		if (version != VERSION) {
			throw new IllegalArgumentException("Unsupported envelope version: " + version);
		}
		String type = readString(buffer);
		boolean hasRequestId = buffer.get() != 0;
		String requestId = hasRequestId ? readString(buffer) : null;
		Json payload = JsonBinary.read(buffer);
		return new MessageEnvelope(type, requestId, payload);
	}

	private static void writeString(ByteArrayOutputStream out, String s) {
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		writeInt(out, bytes.length);
		out.writeBytes(bytes);
	}

	private static String readString(ByteBuffer buffer) {
		int len = buffer.getInt();
		if (len < 0 || len > MAX_STRING_LEN) {
			throw new IllegalArgumentException("Invalid string length: " + len);
		}
		byte[] bytes = new byte[len];
		buffer.get(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static void writeInt(ByteArrayOutputStream out, int v) {
		out.write((v >>> 24) & 0xFF);
		out.write((v >>> 16) & 0xFF);
		out.write((v >>> 8) & 0xFF);
		out.write(v & 0xFF);
	}
}
