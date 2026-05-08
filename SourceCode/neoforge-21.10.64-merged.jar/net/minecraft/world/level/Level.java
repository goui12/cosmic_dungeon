package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import org.apache.commons.lang3.mutable.MutableBoolean;

public abstract class Level extends net.neoforged.neoforge.attachment.AttachmentHolder implements LevelAccessor, AutoCloseable, net.neoforged.neoforge.common.extensions.ILevelExtension {
    public static final Codec<ResourceKey<Level>> RESOURCE_KEY_CODEC = ResourceKey.codec(Registries.DIMENSION);
    public static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"));
    public static final ResourceKey<Level> NETHER = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_nether"));
    public static final ResourceKey<Level> END = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_end"));
    public static final int MAX_LEVEL_SIZE = 30000000;
    public static final int LONG_PARTICLE_CLIP_RANGE = 512;
    public static final int SHORT_PARTICLE_CLIP_RANGE = 32;
    public static final int MAX_BRIGHTNESS = 15;
    public static final int TICKS_PER_DAY = 24000;
    public static final int MAX_ENTITY_SPAWN_Y = 20000000;
    public static final int MIN_ENTITY_SPAWN_Y = -20000000;
    private static final WeightedList<ExplosionParticleInfo> DEFAULT_EXPLOSION_BLOCK_PARTICLES = WeightedList.<ExplosionParticleInfo>builder()
        .add(new ExplosionParticleInfo(ParticleTypes.POOF, 0.5F, 1.0F))
        .add(new ExplosionParticleInfo(ParticleTypes.SMOKE, 1.0F, 1.0F))
        .build();
    protected final List<TickingBlockEntity> blockEntityTickers = Lists.newArrayList();
    protected final CollectingNeighborUpdater neighborUpdater;
    private final List<TickingBlockEntity> pendingBlockEntityTickers = Lists.newArrayList();
    private boolean tickingBlockEntities;
    private final Thread thread;
    private final boolean isDebug;
    private int skyDarken;
    /**
     * Contains the current Linear Congruential Generator seed for block updates. Used with an A value of 3 and a C value of 0x3c6ef35f, producing a highly planar series of values ill-suited for choosing random blocks in a 16x128x16 field.
     */
    protected int randValue = RandomSource.create().nextInt();
    protected final int addend = 1013904223;
    public float oRainLevel;
    public float rainLevel;
    public float oThunderLevel;
    public float thunderLevel;
    public final RandomSource random = RandomSource.create();
    @Deprecated
    private final RandomSource threadSafeRandom = RandomSource.createThreadSafe();
    private final Holder<DimensionType> dimensionTypeRegistration;
    protected final WritableLevelData levelData;
    private final boolean isClientSide;
    private final BiomeManager biomeManager;
    private final ResourceKey<Level> dimension;
    private final RegistryAccess registryAccess;
    private final DamageSources damageSources;
    private final PalettedContainerFactory palettedContainerFactory;
    private long subTickCount;
    public boolean restoringBlockSnapshots = false;
    public boolean captureBlockSnapshots = false;
    public java.util.ArrayList<net.neoforged.neoforge.common.util.BlockSnapshot> capturedBlockSnapshots = new java.util.ArrayList<>();
    private final java.util.ArrayList<BlockEntity> freshBlockEntities = new java.util.ArrayList<>();
    private final java.util.ArrayList<BlockEntity> pendingFreshBlockEntities = new java.util.ArrayList<>();

    protected Level(
        WritableLevelData levelData,
        ResourceKey<Level> dimension,
        RegistryAccess registryAccess,
        Holder<DimensionType> dimensionTypeRegistration,
        boolean isClientSide,
        boolean isDebug,
        long biomeZoomSeed,
        int maxChainedNeighborUpdates
    ) {
        this.levelData = levelData;
        this.dimensionTypeRegistration = dimensionTypeRegistration;
        DimensionType dimensiontype = dimensionTypeRegistration.value();
        this.dimension = dimension;
        this.isClientSide = isClientSide;
        this.thread = Thread.currentThread();
        this.biomeManager = new BiomeManager(this, biomeZoomSeed);
        this.isDebug = isDebug;
        this.neighborUpdater = new CollectingNeighborUpdater(this, maxChainedNeighborUpdates);
        this.registryAccess = registryAccess;
        this.palettedContainerFactory = PalettedContainerFactory.create(registryAccess);
        this.damageSources = new DamageSources(registryAccess);
    }

    @Override
    public boolean isClientSide() {
        return this.isClientSide;
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return null;
    }

    /**
     * Check if the given BlockPos has valid coordinates
     */
    public boolean isInWorldBounds(BlockPos pos) {
        return !this.isOutsideBuildHeight(pos) && isInWorldBoundsHorizontal(pos);
    }

