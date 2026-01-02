// file: src/main/java/net/goui/cosmicdungeon/menu/ClassSelectorMenu.java
package net.goui.cosmicdungeon.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ClassSelectorMenu extends AbstractContainerMenu {

    public ClassSelectorMenu(int id, Inventory inv) {
        super(ModMenus.CLASS_SELECTOR.get(), id);
        // GUI-only menu: no slots
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * REQUIRED in Minecraft / NeoForge 1.21.10
     * Must be PUBLIC to match AbstractContainerMenu.
     * GUI-only menu => shift-click does nothing.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }
}
