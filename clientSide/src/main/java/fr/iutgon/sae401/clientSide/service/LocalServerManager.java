package fr.iutgon.sae401.clientSide.service;

import fr.iutgon.sae401.clientSide.App;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;

/**
 * Manages an embedded local server lifecycle for "Partie local".
 *
 * Behavior:
 * - When entering local mode, start an in-process server if nothing is already listening on localhost:port.
 * - When exiting local mode, stop the server only if we started it.
 */
public final class LocalServerManager {
	private static final String LOCAL_HOST = "localhost";
	private static final int CONNECT_TIMEOUT_MS = 250;

	private static volatile boolean localModeEnabled = false;
	private static volatile boolean ownedServer = false;
	private static volatile Object server;

	private LocalServerManager() {
	}

	public static synchronized void enterLocalMode() {
		localModeEnabled = true;
	}

	public static synchronized boolean isLocalModeEnabled() {
		return localModeEnabled;
	}

	public static synchronized void exitLocalMode() {
		if (!localModeEnabled) {
			return;
		}
		localModeEnabled = false;
		LocalReplayContext.clear();
		try {
			App.disconnect();
		} catch (Exception ignored) {
		}
		stopOwnedServer();
	}

	public static synchronized boolean ensureLocalServerRunning(int port) {
		if (!localModeEnabled) {
			throw new IllegalStateException("Local mode is not enabled");
		}

		if (ownedServer && server != null) {
			return true;
		}

		// If a server already exists, use it but do not claim ownership.
		if (canConnect(LOCAL_HOST, port, CONNECT_TIMEOUT_MS)) {
			ownedServer = false;
			server = null;
			return true;
		}

		// Port not responding. Check if it's bindable before starting.
		if (!canBind(port)) {
			App.LOGGER.log(new LogMessage("[LOCAL] Port " + port + " is already in use but no server responded.", LogLevel.WARNING));
			return false;
		}

		try {
			Object created = startEmbeddedServerReflective(port);
			server = created;
			ownedServer = true;
			App.LOGGER.log(new LogMessage("[LOCAL] Embedded server started on port " + port, LogLevel.INFO));
			return true;
		} catch (ClassNotFoundException e) {
			App.LOGGER.log(new LogMessage("[LOCAL] Embedded server not available in this runtime. Launch via launcher module. " + e.getMessage(), LogLevel.WARNING));
			server = null;
			ownedServer = false;
			return false;
		} catch (Exception e) {
			App.LOGGER.log(new LogMessage("[LOCAL] Failed to start embedded server: " + e.getMessage(), LogLevel.ERROR));
			server = null;
			ownedServer = false;
			return false;
		}
	}

	public static boolean waitForLocalServer(int port, Duration timeout) {
		Objects.requireNonNull(timeout, "timeout");
		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			if (canConnect(LOCAL_HOST, port, CONNECT_TIMEOUT_MS)) {
				return true;
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return canConnect(LOCAL_HOST, port, CONNECT_TIMEOUT_MS);
	}

	private static boolean canConnect(String host, int port, int timeoutMs) {
		try (Socket s = new Socket()) {
			s.connect(new InetSocketAddress(host, port), timeoutMs);
			return true;
		} catch (IOException ignored) {
			return false;
		}
	}

	private static boolean canBind(int port) {
		try (ServerSocket ss = new ServerSocket()) {
			ss.setReuseAddress(true);
			ss.bind(new InetSocketAddress("0.0.0.0", port));
			return true;
		} catch (IOException ignored) {
			return false;
		}
	}

	private static synchronized void stopOwnedServer() {
		if (!ownedServer) {
			return;
		}
		Object s = server;
		server = null;
		ownedServer = false;
		if (s == null) {
			return;
		}
		try {
			s.getClass().getMethod("stop").invoke(s);
			App.LOGGER.log(new LogMessage("[LOCAL] Embedded server stopped", LogLevel.INFO));
		} catch (Exception e) {
			App.LOGGER.log(new LogMessage("[LOCAL] Failed to stop embedded server: " + e.getMessage(), LogLevel.ERROR));
		}
	}

	private static Object startEmbeddedServerReflective(int port) throws Exception {
		Class<?> argsLoaderType = Class.forName("fr.iutgon.sae401.serverSide.server.runtime.config.ArgsConfigLoader");
		Object runtimeConfig = argsLoaderType.getMethod("load", String[].class)
				.invoke(null, (Object) new String[]{"--port", Integer.toString(port), "--udpPort", Integer.toString(port)});
		Object serverConfig = runtimeConfig.getClass().getMethod("server").invoke(runtimeConfig);
		Object gameConfig = runtimeConfig.getClass().getMethod("game").invoke(runtimeConfig);

		Class<?> moduleType = Class.forName("fr.iutgon.sae401.serverSide.game.modules.LobbyRoomsModule");
		Object module = moduleType.getConstructor(String.class, String.class).newInstance("embedded-local-server", "0.1");

		Class<?> gameServerType = Class.forName("fr.iutgon.sae401.serverSide.server.runtime.GameServer");
		Method create = null;
		for (Method method : gameServerType.getMethods()) {
			if ("create".equals(method.getName()) && method.getParameterCount() == 3) {
				create = method;
				break;
			}
		}
		if (create == null) {
			throw new IllegalStateException("Missing GameServer.create(...)");
		}

		Object created = create.invoke(null, serverConfig, gameConfig, module);
		gameServerType.getMethod("start").invoke(created);
		return created;
	}
}
