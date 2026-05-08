package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ConduitBlockEntity extends BlockEntity {
    private static final int BLOCK_REFRESH_RATE = 2;
    private static final int EFFECT_DURATION = 13;
    private static final float ROTATION_SPEED = -0.0375F;
    private static final int MIN_ACTIVE_SIZE = 16;
    private static final int MIN_KILL_SIZE = 42;
    private static final int KILL_RANGE = 8;
    private static final Block[] VALID_BLOCKS = new Block[]{Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.DARK_PRISMARINE};
    public int tickCount;
    private float activeRotation;
    private boolean isActive;
    private boolean isHunting;
    private final List<BlockPos> effectBlocks = Lists.newArrayList();
    @Nullable
    private EntityReference<LivingEntity> destroyTarget;
    private long nextAmbientSoundActivation;

    public ConduitBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityType.CONDUIT, pos, blockState);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.destroyTarget = EntityReference.read(input, "Target");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        EntityReference.store(this.destroyTarget, output, "Target");
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ConduitBlockEntity blockEntity) {
        blockEntity.tickCount++;
        long i = level.getGameTime();
        List<BlockPos> list = blockEntity.effectBlocks;
        if (i % 40L == 0L) {
            blockEntity.isActive = updateShape(level, pos, list);
            updateHunting(blockEntity, list);
        }

        LivingEntity livingentity = EntityReference.getLivingEntity(blockEntity.destroyTarget, level);
        animationTick(level, pos, list, livingentity, blockEntity.tickCount);
        if (blockEntity.isActive()) {
            blockEntity.activeRotation++;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ConduitBlockEntity blockEntity) {
        blockEntity.tickCount++;
        long i = level.getGameTime();
        List<BlockPos> list = blockEntity.effectBlocks;
        if (i % 40L == 0L) {
            boolean flag = updateShape(level, pos, list);
            if (flag != blockEntity.isActive) {
                SoundEvent soundevent = flag ? SoundEvents.CONDUIT_ACTIVATE : SoundEvents.CONDUIT_DEACTIVATE;
                level.playSound(null, pos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            blockEntity.isActive = flag;
            updateHunting(blockEntity, list);
            if (flag) {
                applyEffects(level, pos, list);
                updateAndAttackTarget((ServerLevel)level, pos, state, blockEntity, list.size() >= 42);
            }
        }

        if (blockEntity.isActive()) {
            if (i % 80L == 0L) {
                level.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            if (i > blockEntity.nextAmbientSoundActivation) {
                blockEntity.nextAmbientSoundActivation = i + 60L + level.getRandom().nextInt(40);
                level.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }

    private static void updateHunting(ConduitBlockEntity blockEntity, List<BlockPos> positions) {
        blockEntity.setHunting(positions.size() >= 42);
    }

    private static boolean updateShape(Level level, BlockPos pos, List<BlockPos> positions) {
        positions.clear();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    BlockPos blockpos = pos.offset(i, j, k);
                    if (!level.isWaterAt(blockpos)) {
                        return false;
                    }
                }
            }
        }

        for (int j1 = -2; j1 <= 2; j1++) {
            for (int k1 = -2; k1 <= 2; k1++) {
                for (int l1 = -2; l1 <= 2; l1++) {
                    int i2 = Math.abs(j1);
                    int l = Math.abs(k1);
                    int i1 = Math.abs(l1);
                    if ((i2 > 1 || l > 1 || i1 > 1) && (j1 == 0 && (l == 2 || i1 == 2) || k1 == 0 && (i2 == 2 || i1 == 2) || l1 == 0 && (i2 == 2 || l == 2))) {
                        BlockPos blockpos1 = pos.offset(j1, k1, l1);
                        BlockState blockstate = level.getBlockState(blockpos1);

                        if (blockstate.isConduitFrame(level, blockpos1, pos)) {
                            positions.add(blockpos1);
                        }
                    }
                }
            }
        }

        return positions.size() >= 16;
    }

    private static void applyEffects(Level level, BlockPos pos, List<BlockPos> positions) {
        int i = positions.size();
        int j = i / 7 * 16;
        int k = pos.getX();
        int l = pos.getY();
        int i1 = pos.getZ();
        AABB aabb = new AABB(k, l, i1, k + 1, l + 1, i1 + 1).inflate(j).expandTowards(0.0, level.getHeight(), 0.0);
        List<Player> list = level.getEntitiesOfClass(Player.class, aabb);
        if (!list.isEmpty()) {
            for (Player player : list) {
                if (pos.closerThan(player.blockPosition(), j) && player.isInWaterOrRain()) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260, 0, true, true));
                }
            }
        }
    }

    private static void updateAndAttackTarget(ServerLevel level, BlockPos pos, BlockState state, ConduitBlockEntity blockEntity, boolean canDestroy) {
        EntityReference<LivingEntity> entityreference = updateDestroyTarget(blockEntity.destroyTarget, level, pos, canDestroy);
        LivingEntity livingentity = EntityReference.getLivingEntity(entityreference, level);
        if (livingentity != null) {
            level.playSound(
                null, livingentity.getX(), livingentity.getY(), livingentity.getZ(), SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.BLOCKS, 1.0F, 1.0F
            );
            livingentity.hurtServer(level, level.damageSources().magic(), 4.0F);
        }

        if (!Objects.equals(entityreference, blockEntity.destroyTarget)) {
            blockEntity.destroyTarget = entityreference;
            level.sendBlockUpdated(pos, state, state, 2);
        }
    }

    @Nullable
    private static EntityReference<LivingEntity> updateDestroyTarget(
        @Nullable EntityReference<LivingEntity> destroyTarget, ServerLevel level, BlockPos pos, boolean canDestroy
    ) {
        if (!canDestroy) {
            return null;
        } else if (destroyTarget == null) {
            return selectNewTarget(level, pos);
        } else {
            LivingEntity livingentity = EntityReference.getLivingEntity(destroyTarget, level);
            return livingentity != null && livingentity.isAlive() && pos.closerThan(livingentity.blockPosition(), 8.0) ? destroyTarget : null;
        }
    }

    @Nullable
    private static EntityReference<LivingEntity> selectNewTarget(ServerLevel level, BlockPos pos) {
        List<LivingEntity> list = level.getEntitiesOfClass(
            LivingEntity.class, getDestroyRangeAABB(pos), p_427192_ -> p_427192_ instanceof Enemy && p_427192_.isInWaterOrRain()
        );
        return list.isEmpty() ? null : EntityReference.of(Util.getRandom(list, level.random));
    }

    private static AABB getDestroyRangeAABB(BlockPos pos) {
        return new AABB(pos).inflate(8.0);
    }

    private static void animationTick(Level level, BlockPos pos, List<BlockPos> positions, @Nullable Entity entity, int tickCount) {
        RandomSource randomsource = level.random;
        double d0 = Mth.sin((tickCount + 35) * 0.1F) / 2.0F + 0.5F;
        d0 = (d0 * d0 + d0) * 0.3F;
        Vec3 vec3 = new Vec3(pos.getX() + 0.5, pos.getY() + 1.5 + d0, pos.getZ() + 0.5);

        for (BlockPos blockpos : positions) {
            if (randomsource.nextInt(50) == 0) {
                BlockPos blockpos1 = blockpos.subtract(pos);
                float f = -0.5F + randomsource.nextFloat() + blockpos1.getX();
                float f1 = -2.0F + randomsource.nextFloat() + blockpos1.getY();
                float f2 = -0.5F + randomsource.nextFloat() + blockpos1.getZ();
                level.addParticle(ParticleTypes.NAUTILUS, vec3.x, vec3.y, vec3.z, f, f1, f2);
            }
        }

        if (entity != null) {
            Vec3 vec31 = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
            float f3 = (-0.5F + randomsource.nextFloat()) * (3.0F + entity.getBbWidth());
            float f4 = -1.0F + randomsource.nextFloat() * entity.getBbHeight();
            float f5 = (-0.5F + randomsource.nextFloat()) * (3.0F + entity.getBbWidth());
            Vec3 vec32 = new Vec3(f3, f4, f5);
            level.addParticle(ParticleTypes.NAUTILUS, vec31.x, vec31.y, vec31.z, vec32.x, vec32.y, vec32.z);
        }
    }

    public boolean isActive() {
        return this.isActive;
    }

    public boolean isHunting() {
        return this.isHunting;
    }

    private void setHunting(boolean isHunting) {
        this.isHunting = isHunting;
    }

    public float getActiveRotation(float partialTick) {
        return (this.activeRotation + partialTick) * -0.0375F;
    }
}
