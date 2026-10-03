package net.goui.cosmicdungeon.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.goui.cosmicdungeon.item.identity.ClassItemOwnership;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bind actual inventory ingress, including shift-click, ordinary clicks and damaged-item pickup. */
@Mixin(Inventory.class)
public abstract class AttunedInventoryMixin {
    @Shadow @Final public Player player;

    @Inject(method = "setItem", at = @At("HEAD"))
    private void cosmicdungeon$bindInserted(int slot, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) ClassItemOwnership.bind(serverPlayer, stack);
    }

    @WrapMethod(method = "add(ILnet/minecraft/world/item/ItemStack;)Z")
    private boolean cosmicdungeon$bindCollected(int slot, ItemStack stack, Operation<Boolean> original) {
        if (!(player instanceof ServerPlayer serverPlayer)) return original.call(slot, stack);
        boolean bound = ClassItemOwnership.bind(serverPlayer, stack);
        try { return original.call(slot, stack); }
        finally {
            // Only inserted copies are bound. A full inventory or partial pickup cannot reserve
            // the untouched world remainder for this player.
            if (bound) ClassItemOwnership.unbindRemainder(stack, serverPlayer.getUUID());
        }
    }
}
