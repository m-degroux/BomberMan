package fr.iutgon.sae401.common.model.gameplay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameConfigTest {

    @Test
    void defaultConstructor_setsExpectedDefaults() {
        GameConfig config = new GameConfig();

        assertEquals(15, config.getWidth());
        assertEquals(13, config.getHeight());
        assertEquals(1, config.getInitialHealth());
        assertEquals(1, config.getMaxBombs());
        assertEquals(2, config.getBombRange());
        assertEquals(2000, config.getBombTimer());
        assertEquals(2000, config.getBombCooldownMs());
        assertEquals(0.6f, config.getDestructibleDensity());
    }

    @Test
    void explicitConstructor_acceptsFloatDensity() {
        GameConfig config = new GameConfig(10, 11, 2, 3, 4, 1500, 0.2f);

        assertEquals(10, config.getWidth());
        assertEquals(11, config.getHeight());
        assertEquals(0.2f, config.getDestructibleDensity());
    }

    @Test
    void explicitConstructor_withPercentageDensityConvertsCorrectly() {
        GameConfig config = new GameConfig(7, 8, 2, 3, 1, 1200, 30);

        assertEquals(0.3f, config.getDestructibleDensity());
    }

    @Test
    void constructor_throwsWhenDensityTooSmall() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameConfig(5, 5, 1, 1, 1, 1000, -0.1f));
    }

    @Test
    void constructor_throwsWhenDensityTooLarge() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameConfig(5, 5, 1, 1, 1, 1000, 1.1f));
    }
}
