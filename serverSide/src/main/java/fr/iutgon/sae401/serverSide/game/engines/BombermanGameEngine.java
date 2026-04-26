package fr.iutgon.sae401.serverSide.game.engines;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.common.model.entity.Bomb;
import fr.iutgon.sae401.common.model.entity.Explosion;
import fr.iutgon.sae401.common.model.entity.IPlayer;
import fr.iutgon.sae401.common.model.entity.Player;
import fr.iutgon.sae401.common.model.entity.Position;
import fr.iutgon.sae401.common.model.bonus.BonusPickup;
import fr.iutgon.sae401.common.model.gameplay.GameConfig;
import fr.iutgon.sae401.common.model.gameplay.GameState;
import fr.iutgon.sae401.common.model.map.BombermanMapFactory;
import fr.iutgon.sae401.common.model.map.GameMap;
import fr.iutgon.sae401.common.model.map.MapTheme;
import fr.iutgon.sae401.common.model.map.TileType;
import fr.iutgon.sae401.common.model.ia.IBotStrategy;
import fr.iutgon.sae401.common.model.ia.EliteBotStrategy;
import fr.iutgon.sae401.common.model.ia.KamikazeStrategy;
import fr.iutgon.sae401.common.model.ia.RandomBotStrategy;
import fr.iutgon.sae401.common.model.ia.SurvivorStrategy;
import fr.iutgon.sae401.common.model.gameplay.ActionType;
import fr.iutgon.sae401.common.model.gameplay.PlayerAction;
import fr.iutgon.sae401.common.model.dto.BombDTO;
import fr.iutgon.sae401.common.model.dto.BonusPickupDTO;
import fr.iutgon.sae401.common.model.dto.ExplosionDTO;
import fr.iutgon.sae401.common.model.dto.PlayerDTO;
import fr.iutgon.sae401.serverSide.game.GameEngine;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;
import fr.iutgon.sae401.serverSide.server.clients.ClientRegistry;
import fr.iutgon.sae401.serverSide.server.clients.SkinRegistry;
import fr.iutgon.sae401.common.logger.LogLevel;
import fr.iutgon.sae401.common.logger.LogMessage;
import fr.iutgon.sae401.common.logger.Logger;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Moteur de jeu Bomberman pour une room match-*
 */
public class BombermanGameEngine implements GameEngine {
    
    private static final int FRAME_MS = 80;
    private static final int EXPLOSION_ARM_FRAMES = 6;
    private static final Logger LOGGER = Logger.getLogger();

    private final GameState state;
    private final GameConfig config;
	private final BombermanMapFactory mapFactory;
	private long stateVersion = 0;
	private final Map<Explosion, Set<String>> damagedByExplosion = new IdentityHashMap<>();
    private final Random random = new Random();
    private static final double BONUS_DROP_CHANCE = 0.25;
    private static final int MATCH_MAX_PLAYERS = 4;
    private static final double BOT_DECISION_PERIOD_SECONDS = 0.20;
    private final java.util.Set<String> botIds = new java.util.HashSet<>();
    private final java.util.Map<String, IBotStrategy> botStrategies = new java.util.HashMap<>();
    private double botDecisionAccSeconds = 0.0;
	private final java.util.LinkedHashMap<String, TileType> tileDeltas = new java.util.LinkedHashMap<>();
	private boolean matchFinished = false;
    private int nextBotStrategyIndex = 0;
	private final SkinRegistry skinRegistry;

    public BombermanGameEngine(GameConfig config, ClientRegistry clients) {
        this(config, clients, MapTheme.CLASSIC, null);
    }

    public BombermanGameEngine(GameConfig config, ClientRegistry clients, MapTheme theme) {
        this(config, clients, theme, null);
    }

    public BombermanGameEngine(GameConfig config, ClientRegistry clients, MapTheme theme, SkinRegistry skinRegistry) {
        this.config = config;
        this.mapFactory = new BombermanMapFactory(config.getDestructibleDensity());
        MapTheme safeTheme = theme == null ? MapTheme.CLASSIC : theme;
        GameMap map = mapFactory.create(config.getWidth(), config.getHeight(), safeTheme);
        this.state = new GameState(map);
        this.skinRegistry = skinRegistry;
    }

    /**
     * Ensures the match contains at least {@code desiredTotalPlayers} players by adding server-side bots.
     */
    public void ensureBots(int desiredTotalPlayers) {
		// Don't spawn bots if there are no real players in the match.
		if (countHumanPlayers() == 0) {
			return;
		}
        int desired = Math.max(0, desiredTotalPlayers);
        while (state.getPlayers().size() < desired) {
            String botId = "bot-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            // Avoid collisions (extremely unlikely, but keep it safe).
            if (state.getPlayerById(botId) != null) {
                continue;
            }
            botIds.add(botId);
            botStrategies.put(botId, createBotStrategy());
            onClientConnected(new ClientId(botId));
        }
    }

