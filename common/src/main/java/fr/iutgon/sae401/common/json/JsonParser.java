package fr.iutgon.sae401.common.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @brief Recursive-descent parser for JSON text into {@link Json}.
 * <p>
 * Grammar support: objects, arrays, strings with escapes, numbers, booleans, null.
 * @note This is intentionally minimal and dependency-free.
 */
final class JsonParser {
    private final String text;
    private int i;

    JsonParser(String text) {
        this.text = text;
        this.i = 0;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return 10 + (c - 'a');
        }
        if (c >= 'A' && c <= 'F') {
            return 10 + (c - 'A');
        }
        return -1;
    }

    Json parse() {
        skipWs();
        Json value = parseValue();
        skipWs();
        if (!eof()) {
            throw error("Unexpected trailing characters");
        }
        return value;
    }

    private Json parseValue() {
        skipWs();
        if (eof()) {
            throw error("Unexpected end of input");
        }
        char c = peek();
        switch (c) {
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '"':
                return Json.of(parseString());
            case 't':
                consumeLiteral("true");
                return Json.of(true);
            case 'f':
                consumeLiteral("false");
                return Json.of(false);
            case 'n':
                consumeLiteral("null");
                return Json.nullValue();
            default:
                if (c == '-' || isDigit(c)) {
                    return Json.of(parseNumber());
                }
                throw error("Unexpected character: '" + c + "'");
        }
    }

    private Json parseObject() {
        expect('{');
        skipWs();
        Map<String, Json> map = new LinkedHashMap<>();
        if (tryConsume('}')) {
            return Json.object(map);
        }
        while (true) {
            skipWs();
            String key = parseString();
            skipWs();
            expect(':');
            Json value = parseValue();
            map.put(key, value);
            skipWs();
            if (tryConsume('}')) {
                break;
            }
            expect(',');
        }
        return Json.object(map);
    }

    private Json parseArray() {
        expect('[');
        skipWs();
        List<Json> values = new ArrayList<>();
        if (tryConsume(']')) {
            return Json.array(values);
        }
        while (true) {
            Json value = parseValue();
            values.add(value);
            skipWs();
            if (tryConsume(']')) {
                break;
            }
            expect(',');
        }
        return Json.array(values);
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (!eof()) {
            char c = next();
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                if (eof()) {
                    throw error("Unterminated escape sequence");
                }
                char esc = next();
                switch (esc) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        sb.append(parseUnicodeEscape());
                        break;
                    default:
                        throw error("Invalid escape character: '" + esc + "'");
                }
                continue;
            }
            if (c < 0x20) {
                throw error("Unescaped control character in string");
            }
            sb.append(c);
        }
        throw error("Unterminated string");
    }

    private char parseUnicodeEscape() {
        int codePoint = 0;
        for (int k = 0; k < 4; k++) {
            if (eof()) {
                throw error("Unterminated unicode escape");
            }
            char c = next();
            int hex = hexValue(c);
            if (hex < 0) {
                throw error("Invalid hex digit in unicode escape: '" + c + "'");
            }
            codePoint = (codePoint << 4) | hex;
        }
        return (char) codePoint;
    }

    private BigDecimal parseNumber() {
        int start = i;
        if (peek() == '-') {
            i++;
        }
        if (eof()) {
            throw error("Invalid number");
        }
        if (peek() == '0') {
            i++;
        } else {
            if (!isDigit(peek())) {
                throw error("Invalid number");
            }
            while (!eof() && isDigit(peek())) {
                i++;
            }
        }
        if (!eof() && peek() == '.') {
            i++;
            if (eof() || !isDigit(peek())) {
                throw error("Invalid number fraction");
            }
            while (!eof() && isDigit(peek())) {
                i++;
            }
        }
        if (!eof() && (peek() == 'e' || peek() == 'E')) {
            i++;
            if (!eof() && (peek() == '+' || peek() == '-')) {
                i++;
            }
            if (eof() || !isDigit(peek())) {
                throw error("Invalid number exponent");
            }
            while (!eof() && isDigit(peek())) {
                i++;
            }
        }
        String token = text.substring(start, i);
        try {
            return new BigDecimal(token);
        } catch (NumberFormatException e) {
            throw error("Invalid number: " + token);
        }
    }

    private void consumeLiteral(String literal) {
        for (int k = 0; k < literal.length(); k++) {
            if (eof() || next() != literal.charAt(k)) {
                throw error("Expected literal: " + literal);
            }
        }
    }

    private void skipWs() {
        while (!eof()) {
            char c = peek();
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                i++;
                continue;
            }
            break;
        }
    }

    private void expect(char c) {
        if (eof() || next() != c) {
            throw error("Expected '" + c + "'");
        }
    }

    private boolean tryConsume(char c) {
        if (!eof() && peek() == c) {
            i++;
            return true;
        }
        return false;
    }

    private char peek() {
        return text.charAt(i);
    }

    private char next() {
        return text.charAt(i++);
    }

    private boolean eof() {
        return i >= text.length();
    }

    private JsonParseException error(String message) {
        return new JsonParseException(message, i);
    }
}
