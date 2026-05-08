package net.minecraft.world.level.block;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

public class Block extends BlockBehaviour implements ItemLike, net.neoforged.neoforge.common.extensions.IBlockExtension {
    public static final MapCodec<Block> CODEC = simpleCodec(Block::new);
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Holder.Reference<Block> builtInRegistryHolder = BuiltInRegistries.BLOCK.createIntrusiveHolder(this);
    public static final IdMapper<BlockState> BLOCK_STATE_REGISTRY = net.neoforged.neoforge.registries.GameData.getBlockStateIDMap();
    private static final LoadingCache<VoxelShape, Boolean> SHAPE_FULL_BLOCK_CACHE = CacheBuilder.newBuilder()
        .maximumSize(512L)
        .weakKeys()
        .build(new CacheLoader<VoxelShape, Boolean>() {
            public Boolean load(VoxelShape shape) {
                return !Shapes.joinIsNotEmpty(Shapes.block(), shape, BooleanOp.NOT_SAME);
            }
        });
    public static final int UPDATE_NEIGHBORS = 1;
    public static final int UPDATE_CLIENTS = 2;
    public static final int UPDATE_INVISIBLE = 4;
    public static final int UPDATE_IMMEDIATE = 8;
    public static final int UPDATE_KNOWN_SHAPE = 16;
    public static final int UPDATE_SUPPRESS_DROPS = 32;
    public static final int UPDATE_MOVE_BY_PISTON = 64;
    public static final int UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE = 128;
    public static final int UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS = 256;
    public static final int UPDATE_SKIP_ON_PLACE = 512;
    public static final int UPDATE_NONE = 260;
    public static final int UPDATE_ALL = 3;
    public static final int UPDATE_ALL_IMMEDIATE = 11;
    public static final int UPDATE_SKIP_ALL_SIDEEFFECTS = 816;
    public static final float INDESTRUCTIBLE = -1.0F;
    public static final float INSTANT = 0.0F;
    public static final int UPDATE_LIMIT = 512;
    protected final StateDefinition<Block, BlockState> stateDefinition;
    private BlockState defaultBlockState;
    @Nullable
    private Item item;
    private static final int CACHE_SIZE = 256;
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.ShapePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(() -> {
        Object2ByteLinkedOpenHashMap<Block.ShapePairKey> object2bytelinkedopenhashmap = new Object2ByteLinkedOpenHashMap<Block.ShapePairKey>(256, 0.25F) {
            @Override
            protected void rehash(int newN) {
            }
        };
        object2bytelinkedopenhashmap.defaultReturnValue((byte)127);
        return object2bytelinkedopenhashmap;
    });

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static int getId(@Nullable BlockState state) {
        if (state == null) {
            return 0;
        } else {
            int i = BLOCK_STATE_REGISTRY.getId(state);
            return i == -1 ? 0 : i;
        }
    }

    public static BlockState stateById(int id) {
        BlockState blockstate = BLOCK_STATE_REGISTRY.byId(id);
        return blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
    }

    public static Block byItem(@Nullable Item item) {
        return item instanceof BlockItem ? ((BlockItem)item).getBlock() : Blocks.AIR;
    }

    public static BlockState pushEntitiesUp(BlockState oldState, BlockState newState, LevelAccessor level, BlockPos pos) {
        VoxelShape voxelshape = Shapes.joinUnoptimized(
                oldState.getCollisionShape(level, pos), newState.getCollisionShape(level, pos), BooleanOp.ONLY_SECOND
            )
            .move(pos);
        if (voxelshape.isEmpty()) {
            return newState;
        } else {
            for (Entity entity : level.getEntities(null, voxelshape.bounds())) {
                double d0 = Shapes.collide(Direction.Axis.Y, entity.getBoundingBox().move(0.0, 1.0, 0.0), List.of(voxelshape), -1.0);
                entity.teleportRelative(0.0, 1.0 + d0, 0.0);
            }

            return newState;
        }
    }

