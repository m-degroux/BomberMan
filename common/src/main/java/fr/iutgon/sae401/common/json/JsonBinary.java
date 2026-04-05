package fr.iutgon.sae401.common.json;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact binary encoding for the in-house {@link Json} tree.
 * <p>
 * This is intentionally minimal and dependency-free.
 *
 * Format:
 * - value := [tag][data]
 * - tag (byte): 0=null, 1=false, 2=true, 3=number, 4=string, 5=array, 6=object
 * - string := [int32 byteLen][utf8 bytes]
 * - number := encoded as string (BigDecimal) using the string format
 * - array  := [int32 count][value...]
 * - object := [int32 count][string key][value...]
 */
public final class JsonBinary {
	private static final int MAX_BLOB_LEN = 10_000_000;
	private static final int MAX_CONTAINER_COUNT = 1_000_000;

	private static final byte TAG_NULL = 0;
	private static final byte TAG_FALSE = 1;
	private static final byte TAG_TRUE = 2;
	private static final byte TAG_NUMBER = 3;
	private static final byte TAG_STRING = 4;
	private static final byte TAG_ARRAY = 5;
	private static final byte TAG_OBJECT = 6;

	private JsonBinary() {
	}

	public static byte[] toBytes(Json value) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(256);
		write(out, value == null ? Json.nullValue() : value);
		return out.toByteArray();
	}

	public static Json fromBytes(byte[] bytes) {
		return read(ByteBuffer.wrap(bytes));
	}

	public static Json read(ByteBuffer buffer) {
		byte tag = buffer.get();
		return switch (tag) {
			case TAG_NULL -> Json.nullValue();
			case TAG_FALSE -> Json.of(false);
			case TAG_TRUE -> Json.of(true);
			case TAG_STRING -> Json.of(readString(buffer));
			case TAG_NUMBER -> Json.of(new BigDecimal(readString(buffer)));
			case TAG_ARRAY -> {
				int count = readCount(buffer);
				java.util.ArrayList<Json> arr = new java.util.ArrayList<>(count);
				for (int i = 0; i < count; i++) {
					arr.add(read(buffer));
				}
				yield Json.array(arr);
			}
			case TAG_OBJECT -> {
				int count = readCount(buffer);
				Map<String, Json> obj = new LinkedHashMap<>(Math.min(count, 16));
				for (int i = 0; i < count; i++) {
					String key = readString(buffer);
					Json v = read(buffer);
					obj.put(key, v);
				}
				yield Json.object(obj);
			}
			default -> throw new IllegalArgumentException("Unknown JsonBinary tag: " + tag);
		};
	}

	private static void write(ByteArrayOutputStream out, Json value) {
		switch (value.kind()) {
			case NULL -> out.write(TAG_NULL);
			case BOOLEAN -> out.write(value.asBoolean() ? TAG_TRUE : TAG_FALSE);
			case STRING -> {
				out.write(TAG_STRING);
				writeString(out, value.asString());
			}
			case NUMBER -> {
				out.write(TAG_NUMBER);
				writeString(out, value.asNumber().toString());
			}
			case ARRAY -> {
				out.write(TAG_ARRAY);
				List<Json> arr = value.asArray();
				writeInt(out, arr.size());
				for (Json element : arr) {
					write(out, element == null ? Json.nullValue() : element);
				}
			}
			case OBJECT -> {
				out.write(TAG_OBJECT);
				Map<String, Json> obj = value.asObject();
				writeInt(out, obj.size());
				for (Map.Entry<String, Json> e : obj.entrySet()) {
					writeString(out, e.getKey());
					Json child = e.getValue();
					write(out, child == null ? Json.nullValue() : child);
				}
			}
		}
	}

	private static int readCount(ByteBuffer buffer) {
		int count = buffer.getInt();
		if (count < 0 || count > MAX_CONTAINER_COUNT) {
			throw new IllegalArgumentException("Invalid container count: " + count);
		}
		return count;
	}

	private static String readString(ByteBuffer buffer) {
		int len = buffer.getInt();
		if (len < 0 || len > MAX_BLOB_LEN) {
			throw new IllegalArgumentException("Invalid string length: " + len);
		}
		byte[] bytes = new byte[len];
		buffer.get(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static void writeString(ByteArrayOutputStream out, String s) {
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		writeInt(out, bytes.length);
		out.writeBytes(bytes);
	}

	private static void writeInt(ByteArrayOutputStream out, int v) {
		out.write((v >>> 24) & 0xFF);
		out.write((v >>> 16) & 0xFF);
		out.write((v >>> 8) & 0xFF);
		out.write(v & 0xFF);
	}
}
