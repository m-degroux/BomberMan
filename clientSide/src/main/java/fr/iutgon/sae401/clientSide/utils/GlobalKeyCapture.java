package fr.iutgon.sae401.clientSide.utils;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;
import javafx.scene.input.KeyCode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GlobalKeyCapture implements NativeKeyListener {
	private static GlobalKeyCapture instance;
	private final Map<Integer, Consumer<KeyCode>> keyListeners = new ConcurrentHashMap<>();
	private boolean isRegistered = false;
	

	private GlobalKeyCapture() {
	}

	public static synchronized GlobalKeyCapture getInstance() {
		if (instance == null) {
			instance = new GlobalKeyCapture();
		}
		return instance;
	}

	public void start() {
		if (isRegistered) {
			return;
		}

		try {
			GlobalScreen.registerNativeHook();
			GlobalScreen.addNativeKeyListener(this);
			isRegistered = true;
			App.LOGGER.log(new LogMessage("[GlobalKeyCapture] Capture globale des touches activée", LogLevel.INFO));
		} catch (NativeHookException e) {
			App.LOGGER.log(new LogMessage("[GlobalKeyCapture] Erreur lors de l'activation: " + e.getMessage(),
					LogLevel.ERROR));
		}
	}

	public void stop() {
		if (!isRegistered) {
			return;
		}

		try {
			GlobalScreen.removeNativeKeyListener(this);
			GlobalScreen.unregisterNativeHook();
			isRegistered = false;
			App.LOGGER.log(new LogMessage("[GlobalKeyCapture] Capture globale des touches désactivée", LogLevel.INFO));
		} catch (NativeHookException e) {
			App.LOGGER.log(new LogMessage("[GlobalKeyCapture] Erreur lors de la désactivation: " + e.getMessage(),
					LogLevel.ERROR));
		}
	}

	public void addKeyListener(Consumer<KeyCode> listener) {
		keyListeners.put(listener.hashCode(), listener);
	}

	public void removeKeyListener(Consumer<KeyCode> listener) {
		keyListeners.remove(listener.hashCode());
	}

	@Override
	public void nativeKeyPressed(NativeKeyEvent e) {
		KeyCode keyCode = convertNativeKeyToJavaFX(e.getKeyCode());
		if (keyCode != null) {
			for (Consumer<KeyCode> listener : keyListeners.values()) {
				listener.accept(keyCode);
			}
		}
	}

	@Override
	public void nativeKeyReleased(NativeKeyEvent e) {
		// Non utilisé pour l'instant
	}

	@Override
	public void nativeKeyTyped(NativeKeyEvent e) {
		// Non utilisé pour l'instant
	}

	private KeyCode convertNativeKeyToJavaFX(int nativeKeyCode) {
		return switch (nativeKeyCode) {
		case NativeKeyEvent.VC_ESCAPE -> KeyCode.ESCAPE;
		case NativeKeyEvent.VC_SPACE -> KeyCode.SPACE;
		case NativeKeyEvent.VC_ENTER -> KeyCode.ENTER;

		case NativeKeyEvent.VC_UP -> KeyCode.UP;
		case NativeKeyEvent.VC_DOWN -> KeyCode.DOWN;
		case NativeKeyEvent.VC_LEFT -> KeyCode.LEFT;
		case NativeKeyEvent.VC_RIGHT -> KeyCode.RIGHT;

		case NativeKeyEvent.VC_A -> KeyCode.A;
		case NativeKeyEvent.VC_B -> KeyCode.B;
		case NativeKeyEvent.VC_C -> KeyCode.C;
		case NativeKeyEvent.VC_D -> KeyCode.D;
		case NativeKeyEvent.VC_E -> KeyCode.E;
		case NativeKeyEvent.VC_F -> KeyCode.F;
		case NativeKeyEvent.VC_G -> KeyCode.G;
		case NativeKeyEvent.VC_H -> KeyCode.H;
		case NativeKeyEvent.VC_I -> KeyCode.I;
		case NativeKeyEvent.VC_J -> KeyCode.J;
		case NativeKeyEvent.VC_K -> KeyCode.K;
		case NativeKeyEvent.VC_L -> KeyCode.L;
		case NativeKeyEvent.VC_M -> KeyCode.M;
		case NativeKeyEvent.VC_N -> KeyCode.N;
		case NativeKeyEvent.VC_O -> KeyCode.O;
		case NativeKeyEvent.VC_P -> KeyCode.P;
		case NativeKeyEvent.VC_Q -> KeyCode.Q;
		case NativeKeyEvent.VC_R -> KeyCode.R;
		case NativeKeyEvent.VC_S -> KeyCode.S;
		case NativeKeyEvent.VC_T -> KeyCode.T;
		case NativeKeyEvent.VC_U -> KeyCode.U;
		case NativeKeyEvent.VC_V -> KeyCode.V;
		case NativeKeyEvent.VC_W -> KeyCode.W;
		case NativeKeyEvent.VC_X -> KeyCode.X;
		case NativeKeyEvent.VC_Y -> KeyCode.Y;
		case NativeKeyEvent.VC_Z -> KeyCode.Z;

		case NativeKeyEvent.VC_0 -> KeyCode.DIGIT0;
		case NativeKeyEvent.VC_1 -> KeyCode.DIGIT1;
		case NativeKeyEvent.VC_2 -> KeyCode.DIGIT2;
		case NativeKeyEvent.VC_3 -> KeyCode.DIGIT3;
		case NativeKeyEvent.VC_4 -> KeyCode.DIGIT4;
		case NativeKeyEvent.VC_5 -> KeyCode.DIGIT5;
		case NativeKeyEvent.VC_6 -> KeyCode.DIGIT6;
		case NativeKeyEvent.VC_7 -> KeyCode.DIGIT7;
		case NativeKeyEvent.VC_8 -> KeyCode.DIGIT8;
		case NativeKeyEvent.VC_9 -> KeyCode.DIGIT9;

		case NativeKeyEvent.VC_SHIFT -> KeyCode.SHIFT;
		case NativeKeyEvent.VC_CONTROL -> KeyCode.CONTROL;
		case NativeKeyEvent.VC_ALT -> KeyCode.ALT;
		case NativeKeyEvent.VC_TAB -> KeyCode.TAB;
		case NativeKeyEvent.VC_BACKSPACE -> KeyCode.BACK_SPACE;

		default -> null;
		};
	}
}
