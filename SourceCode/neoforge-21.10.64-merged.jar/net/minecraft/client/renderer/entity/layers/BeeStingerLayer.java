package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.BeeStingerModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BeeStingerLayer<M extends PlayerModel> extends StuckInBodyLayer<M, Unit> {
    private static final ResourceLocation BEE_STINGER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/bee/bee_stinger.png");

    public BeeStingerLayer(LivingEntityRenderer<?, AvatarRenderState, M> renderer, EntityRendererProvider.Context context) {
        super(
            renderer,
            new BeeStingerModel(context.bakeLayer(ModelLayers.BEE_STINGER)),
            Unit.INSTANCE,
            BEE_STINGER_LOCATION,
            StuckInBodyLayer.PlacementStyle.ON_SURFACE
        );
    }

    @Override
    protected int numStuck(AvatarRenderState renderState) {
        return renderState.stingerCount;
    }
}
