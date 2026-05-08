package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.debug.DebugStructureInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StructureRenderer implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum
    ) {
        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.lines());
        debugValueAccess.forEachChunk(DebugSubscriptions.STRUCTURES, (p_448805_, p_448806_) -> {
            for (DebugStructureInfo debugstructureinfo : p_448806_) {
                renderBox(poseStack, camX, camY, camZ, vertexconsumer, debugstructureinfo.boundingBox(), 1.0F, 1.0F, 1.0F, 1.0F);

                for (DebugStructureInfo.Piece debugstructureinfo$piece : debugstructureinfo.pieces()) {
                    if (debugstructureinfo$piece.isStart()) {
                        renderBox(poseStack, camX, camY, camZ, vertexconsumer, debugstructureinfo$piece.boundingBox(), 0.0F, 1.0F, 0.0F, 1.0F);
                    } else {
                        renderBox(poseStack, camX, camY, camZ, vertexconsumer, debugstructureinfo$piece.boundingBox(), 0.0F, 0.0F, 1.0F, 1.0F);
                    }
                }
            }
        });
    }

    private static void renderBox(
        PoseStack poseStack,
        double camX,
        double camY,
        double camZ,
        VertexConsumer consumer,
        BoundingBox box,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        ShapeRenderer.renderLineBox(
            poseStack.last(),
            consumer,
            box.minX() - camX,
            box.minY() - camY,
            box.minZ() - camZ,
            box.maxX() + 1 - camX,
            box.maxY() + 1 - camY,
            box.maxZ() + 1 - camZ,
            red,
            green,
            blue,
            alpha,
            red,
            green,
            blue
        );
    }
}
