package fr.iutgon.sae401.common.model.entity;

import fr.iutgon.sae401.common.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PositionTest {

    @Test
    void add_movesByDirection() {
        Position origin = new Position(2, 3);
        Position moved = origin.add(Direction.RIGHT);

        assertEquals(new Position(3, 3), moved);
        assertEquals(new Position(2, 2), origin.add(Direction.UP));
    }

    @Test
    void distance_returnsManhattanDistance() {
        Position a = new Position(1, 1);
        Position b = new Position(4, 5);

        assertEquals(7, a.distance(b));
        assertEquals(7, b.distance(a));
    }

    @Test
    void equalsAndHashCode_areBasedOnCoordinates() {
        Position a = new Position(5, 7);
        Position b = new Position(5, 7);
        Position c = new Position(5, 8);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void toJson_andFromJson_roundTrip() {
        Position original = new Position(8, 9);
        Json json = original.toJson();

        Position restored = Position.fromJson(json);

        assertEquals(original, restored);
        assertEquals(8, json.at("x").getInt());
        assertEquals(9, json.at("y").getInt());
    }

    @Test
    void fromJson_throwsWhenJsonIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Position.fromJson(Json.nullValue()));
    }
}
