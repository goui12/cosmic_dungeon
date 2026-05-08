package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;

public class PlayerService {
    private static final Component DEFAULT_KICK_MESSAGE = Component.translatable("multiplayer.disconnect.kicked");

    public static List<PlayerDto> get(MinecraftApi api) {
        return api.playerListService().getPlayers().stream().map(PlayerDto::from).toList();
    }

    public static List<PlayerDto> kick(MinecraftApi api, List<PlayerService.KickDto> kicks, ClientInfo clientInfo) {
        List<PlayerDto> list = new ArrayList<>();

        for (PlayerService.KickDto playerservice$kickdto : kicks) {
            ServerPlayer serverplayer = getServerPlayer(api, playerservice$kickdto.player());
            if (serverplayer != null) {
                api.playerListService().remove(serverplayer, clientInfo);
                serverplayer.connection.disconnect(playerservice$kickdto.message.flatMap(Message::asComponent).orElse(DEFAULT_KICK_MESSAGE));
                list.add(playerservice$kickdto.player());
            }
        }

        return list;
    }

    @Nullable
    private static ServerPlayer getServerPlayer(MinecraftApi api, PlayerDto player) {
        if (player.id().isPresent()) {
            return api.playerListService().getPlayer(player.id().get());
        } else {
            return player.name().isPresent() ? api.playerListService().getPlayerByName(player.name().get()) : null;
        }
    }

    public record KickDto(PlayerDto player, Optional<Message> message) {
        public static final MapCodec<PlayerService.KickDto> CODEC = RecordCodecBuilder.mapCodec(
            p_451663_ -> p_451663_.group(
                    PlayerDto.CODEC.codec().fieldOf("player").forGetter(PlayerService.KickDto::player),
                    Message.CODEC.optionalFieldOf("message").forGetter(PlayerService.KickDto::message)
                )
                .apply(p_451663_, PlayerService.KickDto::new)
        );
    }
}
