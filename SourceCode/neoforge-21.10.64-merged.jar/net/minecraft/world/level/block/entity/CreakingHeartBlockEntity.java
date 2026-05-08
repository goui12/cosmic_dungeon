package net.minecraft.world.level.block.entity;

import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

public class CreakingHeartBlockEntity extends BlockEntity {
    private static final int PLAYER_DETECTION_RANGE = 32;
    public static final int CREAKING_ROAMING_RADIUS = 32;
    private static final int DISTANCE_CREAKING_TOO_FAR = 34;
    private static final int SPAWN_RANGE_XZ = 16;
    private static final int SPAWN_RANGE_Y = 8;
    private static final int ATTEMPTS_PER_SPAWN = 5;
    private static final int UPDATE_TICKS = 20;
    private static final int UPDATE_TICKS_VARIANCE = 5;
    private static final int HURT_CALL_TOTAL_TICKS = 100;
    private static final int NUMBER_OF_HURT_CALLS = 10;
    private static final int HURT_CALL_INTERVAL = 10;
    private static final int HURT_CALL_PARTICLE_TICKS = 50;
    private static final int MAX_DEPTH = 2;
    private static final int MAX_COUNT = 64;
    private static final int TICKS_GRACE_PERIOD = 30;
    private static final Optional<Creaking> NO_CREAKING = Optional.empty();
    @Nullable
    private Either<Creaking, UUID> creakingInfo;
    private long ticksExisted;
    private int ticker;
    private int emitter;
    @Nullable
    private Vec3 emitterTarget;
    private int outputSignal;

