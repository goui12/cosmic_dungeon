package net.minecraft.world.level.redstone;

import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class CollectingNeighborUpdater implements NeighborUpdater {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Level level;
    private final int maxChainedNeighborUpdates;
    private final ArrayDeque<CollectingNeighborUpdater.NeighborUpdates> stack = new ArrayDeque<>();
    private final List<CollectingNeighborUpdater.NeighborUpdates> addedThisLayer = new ArrayList<>();
    private int count = 0;
    @Nullable
    private Consumer<BlockPos> debugListener;

    public CollectingNeighborUpdater(Level level, int maxChainedNeighborUpdates) {
        this.level = level;
        this.maxChainedNeighborUpdates = maxChainedNeighborUpdates;
    }

    public void setDebugListener(@Nullable Consumer<BlockPos> debugListener) {
        this.debugListener = debugListener;
    }

    @Override
    public void shapeUpdate(Direction direction, BlockState state, BlockPos pos, BlockPos neighborPos, int flags, int recursionLevel) {
        this.addAndRun(
            pos, new CollectingNeighborUpdater.ShapeUpdate(direction, state, pos.immutable(), neighborPos.immutable(), flags, recursionLevel)
        );
    }

    @Override
    public void neighborChanged(BlockPos pos, Block neighborBlock, @Nullable Orientation orientation) {
        this.addAndRun(pos, new CollectingNeighborUpdater.SimpleNeighborUpdate(pos, neighborBlock, orientation));
    }

    @Override
    public void neighborChanged(BlockState state, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        this.addAndRun(pos, new CollectingNeighborUpdater.FullNeighborUpdate(state, pos.immutable(), neighborBlock, orientation, movedByPiston));
    }

    @Override
    public void updateNeighborsAtExceptFromFacing(BlockPos pos, Block block, @Nullable Direction facing, @Nullable Orientation orientation) {
        this.addAndRun(pos, new CollectingNeighborUpdater.MultiNeighborUpdate(pos.immutable(), block, orientation, facing));
    }

    private void addAndRun(BlockPos pos, CollectingNeighborUpdater.NeighborUpdates updates) {
        boolean flag = this.count > 0;
        boolean flag1 = this.maxChainedNeighborUpdates >= 0 && this.count >= this.maxChainedNeighborUpdates;
        this.count++;
        if (!flag1) {
            if (flag) {
                this.addedThisLayer.add(updates);
            } else {
                this.stack.push(updates);
            }
        } else if (this.count - 1 == this.maxChainedNeighborUpdates) {
            LOGGER.error("Too many chained neighbor updates. Skipping the rest. First skipped position: {}", pos.toShortString());
        }

        if (!flag) {
            this.runUpdates();
        }
    }

    private void runUpdates() {
        try {
            while (!this.stack.isEmpty() || !this.addedThisLayer.isEmpty()) {
                for (int i = this.addedThisLayer.size() - 1; i >= 0; i--) {
                    this.stack.push(this.addedThisLayer.get(i));
                }

                this.addedThisLayer.clear();
                CollectingNeighborUpdater.NeighborUpdates collectingneighborupdater$neighborupdates = this.stack.peek();
                if (this.debugListener != null) {
                    collectingneighborupdater$neighborupdates.forEachUpdatedPos(this.debugListener);
                }

                while (this.addedThisLayer.isEmpty()) {
                    if (!collectingneighborupdater$neighborupdates.runNext(this.level)) {
                        this.stack.pop();
                        break;
                    }
                }
            }
        } finally {
            this.stack.clear();
            this.addedThisLayer.clear();
            this.count = 0;
        }
    }

    record FullNeighborUpdate(BlockState state, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston)
        implements CollectingNeighborUpdater.NeighborUpdates {
        @Override
        public boolean runNext(Level p_230683_) {
            NeighborUpdater.executeUpdate(p_230683_, this.state, this.pos, this.block, this.orientation, this.movedByPiston);
            return false;
        }

        @Override
        public void forEachUpdatedPos(Consumer<BlockPos> p_449368_) {
            p_449368_.accept(this.pos);
        }
    }

    static final class MultiNeighborUpdate implements CollectingNeighborUpdater.NeighborUpdates {
        private final BlockPos sourcePos;
        private final Block sourceBlock;
        @Nullable
        private Orientation orientation;
        @Nullable
        private final Direction skipDirection;
        private int idx = 0;

        MultiNeighborUpdate(BlockPos sourcePos, Block sourceBlock, @Nullable Orientation orientation, @Nullable Direction skipDirection) {
            this.sourcePos = sourcePos;
            this.sourceBlock = sourceBlock;
            this.orientation = orientation;
            this.skipDirection = skipDirection;
            if (NeighborUpdater.UPDATE_ORDER[this.idx] == skipDirection) {
                this.idx++;
            }
        }

        @Override
        public boolean runNext(Level level) {
            Direction direction = NeighborUpdater.UPDATE_ORDER[this.idx++];
            BlockPos blockpos = this.sourcePos.relative(direction);
            BlockState blockstate = level.getBlockState(blockpos);
            Orientation orientation = null;
            if (level.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS)) {
                if (this.orientation == null) {
                    this.orientation = ExperimentalRedstoneUtils.initialOrientation(
                        level, this.skipDirection == null ? null : this.skipDirection.getOpposite(), null
                    );
                }

                orientation = this.orientation.withFront(direction);
            }

            NeighborUpdater.executeUpdate(level, blockstate, blockpos, this.sourceBlock, orientation, false);
            if (this.idx < NeighborUpdater.UPDATE_ORDER.length && NeighborUpdater.UPDATE_ORDER[this.idx] == this.skipDirection) {
                this.idx++;
            }

            return this.idx < NeighborUpdater.UPDATE_ORDER.length;
        }

        @Override
        public void forEachUpdatedPos(Consumer<BlockPos> action) {
            for (Direction direction : NeighborUpdater.UPDATE_ORDER) {
                if (direction != this.skipDirection) {
                    BlockPos blockpos = this.sourcePos.relative(direction);
                    action.accept(blockpos);
                }
            }
        }
    }

    interface NeighborUpdates {
        boolean runNext(Level level);

        void forEachUpdatedPos(Consumer<BlockPos> action);
    }

    record ShapeUpdate(Direction direction, BlockState neighborState, BlockPos pos, BlockPos neighborPos, int updateFlags, int updateLimit)
        implements CollectingNeighborUpdater.NeighborUpdates {
        @Override
        public boolean runNext(Level p_230716_) {
            NeighborUpdater.executeShapeUpdate(p_230716_, this.direction, this.pos, this.neighborPos, this.neighborState, this.updateFlags, this.updateLimit);
            return false;
        }

        @Override
        public void forEachUpdatedPos(Consumer<BlockPos> p_449125_) {
            p_449125_.accept(this.pos);
        }
    }

    record SimpleNeighborUpdate(BlockPos pos, Block block, @Nullable Orientation orientation) implements CollectingNeighborUpdater.NeighborUpdates {
        @Override
        public boolean runNext(Level p_230734_) {
            BlockState blockstate = p_230734_.getBlockState(this.pos);
            NeighborUpdater.executeUpdate(p_230734_, blockstate, this.pos, this.block, this.orientation, false);
            return false;
        }

        @Override
        public void forEachUpdatedPos(Consumer<BlockPos> p_449859_) {
            p_449859_.accept(this.pos);
        }
    }
}
