package net.goui.cosmicdungeon.dungeon.d1;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.goui.cosmicdungeon.mercenary.MercenaryResurrection;
import net.goui.cosmicdungeon.mercenary.MercenaryResurrectionState;
import net.goui.cosmicdungeon.transaction.InventoryTransactionGuard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Provenance and bounded reordering for physical items from the latest real D1 death.
 * The layout never owns items: native world entities and the player's real inventory remain authoritative.
 */
public final class DeathInventoryRecovery {
    private static final String MARKER = "cosmicdungeon_death_inventory_v1";
    private static final String OWNER = "owner";
    private static final String DEATH = "death";
    private static final String RUN = "run";
    private static final String SLOT = "slot";

    private DeathInventoryRecovery() {}

    public record Provenance(UUID owner, UUID death, long run, int slot) {
        public Provenance {
            if (owner == null || death == null || run <= 0 || slot < 0 || slot >= 128)
                throw new IllegalArgumentException("Invalid death item provenance");
        }
    }

    public record Pickup(long run, UUID death, int targetSlot, List<ItemStack> before, ItemStack template) {
        public Pickup {
            before = List.copyOf(before);
            template = template.copyWithCount(1);
        }
    }

    /** Called after NeoForge's cancellable death hook and before native death loot starts. */
    public static void prepareDeath(ServerPlayer player) {
        var run = MercenaryResurrection.activeRun(player);
        if (run == null) return;
        var death = MercenaryResurrection.prepareDeath(player);
        if (death == null) return;
        var data = D1RunData.get(player.level().getServer());
        var existing = DeathInventoryLayout.read(data, run.runId(), player);
        if (existing != null && existing.death().equals(death.id())) return;
        try {
            DeathInventoryLayout.capture(data, run.runId(), player, death.id());
        } catch (RuntimeException error) {
            DeathInventoryLayout.clear(data, run.runId(), player.getUUID());
            com.mojang.logging.LogUtils.getLogger().error(
                    "Death inventory layout disabled for run {} player {}; physical drops remain authoritative",
                    run.runId(), player.getUUID(), error);
        }
    }

    /** Marks the actual world entity produced from one real inventory slot. */
    public static void markDrop(ServerPlayer player, ItemEntity item, int slot) {
        if (item == null || slot < 0) return;
        var run = MercenaryResurrection.activeRun(player);
        if (run == null) return;
        var data = D1RunData.get(player.level().getServer());
        var death = MercenaryResurrectionState.death(data, run.runId(), player.getUUID());
        var layout = DeathInventoryLayout.read(data, run.runId(), player);
        if (death == null || layout == null || !layout.death().equals(death.id()) || layout.entry(slot) == null) return;

        var marker = new CompoundTag();
        marker.putString(OWNER, player.getUUID().toString());
        marker.putString(DEATH, death.id().toString());
        marker.putLong(RUN, run.runId());
        marker.putInt(SLOT, slot);
        item.getPersistentData().put(MARKER, marker);
    }

    public static boolean marked(ItemEntity item) {
        return item != null && item.getPersistentData().contains(MARKER);
    }

