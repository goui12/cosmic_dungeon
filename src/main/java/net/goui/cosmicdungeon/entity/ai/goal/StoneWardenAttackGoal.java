// StoneWardenAttackGoal.java
package net.goui.cosmicdungeon.entity.ai.goal;

import net.goui.cosmicdungeon.entity.StoneWardenEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Custom attack goal that:
 * 1) Navigates toward the target normally.
 * 2) When in melee reach and off cooldown, plays the attack animation immediately.
 * 3) Waits DELAY_TICKS (2.5s) before actually applying damage.
 * 4) Applies damage only if the target is still alive & still in melee reach.
 */
public class StoneWardenAttackGoal extends Goal {
    private static final int DELAY_TICKS = 15;       // 2.5s @ 20 TPS
    private static final int RECOVER_TICKS = 10;     // small recovery after the hit (prevents instant re-windup)

    private final StoneWardenEntity mob;
    private final double speed;
    private final boolean followIfNotSeen;

    private LivingEntity target;
    private int ticksUntilNextAttempt = 0; // generic gate so we don't spam the windup
    private int windupTicks = -1;          // <0 = not winding up; >=0 = counting down
    private boolean wasInReachLastTick = false;

    public StoneWardenAttackGoal(StoneWardenEntity mob, double speed, boolean followIfNotSeen) {
        this.mob = mob;
        this.speed = speed;
        this.followIfNotSeen = followIfNotSeen;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        if (t == null || !t.isAlive()) return false;
        this.target = t;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) return false;
        if (!followIfNotSeen && !mob.getSensing().hasLineOfSight(target)) return false;
        return true;
    }

    @Override
    public void start() {
        ticksUntilNextAttempt = 0;
        windupTicks = -1;
        wasInReachLastTick = false;
    }

    @Override
    public void stop() {
        target = null;
        ticksUntilNextAttempt = 0;
        windupTicks = -1;
        wasInReachLastTick = false;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null) return;

        // Always look at the target
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // If not winding up, navigate toward the target
        if (windupTicks < 0) {
            mob.getNavigation().moveTo(target, speed);
        } else {
            // During windup: stop to "pose" and don't slide
            mob.getNavigation().stop();
            mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.5, 1.0, 0.5));
        }

        // Gate how often we try to (re)start an attack
        if (ticksUntilNextAttempt > 0) {
            ticksUntilNextAttempt--;
        }

        // Are we currently counting down to the delayed strike?
        if (windupTicks >= 0) {
            windupTicks--;
            if (windupTicks == 0) {
                // Land the hit if valid
                if (mob.level() instanceof ServerLevel server && target.isAlive()) {
                    double sq = mob.distanceToSqr(target);
                    if (sq <= attackReachSqr(target)) {
                        mob.doHurtTarget(server, target); // real damage occurs here ONLY
                    }
                }
                // small recovery to avoid instant re-windup spam
                ticksUntilNextAttempt = RECOVER_TICKS;
                windupTicks = -1;
            }
            return; // done for this tick
        }

        // If we're not winding up, check for reach and LOS to begin a new windup
        boolean inReach = mob.distanceToSqr(target) <= attackReachSqr(target);
        boolean canSee = mob.getSensing().hasLineOfSight(target);

        // Only start a windup if we're in reach, can see (or allowed to ignore), and off the local gate.
        if (ticksUntilNextAttempt == 0 && inReach && (canSee || followIfNotSeen)) {
            // Start the client animation immediately
            mob.startAttackWindupClientCue();

            // Begin the delayed strike
            windupTicks = DELAY_TICKS;

            // Prevent trying to restart immediately if target steps out/in of range next tick
            ticksUntilNextAttempt = DELAY_TICKS + RECOVER_TICKS;
        }

        wasInReachLastTick = inReach;
    }

    private double attackReachSqr(LivingEntity target) {
        // Close approximation of vanilla melee reach
        double reach = (double)(mob.getBbWidth() * 2.0F) + (double)target.getBbWidth();
        return reach * reach;
    }
}
