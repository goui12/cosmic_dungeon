package net.goui.cosmicdungeon.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.model.Magma_Glob;
import net.goui.cosmicdungeon.client.renderstate.MagmaGlobRenderState;
import net.goui.cosmicdungeon.entity.MagmaGlobEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MagmaGlobRenderer
        extends MobRenderer<MagmaGlobEntity, MagmaGlobRenderState, Magma_Glob> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/entity/magma_glob.png");

    public MagmaGlobRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Magma_Glob(ctx.bakeLayer(Magma_Glob.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public MagmaGlobRenderState createRenderState() {
        return new MagmaGlobRenderState();
    }

    @Override
    public void extractRenderState(MagmaGlobEntity entity, MagmaGlobRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        // Map entity animation states into our render state
        state.walkAnimation   = entity.walkAnimation;
        state.attackAnimation = entity.attackAnimation;

        // Age used as the time parameter for keyframed animation evaluation
        state.ageInTicks = entity.tickCount + partialTick;
    }


    // 1.21.8 signature: rotates using the RENDER STATE (not the entity)
    @Override
    protected void setupRotations(MagmaGlobRenderState state, PoseStack poseStack,
                                  float bodyYaw, float partialTick) {
        super.setupRotations(state, poseStack, bodyYaw, partialTick);

        // Flip back upright (pick ONE axis; XP fixes "hanging from ceiling" most often)
        poseStack.translate(0.0D, 1.50D, 0.0D);   // try 1.0D first; tweak to 1.2–1.5 if needed
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));


    }

    // 1.21.8 signature: render(state, poseStack, buffer, light)
    @Override
    public void render(MagmaGlobRenderState state, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        // After flipping 180°, push the model down so feet touch the ground.

        // Tweak this value to your model's true height (try -0.9D..-1.2D).


        super.render(state, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MagmaGlobRenderState state) {
        return TEX;
    }
}
