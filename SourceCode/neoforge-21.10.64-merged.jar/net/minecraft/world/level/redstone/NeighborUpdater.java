package net.minecraft.world.level.redstone;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public interface NeighborUpdater {
    Direction[] UPDATE_ORDER = new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};

    void shapeUpdate(Direction direction, BlockState state, BlockPos pos, BlockPos neighborPos, int flags, int recursionLevel);

    void neighborChanged(BlockPos pos, Block neighborBlock, @Nullable Orientation orientation);

    void neighborChanged(BlockState state, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston);

    default void updateNeighborsAtExceptFromFacing(BlockPos pos, Block block, @Nullable Direction facing, @Nullable Orientation orientation) {
        for (Direction direction : UPDATE_ORDER) {
            if (direction != facing) {
                this.neighborChanged(pos.relative(direction), block, null);
            }
        }
    }

    static void executeShapeUpdate(
        LevelAccessor level, Direction direction, BlockPos pos, BlockPos neighborPos, BlockState neighborState, int flags, int recursionLeft
    ) {
        BlockState blockstate = level.getBlockState(pos);
        if ((flags & 128) == 0 || !blockstate.is(Blocks.REDSTONE_WIRE)) {
            BlockState blockstate1 = blockstate.updateShape(level, level, pos, direction, neighborPos, neighborState, level.getRandom());
            Block.updateOrDestroy(blockstate, blockstate1, level, pos, flags, recursionLeft);
        }
    }

    static void executeUpdate(Level level, BlockState state, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        try {
            state.handleNeighborChanged(level, pos, neighborBlock, orientation, movedByPiston);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception while updating neighbours");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being updated");
            crashreportcategory.setDetail(
                "Source block type",
                () -> {
                    try {
                        return String.format(
                            Locale.ROOT,
                            "ID #%s (%s // %s)",
                            BuiltInRegistries.BLOCK.getKey(neighborBlock),
                            neighborBlock.getDescriptionId(),
                            neighborBlock.getClass().getCanonicalName()
                        );
                    } catch (Throwable throwable1) {
                        return "ID #" + BuiltInRegistries.BLOCK.getKey(neighborBlock);
                    }
                }
            );
            CrashReportCategory.populateBlockDetails(crashreportcategory, level, pos, state);
            throw new ReportedException(crashreport);
        }
    }
}
