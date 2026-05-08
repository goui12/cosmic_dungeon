package net.minecraft.client.gui.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public interface PlainTextRenderable extends TextRenderable {
    @Override
    default void render(Matrix4f pose, VertexConsumer consumer, int packedLight, boolean noDepth) {
        float f = 0.0F;
        if (this.shadowColor() != 0) {
            this.renderSprite(pose, consumer, packedLight, this.x() + this.shadowOffset(), this.y() + this.shadowOffset(), 0.0F, this.shadowColor());
            if (!noDepth) {
                f += 0.03F;
            }
        }

        this.renderSprite(pose, consumer, packedLight, this.x(), this.y(), f, this.color());
    }

    void renderSprite(Matrix4f pose, VertexConsumer consumer, int packedLight, float x, float y, float z, int color);

    float x();

    float y();

    int color();

    int shadowColor();

    float shadowOffset();
}
