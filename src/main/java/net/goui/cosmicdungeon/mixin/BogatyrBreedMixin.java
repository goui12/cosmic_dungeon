package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrWolfEvents;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BreedGoal.class)
public abstract class BogatyrBreedMixin {
    @Shadow @Final protected Animal animal;
    @ModifyConstant(method={"tick","canContinueToUse"},constant=@Constant(intValue=60))
    private int cosmicdungeon$breedingTime(int vanilla){
        return animal instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf)?Config.WOLF_BREED_TICKS.get():vanilla;
    }
}
