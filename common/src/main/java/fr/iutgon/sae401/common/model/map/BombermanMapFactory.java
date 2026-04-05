package fr.iutgon.sae401.common.model.map;

import fr.iutgon.sae401.common.model.entity.Position;

import java.util.*;

/**
 * Fabrique de cartes avec support de thèmes.
 */
public class BombermanMapFactory implements MapFactory {

	private final float destructibleDensity;

	public BombermanMapFactory(float destructibleDensity) {
		if (destructibleDensity < 0f || destructibleDensity > 1f) {
			throw new IllegalArgumentException("Density must be between 0 and 1");
		}
		this.destructibleDensity = destructibleDensity;
	}

	@Override
	public GameMap create(int width, int height, MapTheme theme) {
		MapTheme safeTheme = theme == null ? MapTheme.CLASSIC : theme;
		GameMap map = new GameMap(width, height, safeTheme);

		if (safeTheme == MapTheme.CLASSIC) {
			generateClassic(map);
		} else {
			generatePerfectMaze(map);
		}

		return map;
	}

	private void generateClassic(GameMap map) {
		Random random = new Random();

		int width = map.getWidth();
		int height = map.getHeight();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				Position pos = new Position(x, y);

				if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
					map.setTile(pos, new Tile(TileType.WALL));
					continue;
				}

				if (x % 2 == 0 && y % 2 == 0) {
					map.setTile(pos, new Tile(TileType.WALL));
					continue;
				}

				if (isSpawnArea(x, y, width, height)) {
					map.setTile(pos, new Tile(TileType.GROUND));
					continue;
				}

				if (random.nextFloat() < destructibleDensity) {
					map.setTile(pos, new Tile(TileType.DESTRUCTIBLE));
				} else {
					map.setTile(pos, new Tile(TileType.GROUND));
				}
			}
		}
	}

	private boolean isSpawnArea(int x, int y, int width, int height) {
		if (x <= 2 && y <= 2)
			return true;
		if (x >= width - 3 && y <= 2)
			return true;
		if (x <= 2 && y >= height - 3)
			return true;
		if (x >= width - 3 && y >= height - 3)
			return true;
		return false;
	}

	private void generatePerfectMaze(GameMap map) {
		int width = map.getWidth();
		int height = map.getHeight();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				map.setTile(new Position(x, y), new Tile(TileType.WALL));
			}
		}

		carve(1, 1, map);
		ensureSpawnAreasOpen(map);
		placeDestructiblesOnWalls(map);
	}

	private void ensureSpawnAreasOpen(GameMap map) {
		int width = map.getWidth();
		int height = map.getHeight();
		// Important: do not touch the outer border (must remain WALL)
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				if (!isSpawnArea(x, y, width, height)) {
					continue;
				}
				map.setTile(new Position(x, y), new Tile(TileType.GROUND));
			}
		}
	}

	private void placeDestructiblesOnWalls(GameMap map) {
		if (destructibleDensity <= 0f) {
			return;
		}
		Random random = new Random();
		int width = map.getWidth();
		int height = map.getHeight();
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				if (isSpawnArea(x, y, width, height)) {
					continue;
				}
				Position pos = new Position(x, y);
				Tile tile = map.getTile(pos);
				// Only replace walls, never put destructibles in corridors.
				if (tile == null || tile.getType() != TileType.WALL) {
					continue;
				}
				if (random.nextFloat() < destructibleDensity) {
					map.setTile(pos, new Tile(TileType.DESTRUCTIBLE));
				}
			}
		}
	}

	private void carve(int x, int y, GameMap map) {

		map.setTile(new Position(x, y), new Tile(TileType.GROUND));

		List<int[]> dirs = new ArrayList<>(
				List.of(new int[] { 0, -2 }, new int[] { 0, 2 }, new int[] { -2, 0 }, new int[] { 2, 0 }));

		Collections.shuffle(dirs);

		for (int[] d : dirs) {
			int nx = x + d[0];
			int ny = y + d[1];

			if (nx > 0 && ny > 0 && nx < map.getWidth() - 1 && ny < map.getHeight() - 1) {

				if (map.getTile(new Position(nx, ny)).getType() == TileType.WALL) {

					map.setTile(new Position(x + d[0] / 2, y + d[1] / 2), new Tile(TileType.GROUND));

					carve(nx, ny, map);
				}
			}
		}
	}
}