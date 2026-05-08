package net.minecraft.world.level.portal;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;

public class PortalShape {
    private static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;
    private static final BlockBehaviour.StatePredicate FRAME = net.neoforged.neoforge.common.extensions.IBlockStateExtension::isPortalFrame;
    private static final float SAFE_TRAVEL_MAX_ENTITY_XY = 4.0F;
    private static final double SAFE_TRAVEL_MAX_VERTICAL_DELTA = 1.0;
    private final Direction.Axis axis;
    private final Direction rightDir;
    private final int numPortalBlocks;
    private final BlockPos bottomLeft;
    private final int height;
    private final int width;

    private PortalShape(Direction.Axis axis, int numPortalBlocks, Direction rightDir, BlockPos bottomLeft, int width, int height) {
        this.axis = axis;
        this.numPortalBlocks = numPortalBlocks;
        this.rightDir = rightDir;
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
    }

    public static Optional<PortalShape> findEmptyPortalShape(LevelAccessor level, BlockPos bottomLeft, Direction.Axis axis) {
        return findPortalShape(level, bottomLeft, p_77727_ -> p_77727_.isValid() && p_77727_.numPortalBlocks == 0, axis);
    }

    public static Optional<PortalShape> findPortalShape(LevelAccessor level, BlockPos bottomLeft, Predicate<PortalShape> predicate, Direction.Axis axis) {
        Optional<PortalShape> optional = Optional.of(findAnyShape(level, bottomLeft, axis)).filter(predicate);
        if (optional.isPresent()) {
            return optional;
        } else {
            Direction.Axis direction$axis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            return Optional.of(findAnyShape(level, bottomLeft, direction$axis)).filter(predicate);
        }
    }

    public static PortalShape findAnyShape(BlockGetter level, BlockPos bottomLeft, Direction.Axis axis) {
        Direction direction = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos blockpos = calculateBottomLeft(level, direction, bottomLeft);
        if (blockpos == null) {
            return new PortalShape(axis, 0, direction, bottomLeft, 0, 0);
        } else {
            int i = calculateWidth(level, blockpos, direction);
            if (i == 0) {
                return new PortalShape(axis, 0, direction, blockpos, 0, 0);
            } else {
                MutableInt mutableint = new MutableInt();
                int j = calculateHeight(level, blockpos, direction, i, mutableint);
                return new PortalShape(axis, mutableint.getValue(), direction, blockpos, i, j);
            }
        }
    }

    @Nullable
    private static BlockPos calculateBottomLeft(BlockGetter level, Direction p_direction, BlockPos pos) {
        int i = Math.max(level.getMinY(), pos.getY() - 21);

        while (pos.getY() > i && isEmpty(level.getBlockState(pos.below()))) {
            pos = pos.below();
        }

        Direction direction = p_direction.getOpposite();
        int j = getDistanceUntilEdgeAboveFrame(level, pos, direction) - 1;
        return j < 0 ? null : pos.relative(direction, j);
    }

    private static int calculateWidth(BlockGetter level, BlockPos bottomLeft, Direction direction) {
        int i = getDistanceUntilEdgeAboveFrame(level, bottomLeft, direction);
        return i >= 2 && i <= 21 ? i : 0;
    }

    private static int getDistanceUntilEdgeAboveFrame(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int i = 0; i <= 21; i++) {
            blockpos$mutableblockpos.set(pos).move(direction, i);
            BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);
            if (!isEmpty(blockstate)) {
                if (FRAME.test(blockstate, level, blockpos$mutableblockpos)) {
                    return i;
                }
                break;
            }

