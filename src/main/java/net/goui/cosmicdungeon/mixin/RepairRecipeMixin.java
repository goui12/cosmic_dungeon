package net.goui.cosmicdungeon.mixin;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class RepairRecipeMixin {
    @Inject(method="matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$noOrdinaryRepair(CraftingInput input,Level level,CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
