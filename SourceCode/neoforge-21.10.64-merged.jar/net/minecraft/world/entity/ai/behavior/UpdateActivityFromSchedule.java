package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;

public class UpdateActivityFromSchedule {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(p_259429_ -> p_259429_.point((p_428171_, p_428172_, p_428173_) -> {
            p_428172_.getBrain().updateActivityFromSchedule(p_428171_.getDayTime(), p_428171_.getGameTime());
            return true;
        }));
    }
}
