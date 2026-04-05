package fr.iutgon.sae401.clientSide.utils;

import java.util.List;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class GamepadInputManager {
	public enum Action {
		NONE, MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, DROP_BOMB
	}

	private static final float AXIS_DEADZONE = 0.45f;
	private static final long MOVE_COOLDOWN_NS = 130_000_000L;

	public static final String NO_GAMEPAD = "__NONE__";

	private Controller controller;
	private String selectedControllerName = NO_GAMEPAD;
	private long lastMoveAtNs = 0L;
	private boolean bombPressed = false;

	public void initialize() {
		Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
		if (NO_GAMEPAD.equals(selectedControllerName)) {
			controller = null;
			return;
		}
		if (selectedControllerName != null && !selectedControllerName.isBlank()) {
			for (Controller c : controllers) {
				if ((c.getType() == Controller.Type.GAMEPAD || c.getType() == Controller.Type.STICK)
						&& selectedControllerName.equals(c.getName())) {
					controller = c;
					return;
				}
			}
		}
		for (Controller c : controllers) {
			if (c.getType() == Controller.Type.GAMEPAD || c.getType() == Controller.Type.STICK) {
				controller = c;
				return;
			}
		}
		controller = null;
	}

	public String[] listGamepads() {
		try {
			Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
			return java.util.Arrays.stream(controllers)
					.filter(c -> c.getType() == Controller.Type.GAMEPAD || c.getType() == Controller.Type.STICK)
					.map(Controller::getName).distinct().toArray(String[]::new);
		} catch (UnsatisfiedLinkError e) {
			App.LOGGER.log(new LogMessage("Gamepad non supporté sur cette plateforme", LogLevel.WARNING));
			return null;
		}
	}

	public void setSelectedController(String controllerName) {
		if (controllerName == null || controllerName.isBlank() || NO_GAMEPAD.equals(controllerName)) {
			selectedControllerName = NO_GAMEPAD;
			controller = null;
			return;
		}
		selectedControllerName = controllerName;
		controller = null;
		initialize();
	}

	public String getSelectedControllerName() {
		return selectedControllerName;
	}

	public Action poll(long nowNs) {
		if (controller == null) {
			return Action.NONE;
		}

		if (!controller.poll()) {
			return Action.NONE;
		}

		float x = 0f;
		float y = 0f;
		float pov = Component.POV.OFF;
		boolean currentBombPressed = false;

		for (Component component : controller.getComponents()) {
			Component.Identifier identifier = component.getIdentifier();
			float value = component.getPollData();

			if (identifier == Component.Identifier.Axis.X) {
				x = value;
			} else if (identifier == Component.Identifier.Axis.Y) {
				y = value;
			} else if (identifier == Component.Identifier.Axis.POV) {
				pov = value;
			} else if (identifier instanceof Component.Identifier.Button buttonId) {
				// A / B / X / Y on most controllers
				String name = buttonId.getName();
				if (("0".equals(name) || "1".equals(name) || "2".equals(name) || "3".equals(name)) && value > 0.5f) {
					currentBombPressed = true;
				}
			}
		}

		if (currentBombPressed && !bombPressed) {
			bombPressed = true;
			return Action.DROP_BOMB;
		}
		bombPressed = currentBombPressed;

		if (nowNs - lastMoveAtNs < MOVE_COOLDOWN_NS) {
			return Action.NONE;
		}

		Action movement = movementFromInputs(x, y, pov);
		if (movement != Action.NONE) {
			lastMoveAtNs = nowNs;
		}
		return movement;
	}

	private Action movementFromInputs(float x, float y, float pov) {
		if (pov == Component.POV.UP || pov == Component.POV.UP_LEFT || pov == Component.POV.UP_RIGHT) {
			return Action.MOVE_UP;
		}
		if (pov == Component.POV.DOWN || pov == Component.POV.DOWN_LEFT || pov == Component.POV.DOWN_RIGHT) {
			return Action.MOVE_DOWN;
		}
		if (pov == Component.POV.LEFT) {
			return Action.MOVE_LEFT;
		}
		if (pov == Component.POV.RIGHT) {
			return Action.MOVE_RIGHT;
		}

		if (Math.abs(x) < AXIS_DEADZONE && Math.abs(y) < AXIS_DEADZONE) {
			return Action.NONE;
		}
		if (Math.abs(y) >= Math.abs(x)) {
			return y < -AXIS_DEADZONE ? Action.MOVE_UP : Action.MOVE_DOWN;
		}
		return x < -AXIS_DEADZONE ? Action.MOVE_LEFT : Action.MOVE_RIGHT;
	}
}
