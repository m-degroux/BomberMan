package fr.iutgon.sae401.common.model.dto;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.gameplay.GameState;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * DTO pour les mises à jour en temps réel.
 */
public class GameStateUpdateDTO {

    public List<PlayerDTO> players;
    public List<BombDTO> bombs;
    public List<ExplosionDTO> explosions;
	public List<BonusPickupDTO> bonuses;

        public GameStateUpdateDTO(List<PlayerDTO> players, List<BombDTO> bombs, List<ExplosionDTO> explosions, List<BonusPickupDTO> bonuses) {
        this.players = players;
        this.bombs = bombs;
        this.explosions = explosions;
		this.bonuses = bonuses;
    }

    /**
     * Conversion depuis GameState.
     */
    public static GameStateUpdateDTO from(GameState state) {
        return new GameStateUpdateDTO(
                state.getPlayers().stream().map(PlayerDTO::from).toList(),
                state.getBombs().stream().map(BombDTO::from).toList(),
                state.getExplosions().stream().map(ExplosionDTO::from).toList(),
                state.getBonuses().stream().map(BonusPickupDTO::from).toList()
        );
    }

    /**
     * Reconstruit un GameStateUpdateDTO depuis JSON.
     */
    public static GameStateUpdateDTO fromJson(String json) {
        Json j = Json.parse(json);
        List<PlayerDTO> players = new java.util.ArrayList<>();
        Json playersJson = j.value("players", Json.emptyArray());
        if (playersJson.isArray()) {
            for (Json p : playersJson.asArray()) {
                players.add(PlayerDTO.fromJson(p));
            }
        }

        List<BombDTO> bombs = new java.util.ArrayList<>();
        Json bombsJson = j.value("bombs", Json.emptyArray());
        if (bombsJson.isArray()) {
            for (Json b : bombsJson.asArray()) {
                bombs.add(BombDTO.fromJson(b));
            }
        }

        List<ExplosionDTO> explosions = new java.util.ArrayList<>();
        Json expJson = j.value("explosions", Json.emptyArray());
        if (expJson.isArray()) {
            for (Json e : expJson.asArray()) {
                explosions.add(ExplosionDTO.fromJson(e));
            }
        }

		List<BonusPickupDTO> bonuses = new java.util.ArrayList<>();
		Json bonusesJson = j.value("bonuses", Json.emptyArray());
		if (bonusesJson.isArray()) {
			for (Json b : bonusesJson.asArray()) {
				bonuses.add(BonusPickupDTO.fromJson(b));
			}
		}

        return new GameStateUpdateDTO(players, bombs, explosions, bonuses);
    }

    /**
     * Reconstruit depuis binaire.
     */
    public static GameStateUpdateDTO fromBytes(ByteBuffer buffer) {

        int pSize = buffer.getInt();
        List<PlayerDTO> players = new java.util.ArrayList<>();
        for (int i = 0; i < pSize; i++) {
            players.add(PlayerDTO.fromBytes(buffer));
        }

        int bSize = buffer.getInt();
        List<BombDTO> bombs = new java.util.ArrayList<>();
        for (int i = 0; i < bSize; i++) {
            bombs.add(BombDTO.fromBytes(buffer));
        }

        int eSize = buffer.getInt();
        List<ExplosionDTO> explosions = new java.util.ArrayList<>();
        for (int i = 0; i < eSize; i++) {
            explosions.add(ExplosionDTO.fromBytes(buffer));
        }

		int bonusSize = buffer.getInt();
		List<BonusPickupDTO> bonuses = new java.util.ArrayList<>();
		for (int i = 0; i < bonusSize; i++) {
			bonuses.add(BonusPickupDTO.fromBytes(buffer));
		}

        return new GameStateUpdateDTO(players, bombs, explosions, bonuses);
    }

    public Json toJson() {
        List<Json> p = new java.util.ArrayList<>();
        for (PlayerDTO dto : players) {
            p.add(dto.toJson());
        }
        List<Json> b = new java.util.ArrayList<>();
        for (BombDTO dto : bombs) {
            b.add(dto.toJson());
        }
        List<Json> e = new java.util.ArrayList<>();
        for (ExplosionDTO dto : explosions) {
            e.add(dto.toJson());
        }
        List<Json> bo = new java.util.ArrayList<>();
        if (bonuses != null) {
            for (BonusPickupDTO dto : bonuses) {
                bo.add(dto.toJson());
            }
        }
        return Json.object(java.util.Map.of(
				"players", Json.array(p),
				"bombs", Json.array(b),
                "explosions", Json.array(e),
                "bonuses", Json.array(bo)
        ));
    }

    public byte[] toBytes() {
        int pLen = 0;
        byte[][] pBytes = new byte[players.size()][];
        for (int i = 0; i < players.size(); i++) {
            byte[] pb = players.get(i).toBytes();
            pBytes[i] = pb;
            pLen += pb.length;
        }
        int bLen = 0;
        byte[][] bBytes = new byte[bombs.size()][];
        for (int i = 0; i < bombs.size(); i++) {
            byte[] bb = bombs.get(i).toBytes();
            bBytes[i] = bb;
            bLen += bb.length;
        }
        int eLen = 0;
        byte[][] eBytes = new byte[explosions.size()][];
        for (int i = 0; i < explosions.size(); i++) {
            byte[] eb = explosions.get(i).toBytes();
            eBytes[i] = eb;
            eLen += eb.length;
        }
        List<BonusPickupDTO> safeBonuses = bonuses == null ? java.util.List.of() : bonuses;
        int boLen = 0;
        byte[][] boBytes = new byte[safeBonuses.size()][];
        for (int i = 0; i < safeBonuses.size(); i++) {
            byte[] bob = safeBonuses.get(i).toBytes();
            boBytes[i] = bob;
            boLen += bob.length;
        }

        int totalLen = 4 + pLen + 4 + bLen + 4 + eLen + 4 + boLen;
        ByteBuffer buffer = ByteBuffer.allocate(totalLen);
        buffer.putInt(players.size());
        for (byte[] pb : pBytes) buffer.put(pb);
        buffer.putInt(bombs.size());
        for (byte[] bb : bBytes) buffer.put(bb);
        buffer.putInt(explosions.size());
        for (byte[] eb : eBytes) buffer.put(eb);
        buffer.putInt(safeBonuses.size());
        for (byte[] bob : boBytes) buffer.put(bob);
        return buffer.array();
    }
}
