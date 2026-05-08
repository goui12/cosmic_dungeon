package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.model.PandaModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.PandaHoldsItemLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PandaRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Panda;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PandaRenderer extends AgeableMobRenderer<Panda, PandaRenderState, PandaModel> {
    private static final Map<Panda.Gene, ResourceLocation> TEXTURES = Maps.newEnumMap(
        Map.of(
            Panda.Gene.NORMAL,
            ResourceLocation.withDefaultNamespace("textures/entity/panda/panda.png"),
            Panda.Gene.LAZY,
            ResourceLocation.withDefaultNamespace("textures/entity/panda/lazy_panda.png"),
            Panda.Gene.WORRIED,
            ResourceLocation.withDefaultNamespace("textures/entity/panda/worried_panda.png"),
            Panda.Gene.PLAYFUL,
            ResourceLocation.withDefaultNamespace("textures/entity/panda/playful_panda.png"),
            Panda.Gene.BROWN,
            ResourceLocation.withDefaultNamespace("textures/entity/panda/brown_panda.png"),
            Panda.Gene.WEAK,
            ResourceLocation.withDefaultNamespace("textures/entity/panda/weak_panda.png"),
            Panda.Gene.AGGRESSIVE,
            ResourceLocation.withDefaultNamespace("textures/entity/panda/aggressive_panda.png")
        )
    );

    public PandaRenderer(EntityRendererProvider.Context p_174334_) {
        super(p_174334_, new PandaModel(p_174334_.bakeLayer(ModelLayers.PANDA)), new PandaModel(p_174334_.bakeLayer(ModelLayers.PANDA_BABY)), 0.9F);
        this.addLayer(new PandaHoldsItemLayer(this));
    }

    public ResourceLocation getTextureLocation(PandaRenderState renderState) {
        return TEXTURES.getOrDefault(renderState.variant, TEXTURES.get(Panda.Gene.NORMAL));
    }

    public PandaRenderState createRenderState() {
        return new PandaRenderState();
    }

    public void extractRenderState(Panda entity, PandaRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        HoldingEntityRenderState.extractHoldingEntityRenderState(entity, reusedState, this.itemModelResolver);
        reusedState.variant = entity.getVariant();
        reusedState.isUnhappy = entity.getUnhappyCounter() > 0;
        reusedState.isSneezing = entity.isSneezing();
        reusedState.sneezeTime = entity.getSneezeCounter();
        reusedState.isEating = entity.isEating();
        reusedState.isScared = entity.isScared();
        reusedState.isSitting = entity.isSitting();
        reusedState.sitAmount = entity.getSitAmount(partialTick);
        reusedState.lieOnBackAmount = entity.getLieOnBackAmount(partialTick);
        reusedState.rollAmount = entity.isBaby() ? 0.0F : entity.getRollAmount(partialTick);
        reusedState.rollTime = entity.rollCounter > 0 ? entity.rollCounter + partialTick : 0.0F;
    }

    protected void setupRotations(PandaRenderState renderState, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(renderState, poseStack, bodyRot, scale);
        if (renderState.rollTime > 0.0F) {
            float f = Mth.frac(renderState.rollTime);
            int i = Mth.floor(renderState.rollTime);
            int j = i + 1;
            float f1 = 7.0F;
            float f2 = renderState.isBaby ? 0.3F : 0.8F;
            if (i < 8.0F) {
                float f4 = 90.0F * i / 7.0F;
                float f5 = 90.0F * j / 7.0F;
                float f3 = this.getAngle(f4, f5, j, f, 8.0F);
                poseStack.translate(0.0F, (f2 + 0.2F) * (f3 / 90.0F), 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-f3));
            } else if (i < 16.0F) {
                float f14 = (i - 8.0F) / 7.0F;
                float f17 = 90.0F + 90.0F * f14;
                float f6 = 90.0F + 90.0F * (j - 8.0F) / 7.0F;
                float f11 = this.getAngle(f17, f6, j, f, 16.0F);
                poseStack.translate(0.0F, f2 + 0.2F + (f2 - 0.2F) * (f11 - 90.0F) / 90.0F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-f11));
            } else if (i < 24.0F) {
                float f15 = (i - 16.0F) / 7.0F;
                float f18 = 180.0F + 90.0F * f15;
                float f20 = 180.0F + 90.0F * (j - 16.0F) / 7.0F;
                float f12 = this.getAngle(f18, f20, j, f, 24.0F);
                poseStack.translate(0.0F, f2 + f2 * (270.0F - f12) / 90.0F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-f12));
            } else if (i < 32) {
                float f16 = (i - 24.0F) / 7.0F;
                float f19 = 270.0F + 90.0F * f16;
                float f21 = 270.0F + 90.0F * (j - 24.0F) / 7.0F;
                float f13 = this.getAngle(f19, f21, j, f, 32.0F);
                poseStack.translate(0.0F, f2 * ((360.0F - f13) / 90.0F), 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-f13));
            }
        }

        float f7 = renderState.sitAmount;
        if (f7 > 0.0F) {
            poseStack.translate(0.0F, 0.8F * f7, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(f7, renderState.xRot, renderState.xRot + 90.0F)));
            poseStack.translate(0.0F, -1.0F * f7, 0.0F);
            if (renderState.isScared) {
                float f8 = (float)(Math.cos(renderState.ageInTicks * 1.25F) * Math.PI * 0.05F);
                poseStack.mulPose(Axis.YP.rotationDegrees(f8));
                if (renderState.isBaby) {
                    poseStack.translate(0.0F, 0.8F, 0.55F);
                }
            }
        }

        float f9 = renderState.lieOnBackAmount;
        if (f9 > 0.0F) {
            float f10 = renderState.isBaby ? 0.5F : 1.3F;
            poseStack.translate(0.0F, f10 * f9, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(f9, renderState.xRot, renderState.xRot + 180.0F)));
        }
    }

    private float getAngle(float currentAngle, float nextAngle, int nextRollCounter, float partialTick, float rollEndCount) {
        return nextRollCounter < rollEndCount ? Mth.lerp(partialTick, currentAngle, nextAngle) : currentAngle;
    }
}
