package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrWolfEvents;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TamableAnimal.class)
public abstract class BogatyrTeleportMixin {
    @Inject(method="shouldTryTeleportToOwner",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$distance(CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf)&&wolf.getOwner()!=null)
            cir.setReturnValue(wolf.distanceToSqr(wolf.getOwner())>Math.pow(Config.WOLF_TELEPORT_DISTANCE.get(),2));
    }
}
