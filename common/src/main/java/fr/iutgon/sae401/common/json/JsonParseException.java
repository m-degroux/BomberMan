package fr.iutgon.sae401.common.json;

/**
 * @brief Thrown when JSON text cannot be parsed.
 */
public class JsonParseException extends RuntimeException {
    private final int position;

    public JsonParseException(String message, int position) {
        super(message + " at position " + position);
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
