package net.goui.cosmicdungeon.client;

import java.util.List;
import net.goui.cosmicdungeon.playerclass.api.ExtraInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;

/** Client presentation only; native menu identities, item custody and server crafting policy stay intact. */
public final class InventoryCraftingPresentation {
    private InventoryCraftingPresentation() {}

    public static boolean hidden(AbstractContainerScreen<?> screen, Slot slot) {
        // Creative picker slots have independent meanings; never hide them by a shared index.
        if (!(screen instanceof InventoryScreen || screen instanceof ExtraInventoryScreen)
                || !(screen.getMenu() instanceof AbstractCraftingMenu crafting)) return false;
        return craftingSlot(slot, crafting.getResultSlot(), crafting.getInputGridSlots());
    }

    static boolean craftingSlot(Slot slot, Slot result, List<Slot> inputs) {
        if (slot == null) return false;
        if (slot == result) return true;
        for (var input : inputs) if (slot == input) return true;
        return false;
    }

    /** Native slotClicked prefers slot.index over slotId, including keyboard and touchscreen paths. */
    public static boolean hiddenClick(AbstractContainerScreen<?> screen, Slot slot, int slotId) {
        int index = slot == null ? slotId : slot.index;
        var menu = screen.getMenu();
        return index >= 0 && index < menu.slots.size() && hidden(screen, menu.getSlot(index));
    }
}
