package fr.iutgon.sae401.common.json;

/**
 * @brief Thrown when a {@link Json} value is accessed with the wrong expected type.
 */
public class JsonTypeException extends RuntimeException {
    public JsonTypeException(String message) {
        super(message);
    }
}
