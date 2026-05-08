package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public record GlyphRenderState(Matrix3x2f pose, TextRenderable renderable, @Nullable ScreenRectangle scissorArea) implements GuiElementRenderState {
    @Override
    public void buildVertices(VertexConsumer p_415880_) {
        this.renderable.render(new Matrix4f().mul(this.pose), p_415880_, 15728880, true);
    }

    @Override
    public RenderPipeline pipeline() {
        return this.renderable.guiPipeline();
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.singleTextureWithLightmap(this.renderable.textureView());
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        return null;
    }
}
