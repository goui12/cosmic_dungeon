package net.minecraft.client.gui.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public interface TextRenderable {
    void render(Matrix4f pose, VertexConsumer consumer, int packedLight, boolean noDepth);

    /**
     * Neo: returns the
     * {@link RenderType}
     * to use for the given
     * {@link Font.DisplayMode}
     * and blur setting
     */
    default RenderType renderType(Font.DisplayMode p_displayMode, boolean blur) {
        return renderType(p_displayMode);
    }

    /**
 * @deprecated Neo: Use {@link #renderType(Font.DisplayMode, boolean)} instead
 */
    @Deprecated
    RenderType renderType(Font.DisplayMode displayMode);

    GpuTextureView textureView();

    RenderPipeline guiPipeline();

    float left();

    float top();

    float right();

    float bottom();
}
