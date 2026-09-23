package net.goui.cosmicdungeon.item.identity;

import net.goui.cosmicdungeon.block.entity.ClassLockedChestBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;

/** Reviewed vanilla chest quick-move route: source class chest -> this player's36 slots. */
public final class ClassChestTransfers {
    private ClassChestTransfers() {}

    public static boolean toPlayer(ServerPlayer player, AbstractContainerMenu menu, Slot source) {
        // Subclasses may override quickMoveStack; their destinations have not been reviewed.
        if (menu.getClass() != ChestMenu.class || source == null) return false;
        var chestMenu = (ChestMenu) menu;
        if (!(chestMenu.getContainer() instanceof ClassLockedChestBlockEntity chest)
                || source.container != chest || !chest.stillValid(player)) return false;
        int boundary = chestMenu.getRowCount() * 9;
        if (boundary != chest.getContainerSize() || menu.slots.size() != boundary + 36) return false;
        int sourceIndex = menu.slots.indexOf(source);
        if (sourceIndex < 0 || sourceIndex >= boundary) return false;
        for (int index = 0; index < boundary; index++)
            if (menu.slots.get(index).container != chest) return false;
        for (int index = boundary; index < menu.slots.size(); index++)
            if (menu.slots.get(index).container != player.getInventory()) return false;
        // Native ChestMenu merges/inserts only into those36 slots; full inventory leaves remainder.
        return true;
    }
}
