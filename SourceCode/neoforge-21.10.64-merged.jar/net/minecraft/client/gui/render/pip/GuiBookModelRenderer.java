package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.pip.GuiBookModelRenderState;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiBookModelRenderer extends PictureInPictureRenderer<GuiBookModelRenderState> {
    public GuiBookModelRenderer(MultiBufferSource.BufferSource p_415972_) {
        super(p_415972_);
    }

    @Override
    public Class<GuiBookModelRenderState> getRenderStateClass() {
        return GuiBookModelRenderState.class;
    }

    protected void renderToTexture(GuiBookModelRenderState p_415749_, PoseStack p_416266_) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        p_416266_.mulPose(Axis.YP.rotationDegrees(180.0F));
        p_416266_.mulPose(Axis.XP.rotationDegrees(25.0F));
        float f = p_415749_.open();
        p_416266_.translate((1.0F - f) * 0.2F, (1.0F - f) * 0.1F, (1.0F - f) * 0.25F);
        p_416266_.mulPose(Axis.YP.rotationDegrees(-(1.0F - f) * 90.0F - 90.0F));
        p_416266_.mulPose(Axis.XP.rotationDegrees(180.0F));
        float f1 = p_415749_.flip();
        float f2 = Mth.clamp(Mth.frac(f1 + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
        float f3 = Mth.clamp(Mth.frac(f1 + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
        BookModel bookmodel = p_415749_.bookModel();
        bookmodel.setupAnim(new BookModel.State(0.0F, f2, f3, f));
        ResourceLocation resourcelocation = p_415749_.texture();
        VertexConsumer vertexconsumer = this.bufferSource.getBuffer(bookmodel.renderType(resourcelocation));
        bookmodel.renderToBuffer(p_416266_, vertexconsumer, 15728880, OverlayTexture.NO_OVERLAY);
    }

    @Override
    protected float getTranslateY(int p_416680_, int p_415776_) {
        return 17 * p_415776_;
    }

    @Override
    protected String getTextureLabel() {
        return "book model";
    }
}
