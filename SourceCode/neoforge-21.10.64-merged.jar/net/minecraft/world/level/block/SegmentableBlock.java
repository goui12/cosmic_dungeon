package net.minecraft.world.level.block;

import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface SegmentableBlock {
    int MIN_SEGMENT = 1;
    int MAX_SEGMENT = 4;
    IntegerProperty AMOUNT = BlockStateProperties.SEGMENT_AMOUNT;

    default Function<BlockState, VoxelShape> getShapeCalculator(EnumProperty<Direction> directionProperty, IntegerProperty amountProperty) {
        Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.box(0.0, 0.0, 0.0, 8.0, this.getShapeHeight(), 8.0));
        return p_393772_ -> {
            VoxelShape voxelshape = Shapes.empty();
            Direction direction = p_393772_.getValue(directionProperty);
            int i = p_393772_.getValue(amountProperty);

            for (int j = 0; j < i; j++) {
                voxelshape = Shapes.or(voxelshape, map.get(direction));
                direction = direction.getCounterClockWise();
            }

            return voxelshape.singleEncompassing();
        };
    }

    default IntegerProperty getSegmentAmountProperty() {
        return AMOUNT;
    }

    default double getShapeHeight() {
        return 1.0;
    }

    default boolean canBeReplaced(BlockState state, BlockPlaceContext context, IntegerProperty amountProperty) {
        return !context.isSecondaryUseActive() && context.getItemInHand().is(state.getBlock().asItem()) && state.getValue(amountProperty) < 4;
    }

    default BlockState getStateForPlacement(BlockPlaceContext context, Block block, IntegerProperty amountProperty, EnumProperty<Direction> directionProperty) {
        BlockState blockstate = context.getLevel().getBlockState(context.getClickedPos());
        return blockstate.is(block)
            ? blockstate.setValue(amountProperty, Math.min(4, blockstate.getValue(amountProperty) + 1))
            : block.defaultBlockState().setValue(directionProperty, context.getHorizontalDirection().getOpposite());
    }
}
