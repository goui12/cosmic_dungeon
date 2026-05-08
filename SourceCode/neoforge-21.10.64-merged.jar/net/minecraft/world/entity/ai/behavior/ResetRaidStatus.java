package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ResetRaidStatus {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(p_259870_ -> p_259870_.point((p_415171_, p_415172_, p_415173_) -> {
            if (p_415171_.random.nextInt(20) != 0) {
                return false;
            } else {
                Brain<?> brain = p_415172_.getBrain();
                Raid raid = p_415171_.getRaidAt(p_415172_.blockPosition());
                if (raid == null || raid.isStopped() || raid.isLoss()) {
                    brain.setDefaultActivity(Activity.IDLE);
                    brain.updateActivityFromSchedule(p_415171_.getDayTime(), p_415171_.getGameTime());
                }

                return true;
            }
        }));
    }
}
