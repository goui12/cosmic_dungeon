package net.goui.cosmicdungeon.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.model.GoblinAmbusherModel;
import net.goui.cosmicdungeon.client.renderstate.GoblinAmbusherRenderState;
import net.goui.cosmicdungeon.entity.GoblinAmbusherEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class GoblinAmbusherRenderer
        extends MobRenderer<GoblinAmbusherEntity, GoblinAmbusherRenderState, GoblinAmbusherModel> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CosmicDungeonMod.MOD_ID, "textures/entity/goblin_ambusher.png");

    // After Y-flip fix, most models need a 180° yaw so they face forward in-game.
    private static final float MODEL_YAW_FIX_DEG = 180f;

    public GoblinAmbusherRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GoblinAmbusherModel(ctx.bakeLayer(GoblinAmbusherModel.LAYER_LOCATION)), 0.6f);
    }

    @Override
    public GoblinAmbusherRenderState createRenderState() {
        return new GoblinAmbusherRenderState();
    }

    @Override
    protected void scale(GoblinAmbusherRenderState state, PoseStack stack) {
        // No Y flip anymore (upright data). Only correct facing.
        stack.mulPose(Axis.YP.rotationDegrees(MODEL_YAW_FIX_DEG));
    }

    @Override
    public void extractRenderState(GoblinAmbusherEntity entity,
                                   GoblinAmbusherRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackAnimation.copyFrom(entity.attackAnimation);
        if (state.attackAnimation.isStarted()) state.walkAnimationSpeed = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(GoblinAmbusherRenderState state) {
        return TEXTURE;
    }
}
