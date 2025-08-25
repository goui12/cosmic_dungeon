package net.goui.cosmicdungeon.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public class MagmaGlobEntity extends Monster {

    // Animation flags used by the client model
    public final AnimationState walkAnimation = new AnimationState();
    public final AnimationState attackAnimation = new AnimationState();

    // Client-only: remember when we pulsed the attack event so we can stop next tick
    private int lastAttackEventTickClient = Integer.MIN_VALUE;

    public MagmaGlobEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    /* =======================
       Attributes
       ======================= */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ARMOR, 2.0);
    }

    /* =======================
       Goals / Targeting
       ======================= */
    @Override
    protected void registerGoals() {
        // Behavior
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // Targeting
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /* =======================
       Animation driving (client)
       ======================= */
    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // Tighter "moving" test: must be on ground AND actually navigating or has a move target
            boolean moving =
                    this.onGround()
                            && (this.getNavigation().isInProgress() || this.getMoveControl().hasWanted())
                            && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;

            if (moving) {
                this.walkAnimation.startIfStopped(this.tickCount);
            } else {
                this.walkAnimation.stop();
            }

            // Make the attack animation a 1-tick pulse so the model latches once, then plays out
            if (this.attackAnimation.isStarted() && this.tickCount > this.lastAttackEventTickClient) {
                this.attackAnimation.stop(); // stop on the frame after we started it
            }
        }
    }

    /* =======================
       Combat (server -> client cue)
       ======================= */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            // SERVER: tell clients to play the attack animation
            this.level().broadcastEntityEvent(this, (byte) 4);
        }
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            // CLIENT: start the attack animation (model latches for 1s)
            this.attackAnimation.start(this.tickCount);
            // remember we started it this tick; tick() will stop it next tick
            this.lastAttackEventTickClient = this.tickCount;
        } else {
            super.handleEntityEvent(id);
        }
    }
}
