package fr.iutgon.sae401.serverSide.game.modules;

import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;
import fr.iutgon.sae401.serverSide.game.GameEngine;
import fr.iutgon.sae401.serverSide.game.GameModule;
import fr.iutgon.sae401.serverSide.game.SelfRunningGameEngine;
import fr.iutgon.sae401.serverSide.game.ServerGameConfig;
import fr.iutgon.sae401.serverSide.game.engines.BombermanGameEngine;
import fr.iutgon.sae401.serverSide.game.rooms.RoomPerThreadEngine;
import fr.iutgon.sae401.serverSide.server.MessageHandler;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import fr.iutgon.sae401.serverSide.server.clients.ClientRegistry;
import fr.iutgon.sae401.serverSide.server.clients.InMemoryNicknameRegistry;
import fr.iutgon.sae401.serverSide.server.clients.InMemorySkinRegistry;
import fr.iutgon.sae401.serverSide.server.clients.NicknameRegistry;
import fr.iutgon.sae401.serverSide.server.clients.SkinRegistry;
import fr.iutgon.sae401.serverSide.server.handlers.game.InputHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.chat.ChatHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.LobbyServices;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.game.ReadyHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.game.SetNicknameHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.game.SelectSkinHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.game.StartGameHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.game.RestartGameHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.rooms.CreateRoomHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.rooms.JoinRoomHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.rooms.LeaveRoomHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.rooms.ListRoomsHandler;
import fr.iutgon.sae401.serverSide.server.handlers.lobby.state.LobbyStateHandler;
import fr.iutgon.sae401.serverSide.server.handlers.system.EchoHandler;
import fr.iutgon.sae401.serverSide.server.handlers.system.HealthHandler;
import fr.iutgon.sae401.serverSide.server.handlers.system.PingHandler;
import fr.iutgon.sae401.serverSide.server.handlers.system.ServerInfoHandler;
import fr.iutgon.sae401.serverSide.server.rooms.*;
import fr.iutgon.sae401.common.model.map.MapTheme;

import java.util.List;
import java.util.Objects;

/**
 * @brief Module that exposes minimal lobby/rooms handlers on top of a room-per-thread engine.
 */
public final class LobbyRoomsModule implements GameModule {
    private final String serverName;
    private final String protocolVersion;
    private final RoomId lobbyRoom;
    private final List<RoomId> initialRooms;

    // Created during createEngine() and reused by createHandlers().
    private volatile RoomPerThreadEngine roomEngine;
    private volatile ReadyManager ready;
    private volatile NicknameRegistry nicknames;
    private volatile SkinRegistry skins;

    public LobbyRoomsModule(String serverName, String protocolVersion) {
        this(serverName, protocolVersion, new RoomId("lobby"), List.of());
    }

    public LobbyRoomsModule(String serverName, String protocolVersion, RoomId lobbyRoom) {
        this(serverName, protocolVersion, lobbyRoom, List.of());
    }

    public LobbyRoomsModule(String serverName, String protocolVersion, RoomId lobbyRoom, List<RoomId> initialRooms) {
        this.serverName = Objects.requireNonNull(serverName, "serverName");
        this.protocolVersion = Objects.requireNonNull(protocolVersion, "protocolVersion");
        this.lobbyRoom = Objects.requireNonNull(lobbyRoom, "lobbyRoom");
        this.initialRooms = List.copyOf(Objects.requireNonNull(initialRooms, "initialRooms"));
    }

    @Override
    public String name() {
        return "lobby-rooms";
    }

    @Override
    public GameEngine createEngine(ServerGameConfig config, ClientRegistry clients) {
        Logger.getLogger().log(new LogMessage("[SERVER] Starting lobby-rooms server", LogLevel.INFO));
        RoomManager roomManager = new InMemoryRoomManager();
        ReadyManager readyManager = new InMemoryReadyManager();
        NicknameRegistry nicknameRegistry = new InMemoryNicknameRegistry();
        SkinRegistry skinRegistry = new InMemorySkinRegistry();
        RoomPerThreadEngine perRoom = new RoomPerThreadEngine(
                config,
                clients,
                roomManager,
                initialRooms,
                lobbyRoom,
                (roomId) -> {
                    if (roomId.equals(lobbyRoom)) {
                        // empty engine
                        return (GameEngine) dtSeconds -> {
                        };
                    } else {
						var cfg = roomEngine.getGameplayConfig(roomId);
						if (cfg == null) {
							cfg = new fr.iutgon.sae401.common.model.gameplay.GameConfig();
						}
						MapTheme theme = roomEngine.getMapTheme(roomId);
						if (theme == null) {
							theme = MapTheme.CLASSIC;
						}
                        return new BombermanGameEngine(cfg, clients, theme, skinRegistry);
                    }
                },
                skinRegistry
        );

        this.roomEngine = perRoom;
        this.ready = readyManager;
        this.nicknames = nicknameRegistry;
        this.skins = skinRegistry;

		return new LobbyEngine(perRoom, readyManager, clients, nicknameRegistry, skinRegistry);
	}

