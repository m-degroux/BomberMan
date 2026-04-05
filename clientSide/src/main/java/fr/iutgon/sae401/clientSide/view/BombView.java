package fr.iutgon.sae401.clientSide.view;

import fr.iutgon.sae401.clientSide.service.SpriteManager;
import fr.iutgon.sae401.common.model.entity.Bomb;
import fr.iutgon.sae401.common.model.map.GameMap;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class BombView {
    private final Bomb bomb;
    private final ImageView imageView;
    private final DoubleProperty tileSize;
    private final GameMap gameMap;

    public BombView(Bomb bomb, DoubleProperty tileSize, GameMap gameMap) {
        this.bomb = bomb;
        this.tileSize = tileSize;
        this.gameMap = gameMap;
        this.imageView = new ImageView(SpriteManager.getBombFrame(0));
        this.imageView.fitWidthProperty().bind(tileSize);
        this.imageView.fitHeightProperty().bind(tileSize);
        this.imageView.layoutXProperty().bind(tileSize.multiply(bomb.getPosition().getX()));
        this.imageView.layoutYProperty().bind(tileSize.multiply(bomb.getPosition().getY()));
    }

    /**
     * Gère le cycle de vie complet de la bombe :
     * 1. Animation de la mèche (clignotement)
     * 2. Suppression de l'image de la bombe
     * 3. Création et animation de l'explosion (en croix avec propagation)
     * 4. Suppression des flammes
     */
    public void startExplosionChain(Pane parentLayer) {
        Timeline bombTimer = new Timeline();
        for (int i = 0; i < 15; i++) {
            int frameIdx = i;
            bombTimer.getKeyFrames().add(new KeyFrame(
                    Duration.millis(i * 80),
                    e -> imageView.setImage(SpriteManager.getBombFrame(frameIdx))
            ));
        }

        bombTimer.setOnFinished(event -> {
            parentLayer.getChildren().remove(this.imageView);
            ExplosionView explosion = new ExplosionView(
                    bomb.getPosition(),
                    bomb.getRange(),
                    tileSize,
                    gameMap
            );

            parentLayer.getChildren().addAll(explosion.getImageViews());
            explosion.explode(() -> {
                parentLayer.getChildren().removeAll(explosion.getImageViews());
            });
        });

        bombTimer.play();
    }

    public ImageView getImageView() {
        return imageView;
    }
}