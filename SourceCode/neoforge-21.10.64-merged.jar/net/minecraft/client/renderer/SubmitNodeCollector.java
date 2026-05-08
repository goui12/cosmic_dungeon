package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SubmitNodeCollector extends OrderedSubmitNodeCollector {
    OrderedSubmitNodeCollector order(int index);

    @OnlyIn(Dist.CLIENT)
    public interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer consumer);
    }

    @OnlyIn(Dist.CLIENT)
    public interface ParticleGroupRenderer {
        @Nullable
        QuadParticleRenderState.PreparedBuffers prepare(ParticleFeatureRenderer.ParticleBufferCache cache);

        void render(
            QuadParticleRenderState.PreparedBuffers preparedBuffers,
            ParticleFeatureRenderer.ParticleBufferCache cache,
            RenderPass renderPass,
            TextureManager textureManager,
            boolean translucent
        );
    }
}
