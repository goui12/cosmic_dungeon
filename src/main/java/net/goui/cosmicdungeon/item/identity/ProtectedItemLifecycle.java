package net.goui.cosmicdungeon.item.identity;

import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

/** Retain exact stacks in vanilla Inventory/equipment, not a second cross-file item store.
 * Gear Trading 2.0 (2026-08-18): class-issued gear stays no-drop, ordinary gear can be lost.
 * The dungeon failure/abandonment recovery still replaces this inventory with its outside snapshot. */
public final class ProtectedItemLifecycle {
    private ProtectedItemLifecycle() {}
    public static boolean retain(ServerPlayer player, ItemStack stack) {
        return ItemLifecyclePolicy.retain(ItemMovementRules.flags(stack).noDrop(), AccessPolicy.isDeveloper(player));
    }
    public static void dropUnprotected(ServerPlayer player) {
        var inventory = player.getInventory();
        // Includes all mapped equipment slots in Minecraft 1.21.10.
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (!stack.isEmpty() && !retain(player, stack)) {
                player.drop(stack, true, false);
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        inventory.setChanged();
    }
    public static void cloneRetained(ServerPlayer target, ServerPlayer source, boolean keepEverything) {
        if (!ItemLifecyclePolicy.copyRetained(keepEverything,
                target.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY),
                source.isSpectator(), AccessPolicy.isDeveloper(source))) return;
        var before = source.getInventory(); var after = target.getInventory();
        // This runs BEFORE NeoForge's clone event. Vanilla leaves these slots empty in this branch.
        // Fail before any mutation if another mixin violates that contract; never overwrite items.
        for (int slot = 0; slot < before.getContainerSize(); slot++)
            if (retain(source, before.getItem(slot)) && !after.getItem(slot).isEmpty())
                throw new IllegalStateException("Protected respawn slot is occupied: " + slot);
        for (int slot = 0; slot < before.getContainerSize(); slot++)
            if (!before.getItem(slot).isEmpty() && retain(source, before.getItem(slot)))
                after.setItem(slot, before.getItem(slot).copy());
        after.setChanged();
    }
}
