package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlowerBedBlock extends VegetationBlock implements BonemealableBlock, SegmentableBlock {
    public static final MapCodec<FlowerBedBlock> CODEC = simpleCodec(FlowerBedBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty AMOUNT = BlockStateProperties.FLOWER_AMOUNT;
    private final Function<BlockState, VoxelShape> shapes;

    @Override
    public MapCodec<FlowerBedBlock> codec() {
        return CODEC;
    }

    public FlowerBedBlock(BlockBehaviour.Properties p_394040_) {
        super(p_394040_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(AMOUNT, 1));
        this.shapes = this.makeShapes();
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        return this.getShapeForEachState(this.getShapeCalculator(FACING, AMOUNT));
    }

    @Override
    public BlockState rotate(BlockState p_393932_, Rotation p_394491_) {
        return p_393932_.setValue(FACING, p_394491_.rotate(p_393932_.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState p_393796_, Mirror p_393802_) {
        return p_393796_.rotate(p_393802_.getRotation(p_393796_.getValue(FACING)));
    }

    @Override
    public boolean canBeReplaced(BlockState p_393549_, BlockPlaceContext p_393927_) {
        return this.canBeReplaced(p_393549_, p_393927_, AMOUNT) ? true : super.canBeReplaced(p_393549_, p_393927_);
    }

    @Override
    public VoxelShape getShape(BlockState p_393803_, BlockGetter p_394026_, BlockPos p_394322_, CollisionContext p_394474_) {
        return this.shapes.apply(p_393803_);
    }

    @Override
    public double getShapeHeight() {
        return 3.0;
    }

    @Override
    public IntegerProperty getSegmentAmountProperty() {
        return AMOUNT;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_393645_) {
        return this.getStateForPlacement(p_393645_, this, AMOUNT, FACING);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_393753_) {
        p_393753_.add(FACING, AMOUNT);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_393585_, BlockPos p_393679_, BlockState p_394653_) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level p_393489_, RandomSource p_394493_, BlockPos p_394603_, BlockState p_393700_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_394140_, RandomSource p_394672_, BlockPos p_393771_, BlockState p_394008_) {
        int i = p_394008_.getValue(AMOUNT);
        if (i < 4) {
            p_394140_.setBlock(p_393771_, p_394008_.setValue(AMOUNT, i + 1), 2);
        } else {
            popResource(p_394140_, p_393771_, new ItemStack(this));
        }
    }
}
