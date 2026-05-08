package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.PiglinRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PiglinModel extends AbstractPiglinModel<PiglinRenderState> {
    public PiglinModel(ModelPart root) {
        super(root);
    }

    public void setupAnim(PiglinRenderState renderState) {
        super.setupAnim(renderState);
        float f = (float) (Math.PI / 6);
        float f1 = renderState.attackTime;
        PiglinArmPose piglinarmpose = renderState.armPose;
        if (piglinarmpose == PiglinArmPose.DANCING) {
            float f2 = renderState.ageInTicks / 60.0F;
            this.rightEar.zRot = (float) (Math.PI / 6) + (float) (Math.PI / 180.0) * Mth.sin(f2 * 30.0F) * 10.0F;
            this.leftEar.zRot = (float) (-Math.PI / 6) - (float) (Math.PI / 180.0) * Mth.cos(f2 * 30.0F) * 10.0F;
            this.head.x = this.head.x + Mth.sin(f2 * 10.0F);
            this.head.y = this.head.y + (Mth.sin(f2 * 40.0F) + 0.4F);
            this.rightArm.zRot = (float) (Math.PI / 180.0) * (70.0F + Mth.cos(f2 * 40.0F) * 10.0F);
            this.leftArm.zRot = this.rightArm.zRot * -1.0F;
            this.rightArm.y = this.rightArm.y + (Mth.sin(f2 * 40.0F) * 0.5F - 0.5F);
            this.leftArm.y = this.leftArm.y + (Mth.sin(f2 * 40.0F) * 0.5F + 0.5F);
            this.body.y = this.body.y + Mth.sin(f2 * 40.0F) * 0.35F;
        } else if (piglinarmpose == PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON && f1 == 0.0F) {
            this.holdWeaponHigh(renderState);
        } else if (piglinarmpose == PiglinArmPose.CROSSBOW_HOLD) {
            AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, renderState.mainArm == HumanoidArm.RIGHT);
        } else if (piglinarmpose == PiglinArmPose.CROSSBOW_CHARGE) {
            AnimationUtils.animateCrossbowCharge(
                this.rightArm, this.leftArm, renderState.maxCrossbowChageDuration, renderState.ticksUsingItem, renderState.mainArm == HumanoidArm.RIGHT
            );
        } else if (piglinarmpose == PiglinArmPose.ADMIRING_ITEM) {
            this.head.xRot = 0.5F;
            this.head.yRot = 0.0F;
            if (renderState.mainArm == HumanoidArm.LEFT) {
                this.rightArm.yRot = -0.5F;
                this.rightArm.xRot = -0.9F;
            } else {
                this.leftArm.yRot = 0.5F;
                this.leftArm.xRot = -0.9F;
            }
        }
    }

    protected void setupAttackAnimation(PiglinRenderState renderState, float ageInTicks) {
        float f = renderState.attackTime;
        if (f > 0.0F && renderState.armPose == PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON) {
            AnimationUtils.swingWeaponDown(this.rightArm, this.leftArm, renderState.mainArm, f, renderState.ageInTicks);
        } else {
            super.setupAttackAnimation(renderState, ageInTicks);
        }
    }

    private void holdWeaponHigh(PiglinRenderState renderState) {
        if (renderState.mainArm == HumanoidArm.LEFT) {
            this.leftArm.xRot = -1.8F;
        } else {
            this.rightArm.xRot = -1.8F;
        }
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        this.leftSleeve.visible = visible;
        this.rightSleeve.visible = visible;
        this.leftPants.visible = visible;
        this.rightPants.visible = visible;
        this.jacket.visible = visible;
    }
}
