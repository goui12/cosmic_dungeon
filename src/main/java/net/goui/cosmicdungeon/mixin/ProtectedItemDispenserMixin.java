package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.item.identity.ItemMovementRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DispenserBlock.class, DropperBlock.class})
public abstract class ProtectedItemDispenserMixin {
    @Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$stopProtectedAutomation(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (!(level.getBlockEntity(pos) instanceof DispenserBlockEntity container)) return;
        // Nine fixed slots, before random selection, splitting, equipping or an item-handler call.
        // Developer-authored ordinary trap ammunition is unaffected.
        for (int slot = 0; slot < container.getContainerSize(); slot++)
            if (ItemMovementRules.flags(container.getItem(slot)).privateStorage()) { ci.cancel(); return; }
    }
}
