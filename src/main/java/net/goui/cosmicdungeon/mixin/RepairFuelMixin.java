package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class RepairFuelMixin {
    @Inject(method="getBurnDuration",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$noRepairFuel(FuelValues fuels,ItemStack stack,CallbackInfoReturnable<Integer> cir) {
        if(RepairComponents.marked(stack)) cir.setReturnValue(0);
    }
}
