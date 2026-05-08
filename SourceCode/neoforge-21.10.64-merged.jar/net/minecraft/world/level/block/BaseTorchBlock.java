package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseTorchBlock extends Block {
    private static final VoxelShape SHAPE = Block.column(4.0, 0.0, 10.0);

    public BaseTorchBlock(BlockBehaviour.Properties p_304955_) {
        super(p_304955_);
    }

    @Override
    protected abstract MapCodec<? extends BaseTorchBlock> codec();

    @Override
    protected VoxelShape getShape(BlockState p_304673_, BlockGetter p_304919_, BlockPos p_304930_, CollisionContext p_304757_) {
        return SHAPE;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_304418_,
        LevelReader p_374159_,
        ScheduledTickAccess p_374152_,
        BlockPos p_304633_,
        Direction p_304475_,
        BlockPos p_304603_,
        BlockState p_304669_,
        RandomSource p_374111_
    ) {
        return p_304475_ == Direction.DOWN && !this.canSurvive(p_304418_, p_374159_, p_304633_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_304418_, p_374159_, p_374152_, p_304633_, p_304475_, p_304603_, p_304669_, p_374111_);
    }

    @Override
    protected boolean canSurvive(BlockState p_304413_, LevelReader p_304885_, BlockPos p_304808_) {
        return canSupportCenter(p_304885_, p_304808_.below(), Direction.UP);
    }
}
