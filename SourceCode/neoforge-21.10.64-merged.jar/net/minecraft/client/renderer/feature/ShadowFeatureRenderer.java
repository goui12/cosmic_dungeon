package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ShadowFeatureRenderer {
    private static final RenderType SHADOW_RENDER_TYPE = RenderType.entityShadow(ResourceLocation.withDefaultNamespace("textures/misc/shadow.png"));

    public void render(SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource) {
        VertexConsumer vertexconsumer = bufferSource.getBuffer(SHADOW_RENDER_TYPE);

        for (SubmitNodeStorage.ShadowSubmit submitnodestorage$shadowsubmit : nodeCollection.getShadowSubmits()) {
            for (EntityRenderState.ShadowPiece entityrenderstate$shadowpiece : submitnodestorage$shadowsubmit.pieces()) {
                AABB aabb = entityrenderstate$shadowpiece.shapeBelow().bounds();
                float f = entityrenderstate$shadowpiece.relativeX() + (float)aabb.minX;
                float f1 = entityrenderstate$shadowpiece.relativeX() + (float)aabb.maxX;
                float f2 = entityrenderstate$shadowpiece.relativeY() + (float)aabb.minY;
                float f3 = entityrenderstate$shadowpiece.relativeZ() + (float)aabb.minZ;
                float f4 = entityrenderstate$shadowpiece.relativeZ() + (float)aabb.maxZ;
                float f5 = submitnodestorage$shadowsubmit.radius();
                float f6 = -f / 2.0F / f5 + 0.5F;
                float f7 = -f1 / 2.0F / f5 + 0.5F;
                float f8 = -f3 / 2.0F / f5 + 0.5F;
                float f9 = -f4 / 2.0F / f5 + 0.5F;
                int i = ARGB.white(entityrenderstate$shadowpiece.alpha());
                shadowVertex(submitnodestorage$shadowsubmit.pose(), vertexconsumer, i, f, f2, f3, f6, f8);
                shadowVertex(submitnodestorage$shadowsubmit.pose(), vertexconsumer, i, f, f2, f4, f6, f9);
                shadowVertex(submitnodestorage$shadowsubmit.pose(), vertexconsumer, i, f1, f2, f4, f7, f9);
                shadowVertex(submitnodestorage$shadowsubmit.pose(), vertexconsumer, i, f1, f2, f3, f7, f8);
            }
        }
    }

    private static void shadowVertex(
        Matrix4f pose, VertexConsumer consumer, int color, float x, float y, float z, float u, float v
    ) {
        Vector3f vector3f = pose.transformPosition(x, y, z, new Vector3f());
        consumer.addVertex(vector3f.x(), vector3f.y(), vector3f.z(), color, u, v, OverlayTexture.NO_OVERLAY, 15728880, 0.0F, 1.0F, 0.0F);
    }
}
