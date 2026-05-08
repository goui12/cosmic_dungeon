package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class ChunkCullingDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    public static final Direction[] DIRECTIONS = Direction.values();
    private final Minecraft minecraft;

    public ChunkCullingDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum p_frustum
    ) {
        LevelRenderer levelrenderer = this.minecraft.levelRenderer;
        boolean flag = this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_PATHS);
        boolean flag1 = this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_VISIBILITY);
        if (flag || flag1) {
            SectionOcclusionGraph sectionocclusiongraph = levelrenderer.getSectionOcclusionGraph();

            for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : levelrenderer.getVisibleSections()) {
                SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionocclusiongraph.getNode(sectionrenderdispatcher$rendersection);
                if (sectionocclusiongraph$node != null) {
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getRenderOrigin();
                    poseStack.pushPose();
                    poseStack.translate(blockpos.getX() - camX, blockpos.getY() - camY, blockpos.getZ() - camZ);
                    Matrix4f matrix4f = poseStack.last().pose();
                    if (flag) {
                        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.lines());
                        int i = sectionocclusiongraph$node.step == 0 ? 0 : Mth.hsvToRgb(sectionocclusiongraph$node.step / 50.0F, 0.9F, 0.9F);
                        int j = i >> 16 & 0xFF;
                        int k = i >> 8 & 0xFF;
                        int l = i & 0xFF;

                        for (int i1 = 0; i1 < DIRECTIONS.length; i1++) {
                            if (sectionocclusiongraph$node.hasSourceDirection(i1)) {
                                Direction direction = DIRECTIONS[i1];
                                vertexconsumer.addVertex(matrix4f, 8.0F, 8.0F, 8.0F)
                                    .setColor(j, k, l, 255)
                                    .setNormal(direction.getStepX(), direction.getStepY(), direction.getStepZ());
                                vertexconsumer.addVertex(
                                        matrix4f,
                                        (float)(8 - 16 * direction.getStepX()),
                                        (float)(8 - 16 * direction.getStepY()),
                                        (float)(8 - 16 * direction.getStepZ())
                                    )
                                    .setColor(j, k, l, 255)
                                    .setNormal(direction.getStepX(), direction.getStepY(), direction.getStepZ());
                            }
                        }
                    }

                    if (flag1 && sectionrenderdispatcher$rendersection.getSectionMesh().hasRenderableLayers()) {
                        VertexConsumer vertexconsumer3 = bufferSource.getBuffer(RenderType.lines());
                        int j1 = 0;

                        for (Direction direction2 : DIRECTIONS) {
                            for (Direction direction1 : DIRECTIONS) {
                                boolean flag2 = sectionrenderdispatcher$rendersection.getSectionMesh().facesCanSeeEachother(direction2, direction1);
                                if (!flag2) {
                                    j1++;
                                    vertexconsumer3.addVertex(
                                            matrix4f,
                                            (float)(8 + 8 * direction2.getStepX()),
                                            (float)(8 + 8 * direction2.getStepY()),
                                            (float)(8 + 8 * direction2.getStepZ())
                                        )
                                        .setColor(255, 0, 0, 255)
                                        .setNormal(direction2.getStepX(), direction2.getStepY(), direction2.getStepZ());
                                    vertexconsumer3.addVertex(
                                            matrix4f,
                                            (float)(8 + 8 * direction1.getStepX()),
                                            (float)(8 + 8 * direction1.getStepY()),
                                            (float)(8 + 8 * direction1.getStepZ())
                                        )
                                        .setColor(255, 0, 0, 255)
                                        .setNormal(direction1.getStepX(), direction1.getStepY(), direction1.getStepZ());
                                }
                            }
                        }

                        if (j1 > 0) {
                            VertexConsumer vertexconsumer4 = bufferSource.getBuffer(RenderType.debugQuads());
                            float f = 0.5F;
                            float f1 = 0.2F;
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        }
                    }

                    poseStack.popPose();
                }
            }
        }

        Frustum frustum = levelrenderer.getCapturedFrustum();
        if (frustum != null) {
            poseStack.pushPose();
            poseStack.translate((float)(frustum.getCamX() - camX), (float)(frustum.getCamY() - camY), (float)(frustum.getCamZ() - camZ));
            Matrix4f matrix4f1 = poseStack.last().pose();
            Vector4f[] avector4f = frustum.getFrustumPoints();
            VertexConsumer vertexconsumer1 = bufferSource.getBuffer(RenderType.debugQuads());
            this.addFrustumQuad(vertexconsumer1, matrix4f1, avector4f, 0, 1, 2, 3, 0, 1, 1);
            this.addFrustumQuad(vertexconsumer1, matrix4f1, avector4f, 4, 5, 6, 7, 1, 0, 0);
            this.addFrustumQuad(vertexconsumer1, matrix4f1, avector4f, 0, 1, 5, 4, 1, 1, 0);
            this.addFrustumQuad(vertexconsumer1, matrix4f1, avector4f, 2, 3, 7, 6, 0, 0, 1);
            this.addFrustumQuad(vertexconsumer1, matrix4f1, avector4f, 0, 4, 7, 3, 0, 1, 0);
            this.addFrustumQuad(vertexconsumer1, matrix4f1, avector4f, 1, 5, 6, 2, 1, 0, 1);
            VertexConsumer vertexconsumer2 = bufferSource.getBuffer(RenderType.lines());
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[0]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[1]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[1]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[2]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[2]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[3]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[3]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[0]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[4]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[5]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[5]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[6]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[6]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[7]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[7]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[4]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[0]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[4]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[1]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[5]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[2]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[6]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[3]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[7]);
            poseStack.popPose();
        }
    }

    private void addFrustumVertex(VertexConsumer buffer, Matrix4f pose, Vector4f position) {
        buffer.addVertex(pose, position.x(), position.y(), position.z()).setColor(-16777216).setNormal(0.0F, 0.0F, -1.0F);
    }

    private void addFrustumQuad(
        VertexConsumer buffer,
        Matrix4f pose,
        Vector4f[] frustumPoints,
        int point1,
        int point2,
        int point3,
        int point4,
        int red,
        int green,
        int blue
    ) {
        float f = 0.25F;
        buffer.addVertex(pose, frustumPoints[point1].x(), frustumPoints[point1].y(), frustumPoints[point1].z())
            .setColor((float)red, (float)green, (float)blue, 0.25F);
        buffer.addVertex(pose, frustumPoints[point2].x(), frustumPoints[point2].y(), frustumPoints[point2].z())
            .setColor((float)red, (float)green, (float)blue, 0.25F);
        buffer.addVertex(pose, frustumPoints[point3].x(), frustumPoints[point3].y(), frustumPoints[point3].z())
            .setColor((float)red, (float)green, (float)blue, 0.25F);
        buffer.addVertex(pose, frustumPoints[point4].x(), frustumPoints[point4].y(), frustumPoints[point4].z())
            .setColor((float)red, (float)green, (float)blue, 0.25F);
    }
}
