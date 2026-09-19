package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.dungeon.d1.D1StoredInventoryData;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** D20/D24: durable owner handoff before retiring run escrow; no world inventory projection. */
public final class DungeonInventoryHandoffs {
    public static final String RECEIPT = "dungeon_inventory_handoff_v1";
    public static final String COMPLETED = "dungeon_inventory_completed_run_v1";
    private static final Set<ServerPlayer> HOLDS = Collections.newSetFromMap(new WeakHashMap<>());
    private DungeonInventoryHandoffs() {}
    private static PendingDungeonRecoveryData data(ServerPlayer p) { return PendingDungeonRecoveryData.get(p.level().getServer()); }
    private static CompoundTag root(ServerPlayer p) { return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG); }
    public static boolean blocked(ServerPlayer p) {
        var d = data(p);
        return HOLDS.contains(p) || d.handoff(p.getUUID()) != null || d.get(p.getUUID()).isPresent();
    }
    private static boolean hold(ServerPlayer p, RuntimeException error) {
        HOLDS.add(p);
        com.mojang.logging.LogUtils.getLogger().error("Dungeon inventory recovery held for {}; saved evidence retained", p.getUUID(), error);
        p.connection.disconnect(Component.literal("Your dungeon inventory needs recovery. Reconnect; if this repeats, ask a developer to review the saved handoff."));
        return false;
    }
    public static boolean readyForCleanup(MinecraftServer server, List<UUID> owners) {
        var d = PendingDungeonRecoveryData.get(server);
        for (UUID owner : owners) {
            if (d.get(owner).isPresent()) return false;
            var plan = d.handoff(owner);
            if (plan == null) continue;
            var player = server.getPlayerList().getPlayer(owner);
            if (player != null && !reconcile(player)) return false;
            plan = d.handoff(owner);
            if (plan != null && player == null && plan.kind().equals("cleanup")) {
                try {
                    if (!d.flushVerified()) return false;
                    completeWorld(server, plan);
                    plan = d.handoff(owner);
                } catch (RuntimeException failure) {
                    com.mojang.logging.LogUtils.getLogger().error("Offline handoff side effects held for {}", owner, failure);
                    return false;
                }
            }
            if (plan != null && (!plan.kind().equals("cleanup") || !plan.worldReady())) return false;
        }
        return true;
    }
    /** Freeze one decision, including offline owners. Existing decisions are never replaced. */
    public static boolean cleanup(MinecraftServer server, DungeonRunRegistryData.RunRecord run, UUID owner, String reason) {
        var d = PendingDungeonRecoveryData.get(server);
        var online = server.getPlayerList().getPlayer(owner);
        try {
            if (d.completed(owner) >= run.runId()) return d.flushVerified();
            var plan = d.handoff(owner);
            if (plan == null) {
                if (d.get(owner).isPresent()) throw new IllegalStateException("Legacy unreceipted recovery needs review");
                if (!InventoryTransactionGuard.readyForCleanup(server, List.of(owner))) return false;
                if (online != null) {
                    if (!online.isAlive() || HOLDS.contains(online)) return false;
                    online.closeContainer();
                    if (!InventoryTransactionGuard.beforeInventoryChange(online)) return false;
                }
                var original = run.snapshotFor(owner).orElseThrow(() -> new IllegalStateException("Missing pre-entry snapshot"));
                var escrow = DungeonInventoryEscrowData.get(server).get(run.runId(), owner).orElse(null);
                plan = InventoryHandoffPlan.cleanup(owner, run.runId(), reason, original.inventoryNbt(), escrow,
                        DungeonRunRegistryData.get(server).startupImage(run.runId(), owner),
                        ChopOwnershipData.get(server).image(owner),
                        online == null ? new CompoundTag() : ChopTravelRecovery.saveInventory(online), online != null);
                if (online != null) {
                    if (!plan.keep()) ChopTravelRecovery.decode(online, plan.tag("after"));
                    if (plan.reason().equals("COMPLETED")) ChopTravelRecovery.decode(online, plan.tag("stored"));
                    if (!PlayerSaveProof.saveWithLocation(online)) throw new IllegalStateException("Cleanup source player save not verified");
                }
                d.begin(plan);
            }
            if (!plan.kind().equals("cleanup") || plan.run() != run.runId()) throw new IllegalStateException("Different handoff pending");
            if (!d.flushVerified()) throw new IllegalStateException("Cleanup decision not verified");
            if (online != null) return reconcile(online);
            completeWorld(server, plan);
            return true;
        } catch (RuntimeException failure) {
            if (online != null) return hold(online, failure);
            com.mojang.logging.LogUtils.getLogger().error("Offline cleanup held for {} run {}", owner, run.runId(), failure);
            return false;
        }
    }
    private static void completeWorld(MinecraftServer server, InventoryHandoffPlan plan) {
        var d = PendingDungeonRecoveryData.get(server);
        if (plan.worldReady()) return;
        if (plan.kind().equals("cleanup")) {
            if (plan.reason().equals("COMPLETED")) {
                var stored = D1StoredInventoryData.get(server);
                stored.stash(plan.run(), plan.owner(), plan.tag("stored"));
                if (!stored.flushVerified()) throw new IllegalStateException("Success inventory stash not verified");
                DungeonRunProgressData.get(server).markCompleted(plan.owner(), "dungeon_1", "NORMAL");
            }
            if (!ChopOwnershipData.get(server).compareAndSetVerified(plan.owner(), plan.tag("ownership_before"), plan.tag("ownership_after")))
                throw new IllegalStateException("Cleanup Chop entitlement not verified");
            var escrow = DungeonInventoryEscrowData.get(server);
            var current = DungeonInventoryEscrowData.image(escrow.get(plan.run(), plan.owner()).orElse(null));
            if (!current.isEmpty() && !current.equals(plan.tag("escrow"))) throw new IllegalStateException("Cleanup escrow differs");
            escrow.remove(plan.run(), plan.owner());
            if (!escrow.flushVerified()) throw new IllegalStateException("Escrow retirement not verified");
            if (Set.of("KICKED", "LINK_DEAD").contains(plan.reason())) {
                var runs = DungeonRunRegistryData.get(server);
                runs.removePlayer(plan.run(), plan.owner());
                if (!runs.flushVerified()) throw new IllegalStateException("Member removal not verified");
                DungeonRunProgressData.get(server).clearPlayerFromRun(plan.run(), plan.owner());
                net.goui.cosmicdungeon.achievement.plantflags.PlantFlagService.clearPlayerForRun(server, plan.run(), plan.owner());
                net.goui.cosmicdungeon.trade.TradeRecoveryData.get(server).clearOwnerRun(plan.owner(), plan.run());
            }
        } else {
            var stored = D1StoredInventoryData.get(server);
            stored.applyClaim(plan.key(), plan.tag("stored"), plan.tag("stored_after"));
            if (!stored.flushVerified()) throw new IllegalStateException("Claim remainder not verified");
        }
        d.worldReady(plan);
        if (!d.flushVerified()) throw new IllegalStateException("World handoff receipt not verified");
    }
    public static boolean reconcile(ServerPlayer p) {
        if (HOLDS.contains(p)) return false;
        try {
            var d = data(p);
            if (d.get(p.getUUID()).isPresent()) throw new IllegalStateException("Legacy recovery has no player receipt; review complete save before adoption");
            var plan = d.handoff(p.getUUID());
            if (plan == null) return true;
            if (!d.flushVerified()) throw new IllegalStateException("Handoff decision not verified");
            // A partial backup containing overlapping custody is held, never resolved by choosing a winner.
            if (ChopTravelRecovery.blocked(p) || net.goui.cosmicdungeon.economy.DeathCurrencyService.blocked(p)
                    || net.goui.cosmicdungeon.vendor.CommerceTransactions.blocked(p)
                    || net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairTransactions.blocked(p)
                    || net.goui.cosmicdungeon.trade.TradeTransactions.blocked(p))
                throw new IllegalStateException("Other inventory custody is unresolved");
            if (!plan.receipted(root(p).getCompoundOrEmpty(RECEIPT))) {
                if (!p.isAlive()) throw new IllegalStateException("Recovery needs a living owner");
                p.closeContainer();
                if (!plan.canApply(root(p).getCompoundOrEmpty(RECEIPT), root(p).getLongOr(COMPLETED, 0), ChopTravelRecovery.saveInventory(p)))
                    throw new IllegalStateException("Handoff conflicts with the owner inventory or completion history");
                var replacement = plan.keep() ? null : ChopTravelRecovery.decode(p, plan.tag("after"));
                if (plan.kind().equals("cleanup")) {
                    completeWorld(p.level().getServer(), plan);
                    DungeonLifecycleService.applyHandoffCleanup(p, plan);
                }
                if (replacement != null) {
                    for (int slot = 0; slot < replacement.size(); slot++) p.getInventory().setItem(slot, replacement.get(slot).copy());
                    p.getInventory().setChanged();
                }
                var next = root(p).copy(); next.put(RECEIPT, plan.receipt());
                if (plan.kind().equals("cleanup")) next.putLong(COMPLETED, plan.run());
                p.getPersistentData().put(ClassData.ROOT_TAG, next);
            }
            if (!PlayerSaveProof.saveWithLocation(p)) throw new IllegalStateException("Owner handoff receipt not verified");
            // Claim remainder is retired only AFTER the matching owner inventory/receipt is durable.
            completeWorld(p.level().getServer(), d.handoff(p.getUUID()));
            d.acknowledge(plan);
            if (!d.flushVerified()) throw new IllegalStateException("Handoff acknowledgement not verified");
            p.inventoryMenu.broadcastChanges();
            return true;
        } catch (RuntimeException failure) { return hold(p, failure); }
    }
    /** One stored run and a configured stack budget per command/login; simulation never mutates live inventory. */
    public static int claim(ServerPlayer p) {
        if (DungeonRunRegistryData.get(p.level().getServer()).findRunForPlayer(p.getUUID()).isPresent()
                || !p.isAlive() || p.containerMenu != p.inventoryMenu || !p.inventoryMenu.getCarried().isEmpty()
                || !InventoryTransactionGuard.beforeInventoryChange(p)) return 0;
        try {
            var stored = D1StoredInventoryData.get(p.level().getServer()); String key = stored.first(p.getUUID());
            if (key == null) return 0;
            var original = stored.image(key); var remaining = ChopTravelRecovery.decode(p, original);
            var before = ChopTravelRecovery.saveInventory(p); var after = ChopTravelRecovery.inventory(p);
            int returned = 0, attempted = 0;
            for (var stack : remaining) {
                if (stack.isEmpty()) continue;
                if (++attempted > Config.PROTECTED_RECOVERY_BATCH.get()) break;
                int count = stack.getCount();
                for (int slot = 0; slot < Math.min(36, after.size()) && !stack.isEmpty(); slot++) {
                    var target = after.get(slot);
                    if (!target.isEmpty() && ItemStack.isSameItemSameComponents(target, stack)) {
                        int moved = Math.min(stack.getCount(), Math.max(0, Math.min(target.getMaxStackSize(), p.getInventory().getMaxStackSize()) - target.getCount()));
                        target.grow(moved); stack.shrink(moved);
                    }
                }
                for (int slot = 0; slot < Math.min(36, after.size()) && !stack.isEmpty(); slot++) {
                    if (!after.get(slot).isEmpty()) continue;
                    int moved = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), p.getInventory().getMaxStackSize()));
                    after.set(slot, stack.copyWithCount(moved)); stack.shrink(moved);
                }
                returned += count - stack.getCount();
            }
            var remainder = remaining.stream().allMatch(ItemStack::isEmpty) ? new CompoundTag() : ChopTravelRecovery.encode(p, remaining);
            if (returned == 0 && !remainder.isEmpty()) {
                p.sendSystemMessage(Component.literal("Stored belongings need inventory space. Use /d1 claim when you have room."));
                return 0;
            }
            var empty = new CompoundTag();
            var plan = InventoryHandoffPlan.create(p.getUUID(), D1StoredInventoryData.run(key), "claim", "", false, true,
                    before, ChopTravelRecovery.encode(p, after), original, empty, empty, empty, key, remainder);
            if (!PlayerSaveProof.saveWithLocation(p)) throw new IllegalStateException("Claim source player save not verified");
            data(p).begin(plan);
            if (!data(p).flushVerified()) throw new IllegalStateException("Stored claim decision not verified");
            if (!reconcile(p)) return 0;
            if (stored.hasPending(p.getUUID())) p.sendSystemMessage(Component.literal("More belongings remain stored. Use /d1 claim to continue."));
            return returned;
        } catch (RuntimeException failure) { hold(p, failure); return 0; }
    }
    /** Destructive restore may proceed with offline owners only after their independent handoffs are durable. */
    public static boolean dimensionReady(MinecraftServer server, String dimension) {
        var d = PendingDungeonRecoveryData.get(server);
        for (var run : DungeonRunRegistryData.get(server).listAllRuns()) {
            if (!run.dungeonId().equals("dungeon_1") || !run.dungeonDimensionIds().contains(dimension)) continue;
            if (run.stateEnum() != DungeonRunState.RESETTING && run.stateEnum() != DungeonRunState.FAILED) return false;
            for (UUID owner : run.orderedPlayers()) {
                if (!d.durable(owner, run.runId())) return false;
                var player = server.getPlayerList().getPlayer(owner);
                if (player != null && blocked(player)) return false;
            }
        }
        return true;
    }
    // TODO(M03/M93, licensed TEST): Q&A D20/D24 (2026-09-16) requires both exact inventories,
    // success-only lifetime records and Raw return entitlement. Interrupt native dedicated/integrated
    // writes at each decision/stash/owner/escrow/player/ack boundary, including offline completion,
    // full inventories and failed entry. Pre-journal pending records lack proof of prior delivery:
    // retain them for complete-backup review; never automatically replay or delete an ambiguous copy.
}
