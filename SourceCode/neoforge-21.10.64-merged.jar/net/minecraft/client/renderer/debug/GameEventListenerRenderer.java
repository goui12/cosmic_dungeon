package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.debug.DebugGameEventInfo;
import net.minecraft.util.debug.DebugGameEventListenerInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameEventListenerRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final float BOX_HEIGHT = 1.0F;

    private void forEachListener(DebugValueAccess debugValueAccess, GameEventListenerRenderer.ListenerVisitor action) {
        debugValueAccess.forEachBlock(
            DebugSubscriptions.GAME_EVENT_LISTENERS, (p_448791_, p_448792_) -> action.accept(p_448791_.getCenter(), p_448792_.listenerRadius())
        );
        debugValueAccess.forEachEntity(
            DebugSubscriptions.GAME_EVENT_LISTENERS, (p_448794_, p_448795_) -> action.accept(p_448794_.position(), p_448795_.listenerRadius())
        );
    }

    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum
    ) {
        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.lines());
        this.forEachListener(
            debugValueAccess,
            (p_448783_, p_448784_) -> {
                double d0 = p_448784_ * 2.0;
                DebugRenderer.renderVoxelShape(
                    poseStack,
                    vertexconsumer,
                    Shapes.create(AABB.ofSize(p_448783_, d0, d0, d0)),
                    -camX,
                    -camY,
                    -camZ,
                    1.0F,
                    1.0F,
                    0.0F,
                    0.35F,
                    true
                );
            }
        );
        VertexConsumer vertexconsumer1 = bufferSource.getBuffer(RenderType.debugFilledBox());
        this.forEachListener(
            debugValueAccess,
            (p_269724_, p_449917_) -> ShapeRenderer.addChainedFilledBoxVertices(
                poseStack,
                vertexconsumer1,
                p_269724_.x() - 0.25 - camX,
                p_269724_.y() - camY,
                p_269724_.z() - 0.25 - camZ,
                p_269724_.x() + 0.25 - camX,
                p_269724_.y() - camY + 1.0,
                p_269724_.z() + 0.25 - camZ,
                1.0F,
                1.0F,
                0.0F,
                0.35F
            )
        );
        this.forEachListener(
            debugValueAccess,
            (p_274713_, p_449285_) -> {
                DebugRenderer.renderFloatingText(poseStack, bufferSource, "Listener Origin", p_274713_.x(), p_274713_.y() + 1.8F, p_274713_.z(), -1, 0.025F);
                DebugRenderer.renderFloatingText(
                    poseStack, bufferSource, BlockPos.containing(p_274713_).toString(), p_274713_.x(), p_274713_.y() + 1.5, p_274713_.z(), -6959665, 0.025F
                );
            }
        );
        debugValueAccess.forEachEvent(DebugSubscriptions.GAME_EVENTS, (p_448787_, p_448788_, p_448789_) -> {
            Vec3 vec3 = p_448787_.pos();
            double d0 = 0.4;
            AABB aabb = AABB.ofSize(vec3.add(0.0, 0.5, 0.0), 0.4, 0.9, 0.4);
            renderFilledBox(poseStack, bufferSource, aabb, 1.0F, 1.0F, 1.0F, 0.2F);
            DebugRenderer.renderFloatingText(poseStack, bufferSource, p_448787_.event().getRegisteredName(), vec3.x, vec3.y + 0.85F, vec3.z, -7564911, 0.0075F);
        });
    }

    private static void renderFilledBox(
        PoseStack poseStack, MultiBufferSource buffer, AABB boundingBox, float red, float green, float blue, float alpha
    ) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera.isInitialized()) {
            Vec3 vec3 = camera.getPosition().reverse();
            DebugRenderer.renderFilledBox(poseStack, buffer, boundingBox.move(vec3), red, green, blue, alpha);
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    interface ListenerVisitor {
        void accept(Vec3 pos, int radius);
    }
}
