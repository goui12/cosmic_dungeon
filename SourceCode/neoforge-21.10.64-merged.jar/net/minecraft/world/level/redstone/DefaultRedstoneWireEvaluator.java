package net.minecraft.world.level.redstone;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DefaultRedstoneWireEvaluator extends RedstoneWireEvaluator {
    public DefaultRedstoneWireEvaluator(RedStoneWireBlock wireBlock) {
        super(wireBlock);
    }

    @Override
    public void updatePowerStrength(Level level, BlockPos pos, BlockState state, @Nullable Orientation orientation, boolean updateShape) {
        int i = this.calculateTargetStrength(level, pos);
        if (state.getValue(RedStoneWireBlock.POWER) != i) {
            if (level.getBlockState(pos) == state) {
                level.setBlock(pos, state.setValue(RedStoneWireBlock.POWER, i), 2);
            }

            Set<BlockPos> set = Sets.newHashSet();
            set.add(pos);

            for (Direction direction : Direction.values()) {
                set.add(pos.relative(direction));
            }

            for (BlockPos blockpos : set) {
                level.updateNeighborsAt(blockpos, this.wireBlock);
            }
        }
    }

    private int calculateTargetStrength(Level level, BlockPos pos) {
        int i = this.getBlockSignal(level, pos);
        return i == 15 ? i : Math.max(i, this.getIncomingWireSignal(level, pos));
    }
}
