package fr.iutgon.sae401.serverSide.game.rooms;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;
import fr.iutgon.sae401.common.model.gameplay.GameConfig;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.serverSide.game.GameEngine;
import fr.iutgon.sae401.serverSide.game.SelfRunningGameEngine;
import fr.iutgon.sae401.serverSide.game.ServerGameConfig;
import fr.iutgon.sae401.serverSide.game.engines.BombermanGameEngine;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import fr.iutgon.sae401.serverSide.server.clients.ClientRegistry;
import fr.iutgon.sae401.serverSide.server.clients.SkinRegistry;
import fr.iutgon.sae401.serverSide.server.udp.UdpCapableClientRegistry;
import fr.iutgon.sae401.serverSide.server.rooms.RoomId;
import fr.iutgon.sae401.serverSide.server.rooms.RoomManager;
import fr.iutgon.sae401.serverSide.server.runtime.FixedRateGameLoop;
import fr.iutgon.sae401.serverSide.server.runtime.GameLoop;
import fr.iutgon.sae401.common.protocol.MessageEnvelope;
import fr.iutgon.sae401.common.model.dto.BombDTO;
import fr.iutgon.sae401.common.model.dto.BonusPickupDTO;
import fr.iutgon.sae401.common.model.dto.ExplosionDTO;
import fr.iutgon.sae401.common.model.dto.PlayerDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * @brief Engine that runs one game loop thread per room.
 * <p>
 * Each room owns its own {@link GameEngine} instance and its own single-threaded loop.
 * Inputs/join/leave events are enqueued and executed on the room thread.
 */
public final class RoomPerThreadEngine implements GameEngine, SelfRunningGameEngine {
    private static final List<Integer> MATCH_SKIN_POOL = IntStream.rangeClosed(0, 34)
            .filter(id -> id != 6 && id != 9 && id != 30 && id != 32)
            .boxed()
            .toList();
    
    private final ServerGameConfig config;
    private final ClientRegistry clients;
    private final RoomManager rooms;
    private final List<RoomId> availableRooms;
    private final RoomId defaultRoom;
    private final Function<RoomId, GameEngine> engineFactory;
    private final SkinRegistry skins;
    // Stores gameplay config per room
    private final ConcurrentMap<RoomId, GameConfig> gameplayConfigs = new ConcurrentHashMap<>();
	// Stores map theme per room
	private final ConcurrentMap<RoomId, MapTheme> mapThemes = new ConcurrentHashMap<>();
    // Stores passwords per room (empty string if no password)
    private final ConcurrentMap<RoomId, String> roomPasswords = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<RoomId, RoomInstance> instances = new ConcurrentHashMap<>();
    private volatile boolean running;
    
    private static final Logger LOGGER = Logger.getLogger();

    public RoomPerThreadEngine(
            ServerGameConfig config,
            ClientRegistry clients,
            RoomManager rooms,
            List<RoomId> availableRooms,
            RoomId defaultRoom,
            Function<RoomId, GameEngine> engineFactory,
            SkinRegistry skins
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.clients = Objects.requireNonNull(clients, "clients");
        this.rooms = Objects.requireNonNull(rooms, "rooms");
        this.availableRooms = List.copyOf(Objects.requireNonNull(availableRooms, "availableRooms"));
        this.defaultRoom = Objects.requireNonNull(defaultRoom, "defaultRoom");
        this.engineFactory = Objects.requireNonNull(engineFactory, "engineFactory");
        this.skins = Objects.requireNonNull(skins, "skins");
    }

    /**
     * Set the gameplay config for a room before creation.
     */
    public void setGameplayConfig(RoomId roomId, GameConfig config) {
        gameplayConfigs.put(roomId, config);
    }

    /**
     * Get the gameplay config for a room, or null if not set.
     */
    public GameConfig getGameplayConfig(RoomId roomId) {
        return gameplayConfigs.get(roomId);
    }