    public static boolean isInSpawnableBounds(BlockPos pos) {
        return !isOutsideSpawnableHeight(pos.getY()) && isInWorldBoundsHorizontal(pos);
    }

    private static boolean isInWorldBoundsHorizontal(BlockPos pos) {
        return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000;
    }

    private static boolean isOutsideSpawnableHeight(int y) {
        return y < -20000000 || y >= 20000000;
    }

    public LevelChunk getChunkAt(BlockPos pos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public LevelChunk getChunk(int chunkX, int chunkZ) {
        return (LevelChunk)this.getChunk(chunkX, chunkZ, ChunkStatus.FULL);
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus chunkStatus, boolean requireChunk) {
        ChunkAccess chunkaccess = this.getChunkSource().getChunk(x, z, chunkStatus, requireChunk);
        if (chunkaccess == null && requireChunk) {
            throw new IllegalStateException("Should always be able to create a chunk!");
        } else {
            return chunkaccess;
        }
    }

    /**
     * Sets a block state into this world.Flags are as follows:
     * 1 will notify neighboring blocks through {@link net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase#neighborChanged neighborChanged} updates.
     * 2 will send the change to clients.
     * 4 will prevent the block from being re-rendered.
     * 8 will force any re-renders to run on the main thread instead
     * 16 will prevent neighbor reactions (e.g. fences connecting, observers pulsing).
     * 32 will prevent neighbor reactions from spawning drops.
     * 64 will signify the block is being moved.
     * Flags can be OR-ed
     */
    @Override
    public boolean setBlock(BlockPos pos, BlockState newState, int flags) {
        return this.setBlock(pos, newState, flags, 512);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
        if (this.isOutsideBuildHeight(pos)) {
            return false;
        } else if (!this.isClientSide() && this.isDebug()) {
            return false;
        } else {
            LevelChunk levelchunk = this.getChunkAt(pos);
            Block block = state.getBlock();

            pos = pos.immutable(); // Forge - prevent mutable BlockPos leaks
            net.neoforged.neoforge.common.util.BlockSnapshot blockSnapshot = null;
            if (this.captureBlockSnapshots && !this.isClientSide) {
                 blockSnapshot = net.neoforged.neoforge.common.util.BlockSnapshot.create(this.dimension, this, pos, flags);
                 this.capturedBlockSnapshots.add(blockSnapshot);
            }

            BlockState blockstate = levelchunk.setBlockState(pos, state, flags);
            if (blockstate == null) {
                if (blockSnapshot != null) this.capturedBlockSnapshots.remove(blockSnapshot);
                return false;
            } else {
                BlockState blockstate1 = this.getBlockState(pos);

                if (blockSnapshot == null) { // Don't notify clients or update physics while capturing blockstates
                    this.markAndNotifyBlock(pos, levelchunk, blockstate, state, flags, recursionLeft);
                }

                return true;
            }
        }
    }

    // Split off from original setBlockState(BlockPos, BlockState, int, int) method in order to directly send client and physic updates
    public void markAndNotifyBlock(BlockPos p_46605_, @Nullable LevelChunk levelchunk, BlockState blockstate, BlockState p_46606_, int p_46607_, int p_46608_) {
        Block block = p_46606_.getBlock();
        BlockState blockstate1 = getBlockState(p_46605_);
        {
            {
                if (blockstate1 == p_46606_) {
                    if (blockstate != blockstate1) {
                        this.setBlocksDirty(p_46605_, blockstate, blockstate1);
                    }

                    if ((p_46607_ & 2) != 0
                        && (!this.isClientSide() || (p_46607_ & 4) == 0)
                        && (this.isClientSide() || levelchunk.getFullStatus() != null && levelchunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))) {
                        this.sendBlockUpdated(p_46605_, blockstate, p_46606_, p_46607_);
                    }

                    if ((p_46607_ & 1) != 0) {
                        this.updateNeighborsAt(p_46605_, blockstate.getBlock());
                        if (!this.isClientSide() && p_46606_.hasAnalogOutputSignal()) {
                            this.updateNeighbourForOutputSignal(p_46605_, block);
                        }
                    }

                    if ((p_46607_ & 16) == 0 && p_46608_ > 0) {
                        int i = p_46607_ & -34;
                        blockstate.updateIndirectNeighbourShapes(this, p_46605_, i, p_46608_ - 1);
                        p_46606_.updateNeighbourShapes(this, p_46605_, i, p_46608_ - 1);
                        p_46606_.updateIndirectNeighbourShapes(this, p_46605_, i, p_46608_ - 1);
                    }

                    this.updatePOIOnBlockStateChange(p_46605_, blockstate, blockstate1);
                    p_46606_.onBlockStateChange(this, p_46605_, blockstate);
                }
            }
        }
    }

