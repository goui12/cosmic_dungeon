// StoneWardenRenderer.java
package net.goui.cosmicdungeon.client.render;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.model.StoneWardenModel;
import net.goui.cosmicdungeon.client.renderstate.StoneWardenRenderState;
import net.goui.cosmicdungeon.entity.StoneWardenEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class StoneWardenRenderer extends MobRenderer<StoneWardenEntity, StoneWardenRenderState, StoneWardenModel> {
    private static final ResourceLocation TEXTURE =   ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/entity/stone_warden.png");

    public StoneWardenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new StoneWardenModel(ctx.bakeLayer(StoneWardenModel.LAYER_LOCATION)), 3.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(StoneWardenRenderState state) {
        return TEXTURE;
    }

    @Override
    public StoneWardenRenderState createRenderState() {
        return new StoneWardenRenderState();
    }

    @Override
    public void extractRenderState(StoneWardenEntity entity, StoneWardenRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackAnimation.copyFrom(entity.attackAnimation); // <-- copy into the field above
    }
}
