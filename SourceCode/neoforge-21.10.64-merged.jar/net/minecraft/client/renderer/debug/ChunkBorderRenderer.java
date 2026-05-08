package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class ChunkBorderRenderer implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;
    private static final int CELL_BORDER = ARGB.color(255, 0, 155, 155);
    private static final int YELLOW = ARGB.color(255, 255, 255, 0);

    public ChunkBorderRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum
    ) {
        Entity entity = this.minecraft.gameRenderer.getMainCamera().getEntity();
        float f = (float)(this.minecraft.level.getMinY() - camY);
        float f1 = (float)(this.minecraft.level.getMaxY() + 1 - camY);
        ChunkPos chunkpos = entity.chunkPosition();
        float f2 = (float)(chunkpos.getMinBlockX() - camX);
        float f3 = (float)(chunkpos.getMinBlockZ() - camZ);
        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.debugLineStrip(1.0));
        Matrix4f matrix4f = poseStack.last().pose();

        for (int i = -16; i <= 32; i += 16) {
            for (int j = -16; j <= 32; j += 16) {
                vertexconsumer.addVertex(matrix4f, f2 + i, f, f3 + j).setColor(1.0F, 0.0F, 0.0F, 0.0F);
                vertexconsumer.addVertex(matrix4f, f2 + i, f, f3 + j).setColor(1.0F, 0.0F, 0.0F, 0.5F);
                vertexconsumer.addVertex(matrix4f, f2 + i, f1, f3 + j).setColor(1.0F, 0.0F, 0.0F, 0.5F);
                vertexconsumer.addVertex(matrix4f, f2 + i, f1, f3 + j).setColor(1.0F, 0.0F, 0.0F, 0.0F);
            }
        }

        for (int l = 2; l < 16; l += 2) {
            int i2 = l % 4 == 0 ? CELL_BORDER : YELLOW;
            vertexconsumer.addVertex(matrix4f, f2 + l, f, f3).setColor(1.0F, 1.0F, 0.0F, 0.0F);
            vertexconsumer.addVertex(matrix4f, f2 + l, f, f3).setColor(i2);
            vertexconsumer.addVertex(matrix4f, f2 + l, f1, f3).setColor(i2);
            vertexconsumer.addVertex(matrix4f, f2 + l, f1, f3).setColor(1.0F, 1.0F, 0.0F, 0.0F);
            vertexconsumer.addVertex(matrix4f, f2 + l, f, f3 + 16.0F).setColor(1.0F, 1.0F, 0.0F, 0.0F);
            vertexconsumer.addVertex(matrix4f, f2 + l, f, f3 + 16.0F).setColor(i2);
            vertexconsumer.addVertex(matrix4f, f2 + l, f1, f3 + 16.0F).setColor(i2);
            vertexconsumer.addVertex(matrix4f, f2 + l, f1, f3 + 16.0F).setColor(1.0F, 1.0F, 0.0F, 0.0F);
        }

        for (int i1 = 2; i1 < 16; i1 += 2) {
            int j2 = i1 % 4 == 0 ? CELL_BORDER : YELLOW;
            vertexconsumer.addVertex(matrix4f, f2, f, f3 + i1).setColor(1.0F, 1.0F, 0.0F, 0.0F);
            vertexconsumer.addVertex(matrix4f, f2, f, f3 + i1).setColor(j2);
            vertexconsumer.addVertex(matrix4f, f2, f1, f3 + i1).setColor(j2);
            vertexconsumer.addVertex(matrix4f, f2, f1, f3 + i1).setColor(1.0F, 1.0F, 0.0F, 0.0F);
            vertexconsumer.addVertex(matrix4f, f2 + 16.0F, f, f3 + i1).setColor(1.0F, 1.0F, 0.0F, 0.0F);
            vertexconsumer.addVertex(matrix4f, f2 + 16.0F, f, f3 + i1).setColor(j2);
            vertexconsumer.addVertex(matrix4f, f2 + 16.0F, f1, f3 + i1).setColor(j2);
            vertexconsumer.addVertex(matrix4f, f2 + 16.0F, f1, f3 + i1).setColor(1.0F, 1.0F, 0.0F, 0.0F);
        }

        for (int j1 = this.minecraft.level.getMinY(); j1 <= this.minecraft.level.getMaxY() + 1; j1 += 2) {
            float f4 = (float)(j1 - camY);
            int k = j1 % 8 == 0 ? CELL_BORDER : YELLOW;
            vertexconsumer.addVertex(matrix4f, f2, f4, f3).setColor(1.0F, 1.0F, 0.0F, 0.0F);
            vertexconsumer.addVertex(matrix4f, f2, f4, f3).setColor(k);
            vertexconsumer.addVertex(matrix4f, f2, f4, f3 + 16.0F).setColor(k);
            vertexconsumer.addVertex(matrix4f, f2 + 16.0F, f4, f3 + 16.0F).setColor(k);
            vertexconsumer.addVertex(matrix4f, f2 + 16.0F, f4, f3).setColor(k);
            vertexconsumer.addVertex(matrix4f, f2, f4, f3).setColor(k);
            vertexconsumer.addVertex(matrix4f, f2, f4, f3).setColor(1.0F, 1.0F, 0.0F, 0.0F);
        }

        vertexconsumer = bufferSource.getBuffer(RenderType.debugLineStrip(2.0));

        for (int k1 = 0; k1 <= 16; k1 += 16) {
            for (int k2 = 0; k2 <= 16; k2 += 16) {
                vertexconsumer.addVertex(matrix4f, f2 + k1, f, f3 + k2).setColor(0.25F, 0.25F, 1.0F, 0.0F);
                vertexconsumer.addVertex(matrix4f, f2 + k1, f, f3 + k2).setColor(0.25F, 0.25F, 1.0F, 1.0F);
                vertexconsumer.addVertex(matrix4f, f2 + k1, f1, f3 + k2).setColor(0.25F, 0.25F, 1.0F, 1.0F);
                vertexconsumer.addVertex(matrix4f, f2 + k1, f1, f3 + k2).setColor(0.25F, 0.25F, 1.0F, 0.0F);
            }
        }

        for (int l1 = this.minecraft.level.getMinY(); l1 <= this.minecraft.level.getMaxY() + 1; l1 += 16) {
            float f5 = (float)(l1 - camY);
            vertexconsumer.addVertex(matrix4f, f2, f5, f3).setColor(0.25F, 0.25F, 1.0F, 0.0F);
            vertexconsumer.addVertex(matrix4f, f2, f5, f3).setColor(0.25F, 0.25F, 1.0F, 1.0F);
            vertexconsumer.addVertex(matrix4f, f2, f5, f3 + 16.0F).setColor(0.25F, 0.25F, 1.0F, 1.0F);
            vertexconsumer.addVertex(matrix4f, f2 + 16.0F, f5, f3 + 16.0F).setColor(0.25F, 0.25F, 1.0F, 1.0F);
            vertexconsumer.addVertex(matrix4f, f2 + 16.0F, f5, f3).setColor(0.25F, 0.25F, 1.0F, 1.0F);
            vertexconsumer.addVertex(matrix4f, f2, f5, f3).setColor(0.25F, 0.25F, 1.0F, 1.0F);
            vertexconsumer.addVertex(matrix4f, f2, f5, f3).setColor(0.25F, 0.25F, 1.0F, 0.0F);
        }
    }
}
