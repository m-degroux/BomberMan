package fr.iutgon.sae401.clientSide.controller.game;

import fr.iutgon.sae401.TestReflectionUtils;
import fr.iutgon.sae401.clientSide.JavaFxTestUtils;
import fr.iutgon.sae401.common.json.Json;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.Pane;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEffectsSynchronizerTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestUtils.initToolkit();
    }

    @Test
    void bombSnapshotPath_addsAndRemovesBombsAndUsesTimerBasedAnimation() throws Exception {
        FxFixture fixture = new FxFixture();
        JavaFxTestUtils.runAndWait(() -> {
            fixture.sync.setBombTimerTotalMs(3000);
            fixture.sync.applyBombsDeltaOrSnapshot(Json.object(Map.of(
                    "bombs", Json.array(List.of(
                            bomb("b1", 2, 3, 1200),
                            Json.object(Map.of("id", Json.of(""), "x", Json.of(-1), "y", Json.of(3)))
                    ))
            )));
            assertEquals(1, fixture.bombLayer.getChildren().size());

            fixture.sync.updateBombAnimations(System.nanoTime());
            fixture.sync.applyBombsDeltaOrSnapshot(Json.object(Map.of("bombs", Json.emptyArray())));
            assertTrue(fixture.bombLayer.getChildren().isEmpty());
        });
    }

    @Test
    void bombDeltaPath_handlesUpsertMoveAndRemoveBranches() throws Exception {
        FxFixture fixture = new FxFixture();
        JavaFxTestUtils.runAndWait(() -> {
            fixture.sync.applyBombsDeltaOrSnapshot(Json.object(Map.of(
                    "bombsUpsert", Json.array(List.of(bomb("b-up", 1, 1, -1))),
                    "bombsRemove", Json.emptyArray()
            )));
            assertEquals(1, fixture.bombLayer.getChildren().size());

            fixture.sync.updateBombAnimations(System.nanoTime());

            fixture.sync.applyBombsDeltaOrSnapshot(Json.object(Map.of(
                    "bombsUpsert", Json.array(List.of(bomb("b-up", 2, 1, 500))),
                    "bombsRemove", Json.array(List.of(Json.of(123)))
            )));
            fixture.sync.updateBombAnimations(System.nanoTime());
            assertEquals(1, fixture.bombLayer.getChildren().size());

            fixture.sync.applyBombsDeltaOrSnapshot(Json.object(Map.of(
                    "bombsUpsert", Json.emptyArray(),
                    "bombsRemove", Json.array(List.of(Json.of("b-up")))
            )));
            assertTrue(fixture.bombLayer.getChildren().isEmpty());
        });
    }

    @Test
    void bonusSnapshotAndDeltaPaths_coverAddUnknownAndRemoveCases() throws Exception {
        FxFixture fixture = new FxFixture();
        JavaFxTestUtils.runAndWait(() -> {
            fixture.sync.applyBonusesDeltaOrSnapshot(Json.object(Map.of(
                    "bonuses", Json.array(List.of(
                            bonus("SPEED", 1, 1),
                            bonus("UNKNOWN", 2, 2),
                            Json.nullValue()
                    ))
            )));
            assertEquals(1, fixture.bonusLayer.getChildren().size());

            fixture.sync.applyBonusesDeltaOrSnapshot(Json.object(Map.of(
                    "bonusesUpsert", Json.array(List.of(
                            bonus("MAX_BOMBS", 3, 1),
                            bonus("BOMB_RANGE", 4, 1),
                            bonus("", 5, 1)
                    )),
                    "bonusesRemove", Json.array(List.of(Json.of("1:1"), Json.of(123)))
            )));
            assertEquals(2, fixture.bonusLayer.getChildren().size());

            fixture.sync.applyBonusesDeltaOrSnapshot(Json.object(Map.of("bonuses", Json.emptyArray())));
            assertTrue(fixture.bonusLayer.getChildren().isEmpty());
        });
    }

    @Test
    void applyExplosions_deduplicatesByKeyAndCanResetSeenState() throws Exception {
        FxFixture fixture = new FxFixture();
        JavaFxTestUtils.runAndWait(() -> {
            Json e1 = Json.object(Map.of(
                    "origin", Json.object(Map.of("x", Json.of(3), "y", Json.of(3))),
                    "tiles", Json.array(List.of(
                            point(3, 3),
                            point(3, 2),
                            point(3, 4),
                            point(2, 3),
                            point(4, 3)
                    ))
            ));

            fixture.sync.applyExplosions(Json.object(Map.of("explosions", Json.array(List.of(e1)))));
            int firstChildren = fixture.explosionLayer.getChildren().size();
            assertTrue(firstChildren > 0);

            fixture.sync.applyExplosions(Json.object(Map.of("explosions", Json.array(List.of(e1)))));
            assertEquals(firstChildren, fixture.explosionLayer.getChildren().size());

            try {
                Set<?> seen = (Set<?>) TestReflectionUtils.getField(fixture.sync, "seenExplosions");
                assertEquals(1, seen.size());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            fixture.sync.applyExplosions(Json.object(Map.of()));
            try {
                Set<?> seen = (Set<?>) TestReflectionUtils.getField(fixture.sync, "seenExplosions");
                assertTrue(seen.isEmpty());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void applyExplosions_guessCenterPath_handlesMissingOrigin() throws Exception {
        FxFixture fixture = new FxFixture();
        JavaFxTestUtils.runAndWait(() -> {
            Json e = Json.object(Map.of(
                    "tiles", Json.array(List.of(
                            point(5, 5),
                            point(5, 4),
                            point(5, 6),
                            point(4, 5),
                            point(6, 5)
                    ))
            ));
            fixture.sync.applyExplosions(Json.object(Map.of("explosions", Json.array(List.of(e)))));
            assertFalse(fixture.explosionLayer.getChildren().isEmpty());
        });
    }

    private static Json bomb(String id, int x, int y, int timerMs) {
        return Json.object(Map.of(
                "id", Json.of(id),
                "x", Json.of(x),
                "y", Json.of(y),
                "timerMs", Json.of(timerMs)
        ));
    }

    private static Json bonus(String type, int x, int y) {
        return Json.object(Map.of(
                "type", Json.of(type),
                "x", Json.of(x),
                "y", Json.of(y)
        ));
    }

    private static Json point(int x, int y) {
        return Json.object(Map.of(
                "x", Json.of(x),
                "y", Json.of(y)
        ));
    }

    private static final class FxFixture {
        private final SimpleDoubleProperty tileSize = new SimpleDoubleProperty(32.0);
        private final Pane bombLayer = new Pane();
        private final Pane bonusLayer = new Pane();
        private final Pane explosionLayer = new Pane();
        private final GameEffectsSynchronizer sync = new GameEffectsSynchronizer(tileSize, bombLayer, bonusLayer, explosionLayer);
    }
}
