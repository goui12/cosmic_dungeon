package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.SnifferModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnifferRenderer extends AgeableMobRenderer<Sniffer, SnifferRenderState, SnifferModel> {
    private static final ResourceLocation SNIFFER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/sniffer/sniffer.png");

    public SnifferRenderer(EntityRendererProvider.Context p_272933_) {
        super(p_272933_, new SnifferModel(p_272933_.bakeLayer(ModelLayers.SNIFFER)), new SnifferModel(p_272933_.bakeLayer(ModelLayers.SNIFFER_BABY)), 1.1F);
    }

    public ResourceLocation getTextureLocation(SnifferRenderState p_360679_) {
        return SNIFFER_LOCATION;
    }

    public SnifferRenderState createRenderState() {
        return new SnifferRenderState();
    }

    public void extractRenderState(Sniffer p_363720_, SnifferRenderState p_363011_, float p_363157_) {
        super.extractRenderState(p_363720_, p_363011_, p_363157_);
        p_363011_.isSearching = p_363720_.isSearching();
        p_363011_.diggingAnimationState.copyFrom(p_363720_.diggingAnimationState);
        p_363011_.sniffingAnimationState.copyFrom(p_363720_.sniffingAnimationState);
        p_363011_.risingAnimationState.copyFrom(p_363720_.risingAnimationState);
        p_363011_.feelingHappyAnimationState.copyFrom(p_363720_.feelingHappyAnimationState);
        p_363011_.scentingAnimationState.copyFrom(p_363720_.scentingAnimationState);
    }

    protected AABB getBoundingBoxForCulling(Sniffer p_364991_) {
        return super.getBoundingBoxForCulling(p_364991_).inflate(0.6F);
    }
}
