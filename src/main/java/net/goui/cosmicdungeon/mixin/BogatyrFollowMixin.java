package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrWolfEvents;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FollowOwnerGoal.class)
public abstract class BogatyrFollowMixin {
    @Shadow @Final private TamableAnimal tamable;
    @Shadow @Final @Mutable private float startDistance;
    @Inject(method="canUse",at=@At("HEAD"))
    private void cosmicdungeon$follow(CallbackInfoReturnable<Boolean> cir){
        if(tamable instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf))startDistance=Config.WOLF_FOLLOW_RANGE.get().floatValue();
    }
}
