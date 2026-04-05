package fr.iutgon.sae401.clientSide.controller.game;

import fr.iutgon.sae401.clientSide.JavaFxTestUtils;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.model.map.Tile;
import fr.iutgon.sae401.common.model.map.TileType;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameMapRendererTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestUtils.initToolkit();
    }

    @Test
    void setupResponsiveGrid_usesFallbackThenComputesFromPaneSize() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            Pane gamePane = new Pane();
            Pane world = new Pane();
            Pane floor = new Pane();
            Pane objects = new Pane();
            SimpleDoubleProperty tileSize = new SimpleDoubleProperty();
            GameMap map = GameMapRenderer.createPlaceholderMap(15, 13);
            GameMapRenderer renderer = new GameMapRenderer(gamePane, world, floor, objects, tileSize, () -> map);

            renderer.setupResponsiveGrid();
            assertEquals(64.0, tileSize.get(), 0.0001);

            gamePane.resize(700, 560);
            assertEquals(Math.min(700.0 / 15.0, 560.0 / 13.0), tileSize.get(), 0.0001);
        });
    }

    @Test
    void drawMap_ignoresNullAndPopulatesLayersForGroundAndBlocks() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            Pane gamePane = new Pane();
            Pane world = new Pane();
            Pane floor = new Pane();
            Pane objects = new Pane();
            SimpleDoubleProperty tileSize = new SimpleDoubleProperty(32.0);

            GameMap map = new GameMap(4, 4, MapTheme.CLASSIC);
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    map.setTile(new Position(x, y), new Tile(TileType.GROUND));
                }
            }
            map.setTile(new Position(1, 1), new Tile(TileType.WALL));
            map.setTile(new Position(2, 2), new Tile(TileType.DESTRUCTIBLE));

            GameMapRenderer renderer = new GameMapRenderer(gamePane, world, floor, objects, tileSize, () -> map);
            renderer.drawMap(null);
            assertTrue(floor.getChildren().isEmpty());
            assertTrue(objects.getChildren().isEmpty());

            renderer.drawMap(map);
            assertEquals(16, floor.getChildren().size());
            assertEquals(2, objects.getChildren().size());
        });
    }

    @Test
    void createPlaceholderMap_returnsMapFilledWithGround() {
        GameMap map = GameMapRenderer.createPlaceholderMap(5, 3);
        assertEquals(5, map.getWidth());
        assertEquals(3, map.getHeight());
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                assertEquals(TileType.GROUND, map.getTile(new Position(x, y)).getType());
            }
        }
    }

    @Test
    void setupResponsiveGrid_recomputesCenterWhenMapDimensionsChange() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            Pane gamePane = new Pane();
            Pane world = new Pane();
            Pane floor = new Pane();
            Pane objects = new Pane();
            SimpleDoubleProperty tileSize = new SimpleDoubleProperty();
            AtomicReference<GameMap> mapRef = new AtomicReference<>(GameMapRenderer.createPlaceholderMap(15, 13));

            GameMapRenderer renderer = new GameMapRenderer(gamePane, world, floor, objects, tileSize, mapRef::get);
            renderer.setupResponsiveGrid();
            gamePane.resize(700, 560);

            GameMap largerMap = GameMapRenderer.createPlaceholderMap(35, 25);
            mapRef.set(largerMap);
            renderer.drawMap(largerMap);

            double expectedTile = Math.min(700.0 / 35.0, 560.0 / 25.0);
            assertEquals(expectedTile, tileSize.get(), 0.0001);
            assertEquals((700.0 - expectedTile * 35.0) / 2.0, world.getLayoutX(), 0.0001);
            assertEquals((560.0 - expectedTile * 25.0) / 2.0, world.getLayoutY(), 0.0001);
            assertTrue(world.getLayoutX() >= 0.0);
            assertTrue(world.getLayoutY() >= 0.0);
        });
    }

    @Test
    void setupResponsiveGrid_smallMapsDoNotUpscaleBeyondReferenceAndStayCentered() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            Pane gamePane = new Pane();
            Pane world = new Pane();
            Pane floor = new Pane();
            Pane objects = new Pane();
            SimpleDoubleProperty tileSize = new SimpleDoubleProperty();
            AtomicReference<GameMap> mapRef = new AtomicReference<>(GameMapRenderer.createPlaceholderMap(15, 13));

            GameMapRenderer renderer = new GameMapRenderer(gamePane, world, floor, objects, tileSize, mapRef::get);
            renderer.setupResponsiveGrid();
            gamePane.resize(960, 640);

            GameMap smallerMap = GameMapRenderer.createPlaceholderMap(7, 7);
            mapRef.set(smallerMap);
            renderer.drawMap(smallerMap);

            double referenceTile = Math.min(960.0 / 15.0, 640.0 / 13.0);
            assertEquals(referenceTile, tileSize.get(), 0.0001);
            assertEquals((960.0 - referenceTile * 7.0) / 2.0, world.getLayoutX(), 0.0001);
            assertEquals((640.0 - referenceTile * 7.0) / 2.0, world.getLayoutY(), 0.0001);
            assertTrue(world.getLayoutX() > 0.0);
            assertTrue(world.getLayoutY() > 0.0);
        });
    }

    @Test
    void drawMap_sameDimensions_reusesTileNodesAndOnlyUpdatesChangedTiles() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            Pane gamePane = new Pane();
            Pane world = new Pane();
            Pane floor = new Pane();
            Pane objects = new Pane();
            SimpleDoubleProperty tileSize = new SimpleDoubleProperty(32.0);

            GameMap map = new GameMap(4, 4, MapTheme.CLASSIC);
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    map.setTile(new Position(x, y), new Tile(TileType.GROUND));
                }
            }
            map.setTile(new Position(1, 1), new Tile(TileType.WALL));

            GameMapRenderer renderer = new GameMapRenderer(gamePane, world, floor, objects, tileSize, () -> map);
            renderer.drawMap(map);

            Node firstFloorNode = floor.getChildren().getFirst();
            Node wallNode = objects.getChildren().getFirst();
            assertEquals(16, floor.getChildren().size());
            assertEquals(1, objects.getChildren().size());

            // Add one new block while keeping the existing one.
            map.setTile(new Position(2, 2), new Tile(TileType.DESTRUCTIBLE));
            renderer.drawMap(map);
            assertTrue(floor.getChildren().contains(firstFloorNode));
            assertTrue(objects.getChildren().contains(wallNode));
            assertEquals(16, floor.getChildren().size());
            assertEquals(2, objects.getChildren().size());

            // Remove the original wall only.
            map.setTile(new Position(1, 1), new Tile(TileType.GROUND));
            renderer.drawMap(map);
            assertFalse(objects.getChildren().contains(wallNode));
            assertEquals(1, objects.getChildren().size());
        });
    }
}
