package net.minecraft.world.entity.vehicle;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MinecartTNT extends AbstractMinecart {
    private static final byte EVENT_PRIME = 10;
    private static final String TAG_EXPLOSION_POWER = "explosion_power";
    private static final String TAG_EXPLOSION_SPEED_FACTOR = "explosion_speed_factor";
    private static final String TAG_FUSE = "fuse";
    private static final float DEFAULT_EXPLOSION_POWER_BASE = 4.0F;
    private static final float DEFAULT_EXPLOSION_SPEED_FACTOR = 1.0F;
    private static final int NO_FUSE = -1;
    @Nullable
    private DamageSource ignitionSource;
    private int fuse = -1;
    private float explosionPowerBase = 4.0F;
    private float explosionSpeedFactor = 1.0F;

    public MinecartTNT(EntityType<? extends MinecartTNT> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.TNT.defaultBlockState();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.fuse > 0) {
            this.fuse--;
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
        } else if (this.fuse == 0) {
            this.explode(this.ignitionSource, this.getDeltaMovement().horizontalDistanceSqr());
        }

        if (this.horizontalCollision) {
            double d0 = this.getDeltaMovement().horizontalDistanceSqr();
            if (d0 >= 0.01F) {
                this.explode(this.ignitionSource, d0);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (damageSource.getDirectEntity() instanceof AbstractArrow abstractarrow && abstractarrow.isOnFire()) {
            DamageSource damagesource = this.damageSources().explosion(this, damageSource.getEntity());
            this.explode(damagesource, abstractarrow.getDeltaMovement().lengthSqr());
        }

        return super.hurtServer(level, damageSource, amount);
    }

    @Override
    public void destroy(ServerLevel level, DamageSource damageSource) {
        double d0 = this.getDeltaMovement().horizontalDistanceSqr();
        if (!damageSourceIgnitesTnt(damageSource) && !(d0 >= 0.01F)) {
            this.destroy(level, this.getDropItem());
        } else {
            if (this.fuse < 0) {
                this.primeFuse(damageSource);
                this.fuse = this.random.nextInt(20) + this.random.nextInt(20);
            }
        }
    }

    @Override
    protected Item getDropItem() {
        return Items.TNT_MINECART;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.TNT_MINECART);
    }

    protected void explode(@Nullable DamageSource damageSource, double radiusModifier) {
        if (this.level() instanceof ServerLevel serverlevel) {
            if (serverlevel.getGameRules().getBoolean(GameRules.RULE_TNT_EXPLODES)) {
                double d0 = Math.min(Math.sqrt(radiusModifier), 5.0);
                serverlevel.explode(
                    this,
                    damageSource,
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    (float)(this.explosionPowerBase + this.explosionSpeedFactor * this.random.nextDouble() * 1.5 * d0),
                    false,
                    Level.ExplosionInteraction.TNT
                );
                this.discard();
            } else if (this.isPrimed()) {
                this.discard();
            }
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (fallDistance >= 3.0) {
            double d0 = fallDistance / 10.0;
            this.explode(this.ignitionSource, d0 * d0);
        }

        return super.causeFallDamage(fallDistance, damageMultiplier, damageSource);
    }

    /**
     * Called every tick the minecart is on an activator rail.
     */
    @Override
    public void activateMinecart(int x, int y, int z, boolean receivingPower) {
        if (receivingPower && this.fuse < 0) {
            this.primeFuse(null);
        }
    }

    /**
     * Handles an entity event received from a {@link net.minecraft.network.protocol.game.ClientboundEntityEventPacket}.
     */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 10) {
            this.primeFuse(null);
        } else {
            super.handleEntityEvent(id);
        }
    }

    public void primeFuse(@Nullable DamageSource damageSource) {
        if (!(this.level() instanceof ServerLevel serverlevel && !serverlevel.getGameRules().getBoolean(GameRules.RULE_TNT_EXPLODES))) {
            this.fuse = 80;
            if (!this.level().isClientSide()) {
                if (damageSource != null && this.ignitionSource == null) {
                    this.ignitionSource = this.damageSources().explosion(this, damageSource.getEntity());
                }

                this.level().broadcastEntityEvent(this, (byte)10);
                if (!this.isSilent()) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }
    }

    public int getFuse() {
        return this.fuse;
    }

    public boolean isPrimed() {
        return this.fuse > -1;
    }

    /**
     * Explosion resistance of a block relative to this entity
     */
    @Override
    public float getBlockExplosionResistance(
        Explosion explosion, BlockGetter level, BlockPos pos, BlockState blockState, FluidState fluidState, float explosionPower
    ) {
        return !this.isPrimed() || !blockState.is(BlockTags.RAILS) && !level.getBlockState(pos.above()).is(BlockTags.RAILS)
            ? super.getBlockExplosionResistance(explosion, level, pos, blockState, fluidState, explosionPower)
            : 0.0F;
    }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState blockState, float explosionPower) {
        return !this.isPrimed() || !blockState.is(BlockTags.RAILS) && !level.getBlockState(pos.above()).is(BlockTags.RAILS)
            ? super.shouldBlockExplode(explosion, level, pos, blockState, explosionPower)
            : false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.fuse = input.getIntOr("fuse", -1);
        this.explosionPowerBase = Mth.clamp(input.getFloatOr("explosion_power", 4.0F), 0.0F, 128.0F);
        this.explosionSpeedFactor = Mth.clamp(input.getFloatOr("explosion_speed_factor", 1.0F), 0.0F, 128.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("fuse", this.fuse);
        if (this.explosionPowerBase != 4.0F) {
            output.putFloat("explosion_power", this.explosionPowerBase);
        }

        if (this.explosionSpeedFactor != 1.0F) {
            output.putFloat("explosion_speed_factor", this.explosionSpeedFactor);
        }
    }

    @Override
    boolean shouldSourceDestroy(DamageSource source) {
        return damageSourceIgnitesTnt(source);
    }

    private static boolean damageSourceIgnitesTnt(DamageSource source) {
        return source.getDirectEntity() instanceof Projectile projectile
            ? projectile.isOnFire()
            : source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION);
    }
}
