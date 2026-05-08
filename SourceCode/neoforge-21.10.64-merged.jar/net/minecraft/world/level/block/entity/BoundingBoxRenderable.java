package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

public interface BoundingBoxRenderable {
    BoundingBoxRenderable.Mode renderMode();

    BoundingBoxRenderable.RenderableBox getRenderableBox();

    public static enum Mode {
        NONE,
        BOX,
        BOX_AND_INVISIBLE_BLOCKS;
    }

    public record RenderableBox(BlockPos localPos, Vec3i size) {
        public static BoundingBoxRenderable.RenderableBox fromCorners(int x1, int y1, int z1, int x2, int y2, int z2) {
            int i = Math.min(x1, x2);
            int j = Math.min(y1, y2);
            int k = Math.min(z1, z2);
            return new BoundingBoxRenderable.RenderableBox(
                new BlockPos(i, j, k), new Vec3i(Math.max(x1, x2) - i, Math.max(y1, y2) - j, Math.max(z1, z2) - k)
            );
        }
    }
}
