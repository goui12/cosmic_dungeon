package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.state.HitboxRenderState;
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
import net.minecraft.client.renderer.entity.state.ServerHitboxesRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class HitboxFeatureRenderer {
    public void render(SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource) {
        for (SubmitNodeStorage.HitboxSubmit submitnodestorage$hitboxsubmit : nodeCollection.getHitboxSubmits()) {
            VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.lines());
            PoseStack posestack = new PoseStack();
            posestack.mulPose(submitnodestorage$hitboxsubmit.pose());
            renderHitboxesAndViewVector(
                posestack, submitnodestorage$hitboxsubmit.hitboxesRenderState(), vertexconsumer, submitnodestorage$hitboxsubmit.entityRenderState().eyeHeight
            );
            ServerHitboxesRenderState serverhitboxesrenderstate = submitnodestorage$hitboxsubmit.entityRenderState().serverHitboxesRenderState;
            if (serverhitboxesrenderstate != null) {
                if (serverhitboxesrenderstate.missing()) {
                    HitboxRenderState hitboxrenderstate = submitnodestorage$hitboxsubmit.hitboxesRenderState().hitboxes().getFirst();
                    DebugRenderer.renderFloatingText(
                        posestack,
                        bufferSource,
                        "Missing",
                        submitnodestorage$hitboxsubmit.entityRenderState().x,
                        hitboxrenderstate.y1() + 1.5,
                        submitnodestorage$hitboxsubmit.entityRenderState().z,
                        -65536
                    );
                } else if (serverhitboxesrenderstate.hitboxes() != null) {
                    posestack.translate(
                        serverhitboxesrenderstate.serverEntityX() - submitnodestorage$hitboxsubmit.entityRenderState().x,
                        serverhitboxesrenderstate.serverEntityY() - submitnodestorage$hitboxsubmit.entityRenderState().y,
                        serverhitboxesrenderstate.serverEntityZ() - submitnodestorage$hitboxsubmit.entityRenderState().z
                    );
                    renderHitboxesAndViewVector(posestack, serverhitboxesrenderstate.hitboxes(), vertexconsumer, serverhitboxesrenderstate.eyeHeight());
                    Vec3 vec3 = new Vec3(
                        serverhitboxesrenderstate.deltaMovementX(), serverhitboxesrenderstate.deltaMovementY(), serverhitboxesrenderstate.deltaMovementZ()
                    );
                    ShapeRenderer.renderVector(posestack, vertexconsumer, new Vector3f(), vec3, -256);
                }
            }
        }
    }

    private static void renderHitboxesAndViewVector(PoseStack poseStack, HitboxesRenderState renderState, VertexConsumer consumer, float eyeHeight) {
        for (HitboxRenderState hitboxrenderstate : renderState.hitboxes()) {
            renderHitbox(poseStack, consumer, hitboxrenderstate);
        }

        Vec3 vec3 = new Vec3(renderState.viewX(), renderState.viewY(), renderState.viewZ());
        ShapeRenderer.renderVector(poseStack, consumer, new Vector3f(0.0F, eyeHeight, 0.0F), vec3.scale(2.0), -16776961);
    }

    private static void renderHitbox(PoseStack poseStack, VertexConsumer consumer, HitboxRenderState renderState) {
        poseStack.pushPose();
        poseStack.translate(renderState.offsetX(), renderState.offsetY(), renderState.offsetZ());
        ShapeRenderer.renderLineBox(
            poseStack.last(),
            consumer,
            renderState.x0(),
            renderState.y0(),
            renderState.z0(),
            renderState.x1(),
            renderState.y1(),
            renderState.z1(),
            renderState.red(),
            renderState.green(),
            renderState.blue(),
            1.0F
        );
        poseStack.popPose();
    }
}
