package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FallenTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

public class FallenTreeFeature extends Feature<FallenTreeConfiguration> {
    private static final int STUMP_HEIGHT = 1;
    private static final int STUMP_HEIGHT_PLUS_EMPTY_SPACE = 2;
    private static final int FALLEN_LOG_MAX_FALL_HEIGHT_TO_GROUND = 5;
    private static final int FALLEN_LOG_MAX_GROUND_GAP = 2;
    private static final int FALLEN_LOG_MAX_SPACE_FROM_STUMP = 2;
    private static final int BLOCK_UPDATE_FLAGS = 19;

    public FallenTreeFeature(Codec<FallenTreeConfiguration> codec) {
        super(codec);
    }

    /**
     * Places the given feature at the given location.
     * During world generation, features are provided with a 3x3 region of chunks, centered on the chunk being generated, that they can safely generate into.
     *
     * @param context A context object with a reference to the level and the position
     *                the feature is being placed at
     */
    @Override
    public boolean place(FeaturePlaceContext<FallenTreeConfiguration> context) {
        this.placeFallenTree(context.config(), context.origin(), context.level(), context.random());
        return true;
    }

    private void placeFallenTree(FallenTreeConfiguration config, BlockPos origin, WorldGenLevel level, RandomSource random) {
        this.placeStump(config, level, random, origin.mutable());
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int i = config.logLength.sample(random) - 2;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = origin.relative(direction, 2 + random.nextInt(2)).mutable();
        this.setGroundHeightForFallenLogStartPos(level, blockpos$mutableblockpos);
        if (this.canPlaceEntireFallenLog(level, i, blockpos$mutableblockpos, direction)) {
            this.placeFallenLog(config, level, random, i, blockpos$mutableblockpos, direction);
        }
    }

    private void setGroundHeightForFallenLogStartPos(WorldGenLevel level, BlockPos.MutableBlockPos pos) {
        pos.move(Direction.UP, 1);

        for (int i = 0; i < 6; i++) {
            if (this.mayPlaceOn(level, pos)) {
                return;
            }

            pos.move(Direction.DOWN);
        }
    }

    private void placeStump(FallenTreeConfiguration config, WorldGenLevel level, RandomSource random, BlockPos.MutableBlockPos pos) {
        BlockPos blockpos = this.placeLogBlock(config, level, random, pos, Function.identity());
        this.decorateLogs(level, random, Set.of(blockpos), config.stumpDecorators);
    }

    private boolean canPlaceEntireFallenLog(WorldGenLevel level, int logLength, BlockPos.MutableBlockPos pos, Direction direction) {
        int i = 0;

        for (int j = 0; j < logLength; j++) {
            if (!TreeFeature.validTreePos(level, pos)) {
                return false;
            }

            if (!this.isOverSolidGround(level, pos)) {
                if (++i > 2) {
                    return false;
                }
            } else {
                i = 0;
            }

            pos.move(direction);
        }

        pos.move(direction.getOpposite(), logLength);
        return true;
    }

    private void placeFallenLog(
        FallenTreeConfiguration config,
        WorldGenLevel level,
        RandomSource random,
        int logLength,
        BlockPos.MutableBlockPos pos,
        Direction direction
    ) {
        Set<BlockPos> set = new HashSet<>();

        for (int i = 0; i < logLength; i++) {
            set.add(this.placeLogBlock(config, level, random, pos, getSidewaysStateModifier(direction)));
            pos.move(direction);
        }

        this.decorateLogs(level, random, set, config.logDecorators);
    }

    private boolean mayPlaceOn(LevelAccessor level, BlockPos pos) {
        return TreeFeature.validTreePos(level, pos) && this.isOverSolidGround(level, pos);
    }

    private boolean isOverSolidGround(LevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos, Direction.UP);
    }

    private BlockPos placeLogBlock(
        FallenTreeConfiguration config,
        WorldGenLevel level,
        RandomSource random,
        BlockPos.MutableBlockPos pos,
        Function<BlockState, BlockState> stateModifier
    ) {
        level.setBlock(pos, stateModifier.apply(config.trunkProvider.getState(random, pos)), 3);
        this.markAboveForPostProcessing(level, pos);
        return pos.immutable();
    }

    private void decorateLogs(WorldGenLevel level, RandomSource random, Set<BlockPos> logPositions, List<TreeDecorator> decorators) {
        if (!decorators.isEmpty()) {
            TreeDecorator.Context treedecorator$context = new TreeDecorator.Context(
                level, this.getDecorationSetter(level), random, logPositions, Set.of(), Set.of()
            );
            decorators.forEach(p_409702_ -> p_409702_.place(treedecorator$context));
        }
    }

    private BiConsumer<BlockPos, BlockState> getDecorationSetter(WorldGenLevel level) {
        return (p_409651_, p_410621_) -> level.setBlock(p_409651_, p_410621_, 19);
    }

    private static Function<BlockState, BlockState> getSidewaysStateModifier(Direction direction) {
        return p_410651_ -> p_410651_.trySetValue(RotatedPillarBlock.AXIS, direction.getAxis());
    }
}
