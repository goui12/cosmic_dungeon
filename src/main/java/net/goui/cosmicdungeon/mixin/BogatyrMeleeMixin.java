package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrWolfEvents;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bound heavy path rebuilding without skipping native movement or attack cooldown ticks. */
@Mixin(MeleeAttackGoal.class)
public abstract class BogatyrMeleeMixin {
    @Shadow @Final protected PathfinderMob mob;
    @Shadow private int ticksUntilNextPathRecalculation;
    private void cosmicdungeon$pathDelay(){
        if(mob instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf))
            ticksUntilNextPathRecalculation=Math.max(ticksUntilNextPathRecalculation,20+Math.floorMod(wolf.getUUID().hashCode(),5));
    }
    @Inject(method="start",at=@At("TAIL"))
    private void cosmicdungeon$start(CallbackInfo ci){cosmicdungeon$pathDelay();}
    @Inject(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(Lnet/minecraft/world/entity/Entity;D)Z"))
    private void cosmicdungeon$path(CallbackInfo ci){cosmicdungeon$pathDelay();}
}
