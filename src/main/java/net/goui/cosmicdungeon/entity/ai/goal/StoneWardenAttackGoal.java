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
 * 3) Waits DELAY_TICKS (~2.5s) before actually applying damage.
 * 4) Applies damage only if the target is still alive & still in melee reach.
 */
public class StoneWardenAttackGoal extends Goal {
    private static final int DELAY_TICKS = 50;      // 2.5s @ 20 TPS
    private static final int RECOVER_TICKS = 10;    // short recovery after hit

    private final StoneWardenEntity mob;
    private final double speed;
    private final boolean followIfNotSeen;

    private LivingEntity target;
    private int ticksUntilNextAttempt = 0; // generic gate so we don't spam the windup
    private int windupTicks = -1;          // <0 = not winding up; >=0 = counting down

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
    }

    @Override
    public void stop() {
        target = null;
        ticksUntilNextAttempt = 0;
        windupTicks = -1;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null) return;

        // Always look at the target
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Movement vs. windup pose
        if (windupTicks < 0) {
            mob.getNavigation().moveTo(target, speed);
        } else {
            mob.getNavigation().stop();
            mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.5D, 1.0D, 0.5D));
        }

        if (ticksUntilNextAttempt > 0) {
            ticksUntilNextAttempt--;
        }

        // Already winding up: countdown to the delayed strike
        if (windupTicks >= 0) {
            windupTicks--;
            if (windupTicks == 0) {
                if (mob.level() instanceof ServerLevel server && target.isAlive()) {
                    double sq = mob.distanceToSqr(target);
                    if (sq <= attackReachSqr(target)) {
                        // Real damage happens here, once.
                        mob.doHurtTarget(server, target);
                    }
                }
                ticksUntilNextAttempt = RECOVER_TICKS;
                windupTicks = -1;
            }
            return;
        }

        // Not winding up: consider starting a new attack
        boolean inReach = mob.distanceToSqr(target) <= attackReachSqr(target);
        boolean canSee = mob.getSensing().hasLineOfSight(target);

        if (ticksUntilNextAttempt == 0 && inReach && (canSee || followIfNotSeen)) {
            // Trigger client animation right away
            mob.startAttackWindupClientCue();

            // Begin the delayed strike
            windupTicks = DELAY_TICKS;

            // Gate re-entry until after windup + recovery
            ticksUntilNextAttempt = DELAY_TICKS + RECOVER_TICKS;
        }
    }

    private double attackReachSqr(LivingEntity target) {
        // Close approximation of vanilla melee reach
        double reach = (double) (mob.getBbWidth() * 2.0F) + (double) target.getBbWidth();
        return reach * reach;
    }
}
