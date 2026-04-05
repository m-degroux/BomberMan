package fr.iutgon.sae401.common.json;

import java.math.BigDecimal;
import java.util.*;

/**
 * @brief Minimal in-house JSON value tree (nlohmann-like API).
 * <p>
 * Supported types: object, array, string, number (BigDecimal), boolean, null.
 * <p>
 * Main entry points:
 * - {@link #parse(String)}
 * - {@link #stringify()}
 * - {@link #at(String)} / {@link #at(int)}
 * - {@link #value(String, String)} / {@link #value(String, int)} / ...
 */
public final class Json {
    private static final Json NULL = new Json(Kind.NULL, null);
    private final Kind kind;
    private final Object value;
    private Json(Kind kind, Object value) {
        this.kind = kind;
        this.value = value;
    }

    /**
     * @param text JSON text
     * @return parsed JSON value
     * @throws JsonParseException if the input is not valid JSON
     * @brief Parse JSON text into a {@link Json} tree.
     */
    public static Json parse(String text) {
        return new JsonParser(text).parse();
    }

    /**
     * @brief Singleton JSON null value.
     */
    public static Json nullValue() {
        return NULL;
    }

    /**
     * @brief Create a JSON string value.
     */
    public static Json of(String value) {
        return new Json(Kind.STRING, Objects.requireNonNull(value, "value"));
    }

    /**
     * @brief Create a JSON boolean value.
     */
    public static Json of(boolean value) {
        return new Json(Kind.BOOLEAN, value);
    }

    /**
     * @brief Create a JSON number value.
     */
    public static Json of(BigDecimal value) {
        return new Json(Kind.NUMBER, Objects.requireNonNull(value, "value"));
    }

    /**
     * @brief Create a JSON number value from an int.
     */
    public static Json of(int value) {
        return new Json(Kind.NUMBER, BigDecimal.valueOf(value));
    }

    /**
     * @brief Create a JSON number value from a long.
     */
    public static Json of(long value) {
        return new Json(Kind.NUMBER, BigDecimal.valueOf(value));
    }

    /**
     * @brief Create a JSON number value from a double.
     */
    public static Json of(double value) {
        return new Json(Kind.NUMBER, BigDecimal.valueOf(value));
    }

    /**
     * @param values elements
     * @brief Create a JSON array.
     */
    public static Json array(List<Json> values) {
        return new Json(Kind.ARRAY, List.copyOf(values));
    }

    /**
     * @param values key/value pairs
     * @brief Create a JSON object.
     */
    public static Json object(Map<String, Json> values) {
        return new Json(Kind.OBJECT, Collections.unmodifiableMap(new LinkedHashMap<>(values)));
    }

    /**
     * @param value Java value
     * @return JSON value
     * @throws IllegalArgumentException if the input type is not supported
     * @brief Convert a supported Java value into a {@link Json} tree.
     * <p>
     * Supported inputs: {@link Json}, String, Boolean, Number, Map&lt;String,?&gt;, List&lt;?&gt;.
     */
    public static Json of(Object value) {
        if (value == null) {
            return nullValue();
        }
        if (value instanceof Json json) {
            return json;
        }
        if (value instanceof String s) {
            return of(s);
        }
        if (value instanceof Boolean b) {
            return of(b.booleanValue());
        }
        if (value instanceof Integer i) {
            return of(i.intValue());
        }
        if (value instanceof Long l) {
            return of(l.longValue());
        }
        if (value instanceof Double d) {
            return of(d.doubleValue());
        }
        if (value instanceof Float f) {
            return of(f.doubleValue());
        }
        if (value instanceof BigDecimal bd) {
            return of(bd);
        }
        if (value instanceof Number n) {
            return of(new BigDecimal(n.toString()));
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Json> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                if (!(key instanceof String)) {
                    throw new IllegalArgumentException("JSON object keys must be strings");
                }
                out.put((String) key, of(entry.getValue()));
            }
            return object(out);
        }
        if (value instanceof List<?> list) {
            List<Json> out = new ArrayList<>(list.size());
            for (Object element : list) {
                out.add(of(element));
            }
            return array(out);
        }

