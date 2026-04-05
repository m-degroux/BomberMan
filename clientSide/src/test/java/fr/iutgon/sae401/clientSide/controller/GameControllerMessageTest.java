package fr.iutgon.sae401.clientSide.controller;

import fr.iutgon.sae401.TestReflectionUtils;
import fr.iutgon.sae401.clientSide.JavaFxTestUtils;
import fr.iutgon.sae401.clientSide.controller.game.GameStateSynchronizer;
import fr.iutgon.sae401.clientSide.view.ChatOverlay;
import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.model.map.Tile;
import fr.iutgon.sae401.common.model.map.TileType;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameControllerMessageTest {

    @BeforeAll
    static void initJavaFx() throws Exception {
        JavaFxTestUtils.initToolkit();
    }

    @Test
    void onMessage_chatAppendsOnlyWhenMessageIsNotBlank() throws Exception {
        Fixture fixture = new Fixture();
        fixture.controller.onMessage(new MessageEnvelope("chat", "c1", Json.object(Map.of(
                "from", Json.of("alice"),
                "message", Json.of("   ")
        ))));
        JavaFxTestUtils.runAndWait(() -> {
        });
        assertTrue(fixture.chat.messages.isEmpty());

        fixture.controller.onMessage(new MessageEnvelope("chat", "c2", Json.object(Map.of(
                "from", Json.of("bob"),
                "message", Json.of("hello")
        ))));
        JavaFxTestUtils.runAndWait(() -> {
        });
        assertEquals(List.of("bob:hello"), fixture.chat.messages);
    }

    @Test
    void onMessage_gameInitGameDeltaGamePositionsAndGameState_updateSynchronizer() throws Exception {
        Fixture fixture = new Fixture();

        String mapJson = fixture.sync.gameMap().toJson().stringify();
        Json initState = Json.object(Map.of(
                "width", Json.of(7),
                "height", Json.of(7),
                "map", Json.of(mapJson),
                "players", Json.array(List.of(player("p1", 2, 2, 3, 1, true))),
                "bombs", Json.emptyArray(),
                "explosions", Json.emptyArray(),
                "bonuses", Json.emptyArray()
        ));
        fixture.controller.onMessage(new MessageEnvelope("game_init", "i1", Json.object(Map.of(
                "roomId", Json.of("room-a"),
                "state", initState
        ))));
        JavaFxTestUtils.runAndWait(() -> {
        });
        assertEquals(1, fixture.sync.players().size());

        Json delta = Json.object(Map.of(
                "playersUpsert", Json.array(List.of(player("p1", 3, 2, 3, 1, true))),
                "playersRemove", Json.emptyArray(),
                "tilesUpsert", Json.emptyArray(),
                "explosions", Json.emptyArray(),
                "bombs", Json.emptyArray(),
                "bonuses", Json.emptyArray()
        ));
        fixture.controller.onMessage(new MessageEnvelope("game_delta", "d1", Json.object(Map.of("delta", delta))));
        JavaFxTestUtils.runAndWait(() -> {
        });
        assertEquals(3, fixture.sync.players().get("p1").x);

        fixture.controller.onMessage(new MessageEnvelope("game_positions", "p1", Json.object(Map.of(
                "roomId", Json.of("room-a"),
                "seq", Json.of(1L),
                "players", Json.array(List.of(player("p1", 4, 2, 3, 1, true)))
        ))));
        JavaFxTestUtils.runAndWait(() -> {
        });
        assertEquals(4, fixture.sync.players().get("p1").x);

        Json state2 = Json.object(Map.of(
                "width", Json.of(7),
                "height", Json.of(7),
                "map", Json.of(mapJson),
                "players", Json.array(List.of(player("p1", 5, 2, 3, 1, true))),
                "bombs", Json.emptyArray(),
                "explosions", Json.emptyArray(),
                "bonuses", Json.emptyArray()
        ));
        fixture.controller.onMessage(new MessageEnvelope("game_state", "s1", Json.object(Map.of("state", state2))));
        JavaFxTestUtils.runAndWait(() -> {
        });
        assertEquals(5, fixture.sync.players().get("p1").x);
    }

    @Test
    void onMessage_ignoresInvalidPayloadShapes() throws Exception {
        Fixture fixture = new Fixture();

        fixture.controller.onMessage(new MessageEnvelope("game_init", "i1", Json.emptyObject()));
        fixture.controller.onMessage(new MessageEnvelope("game_delta", "d1", Json.object(Map.of("delta", Json.of("bad")))));
        fixture.controller.onMessage(new MessageEnvelope("game_positions", "p1", Json.object(Map.of(
                "roomId", Json.of("room-a"),
                "seq", Json.of(-1L),
                "players", Json.array(List.of(player("p1", 1, 1, 3, 1, true)))
        ))));
        fixture.controller.onMessage(new MessageEnvelope("game_positions", "p2", Json.object(Map.of(
                "roomId", Json.of("room-a"),
                "seq", Json.of(2L),
                "players", Json.of("bad")
        ))));
        fixture.controller.onMessage(new MessageEnvelope("unknown_type", "u1", Json.emptyObject()));
        fixture.controller.onMessage(null);

        JavaFxTestUtils.runAndWait(() -> {
        });
        assertTrue(fixture.sync.players().isEmpty());
    }

    @Test
    void onMessage_roomRestarted_setsControllerAsCleanedUp_andRestartFailedIsHandled() throws Exception {
        Fixture fixture = new Fixture();
        JavaFxTestUtils.runAndWait(() ->
                fixture.sync.applyUdpPlayerSnapshot("room-a", 10, Json.array(List.of(player("p1", 1, 1, 3, 1, true))))
        );
        fixture.controller.onMessage(new MessageEnvelope("restart_failed", "r1", Json.object(Map.of("reason", Json.of("nope")))));
        fixture.controller.onMessage(new MessageEnvelope("room_restarted", "r2", Json.emptyObject()));
        JavaFxTestUtils.runAndWait(() -> {
        });

        Object cleanedUp = TestReflectionUtils.getField(fixture.controller, "cleanedUp");
        assertEquals(Boolean.TRUE, cleanedUp);

        JavaFxTestUtils.runAndWait(() -> {
            fixture.sync.clearLatestUdpSeqAll();
            fixture.sync.applyUdpPlayerSnapshot("room-a", 1, Json.array(List.of(player("p1", 2, 1, 3, 1, true))));
        });
        assertEquals(2, fixture.sync.players().get("p1").x);
    }

    private static Json player(String id, int x, int y, int health, int bombs, boolean alive) {
        return Json.object(Map.of(
                "id", Json.of(id),
                "x", Json.of(x),
                "y", Json.of(y),
                "health", Json.of(health),
                "bombs", Json.of(bombs),
                "alive", Json.of(alive),
                "speed", Json.of(1.0f)
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

    private static final class CapturingChatOverlay extends ChatOverlay {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void appendMessage(String from, String message) {
            messages.add(from + ":" + message);
        }
    }

    private static final class Fixture {
        private final GameController controller = new GameController();
        private final CapturingChatOverlay chat = new CapturingChatOverlay();
        private final AtomicInteger health = new AtomicInteger();
        private final AtomicInteger bombs = new AtomicInteger();
        private final GameStateSynchronizer sync;

        private Fixture() throws Exception {
            SimpleDoubleProperty tileSize = new SimpleDoubleProperty(32.0);
            Pane entity = new Pane();
            Pane bombLayer = new Pane();
            Pane bonusLayer = new Pane();
            Pane explosionLayer = new Pane();
            sync = new GameStateSynchronizer(
                    tileSize,
                    entity,
                    bombLayer,
                    bonusLayer,
                    explosionLayer,
                    () -> "p1",
                    () -> false,
                    () -> false,
                    _victory -> {
                    },
                    health::set,
                    bombs::set,
                    () -> {
                    }
            );
            sync.setInitialMap(openMap(7, 7));

            TestReflectionUtils.setField(controller, "stateSynchronizer", sync);
            TestReflectionUtils.setField(controller, "chatOverlay", chat);
            TestReflectionUtils.setField(controller, "gamePane", new Pane());
        }
    }
}
