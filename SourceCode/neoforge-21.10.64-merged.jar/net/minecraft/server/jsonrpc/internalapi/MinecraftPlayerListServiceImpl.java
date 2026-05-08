package net.minecraft.server.jsonrpc.internalapi;

import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public class MinecraftPlayerListServiceImpl implements MinecraftPlayerListService {
    private final JsonRpcLogger jsonRpcLogger;
    private final DedicatedServer server;

    public MinecraftPlayerListServiceImpl(DedicatedServer server, JsonRpcLogger jsonrpcLogger) {
        this.jsonRpcLogger = jsonrpcLogger;
        this.server = server;
    }

    @Override
    public List<ServerPlayer> getPlayers() {
        return this.server.getPlayerList().getPlayers();
    }

    @Nullable
    @Override
    public ServerPlayer getPlayer(UUID uuid) {
        return this.server.getPlayerList().getPlayer(uuid);
    }

    @Override
    public Optional<NameAndId> fetchUserByName(String name) {
        return this.server.services().nameToIdCache().get(name);
    }

    @Override
    public Optional<NameAndId> fetchUserById(UUID id) {
        return Optional.ofNullable(this.server.services().sessionService().fetchProfile(id, true)).map(p_449360_ -> new NameAndId(p_449360_.profile()));
    }

    @Override
    public Optional<NameAndId> getCachedUserById(UUID id) {
        return this.server.services().nameToIdCache().get(id);
    }

    @Override
    public Optional<ServerPlayer> getPlayer(Optional<UUID> uuid, Optional<String> playerName) {
        if (uuid.isPresent()) {
            return Optional.ofNullable(this.server.getPlayerList().getPlayer(uuid.get()));
        } else {
            return playerName.isPresent() ? Optional.ofNullable(this.server.getPlayerList().getPlayerByName(playerName.get())) : Optional.empty();
        }
    }

    @Override
    public List<ServerPlayer> getPlayersWithAddress(String address) {
        return this.server.getPlayerList().getPlayersWithAddress(address);
    }

    @Override
    public void remove(ServerPlayer player, ClientInfo clientInfo) {
        this.server.getPlayerList().remove(player);
        this.jsonRpcLogger.log(clientInfo, "Remove player '{}'", player.getPlainTextName());
    }

    @Nullable
    @Override
    public ServerPlayer getPlayerByName(String name) {
        return this.server.getPlayerList().getPlayerByName(name);
    }
}
