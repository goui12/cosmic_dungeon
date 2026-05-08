package net.minecraft.world.level.material;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class FlowingFluid extends Fluid {
    public static final BooleanProperty FALLING = BlockStateProperties.FALLING;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_FLOWING;
    private static final int CACHE_SIZE = 200;
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(
        () -> {
            Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey> object2bytelinkedopenhashmap = new Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey>(
                200
            ) {
                @Override
                protected void rehash(int newSize) {
                }
            };
            object2bytelinkedopenhashmap.defaultReturnValue((byte)127);
            return object2bytelinkedopenhashmap;
        }
    );
    private final Map<FluidState, VoxelShape> shapes = Maps.newIdentityHashMap();

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
        builder.add(FALLING);
    }

    @Override
    public Vec3 getFlow(BlockGetter blockReader, BlockPos pos, FluidState fluidState) {
        double d0 = 0.0;
        double d1 = 0.0;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            blockpos$mutableblockpos.setWithOffset(pos, direction);
            FluidState fluidstate = blockReader.getFluidState(blockpos$mutableblockpos);
            if (this.affectsFlow(fluidstate)) {
                float f = fluidstate.getOwnHeight();
                float f1 = 0.0F;
                if (f == 0.0F) {
                    if (!blockReader.getBlockState(blockpos$mutableblockpos).blocksMotion()) {
                        BlockPos blockpos = blockpos$mutableblockpos.below();
                        FluidState fluidstate1 = blockReader.getFluidState(blockpos);
                        if (this.affectsFlow(fluidstate1)) {
                            f = fluidstate1.getOwnHeight();
                            if (f > 0.0F) {
                                f1 = fluidState.getOwnHeight() - (f - 0.8888889F);
                            }
                        }
                    }
                } else if (f > 0.0F) {
                    f1 = fluidState.getOwnHeight() - f;
                }

                if (f1 != 0.0F) {
                    d0 += direction.getStepX() * f1;
                    d1 += direction.getStepZ() * f1;
                }
            }
        }

        Vec3 vec3 = new Vec3(d0, 0.0, d1);
        if (fluidState.getValue(FALLING)) {
            for (Direction direction1 : Direction.Plane.HORIZONTAL) {
                blockpos$mutableblockpos.setWithOffset(pos, direction1);
                if (this.isSolidFace(blockReader, blockpos$mutableblockpos, direction1)
                    || this.isSolidFace(blockReader, blockpos$mutableblockpos.above(), direction1)) {
                    vec3 = vec3.normalize().add(0.0, -6.0, 0.0);
                    break;
                }
            }
        }

        return vec3.normalize();
    }

    private boolean affectsFlow(FluidState state) {
        return state.isEmpty() || state.getType().isSame(this);
    }

    protected boolean isSolidFace(BlockGetter level, BlockPos neighborPos, Direction side) {
        BlockState blockstate = level.getBlockState(neighborPos);
        FluidState fluidstate = level.getFluidState(neighborPos);
        if (fluidstate.getType().isSame(this)) {
            return false;
        } else if (side == Direction.UP) {
            return true;
        } else {
            return blockstate.getBlock() instanceof IceBlock ? false : blockstate.isFaceSturdy(level, neighborPos, side);
        }
    }

    protected void spread(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (!fluidState.isEmpty()) {
            BlockPos blockpos = pos.below();
            BlockState blockstate = level.getBlockState(blockpos);
            FluidState fluidstate = blockstate.getFluidState();
            if (this.canMaybePassThrough(level, pos, blockState, Direction.DOWN, blockpos, blockstate, fluidstate)) {
                FluidState fluidstate1 = this.getNewLiquid(level, blockpos, blockstate);
                Fluid fluid = fluidstate1.getType();
                if (fluidstate.canBeReplacedWith(level, blockpos, fluid, Direction.DOWN) && canHoldSpecificFluid(level, blockpos, blockstate, fluid)) {
                    this.spreadTo(level, blockpos, blockstate, Direction.DOWN, fluidstate1);
                    if (this.sourceNeighborCount(level, pos) >= 3) {
                        this.spreadToSides(level, pos, fluidState, blockState);
                    }

                    return;
                }
            }

            if (fluidState.isSource() || !this.isWaterHole(level, pos, blockState, blockpos, blockstate)) {
                this.spreadToSides(level, pos, fluidState, blockState);
            }
        }
    }

    private void spreadToSides(ServerLevel level, BlockPos pos, FluidState fluidState, BlockState blockState) {
        int i = fluidState.getAmount() - this.getDropOff(level);
        if (fluidState.getValue(FALLING)) {
            i = 7;
        }

        if (i > 0) {
            Map<Direction, FluidState> map = this.getSpread(level, pos, blockState);

            for (Entry<Direction, FluidState> entry : map.entrySet()) {
                Direction direction = entry.getKey();
                FluidState fluidstate = entry.getValue();
                BlockPos blockpos = pos.relative(direction);
                this.spreadTo(level, blockpos, level.getBlockState(blockpos), direction, fluidstate);
            }
        }
    }

    protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state) {
        int i = 0;
        int j = 0;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = blockpos$mutableblockpos.setWithOffset(pos, direction);
            BlockState blockstate = level.getBlockState(blockpos);
            FluidState fluidstate = blockstate.getFluidState();
            if (fluidstate.getType().isSame(this) && canPassThroughWall(direction, level, pos, state, blockpos, blockstate)) {
                if (fluidstate.isSource() && net.neoforged.neoforge.event.EventHooks.canCreateFluidSource(level, blockpos, blockstate)) {
                    j++;
                }

                i = Math.max(i, fluidstate.getAmount());
            }
        }

        if (j >= 2) {
            BlockState blockstate1 = level.getBlockState(blockpos$mutableblockpos.setWithOffset(pos, Direction.DOWN));
            FluidState fluidstate1 = blockstate1.getFluidState();
            if (blockstate1.isSolid() || this.isSourceBlockOfThisType(fluidstate1)) {
                return this.getSource(false);
            }
        }

        BlockPos blockpos1 = blockpos$mutableblockpos.setWithOffset(pos, Direction.UP);
        BlockState blockstate2 = level.getBlockState(blockpos1);
        FluidState fluidstate2 = blockstate2.getFluidState();
        if (!fluidstate2.isEmpty()
            && fluidstate2.getType().isSame(this)
            && canPassThroughWall(Direction.UP, level, pos, state, blockpos1, blockstate2)) {
            return this.getFlowing(8, true);
        } else {
            int k = i - this.getDropOff(level);
            return k <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(k, false);
        }
    }

    private static boolean canPassThroughWall(
        Direction direction, BlockGetter level, BlockPos pos, BlockState state, BlockPos spreadPos, BlockState spreadState
    ) {
        if (!SharedConstants.DEBUG_DISABLE_LIQUID_SPREADING && (!SharedConstants.DEBUG_ONLY_GENERATE_HALF_THE_WORLD || spreadPos.getZ() >= 0)) {
            VoxelShape voxelshape = spreadState.getCollisionShape(level, spreadPos);
            if (voxelshape == Shapes.block()) {
                return false;
            } else {
                VoxelShape voxelshape1 = state.getCollisionShape(level, pos);
                if (voxelshape1 == Shapes.block()) {
                    return false;
                } else if (voxelshape1 == Shapes.empty() && voxelshape == Shapes.empty()) {
                    return true;
                } else {
                    Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey> object2bytelinkedopenhashmap;
                    if (!state.getBlock().hasDynamicShape() && !spreadState.getBlock().hasDynamicShape()) {
                        object2bytelinkedopenhashmap = OCCLUSION_CACHE.get();
                    } else {
                        object2bytelinkedopenhashmap = null;
                    }

                    FlowingFluid.BlockStatePairKey flowingfluid$blockstatepairkey;
                    if (object2bytelinkedopenhashmap != null) {
                        flowingfluid$blockstatepairkey = new FlowingFluid.BlockStatePairKey(state, spreadState, direction);
                        byte b0 = object2bytelinkedopenhashmap.getAndMoveToFirst(flowingfluid$blockstatepairkey);
                        if (b0 != 127) {
                            return b0 != 0;
                        }
                    } else {
                        flowingfluid$blockstatepairkey = null;
                    }

                    boolean flag = !Shapes.mergedFaceOccludes(voxelshape1, voxelshape, direction);
                    if (object2bytelinkedopenhashmap != null) {
                        if (object2bytelinkedopenhashmap.size() == 200) {
                            object2bytelinkedopenhashmap.removeLastByte();
                        }

                        object2bytelinkedopenhashmap.putAndMoveToFirst(flowingfluid$blockstatepairkey, (byte)(flag ? 1 : 0));
                    }

                    return flag;
                }
            }
        } else {
            return false;
        }
    }

    public abstract Fluid getFlowing();

    public FluidState getFlowing(int level, boolean falling) {
        return this.getFlowing().defaultFluidState().setValue(LEVEL, level).setValue(FALLING, falling);
    }

    public abstract Fluid getSource();

    public FluidState getSource(boolean falling) {
        return this.getSource().defaultFluidState().setValue(FALLING, falling);
    }

    @Override
    public boolean canConvertToSource(FluidState state, ServerLevel level, BlockPos pos) {
        return this.canConvertToSource(level);
    }

    /**
     * @deprecated Forge: Use {@link #canConvertToSource(FluidState, ServerLevel,
     *             BlockPos)} instead.
     */
    @Deprecated
    protected abstract boolean canConvertToSource(ServerLevel level);

    protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction, FluidState fluidState) {
        if (blockState.getBlock() instanceof LiquidBlockContainer liquidblockcontainer) {
            liquidblockcontainer.placeLiquid(level, pos, blockState, fluidState);
        } else {
            if (!blockState.isAir()) {
                this.beforeDestroyingBlock(level, pos, blockState);
            }

            level.setBlock(pos, fluidState.createLegacyBlock(), 3);
        }
    }

    protected abstract void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state);

    protected int getSlopeDistance(
        LevelReader level, BlockPos pos, int depth, Direction p_direction, BlockState state, FlowingFluid.SpreadContext spreadContext
    ) {
        int i = 1000;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != p_direction) {
                BlockPos blockpos = pos.relative(direction);
                BlockState blockstate = spreadContext.getBlockState(blockpos);
                FluidState fluidstate = blockstate.getFluidState();
                if (this.canPassThrough(level, this.getFlowing(), pos, state, direction, blockpos, blockstate, fluidstate)) {
                    if (spreadContext.isHole(blockpos)) {
                        return depth;
                    }

                    if (depth < this.getSlopeFindDistance(level)) {
                        int j = this.getSlopeDistance(level, blockpos, depth + 1, direction.getOpposite(), blockstate, spreadContext);
                        if (j < i) {
                            i = j;
                        }
                    }
                }
            }
        }

        return i;
    }

    boolean isWaterHole(BlockGetter level, BlockPos pos, BlockState state, BlockPos belowPos, BlockState belowState) {
        if (!canPassThroughWall(Direction.DOWN, level, pos, state, belowPos, belowState)) {
            return false;
        } else {
            return belowState.getFluidState().getType().isSame(this) ? true : canHoldFluid(level, belowPos, belowState, this.getFlowing());
        }
    }

    private boolean canPassThrough(
        BlockGetter level,
        Fluid fluid,
        BlockPos pos,
        BlockState state,
        Direction direction,
        BlockPos spreadPos,
        BlockState spreadState,
        FluidState fluidState
    ) {
        return this.canMaybePassThrough(level, pos, state, direction, spreadPos, spreadState, fluidState)
            && canHoldSpecificFluid(level, spreadPos, spreadState, fluid);
    }

    private boolean canMaybePassThrough(
        BlockGetter level, BlockPos pos, BlockState state, Direction direction, BlockPos spreadPos, BlockState spreadState, FluidState fluidState
    ) {
        return !this.isSourceBlockOfThisType(fluidState)
            && canHoldAnyFluid(spreadState)
            && canPassThroughWall(direction, level, pos, state, spreadPos, spreadState);
    }

    private boolean isSourceBlockOfThisType(FluidState state) {
        return state.getType().isSame(this) && state.isSource();
    }

    protected abstract int getSlopeFindDistance(LevelReader level);

    /**
     * Returns the number of immediately adjacent source blocks of the same fluid that lie on the horizontal plane.
     */
    private int sourceNeighborCount(LevelReader level, BlockPos pos) {
        int i = 0;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pos.relative(direction);
            FluidState fluidstate = level.getFluidState(blockpos);
            if (this.isSourceBlockOfThisType(fluidstate)) {
                i++;
            }
        }

        return i;
    }

    protected Map<Direction, FluidState> getSpread(ServerLevel level, BlockPos pos, BlockState state) {
        int i = 1000;
        Map<Direction, FluidState> map = Maps.newEnumMap(Direction.class);
        FlowingFluid.SpreadContext flowingfluid$spreadcontext = null;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pos.relative(direction);
            BlockState blockstate = level.getBlockState(blockpos);
            FluidState fluidstate = blockstate.getFluidState();
            if (this.canMaybePassThrough(level, pos, state, direction, blockpos, blockstate, fluidstate)) {
                FluidState fluidstate1 = this.getNewLiquid(level, blockpos, blockstate);
                if (canHoldSpecificFluid(level, blockpos, blockstate, fluidstate1.getType())) {
                    if (flowingfluid$spreadcontext == null) {
                        flowingfluid$spreadcontext = new FlowingFluid.SpreadContext(level, pos);
                    }

                    int j;
                    if (flowingfluid$spreadcontext.isHole(blockpos)) {
                        j = 0;
                    } else {
                        j = this.getSlopeDistance(level, blockpos, 1, direction.getOpposite(), blockstate, flowingfluid$spreadcontext);
                    }

                    if (j < i) {
                        map.clear();
                    }

                    if (j <= i) {
                        if (fluidstate.canBeReplacedWith(level, blockpos, fluidstate1.getType(), direction)) {
                            map.put(direction, fluidstate1);
                        }

                        i = j;
                    }
                }
            }
        }

        return map;
    }

    private static boolean canHoldAnyFluid(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LiquidBlockContainer) {
            return true;
        } else {
            return state.blocksMotion()
                ? false
                : !(block instanceof DoorBlock)
                    && !state.is(BlockTags.SIGNS)
                    && !state.is(Blocks.LADDER)
                    && !state.is(Blocks.SUGAR_CANE)
                    && !state.is(Blocks.BUBBLE_COLUMN)
                    && !state.is(Blocks.NETHER_PORTAL)
                    && !state.is(Blocks.END_PORTAL)
                    && !state.is(Blocks.END_GATEWAY)
                    && !state.is(Blocks.STRUCTURE_VOID);
        }
    }

    private static boolean canHoldFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return canHoldAnyFluid(state) && canHoldSpecificFluid(level, pos, state, fluid);
    }

    private static boolean canHoldSpecificFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return state.getBlock() instanceof LiquidBlockContainer liquidblockcontainer
            ? liquidblockcontainer.canPlaceLiquid(null, level, pos, state, fluid)
            : true;
    }

    protected abstract int getDropOff(LevelReader level);

    protected int getSpreadDelay(Level level, BlockPos pos, FluidState currentState, FluidState newState) {
        return this.getTickDelay(level);
    }

    @Override
    public void tick(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (!fluidState.isSource()) {
            FluidState fluidstate = this.getNewLiquid(level, pos, level.getBlockState(pos));
            int i = this.getSpreadDelay(level, pos, fluidState, fluidstate);
            if (fluidstate.isEmpty()) {
                fluidState = fluidstate;
                blockState = Blocks.AIR.defaultBlockState();
                level.setBlock(pos, blockState, 3);
            } else if (fluidstate != fluidState) {
                fluidState = fluidstate;
                blockState = fluidstate.createLegacyBlock();
                level.setBlock(pos, blockState, 3);
                level.scheduleTick(pos, fluidstate.getType(), i);
            }
        }

        this.spread(level, pos, blockState, fluidState);
    }

    protected static int getLegacyLevel(FluidState state) {
        return state.isSource() ? 0 : 8 - Math.min(state.getAmount(), 8) + (state.getValue(FALLING) ? 8 : 0);
    }

    private static boolean hasSameAbove(FluidState fluidState, BlockGetter level, BlockPos pos) {
        return fluidState.getType().isSame(level.getFluidState(pos.above()).getType());
    }

    @Override
    public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
        return hasSameAbove(state, level, pos) ? 1.0F : state.getOwnHeight();
    }

    @Override
    public float getOwnHeight(FluidState state) {
        return state.getAmount() / 9.0F;
    }

    @Override
    public abstract int getAmount(FluidState state);

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        return state.getAmount() == 9 && hasSameAbove(state, level, pos)
            ? Shapes.block()
            : this.shapes.computeIfAbsent(state, p_76073_ -> Shapes.box(0.0, 0.0, 0.0, 1.0, p_76073_.getHeight(level, pos), 1.0));
    }

    record BlockStatePairKey(BlockState first, BlockState second, Direction direction) {
        @Override
        public boolean equals(Object p_368747_) {
            return p_368747_ instanceof FlowingFluid.BlockStatePairKey flowingfluid$blockstatepairkey
                && this.first == flowingfluid$blockstatepairkey.first
                && this.second == flowingfluid$blockstatepairkey.second
                && this.direction == flowingfluid$blockstatepairkey.direction;
        }

        @Override
        public int hashCode() {
            int i = System.identityHashCode(this.first);
            i = 31 * i + System.identityHashCode(this.second);
            return 31 * i + this.direction.hashCode();
        }
    }

    protected class SpreadContext {
        private final BlockGetter level;
        private final BlockPos origin;
        private final Short2ObjectMap<BlockState> stateCache = new Short2ObjectOpenHashMap<>();
        private final Short2BooleanMap holeCache = new Short2BooleanOpenHashMap();

        SpreadContext(BlockGetter level, BlockPos origin) {
            this.level = level;
            this.origin = origin;
        }

        public BlockState getBlockState(BlockPos pos) {
            return this.getBlockState(pos, this.getCacheKey(pos));
        }

        private BlockState getBlockState(BlockPos pos, short cacheKey) {
            return this.stateCache.computeIfAbsent(cacheKey, p_361149_ -> this.level.getBlockState(pos));
        }

        public boolean isHole(BlockPos pos) {
            return this.holeCache.computeIfAbsent(this.getCacheKey(pos), p_363178_ -> {
                BlockState blockstate = this.getBlockState(pos, p_363178_);
                BlockPos blockpos = pos.below();
                BlockState blockstate1 = this.level.getBlockState(blockpos);
                return FlowingFluid.this.isWaterHole(this.level, pos, blockstate, blockpos, blockstate1);
            });
        }

        private short getCacheKey(BlockPos pos) {
            int i = pos.getX() - this.origin.getX();
            int j = pos.getZ() - this.origin.getZ();
            return (short)((i + 128 & 0xFF) << 8 | j + 128 & 0xFF);
        }
    }
}
