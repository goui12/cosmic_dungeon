package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class LeashFeatureRenderer {
    private static final int LEASH_RENDER_STEPS = 24;
    private static final float LEASH_WIDTH = 0.05F;

    public void render(SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource) {
        for (SubmitNodeStorage.LeashSubmit submitnodestorage$leashsubmit : nodeCollection.getLeashSubmits()) {
            renderLeash(submitnodestorage$leashsubmit.pose(), bufferSource, submitnodestorage$leashsubmit.leashState());
        }
    }

    private static void renderLeash(Matrix4f pose, MultiBufferSource bufferSource, EntityRenderState.LeashState leashState) {
        float f = (float)(leashState.end.x - leashState.start.x);
        float f1 = (float)(leashState.end.y - leashState.start.y);
        float f2 = (float)(leashState.end.z - leashState.start.z);
        float f3 = Mth.invSqrt(f * f + f2 * f2) * 0.05F / 2.0F;
        float f4 = f2 * f3;
        float f5 = f * f3;
        pose.translate((float)leashState.offset.x, (float)leashState.offset.y, (float)leashState.offset.z);
        VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.leash());

        for (int i = 0; i <= 24; i++) {
            addVertexPair(vertexconsumer, pose, f, f1, f2, 0.05F, f4, f5, i, false, leashState);
        }

        for (int j = 24; j >= 0; j--) {
            addVertexPair(vertexconsumer, pose, f, f1, f2, 0.0F, f4, f5, j, true, leashState);
        }
    }

    private static void addVertexPair(
        VertexConsumer consumer,
        Matrix4f pose,
        float startX,
        float startY,
        float startZ,
        float yOffset,
        float dx,
        float dz,
        int index,
        boolean reverse,
        EntityRenderState.LeashState leashState
    ) {
        float f = index / 24.0F;
        int i = (int)Mth.lerp(f, (float)leashState.startBlockLight, (float)leashState.endBlockLight);
        int j = (int)Mth.lerp(f, (float)leashState.startSkyLight, (float)leashState.endSkyLight);
        int k = LightTexture.pack(i, j);
        float f1 = index % 2 == (reverse ? 1 : 0) ? 0.7F : 1.0F;
        float f2 = 0.5F * f1;
        float f3 = 0.4F * f1;
        float f4 = 0.3F * f1;
        float f5 = startX * f;
        float f6;
        if (leashState.slack) {
            f6 = startY > 0.0F ? startY * f * f : startY - startY * (1.0F - f) * (1.0F - f);
        } else {
            f6 = startY * f;
        }

        float f7 = startZ * f;
        consumer.addVertex(pose, f5 - dx, f6 + yOffset, f7 + dz).setColor(f2, f3, f4, 1.0F).setLight(k);
        consumer.addVertex(pose, f5 + dx, f6 + 0.05F - yOffset, f7 - dz).setColor(f2, f3, f4, 1.0F).setLight(k);
    }
}
