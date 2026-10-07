package net.goui.cosmicdungeon.mixin.client;

import net.goui.cosmicdungeon.client.DungeonInventoryRecipeBook;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class InventoryRecipeBookScreenMixin extends AbstractContainerScreen<RecipeBookMenu> {
    protected InventoryRecipeBookScreenMixin(RecipeBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "initButton", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$omitRecipeControls(CallbackInfo ci) {
        if (DungeonInventoryRecipeBook.hidden(this.menu)) ci.cancel();
    }

}
