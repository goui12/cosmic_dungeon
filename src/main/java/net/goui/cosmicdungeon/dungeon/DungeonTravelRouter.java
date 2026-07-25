package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** Single server-authoritative resolver for every dungeon-bound travel path. */
public final class DungeonTravelRouter {
    private DungeonTravelRouter() {}

    public sealed interface Result {
        record Allowed(ServerLevel level, BlockPos pos) implements Result {}
        record Rejected(String message) implements Result {}
    }

    public static Result resolve(ServerPlayer player, ResourceKey<Level> requestedDimension, BlockPos requestedPos) {
        if (player == null || requestedDimension == null || requestedPos == null) return new Result.Rejected("Invalid destination.");
        MinecraftServer server = player.level().getServer();
        if (server == null) return new Result.Rejected("Server unavailable.");

        Optional<DungeonDefinition> template = DungeonDefinitions.byDimension(requestedDimension);
        Optional<DungeonRunRegistryData.RunRecord> active = DungeonLifecycleService.findActiveRunForPlayer(player);

        if (template.isPresent()) {
            if (active.isPresent()) {
                DungeonRunRegistryData.RunRecord run = active.get();
                if (!run.dungeonId().equalsIgnoreCase(template.get().id())) {
                    return new Result.Rejected("That dungeon destination is outside your active lifecycle.");
                }
                ResourceKey<Level> physical = DungeonInstanceSlots.translateTemplate(template.get(), run.instanceSlot(), requestedDimension)
                        .orElse(null);
                ServerLevel level = physical == null ? null : server.getLevel(physical);
                return level == null ? new Result.Rejected("Your dungeon instance is unavailable.")
                        : new Result.Allowed(level, requestedPos);
            }
            if (!AccessPolicy.isDeveloper(player)) {
                return new Result.Rejected("Only developers may enter dungeon template worlds.");
            }
            ServerLevel level = server.getLevel(requestedDimension);
            return level == null ? new Result.Rejected("Dungeon template is unavailable.") : new Result.Allowed(level, requestedPos);
        }

        Optional<Integer> slot = DungeonInstanceSlots.slotOf(requestedDimension);
        if (slot.isPresent()) {
            Optional<DungeonRunRegistryData.RunRecord> owner = DungeonRunRegistryData.get(server).findRunForInstanceDimension(requestedDimension);
            if (owner.isPresent() && owner.get().containsPlayer(player.getUUID())) {
                ServerLevel level = server.getLevel(requestedDimension);
                return level == null ? new Result.Rejected("Dungeon instance is unavailable.") : new Result.Allowed(level, requestedPos);
            }
            if (AccessPolicy.isDeveloper(player)) {
                ServerLevel level = server.getLevel(requestedDimension);
                return level == null ? new Result.Rejected("Dungeon instance is unavailable.") : new Result.Allowed(level, requestedPos);
            }
            return new Result.Rejected("You are not a member of that dungeon instance.");
        }

        ServerLevel level = server.getLevel(requestedDimension);
        return level == null ? new Result.Rejected("Destination dimension is unavailable.") : new Result.Allowed(level, requestedPos);
    }

    public static boolean evacuateUnauthorizedLocation(ServerPlayer player) {
        if (player == null) return false;
        ResourceKey<Level> current = player.level().dimension();
        boolean templateViolation = DungeonDefinitions.byDimension(current).isPresent() && !AccessPolicy.isDeveloper(player);
        boolean instanceViolation = DungeonInstanceSlots.slotOf(current).isPresent()
                && DungeonLifecycleService.findActiveRunForPlayer(player)
                .filter(run -> run.containsDimension(current)).isEmpty()
                && !AccessPolicy.isDeveloper(player);
        if (!templateViolation && !instanceViolation) return false;
        DungeonLifecycleService.teleportToMainWorldSpawn(player);
        return true;
    }
}
