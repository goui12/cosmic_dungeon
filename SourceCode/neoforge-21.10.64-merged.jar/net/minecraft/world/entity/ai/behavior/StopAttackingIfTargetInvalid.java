package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StopAttackingIfTargetInvalid {
    private static final int TIMEOUT_TO_GET_WITHIN_ATTACK_RANGE = 200;

    public static <E extends Mob> BehaviorControl<E> create(StopAttackingIfTargetInvalid.TargetErasedCallback<E> onStopAttacking) {
        return create((p_376480_, p_147988_) -> false, onStopAttacking, true);
    }

    public static <E extends Mob> BehaviorControl<E> create(StopAttackingIfTargetInvalid.StopAttackCondition canStopAttacking) {
        return create(canStopAttacking, (p_376121_, p_217411_, p_217412_) -> {}, true);
    }

    public static <E extends Mob> BehaviorControl<E> create() {
        return create((p_376685_, p_147986_) -> false, (p_376808_, p_217408_, p_217409_) -> {}, true);
    }

    public static <E extends Mob> BehaviorControl<E> create(
        StopAttackingIfTargetInvalid.StopAttackCondition canStopAttacking, StopAttackingIfTargetInvalid.TargetErasedCallback<E> onStopAttacking, boolean canGrowTiredOfTryingToReachTarget
    ) {
        return BehaviorBuilder.create(
            p_258801_ -> p_258801_.group(p_258801_.present(MemoryModuleType.ATTACK_TARGET), p_258801_.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE))
                .apply(
                    p_258801_,
                    (p_258787_, p_258788_) -> (p_375693_, p_375694_, p_375695_) -> {
                        LivingEntity livingentity = p_258801_.get(p_258787_);
                        if (p_375694_.canAttack(livingentity)
                            && (!canGrowTiredOfTryingToReachTarget || !isTiredOfTryingToReachTarget(p_375694_, p_258801_.tryGet(p_258788_)))
                            && livingentity.isAlive()
                            && livingentity.level() == p_375694_.level()
                            && !canStopAttacking.test(p_375693_, livingentity)) {
                            return true;
                        } else {
                            onStopAttacking.accept(p_375693_, p_375694_, livingentity);
                            p_258787_.erase();
                            return true;
                        }
                    }
                )
        );
    }

    private static boolean isTiredOfTryingToReachTarget(LivingEntity entity, Optional<Long> timeSinceInvalidTarget) {
        return timeSinceInvalidTarget.isPresent() && entity.level().getGameTime() - timeSinceInvalidTarget.get() > 200L;
    }

    @FunctionalInterface
    public interface StopAttackCondition {
        boolean test(ServerLevel level, LivingEntity entity);
    }

    @FunctionalInterface
    public interface TargetErasedCallback<E> {
        void accept(ServerLevel level, E entity, LivingEntity target);
    }
}
