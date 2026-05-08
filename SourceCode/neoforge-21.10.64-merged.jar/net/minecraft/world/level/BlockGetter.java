package net.minecraft.world.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface BlockGetter extends LevelHeightAccessor, net.neoforged.neoforge.common.extensions.IBlockGetterExtension {
    @Nullable
    BlockEntity getBlockEntity(BlockPos pos);

    default <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> blockEntityType) {
        BlockEntity blockentity = this.getBlockEntity(pos);
        return blockentity != null && blockentity.getType() == blockEntityType ? Optional.of((T)blockentity) : Optional.empty();
    }

    BlockState getBlockState(BlockPos pos);

    FluidState getFluidState(BlockPos pos);

    default int getLightEmission(BlockPos pos) {
        return this.getBlockState(pos).getLightEmission(this, pos);
    }

    default Stream<BlockState> getBlockStates(AABB area) {
        return BlockPos.betweenClosedStream(area).map(this::getBlockState);
    }

    default BlockHitResult isBlockInLine(ClipBlockStateContext context) {
        return traverseBlocks(
            context.getFrom(),
            context.getTo(),
            context,
            (p_360108_, p_360109_) -> {
                BlockState blockstate = this.getBlockState(p_360109_);
                Vec3 vec3 = p_360108_.getFrom().subtract(p_360108_.getTo());
                return p_360108_.isTargetBlock().test(blockstate)
                    ? new BlockHitResult(
                        p_360108_.getTo(), Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(p_360108_.getTo()), false
                    )
                    : null;
            },
            p_360111_ -> {
                Vec3 vec3 = p_360111_.getFrom().subtract(p_360111_.getTo());
                return BlockHitResult.miss(p_360111_.getTo(), Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(p_360111_.getTo()));
            }
        );
    }

    /**
     * Checks if there's block between {@code from} and {@code to} of context.
     * This uses the collision shape of provided block.
     */
    default BlockHitResult clip(ClipContext context) {
        return traverseBlocks(context.getFrom(), context.getTo(), context, (p_151359_, p_151360_) -> {
            BlockState blockstate = this.getBlockState(p_151360_);
            FluidState fluidstate = this.getFluidState(p_151360_);
            Vec3 vec3 = p_151359_.getFrom();
            Vec3 vec31 = p_151359_.getTo();
            VoxelShape voxelshape = p_151359_.getBlockShape(blockstate, this, p_151360_);
            BlockHitResult blockhitresult = this.clipWithInteractionOverride(vec3, vec31, p_151360_, voxelshape, blockstate);
            VoxelShape voxelshape1 = p_151359_.getFluidShape(fluidstate, this, p_151360_);
            BlockHitResult blockhitresult1 = voxelshape1.clip(vec3, vec31, p_151360_);
            double d0 = blockhitresult == null ? Double.MAX_VALUE : p_151359_.getFrom().distanceToSqr(blockhitresult.getLocation());
            double d1 = blockhitresult1 == null ? Double.MAX_VALUE : p_151359_.getFrom().distanceToSqr(blockhitresult1.getLocation());
            return d0 <= d1 ? blockhitresult : blockhitresult1;
        }, p_360110_ -> {
            Vec3 vec3 = p_360110_.getFrom().subtract(p_360110_.getTo());
            return BlockHitResult.miss(p_360110_.getTo(), Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(p_360110_.getTo()));
        });
    }

    @Nullable
    default BlockHitResult clipWithInteractionOverride(Vec3 startVec, Vec3 endVec, BlockPos pos, VoxelShape shape, BlockState state) {
        BlockHitResult blockhitresult = shape.clip(startVec, endVec, pos);
        if (blockhitresult != null) {
            BlockHitResult blockhitresult1 = state.getInteractionShape(this, pos).clip(startVec, endVec, pos);
            if (blockhitresult1 != null
                && blockhitresult1.getLocation().subtract(startVec).lengthSqr() < blockhitresult.getLocation().subtract(startVec).lengthSqr()) {
                return blockhitresult.withDirection(blockhitresult1.getDirection());
            }
        }

        return blockhitresult;
    }

    default double getBlockFloorHeight(VoxelShape shape, Supplier<VoxelShape> belowShapeSupplier) {
        if (!shape.isEmpty()) {
            return shape.max(Direction.Axis.Y);
        } else {
            double d0 = belowShapeSupplier.get().max(Direction.Axis.Y);
            return d0 >= 1.0 ? d0 - 1.0 : Double.NEGATIVE_INFINITY;
        }
    }

    default double getBlockFloorHeight(BlockPos pos) {
        return this.getBlockFloorHeight(this.getBlockState(pos).getCollisionShape(this, pos), () -> {
            BlockPos blockpos = pos.below();
            return this.getBlockState(blockpos).getCollisionShape(this, blockpos);
        });
    }

    static <T, C> T traverseBlocks(Vec3 from, Vec3 to, C context, BiFunction<C, BlockPos, T> tester, Function<C, T> onFail) {
        if (from.equals(to)) {
            return onFail.apply(context);
        } else {
            double d0 = Mth.lerp(-1.0E-7, to.x, from.x);
            double d1 = Mth.lerp(-1.0E-7, to.y, from.y);
            double d2 = Mth.lerp(-1.0E-7, to.z, from.z);
            double d3 = Mth.lerp(-1.0E-7, from.x, to.x);
            double d4 = Mth.lerp(-1.0E-7, from.y, to.y);
            double d5 = Mth.lerp(-1.0E-7, from.z, to.z);
            int i = Mth.floor(d3);
            int j = Mth.floor(d4);
            int k = Mth.floor(d5);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(i, j, k);
            T t = tester.apply(context, blockpos$mutableblockpos);
            if (t != null) {
                return t;
            } else {
                double d6 = d0 - d3;
                double d7 = d1 - d4;
                double d8 = d2 - d5;
                int l = Mth.sign(d6);
                int i1 = Mth.sign(d7);
                int j1 = Mth.sign(d8);
                double d9 = l == 0 ? Double.MAX_VALUE : l / d6;
                double d10 = i1 == 0 ? Double.MAX_VALUE : i1 / d7;
                double d11 = j1 == 0 ? Double.MAX_VALUE : j1 / d8;
                double d12 = d9 * (l > 0 ? 1.0 - Mth.frac(d3) : Mth.frac(d3));
                double d13 = d10 * (i1 > 0 ? 1.0 - Mth.frac(d4) : Mth.frac(d4));
                double d14 = d11 * (j1 > 0 ? 1.0 - Mth.frac(d5) : Mth.frac(d5));

                while (d12 <= 1.0 || d13 <= 1.0 || d14 <= 1.0) {
                    if (d12 < d13) {
                        if (d12 < d14) {
                            i += l;
                            d12 += d9;
                        } else {
                            k += j1;
                            d14 += d11;
                        }
                    } else if (d13 < d14) {
                        j += i1;
                        d13 += d10;
                    } else {
                        k += j1;
                        d14 += d11;
                    }

                    T t1 = tester.apply(context, blockpos$mutableblockpos.set(i, j, k));
                    if (t1 != null) {
                        return t1;
                    }
                }

                return onFail.apply(context);
            }
        }
    }

    static boolean forEachBlockIntersectedBetween(Vec3 from, Vec3 to, AABB boundingBox, BlockGetter.BlockStepVisitor visitor) {
        Vec3 vec3 = to.subtract(from);
        if (vec3.lengthSqr() < Mth.square(1.0E-5F)) {
            for (BlockPos blockpos2 : BlockPos.betweenClosed(boundingBox)) {
                if (!visitor.visit(blockpos2, 0)) {
                    return false;
                }
            }

            return true;
        } else {
            LongSet longset = new LongOpenHashSet();

            for (BlockPos blockpos : BlockPos.betweenCornersInDirection(boundingBox.move(vec3.scale(-1.0)), vec3)) {
                if (!visitor.visit(blockpos, 0)) {
                    return false;
                }

                longset.add(blockpos.asLong());
            }

            int i = addCollisionsAlongTravel(longset, vec3, boundingBox, visitor);
            if (i < 0) {
                return false;
            } else {
                for (BlockPos blockpos1 : BlockPos.betweenCornersInDirection(boundingBox, vec3)) {
                    if (longset.add(blockpos1.asLong()) && !visitor.visit(blockpos1, i + 1)) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    private static int addCollisionsAlongTravel(LongSet visited, Vec3 travelVector, AABB boundingBox, BlockGetter.BlockStepVisitor visitor) {
        double d0 = boundingBox.getXsize();
        double d1 = boundingBox.getYsize();
        double d2 = boundingBox.getZsize();
        Vec3i vec3i = getFurthestCorner(travelVector);
        Vec3 vec3 = boundingBox.getCenter();
        Vec3 vec31 = new Vec3(vec3.x() + d0 * 0.5 * vec3i.getX(), vec3.y() + d1 * 0.5 * vec3i.getY(), vec3.z() + d2 * 0.5 * vec3i.getZ());
        Vec3 vec32 = vec31.subtract(travelVector);
        int i = Mth.floor(vec32.x);
        int j = Mth.floor(vec32.y);
        int k = Mth.floor(vec32.z);
        int l = Mth.sign(travelVector.x);
        int i1 = Mth.sign(travelVector.y);
        int j1 = Mth.sign(travelVector.z);
        double d3 = l == 0 ? Double.MAX_VALUE : l / travelVector.x;
        double d4 = i1 == 0 ? Double.MAX_VALUE : i1 / travelVector.y;
        double d5 = j1 == 0 ? Double.MAX_VALUE : j1 / travelVector.z;
        double d6 = d3 * (l > 0 ? 1.0 - Mth.frac(vec32.x) : Mth.frac(vec32.x));
        double d7 = d4 * (i1 > 0 ? 1.0 - Mth.frac(vec32.y) : Mth.frac(vec32.y));
        double d8 = d5 * (j1 > 0 ? 1.0 - Mth.frac(vec32.z) : Mth.frac(vec32.z));
        int k1 = 0;

        while (d6 <= 1.0 || d7 <= 1.0 || d8 <= 1.0) {
            if (d6 < d7) {
                if (d6 < d8) {
                    i += l;
                    d6 += d3;
                } else {
                    k += j1;
                    d8 += d5;
                }
            } else if (d7 < d8) {
                j += i1;
                d7 += d4;
            } else {
                k += j1;
                d8 += d5;
            }

            Optional<Vec3> optional = AABB.clip(i, j, k, i + 1, j + 1, k + 1, vec32, vec31);
            if (!optional.isEmpty()) {
                k1++;
                Vec3 vec33 = optional.get();
                double d9 = Mth.clamp(vec33.x, i + 1.0E-5F, i + 1.0 - 1.0E-5F);
                double d10 = Mth.clamp(vec33.y, j + 1.0E-5F, j + 1.0 - 1.0E-5F);
                double d11 = Mth.clamp(vec33.z, k + 1.0E-5F, k + 1.0 - 1.0E-5F);
                int l1 = Mth.floor(d9 - d0 * vec3i.getX());
                int i2 = Mth.floor(d10 - d1 * vec3i.getY());
                int j2 = Mth.floor(d11 - d2 * vec3i.getZ());
                int k2 = k1;

                for (BlockPos blockpos : BlockPos.betweenCornersInDirection(i, j, k, l1, i2, j2, travelVector)) {
                    if (visited.add(blockpos.asLong()) && !visitor.visit(blockpos, k2)) {
                        return -1;
                    }
                }
            }
        }

        return k1;
    }

    private static Vec3i getFurthestCorner(Vec3 travelVector) {
        double d0 = Math.abs(Vec3.X_AXIS.dot(travelVector));
        double d1 = Math.abs(Vec3.Y_AXIS.dot(travelVector));
        double d2 = Math.abs(Vec3.Z_AXIS.dot(travelVector));
        int i = travelVector.x >= 0.0 ? 1 : -1;
        int j = travelVector.y >= 0.0 ? 1 : -1;
        int k = travelVector.z >= 0.0 ? 1 : -1;
        if (d0 <= d1 && d0 <= d2) {
            return new Vec3i(-i, -k, j);
        } else {
            return d1 <= d2 ? new Vec3i(k, -j, -i) : new Vec3i(-j, i, -k);
        }
    }

    @FunctionalInterface
    public interface BlockStepVisitor {
        boolean visit(BlockPos pos, int index);
    }
}
