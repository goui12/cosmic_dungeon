package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.ModelBakery;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class FlameFeatureRenderer {
    public void render(SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource, AtlasManager atlasManager) {
        for (SubmitNodeStorage.FlameSubmit submitnodestorage$flamesubmit : nodeCollection.getFlameSubmits()) {
            this.renderFlame(
                submitnodestorage$flamesubmit.pose(),
                bufferSource,
                submitnodestorage$flamesubmit.entityRenderState(),
                submitnodestorage$flamesubmit.rotation(),
                atlasManager
            );
        }
    }

    private void renderFlame(PoseStack.Pose pose, MultiBufferSource bufferSource, EntityRenderState renderState, Quaternionf rotation, AtlasManager atlasManager) {
        TextureAtlasSprite textureatlassprite = atlasManager.get(ModelBakery.FIRE_0);
        TextureAtlasSprite textureatlassprite1 = atlasManager.get(ModelBakery.FIRE_1);
        float f = renderState.boundingBoxWidth * 1.4F;
        pose.scale(f, f, f);
        float f1 = 0.5F;
        float f2 = 0.0F;
        float f3 = renderState.boundingBoxHeight / f;
        float f4 = 0.0F;
        pose.rotate(rotation);
        pose.translate(0.0F, 0.0F, 0.3F - (int)f3 * 0.02F);
        float f5 = 0.0F;
        int i = 0;

        for (VertexConsumer vertexconsumer = bufferSource.getBuffer(Sheets.cutoutBlockSheet()); f3 > 0.0F; i++) {
            TextureAtlasSprite textureatlassprite2 = i % 2 == 0 ? textureatlassprite : textureatlassprite1;
            float f6 = textureatlassprite2.getU0();
            float f7 = textureatlassprite2.getV0();
            float f8 = textureatlassprite2.getU1();
            float f9 = textureatlassprite2.getV1();
            if (i / 2 % 2 == 0) {
                float f10 = f8;
                f8 = f6;
                f6 = f10;
            }

            fireVertex(pose, vertexconsumer, -f1 - 0.0F, 0.0F - f4, f5, f8, f9);
            fireVertex(pose, vertexconsumer, f1 - 0.0F, 0.0F - f4, f5, f6, f9);
            fireVertex(pose, vertexconsumer, f1 - 0.0F, 1.4F - f4, f5, f6, f7);
            fireVertex(pose, vertexconsumer, -f1 - 0.0F, 1.4F - f4, f5, f8, f7);
            f3 -= 0.45F;
            f4 -= 0.45F;
            f1 *= 0.9F;
            f5 -= 0.03F;
        }
    }

    private static void fireVertex(
        PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(-1)
            .setUv(u, v)
            .setUv1(0, 10)
            .setLight(240)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
