package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.dragon.EnderDragonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.entity.state.HitboxRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class EnderDragonRenderer extends EntityRenderer<EnderDragon, EnderDragonRenderState> {
    public static final ResourceLocation CRYSTAL_BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/end_crystal/end_crystal_beam.png");
    private static final ResourceLocation DRAGON_EXPLODING_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon_exploding.png");
    private static final ResourceLocation DRAGON_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
    private static final ResourceLocation DRAGON_EYES_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(DRAGON_LOCATION);
    private static final RenderType DECAL = RenderType.entityDecal(DRAGON_LOCATION);
    private static final RenderType EYES = RenderType.eyes(DRAGON_EYES_LOCATION);
    private static final RenderType BEAM = RenderType.entitySmoothCutout(CRYSTAL_BEAM_LOCATION);
    private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0) / 2.0);
    private final EnderDragonModel model;

    public EnderDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.model = new EnderDragonModel(context.bakeLayer(ModelLayers.ENDER_DRAGON));
    }

    public void submit(EnderDragonRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        float f = renderState.getHistoricalPos(7).yRot();
        float f1 = (float)(renderState.getHistoricalPos(5).y() - renderState.getHistoricalPos(10).y());
        poseStack.mulPose(Axis.YP.rotationDegrees(-f));
        poseStack.mulPose(Axis.XP.rotationDegrees(f1 * 10.0F));
        poseStack.translate(0.0F, 0.0F, 1.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        int i = OverlayTexture.pack(0.0F, renderState.hasRedOverlay);
        if (renderState.deathTime > 0.0F) {
            int j = ARGB.white(renderState.deathTime / 200.0F);
            nodeCollector.order(0)
                .submitModel(
                    this.model,
                    renderState,
                    poseStack,
                    RenderType.dragonExplosionAlpha(DRAGON_EXPLODING_LOCATION),
                    renderState.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    j,
                    null,
                    renderState.outlineColor,
                    null
                );
            nodeCollector.order(1).submitModel(this.model, renderState, poseStack, DECAL, renderState.lightCoords, i, -1, null, renderState.outlineColor, null);
        } else {
            nodeCollector.order(0).submitModel(this.model, renderState, poseStack, RENDER_TYPE, renderState.lightCoords, i, -1, null, renderState.outlineColor, null);
        }

        nodeCollector.submitModel(this.model, renderState, poseStack, EYES, renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor, null);
        if (renderState.deathTime > 0.0F) {
            float f2 = renderState.deathTime / 200.0F;
            poseStack.pushPose();
            poseStack.translate(0.0F, -1.0F, -2.0F);
            submitRays(poseStack, f2, nodeCollector, RenderType.dragonRays());
            submitRays(poseStack, f2, nodeCollector, RenderType.dragonRaysDepth());
            poseStack.popPose();
        }

        poseStack.popPose();
        if (renderState.beamOffset != null) {
            submitCrystalBeams(
                (float)renderState.beamOffset.x,
                (float)renderState.beamOffset.y,
                (float)renderState.beamOffset.z,
                renderState.ageInTicks,
                poseStack,
                nodeCollector,
                renderState.lightCoords
            );
        }

        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
    }

    private static void submitRays(PoseStack poseStack, float deathProgress, SubmitNodeCollector nodeCollector, RenderType renderType) {
        nodeCollector.submitCustomGeometry(
            poseStack,
            renderType,
            (p_434141_, p_434217_) -> {
                float f = Math.min(deathProgress > 0.8F ? (deathProgress - 0.8F) / 0.2F : 0.0F, 1.0F);
                int i = ARGB.colorFromFloat(1.0F - f, 1.0F, 1.0F, 1.0F);
                int j = 16711935;
                RandomSource randomsource = RandomSource.create(432L);
                Vector3f vector3f = new Vector3f();
                Vector3f vector3f1 = new Vector3f();
                Vector3f vector3f2 = new Vector3f();
                Vector3f vector3f3 = new Vector3f();
                Quaternionf quaternionf = new Quaternionf();
                int k = Mth.floor((deathProgress + deathProgress * deathProgress) / 2.0F * 60.0F);

                for (int l = 0; l < k; l++) {
                    quaternionf.rotationXYZ(
                            randomsource.nextFloat() * (float) (Math.PI * 2),
                            randomsource.nextFloat() * (float) (Math.PI * 2),
                            randomsource.nextFloat() * (float) (Math.PI * 2)
                        )
                        .rotateXYZ(
                            randomsource.nextFloat() * (float) (Math.PI * 2),
                            randomsource.nextFloat() * (float) (Math.PI * 2),
                            randomsource.nextFloat() * (float) (Math.PI * 2) + deathProgress * (float) (Math.PI / 2)
                        );
                    p_434141_.rotate(quaternionf);
                    float f1 = randomsource.nextFloat() * 20.0F + 5.0F + f * 10.0F;
                    float f2 = randomsource.nextFloat() * 2.0F + 1.0F + f * 2.0F;
                    vector3f1.set(-HALF_SQRT_3 * f2, f1, -0.5F * f2);
                    vector3f2.set(HALF_SQRT_3 * f2, f1, -0.5F * f2);
                    vector3f3.set(0.0F, f1, f2);
                    p_434217_.addVertex(p_434141_, vector3f).setColor(i);
                    p_434217_.addVertex(p_434141_, vector3f1).setColor(16711935);
                    p_434217_.addVertex(p_434141_, vector3f2).setColor(16711935);
                    p_434217_.addVertex(p_434141_, vector3f).setColor(i);
                    p_434217_.addVertex(p_434141_, vector3f2).setColor(16711935);
                    p_434217_.addVertex(p_434141_, vector3f3).setColor(16711935);
                    p_434217_.addVertex(p_434141_, vector3f).setColor(i);
                    p_434217_.addVertex(p_434141_, vector3f3).setColor(16711935);
                    p_434217_.addVertex(p_434141_, vector3f1).setColor(16711935);
                }
            }
        );
    }

    public static void submitCrystalBeams(
        float offsetX, float offsetY, float offsetZ, float ageInTicks, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight
    ) {
        float f = Mth.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        float f1 = Mth.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotation((float)(-Math.atan2(offsetZ, offsetX)) - (float) (Math.PI / 2)));
        poseStack.mulPose(Axis.XP.rotation((float)(-Math.atan2(f, offsetY)) - (float) (Math.PI / 2)));
        float f2 = 0.0F - ageInTicks * 0.01F;
        float f3 = f1 / 32.0F - ageInTicks * 0.01F;
        nodeCollector.submitCustomGeometry(
            poseStack,
            BEAM,
            (p_435473_, p_434643_) -> {
                int i = 8;
                float f4 = 0.0F;
                float f5 = 0.75F;
                float f6 = 0.0F;

                for (int j = 1; j <= 8; j++) {
                    float f7 = Mth.sin(j * (float) (Math.PI * 2) / 8.0F) * 0.75F;
                    float f8 = Mth.cos(j * (float) (Math.PI * 2) / 8.0F) * 0.75F;
                    float f9 = j / 8.0F;
                    p_434643_.addVertex(p_435473_, f4 * 0.2F, f5 * 0.2F, 0.0F)
                        .setColor(-16777216)
                        .setUv(f6, f2)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(p_435473_, 0.0F, -1.0F, 0.0F);
                    p_434643_.addVertex(p_435473_, f4, f5, f1)
                        .setColor(-1)
                        .setUv(f6, f3)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(p_435473_, 0.0F, -1.0F, 0.0F);
                    p_434643_.addVertex(p_435473_, f7, f8, f1)
                        .setColor(-1)
                        .setUv(f9, f3)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(p_435473_, 0.0F, -1.0F, 0.0F);
                    p_434643_.addVertex(p_435473_, f7 * 0.2F, f8 * 0.2F, 0.0F)
                        .setColor(-16777216)
                        .setUv(f9, f2)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(p_435473_, 0.0F, -1.0F, 0.0F);
                    f4 = f7;
                    f5 = f8;
                    f6 = f9;
                }
            }
        );
        poseStack.popPose();
    }

    public EnderDragonRenderState createRenderState() {
        return new EnderDragonRenderState();
    }

    public void extractRenderState(EnderDragon entity, EnderDragonRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.flapTime = Mth.lerp(partialTick, entity.oFlapTime, entity.flapTime);
        reusedState.deathTime = entity.dragonDeathTime > 0 ? entity.dragonDeathTime + partialTick : 0.0F;
        reusedState.hasRedOverlay = entity.hurtTime > 0;
        EndCrystal endcrystal = entity.nearestCrystal;
        if (endcrystal != null) {
            Vec3 vec3 = endcrystal.getPosition(partialTick).add(0.0, EndCrystalRenderer.getY(endcrystal.time + partialTick), 0.0);
            reusedState.beamOffset = vec3.subtract(entity.getPosition(partialTick));
        } else {
            reusedState.beamOffset = null;
        }

        DragonPhaseInstance dragonphaseinstance = entity.getPhaseManager().getCurrentPhase();
        reusedState.isLandingOrTakingOff = dragonphaseinstance == EnderDragonPhase.LANDING || dragonphaseinstance == EnderDragonPhase.TAKEOFF;
        reusedState.isSitting = dragonphaseinstance.isSitting();
        BlockPos blockpos = entity.level()
            .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(entity.getFightOrigin()));
        reusedState.distanceToEgg = blockpos.distToCenterSqr(entity.position());
        reusedState.partialTicks = entity.isDeadOrDying() ? 0.0F : partialTick;
        reusedState.flightHistory.copyFrom(entity.flightHistory);
    }

    protected void extractAdditionalHitboxes(EnderDragon entity, Builder<HitboxRenderState> hitboxes, float partialTick) {
        super.extractAdditionalHitboxes(entity, hitboxes, partialTick);
        double d0 = -Mth.lerp((double)partialTick, entity.xOld, entity.getX());
        double d1 = -Mth.lerp((double)partialTick, entity.yOld, entity.getY());
        double d2 = -Mth.lerp((double)partialTick, entity.zOld, entity.getZ());

        for (EnderDragonPart enderdragonpart : entity.getSubEntities()) {
            AABB aabb = enderdragonpart.getBoundingBox();
            HitboxRenderState hitboxrenderstate = new HitboxRenderState(
                aabb.minX - enderdragonpart.getX(),
                aabb.minY - enderdragonpart.getY(),
                aabb.minZ - enderdragonpart.getZ(),
                aabb.maxX - enderdragonpart.getX(),
                aabb.maxY - enderdragonpart.getY(),
                aabb.maxZ - enderdragonpart.getZ(),
                (float)(d0 + Mth.lerp((double)partialTick, enderdragonpart.xOld, enderdragonpart.getX())),
                (float)(d1 + Mth.lerp((double)partialTick, enderdragonpart.yOld, enderdragonpart.getY())),
                (float)(d2 + Mth.lerp((double)partialTick, enderdragonpart.zOld, enderdragonpart.getZ())),
                0.25F,
                1.0F,
                0.0F
            );
            hitboxes.add(hitboxrenderstate);
        }
    }

    protected boolean affectedByCulling(EnderDragon display) {
        return false;
    }
}
