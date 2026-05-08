package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillagerRenderer extends AgeableMobRenderer<Villager, VillagerRenderState, VillagerModel> {
    private static final ResourceLocation VILLAGER_BASE_SKIN = ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
    public static final CustomHeadLayer.Transforms CUSTOM_HEAD_TRANSFORMS = new CustomHeadLayer.Transforms(-0.1171875F, -0.07421875F, 1.0F);

    public VillagerRenderer(EntityRendererProvider.Context p_174437_) {
        super(p_174437_, new VillagerModel(p_174437_.bakeLayer(ModelLayers.VILLAGER)), new VillagerModel(p_174437_.bakeLayer(ModelLayers.VILLAGER_BABY)), 0.5F);
        this.addLayer(new CustomHeadLayer<>(this, p_174437_.getModelSet(), p_174437_.getPlayerSkinRenderCache(), CUSTOM_HEAD_TRANSFORMS));
        this.addLayer(
            new VillagerProfessionLayer<>(
                this,
                p_174437_.getResourceManager(),
                "villager",
                new VillagerModel(p_174437_.bakeLayer(ModelLayers.VILLAGER_NO_HAT)),
                new VillagerModel(p_174437_.bakeLayer(ModelLayers.VILLAGER_BABY_NO_HAT))
            )
        );
        this.addLayer(new CrossedArmsItemLayer<>(this));
    }

    public ResourceLocation getTextureLocation(VillagerRenderState p_362235_) {
        return VILLAGER_BASE_SKIN;
    }

    protected float getShadowRadius(VillagerRenderState p_361003_) {
        float f = super.getShadowRadius(p_361003_);
        return p_361003_.isBaby ? f * 0.5F : f;
    }

    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    public void extractRenderState(Villager p_365096_, VillagerRenderState p_363733_, float p_365246_) {
        super.extractRenderState(p_365096_, p_363733_, p_365246_);
        HoldingEntityRenderState.extractHoldingEntityRenderState(p_365096_, p_363733_, this.itemModelResolver);
        p_363733_.isUnhappy = p_365096_.getUnhappyCounter() > 0;
        p_363733_.villagerData = p_365096_.getVillagerData();
    }
}
