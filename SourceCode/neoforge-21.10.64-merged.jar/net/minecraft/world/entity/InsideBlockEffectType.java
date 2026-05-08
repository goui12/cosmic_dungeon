package net.minecraft.world.entity;

import java.util.function.Consumer;
import net.minecraft.world.level.block.BaseFireBlock;

public enum InsideBlockEffectType {
    FREEZE(p_405140_ -> {
        p_405140_.setIsInPowderSnow(true);
        if (p_405140_.canFreeze()) {
            p_405140_.setTicksFrozen(Math.min(p_405140_.getTicksRequiredToFreeze(), p_405140_.getTicksFrozen() + 1));
        }
    }),
    CLEAR_FREEZE(Entity::clearFreeze),
    FIRE_IGNITE(BaseFireBlock::fireIgnite),
    LAVA_IGNITE(Entity::lavaIgnite),
    EXTINGUISH(Entity::clearFire);

    private final Consumer<Entity> effect;

    private InsideBlockEffectType(Consumer<Entity> effect) {
        this.effect = effect;
    }

    public Consumer<Entity> effect() {
        return this.effect;
    }
}
