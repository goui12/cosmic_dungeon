package net.minecraft.client.renderer.entity.state;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record HitboxRenderState(
    double x0, double y0, double z0, double x1, double y1, double z1, float offsetX, float offsetY, float offsetZ, float red, float green, float blue
) {
    public HitboxRenderState(
        double p_412445_,
        double p_412417_,
        double p_412502_,
        double p_412113_,
        double p_412754_,
        double p_412099_,
        float p_412628_,
        float p_412369_,
        float p_412175_
    ) {
        this(p_412445_, p_412417_, p_412502_, p_412113_, p_412754_, p_412099_, 0.0F, 0.0F, 0.0F, p_412628_, p_412369_, p_412175_);
    }
}
