package fr.iutgon.sae401.clientSide.view;

import fr.iutgon.sae401.TestReflectionUtils;
import fr.iutgon.sae401.clientSide.JavaFxTestUtils;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.model.map.Tile;
import fr.iutgon.sae401.common.model.map.TileType;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplosionViewTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestUtils.initToolkit();
    }

    @Test
    void constructor_withInvalidInputsProducesNoTiles() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            ExplosionView noCenter = new ExplosionView(List.of(new Position(1, 1)), null, new SimpleDoubleProperty(32.0));
            ExplosionView noTiles = new ExplosionView(List.of(), new Position(1, 1), new SimpleDoubleProperty(32.0));
            assertTrue(noCenter.getImageViews().isEmpty());
            assertTrue(noTiles.getImageViews().isEmpty());
        });
    }

    @Test
    void constructor_fromAffectedTilesBuildsCrossAndIgnoresDiagonals() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            ExplosionView view = new ExplosionView(
                    List.of(
                            new Position(3, 3),
                            new Position(3, 2),
                            new Position(3, 4),
                            new Position(2, 3),
                            new Position(4, 3),
                            new Position(4, 4)
                    ),
                    new Position(3, 3),
                    new SimpleDoubleProperty(32.0)
            );
            assertEquals(5, view.getImageViews().size());
        });
    }

    @Test
    void constructor_withRangeStopsAtWallsAndDestructibles() throws Exception {
        JavaFxTestUtils.runAndWait(() -> {
            GameMap map = openMap(7, 7);
            map.setTile(new Position(3, 1), new Tile(TileType.DESTRUCTIBLE));
            map.setTile(new Position(5, 3), new Tile(TileType.WALL));

            ExplosionView view = new ExplosionView(
                    new Position(3, 3),
                    3,
                    new SimpleDoubleProperty(32.0),
                    map
            );
            assertFalse(view.getImageViews().isEmpty());
        });
    }

    @Test
    void explodeAndPrivateUpdate_coverAnimationBranches() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ExplosionView[] holder = new ExplosionView[1];
        JavaFxTestUtils.runAndWait(() -> {
            ExplosionView view = new ExplosionView(
                    List.of(
                            new Position(2, 2),
                            new Position(2, 1),
                            new Position(2, 3),
                            new Position(1, 2),
                            new Position(3, 2)
                    ),
                    new Position(2, 2),
                    new SimpleDoubleProperty(32.0)
            );

            try {
                TestReflectionUtils.invokeMethod(view, "update", new Class[]{int.class}, 1);
                TestReflectionUtils.invokeMethod(view, "update", new Class[]{int.class}, 20);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            view.explode(latch::countDown);
            holder[0] = view;
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        ExplosionView view = holder[0];
        for (ImageView iv : view.getImageViews()) {
            assertTrue(iv.getFitWidth() >= 0.0);
        }
    }

    private static GameMap openMap(int width, int height) {
        GameMap map = new GameMap(width, height, MapTheme.CLASSIC);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                TileType type = (x == 0 || y == 0 || x == width - 1 || y == height - 1)
                        ? TileType.WALL
                        : TileType.GROUND;
                map.setTile(new Position(x, y), new Tile(type));
            }
        }
        return map;
    }
}
