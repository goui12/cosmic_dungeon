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

    // Model exported facing the wrong way; rotate once here.
    private static final float MODEL_YAW_FIX_DEG = 180.0F;

    public GoblinAmbusherRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GoblinAmbusherModel(ctx.bakeLayer(GoblinAmbusherModel.LAYER_LOCATION)), 0.6F);
    }

    @Override
    public GoblinAmbusherRenderState createRenderState() {
        return new GoblinAmbusherRenderState();
    }

    @Override
    public void extractRenderState(GoblinAmbusherEntity entity,
                                   GoblinAmbusherRenderState state,
                                   float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        // Wire up entity animation channels to the render state
        state.walkAnimation = entity.walkLoop;
        state.attackAnimation = entity.attackAnimation;
        state.ageInTicks = entity.tickCount + partialTicks;

        // When attacking, clamp walk speed so footsteps don't play over the windup.
        if (state.attackAnimation != null && state.attackAnimation.isStarted()) {
            state.walkAnimationSpeed = 0.0F;
        }
    }

    // 1.21.9+ signature: (state, poseStack, bodyRot, unusedScaleParam)
    @Override
    protected void setupRotations(GoblinAmbusherRenderState state,
                                  PoseStack poseStack,
                                  float bodyRot,
                                  float unused) {
        super.setupRotations(state, poseStack, bodyRot, unused);
        poseStack.mulPose(Axis.YP.rotationDegrees(MODEL_YAW_FIX_DEG));
    }

    @Override
    public ResourceLocation getTextureLocation(GoblinAmbusherRenderState state) {
        return TEXTURE;
    }
}
