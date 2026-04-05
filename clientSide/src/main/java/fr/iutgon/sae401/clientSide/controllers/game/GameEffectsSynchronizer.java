package fr.iutgon.sae401.clientSide.controller.game;

import fr.iutgon.sae401.clientSide.service.SpriteManager;
import fr.iutgon.sae401.clientSide.view.ExplosionView;
import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Position;
import javafx.beans.property.DoubleProperty;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GameEffectsSynchronizer {
    private final DoubleProperty tileSize;
    private final Pane bombLayer;
    private final Pane bonusLayer;
    private final Pane explosionLayer;

    private final Map<String, ImageView> bonuses = new HashMap<>();
    private final Map<String, ImageView> bombs = new HashMap<>();
    private final Map<String, Long> bombSpawnNs = new HashMap<>();
    private final Map<String, Integer> bombRemainingMs = new HashMap<>();
    private final Set<String> seenExplosions = new HashSet<>();
    private int bombTimerTotalMs = 3000;

    GameEffectsSynchronizer(DoubleProperty tileSize, Pane bombLayer, Pane bonusLayer, Pane explosionLayer) {
        this.tileSize = tileSize;
        this.bombLayer = bombLayer;
        this.bonusLayer = bonusLayer;
        this.explosionLayer = explosionLayer;
    }

    void setBombTimerTotalMs(int bombTimerTotalMs) {
        this.bombTimerTotalMs = bombTimerTotalMs;
    }

    int bombTimerTotalMs() {
        return bombTimerTotalMs;
    }

    void updateBombAnimations(long nowNs) {
        final long frameNs = 80_000_000L;
        final int frameCount = 15;
        for (Map.Entry<String, ImageView> e : bombs.entrySet()) {
            String key = e.getKey();
            Integer remaining = bombRemainingMs.get(key);
            if (remaining != null && bombTimerTotalMs > 0) {
                double progress = 1.0 - (Math.max(0, remaining) / (double) bombTimerTotalMs);
                int frame = (int) Math.floor(progress * frameCount);
                if (frame < 0) {
                    frame = 0;
                }
                if (frame >= frameCount) {
                    frame = frameCount - 1;
                }
                e.getValue().setImage(SpriteManager.getBombFrame(frame));
                continue;
            }

            Long born = bombSpawnNs.get(key);
            if (born == null) {
                born = nowNs;
                bombSpawnNs.put(key, born);
            }
            long age = Math.max(0L, nowNs - born);
            int frame = (int) ((age / frameNs) % frameCount);
            e.getValue().setImage(SpriteManager.getBombFrame(frame));
        }
    }

    void applyBombsDeltaOrSnapshot(Json delta) {
        if ((delta.contains("bombsUpsert") && delta.at("bombsUpsert").isArray())
                || (delta.contains("bombsRemove") && delta.at("bombsRemove").isArray())) {
            applyBombsUpsert(delta.value("bombsUpsert", Json.emptyArray()));
            applyBombsRemove(delta.value("bombsRemove", Json.emptyArray()));
        } else {
            Json bombsLike = Json.object(Map.of(
                    "bombs", delta.value("bombs", Json.emptyArray())
            ));
            applyBombs(bombsLike);
        }
    }

    void applyBonusesDeltaOrSnapshot(Json delta) {
        if ((delta.contains("bonusesUpsert") && delta.at("bonusesUpsert").isArray())
                || (delta.contains("bonusesRemove") && delta.at("bonusesRemove").isArray())) {
            applyBonusesUpsert(delta.value("bonusesUpsert", Json.emptyArray()));
            applyBonusesRemove(delta.value("bonusesRemove", Json.emptyArray()));
        } else {
            Json bonusesLike = Json.object(Map.of(
                    "bonuses", delta.value("bonuses", Json.emptyArray())
            ));
            applyBonuses(bonusesLike);
        }
    }

    void applyBombs(Json state) {
        Set<String> present = new HashSet<>();
        if (state.contains("bombs") && state.at("bombs").isArray()) {
            for (Json b : state.at("bombs").asArray()) {
                String bombId = b.value("id", "");
                int x = b.value("x", -1);
                int y = b.value("y", -1);
                if (bombId == null || bombId.isBlank() || x < 0 || y < 0) {
                    continue;
                }
                String key = bombId;
                present.add(key);
                int remaining = b.value("timerMs", -1);
                if (remaining >= 0) {
                    bombRemainingMs.put(key, remaining);
                } else {
                    bombRemainingMs.remove(key);
                }
                ImageView iv = bombs.get(key);
                if (iv == null) {
                    iv = new ImageView(SpriteManager.getBombFrame(0));
                    iv.fitWidthProperty().bind(tileSize);
                    iv.fitHeightProperty().bind(tileSize);
                    iv.layoutXProperty().bind(tileSize.multiply(x));
                    iv.layoutYProperty().bind(tileSize.multiply(y));
                    bombs.put(key, iv);
                    bombSpawnNs.put(key, System.nanoTime());
                    bombLayer.getChildren().add(iv);
                }
            }
        }

        Set<String> toRemove = new HashSet<>();
        for (String key : bombs.keySet()) {
            if (!present.contains(key)) {
                toRemove.add(key);
            }
        }
        for (String key : toRemove) {
            ImageView iv = bombs.remove(key);
            if (iv != null) {
                bombLayer.getChildren().remove(iv);
            }
            bombSpawnNs.remove(key);
            bombRemainingMs.remove(key);
        }
        bombRemainingMs.keySet().retainAll(present);
    }

    void applyBonuses(Json state) {
        Set<String> present = new HashSet<>();
        if (state.contains("bonuses") && state.at("bonuses").isArray()) {
            for (Json b : state.at("bonuses").asArray()) {
                if (b == null || !b.isObject()) {
                    continue;
                }
                int x = b.value("x", -1);
                int y = b.value("y", -1);
                String type = b.value("type", "");
                if (x < 0 || y < 0 || type == null || type.isBlank()) {
                    continue;
                }
                String key = x + ":" + y;
                present.add(key);
                ImageView iv = bonuses.get(key);
                if (iv == null) {
                    var img = switch (type) {
                        case "SPEED" -> SpriteManager.get("boost", "eclair.png");
                        case "MAX_BOMBS" -> SpriteManager.get("boost", "bomb.png");
                        case "BOMB_RANGE" -> SpriteManager.get("boost", "cible.png");
                        default -> null;
                    };
                    if (img == null) {
                        continue;
                    }
                    iv = new ImageView(img);
                    iv.fitWidthProperty().bind(tileSize);
                    iv.fitHeightProperty().bind(tileSize);
                    iv.layoutXProperty().bind(tileSize.multiply(x));
                    iv.layoutYProperty().bind(tileSize.multiply(y));
                    bonuses.put(key, iv);
                    bonusLayer.getChildren().add(iv);
                }
            }
        }

        Set<String> toRemove = new HashSet<>();
        for (String key : bonuses.keySet()) {
            if (!present.contains(key)) {
                toRemove.add(key);
            }
        }
        for (String key : toRemove) {
            ImageView iv = bonuses.remove(key);
            if (iv != null) {
                bonusLayer.getChildren().remove(iv);
            }
        }
    }

    void applyExplosions(Json state) {
        if (!state.contains("explosions") || !state.at("explosions").isArray()) {
            seenExplosions.clear();
            return;
        }
        Set<String> presentKeys = new HashSet<>();
        for (Json e : state.at("explosions").asArray()) {
            if (e == null || !e.isObject()) {
                continue;
            }
            Json tilesJson = e.value("tiles", Json.emptyArray());
            if (!tilesJson.isArray() || tilesJson.asArray().isEmpty()) {
                continue;
            }

            List<Position> tiles = tilesJson.asArray().stream().map(t -> {
                int x = t.value("x", Integer.MIN_VALUE);
                int y = t.value("y", Integer.MIN_VALUE);
                if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE) {
                    return null;
                }
                return new Position(x, y);
            }).filter(p -> p != null).toList();
            if (tiles.isEmpty()) {
                continue;
            }

            Position center = null;
            Json originJson = e.contains("origin") ? e.at("origin") : Json.nullValue();
            if (originJson.isObject()) {
                int ox = originJson.value("x", Integer.MIN_VALUE);
                int oy = originJson.value("y", Integer.MIN_VALUE);
                if (ox != Integer.MIN_VALUE && oy != Integer.MIN_VALUE) {
                    center = new Position(ox, oy);
                }
            }
            if (center == null) {
                center = guessCenter(tiles);
                if (center == null) {
                    continue;
                }
            }
            String key = explosionKey(center, tiles);
            presentKeys.add(key);
            if (seenExplosions.contains(key)) {
                continue;
            }
            seenExplosions.add(key);

            ExplosionView view = new ExplosionView(tiles, center, tileSize);
            explosionLayer.getChildren().addAll(view.getImageViews());
            view.explode(() -> explosionLayer.getChildren().removeAll(view.getImageViews()));
        }
        seenExplosions.retainAll(presentKeys);
    }

    private void applyBombsUpsert(Json bombsUpsert) {
        if (bombsUpsert == null || !bombsUpsert.isArray()) {
            return;
        }
        for (Json b : bombsUpsert.asArray()) {
            if (b == null || !b.isObject()) {
                continue;
            }
            String bombId = b.value("id", "");
            int x = b.value("x", -1);
            int y = b.value("y", -1);
            if (bombId == null || bombId.isBlank() || x < 0 || y < 0) {
                continue;
            }
            String key = bombId;
            int remaining = b.value("timerMs", -1);
            if (remaining >= 0) {
                bombRemainingMs.put(key, remaining);
            } else {
                bombRemainingMs.remove(key);
            }

            ImageView iv = bombs.get(key);
            if (iv == null) {
                iv = new ImageView(SpriteManager.getBombFrame(0));
                iv.fitWidthProperty().bind(tileSize);
                iv.fitHeightProperty().bind(tileSize);
                iv.layoutXProperty().bind(tileSize.multiply(x));
                iv.layoutYProperty().bind(tileSize.multiply(y));
                bombs.put(key, iv);
                bombSpawnNs.put(key, System.nanoTime());
                bombLayer.getChildren().add(iv);
            } else {
                try {
                    iv.layoutXProperty().unbind();
                    iv.layoutYProperty().unbind();
                } catch (Exception ignored) {
                }
                iv.layoutXProperty().bind(tileSize.multiply(x));
                iv.layoutYProperty().bind(tileSize.multiply(y));
            }
        }
    }

    private void applyBombsRemove(Json bombsRemove) {
        if (bombsRemove == null || !bombsRemove.isArray()) {
            return;
        }
        for (Json idVal : bombsRemove.asArray()) {
            if (idVal == null || !idVal.isString()) {
                continue;
            }
            String key = idVal.getString();
            ImageView iv = bombs.remove(key);
            if (iv != null) {
                bombLayer.getChildren().remove(iv);
            }
            bombSpawnNs.remove(key);
            bombRemainingMs.remove(key);
        }
    }

    private void applyBonusesUpsert(Json bonusesUpsert) {
        if (bonusesUpsert == null || !bonusesUpsert.isArray()) {
            return;
        }
        for (Json b : bonusesUpsert.asArray()) {
            if (b == null || !b.isObject()) {
                continue;
            }
            int x = b.value("x", -1);
            int y = b.value("y", -1);
            String type = b.value("type", "");
            if (x < 0 || y < 0 || type == null || type.isBlank()) {
                continue;
            }
            String key = x + ":" + y;
            ImageView iv = bonuses.get(key);
            if (iv == null) {
                var img = switch (type) {
                    case "SPEED" -> SpriteManager.get("boost", "eclair.png");
                    case "MAX_BOMBS" -> SpriteManager.get("boost", "bomb.png");
                    case "BOMB_RANGE" -> SpriteManager.get("boost", "cible.png");
                    default -> null;
                };
                if (img == null) {
                    continue;
                }
                iv = new ImageView(img);
                iv.fitWidthProperty().bind(tileSize);
                iv.fitHeightProperty().bind(tileSize);
                iv.layoutXProperty().bind(tileSize.multiply(x));
                iv.layoutYProperty().bind(tileSize.multiply(y));
                bonuses.put(key, iv);
                bonusLayer.getChildren().add(iv);
            }
        }
    }

    private void applyBonusesRemove(Json bonusesRemove) {
        if (bonusesRemove == null || !bonusesRemove.isArray()) {
            return;
        }
        for (Json keyVal : bonusesRemove.asArray()) {
            if (keyVal == null || !keyVal.isString()) {
                continue;
            }
            String key = keyVal.getString();
            ImageView iv = bonuses.remove(key);
            if (iv != null) {
                bonusLayer.getChildren().remove(iv);
            }
        }
    }

    private static String explosionKey(Position center, List<Position> tiles) {
        long hash = 1469598103934665603L;
        List<Position> ordered = tiles.stream()
                .sorted((a, b) -> {
                    int dx = Integer.compare(a.getX(), b.getX());
                    return dx != 0 ? dx : Integer.compare(a.getY(), b.getY());
                })
                .toList();
        for (Position p : ordered) {
            hash ^= (p.getX() * 31L + p.getY());
            hash *= 1099511628211L;
        }
        return center.getX() + ":" + center.getY() + ":" + Long.toUnsignedString(hash);
    }

    private static Position guessCenter(List<Position> tiles) {
        Map<Integer, Integer> xCount = new HashMap<>();
        Map<Integer, Integer> yCount = new HashMap<>();
        for (Position p : tiles) {
            xCount.merge(p.getX(), 1, Integer::sum);
            yCount.merge(p.getY(), 1, Integer::sum);
        }
        int cx = mostFrequentKey(xCount);
        int cy = mostFrequentKey(yCount);
        Position center = new Position(cx, cy);
        for (Position p : tiles) {
            if (p.equals(center)) {
                return center;
            }
        }
        return tiles.get(0);
    }

    private static int mostFrequentKey(Map<Integer, Integer> counts) {
        int bestKey = 0;
        int bestCount = Integer.MIN_VALUE;
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                bestKey = e.getKey();
            }
        }
        return bestKey;
    }
}
