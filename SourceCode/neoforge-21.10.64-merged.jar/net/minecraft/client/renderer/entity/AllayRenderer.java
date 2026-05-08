package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.AllayModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.allay.Allay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AllayRenderer extends MobRenderer<Allay, AllayRenderState, AllayModel> {
    private static final ResourceLocation ALLAY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/allay/allay.png");

    public AllayRenderer(EntityRendererProvider.Context p_234551_) {
        super(p_234551_, new AllayModel(p_234551_.bakeLayer(ModelLayers.ALLAY)), 0.4F);
        this.addLayer(new ItemInHandLayer<>(this));
    }

    public ResourceLocation getTextureLocation(AllayRenderState p_361874_) {
        return ALLAY_TEXTURE;
    }

    public AllayRenderState createRenderState() {
        return new AllayRenderState();
    }

    public void extractRenderState(Allay p_364238_, AllayRenderState p_364959_, float p_364487_) {
        super.extractRenderState(p_364238_, p_364959_, p_364487_);
        ArmedEntityRenderState.extractArmedEntityRenderState(p_364238_, p_364959_, this.itemModelResolver);
        p_364959_.isDancing = p_364238_.isDancing();
        p_364959_.isSpinning = p_364238_.isSpinning();
        p_364959_.spinningProgress = p_364238_.getSpinningProgress(p_364487_);
        p_364959_.holdingAnimationProgress = p_364238_.getHoldingItemAnimationProgress(p_364487_);
    }

    protected int getBlockLightLevel(Allay p_234560_, BlockPos p_234561_) {
        return 15;
    }
}
