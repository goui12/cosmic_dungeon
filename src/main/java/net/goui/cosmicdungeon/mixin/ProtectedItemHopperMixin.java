package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.item.identity.ItemLifecyclePolicy;
import net.goui.cosmicdungeon.item.identity.ItemMovementRules;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class ProtectedItemHopperMixin {
    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void cosmicdungeon$worldOwner(Container container, ItemEntity item, CallbackInfoReturnable<Boolean> cir) {
        if (!ItemLifecyclePolicy.mayAutomate(ItemMovementRules.flags(item.getItem()).privateStorage(), item.getTarget()))
            cir.setReturnValue(false);
    }
    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), cancellable = true)
    private static void cosmicdungeon$insert(Container source, Container destination, ItemStack stack,
                                             Direction direction, CallbackInfoReturnable<ItemStack> cir) {
        if (ItemMovementRules.flags(stack).privateStorage()) cir.setReturnValue(stack);
    }
    @Inject(method = "canTakeItemFromContainer", at = @At("HEAD"), cancellable = true)
    private static void cosmicdungeon$extract(Container source, Container destination, ItemStack stack,
                                              int slot, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (ItemMovementRules.flags(stack).privateStorage()) cir.setReturnValue(false);
    }
    @Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
    private static void cosmicdungeon$capabilityPush(net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, HopperBlockEntity hopper, CallbackInfoReturnable<Boolean> cir) {
        // Five fixed slots. Also protects NeoForge's item-handler push branch before extraction.
        for (int slot = 0; slot < hopper.getContainerSize(); slot++)
            if (ItemMovementRules.flags(hopper.getItem(slot)).privateStorage()) {
                cir.setReturnValue(false); return;
            }
    }
    // TODO(M10/M114, Gear Trading 2.0): third-party item-handler capabilities can bypass
    // vanilla container checks. Verify each installed automation adapter before enabling it;
    // simulation must not extract or reserve a protected stack, and cancellation must retain it.
}
