package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AnimationUtils {
    public static void animateCrossbowHold(ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean rightHanded) {
        ModelPart modelpart = rightHanded ? rightArm : leftArm;
        ModelPart modelpart1 = rightHanded ? leftArm : rightArm;
        modelpart.yRot = (rightHanded ? -0.3F : 0.3F) + head.yRot;
        modelpart1.yRot = (rightHanded ? 0.6F : -0.6F) + head.yRot;
        modelpart.xRot = (float) (-Math.PI / 2) + head.xRot + 0.1F;
        modelpart1.xRot = -1.5F + head.xRot;
    }

    public static void animateCrossbowCharge(ModelPart rightArm, ModelPart leftArm, float maxCrossbowChargeDuration, int ticksUsingItem, boolean isRightHand) {
        ModelPart modelpart = isRightHand ? rightArm : leftArm;
        ModelPart modelpart1 = isRightHand ? leftArm : rightArm;
        modelpart.yRot = isRightHand ? -0.8F : 0.8F;
        modelpart.xRot = -0.97079635F;
        modelpart1.xRot = modelpart.xRot;
        float f = Mth.clamp((float)ticksUsingItem, 0.0F, maxCrossbowChargeDuration);
        float f1 = f / maxCrossbowChargeDuration;
        modelpart1.yRot = Mth.lerp(f1, 0.4F, 0.85F) * (isRightHand ? 1 : -1);
        modelpart1.xRot = Mth.lerp(f1, modelpart1.xRot, (float) (-Math.PI / 2));
    }

    public static void swingWeaponDown(ModelPart rightArm, ModelPart leftArm, HumanoidArm mainArm, float attackTime, float ageInTicks) {
        float f = Mth.sin(attackTime * (float) Math.PI);
        float f1 = Mth.sin((1.0F - (1.0F - attackTime) * (1.0F - attackTime)) * (float) Math.PI);
        rightArm.zRot = 0.0F;
        leftArm.zRot = 0.0F;
        rightArm.yRot = (float) (Math.PI / 20);
        leftArm.yRot = (float) (-Math.PI / 20);
        if (mainArm == HumanoidArm.RIGHT) {
            rightArm.xRot = -1.8849558F + Mth.cos(ageInTicks * 0.09F) * 0.15F;
            leftArm.xRot = -0.0F + Mth.cos(ageInTicks * 0.19F) * 0.5F;
            rightArm.xRot += f * 2.2F - f1 * 0.4F;
            leftArm.xRot += f * 1.2F - f1 * 0.4F;
        } else {
            rightArm.xRot = -0.0F + Mth.cos(ageInTicks * 0.19F) * 0.5F;
            leftArm.xRot = -1.8849558F + Mth.cos(ageInTicks * 0.09F) * 0.15F;
            rightArm.xRot += f * 1.2F - f1 * 0.4F;
            leftArm.xRot += f * 2.2F - f1 * 0.4F;
        }

        bobArms(rightArm, leftArm, ageInTicks);
    }

    public static void bobModelPart(ModelPart modelPart, float ageInTicks, float multiplier) {
        modelPart.zRot = modelPart.zRot + multiplier * (Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F);
        modelPart.xRot = modelPart.xRot + multiplier * (Mth.sin(ageInTicks * 0.067F) * 0.05F);
    }

    public static void bobArms(ModelPart rightArm, ModelPart leftArm, float ageInTicks) {
        bobModelPart(rightArm, ageInTicks, 1.0F);
        bobModelPart(leftArm, ageInTicks, -1.0F);
    }

    public static void animateZombieArms(ModelPart leftArm, ModelPart rightArm, boolean isAggressive, float attackTime, float ageInTicks) {
        float f = Mth.sin(attackTime * (float) Math.PI);
        float f1 = Mth.sin((1.0F - (1.0F - attackTime) * (1.0F - attackTime)) * (float) Math.PI);
        rightArm.zRot = 0.0F;
        leftArm.zRot = 0.0F;
        rightArm.yRot = -(0.1F - f * 0.6F);
        leftArm.yRot = 0.1F - f * 0.6F;
        float f2 = (float) -Math.PI / (isAggressive ? 1.5F : 2.25F);
        rightArm.xRot = f2;
        leftArm.xRot = f2;
        rightArm.xRot += f * 1.2F - f1 * 0.4F;
        leftArm.xRot += f * 1.2F - f1 * 0.4F;
        bobArms(rightArm, leftArm, ageInTicks);
    }
}
