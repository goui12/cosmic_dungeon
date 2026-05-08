package net.minecraft.server.jsonrpc.methods;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.UserWhiteListEntry;

public class AllowlistService {
    public static List<PlayerDto> get(MinecraftApi api) {
        return api.allowListService()
            .getEntries()
            .stream()
            .filter(p_443314_ -> p_443314_.getUser() != null)
            .map(p_443621_ -> PlayerDto.from(p_443621_.getUser()))
            .toList();
    }

    public static List<PlayerDto> add(MinecraftApi api, List<PlayerDto> players, ClientInfo clientInfo) {
        List<CompletableFuture<Optional<NameAndId>>> list = players.stream()
            .map(p_448839_ -> api.playerListService().getUser(p_448839_.id(), p_448839_.name()))
            .toList();

        for (Optional<NameAndId> optional : Util.sequence(list).join()) {
            optional.ifPresent(p_448834_ -> api.allowListService().add(new UserWhiteListEntry(p_448834_), clientInfo));
        }

        return get(api);
    }

    public static List<PlayerDto> clear(MinecraftApi api, ClientInfo clientInfo) {
        api.allowListService().clear(clientInfo);
        return get(api);
    }

    public static List<PlayerDto> remove(MinecraftApi api, List<PlayerDto> players, ClientInfo clientInfo) {
        List<CompletableFuture<Optional<NameAndId>>> list = players.stream()
            .map(p_448841_ -> api.playerListService().getUser(p_448841_.id(), p_448841_.name()))
            .toList();

        for (Optional<NameAndId> optional : Util.sequence(list).join()) {
            optional.ifPresent(p_448837_ -> api.allowListService().remove(p_448837_, clientInfo));
        }

        api.allowListService().kickUnlistedPlayers(clientInfo);
        return get(api);
    }

    public static List<PlayerDto> set(MinecraftApi api, List<PlayerDto> players, ClientInfo clientInfo) {
        List<CompletableFuture<Optional<NameAndId>>> list = players.stream()
            .map(p_448849_ -> api.playerListService().getUser(p_448849_.id(), p_448849_.name()))
            .toList();
        Set<NameAndId> set = Util.sequence(list).join().stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        Set<NameAndId> set1 = api.allowListService().getEntries().stream().map(StoredUserEntry::getUser).collect(Collectors.toSet());
        set1.stream().filter(p_443565_ -> !set.contains(p_443565_)).forEach(p_448844_ -> api.allowListService().remove(p_448844_, clientInfo));
        set.stream()
            .filter(p_442755_ -> !set1.contains(p_442755_))
            .forEach(p_448847_ -> api.allowListService().add(new UserWhiteListEntry(p_448847_), clientInfo));
        api.allowListService().kickUnlistedPlayers(clientInfo);
        return get(api);
    }
}
