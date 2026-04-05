package fr.iutgon.sae401.clientSide.controller.game;

import fr.iutgon.sae401.clientSide.JavaFxTestUtils;
import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.model.map.Tile;
import fr.iutgon.sae401.common.model.map.TileType;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateSynchronizerTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestUtils.initToolkit();
    }

    @Test
    void applyStateThenDelta_playerRemovalCanTriggerDefeatAndHudUpdates() throws Exception {
        Fixture fixture = new Fixture();
        JavaFxTestUtils.runAndWait(() -> {
            fixture.sync.setInitialMap(openMap(7, 7));
            String mapJson = fixture.sync.gameMap().toJson().stringify();

            fixture.sync.applyState(Json.object(Map.of(
                    "width", Json.of(7),
                    "height", Json.of(7),
                    "bombTimerMs", Json.of(2000),
                    "map", Json.of(mapJson),
                    "players", Json.array(List.of(
                            player("me", 2, 2, 3, 1, true, 1.0f),
                            player("other", 4, 2, 3, 1, true, 1.0f)
                    )),
                    "bombs", Json.emptyArray(),
                    "explosions", Json.emptyArray(),
                    "bonuses", Json.emptyArray()
            )));

            assertEquals(2, fixture.sync.players().size());
            assertEquals(3, fixture.health.get());
            assertEquals(1, fixture.bombs.get());

            fixture.sync.applyDelta(Json.object(Map.of(
                    "playersUpsert", Json.emptyArray(),
                    "playersRemove", Json.array(List.of(Json.of("me"))),
                    "tilesUpsert", Json.emptyArray(),
                    "explosions", Json.emptyArray(),
                    "bombs", Json.emptyArray(),
                    "bonuses", Json.emptyArray()
            )));

            assertTrue(fixture.gameOverCalls.contains(Boolean.FALSE));
            assertFalse(fixture.sync.players().containsKey("me"));
        });
    }

    @Test
    void applyDelta_tilesUpsertOnlyChangesMapWhenMapReady() throws Exception {
        Fixture fixture = new Fixture();
        JavaFxTestUtils.runAndWait(() -> {
            fixture.sync.setInitialMap(openMap(7, 7));
            Position p = new Position(2, 2);
            assertEquals(TileType.GROUND, fixture.sync.gameMap().getTile(p).getType());

            Json tilesDelta = Json.object(Map.of(
                    "tilesUpsert", Json.array(List.of(
                            tile(2, 2, "DESTRUCTIBLE"),
                            tile(-1, 2, "GROUND"),
                            tile(9, 9, "GROUND"),
                            tile(3, 3, "NOT_A_TILE")
                    )),
                    "playersUpsert", Json.emptyArray(),
                    "playersRemove", Json.emptyArray(),
                    "explosions", Json.emptyArray(),
                    "bombs", Json.emptyArray(),
                    "bonuses", Json.emptyArray()
            ));

            fixture.sync.applyDelta(tilesDelta);
            assertEquals(TileType.GROUND, fixture.sync.gameMap().getTile(p).getType());
            assertEquals(0, fixture.redrawCount.get());

            fixture.sync.applyState(Json.object(Map.of(
                    "width", Json.of(7),
                    "height", Json.of(7),
                    "map", Json.of(fixture.sync.gameMap().toJson().stringify()),
                    "players", Json.emptyArray(),
                    "bombs", Json.emptyArray(),
                    "explosions", Json.emptyArray(),
                    "bonuses", Json.emptyArray()
            )));
            assertTrue(fixture.sync.isMapReady());
            assertTrue(fixture.redrawCount.get() >= 1);

            fixture.sync.applyDelta(tilesDelta);
            assertEquals(TileType.DESTRUCTIBLE, fixture.sync.gameMap().getTile(p).getType());
            assertTrue(fixture.redrawCount.get() >= 2);
        });
    }

    @Test
    void applyUdpPlayerSnapshot_respectsSequenceAndClearApis() throws Exception {
        Fixture fixture = new Fixture();
        JavaFxTestUtils.runAndWait(() -> {
            fixture.sync.setInitialMap(openMap(7, 7));

            Json upsertSeq10 = Json.array(List.of(player("me", 1, 1, 3, 1, true, 1.0f)));
            fixture.sync.applyUdpPlayerSnapshot("room-a", 10, upsertSeq10);
            assertEquals(1, fixture.sync.players().get("me").x);

            Json staleSeq9 = Json.array(List.of(player("me", 4, 1, 3, 1, true, 1.0f)));
            fixture.sync.applyUdpPlayerSnapshot("room-a", 9, staleSeq9);
            assertEquals(1, fixture.sync.players().get("me").x);

            Json seq11Dead = Json.array(List.of(player("me", 4, 1, 0, 0, false, 1.0f)));
            fixture.sync.applyUdpPlayerSnapshot("room-a", 11, seq11Dead);
            assertEquals(4, fixture.sync.players().get("me").x);
            assertTrue(fixture.gameOverCalls.contains(Boolean.FALSE));

            fixture.sync.clearLatestUdpSeqForRoom("");
            fixture.sync.clearLatestUdpSeqForRoom("room-a");
            fixture.sync.applyUdpPlayerSnapshot("room-a", 1, Json.array(List.of(player("me", 2, 1, 3, 1, true, 1.0f))));
            assertEquals(2, fixture.sync.players().get("me").x);

            fixture.sync.clearLatestUdpSeqAll();
            fixture.sync.applyUdpPlayerSnapshot("room-a", 0, Json.array(List.of(player("me", 3, 1, 3, 1, true, 1.0f))));
            assertEquals(3, fixture.sync.players().get("me").x);
        });
    }

    @Test
    void applyState_endConditionsCanTriggerVictoryAndRemoveMissingPlayers() throws Exception {
        Fixture fixture = new Fixture();
        JavaFxTestUtils.runAndWait(() -> {
            fixture.sync.setInitialMap(openMap(7, 7));
            String mapJson = fixture.sync.gameMap().toJson().stringify();

            fixture.sync.applyState(Json.object(Map.of(
                    "width", Json.of(7),
                    "height", Json.of(7),
                    "map", Json.of(mapJson),
                    "players", Json.array(List.of(
                            player("me", 2, 2, 3, 1, true, 1.0f),
                            player("enemy", 4, 2, 3, 1, true, 1.0f)
                    )),
                    "bombs", Json.emptyArray(),
                    "explosions", Json.emptyArray(),
                    "bonuses", Json.emptyArray()
            )));
            assertEquals(2, fixture.sync.players().size());

            fixture.sync.applyState(Json.object(Map.of(
                    "width", Json.of(7),
                    "height", Json.of(7),
                    "map", Json.of(mapJson),
                    "players", Json.array(List.of(player("me", 2, 2, 3, 1, true, 1.0f))),
                    "bombs", Json.emptyArray(),
                    "explosions", Json.emptyArray(),
                    "bonuses", Json.emptyArray()
            )));

            assertEquals(1, fixture.sync.players().size());
            assertFalse(fixture.sync.players().containsKey("enemy"));
            assertTrue(fixture.gameOverCalls.contains(Boolean.TRUE));
        });
    }

    @Test
    void applyStateAndDelta_ignoreInvalidPayloadsAndHandleDimensionMismatchGracefully() throws Exception {
        Fixture fixture = new Fixture();
        JavaFxTestUtils.runAndWait(() -> {
            fixture.sync.setInitialMap(openMap(5, 5));
            fixture.sync.applyDelta(null);
            fixture.sync.applyState(null);
            fixture.sync.applyUdpPlayerSnapshot("room", 1, Json.of("not-an-array"));

            fixture.sync.applyState(Json.object(Map.of(
                    "width", Json.of(5),
                    "height", Json.of(5),
                    "map", Json.of("{bad-map-json"),
                    "players", Json.emptyArray(),
                    "bombs", Json.emptyArray(),
                    "explosions", Json.emptyArray(),
                    "bonuses", Json.emptyArray()
            )));

            fixture.sync.applyState(Json.object(Map.of(
                    "width", Json.of(6),
                    "height", Json.of(6),
                    "players", Json.emptyArray(),
                    "bombs", Json.emptyArray(),
                    "explosions", Json.emptyArray(),
                    "bonuses", Json.emptyArray()
            )));

            assertEquals(5, fixture.sync.gameMap().getWidth());
            assertEquals(5, fixture.sync.gameMap().getHeight());
        });
    }

    @Test
    void applyState_assignsSameSkinsRegardlessOfPlayerArrivalOrder() throws Exception {
        Fixture fixtureA = new Fixture();
        Fixture fixtureB = new Fixture();
        JavaFxTestUtils.runAndWait(() -> {
            fixtureA.sync.setInitialMap(openMap(7, 7));
            fixtureB.sync.setInitialMap(openMap(7, 7));

            String mapJson = fixtureA.sync.gameMap().toJson().stringify();
            Json playersOrderA = Json.array(List.of(
                    player("alice", 1, 1, 3, 1, true, 1.0f),
                    player("bob", 2, 1, 3, 1, true, 1.0f),
                    player("charlie", 3, 1, 3, 1, true, 1.0f)
            ));
            Json playersOrderB = Json.array(List.of(
                    player("charlie", 3, 1, 3, 1, true, 1.0f),
                    player("bob", 2, 1, 3, 1, true, 1.0f),
                    player("alice", 1, 1, 3, 1, true, 1.0f)
            ));

            fixtureA.sync.applyState(Json.object(Map.of(
                    "width", Json.of(7),
                    "height", Json.of(7),
                    "map", Json.of(mapJson),
                    "players", playersOrderA,
                    "bombs", Json.emptyArray(),
                    "explosions", Json.emptyArray(),
                    "bonuses", Json.emptyArray()
            )));
            fixtureB.sync.applyState(Json.object(Map.of(
                    "width", Json.of(7),
                    "height", Json.of(7),
                    "map", Json.of(mapJson),
                    "players", playersOrderB,
                    "bombs", Json.emptyArray(),
                    "explosions", Json.emptyArray(),
                    "bonuses", Json.emptyArray()
            )));

            assertEquals(fixtureA.sync.players().get("alice").skinId, fixtureB.sync.players().get("alice").skinId);
            assertEquals(fixtureA.sync.players().get("bob").skinId, fixtureB.sync.players().get("bob").skinId);
            assertEquals(fixtureA.sync.players().get("charlie").skinId, fixtureB.sync.players().get("charlie").skinId);
        });
    }

    @Test
    void applyState_disallowedSkins30And32FallbackToAllowedSkins() throws Exception {
        Fixture fixture = new Fixture();
        JavaFxTestUtils.runAndWait(() -> {
            fixture.sync.setInitialMap(openMap(7, 7));
            String mapJson = fixture.sync.gameMap().toJson().stringify();

            fixture.sync.applyState(Json.object(Map.of(
                    "width", Json.of(7),
                    "height", Json.of(7),
                    "map", Json.of(mapJson),
                    "players", Json.array(List.of(
                            playerWithSkin("p30", 1, 1, 3, 1, true, 1.0f, 30),
                            playerWithSkin("p32", 2, 1, 3, 1, true, 1.0f, 32)
                    )),
                    "bombs", Json.emptyArray(),
                    "explosions", Json.emptyArray(),
                    "bonuses", Json.emptyArray()
            )));

            int skinP30 = fixture.sync.players().get("p30").skinId;
            int skinP32 = fixture.sync.players().get("p32").skinId;
            assertNotEquals(30, skinP30);
            assertNotEquals(32, skinP30);
            assertNotEquals(30, skinP32);
            assertNotEquals(32, skinP32);
        });
    }

    private static Json player(String id, int x, int y, int health, int bombs, boolean alive, float speed) {
        return Json.object(Map.of(
                "id", Json.of(id),
                "x", Json.of(x),
                "y", Json.of(y),
                "health", Json.of(health),
                "bombs", Json.of(bombs),
                "alive", Json.of(alive),
                "speed", Json.of(speed)
        ));
    }

    private static Json playerWithSkin(String id, int x, int y, int health, int bombs, boolean alive, float speed, int skinId) {
        return Json.object(Map.of(
                "id", Json.of(id),
                "x", Json.of(x),
                "y", Json.of(y),
                "health", Json.of(health),
                "bombs", Json.of(bombs),
                "alive", Json.of(alive),
                "speed", Json.of(speed),
                "skinId", Json.of(skinId)
        ));
    }

    private static Json tile(int x, int y, String type) {
        return Json.object(Map.of(
                "x", Json.of(x),
                "y", Json.of(y),
                "type", Json.of(type)
        ));
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

    private static final class Fixture {
        private final SimpleDoubleProperty tileSize = new SimpleDoubleProperty(32.0);
        private final Pane entityLayer = new Pane();
        private final Pane bombLayer = new Pane();
        private final Pane bonusLayer = new Pane();
        private final Pane explosionLayer = new Pane();
        private String currentPlayerId = "me";
        private boolean cleanedUp = false;
        private boolean gameEnded = false;
        private final List<Boolean> gameOverCalls = new ArrayList<>();
        private final AtomicInteger health = new AtomicInteger(-1);
        private final AtomicInteger bombs = new AtomicInteger(-1);
        private final AtomicInteger redrawCount = new AtomicInteger(0);
        private final GameStateSynchronizer sync = new GameStateSynchronizer(
                tileSize,
                entityLayer,
                bombLayer,
                bonusLayer,
                explosionLayer,
                () -> currentPlayerId,
                () -> cleanedUp,
                () -> gameEnded,
                gameOverCalls::add,
                health::set,
                bombs::set,
                redrawCount::incrementAndGet
        );
    }
}