    public void ensureMatchFilledWithBots() {
        ensureBots(MATCH_MAX_PLAYERS);
    }

    public boolean isMatchFinished() {
        return matchFinished;
    }

    public java.util.List<Json> consumeTileDeltasJson() {
        if (tileDeltas.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<Json> out = new java.util.ArrayList<>(tileDeltas.size());
        for (var e : tileDeltas.entrySet()) {
            String[] parts = e.getKey().split(":", 2);
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            out.add(Json.object(java.util.Map.of(
                    "x", Json.of(x),
                    "y", Json.of(y),
                    "type", Json.of(e.getValue().name())
            )));
        }
        tileDeltas.clear();
        return out;
    }

    private boolean isBotId(String id) {
        if (id == null) {
            return false;
        }
        return botIds.contains(id) || id.startsWith("bot-");
    }

    private int countHumanPlayers() {
        int count = 0;
        for (IPlayer p : state.getPlayers()) {
            if (p == null) {
                continue;
            }
            if (!isBotId(p.getId())) {
                count++;
            }
        }
        return count;
    }

    private void removeOneBotToMakeRoom() {
        String candidate = null;
        for (String botId : botIds) {
            IPlayer p = state.getPlayerById(botId);
            if (p == null) {
                candidate = botId;
                break;
            }
            if (!p.isAlive()) {
                candidate = botId;
                break;
            }
            candidate = botId;
        }
        if (candidate == null) {
            return;
        }
        String id = candidate;
        state.getPlayers().removeIf(p -> p != null && id.equals(p.getId()));
        botIds.remove(id);
        botStrategies.remove(id);
    }

    private void removeAllBots() {
        if (botIds.isEmpty()) {
            return;
        }
        java.util.Set<String> ids = new java.util.HashSet<>(botIds);
        state.getPlayers().removeIf(p -> p != null && ids.contains(p.getId()));
        botIds.clear();
        botStrategies.clear();
        botDecisionAccSeconds = 0.0;
    }

    private IBotStrategy createBotStrategy() {
        int index = Math.floorMod(nextBotStrategyIndex++, 4);
        return switch (index) {
            case 0 -> new RandomBotStrategy();
            case 1 -> new KamikazeStrategy();
            case 2 -> new SurvivorStrategy();
            default -> new EliteBotStrategy();
        };
    }

    public long stateVersion() {
        return stateVersion;
    }

    private void bumpVersion() {
        stateVersion++;
    }

    @Override
    public void onClientConnected(ClientId clientId) {
        String id = clientId.value();
        if (state.getPlayerById(id) != null) {
            return;
        }
        boolean isBot = isBotId(id);
        // Enforce match max players. If a real player joins a room already filled with bots, kick bots to make room.
        if (!isBot) {
            while (state.getPlayers().size() >= MATCH_MAX_PLAYERS && !botIds.isEmpty()) {
                removeOneBotToMakeRoom();
            }
            if (state.getPlayers().size() >= MATCH_MAX_PLAYERS) {
                return;
            }
        }
        Position spawn = pickSpawnAvoidingPlayers(config.getWidth(), config.getHeight());
        int skinId = skinRegistry != null ? skinRegistry.getSkin(clientId) : -1;
        Player base = new Player(id, spawn, config.getInitialHealth(), config.getMaxBombs(), skinId);
        base.setBombRange(config.getBombRange());
        state.addPlayer(base);
		bumpVersion();
        matchFinished = false;
    }

    @Override
    public void onClientDisconnected(ClientId clientId) {
        String id = clientId.value();
        boolean removed = state.getPlayers().removeIf(p -> p.getId().equals(id));
        if (removed) {
            bumpVersion();
        }
        if (isBotId(id)) {
            botIds.remove(id);
            botStrategies.remove(id);
        } else {
            // If the last real player leaves, stop the match and cleanup bots.
            if (countHumanPlayers() == 0) {
                removeAllBots();
                matchFinished = true;
                tileDeltas.clear();
            }
        }
    }

    public Json snapshotJson() {
        return snapshotJson(true);
    }

    public List<PlayerDTO> snapshotPlayersDTO() {
        return state.getPlayers().stream().map(PlayerDTO::from).toList();
    }

    public List<BombDTO> snapshotBombsDTO() {
        return state.getBombs().stream().map(BombDTO::from).toList();
    }

    public List<ExplosionDTO> snapshotExplosionsDTO() {
        return state.getExplosions().stream().map(ExplosionDTO::from).toList();
    }

    public List<BonusPickupDTO> snapshotBonusesDTO() {
        return state.getBonuses().stream().map(BonusPickupDTO::from).toList();
    }

    /**
     * Snapshot used for frequent network updates.
     * Same schema as {@link #snapshotJson()} but without the map payload.
     */
    public Json snapshotRuntimeJson() {
        return snapshotJson(false);
    }

    private Json snapshotJson(boolean includeMap) {
        return BombermanEngineStateOps.snapshotJson(state, config, includeMap);
    }

    private Position pickSpawnAvoidingPlayers(int width, int height) {
        return BombermanEngineStateOps.pickSpawnAvoidingPlayers(state, width, height);
    }

    @Override
    public void start() {
    }

    @Override
    public void netTick() {
		if (matchFinished) {
			return;
		}
        // Important: le timer des bombes doit être diffusé régulièrement pour que le client anime sans saccades.
        // On bump la version à la fréquence réseau uniquement si au moins une bombe est active.
        if (!state.getBombs().isEmpty()) {
            bumpVersion();
        }
    }

    @Override
    public void tick(double dt) {
        if (matchFinished) {
            return;
        }
		tickBots(dt);
        boolean changed = false;
                Set<Explosion> explosionsSnapshot = new HashSet<>(state.getExplosions());
		damagedByExplosion.keySet().retainAll(explosionsSnapshot);

        LinkedList<Bomb> bombsToExplode = new LinkedList<>();
        Set<String> scheduledBombIds = new HashSet<>();
        for (Bomb b : new ArrayList<>(state.getBombs())) {
            b.tick(dt);
            if (!b.isExploded()) {
                continue;
            }
            if (scheduledBombIds.add(b.getBombId())) {
                bombsToExplode.add(b);
            }
        }

        while (!bombsToExplode.isEmpty()) {
            Bomb b = bombsToExplode.removeFirst();
            if (!state.getBombs().contains(b)) {
                continue;
            }
            state.removeBomb(b);

			List<Position> destroyed = new ArrayList<>();
			List<Position> positions = computeExplosionTiles(b.getPosition(), b.getRange(), destroyed);
			if (!destroyed.isEmpty()) {
				changed |= spawnBonuses(destroyed);
			}
            state.addExplosion(new Explosion(b.getPosition(), positions, explosionDurationMs(b.getRange())));
            changed = true;

            Set<Position> blastTiles = new HashSet<>(positions);
            for (Bomb other : new ArrayList<>(state.getBombs())) {
                if (other == null || other.getPosition() == null) {
                    continue;
                }
                if (!blastTiles.contains(other.getPosition())) {
                    continue;
                }
                if (scheduledBombIds.add(other.getBombId())) {
                    bombsToExplode.add(other);
                }
            }
        }

        for (Explosion e : new ArrayList<>(state.getExplosions())) {
            e.tick(dt);
            if (e.isFinished()) {
                state.removeExplosion(e);
                changed = true;
            }
        }
        
        for (IPlayer p : state.getPlayers()) {
            p.tickBombCooldowns(dt);
        }
        
        applyExplosionDamageOnce();
        changed |= applyBonusPickups();
		if (changed) {
			bumpVersion();
		}
        boolean finishedNow = computeMatchFinished();
        if (finishedNow != matchFinished) {
            matchFinished = finishedNow;
            bumpVersion();
        }
    }

    private boolean computeMatchFinished() {
        int humans = 0;
        int humansAlive = 0;
        int botsAlive = 0;
        for (IPlayer p : state.getPlayers()) {
            if (p == null) {
                continue;
            }
            boolean bot = isBotId(p.getId());
            if (bot) {
                if (p.isAlive()) {
                    botsAlive++;
                }
                continue;
            }
            humans++;
            if (p.isAlive()) {
                humansAlive++;
            }
        }
        if (humans == 0) {
            return true;
        }
        if (humansAlive == 0) {
            return true;
        }
        // Important: bots do NOT count for end condition.
        return humansAlive == 1 && botsAlive == 0;
    }

    private void tickBots(double dtSeconds) {
        if (matchFinished || botIds.isEmpty()) {
            return;
        }
		if (countHumanPlayers() == 0) {
			return;
		}
        botDecisionAccSeconds += dtSeconds;
        if (botDecisionAccSeconds < BOT_DECISION_PERIOD_SECONDS) {
            return;
        }
        botDecisionAccSeconds = 0.0;

        for (String botId : new java.util.ArrayList<>(botIds)) {
            IPlayer bot = state.getPlayerById(botId);
            if (bot == null) {
                botIds.remove(botId);
                botStrategies.remove(botId);
                continue;
            }
            if (!bot.isAlive()) {
                continue;
            }
            IBotStrategy strategy = botStrategies.getOrDefault(botId, new RandomBotStrategy());
            PlayerAction action;
            try {
                action = strategy.decideNextMove(state, bot);
            } catch (Exception ignored) {
                continue;
            }
            if (action == null || action.getType() == null) {
                continue;
            }
            switch (action.getType()) {
                case PLACE_BOMB -> onInput(new ClientId(botId), Json.object(java.util.Map.of("bomb", Json.of(true))));
                case MOVE -> {
                    var dir = action.getDirection();
                    if (dir == null) {
                        break;
                    }
                    Position pos = bot.getPosition();
                    if (pos == null) {
                        break;
                    }
                    Position next = switch (dir) {
                        case UP -> new Position(pos.getX(), pos.getY() - 1);
                        case DOWN -> new Position(pos.getX(), pos.getY() + 1);
                        case LEFT -> new Position(pos.getX() - 1, pos.getY());
                        case RIGHT -> new Position(pos.getX() + 1, pos.getY());
                    };
                    onInput(new ClientId(botId), Json.object(java.util.Map.of(
                            "x", Json.of(next.getX()),
                            "y", Json.of(next.getY())
                    )));
                }
                case NONE -> {
                }
            }
        }
    }

    private boolean spawnBonuses(List<Position> destroyedDestructibles) {
        return BombermanEngineStateOps.spawnBonuses(state, random, BONUS_DROP_CHANCE, destroyedDestructibles);
    }

    private boolean applyBonusPickups() {
        return BombermanEngineStateOps.applyBonusPickups(state);
    }

    private void applyExplosionDamageOnce() {
        if (BombermanEngineStateOps.applyExplosionDamageOnce(state, damagedByExplosion)) {
            bumpVersion();
        }
    }

    private static int explosionDurationMs(int range) {
        return BombermanEngineStateOps.explosionDurationMs(range, EXPLOSION_ARM_FRAMES, FRAME_MS);
    }

	private List<Position> computeExplosionTiles(Position center, int range, List<Position> destroyedDestructiblesOut) {
        return BombermanEngineStateOps.computeExplosionTiles(state, center, range, destroyedDestructiblesOut, tileDeltas);
    }

    @Override
    public void onInput(ClientId clientId, Json payload) {
        if (payload == null || !payload.isObject()) {
            return;
        }
        IPlayer player = state.getPlayerById(clientId.value());
        if (player == null || !player.isAlive()) {
            return;
        }

        // Bomb input: { bomb: true } (preferred) or legacy { range: int }
        if (payload.value("bomb", false) || payload.contains("range")) {
            if (!player.canPlaceBomb()) {
                return;
            }
            Position at = player.getPosition();
            if (at == null) {
                return;
            }
            if (!state.getMap().isWalkable(at)) {
                return;
            }
            boolean alreadyThere = state.getBombs().stream().anyMatch(b -> at.equals(b.getPosition()));
            if (alreadyThere) {
                return;
            }
            int range = player.getBombRange();
            if (range <= 0) {
                range = config.getBombRange();
            }
            Bomb bomb = Bomb.createUnique(at, clientId.value(), config.getBombTimer(), range);
            state.addBomb(bomb);
            player.useBomb(config.getBombCooldownMs() / 1000.0);
            LOGGER.log(new LogMessage("[GAME] input bomb id=" + bomb.getBombId() + " from=" + clientId.value() + " at (" + at.getX() + "," + at.getY() + ")", LogLevel.INFO));
            bumpVersion();
            return;
        }

        // Player input: { x, y }
        int x = payload.value("x", -1);
        int y = payload.value("y", -1);
        if (x < 0 || y < 0) {
            return;
        }
        Position old = player.getPosition();
        if (old == null) {
            return;
        }
        int dx = Math.abs(x - old.getX());
        int dy = Math.abs(y - old.getY());
        if (dx + dy != 1) {
            return;
        }
        Position next = new Position(x, y);
        if (!state.getMap().isWalkable(next)) {
            return;
        }
        boolean bombThere = state.getBombs().stream().anyMatch(b -> next.equals(b.getPosition()));
        if (bombThere) {
            return;
        }
        player.setPosition(next);
        LOGGER.log(new LogMessage("[GAME] input move from=" + clientId.value() + " -> (" + x + "," + y + ")", LogLevel.INFO));
        bumpVersion();
    }
}
