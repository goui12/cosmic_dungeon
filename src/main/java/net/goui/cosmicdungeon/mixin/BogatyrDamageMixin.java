package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrModes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wolf.class)
public abstract class BogatyrDamageMixin {
    @Inject(method="hurtServer",at=@At("RETURN"))
    private void cosmicdungeon$dropRetaliation(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        var wolf=(Wolf)(Object)this;
        if(BogatyrModes.standing(wolf))BogatyrModes.hold(wolf);
    }
}
