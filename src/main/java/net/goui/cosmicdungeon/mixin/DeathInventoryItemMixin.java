package net.goui.cosmicdungeon.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.goui.cosmicdungeon.dungeon.d1.DeathInventoryRecovery;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class DeathInventoryItemMixin {
    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$preserveDeathSlotProvenance(ItemEntity other, CallbackInfo ci) {
        if (!DeathInventoryRecovery.mayMerge((ItemEntity)(Object)this, other)) ci.cancel();
    }

    @WrapOperation(method = "playerTouch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean cosmicdungeon$organizeLatestDeathPickup(Inventory inventory, ItemStack stack,
                                                            Operation<Boolean> original, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return original.call(inventory, stack);
        var item = (ItemEntity)(Object)this;
        var pickup = DeathInventoryRecovery.beginPickup(serverPlayer, item);
        int before = stack.getCount();
        boolean result = original.call(inventory, stack);
        DeathInventoryRecovery.finishPickup(serverPlayer, item, pickup, Math.max(0, before - stack.getCount()));
        return result;
    }
}
