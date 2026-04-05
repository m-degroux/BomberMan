package fr.iutgon.sae401.common.model.entity;

import java.util.ArrayList;
import java.util.List;

import fr.iutgon.sae401.common.json.Json;

public class Player extends Entity implements IPlayer {
	private int health;
	private int maxBombs;
	private int currentBombs;
	private boolean alive;
	private String nickname;
	private float speed;
	private int bombRange;
	private List<Double> bombCooldowns = new ArrayList<>();

	public Player(String id, Position position, int health, int maxBombs) {
		super(id, position);
		this.health = health;
		this.maxBombs = maxBombs;
		this.currentBombs = maxBombs;
		this.alive = true;
		this.nickname = id;
		this.speed = 1f;
		this.bombRange = 1;
	}

	public static IPlayer fromJson(String json) {
		return fromJson(Json.parse(json));
	}

	public static Player fromJson(Json json) {
		if (json == null || !json.isObject()) {
			throw new IllegalArgumentException("Player JSON must be an object");
		}
		String id = json.at("id").getString();
		Position pos = Position.fromJson(json.at("position"));
		int health = json.at("health").getInt();
		boolean alive = json.at("alive").getBoolean();
		int bombs = json.at("bombs").getInt();
		String nickname = json.value("nickname", id);

		Player p = new Player(id, pos, health, bombs);
		p.nickname = nickname;
		if (!alive) {
			p.die();
		}
		return p;
	}

	@Override
	public void move(Direction dir) {
		this.position = this.position.add(dir);
	}

	@Override
	public void takeDamage() {
		if (!alive)
			return;

		health--;
		if (health <= 0) {
			die();
		}
	}

	@Override
	public void die() {
		alive = false;
	}

	@Override
	public boolean isAlive() {
		return alive;
	}

	@Override
	public boolean canPlaceBomb() {
		return currentBombs > 0;
	}

	@Override
	public void useBomb(double cooldown) {
		if (currentBombs > 0) {
			currentBombs--;
			bombCooldowns.add(cooldown);
		}
	}

	@Override
	public void tickBombCooldowns(double dt) {
		for (int i = 0; i < bombCooldowns.size(); i++) {
			double remaining = bombCooldowns.get(i) - dt;
			if (remaining <= 0) {
				restoreBomb();
				bombCooldowns.remove(i);
				i--;
			} else {
				bombCooldowns.set(i, remaining);
			}
		}
	}

	@Override
	public void restoreBomb() {
		if (currentBombs < maxBombs) {
			currentBombs++;
		}
	}

	@Override
	public int getHealth() {
		return health;
	}

	@Override
	public int getCurrentBombs() {
		return currentBombs;
	}

	@Override
	public String getNickname() {
		return nickname;
	}

	@Override
	public void setNickname(String nickname) {
		if (nickname != null && !nickname.isBlank()) {
			this.nickname = nickname;
		}
	}

	@Override
	public Json toJson() {
		return Json.object(java.util.Map.of("id", Json.of(id), "position",
				position == null ? Json.nullValue() : position.toJson(), "health", Json.of(health), "alive",
				Json.of(alive), "bombs", Json.of(currentBombs), "nickname", Json.of(nickname)));
	}

	@Override
	public float getSpeed() {
		return speed;
	}

	@Override
	public int getBombRange() {
		return bombRange;
	}

	public void setBombRange(int bombRange) {
		if (bombRange > 0) {
			this.bombRange = bombRange;
		}
	}

	@Override
	public int getMaxBombs() {
		return maxBombs;
	}
	
	
}