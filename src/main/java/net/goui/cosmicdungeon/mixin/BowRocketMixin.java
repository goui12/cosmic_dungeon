package net.goui.cosmicdungeon.mixin;

import java.util.function.Predicate;
import net.goui.cosmicdungeon.item.custom.D1FireworkItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native draw/consumption still own the stack; both inventory and held rockets are loadable. */
@Mixin(BowItem.class)
public abstract class BowRocketMixin {
    @Inject(method="getAllSupportedProjectiles",at=@At("RETURN"),cancellable=true)
    private void cosmicdungeon$rockets(CallbackInfoReturnable<Predicate<ItemStack>> ci){
        ci.setReturnValue(ci.getReturnValue().or(ammo->ammo.is(Items.FIREWORK_ROCKET)
                ||D1FireworkItem.isCustomRocket(ammo)));
    }
}
