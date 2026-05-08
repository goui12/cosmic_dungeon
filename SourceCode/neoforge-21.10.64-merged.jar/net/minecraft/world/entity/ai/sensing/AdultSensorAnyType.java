package net.minecraft.world.entity.ai.sensing;

import java.util.Optional;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class AdultSensorAnyType extends AdultSensor {
    @Override
    protected void setNearestVisibleAdult(LivingEntity p_416217_, NearestVisibleLivingEntities p_416574_) {
        Optional<LivingEntity> optional = p_416574_.findClosest(
                p_423265_ -> p_423265_.getType().is(EntityTypeTags.FOLLOWABLE_FRIENDLY_MOBS) && !p_423265_.isBaby()
            )
            .map(LivingEntity.class::cast);
        p_416217_.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT, optional);
    }
}
