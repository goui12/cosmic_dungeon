package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Ingredient.class)
public abstract class RepairIngredientMixin {
    @Inject(method="test(Lnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$repairOnly(ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(RepairComponents.marked(stack)) cir.setReturnValue(false);
    }
    @Inject(method="isSimple",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$componentAware(CallbackInfoReturnable<Boolean> cir) {
        // The item-only fast path cannot distinguish marked repair supplies from normal materials.
        cir.setReturnValue(false);
    }
}
