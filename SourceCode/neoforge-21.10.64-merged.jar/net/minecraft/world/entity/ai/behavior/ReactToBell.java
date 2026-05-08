package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ReactToBell {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(
            p_259349_ -> p_259349_.group(p_259349_.present(MemoryModuleType.HEARD_BELL_TIME))
                .apply(p_259349_, p_259472_ -> (p_427038_, p_427039_, p_427040_) -> {
                    Raid raid = p_427038_.getRaidAt(p_427039_.blockPosition());
                    if (raid == null) {
                        p_427039_.getBrain().setActiveActivityIfPossible(Activity.HIDE);
                    }

                    return true;
                })
        );
    }
}
