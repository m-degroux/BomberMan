package fr.iutgon.sae401.serverSide.server.rooms;

import java.util.Objects;

/**
 * @brief Room identifier.
 */
public final class RoomId {
    private final String value;

    public RoomId(String value) {
        this.value = Objects.requireNonNull(value, "value").trim();
        if (this.value.isEmpty()) {
            throw new IllegalArgumentException("RoomId cannot be empty");
        }
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RoomId roomId = (RoomId) o;
        return value.equals(roomId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
