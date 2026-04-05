package fr.iutgon.sae401.clientSide.controller.game;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.clientSide.view.PlayerView;
import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.Tile;
import fr.iutgon.sae401.common.model.map.TileType;
import javafx.beans.property.DoubleProperty;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public final class GameStateSynchronizer {
    private static final int[] FALLBACK_SKIN_IDS = IntStream.rangeClosed(0, 34)
            .filter(id -> id != 6 && id != 9 && id != 30 && id != 32)
            .toArray();

    private final DoubleProperty tileSize;
    private final Pane entityLayer;
    private final Supplier<String> currentPlayerIdSupplier;
    private final Supplier<Boolean> cleanedUpSupplier;
    private final BooleanSupplier gameEndedSupplier;
    private final Consumer<Boolean> gameOverConsumer;
    private final IntConsumer healthUpdater;
    private final IntConsumer bombUpdater;
    private final Runnable redrawMap;
    private final GameEffectsSynchronizer effects;

    private final Map<String, RemotePlayer> players = new HashMap<>();
    private final Map<String, Integer> playerSkinMap = new HashMap<>();
    private final Map<String, Long> latestUdpSeqByRoom = new HashMap<>();
    private GameMap gameMap;
    private String lastMapJson;
    private boolean mapReady = false;
    private int maxPlayersSeenAlive = 0;

    public GameStateSynchronizer(
            DoubleProperty tileSize,
            Pane entityLayer,
            Pane bombLayer,
            Pane bonusLayer,
            Pane explosionLayer,
            Supplier<String> currentPlayerIdSupplier,
            Supplier<Boolean> cleanedUpSupplier,
            BooleanSupplier gameEndedSupplier,
            Consumer<Boolean> gameOverConsumer,
            IntConsumer healthUpdater,
            IntConsumer bombUpdater,
            Runnable redrawMap
    ) {
        this.tileSize = tileSize;
        this.entityLayer = entityLayer;
        this.currentPlayerIdSupplier = currentPlayerIdSupplier;
        this.cleanedUpSupplier = cleanedUpSupplier;
        this.gameEndedSupplier = gameEndedSupplier;
        this.gameOverConsumer = gameOverConsumer;
        this.healthUpdater = healthUpdater;
        this.bombUpdater = bombUpdater;
        this.redrawMap = redrawMap;
        this.effects = new GameEffectsSynchronizer(tileSize, bombLayer, bonusLayer, explosionLayer);
    }

    public void setInitialMap(GameMap gameMap) {
        this.gameMap = gameMap;
        this.mapReady = false;
        this.lastMapJson = null;
        this.maxPlayersSeenAlive = 0;
        this.players.clear();
        this.playerSkinMap.clear();
    }

    public GameMap gameMap() {
        return gameMap;
    }

    public boolean isMapReady() {
        return mapReady;
    }

    public Map<String, RemotePlayer> players() {
        return players;
    }

    public void clearLatestUdpSeqAll() {
        latestUdpSeqByRoom.clear();
    }

    public void clearLatestUdpSeqForRoom(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }
        latestUdpSeqByRoom.remove(roomId);
    }

    public void updateBombAnimations(long nowNs) {
        effects.updateBombAnimations(nowNs);
    }

    public void applyDelta(Json delta) {
        if (delta == null || !delta.isObject()) {
            return;
        }
        effects.setBombTimerTotalMs(delta.value("bombTimerMs", effects.bombTimerTotalMs()));

        if (delta.contains("playersUpsert") && delta.at("playersUpsert").isArray()) {
            applyPlayersUpsert(delta.at("playersUpsert"));
        }
        if (delta.contains("playersRemove") && delta.at("playersRemove").isArray()) {
            for (Json idVal : delta.at("playersRemove").asArray()) {
                if (idVal == null || !idVal.isString()) {
                    continue;
                }
                String id = idVal.getString();
                String currentPlayerId = currentPlayerIdSupplier.get();
                if (!cleanedUpSupplier.get() && !gameEndedSupplier.getAsBoolean() && currentPlayerId != null
                        && !currentPlayerId.isBlank() && currentPlayerId.equals(id) && maxPlayersSeenAlive >= 2) {
                    gameOverConsumer.accept(false);
                }
                RemotePlayer rp = players.remove(id);
                if (rp != null) {
                    entityLayer.getChildren().remove(rp.view.getImageView());
                }
            }
        }

        boolean mapChanged = false;
        if (this.mapReady && gameMap != null && delta.contains("tilesUpsert") && delta.at("tilesUpsert").isArray()) {
            for (Json t : delta.at("tilesUpsert").asArray()) {
                if (t == null || !t.isObject()) {
                    continue;
                }
                int x = t.value("x", -1);
                int y = t.value("y", -1);
                String type = t.value("type", "");
                if (x < 0 || y < 0 || type == null || type.isBlank()) {
                    continue;
                }
                try {
                    TileType tt = TileType.valueOf(type);
                    Position p = new Position(x, y);
                    if (!gameMap.isInside(p)) {
                        continue;
                    }
                    Tile existing = gameMap.getTile(p);
                    if (existing == null) {
                        gameMap.setTile(p, new Tile(tt));
                    } else {
                        existing.setType(tt);
                    }
                    mapChanged = true;
                } catch (Exception ignored) {
                }
            }
        }
        if (mapChanged) {
            redrawMap.run();
        }

        recomputeEndConditionsAndHud();
        effects.applyExplosions(Json.object(Map.of("explosions", delta.value("explosions", Json.emptyArray()))));
        effects.applyBombsDeltaOrSnapshot(delta);
        effects.applyBonusesDeltaOrSnapshot(delta);
    }

    public void applyUdpPlayerSnapshot(String roomId, long seq, Json playersArray) {
        if (playersArray == null || !playersArray.isArray()) {
            return;
        }
        String key = roomId == null ? "" : roomId;
        long previous = latestUdpSeqByRoom.getOrDefault(key, Long.MIN_VALUE);
        if (seq <= previous) {
            return;
        }
        latestUdpSeqByRoom.put(key, seq);
        applyPlayersUpsert(playersArray);
        recomputeEndConditionsAndHud();
    }

    public void applyState(Json state) {
        if (state == null || !state.isObject() || gameMap == null) {
            return;
        }
        effects.setBombTimerTotalMs(state.value("bombTimerMs", effects.bombTimerTotalMs()));
        int width = state.value("width", gameMap.getWidth());
        int height = state.value("height", gameMap.getHeight());

        String mapSignature = null;
        if (state.contains("map") && !state.at("map").isNull()) {
            Json mapVal = state.at("map");
            mapSignature = mapVal.isString() ? mapVal.getString() : mapVal.stringify();
        }
        if (mapSignature != null && !mapSignature.isBlank() && !mapSignature.equals(lastMapJson)) {
            try {
                gameMap = GameMap.fromJson(mapSignature);
                lastMapJson = mapSignature;
                redrawMap.run();
                this.mapReady = true;
            } catch (Exception ex) {
                App.LOGGER.log(new LogMessage("[GAME] Failed to parse server map; keeping current map. err=" + ex.getMessage(), LogLevel.WARNING));
            }
        } else if (width != gameMap.getWidth() || height != gameMap.getHeight()) {
            App.LOGGER.log(new LogMessage("[GAME] Server dimensions differ (" + width + "x" + height + ") but no usable map payload; keeping current map.", LogLevel.WARNING));
        }

        Set<String> present = new HashSet<>();
        String currentPlayerId = currentPlayerIdSupplier.get();
        int aliveCount = 0;
        boolean currentPlayerAlive = false;

        if (state.contains("players") && state.at("players").isArray()) {
            for (Json p : state.at("players").asArray()) {
                String id = p.value("id", "");
                if (id.isBlank()) {
                    continue;
                }
                int x = p.value("x", 0);
                int y = p.value("y", 0);
                int health = p.value("health", 0);
                int bombs = p.value("bombs", 0);
                boolean alive = p.value("alive", true);
                float speed = p.contains("speed") && p.at("speed").isNumber() ? p.at("speed").getFloat() : 1f;

                if (alive) {
                    aliveCount++;
                }
                if (id.equals(currentPlayerId) && alive) {
                    currentPlayerAlive = true;
                }

                present.add(id);
                RemotePlayer existing = players.get(id);
                if (existing == null) {
                    int skinId = resolveSkinIdForPlayer(id, p);
                    PlayerView view = new PlayerView(tileSize, skinId);
                    entityLayer.getChildren().add(view.getImageView());
                    existing = new RemotePlayer(view, x, y, skinId);
                    players.put(id, existing);
                } else {
                    existing.lastX = existing.x;
                    existing.lastY = existing.y;
                }
                if (existing.alive && !alive && !gameEndedSupplier.getAsBoolean()) {
                    existing.view.setDead();
                }

                existing.x = x;
                existing.y = y;
                existing.health = health;
                existing.currentBombs = bombs;
                existing.speed = speed;
                existing.alive = alive;
                existing.dir = computeDir(existing.lastX, existing.lastY, x, y, existing.dir);

                if (id.equals(currentPlayerId)) {
                    healthUpdater.accept(health);
                    bombUpdater.accept(bombs);
                }
            }
        }

        if (aliveCount > maxPlayersSeenAlive) {
            maxPlayersSeenAlive = aliveCount;
            App.LOGGER.log(new LogMessage("[GAME] Max players seen alive updated to: " + maxPlayersSeenAlive, LogLevel.INFO));
        }
        if (!gameEndedSupplier.getAsBoolean() && currentPlayerId != null && !currentPlayerId.isBlank() && present.contains(currentPlayerId)) {
            App.LOGGER.log(new LogMessage("[GAME] Checking end conditions: aliveCount=" + aliveCount + ", currentPlayerAlive=" + currentPlayerAlive, LogLevel.INFO));
            if (!currentPlayerAlive) {
                App.LOGGER.log(new LogMessage("[GAME] Current player died - showing defeat", LogLevel.INFO));
                gameOverConsumer.accept(false);
            } else if (aliveCount == 1 && maxPlayersSeenAlive >= 2) {
                App.LOGGER.log(new LogMessage("[GAME] Current player is last survivor - showing victory", LogLevel.INFO));
                gameOverConsumer.accept(true);
            }
        }

        Set<String> toRemove = new HashSet<>();
        for (String id : players.keySet()) {
            if (!present.contains(id)) {
                toRemove.add(id);
            }
        }
        for (String id : toRemove) {
            RemotePlayer rp = players.remove(id);
            if (rp != null) {
                entityLayer.getChildren().remove(rp.view.getImageView());
            }
        }

        effects.applyBombs(state);
        effects.applyExplosions(state);
        effects.applyBonuses(state);
    }

    private void applyPlayersUpsert(Json playersArray) {
        if (playersArray == null || !playersArray.isArray()) {
            return;
        }
        String currentPlayerId = currentPlayerIdSupplier.get();
        for (Json p : playersArray.asArray()) {
            if (p == null || !p.isObject()) {
                continue;
            }
            String id = p.value("id", "");
            if (id == null || id.isBlank()) {
                continue;
            }
            int x = p.value("x", 0);
            int y = p.value("y", 0);
            int health = p.value("health", 0);
            int bombs = p.value("bombs", 0);
            boolean alive = p.value("alive", true);
            float speed = p.contains("speed") && p.at("speed").isNumber() ? p.at("speed").getFloat() : 1f;

            RemotePlayer existing = players.get(id);
            if (existing == null) {
                int skinId = resolveSkinIdForPlayer(id, p);
                PlayerView view = new PlayerView(tileSize, skinId);
                entityLayer.getChildren().add(view.getImageView());
                existing = new RemotePlayer(view, x, y, skinId);
                players.put(id, existing);
            } else {
                existing.lastX = existing.x;
                existing.lastY = existing.y;
            }

            if (existing.alive && !alive && !gameEndedSupplier.getAsBoolean()) {
                existing.view.setDead();
            }
            existing.x = x;
            existing.y = y;
            existing.health = health;
            existing.currentBombs = bombs;
            existing.speed = speed;
            existing.alive = alive;
            existing.dir = computeDir(existing.lastX, existing.lastY, x, y, existing.dir);

            if (id.equals(currentPlayerId)) {
                healthUpdater.accept(health);
                bombUpdater.accept(bombs);
            }
        }
    }

    private int resolveSkinIdForPlayer(String playerId, Json playerJson) {
        if (playerId == null || playerId.isBlank()) {
            return fallbackSkinIdFor(playerId);
        }
        return playerSkinMap.computeIfAbsent(playerId, key -> {
            if (playerJson != null && playerJson.isObject() && playerJson.contains("skinId") && playerJson.at("skinId").isNumber()) {
                int serverSkinId = playerJson.at("skinId").getInt();
                if (isAllowedSkinId(serverSkinId)) {
                    return serverSkinId;
                }
            }
            return fallbackSkinIdFor(key);
        });
    }

    private static int fallbackSkinIdFor(String key) {
        if (FALLBACK_SKIN_IDS.length == 0) {
            return 0;
        }
        int hash = key == null ? 0 : key.hashCode();
        return FALLBACK_SKIN_IDS[Math.floorMod(hash, FALLBACK_SKIN_IDS.length)];
    }

    private static boolean isAllowedSkinId(int skinId) {
        return skinId >= 0 && skinId <= 34 && skinId != 6 && skinId != 9 && skinId != 30 && skinId != 32;
    }

    private void recomputeEndConditionsAndHud() {
        String currentPlayerId = currentPlayerIdSupplier.get();
        int aliveCount = 0;
        RemotePlayer me = currentPlayerId == null ? null : players.get(currentPlayerId);
        if (me != null) {
            healthUpdater.accept(me.health);
            bombUpdater.accept(me.currentBombs);
        }
        for (RemotePlayer rp : players.values()) {
            if (rp != null && rp.alive) {
                aliveCount++;
            }
        }
        if (aliveCount > maxPlayersSeenAlive) {
            maxPlayersSeenAlive = aliveCount;
        }
        if (gameEndedSupplier.getAsBoolean() || currentPlayerId == null || currentPlayerId.isBlank() || me == null) {
            return;
        }
        if (!me.alive) {
            gameOverConsumer.accept(false);
            return;
        }
        if (aliveCount == 1 && maxPlayersSeenAlive >= 2) {
            gameOverConsumer.accept(true);
        }
    }

    private static String computeDir(int lastX, int lastY, int x, int y, String fallback) {
        if (x > lastX) {
            return "E";
        }
        if (x < lastX) {
            return "W";
        }
        if (y > lastY) {
            return "S";
        }
        if (y < lastY) {
            return "N";
        }
        return fallback == null ? "S" : fallback;
    }
}
