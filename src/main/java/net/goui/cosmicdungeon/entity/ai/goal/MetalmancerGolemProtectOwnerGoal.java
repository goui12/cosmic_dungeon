package net.goui.cosmicdungeon.entity.ai.goal;

import net.goui.cosmicdungeon.entity.MetalmancerGolemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Makes the Metalmancer golem:
 * - Attack mobs that the owner is attacking (highest priority).
 * - Otherwise attack mobs that recently attacked the owner.
 *
 * This is a "target" goal: it picks a target and feeds it into the normal
 * MeleeAttackGoal in the golem's goalSelector.
 */
public class MetalmancerGolemProtectOwnerGoal extends Goal {
    private final MetalmancerGolemEntity golem;
    private final double maxDistanceSqr;

    private LivingEntity target;

    /**
     * @param golem       The golem entity.
     * @param maxDistance How far (in blocks) the golem will consider targets around the owner.
     */
    public MetalmancerGolemProtectOwnerGoal(MetalmancerGolemEntity golem, double maxDistance) {
        this.golem = golem;
        this.maxDistanceSqr = maxDistance * maxDistance;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        Player owner = golem.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        // 1) Prefer the mob the owner is attacking
        LivingEntity ownersTarget = owner.getLastHurtMob();

        // 2) Otherwise, fall back to the mob that last hurt the owner
        LivingEntity ownersAttacker = owner.getLastHurtByMob();

        LivingEntity best = selectBestTarget(owner, ownersTarget, ownersAttacker);
        if (best == null) {
            return false;
        }

        this.target = best;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) {
            return false;
        }
        // Don't chase infinitely far away
        return golem.distanceToSqr(target) <= maxDistanceSqr * 2.0D;
    }

    @Override
    public void start() {
        if (this.target != null) {
            golem.setTarget(this.target);
            golem.setAggressive(true);
            // Ensure we are not counted as "resonating" while actively fighting
            golem.setResonating(false);
        }
    }

    @Override
    public void stop() {
        if (golem.getTarget() == this.target) {
            golem.setTarget(null);
        }
        golem.setAggressive(false);
        this.target = null;
    }

    private LivingEntity selectBestTarget(Player owner,
                                          LivingEntity ownersTarget,
                                          LivingEntity ownersAttacker) {
        // Helper: ensure candidate is valid and within range of owner
        java.util.function.Predicate<LivingEntity> valid = candidate -> {
            if (candidate == null) return false;
            if (!candidate.isAlive()) return false;
            if (candidate == owner) return false;
            if (candidate == golem) return false;
            return owner.distanceToSqr(candidate) <= maxDistanceSqr;
        };

        // Priority 1: mob the owner is attacking
        if (valid.test(ownersTarget)) {
            return ownersTarget;
        }

        // Priority 2: mob that attacked the owner
        if (valid.test(ownersAttacker)) {
            return ownersAttacker;
        }

        return null;
    }
}
