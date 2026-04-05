package fr.iutgon.sae401.common.model.entity;

import fr.iutgon.sae401.common.json.Json;

public interface IPlayer {
	
	String getId();
    
	Position getPosition();
    
    void setPosition(Position position);

	void move(Direction dir);

	void takeDamage();

	void die();

	boolean isAlive();

	boolean canPlaceBomb();

	void useBomb(double cooldown);

	void tickBombCooldowns(double dt);

	void restoreBomb();

	int getHealth();

	int getCurrentBombs();

	String getNickname();

	void setNickname(String nickname);

	Json toJson();

	float getSpeed();

	int getBombRange();

	int getMaxBombs();

}