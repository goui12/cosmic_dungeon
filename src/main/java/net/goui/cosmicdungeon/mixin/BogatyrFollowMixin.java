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
    @org.spongepowered.asm.mixin.injection.ModifyConstant(method="tick",constant=@org.spongepowered.asm.mixin.injection.Constant(intValue=10))
    private int cosmicdungeon$pathDelay(int vanilla){
        return tamable instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf)?20+Math.floorMod(wolf.getUUID().hashCode(),5):vanilla;
    }
    @Inject(method="canUse",at=@At("HEAD"))
    private void cosmicdungeon$follow(CallbackInfoReturnable<Boolean> cir){
        if(tamable instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf))startDistance=Config.WOLF_FOLLOW_RANGE.get().floatValue();
    }
}
