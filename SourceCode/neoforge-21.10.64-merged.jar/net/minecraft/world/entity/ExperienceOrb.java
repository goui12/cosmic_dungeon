package net.minecraft.world.entity;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ExperienceOrb extends Entity {
    protected static final EntityDataAccessor<Integer> DATA_VALUE = SynchedEntityData.defineId(ExperienceOrb.class, EntityDataSerializers.INT);
    private static final int LIFETIME = 6000;
    private static final int ENTITY_SCAN_PERIOD = 20;
    private static final int MAX_FOLLOW_DIST = 8;
    private static final int ORB_GROUPS_PER_AREA = 40;
    private static final double ORB_MERGE_DISTANCE = 0.5;
    private static final short DEFAULT_HEALTH = 5;
    private static final short DEFAULT_AGE = 0;
    private static final short DEFAULT_VALUE = 0;
    private static final int DEFAULT_COUNT = 1;
    private int age = 0;
    private int health = 5;
    private int count = 1;
    @Nullable
    private Player followingPlayer;
    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    public ExperienceOrb(Level level, double x, double y, double z, int value) {
        this(level, new Vec3(x, y, z), Vec3.ZERO, value);
    }

    public ExperienceOrb(Level level, Vec3 pos, Vec3 direction, int value) {
        this(EntityType.EXPERIENCE_ORB, level);
        this.setPos(pos);
        if (!level.isClientSide()) {
            this.setYRot(this.random.nextFloat() * 360.0F);
            Vec3 vec3 = new Vec3(
                (this.random.nextDouble() * 0.2 - 0.1) * 2.0, this.random.nextDouble() * 0.2 * 2.0, (this.random.nextDouble() * 0.2 - 0.1) * 2.0
            );
            if (direction.lengthSqr() > 0.0 && direction.dot(vec3) < 0.0) {
                vec3 = vec3.scale(-1.0);
            }

            double d0 = this.getBoundingBox().getSize();
            this.setPos(pos.add(direction.normalize().scale(d0 * 0.5)));
            this.setDeltaMovement(vec3);
            if (!level.noCollision(this.getBoundingBox())) {
                this.unstuckIfPossible(d0);
            }
        }

        this.setValue(value);
    }

    public ExperienceOrb(EntityType<? extends ExperienceOrb> entityType, Level level) {
        super(entityType, level);
    }

    protected void unstuckIfPossible(double size) {
        Vec3 vec3 = this.position().add(0.0, this.getBbHeight() / 2.0, 0.0);
        VoxelShape voxelshape = Shapes.create(AABB.ofSize(vec3, size, size, size));
        this.level()
            .findFreePosition(this, voxelshape, vec3, this.getBbWidth(), this.getBbHeight(), this.getBbWidth())
            .ifPresent(p_426977_ -> this.setPos(p_426977_.add(0.0, -this.getBbHeight() / 2.0, 0.0)));
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VALUE, 0);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        this.interpolation.interpolate();
        if (this.firstTick && this.level().isClientSide()) {
            this.firstTick = false;
        } else {
            super.tick();
            boolean flag = !this.level().noCollision(this.getBoundingBox());
            if (this.isEyeInFluid(FluidTags.WATER)) {
                this.setUnderwaterMovement();
            } else if (!flag) {
                this.applyGravity();
            }

            if (this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA)) {
                this.setDeltaMovement(
                    (this.random.nextFloat() - this.random.nextFloat()) * 0.2F, 0.2F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
                );
            }

            if (this.tickCount % 20 == 1) {
                this.scanForMerges();
            }

            this.followNearbyPlayer();
            if (this.followingPlayer == null && !this.level().isClientSide() && flag) {
                boolean flag1 = !this.level().noCollision(this.getBoundingBox().move(this.getDeltaMovement()));
                if (flag1) {
                    this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
                    this.hasImpulse = true;
                }
            }

            double d0 = this.getDeltaMovement().y;
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.applyEffectsFromBlocks();
            float f = 0.98F;
            if (this.onGround()) {
                BlockPos pos = this.getBlockPosBelowThatAffectsMyMovement();
                f = this.level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98F;
            }

            this.setDeltaMovement(this.getDeltaMovement().scale(f));
            if (this.verticalCollisionBelow && d0 < -this.getGravity()) {
                this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, -d0 * 0.4, this.getDeltaMovement().z));
            }

            this.age++;
            if (this.age >= 6000) {
                this.discard();
            }
        }
    }

    private void followNearbyPlayer() {
        if (this.followingPlayer == null || this.followingPlayer.isSpectator() || this.followingPlayer.distanceToSqr(this) > 64.0) {
            Player player = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.entity.XpOrbTargetingEvent(this, 8.0)).getFollowingPlayer();
            if (player != null && !player.isSpectator() && !player.isDeadOrDying()) {
                this.followingPlayer = player;
            } else {
                this.followingPlayer = null;
            }
        }

        if (this.followingPlayer != null) {
            Vec3 vec3 = new Vec3(
                this.followingPlayer.getX() - this.getX(),
                this.followingPlayer.getY() + this.followingPlayer.getEyeHeight() / 2.0 - this.getY(),
                this.followingPlayer.getZ() - this.getZ()
            );
            double d0 = vec3.lengthSqr();
            double d1 = 1.0 - Math.sqrt(d0) / 8.0;
            this.setDeltaMovement(this.getDeltaMovement().add(vec3.normalize().scale(d1 * d1 * 0.1)));
        }
    }

    @Override
    public BlockPos getBlockPosBelowThatAffectsMyMovement() {
        return this.getOnPos(0.999999F);
    }

    private void scanForMerges() {
        if (this.level() instanceof ServerLevel) {
            for (ExperienceOrb experienceorb : this.level()
                .getEntities(EntityTypeTest.forClass(ExperienceOrb.class), this.getBoundingBox().inflate(0.5), this::canMerge)) {
                this.merge(experienceorb);
            }
        }
    }

    public static void award(ServerLevel level, Vec3 pos, int amount) {
        awardWithDirection(level, pos, Vec3.ZERO, amount);
    }

    public static void awardWithDirection(ServerLevel level, Vec3 pos, Vec3 direction, int amount) {
        while (amount > 0) {
            int i = getExperienceValue(amount);
            amount -= i;
            if (!tryMergeToExisting(level, pos, i)) {
                level.addFreshEntity(new ExperienceOrb(level, pos, direction, i));
            }
        }
    }

    private static boolean tryMergeToExisting(ServerLevel level, Vec3 pos, int amount) {
        AABB aabb = AABB.ofSize(pos, 1.0, 1.0, 1.0);
        int i = level.getRandom().nextInt(40);
        List<ExperienceOrb> list = level.getEntities(EntityTypeTest.forClass(ExperienceOrb.class), aabb, p_147081_ -> canMerge(p_147081_, i, amount));
        if (!list.isEmpty()) {
            ExperienceOrb experienceorb = list.get(0);
            experienceorb.count++;
            experienceorb.age = 0;
            return true;
        } else {
            return false;
        }
    }

    private boolean canMerge(ExperienceOrb orb) {
        return orb != this && canMerge(orb, this.getId(), this.getValue());
    }

    private static boolean canMerge(ExperienceOrb orb, int amount, int other) {
        return !orb.isRemoved() && (orb.getId() - amount) % 40 == 0 && orb.getValue() == other;
    }

    private void merge(ExperienceOrb orb) {
        this.count = this.count + orb.count;
        this.age = Math.min(this.age, orb.age);
        orb.discard();
    }

    private void setUnderwaterMovement() {
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(vec3.x * 0.99F, Math.min(vec3.y + 5.0E-4F, 0.06F), vec3.z * 0.99F);
    }

    @Override
    protected void doWaterSplashEffect() {
    }

    @Override
    public final boolean hurtClient(DamageSource damageSource) {
        return !this.isInvulnerableToBase(damageSource);
    }

    @Override
    public final boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (this.isInvulnerableToBase(damageSource)) {
            return false;
        } else {
            this.markHurt();
            this.health = (int)(this.health - amount);
            if (this.health <= 0) {
                this.discard();
            }

            return true;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putShort("Health", (short)this.health);
        output.putShort("Age", (short)this.age);
        output.putShort("Value", (short)this.getValue());
        output.putInt("Count", this.count);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.health = input.getShortOr("Health", (short)5);
        this.age = input.getShortOr("Age", (short)0);
        this.setValue(input.getShortOr("Value", (short)0));
        this.count = input.read("Count", ExtraCodecs.POSITIVE_INT).orElse(1);
    }

    /**
     * Called by a player entity when they collide with an entity
     */
    @Override
    public void playerTouch(Player entity) {
        if (entity instanceof ServerPlayer serverplayer) {
            if (entity.takeXpDelay == 0) {
                if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerXpEvent.PickupXp(entity, this)).isCanceled()) return;
                entity.takeXpDelay = 2;
                entity.take(this, 1);
                int i = this.repairPlayerItems(serverplayer, this.getValue());
                if (i > 0) {
                    entity.giveExperiencePoints(i);
                }

                this.count--;
                if (this.count == 0) {
                    this.discard();
                }
            }
        }
    }

    private int repairPlayerItems(ServerPlayer player, int value) {
        Optional<EnchantedItemInUse> optional = EnchantmentHelper.getRandomItemWith(EnchantmentEffectComponents.REPAIR_WITH_XP, player, ItemStack::isDamaged);
        if (optional.isPresent()) {
            ItemStack itemstack = optional.get().itemStack();
            int i = EnchantmentHelper.modifyDurabilityToRepairFromXp(player.level(), itemstack, (int) (value * itemstack.getXpRepairRatio()));
            int j = Math.min(i, itemstack.getDamageValue());
            itemstack.setDamageValue(itemstack.getDamageValue() - j);
            if (j > 0) {
                int k = value - j * value / i;
                if (k > 0) {
                    return this.repairPlayerItems(player, k);
                }
            }

            return 0;
        } else {
            return value;
        }
    }

    public int getValue() {
        return this.entityData.get(DATA_VALUE);
    }

    public void setValue(int value) {
        this.entityData.set(DATA_VALUE, value);
    }

    public int getIcon() {
        int i = this.getValue();
        if (i >= 2477) {
            return 10;
        } else if (i >= 1237) {
            return 9;
        } else if (i >= 617) {
            return 8;
        } else if (i >= 307) {
            return 7;
        } else if (i >= 149) {
            return 6;
        } else if (i >= 73) {
            return 5;
        } else if (i >= 37) {
            return 4;
        } else if (i >= 17) {
            return 3;
        } else if (i >= 7) {
            return 2;
        } else {
            return i >= 3 ? 1 : 0;
        }
    }

    /**
     * Get a fragment of the maximum experience points value for the supplied value of experience points value.
     */
    public static int getExperienceValue(int expValue) {
        if (expValue >= 2477) {
            return 2477;
        } else if (expValue >= 1237) {
            return 1237;
        } else if (expValue >= 617) {
            return 617;
        } else if (expValue >= 307) {
            return 307;
        } else if (expValue >= 149) {
            return 149;
        } else if (expValue >= 73) {
            return 73;
        } else if (expValue >= 37) {
            return 37;
        } else if (expValue >= 17) {
            return 17;
        } else if (expValue >= 7) {
            return 7;
        } else {
            return expValue >= 3 ? 3 : 1;
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.AMBIENT;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }
}
