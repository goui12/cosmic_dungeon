package net.minecraft.server.jsonrpc.internalapi;

import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public interface MinecraftServerSettingsService {
    boolean isAutoSave();

    boolean setAutoSave(boolean autoSave, ClientInfo clientInfo);

    Difficulty getDifficulty();

    Difficulty setDifficulty(Difficulty difficulty, ClientInfo clientInfo);

    boolean isEnforceWhitelist();

    boolean setEnforceWhitelist(boolean enforceWhitelist, ClientInfo clientInfo);

    boolean isUsingWhitelist();

    boolean setUsingWhitelist(boolean usingWhitelist, ClientInfo clientInfo);

    int getMaxPlayers();

    int setMaxPlayers(int maxPlayers, ClientInfo clientInfo);

    int getPauseWhenEmptySeconds();

    int setPauseWhenEmptySeconds(int pauseWhenEmptySeconds, ClientInfo clientInfo);

    int getPlayerIdleTimeout();

    int setPlayerIdleTimeout(int playerIdleTimeout, ClientInfo clientInfo);

    boolean allowFlight();

    boolean setAllowFlight(boolean allowFlight, ClientInfo clientInfo);

    int getSpawnProtectionRadius();

    int setSpawnProtectionRadius(int spawnProtectionRadius, ClientInfo clientInfo);

    String getMotd();

    String setMotd(String motd, ClientInfo clientInfo);

    boolean forceGameMode();

    boolean setForceGameMode(boolean forceGamemode, ClientInfo clientInfo);

    GameType getGameMode();

    GameType setGameMode(GameType gameMode, ClientInfo clientInfo);

    int getViewDistance();

    int setViewDistance(int viewDistance, ClientInfo clientInfo);

    int getSimulationDistance();

    int setSimulationDistance(int simulationDistance, ClientInfo clientInfo);

    boolean acceptsTransfers();

    boolean setAcceptsTransfers(boolean acceptsTransfers, ClientInfo clientInfo);

    int getStatusHeartbeatInterval();

    int setStatusHeartbeatInterval(int statusHeartbeatInterval, ClientInfo clientInfo);

    int getOperatorUserPermissionLevel();

    int setOperatorUserPermissionLevel(int operatorUserPermissionLevel, ClientInfo clientInfo);

    boolean hidesOnlinePlayers();

    boolean setHidesOnlinePlayers(boolean hidesOnlinePlayers, ClientInfo clientInfo);

    boolean repliesToStatus();

    boolean setRepliesToStatus(boolean repliesToStatus, ClientInfo clientInfo);

    int getEntityBroadcastRangePercentage();

    int setEntityBroadcastRangePercentage(int entityBroadcastRangePercentage, ClientInfo clientInfo);
}
