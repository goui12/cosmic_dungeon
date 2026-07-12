package net.goui.cosmicdungeon.rift;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Set;

public final class SafeTeleportUtil {
    private static final int SAFE_SEARCH_RADIUS = 6;
    private static final int SAFE_SEARCH_UP = 4;
    private static final int SAFE_SEARCH_DOWN = 2;

    private SafeTeleportUtil() {}

    public static BlockPos findSafeTeleportPos(ServerLevel level, BlockPos preferred) {
        if (level == null || preferred == null) return null;
        if (isStandable(level, preferred)) return preferred;

        int baseX = preferred.getX();
        int baseY = preferred.getY();
        int baseZ = preferred.getZ();

        for (int r = 1; r <= SAFE_SEARCH_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    for (int dy = -SAFE_SEARCH_DOWN; dy <= SAFE_SEARCH_UP; dy++) {
                        BlockPos p = new BlockPos(baseX + dx, baseY + dy, baseZ + dz);
                        if (isStandable(level, p)) return p;
                    }
                }
            }
        }
        return null;
    }

    public static boolean teleportSafely(ServerPlayer player, ServerLevel targetLevel, BlockPos preferred, float yRot, float xRot) {
        if (player == null || targetLevel == null || preferred == null) return false;
        targetLevel.getChunk(preferred);
        BlockPos safe = findSafeTeleportPos(targetLevel, preferred);
        if (safe == null) return false;
        return player.teleportTo(targetLevel, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, Set.of(), yRot, xRot, true);
    }

    public static boolean isStandable(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        if (pos.getY() <= level.getMinY() || pos.getY() >= level.getMinY() + level.getLogicalHeight() - 1) return false;
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());
        if (!feet.getCollisionShape(level, pos).isEmpty()) return false;
        if (!head.getCollisionShape(level, pos.above()).isEmpty()) return false;
        FluidState ff = feet.getFluidState();
        FluidState hf = head.getFluidState();
        if (!ff.isEmpty() || !hf.isEmpty()) return false;
        return !floor.getCollisionShape(level, pos.below()).isEmpty();
    }
}
