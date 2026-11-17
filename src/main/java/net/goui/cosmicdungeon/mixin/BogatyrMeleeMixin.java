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
    @Shadow private int ticksUntilNextAttack;
    @Unique private int cosmicdungeon$hasteTenths;
    private void cosmicdungeon$pathDelay(){
        if(mob instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf))
            ticksUntilNextPathRecalculation=Math.max(ticksUntilNextPathRecalculation,20+Math.floorMod(wolf.getUUID().hashCode(),5));
    }
    @Inject(method="start",at=@At("TAIL"))
    private void cosmicdungeon$start(CallbackInfo ci){cosmicdungeon$hasteTenths=0;cosmicdungeon$pathDelay();}
    /** Native wolf melee ignores ATTACK_SPEED; add one cooldown tick per ten active ticks. */
    @Inject(method="tick",at=@At("HEAD"))
    private void cosmicdungeon$haste(CallbackInfo ci){
        if(!(mob instanceof Wolf)||mob.getTarget()==null||ticksUntilNextAttack<=0
                ||!net.goui.cosmicdungeon.playerclass.skill.ClassBuffs.hasted(mob)){
            cosmicdungeon$hasteTenths=0;return;
        }
        if(++cosmicdungeon$hasteTenths>=10){
            // Native tick also subtracts one; keep that unused tick when haste reaches zero first.
            if(ticksUntilNextAttack==1)cosmicdungeon$hasteTenths+=10;
            ticksUntilNextAttack--;
            cosmicdungeon$hasteTenths-=10;
        }
    }
    @Inject(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(Lnet/minecraft/world/entity/Entity;D)Z"))
    private void cosmicdungeon$path(CallbackInfo ci){cosmicdungeon$pathDelay();}
}
