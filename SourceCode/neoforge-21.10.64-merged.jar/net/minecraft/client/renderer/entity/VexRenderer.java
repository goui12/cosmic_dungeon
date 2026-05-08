package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.VexModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Vex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VexRenderer extends MobRenderer<Vex, VexRenderState, VexModel> {
    private static final ResourceLocation VEX_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/illager/vex.png");
    private static final ResourceLocation VEX_CHARGING_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/illager/vex_charging.png");

    public VexRenderer(EntityRendererProvider.Context p_174435_) {
        super(p_174435_, new VexModel(p_174435_.bakeLayer(ModelLayers.VEX)), 0.3F);
        this.addLayer(new ItemInHandLayer<>(this));
    }

    protected int getBlockLightLevel(Vex entity, BlockPos pos) {
        return 15;
    }

    public ResourceLocation getTextureLocation(VexRenderState renderState) {
        return renderState.isCharging ? VEX_CHARGING_LOCATION : VEX_LOCATION;
    }

    public VexRenderState createRenderState() {
        return new VexRenderState();
    }

    public void extractRenderState(Vex entity, VexRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, reusedState, this.itemModelResolver);
        reusedState.isCharging = entity.isCharging();
    }
}