    @Override
    public Iterable<MessageHandler> createHandlers(GameEngine engine, ClientRegistry clients) {
        RoomPerThreadEngine perRoom = this.roomEngine;
        ReadyManager readyManager = this.ready;
        NicknameRegistry nicknameRegistry = this.nicknames;
        SkinRegistry skinRegistry = this.skins;
        if (perRoom == null || readyManager == null || nicknameRegistry == null || skinRegistry == null) {
            throw new IllegalStateException("createEngine must be called before createHandlers");
        }

        RoomManager roomManager = perRoom.rooms();
		LobbyServices lobby = new LobbyServices(perRoom, readyManager, clients, nicknameRegistry, skinRegistry);
        return List.of(
                // system
                new PingHandler(),
                new EchoHandler(),
                new HealthHandler(),
                new ServerInfoHandler(serverName, protocolVersion),

				// lobby/rooms
				new ListRoomsHandler(perRoom),
				new LobbyStateHandler(perRoom, readyManager, nicknameRegistry, skinRegistry),
                new CreateRoomHandler(lobby),
                new JoinRoomHandler(lobby),
                new LeaveRoomHandler(lobby),
                new ReadyHandler(lobby),
                new SetNicknameHandler(lobby),
                new SelectSkinHandler(lobby),
                new StartGameHandler(lobby),
				new RestartGameHandler(lobby),
				new ChatHandler(roomManager, clients, nicknameRegistry),

                // inputs (forwarded to the engine)
                new InputHandler(engine)
        );
    }

	private static final class LobbyEngine implements GameEngine, SelfRunningGameEngine {
		private final RoomPerThreadEngine delegate;
		private final ReadyManager ready;
		private final NicknameRegistry nicknames;
		private final SkinRegistry skins;
        private final LobbyServices lobby;

        private final Logger logger = Logger.getLogger();

		private LobbyEngine(RoomPerThreadEngine delegate, ReadyManager ready, ClientRegistry clients, NicknameRegistry nicknames, SkinRegistry skins) {
			this.delegate = Objects.requireNonNull(delegate, "delegate");
			this.ready = Objects.requireNonNull(ready, "ready");
			this.nicknames = Objects.requireNonNull(nicknames, "nicknames");
			this.skins = Objects.requireNonNull(skins, "skins");
            this.lobby = new LobbyServices(delegate, ready, Objects.requireNonNull(clients, "clients"), nicknames, skins);
		}

        @Override
        public void start() {
            logger.log(new LogMessage("[SERVER] Server is starting", LogLevel.INFO));
            delegate.start();
        }

        @Override
        public void stop() {
            logger.log(new LogMessage("[SERVER] Server is stopping", LogLevel.INFO));
            delegate.stop();
        }

		@Override
		public void onClientConnected(ClientId clientId) {
			logger.log(new LogMessage("[CONNECTION] Client connected: " + clientId, LogLevel.INFO));
			ready.clear(clientId);
			// Assigner un pseudo par défaut "Joueur X"
			if (nicknames instanceof InMemoryNicknameRegistry) {
				InMemoryNicknameRegistry registry = (InMemoryNicknameRegistry) nicknames;
				int playerNumber = registry.getPlayerCount() + 1;
				nicknames.setNickname(clientId, "Joueur " + playerNumber);
				registry.incrementPlayerCounter();
			} else {
				nicknames.setNickname(clientId, clientId.value());
			}
			delegate.onClientConnected(clientId);
            lobby.broadcastRooms();
		}

		@Override
		public void onClientDisconnected(ClientId clientId) {
			logger.log(new LogMessage("[DISCONNECTION] Client disconnected: " + clientId, LogLevel.INFO));
			try {
				delegate.onClientDisconnected(clientId);
			} finally {
				ready.clear(clientId);
				nicknames.remove(clientId);
				skins.remove(clientId);
			}
            lobby.broadcastRooms();
		}

        @Override
        public void onInput(ClientId clientId, fr.iutgon.sae401.common.json.Json payload) {
            delegate.onInput(clientId, payload);
        }

        @Override
        public void tick(double dtSeconds) {
            delegate.tick(dtSeconds);
        }

        @Override
        public void netTick() {
            delegate.netTick();
        }
    }
}
