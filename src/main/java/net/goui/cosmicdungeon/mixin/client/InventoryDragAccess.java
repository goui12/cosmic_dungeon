package net.goui.cosmicdungeon.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Native touchscreen drags are separate from the menu cursor stack. */
@Mixin(AbstractContainerScreen.class)
public interface InventoryDragAccess {
    @Accessor("draggingItem")
    ItemStack cosmicdungeon$draggingItem();
}
