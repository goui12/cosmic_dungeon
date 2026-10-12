package net.goui.cosmicdungeon.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/** Detached placement plans. A caller must commit the selected image with its resource transaction. */
public final class PotionOutputPlacement {
    private PotionOutputPlacement() {}
    /** One craft, no mutation: null means the cursor is incompatible or full. */
    public static ItemStack cursor(ItemStack carried, ItemStack singleOutput) {
        if (singleOutput.isEmpty() || singleOutput.getCount() != 1) return null;
        if (carried.isEmpty()) return singleOutput.copy();
        if (!ItemStack.isSameItemSameComponents(carried, singleOutput)
                || carried.getCount() >= carried.getMaxStackSize()) return null;
        var result = carried.copy(); result.grow(1); return result;
    }
    /** One Shift craft. Prefer hotbar merges/empty slots, then main storage; never armor/offhand. */
    public static NonNullList<ItemStack> inventory(NonNullList<ItemStack> inventory, ItemStack singleOutput) {
        return singleOutput.getCount() == 1 ? inventoryStack(inventory, singleOutput) : null;
    }
    /** Legacy trusted recipes may produce several identical items; all-or-nothing placement stays bounded. */
    static NonNullList<ItemStack> inventoryStack(NonNullList<ItemStack> inventory, ItemStack output) {
        if (output.isEmpty() || output.getCount() > output.getMaxStackSize()) return null;
        var result = NonNullList.withSize(inventory.size(), ItemStack.EMPTY);
        for (int i = 0; i < inventory.size(); i++) result.set(i, inventory.get(i).copy());
        var remaining = output.copy();
        for (int start : new int[] {0, 9}) {
            int end = Math.min(start == 0 ? 9 : 36, result.size());
            for (int slot = start; slot < end; slot++) {
                var current = result.get(slot);
                if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, remaining)) continue;
                int amount = Math.min(remaining.getCount(), Math.max(0, current.getMaxStackSize() - current.getCount()));
                current.grow(amount); remaining.shrink(amount);
                if (remaining.isEmpty()) return result;
            }
            for (int slot = start; slot < end; slot++) if (result.get(slot).isEmpty()) {
                result.set(slot, remaining.copy()); return result;
            }
        }
        return null;
    }
}
