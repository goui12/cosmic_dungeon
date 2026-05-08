package net.minecraft.client.renderer.entity;

import net.minecraft.client.renderer.entity.state.TippableArrowRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.Arrow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TippableArrowRenderer extends ArrowRenderer<Arrow, TippableArrowRenderState> {
    public static final ResourceLocation NORMAL_ARROW_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    public static final ResourceLocation TIPPED_ARROW_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/tipped_arrow.png");

    public TippableArrowRenderer(EntityRendererProvider.Context p_174422_) {
        super(p_174422_);
    }

    protected ResourceLocation getTextureLocation(TippableArrowRenderState p_362029_) {
        return p_362029_.isTipped ? TIPPED_ARROW_LOCATION : NORMAL_ARROW_LOCATION;
    }

    public TippableArrowRenderState createRenderState() {
        return new TippableArrowRenderState();
    }

    public void extractRenderState(Arrow p_362451_, TippableArrowRenderState p_360886_, float p_363722_) {
        super.extractRenderState(p_362451_, p_360886_, p_363722_);
        p_360886_.isTipped = p_362451_.getColor() > 0;
    }
}
