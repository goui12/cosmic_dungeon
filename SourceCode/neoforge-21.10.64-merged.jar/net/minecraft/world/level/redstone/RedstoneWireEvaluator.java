package net.minecraft.world.level.redstone;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RedstoneWireEvaluator {
    protected final RedStoneWireBlock wireBlock;

    protected RedstoneWireEvaluator(RedStoneWireBlock wireBlock) {
        this.wireBlock = wireBlock;
    }

    public abstract void updatePowerStrength(Level level, BlockPos pos, BlockState state, @Nullable Orientation orientation, boolean updateShape);

    protected int getBlockSignal(Level level, BlockPos pos) {
        return this.wireBlock.getBlockSignal(level, pos);
    }

    protected int getWireSignal(BlockPos pos, BlockState state) {
        return state.is(this.wireBlock) ? state.getValue(RedStoneWireBlock.POWER) : 0;
    }

    protected int getIncomingWireSignal(Level level, BlockPos pos) {
        int i = 0;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pos.relative(direction);
            BlockState blockstate = level.getBlockState(blockpos);
            i = Math.max(i, this.getWireSignal(blockpos, blockstate));
            BlockPos blockpos1 = pos.above();
            if (blockstate.isRedstoneConductor(level, blockpos) && !level.getBlockState(blockpos1).isRedstoneConductor(level, blockpos1)) {
                BlockPos blockpos3 = blockpos.above();
                i = Math.max(i, this.getWireSignal(blockpos3, level.getBlockState(blockpos3)));
            } else if (!blockstate.isRedstoneConductor(level, blockpos)) {
                BlockPos blockpos2 = blockpos.below();
                i = Math.max(i, this.getWireSignal(blockpos2, level.getBlockState(blockpos2)));
            }
        }

        return Math.max(0, i - 1);
    }
}
