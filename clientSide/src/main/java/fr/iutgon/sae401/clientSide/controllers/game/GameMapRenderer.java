package fr.iutgon.sae401.clientSide.controller.game;

import fr.iutgon.sae401.clientSide.service.SpriteManager;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.model.map.Tile;
import fr.iutgon.sae401.common.model.map.TileType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.CacheHint;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class GameMapRenderer {
    private static final int REFERENCE_MAP_WIDTH = 15;
    private static final int REFERENCE_MAP_HEIGHT = 13;

    private final Pane gamePane;
    private final Pane worldLayer;
    private final Pane floorLayer;
    private final Pane objectLayer;
    private final DoubleProperty tileSize;
    private final Supplier<GameMap> mapSupplier;
    private final IntegerProperty mapWidth = new SimpleIntegerProperty(REFERENCE_MAP_WIDTH);
    private final IntegerProperty mapHeight = new SimpleIntegerProperty(REFERENCE_MAP_HEIGHT);
    private TileType[][] renderedTileTypes;
    private final Map<String, ImageView> objectViewsByTile = new HashMap<>();

    public GameMapRenderer(
            Pane gamePane,
            Pane worldLayer,
            Pane floorLayer,
            Pane objectLayer,
            DoubleProperty tileSize,
            Supplier<GameMap> mapSupplier
    ) {
        this.gamePane = gamePane;
        this.worldLayer = worldLayer;
        this.floorLayer = floorLayer;
        this.objectLayer = objectLayer;
        this.tileSize = tileSize;
        this.mapSupplier = mapSupplier;
        this.floorLayer.setCache(true);
        this.floorLayer.setCacheHint(CacheHint.SPEED);
        this.objectLayer.setCache(true);
        this.objectLayer.setCacheHint(CacheHint.SPEED);
    }

    public void setupResponsiveGrid() {
        GameMap initialMap = mapSupplier.get();
        mapWidth.set(initialMap == null ? REFERENCE_MAP_WIDTH : initialMap.getWidth());
        mapHeight.set(initialMap == null ? REFERENCE_MAP_HEIGHT : initialMap.getHeight());

        tileSize.bind(Bindings.createDoubleBinding(() -> {
            if (gamePane.getWidth() <= 0 || gamePane.getHeight() <= 0) {
                return 64.0;
            }
            int widthTiles = Math.max(1, mapWidth.get());
            int heightTiles = Math.max(1, mapHeight.get());
            double fitCurrentMap = Math.min(gamePane.getWidth() / widthTiles, gamePane.getHeight() / heightTiles);

            // For smaller-than-default maps, keep the same max visual scale as the 15x13 baseline.
            // This prevents over-zoom and perceived off-centering on small maps.
            if (widthTiles <= REFERENCE_MAP_WIDTH && heightTiles <= REFERENCE_MAP_HEIGHT) {
                double fitReferenceMap = Math.min(
                        gamePane.getWidth() / REFERENCE_MAP_WIDTH,
                        gamePane.getHeight() / REFERENCE_MAP_HEIGHT
                );
                return Math.min(fitCurrentMap, fitReferenceMap);
            }
            return fitCurrentMap;
        }, gamePane.widthProperty(), gamePane.heightProperty(), mapWidth, mapHeight));

        worldLayer.layoutXProperty().bind(
                gamePane.widthProperty()
                        .subtract(tileSize.multiply(mapWidth))
                        .divide(2)
        );
        worldLayer.layoutYProperty().bind(
                gamePane.heightProperty()
                        .subtract(tileSize.multiply(mapHeight))
                        .divide(2)
        );
    }

    public void drawMap(GameMap map) {
        if (map == null) {
            return;
        }
        mapWidth.set(map.getWidth());
        mapHeight.set(map.getHeight());

        if (renderedTileTypes == null
                || renderedTileTypes.length != map.getHeight()
                || renderedTileTypes[0].length != map.getWidth()) {
            rebuildAllTiles(map);
            return;
        }

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                TileType currentType = tileTypeAt(map, x, y);
                if (renderedTileTypes[y][x] == currentType) {
                    continue;
                }
                renderedTileTypes[y][x] = currentType;
                updateObjectTile(x, y, currentType);
            }
        }
    }

    public static GameMap createPlaceholderMap(int width, int height) {
        GameMap map = new GameMap(width, height, MapTheme.CLASSIC);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map.setTile(new Position(x, y), new Tile(TileType.GROUND));
            }
        }
        return map;
    }

    private ImageView createStaticTile(TileType type, int x, int y) {
        ImageView iv = new ImageView(resolveTileImage(type));
        iv.setSmooth(false);
        iv.fitWidthProperty().bind(tileSize);
        iv.fitHeightProperty().bind(tileSize);
        iv.layoutXProperty().bind(tileSize.multiply(x));
        iv.layoutYProperty().bind(tileSize.multiply(y));
        return iv;
    }

    private void rebuildAllTiles(GameMap map) {
        floorLayer.getChildren().clear();
        objectLayer.getChildren().clear();
        objectViewsByTile.clear();
        renderedTileTypes = new TileType[map.getHeight()][map.getWidth()];
        floorLayer.setCache(false);
        objectLayer.setCache(false);

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                floorLayer.getChildren().add(createStaticTile(TileType.GROUND, x, y));
                TileType currentType = tileTypeAt(map, x, y);
                renderedTileTypes[y][x] = currentType;
                if (currentType != TileType.GROUND) {
                    String key = tileKey(x, y);
                    ImageView objectTile = createStaticTile(currentType, x, y);
                    objectViewsByTile.put(key, objectTile);
                    objectLayer.getChildren().add(objectTile);
                }
            }
        }
        floorLayer.setCache(true);
        objectLayer.setCache(true);
    }

    private void updateObjectTile(int x, int y, TileType type) {
        String key = tileKey(x, y);
        if (type == TileType.GROUND) {
            ImageView existing = objectViewsByTile.remove(key);
            if (existing != null) {
                objectLayer.getChildren().remove(existing);
            }
            return;
        }

        ImageView existing = objectViewsByTile.get(key);
        if (existing == null) {
            ImageView objectTile = createStaticTile(type, x, y);
            objectViewsByTile.put(key, objectTile);
            objectLayer.getChildren().add(objectTile);
            return;
        }
        existing.setImage(resolveTileImage(type));
    }

    private static TileType tileTypeAt(GameMap map, int x, int y) {
        Tile t = map.getTile(new Position(x, y));
        if (t == null || t.getType() == null) {
            return TileType.GROUND;
        }
        return t.getType();
    }

    private static String tileKey(int x, int y) {
        return x + ":" + y;
    }

    private static javafx.scene.image.Image resolveTileImage(TileType type) {
        String cat = (type == TileType.GROUND) ? "ground" : "walls";
        String imgName = "ground_01.png";
        if (type == TileType.WALL) {
            imgName = "block_01.png";
        } else if (type == TileType.DESTRUCTIBLE) {
            imgName = "block_03.png";
        }
        return SpriteManager.get(cat, imgName);
    }
}
