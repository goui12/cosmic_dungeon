package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.slf4j.Logger;

public class StructureTemplate {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String PALETTE_TAG = "palette";
    public static final String PALETTE_LIST_TAG = "palettes";
    public static final String ENTITIES_TAG = "entities";
    public static final String BLOCKS_TAG = "blocks";
    public static final String BLOCK_TAG_POS = "pos";
    public static final String BLOCK_TAG_STATE = "state";
    public static final String BLOCK_TAG_NBT = "nbt";
    public static final String ENTITY_TAG_POS = "pos";
    public static final String ENTITY_TAG_BLOCKPOS = "blockPos";
    public static final String ENTITY_TAG_NBT = "nbt";
    public static final String SIZE_TAG = "size";
    private final List<StructureTemplate.Palette> palettes = Lists.newArrayList();
    private final List<StructureTemplate.StructureEntityInfo> entityInfoList = Lists.newArrayList();
    private Vec3i size = Vec3i.ZERO;
    private String author = "?";

    public Vec3i getSize() {
        return this.size;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthor() {
        return this.author;
    }

    public void fillFromWorld(Level level, BlockPos pos, Vec3i size, boolean includeEntities, List<Block> ignoredBlocks) {
        if (size.getX() >= 1 && size.getY() >= 1 && size.getZ() >= 1) {
            BlockPos blockpos = pos.offset(size).offset(-1, -1, -1);
            List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
            List<StructureTemplate.StructureBlockInfo> list1 = Lists.newArrayList();
            List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
            BlockPos blockpos1 = new BlockPos(
                Math.min(pos.getX(), blockpos.getX()), Math.min(pos.getY(), blockpos.getY()), Math.min(pos.getZ(), blockpos.getZ())
            );
            BlockPos blockpos2 = new BlockPos(
                Math.max(pos.getX(), blockpos.getX()), Math.max(pos.getY(), blockpos.getY()), Math.max(pos.getZ(), blockpos.getZ())
            );
            this.size = size;

            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
                for (BlockPos blockpos3 : BlockPos.betweenClosed(blockpos1, blockpos2)) {
                    BlockPos blockpos4 = blockpos3.subtract(blockpos1);
                    BlockState blockstate = level.getBlockState(blockpos3);
                    if (!ignoredBlocks.stream().anyMatch(blockstate::is)) {
                        BlockEntity blockentity = level.getBlockEntity(blockpos3);
                        StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo;
                        if (blockentity != null) {
                            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, level.registryAccess());
                            blockentity.saveWithId(tagvalueoutput);
                            structuretemplate$structureblockinfo = new StructureTemplate.StructureBlockInfo(blockpos4, blockstate, tagvalueoutput.buildResult());
                        } else {
                            structuretemplate$structureblockinfo = new StructureTemplate.StructureBlockInfo(blockpos4, blockstate, null);
                        }

                        addToLists(structuretemplate$structureblockinfo, list, list1, list2);
                    }
                }

                List<StructureTemplate.StructureBlockInfo> list3 = buildInfoList(list, list1, list2);
                this.palettes.clear();
                this.palettes.add(new StructureTemplate.Palette(list3));
                if (includeEntities) {
                    this.fillEntityList(level, blockpos1, blockpos2, problemreporter$scopedcollector);
                } else {
                    this.entityInfoList.clear();
                }
            }
        }
    }

    private static void addToLists(
        StructureTemplate.StructureBlockInfo blockInfo,
        List<StructureTemplate.StructureBlockInfo> normalBlocks,
        List<StructureTemplate.StructureBlockInfo> blocksWithNbt,
        List<StructureTemplate.StructureBlockInfo> blocksWithSpecialShape
    ) {
        if (blockInfo.nbt != null) {
            blocksWithNbt.add(blockInfo);
        } else if (!blockInfo.state.getBlock().hasDynamicShape() && blockInfo.state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            normalBlocks.add(blockInfo);
        } else {
            blocksWithSpecialShape.add(blockInfo);
        }
    }

    private static List<StructureTemplate.StructureBlockInfo> buildInfoList(
        List<StructureTemplate.StructureBlockInfo> normalBlocks,
        List<StructureTemplate.StructureBlockInfo> blocksWithNbt,
        List<StructureTemplate.StructureBlockInfo> blocksWithSpecialShape
    ) {
        Comparator<StructureTemplate.StructureBlockInfo> comparator = Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(
                p_74641_ -> p_74641_.pos.getY()
            )
            .thenComparingInt(p_74637_ -> p_74637_.pos.getX())
            .thenComparingInt(p_74572_ -> p_74572_.pos.getZ());
        normalBlocks.sort(comparator);
        blocksWithSpecialShape.sort(comparator);
        blocksWithNbt.sort(comparator);
        List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
        list.addAll(normalBlocks);
        list.addAll(blocksWithSpecialShape);
        list.addAll(blocksWithNbt);
        return list;
    }

    private void fillEntityList(Level level, BlockPos pos1, BlockPos pos2, ProblemReporter problemReporter) {
        List<Entity> list = level.getEntitiesOfClass(
            Entity.class, AABB.encapsulatingFullBlocks(pos1, pos2), p_74499_ -> !(p_74499_ instanceof Player)
        );
        this.entityInfoList.clear();

        for (Entity entity : list) {
            Vec3 vec3 = new Vec3(entity.getX() - pos1.getX(), entity.getY() - pos1.getY(), entity.getZ() - pos1.getZ());
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemReporter.forChild(entity.problemPath()), entity.registryAccess());
            entity.save(tagvalueoutput);
            BlockPos blockpos;
            if (entity instanceof Painting painting) {
                blockpos = painting.getPos().subtract(pos1);
            } else {
                blockpos = BlockPos.containing(vec3);
            }

            this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(vec3, blockpos, tagvalueoutput.buildResult().copy()));
        }
    }

    public List<StructureTemplate.StructureBlockInfo> filterBlocks(BlockPos pos, StructurePlaceSettings settings, Block block) {
        return this.filterBlocks(pos, settings, block, true);
    }

    public List<StructureTemplate.JigsawBlockInfo> getJigsaws(BlockPos pos, Rotation rotation) {
        if (this.palettes.isEmpty()) {
            return new ArrayList<>();
        } else {
            StructurePlaceSettings structureplacesettings = new StructurePlaceSettings().setRotation(rotation);
            List<StructureTemplate.JigsawBlockInfo> list = structureplacesettings.getRandomPalette(this.palettes, pos).jigsaws();
            List<StructureTemplate.JigsawBlockInfo> list1 = new ArrayList<>(list.size());

            for (StructureTemplate.JigsawBlockInfo structuretemplate$jigsawblockinfo : list) {
                StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo = structuretemplate$jigsawblockinfo.info;
                list1.add(
                    structuretemplate$jigsawblockinfo.withInfo(
                        new StructureTemplate.StructureBlockInfo(
                            calculateRelativePosition(structureplacesettings, structuretemplate$structureblockinfo.pos()).offset(pos),
                            structuretemplate$structureblockinfo.state.rotate(structureplacesettings.getRotation()),
                            structuretemplate$structureblockinfo.nbt
                        )
                    )
                );
            }

            return list1;
        }
    }

    public ObjectArrayList<StructureTemplate.StructureBlockInfo> filterBlocks(
        BlockPos pos, StructurePlaceSettings settings, Block block, boolean relativePosition
    ) {
        ObjectArrayList<StructureTemplate.StructureBlockInfo> objectarraylist = new ObjectArrayList<>();
        BoundingBox boundingbox = settings.getBoundingBox();
        if (this.palettes.isEmpty()) {
            return objectarraylist;
        } else {
            for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : settings.getRandomPalette(this.palettes, pos)
                .blocks(block)) {
                BlockPos blockpos = relativePosition
                    ? calculateRelativePosition(settings, structuretemplate$structureblockinfo.pos).offset(pos)
                    : structuretemplate$structureblockinfo.pos;
                if (boundingbox == null || boundingbox.isInside(blockpos)) {
                    objectarraylist.add(
                        new StructureTemplate.StructureBlockInfo(
                            blockpos, structuretemplate$structureblockinfo.state.rotate(settings.getRotation()), structuretemplate$structureblockinfo.nbt
                        )
                    );
                }
            }

            return objectarraylist;
        }
    }

    public BlockPos calculateConnectedPosition(StructurePlaceSettings decorator, BlockPos start, StructurePlaceSettings settings, BlockPos end) {
        BlockPos blockpos = calculateRelativePosition(decorator, start);
        BlockPos blockpos1 = calculateRelativePosition(settings, end);
        return blockpos.subtract(blockpos1);
    }

    public static BlockPos calculateRelativePosition(StructurePlaceSettings decorator, BlockPos pos) {
        return transform(pos, decorator.getMirror(), decorator.getRotation(), decorator.getRotationPivot());
    }

    public static Vec3 transformedVec3d(StructurePlaceSettings placementIn, Vec3 pos) {
        return transform(pos, placementIn.getMirror(), placementIn.getRotation(), placementIn.getRotationPivot());
    }

    public boolean placeInWorld(
        ServerLevelAccessor serverLevel, BlockPos offset, BlockPos pos, StructurePlaceSettings settings, RandomSource random, int flags
    ) {
        if (this.palettes.isEmpty()) {
            return false;
        } else {
            List<StructureTemplate.StructureBlockInfo> list = settings.getRandomPalette(this.palettes, offset).blocks();
            if ((!list.isEmpty() || !settings.isIgnoreEntities() && !this.entityInfoList.isEmpty())
                && this.size.getX() >= 1
                && this.size.getY() >= 1
                && this.size.getZ() >= 1) {
                BoundingBox boundingbox = settings.getBoundingBox();
                List<BlockPos> list1 = Lists.newArrayListWithCapacity(settings.shouldApplyWaterlogging() ? list.size() : 0);
                List<BlockPos> list2 = Lists.newArrayListWithCapacity(settings.shouldApplyWaterlogging() ? list.size() : 0);
                List<Pair<BlockPos, CompoundTag>> list3 = Lists.newArrayListWithCapacity(list.size());
                int i = Integer.MAX_VALUE;
                int j = Integer.MAX_VALUE;
                int k = Integer.MAX_VALUE;
                int l = Integer.MIN_VALUE;
                int i1 = Integer.MIN_VALUE;
                int j1 = Integer.MIN_VALUE;
                List<StructureTemplate.StructureBlockInfo> list4 = processBlockInfos(serverLevel, offset, pos, settings, list, this);

                try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
                    for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : list4) {
                        BlockPos blockpos = structuretemplate$structureblockinfo.pos;
                        if (boundingbox == null || boundingbox.isInside(blockpos)) {
                            FluidState fluidstate = settings.shouldApplyWaterlogging() ? serverLevel.getFluidState(blockpos) : null;
                            BlockState blockstate = structuretemplate$structureblockinfo.state.mirror(settings.getMirror()).rotate(settings.getRotation());
                            if (structuretemplate$structureblockinfo.nbt != null) {
                                serverLevel.setBlock(blockpos, Blocks.BARRIER.defaultBlockState(), 820);
                            }

                            if (serverLevel.setBlock(blockpos, blockstate, flags)) {
                                i = Math.min(i, blockpos.getX());
                                j = Math.min(j, blockpos.getY());
                                k = Math.min(k, blockpos.getZ());
                                l = Math.max(l, blockpos.getX());
                                i1 = Math.max(i1, blockpos.getY());
                                j1 = Math.max(j1, blockpos.getZ());
                                list3.add(Pair.of(blockpos, structuretemplate$structureblockinfo.nbt));
                                if (structuretemplate$structureblockinfo.nbt != null) {
                                    BlockEntity blockentity = serverLevel.getBlockEntity(blockpos);
                                    if (blockentity != null) {
                                        if (!SharedConstants.DEBUG_STRUCTURE_EDIT_MODE && blockentity instanceof RandomizableContainer) {
                                            structuretemplate$structureblockinfo.nbt.putLong("LootTableSeed", random.nextLong());
                                        }

                                        blockentity.loadWithComponents(
                                            TagValueInput.create(
                                                problemreporter$scopedcollector.forChild(blockentity.problemPath()),
                                                serverLevel.registryAccess(),
                                                structuretemplate$structureblockinfo.nbt
                                            )
                                        );
                                    }
                                }

                                if (fluidstate != null) {
                                    if (blockstate.getFluidState().isSource()) {
                                        list2.add(blockpos);
                                    } else if (blockstate.getBlock() instanceof LiquidBlockContainer) {
                                        ((LiquidBlockContainer)blockstate.getBlock()).placeLiquid(serverLevel, blockpos, blockstate, fluidstate);
                                        if (!fluidstate.isSource()) {
                                            list1.add(blockpos);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    boolean flag = true;
                    Direction[] adirection = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

                    while (flag && !list1.isEmpty()) {
                        flag = false;
                        Iterator<BlockPos> iterator = list1.iterator();

                        while (iterator.hasNext()) {
                            BlockPos blockpos3 = iterator.next();
                            FluidState fluidstate2 = serverLevel.getFluidState(blockpos3);

                            for (int i2 = 0; i2 < adirection.length && !fluidstate2.isSource(); i2++) {
                                BlockPos blockpos1 = blockpos3.relative(adirection[i2]);
                                FluidState fluidstate1 = serverLevel.getFluidState(blockpos1);
                                if (fluidstate1.isSource() && !list2.contains(blockpos1)) {
                                    fluidstate2 = fluidstate1;
                                }
                            }

                            if (fluidstate2.isSource()) {
                                BlockState blockstate1 = serverLevel.getBlockState(blockpos3);
                                Block block = blockstate1.getBlock();
                                if (block instanceof LiquidBlockContainer) {
                                    ((LiquidBlockContainer)block).placeLiquid(serverLevel, blockpos3, blockstate1, fluidstate2);
                                    flag = true;
                                    iterator.remove();
                                }
                            }
                        }
                    }

                    if (i <= l) {
                        if (!settings.getKnownShape()) {
                            DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(l - i + 1, i1 - j + 1, j1 - k + 1);
                            int k1 = i;
                            int l1 = j;
                            int j2 = k;

                            for (Pair<BlockPos, CompoundTag> pair1 : list3) {
                                BlockPos blockpos2 = pair1.getFirst();
                                discretevoxelshape.fill(blockpos2.getX() - k1, blockpos2.getY() - l1, blockpos2.getZ() - j2);
                            }

                            updateShapeAtEdge(serverLevel, flags, discretevoxelshape, k1, l1, j2);
                        }

                        for (Pair<BlockPos, CompoundTag> pair : list3) {
                            BlockPos blockpos4 = pair.getFirst();
                            if (!settings.getKnownShape()) {
                                BlockState blockstate2 = serverLevel.getBlockState(blockpos4);
                                BlockState blockstate3 = Block.updateFromNeighbourShapes(blockstate2, serverLevel, blockpos4);
                                if (blockstate2 != blockstate3) {
                                    serverLevel.setBlock(blockpos4, blockstate3, flags & -2 | 16);
                                }

                                serverLevel.updateNeighborsAt(blockpos4, blockstate3.getBlock());
                            }

                            if (pair.getSecond() != null) {
                                BlockEntity blockentity1 = serverLevel.getBlockEntity(blockpos4);
                                if (blockentity1 != null) {
                                    blockentity1.setChanged();
                                }
                            }
                        }
                    }

                    if (!settings.isIgnoreEntities()) {
                        this.addEntitiesToWorld(serverLevel, offset, settings, problemreporter$scopedcollector);
                    }
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public static void updateShapeAtEdge(LevelAccessor level, int flags, DiscreteVoxelShape shape, BlockPos pos) {
        updateShapeAtEdge(level, flags, shape, pos.getX(), pos.getY(), pos.getZ());
    }

    public static void updateShapeAtEdge(LevelAccessor level, int flags, DiscreteVoxelShape shape, int x, int y, int z) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();
        shape.forAllFaces(
            (p_374018_, p_374019_, p_374020_, p_374021_) -> {
                blockpos$mutableblockpos.set(x + p_374019_, y + p_374020_, z + p_374021_);
                blockpos$mutableblockpos1.setWithOffset(blockpos$mutableblockpos, p_374018_);
                BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);
                BlockState blockstate1 = level.getBlockState(blockpos$mutableblockpos1);
                BlockState blockstate2 = blockstate.updateShape(
                    level, level, blockpos$mutableblockpos, p_374018_, blockpos$mutableblockpos1, blockstate1, level.getRandom()
                );
                if (blockstate != blockstate2) {
                    level.setBlock(blockpos$mutableblockpos, blockstate2, flags & -2);
                }

                BlockState blockstate3 = blockstate1.updateShape(
                    level, level, blockpos$mutableblockpos1, p_374018_.getOpposite(), blockpos$mutableblockpos, blockstate2, level.getRandom()
                );
                if (blockstate1 != blockstate3) {
                    level.setBlock(blockpos$mutableblockpos1, blockstate3, flags & -2);
                }
            }
        );
    }

    /**
     * @deprecated Forge: Use {@link #processBlockInfos(ServerLevelAccessor, BlockPos,
     *             BlockPos, StructurePlaceSettings, List, StructureTemplate)}
     *             instead.
     */
    @Deprecated
    public static List<StructureTemplate.StructureBlockInfo> processBlockInfos(
        ServerLevelAccessor serverLevel,
        BlockPos offset,
        BlockPos pos,
        StructurePlaceSettings settings,
        List<StructureTemplate.StructureBlockInfo> blockInfos
    ) {
        return processBlockInfos(serverLevel, offset, pos, settings, blockInfos, null);
    }

    public static List<StructureTemplate.StructureBlockInfo> processBlockInfos(ServerLevelAccessor serverLevel, BlockPos offset, BlockPos pos, StructurePlaceSettings settings, List<StructureTemplate.StructureBlockInfo> blockInfos, @Nullable StructureTemplate template) {
        List<StructureTemplate.StructureBlockInfo> list = new ArrayList<>();
        List<StructureTemplate.StructureBlockInfo> list1 = new ArrayList<>();

        for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : blockInfos) {
            BlockPos blockpos = calculateRelativePosition(settings, structuretemplate$structureblockinfo.pos).offset(offset);
            StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo1 = new StructureTemplate.StructureBlockInfo(
                blockpos,
                structuretemplate$structureblockinfo.state,
                structuretemplate$structureblockinfo.nbt != null ? structuretemplate$structureblockinfo.nbt.copy() : null
            );
            Iterator<StructureProcessor> iterator = settings.getProcessors().iterator();

            while (structuretemplate$structureblockinfo1 != null && iterator.hasNext()) {
                structuretemplate$structureblockinfo1 = iterator.next()
                    .process(serverLevel, offset, pos, structuretemplate$structureblockinfo, structuretemplate$structureblockinfo1, settings, template);
            }

            if (structuretemplate$structureblockinfo1 != null) {
                list1.add(structuretemplate$structureblockinfo1);
                list.add(structuretemplate$structureblockinfo);
            }
        }

        for (StructureProcessor structureprocessor : settings.getProcessors()) {
            list1 = structureprocessor.finalizeProcessing(serverLevel, offset, pos, list, list1, settings);
        }

        return list1;
    }

    public static List<StructureTemplate.StructureEntityInfo> processEntityInfos(@Nullable StructureTemplate template, LevelAccessor p_215387_0_, BlockPos p_215387_1_, StructurePlaceSettings p_215387_2_, List<StructureTemplate.StructureEntityInfo> p_215387_3_) {
        List<StructureTemplate.StructureEntityInfo> list = Lists.newArrayList();
        for(StructureTemplate.StructureEntityInfo entityInfo : p_215387_3_) {
            Vec3 pos = transformedVec3d(p_215387_2_, entityInfo.pos).add(Vec3.atLowerCornerOf(p_215387_1_));
            BlockPos blockpos = calculateRelativePosition(p_215387_2_, entityInfo.blockPos).offset(p_215387_1_);
            StructureTemplate.StructureEntityInfo info = new StructureTemplate.StructureEntityInfo(pos, blockpos, entityInfo.nbt);
            for (StructureProcessor proc : p_215387_2_.getProcessors()) {
                info = proc.processEntity(p_215387_0_, p_215387_1_, entityInfo, info, p_215387_2_, template);
                if (info == null)
                    break;
            }
            if (info != null)
                list.add(info);
        }
        return list;
    }

    private void addEntitiesToWorld(ServerLevelAccessor p_74524_, BlockPos p_74525_, StructurePlaceSettings placementIn, ProblemReporter p_421857_) {
        for(StructureTemplate.StructureEntityInfo structuretemplate$structureentityinfo : processEntityInfos(this, p_74524_, p_74525_, placementIn, this.entityInfoList)) {
            BlockPos blockpos = structuretemplate$structureentityinfo.blockPos; // FORGE: Position will have already been transformed by processEntityInfos
            if (placementIn.getBoundingBox() == null || placementIn.getBoundingBox().isInside(blockpos)) {
                CompoundTag compoundtag = structuretemplate$structureentityinfo.nbt.copy();
                Vec3 vec31 = structuretemplate$structureentityinfo.pos; // FORGE: Position will have already been transformed by processEntityInfos
                ListTag listtag = new ListTag();
                listtag.add(DoubleTag.valueOf(vec31.x));
                listtag.add(DoubleTag.valueOf(vec31.y));
                listtag.add(DoubleTag.valueOf(vec31.z));
                compoundtag.put("Pos", listtag);
                compoundtag.remove("UUID");
                createEntityIgnoreException(p_421857_, p_74524_, compoundtag).ifPresent(p_432733_ -> {
                    float f = p_432733_.rotate(placementIn.getRotation());
                    f += p_432733_.mirror(placementIn.getMirror()) - p_432733_.getYRot();
                    p_432733_.snapTo(vec31.x, vec31.y, vec31.z, f, p_432733_.getXRot());
                    p_432733_.setYBodyRot(f);
                    p_432733_.setYHeadRot(f);
                    if (placementIn.shouldFinalizeEntities() && p_432733_ instanceof Mob mob) {
                        mob.finalizeSpawn(p_74524_, p_74524_.getCurrentDifficultyAt(BlockPos.containing(vec31)), EntitySpawnReason.STRUCTURE, null);
                    }

                    p_74524_.addFreshEntityWithPassengers(p_432733_);
                });
            }
        }
    }

    private static Optional<Entity> createEntityIgnoreException(ProblemReporter problemReporter, ServerLevelAccessor level, CompoundTag tag) {
        try {
            return EntityType.create(TagValueInput.create(problemReporter, level.registryAccess(), tag), level.getLevel(), EntitySpawnReason.STRUCTURE);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public Vec3i getSize(Rotation rotation) {
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                return new Vec3i(this.size.getZ(), this.size.getY(), this.size.getX());
            default:
                return this.size;
        }
    }

    public static BlockPos transform(BlockPos targetPos, Mirror mirror, Rotation rotation, BlockPos offset) {
        int i = targetPos.getX();
        int j = targetPos.getY();
        int k = targetPos.getZ();
        boolean flag = true;
        switch (mirror) {
            case LEFT_RIGHT:
                k = -k;
                break;
            case FRONT_BACK:
                i = -i;
                break;
            default:
                flag = false;
        }

        int l = offset.getX();
        int i1 = offset.getZ();
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
                return new BlockPos(l - i1 + k, j, l + i1 - i);
            case CLOCKWISE_90:
                return new BlockPos(l + i1 - k, j, i1 - l + i);
            case CLOCKWISE_180:
                return new BlockPos(l + l - i, j, i1 + i1 - k);
            default:
                return flag ? new BlockPos(i, j, k) : targetPos;
        }
    }

    public static Vec3 transform(Vec3 target, Mirror mirror, Rotation rotation, BlockPos centerOffset) {
        double d0 = target.x;
        double d1 = target.y;
        double d2 = target.z;
        boolean flag = true;
        switch (mirror) {
            case LEFT_RIGHT:
                d2 = 1.0 - d2;
                break;
            case FRONT_BACK:
                d0 = 1.0 - d0;
                break;
            default:
                flag = false;
        }

        int i = centerOffset.getX();
        int j = centerOffset.getZ();
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
                return new Vec3(i - j + d2, d1, i + j + 1 - d0);
            case CLOCKWISE_90:
                return new Vec3(i + j + 1 - d2, d1, j - i + d0);
            case CLOCKWISE_180:
                return new Vec3(i + i + 1 - d0, d1, j + j + 1 - d2);
            default:
                return flag ? new Vec3(d0, d1, d2) : target;
        }
    }

    public BlockPos getZeroPositionWithTransform(BlockPos targetPos, Mirror mirror, Rotation rotation) {
        return getZeroPositionWithTransform(targetPos, mirror, rotation, this.getSize().getX(), this.getSize().getZ());
    }

    public static BlockPos getZeroPositionWithTransform(BlockPos pos, Mirror mirror, Rotation rotation, int sizeX, int sizeZ) {
        sizeX--;
        sizeZ--;
        int i = mirror == Mirror.FRONT_BACK ? sizeX : 0;
        int j = mirror == Mirror.LEFT_RIGHT ? sizeZ : 0;
        BlockPos blockpos = pos;
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
                blockpos = pos.offset(j, 0, sizeX - i);
                break;
            case CLOCKWISE_90:
                blockpos = pos.offset(sizeZ - j, 0, i);
                break;
            case CLOCKWISE_180:
                blockpos = pos.offset(sizeX - i, 0, sizeZ - j);
                break;
            case NONE:
                blockpos = pos.offset(i, 0, j);
        }

        return blockpos;
    }

    public BoundingBox getBoundingBox(StructurePlaceSettings settings, BlockPos startPos) {
        return this.getBoundingBox(startPos, settings.getRotation(), settings.getRotationPivot(), settings.getMirror());
    }

    public BoundingBox getBoundingBox(BlockPos startPos, Rotation rotation, BlockPos pivotPos, Mirror mirror) {
        return getBoundingBox(startPos, rotation, pivotPos, mirror, this.size);
    }

    @VisibleForTesting
    protected static BoundingBox getBoundingBox(BlockPos startPos, Rotation rotation, BlockPos pivotPos, Mirror mirror, Vec3i size) {
        Vec3i vec3i = size.offset(-1, -1, -1);
        BlockPos blockpos = transform(BlockPos.ZERO, mirror, rotation, pivotPos);
        BlockPos blockpos1 = transform(BlockPos.ZERO.offset(vec3i), mirror, rotation, pivotPos);
        return BoundingBox.fromCorners(blockpos, blockpos1).move(startPos);
    }

    public CompoundTag save(CompoundTag tag) {
        if (this.palettes.isEmpty()) {
            tag.put("blocks", new ListTag());
            tag.put("palette", new ListTag());
        } else {
            List<StructureTemplate.SimplePalette> list = Lists.newArrayList();
            StructureTemplate.SimplePalette structuretemplate$simplepalette = new StructureTemplate.SimplePalette();
            list.add(structuretemplate$simplepalette);

            for (int i = 1; i < this.palettes.size(); i++) {
                list.add(new StructureTemplate.SimplePalette());
            }

            ListTag listtag1 = new ListTag();
            List<StructureTemplate.StructureBlockInfo> list1 = this.palettes.get(0).blocks();

            for (int j = 0; j < list1.size(); j++) {
                StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo = list1.get(j);
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.put(
                    "pos",
                    this.newIntegerList(
                        structuretemplate$structureblockinfo.pos.getX(),
                        structuretemplate$structureblockinfo.pos.getY(),
                        structuretemplate$structureblockinfo.pos.getZ()
                    )
                );
                int k = structuretemplate$simplepalette.idFor(structuretemplate$structureblockinfo.state);
                compoundtag.putInt("state", k);
                if (structuretemplate$structureblockinfo.nbt != null) {
                    compoundtag.put("nbt", structuretemplate$structureblockinfo.nbt);
                }

                listtag1.add(compoundtag);

                for (int l = 1; l < this.palettes.size(); l++) {
                    StructureTemplate.SimplePalette structuretemplate$simplepalette1 = list.get(l);
                    structuretemplate$simplepalette1.addMapping(this.palettes.get(l).blocks().get(j).state, k);
                }
            }

            tag.put("blocks", listtag1);
            if (list.size() == 1) {
                ListTag listtag2 = new ListTag();

                for (BlockState blockstate : structuretemplate$simplepalette) {
                    listtag2.add(NbtUtils.writeBlockState(blockstate));
                }

                tag.put("palette", listtag2);
            } else {
                ListTag listtag3 = new ListTag();

                for (StructureTemplate.SimplePalette structuretemplate$simplepalette2 : list) {
                    ListTag listtag4 = new ListTag();

                    for (BlockState blockstate1 : structuretemplate$simplepalette2) {
                        listtag4.add(NbtUtils.writeBlockState(blockstate1));
                    }

                    listtag3.add(listtag4);
                }

                tag.put("palettes", listtag3);
            }
        }

        ListTag listtag = new ListTag();

        for (StructureTemplate.StructureEntityInfo structuretemplate$structureentityinfo : this.entityInfoList) {
            CompoundTag compoundtag1 = new CompoundTag();
            compoundtag1.put(
                "pos",
                this.newDoubleList(
                    structuretemplate$structureentityinfo.pos.x, structuretemplate$structureentityinfo.pos.y, structuretemplate$structureentityinfo.pos.z
                )
            );
            compoundtag1.put(
                "blockPos",
                this.newIntegerList(
                    structuretemplate$structureentityinfo.blockPos.getX(),
                    structuretemplate$structureentityinfo.blockPos.getY(),
                    structuretemplate$structureentityinfo.blockPos.getZ()
                )
            );
            if (structuretemplate$structureentityinfo.nbt != null) {
                compoundtag1.put("nbt", structuretemplate$structureentityinfo.nbt);
            }

            listtag.add(compoundtag1);
        }

        tag.put("entities", listtag);
        tag.put("size", this.newIntegerList(this.size.getX(), this.size.getY(), this.size.getZ()));
        return NbtUtils.addCurrentDataVersion(tag);
    }

    public void load(HolderGetter<Block> blockGetter, CompoundTag tag) {
        this.palettes.clear();
        this.entityInfoList.clear();
        ListTag listtag = tag.getListOrEmpty("size");
        this.size = new Vec3i(listtag.getIntOr(0, 0), listtag.getIntOr(1, 0), listtag.getIntOr(2, 0));
        ListTag listtag1 = tag.getListOrEmpty("blocks");
        Optional<ListTag> optional = tag.getList("palettes");
        if (optional.isPresent()) {
            for (int i = 0; i < optional.get().size(); i++) {
                this.loadPalette(blockGetter, optional.get().getListOrEmpty(i), listtag1);
            }
        } else {
            this.loadPalette(blockGetter, tag.getListOrEmpty("palette"), listtag1);
        }

        tag.getListOrEmpty("entities").compoundStream().forEach(p_409571_ -> {
            ListTag listtag2 = p_409571_.getListOrEmpty("pos");
            Vec3 vec3 = new Vec3(listtag2.getDoubleOr(0, 0.0), listtag2.getDoubleOr(1, 0.0), listtag2.getDoubleOr(2, 0.0));
            ListTag listtag3 = p_409571_.getListOrEmpty("blockPos");
            BlockPos blockpos = new BlockPos(listtag3.getIntOr(0, 0), listtag3.getIntOr(1, 0), listtag3.getIntOr(2, 0));
            p_409571_.getCompound("nbt").ifPresent(p_409570_ -> this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(vec3, blockpos, p_409570_)));
        });
    }

    private void loadPalette(HolderGetter<Block> blockGetter, ListTag paletteTag, ListTag blocksTag) {
        StructureTemplate.SimplePalette structuretemplate$simplepalette = new StructureTemplate.SimplePalette();

        for (int i = 0; i < paletteTag.size(); i++) {
            structuretemplate$simplepalette.addMapping(NbtUtils.readBlockState(blockGetter, paletteTag.getCompoundOrEmpty(i)), i);
        }

        List<StructureTemplate.StructureBlockInfo> list3 = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> list1 = Lists.newArrayList();
        blocksTag.compoundStream()
            .forEach(
                p_409576_ -> {
                    ListTag listtag = p_409576_.getListOrEmpty("pos");
                    BlockPos blockpos = new BlockPos(listtag.getIntOr(0, 0), listtag.getIntOr(1, 0), listtag.getIntOr(2, 0));
                    BlockState blockstate = structuretemplate$simplepalette.stateFor(p_409576_.getIntOr("state", 0));
                    CompoundTag compoundtag = p_409576_.getCompound("nbt").orElse(null);
                    StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo = new StructureTemplate.StructureBlockInfo(
                        blockpos, blockstate, compoundtag
                    );
                    addToLists(structuretemplate$structureblockinfo, list3, list, list1);
                }
            );
        List<StructureTemplate.StructureBlockInfo> list2 = buildInfoList(list3, list, list1);
        this.palettes.add(new StructureTemplate.Palette(list2));
    }

    private ListTag newIntegerList(int... values) {
        ListTag listtag = new ListTag();

        for (int i : values) {
            listtag.add(IntTag.valueOf(i));
        }

        return listtag;
    }

    private ListTag newDoubleList(double... values) {
        ListTag listtag = new ListTag();

        for (double d0 : values) {
            listtag.add(DoubleTag.valueOf(d0));
        }

        return listtag;
    }

    public static JigsawBlockEntity.JointType getJointType(CompoundTag tag, BlockState state) {
        return tag.read("joint", JigsawBlockEntity.JointType.CODEC).orElseGet(() -> getDefaultJointType(state));
    }

    public static JigsawBlockEntity.JointType getDefaultJointType(BlockState state) {
        return JigsawBlock.getFrontFacing(state).getAxis().isHorizontal() ? JigsawBlockEntity.JointType.ALIGNED : JigsawBlockEntity.JointType.ROLLABLE;
    }

    public record JigsawBlockInfo(
        StructureTemplate.StructureBlockInfo info,
        JigsawBlockEntity.JointType jointType,
        ResourceLocation name,
        ResourceKey<StructureTemplatePool> pool,
        ResourceLocation target,
        int placementPriority,
        int selectionPriority
    ) {
        public static StructureTemplate.JigsawBlockInfo of(StructureTemplate.StructureBlockInfo structureBlockInfo) {
            CompoundTag compoundtag = Objects.requireNonNull(structureBlockInfo.nbt(), () -> structureBlockInfo + " nbt was null");
            return new StructureTemplate.JigsawBlockInfo(
                structureBlockInfo,
                StructureTemplate.getJointType(compoundtag, structureBlockInfo.state()),
                compoundtag.read("name", ResourceLocation.CODEC).orElse(JigsawBlockEntity.EMPTY_ID),
                compoundtag.read("pool", JigsawBlockEntity.POOL_CODEC).orElse(Pools.EMPTY),
                compoundtag.read("target", ResourceLocation.CODEC).orElse(JigsawBlockEntity.EMPTY_ID),
                compoundtag.getIntOr("placement_priority", 0),
                compoundtag.getIntOr("selection_priority", 0)
            );
        }

        @Override
        public String toString() {
            return String.format(
                Locale.ROOT,
                "<JigsawBlockInfo | %s | %s | name: %s | pool: %s | target: %s | placement: %d | selection: %d | %s>",
                this.info.pos,
                this.info.state,
                this.name,
                this.pool.location(),
                this.target,
                this.placementPriority,
                this.selectionPriority,
                this.info.nbt
            );
        }

        public StructureTemplate.JigsawBlockInfo withInfo(StructureTemplate.StructureBlockInfo info) {
            return new StructureTemplate.JigsawBlockInfo(
                info, this.jointType, this.name, this.pool, this.target, this.placementPriority, this.selectionPriority
            );
        }
    }

    public static final class Palette {
        private final List<StructureTemplate.StructureBlockInfo> blocks;
        private final Map<Block, List<StructureTemplate.StructureBlockInfo>> cache = Maps.newConcurrentMap(); // Neo: Fixes MC-271899 - Make the global StructureTemplate's palette caches now thread safe for worldgen
        @Nullable
        private List<StructureTemplate.JigsawBlockInfo> cachedJigsaws;

        Palette(List<StructureTemplate.StructureBlockInfo> blocks) {
            this.blocks = blocks;
        }

        public List<StructureTemplate.JigsawBlockInfo> jigsaws() {
            if (this.cachedJigsaws == null) {
                this.cachedJigsaws = this.blocks(Blocks.JIGSAW).stream().map(StructureTemplate.JigsawBlockInfo::of).toList();
            }

            return this.cachedJigsaws;
        }

        public List<StructureTemplate.StructureBlockInfo> blocks() {
            return this.blocks;
        }

        public List<StructureTemplate.StructureBlockInfo> blocks(Block block) {
            return this.cache
                .computeIfAbsent(block, p_74659_ -> this.blocks.stream().filter(p_163818_ -> p_163818_.state.is(p_74659_)).collect(Collectors.toList()));
        }
    }

    static class SimplePalette implements Iterable<BlockState> {
        public static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
        private final IdMapper<BlockState> ids = new IdMapper<>(16);
        private int lastId;

        public int idFor(BlockState state) {
            int i = this.ids.getId(state);
            if (i == -1) {
                i = this.lastId++;
                this.ids.addMapping(state, i);
            }

            return i;
        }

        @Nullable
        public BlockState stateFor(int id) {
            BlockState blockstate = this.ids.byId(id);
            return blockstate == null ? DEFAULT_BLOCK_STATE : blockstate;
        }

        @Override
        public Iterator<BlockState> iterator() {
            return this.ids.iterator();
        }

        public void addMapping(BlockState state, int id) {
            this.ids.addMapping(state, id);
        }
    }

    public record StructureBlockInfo(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {
        @Override
        public String toString() {
            return String.format(Locale.ROOT, "<StructureBlockInfo | %s | %s | %s>", this.pos, this.state, this.nbt);
        }
    }

    public static class StructureEntityInfo {
        public final Vec3 pos;
        public final BlockPos blockPos;
        public final CompoundTag nbt;

        public StructureEntityInfo(Vec3 pos, BlockPos blockPos, CompoundTag nbt) {
            this.pos = pos;
            this.blockPos = blockPos;
            this.nbt = nbt;
        }
    }
}
