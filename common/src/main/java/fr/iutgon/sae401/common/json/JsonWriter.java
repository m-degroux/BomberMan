package fr.iutgon.sae401.common.json;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @brief JSON serializer for {@link Json}.
 * <p>
 * Produces compact JSON without extra whitespace.
 */
final class JsonWriter {
    static void write(Json json, StringBuilder out) {
        switch (json.kind()) {
            case NULL -> out.append("null");
            case BOOLEAN -> out.append(json.asBoolean() ? "true" : "false");
            case NUMBER -> writeNumber(json.asNumber(), out);
            case STRING -> writeString(json.asString(), out);
            case ARRAY -> writeArray(json.asArray(), out);
            case OBJECT -> writeObject(json.asObject(), out);
        }
    }

    private static void writeObject(Map<String, Json> object, StringBuilder out) {
        out.append('{');
        Iterator<Map.Entry<String, Json>> it = object.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Json> entry = it.next();
            writeString(entry.getKey(), out);
            out.append(':');
            write(entry.getValue(), out);
            if (it.hasNext()) {
                out.append(',');
            }
        }
        out.append('}');
    }

    private static void writeArray(List<Json> array, StringBuilder out) {
        out.append('[');
        for (int i = 0; i < array.size(); i++) {
            write(array.get(i), out);
            if (i + 1 < array.size()) {
                out.append(',');
            }
        }
        out.append(']');
    }

    private static void writeNumber(BigDecimal number, StringBuilder out) {
        out.append(number.toPlainString());
    }

    private static void writeString(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append("\\u");
                        String hex = Integer.toHexString(c);
                        out.append("0".repeat(4 - hex.length()));
                        out.append(hex);
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
