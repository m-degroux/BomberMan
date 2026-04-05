package fr.iutgon.sae401.clientSide.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalServerManagerTest {

    @AfterEach
    void tearDown() {
        LocalServerManager.exitLocalMode();
    }

    @Test
    void enterLocalMode_enablesLocalMode() {
        LocalServerManager.enterLocalMode();

        assertTrue(LocalServerManager.isLocalModeEnabled());
    }

    @Test
    void exitLocalMode_disablesLocalMode() {
        LocalServerManager.enterLocalMode();
        assertTrue(LocalServerManager.isLocalModeEnabled());

        LocalServerManager.exitLocalMode();

        assertFalse(LocalServerManager.isLocalModeEnabled());
    }

    @Test
    void ensureLocalServerRunning_whenLocalModeDisabled_throwsException() {
        LocalServerManager.exitLocalMode();

        assertThrows(IllegalStateException.class, () -> LocalServerManager.ensureLocalServerRunning(0));
    }
}
