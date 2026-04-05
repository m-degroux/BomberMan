package fr.iutgon.sae401.common.model.bonus;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Direction;
import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Position;

public abstract class Bonus implements IPlayer {

	private IPlayer player;
	
	public Bonus(IPlayer player) {
		this.player = player;
	}

	@Override
	public float getSpeed() {
		return player.getSpeed();
	}

	@Override
	public int getMaxBombs() {
		return player.getMaxBombs();
	}

	@Override
	public int getBombRange() {
		return player.getBombRange();
	}

	@Override
	public void move(Direction dir) {
		player.move(dir);
	}

	@Override
	public void takeDamage() {
		player.takeDamage();
	}

	@Override
	public void die() {
		player.die();
	}

	@Override
	public boolean isAlive() {
		return player.isAlive();
	}

	@Override
	public boolean canPlaceBomb() {
		return player.canPlaceBomb();
	}

	@Override
	public void useBomb(double cooldown) {
		player.useBomb(cooldown);
	}

	@Override
	public void tickBombCooldowns(double dt) {
		player.tickBombCooldowns(dt);
	}

	@Override
	public void restoreBomb() {
		player.restoreBomb();
	}

	@Override
	public int getHealth() {
		return player.getHealth();
	}

	@Override
	public int getCurrentBombs() {
		return player.getCurrentBombs();
	}

	@Override
	public String getNickname() {
		return player.getNickname();
	}

	@Override
	public void setNickname(String nickname) {
		player.setNickname(nickname);
	}

	@Override
	public Json toJson() {
		return player.toJson();
	}

	@Override
	public String getId() {
		return player.getId();
	}

	@Override
	public Position getPosition() {
		return player.getPosition();
	}

	@Override
	public void setPosition(Position position) {
		player.setPosition(position);
	}

}
