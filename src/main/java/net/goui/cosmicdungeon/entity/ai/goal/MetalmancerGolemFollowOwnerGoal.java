package net.goui.cosmicdungeon.entity.ai.goal;

import net.goui.cosmicdungeon.entity.MetalmancerGolemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Keeps the Metalmancer golem within a configurable radius of its owner.
 *
 * - If farther than startDistance, it will pathfind toward the owner.
 * - It stops when closer than stopDistance.
 */
public class MetalmancerGolemFollowOwnerGoal extends Goal {

    private final MetalmancerGolemEntity mob;
    private final double speedModifier;
    private final double startDistanceSqr;
    private final double stopDistanceSqr;

    private Player owner;

    public MetalmancerGolemFollowOwnerGoal(MetalmancerGolemEntity mob,
                                           double speedModifier,
                                           double startDistance,
                                           double stopDistance) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.startDistanceSqr = startDistance * startDistance;
        this.stopDistanceSqr = stopDistance * stopDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player p = mob.getOwner();
        if (p == null || !p.isAlive() || p.isSpectator()) return false;
        if (mob.isPassenger()) return false;

        double distSqr = mob.distanceToSqr(p);
        if (distSqr <= startDistanceSqr) return false;

        this.owner = p;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null || !owner.isAlive() || owner.isSpectator()) return false;
        if (mob.isPassenger()) return false;

        double distSqr = mob.distanceToSqr(owner);
        // Stop following if we've come sufficiently close
        return distSqr > stopDistanceSqr && !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (owner != null) {
            mob.getNavigation().moveTo(owner, speedModifier);
        }
    }

    @Override
    public void stop() {
        owner = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;

        mob.getLookControl().setLookAt(owner, 30.0F, 30.0F);

        double distSqr = mob.distanceToSqr(owner);
        if (distSqr > stopDistanceSqr) {
            mob.getNavigation().moveTo(owner, speedModifier);
        } else {
            mob.getNavigation().stop();
        }
    }
}
