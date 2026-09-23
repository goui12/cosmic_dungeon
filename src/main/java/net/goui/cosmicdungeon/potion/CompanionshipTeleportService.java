package net.goui.cosmicdungeon.potion;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.rift.SafeTeleportUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class CompanionshipTeleportService {
    private static final Map<UUID, CompanionshipSelection> PENDING_SELECTIONS = new HashMap<>();
    private CompanionshipTeleportService() {}
    public static boolean eligible(ServerPlayer player) {
        return player != null && DungeonLifecycleService.findActiveRunForPlayer(player)
                .filter(run -> DungeonTravelRouter.canTravelInside(player, run)).isPresent();
    }
    public static boolean eligibleTarget(ServerPlayer player, ServerPlayer target) {
        if (player == null || target == null || player.getUUID().equals(target.getUUID())) return false;
        var run = DungeonLifecycleService.findActiveRunForPlayer(player).orElse(null);
        return run != null && DungeonTravelRouter.canTravelInside(player, run)
                && DungeonTravelRouter.canTravelInside(target, run)
                && DungeonLifecycleService.findActiveRunForPlayer(target).filter(r -> r.runId() == run.runId()).isPresent();
    }
    public static void beginSelection(ServerPlayer player, int durationTicks) {
        if (!eligible(player) || durationTicks <= 0) return;
        var run = DungeonLifecycleService.findActiveRunForPlayer(player).orElseThrow();
        PENDING_SELECTIONS.put(player.getUUID(), CompanionshipSelection.create(run.runId(),
                player.level().getServer().overworld().getGameTime(), durationTicks));
    }
    public static void teleport(ServerPlayer player, UUID targetId) {
        if (player == null || targetId == null) return;
        var run = DungeonLifecycleService.findActiveRunForPlayer(player).orElse(null);
        var selection = PENDING_SELECTIONS.get(player.getUUID());
        long now = player.level().getServer().overworld().getGameTime();
        if (run == null || selection == null || !selection.valid(run.runId(), now)
                || !DungeonTravelRouter.canTravelInside(player, run)) {
            PENDING_SELECTIONS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("Teleportation selection expired or your dungeon travel is unavailable.")); return;
        }
        var target = player.level().getServer().getPlayerList().getPlayer(targetId);
        if (!eligibleTarget(player, target)) {
            player.sendSystemMessage(Component.literal("Choose another active dungeoneer inside your instance.")); return;
        }
        var safe = SafeTeleportUtil.findLoadedSafeTeleportPos(target.level(), target.blockPosition());
        if (safe == null || !target.level().noCollision(player, player.getBoundingBox().move(
                safe.getX() + 0.5D - player.getX(), safe.getY() - player.getY(), safe.getZ() + 0.5D - player.getZ()))) {
            player.sendSystemMessage(Component.literal("There is no safe arrival position near that dungeoneer.")); return;
        }
        if (player.teleportTo(target.level(), safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D,
                Set.of(), target.getYRot(), target.getXRot(), true)) PENDING_SELECTIONS.remove(player.getUUID());
        else player.sendSystemMessage(Component.literal("Teleportation was blocked. You may choose again."));
        // Never clear the already-paid cooldown because a target left or a teleport was cancelled.
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { PENDING_SELECTIONS.remove(e.getEntity().getUUID()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { PENDING_SELECTIONS.remove(e.getEntity().getUUID()); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) { PENDING_SELECTIONS.remove(e.getEntity().getUUID()); }
    @SubscribeEvent public static void death(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) PENDING_SELECTIONS.remove(p.getUUID());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { PENDING_SELECTIONS.clear(); }
    // TODO(M101, retained legacy item): no approved Companionship recipe/unlock table appears in
    // the retained D1 sheet/doc scope. Preserve its registered item and existing five-minute behavior;
    // do not invent stock, class unlocks, waypoint binding, or D2 features from Notes Teleport
    // 1FIcIf82rCAEAhbEE2gbR7jdoFA0BWSIcnzeOcKlUG4M (2026-07-07, explicitly future ideas).
    // Licensed TEST: stale UI after reset/rejoin, escrow outside, two slots, death/logout, blocked
    // arrival and cancelled teleport; the selection must never bypass the journaled Chop handoff.
}
