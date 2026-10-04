package net.goui.cosmicdungeon.block.custom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Extends ordinary pointing to the selector's overhangs without adding invisible world blocks. */
public final class ClassSelectorTargeting {
    public static final ClassSelectorTargeting INSTANCE = new ClassSelectorTargeting();
    private static final int MAX_RAY_CELLS = 64;
    private final VoxelShape shape = ClassSelectorShape.INSTANCE.shape();
    private final List<BlockPos> offsets = new ArrayList<>();

    private ClassSelectorTargeting() {
        int minX = -(int) Math.floor(Math.nextDown(shape.max(Direction.Axis.X)));
        int maxX = -(int) Math.floor(shape.min(Direction.Axis.X));
        int minY = -(int) Math.floor(Math.nextDown(shape.max(Direction.Axis.Y)));
        int maxY = -(int) Math.floor(shape.min(Direction.Axis.Y));
        int minZ = -(int) Math.floor(Math.nextDown(shape.max(Direction.Axis.Z)));
        int maxZ = -(int) Math.floor(shape.min(Direction.Axis.Z));
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) offsets.add(new BlockPos(x, y, z));
        if (offsets.size() > 27) throw new IllegalStateException("Selector overhang exceeds bounded targeting");
    }

    public HitResult refine(BlockGetter level, Vec3 eye, Vec3 view, double reach, HitResult original) {
        if (original == null || !Double.isFinite(reach) || reach <= 0) return original;
        double distance = original.getType() == HitResult.Type.MISS ? reach
                : Math.min(reach, eye.distanceTo(original.getLocation()));
        Search search = new Search(level, eye, eye.add(view.scale(distance)), original, distance * distance);
        return BlockGetter.traverseBlocks(search.from, search.to, search,
                (current, pos) -> current.visit(pos), current -> current.finish());
    }

    private final class Search {
        private final BlockGetter level;
        private final Vec3 from;
        private final Vec3 to;
        private final BlockPos.MutableBlockPos root = new BlockPos.MutableBlockPos();
        private final Set<Long> tested = new HashSet<>();
        private HitResult closest;
        private double distanceSquared;
        private int cells;

        private Search(BlockGetter level, Vec3 from, Vec3 to, HitResult original, double distanceSquared) {
            this.level = level; this.from = from; this.to = to;
            this.closest = original; this.distanceSquared = distanceSquared;
        }

        private HitResult finish() {
            if (closest instanceof net.minecraft.world.phys.BlockHitResult hit
                    && hit.getType() == HitResult.Type.BLOCK
                    && level.getBlockState(hit.getBlockPos()).getBlock() instanceof D1_Class_Selector_Block) {
                Vec3 location = hit.getLocation();
                double ceiling = hit.getBlockPos().getY() + 1.499;
                if (location.y > ceiling) {
                    // Reach and occlusion use the real surface above. Encode the same root interaction
                    // inside vanilla's +/-1-from-center packet envelope; server permission checks stay native.
                    return new net.minecraft.world.phys.BlockHitResult(
                            new Vec3(location.x, ceiling, location.z), hit.getDirection(),
                            hit.getBlockPos(), hit.isInside());
                }
            }
            return closest;
        }

        private HitResult visit(BlockPos cell) {
            if (++cells > MAX_RAY_CELLS) return finish();
            for (BlockPos offset : offsets) {
                root.set(cell.getX() + offset.getX(), cell.getY() + offset.getY(), cell.getZ() + offset.getZ());
                if (!(level.getBlockState(root).getBlock() instanceof D1_Class_Selector_Block)
                        || !tested.add(root.asLong())) continue;
                var hit = shape.clip(from, to, root.immutable());
                if (hit == null) continue;
                double nextDistance = from.distanceToSqr(hit.getLocation());
                // Preserve the native result at equal depth; never select through a nearer wall/entity.
                if (nextDistance + 1.0e-8 < distanceSquared) {
                    closest = hit;
                    distanceSquared = nextDistance;
                }
            }
            return null;
        }
    }
}
