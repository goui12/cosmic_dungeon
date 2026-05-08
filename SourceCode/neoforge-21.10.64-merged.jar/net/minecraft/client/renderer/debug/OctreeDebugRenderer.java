package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableInt;

@OnlyIn(Dist.CLIENT)
public class OctreeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;

    public OctreeDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum
    ) {
        Octree octree = this.minecraft.levelRenderer.getSectionOcclusionGraph().getOctree();
        MutableInt mutableint = new MutableInt(0);
        octree.visitNodes(
            (p_370341_, p_370342_, p_370343_, p_370344_) -> this.renderNode(
                p_370341_, poseStack, bufferSource, camX, camY, camZ, p_370343_, p_370342_, mutableint, p_370344_
            ),
            frustum,
            32
        );
    }

    private void renderNode(
        Octree.Node node,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        double camX,
        double camY,
        double camZ,
        int recursionDepth,
        boolean isLeafNode,
        MutableInt nodesRendered,
        boolean isNearby
    ) {
        AABB aabb = node.getAABB();
        double d0 = aabb.getXsize();
        long i = Math.round(d0 / 16.0);
        if (i == 1L) {
            nodesRendered.add(1);
            double d1 = aabb.getCenter().x;
            double d2 = aabb.getCenter().y;
            double d3 = aabb.getCenter().z;
            int k = isNearby ? -16711936 : -1;
            DebugRenderer.renderFloatingText(poseStack, bufferSource, String.valueOf(nodesRendered.getValue()), d1, d2, d3, k, 0.3F);
        }

        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.lines());
        long j = i + 5L;
        ShapeRenderer.renderLineBox(
            poseStack.last(),
            vertexconsumer,
            aabb.deflate(0.1 * recursionDepth).move(-camX, -camY, -camZ),
            getColorComponent(j, 0.3F),
            getColorComponent(j, 0.8F),
            getColorComponent(j, 0.5F),
            isLeafNode ? 0.4F : 1.0F
        );
    }

    private static float getColorComponent(long value, float multiplier) {
        float f = 0.1F;
        return Mth.frac(multiplier * (float)value) * 0.9F + 0.1F;
    }
}
