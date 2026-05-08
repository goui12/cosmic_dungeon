package net.minecraft.world.level;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CollisionGetter extends BlockGetter {
    WorldBorder getWorldBorder();

    @Nullable
    BlockGetter getChunkForCollisions(int chunkX, int chunkZ);

    default boolean isUnobstructed(@Nullable Entity entity, VoxelShape shape) {
        return true;
    }

    default boolean isUnobstructed(BlockState state, BlockPos pos, CollisionContext context) {
        VoxelShape voxelshape = state.getCollisionShape(this, pos, context);
        return voxelshape.isEmpty() || this.isUnobstructed(null, voxelshape.move(pos));
    }

    default boolean isUnobstructed(Entity entity) {
        return this.isUnobstructed(entity, Shapes.create(entity.getBoundingBox()));
    }

    default boolean noCollision(AABB collisionBox) {
        return this.noCollision(null, collisionBox);
    }

    default boolean noCollision(Entity entity) {
        return this.noCollision(entity, entity.getBoundingBox());
    }

    default boolean noCollision(@Nullable Entity entity, AABB collisionBox) {
        return this.noCollision(entity, collisionBox, false);
    }

    default boolean noCollision(@Nullable Entity entity, AABB collisionBox, boolean checkLiquid) {
        for (VoxelShape voxelshape : checkLiquid ? this.getBlockAndLiquidCollisions(entity, collisionBox) : this.getBlockCollisions(entity, collisionBox)) {
            if (!voxelshape.isEmpty()) {
                return false;
            }
        }

        if (!this.getEntityCollisions(entity, collisionBox).isEmpty()) {
            return false;
        } else if (entity == null) {
            return true;
        } else {
            VoxelShape voxelshape1 = this.borderCollision(entity, collisionBox);
            return voxelshape1 == null || !Shapes.joinIsNotEmpty(voxelshape1, Shapes.create(collisionBox), BooleanOp.AND);
        }
    }

    default boolean noBlockCollision(@Nullable Entity entity, AABB boundingBox) {
        for (VoxelShape voxelshape : this.getBlockCollisions(entity, boundingBox)) {
            if (!voxelshape.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB collisionBox);

    default Iterable<VoxelShape> getCollisions(@Nullable Entity entity, AABB collisionBox) {
        List<VoxelShape> list = this.getEntityCollisions(entity, collisionBox);
        Iterable<VoxelShape> iterable = this.getBlockCollisions(entity, collisionBox);
        return list.isEmpty() ? iterable : Iterables.concat(list, iterable);
    }

    default Iterable<VoxelShape> getPreMoveCollisions(@Nullable Entity entity, AABB collisionBox, Vec3 pos) {
        List<VoxelShape> list = this.getEntityCollisions(entity, collisionBox);
        Iterable<VoxelShape> iterable = this.getBlockCollisionsFromContext(CollisionContext.withPosition(entity, pos.y), collisionBox);
        return list.isEmpty() ? iterable : Iterables.concat(list, iterable);
    }

    default Iterable<VoxelShape> getBlockCollisions(@Nullable Entity entity, AABB collisionBox) {
        return this.getBlockCollisionsFromContext(entity == null ? CollisionContext.empty() : CollisionContext.of(entity), collisionBox);
    }

    default Iterable<VoxelShape> getBlockAndLiquidCollisions(@Nullable Entity entity, AABB collisionBox) {
        return this.getBlockCollisionsFromContext(
            entity == null ? CollisionContext.emptyWithFluidCollisions() : CollisionContext.of(entity, true), collisionBox
        );
    }

    private Iterable<VoxelShape> getBlockCollisionsFromContext(CollisionContext context, AABB collisionBox) {
        return () -> new BlockCollisions<>(this, context, collisionBox, false, (p_286215_, p_286216_) -> p_286216_);
    }

    @Nullable
    private VoxelShape borderCollision(Entity entity, AABB box) {
        WorldBorder worldborder = this.getWorldBorder();
        return worldborder.isInsideCloseToBorder(entity, box) ? worldborder.getCollisionShape() : null;
    }

    default BlockHitResult clipIncludingBorder(ClipContext clipContext) {
        BlockHitResult blockhitresult = this.clip(clipContext);
        WorldBorder worldborder = this.getWorldBorder();
        if (worldborder.isWithinBounds(clipContext.getFrom()) && !worldborder.isWithinBounds(blockhitresult.getLocation())) {
            Vec3 vec3 = blockhitresult.getLocation().subtract(clipContext.getFrom());
            Direction direction = Direction.getApproximateNearest(vec3.x, vec3.y, vec3.z);
            Vec3 vec31 = worldborder.clampVec3ToBound(blockhitresult.getLocation());
            return new BlockHitResult(vec31, direction, BlockPos.containing(vec31), false, true);
        } else {
            return blockhitresult;
        }
    }

    default boolean collidesWithSuffocatingBlock(@Nullable Entity entity, AABB box) {
        BlockCollisions<VoxelShape> blockcollisions = new BlockCollisions<>(this, entity, box, true, (p_286211_, p_286212_) -> p_286212_);

        while (blockcollisions.hasNext()) {
            if (!blockcollisions.next().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    default Optional<BlockPos> findSupportingBlock(Entity entity, AABB box) {
        BlockPos blockpos = null;
        double d0 = Double.MAX_VALUE;
        BlockCollisions<BlockPos> blockcollisions = new BlockCollisions<>(this, entity, box, false, (p_286213_, p_286214_) -> p_286213_);

        while (blockcollisions.hasNext()) {
            BlockPos blockpos1 = blockcollisions.next();
            double d1 = blockpos1.distToCenterSqr(entity.position());
            if (d1 < d0 || d1 == d0 && (blockpos == null || blockpos.compareTo(blockpos1) < 0)) {
                blockpos = blockpos1.immutable();
                d0 = d1;
            }
        }

        return Optional.ofNullable(blockpos);
    }

    default Optional<Vec3> findFreePosition(
        @Nullable Entity entity, VoxelShape shape, Vec3 pos, double x, double y, double z
    ) {
        if (shape.isEmpty()) {
            return Optional.empty();
        } else {
            AABB aabb = shape.bounds().inflate(x, y, z);
            VoxelShape voxelshape = StreamSupport.stream(this.getBlockCollisions(entity, aabb).spliterator(), false)
                .filter(p_186430_ -> this.getWorldBorder() == null || this.getWorldBorder().isWithinBounds(p_186430_.bounds()))
                .flatMap(p_186426_ -> p_186426_.toAabbs().stream())
                .map(p_186424_ -> p_186424_.inflate(x / 2.0, y / 2.0, z / 2.0))
                .map(Shapes::create)
                .reduce(Shapes.empty(), Shapes::or);
            VoxelShape voxelshape1 = Shapes.join(shape, voxelshape, BooleanOp.ONLY_FIRST);
            return voxelshape1.closestPointTo(pos);
        }
    }
}
