package net.minecraft.server.jsonrpc.internalapi;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class MinecraftServerSettingsServiceImpl implements MinecraftServerSettingsService {
    private final DedicatedServer server;
    private final JsonRpcLogger jsonrpcLogger;

    public MinecraftServerSettingsServiceImpl(DedicatedServer server, JsonRpcLogger jsonrpcLogger) {
        this.server = server;
        this.jsonrpcLogger = jsonrpcLogger;
    }

    @Override
    public boolean isAutoSave() {
        return this.server.isAutoSave();
    }

    @Override
    public boolean setAutoSave(boolean autoSave, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update autosave from {} to {}", this.isAutoSave(), autoSave);
        this.server.setAutoSave(autoSave);
        return this.isAutoSave();
    }

    @Override
    public Difficulty getDifficulty() {
        return this.server.getWorldData().getDifficulty();
    }

    @Override
    public Difficulty setDifficulty(Difficulty difficulty, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update difficulty from '{}' to '{}'", this.getDifficulty(), difficulty);
        this.server.setDifficulty(difficulty);
        return this.getDifficulty();
    }

    @Override
    public boolean isEnforceWhitelist() {
        return this.server.isEnforceWhitelist();
    }

    @Override
    public boolean setEnforceWhitelist(boolean enforceWhitelist, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update enforce allowlist from {} to {}", this.isEnforceWhitelist(), enforceWhitelist);
        this.server.setEnforceWhitelist(enforceWhitelist);
        this.server.kickUnlistedPlayers();
        return this.isEnforceWhitelist();
    }

    @Override
    public boolean isUsingWhitelist() {
        return this.server.isUsingWhitelist();
    }

    @Override
    public boolean setUsingWhitelist(boolean usingWhitelist, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update using allowlist from {} to {}", this.isUsingWhitelist(), usingWhitelist);
        this.server.setUsingWhitelist(usingWhitelist);
        this.server.kickUnlistedPlayers();
        return this.isUsingWhitelist();
    }

    @Override
    public int getMaxPlayers() {
        return this.server.getMaxPlayers();
    }

    @Override
    public int setMaxPlayers(int maxPlayers, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update max players from {} to {}", this.getMaxPlayers(), maxPlayers);
        this.server.setMaxPlayers(maxPlayers);
        return this.getMaxPlayers();
    }

    @Override
    public int getPauseWhenEmptySeconds() {
        return this.server.pauseWhenEmptySeconds();
    }

    @Override
    public int setPauseWhenEmptySeconds(int pauseWhenEmptySeconds, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update pause when empty from {} seconds to {} seconds", this.getPauseWhenEmptySeconds(), pauseWhenEmptySeconds);
        this.server.setPauseWhenEmptySeconds(pauseWhenEmptySeconds);
        return this.getPauseWhenEmptySeconds();
    }

    @Override
    public int getPlayerIdleTimeout() {
        return this.server.playerIdleTimeout();
    }

    @Override
    public int setPlayerIdleTimeout(int playerIdleTimeout, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update player idle timeout from {} minutes to {} minutes", this.getPlayerIdleTimeout(), playerIdleTimeout);
        this.server.setPlayerIdleTimeout(playerIdleTimeout);
        return this.getPlayerIdleTimeout();
    }

    @Override
    public boolean allowFlight() {
        return this.server.allowFlight();
    }

    @Override
    public boolean setAllowFlight(boolean allowFlight, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update allow flight from {} to {}", this.allowFlight(), allowFlight);
        this.server.setAllowFlight(allowFlight);
        return this.allowFlight();
    }

    @Override
    public int getSpawnProtectionRadius() {
        return this.server.spawnProtectionRadius();
    }

    @Override
    public int setSpawnProtectionRadius(int spawnProtectionRadius, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update spawn protection radius from {} to {}", this.getSpawnProtectionRadius(), spawnProtectionRadius);
        this.server.setSpawnProtectionRadius(spawnProtectionRadius);
        return this.getSpawnProtectionRadius();
    }

    @Override
    public String getMotd() {
        return this.server.getMotd();
    }

    @Override
    public String setMotd(String motd, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update MOTD from '{}' to '{}'", this.getMotd(), motd);
        this.server.setMotd(motd);
        return this.getMotd();
    }

    @Override
    public boolean forceGameMode() {
        return this.server.forceGameMode();
    }

    @Override
    public boolean setForceGameMode(boolean forceGamemode, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update force game mode from {} to {}", this.forceGameMode(), forceGamemode);
        this.server.setForceGameMode(forceGamemode);
        return this.forceGameMode();
    }

    @Override
    public GameType getGameMode() {
        return this.server.gameMode();
    }

    @Override
    public GameType setGameMode(GameType gameMode, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update game mode from '{}' to '{}'", this.getGameMode(), gameMode);
        this.server.setGameMode(gameMode);
        return this.getGameMode();
    }

    @Override
    public int getViewDistance() {
        return this.server.viewDistance();
    }

    @Override
    public int setViewDistance(int viewDistance, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update view distance from {} to {}", this.getViewDistance(), viewDistance);
        this.server.setViewDistance(viewDistance);
        return this.getViewDistance();
    }

    @Override
    public int getSimulationDistance() {
        return this.server.simulationDistance();
    }

    @Override
    public int setSimulationDistance(int simulationDistance, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update simulation distance from {} to {}", this.getSimulationDistance(), simulationDistance);
        this.server.setSimulationDistance(simulationDistance);
        return this.getSimulationDistance();
    }

    @Override
    public boolean acceptsTransfers() {
        return this.server.acceptsTransfers();
    }

    @Override
    public boolean setAcceptsTransfers(boolean acceptsTransfers, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update accepts transfers from {} to {}", this.acceptsTransfers(), acceptsTransfers);
        this.server.setAcceptsTransfers(acceptsTransfers);
        return this.acceptsTransfers();
    }

    @Override
    public int getStatusHeartbeatInterval() {
        return this.server.statusHeartbeatInterval();
    }

    @Override
    public int setStatusHeartbeatInterval(int statusHeartbeatInterval, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update status heartbeat interval from {} to {}", this.getStatusHeartbeatInterval(), statusHeartbeatInterval);
        this.server.setStatusHeartbeatInterval(statusHeartbeatInterval);
        return this.getStatusHeartbeatInterval();
    }

    @Override
    public int getOperatorUserPermissionLevel() {
        return this.server.operatorUserPermissionLevel();
    }

    @Override
    public int setOperatorUserPermissionLevel(int operatorUserPermissionLevel, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update operator user permission level from {} to {}", this.getOperatorUserPermissionLevel(), operatorUserPermissionLevel);
        this.server.setOperatorUserPermissionLevel(operatorUserPermissionLevel);
        return this.getOperatorUserPermissionLevel();
    }

    @Override
    public boolean hidesOnlinePlayers() {
        return this.server.hidesOnlinePlayers();
    }

    @Override
    public boolean setHidesOnlinePlayers(boolean hidesOnlinePlayers, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update hides online players from {} to {}", this.hidesOnlinePlayers(), hidesOnlinePlayers);
        this.server.setHidesOnlinePlayers(hidesOnlinePlayers);
        return this.hidesOnlinePlayers();
    }

    @Override
    public boolean repliesToStatus() {
        return this.server.repliesToStatus();
    }

    @Override
    public boolean setRepliesToStatus(boolean repliesToStatus, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update replies to status from {} to {}", this.repliesToStatus(), repliesToStatus);
        this.server.setRepliesToStatus(repliesToStatus);
        return this.repliesToStatus();
    }

    @Override
    public int getEntityBroadcastRangePercentage() {
        return this.server.entityBroadcastRangePercentage();
    }

    @Override
    public int setEntityBroadcastRangePercentage(int entityBroadcastRangePercentage, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Update entity broadcast range percentage from {}% to {}%", this.getEntityBroadcastRangePercentage(), entityBroadcastRangePercentage);
        this.server.setEntityBroadcastRangePercentage(entityBroadcastRangePercentage);
        return this.getEntityBroadcastRangePercentage();
    }
}