    /**
     * Set the map theme for a room before creation.
     */
    public void setMapTheme(RoomId roomId, MapTheme theme) {
        mapThemes.put(roomId, theme == null ? MapTheme.CLASSIC : theme);
    }

    /**
     * Get the map theme for a room, or null if not set.
     */
    public MapTheme getMapTheme(RoomId roomId) {
        return mapThemes.get(roomId);
    }

    /**
     * Set the password for a room.
     */
    public void setRoomPassword(RoomId roomId, String password) {
        roomPasswords.put(roomId, password == null ? "" : password);
    }

    /**
     * Get the password for a room. Returns empty string if no password.
     */
    public String getRoomPassword(RoomId roomId) {
        return roomPasswords.getOrDefault(roomId, "");
    }

    /**
     * Check if a room has a password.
     */
    public boolean hasPassword(RoomId roomId) {
        String pwd = roomPasswords.get(roomId);
        return pwd != null && !pwd.isEmpty();
    }

    public RoomManager rooms() {
        return rooms;
    }

	public RoomId defaultRoom() {
		return defaultRoom;
	}

	public List<RoomId> availableRooms() {
		return availableRooms;
	}

    /**
     * @return true if the given match room exists and its game engine considers the match finished.
     */
    public boolean isMatchFinished(RoomId roomId) {
        if (roomId == null) {
            return false;
        }
        RoomInstance instance = instances.get(roomId);
        if (instance == null) {
            return false;
        }
        if (instance.engine instanceof BombermanGameEngine bomberman) {
            return bomberman.isMatchFinished();
        }
        // Unknown engine type: we cannot reliably compute match status, so do not block restart.
        return true;
    }

    public void joinRoom(RoomId roomId, ClientId clientId) {
        LOGGER.log(new LogMessage("[ROOM] Join request: room=" + roomId + ", client=" + clientId, LogLevel.INFO));
        Objects.requireNonNull(roomId, "roomId");
        Objects.requireNonNull(clientId, "clientId");

        Optional<RoomId> previous = rooms.roomOf(clientId);
        if (previous.isPresent() && previous.get().equals(roomId)) {
            return;
        }

        previous.ifPresent(prev -> {
            RoomInstance prevInstance = instances.get(prev);
            if (prevInstance != null) {
                prevInstance.enqueue(() -> prevInstance.engine.onClientDisconnected(clientId));
            }
        });

		rooms.join(roomId, clientId);
		previous.ifPresent(this::cleanupRoomIfEmpty);
		RoomInstance instance = ensureRoom(roomId);
		instance.enqueue(() -> instance.engine.onClientConnected(clientId));
	}

	public void leaveRoom(ClientId clientId) {
		LOGGER.log(new LogMessage("[ROOM] Client leaves room: " + clientId, LogLevel.INFO));
		Objects.requireNonNull(clientId, "clientId");
		Optional<RoomId> previous = rooms.roomOf(clientId);
		rooms.leave(clientId);
		previous.ifPresent(prev -> {
			RoomInstance instance = instances.get(prev);
			if (instance != null) {
				instance.enqueue(() -> instance.engine.onClientDisconnected(clientId));
			}
		});
		previous.ifPresent(this::cleanupRoomIfEmpty);
	}

	private void cleanupRoomIfEmpty(RoomId roomId) {
		if (roomId == null) {
			return;
		}
		if (roomId.equals(defaultRoom) || availableRooms.contains(roomId)) {
			return;
		}
		if (!rooms.members(roomId).isEmpty()) {
			return;
		}
		RoomInstance instance = instances.remove(roomId);
		if (instance != null) {
			LOGGER.log(new LogMessage("[ROOM] Deleting empty room instance: " + roomId, LogLevel.INFO));
			instance.loop.stop();
		}
		gameplayConfigs.remove(roomId);
        mapThemes.remove(roomId);
	}

