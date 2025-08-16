package net.goui.cosmicdungeon.block.custom.amethyst;

import net.goui.cosmicdungeon.common.color.AmethystColor;
import net.goui.cosmicdungeon.common.properties.ModProperties;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Colored version of an amethyst cluster.
 * Has FACING, WATERLOGGED, and COLOR properties.
 */
public class ColoredClusterBlock extends Block implements SimpleWaterloggedBlock {
    protected static final VoxelShape UP_AABB = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D);
    protected static final VoxelShape DOWN_AABB = Block.box(3.0D, 6.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(3.0D, 3.0D, 6.0D, 13.0D, 13.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(3.0D, 3.0D, 0.0D, 13.0D, 13.0D, 10.0D);
    protected static final VoxelShape WEST_AABB = Block.box(6.0D, 3.0D, 3.0D, 16.0D, 13.0D, 13.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 3.0D, 3.0D, 10.0D, 13.0D, 13.0D);

    public ColoredClusterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.UP)
                        .setValue(BlockStateProperties.WATERLOGGED, false)
                        .setValue(ModProperties.COLOR, AmethystColor.PURPLE)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING, BlockStateProperties.WATERLOGGED, ModProperties.COLOR);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getClickedFace();
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(BlockStateProperties.FACING, dir)
                .setValue(BlockStateProperties.WATERLOGGED, fluid.getType() == Fluids.WATER)
                .setValue(ModProperties.COLOR, AmethystColor.PURPLE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(BlockStateProperties.FACING)) {
            case UP -> UP_AABB;
            case DOWN -> DOWN_AABB;
            case NORTH -> NORTH_AABB;
            case SOUTH -> SOUTH_AABB;
            case WEST -> WEST_AABB;
            case EAST -> EAST_AABB;
        };
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }
}
