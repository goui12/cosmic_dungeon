package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.GuardianRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuardianRenderer extends MobRenderer<Guardian, GuardianRenderState, GuardianModel> {
    private static final ResourceLocation GUARDIAN_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/guardian.png");
    private static final ResourceLocation GUARDIAN_BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/guardian_beam.png");
    private static final RenderType BEAM_RENDER_TYPE = RenderType.entityCutoutNoCull(GUARDIAN_BEAM_LOCATION);

    public GuardianRenderer(EntityRendererProvider.Context p_174159_) {
        this(p_174159_, 0.5F, ModelLayers.GUARDIAN);
    }

    protected GuardianRenderer(EntityRendererProvider.Context context, float shadowRadius, ModelLayerLocation layer) {
        super(context, new GuardianModel(context.bakeLayer(layer)), shadowRadius);
    }

    public boolean shouldRender(Guardian livingEntity, Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntity, camera, camX, camY, camZ)) {
            return true;
        } else {
            if (livingEntity.hasActiveAttackTarget()) {
                LivingEntity livingentity = livingEntity.getActiveAttackTarget();
                if (livingentity != null) {
                    Vec3 vec3 = this.getPosition(livingentity, livingentity.getBbHeight() * 0.5, 1.0F);
                    Vec3 vec31 = this.getPosition(livingEntity, livingEntity.getEyeHeight(), 1.0F);
                    return camera.isVisible(new AABB(vec31.x, vec31.y, vec31.z, vec3.x, vec3.y, vec3.z));
                }
            }

            return false;
        }
    }

    private Vec3 getPosition(LivingEntity livingEntity, double yOffset, float partialTick) {
        double d0 = Mth.lerp((double)partialTick, livingEntity.xOld, livingEntity.getX());
        double d1 = Mth.lerp((double)partialTick, livingEntity.yOld, livingEntity.getY()) + yOffset;
        double d2 = Mth.lerp((double)partialTick, livingEntity.zOld, livingEntity.getZ());
        return new Vec3(d0, d1, d2);
    }

    public void submit(GuardianRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        Vec3 vec3 = renderState.attackTargetPosition;
        if (vec3 != null) {
            float f = renderState.attackTime * 0.5F % 1.0F;
            poseStack.pushPose();
            poseStack.translate(0.0F, renderState.eyeHeight, 0.0F);
            renderBeam(poseStack, nodeCollector, vec3.subtract(renderState.eyePosition), renderState.attackTime, renderState.attackScale, f);
            poseStack.popPose();
        }
    }

    private static void renderBeam(PoseStack poseStack, SubmitNodeCollector nodeCollector, Vec3 beamVector, float attackTime, float scale, float animationTime) {
        float f = (float)(beamVector.length() + 1.0);
        beamVector = beamVector.normalize();
        float f1 = (float)Math.acos(beamVector.y);
        float f2 = (float) (Math.PI / 2) - (float)Math.atan2(beamVector.z, beamVector.x);
        poseStack.mulPose(Axis.YP.rotationDegrees(f2 * (180.0F / (float)Math.PI)));
        poseStack.mulPose(Axis.XP.rotationDegrees(f1 * (180.0F / (float)Math.PI)));
        float f3 = attackTime * 0.05F * -1.5F;
        float f4 = scale * scale;
        int i = 64 + (int)(f4 * 191.0F);
        int j = 32 + (int)(f4 * 191.0F);
        int k = 128 - (int)(f4 * 64.0F);
        float f5 = 0.2F;
        float f6 = 0.282F;
        float f7 = Mth.cos(f3 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
        float f8 = Mth.sin(f3 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
        float f9 = Mth.cos(f3 + (float) (Math.PI / 4)) * 0.282F;
        float f10 = Mth.sin(f3 + (float) (Math.PI / 4)) * 0.282F;
        float f11 = Mth.cos(f3 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
        float f12 = Mth.sin(f3 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
        float f13 = Mth.cos(f3 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
        float f14 = Mth.sin(f3 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
        float f15 = Mth.cos(f3 + (float) Math.PI) * 0.2F;
        float f16 = Mth.sin(f3 + (float) Math.PI) * 0.2F;
        float f17 = Mth.cos(f3 + 0.0F) * 0.2F;
        float f18 = Mth.sin(f3 + 0.0F) * 0.2F;
        float f19 = Mth.cos(f3 + (float) (Math.PI / 2)) * 0.2F;
        float f20 = Mth.sin(f3 + (float) (Math.PI / 2)) * 0.2F;
        float f21 = Mth.cos(f3 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
        float f22 = Mth.sin(f3 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
        float f23 = 0.0F;
        float f24 = 0.4999F;
        float f25 = -1.0F + animationTime;
        float f26 = f25 + f * 2.5F;
        nodeCollector.submitCustomGeometry(poseStack, BEAM_RENDER_TYPE, (p_432958_, p_433082_) -> {
            vertex(p_433082_, p_432958_, f15, f, f16, i, j, k, 0.4999F, f26);
            vertex(p_433082_, p_432958_, f15, 0.0F, f16, i, j, k, 0.4999F, f25);
            vertex(p_433082_, p_432958_, f17, 0.0F, f18, i, j, k, 0.0F, f25);
            vertex(p_433082_, p_432958_, f17, f, f18, i, j, k, 0.0F, f26);
            vertex(p_433082_, p_432958_, f19, f, f20, i, j, k, 0.4999F, f26);
            vertex(p_433082_, p_432958_, f19, 0.0F, f20, i, j, k, 0.4999F, f25);
            vertex(p_433082_, p_432958_, f21, 0.0F, f22, i, j, k, 0.0F, f25);
            vertex(p_433082_, p_432958_, f21, f, f22, i, j, k, 0.0F, f26);
            float f27 = Mth.floor(attackTime) % 2 == 0 ? 0.5F : 0.0F;
            vertex(p_433082_, p_432958_, f7, f, f8, i, j, k, 0.5F, f27 + 0.5F);
            vertex(p_433082_, p_432958_, f9, f, f10, i, j, k, 1.0F, f27 + 0.5F);
            vertex(p_433082_, p_432958_, f13, f, f14, i, j, k, 1.0F, f27);
            vertex(p_433082_, p_432958_, f11, f, f12, i, j, k, 0.5F, f27);
        });
    }

    private static void vertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float x,
        float y,
        float z,
        int red,
        int green,
        int blue,
        float u,
        float v
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(red, green, blue, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    public ResourceLocation getTextureLocation(GuardianRenderState renderState) {
        return GUARDIAN_LOCATION;
    }

    public GuardianRenderState createRenderState() {
        return new GuardianRenderState();
    }

    public void extractRenderState(Guardian p_entity, GuardianRenderState reusedState, float partialTick) {
        super.extractRenderState(p_entity, reusedState, partialTick);
        reusedState.spikesAnimation = p_entity.getSpikesAnimation(partialTick);
        reusedState.tailAnimation = p_entity.getTailAnimation(partialTick);
        reusedState.eyePosition = p_entity.getEyePosition(partialTick);
        Entity entity = getEntityToLookAt(p_entity);
        if (entity != null) {
            reusedState.lookDirection = p_entity.getViewVector(partialTick);
            reusedState.lookAtPosition = entity.getEyePosition(partialTick);
        } else {
            reusedState.lookDirection = null;
            reusedState.lookAtPosition = null;
        }

        LivingEntity livingentity = p_entity.getActiveAttackTarget();
        if (livingentity != null) {
            reusedState.attackScale = p_entity.getAttackAnimationScale(partialTick);
            reusedState.attackTime = p_entity.getClientSideAttackTime() + partialTick;
            reusedState.attackTargetPosition = this.getPosition(livingentity, livingentity.getBbHeight() * 0.5, partialTick);
        } else {
            reusedState.attackTargetPosition = null;
        }
    }

    @Nullable
    private static Entity getEntityToLookAt(Guardian guardian) {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        return (Entity)(guardian.hasActiveAttackTarget() ? guardian.getActiveAttackTarget() : entity);
    }
}