        throw new IllegalArgumentException("Unsupported JSON value type: " + value.getClass().getName());
    }

    public static Json emptyObject() {
        return object(Collections.emptyMap());
    }

    /**
     * @brief Convenience: empty JSON array ([]).
     */
    public static Json emptyArray() {
        return array(Collections.emptyList());
    }

    /**
     * @brief Return the JSON kind (object/array/string/number/boolean/null).
     */
    public Kind kind() {
        return kind;
    }

    /**
     * @brief @return true if this value is an object.
     */
    public boolean isObject() {
        return kind == Kind.OBJECT;
    }

    /**
     * @brief @return true if this value is an array.
     */
    public boolean isArray() {
        return kind == Kind.ARRAY;
    }

    /**
     * @brief @return true if this value is a string.
     */
    public boolean isString() {
        return kind == Kind.STRING;
    }

    /**
     * @brief @return true if this value is a number.
     */
    public boolean isNumber() {
        return kind == Kind.NUMBER;
    }

    /**
     * @brief @return true if this value is a boolean.
     */
    public boolean isBoolean() {
        return kind == Kind.BOOLEAN;
    }

    /**
     * @brief @return true if this value is null.
     */
    public boolean isNull() {
        return kind == Kind.NULL;
    }

    /**
     * @throws JsonTypeException if not an object
     * @brief Cast this value to an object.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Json> asObject() {
        if (!isObject()) {
            throw new JsonTypeException("Expected object but was " + kind);
        }
        return (Map<String, Json>) value;
    }

    /**
     * @throws JsonTypeException if not an array
     * @brief Cast this value to an array.
     */
    @SuppressWarnings("unchecked")
    public List<Json> asArray() {
        if (!isArray()) {
            throw new JsonTypeException("Expected array but was " + kind);
        }
        return (List<Json>) value;
    }

    /**
     * @throws JsonTypeException if not a string
     * @brief Cast this value to a string.
     */
    public String asString() {
        if (!isString()) {
            throw new JsonTypeException("Expected string but was " + kind);
        }
        return (String) value;
    }

    /**
     * @throws JsonTypeException if not a number
     * @brief Cast this value to a number.
     */
    public BigDecimal asNumber() {
        if (!isNumber()) {
            throw new JsonTypeException("Expected number but was " + kind);
        }
        return (BigDecimal) value;
    }

    /**
     * @throws JsonTypeException if not a boolean
     * @brief Cast this value to a boolean.
     */
    public boolean asBoolean() {
        if (!isBoolean()) {
            throw new JsonTypeException("Expected boolean but was " + kind);
        }
        return (boolean) value;
    }

    /**
     * @param key object key
     * @return child value
     * @throws JsonTypeException      if this is not an object
     * @throws NoSuchElementException if the key does not exist
     * @brief Strict object lookup.
     * <p>
     * Equivalent to nlohmann::json::at(key).
     */
    public Json at(String key) {
        Json child = asObject().get(key);
        if (child == null) {
            throw new NoSuchElementException("Missing key: " + key);
        }
        return child;
    }

    /**
     * @param index element index
     * @return element
     * @throws JsonTypeException         if this is not an array
     * @throws IndexOutOfBoundsException if the index is out of bounds
     * @brief Strict array access.
     */
    public Json at(int index) {
        List<Json> array = asArray();
        if (index < 0 || index >= array.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds (size=" + array.size() + ")");
        }
        return array.get(index);
    }

    /**
     * @brief Optional object lookup (never throws for missing key).
     */
    public Optional<Json> get(String key) {
        if (!isObject()) {
            return Optional.empty();
        }
        return Optional.ofNullable(asObject().get(key));
    }

    /**
     * @brief @return true if this is an object and it contains the given key.
     */
    public boolean contains(String key) {
        return isObject() && asObject().containsKey(key);
    }

    /**
     * @throws JsonTypeException if this is not an object
     * @brief Object lookup with default (works for any JSON type).
     * <p>
     * Returns default when key is missing or value is null.
     */
    public Json value(String key, Json defaultValue) {
        if (!isObject()) {
            throw new JsonTypeException("Expected object but was " + kind);
        }
        Json child = asObject().get(key);
        return child == null || child.isNull() ? defaultValue : child;
    }

    /**
     * @brief String lookup with default.
     * <p>
     * Returns default when key is missing or value is null.
     * Throws when present but not a string.
     */
    public String value(String key, String defaultValue) {
        if (!isObject()) {
            throw new JsonTypeException("Expected object but was " + kind);
        }
        Json child = asObject().get(key);
        if (child == null || child.isNull()) {
            return defaultValue;
        }
        if (!child.isString()) {
            throw new JsonTypeException("Expected string at key '" + key + "' but was " + child.kind());
        }
        return child.asString();
    }

    /**
     * @brief Int lookup with default.
     * <p>
     * Returns default when key is missing or value is null.
     * Throws when present but not an exact int number.
     */
    public int value(String key, int defaultValue) {
        if (!isObject()) {
            throw new JsonTypeException("Expected object but was " + kind);
        }
        Json child = asObject().get(key);
        if (child == null || child.isNull()) {
            return defaultValue;
        }
        if (!child.isNumber()) {
            throw new JsonTypeException("Expected number at key '" + key + "' but was " + child.kind());
        }
        try {
            return child.asNumber().intValueExact();
        } catch (ArithmeticException e) {
            throw new JsonTypeException("Expected int number at key '" + key + "' but was " + child.asNumber());
        }
    }

    /**
     * @brief Long lookup with default.
     * <p>
     * Returns default when key is missing or value is null.
     * Throws when present but not an exact long number.
     */
    public long value(String key, long defaultValue) {
        if (!isObject()) {
            throw new JsonTypeException("Expected object but was " + kind);
        }
        Json child = asObject().get(key);
        if (child == null || child.isNull()) {
            return defaultValue;
        }
        if (!child.isNumber()) {
            throw new JsonTypeException("Expected number at key '" + key + "' but was " + child.kind());
        }
        try {
            return child.asNumber().longValueExact();
        } catch (ArithmeticException e) {
            throw new JsonTypeException("Expected long number at key '" + key + "' but was " + child.asNumber());
        }
    }

    /**
     * @brief Boolean lookup with default.
     * <p>
     * Returns default when key is missing or value is null.
     * Throws when present but not a boolean.
     */
    public boolean value(String key, boolean defaultValue) {
        if (!isObject()) {
            throw new JsonTypeException("Expected object but was " + kind);
        }
        Json child = asObject().get(key);
        if (child == null || child.isNull()) {
            return defaultValue;
        }
        if (!child.isBoolean()) {
            throw new JsonTypeException("Expected boolean at key '" + key + "' but was " + child.kind());
        }
        return child.asBoolean();
    }

    /**
     * @throws JsonTypeException if not a number or not an exact int
     * @brief Strict int conversion.
     */
    public int getInt() {
        try {
            return asNumber().intValueExact();
        } catch (ArithmeticException e) {
            throw new JsonTypeException("Expected int number but was " + asNumber());
        }
    }

    /**
     * @throws JsonTypeException if not a number or not an exact long
     * @brief Strict long conversion.
     */
    public long getLong() {
        try {
            return asNumber().longValueExact();
        } catch (ArithmeticException e) {
            throw new JsonTypeException("Expected long number but was " + asNumber());
        }
    }

    /**
     * @throws JsonTypeException if not a number
     * @brief Convert number to double.
     */
    public double getDouble() {
        return asNumber().doubleValue();
    }

    /**
     * @throws JsonTypeException if not a string
     * @brief Strict string conversion.
     */
    public String getString() {
        return asString();
    }

    /**
     * @throws JsonTypeException if not a boolean
     * @brief Strict boolean conversion.
     */
    public boolean getBoolean() {
        return asBoolean();
    }
    
    /**
     * @throws JsonTypeException if not a number
     * @brief Convert number to float.
     */
    public float getFloat() {
    	return asNumber().floatValue();
    }

    /**
     * @brief Serialize this value to compact JSON text.
     */
    public String stringify() {
        StringBuilder sb = new StringBuilder();
        JsonWriter.write(this, sb);
        return sb.toString();
    }

    @Override
    public String toString() {
        return stringify();
    }

    public enum Kind {
        OBJECT,
        ARRAY,
        STRING,
        NUMBER,
        BOOLEAN,
        NULL
    }
}
