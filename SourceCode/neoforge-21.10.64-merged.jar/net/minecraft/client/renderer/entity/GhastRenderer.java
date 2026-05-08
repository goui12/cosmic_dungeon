package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.GhastModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.GhastRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Ghast;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GhastRenderer extends MobRenderer<Ghast, GhastRenderState, GhastModel> {
    private static final ResourceLocation GHAST_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/ghast/ghast.png");
    private static final ResourceLocation GHAST_SHOOTING_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/ghast/ghast_shooting.png");

    public GhastRenderer(EntityRendererProvider.Context p_174129_) {
        super(p_174129_, new GhastModel(p_174129_.bakeLayer(ModelLayers.GHAST)), 1.5F);
    }

    public ResourceLocation getTextureLocation(GhastRenderState p_361669_) {
        return p_361669_.isCharging ? GHAST_SHOOTING_LOCATION : GHAST_LOCATION;
    }

    public GhastRenderState createRenderState() {
        return new GhastRenderState();
    }

    public void extractRenderState(Ghast p_361127_, GhastRenderState p_365502_, float p_363045_) {
        super.extractRenderState(p_361127_, p_365502_, p_363045_);
        p_365502_.isCharging = p_361127_.isCharging();
    }
}