    public static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Shapes.box(x1 / 16.0, y1 / 16.0, z1 / 16.0, x2 / 16.0, y2 / 16.0, z2 / 16.0);
    }

    public static VoxelShape[] boxes(int count, IntFunction<VoxelShape> boxCreator) {
        return IntStream.rangeClosed(0, count).mapToObj(boxCreator).toArray(VoxelShape[]::new);
    }

    public static VoxelShape cube(double size) {
        return cube(size, size, size);
    }

    public static VoxelShape cube(double xSize, double ySize, double zSize) {
        double d0 = ySize / 2.0;
        return column(xSize, zSize, 8.0 - d0, 8.0 + d0);
    }

    public static VoxelShape column(double size, double y1, double y2) {
        return column(size, size, y1, y2);
    }

    public static VoxelShape column(double xSize, double zSize, double y1, double y2) {
        double d0 = xSize / 2.0;
        double d1 = zSize / 2.0;
        return box(8.0 - d0, y1, 8.0 - d1, 8.0 + d0, y2, 8.0 + d1);
    }

    public static VoxelShape boxZ(double size, double z1, double z2) {
        return boxZ(size, size, z1, z2);
    }

    public static VoxelShape boxZ(double xSize, double ySize, double z1, double z2) {
        double d0 = ySize / 2.0;
        return boxZ(xSize, 8.0 - d0, 8.0 + d0, z1, z2);
    }

    public static VoxelShape boxZ(double xSize, double y1, double y2, double z1, double z2) {
        double d0 = xSize / 2.0;
        return box(8.0 - d0, y1, z1, 8.0 + d0, y2, z2);
    }

    /**
     * With the provided block state, performs neighbor checks for all neighboring blocks to get an "adjusted" blockstate for placement in the world, if the current state is not valid.
     */
    public static BlockState updateFromNeighbourShapes(BlockState currentState, LevelAccessor level, BlockPos pos) {
        BlockState blockstate = currentState;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (Direction direction : UPDATE_SHAPE_ORDER) {
            blockpos$mutableblockpos.setWithOffset(pos, direction);
            blockstate = blockstate.updateShape(
                level, level, pos, direction, blockpos$mutableblockpos, level.getBlockState(blockpos$mutableblockpos), level.getRandom()
            );
        }

        return blockstate;
    }

    /**
     * Replaces oldState with newState, possibly playing effects and creating drops. Flags are as in {@link net.minecraft.world.level.Level#setBlock(net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, int)}.
     */
    public static void updateOrDestroy(BlockState oldState, BlockState newState, LevelAccessor level, BlockPos pos, int flags) {
        updateOrDestroy(oldState, newState, level, pos, flags, 512);
    }

    public static void updateOrDestroy(BlockState oldState, BlockState newState, LevelAccessor level, BlockPos pos, int flags, int recursionLeft) {
        if (newState != oldState) {
            if (newState.isAir()) {
                if (!level.isClientSide()) {
                    level.destroyBlock(pos, (flags & 32) == 0, null, recursionLeft);
                }
            } else {
                level.setBlock(pos, newState, flags & -33, recursionLeft);
            }
        }
    }

    public Block(BlockBehaviour.Properties properties) {
        super(properties);
        StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<>(this);
        this.createBlockStateDefinition(builder);
        this.stateDefinition = builder.create(Block::defaultBlockState, BlockState::new);
        this.registerDefaultState(this.stateDefinition.any());
        if (SharedConstants.IS_RUNNING_IN_IDE && false) {
            String s = this.getClass().getSimpleName();
            if (!s.endsWith("Block")) {
                LOGGER.error("Block classes should end with Block and {} doesn't.", s);
            }
        }
    }

    public static boolean isExceptionForConnection(BlockState state) {
        return state.getBlock() instanceof LeavesBlock
            || state.is(Blocks.BARRIER)
            || state.is(Blocks.CARVED_PUMPKIN)
            || state.is(Blocks.JACK_O_LANTERN)
            || state.is(Blocks.MELON)
            || state.is(Blocks.PUMPKIN)
            || state.is(BlockTags.SHULKER_BOXES);
    }

    protected static boolean dropFromBlockInteractLootTable(
        ServerLevel level,
        ResourceKey<LootTable> lootTable,
        BlockState state,
        @Nullable BlockEntity blockEntity,
        @Nullable ItemStack tool,
        @Nullable Entity entity,
        BiConsumer<ServerLevel, ItemStack> dropConsumer
    ) {
        return dropFromLootTable(
            level,
            lootTable,
            p_432610_ -> p_432610_.withParameter(LootContextParams.BLOCK_STATE, state)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                .withOptionalParameter(LootContextParams.INTERACTING_ENTITY, entity)
                .withOptionalParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK_INTERACT),
            dropConsumer
        );
    }

    protected static boolean dropFromLootTable(
        ServerLevel level,
        ResourceKey<LootTable> lootTable,
        Function<LootParams.Builder, LootParams> params,
        BiConsumer<ServerLevel, ItemStack> dropConsumer
    ) {
        LootTable loottable = level.getServer().reloadableRegistries().getLootTable(lootTable);
        LootParams lootparams = params.apply(new LootParams.Builder(level));
        List<ItemStack> list = loottable.getRandomItems(lootparams);
        if (!list.isEmpty()) {
            list.forEach(p_432605_ -> dropConsumer.accept(level, p_432605_));
            return true;
        } else {
            return false;
        }
    }

    /**
     * @deprecated Neo: use overload with level context instead
     */
    @Deprecated
    public static boolean shouldRenderFace(BlockState currentFace, BlockState neighboringFace, Direction face) {
        return shouldRenderFace(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO, currentFace, neighboringFace, face);
    }

    public static boolean shouldRenderFace(BlockGetter level, BlockPos pos, BlockState p_152445_, BlockState p_361252_, Direction p_152448_) {
        VoxelShape voxelshape = p_361252_.getFaceOcclusionShape(p_152448_.getOpposite());
        if (voxelshape == Shapes.block()) {
            return false;
        } else if (p_152445_.skipRendering(p_361252_, p_152448_)) {
            return false;
        } else if (p_361252_.hidesNeighborFace(level, pos.relative(p_152448_), p_152445_, p_152448_.getOpposite()) && p_152445_.supportsExternalFaceHiding()) {
            return false;
        } else if (voxelshape == Shapes.empty()) {
            return true;
        } else {
            VoxelShape voxelshape1 = p_152445_.getFaceOcclusionShape(p_152448_);
            if (voxelshape1 == Shapes.empty()) {
                return true;
            } else {
                Block.ShapePairKey block$shapepairkey = new Block.ShapePairKey(voxelshape1, voxelshape);
                Object2ByteLinkedOpenHashMap<Block.ShapePairKey> object2bytelinkedopenhashmap = OCCLUSION_CACHE.get();
                byte b0 = object2bytelinkedopenhashmap.getAndMoveToFirst(block$shapepairkey);
                if (b0 != 127) {
                    return b0 != 0;
                } else {
                    boolean flag = Shapes.joinIsNotEmpty(voxelshape1, voxelshape, BooleanOp.ONLY_FIRST);
                    if (object2bytelinkedopenhashmap.size() == 256) {
                        object2bytelinkedopenhashmap.removeLastByte();
                    }

                    object2bytelinkedopenhashmap.putAndMoveToFirst(block$shapepairkey, (byte)(flag ? 1 : 0));
                    return flag;
                }
            }
        }
    }

    /**
     * @return whether the given position has a rigid top face
     */
    public static boolean canSupportRigidBlock(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP, SupportType.RIGID);
    }

    /**
     * @return whether the given position has a solid center in the given direction
     */
    public static boolean canSupportCenter(LevelReader level, BlockPos pos, Direction direction) {
        BlockState blockstate = level.getBlockState(pos);
        return direction == Direction.DOWN && blockstate.is(BlockTags.UNSTABLE_BOTTOM_CENTER)
            ? false
            : blockstate.isFaceSturdy(level, pos, direction, SupportType.CENTER);
    }

    public static boolean isFaceFull(VoxelShape shape, Direction face) {
        VoxelShape voxelshape = shape.getFaceShape(face);
        return isShapeFullBlock(voxelshape);
    }

    /**
     * @return whether the provided {@link net.minecraft.world.phys.shapes.VoxelShape} is a full block (1x1x1)
     */
    public static boolean isShapeFullBlock(VoxelShape shape) {
        return SHAPE_FULL_BLOCK_CACHE.getUnchecked(shape);
    }

    /**
     * Called periodically clientside on blocks near the player to show effects (like furnace fire particles).
     */
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    /**
     * Called after this block has been removed by a player.
     */
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
    }

    public static List<ItemStack> getDrops(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity) {
        LootParams.Builder lootparams$builder = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
        return state.getDrops(lootparams$builder);
    }

    public static List<ItemStack> getDrops(
        BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack tool
    ) {
        LootParams.Builder lootparams$builder = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withParameter(LootContextParams.TOOL, tool)
            .withOptionalParameter(LootContextParams.THIS_ENTITY, entity)
            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
        return state.getDrops(lootparams$builder);
    }

    public static void dropResources(BlockState state, Level level, BlockPos pos) {
        if (level instanceof ServerLevel) {
            beginCapturingDrops();
            getDrops(state, (ServerLevel)level, pos, null).forEach(p_152406_ -> popResource(level, pos, p_152406_));
            List<ItemEntity> captured = stopCapturingDrops();
            net.neoforged.neoforge.common.CommonHooks.handleBlockDrops((ServerLevel) level, pos, state, null, captured, null, ItemStack.EMPTY);
        }
    }

    public static void dropResources(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (level instanceof ServerLevel) {
            beginCapturingDrops();
            getDrops(state, (ServerLevel)level, pos, blockEntity).forEach(p_49859_ -> popResource((ServerLevel)level, pos, p_49859_));
            List<ItemEntity> captured = stopCapturingDrops();
            net.neoforged.neoforge.common.CommonHooks.handleBlockDrops((ServerLevel) level, pos, state, blockEntity, captured, null, ItemStack.EMPTY);
        }
    }

    public static void dropResources(
            BlockState state, Level level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack tool
    ) {
        if (level instanceof ServerLevel) {
            beginCapturingDrops();
            getDrops(state, (ServerLevel)level, pos, blockEntity, entity, tool).forEach(p_49944_ -> popResource(level, pos, p_49944_));
            List<ItemEntity> captured = stopCapturingDrops();
            net.neoforged.neoforge.common.CommonHooks.handleBlockDrops((ServerLevel) level, pos, state, blockEntity, captured, entity, tool);
        }
    }

    /**
     * Spawns the given stack into the Level at the given position, respecting the doTileDrops gamerule
     */
    public static void popResource(Level level, BlockPos pos, ItemStack stack) {
        double d0 = EntityType.ITEM.getHeight() / 2.0;
        double d1 = pos.getX() + 0.5 + Mth.nextDouble(level.random, -0.25, 0.25);
        double d2 = pos.getY() + 0.5 + Mth.nextDouble(level.random, -0.25, 0.25) - d0;
        double d3 = pos.getZ() + 0.5 + Mth.nextDouble(level.random, -0.25, 0.25);
        popResource(level, () -> new ItemEntity(level, d1, d2, d3, stack), stack);
    }

    public static void popResourceFromFace(Level level, BlockPos pos, Direction direction, ItemStack stack) {
        int i = direction.getStepX();
        int j = direction.getStepY();
        int k = direction.getStepZ();
        double d0 = EntityType.ITEM.getWidth() / 2.0;
        double d1 = EntityType.ITEM.getHeight() / 2.0;
        double d2 = pos.getX() + 0.5 + (i == 0 ? Mth.nextDouble(level.random, -0.25, 0.25) : i * (0.5 + d0));
        double d3 = pos.getY() + 0.5 + (j == 0 ? Mth.nextDouble(level.random, -0.25, 0.25) : j * (0.5 + d1)) - d1;
        double d4 = pos.getZ() + 0.5 + (k == 0 ? Mth.nextDouble(level.random, -0.25, 0.25) : k * (0.5 + d0));
        double d5 = i == 0 ? Mth.nextDouble(level.random, -0.1, 0.1) : i * 0.1;
        double d6 = j == 0 ? Mth.nextDouble(level.random, 0.0, 0.1) : j * 0.1 + 0.1;
        double d7 = k == 0 ? Mth.nextDouble(level.random, -0.1, 0.1) : k * 0.1;
        popResource(level, () -> new ItemEntity(level, d2, d3, d4, stack, d5, d6, d7), stack);
    }

    private static void popResource(Level level, Supplier<ItemEntity> itemEntitySupplier, ItemStack stack) {
        if (level instanceof ServerLevel serverLevel && !stack.isEmpty() && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !level.restoringBlockSnapshots) {
            ItemEntity itementity = itemEntitySupplier.get();
            itementity.setDefaultPickUpDelay();
            // Neo: Add drops to the captured list if capturing is enabled.
            if (capturedDrops != null) {
                capturedDrops.add(itementity);
            }
            else {
                level.addFreshEntity(itementity);
            }
        }
    }

    /**
     * Spawns the given amount of experience into the Level as experience orb entities.
     */
    public void popExperience(ServerLevel level, BlockPos pos, int amount) {
        if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !level.restoringBlockSnapshots) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos), amount);
        }
    }

    @Deprecated //Forge: Use more sensitive version
    public float getExplosionResistance() {
        return this.explosionResistance;
    }

    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
    }

    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    /**
     * Called after a player has successfully harvested this block. This method will only be called if the player has used the correct tool and drops should be spawned.
     */
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        dropResources(state, level, pos, blockEntity, player, tool);
    }

    /**
     * Called by BlockItem after this block has been placed.
     */
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
    }

    public boolean isPossibleToRespawnInThis(BlockState state) {
        return !state.isSolid() && !state.liquid();
    }

    public MutableComponent getName() {
        return Component.translatable(this.getDescriptionId());
    }

    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }

    public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
    }

    public float getFriction() {
        return this.friction;
    }

    public float getSpeedFactor() {
        return this.speedFactor;
    }

    public float getJumpFactor() {
        return this.jumpFactor;
    }

    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        level.levelEvent(player, 2001, pos, getId(state));
    }

    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        this.spawnDestroyParticles(level, player, pos, state);
        if (state.is(BlockTags.GUARDED_BY_PIGLINS) && level instanceof ServerLevel serverlevel) {
            PiglinAi.angerNearbyPiglins(serverlevel, player, false);
        }

        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        return state;
    }

    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
    }

    /**
     * @return whether this block should drop its drops when destroyed by the given explosion
     */
    @Deprecated //Forge: Use more sensitive version
    public boolean dropFromExplosion(Explosion explosion) {
        return true;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    }

    public StateDefinition<Block, BlockState> getStateDefinition() {
        return this.stateDefinition;
    }

    protected final void registerDefaultState(BlockState state) {
        this.defaultBlockState = state;
    }

    public final BlockState defaultBlockState() {
        return this.defaultBlockState;
    }

    public final BlockState withPropertiesOf(BlockState state) {
        BlockState blockstate = this.defaultBlockState();

        for (Property<?> property : state.getBlock().getStateDefinition().getProperties()) {
            if (blockstate.hasProperty(property)) {
                blockstate = copyProperty(state, blockstate, property);
            }
        }

        return blockstate;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState sourceState, BlockState targetState, Property<T> property) {
        return targetState.setValue(property, sourceState.getValue(property));
    }

    @Override
    public Item asItem() {
        if (this.item == null) {
            this.item = Item.byBlock(this);
        }

        return this.item;
    }

    public boolean hasDynamicShape() {
        return this.dynamicShape;
    }

    @Override
    public String toString() {
        return "Block{" + BuiltInRegistries.BLOCK.wrapAsHolder(this).getRegisteredName() + "}";
    }

    @Override
    protected Block asBlock() {
        return this;
    }

    protected Function<BlockState, VoxelShape> getShapeForEachState(Function<BlockState, VoxelShape> shapeGetter) {
        return this.stateDefinition.getPossibleStates().stream().collect(ImmutableMap.toImmutableMap(Function.identity(), shapeGetter))::get;
    }

    protected Function<BlockState, VoxelShape> getShapeForEachState(Function<BlockState, VoxelShape> shapeGetter, Property<?>... properties) {
        Map<? extends Property<?>, Object> map = Arrays.stream(properties)
            .collect(Collectors.toMap(p_393321_ -> p_393321_, p_393320_ -> p_393320_.getPossibleValues().getFirst()));
        ImmutableMap<BlockState, VoxelShape> immutablemap = this.stateDefinition
            .getPossibleStates()
            .stream()
            .filter(p_393327_ -> map.entrySet().stream().allMatch(p_393329_ -> p_393327_.getValue((Property<?>)p_393329_.getKey()) == p_393329_.getValue()))
            .collect(ImmutableMap.toImmutableMap(Function.identity(), shapeGetter));
        return p_393324_ -> {
            for (Entry<? extends Property<?>, Object> entry : map.entrySet()) {
                p_393324_ = setValueHelper(p_393324_, (Property<?>)entry.getKey(), entry.getValue());
            }

            return immutablemap.get(p_393324_);
        };
    }

    private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(S stateHolder, Property<T> property, Object value) {
        return stateHolder.setValue(property, (T)value);
    }

    /**
     * Neo: Short-lived holder of dropped item entities. Used mainly for Neo hooks and event logic.
     * <p>
     * When not null, records all item entities from {@link #popResource(Level, Supplier, ItemStack)} instead of adding them to the world.
     */
    @Nullable
    private static List<ItemEntity> capturedDrops = null;

    /**
     * Initializes {@link #capturedDrops}, starting the drop capture process.
     * <p>
     * Must only be called on the server thread.
     */
    private static void beginCapturingDrops() {
        capturedDrops = new java.util.ArrayList<>();
    }

    /**
     * Ends the drop capture process by setting {@link #capturedDrops} to null and returning the old list.
     * <p>
     * Must only be called on the server thread.
     */
    private static List<ItemEntity> stopCapturingDrops() {
        List<ItemEntity> drops = capturedDrops;
        capturedDrops = null;
        return drops;
    }

    /** @deprecated */
    @Deprecated
    public Holder.Reference<Block> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    protected void tryDropExperience(ServerLevel level, BlockPos pos, ItemStack heldItem, IntProvider amount) {
        int i = EnchantmentHelper.processBlockExperience(level, heldItem, amount.sample(level.getRandom()));
        if (i > 0) {
            this.popExperience(level, pos, i);
        }
    }

    record ShapePairKey(VoxelShape first, VoxelShape second) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Block.ShapePairKey block$shapepairkey
                && this.first == block$shapepairkey.first
                && this.second == block$shapepairkey.second;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this.first) * 31 + System.identityHashCode(this.second);
        }
    }
}
