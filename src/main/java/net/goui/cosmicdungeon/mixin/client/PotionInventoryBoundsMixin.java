package net.goui.cosmicdungeon.mixin.client;

import net.goui.cosmicdungeon.client.screen.crafting.PotionCraftClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A release over the potion list is inside the UI, so native drag cleanup cannot drop carried items. */
@Mixin({AbstractContainerScreen.class, AbstractRecipeBookScreen.class})
public abstract class PotionInventoryBoundsMixin {
    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$includePotionList(double x, double y, int left, int top,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (PotionCraftClient.ownsInput((Screen)(Object)this, x, y)) cir.setReturnValue(false);
    }
}
