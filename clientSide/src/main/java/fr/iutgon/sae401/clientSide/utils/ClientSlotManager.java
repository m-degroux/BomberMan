package fr.iutgon.sae401.clientSide.utils;

import javafx.scene.input.KeyCode;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;

public class ClientSlotManager {
	private static final String LOCK_DIR = System.getProperty("user.home") + "/.bomberman_instances/";
	private static final int MAX_CLIENTS = 4;
	

	private int mySlot = -1;
	private FileChannel lockChannel;
	private FileLock fileLock;
	private File lockFile;

	public ClientSlotManager() {
		acquireSlot();
	}

	private void acquireSlot() {
		try {
			Files.createDirectories(Paths.get(LOCK_DIR));

			for (int slot = 0; slot < MAX_CLIENTS; slot++) {
				lockFile = new File(LOCK_DIR + "client_slot_" + slot + ".lock");

				try {
					lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
					fileLock = lockChannel.tryLock();

					if (fileLock != null) {
						mySlot = slot;
						App.LOGGER.log(new LogMessage("[ClientSlot] Slot " + slot + " acquis (Client " + (slot + 1) + ")",
								LogLevel.INFO));

						Runtime.getRuntime().addShutdownHook(new Thread(this::releaseSlot));
						return;
					} else {
						lockChannel.close();
					}
				} catch (IOException e) {
					if (lockChannel != null) {
						try {
							lockChannel.close();
						} catch (IOException ignored) {
						}
					}
				}
			}

			App.LOGGER.log(new LogMessage("[ClientSlot] Aucun slot disponible (4 clients déjà lancés)", LogLevel.ERROR));
			mySlot = 0;

		} catch (IOException e) {
			App.LOGGER.log(new LogMessage("[ClientSlot] Erreur lors de l'acquisition du slot: " + e.getMessage(), LogLevel.ERROR));
			mySlot = 0;
		}
	}

	public void releaseSlot() {
		if (fileLock != null) {
			try {
				fileLock.release();
				App.LOGGER.log(new LogMessage("[ClientSlot] Slot " + mySlot + " libéré", LogLevel.INFO));
			} catch (IOException e) {
				App.LOGGER.log(new LogMessage("[ClientSlot] Erreur lors de la libération du verrou: " + e.getMessage(), LogLevel.INFO));
			}
		}

		if (lockChannel != null) {
			try {
				lockChannel.close();
			} catch (IOException e) {
				App.LOGGER.log(new LogMessage("[ClientSlot] Erreur lors de la fermeture du canal: " + e.getMessage(), LogLevel.ERROR));
			}
		}

		if (lockFile != null && lockFile.exists()) {
			lockFile.delete();
		}
	}

	public int getSlot() {
		return mySlot;
	}

	public Map<KeyBindingsManager.Action, KeyCode> getDefaultKeysForSlot() {
		Map<KeyBindingsManager.Action, KeyCode> keys = new HashMap<>();

		switch (mySlot) {
		case 0 -> {
			// Client 1: Flèches + ESPACE
			keys.put(KeyBindingsManager.Action.MOVE_UP, KeyCode.UP);
			keys.put(KeyBindingsManager.Action.MOVE_DOWN, KeyCode.DOWN);
			keys.put(KeyBindingsManager.Action.MOVE_LEFT, KeyCode.LEFT);
			keys.put(KeyBindingsManager.Action.MOVE_RIGHT, KeyCode.RIGHT);
			keys.put(KeyBindingsManager.Action.DROP_BOMB, KeyCode.SPACE);
			App.LOGGER.log(new LogMessage("[ClientSlot] Client 1 - Touches: ↑↓←→ + ESPACE", LogLevel.INFO));
		}
		case 1 -> {
			// Client 2: Z,Q,S,D + SHIFT
			keys.put(KeyBindingsManager.Action.MOVE_UP, KeyCode.Z);
			keys.put(KeyBindingsManager.Action.MOVE_DOWN, KeyCode.S);
			keys.put(KeyBindingsManager.Action.MOVE_LEFT, KeyCode.Q);
			keys.put(KeyBindingsManager.Action.MOVE_RIGHT, KeyCode.D);
			keys.put(KeyBindingsManager.Action.DROP_BOMB, KeyCode.SHIFT);
			App.LOGGER.log(new LogMessage("[ClientSlot] Client 2 - Touches: Z,Q,S,D + SHIFT", LogLevel.INFO));
		}
		case 2 -> {
			// Client 3: I,J,K,L + U
			keys.put(KeyBindingsManager.Action.MOVE_UP, KeyCode.I);
			keys.put(KeyBindingsManager.Action.MOVE_DOWN, KeyCode.K);
			keys.put(KeyBindingsManager.Action.MOVE_LEFT, KeyCode.J);
			keys.put(KeyBindingsManager.Action.MOVE_RIGHT, KeyCode.L);
			keys.put(KeyBindingsManager.Action.DROP_BOMB, KeyCode.U);
			App.LOGGER.log(new LogMessage("[ClientSlot] Client 3 - Touches: I,J,K,L + U", LogLevel.INFO));
		}
		case 3 -> {
			// Client 4: T,F,G,H + B
			keys.put(KeyBindingsManager.Action.MOVE_UP, KeyCode.T);
			keys.put(KeyBindingsManager.Action.MOVE_DOWN, KeyCode.G);
			keys.put(KeyBindingsManager.Action.MOVE_LEFT, KeyCode.F);
			keys.put(KeyBindingsManager.Action.MOVE_RIGHT, KeyCode.H);
			keys.put(KeyBindingsManager.Action.DROP_BOMB, KeyCode.B);
			App.LOGGER.log(new LogMessage("[ClientSlot] Client 4 - Touches: T,F,G,H + B", LogLevel.INFO));
		}
		default -> {
			// Fallback: touches par défaut
			keys.put(KeyBindingsManager.Action.MOVE_UP, KeyCode.UP);
			keys.put(KeyBindingsManager.Action.MOVE_DOWN, KeyCode.DOWN);
			keys.put(KeyBindingsManager.Action.MOVE_LEFT, KeyCode.LEFT);
			keys.put(KeyBindingsManager.Action.MOVE_RIGHT, KeyCode.RIGHT);
			keys.put(KeyBindingsManager.Action.DROP_BOMB, KeyCode.SPACE);
		}
		}

		return keys;
	}

	public String getSlotName() {
		return switch (mySlot) {
		case 0 -> "Client 1 (Flèches)";
		case 1 -> "Client 2 (ZQSD)";
		case 2 -> "Client 3 (IJKL)";
		case 3 -> "Client 4 (TFGH)";
		default -> "Client " + (mySlot + 1);
		};
	}
}
