package fr.iutgon.sae401.serverSide.server.runtime.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArgsConfigLoaderTest {
    @Test
    void usesDefaultsWhenNoArgs() {
        ServerRuntimeConfig cfg = ArgsConfigLoader.load(new String[0]);
        assertEquals(7777, cfg.server().port());
        assertEquals(7777, cfg.server().udpPort());
        assertEquals(32, cfg.server().maxClients());
        assertEquals(60, cfg.game().tickRateHz());
        assertEquals(20, cfg.game().netRateHz());
    }

    @Test
    void parsesAllSupportedFlags() {
        ServerRuntimeConfig cfg = ArgsConfigLoader.load(new String[]{
                "--port", "9000",
                "--maxClients", "10",
                "--tickHz", "30",
                "--netHz", "15"
        });
        assertEquals(9000, cfg.server().port());
        assertEquals(9000, cfg.server().udpPort());
        assertEquals(10, cfg.server().maxClients());
        assertEquals(30, cfg.game().tickRateHz());
        assertEquals(15, cfg.game().netRateHz());
    }

    @Test
    void parsesExplicitUdpPort() {
        ServerRuntimeConfig cfg = ArgsConfigLoader.load(new String[]{
                "--port", "9000",
                "--udpPort", "9100"
        });
        assertEquals(9000, cfg.server().port());
        assertEquals(9100, cfg.server().udpPort());
    }

    @Test
    void rejectsUnknownFlag() {
        assertThrows(IllegalArgumentException.class, () -> ArgsConfigLoader.load(new String[]{"--nope", "1"}));
    }

    @Test
    void rejectsMissingValue() {
        assertThrows(IllegalArgumentException.class, () -> ArgsConfigLoader.load(new String[]{"--port"}));
    }
}
