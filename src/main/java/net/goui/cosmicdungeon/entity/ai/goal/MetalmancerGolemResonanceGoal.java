package net.goui.cosmicdungeon.entity.ai.goal;

import net.goui.cosmicdungeon.entity.MetalmancerGolemEntity;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import net.goui.cosmicdungeon.playerclass.ore.SatchelIdleTicker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Resonance/forage goal:
 *
 * When the Metalmancer owner is in Stationary Resonance and the golem is
 * within a small radius, the golem:
 *  - stands nearly still
 *  - looks at the owner
 *  - marks itself as resonating so SatchelIdleTicker can double ore income.
 *
 * This goal is automatically pre-empted by higher-priority goals such as
 * melee attacks or recall.
 */
public class MetalmancerGolemResonanceGoal extends Goal {

    private final MetalmancerGolemEntity mob;
    private final double maxDistanceSqr;

    private ServerPlayer owner;

    public MetalmancerGolemResonanceGoal(MetalmancerGolemEntity mob, double maxDistance) {
        this.mob = mob;
        this.maxDistanceSqr = maxDistance * maxDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.level().isClientSide()) return false;
        if (mob.isDeadOrDying()) return false;

        // Never try to resonate while actively fighting
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget != null && currentTarget.isAlive()) {
            return false;
        }

        Player p = mob.getOwner();
        if (!(p instanceof ServerPlayer sp)) return false;
        if (!sp.isAlive() || sp.isSpectator()) return false;

        // Only Metalmancers can produce Stationary Resonance
        if (!ClassNbtUtil.isMetalmancer(sp)) return false;

        // Must be currently in Stationary Resonance (still, grounded, satchel present)
        if (!SatchelIdleTicker.isPlayerResonating(sp)) return false;

        double distSqr = mob.distanceToSqr(sp);
        if (distSqr > maxDistanceSqr) return false;

        this.owner = sp;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.level().isClientSide()) return false;
        if (owner == null || !owner.isAlive() || owner.isSpectator()) return false;

        // If we pick up a combat target mid-goal, bail out of resonance
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget != null && currentTarget.isAlive()) {
            return false;
        }

        // Owner must still be resonating as a Metalmancer
        if (!ClassNbtUtil.isMetalmancer(owner) || !SatchelIdleTicker.isPlayerResonating(owner)) {
            return false;
        }

        double distSqr = mob.distanceToSqr(owner);
        return distSqr <= maxDistanceSqr;
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        // Mark as resonating for SatchelIdleTicker multiplier
        mob.setResonating(true);
    }

    @Override
    public void stop() {
        mob.setResonating(false);
        owner = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;

        // Softly lock movement, but allow tiny drift if physics push
        mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));

        // Always look at the owner
        mob.getLookControl().setLookAt(owner, 30.0F, 30.0F);
    }
}
