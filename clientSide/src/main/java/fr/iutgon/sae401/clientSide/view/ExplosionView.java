package fr.iutgon.sae401.clientSide.view;

import fr.iutgon.sae401.clientSide.service.SpriteManager;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.TileType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExplosionView {
    private static final int FRAME_DELAY_PER_CASE = 2;
    private final List<ImageView> tiles = new ArrayList<>();
    private final DoubleProperty tileSize;

    public ExplosionView(List<Position> affectedTiles, Position center, DoubleProperty tileSize) {
        this.tileSize = Objects.requireNonNull(tileSize, "tileSize");
        if (center == null || affectedTiles == null || affectedTiles.isEmpty()) {
            return;
        }

        int cx = center.getX();
        int cy = center.getY();

        // Center first.
        addTile(cx, cy, "milieu", 0, 0);

        for (Position p : affectedTiles) {
            if (p == null) {
                continue;
            }
            if (p.getX() == cx && p.getY() == cy) {
                continue;
            }

            int rotation;
            int distance;

            if (p.getX() == cx) {
                // vertical arm
                distance = Math.abs(p.getY() - cy);
                rotation = p.getY() > cy ? 0 : 180;
            } else if (p.getY() == cy) {
                // horizontal arm
                distance = Math.abs(p.getX() - cx);
                rotation = p.getX() < cx ? 90 : 270;
            } else {
                // Not part of the cross; ignore.
                continue;
            }

            addTile(p.getX(), p.getY(), "bas", rotation, distance);
        }
    }

    public ExplosionView(Position center, int range, DoubleProperty tileSize, GameMap gameMap) {
        this.tileSize = tileSize;

        // 1. CENTRE (Distance 0)
        addTile(center.getX(), center.getY(), "milieu", 0, 0);

        int[][] directions = {{0, 1, 0}, {0, -1, 180}, {-1, 0, 90}, {1, 0, 270}};

        for (int[] d : directions) {
            for (int i = 1; i <= range; i++) {
                int targetX = center.getX() + d[0] * i;
                int targetY = center.getY() + d[1] * i;
                Position pos = new Position(targetX, targetY);


                if (targetX < 0 || targetX >= gameMap.getWidth() || targetY < 0 || targetY >= gameMap.getHeight()) {
                    break;
                }

                TileType type = gameMap.getTile(pos).getType();


                if (type == TileType.WALL || type == TileType.DESTRUCTIBLE) {
                    break;
                }

                addTile(targetX, targetY, "bas", d[2], i);
            }
        }
    }

    private void addTile(int x, int y, String folder, int rotation, int distance) {
        ImageView iv = new ImageView();
        iv.setUserData(folder + "|" + distance);
        iv.setRotate(rotation);
        iv.fitWidthProperty().bind(tileSize);
        iv.fitHeightProperty().bind(tileSize);
        iv.layoutXProperty().bind(tileSize.multiply(x));
        iv.layoutYProperty().bind(tileSize.multiply(y));
        tiles.add(iv);
    }

    public void explode(Runnable onFinished) {
        Timeline timeline = new Timeline();
        int totalFrames = 7 + 10;

        for (int i = 1; i <= totalFrames; i++) {
            int currentGlobalFrame = i;
            timeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(i * 80),
                    e -> update(currentGlobalFrame)
            ));
        }
        timeline.setOnFinished(e -> {
            if (onFinished != null) onFinished.run();
        });
        timeline.play();
    }

    private void update(int globalFrame) {
        for (ImageView iv : tiles) {
            String[] data = ((String) iv.getUserData()).split("\\|");
            String folder = data[0];
            int distance = Integer.parseInt(data[1]);

            int localFrame = globalFrame - (distance * FRAME_DELAY_PER_CASE);

            if (localFrame >= 1) {
                int maxFrames = folder.equals("bas") ? 6 : 7;
                if (localFrame <= maxFrames) {
                    iv.setImage(SpriteManager.get("explosions/" + folder, localFrame + ".png"));
                } else {
                    iv.setImage(null);
                }
            } else {
                iv.setImage(null);
            }
        }
    }

    public List<ImageView> getImageViews() {
        return tiles;
    }
}