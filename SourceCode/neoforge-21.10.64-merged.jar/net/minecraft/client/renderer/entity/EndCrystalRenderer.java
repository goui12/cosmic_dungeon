package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EndCrystalModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EndCrystalRenderer extends EntityRenderer<EndCrystal, EndCrystalRenderState> {
    private static final ResourceLocation END_CRYSTAL_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(END_CRYSTAL_LOCATION);
    private final EndCrystalModel model;

    public EndCrystalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.model = new EndCrystalModel(context.bakeLayer(ModelLayers.END_CRYSTAL));
    }

    public void submit(EndCrystalRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.scale(2.0F, 2.0F, 2.0F);
        poseStack.translate(0.0F, -0.5F, 0.0F);
        nodeCollector.submitModel(this.model, renderState, poseStack, RENDER_TYPE, renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor, null);
        poseStack.popPose();
        Vec3 vec3 = renderState.beamOffset;
        if (vec3 != null) {
            float f = getY(renderState.ageInTicks);
            float f1 = (float)vec3.x;
            float f2 = (float)vec3.y;
            float f3 = (float)vec3.z;
            poseStack.translate(vec3);
            EnderDragonRenderer.submitCrystalBeams(-f1, -f2 + f, -f3, renderState.ageInTicks, poseStack, nodeCollector, renderState.lightCoords);
        }

        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
    }

    public static float getY(float ageInTicks) {
        float f = Mth.sin(ageInTicks * 0.2F) / 2.0F + 0.5F;
        f = (f * f + f) * 0.4F;
        return f - 1.4F;
    }

    public EndCrystalRenderState createRenderState() {
        return new EndCrystalRenderState();
    }

    public void extractRenderState(EndCrystal entity, EndCrystalRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.ageInTicks = entity.time + partialTick;
        reusedState.showsBottom = entity.showsBottom();
        BlockPos blockpos = entity.getBeamTarget();
        if (blockpos != null) {
            reusedState.beamOffset = Vec3.atCenterOf(blockpos).subtract(entity.getPosition(partialTick));
        } else {
            reusedState.beamOffset = null;
        }
    }

    public boolean shouldRender(EndCrystal livingEntity, Frustum camera, double camX, double camY, double camZ) {
        return super.shouldRender(livingEntity, camera, camX, camY, camZ) || livingEntity.getBeamTarget() != null;
    }
}
