package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitherArmorLayer extends EnergySwirlLayer<WitherRenderState, WitherBossModel> {
    private static final ResourceLocation WITHER_ARMOR_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/wither/wither_armor.png");
    private final WitherBossModel model;

    public WitherArmorLayer(RenderLayerParent<WitherRenderState, WitherBossModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new WitherBossModel(modelSet.bakeLayer(ModelLayers.WITHER_ARMOR));
    }

    protected boolean isPowered(WitherRenderState renderState) {
        return renderState.isPowered;
    }

    @Override
    protected float xOffset(float tickCount) {
        return Mth.cos(tickCount * 0.02F) * 3.0F;
    }

    @Override
    protected ResourceLocation getTextureLocation() {
        return WITHER_ARMOR_LOCATION;
    }

    protected WitherBossModel model() {
        return this.model;
    }
}
