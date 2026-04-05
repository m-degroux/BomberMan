package fr.iutgon.sae401.common.model.map;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente la carte du jeu sous forme de grille.
 */
public class GameMap {

	private final int width;
	private final int height;
	private final Tile[][] grid;
	private final MapTheme theme;

	public GameMap(int width, int height, MapTheme theme) {
		this.width = width;
		this.height = height;
		this.grid = new Tile[height][width];
		this.theme = theme;
	}

	public MapTheme getTheme() {
		return theme;
	}

	public boolean isInside(Position pos) {
		return pos.getX() >= 0 && pos.getX() < width && pos.getY() >= 0 && pos.getY() < height;
	}

	public Tile getTile(Position pos) {
		if (!isInside(pos))
			return null;
		return grid[pos.getY()][pos.getX()];
	}

	public void setTile(Position pos, Tile tile) {
		if (isInside(pos)) {
			grid[pos.getY()][pos.getX()] = tile;
		}
	}

	public boolean isWalkable(Position pos) {
		if (!isInside(pos))
			return false;
		Tile tile = getTile(pos);
		return tile != null && tile.isWalkable();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Json toJson() {
		List<Json> rows = new ArrayList<>();
		for (int y = 0; y < height; y++) {
			List<Json> row = new ArrayList<>();
			for (int x = 0; x < width; x++) {
				Tile tile = grid[y][x];
				row.add(tile == null ? Json.nullValue() : tile.toJson());
			}
			rows.add(Json.array(row));
		}
		return Json.object(java.util.Map.of(
				"theme", Json.of(theme == null ? MapTheme.CLASSIC.name() : theme.name()),
				"tiles", Json.array(rows)
		));
	}

	/**
	 * Compatibilité: parse avec thème par défaut.
	 */
	public static GameMap fromJson(String json) {
		return fromJson(json, MapTheme.CLASSIC);
	}

	/**
	 * Parse une carte. Supporte:
	 * - legacy: JSON array de rows
	 * - moderne: { theme: "CLASSIC", tiles: [...] }
	 */
	public static GameMap fromJson(String json, MapTheme fallbackTheme) {
		Json parsed = Json.parse(json);
		return fromJson(parsed, fallbackTheme);
	}

	private static GameMap fromJson(Json json, MapTheme fallbackTheme) {
		if (json == null) {
			throw new IllegalArgumentException("GameMap JSON must not be null");
		}

		MapTheme theme = fallbackTheme == null ? MapTheme.CLASSIC : fallbackTheme;
		Json tilesJson;
		if (json.isObject()) {
			String themeStr = json.value("theme", (String) null);
			if (themeStr != null && !themeStr.isBlank()) {
				try {
					theme = MapTheme.valueOf(themeStr.trim().toUpperCase());
				} catch (Exception ignored) {
					// keep fallback
				}
			}
			tilesJson = json.value("tiles", Json.emptyArray());
		} else {
			// legacy: root is the tiles array
			tilesJson = json;
		}

		if (!tilesJson.isArray()) {
			throw new IllegalArgumentException("GameMap JSON must contain an array of tiles");
		}
		var rows = tilesJson.asArray();
		int height = rows.size();
		int width = height == 0 ? 0 : (rows.get(0).isArray() ? rows.get(0).asArray().size() : 0);
		GameMap map = new GameMap(width, height, theme);
		for (int y = 0; y < height; y++) {
			Json rowJson = rows.get(y);
			if (rowJson == null || !rowJson.isArray()) {
				continue;
			}
			var cols = rowJson.asArray();
			for (int x = 0; x < cols.size(); x++) {
				Json tileJson = cols.get(x);
				Tile tile = (tileJson == null || tileJson.isNull()) ? null : Tile.fromJson(tileJson);
				map.setTile(new Position(x, y), tile);
			}
		}
		return map;
	}
}