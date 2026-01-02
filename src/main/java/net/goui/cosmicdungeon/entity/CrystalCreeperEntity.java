package net.goui.cosmicdungeon.entity;

import net.goui.cosmicdungeon.entity.ai.goal.CrystalCreeperEatAmethystGoal;
import net.goui.cosmicdungeon.entity.ai.goal.CrystalCreeperSwellGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import javax.annotation.Nullable;

/**
 * Crystal Creeper:
 * - 4 growth stages (0–3) affecting size, speed, HP, and explosion radius.
 * - Eats amethyst-like items on ground to grow (handled by CrystalCreeperEatAmethystGoal).
 * - Creeper-style swelling + non-block-destroying crystal explosion.
 */
public class CrystalCreeperEntity extends Monster {

    // -----------------------------
    // Growth stages: 0–3
    // -----------------------------
    private static final float[] SIZE_BY_STAGE  = {0.5F, 0.85F, 1.25F, 1.5F};
    private static final float[] SPEED_BY_STAGE = {0.5F, 1.0F, 1.5F, 2.0F};
    private static final int[]   HP_BY_STAGE    = {6, 12, 18, 24};

    private static final double BASE_SPEED = 0.25D;

    // -----------------------------
    // Synced data
    // -----------------------------
    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(CrystalCreeperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(CrystalCreeperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_EATING =
            SynchedEntityData.defineId(CrystalCreeperEntity.class, EntityDataSerializers.BOOLEAN);

    // Client-side animation state (fed into render state)
    public final AnimationState eatAnimation = new AnimationState();
    public AnimationState getEatAnimationState() {
        return this.eatAnimation;
    }

    // Creeper-style swelling
    private int swell;
    private int swellDir;
    private int maxSwell = 30;

    // One-time server-side init flag
    private boolean initialized;

    // -----------------------------
    // Variants
    // -----------------------------
    public enum Variant {
        TEAL,
        BLUE,
        GREEN,
        ORANGE,
        PURPLE,
        RED;

        private static final Variant[] VALUES = values();

        public static Variant byId(int id) {
            if (id < 0 || id >= VALUES.length) {
                return TEAL;
            }
            return VALUES[id];
        }
    }

    public CrystalCreeperEntity(EntityType<? extends CrystalCreeperEntity> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    // ------------------------------------------------------------
    // Synced data
    // ------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAGE, 0);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_EATING, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        // When stage changes client-side, update bounding box
        if (key.equals(DATA_STAGE)) {
            this.refreshDimensions();
        }

        if (key.equals(DATA_EATING) && this.level().isClientSide()) {
            if (this.isEating()) {
                this.eatAnimation.startIfStopped(this.tickCount);
            } else {
                this.eatAnimation.stop();
            }
        }
    }

    // ------------------------------------------------------------
    // Attributes / dimensions / scaling
    // ------------------------------------------------------------

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HP_BY_STAGE[0])
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED * SPEED_BY_STAGE[0])
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        float scale = SIZE_BY_STAGE[this.getStage()];
        return super.getDefaultDimensions(pose).scale(scale);
    }

    public float getVisualScale() {
        return SIZE_BY_STAGE[this.getStage()];
    }

    public int getStage() {
        return this.entityData.get(DATA_STAGE);
    }

    public void setStage(int stage) {
        int clamped = Mth.clamp(stage, 0, 3);
        this.entityData.set(DATA_STAGE, clamped);
        // Server side: update size + attributes
        this.refreshDimensions();
        this.updateAttributesFromStage();
    }

    private void updateAttributesFromStage() {
        int stage = this.getStage();

        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(HP_BY_STAGE[stage]);
        }

        AttributeInstance move = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null) {
            move.setBaseValue(BASE_SPEED * SPEED_BY_STAGE[stage]);
        }

        // Heal to full on growth
        this.setHealth(this.getMaxHealth());
    }

    // ------------------------------------------------------------
    // Variant
    // ------------------------------------------------------------

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    private void randomizeVariant() {
        float f = this.random.nextFloat();
        if (f < 0.2F) {
            this.setVariant(Variant.BLUE);
        } else if (f < 0.4F) {
            this.setVariant(Variant.GREEN);
        } else if (f < 0.6F) {
            this.setVariant(Variant.ORANGE);
        } else if (f < 0.8F) {
            this.setVariant(Variant.PURPLE);
        } else if (f < 0.9F) {
            this.setVariant(Variant.RED);
        } else {
            this.setVariant(Variant.TEAL);
        }
    }

    // ------------------------------------------------------------
    // Eating / animations
    // ------------------------------------------------------------

    public boolean isEating() {
        return this.entityData.get(DATA_EATING);
    }

    public void setEating(boolean eating) {
        this.entityData.set(DATA_EATING, eating);

        // Drive client-side animation immediately when toggled locally
        if (this.level().isClientSide()) {
            if (eating) {
                this.eatAnimation.startIfStopped(this.tickCount);
            } else {
                this.eatAnimation.stop();
            }
        }
    }

    /**
     * Called by the EatAmethystGoal when the creeper finishes eating a bud/shard.
     */
    public void onEatAmethyst() {
        if (!this.level().isClientSide()) {
            int stage = this.getStage();
            if (stage < 3) {
                this.setStage(stage + 1);
            }
        }
    }

    // ------------------------------------------------------------
    // Goals
    // ------------------------------------------------------------

    @Override
    protected void registerGoals() {
        // Highest: eating amethyst
        this.goalSelector.addGoal(1, new CrystalCreeperEatAmethystGoal(this, 1.1D));

        // Swell/explode when close to target
        this.goalSelector.addGoal(2, new CrystalCreeperSwellGoal(this));

        // Chase target (movement towards player) — this is what was missing
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, false));

        // Standard wander / look
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // Targets: players, but only if stage >= 1
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return CrystalCreeperEntity.this.getStage() >= 1 && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return CrystalCreeperEntity.this.getStage() >= 1 && super.canContinueToUse();
            }
        });

        // Retaliate if hurt
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    // ------------------------------------------------------------
    // Tick / one-time init
    // ------------------------------------------------------------

    @Override
    public void tick() {
        // One-time server-side initialization (replaces onAddedToWorld())
        if (!this.level().isClientSide() && !this.initialized) {
            this.randomizeVariant();
            this.setStage(0);
            this.initialized = true;
        }

        if (this.level().isClientSide()) {
            // Keep eat animation ticking while flag is true
            if (this.isEating()) {
                this.eatAnimation.startIfStopped(this.tickCount);
            }
        }

        super.tick();

        if (!this.level().isClientSide() && this.isAlive()) {
            if (this.swellDir > 0 && this.swell == 0) {
                this.level().playSound(
                        null,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        SoundEvents.CREEPER_PRIMED,
                        SoundSource.HOSTILE,
                        1.0F,
                        0.5F
                );
            }

            this.swell += this.swellDir;
            if (this.swell < 0) {
                this.swell = 0;
            }

            if (this.swell >= this.maxSwell) {
                this.swell = this.maxSwell;
                this.doCrystalExplosion();
            }
        }
    }

    public int getSwell() {
        return this.swell;
    }

    public void setSwellDir(int dir) {
        this.swellDir = dir;
    }

    public int getMaxSwell() {
        return this.maxSwell;
    }

    // ------------------------------------------------------------
    // Explosion: damages mobs/players, NO blocks/items, no Crystal Creepers
    // ------------------------------------------------------------

    private double getExplosionRadius() {
        return switch (this.getStage()) {
            case 1 -> 8.0D;
            case 2 -> 10.0D;
            case 3 -> 12.0D;
            default -> 0.0D;
        };
    }

    private float getExplosionPower() {
        return switch (this.getStage()) {
            case 1 -> 1.0F;
            case 2 -> 1.5F;
            case 3 -> 2.0F;
            default -> 0.0F;
        };
    }

    private void doCrystalExplosion() {
        if (this.level().isClientSide()) {
            return;
        }

        float power = this.getExplosionPower();
        double radius = this.getExplosionRadius();
        if (power <= 0.0F || radius <= 0.0D) {
            this.discard();
            return;
        }

        AABB area = this.getBoundingBox().inflate(radius);

        this.level().getEntities(
                this,
                area,
                entity ->
                        entity.isAlive()
                                && !(entity instanceof CrystalCreeperEntity)
                                && !(entity instanceof ItemEntity)
                                && entity instanceof LivingEntity
        ).forEach(entity -> {
            LivingEntity target = (LivingEntity) entity;
            double dist = Math.sqrt(this.distanceToSqr(target));
            double scale = 1.0D - Mth.clamp(dist / radius, 0.0D, 1.0D);
            if (scale <= 0.0D) return;

            float baseDamage = 8.0F * power;
            float damage = (float) (baseDamage * scale);

            // Use standard explosion damage source; no damageTypes() call
            DamageSource source = this.damageSources().explosion(this, this);
            target.hurt(source, damage);

            // Knockback
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double norm = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
            target.push(
                    dx / norm * 0.5D * scale,
                    0.2D * scale,
                    dz / norm * 0.5D * scale
            );
        });

        // Cosmetic explosion sound + particles only
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(
                    ParticleTypes.EXPLOSION,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    1,
                    0.0D, 0.0D, 0.0D,
                    0.0D
            );
        }

        this.level().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                this.getSoundSource(),
                1.0F,
                0.9F + this.random.nextFloat() * 0.2F
        );

        this.discard();
    }

    // ------------------------------------------------------------
    // Explosion invulnerability logic
    // ------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        // Ignore OTHER Crystal Creeper explosions (or any explosion caused by another CrystalCreeper)
        if (source.is(DamageTypeTags.IS_EXPLOSION)
                && source.getEntity() instanceof CrystalCreeperEntity) {
            return true;
        }
        return super.isInvulnerableTo(level, source);
    }
}