    public CreakingHeartBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.CREAKING_HEART, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CreakingHeartBlockEntity creakingHeart) {
        creakingHeart.ticksExisted++;
        if (level instanceof ServerLevel serverlevel) {
            int $$6 = creakingHeart.computeAnalogOutputSignal();
            if (creakingHeart.outputSignal != $$6) {
                creakingHeart.outputSignal = $$6;
                level.updateNeighbourForOutputSignal(pos, Blocks.CREAKING_HEART);
            }

            if (creakingHeart.emitter > 0) {
                if (creakingHeart.emitter > 50) {
                    creakingHeart.emitParticles(serverlevel, 1, true);
                    creakingHeart.emitParticles(serverlevel, 1, false);
                }

                if (creakingHeart.emitter % 10 == 0 && creakingHeart.emitterTarget != null) {
                    creakingHeart.getCreakingProtector().ifPresent(p_427194_ -> creakingHeart.emitterTarget = p_427194_.getBoundingBox().getCenter());
                    Vec3 vec3 = Vec3.atCenterOf(pos);
                    float f = 0.2F + 0.8F * (100 - creakingHeart.emitter) / 100.0F;
                    Vec3 vec31 = vec3.subtract(creakingHeart.emitterTarget).scale(f).add(creakingHeart.emitterTarget);
                    BlockPos blockpos = BlockPos.containing(vec31);
                    float f1 = creakingHeart.emitter / 2.0F / 100.0F + 0.5F;
                    serverlevel.playSound(null, blockpos, SoundEvents.CREAKING_HEART_HURT, SoundSource.BLOCKS, f1, 1.0F);
                }

                creakingHeart.emitter--;
            }

            if (creakingHeart.ticker-- < 0) {
                creakingHeart.ticker = creakingHeart.level == null ? 20 : creakingHeart.level.random.nextInt(5) + 20;
                BlockState blockstate = updateCreakingState(level, state, pos, creakingHeart);
                if (blockstate != state) {
                    level.setBlock(pos, blockstate, 3);
                    if (blockstate.getValue(CreakingHeartBlock.STATE) == CreakingHeartState.UPROOTED) {
                        return;
                    }
                }

                if (creakingHeart.creakingInfo == null) {
                    if (blockstate.getValue(CreakingHeartBlock.STATE) == CreakingHeartState.AWAKE) {
                        if (serverlevel.isSpawningMonsters()) {
                            Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 32.0, false);
                            if (player != null) {
                                Creaking creaking1 = spawnProtector(serverlevel, creakingHeart);
                                if (creaking1 != null) {
                                    creakingHeart.setCreakingInfo(creaking1);
                                    creaking1.makeSound(SoundEvents.CREAKING_SPAWN);
                                    level.playSound(null, creakingHeart.getBlockPos(), SoundEvents.CREAKING_HEART_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                                }
                            }
                        }
                    }
                } else {
                    Optional<Creaking> optional = creakingHeart.getCreakingProtector();
                    if (optional.isPresent()) {
                        Creaking creaking = optional.get();
                        if (!CreakingHeartBlock.isNaturalNight(level) && !creaking.isPersistenceRequired()
                            || creakingHeart.distanceToCreaking() > 34.0
                            || creaking.playerIsStuckInYou()) {
                            creakingHeart.removeProtector(null);
                        }
                    }
                }
            }
        }
    }

    private static BlockState updateCreakingState(Level level, BlockState state, BlockPos pos, CreakingHeartBlockEntity creakingHeart) {
        if (!CreakingHeartBlock.hasRequiredLogs(state, level, pos) && creakingHeart.creakingInfo == null) {
            return state.setValue(CreakingHeartBlock.STATE, CreakingHeartState.UPROOTED);
        } else {
            boolean flag = CreakingHeartBlock.isNaturalNight(level);
            return state.setValue(CreakingHeartBlock.STATE, flag ? CreakingHeartState.AWAKE : CreakingHeartState.DORMANT);
        }
    }

    private double distanceToCreaking() {
        return this.getCreakingProtector().map(p_432710_ -> Math.sqrt(p_432710_.distanceToSqr(Vec3.atBottomCenterOf(this.getBlockPos())))).orElse(0.0);
    }

    private void clearCreakingInfo() {
        this.creakingInfo = null;
        this.setChanged();
    }

    public void setCreakingInfo(Creaking creaking) {
        this.creakingInfo = Either.left(creaking);
        this.setChanged();
    }

    public void setCreakingInfo(UUID creakingUuid) {
        this.creakingInfo = Either.right(creakingUuid);
        this.ticksExisted = 0L;
        this.setChanged();
    }

    private Optional<Creaking> getCreakingProtector() {
        if (this.creakingInfo == null) {
            return NO_CREAKING;
        } else {
            if (this.creakingInfo.left().isPresent()) {
                Creaking creaking = this.creakingInfo.left().get();
                if (!creaking.isRemoved()) {
                    return Optional.of(creaking);
                }

                this.setCreakingInfo(creaking.getUUID());
            }

            if (this.level instanceof ServerLevel serverlevel && this.creakingInfo.right().isPresent()) {
                UUID uuid = this.creakingInfo.right().get();
                if (serverlevel.getEntity(uuid) instanceof Creaking creaking1) {
                    this.setCreakingInfo(creaking1);
                    return Optional.of(creaking1);
                } else {
                    if (this.ticksExisted >= 30L) {
                        this.clearCreakingInfo();
                    }

                    return NO_CREAKING;
                }
            } else {
                return NO_CREAKING;
            }
        }
    }

    @Nullable
    private static Creaking spawnProtector(ServerLevel level, CreakingHeartBlockEntity creakingHeart) {
        BlockPos blockpos = creakingHeart.getBlockPos();
        Optional<Creaking> optional = SpawnUtil.trySpawnMob(
            EntityType.CREAKING, EntitySpawnReason.SPAWNER, level, blockpos, 5, 16, 8, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER_NO_LEAVES, true
        );
        if (optional.isEmpty()) {
            return null;
        } else {
            Creaking creaking = optional.get();
            level.gameEvent(creaking, GameEvent.ENTITY_PLACE, creaking.position());
            level.broadcastEntityEvent(creaking, (byte)60);
            creaking.setTransient(blockpos);
            return creaking;
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    public void creakingHurt() {
        if (this.getCreakingProtector().orElse(null) instanceof Creaking creaking) {
            if (this.level instanceof ServerLevel serverlevel) {
                if (this.emitter <= 0) {
                    this.emitParticles(serverlevel, 20, false);
                    if (this.getBlockState().getValue(CreakingHeartBlock.STATE) == CreakingHeartState.AWAKE) {
                        int j = this.level.getRandom().nextIntBetweenInclusive(2, 3);

                        for (int i = 0; i < j; i++) {
                            this.spreadResin().ifPresent(p_432709_ -> {
                                this.level.playSound(null, p_432709_, SoundEvents.RESIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                                this.level.gameEvent(GameEvent.BLOCK_PLACE, p_432709_, GameEvent.Context.of(this.getBlockState()));
                            });
                        }
                    }

                    this.emitter = 100;
                    this.emitterTarget = creaking.getBoundingBox().getCenter();
                }
            }
        }
    }

    private Optional<BlockPos> spreadResin() {
        Mutable<BlockPos> mutable = new MutableObject<>(null);
        BlockPos.breadthFirstTraversal(this.worldPosition, 2, 64, (p_389388_, p_389389_) -> {
            for (Direction direction : Util.shuffledCopy(Direction.values(), this.level.random)) {
                BlockPos blockpos = p_389388_.relative(direction);
                if (this.level.getBlockState(blockpos).is(BlockTags.PALE_OAK_LOGS)) {
                    p_389389_.accept(blockpos);
                }
            }
        }, p_389384_ -> {
            if (!this.level.getBlockState(p_389384_).is(BlockTags.PALE_OAK_LOGS)) {
                return BlockPos.TraversalNodeStatus.ACCEPT;
            } else {
                for (Direction direction : Util.shuffledCopy(Direction.values(), this.level.random)) {
                    BlockPos blockpos = p_389384_.relative(direction);
                    BlockState blockstate = this.level.getBlockState(blockpos);
                    Direction direction1 = direction.getOpposite();
                    if (blockstate.isAir()) {
                        blockstate = Blocks.RESIN_CLUMP.defaultBlockState();
                    } else if (blockstate.is(Blocks.WATER) && blockstate.getFluidState().isSource()) {
                        blockstate = Blocks.RESIN_CLUMP.defaultBlockState().setValue(MultifaceBlock.WATERLOGGED, true);
                    }

                    if (blockstate.is(Blocks.RESIN_CLUMP) && !MultifaceBlock.hasFace(blockstate, direction1)) {
                        this.level.setBlock(blockpos, blockstate.setValue(MultifaceBlock.getFaceProperty(direction1), true), 3);
                        mutable.setValue(blockpos);
                        return BlockPos.TraversalNodeStatus.STOP;
                    }
                }

                return BlockPos.TraversalNodeStatus.ACCEPT;
            }
        });
        return Optional.ofNullable(mutable.getValue());
    }

    private void emitParticles(ServerLevel level, int count, boolean reverseDirection) {
        if (this.getCreakingProtector().orElse(null) instanceof Creaking creaking) {
            int i = reverseDirection ? 16545810 : 6250335;
            RandomSource randomsource = level.random;

            for (double d0 = 0.0; d0 < count; d0++) {
                AABB aabb = creaking.getBoundingBox();
                Vec3 vec3 = aabb.getMinPosition()
                    .add(randomsource.nextDouble() * aabb.getXsize(), randomsource.nextDouble() * aabb.getYsize(), randomsource.nextDouble() * aabb.getZsize());
                Vec3 vec31 = Vec3.atLowerCornerOf(this.getBlockPos()).add(randomsource.nextDouble(), randomsource.nextDouble(), randomsource.nextDouble());
                if (reverseDirection) {
                    Vec3 vec32 = vec3;
                    vec3 = vec31;
                    vec31 = vec32;
                }

                TrailParticleOption trailparticleoption = new TrailParticleOption(vec31, i, randomsource.nextInt(40) + 10);
                level.sendParticles(trailparticleoption, true, true, vec3.x, vec3.y, vec3.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        this.removeProtector(null);
    }

    public void removeProtector(@Nullable DamageSource damageSource) {
        if (this.getCreakingProtector().orElse(null) instanceof Creaking creaking) {
            if (damageSource == null) {
                creaking.tearDown();
            } else {
                creaking.creakingDeathEffects(damageSource);
                creaking.setTearingDown();
                creaking.setHealth(0.0F);
            }

            this.clearCreakingInfo();
        }
    }

    public boolean isProtector(Creaking creaking) {
        return this.getCreakingProtector().map(p_389391_ -> p_389391_ == creaking).orElse(false);
    }

    public int getAnalogOutputSignal() {
        return this.outputSignal;
    }

    public int computeAnalogOutputSignal() {
        if (this.creakingInfo != null && !this.getCreakingProtector().isEmpty()) {
            double d0 = this.distanceToCreaking();
            double d1 = Math.clamp(d0, 0.0, 32.0) / 32.0;
            return 15 - (int)Math.floor(d1 * 15.0);
        } else {
            return 0;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("creaking", UUIDUtil.CODEC).ifPresentOrElse(this::setCreakingInfo, this::clearCreakingInfo);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.creakingInfo != null) {
            output.store("creaking", UUIDUtil.CODEC, this.creakingInfo.map(Entity::getUUID, p_389392_ -> (UUID)p_389392_));
        }
    }
}
