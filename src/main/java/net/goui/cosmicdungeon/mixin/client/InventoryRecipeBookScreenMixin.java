package net.goui.cosmicdungeon.mixin.client;

import net.goui.cosmicdungeon.client.DungeonInventoryRecipeBook;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class InventoryRecipeBookScreenMixin extends AbstractContainerScreen<RecipeBookMenu> {
    @Unique
    private boolean cosmicdungeon$bookHidden;

    protected InventoryRecipeBookScreenMixin(RecipeBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "initButton", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$omitRecipeControls(CallbackInfo ci) {
        cosmicdungeon$bookHidden = DungeonInventoryRecipeBook.hidden(this.menu);
        if (cosmicdungeon$bookHidden) ci.cancel();
    }

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void cosmicdungeon$refreshRecipeControls(CallbackInfo ci) {
        if (cosmicdungeon$bookHidden != DungeonInventoryRecipeBook.hidden(this.menu)) {
            // Reinitialize layout/widgets only on a mode, permission or dimension boundary.
            // Vanilla keeps the same menu, slots and carried stack.
            this.rebuildWidgets();
        }
    }
}
