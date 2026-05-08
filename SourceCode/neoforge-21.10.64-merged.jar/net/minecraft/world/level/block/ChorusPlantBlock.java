package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class ChorusPlantBlock extends PipeBlock {
    public static final MapCodec<ChorusPlantBlock> CODEC = simpleCodec(ChorusPlantBlock::new);

    @Override
    public MapCodec<ChorusPlantBlock> codec() {
        return CODEC;
    }

    public ChorusPlantBlock(BlockBehaviour.Properties p_51707_) {
        super(10.0F, p_51707_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return getStateWithConnections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    public static BlockState getStateWithConnections(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState blockstate = level.getBlockState(pos.below());
        BlockState blockstate1 = level.getBlockState(pos.above());
        BlockState blockstate2 = level.getBlockState(pos.north());
        BlockState blockstate3 = level.getBlockState(pos.east());
        BlockState blockstate4 = level.getBlockState(pos.south());
        BlockState blockstate5 = level.getBlockState(pos.west());
        Block block = state.getBlock();
        var soilDecision = blockstate.canSustainPlant(level, pos.below(), Direction.UP, state);
        return state.trySetValue(DOWN, blockstate.is(block) || blockstate.is(Blocks.CHORUS_FLOWER) || blockstate.is(Blocks.END_STONE) || soilDecision.isTrue())
            .trySetValue(UP, blockstate1.is(block) || blockstate1.is(Blocks.CHORUS_FLOWER))
            .trySetValue(NORTH, blockstate2.is(block) || blockstate2.is(Blocks.CHORUS_FLOWER))
            .trySetValue(EAST, blockstate3.is(block) || blockstate3.is(Blocks.CHORUS_FLOWER))
            .trySetValue(SOUTH, blockstate4.is(block) || blockstate4.is(Blocks.CHORUS_FLOWER))
            .trySetValue(WEST, blockstate5.is(block) || blockstate5.is(Blocks.CHORUS_FLOWER));
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess scheduledTickAccess,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random
    ) {
        if (!state.canSurvive(level, pos)) {
            scheduledTickAccess.scheduleTick(pos, this, 1);
            return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
        } else {
            boolean flag = neighborState.is(this) || neighborState.is(Blocks.CHORUS_FLOWER) || direction == Direction.DOWN && neighborState.is(Blocks.END_STONE);
            if (direction == Direction.DOWN) {
                var soilDecision = neighborState.canSustainPlant(level, neighborPos.relative(direction), direction.getOpposite(), state);
                if (!soilDecision.isDefault()) {
                    flag = soilDecision.isTrue();
                }
            }
            return state.setValue(PROPERTY_BY_DIRECTION.get(direction), flag);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos.below());
        boolean flag = !level.getBlockState(pos.above()).isAir() && !blockstate.isAir();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pos.relative(direction);
            BlockState blockstate1 = level.getBlockState(blockpos);
            if (blockstate1.is(this)) {
                if (flag) {
                    return false;
                }

                BlockState blockstate2 = level.getBlockState(blockpos.below());
                if (blockstate2.is(this) || blockstate2.is(Blocks.END_STONE)) {
                    return true;
                }
            }
        }

        var soilDecision = blockstate.canSustainPlant(level, pos.below(), Direction.UP, state);
        if (!soilDecision.isDefault()) return soilDecision.isTrue();
        return blockstate.is(this) || blockstate.is(Blocks.END_STONE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }
}
