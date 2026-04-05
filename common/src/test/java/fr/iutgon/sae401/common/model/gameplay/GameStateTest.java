package fr.iutgon.sae401.common.model.gameplay;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.bonus.BonusPickup;
import fr.iutgon.sae401.common.model.bonus.BonusType;
import fr.iutgon.sae401.common.model.entity.Bomb;
import fr.iutgon.sae401.common.model.entity.Explosion;
import fr.iutgon.sae401.common.model.entity.Player;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.model.map.Tile;
import fr.iutgon.sae401.common.model.map.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameStateTest {

    @Test
    void addAndRemoveEntities_updateCollectionsCorrectly() {
        GameState state = new GameState(new GameMap(2, 2, MapTheme.CLASSIC));
        Player player = new Player("p1", new Position(0, 0), 1, 1);
        Bomb bomb = new Bomb("b1", new Position(0, 1), "p1", 1000, 1);
        Explosion explosion = new Explosion(java.util.List.of(new Position(1, 1)), 500);
        BonusPickup bonus = new BonusPickup(new Position(1, 0), BonusType.SPEED);

        state.addPlayer(player);
        state.addBomb(bomb);
        state.addExplosion(explosion);
        state.addBonus(bonus);

        assertEquals(player, state.getPlayerById("p1"));
        assertTrue(state.getBonuses().contains(bonus));

        state.removeBomb(bomb);
        state.removeExplosion(explosion);
        state.removeBonus(bonus);

        assertFalse(state.getBombs().contains(bomb));
        assertFalse(state.getExplosions().contains(explosion));
        assertFalse(state.getBonuses().contains(bonus));
    }

    @Test
    void getAlivePlayers_andWinner_handleAliveStatus() {
        GameState state = new GameState(new GameMap(2, 2, MapTheme.CLASSIC));
        Player alive = new Player("alive", new Position(0, 0), 1, 1);
        Player dead = new Player("dead", new Position(1, 1), 1, 1);
        dead.die();

        state.addPlayer(alive);
        state.addPlayer(dead);

        assertEquals(1, state.getAlivePlayers().size());
        assertEquals(alive, state.getWinner());
        assertTrue(state.isGameOver());
    }

    @Test
    void replacePlayer_swapsInstanceById() {
        GameState state = new GameState(new GameMap(1, 1, MapTheme.CLASSIC));
        Player original = new Player("p2", new Position(0, 0), 2, 1);
        Player replacement = new Player("p2", new Position(0, 0), 3, 2);

        state.addPlayer(original);
        state.replacePlayer("p2", replacement);

        assertSame(replacement, state.getPlayerById("p2"));
    }

    @Test
    void toJson_andFromJson_roundTripPreservesState() {
        GameMap map = new GameMap(2, 1, MapTheme.CLASSIC);
        map.setTile(new Position(0, 0), new Tile(TileType.GROUND));
        GameState state = new GameState(map);
        state.addPlayer(new Player("p3", new Position(0, 0), 1, 1));
        state.addBomb(new Bomb("b2", new Position(1, 0), "p3", 1000, 1));
        state.addExplosion(new Explosion(java.util.List.of(new Position(1, 0)), 1000));

        Json json = state.toJson();
        GameState restored = GameState.fromJson(json);

        assertEquals(1, restored.getPlayers().size());
        assertEquals(1, restored.getBombs().size());
        assertEquals(1, restored.getExplosions().size());
        assertEquals(2, restored.getMap().getWidth());
    }
}
