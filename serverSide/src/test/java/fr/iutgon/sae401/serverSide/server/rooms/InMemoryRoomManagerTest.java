package fr.iutgon.sae401.serverSide.server.rooms;

import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class InMemoryRoomManagerTest {
    @Test
    void joinCreatesRoomAndTracksMembership() {
        RoomManager rooms = new InMemoryRoomManager();
        RoomId lobby = new RoomId("lobby");
        ClientId c1 = new ClientId("c1");

        rooms.join(lobby, c1);

        assertEquals(lobby, rooms.roomOf(c1).orElseThrow());
        assertTrue(rooms.members(lobby).contains(c1));
        assertTrue(rooms.rooms().contains(lobby));
    }

    @Test
    void joinMovesClientBetweenRooms() {
        RoomManager rooms = new InMemoryRoomManager();
        RoomId lobby = new RoomId("lobby");
        RoomId match = new RoomId("match-1");
        ClientId c1 = new ClientId("c1");

        rooms.join(lobby, c1);
        rooms.join(match, c1);

        assertEquals(match, rooms.roomOf(c1).orElseThrow());
        assertFalse(rooms.members(lobby).contains(c1));
        assertTrue(rooms.members(match).contains(c1));
    }

    @Test
    void leaveRemovesMembershipAndDeletesEmptyRoom() {
        RoomManager rooms = new InMemoryRoomManager();
        RoomId lobby = new RoomId("lobby");
        ClientId c1 = new ClientId("c1");

        rooms.join(lobby, c1);
        rooms.leave(c1);

        assertTrue(rooms.roomOf(c1).isEmpty());
        assertTrue(rooms.members(lobby).isEmpty());
        assertFalse(rooms.rooms().contains(lobby));
    }
}
