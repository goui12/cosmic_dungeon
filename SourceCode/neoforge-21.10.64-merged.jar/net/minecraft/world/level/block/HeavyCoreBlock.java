package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HeavyCoreBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<HeavyCoreBlock> CODEC = simpleCodec(HeavyCoreBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 8.0);

    public HeavyCoreBlock(BlockBehaviour.Properties p_333904_) {
        super(p_333904_);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    public MapCodec<HeavyCoreBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_333925_) {
        p_333925_.add(WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_338756_,
        LevelReader p_374371_,
        ScheduledTickAccess p_374063_,
        BlockPos p_338346_,
        Direction p_338189_,
        BlockPos p_338401_,
        BlockState p_338723_,
        RandomSource p_374262_
    ) {
        if (p_338756_.getValue(WATERLOGGED)) {
            p_374063_.scheduleTick(p_338346_, Fluids.WATER, Fluids.WATER.getTickDelay(p_374371_));
        }

        return super.updateShape(p_338756_, p_374371_, p_374063_, p_338346_, p_338189_, p_338401_, p_338723_, p_374262_);
    }

    @Override
    protected FluidState getFluidState(BlockState p_335518_) {
        return p_335518_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_335518_);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_338691_) {
        FluidState fluidstate = p_338691_.getLevel().getFluidState(p_338691_.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluidstate.is(Fluids.WATER));
    }

    @Override
    protected VoxelShape getShape(BlockState p_334026_, BlockGetter p_334049_, BlockPos p_334056_, CollisionContext p_333870_) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState p_333758_, PathComputationType p_333728_) {
        return false;
    }
}