    @Override
    public void start() {
        LOGGER.log(new LogMessage("[ROOMS] Starting all rooms (server)", LogLevel.INFO));
        running = true;
        for (RoomId id : availableRooms) {
            ensureRoom(id);
        }
        ensureRoom(defaultRoom);
    }

    @Override
    public void stop() {
        LOGGER.log(new LogMessage("[ROOMS] Stopping all rooms (server)", LogLevel.INFO));
        running = false;
        for (RoomInstance instance : instances.values()) {
            instance.loop.stop();
        }
        instances.clear();
    }

    @Override
    public void onClientConnected(ClientId clientId) {
        joinRoom(defaultRoom, clientId);
    }

    @Override
    public void onClientDisconnected(ClientId clientId) {
        leaveRoom(clientId);
        rooms.onClientDisconnected(clientId);
    }

    @Override
    public void onInput(ClientId clientId, Json payload) {
        RoomId roomId = rooms.roomOf(clientId).orElse(defaultRoom);
        RoomInstance instance = ensureRoom(roomId);
        instance.enqueue(() -> instance.engine.onInput(clientId, payload));
    }

    @Override
    public void tick(double dtSeconds) {
        // no-op: each room has its own loop.
    }

    @Override
    public void netTick() {
        // no-op: each room has its own loop.
    }

    private RoomInstance ensureRoom(RoomId roomId) {
        try {
            return instances.computeIfAbsent(roomId, id -> {
                LOGGER.log(new LogMessage("[ROOM] Creating room: " + id, LogLevel.INFO));
                GameEngine engine = engineFactory.apply(id);
                RoomInstance instance = new RoomInstance(id, config, engine, clients, rooms, skins);
                if (running) {
                    instance.loop.start();
                }
                return instance;
            });
        } catch (Exception e) {
            LOGGER.log(new LogMessage("[ERROR] Failed to create room: " + roomId + " - " + e.getMessage(), LogLevel.ERROR));
            throw e;
        }
    }

    private static final class RoomInstance {
        private final GameEngine engine;
        private final SkinRegistry skins;
        private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
        private final GameLoop loop;
		private long lastSentStateVersion = Long.MIN_VALUE;
        private long seq = 0L;
        private long udpSeq = 0L;
        private final java.util.Set<ClientId> initSent = new java.util.HashSet<>();
		private boolean botsFilledAtMatchStart = false;
        private final java.util.Map<String, PlayerNetState> lastPlayers = new java.util.HashMap<>();
		private final java.util.Map<String, BombNetState> lastBombs = new java.util.HashMap<>();
		private final java.util.Map<String, String> lastBonusesByPos = new java.util.HashMap<>();
        private final java.util.Map<String, Integer> skinByPlayerId = new java.util.HashMap<>();
        private final Random random = new Random();

        private record PlayerNetState(int x, int y, boolean alive, int health, int bombs, float speed, int bombRange, int maxBombs) {
            static PlayerNetState fromJson(fr.iutgon.sae401.common.json.Json p) {
                int x = p.value("x", 0);
                int y = p.value("y", 0);
                boolean alive = p.value("alive", true);
                int health = p.value("health", 0);
                int bombs = p.value("bombs", 0);
                float speed = 1.0f;
                if (p.contains("speed") && p.at("speed").isNumber()) {
                    speed = p.at("speed").getFloat();
                }
                int bombRange = p.value("bombRange", 1);
                int maxBombs = p.value("maxBombs", 1);
                return new PlayerNetState(x, y, alive, health, bombs, speed, bombRange, maxBombs);
            }
        }

        private record BombNetState(int x, int y, int range, int timerMs) {
            static BombNetState fromDto(BombDTO b) {
                return new BombNetState(b.x, b.y, b.range, b.timerMs);
            }
        }

