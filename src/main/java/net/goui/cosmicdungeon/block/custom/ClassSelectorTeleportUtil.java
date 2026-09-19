// file: src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorTeleportUtil.java
package net.goui.cosmicdungeon.block.custom;

import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.block.entity.ClassSelectorBlockEntity;
import net.goui.cosmicdungeon.playerclass.api.ClassNet;
import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ClassSelectorTeleportUtil {
    private ClassSelectorTeleportUtil() {}

    private static final String PD_ROOT = "cosmicdungeon";
    private static final String PD_PENDING_SEL = "pending_class_selector";
    private static final String PD_DIM = "dim";
    private static final String PD_POS = "pos";
    private static final String PD_EXPIRES = "expires";

    /** Called when a dungeoneer opens the selector from a specific block. */
    public static void markPendingSelectorSource(ServerPlayer sp, ServerLevel level, BlockPos pos) {
        if (sp == null || level == null || pos == null) return;

        CompoundTag root = sp.getPersistentData().getCompoundOrEmpty(PD_ROOT).copy();

        CompoundTag pend = new CompoundTag();
        pend.putString(PD_DIM, level.dimension().location().toString());
        pend.putLong(PD_POS, pos.asLong());
        pend.putLong(PD_EXPIRES, level.getGameTime() + 20L * net.goui.cosmicdungeon.Config.SELECTOR_SESSION_SECONDS.get()); // Configured session TTL

        root.put(PD_PENDING_SEL, pend);
        sp.getPersistentData().put(PD_ROOT, root);
    }

    public static boolean validSession(ServerPlayer player) {
        return !AccessPolicy.isDeveloper(player)
                && player.containerMenu instanceof net.goui.cosmicdungeon.menu.ClassSelectorMenu menu
                && menu.stillValid(player)
                && net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.canReady(
                        net.goui.cosmicdungeon.npc.tamsin.TamsinService.accepted(player), menu.stage());
    }

    public static boolean validSelectionSession(ServerPlayer player) {
        return validSession(player)
                && ((net.goui.cosmicdungeon.menu.ClassSelectorMenu) player.containerMenu).stage()
                == net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage.SELECTOR;
    }

    public static boolean validSourceSession(ServerPlayer player){
        if(!(player.containerMenu instanceof net.goui.cosmicdungeon.menu.ClassSelectorMenu)||!player.isAlive()||player.isSpectator())return false;
        if(net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).isPresent())return false;
        var pending=player.getPersistentData().getCompoundOrEmpty(PD_ROOT).getCompoundOrEmpty(PD_PENDING_SEL);
        if(pending.isEmpty()||pending.getLongOr(PD_EXPIRES,-1)<player.level().getGameTime()
                ||!pending.getStringOr(PD_DIM,"").equals(player.level().dimension().location().toString()))return false;
        var pos=BlockPos.of(pending.getLongOr(PD_POS,0));
        double range=net.goui.cosmicdungeon.Config.SELECTOR_RANGE.get();
        return player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))<=range*range
                &&player.level().getBlockEntity(pos) instanceof ClassSelectorBlockEntity;
    }

    public record SelectorSource(String dimId, long posLong) {}
    public static SelectorSource pendingSource(ServerPlayer player) {
        var pending = player.getPersistentData().getCompoundOrEmpty(PD_ROOT).getCompoundOrEmpty(PD_PENDING_SEL);
        return pending.isEmpty() ? null : new SelectorSource(pending.getStringOr(PD_DIM, ""), pending.getLongOr(PD_POS, 0));
    }

    public static boolean isReadyEligibleClass(String classId) {
        return ClassItemUtil.isPlayableClass(classId) && !ClassNet.isDisabledClassSelection(classId);
    }

    private static void clearPending(CompoundTag playerPd) {
        if (playerPd == null) return;
        CompoundTag root = playerPd.getCompoundOrEmpty(PD_ROOT).copy();
        root.remove(PD_PENDING_SEL);
        playerPd.put(PD_ROOT, root);
    }

    /** Exposed so ReadyManager can use the same resolver. */
    public static ServerLevel resolveLevel(MinecraftServer server, String dimId) {
        if (server == null || dimId == null || dimId.isBlank()) return null;

        ResourceLocation rl = ResourceLocation.tryParse(dimId);
        if (rl == null) return null;

        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, rl);
        return server.getLevel(key);
    }

    /** Exposed so ReadyManager can use the same dev warning. */
    public static void notifyDevelopersMissingDestination(MinecraftServer server, ServerPlayer user, ServerLevel selectorLevel, BlockPos selectorPos) {
        if (server == null || user == null || selectorLevel == null || selectorPos == null) return;

        String dim = selectorLevel.dimension().location().toString();
        String coords = selectorPos.toShortString();

        Component msg = Component.literal("USER ")
                .withStyle(ChatFormatting.RED)
                .append(user.getName().copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" tried to teleport from a class selection block at ").withStyle(ChatFormatting.RED))
                .append(Component.literal(dim).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" ").withStyle(ChatFormatting.RED))
                .append(Component.literal(coords).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" but no destination is selected.").withStyle(ChatFormatting.RED));

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!AccessPolicy.isDeveloper(p)) continue;
            p.sendSystemMessage(msg);
        }

        user.displayClientMessage(
                Component.literal("That Class Selector has no teleport destination set.").withStyle(ChatFormatting.RED),
                true
        );
    }
}
