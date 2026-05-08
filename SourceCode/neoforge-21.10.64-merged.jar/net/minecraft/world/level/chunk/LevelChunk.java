package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.debug.DebugStructureInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.slf4j.Logger;

public class LevelChunk extends ChunkAccess implements DebugValueSource, net.neoforged.neoforge.attachment.IAttachmentHolder {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final TickingBlockEntity NULL_TICKER = new TickingBlockEntity() {
        @Override
        public void tick() {
        }

        @Override
        public boolean isRemoved() {
            return true;
        }

        @Override
        public BlockPos getPos() {
            return BlockPos.ZERO;
        }

        @Override
        public String getType() {
            return "<null>";
        }
    };
    private final Map<BlockPos, LevelChunk.RebindableTickingBlockEntityWrapper> tickersInLevel = Maps.newHashMap();
    private boolean loaded;
    final Level level;
    @Nullable
    private Supplier<FullChunkStatus> fullStatus;
    @Nullable
    private LevelChunk.PostLoadProcessor postLoad;
    private final Int2ObjectMap<GameEventListenerRegistry> gameEventListenerRegistrySections;
    private final LevelChunkTicks<Block> blockTicks;
    private final LevelChunkTicks<Fluid> fluidTicks;
    private LevelChunk.UnsavedListener unsavedListener = p_381697_ -> {};

    public LevelChunk(Level level, ChunkPos pos) {
        this(level, pos, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, null, null, null);
    }

    public LevelChunk(
        Level level,
        ChunkPos pos,
        UpgradeData data,
        LevelChunkTicks<Block> blockTicks,
        LevelChunkTicks<Fluid> fluidTicks,
        long inhabitedTime,
        @Nullable LevelChunkSection[] sections,
        @Nullable LevelChunk.PostLoadProcessor postLoad,
        @Nullable BlendingData blendingData
    ) {
        super(pos, data, level, level.palettedContainerFactory(), inhabitedTime, sections, blendingData);
        this.level = level;
        this.gameEventListenerRegistrySections = new Int2ObjectOpenHashMap<>();

        for (Heightmap.Types heightmap$types : Heightmap.Types.values()) {
            if (ChunkStatus.FULL.heightmapsAfter().contains(heightmap$types)) {
                this.heightmaps.put(heightmap$types, new Heightmap(this, heightmap$types));
            }
        }

        this.postLoad = postLoad;
        this.blockTicks = blockTicks;
        this.fluidTicks = fluidTicks;
    }

    public LevelChunk(ServerLevel level, ProtoChunk chunk, @Nullable LevelChunk.PostLoadProcessor postLoad) {
        this(
            level,
            chunk.getPos(),
            chunk.getUpgradeData(),
            chunk.unpackBlockTicks(),
            chunk.unpackFluidTicks(),
            chunk.getInhabitedTime(),
            chunk.getSections(),
            postLoad,
            chunk.getBlendingData()
        );
        if (!Collections.disjoint(chunk.pendingBlockEntities.keySet(), chunk.blockEntities.keySet())) {
            LOGGER.error("Chunk at {} contains duplicated block entities", chunk.getPos());
        }

        for (BlockEntity blockentity : chunk.getBlockEntities().values()) {
            this.setBlockEntity(blockentity);
        }

        this.pendingBlockEntities.putAll(chunk.getBlockEntityNbts());

        for (int i = 0; i < chunk.getPostProcessing().length; i++) {
            this.postProcessing[i] = chunk.getPostProcessing()[i];
        }

        this.setAllStarts(chunk.getAllStarts());
        this.setAllReferences(chunk.getAllReferences());

        net.neoforged.neoforge.attachment.AttachmentInternals.copyChunkAttachmentsOnPromotion(level.registryAccess(), chunk.getAttachmentHolder(), this.getAttachmentHolder());
        for (Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
            if (ChunkStatus.FULL.heightmapsAfter().contains(entry.getKey())) {
                this.setHeightmap(entry.getKey(), entry.getValue().getRawData());
            }
        }

        this.skyLightSources = chunk.skyLightSources;
        this.setLightCorrect(chunk.isLightCorrect());
        this.markUnsaved();
    }

