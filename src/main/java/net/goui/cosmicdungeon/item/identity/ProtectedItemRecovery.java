package net.goui.cosmicdungeon.item.identity;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

/** Exact overflow lives beside the vanilla inventory in this player's own save and clone root.
 * Gear Trading 2.0 (2026-08-18) requires owner-safe recovery, never cancel-and-delete.
 * Scope 0 is outside inventory; positive IDs belong only to that dungeon run.
 * This is NOT the cross-file trade/repair/currency journal requested by M03/M25/M115. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class ProtectedItemRecovery {
    private static final String KEY = "protected_item_returns_v1";
    private ProtectedItemRecovery() {}
    private static CompoundTag read(ServerPlayer player) {
        return player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).getCompoundOrEmpty(KEY);
    }
    private static void write(ServerPlayer player, CompoundTag pending) {
        var root = player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        if (pending.isEmpty()) root.remove(KEY); else root.put(KEY, pending);
        player.getPersistentData().put(ClassData.ROOT_TAG, root);
    }
    public static boolean pending(ServerPlayer player) { return !read(player).isEmpty(); }
    public static boolean pendingHere(ServerPlayer player) {
        return ProtectedRecoveryEntries.hasClaimable(read(player), scope(player));
    }
    public static long scope(ServerPlayer player) {
        var run = DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).orElse(null);
        boolean outside = run != null && DungeonInventoryEscrowData.get(player.level().getServer())
                .get(run.runId(), player.getUUID()).map(DungeonInventoryEscrowData.Entry::outsideActive).orElse(false);
        return ProtectedRecoveryRules.scope(run == null ? 0 : run.runId(), outside);
    }
    private static CompoundTag encode(ServerPlayer player, ItemStack stack) {
        var problems = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(problems, player.registryAccess());
        output.store("item", ItemStack.CODEC, stack);
        if (!problems.isEmpty()) throw new IllegalStateException("Cannot serialize protected item: " + problems.getReport());
        return output.buildResult();
    }
    public static void validateSerializable(ServerPlayer player, ItemStack stack) { encode(player, stack); }
    /** Source is already detached, and caller must not later restore its old copy.
     * Encode before movement; partial Inventory.add is accounted by the exact remaining count.
     * No world entity is made. Inventory + remainder enter the SAME subsequent player snapshot. */
    public static void returnDetached(ServerPlayer player, ItemStack detached) {
        if (detached.isEmpty()) return;
        var image = encode(player, detached);
        var remaining = detached.copy();
        player.getInventory().add(remaining);
        if (!remaining.isEmpty()) {
            image = ProtectedRecoveryEntries.remainder(image, remaining.getCount());
            image.putLong("run", scope(player));
            var next = read(player).copy();
            next.put(UUID.randomUUID().toString(), image);
            write(player, next);
            notifyPending(player);
        }
        detached.setCount(0);
        player.getInventory().setChanged();
    }
    /** Queue an already detached stack in its original inventory scope; never emit a world item. */
    public static void queueDetached(ServerPlayer player,ItemStack detached,long run){
        if(detached.isEmpty())return;
        if(run<0)throw new IllegalArgumentException("Invalid item return scope");
        var image=encode(player,detached);image.putLong("run",run);
        var next=read(player).copy();next.put(UUID.randomUUID().toString(),image);write(player,next);
        detached.setCount(0);
    }
    public static void notifyPending(ServerPlayer player) {
        if (pending(player)) player.sendSystemMessage(Component.literal(pendingHere(player)
                ? "A protected item is stored safely. Make room and use /d1 recover."
                : "Stored protected belongings are waiting for their original inventory context."));
    }
    public static int claim(ServerPlayer player) {
        if (player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()) {
            player.sendSystemMessage(Component.literal("Close the open interface before recovering items."));
            return 0;
        }
        boolean resetting = PendingDungeonRecoveryData.get(player.level().getServer()).get(player.getUUID()).isPresent();
        long current = scope(player);
        var next = read(player).copy();
        int count = 0, attempted = 0;
        boolean changed = false;
        // Decode/move only a configured number of eligible entries per deliberate command.
        // Malformed or future entries remain untouched for diagnosis, never silently deleted.
        for (String id : new ArrayList<>(next.keySet())) {
            var entry = next.getCompoundOrEmpty(id);
            if (!ProtectedRecoveryRules.claimable(entry.getLongOr("run", -1), current, resetting)) continue;
            if (++attempted > Config.PROTECTED_RECOVERY_BATCH.get()) break;
            var problems = new ProblemReporter.Collector();
            var stack = TagValueInput.create(problems, player.registryAccess(), entry)
                    .read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (!problems.isEmpty() || stack.isEmpty()) continue;
            int before = stack.getCount();
            player.getInventory().add(stack);
            int accepted = before - stack.getCount();
            if (accepted == 0) continue;
            count += accepted; changed = true;
            if (stack.isEmpty()) next.remove(id);
            else {
                next.put(id, ProtectedRecoveryEntries.remainder(entry, stack.getCount()));
            }
        }
        if (changed) {
            write(player, next);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        notifyPending(player);
        return count;
    }
    /** Called after closing menus and before restoring/resetting the player's inventory. */
    public static void finishRun(ServerPlayer player, long runId, boolean successfulD1) {
        var before = read(player);
        var after = ProtectedRecoveryEntries.finish(before, runId, successfulD1);
        if (!after.equals(before)) write(player, after);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        // PlayerList fires this BEFORE save(player). Close even inventoryMenu's carried stack.
        if (event.getEntity() instanceof ServerPlayer player) player.closeContainer();
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) notifyPending(player);
    }
    // TODO(M10/M115, licensed TEST): forced shutdown while an interface is still OPEN is not
    // equivalent to close/logout. World-container/trade/repair save boundaries need their journals.
    // Verify dedicated + integrated save/reload, dead-player clone, exact nested components,
    // partial stacks, Village escrow and offline success/failure before runtime acceptance.
}
