package fr.iutgon.sae401.common.model.bonus;

import fr.iutgon.sae401.common.model.entity.IPlayer;

public class BombRangeBonus extends Bonus {

	public BombRangeBonus(IPlayer player) {
		super(player);
	}

	@Override
	public int getBombRange() {
		return super.getBombRange() + 1;
	}
}
