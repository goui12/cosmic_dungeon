package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class WaterloggedTransparentBlock extends TransparentBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<WaterloggedTransparentBlock> CODEC = simpleCodec(WaterloggedTransparentBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    @Override
    protected MapCodec<? extends WaterloggedTransparentBlock> codec() {
        return CODEC;
    }

    public WaterloggedTransparentBlock(BlockBehaviour.Properties p_313902_) {
        super(p_313902_);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_313836_) {
        FluidState fluidstate = p_313836_.getLevel().getFluidState(p_313836_.getClickedPos());
        return super.getStateForPlacement(p_313836_).setValue(WATERLOGGED, fluidstate.is(Fluids.WATER));
    }

    @Override
    protected BlockState updateShape(
        BlockState p_313906_,
        LevelReader p_374178_,
        ScheduledTickAccess p_374293_,
        BlockPos p_313842_,
        Direction p_313739_,
        BlockPos p_313843_,
        BlockState p_313829_,
        RandomSource p_374433_
    ) {
        if (p_313906_.getValue(WATERLOGGED)) {
            p_374293_.scheduleTick(p_313842_, Fluids.WATER, Fluids.WATER.getTickDelay(p_374178_));
        }

        return super.updateShape(p_313906_, p_374178_, p_374293_, p_313842_, p_313739_, p_313843_, p_313829_, p_374433_);
    }

    @Override
    protected FluidState getFluidState(BlockState p_313789_) {
        return p_313789_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(true) : super.getFluidState(p_313789_);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_313896_) {
        p_313896_.add(WATERLOGGED);
    }
}
