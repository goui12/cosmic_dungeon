package net.minecraft.server.jsonrpc.methods;

import com.google.common.net.InetAddresses;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.util.ExtraCodecs;

public class IpBanlistService {
    private static final String BAN_SOURCE = "Management server";

    public static List<IpBanlistService.IpBanDto> get(MinecraftApi api) {
        return api.banListService().getIpBanEntries().stream().map(IpBanlistService.IpBan::from).map(IpBanlistService.IpBanDto::from).toList();
    }

    public static List<IpBanlistService.IpBanDto> add(MinecraftApi api, List<IpBanlistService.IncomingIpBanDto> entries, ClientInfo clientInfo) {
        entries.stream()
            .map(p_443411_ -> banIp(api, p_443411_, clientInfo))
            .flatMap(Collection::stream)
            .forEach(p_442542_ -> p_442542_.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned")));
        return get(api);
    }

    private static List<ServerPlayer> banIp(MinecraftApi api, IpBanlistService.IncomingIpBanDto entry, ClientInfo clientInfo) {
        IpBanlistService.IpBan ipbanlistservice$ipban = entry.toIpBan();
        if (ipbanlistservice$ipban != null) {
            return banIp(api, ipbanlistservice$ipban, clientInfo);
        } else {
            if (entry.player().isPresent()) {
                Optional<ServerPlayer> optional = api.playerListService().getPlayer(entry.player().get().id(), entry.player().get().name());
                if (optional.isPresent()) {
                    return banIp(api, entry.toIpBan(optional.get()), clientInfo);
                }
            }

            return List.of();
        }
    }

    private static List<ServerPlayer> banIp(MinecraftApi api, IpBanlistService.IpBan ipBan, ClientInfo clientInfo) {
        api.banListService().addIpBan(ipBan.toIpBanEntry(), clientInfo);
        return api.playerListService().getPlayersWithAddress(ipBan.ip());
    }

    public static List<IpBanlistService.IpBanDto> clear(MinecraftApi api, ClientInfo clientInfo) {
        api.banListService().clearIpBans(clientInfo);
        return get(api);
    }

    public static List<IpBanlistService.IpBanDto> remove(MinecraftApi api, List<String> ips, ClientInfo clientInfo) {
        ips.forEach(p_448867_ -> api.banListService().removeIpBan(p_448867_, clientInfo));
        return get(api);
    }

    public static List<IpBanlistService.IpBanDto> set(MinecraftApi api, List<IpBanlistService.IpBanDto> entries, ClientInfo clientInfo) {
        Set<IpBanlistService.IpBan> set = entries.stream()
            .filter(p_442743_ -> InetAddresses.isInetAddress(p_442743_.ip()))
            .map(IpBanlistService.IpBanDto::toIpBan)
            .collect(Collectors.toSet());
        Set<IpBanlistService.IpBan> set1 = api.banListService().getIpBanEntries().stream().map(IpBanlistService.IpBan::from).collect(Collectors.toSet());
        set1.stream().filter(p_442573_ -> !set.contains(p_442573_)).forEach(p_448864_ -> api.banListService().removeIpBan(p_448864_.ip(), clientInfo));
        set.stream()
            .filter(p_443035_ -> !set1.contains(p_443035_))
            .forEach(p_448872_ -> api.banListService().addIpBan(p_448872_.toIpBanEntry(), clientInfo));
        set.stream()
            .filter(p_443230_ -> !set1.contains(p_443230_))
            .flatMap(p_448869_ -> api.playerListService().getPlayersWithAddress(p_448869_.ip()).stream())
            .forEach(p_443294_ -> p_443294_.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned")));
        return get(api);
    }

    public record IncomingIpBanDto(Optional<PlayerDto> player, Optional<String> ip, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
        public static final MapCodec<IpBanlistService.IncomingIpBanDto> CODEC = RecordCodecBuilder.mapCodec(
            p_442610_ -> p_442610_.group(
                    PlayerDto.CODEC.codec().optionalFieldOf("player").forGetter(IpBanlistService.IncomingIpBanDto::player),
                    Codec.STRING.optionalFieldOf("ip").forGetter(IpBanlistService.IncomingIpBanDto::ip),
                    Codec.STRING.optionalFieldOf("reason").forGetter(IpBanlistService.IncomingIpBanDto::reason),
                    Codec.STRING.optionalFieldOf("source").forGetter(IpBanlistService.IncomingIpBanDto::source),
                    ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(IpBanlistService.IncomingIpBanDto::expires)
                )
                .apply(p_442610_, IpBanlistService.IncomingIpBanDto::new)
        );

        IpBanlistService.IpBan toIpBan(ServerPlayer player) {
            return new IpBanlistService.IpBan(player.getIpAddress(), this.reason().orElse(null), this.source().orElse("Management server"), this.expires());
        }

        @Nullable
        IpBanlistService.IpBan toIpBan() {
            return !this.ip().isEmpty() && InetAddresses.isInetAddress(this.ip().get())
                ? new IpBanlistService.IpBan(this.ip().get(), this.reason().orElse(null), this.source().orElse("Management server"), this.expires())
                : null;
        }
    }

    record IpBan(String ip, @Nullable String reason, String source, Optional<Instant> expires) {
        static IpBanlistService.IpBan from(IpBanListEntry entry) {
            return new IpBanlistService.IpBan(
                Objects.requireNonNull(entry.getUser()),
                entry.getReason(),
                entry.getSource(),
                Optional.ofNullable(entry.getExpires()).map(Date::toInstant)
            );
        }

        IpBanListEntry toIpBanEntry() {
            return new IpBanListEntry(this.ip(), null, this.source(), this.expires().map(Date::from).orElse(null), this.reason());
        }
    }

    public record IpBanDto(String ip, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
        public static final MapCodec<IpBanlistService.IpBanDto> CODEC = RecordCodecBuilder.mapCodec(
            p_442908_ -> p_442908_.group(
                    Codec.STRING.fieldOf("ip").forGetter(IpBanlistService.IpBanDto::ip),
                    Codec.STRING.optionalFieldOf("reason").forGetter(IpBanlistService.IpBanDto::reason),
                    Codec.STRING.optionalFieldOf("source").forGetter(IpBanlistService.IpBanDto::source),
                    ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(IpBanlistService.IpBanDto::expires)
                )
                .apply(p_442908_, IpBanlistService.IpBanDto::new)
        );

        private static IpBanlistService.IpBanDto from(IpBanlistService.IpBan ipBan) {
            return new IpBanlistService.IpBanDto(ipBan.ip(), Optional.ofNullable(ipBan.reason()), Optional.of(ipBan.source()), ipBan.expires());
        }

        public static IpBanlistService.IpBanDto from(IpBanListEntry entry) {
            return from(IpBanlistService.IpBan.from(entry));
        }

        private IpBanlistService.IpBan toIpBan() {
            return new IpBanlistService.IpBan(this.ip(), this.reason().orElse(null), this.source().orElse("Management server"), this.expires());
        }
    }
}
