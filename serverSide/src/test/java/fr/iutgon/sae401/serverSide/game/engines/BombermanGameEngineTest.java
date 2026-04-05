package fr.iutgon.sae401.serverSide.game.engines;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.gameplay.GameConfig;
import fr.iutgon.sae401.common.model.dto.PlayerDTO;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import fr.iutgon.sae401.serverSide.server.clients.ClientRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BombermanGameEngineTest {

    @Test
    void onClientConnected_addsHumanPlayer() {
        BombermanGameEngine engine = createEngine(new GameConfig(7, 7, 3, 1, 1, 2000, 0.1f));

        engine.onClientConnected(new ClientId("player-1"));

        assertFalse(engine.snapshotPlayersDTO().isEmpty());
        assertEquals(1, engine.snapshotPlayersDTO().size());
        assertEquals(1, engine.stateVersion());
    }

    @Test
    void ensureBots_doesNotSpawnWithoutRealPlayers() {
        BombermanGameEngine engine = createEngine(new GameConfig(7, 7, 3, 1, 1, 2000, 0.1f));

        engine.ensureBots(3);

        assertTrue(engine.snapshotPlayersDTO().isEmpty());
    }

    @Test
    void ensureBots_addsBotsWhenARealPlayerIsPresent() {
        BombermanGameEngine engine = createEngine(new GameConfig(7, 7, 3, 1, 1, 2000, 0.1f));
        engine.onClientConnected(new ClientId("human-1"));

        engine.ensureBots(4);

        assertEquals(4, engine.snapshotPlayersDTO().size());
        assertTrue(engine.snapshotPlayersDTO().stream().anyMatch(p -> p.id.startsWith("bot-")));
    }

    @Test
    void onClientDisconnected_removesBotsAndFinishesMatchWhenLastHumanLeaves() {
        BombermanGameEngine engine = createEngine(new GameConfig(7, 7, 3, 1, 1, 2000, 0.1f));
        engine.onClientConnected(new ClientId("human-2"));
        engine.ensureBots(4);

        engine.onClientDisconnected(new ClientId("human-2"));

        assertTrue(engine.isMatchFinished());
        assertTrue(engine.snapshotPlayersDTO().isEmpty());
    }

    @Test
    void onInput_placesBombWhenPlayerRequestsBomb() {
        BombermanGameEngine engine = createEngine(new GameConfig(7, 7, 3, 1, 1, 2000, 0.1f));
        ClientId playerId = new ClientId("player-3");
        engine.onClientConnected(playerId);

        engine.onInput(playerId, Json.object(java.util.Map.of("bomb", Json.of(true))));

        assertEquals(1, engine.snapshotBombsDTO().size());
    }

    @Test
    void tick_explodesBombAndCreatesAnExplosion() {
        BombermanGameEngine engine = createEngine(new GameConfig(7, 7, 3, 1, 1, 500, 0.1f));
        ClientId playerId = new ClientId("player-4");
        engine.onClientConnected(playerId);
        engine.onInput(playerId, Json.object(java.util.Map.of("bomb", Json.of(true))));

        assertEquals(1, engine.snapshotBombsDTO().size());

        engine.tick(0.6);

        assertTrue(engine.snapshotBombsDTO().isEmpty(), "Bomb should have exploded and been removed");
        assertEquals(1, engine.snapshotExplosionsDTO().size(), "An explosion should be present after the bomb explodes");
    }

    @Test
    void tick_chainReaction_detonatesNearbyBombsBeforeTheirOwnTimer() {
        BombermanGameEngine engine = createEngine(new GameConfig(7, 7, 3, 2, 2, 1500, 0.0f));
        ClientId playerId = new ClientId("player-chain");
        engine.onClientConnected(playerId);
        engine.onClientConnected(new ClientId("player-other"));

        PlayerDTO before = requirePlayer(engine, playerId.value());
        engine.onInput(playerId, Json.object(java.util.Map.of("bomb", Json.of(true))));
        assertEquals(1, engine.snapshotBombsDTO().size());

        assertTrue(tryMoveToAdjacentWalkableTile(engine, playerId, before.x, before.y));
        engine.tick(1.0);
        assertEquals(1, engine.snapshotBombsDTO().size(), "First bomb should not have exploded yet");

        engine.onInput(playerId, Json.object(java.util.Map.of("bomb", Json.of(true))));
        assertEquals(2, engine.snapshotBombsDTO().size(), "Second bomb should be placed while first is still active");

        engine.tick(0.6);

        var remainingBombs = engine.snapshotBombsDTO();
        assertTrue(
                remainingBombs.isEmpty(),
                "Second bomb should be detonated by chain reaction. Remaining bombs="
                        + remainingBombs.stream().map(b -> "(" + b.x + "," + b.y + ") t=" + b.timerMs).toList()
        );
        assertTrue(engine.snapshotExplosionsDTO().size() >= 2, "Chain reaction should create at least two explosions");
    }

    private static BombermanGameEngine createEngine(GameConfig config) {
        return new BombermanGameEngine(config, new NoopClientRegistry());
    }

    private static PlayerDTO requirePlayer(BombermanGameEngine engine, String playerId) {
        return engine.snapshotPlayersDTO().stream()
                .filter(p -> p != null && playerId.equals(p.id))
                .findFirst()
                .orElseThrow();
    }

    private static boolean tryMoveToAdjacentWalkableTile(BombermanGameEngine engine, ClientId playerId, int x, int y) {
        int[][] dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            engine.onInput(playerId, Json.object(java.util.Map.of(
                    "x", Json.of(nx),
                    "y", Json.of(ny)
            )));
            PlayerDTO moved = requirePlayer(engine, playerId.value());
            if (moved.x == nx && moved.y == ny) {
                return true;
            }
        }
        return false;
    }

    private static final class NoopClientRegistry implements ClientRegistry {
        @Override
        public void register(fr.iutgon.sae401.serverSide.server.ClientContext client) {
        }

        @Override
        public void unregister(ClientId id) {
        }

        @Override
        public Optional<fr.iutgon.sae401.serverSide.server.ClientContext> get(ClientId id) {
            return Optional.empty();
        }

        @Override
        public Collection<ClientId> ids() {
            return java.util.Collections.emptyList();
        }

        @Override
        public boolean send(ClientId id, MessageEnvelope message) {
            return false;
        }

        @Override
        public int broadcast(MessageEnvelope message) {
            return 0;
        }
    }
}
