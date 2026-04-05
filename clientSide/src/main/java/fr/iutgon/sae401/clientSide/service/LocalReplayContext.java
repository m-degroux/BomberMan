package fr.iutgon.sae401.clientSide.service;

import fr.iutgon.sae401.common.model.map.MapTheme;

import java.util.Optional;

/**
 * Stores local match settings to prefill the local CreateLobby screen
 * when a replay returns to configuration.
 */
public final class LocalReplayContext {
    private static volatile LocalGameSettings lastLocalGameSettings;
    private static volatile boolean restoreOnNextCreateLobby;

    private LocalReplayContext() {
    }

    public static synchronized void rememberLastLocalGameSettings(LocalGameSettings settings) {
        if (settings == null) {
            return;
        }
        lastLocalGameSettings = settings;
    }

    public static synchronized void requestRestoreOnNextCreateLobby() {
        restoreOnNextCreateLobby = true;
    }

    public static synchronized Optional<LocalGameSettings> consumeRequestedSettings() {
        if (!restoreOnNextCreateLobby || lastLocalGameSettings == null) {
            restoreOnNextCreateLobby = false;
            return Optional.empty();
        }
        restoreOnNextCreateLobby = false;
        return Optional.of(lastLocalGameSettings);
    }

    public static synchronized void clear() {
        lastLocalGameSettings = null;
        restoreOnNextCreateLobby = false;
    }

    public record LocalGameSettings(
            String roomName,
            String password,
            int width,
            int height,
            int initialHealth,
            int maxBombs,
            int bombRange,
            int bombCooldownMs,
            int destructibleDensity,
            MapTheme mapTheme
    ) {
        public LocalGameSettings {
            roomName = roomName == null ? "" : roomName;
            password = password == null ? "" : password;
            mapTheme = mapTheme == null ? MapTheme.CLASSIC : mapTheme;
        }
    }
}
