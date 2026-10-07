package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.bogatyr.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(PathNavigation.class)
public abstract class BogatyrNavigationMixin {
    @Shadow @Final protected Mob mob;
    @Inject(method="moveTo(Lnet/minecraft/world/level/pathfinder/Path;D)Z",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$route(Path path,double speed,CallbackInfoReturnable<Boolean> cir){
        if(mob instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf)&&!BogatyrBoundary.allowPath(wolf,path)){
            wolf.getNavigation().stop();cir.setReturnValue(false);
        }
    }
    @Inject(method="getMaxPathLength",at=@At("RETURN"),cancellable=true)
    private void cosmicdungeon$diameter(CallbackInfoReturnable<Float> cir){
        if(mob instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf)&&BogatyrModes.mode(wolf)==WolfMode.DANGER_CLOSE)
            cir.setReturnValue(Math.max(32F,cir.getReturnValue()));
    }
    @Inject(method="recomputePath",at=@At("RETURN"))
    private void cosmicdungeon$recomputed(CallbackInfo ci){
        if(mob instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf)
                &&!BogatyrBoundary.allowPath(wolf,wolf.getNavigation().getPath()))wolf.getNavigation().stop();
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void cosmicdungeon$movingBoundary(CallbackInfo ci){
        if(mob instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf))BogatyrBoundary.check(wolf);
    }
}
