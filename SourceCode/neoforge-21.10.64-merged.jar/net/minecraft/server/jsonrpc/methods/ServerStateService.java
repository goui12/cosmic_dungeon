package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;

public class ServerStateService {
    public static ServerStateService.ServerState status(MinecraftApi api) {
        return !api.serverStateService().isReady()
            ? ServerStateService.ServerState.NOT_STARTED
            : new ServerStateService.ServerState(true, PlayerService.get(api), ServerStatus.Version.current());
    }

    public static boolean save(MinecraftApi api, boolean flush, ClientInfo clientInfo) {
        return api.serverStateService().saveEverything(true, flush, true, clientInfo);
    }

    public static boolean stop(MinecraftApi api, ClientInfo clientInfo) {
        api.submit(() -> api.serverStateService().halt(false, clientInfo));
        return true;
    }

    public static boolean systemMessage(MinecraftApi api, ServerStateService.SystemMessage message, ClientInfo clientInfo) {
        Component component = message.message().asComponent().orElse(null);
        if (component == null) {
            return false;
        } else {
            if (message.receivingPlayers().isPresent()) {
                if (message.receivingPlayers().get().isEmpty()) {
                    return false;
                }

                for (PlayerDto playerdto : message.receivingPlayers().get()) {
                    ServerPlayer serverplayer;
                    if (playerdto.id().isPresent()) {
                        serverplayer = api.playerListService().getPlayer(playerdto.id().get());
                    } else {
                        if (!playerdto.name().isPresent()) {
                            continue;
                        }

                        serverplayer = api.playerListService().getPlayerByName(playerdto.name().get());
                    }

                    if (serverplayer != null) {
                        serverplayer.sendSystemMessage(component, message.overlay());
                    }
                }
            } else {
                api.serverStateService().broadcastSystemMessage(component, message.overlay(), clientInfo);
            }

            return true;
        }
    }

    public record ServerState(boolean started, List<PlayerDto> players, ServerStatus.Version version) {
        public static final Codec<ServerStateService.ServerState> CODEC = RecordCodecBuilder.create(
            p_443402_ -> p_443402_.group(
                    Codec.BOOL.fieldOf("started").forGetter(ServerStateService.ServerState::started),
                    PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("players", List.of()).forGetter(ServerStateService.ServerState::players),
                    ServerStatus.Version.CODEC.fieldOf("version").forGetter(ServerStateService.ServerState::version)
                )
                .apply(p_443402_, ServerStateService.ServerState::new)
        );
        public static final ServerStateService.ServerState NOT_STARTED = new ServerStateService.ServerState(false, List.of(), ServerStatus.Version.current());
    }

    public record SystemMessage(Message message, boolean overlay, Optional<List<PlayerDto>> receivingPlayers) {
        public static final Codec<ServerStateService.SystemMessage> CODEC = RecordCodecBuilder.create(
            p_442613_ -> p_442613_.group(
                    Message.CODEC.fieldOf("message").forGetter(ServerStateService.SystemMessage::message),
                    Codec.BOOL.fieldOf("overlay").forGetter(ServerStateService.SystemMessage::overlay),
                    PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("receivingPlayers").forGetter(ServerStateService.SystemMessage::receivingPlayers)
                )
                .apply(p_442613_, ServerStateService.SystemMessage::new)
        );
    }
}
