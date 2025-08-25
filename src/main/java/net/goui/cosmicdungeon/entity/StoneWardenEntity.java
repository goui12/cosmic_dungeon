// StoneWardenEntity.java
package net.goui.cosmicdungeon.entity;

import net.goui.cosmicdungeon.entity.ai.goal.StoneWardenAttackGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class StoneWardenEntity extends Monster {

    // === Client animation flags ===
    public final AnimationState walkAnimation = new AnimationState();
    public final AnimationState attackAnimation = new AnimationState();

    // Client-only: let the long attack animation play fully
    private int attackAnimEndClient = -1; // tick when we stop the anim

    public StoneWardenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 50;
        this.setPersistenceRequired();
    }

    /* =======================
       Attributes
       ======================= */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE, 20.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    /* =======================
       Goals / Targeting
       ======================= */
    @Override
    protected void registerGoals() {
        // REPLACEMENT: use our custom delayed‑hit goal instead of vanilla MeleeAttackGoal
        this.goalSelector.addGoal(2, new StoneWardenAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /* =======================
       Tick
       ======================= */
    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // Drive walk cycle by movement
            boolean moving =
                    this.onGround()
                            && (this.getNavigation().isInProgress() || this.getMoveControl().hasWanted())
                            && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;

            if (moving) {
                this.walkAnimation.startIfStopped(this.tickCount);
            } else {
                this.walkAnimation.stop();
            }

            // Keep the 3.0s attack animation playing to completion
            if (attackAnimEndClient != -1 && this.tickCount >= attackAnimEndClient) {
                this.attackAnimation.stop();
                attackAnimEndClient = -1;
            }
        }
    }

    /* =======================
       Combat cue (server → client)
       ======================= */

    /** Called by the custom goal to begin the wind‑up animation on clients immediately. */
    public void startAttackWindupClientCue() {
        this.level().broadcastEntityEvent(this, (byte) 4);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) { // attack animation cue
            this.attackAnimation.start(this.tickCount);
            // Your attack animation is ~3.0s => stop after 60 ticks
            this.attackAnimEndClient = this.tickCount + 60;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /* NOTE:
     * We do NOT override doHurtTarget here anymore.
     * The custom goal performs the real damage at the delayed moment.
     */
}
