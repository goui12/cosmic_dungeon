package net.goui.cosmicdungeon.block.entity;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** Fixed one-block search; entity overlap is intentional, solid-block overlap is not. */
public final class CosmicSpawnerPlacement {
    private CosmicSpawnerPlacement() {}
    public static List<BlockPos> candidates(BlockPos pos) {
        return List.of(pos.above(), pos.north(), pos.west(), pos.south(), pos.east(),
                pos.north().east(), pos.north().west(), pos.south().west(), pos.south().east());
    }
    static @Nullable BlockPos firstClear(BlockPos origin, Predicate<BlockPos> clear) {
        for (var pos : candidates(origin)) if (clear.test(pos)) return pos;
        return null;
    }
    static boolean place(ServerLevel level, Entity entity, BlockPos origin) {
        return firstClear(origin, pos -> {
            entity.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    entity.getYRot(), entity.getXRot());
            positionPassengers(entity);
            return entity.getSelfAndPassengers().allMatch(part -> clear(level, part));
        }) != null;
    }
    private static void positionPassengers(Entity entity) {
        for (var passenger : entity.getPassengers()) {
            entity.positionRider(passenger);
            positionPassengers(passenger);
        }
    }
    static boolean clear(ServerLevel level, Entity entity) {
        var box = entity.getBoundingBox();
        if (box.minY < level.getMinY() || box.maxY > level.getMaxY() + 1
                || !level.getWorldBorder().isWithinBounds(box)) return false;
        // Never turn placement into an implicit chunk load.
        for (int x = ((int)Math.floor(box.minX)) >> 4; x <= ((int)Math.floor(box.maxX)) >> 4; x++)
            for (int z = ((int)Math.floor(box.minZ)) >> 4; z <= ((int)Math.floor(box.maxZ)) >> 4; z++)
                if (!level.hasChunk(x, z)) return false;
        for (var shape : level.getBlockCollisions(entity, box)) if (!shape.isEmpty()) return false;
        return true;
    }
}
