package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;

@OnlyIn(Dist.CLIENT)
public record ColoredRectangleRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    int x0,
    int y0,
    int x1,
    int y1,
    int col1,
    int col2,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public ColoredRectangleRenderState(
        RenderPipeline p_416046_,
        TextureSetup p_415678_,
        Matrix3x2f p_416570_,
        int p_416448_,
        int p_416038_,
        int p_416700_,
        int p_416540_,
        int p_415887_,
        int p_416111_,
        @Nullable ScreenRectangle p_416231_
    ) {
        this(
            p_416046_,
            p_415678_,
            p_416570_,
            p_416448_,
            p_416038_,
            p_416700_,
            p_416540_,
            p_415887_,
            p_416111_,
            p_416231_,
            getBounds(p_416448_, p_416038_, p_416700_, p_416540_, p_416570_, p_416231_)
        );
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        consumer.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setColor(this.col1());
        consumer.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setColor(this.col2());
        consumer.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setColor(this.col2());
        consumer.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setColor(this.col1());
    }

    @Nullable
    private static ScreenRectangle getBounds(
        int x0, int y0, int x1, int y1, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea
    ) {
        ScreenRectangle screenrectangle = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(screenrectangle) : screenrectangle;
    }
}
