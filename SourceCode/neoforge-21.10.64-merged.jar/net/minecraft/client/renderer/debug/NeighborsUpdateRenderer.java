package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NeighborsUpdateRenderer implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum
    ) {
        int i = DebugSubscriptions.NEIGHBOR_UPDATES.expireAfterTicks();
        double d0 = 1.0 / (i * 2);
        Map<BlockPos, NeighborsUpdateRenderer.LastUpdate> map = new HashMap<>();
        debugValueAccess.forEachEvent(DebugSubscriptions.NEIGHBOR_UPDATES, (p_448797_, p_448798_, p_448799_) -> {
            long j = p_448799_ - p_448798_;
            NeighborsUpdateRenderer.LastUpdate neighborsupdaterenderer$lastupdate2 = map.getOrDefault(p_448797_, NeighborsUpdateRenderer.LastUpdate.NONE);
            map.put(p_448797_, neighborsupdaterenderer$lastupdate2.tryCount((int)j));
        });
        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.lines());

        for (Entry<BlockPos, NeighborsUpdateRenderer.LastUpdate> entry : map.entrySet()) {
            BlockPos blockpos = entry.getKey();
            NeighborsUpdateRenderer.LastUpdate neighborsupdaterenderer$lastupdate = entry.getValue();
            AABB aabb = new AABB(BlockPos.ZERO)
                .inflate(0.002)
                .deflate(d0 * neighborsupdaterenderer$lastupdate.age)
                .move(blockpos.getX(), blockpos.getY(), blockpos.getZ())
                .move(-camX, -camY, -camZ);
            ShapeRenderer.renderLineBox(
                poseStack.last(), vertexconsumer, aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, 1.0F, 1.0F, 1.0F, 1.0F
            );
        }

        for (Entry<BlockPos, NeighborsUpdateRenderer.LastUpdate> entry1 : map.entrySet()) {
            BlockPos blockpos1 = entry1.getKey();
            NeighborsUpdateRenderer.LastUpdate neighborsupdaterenderer$lastupdate1 = entry1.getValue();
            DebugRenderer.renderFloatingText(
                poseStack, bufferSource, String.valueOf(neighborsupdaterenderer$lastupdate1.count), blockpos1.getX(), blockpos1.getY(), blockpos1.getZ(), -1
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    record LastUpdate(int count, int age) {
        static final NeighborsUpdateRenderer.LastUpdate NONE = new NeighborsUpdateRenderer.LastUpdate(0, Integer.MAX_VALUE);

        public NeighborsUpdateRenderer.LastUpdate tryCount(int age) {
            if (age == this.age) {
                return new NeighborsUpdateRenderer.LastUpdate(this.count + 1, age);
            } else {
                return age < this.age ? new NeighborsUpdateRenderer.LastUpdate(1, age) : this;
            }
        }
    }
}
