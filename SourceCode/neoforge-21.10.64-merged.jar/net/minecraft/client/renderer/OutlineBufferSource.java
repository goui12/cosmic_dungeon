package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OutlineBufferSource implements MultiBufferSource {
    private final MultiBufferSource.BufferSource outlineBufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
    private int outlineColor = -1;

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        if (renderType.isOutline()) {
            VertexConsumer vertexconsumer1 = this.outlineBufferSource.getBuffer(renderType);
            return new OutlineBufferSource.EntityOutlineGenerator(vertexconsumer1, this.outlineColor);
        } else {
            Optional<RenderType> optional = renderType.outline();
            if (optional.isPresent()) {
                VertexConsumer vertexconsumer = this.outlineBufferSource.getBuffer(optional.get());
                return new OutlineBufferSource.EntityOutlineGenerator(vertexconsumer, this.outlineColor);
            } else {
                throw new IllegalStateException("Can't render an outline for this rendertype!");
            }
        }
    }

    public void setColor(int color) {
        this.outlineColor = color;
    }

    public void endOutlineBatch() {
        this.outlineBufferSource.endBatch();
    }

    @OnlyIn(Dist.CLIENT)
    record EntityOutlineGenerator(VertexConsumer delegate, int color) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float p_350357_, float p_350369_, float p_350557_) {
            this.delegate.addVertex(p_350357_, p_350369_, p_350557_).setColor(this.color);
            return this;
        }

        @Override
        public VertexConsumer setColor(int p_350802_, int p_351011_, int p_350273_, int p_351040_) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float p_350507_, float p_350470_) {
            this.delegate.setUv(p_350507_, p_350470_);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int p_350412_, int p_350568_) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int p_350636_, int p_351006_) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float p_350484_, float p_350765_, float p_350737_) {
            return this;
        }
    }
}
