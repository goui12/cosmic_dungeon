package net.goui.cosmicdungeon.mixin;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SpectralArrow.class)
public abstract class D1SpectralArrowMixin {
    @Inject(method="doPostHurtEffects",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$ability(LivingEntity target,CallbackInfo ci){
        if(net.goui.cosmicdungeon.playerclass.d1.D1ArrowAbilities.apply((AbstractArrow)(Object)this,target))ci.cancel();
    }
}