    public void updatePOIOnBlockStateChange(BlockPos pos, BlockState oldState, BlockState newState) {
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean isMoving) {
        FluidState fluidstate = this.getFluidState(pos);
        return this.setBlock(pos, fluidstate.createLegacyBlock(), 3 | (isMoving ? 64 : 0));
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropBlock, @Nullable Entity entity, int recursionLeft) {
        BlockState blockstate = this.getBlockState(pos);
        if (blockstate.isAir()) {
            return false;
        } else {
            FluidState fluidstate = this.getFluidState(pos);
            if (!(blockstate.getBlock() instanceof BaseFireBlock)) {
                this.levelEvent(2001, pos, Block.getId(blockstate));
            }

            if (dropBlock) {
                BlockEntity blockentity = blockstate.hasBlockEntity() ? this.getBlockEntity(pos) : null;
                Block.dropResources(blockstate, this, pos, blockentity, entity, ItemStack.EMPTY);
            }

            boolean flag = this.setBlock(pos, fluidstate.createLegacyBlock(), 3, recursionLeft);
            if (flag) {
                this.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(entity, blockstate));
            }

            return flag;
        }
    }

    public void addDestroyBlockEffect(BlockPos pos, BlockState state) {
    }

    /**
     * Convenience method to update the block on both the client and server
     */
    public boolean setBlockAndUpdate(BlockPos pos, BlockState state) {
        return this.setBlock(pos, state, 3);
    }

    /**
     * Flags are as in setBlockState
     */
    public abstract void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags);

    public void setBlocksDirty(BlockPos blockPos, BlockState oldState, BlockState newState) {
    }

    public void updateNeighborsAt(BlockPos pos, Block block, @Nullable Orientation orientation) {
        net.neoforged.neoforge.event.EventHooks.onNeighborNotify(this, pos, this.getBlockState(pos), java.util.EnumSet.allOf(Direction.class), false).isCanceled();
    }

    public void updateNeighborsAtExceptFromFacing(BlockPos pos, Block block, Direction facing, @Nullable Orientation orientation) {
    }

    public void neighborChanged(BlockPos pos, Block block, @Nullable Orientation orientation) {
    }

    public void neighborChanged(BlockState state, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
    }

    @Override
    public void neighborShapeChanged(Direction direction, BlockPos pos, BlockPos neighborPos, BlockState neighborState, int flags, int recursionLeft) {
        this.neighborUpdater.shapeUpdate(direction, neighborState, pos, neighborPos, flags, recursionLeft);
    }

    @Override
    public int getHeight(Heightmap.Types heightmapType, int x, int z) {
        int i;
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000) {
            if (this.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                i = this.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))
                        .getHeight(heightmapType, x & 15, z & 15)
                    + 1;
            } else {
                i = this.getMinY();
            }
        } else {
            i = this.getSeaLevel() + 1;
        }

        return i;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.getChunkSource().getLightEngine();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (this.isOutsideBuildHeight(pos)) {
            return Blocks.VOID_AIR.defaultBlockState();
        } else {
            LevelChunk levelchunk = this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
            return levelchunk.getBlockState(pos);
        }
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        if (this.isOutsideBuildHeight(pos)) {
            return Fluids.EMPTY.defaultFluidState();
        } else {
            LevelChunk levelchunk = this.getChunkAt(pos);
            return levelchunk.getFluidState(pos);
        }
    }

    public boolean isBrightOutside() {
        return !this.dimensionType().hasFixedTime() && this.skyDarken < 4;
    }

    public boolean isDarkOutside() {
        return !this.dimensionType().hasFixedTime() && !this.isBrightOutside();
    }

    public boolean isMoonVisible() {
        if (!this.dimensionType().natural()) {
            return false;
        } else {
            int i = (int)(this.getDayTime() % 24000L);
            return i >= 12600 && i <= 23400;
        }
    }

    @Override
    public void playSound(@Nullable Entity entity, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch) {
        this.playSound(entity, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, category, volume, pitch);
    }

    public abstract void playSeededSound(
        @Nullable Entity entity,
        double x,
        double y,
        double z,
        Holder<SoundEvent> sound,
        SoundSource source,
        float volume,
        float pitch,
        long seed
    );

    public void playSeededSound(
        @Nullable Entity entity,
        double x,
        double y,
        double z,
        SoundEvent sound,
        SoundSource source,
        float volume,
        float pitch,
        long seed
    ) {
        this.playSeededSound(
            entity, x, y, z, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, volume, pitch, seed
        );
    }

    public abstract void playSeededSound(
        @Nullable Entity entity, Entity sourceEntity, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed
    );

    public void playSound(@Nullable Entity entity, double x, double y, double z, SoundEvent sound, SoundSource source) {
        this.playSound(entity, x, y, z, sound, source, 1.0F, 1.0F);
    }

    public void playSound(
        @Nullable Entity entity,
        double x,
        double y,
        double z,
        SoundEvent sound,
        SoundSource source,
        float volume,
        float pitch
    ) {
        this.playSeededSound(entity, x, y, z, sound, source, volume, pitch, this.threadSafeRandom.nextLong());
    }

    public void playSound(
        @Nullable Entity entity,
        double x,
        double y,
        double z,
        Holder<SoundEvent> sound,
        SoundSource source,
        float volume,
        float pitch
    ) {
        this.playSeededSound(entity, x, y, z, sound, source, volume, pitch, this.threadSafeRandom.nextLong());
    }

    public void playSound(@Nullable Entity entity, Entity sourceEntity, SoundEvent sound, SoundSource source, float volume, float pitch) {
        this.playSeededSound(
            entity, sourceEntity, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, volume, pitch, this.threadSafeRandom.nextLong()
        );
    }

    public void playLocalSound(BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay) {
        this.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, source, volume, pitch, distanceDelay);
    }

    public void playLocalSound(Entity entity, SoundEvent sound, SoundSource source, float volume, float pitch) {
    }

    public void playLocalSound(
        double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay
    ) {
    }

    public void playPlayerSound(SoundEvent sound, SoundSource source, float volume, float pitch) {
    }

    @Override
    public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
    }

    public void addParticle(
        ParticleOptions particle,
        boolean overrideLimiter,
        boolean alwaysShow,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed
    ) {
    }

    public void addAlwaysVisibleParticle(
        ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
    ) {
    }

    public void addAlwaysVisibleParticle(
        ParticleOptions particle, boolean overrideLimiter, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
    ) {
    }

    /**
     * Return getCelestialAngle()*2*PI
     */
    public float getSunAngle(float partialTick) {
        float f = this.getTimeOfDay(partialTick);
        return f * (float) (Math.PI * 2);
    }

    public void addBlockEntityTicker(TickingBlockEntity ticker) {
        (this.tickingBlockEntities ? this.pendingBlockEntityTickers : this.blockEntityTickers).add(ticker);
    }

    public void addFreshBlockEntities(java.util.Collection<BlockEntity> beList) {
        if (this.tickingBlockEntities) {
            this.pendingFreshBlockEntities.addAll(beList);
        } else {
            this.freshBlockEntities.addAll(beList);
        }
    }

    public void tickBlockEntities() {
        if (!this.pendingFreshBlockEntities.isEmpty()) {
            this.freshBlockEntities.addAll(this.pendingFreshBlockEntities);
            this.pendingFreshBlockEntities.clear();
        }
        this.tickingBlockEntities = true;
        if (!this.freshBlockEntities.isEmpty()) {
            this.freshBlockEntities.forEach(blockEntity -> {
                // Only call onLoad() on BEs which have been fully added to the level, prevents crashes with BEs that
                // were discarded due to incompatibility with the BlockState at their position
                if (!blockEntity.isRemoved() && blockEntity.hasLevel()) {
                    blockEntity.onLoad();
                }
            });
            this.freshBlockEntities.clear();
        }
        if (!this.pendingBlockEntityTickers.isEmpty()) {
            this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
            this.pendingBlockEntityTickers.clear();
        }

        Iterator<TickingBlockEntity> iterator = this.blockEntityTickers.iterator();
        boolean flag = this.tickRateManager().runsNormally();

        while (iterator.hasNext()) {
            TickingBlockEntity tickingblockentity = iterator.next();
            if (tickingblockentity.isRemoved()) {
                iterator.remove();
            } else if (flag && this.shouldTickBlocksAt(tickingblockentity.getPos())) {
                tickingblockentity.tick();
            }
        }

        this.tickingBlockEntities = false;
    }

    public <T extends Entity> void guardEntityTick(Consumer<T> action, T entity) {
        try {
            net.neoforged.neoforge.server.timings.TimeTracker.ENTITY_UPDATE.trackStart(entity);
            action.accept(entity);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking entity");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being ticked");
            entity.fillCrashReportCategory(crashreportcategory);
            if (net.neoforged.neoforge.common.config.NeoForgeServerConfig.INSTANCE.removeErroringEntities.get()) {
                com.mojang.logging.LogUtils.getLogger().error("{}", crashreport.getFriendlyReport(net.minecraft.ReportType.CRASH));
                entity.discard();
            } else
            throw new ReportedException(crashreport);
        } finally {
            net.neoforged.neoforge.server.timings.TimeTracker.ENTITY_UPDATE.trackEnd(entity);
        }
    }

    public boolean shouldTickDeath(Entity entity) {
        return true;
    }

    public boolean shouldTickBlocksAt(long chunkPos) {
        return true;
    }

    public boolean shouldTickBlocksAt(BlockPos pos) {
        return this.shouldTickBlocksAt(ChunkPos.asLong(pos));
    }

    public void explode(@Nullable Entity source, double x, double y, double z, float radius, Level.ExplosionInteraction explosionInteraction) {
        this.explode(
            source,
            Explosion.getDefaultDamageSource(this, source),
            null,
            x,
            y,
            z,
            radius,
            false,
            explosionInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            DEFAULT_EXPLOSION_BLOCK_PARTICLES,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public void explode(
        @Nullable Entity source,
        double x,
        double y,
        double z,
        float radius,
        boolean fire,
        Level.ExplosionInteraction explosionInteraction
    ) {
        this.explode(
            source,
            Explosion.getDefaultDamageSource(this, source),
            null,
            x,
            y,
            z,
            radius,
            fire,
            explosionInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            DEFAULT_EXPLOSION_BLOCK_PARTICLES,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public void explode(
        @Nullable Entity source,
        @Nullable DamageSource damageSource,
        @Nullable ExplosionDamageCalculator damageCalculator,
        Vec3 pos,
        float radius,
        boolean fire,
        Level.ExplosionInteraction explosionInteraction
    ) {
        this.explode(
            source,
            damageSource,
            damageCalculator,
            pos.x(),
            pos.y(),
            pos.z(),
            radius,
            fire,
            explosionInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            DEFAULT_EXPLOSION_BLOCK_PARTICLES,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public void explode(
        @Nullable Entity source,
        @Nullable DamageSource damageSource,
        @Nullable ExplosionDamageCalculator damageCalculator,
        double x,
        double y,
        double z,
        float radius,
        boolean fire,
        Level.ExplosionInteraction explosionInteraction
    ) {
        this.explode(
            source,
            damageSource,
            damageCalculator,
            x,
            y,
            z,
            radius,
            fire,
            explosionInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            DEFAULT_EXPLOSION_BLOCK_PARTICLES,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public abstract void explode(
        @Nullable Entity source,
        @Nullable DamageSource damageSource,
        @Nullable ExplosionDamageCalculator damageCalculator,
        double x,
        double y,
        double z,
        float radius,
        boolean fire,
        Level.ExplosionInteraction explosionInteraction,
        ParticleOptions smallExplosionParticles,
        ParticleOptions largeExplosionParticles,
        WeightedList<ExplosionParticleInfo> blockParticles,
        Holder<SoundEvent> explosionSound
    );

    public abstract String gatherChunkSourceStats();

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        if (this.isOutsideBuildHeight(pos)) {
            return null;
        } else {
            return !this.isClientSide() && Thread.currentThread() != this.thread
                ? null
                : this.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
        }
    }

    public void setBlockEntity(BlockEntity blockEntity) {
        BlockPos blockpos = blockEntity.getBlockPos();
        if (!this.isOutsideBuildHeight(blockpos)) {
            this.getChunkAt(blockpos).addAndRegisterBlockEntity(blockEntity);
        }
    }

    public void removeBlockEntity(BlockPos pos) {
        if (!this.isOutsideBuildHeight(pos)) {
            this.getChunkAt(pos).removeBlockEntity(pos);
        }
        this.updateNeighbourForOutputSignal(pos, getBlockState(pos).getBlock()); //Notify neighbors of changes
    }

    public boolean isLoaded(BlockPos pos) {
        return this.isOutsideBuildHeight(pos)
            ? false
            : this.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public boolean loadedAndEntityCanStandOnFace(BlockPos pos, Entity entity, Direction direction) {
        if (this.isOutsideBuildHeight(pos)) {
            return false;
        } else {
            ChunkAccess chunkaccess = this.getChunk(
                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false
            );
            return chunkaccess == null ? false : chunkaccess.getBlockState(pos).entityCanStandOnFace(this, pos, entity, direction);
        }
    }

    public boolean loadedAndEntityCanStandOn(BlockPos pos, Entity entity) {
        return this.loadedAndEntityCanStandOnFace(pos, entity, Direction.UP);
    }

    public void updateSkyBrightness() {
        double d0 = 1.0 - this.getRainLevel(1.0F) * 5.0F / 16.0;
        double d1 = 1.0 - this.getThunderLevel(1.0F) * 5.0F / 16.0;
        double d2 = 0.5 + 2.0 * Mth.clamp((double)Mth.cos(this.getTimeOfDay(1.0F) * (float) (Math.PI * 2)), -0.25, 0.25);
        this.skyDarken = (int)((1.0 - d2 * d0 * d1) * 11.0);
    }

    public void setSpawnSettings(boolean spawnSettings) {
        this.getChunkSource().setSpawnSettings(spawnSettings);
    }

    public abstract void setRespawnData(LevelData.RespawnData respawnData);

    public abstract LevelData.RespawnData getRespawnData();

    public LevelData.RespawnData getWorldBorderAdjustedRespawnData(LevelData.RespawnData respawnData) {
        WorldBorder worldborder = this.getWorldBorder();
        if (!worldborder.isWithinBounds(respawnData.pos())) {
            BlockPos blockpos = this.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(worldborder.getCenterX(), 0.0, worldborder.getCenterZ())
            );
            return LevelData.RespawnData.of(respawnData.dimension(), blockpos, respawnData.yaw(), respawnData.pitch());
        } else {
            return respawnData;
        }
    }

    protected void prepareWeather() {
        if (this.levelData.isRaining()) {
            this.rainLevel = 1.0F;
            if (this.levelData.isThundering()) {
                this.thunderLevel = 1.0F;
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.getChunkSource().close();
    }

    @Nullable
    @Override
    public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
    }

    /**
     * Gets all entities within the specified AABB excluding the one passed into it.
     */
    @Override
    public List<Entity> getEntities(@Nullable Entity entity, AABB boundingBox, Predicate<? super Entity> predicate) {
        Profiler.get().incrementCounter("getEntities");
        List<Entity> list = Lists.newArrayList();
        this.getEntities().get(boundingBox, p_382758_ -> {
            if (p_382758_ != entity && predicate.test(p_382758_)) {
                list.add(p_382758_);
            }
        });

        for (net.neoforged.neoforge.entity.PartEntity<?> enderdragonpart : this.dragonParts()) {
            if (enderdragonpart != entity
                && enderdragonpart.getParent() != entity
                && predicate.test(enderdragonpart)
                && boundingBox.intersects(enderdragonpart.getBoundingBox())) {
                list.add(enderdragonpart);
            }
        }

        return list;
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB bounds, Predicate<? super T> predicate) {
        List<T> list = Lists.newArrayList();
        this.getEntities(entityTypeTest, bounds, predicate, list);
        return list;
    }

    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB bounds, Predicate<? super T> predicate, List<? super T> output) {
        this.getEntities(entityTypeTest, bounds, predicate, output, Integer.MAX_VALUE);
    }

    public <T extends Entity> void getEntities(
        EntityTypeTest<Entity, T> entityTypeTest, AABB bounds, Predicate<? super T> predicate, List<? super T> output, int maxResults
    ) {
        Profiler.get().incrementCounter("getEntities");
        this.getEntities().get(entityTypeTest, bounds, p_261454_ -> {
            if (predicate.test(p_261454_)) {
                output.add(p_261454_);
                if (output.size() >= maxResults) {
                    return AbortableIterationConsumer.Continuation.ABORT;
                }
            }


            if (false)
            if (p_261454_ instanceof EnderDragon enderdragon) {
                for (EnderDragonPart enderdragonpart : enderdragon.getSubEntities()) {
                    T t = entityTypeTest.tryCast(enderdragonpart);
                    if (t != null && predicate.test(t)) {
                        output.add(t);
                        if (output.size() >= maxResults) {
                            return AbortableIterationConsumer.Continuation.ABORT;
                        }
                    }
                }
            }

            return AbortableIterationConsumer.Continuation.CONTINUE;
        });
        for (net.neoforged.neoforge.entity.PartEntity<?> p : this.dragonParts()) {
            T t = entityTypeTest.tryCast(p);
            if (t != null && t.getBoundingBox().intersects(bounds) && predicate.test(t)) {
                output.add(t);
                if (output.size() >= maxResults) {
                    break;
                }
            }
        }
    }

    public <T extends Entity> boolean hasEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB bounds, Predicate<? super T> predicate) {
        Profiler.get().incrementCounter("hasEntities");
        MutableBoolean mutableboolean = new MutableBoolean();
        this.getEntities().get(entityTypeTest, bounds, p_445349_ -> {
            if (predicate.test(p_445349_)) {
                mutableboolean.setTrue();
                return AbortableIterationConsumer.Continuation.ABORT;
            } else {
                if (false)
                if (p_445349_ instanceof EnderDragon enderdragon) {
                    for (EnderDragonPart enderdragonpart : enderdragon.getSubEntities()) {
                        T t = entityTypeTest.tryCast(enderdragonpart);
                        if (t != null && predicate.test(t)) {
                            mutableboolean.setTrue();
                            return AbortableIterationConsumer.Continuation.ABORT;
                        }
                    }
                }

                return AbortableIterationConsumer.Continuation.CONTINUE;
            }
        });
        for (net.neoforged.neoforge.entity.PartEntity<?> p : this.dragonParts()) {
            T t = entityTypeTest.tryCast(p);
            if (t != null && t.getBoundingBox().intersects(bounds) && predicate.test(t)) {
                mutableboolean.setTrue();
                break;
            }
        }
        return mutableboolean.isTrue();
    }

    public List<Entity> getPushableEntities(Entity entity, AABB boundingBox) {
        return this.getEntities(entity, boundingBox, EntitySelector.pushableBy(entity));
    }

    /**
     * Returns the Entity with the given ID, or null if it doesn't exist in this Level.
     */
    @Nullable
    public abstract Entity getEntity(int id);

    @Nullable
    public Entity getEntity(UUID uuid) {
        return this.getEntities().get(uuid);
    }

    @Nullable
    public Entity getEntityInAnyDimension(UUID uuid) {
        return this.getEntity(uuid);
    }

    @Nullable
    public Player getPlayerInAnyDimension(UUID uuid) {
        return this.getPlayerByUUID(uuid);
    }

    public abstract Collection<net.neoforged.neoforge.entity.PartEntity<?>> dragonParts();

    public void blockEntityChanged(BlockPos pos) {
        if (this.hasChunkAt(pos)) {
            this.getChunkAt(pos).markUnsaved();
        }
    }

    public void onBlockEntityAdded(BlockEntity entity) {
    }

    public long getGameTime() {
        return this.levelData.getGameTime();
    }

    public long getDayTime() {
        return this.levelData.getDayTime();
    }

    public boolean mayInteract(Entity entity, BlockPos pos) {
        return true;
    }

    /**
     * Sends a {@link net.minecraft.network.protocol.game.ClientboundEntityEventPacket} to all tracked players of that entity.
     */
    public void broadcastEntityEvent(Entity entity, byte state) {
    }

    public void broadcastDamageEvent(Entity entity, DamageSource damageSource) {
    }

    public void blockEvent(BlockPos pos, Block block, int eventID, int eventParam) {
        this.getBlockState(pos).triggerEvent(this, pos, eventID, eventParam);
    }

    @Override
    public LevelData getLevelData() {
        return this.levelData;
    }

    public abstract TickRateManager tickRateManager();

    public float getThunderLevel(float partialTick) {
        return Mth.lerp(partialTick, this.oThunderLevel, this.thunderLevel) * this.getRainLevel(partialTick);
    }

    /**
     * Sets the strength of the thunder.
     */
    public void setThunderLevel(float strength) {
        float f = Mth.clamp(strength, 0.0F, 1.0F);
        this.oThunderLevel = f;
        this.thunderLevel = f;
    }

    /**
     * Returns rain strength.
     */
    public float getRainLevel(float partialTick) {
        return Mth.lerp(partialTick, this.oRainLevel, this.rainLevel);
    }

    /**
     * Sets the strength of the rain.
     */
    public void setRainLevel(float strength) {
        float f = Mth.clamp(strength, 0.0F, 1.0F);
        this.oRainLevel = f;
        this.rainLevel = f;
    }

    private boolean canHaveWeather() {
        return this.dimensionType().hasSkyLight() && !this.dimensionType().hasCeiling();
    }

    public boolean isThundering() {
        return this.canHaveWeather() && this.getThunderLevel(1.0F) > 0.9;
    }

    public boolean isRaining() {
        return this.canHaveWeather() && this.getRainLevel(1.0F) > 0.2;
    }

    /**
     * Check if precipitation is currently happening at a position
     */
    public boolean isRainingAt(BlockPos pos) {
        return this.precipitationAt(pos) == Biome.Precipitation.RAIN;
    }

    public Biome.Precipitation precipitationAt(BlockPos pos) {
        if (!this.isRaining()) {
            return Biome.Precipitation.NONE;
        } else if (!this.canSeeSky(pos)) {
            return Biome.Precipitation.NONE;
        } else if (this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return Biome.Precipitation.NONE;
        } else {
            Biome biome = this.getBiome(pos).value();
            return biome.getPrecipitationAt(pos, this.getSeaLevel());
        }
    }

    @Nullable
    public abstract MapItemSavedData getMapData(MapId mapId);

    public void globalLevelEvent(int id, BlockPos pos, int data) {
    }

    /**
     * Adds some basic stats of the world to the given crash report.
     */
    public CrashReportCategory fillReportDetails(CrashReport report) {
        CrashReportCategory crashreportcategory = report.addCategory("Affected level", 1);
        crashreportcategory.setDetail("All players", () -> {
            List<? extends Player> list = this.players();
            return list.size() + " total; " + list.stream().map(Player::debugInfo).collect(Collectors.joining(", "));
        });
        crashreportcategory.setDetail("Chunk stats", this.getChunkSource()::gatherStats);
        crashreportcategory.setDetail("Level dimension", () -> this.dimension().location().toString());

        try {
            this.levelData.fillCrashReportCategory(crashreportcategory, this);
        } catch (Throwable throwable) {
            crashreportcategory.setDetailError("Level Data Unobtainable", throwable);
        }

        return crashreportcategory;
    }

    public abstract void destroyBlockProgress(int breakerId, BlockPos pos, int progress);

    public void createFireworks(
        double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, List<FireworkExplosion> explosions
    ) {
    }

    public abstract Scoreboard getScoreboard();

    private static final Direction[] NEIGHBOR_UPDATE_LIST = java.util.stream.Stream.concat(Direction.Plane.HORIZONTAL.stream(), Direction.Plane.VERTICAL.stream()).toArray(Direction[]::new);

    public void updateNeighbourForOutputSignal(BlockPos pos, Block block) {
        // Neo: send update to vertical directions as well, after horizontal ones
        for (Direction direction : NEIGHBOR_UPDATE_LIST) {
            BlockPos blockpos = pos.relative(direction);
            if (this.hasChunkAt(blockpos)) {
                BlockState blockstate = this.getBlockState(blockpos);
                blockstate.onNeighborChange(this, blockpos, pos);
                if (blockstate.isRedstoneConductor(this, blockpos)) {
                    blockpos = blockpos.relative(direction);
                    blockstate = this.getBlockState(blockpos);
                    // Neo: send to any blockstate that wants weak changes, but exclude vanilla comparators from vertical updates
                    if (blockstate.getWeakChanges(this, blockpos) && (direction.getAxis().isHorizontal() || !blockstate.is(Blocks.COMPARATOR))) {
                        this.neighborChanged(blockstate, blockpos, block, null, false);
                    }
                }
            }
        }
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        long i = 0L;
        float f = 0.0F;
        if (this.hasChunkAt(pos)) {
            f = this.getMoonBrightness();
            i = this.getChunkAt(pos).getInhabitedTime();
        }

        return new DifficultyInstance(this.getDifficulty(), this.getDayTime(), i, f);
    }

    @Override
    public int getSkyDarken() {
        return this.skyDarken;
    }

    public void setSkyFlashTime(int timeFlash) {
    }

    public void sendPacketToServer(Packet<?> packet) {
        throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
    }

    @Override
    public DimensionType dimensionType() {
        return this.dimensionTypeRegistration.value();
    }

    public Holder<DimensionType> dimensionTypeRegistration() {
        return this.dimensionTypeRegistration;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    @Override
    public RandomSource getRandom() {
        return this.random;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> state) {
        return state.test(this.getBlockState(pos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) {
        return predicate.test(this.getFluidState(pos));
    }

    public abstract RecipeAccess recipeAccess();

    public BlockPos getBlockRandomPos(int x, int y, int z, int yMask) {
        this.randValue = this.randValue * 3 + 1013904223;
        int i = this.randValue >> 2;
        return new BlockPos(x + (i & 15), y + (i >> 16 & yMask), z + (i >> 8 & 15));
    }

    public boolean noSave() {
        return false;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    private double maxEntityRadius = 2.0D;
    @Override
    public double getMaxEntityRadius() {
        return maxEntityRadius;
    }
    @Override
    public double increaseMaxEntityRadius(double value) {
        if (value > maxEntityRadius)
            maxEntityRadius = value;
        return maxEntityRadius;
    }

    public final boolean isDebug() {
        return this.isDebug;
    }

    protected abstract LevelEntityGetter<Entity> getEntities();

    @Override
    public long nextSubTickCount() {
        return this.subTickCount++;
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    public DamageSources damageSources() {
        return this.damageSources;
    }

    public abstract PotionBrewing potionBrewing();

    public abstract FuelValues fuelValues();

    public int getClientLeafTintColor(BlockPos pos) {
        return 0;
    }

    public PalettedContainerFactory palettedContainerFactory() {
        return this.palettedContainerFactory;
    }

    public static enum ExplosionInteraction implements StringRepresentable {
        NONE("none"),
        BLOCK("block"),
        MOB("mob"),
        TNT("tnt"),
        TRIGGER("trigger");

        public static final Codec<Level.ExplosionInteraction> CODEC = StringRepresentable.fromEnum(Level.ExplosionInteraction::values);
        private final String id;

        private ExplosionInteraction(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }
    }

    // Neo: Variable day time code

    @org.jetbrains.annotations.ApiStatus.Internal
    public abstract void setDayTimeFraction(float dayTimeFraction);

    @org.jetbrains.annotations.ApiStatus.Internal
    public abstract float getDayTimeFraction();

    /**
     * Returns the current ratio between game ticks and clock ticks. If this value is negative, no
     * speed has been set and those two are coupled 1:1 (i.e. vanilla mode).
     */
    public abstract float getDayTimePerTick();

    /**
     * DO NOT CALL.
     * <p>
     * Use {@link net.minecraft.server.level.ServerLevel#setDayTimePerTick(float)} instead.
     */
    public abstract void setDayTimePerTick(float dayTimePerTick);

    // advances the fractional daytime, returns the integer part of it
    @org.jetbrains.annotations.ApiStatus.Internal
    protected long advanceDaytime() {
        if (getDayTimePerTick() < 0) {
            return 1L; // avoid doing math (and rounding errors) if no speed has been set
        }
        float dayTimeStep = getDayTimeFraction() + getDayTimePerTick();
        long result = (long)dayTimeStep;
        setDayTimeFraction(dayTimeStep - result);
        return result;
    }
}
