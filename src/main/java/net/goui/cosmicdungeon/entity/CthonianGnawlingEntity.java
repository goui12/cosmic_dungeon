package net.goui.cosmicdungeon.entity;

import net.goui.cosmicdungeon.entity.ai.goal.CthonianGnawlingLatchGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class CthonianGnawlingEntity extends Monster {
    private static final int DAMAGE_INTERVAL_TICKS = 40;
    private static final EntityDataAccessor<Boolean> DATA_LATCHED =
            SynchedEntityData.defineId(CthonianGnawlingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_LATCHED_TARGET_ID =
            SynchedEntityData.defineId(CthonianGnawlingEntity.class, EntityDataSerializers.INT);

    private int damageTickTimer = DAMAGE_INTERVAL_TICKS;
    private float clientCrawlAmount;
    public final AnimationState chompAnimation = new AnimationState();

    public CthonianGnawlingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new GnawlingMoveControl(this);
        this.setNoGravity(true);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LATCHED, false);
        builder.define(DATA_LATCHED_TARGET_ID, -1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new CthonianGnawlingLatchGoal(this));
        this.goalSelector.addGoal(5, new RandomPhaseWanderGoal(this));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        this.setNoGravity(true);

        super.tick();

        if (this.isLatched()) {
            maintainLatch();
            if (!this.level().isClientSide()) {
                tickLatchedDamage();
            }
        }

        Vec3 motion = this.getDeltaMovement();
        float targetCrawl = (float) Mth.clamp(motion.horizontalDistance() * 2.2D, 0.12D, 1.0D);
        if (this.isLatched()) targetCrawl = 0.9F;
        this.clientCrawlAmount = Mth.lerp(0.2F, this.clientCrawlAmount, targetCrawl);
    }

    public boolean isLatched() {
        return this.entityData.get(DATA_LATCHED);
    }

    public void latchTo(LivingEntity target) {
        if (!target.isAlive()) return;
        this.entityData.set(DATA_LATCHED_TARGET_ID, target.getId());
        this.damageTickTimer = DAMAGE_INTERVAL_TICKS;
        this.entityData.set(DATA_LATCHED, true);

        this.setDeltaMovement(Vec3.ZERO);
    }

    public void maintainLatch() {
        LivingEntity target = this.getLatchedTarget();
        if (target == null || !target.isAlive()) {
            this.releaseLatch();
            return;
        }

        float yawRad = target.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-net.minecraft.util.Mth.sin(yawRad), 0.0D, net.minecraft.util.Mth.cos(yawRad));
        Vec3 latchOffset = right.scale(0.35D).add(0.0D, target.getBbHeight() * 0.35D, 0.0D);
        Vec3 attachPos = target.position().add(latchOffset);
        this.setPos(attachPos.x, attachPos.y, attachPos.z);
        this.setDeltaMovement(Vec3.ZERO);

        this.setYRot(target.getYRot());
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private void tickLatchedDamage() {
        LivingEntity target = this.getLatchedTarget();
        if (target == null) {
            this.releaseLatch();
            return;
        }

        if (--this.damageTickTimer > 0) return;
        this.damageTickTimer = DAMAGE_INTERVAL_TICKS;

        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        boolean damaged = target.hurtServer(serverLevel, this.damageSources().mobAttack(this), 2.0F);
        if (damaged && this.random.nextBoolean()) {
            applyArmorGouge(target, 2);
        }
        this.level().broadcastEntityEvent(this, (byte) 61);
    }

    private void applyArmorGouge(LivingEntity target, int bonusDurabilityDamage) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isDamageableItem()) continue;
            stack.hurtAndBreak(bonusDurabilityDamage, target, slot);
        }
    }

    @Nullable
    private LivingEntity getLatchedTarget() {
        int targetId = this.entityData.get(DATA_LATCHED_TARGET_ID);
        if (targetId == -1) {
            return null;
        }
        if (!(this.level().getEntity(targetId) instanceof LivingEntity livingEntity)) {
            return null;
        }
        return livingEntity;
    }

    private void releaseLatch() {
        this.entityData.set(DATA_LATCHED_TARGET_ID, -1);
        this.entityData.set(DATA_LATCHED, false);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 61) {
            this.chompAnimation.start(this.tickCount);
            return;
        }
        super.handleEntityEvent(id);
    }

    public float getClientCrawlAmount(float partialTick) {
        return this.clientCrawlAmount;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        // No physical pushing while phasing.
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return super.hurtServer(level, damageSource, amount);
    }

    private static class GnawlingMoveControl extends MoveControl {
        private final CthonianGnawlingEntity gnawling;

        private GnawlingMoveControl(CthonianGnawlingEntity gnawling) {
            super(gnawling);
            this.gnawling = gnawling;
        }

        @Override
        public void tick() {
            if (this.operation != Operation.MOVE_TO) {
                this.gnawling.setDeltaMovement(this.gnawling.getDeltaMovement().scale(0.92D));
                return;
            }

            Vec3 vec3 = new Vec3(this.wantedX - this.gnawling.getX(), this.wantedY - this.gnawling.getY(), this.wantedZ - this.gnawling.getZ());
            double dist = vec3.length();
            if (dist < 0.05D) {
                this.operation = Operation.WAIT;
                this.gnawling.setDeltaMovement(this.gnawling.getDeltaMovement().scale(0.7D));
                return;
            }

            Vec3 accel = vec3.scale(this.speedModifier * 0.06D / dist);
            Vec3 nextMotion = this.gnawling.getDeltaMovement().add(accel).scale(0.96D);
            double maxSpeed = 0.45D;
            if (nextMotion.lengthSqr() > maxSpeed * maxSpeed) {
                nextMotion = nextMotion.normalize().scale(maxSpeed);
            }
            this.gnawling.setDeltaMovement(nextMotion);

            Vec3 motion = this.gnawling.getDeltaMovement();
            if (motion.horizontalDistanceSqr() > 1.0E-5D) {
                float yaw = (float) (Mth.atan2(motion.z, motion.x) * (180F / Math.PI)) - 90.0F;
                this.gnawling.setYRot(Mth.rotLerp(this.gnawling.getYRot(), yaw, 25.0F));
                this.gnawling.yBodyRot = this.gnawling.getYRot();
            }
        }
    }

    private static class RandomPhaseWanderGoal extends Goal {
        private final CthonianGnawlingEntity gnawling;

        private RandomPhaseWanderGoal(CthonianGnawlingEntity gnawling) {
            this.gnawling = gnawling;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !gnawling.isLatched() && gnawling.getTarget() == null && gnawling.getRandom().nextInt(8) == 0;
        }

        @Override
        public void start() {
            RandomSource random = gnawling.getRandom();
            Vec3 from = gnawling.position();
            Vec3 to = from.add(
                    random.nextIntBetweenInclusive(-8, 8),
                    random.nextIntBetweenInclusive(-2, 3),
                    random.nextIntBetweenInclusive(-8, 8)
            );
            gnawling.getMoveControl().setWantedPosition(to.x, to.y, to.z, 1.0D);
        }
    }
}
