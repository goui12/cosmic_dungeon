package net.goui.cosmicdungeon.client.render;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.model.CthonianGnawlingModel;
import net.goui.cosmicdungeon.client.renderstate.CthonianGnawlingRenderState;
import net.goui.cosmicdungeon.entity.CthonianGnawlingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CthonianGnawlingRenderer
        extends MobRenderer<CthonianGnawlingEntity, CthonianGnawlingRenderState, CthonianGnawlingModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/entity/cthonian_gnawling.png");

    public CthonianGnawlingRenderer(EntityRendererProvider.Context context) {
        super(context, new CthonianGnawlingModel(context.bakeLayer(CthonianGnawlingModel.LAYER_LOCATION)), 0.45F);
    }

    @Override
    public CthonianGnawlingRenderState createRenderState() {
        return new CthonianGnawlingRenderState();
    }

    @Override
    public void extractRenderState(CthonianGnawlingEntity entity, CthonianGnawlingRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.ageInTicks = entity.tickCount + partialTick;
        state.walkAmount = entity.getClientCrawlAmount(partialTick);
        state.isLatched = entity.isLatched();
    }

    @Override
    public ResourceLocation getTextureLocation(CthonianGnawlingRenderState state) {
        return TEXTURE;
    }
}
