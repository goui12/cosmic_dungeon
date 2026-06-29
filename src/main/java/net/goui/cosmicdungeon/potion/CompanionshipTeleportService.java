package net.goui.cosmicdungeon.potion;

import net.goui.cosmicdungeon.dungeon.DungeonLifecycleService;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CompanionshipTeleportService {
    private static final Map<UUID, Long> PENDING_SELECTIONS = new HashMap<>();

    private CompanionshipTeleportService() {}

    public static void beginSelection(ServerPlayer sp, int durationTicks) {
        if (sp == null || durationTicks <= 0) return;

        PENDING_SELECTIONS.put(sp.getUUID(), sp.serverLevel().getGameTime() + durationTicks);
    }

    public static void teleport(ServerPlayer sp, UUID targetId) {
        if (sp == null || targetId == null) return;
        var runOpt = DungeonLifecycleService.findActiveRunForPlayer(sp);
        if (runOpt.isEmpty()) { sp.sendSystemMessage(Component.literal("You’re not part of an active dungeon group").withStyle(ChatFormatting.RED)); return; }
        if (!runOpt.get().orderedPlayers().contains(targetId)) { sp.sendSystemMessage(Component.literal("That player is not in your active dungeon group.").withStyle(ChatFormatting.RED)); return; }
        if (!hasPendingSelection(sp)) { sp.sendSystemMessage(Component.literal("Teleportation selection expired.").withStyle(ChatFormatting.RED)); return; }
        ServerPlayer target = sp.server.getPlayerList().getPlayer(targetId);
        if (target == null) { sp.sendSystemMessage(Component.literal("That dungeoneer is no longer online.").withStyle(ChatFormatting.RED)); return; }

        var run = runOpt.get();
        var targetRunOpt = DungeonLifecycleService.findActiveRunForPlayer(target);
        if (targetRunOpt.isEmpty()
                || targetRunOpt.get().runId() != run.runId()
                || targetRunOpt.get().isCompletionExited(targetId)) {
            sp.sendSystemMessage(Component.literal("That player is no longer in your active dungeon group.").withStyle(ChatFormatting.RED));
            return;
        }

        if (!isTargetInTeleportableDungeonDimension(run, target)) {
            sp.removeEffect(ModMobEffects.TELEPORT_COOLDOWN);
            sp.sendSystemMessage(Component.literal("That player is no longer inside the dungeon.").withStyle(ChatFormatting.RED));
            return;
        }

        PENDING_SELECTIONS.remove(sp.getUUID());
        sp.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), java.util.Set.of(), target.getYRot(), target.getXRot(), true);
    }

    private static boolean isTargetInTeleportableDungeonDimension(DungeonRunRegistryData.RunRecord run, ServerPlayer target) {
        if (run == null || target == null) return false;

        var targetDimension = target.level().dimension();
        if (run.containsDimension(targetDimension)) return true;

        return "dungeon_1".equals(run.dungeonId()) && Level.NETHER.equals(targetDimension);
    }

    private static boolean hasPendingSelection(ServerPlayer sp) {
        Long expiresAtTick = PENDING_SELECTIONS.get(sp.getUUID());
        if (expiresAtTick == null) return false;

        if (sp.serverLevel().getGameTime() > expiresAtTick) {
            PENDING_SELECTIONS.remove(sp.getUUID());
            return false;
        }

        return true;
    }
}
