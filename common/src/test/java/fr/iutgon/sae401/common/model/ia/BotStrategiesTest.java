package fr.iutgon.sae401.common.model.ia;

import fr.iutgon.sae401.common.model.entity.Bomb;
import fr.iutgon.sae401.common.model.entity.Direction;
import fr.iutgon.sae401.common.model.entity.Explosion;
import fr.iutgon.sae401.common.model.entity.Player;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.gameplay.ActionType;
import fr.iutgon.sae401.common.model.gameplay.GameState;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.model.map.Tile;
import fr.iutgon.sae401.common.model.map.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotStrategiesTest {

    @Test
    void kamikazeBreaksBlocksWhenIsolatedFromEnemy() {
        GameState state = new GameState(openMap(7, 5));
        state.getMap().setTile(new Position(3, 1), new Tile(TileType.DESTRUCTIBLE));
        state.getMap().setTile(new Position(3, 2), new Tile(TileType.DESTRUCTIBLE));
        state.getMap().setTile(new Position(3, 3), new Tile(TileType.DESTRUCTIBLE));

        Player bot = player("bot", 2, 2, 1);
        Player enemy = player("enemy", 5, 2, 1);
        state.addPlayer(bot);
        state.addPlayer(enemy);

        KamikazeStrategy strategy = new KamikazeStrategy();

        ActionType action = strategy.decideNextMove(state, bot).getType();
        assertTrue(action == ActionType.PLACE_BOMB || action == ActionType.MOVE);
    }

    @Test
    void survivorCanAttackWhileEscapingFromAnotherBomb() {
        GameState state = new GameState(openMap(7, 7));

        Player bot = player("bot", 3, 3, 1);
        Player enemy = player("enemy", 4, 3, 1);
        state.addPlayer(bot);
        state.addPlayer(enemy);
        state.addBomb(Bomb.createUnique(new Position(2, 3), "other", 1000, 1));

        SurvivorStrategy strategy = new SurvivorStrategy();

        ActionType action = strategy.decideNextMove(state, bot).getType();
        assertTrue(action == ActionType.PLACE_BOMB || action == ActionType.MOVE);
    }

    @Test
    void survivorBreaksBlocksWhenIsolatedFromEnemy() {
        GameState state = new GameState(openMap(7, 5));
        state.getMap().setTile(new Position(3, 1), new Tile(TileType.DESTRUCTIBLE));
        state.getMap().setTile(new Position(3, 2), new Tile(TileType.DESTRUCTIBLE));
        state.getMap().setTile(new Position(3, 3), new Tile(TileType.DESTRUCTIBLE));

        Player bot = player("bot", 2, 2, 1);
        Player enemy = player("enemy", 5, 2, 1);
        state.addPlayer(bot);
        state.addPlayer(enemy);

        SurvivorStrategy strategy = new SurvivorStrategy();

        ActionType action = strategy.decideNextMove(state, bot).getType();
        assertTrue(action == ActionType.PLACE_BOMB || action == ActionType.MOVE);
    }

    @Test
    void botsDoNotPlantUnsafeBombWhenNoEscapeExists() {
        GameState state = new GameState(openMap(5, 5));
        state.getMap().setTile(new Position(2, 1), new Tile(TileType.WALL));
        state.getMap().setTile(new Position(1, 2), new Tile(TileType.WALL));
        state.getMap().setTile(new Position(3, 2), new Tile(TileType.DESTRUCTIBLE));
        state.getMap().setTile(new Position(2, 3), new Tile(TileType.WALL));

        Player bot = player("bot", 2, 2, 1);
        Player enemy = player("enemy", 3, 2, 1);
        state.addPlayer(bot);
        state.addPlayer(enemy);

        assertTrue(BotStrategySupport.canPlaceBomb(state, bot));
        assertTrue(!BotStrategySupport.canSafelyPlantBomb(state, bot));
        assertTrue(new KamikazeStrategy().decideNextMove(state, bot).getType() != ActionType.PLACE_BOMB);
        assertTrue(new SurvivorStrategy().decideNextMove(state, bot).getType() != ActionType.PLACE_BOMB);
    }

    @Test
    void kamikazeEscapesWhenThreatenedEvenIfEnemyIsInBombRange() {
        GameState state = new GameState(openMap(7, 7));

        Player bot = player("bot", 3, 3, 1);
        Player enemy = player("enemy", 4, 3, 1);
        state.addPlayer(bot);
        state.addPlayer(enemy);
        state.addBomb(Bomb.createUnique(new Position(2, 3), "other", 1000, 1));

        KamikazeStrategy strategy = new KamikazeStrategy();

        assertEquals(ActionType.MOVE, strategy.decideNextMove(state, bot).getType());
    }

    @Test
    void kamikazeDoesNotPlantWallBombWhenTargetIsStillFarAway() {
        GameState state = new GameState(openMap(11, 5));
        state.getMap().setTile(new Position(3, 1), new Tile(TileType.DESTRUCTIBLE));
        state.getMap().setTile(new Position(3, 2), new Tile(TileType.DESTRUCTIBLE));
        state.getMap().setTile(new Position(3, 3), new Tile(TileType.DESTRUCTIBLE));

        Player bot = player("bot", 2, 2, 1);
        Player enemy = player("enemy", 9, 2, 1);
        state.addPlayer(bot);
        state.addPlayer(enemy);

        ActionType action = new KamikazeStrategy().decideNextMove(state, bot).getType();
        assertTrue(action != ActionType.PLACE_BOMB);
    }

    @Test
    void survivorPushesTowardAttackPositionInsteadOfRetreatingByDefault() {
        GameState state = new GameState(horizontalCorridorMap(9, 5, 2));

        Player bot = player("bot", 2, 2, 1);
        Player enemy = player("enemy", 6, 2, 1);
        state.addPlayer(bot);
        state.addPlayer(enemy);

        var action = new SurvivorStrategy().decideNextMove(state, bot);
        assertEquals(ActionType.MOVE, action.getType());
        assertEquals(Direction.RIGHT, action.getDirection());
    }

    @Test
    void eliteBotPlantsBombWhenKillIsSafe() {
        GameState state = new GameState(openMap(7, 7));

        Player bot = player("elite", 3, 3, 1);
        Player enemy = player("enemy", 4, 3, 1);
        state.addPlayer(bot);
        state.addPlayer(enemy);

        EliteBotStrategy strategy = new EliteBotStrategy();

        assertEquals(ActionType.PLACE_BOMB, strategy.decideNextMove(state, bot).getType());
    }

    @Test
    void eliteBotRemembersEscapeAfterPlantingBomb() {
        GameState state = new GameState(openMap(7, 7));

        Player bot = player("elite", 3, 3, 1);
        Player enemy = player("enemy", 4, 3, 1);
        state.addPlayer(bot);
        state.addPlayer(enemy);

        EliteBotStrategy strategy = new EliteBotStrategy();

        assertEquals(ActionType.PLACE_BOMB, strategy.decideNextMove(state, bot).getType());
        state.addBomb(Bomb.createUnique(new Position(3, 3), "elite", 1000, 1));

        assertEquals(ActionType.MOVE, strategy.decideNextMove(state, bot).getType());
    }

    @Test
    void eliteBotAvoidsActiveExplosionTiles() {
        GameState state = new GameState(openMap(7, 7));

        Player bot = player("elite", 3, 3, 1);
        Player enemy = player("enemy", 5, 3, 1);
        state.addPlayer(bot);
        state.addPlayer(enemy);
        state.addExplosion(new Explosion(new Position(3, 3), java.util.List.of(
                new Position(3, 3),
                new Position(4, 3),
                new Position(2, 3)
        ), 500));

        EliteBotStrategy strategy = new EliteBotStrategy();

        assertEquals(ActionType.MOVE, strategy.decideNextMove(state, bot).getType());
    }

    private static Player player(String id, int x, int y, int range) {
        Player player = new Player(id, new Position(x, y), 3, 3);
        player.setBombRange(range);
        return player;
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

    private static GameMap horizontalCorridorMap(int width, int height, int corridorY) {
        GameMap map = openMap(width, height);
        for (int y = 1; y < height - 1; y++) {
            if (y == corridorY) {
                continue;
            }
            for (int x = 1; x < width - 1; x++) {
                map.setTile(new Position(x, y), new Tile(TileType.WALL));
            }
        }
        return map;
    }
}
