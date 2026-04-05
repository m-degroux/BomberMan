package fr.iutgon.sae401.common.model.gameplay;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Représente un salon de jeu (avant la partie).
 */
public class Lobby {

    private final String id;
    private final int maxPlayers;
    private final List<Player> players;
    private final Set<String> readyPlayers;
    private boolean inGame;

    public Lobby(String id, int maxPlayers) {
        this.id = id;
        this.maxPlayers = maxPlayers;
        this.players = new ArrayList<>();
        this.readyPlayers = new HashSet<>();
        this.inGame = false;
    }

    /**
     * Reconstruit un lobby à partir d'un JSON.
     */
    public static Lobby fromJson(String json) {
		return fromJson(Json.parse(json));
    }

	public static Lobby fromJson(Json json) {
		if (json == null || !json.isObject()) {
			throw new IllegalArgumentException("Lobby JSON must be an object");
		}
		String id = json.at("id").getString();
		int maxPlayers = json.at("maxPlayers").getInt();
		boolean inGame = json.value("inGame", false);

		Lobby lobby = new Lobby(id, maxPlayers);
		Json players = json.value("players", Json.emptyArray());
		if (players.isArray()) {
			for (Json p : players.asArray()) {
				lobby.addPlayer(Player.fromJson(p));
			}
		}

		Json readyPlayers = json.value("readyPlayers", Json.emptyArray());
		if (readyPlayers.isArray()) {
			for (Json pid : readyPlayers.asArray()) {
				if (pid != null && pid.isString()) {
					lobby.setReady(pid.getString(), true);
				}
			}
		}

		if (inGame) {
			lobby.startGame();
		}
		return lobby;
	}

    /**
     * Ajoute un joueur au lobby.
     */
    public boolean addPlayer(Player player) {
        if (players.size() >= maxPlayers || inGame)
            return false;
        return players.add(player);
    }

    /**
     * Retire un joueur du lobby.
     */
    public void removePlayer(Player player) {
        players.remove(player);
        readyPlayers.remove(player.getId());
    }

    /**
     * Marque un joueur comme prêt.
     */
    public void setReady(String playerId, boolean ready) {
        if (ready) {
            readyPlayers.add(playerId);
        } else {
            readyPlayers.remove(playerId);
        }
    }

    /**
     * Vérifie si tous les joueurs sont prêts.
     */
    public boolean allReady() {
        return players.size() >= 2 && readyPlayers.size() == players.size();
    }

    public List<Player> getPlayers() {
        return players;
    }

    public String getId() {
        return id;
    }

    public boolean isInGame() {
        return inGame;
    }

    public void startGame() {
        this.inGame = true;
    }

    /**
     * Convertit le lobby en JSON.
     */
    public Json toJson() {
        List<Json> playerValues = new ArrayList<>();
        for (IPlayer p : players) {
            playerValues.add(p.toJson());
        }
        List<Json> readyValues = new ArrayList<>();
        for (String pid : readyPlayers) {
            readyValues.add(Json.of(pid));
        }
        return Json.object(java.util.Map.of(
                "id", Json.of(id),
                "maxPlayers", Json.of(maxPlayers),
                "inGame", Json.of(inGame),
                "players", Json.array(playerValues),
                "readyPlayers", Json.array(readyValues)
        ));
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Set<String> getReadyPlayers() {
        return readyPlayers;
    }
}