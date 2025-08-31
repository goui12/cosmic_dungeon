package net.goui.cosmicdungeon.entity;

import net.goui.cosmicdungeon.entity.ai.goal.GoblinAmbusherAttackGoal;
import net.goui.cosmicdungeon.sound.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class GoblinAmbusherEntity extends Monster {

    /** Renderer handles model forward; keep this 0 so rotations don't stack. */
    private static final float MODEL_FORWARD_OFFSET_DEG = 0.0F;

    public final AnimationState walkLoop = new AnimationState();
    public final AnimationState attackAnimation = new AnimationState();

    private int attackAnimEndClient = -1;
    private int attackSfxTickClient = -1;

    private int faceTargetTicksServer = 0;

    public GoblinAmbusherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.ARMOR, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GoblinAmbusherAttackGoal(this, 1.6, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 12.0f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.player.Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.faceTargetTicksServer > 0 && this.getTarget() != null) {
            faceTargetInstant(this.getTarget().getX(), this.getTarget().getZ());
            this.faceTargetTicksServer--;
        }

        if (this.level().isClientSide) {
            boolean moving = (this.getNavigation().isInProgress() || this.getMoveControl().hasWanted())
                    && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
            if (moving) this.walkLoop.startIfStopped(this.tickCount);
            else this.walkLoop.stop();

            if (this.attackAnimation.isStarted() && this.getTarget() != null) {
                faceTargetInstant(this.getTarget().getX(), this.getTarget().getZ());
            }

            if (this.attackSfxTickClient != -1 && this.tickCount >= this.attackSfxTickClient) {
                this.attackSfxTickClient = -1;
                SoundEvent blowdart = ModSounds.BLOWDART.get();
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                        blowdart, SoundSource.HOSTILE, 0.9F, 1.0F + this.getRandom().nextFloat() * 0.2F, false);
            }

            if (this.attackAnimEndClient != -1 && this.tickCount >= this.attackAnimEndClient) {
                this.attackAnimation.stop();
                this.attackAnimEndClient = -1;
            }
        }
    }

    public void startAttackWindupClientCue() {
        if (!this.level().isClientSide) {
            this.faceTargetTicksServer = 20; // 1s
        }
        this.level().broadcastEntityEvent(this, (byte) 5);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 5) {
            if (this.level().isClientSide) {
                this.attackAnimation.start(this.tickCount);
                this.attackAnimEndClient = this.tickCount + 20;
                this.attackSfxTickClient = this.tickCount + 10;
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    protected float getEyeHeight(Pose pose, EntityDimensions size) { return 1.05F; }
    protected float getStandingEyeHeight(Pose pose, EntityDimensions size) { return 1.05F; }

    /** Instantly rotate body + head to face a world X/Z and also update 'old' fields to stop interpolation. */
    private void faceTargetInstant(double targetX, double targetZ) {
        double dx = targetX - this.getX();
        double dz = targetZ - this.getZ();
        float yaw = (float)(Mth.atan2(dz, dx) * (180.0F / Math.PI)) + MODEL_FORWARD_OFFSET_DEG;

        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;

        this.yRotO = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRotO = yaw;
    }
}
