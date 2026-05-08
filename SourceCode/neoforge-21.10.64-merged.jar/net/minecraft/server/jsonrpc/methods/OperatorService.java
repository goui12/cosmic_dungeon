package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;

public class OperatorService {
    public static List<OperatorService.OperatorDto> get(MinecraftApi api) {
        return api.operatorListService()
            .getEntries()
            .stream()
            .filter(p_443484_ -> p_443484_.getUser() != null)
            .map(OperatorService.OperatorDto::from)
            .toList();
    }

    public static List<OperatorService.OperatorDto> clear(MinecraftApi api, ClientInfo clientInfo) {
        api.operatorListService().clear(clientInfo);
        return get(api);
    }

    public static List<OperatorService.OperatorDto> remove(MinecraftApi api, List<PlayerDto> players, ClientInfo clientInfo) {
        List<CompletableFuture<Optional<NameAndId>>> list = players.stream()
            .map(p_448888_ -> api.playerListService().getUser(p_448888_.id(), p_448888_.name()))
            .toList();

        for (Optional<NameAndId> optional : Util.sequence(list).join()) {
            optional.ifPresent(p_448886_ -> api.operatorListService().deop(p_448886_, clientInfo));
        }

        return get(api);
    }

    public static List<OperatorService.OperatorDto> add(MinecraftApi api, List<OperatorService.OperatorDto> entries, ClientInfo clientInfo) {
        List<CompletableFuture<Optional<OperatorService.Op>>> list = entries.stream()
            .map(
                p_448883_ -> api.playerListService()
                    .getUser(p_448883_.player().id(), p_448883_.player().name())
                    .thenApply(
                        p_442900_ -> p_442900_.map(p_442579_ -> new OperatorService.Op(p_442579_, p_448883_.permissionLevel(), p_448883_.bypassesPlayerLimit()))
                    )
            )
            .toList();

        for (Optional<OperatorService.Op> optional : Util.sequence(list).join()) {
            optional.ifPresent(
                p_448878_ -> api.operatorListService().op(p_448878_.user(), p_448878_.permissionLevel(), p_448878_.bypassesPlayerLimit(), clientInfo)
            );
        }

        return get(api);
    }

    public static List<OperatorService.OperatorDto> set(MinecraftApi api, List<OperatorService.OperatorDto> entries, ClientInfo clientInfo) {
        List<CompletableFuture<Optional<OperatorService.Op>>> list = entries.stream()
            .map(
                p_448890_ -> api.playerListService()
                    .getUser(p_448890_.player().id(), p_448890_.player().name())
                    .thenApply(
                        p_442611_ -> p_442611_.map(p_443329_ -> new OperatorService.Op(p_443329_, p_448890_.permissionLevel(), p_448890_.bypassesPlayerLimit()))
                    )
            )
            .toList();
        Set<OperatorService.Op> set = Util.sequence(list).join().stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        Set<OperatorService.Op> set1 = api.operatorListService()
            .getEntries()
            .stream()
            .filter(p_443549_ -> p_443549_.getUser() != null)
            .map(p_443213_ -> new OperatorService.Op(p_443213_.getUser(), Optional.of(p_443213_.getLevel()), Optional.of(p_443213_.getBypassesPlayerLimit())))
            .collect(Collectors.toSet());
        set1.stream().filter(p_442831_ -> !set.contains(p_442831_)).forEach(p_448875_ -> api.operatorListService().deop(p_448875_.user(), clientInfo));
        set.stream()
            .filter(p_442947_ -> !set1.contains(p_442947_))
            .forEach(p_448881_ -> api.operatorListService().op(p_448881_.user(), p_448881_.permissionLevel(), p_448881_.bypassesPlayerLimit(), clientInfo));
        return get(api);
    }

    record Op(NameAndId user, Optional<Integer> permissionLevel, Optional<Boolean> bypassesPlayerLimit) {
    }

    public record OperatorDto(PlayerDto player, Optional<Integer> permissionLevel, Optional<Boolean> bypassesPlayerLimit) {
        public static final MapCodec<OperatorService.OperatorDto> CODEC = RecordCodecBuilder.mapCodec(
            p_442612_ -> p_442612_.group(
                    PlayerDto.CODEC.codec().fieldOf("player").forGetter(OperatorService.OperatorDto::player),
                    Codec.INT.optionalFieldOf("permissionLevel").forGetter(OperatorService.OperatorDto::permissionLevel),
                    Codec.BOOL.optionalFieldOf("bypassesPlayerLimit").forGetter(OperatorService.OperatorDto::bypassesPlayerLimit)
                )
                .apply(p_442612_, OperatorService.OperatorDto::new)
        );

        public static OperatorService.OperatorDto from(ServerOpListEntry emtry) {
            return new OperatorService.OperatorDto(
                PlayerDto.from(Objects.requireNonNull(emtry.getUser())), Optional.of(emtry.getLevel()), Optional.of(emtry.getBypassesPlayerLimit())
            );
        }
    }
}