            BlockState blockstate1 = level.getBlockState(blockpos$mutableblockpos.move(Direction.DOWN));
            if (!FRAME.test(blockstate1, level, blockpos$mutableblockpos)) {
                break;
            }
        }

        return 0;
    }

    private static int calculateHeight(BlockGetter level, BlockPos pos, Direction direction, int width, MutableInt portalBlocks) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        int i = getDistanceUntilTop(level, pos, direction, blockpos$mutableblockpos, width, portalBlocks);
        return i >= 3 && i <= 21 && hasTopFrame(level, pos, direction, blockpos$mutableblockpos, width, i) ? i : 0;
    }

    private static boolean hasTopFrame(
        BlockGetter level, BlockPos pos, Direction direction, BlockPos.MutableBlockPos checkPos, int width, int distanceUntilTop
    ) {
        for (int i = 0; i < width; i++) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = checkPos.set(pos).move(Direction.UP, distanceUntilTop).move(direction, i);
            if (!FRAME.test(level.getBlockState(blockpos$mutableblockpos), level, blockpos$mutableblockpos)) {
                return false;
            }
        }

        return true;
    }

    private static int getDistanceUntilTop(
        BlockGetter level, BlockPos pos, Direction direction, BlockPos.MutableBlockPos checkPos, int width, MutableInt portalBlocks
    ) {
        for (int i = 0; i < 21; i++) {
            checkPos.set(pos).move(Direction.UP, i).move(direction, -1);
            if (!FRAME.test(level.getBlockState(checkPos), level, checkPos)) {
                return i;
            }

            checkPos.set(pos).move(Direction.UP, i).move(direction, width);
            if (!FRAME.test(level.getBlockState(checkPos), level, checkPos)) {
                return i;
            }

            for (int j = 0; j < width; j++) {
                checkPos.set(pos).move(Direction.UP, i).move(direction, j);
                BlockState blockstate = level.getBlockState(checkPos);
                if (!isEmpty(blockstate)) {
                    return i;
                }

                if (blockstate.is(Blocks.NETHER_PORTAL)) {
                    portalBlocks.increment();
                }
            }
        }

        return 21;
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.is(Blocks.NETHER_PORTAL);
    }

    public boolean isValid() {
        return this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
    }

    public void createPortalBlocks(LevelAccessor level) {
        BlockState blockstate = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, this.axis);
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
            .forEach(p_374024_ -> level.setBlock(p_374024_, blockstate, 18));
    }

    public boolean isComplete() {
        return this.isValid() && this.numPortalBlocks == this.width * this.height;
    }

    public static Vec3 getRelativePosition(BlockUtil.FoundRectangle foundRectangle, Direction.Axis axis, Vec3 pos, EntityDimensions entityDimensions) {
        double d0 = (double)foundRectangle.axis1Size - entityDimensions.width();
        double d1 = (double)foundRectangle.axis2Size - entityDimensions.height();
        BlockPos blockpos = foundRectangle.minCorner;
        double d2;
        if (d0 > 0.0) {
            double d3 = blockpos.get(axis) + entityDimensions.width() / 2.0;
            d2 = Mth.clamp(Mth.inverseLerp(pos.get(axis) - d3, 0.0, d0), 0.0, 1.0);
        } else {
            d2 = 0.5;
        }

        double d5;
        if (d1 > 0.0) {
            Direction.Axis direction$axis = Direction.Axis.Y;
            d5 = Mth.clamp(Mth.inverseLerp(pos.get(direction$axis) - blockpos.get(direction$axis), 0.0, d1), 0.0, 1.0);
        } else {
            d5 = 0.0;
        }

        Direction.Axis direction$axis1 = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        double d4 = pos.get(direction$axis1) - (blockpos.get(direction$axis1) + 0.5);
        return new Vec3(d2, d5, d4);
    }

    public static Vec3 findCollisionFreePosition(Vec3 pos, ServerLevel level, Entity entity, EntityDimensions dimensions) {
        if (!(dimensions.width() > 4.0F) && !(dimensions.height() > 4.0F)) {
            double d0 = dimensions.height() / 2.0;
            Vec3 vec3 = pos.add(0.0, d0, 0.0);
            VoxelShape voxelshape = Shapes.create(AABB.ofSize(vec3, dimensions.width(), 0.0, dimensions.width()).expandTowards(0.0, 1.0, 0.0).inflate(1.0E-6));
            Optional<Vec3> optional = level.findFreePosition(entity, voxelshape, vec3, dimensions.width(), dimensions.height(), dimensions.width());
            Optional<Vec3> optional1 = optional.map(p_259019_ -> p_259019_.subtract(0.0, d0, 0.0));
            return optional1.orElse(pos);
        } else {
            return pos;
        }
    }
}
