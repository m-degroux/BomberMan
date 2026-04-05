package fr.iutgon.sae401.common.model.bonus;

import fr.iutgon.sae401.common.model.entity.IPlayer;

public class SpeedBonus extends Bonus {
	
	public SpeedBonus(IPlayer player) {
		super(player);
	}

	@Override
	public float getSpeed() {
		return super.getSpeed() * 1.25f;
	}
}
