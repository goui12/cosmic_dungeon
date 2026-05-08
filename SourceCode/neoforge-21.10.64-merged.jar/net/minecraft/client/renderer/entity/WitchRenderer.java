package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.WitchModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.WitchItemLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitchRenderer extends MobRenderer<Witch, WitchRenderState, WitchModel> {
    private static final ResourceLocation WITCH_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/witch.png");

    public WitchRenderer(EntityRendererProvider.Context p_174443_) {
        super(p_174443_, new WitchModel(p_174443_.bakeLayer(ModelLayers.WITCH)), 0.5F);
        this.addLayer(new WitchItemLayer(this));
    }

    public ResourceLocation getTextureLocation(WitchRenderState p_363921_) {
        return WITCH_LOCATION;
    }

    public WitchRenderState createRenderState() {
        return new WitchRenderState();
    }

    public void extractRenderState(Witch p_360566_, WitchRenderState p_364734_, float p_364448_) {
        super.extractRenderState(p_360566_, p_364734_, p_364448_);
        HoldingEntityRenderState.extractHoldingEntityRenderState(p_360566_, p_364734_, this.itemModelResolver);
        p_364734_.entityId = p_360566_.getId();
        ItemStack itemstack = p_360566_.getMainHandItem();
        p_364734_.isHoldingItem = !itemstack.isEmpty();
        p_364734_.isHoldingPotion = itemstack.is(Items.POTION);
    }
}
