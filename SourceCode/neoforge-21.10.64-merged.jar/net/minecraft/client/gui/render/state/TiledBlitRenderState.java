package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;

@OnlyIn(Dist.CLIENT)
public record TiledBlitRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    int tileWidth,
    int tileHeight,
    int x0,
    int y0,
    int x1,
    int y1,
    float u0,
    float u1,
    float v0,
    float v1,
    int color,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public TiledBlitRenderState(
        RenderPipeline p_449927_,
        TextureSetup p_449129_,
        Matrix3x2f p_449634_,
        int p_449008_,
        int p_449445_,
        int p_449624_,
        int p_449934_,
        int p_449960_,
        int p_449423_,
        float p_449551_,
        float p_449831_,
        float p_449073_,
        float p_449677_,
        int p_449197_,
        @Nullable ScreenRectangle p_449741_
    ) {
        this(
            p_449927_,
            p_449129_,
            p_449634_,
            p_449008_,
            p_449445_,
            p_449624_,
            p_449934_,
            p_449960_,
            p_449423_,
            p_449551_,
            p_449831_,
            p_449073_,
            p_449677_,
            p_449197_,
            p_449741_,
            getBounds(p_449624_, p_449934_, p_449960_, p_449423_, p_449634_, p_449741_)
        );
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        int i = this.x1() - this.x0();
        int j = this.y1() - this.y0();

        for (int k = 0; k < i; k += this.tileWidth()) {
            int i1 = i - k;
            int l;
            float f;
            if (this.tileWidth() <= i1) {
                l = this.tileWidth();
                f = this.u1();
            } else {
                l = i1;
                f = Mth.lerp((float)i1 / this.tileWidth(), this.u0(), this.u1());
            }

            for (int j1 = 0; j1 < j; j1 += this.tileHeight()) {
                int l1 = j - j1;
                int k1;
                float f1;
                if (this.tileHeight() <= l1) {
                    k1 = this.tileHeight();
                    f1 = this.v1();
                } else {
                    k1 = l1;
                    f1 = Mth.lerp((float)l1 / this.tileHeight(), this.v0(), this.v1());
                }

                int i2 = this.x0() + k;
                int j2 = this.x0() + k + l;
                int k2 = this.y0() + j1;
                int l2 = this.y0() + j1 + k1;
                consumer.addVertexWith2DPose(this.pose(), i2, k2).setUv(this.u0(), this.v0()).setColor(this.color());
                consumer.addVertexWith2DPose(this.pose(), i2, l2).setUv(this.u0(), f1).setColor(this.color());
                consumer.addVertexWith2DPose(this.pose(), j2, l2).setUv(f, f1).setColor(this.color());
                consumer.addVertexWith2DPose(this.pose(), j2, k2).setUv(f, this.v0()).setColor(this.color());
            }
        }
    }

    @Nullable
    private static ScreenRectangle getBounds(
        int x0, int y0, int x1, int y1, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea
    ) {
        ScreenRectangle screenrectangle = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(screenrectangle) : screenrectangle;
    }
}
