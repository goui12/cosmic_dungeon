package net.goui.cosmicdungeon.mixin.client;

import net.goui.cosmicdungeon.client.InventoryCraftingPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hide only personal-inventory crafting slots, without removing slots or mutating their stacks. */
@Mixin(AbstractContainerScreen.class)
public abstract class InventoryCraftingSlotsMixin {
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$omitCraftingSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (InventoryCraftingPresentation.hidden((AbstractContainerScreen<?>)(Object)this, slot)) ci.cancel();
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$omitCraftingHitTarget(Slot slot, double x, double y,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (InventoryCraftingPresentation.hidden((AbstractContainerScreen<?>)(Object)this, slot))
            cir.setReturnValue(false);
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$omitCraftingInput(Slot slot, int slotId, int button, ClickType type, CallbackInfo ci) {
        if (InventoryCraftingPresentation.hiddenClick((AbstractContainerScreen<?>)(Object)this, slot, slotId))
            ci.cancel();
    }
}
