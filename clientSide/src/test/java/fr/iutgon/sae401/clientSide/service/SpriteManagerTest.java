package fr.iutgon.sae401.clientSide.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpriteManagerTest {

    @Test
    void get_unknownAsset_returnsNull() {
        assertNull(SpriteManager.get("unknown-category", "missing.png"));
    }

    @Test
    void getPlayerFrame_unknownReturnsNull() {
        assertNull(SpriteManager.getPlayerFrame(99, "unused"));
    }

    @Test
    void getBombFrame_unknownReturnsNull() {
        assertNull(SpriteManager.getBombFrame(999));
    }
}
