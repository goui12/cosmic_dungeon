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

    public static boolean canTravelInside(ServerPlayer player, DungeonRunRegistryData.RunRecord run) {
        if (player == null || run == null) return false;
        var server = player.level().getServer();
        boolean d1 = run.dungeonId().equals("dungeon_1");
        var escrows = DungeonInventoryEscrowData.get(server);
        var escrow = escrows.get(run.runId(), player.getUUID());
        boolean eligibleClass = !d1 || net.goui.cosmicdungeon.playerclass.api.ClassNet.getSelectableClasses(player)
                .contains(net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil.getClassId(player));
        return DungeonTravelRules.inside(new DungeonTravelRules.Member(run.runId(),
                run.stateEnum() == DungeonRunState.ACTIVE, run.containsPlayer(player.getUUID()),
                run.isCompletionExited(player.getUUID()), DungeonRunRegistryData.get(server).starting(run.runId()),
                d1 && net.goui.cosmicdungeon.dungeon.d1.D1RunData.get(server).sealed(run.runId()),
                escrow.map(DungeonInventoryEscrowData.Entry::outsideActive).orElse(escrows.hasOwner(player.getUUID())),
                run.containsDimension(player.level().dimension()), player.isAlive(), player.isSpectator(),
                AccessPolicy.isDeveloper(player), net.goui.cosmicdungeon.transaction.InventoryTransactionGuard.blocked(player),
                eligibleClass));
    }

    public static Result resolve(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
        return resolveRift(player, "", dimension, pos, false);
    }

    public static Result resolveRift(ServerPlayer player, String destinationName, ResourceKey<Level> requestedDimension,
                                     BlockPos requestedPos, boolean resetTrigger) {
        if (player == null || requestedDimension == null || requestedPos == null) return new Result.Rejected("Invalid destination.");
        MinecraftServer server = player.level().getServer();
        if (server == null) return new Result.Rejected("Server unavailable.");
        if (net.goui.cosmicdungeon.transaction.InventoryTransactionGuard.blocked(player))
            return new Result.Rejected("Finish inventory recovery before travelling.");
        boolean developer = AccessPolicy.isDeveloper(player);
        if (!developer && (!player.isAlive() || player.isSpectator()))
            return new Result.Rejected("You cannot travel in your current state.");
        if (!DungeonTravelRules.village(DungeonTravelRules.villageName(destinationName),
                net.goui.cosmicdungeon.progression.ProgressionService.hasVillageAccess(player), developer))
            return new Result.Rejected("Complete Dungeon 1 with Watson to unlock Main Village.");

        var registry = DungeonRunRegistryData.get(server);
        var active = DungeonLifecycleService.findActiveRunForPlayer(player);
        var currentOwner = registry.findRunForInstanceDimension(player.level().dimension());
        boolean inTemplate = DungeonDefinitions.byDimension(player.level().dimension()).isPresent();
        boolean inPhysical = DungeonInstanceSlots.slotOf(player.level().dimension()).isPresent();
        if (!developer && (inTemplate || inPhysical)
                && (currentOwner.isEmpty() || !canTravelInside(player, currentOwner.get())))
            return new Result.Rejected("Your dungeon membership or inventory handoff is not ready.");

        var template = DungeonDefinitions.byDimension(requestedDimension);
        boolean physicalTarget = DungeonInstanceSlots.slotOf(requestedDimension).isPresent();
        boolean insideD1 = currentOwner.filter(r -> r.dungeonId().equals("dungeon_1")).isPresent();
        if (!DungeonTravelRules.riftBoundary(insideD1, template.isPresent() || physicalTarget, resetTrigger, developer))
            return new Result.Rejected(resetTrigger ? "A reset exit must leave the dungeon."
                    : "Use your Chop or the dungeon's reset exit to leave.");

        if (template.isPresent()) {
            if (active.isPresent() && !developer) {
                var run = active.get();
                if (!run.dungeonId().equalsIgnoreCase(template.get().id()) || !canTravelInside(player, run))
                    return new Result.Rejected("That destination is outside your active dungeon travel.");
                ResourceKey<Level> physical = DungeonInstanceSlots.translateTemplate(template.get(), run.instanceSlot(), requestedDimension).orElse(null);
                ServerLevel level = physical == null ? null : server.getLevel(physical);
                return level == null ? new Result.Rejected("Your dungeon instance is unavailable.") : new Result.Allowed(level, requestedPos);
            }
            if (!developer) return new Result.Rejected("Enter through Tamsin with your ready dungeon group.");
        } else if (physicalTarget && !developer) {
            var owner = registry.findRunForInstanceDimension(requestedDimension);
            if (owner.isEmpty() || !canTravelInside(player, owner.get()))
                return new Result.Rejected("You are not an active participant inside that dungeon instance.");
        }
        ServerLevel level = server.getLevel(requestedDimension);
        return level == null ? new Result.Rejected("Destination dimension is unavailable.") : new Result.Allowed(level, requestedPos);
    }

    /** D1 reset exits are performed entirely by the existing saved cleanup handoff.
     * Return true even when held: the tile must never fall through to a raw teleport. */
    public static boolean handleD1ResetExit(ServerPlayer player) {
        var run = DungeonLifecycleService.findActiveRunForPlayer(player).orElse(null);
        if (AccessPolicy.isDeveloper(player) || run == null || !run.dungeonId().equals("dungeon_1")) return false;
        if (!readyForResetExit(player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("The group must finish inventory recovery before exiting."));
        } else {
            DungeonLifecycleService.onPlayerExitedThroughResetRift(player.level(), player);
        }
        return true;
    }

    /** Reset rifts must not move one player before the existing group cleanup can begin. */
    private static boolean readyForResetExit(ServerPlayer player) {
        if (AccessPolicy.isDeveloper(player)) return true;
        var run = DungeonLifecycleService.findActiveRunForPlayer(player).orElse(null);
        return run == null || !run.dungeonId().equals("dungeon_1")
                || canTravelInside(player, run)
                && net.goui.cosmicdungeon.transaction.InventoryTransactionGuard.readyForCleanup(
                        player.level().getServer(), run.orderedPlayers()) && canTravelInside(player, run);
    }

    public static boolean evacuateUnauthorizedLocation(ServerPlayer player) {
        if (player == null || net.goui.cosmicdungeon.dungeon.d1.D1WatsonRecovery.blocked(player) || ChopTravelRecovery.blocked(player) || DungeonInventoryHandoffs.blocked(player)) return false;
        ResourceKey<Level> current = player.level().dimension();
        // A saved reset decision owns the final inventory/location handoff, even before its
        // per-player record can be written. Never evacuate that member around save recovery.
        var owner = DungeonRunRegistryData.get(player.level().getServer()).findRunForInstanceDimension(current);
        if (owner.filter(run -> run.containsPlayer(player.getUUID()) && run.stateEnum() == DungeonRunState.RESETTING).isPresent()) return false;
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
