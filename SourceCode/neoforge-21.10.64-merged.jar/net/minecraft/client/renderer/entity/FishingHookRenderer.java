package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FishingHookRenderer extends EntityRenderer<FishingHook, FishingHookRenderState> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/fishing_hook.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE_LOCATION);
    private static final double VIEW_BOBBING_SCALE = 960.0;

    public FishingHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public boolean shouldRender(FishingHook livingEntity, Frustum camera, double camX, double camY, double camZ) {
        return super.shouldRender(livingEntity, camera, camX, camY, camZ) && livingEntity.getPlayerOwner() != null;
    }

    public void submit(FishingHookRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(cameraRenderState.orientation);
        nodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (p_434943_, p_432878_) -> {
            vertex(p_432878_, p_434943_, renderState.lightCoords, 0.0F, 0, 0, 1);
            vertex(p_432878_, p_434943_, renderState.lightCoords, 1.0F, 0, 1, 1);
            vertex(p_432878_, p_434943_, renderState.lightCoords, 1.0F, 1, 1, 0);
            vertex(p_432878_, p_434943_, renderState.lightCoords, 0.0F, 1, 0, 0);
        });
        poseStack.popPose();
        float f = (float)renderState.lineOriginOffset.x;
        float f1 = (float)renderState.lineOriginOffset.y;
        float f2 = (float)renderState.lineOriginOffset.z;
        nodeCollector.submitCustomGeometry(poseStack, RenderType.lines(), (p_436505_, p_436506_) -> {
            int i = 16;

            for (int j = 0; j < 16; j++) {
                float f3 = fraction(j, 16);
                float f4 = fraction(j + 1, 16);
                stringVertex(f, f1, f2, p_436506_, p_436505_, f3, f4);
                stringVertex(f, f1, f2, p_436506_, p_436505_, f4, f3);
            }
        });
        poseStack.popPose();
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
    }

    public static HumanoidArm getHoldingArm(Player player) {
        return player.getMainHandItem().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.FISHING_ROD_CAST) ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    private Vec3 getPlayerHandPos(Player player, float handAngle, float partialTick) {
        int i = getHoldingArm(player) == HumanoidArm.RIGHT ? 1 : -1;
        if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && player == Minecraft.getInstance().player) {
            double d4 = 960.0 / this.entityRenderDispatcher.options.fov().get().intValue();
            Vec3 vec3 = this.entityRenderDispatcher
                .camera
                .getNearPlane()
                .getPointOnPlane(i * 0.525F, -0.1F)
                .scale(d4)
                .yRot(handAngle * 0.5F)
                .xRot(-handAngle * 0.7F);
            return player.getEyePosition(partialTick).add(vec3);
        } else {
            float f = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * (float) (Math.PI / 180.0);
            double d0 = Mth.sin(f);
            double d1 = Mth.cos(f);
            float f1 = player.getScale();
            double d2 = i * 0.35 * f1;
            double d3 = 0.8 * f1;
            float f2 = player.isCrouching() ? -0.1875F : 0.0F;
            return player.getEyePosition(partialTick).add(-d1 * d2 - d0 * d3, f2 - 0.45 * f1, -d0 * d2 + d1 * d3);
        }
    }

    private static float fraction(int numerator, int denominator) {
        return (float)numerator / denominator;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight, float x, int y, int u, int v) {
        consumer.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void stringVertex(
        float x, float y, float z, VertexConsumer consumer, PoseStack.Pose pose, float stringFraction, float nextStringFraction
    ) {
        float f = x * stringFraction;
        float f1 = y * (stringFraction * stringFraction + stringFraction) * 0.5F + 0.25F;
        float f2 = z * stringFraction;
        float f3 = x * nextStringFraction - f;
        float f4 = y * (nextStringFraction * nextStringFraction + nextStringFraction) * 0.5F + 0.25F - f1;
        float f5 = z * nextStringFraction - f2;
        float f6 = Mth.sqrt(f3 * f3 + f4 * f4 + f5 * f5);
        f3 /= f6;
        f4 /= f6;
        f5 /= f6;
        consumer.addVertex(pose, f, f1, f2).setColor(-16777216).setNormal(pose, f3, f4, f5);
    }

    public FishingHookRenderState createRenderState() {
        return new FishingHookRenderState();
    }

    public void extractRenderState(FishingHook entity, FishingHookRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        Player player = entity.getPlayerOwner();
        if (player == null) {
            reusedState.lineOriginOffset = Vec3.ZERO;
        } else {
            float f = player.getAttackAnim(partialTick);
            float f1 = Mth.sin(Mth.sqrt(f) * (float) Math.PI);
            Vec3 vec3 = this.getPlayerHandPos(player, f1, partialTick);
            Vec3 vec31 = entity.getPosition(partialTick).add(0.0, 0.25, 0.0);
            reusedState.lineOriginOffset = vec3.subtract(vec31);
        }
    }

    protected boolean affectedByCulling(FishingHook display) {
        return false;
    }
}
