package net.goui.cosmicdungeon.dungeon.d1;

import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class D1Members {
    private D1Members() {}
    public static Optional<DungeonRunRegistryData.RunRecord> run(ServerLevel level) {
        return DungeonRunRegistryData.get(level.getServer()).findRunForInstanceDimension(level.dimension())
                .filter(r -> r.stateEnum() == DungeonRunState.ACTIVE && r.dungeonId().equals("dungeon_1"));
    }
    public static List<ServerPlayer> active(MinecraftServer server, DungeonRunRegistryData.RunRecord run) {
        List<ServerPlayer> members = new ArrayList<>();
        for (UUID id : run.orderedPlayers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && player.isAlive() && !player.isSpectator()
                    && !AccessPolicy.isDeveloper(player)
                    && !run.completionExitedPlayers().contains(id)) members.add(player);
        }
        return List.copyOf(members);
    }
    public static boolean inside(ServerPlayer player, DungeonRunRegistryData.RunRecord run) {
        return run.containsPlayer(player.getUUID()) && run.containsDimension(player.level().dimension())
                && active(player.level().getServer(), run).contains(player);
    }
}
