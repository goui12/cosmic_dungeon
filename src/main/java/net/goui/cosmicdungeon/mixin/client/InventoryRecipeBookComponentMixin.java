package net.goui.cosmicdungeon.mixin.client;

import net.goui.cosmicdungeon.client.DungeonInventoryRecipeBook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
public abstract class InventoryRecipeBookComponentMixin {
    @Shadow @Final protected RecipeBookMenu menu;

    @Inject(method = "isVisibleAccordingToBookData", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$hideWithoutChangingPreference(CallbackInfoReturnable<Boolean> cir) {
        // Used by vanilla init AND tick, so a remembered-open book cannot reopen itself.
        // Do not call setVisible: it changes saved book preferences and sends a packet.
        if (DungeonInventoryRecipeBook.hidden(this.menu)) cir.setReturnValue(false);
    }

    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$hideBook(CallbackInfoReturnable<Boolean> cir) {
        if (DungeonInventoryRecipeBook.hidden(this.menu)) cir.setReturnValue(false);
    }

    @Inject(method = "toggleVisibility", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$preventOpening(CallbackInfo ci) {
        if (DungeonInventoryRecipeBook.hidden(this.menu)) ci.cancel();
    }

    @Inject(method = "renderGhostRecipe", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$hideRecipePreview(GuiGraphics graphics, boolean biggerResultSlot, CallbackInfo ci) {
        if (DungeonInventoryRecipeBook.hidden(this.menu)) ci.cancel();
    }
}
