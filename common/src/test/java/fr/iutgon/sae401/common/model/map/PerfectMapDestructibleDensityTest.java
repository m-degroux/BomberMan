package fr.iutgon.sae401.common.model.map;

import fr.iutgon.sae401.common.model.entity.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerfectMapDestructibleDensityTest {
	@Test
	void densityZeroProducesNoDestructibles() {
		BombermanMapFactory factory = new BombermanMapFactory(0f);
		GameMap map = factory.create(15, 13, MapTheme.PERFECT);
		assertEquals(0, countTiles(map, TileType.DESTRUCTIBLE));
	}

	@Test
	void densityOneProducesDestructiblesButKeepsSpawnGround() {
		BombermanMapFactory factory = new BombermanMapFactory(1f);
		GameMap map = factory.create(15, 13, MapTheme.PERFECT);

		// spawns (inside border) must remain walkable ground
		assertAll(
				() -> assertEquals(TileType.GROUND, map.getTile(new Position(1, 1)).getType()),
				() -> assertEquals(TileType.GROUND, map.getTile(new Position(2, 1)).getType()),
				() -> assertEquals(TileType.GROUND, map.getTile(new Position(1, 2)).getType()),
				() -> assertEquals(TileType.GROUND, map.getTile(new Position(2, 2)).getType()),
				() -> assertEquals(TileType.GROUND, map.getTile(new Position(map.getWidth() - 2, 1)).getType()),
				() -> assertEquals(TileType.GROUND, map.getTile(new Position(map.getWidth() - 3, 1)).getType()),
				() -> assertEquals(TileType.GROUND, map.getTile(new Position(1, map.getHeight() - 2)).getType()),
				() -> assertEquals(TileType.GROUND, map.getTile(new Position(map.getWidth() - 2, map.getHeight() - 2)).getType())
		);

		// borders must stay solid WALL
		for (int x = 0; x < map.getWidth(); x++) {
			assertEquals(TileType.WALL, map.getTile(new Position(x, 0)).getType());
			assertEquals(TileType.WALL, map.getTile(new Position(x, map.getHeight() - 1)).getType());
		}
		for (int y = 0; y < map.getHeight(); y++) {
			assertEquals(TileType.WALL, map.getTile(new Position(0, y)).getType());
			assertEquals(TileType.WALL, map.getTile(new Position(map.getWidth() - 1, y)).getType());
		}

		assertTrue(countTiles(map, TileType.DESTRUCTIBLE) > 0);
	}

	private static int countTiles(GameMap map, TileType type) {
		int count = 0;
		for (int y = 0; y < map.getHeight(); y++) {
			for (int x = 0; x < map.getWidth(); x++) {
				Tile tile = map.getTile(new Position(x, y));
				if (tile != null && tile.getType() == type) {
					count++;
				}
			}
		}
		return count;
	}
}
