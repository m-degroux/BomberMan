package fr.iutgon.sae401.clientSide.view;

import fr.iutgon.sae401.clientSide.service.SpriteManager;
import javafx.beans.property.DoubleProperty;
import javafx.scene.image.ImageView;

public class PlayerView {
    private static final int[] WALK_FRAME_SEQUENCE = {0, 1, 2, 1};

    private final ImageView imageView;
    private final DoubleProperty tileSize;
    private final int skinId;
    private double currentX, currentY;
    private boolean isFirstFrame = true;
    private String state = "normal"; // "normal", "dead", "win", "lose"
    private long stateStartTime = -1;
    private long lastUpdateNs = -1;
    private long walkAccumulatorNs = 0;
    private int walkSequenceIndex = 0;
    private boolean wasMoving = false;

    public PlayerView(DoubleProperty tileSize, int skinId) {
        this.tileSize = tileSize;
        this.skinId = skinId;
        this.imageView = new ImageView();
        this.imageView.setPreserveRatio(true);
    }

    public void setDead() {
        if (!state.equals("dead")) {
            this.state = "dead";
            this.stateStartTime = System.nanoTime();
        }
    }

    public void setWin() {
        if (!state.equals("win")) {
            this.state = "win";
            this.stateStartTime = System.nanoTime();
        }
    }

    public void setLose() {
        if (!state.equals("lose")) {
            this.state = "lose";
            this.stateStartTime = System.nanoTime();
        }
    }

    public void update(long now, int gridX, int gridY, String dir, double moveSpeed, long animSpeed, int nbFrames) {
        double tSize = tileSize.get();
        if (tSize <= 0) return;
        if (animSpeed <= 0L) {
            animSpeed = 1L;
        }

        long elapsedNs = 0L;
        if (lastUpdateNs > 0L) {
            elapsedNs = Math.max(0L, now - lastUpdateNs);
        }
        lastUpdateNs = now;

        double spriteSize = tSize * 0.8;
        imageView.setFitWidth(spriteSize);

        double targetX = (gridX * tSize) + (tSize - spriteSize) / 2;
        double targetY = (gridY * tSize) + (tSize - spriteSize) / 2;

        if (isFirstFrame) {
            currentX = targetX;
            currentY = targetY;
            isFirstFrame = false;
        } else {
            currentX += (targetX - currentX) * moveSpeed;
            currentY += (targetY - currentY) * moveSpeed;
        }

        imageView.setLayoutX(currentX);
        imageView.setLayoutY(currentY);

        if (state.equals("dead")) {
            // Death animation: D_2, D_0, D_1 - played once then stays on last frame
            long elapsed = (now - stateStartTime);
            int frameIndex = (int) ((elapsed / animSpeed) % 3);
            int[] deathFrames = {2, 0, 1};
            
            // If animation is complete (after showing all 3 frames), stay on last frame
            if (elapsed >= (3 * animSpeed)) {
                frameIndex = 2; // Stay on last frame (D_1)
            }
            
            int frame = deathFrames[frameIndex];
            imageView.setImage(SpriteManager.getPlayerFrame(skinId, "D_" + frame));
        } else if (state.equals("win")) {
            // Win animation: V_0, V_2, V_1 - looped
            long elapsed = (now - stateStartTime);
            int frameIndex = (int) ((elapsed / animSpeed) % 3);
            int[] winFrames = {0, 2, 1};
            int frame = winFrames[frameIndex];
            imageView.setImage(SpriteManager.getPlayerFrame(skinId, "V_" + frame));
        } else if (state.equals("lose")) {
            // Lose animation: D_2, D_0, D_1 - played once then stays on last frame
            long elapsed = (now - stateStartTime);
            int frameIndex = (int) ((elapsed / animSpeed) % 3);
            int[] deathFrames = {2, 0, 1};
            
            // If animation is complete, stay on last frame
            if (elapsed >= (3 * animSpeed)) {
                frameIndex = 2; // Stay on last frame (D_1)
            }
            
            int frame = deathFrames[frameIndex];
            imageView.setImage(SpriteManager.getPlayerFrame(skinId, "D_" + frame));
        } else {
            // Normal movement animation
            boolean isMoving = Math.abs(currentX - targetX) > 0.5 || Math.abs(currentY - targetY) > 0.5;
            int frame = 0;
            if (isMoving) {
                if (!wasMoving) {
                    // Start the walk cycle immediately on movement start.
                    walkSequenceIndex = 1;
                    walkAccumulatorNs = 0L;
                } else {
                    walkAccumulatorNs += elapsedNs;
                    while (walkAccumulatorNs >= animSpeed) {
                        walkAccumulatorNs -= animSpeed;
                        walkSequenceIndex = (walkSequenceIndex + 1) % WALK_FRAME_SEQUENCE.length;
                    }
                }
                frame = WALK_FRAME_SEQUENCE[walkSequenceIndex];
                if (nbFrames > 0) {
                    frame = Math.floorMod(frame, nbFrames);
                } else {
                    frame = 0;
                }
            } else {
                walkAccumulatorNs = 0L;
                walkSequenceIndex = 0;
            }
            wasMoving = isMoving;
            imageView.setImage(SpriteManager.getPlayerFrame(skinId, dir, frame));
        }
    }

    public ImageView getImageView() {
        return imageView;
    }
}
