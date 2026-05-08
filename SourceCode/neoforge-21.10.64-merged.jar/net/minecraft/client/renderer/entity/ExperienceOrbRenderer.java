package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExperienceOrbRenderer extends EntityRenderer<ExperienceOrb, ExperienceOrbRenderState> {
    private static final ResourceLocation EXPERIENCE_ORB_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/experience_orb.png");
    private static final RenderType RENDER_TYPE = RenderType.itemEntityTranslucentCull(EXPERIENCE_ORB_LOCATION);

    public ExperienceOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }

    protected int getBlockLightLevel(ExperienceOrb entity, BlockPos pos) {
        return Mth.clamp(super.getBlockLightLevel(entity, pos) + 7, 0, 15);
    }

    public void submit(ExperienceOrbRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        int i = renderState.icon;
        float f = (i % 4 * 16 + 0) / 64.0F;
        float f1 = (i % 4 * 16 + 16) / 64.0F;
        float f2 = (i / 4 * 16 + 0) / 64.0F;
        float f3 = (i / 4 * 16 + 16) / 64.0F;
        float f4 = 1.0F;
        float f5 = 0.5F;
        float f6 = 0.25F;
        float f7 = 255.0F;
        float f8 = renderState.ageInTicks / 2.0F;
        int j = (int)((Mth.sin(f8 + 0.0F) + 1.0F) * 0.5F * 255.0F);
        int k = 255;
        int l = (int)((Mth.sin(f8 + (float) (Math.PI * 4.0 / 3.0)) + 1.0F) * 0.1F * 255.0F);
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.mulPose(cameraRenderState.orientation);
        float f9 = 0.3F;
        poseStack.scale(0.3F, 0.3F, 0.3F);
        nodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (p_433894_, p_433372_) -> {
            vertex(p_433372_, p_433894_, -0.5F, -0.25F, j, 255, l, f, f3, renderState.lightCoords);
            vertex(p_433372_, p_433894_, 0.5F, -0.25F, j, 255, l, f1, f3, renderState.lightCoords);
            vertex(p_433372_, p_433894_, 0.5F, 0.75F, j, 255, l, f1, f2, renderState.lightCoords);
            vertex(p_433372_, p_433894_, -0.5F, 0.75F, j, 255, l, f, f2, renderState.lightCoords);
        });
        poseStack.popPose();
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
    }

    private static void vertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float x,
        float y,
        int red,
        int green,
        int blue,
        float u,
        float v,
        int packedLight
    ) {
        consumer.addVertex(pose, x, y, 0.0F)
            .setColor(red, green, blue, 128)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    public ExperienceOrbRenderState createRenderState() {
        return new ExperienceOrbRenderState();
    }

    public void extractRenderState(ExperienceOrb entity, ExperienceOrbRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.icon = entity.getIcon();
    }
}
