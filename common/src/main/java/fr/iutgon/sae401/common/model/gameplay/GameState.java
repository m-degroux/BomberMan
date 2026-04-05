package fr.iutgon.sae401.common.model.gameplay;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.bonus.BonusPickup;
import fr.iutgon.sae401.common.model.entity.Bomb;
import fr.iutgon.sae401.common.model.entity.Explosion;
import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Player;
import fr.iutgon.sae401.common.model.map.GameMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente l'état global d'une partie. Contient tous les éléments dynamiques
 * du jeu : joueurs, bombes, explosions et la carte.
 */
public class GameState {

    private final List<IPlayer> players;
    private final List<Bomb> bombs;
    private final List<Explosion> explosions;
    private final List<BonusPickup> bonuses;
    private final GameMap map;

    /**
     * Initialise un nouvel état de jeu avec une carte donnée.
     *
     * @param map la carte du jeu
     */
    public GameState(GameMap map) {
        this.map = map;
        this.players = new ArrayList<>();
        this.bombs = new ArrayList<>();
        this.explosions = new ArrayList<>();
		this.bonuses = new ArrayList<>();
    }

    public static GameState fromJson(String json) {
		return fromJson(Json.parse(json));
    }

	public static GameState fromJson(Json json) {
		if (json == null || !json.isObject()) {
			throw new IllegalArgumentException("GameState JSON must be an object");
		}
		Json mapJson = json.at("map");
		GameMap map;
		if (mapJson.isString()) {
			map = GameMap.fromJson(mapJson.getString());
		} else {
			map = GameMap.fromJson(mapJson.stringify());
		}
		GameState state = new GameState(map);

		Json players = json.value("players", Json.emptyArray());
		if (players.isArray()) {
			for (Json p : players.asArray()) {
				state.addPlayer(Player.fromJson(p));
			}
		}
		Json bombs = json.value("bombs", Json.emptyArray());
		if (bombs.isArray()) {
			for (Json b : bombs.asArray()) {
				state.addBomb(Bomb.fromJson(b));
			}
		}
		Json explosions = json.value("explosions", Json.emptyArray());
		if (explosions.isArray()) {
			for (Json e : explosions.asArray()) {
				state.addExplosion(Explosion.fromJson(e));
			}
		}
		return state;
	}

    /**
     * Ajoute un joueur à la partie.
     *
     * @param player joueur à ajouter
     */
    public void addPlayer(IPlayer player) {
        players.add(player);
    }

    public List<BonusPickup> getBonuses() {
        return bonuses;
    }

    public void addBonus(BonusPickup bonus) {
        if (bonus != null) {
            bonuses.add(bonus);
        }
    }

    public void removeBonus(BonusPickup bonus) {
        bonuses.remove(bonus);
    }

    /**
     * Retourne la liste des joueurs.
     *
     * @return liste des joueurs
     */
    public List<IPlayer> getPlayers() {
        return players;
    }

    /**
     * Retourne un joueur à partir de son identifiant.
     *
     * @param id identifiant du joueur
     * @return joueur correspondant ou null
     */
    public IPlayer getPlayerById(String id) {
        return players.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Remplace un joueur (utile pour appliquer des décorateurs/bonus).
     */
    public void replacePlayer(String id, IPlayer replacement) {
        if (id == null || id.isBlank() || replacement == null) {
            return;
        }
        for (int i = 0; i < players.size(); i++) {
            IPlayer p = players.get(i);
            if (p != null && id.equals(p.getId())) {
                players.set(i, replacement);
                return;
            }
        }
    }

    /**
     * Retourne la liste des joueurs encore en vie.
     *
     * @return joueurs vivants
     */
    public List<IPlayer> getAlivePlayers() {
        return players.stream().filter(IPlayer::isAlive).toList();
    }

    /**
     * Ajoute une bombe.
     *
     * @param bomb bombe à ajouter
     */
    public void addBomb(Bomb bomb) {
        bombs.add(bomb);
    }

    /**
     * Supprime une bombe.
     *
     * @param bomb bombe à retirer
     */
    public void removeBomb(Bomb bomb) {
        bombs.remove(bomb);
    }

    /**
     * Retourne la liste des bombes.
     *
     * @return bombes actives
     */
    public List<Bomb> getBombs() {
        return bombs;
    }

    /**
     * Ajoute une explosion.
     *
     * @param explosion explosion à ajouter
     */
    public void addExplosion(Explosion explosion) {
        explosions.add(explosion);
    }

    /**
     * Supprime une explosion.
     *
     * @param explosion explosion à retirer
     */
    public void removeExplosion(Explosion explosion) {
        explosions.remove(explosion);
    }

    /**
     * Retourne les explosions actives.
     *
     * @return liste des explosions
     */
    public List<Explosion> getExplosions() {
        return explosions;
    }

    /**
     * Retourne la carte du jeu.
     *
     * @return carte
     */
    public GameMap getMap() {
        return map;
    }

    /**
     * Indique si la partie est terminée. (0 ou 1 joueur vivant)
     *
     * @return true si la partie est finie
     */
    public boolean isGameOver() {
        return getAlivePlayers().size() <= 1;
    }

    /**
     * Retourne le joueur gagnant s'il existe.
     *
     * @return gagnant ou null
     */
    public IPlayer getWinner() {
        return getAlivePlayers().stream().findFirst().orElse(null);
    }

    /**
     * Convertit l'état complet du jeu en JSON.
     */
    public Json toJson() {
        List<Json> playerValues = new ArrayList<>();
        for (IPlayer p : players) {
            playerValues.add(p.toJson());
        }
        List<Json> bombValues = new ArrayList<>();
        for (Bomb b : bombs) {
            bombValues.add(b.toJson());
        }
        List<Json> explosionValues = new ArrayList<>();
        for (Explosion e : explosions) {
            explosionValues.add(e.toJson());
        }
        return Json.object(java.util.Map.of(
                "players", Json.array(playerValues),
                "bombs", Json.array(bombValues),
                "explosions", Json.array(explosionValues),
                "map", map == null ? Json.nullValue() : map.toJson()
        ));
    }
}
