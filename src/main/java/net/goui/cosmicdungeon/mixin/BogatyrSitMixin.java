package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrModes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native damage and vanilla sit-goal cleanup cannot undo an authoritative Stand Ground command. */
@Mixin(TamableAnimal.class)
public abstract class BogatyrSitMixin {
    @Inject(method={"setOrderedToSit","setInSittingPose"},at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$keepGround(boolean sitting,CallbackInfo ci){
        if(!sitting&&(Object)this instanceof Wolf wolf&&BogatyrModes.standing(wolf))ci.cancel();
    }
}
