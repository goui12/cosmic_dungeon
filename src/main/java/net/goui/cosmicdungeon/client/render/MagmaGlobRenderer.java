package net.goui.cosmicdungeon.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.model.Magma_Glob;
import net.goui.cosmicdungeon.client.renderstate.MagmaGlobRenderState;
import net.goui.cosmicdungeon.entity.MagmaGlobEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MagmaGlobRenderer
        extends MobRenderer<MagmaGlobEntity, MagmaGlobRenderState, Magma_Glob> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/entity/magma_glob.png");

    public MagmaGlobRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Magma_Glob(ctx.bakeLayer(Magma_Glob.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public MagmaGlobRenderState createRenderState() {
        return new MagmaGlobRenderState();
    }

    @Override
    public void extractRenderState(MagmaGlobEntity entity,
                                   MagmaGlobRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.walkAnimation = entity.walkAnimation;
        state.attackAnimation = entity.attackAnimation;
        state.ageInTicks = entity.tickCount + partialTick;
    }

    // 1.21.9 signature: (state, poseStack, bodyRot, scaleParam)
    @Override
    protected void setupRotations(MagmaGlobRenderState state,
                                  PoseStack poseStack,
                                  float bodyRot,
                                  float unused) {
        super.setupRotations(state, poseStack, bodyRot, unused);

        // Flip + height nudge to correct the export orientation
        poseStack.translate(0.0D, 1.20D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
    }

    @Override
    public ResourceLocation getTextureLocation(MagmaGlobRenderState state) {
        return TEXTURE;
    }
}
