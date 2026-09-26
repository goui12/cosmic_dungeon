package net.goui.cosmicdungeon.mixin.client;

import net.goui.cosmicdungeon.client.DungeonInventoryRecipeBook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
public abstract class InventoryRecipeBookComponentMixin {
    @Shadow @Final protected RecipeBookMenu menu;
    @Unique private boolean cosmicdungeon$bookHidden;

    @Inject(method = "isVisibleAccordingToBookData", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$hideWithoutChangingPreference(CallbackInfoReturnable<Boolean> cir) {
        cosmicdungeon$bookHidden = DungeonInventoryRecipeBook.hidden(this.menu);
        // Used by vanilla init AND tick, so a remembered-open book cannot reopen itself.
        // Do not call setVisible: it changes saved book preferences and sends a packet.
        if (cosmicdungeon$bookHidden) cir.setReturnValue(false);
    }

    @Inject(method = "renderGhostRecipe", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$hideRecipePreview(GuiGraphics graphics, boolean biggerResultSlot, CallbackInfo ci) {
        if (cosmicdungeon$bookHidden) ci.cancel();
    }
}
