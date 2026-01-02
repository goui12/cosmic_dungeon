package net.goui.cosmicdungeon.client.render;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.model.MetalmancerGolemModel;
import net.goui.cosmicdungeon.client.renderstate.MetalmancerGolemRenderState;
import net.goui.cosmicdungeon.entity.MetalmancerGolemEntity; // <-- change if your package differs

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MetalmancerGolemRenderer
        extends MobRenderer<MetalmancerGolemEntity, MetalmancerGolemRenderState, MetalmancerGolemModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    CosmicDungeonMod.MOD_ID,
                    "textures/entity/metalmancer_golem.png"
            );

    public MetalmancerGolemRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MetalmancerGolemModel(ctx.bakeLayer(MetalmancerGolemModel.LAYER_LOCATION)), 0.6F);
    }

    @Override
    public MetalmancerGolemRenderState createRenderState() {
        return new MetalmancerGolemRenderState();
    }

    @Override
    public void extractRenderState(MetalmancerGolemEntity entity,
                                   MetalmancerGolemRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.walkAnimation = entity.walkAnimation;
        state.attackAnimation = entity.attackAnimation;
        state.summonAnimation = entity.summonAnimation;
        state.idle1Animation = entity.idle1Animation;
        state.idle2Animation = entity.idle2Animation;
        state.idle3Animation = entity.idle3Animation;
        state.deathAnimation = entity.deathAnimation;
        state.ageInTicks = entity.tickCount + partialTicks;

    }

    @Override
    public ResourceLocation getTextureLocation(MetalmancerGolemRenderState state) {

        return TEXTURE;
    }
}
