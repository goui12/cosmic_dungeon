package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.item.identity.ProtectedItemLifecycle;
import net.goui.cosmicdungeon.item.identity.ProtectedItemRecovery;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Validate serialization while menu inputs still own the original stacks. */
@Mixin(AbstractContainerMenu.class)
public abstract class ProtectedItemReturnMenuMixin {
    @Inject(method = "clearContainer", at = @At("HEAD"))
    private void cosmicdungeon$preflightInputs(Player player, Container container, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot);
            if (ProtectedItemLifecycle.retain(serverPlayer, stack))
                ProtectedItemRecovery.validateSerializable(serverPlayer, stack);
        }
    }
    @Inject(method = "removed", at = @At("HEAD"))
    private void cosmicdungeon$preflightCursor(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            var stack = ((AbstractContainerMenu)(Object)this).getCarried();
            if (ProtectedItemLifecycle.retain(serverPlayer, stack))
                ProtectedItemRecovery.validateSerializable(serverPlayer, stack);
        }
    }
}
