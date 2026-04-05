package fr.iutgon.sae401.common.model.bonus;

import fr.iutgon.sae401.common.model.entity.IPlayer;

import java.util.ArrayList;
import java.util.List;

public class MaxBombBonus extends Bonus{
	private static final int EXTRA_SLOTS = 1;
	private int extraBombs = EXTRA_SLOTS;
	private final List<Double> extraCooldowns = new ArrayList<>();
	
	public MaxBombBonus(IPlayer player) {
		super(player);
	}

	@Override
	public int getMaxBombs() {
		return super.getMaxBombs() + 1;
	}

	@Override
	public int getCurrentBombs() {
		return super.getCurrentBombs() + extraBombs;
	}

	@Override
	public boolean canPlaceBomb() {
		return getCurrentBombs() > 0;
	}

	@Override
	public void useBomb(double cooldown) {
		if (extraBombs > 0) {
			extraBombs--;
			extraCooldowns.add(cooldown);
			return;
		}
		super.useBomb(cooldown);
	}

	@Override
	public void tickBombCooldowns(double dt) {
		super.tickBombCooldowns(dt);
		for (int i = 0; i < extraCooldowns.size(); i++) {
			double remaining = extraCooldowns.get(i) - dt;
			if (remaining <= 0) {
				extraCooldowns.remove(i);
				i--;
				if (extraBombs < EXTRA_SLOTS) {
					extraBombs++;
				}
			} else {
				extraCooldowns.set(i, remaining);
			}
		}
	}
}
