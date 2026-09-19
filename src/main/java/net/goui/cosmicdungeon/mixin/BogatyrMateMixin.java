package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrWolfEvents;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wolf.class)
public abstract class BogatyrMateMixin {
    @Inject(method="canMate", at=@At("HEAD"), cancellable=true)
    private void cosmicdungeon$healthyPack(Animal other, CallbackInfoReturnable<Boolean> result) {
        Wolf wolf = (Wolf)(Object)this;
        if (other instanceof Wolf mate && !BogatyrWolfEvents.breedingAllowed(wolf, mate))
            result.setReturnValue(false);
    }
}