    public void setUnsavedListener(LevelChunk.UnsavedListener unsavedListener) {
        this.unsavedListener = unsavedListener;
        if (this.isUnsaved()) {
            unsavedListener.setUnsaved(this.chunkPos);
        }
    }

    @Override
    public void markUnsaved() {
        boolean flag = this.isUnsaved();
        super.markUnsaved();
        if (!flag) {
            this.unsavedListener.setUnsaved(this.chunkPos);
        }
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public ChunkAccess.PackedTicks getTicksForSerialization(long gametime) {
        return new ChunkAccess.PackedTicks(this.blockTicks.pack(gametime), this.fluidTicks.pack(gametime));
    }

    @Override
    public GameEventListenerRegistry getListenerRegistry(int sectionY) {
        return this.level instanceof ServerLevel serverlevel
            ? this.gameEventListenerRegistrySections
                .computeIfAbsent(sectionY, p_281221_ -> new EuclideanGameEventListenerRegistry(serverlevel, sectionY, this::removeGameEventListenerRegistry))
            : super.getListenerRegistry(sectionY);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        if (this.level.isDebug()) {
            BlockState blockstate = null;
            if (j == 60) {
                blockstate = Blocks.BARRIER.defaultBlockState();
            }

            if (j == 70) {
                blockstate = DebugLevelSource.getBlockStateFor(i, k);
            }

            return blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
        } else {
            try {
                int l = this.getSectionIndex(j);
                if (l >= 0 && l < this.sections.length) {
                    LevelChunkSection levelchunksection = this.sections[l];
                    if (!levelchunksection.hasOnlyAir()) {
                        return levelchunksection.getBlockState(i & 15, j & 15, k & 15);
                    }
                }

                return Blocks.AIR.defaultBlockState();
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting block state");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
                crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, i, j, k));
                throw new ReportedException(crashreport);
            }
        }
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
    }

    public FluidState getFluidState(int x, int y, int z) {
        try {
            int i = this.getSectionIndex(y);
            if (i >= 0 && i < this.sections.length) {
                LevelChunkSection levelchunksection = this.sections[i];
                if (!levelchunksection.hasOnlyAir()) {
                    return levelchunksection.getFluidState(x & 15, y & 15, z & 15);
                }
            }

            return Fluids.EMPTY.defaultFluidState();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting fluid state");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
            crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, x, y, z));
            throw new ReportedException(crashreport);
        }
    }

    @Nullable
    @Override
    public BlockState setBlockState(BlockPos pos, BlockState state, int flags) {
        int i = pos.getY();
        LevelChunkSection levelchunksection = this.getSection(this.getSectionIndex(i));
        boolean flag = levelchunksection.hasOnlyAir();
        if (flag && state.isAir()) {
            return null;
        } else {
            int j = pos.getX() & 15;
            int k = i & 15;
            int l = pos.getZ() & 15;
            BlockState blockstate = levelchunksection.setBlockState(j, k, l, state);
            if (blockstate == state) {
                return null;
            } else {
                Block block = state.getBlock();
                this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(j, i, l, state);
                this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(j, i, l, state);
                this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(j, i, l, state);
                this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(j, i, l, state);
                boolean flag1 = levelchunksection.hasOnlyAir();
                if (flag != flag1) {
                    this.level.getChunkSource().getLightEngine().updateSectionStatus(pos, flag1);
                    this.level.getChunkSource().onSectionEmptinessChanged(this.chunkPos.x, SectionPos.blockToSectionCoord(i), this.chunkPos.z, flag1);
                }

                if (LightEngine.hasDifferentLightProperties(this, pos, blockstate, state)) {
                    ProfilerFiller profilerfiller = Profiler.get();
                    profilerfiller.push("updateSkyLightSources");
                    this.skyLightSources.update(this, j, i, l);
                    profilerfiller.popPush("queueCheckLight");
                    this.level.getChunkSource().getLightEngine().checkBlock(pos);
                    profilerfiller.pop();
                }

                boolean flag4 = !blockstate.is(block);
                boolean flag2 = (flags & 64) != 0;
                boolean flag3 = (flags & 256) == 0;
                if (flag4 && blockstate.hasBlockEntity() && !state.shouldChangedStateKeepBlockEntity(blockstate)) {
                    if (!this.level.isClientSide() && flag3) {
                        BlockEntity blockentity = this.level.getBlockEntity(pos);
                        if (blockentity != null) {
                            blockentity.preRemoveSideEffects(pos, blockstate);
                        }
                    }

                    this.removeBlockEntity(pos);
                }

                if ((flag4 || block instanceof BaseRailBlock) && this.level instanceof ServerLevel serverlevel && ((flags & 1) != 0 || flag2)) {
                    blockstate.affectNeighborsAfterRemoval(serverlevel, pos, flag2);
                }

                if (!levelchunksection.getBlockState(j, k, l).is(block)) {
                    return null;
                } else {
                    if (!this.level.isClientSide() && !this.level.captureBlockSnapshots && (flags & 512) == 0) {
                        state.onPlace(this.level, pos, blockstate, flag2);
                    }

                    if (state.hasBlockEntity()) {
                        BlockEntity blockentity1 = this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
                        if (blockentity1 != null && !blockentity1.isValidBlockState(state)) {
                            LOGGER.warn(
                                "Found mismatched block entity @ {}: type = {}, state = {}",
                                pos,
                                blockentity1.getType().builtInRegistryHolder().key().location(),
                                state
                            );
                            this.removeBlockEntity(pos);
                            blockentity1 = null;
                        }

                        if (blockentity1 == null) {
                            blockentity1 = ((EntityBlock)block).newBlockEntity(pos, state);
                            if (blockentity1 != null) {
                                this.addAndRegisterBlockEntity(blockentity1);
                            }
                        } else {
                            blockentity1.setBlockState(state);
                            this.updateBlockEntityTicker(blockentity1);
                        }
                    }

                    this.markUnsaved();
                    return blockstate;
                }
            }
        }
    }

    @Deprecated
    @Override
    public void addEntity(Entity entity) {
    }

    @Nullable
    private BlockEntity createBlockEntity(BlockPos pos) {
        BlockState blockstate = this.getBlockState(pos);
        return !blockstate.hasBlockEntity() ? null : ((EntityBlock)blockstate.getBlock()).newBlockEntity(pos, blockstate);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos, LevelChunk.EntityCreationType creationType) {
        BlockEntity blockentity = this.blockEntities.get(pos);
        if (blockentity != null && blockentity.isRemoved()) {
            blockEntities.remove(pos);
            blockentity = null;
        }
        if (blockentity == null) {
            CompoundTag compoundtag = this.pendingBlockEntities.remove(pos);
            if (compoundtag != null) {
                BlockEntity blockentity1 = this.promotePendingBlockEntity(pos, compoundtag);
                if (blockentity1 != null) {
                    return blockentity1;
                }
            }
        }

        if (blockentity == null) {
            if (creationType == LevelChunk.EntityCreationType.IMMEDIATE) {
                blockentity = this.createBlockEntity(pos);
                if (blockentity != null) {
                    this.addAndRegisterBlockEntity(blockentity);
                }
            }
        }

        return blockentity;
    }

    public void addAndRegisterBlockEntity(BlockEntity blockEntity) {
        this.setBlockEntity(blockEntity);
        if (this.isInLevel()) {
            if (this.level instanceof ServerLevel serverlevel) {
                this.addGameEventListener(blockEntity, serverlevel);
            }

            this.level.onBlockEntityAdded(blockEntity);
            this.updateBlockEntityTicker(blockEntity);
            this.level.addFreshBlockEntities(java.util.List.of(blockEntity));
        }
    }

    private boolean isInLevel() {
        return this.loaded || this.level.isClientSide();
    }

    boolean isTicking(BlockPos pos) {
        if (!this.level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        } else {
            return !(this.level instanceof ServerLevel serverlevel)
                ? true
                : this.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING) && serverlevel.areEntitiesLoaded(ChunkPos.asLong(pos));
        }
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        BlockPos blockpos = blockEntity.getBlockPos();
        BlockState blockstate = this.getBlockState(blockpos);
        if (!blockstate.hasBlockEntity()) {
            LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", blockEntity, blockpos, blockstate);
        } else {
            BlockState blockstate1 = blockEntity.getBlockState();
            if (blockstate != blockstate1) {
                if (!blockEntity.getType().isValid(blockstate)) {
                    LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", blockEntity, blockpos, blockstate);
                    return;
                }

                if (blockstate.getBlock() != blockstate1.getBlock()) {
                    LOGGER.warn("Block state mismatch on block entity {} in position {}, {} != {}, updating", blockEntity, blockpos, blockstate, blockstate1);
                }

                blockEntity.setBlockState(blockstate);
            }

            blockEntity.setLevel(this.level);
            blockEntity.clearRemoved();
            BlockEntity blockentity = this.blockEntities.put(blockpos.immutable(), blockEntity);
            if (blockentity != null && blockentity != blockEntity) {
                blockentity.setRemoved();
                auxLightManager.removeLightAt(blockpos);
            }
        }
    }

    @Nullable
    @Override
    public CompoundTag getBlockEntityNbtForSaving(BlockPos pos, HolderLookup.Provider registries) {
        BlockEntity blockentity = this.getBlockEntity(pos);
        if (blockentity != null && !blockentity.isRemoved()) {
            try {
            CompoundTag compoundtag1 = blockentity.saveWithFullMetadata(this.level.registryAccess());
            compoundtag1.putBoolean("keepPacked", false);
            return compoundtag1;
            } catch (Exception e) {
                LOGGER.error("A BlockEntity type {} has thrown an exception trying to write state. It will not persist, Report this to the mod author", blockentity.getClass().getName(), e);
                return null;
            }
        } else {
            CompoundTag compoundtag = this.pendingBlockEntities.get(pos);
            if (compoundtag != null) {
                compoundtag = compoundtag.copy();
                compoundtag.putBoolean("keepPacked", true);
            }

            return compoundtag;
        }
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
        if (this.isInLevel()) {
            BlockEntity blockentity = this.blockEntities.remove(pos);
            if (blockentity != null) {
                if (this.level instanceof ServerLevel serverlevel) {
                    this.removeGameEventListener(blockentity, serverlevel);
                    serverlevel.debugSynchronizers().dropBlockEntity(pos);
                }

                blockentity.setRemoved();
                auxLightManager.removeLightAt(pos);
            }
        }

        this.removeBlockEntityTicker(pos);
    }

    private <T extends BlockEntity> void removeGameEventListener(T blockEntity, ServerLevel level) {
        Block block = blockEntity.getBlockState().getBlock();
        if (block instanceof EntityBlock) {
            GameEventListener gameeventlistener = ((EntityBlock)block).getListener(level, blockEntity);
            if (gameeventlistener != null) {
                int i = SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY());
                GameEventListenerRegistry gameeventlistenerregistry = this.getListenerRegistry(i);
                gameeventlistenerregistry.unregister(gameeventlistener);
            }
        }
    }

    private void removeGameEventListenerRegistry(int sectionY) {
        this.gameEventListenerRegistrySections.remove(sectionY);
    }

    private void removeBlockEntityTicker(BlockPos pos) {
        LevelChunk.RebindableTickingBlockEntityWrapper levelchunk$rebindabletickingblockentitywrapper = this.tickersInLevel.remove(pos);
        if (levelchunk$rebindabletickingblockentitywrapper != null) {
            levelchunk$rebindabletickingblockentitywrapper.rebind(NULL_TICKER);
        }
    }

    public void runPostLoad() {
        if (this.postLoad != null) {
            this.postLoad.run(this);
            this.postLoad = null;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public void replaceWithPacketData(
        FriendlyByteBuf buffer, Map<Heightmap.Types, long[]> heightmaps, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> outputTagConsumer
    ) {
        this.clearAllBlockEntities();

        for (LevelChunkSection levelchunksection : this.sections) {
            levelchunksection.read(buffer);
        }

        heightmaps.forEach(this::setHeightmap);
        this.initializeLightSources();

        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
            outputTagConsumer.accept(
                (p_421432_, p_421433_, p_421434_) -> {
                    BlockEntity blockentity = this.getBlockEntity(p_421432_, LevelChunk.EntityCreationType.IMMEDIATE);
                    if (blockentity != null && p_421434_ != null && blockentity.getType() == p_421433_) {
                        blockentity.handleUpdateTag(
                            TagValueInput.create(problemreporter$scopedcollector.forChild(blockentity.problemPath()), this.level.registryAccess(), p_421434_)
                        );
                    }
                }
            );
        }
    }

    public void replaceBiomes(FriendlyByteBuf buffer) {
        for (LevelChunkSection levelchunksection : this.sections) {
            levelchunksection.readBiomes(buffer);
        }
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public Level getLevel() {
        return this.level;
    }

    public Map<BlockPos, BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public void postProcessGeneration(ServerLevel level) {
        ChunkPos chunkpos = this.getPos();

        for (int i = 0; i < this.postProcessing.length; i++) {
            if (this.postProcessing[i] != null) {
                for (Short oshort : this.postProcessing[i]) {
                    BlockPos blockpos = ProtoChunk.unpackOffsetCoordinates(oshort, this.getSectionYFromSectionIndex(i), chunkpos);
                    BlockState blockstate = this.getBlockState(blockpos);
                    FluidState fluidstate = blockstate.getFluidState();
                    if (!fluidstate.isEmpty()) {
                        fluidstate.tick(level, blockpos, blockstate);
                    }

                    if (!(blockstate.getBlock() instanceof LiquidBlock)) {
                        BlockState blockstate1 = Block.updateFromNeighbourShapes(blockstate, level, blockpos);
                        if (blockstate1 != blockstate) {
                            level.setBlock(blockpos, blockstate1, 276);
                        }
                    }
                }

                this.postProcessing[i].clear();
            }
        }

        for (BlockPos blockpos1 : ImmutableList.copyOf(this.pendingBlockEntities.keySet())) {
            this.getBlockEntity(blockpos1);
        }

        this.pendingBlockEntities.clear();
        this.upgradeData.upgrade(this);
    }

    @Nullable
    private BlockEntity promotePendingBlockEntity(BlockPos pos, CompoundTag tag) {
        BlockState blockstate = this.getBlockState(pos);
        BlockEntity blockentity;
        if ("DUMMY".equals(tag.getStringOr("id", ""))) {
            if (blockstate.hasBlockEntity()) {
                blockentity = ((EntityBlock)blockstate.getBlock()).newBlockEntity(pos, blockstate);
            } else {
                blockentity = null;
                LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", pos, blockstate);
            }
        } else {
            blockentity = BlockEntity.loadStatic(pos, blockstate, tag, this.level.registryAccess());
        }

        if (blockentity != null) {
            blockentity.setLevel(this.level);
            this.addAndRegisterBlockEntity(blockentity);
        } else {
            LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", blockstate, pos);
        }

        return blockentity;
    }

    public void unpackTicks(long pos) {
        this.blockTicks.unpack(pos);
        this.fluidTicks.unpack(pos);
    }

    public void registerTickContainerInLevel(ServerLevel level) {
        level.getBlockTicks().addContainer(this.chunkPos, this.blockTicks);
        level.getFluidTicks().addContainer(this.chunkPos, this.fluidTicks);
    }

    public void unregisterTickContainerFromLevel(ServerLevel level) {
        level.getBlockTicks().removeContainer(this.chunkPos);
        level.getFluidTicks().removeContainer(this.chunkPos);
    }

    @Override
    public void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration) {
        if (!this.getAllStarts().isEmpty()) {
            registration.register(DebugSubscriptions.STRUCTURES, () -> {
                List<DebugStructureInfo> list = new ArrayList<>();

                for (StructureStart structurestart : this.getAllStarts().values()) {
                    BoundingBox boundingbox = structurestart.getBoundingBox();
                    List<StructurePiece> list1 = structurestart.getPieces();
                    List<DebugStructureInfo.Piece> list2 = new ArrayList<>(list1.size());

                    for (int i = 0; i < list1.size(); i++) {
                        boolean flag = i == 0;
                        list2.add(new DebugStructureInfo.Piece(list1.get(i).getBoundingBox(), flag));
                    }

                    list.add(new DebugStructureInfo(boundingbox, list2));
                }

                return list;
            });
        }

        registration.register(DebugSubscriptions.RAIDS, () -> level.getRaids().getRaidCentersInChunk(this.chunkPos));
    }

    @Override
    public ChunkStatus getPersistedStatus() {
        return ChunkStatus.FULL;
    }

    public FullChunkStatus getFullStatus() {
        return this.fullStatus == null ? FullChunkStatus.FULL : this.fullStatus.get();
    }

    public void setFullStatus(Supplier<FullChunkStatus> fullStatus) {
        this.fullStatus = fullStatus;
    }

    public void clearAllBlockEntities() {
        this.blockEntities.values().forEach(BlockEntity::onChunkUnloaded);
        this.blockEntities.values().forEach(BlockEntity::setRemoved);
        this.blockEntities.clear();
        this.tickersInLevel.values().forEach(p_187966_ -> p_187966_.rebind(NULL_TICKER));
        this.tickersInLevel.clear();
    }

    public void registerAllBlockEntitiesAfterLevelLoad() {
        this.level.addFreshBlockEntities(this.blockEntities.values());
        this.blockEntities.values().forEach(p_427220_ -> {
            if (this.level instanceof ServerLevel serverlevel) {
                this.addGameEventListener(p_427220_, serverlevel);
            }

            this.level.onBlockEntityAdded(p_427220_);
            this.updateBlockEntityTicker(p_427220_);
        });
    }

    private <T extends BlockEntity> void addGameEventListener(T blockEntity, ServerLevel level) {
        Block block = blockEntity.getBlockState().getBlock();
        if (block instanceof EntityBlock) {
            GameEventListener gameeventlistener = ((EntityBlock)block).getListener(level, blockEntity);
            if (gameeventlistener != null) {
                this.getListenerRegistry(SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY())).register(gameeventlistener);
            }
        }
    }

    private <T extends BlockEntity> void updateBlockEntityTicker(T blockEntity) {
        BlockState blockstate = blockEntity.getBlockState();
        BlockEntityTicker<T> blockentityticker = blockstate.getTicker(this.level, (BlockEntityType<T>)blockEntity.getType());
        if (blockentityticker == null) {
            this.removeBlockEntityTicker(blockEntity.getBlockPos());
        } else {
            this.tickersInLevel
                .compute(
                    blockEntity.getBlockPos(),
                    (p_386425_, p_386426_) -> {
                        TickingBlockEntity tickingblockentity = this.createTicker(blockEntity, blockentityticker);
                        if (p_386426_ != null) {
                            p_386426_.rebind(tickingblockentity);
                            return (LevelChunk.RebindableTickingBlockEntityWrapper)p_386426_;
                        } else if (this.isInLevel()) {
                            LevelChunk.RebindableTickingBlockEntityWrapper levelchunk$rebindabletickingblockentitywrapper = new LevelChunk.RebindableTickingBlockEntityWrapper(
                                tickingblockentity
                            );
                            this.level.addBlockEntityTicker(levelchunk$rebindabletickingblockentitywrapper);
                            return levelchunk$rebindabletickingblockentitywrapper;
                        } else {
                            return null;
                        }
                    }
                );
        }
    }

    private <T extends BlockEntity> TickingBlockEntity createTicker(T blockEntity, BlockEntityTicker<T> ticker) {
        return new LevelChunk.BoundTickingBlockEntity<>(blockEntity, ticker);
    }

    // Neo: Threadsafe lighting system for BlockEntities that change lighting based on dynamic data changing.
    private final net.neoforged.neoforge.common.world.LevelChunkAuxiliaryLightManager auxLightManager = new net.neoforged.neoforge.common.world.LevelChunkAuxiliaryLightManager(this);

    @Override
    public net.neoforged.neoforge.common.world.LevelChunkAuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
        return auxLightManager;
    }

    @Override
    public final void syncData(net.neoforged.neoforge.attachment.AttachmentType<?> type) {
        net.neoforged.neoforge.attachment.AttachmentSync.syncChunkUpdate(this, getAttachmentHolder(), type);
    }

    class BoundTickingBlockEntity<T extends BlockEntity> implements TickingBlockEntity {
        private final T blockEntity;
        private final BlockEntityTicker<T> ticker;
        private boolean loggedInvalidBlockState;

        BoundTickingBlockEntity(T blockEntity, BlockEntityTicker<T> ticker) {
            this.blockEntity = blockEntity;
            this.ticker = ticker;
        }

        @Override
        public void tick() {
            if (!this.blockEntity.isRemoved() && this.blockEntity.hasLevel()) {
                BlockPos blockpos = this.blockEntity.getBlockPos();
                if (LevelChunk.this.isTicking(blockpos)) {
                    try {
                        ProfilerFiller profilerfiller = Profiler.get();
                        net.neoforged.neoforge.server.timings.TimeTracker.BLOCK_ENTITY_UPDATE.trackStart(blockEntity);
                        profilerfiller.push(this::getType);
                        BlockState blockstate = LevelChunk.this.getBlockState(blockpos);
                        if (this.blockEntity.getType().isValid(blockstate)) {
                            this.ticker.tick(LevelChunk.this.level, this.blockEntity.getBlockPos(), blockstate, this.blockEntity);
                            this.loggedInvalidBlockState = false;
                        } else if (!this.loggedInvalidBlockState) {
                            this.loggedInvalidBlockState = true;
                            LevelChunk.LOGGER
                                .warn(
                                    "Block entity {} @ {} state {} invalid for ticking:",
                                    LogUtils.defer(this::getType),
                                    LogUtils.defer(this::getPos),
                                    blockstate
                                );
                        }

                        profilerfiller.pop();
                    } catch (Throwable throwable) {
                        CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking block entity");
                        CrashReportCategory crashreportcategory = crashreport.addCategory("Block entity being ticked");
                        this.blockEntity.fillCrashReportCategory(crashreportcategory);

                        if (net.neoforged.neoforge.common.config.NeoForgeServerConfig.INSTANCE.removeErroringBlockEntities.get()) {
                            LOGGER.error("{}", crashreport.getFriendlyReport(net.minecraft.ReportType.CRASH));
                            blockEntity.setRemoved();
                            LevelChunk.this.removeBlockEntity(blockEntity.getBlockPos());
                        } else
                        throw new ReportedException(crashreport);
                    } finally {
                        net.neoforged.neoforge.server.timings.TimeTracker.BLOCK_ENTITY_UPDATE.trackEnd(blockEntity);
                    }
                }
            }
        }

        @Override
        public boolean isRemoved() {
            return this.blockEntity.isRemoved();
        }

        @Override
        public BlockPos getPos() {
            return this.blockEntity.getBlockPos();
        }

        @Override
        public String getType() {
            return BlockEntityType.getKey(this.blockEntity.getType()).toString();
        }

        @Override
        public String toString() {
            return "Level ticker for " + this.getType() + "@" + this.getPos();
        }
    }

    public static enum EntityCreationType {
        IMMEDIATE,
        QUEUED,
        CHECK;
    }

    @FunctionalInterface
    public interface PostLoadProcessor {
        void run(LevelChunk chunk);
    }

    static class RebindableTickingBlockEntityWrapper implements TickingBlockEntity {
        private TickingBlockEntity ticker;

        RebindableTickingBlockEntityWrapper(TickingBlockEntity ticker) {
            this.ticker = ticker;
        }

        void rebind(TickingBlockEntity ticker) {
            this.ticker = ticker;
        }

        @Override
        public void tick() {
            this.ticker.tick();
        }

        @Override
        public boolean isRemoved() {
            return this.ticker.isRemoved();
        }

        @Override
        public BlockPos getPos() {
            return this.ticker.getPos();
        }

        @Override
        public String getType() {
            return this.ticker.getType();
        }

        @Override
        public String toString() {
            return this.ticker + " <wrapped>";
        }
    }

    @FunctionalInterface
    public interface UnsavedListener {
        void setUnsaved(ChunkPos chunkPos);
    }
}