        private RoomInstance(RoomId roomId, ServerGameConfig config, GameEngine engine, ClientRegistry clientRegistry, RoomManager roomManager, SkinRegistry skins) {
            this.engine = Objects.requireNonNull(engine, "engine");
            this.skins = Objects.requireNonNull(skins, "skins");
            Objects.requireNonNull(clientRegistry, "clients");
            Objects.requireNonNull(roomManager, "rooms");
            GameEngine wrapper = new GameEngine() {
                @Override
                public void start() {
                    engine.start();
                }

                @Override
                public void stop() {
                    engine.stop();
                }

                @Override
                public void tick(double dtSeconds) {
                    drainTasks();
                    engine.tick(dtSeconds);
                }

                @Override
                public void netTick() {
                    engine.netTick();
                    broadcastGameStateIfMatch();
                }

                private void broadcastGameStateIfMatch() {
                    if (!roomId.value().startsWith("match-")) {
                        return;
                    }
                    if (!(engine instanceof BombermanGameEngine bomberman)) {
                        return;
                    }
                    java.util.Set<ClientId> members = roomManager.members(roomId);
                    if (members.isEmpty()) {
                        return;
                    }
					// Fill with bots once at match start (do not re-fill after players leave).
					if (!botsFilledAtMatchStart) {
						bomberman.ensureMatchFilledWithBots();
						botsFilledAtMatchStart = true;
					}
					java.util.List<PlayerDTO> runtimePlayers = assignRandomSkinsWithoutDuplicates(bomberman.snapshotPlayersDTO());
                    Json stateWithSkins = withPlayersSkinsInState(bomberman.snapshotJson(), runtimePlayers);
                    // If a client leaves and later rejoins the match room, it must receive init again.
                    initSent.retainAll(members);

                    // Send init (map included) once per member.
                    for (ClientId member : members) {
                        if (initSent.contains(member)) {
                            continue;
                        }
                        Json initPayload = Json.object(java.util.Map.of(
                                "roomId", Json.of(roomId.value()),
                                "seq", Json.of(seq),
                                "state", stateWithSkins
                        ));
                        clientRegistry.send(member, MessageEnvelope.of("game_init", initPayload));
                        initSent.add(member);
                    }

                    if (clientRegistry instanceof UdpCapableClientRegistry udpClients) {
                        java.util.List<Json> playerPayload = runtimePlayers.stream()
                                .filter(java.util.Objects::nonNull)
                                .map(PlayerDTO::toJson)
                                .toList();
                        boolean sentOverUdp = false;
                        for (ClientId member : members) {
                            if (!initSent.contains(member) || !udpClients.hasUdpEndpoint(member)) {
                                continue;
                            }
                            Json udpPayload = Json.object(java.util.Map.of(
                                    "roomId", Json.of(roomId.value()),
                                    "seq", Json.of(udpSeq + 1L),
                                    "players", Json.array(playerPayload)
                            ));
                            sentOverUdp |= udpClients.sendUdp(member, MessageEnvelope.of("game_positions", udpPayload));
                        }
                        if (sentOverUdp) {
                            udpSeq++;
                        }
                    }

                    long v = bomberman.stateVersion();
                    if (v == lastSentStateVersion) {
                        return;
                    }
                    lastSentStateVersion = v;
                    seq++;

					Json tilesUpsert = Json.array(bomberman.consumeTileDeltasJson());
					java.util.List<BombDTO> runtimeBombs = bomberman.snapshotBombsDTO();
					java.util.List<ExplosionDTO> runtimeExplosions = bomberman.snapshotExplosionsDTO();
					java.util.List<BonusPickupDTO> runtimeBonuses = bomberman.snapshotBonusesDTO();
                    java.util.Map<String, PlayerNetState> currentPlayers = new java.util.HashMap<>();
                    java.util.List<Json> upserts = new java.util.ArrayList<>();
                    java.util.List<Json> removals = new java.util.ArrayList<>();
                    for (PlayerDTO dto : runtimePlayers) {
						if (dto == null || dto.id == null || dto.id.isBlank()) {
							continue;
						}
						Json p = dto.toJson();
						PlayerNetState s = PlayerNetState.fromJson(p);
						currentPlayers.put(dto.id, s);
						PlayerNetState prev = lastPlayers.get(dto.id);
						if (prev == null || !prev.equals(s)) {
							upserts.add(p);
						}
					}
                    for (String prevId : lastPlayers.keySet()) {
                        if (!currentPlayers.containsKey(prevId)) {
                            removals.add(Json.of(prevId));
                        }
                    }
                    lastPlayers.clear();
                    lastPlayers.putAll(currentPlayers);

                    // Bomb deltas (id -> state)
                    java.util.Map<String, BombNetState> currentBombs = new java.util.HashMap<>();
                    java.util.List<Json> bombsUpsert = new java.util.ArrayList<>();
                    java.util.List<Json> bombsRemove = new java.util.ArrayList<>();
                    for (BombDTO dto : runtimeBombs) {
                        if (dto == null || dto.id == null || dto.id.isBlank()) {
                            continue;
                        }
                        Json b = dto.toJson();
                        BombNetState st = BombNetState.fromDto(dto);
                        currentBombs.put(dto.id, st);
                        BombNetState prev = lastBombs.get(dto.id);
                        if (prev == null || !prev.equals(st)) {
                            bombsUpsert.add(b);
                        }
                    }
                    for (String prevId : lastBombs.keySet()) {
                        if (!currentBombs.containsKey(prevId)) {
                            bombsRemove.add(Json.of(prevId));
                        }
                    }
                    lastBombs.clear();
                    lastBombs.putAll(currentBombs);

                    // Bonus deltas (posKey=x:y -> type)
                    java.util.Map<String, String> currentBonuses = new java.util.HashMap<>();
                    java.util.List<Json> bonusesUpsert = new java.util.ArrayList<>();
                    java.util.List<Json> bonusesRemove = new java.util.ArrayList<>();
                    for (BonusPickupDTO dto : runtimeBonuses) {
                        if (dto == null) {
                            continue;
                        }
                        Json b = dto.toJson();
                        int x = dto.x;
                        int y = dto.y;
                        String type = dto.type == null ? "" : dto.type.name();
                        if (type.isBlank()) {
                            continue;
                        }
                        String key = x + ":" + y;
                        currentBonuses.put(key, type);
                        String prevType = lastBonusesByPos.get(key);
                        if (prevType == null || !prevType.equals(type)) {
                            bonusesUpsert.add(b);
                        }
                    }
                    for (String prevKey : lastBonusesByPos.keySet()) {
                        if (!currentBonuses.containsKey(prevKey)) {
                            bonusesRemove.add(Json.of(prevKey));
                        }
                    }
                    lastBonusesByPos.clear();
                    lastBonusesByPos.putAll(currentBonuses);

                    Json playersUpsertJson = Json.array(upserts);
                    Json playersRemoveJson = Json.array(removals);
                    Json delta = Json.object(java.util.Map.of(
                            "seq", Json.of(seq),
                            "playersUpsert", playersUpsertJson,
                            "playersRemove", playersRemoveJson,
                            "tilesUpsert", tilesUpsert,
                            "bombsUpsert", Json.array(bombsUpsert),
                            "bombsRemove", Json.array(bombsRemove),
                            "explosions", Json.array(runtimeExplosions.stream().map(ExplosionDTO::toJson).toList()),
                            "bonusesUpsert", Json.array(bonusesUpsert),
                            "bonusesRemove", Json.array(bonusesRemove)
                    ));
                    for (ClientId member : members) {
                        // Skip delta for members that did not get init yet.
                        if (!initSent.contains(member)) {
                            continue;
                        }
                        Json memberDelta = delta;
                        if (clientRegistry instanceof UdpCapableClientRegistry udpClients && udpClients.hasUdpEndpoint(member)) {
                            memberDelta = Json.object(java.util.Map.of(
                                    "seq", Json.of(seq),
                                    "playersUpsert", Json.emptyArray(),
                                    "playersRemove", playersRemoveJson,
                                    "tilesUpsert", tilesUpsert,
                                    "bombsUpsert", Json.array(bombsUpsert),
                                    "bombsRemove", Json.array(bombsRemove),
                                    "explosions", Json.array(runtimeExplosions.stream().map(ExplosionDTO::toJson).toList()),
                                    "bonusesUpsert", Json.array(bonusesUpsert),
                                    "bonusesRemove", Json.array(bonusesRemove)
                            ));
                        }
                        Json payload = Json.object(java.util.Map.of(
                                "roomId", Json.of(roomId.value()),
                                "delta", memberDelta
                        ));
                        clientRegistry.send(member, MessageEnvelope.of("game_delta", payload));
                    }
                }

                private void drainTasks() {
                    Runnable r;
                    while ((r = tasks.poll()) != null) {
                        r.run();
                    }
                }
            };
            this.loop = new FixedRateGameLoop(config, wrapper, "room-" + roomId.value() + "-loop");
        }