    public static Provenance provenance(ItemEntity item) {
        if (!marked(item)) return null;
        try {
            var marker = item.getPersistentData().getCompoundOrEmpty(MARKER);
            return new Provenance(UUID.fromString(marker.getStringOr(OWNER, "")),
                    UUID.fromString(marker.getStringOr(DEATH, "")),
                    marker.getLongOr(RUN, -1), marker.getIntOr(SLOT, -1));
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    /** Native merges are allowed only when both physical entities carry identical slot provenance. */
    public static boolean mayMerge(ItemEntity first, ItemEntity second) {
        boolean a = marked(first), b = marked(second);
        if (!a && !b) return true;
        if (a != b) return false;
        var left = provenance(first);
        var right = provenance(second);
        return left != null && left.equals(right);
    }

    /**
     * Snapshot only when this is a current latest-death item and ordinary pickup is transaction-safe.
     * Unsafe pickup still proceeds natively; only the optional reordering is skipped.
     */
    public static Pickup beginPickup(ServerPlayer player, ItemEntity item) {
        var provenance = provenance(item);
        if (provenance == null || !provenance.owner().equals(player.getUUID()) || !player.isAlive()) return null;
        var run = MercenaryResurrection.activeRun(player);
        if (run == null || run.runId() != provenance.run()) return null;
        var layout = DeathInventoryLayout.read(D1RunData.get(player.level().getServer()), run.runId(), player);
        if (layout == null || !layout.death().equals(provenance.death()) || layout.entry(provenance.slot()) == null
                || !layout.dimension().equals(item.level().dimension().location().toString())) return null;
        if (!pickupSafe(player)) return null;

        var inventory = player.getInventory();
        var before = new ArrayList<ItemStack>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) before.add(inventory.getItem(slot).copy());
        return new Pickup(run.runId(), provenance.death(), provenance.slot(), before, item.getItem());
    }

    public static void finishPickup(ServerPlayer player, ItemEntity item, Pickup pickup, int inserted) {
        if (pickup == null || inserted <= 0 || !pickupSafe(player)) return;
        var provenance = provenance(item);
        if (provenance == null || provenance.run() != pickup.run() || !provenance.death().equals(pickup.death())
                || provenance.slot() != pickup.targetSlot() || !provenance.owner().equals(player.getUUID())) return;
        var run = MercenaryResurrection.activeRun(player);
        if (run == null || run.runId() != pickup.run()) return;
        var layout = DeathInventoryLayout.read(D1RunData.get(player.level().getServer()), run.runId(), player);
        if (layout == null || !layout.death().equals(pickup.death()) || layout.entry(pickup.targetSlot()) == null) return;

        try {
            organize(player, pickup);
        } catch (RuntimeException error) {
            // Never roll back or duplicate an already successful native pickup.
            com.mojang.logging.LogUtils.getLogger().error(
                    "Latest-death inventory organization skipped after native pickup for {}", player.getUUID(), error);
        }
    }

    private static boolean pickupSafe(ServerPlayer player) {
        return player.containerMenu == player.inventoryMenu && player.inventoryMenu.getCarried().isEmpty()
                && !InventoryTransactionGuard.blocked(player);
    }

    private static void organize(ServerPlayer player, Pickup pickup) {
        var inventory = player.getInventory();
        if (pickup.before().size() != inventory.getContainerSize()) return;
        var after = new ArrayList<ItemStack>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) after.add(inventory.getItem(slot).copy());

        net.minecraft.world.inventory.Slot menuSlot = null;
        for (var slot : player.inventoryMenu.slots) {
            if (slot.container == inventory && slot.getContainerSlot() == pickup.targetSlot()) {
                menuSlot = slot;
                break;
            }
        }
        if (menuSlot == null) return;
        int limit = menuSlot.getMaxStackSize(pickup.template());
        var plan = DeathInventoryLayout.plan(pickup.before(), after, pickup.template(), pickup.targetSlot(),
                limit, menuSlot.mayPlace(pickup.template()));
        if (plan.empty()) return;

        // Validate the entire plan before the first mutation.
        for (var move : plan.moves()) {
            var current = inventory.getItem(move.sourceSlot());
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, pickup.template())
                    || current.getCount() < move.count()) return;
        }
        var target = inventory.getItem(pickup.targetSlot());
        if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, pickup.template())) return;
        if (target.getCount() + plan.addToTarget() > Math.min(limit, pickup.template().getMaxStackSize())) return;

        for (var move : plan.moves()) {
            var next = inventory.getItem(move.sourceSlot()).copy();
            next.shrink(move.count());
            inventory.setItem(move.sourceSlot(), next.isEmpty() ? ItemStack.EMPTY : next);
        }
        var nextTarget = inventory.getItem(pickup.targetSlot()).copy();
        if (nextTarget.isEmpty()) nextTarget = pickup.template().copyWithCount(plan.addToTarget());
        else nextTarget.grow(plan.addToTarget());
        inventory.setItem(pickup.targetSlot(), nextTarget);
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
    }
}
