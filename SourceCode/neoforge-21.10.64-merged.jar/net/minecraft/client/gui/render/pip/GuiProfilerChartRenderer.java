package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.render.state.pip.GuiProfilerChartRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ResultField;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class GuiProfilerChartRenderer extends PictureInPictureRenderer<GuiProfilerChartRenderState> {
    public GuiProfilerChartRenderer(MultiBufferSource.BufferSource p_415764_) {
        super(p_415764_);
    }

    @Override
    public Class<GuiProfilerChartRenderState> getRenderStateClass() {
        return GuiProfilerChartRenderState.class;
    }

    protected void renderToTexture(GuiProfilerChartRenderState p_416582_, PoseStack p_415956_) {
        double d0 = 0.0;
        p_415956_.translate(0.0F, -5.0F, 0.0F);
        Matrix4f matrix4f = p_415956_.last().pose();

        for (ResultField resultfield : p_416582_.chartData()) {
            int i = Mth.floor(resultfield.percentage / 4.0) + 1;
            VertexConsumer vertexconsumer = this.bufferSource.getBuffer(RenderType.debugTriangleFan());
            int j = ARGB.opaque(resultfield.getColor());
            int k = ARGB.multiply(j, -8355712);
            vertexconsumer.addVertex(matrix4f, 0.0F, 0.0F, 0.0F).setColor(j);

            for (int l = i; l >= 0; l--) {
                float f = (float)((d0 + resultfield.percentage * l / i) * (float) (Math.PI * 2) / 100.0);
                float f1 = Mth.sin(f) * 105.0F;
                float f2 = Mth.cos(f) * 105.0F * 0.5F;
                vertexconsumer.addVertex(matrix4f, f1, f2, 0.0F).setColor(j);
            }

            vertexconsumer = this.bufferSource.getBuffer(RenderType.debugQuads());

            for (int i1 = i; i1 > 0; i1--) {
                float f6 = (float)((d0 + resultfield.percentage * i1 / i) * (float) (Math.PI * 2) / 100.0);
                float f7 = Mth.sin(f6) * 105.0F;
                float f8 = Mth.cos(f6) * 105.0F * 0.5F;
                float f3 = (float)((d0 + resultfield.percentage * (i1 - 1) / i) * (float) (Math.PI * 2) / 100.0);
                float f4 = Mth.sin(f3) * 105.0F;
                float f5 = Mth.cos(f3) * 105.0F * 0.5F;
                if (!((f8 + f5) / 2.0F < 0.0F)) {
                    vertexconsumer.addVertex(matrix4f, f7, f8, 0.0F).setColor(k);
                    vertexconsumer.addVertex(matrix4f, f7, f8 + 10.0F, 0.0F).setColor(k);
                    vertexconsumer.addVertex(matrix4f, f4, f5 + 10.0F, 0.0F).setColor(k);
                    vertexconsumer.addVertex(matrix4f, f4, f5, 0.0F).setColor(k);
                }
            }

            d0 += resultfield.percentage;
        }
    }

    @Override
    protected float getTranslateY(int p_416710_, int p_416674_) {
        return p_416710_ / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "profiler chart";
    }
}
