package net.minecraft.server.jsonrpc.methods;

import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class ServerSettingsService {
    public static boolean autosave(MinecraftApi api) {
        return api.serverSettingsService().isAutoSave();
    }

    public static boolean setAutosave(MinecraftApi api, boolean autosave, ClientInfo clientInfo) {
        return api.serverSettingsService().setAutoSave(autosave, clientInfo);
    }

    public static Difficulty difficulty(MinecraftApi api) {
        return api.serverSettingsService().getDifficulty();
    }

    public static Difficulty setDifficulty(MinecraftApi api, Difficulty difficulty, ClientInfo clientInfo) {
        return api.serverSettingsService().setDifficulty(difficulty, clientInfo);
    }

    public static boolean enforceAllowlist(MinecraftApi api) {
        return api.serverSettingsService().isEnforceWhitelist();
    }

    public static boolean setEnforceAllowlist(MinecraftApi api, boolean enforceAllowlist, ClientInfo clientInfo) {
        return api.serverSettingsService().setEnforceWhitelist(enforceAllowlist, clientInfo);
    }

    public static boolean usingAllowlist(MinecraftApi api) {
        return api.serverSettingsService().isUsingWhitelist();
    }

    public static boolean setUsingAllowlist(MinecraftApi api, boolean usingAllowlist, ClientInfo clientInfo) {
        return api.serverSettingsService().setUsingWhitelist(usingAllowlist, clientInfo);
    }

    public static int maxPlayers(MinecraftApi api) {
        return api.serverSettingsService().getMaxPlayers();
    }

    public static int setMaxPlayers(MinecraftApi api, int maxPlayers, ClientInfo clientInfo) {
        return api.serverSettingsService().setMaxPlayers(maxPlayers, clientInfo);
    }

    public static int pauseWhenEmpty(MinecraftApi api) {
        return api.serverSettingsService().getPauseWhenEmptySeconds();
    }

    public static int setPauseWhenEmpty(MinecraftApi api, int pauseWhenEmptySeconds, ClientInfo clientInfo) {
        return api.serverSettingsService().setPauseWhenEmptySeconds(pauseWhenEmptySeconds, clientInfo);
    }

    public static int playerIdleTimeout(MinecraftApi api) {
        return api.serverSettingsService().getPlayerIdleTimeout();
    }

    public static int setPlayerIdleTimeout(MinecraftApi api, int playerIdleTimeout, ClientInfo clientInfo) {
        return api.serverSettingsService().setPlayerIdleTimeout(playerIdleTimeout, clientInfo);
    }

    public static boolean allowFlight(MinecraftApi api) {
        return api.serverSettingsService().allowFlight();
    }

    public static boolean setAllowFlight(MinecraftApi api, boolean allowFlight, ClientInfo clientInfo) {
        return api.serverSettingsService().setAllowFlight(allowFlight, clientInfo);
    }

    public static int spawnProtection(MinecraftApi api) {
        return api.serverSettingsService().getSpawnProtectionRadius();
    }

    public static int setSpawnProtection(MinecraftApi api, int spawnProtectionRadius, ClientInfo clientInfo) {
        return api.serverSettingsService().setSpawnProtectionRadius(spawnProtectionRadius, clientInfo);
    }

    public static String motd(MinecraftApi api) {
        return api.serverSettingsService().getMotd();
    }

    public static String setMotd(MinecraftApi api, String motd, ClientInfo clientInfo) {
        return api.serverSettingsService().setMotd(motd, clientInfo);
    }

    public static boolean forceGameMode(MinecraftApi api) {
        return api.serverSettingsService().forceGameMode();
    }

    public static boolean setForceGameMode(MinecraftApi api, boolean forceGameMode, ClientInfo clientInfo) {
        return api.serverSettingsService().setForceGameMode(forceGameMode, clientInfo);
    }

    public static GameType gameMode(MinecraftApi api) {
        return api.serverSettingsService().getGameMode();
    }

    public static GameType setGameMode(MinecraftApi api, GameType gameMode, ClientInfo clientInfo) {
        return api.serverSettingsService().setGameMode(gameMode, clientInfo);
    }

    public static int viewDistance(MinecraftApi api) {
        return api.serverSettingsService().getViewDistance();
    }

    public static int setViewDistance(MinecraftApi api, int viewDistance, ClientInfo clientInfo) {
        return api.serverSettingsService().setViewDistance(viewDistance, clientInfo);
    }

    public static int simulationDistance(MinecraftApi api) {
        return api.serverSettingsService().getSimulationDistance();
    }

    public static int setSimulationDistance(MinecraftApi api, int simulationDistance, ClientInfo clientInfo) {
        return api.serverSettingsService().setSimulationDistance(simulationDistance, clientInfo);
    }

    public static boolean acceptTransfers(MinecraftApi api) {
        return api.serverSettingsService().acceptsTransfers();
    }

    public static boolean setAcceptTransfers(MinecraftApi api, boolean acceptsTransfers, ClientInfo clientInfo) {
        return api.serverSettingsService().setAcceptsTransfers(acceptsTransfers, clientInfo);
    }

    public static int statusHeartbeatInterval(MinecraftApi api) {
        return api.serverSettingsService().getStatusHeartbeatInterval();
    }

    public static int setStatusHeartbeatInterval(MinecraftApi api, int statusHeartbeatInterval, ClientInfo clientInfo) {
        return api.serverSettingsService().setStatusHeartbeatInterval(statusHeartbeatInterval, clientInfo);
    }

    public static int operatorUserPermissionLevel(MinecraftApi api) {
        return api.serverSettingsService().getOperatorUserPermissionLevel();
    }

    public static int setOperatorUserPermissionLevel(MinecraftApi api, int operatorUserPermissionLevel, ClientInfo clientInfo) {
        return api.serverSettingsService().setOperatorUserPermissionLevel(operatorUserPermissionLevel, clientInfo);
    }

    public static boolean hidesOnlinePlayers(MinecraftApi api) {
        return api.serverSettingsService().hidesOnlinePlayers();
    }

    public static boolean setHidesOnlinePlayers(MinecraftApi api, boolean hidesOnlinePlayers, ClientInfo clientInfo) {
        return api.serverSettingsService().setHidesOnlinePlayers(hidesOnlinePlayers, clientInfo);
    }

    public static boolean repliesToStatus(MinecraftApi api) {
        return api.serverSettingsService().repliesToStatus();
    }

    public static boolean setRepliesToStatus(MinecraftApi api, boolean repliesToStatus, ClientInfo clientInfo) {
        return api.serverSettingsService().setRepliesToStatus(repliesToStatus, clientInfo);
    }

    public static int entityBroadcastRangePercentage(MinecraftApi api) {
        return api.serverSettingsService().getEntityBroadcastRangePercentage();
    }

    public static int setEntityBroadcastRangePercentage(MinecraftApi api, int entityBroadcastRangePercentage, ClientInfo clientInfo) {
        return api.serverSettingsService().setEntityBroadcastRangePercentage(entityBroadcastRangePercentage, clientInfo);
    }
}
