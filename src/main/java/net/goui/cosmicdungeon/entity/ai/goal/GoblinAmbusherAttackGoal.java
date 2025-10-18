package net.goui.cosmicdungeon.entity.ai.goal;

import net.goui.cosmicdungeon.entity.GoblinAmbusherEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class GoblinAmbusherAttackGoal extends Goal {
    // Tuning
    private static final double MAX_SHOOT_RANGE = 16.0;
    private static final double MAX_SHOOT_RANGE_SQR = MAX_SHOOT_RANGE * MAX_SHOOT_RANGE;
    private static final double RUN_SPEED_MULT = 1.6; // sprint while closing

    private static final int WINDUP_TICKS = 6;   // ~0.3s windup @20 TPS
    private static final int RECOVER_TICKS = 8;  // short recovery/cooldown

    private final GoblinAmbusherEntity mob;
    private final double baseSpeed;          // passed from constructor (e.g., 1.25)
    private final boolean followIfNotSeen;   // keep pursuing without LoS, but only shoot with LoS

    private LivingEntity target;
    private int cooldownTicks = 0;
    private int windupTicks = -1;

    public GoblinAmbusherAttackGoal(GoblinAmbusherEntity mob, double speed, boolean followIfNotSeen) {
        this.mob = mob;
        this.baseSpeed = speed;
        this.followIfNotSeen = followIfNotSeen;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        cooldownTicks = 0;
        windupTicks = -1;
    }

    @Override
    public void stop() {
        target = null;
        cooldownTicks = 0;
        windupTicks = -1;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null) return;

        final double distSq = mob.distanceToSqr(target);
        final boolean canSee = mob.getSensing().hasLineOfSight(target);

        // While winding up or in short post-shot cooldown, snap the look super fast so he visually tracks the player
        final boolean snapLook = (windupTicks >= 0) || (cooldownTicks > 0 && distSq <= MAX_SHOOT_RANGE_SQR);
        float lookSpeed = snapLook ? 180.0F : 60.0F;
        mob.getLookControl().setLookAt(target, lookSpeed, lookSpeed);

        if (cooldownTicks > 0) cooldownTicks--;

        // If we're currently winding up the shot, hold position and fire when timer elapses.
        if (windupTicks >= 0) {
            mob.getNavigation().stop();
            // slight brake to prevent sliding
            mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.4, 1.0, 0.4));

            if (--windupTicks == 0) {
                // Fire on the server if still valid: in range and (re)has line-of-sight.
                if (!mob.level().isClientSide() && target.isAlive()
                        && distSq <= MAX_SHOOT_RANGE_SQR && mob.getSensing().hasLineOfSight(target)) {

                    float dmg = (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    // Hitscan "dart" damage; swap for a real projectile later if you add one.
                    target.hurt(mob.damageSources().mobAttack(mob), dmg);
                }
                cooldownTicks = RECOVER_TICKS;
                windupTicks = -1;
            }
            return;
        }

        // Not winding up: decide to chase or shoot
        boolean shouldChase = distSq > MAX_SHOOT_RANGE_SQR || (!canSee && !followIfNotSeen);

        if (shouldChase) {
            // Sprint into position
            mob.getNavigation().moveTo(target, baseSpeed * RUN_SPEED_MULT);
            return;
        }

        // We're in range (<=16) and either can see or we're allowed to follow when not seen.
        // Only shoot with LoS; if no LoS, keep inching until we get it.
        if (!canSee) {
            mob.getNavigation().moveTo(target, baseSpeed * RUN_SPEED_MULT);
            return;
        }

        // In range & LoS: stop to take the shot (respect cooldown).
        mob.getNavigation().stop();
        if (cooldownTicks == 0) {
            mob.startAttackWindupClientCue(); // plays the shoot animation client-side
            windupTicks = WINDUP_TICKS;
            cooldownTicks = WINDUP_TICKS + RECOVER_TICKS;
        }
    }
}