        private void enqueue(Runnable task) {
            tasks.add(Objects.requireNonNull(task, "task"));
        }

        private List<PlayerDTO> assignRandomSkinsWithoutDuplicates(List<PlayerDTO> runtimePlayers) {
            if (runtimePlayers == null || runtimePlayers.isEmpty()) {
                skinByPlayerId.clear();
                return runtimePlayers == null ? List.of() : runtimePlayers;
            }

            Set<String> currentPlayerIds = runtimePlayers.stream()
                    .filter(Objects::nonNull)
                    .map(dto -> dto.id)
                    .filter(Objects::nonNull)
                    .filter(id -> !id.isBlank())
                    .collect(java.util.stream.Collectors.toSet());
            skinByPlayerId.keySet().retainAll(currentPlayerIds);

            Set<Integer> usedSkins = new HashSet<>(skinByPlayerId.values());
            for (PlayerDTO dto : runtimePlayers) {
                if (dto == null || dto.id == null || dto.id.isBlank()) {
                    continue;
                }
                Integer assigned = skinByPlayerId.get(dto.id);
                if (assigned == null) {
                    // Try to use the skin selected in lobby first
                    int lobbySkin = skins.getSkin(new ClientId(dto.id));
                    if (lobbySkin >= 0 && !usedSkins.contains(lobbySkin)) {
                        assigned = lobbySkin;
                    } else {
                        assigned = pickRandomUnusedSkin(usedSkins);
                    }
                    skinByPlayerId.put(dto.id, assigned);
                    usedSkins.add(assigned);
                }
                dto.skinId = assigned;
            }
            return runtimePlayers;
        }

        private int pickRandomUnusedSkin(Set<Integer> usedSkins) {
            List<Integer> available = new ArrayList<>();
            for (Integer skinId : MATCH_SKIN_POOL) {
                if (!usedSkins.contains(skinId)) {
                    available.add(skinId);
                }
            }
            if (available.isEmpty()) {
                return MATCH_SKIN_POOL.get(random.nextInt(MATCH_SKIN_POOL.size()));
            }
            return available.get(random.nextInt(available.size()));
        }

        private Json withPlayersSkinsInState(Json state, List<PlayerDTO> playersWithSkins) {
            if (state == null || !state.isObject()) {
                return state;
            }
            java.util.Map<String, Json> out = new LinkedHashMap<>(state.asObject());
            List<Json> playersJson = playersWithSkins == null
                    ? List.of()
                    : playersWithSkins.stream()
                    .filter(Objects::nonNull)
                    .map(PlayerDTO::toJson)
                    .toList();
            out.put("players", Json.array(playersJson));
            return Json.object(out);
        }
    }
}
