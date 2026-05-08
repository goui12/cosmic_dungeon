package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugBreezeInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class BreezeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int JUMP_TARGET_LINE_COLOR = ARGB.color(255, 255, 100, 255);
    private static final int TARGET_LINE_COLOR = ARGB.color(255, 100, 255, 255);
    private static final int INNER_CIRCLE_COLOR = ARGB.color(255, 0, 255, 0);
    private static final int MIDDLE_CIRCLE_COLOR = ARGB.color(255, 255, 165, 0);
    private static final int OUTER_CIRCLE_COLOR = ARGB.color(255, 255, 0, 0);
    private static final int CIRCLE_VERTICES = 20;
    private static final float SEGMENT_SIZE_RADIANS = (float) (Math.PI / 10);
    private final Minecraft minecraft;

    public BreezeDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum
    ) {
        ClientLevel clientlevel = this.minecraft.level;
        debugValueAccess.forEachEntity(
            DebugSubscriptions.BREEZES,
            (p_448762_, p_448763_) -> {
                p_448763_.attackTarget()
                    .map(clientlevel::getEntity)
                    .map(p_359255_ -> p_359255_.getPosition(this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)))
                    .ifPresent(
                        p_448777_ -> {
                            drawLine(poseStack, bufferSource, camX, camY, camZ, p_448762_.position(), p_448777_, TARGET_LINE_COLOR);
                            Vec3 vec3 = p_448777_.add(0.0, 0.01F, 0.0);
                            drawCircle(
                                poseStack.last().pose(),
                                camX,
                                camY,
                                camZ,
                                bufferSource.getBuffer(RenderType.debugLineStrip(2.0)),
                                vec3,
                                4.0F,
                                INNER_CIRCLE_COLOR
                            );
                            drawCircle(
                                poseStack.last().pose(),
                                camX,
                                camY,
                                camZ,
                                bufferSource.getBuffer(RenderType.debugLineStrip(2.0)),
                                vec3,
                                8.0F,
                                MIDDLE_CIRCLE_COLOR
                            );
                            drawCircle(
                                poseStack.last().pose(),
                                camX,
                                camY,
                                camZ,
                                bufferSource.getBuffer(RenderType.debugLineStrip(2.0)),
                                vec3,
                                24.0F,
                                OUTER_CIRCLE_COLOR
                            );
                        }
                    );
                p_448763_.jumpTarget()
                    .ifPresent(
                        p_448770_ -> {
                            drawLine(poseStack, bufferSource, camX, camY, camZ, p_448762_.position(), p_448770_.getCenter(), JUMP_TARGET_LINE_COLOR);
                            DebugRenderer.renderFilledBox(
                                poseStack,
                                bufferSource,
                                AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(p_448770_)).move(-camX, -camY, -camZ),
                                1.0F,
                                0.0F,
                                0.0F,
                                1.0F
                            );
                        }
                    );
            }
        );
    }

    private static void drawLine(
        PoseStack poseStack, MultiBufferSource buffer, double xOffset, double yOffset, double zOffset, Vec3 fromPos, Vec3 toPos, int color
    ) {
        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.debugLineStrip(2.0));
        vertexconsumer.addVertex(poseStack.last(), (float)(fromPos.x - xOffset), (float)(fromPos.y - yOffset), (float)(fromPos.z - zOffset))
            .setColor(color);
        vertexconsumer.addVertex(poseStack.last(), (float)(toPos.x - xOffset), (float)(toPos.y - yOffset), (float)(toPos.z - zOffset))
            .setColor(color);
    }

    private static void drawCircle(
        Matrix4f pose, double xOffset, double yOffset, double zOffset, VertexConsumer consumer, Vec3 pos, float radius, int color
    ) {
        for (int i = 0; i < 20; i++) {
            drawCircleVertex(i, pose, xOffset, yOffset, zOffset, consumer, pos, radius, color);
        }

        drawCircleVertex(0, pose, xOffset, yOffset, zOffset, consumer, pos, radius, color);
    }

    private static void drawCircleVertex(
        int index,
        Matrix4f pose,
        double xOffset,
        double yOffset,
        double zOffset,
        VertexConsumer consumer,
        Vec3 circleCenter,
        float radius,
        int color
    ) {
        float f = index * (float) (Math.PI / 10);
        Vec3 vec3 = circleCenter.add(radius * Math.cos(f), 0.0, radius * Math.sin(f));
        consumer.addVertex(pose, (float)(vec3.x - xOffset), (float)(vec3.y - yOffset), (float)(vec3.z - zOffset)).setColor(color);
    }
}
