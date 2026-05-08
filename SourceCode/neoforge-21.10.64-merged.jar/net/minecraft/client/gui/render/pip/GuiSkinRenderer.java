package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.pip.GuiSkinRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4fStack;

@OnlyIn(Dist.CLIENT)
public class GuiSkinRenderer extends PictureInPictureRenderer<GuiSkinRenderState> {
    public GuiSkinRenderer(MultiBufferSource.BufferSource p_416347_) {
        super(p_416347_);
    }

    @Override
    public Class<GuiSkinRenderState> getRenderStateClass() {
        return GuiSkinRenderState.class;
    }

    protected void renderToTexture(GuiSkinRenderState p_416045_, PoseStack p_415581_) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.PLAYER_SKIN);
        int i = Minecraft.getInstance().getWindow().getGuiScale();
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        float f = p_416045_.scale() * i;
        matrix4fstack.rotateAround(Axis.XP.rotationDegrees(p_416045_.rotationX()), 0.0F, f * -p_416045_.pivotY(), 0.0F);
        p_415581_.mulPose(Axis.YP.rotationDegrees(-p_416045_.rotationY()));
        p_415581_.translate(0.0F, -1.6010001F, 0.0F);
        RenderType rendertype = p_416045_.playerModel().renderType(p_416045_.texture());
        p_416045_.playerModel().renderToBuffer(p_415581_, this.bufferSource.getBuffer(rendertype), 15728880, OverlayTexture.NO_OVERLAY);
        this.bufferSource.endBatch();
        matrix4fstack.popMatrix();
    }

    @Override
    protected String getTextureLabel() {
        return "player skin";
    }
}
