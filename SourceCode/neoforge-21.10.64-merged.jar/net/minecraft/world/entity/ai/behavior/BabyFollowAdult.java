package net.minecraft.world.entity.ai.behavior;

import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class BabyFollowAdult {
    public static OneShot<LivingEntity> create(UniformInt followRange, float speedModifier) {
        return create(followRange, p_147421_ -> speedModifier, MemoryModuleType.NEAREST_VISIBLE_ADULT, false);
    }

    public static OneShot<LivingEntity> create(
        UniformInt followRange, Function<LivingEntity, Float> speedModifier, MemoryModuleType<? extends LivingEntity> nearestVisibleAdult, boolean targetEyeHeight
    ) {
        return BehaviorBuilder.create(
            p_419440_ -> p_419440_.group(
                    p_419440_.present(nearestVisibleAdult), p_419440_.registered(MemoryModuleType.LOOK_TARGET), p_419440_.absent(MemoryModuleType.WALK_TARGET)
                )
                .apply(
                    p_419440_,
                    (p_419445_, p_419446_, p_419447_) -> (p_258326_, p_416204_, p_258328_) -> {
                        if (!p_416204_.isBaby()) {
                            return false;
                        } else {
                            LivingEntity livingentity = p_419440_.get(p_419445_);
                            if (p_416204_.closerThan(livingentity, followRange.getMaxValue() + 1) && !p_416204_.closerThan(livingentity, followRange.getMinValue())
                                )
                             {
                                WalkTarget walktarget = new WalkTarget(
                                    new EntityTracker(livingentity, targetEyeHeight, targetEyeHeight), speedModifier.apply(p_416204_), followRange.getMinValue() - 1
                                );
                                p_419446_.set(new EntityTracker(livingentity, true, targetEyeHeight));
                                p_419447_.set(walktarget);
                                return true;
                            } else {
                                return false;
                            }
                        }
                    }
                )
        );
    }
}
