package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombifiedPiglinModel extends AbstractPiglinModel<ZombifiedPiglinRenderState> {
    public ZombifiedPiglinModel(ModelPart p_362330_) {
        super(p_362330_);
    }

    public void setupAnim(ZombifiedPiglinRenderState p_364839_) {
        super.setupAnim(p_364839_);
        AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, p_364839_.isAggressive, p_364839_.attackTime, p_364839_.ageInTicks);
    }

    @Override
    public void setAllVisible(boolean p_363572_) {
        super.setAllVisible(p_363572_);
        this.leftSleeve.visible = p_363572_;
        this.rightSleeve.visible = p_363572_;
        this.leftPants.visible = p_363572_;
        this.rightPants.visible = p_363572_;
        this.jacket.visible = p_363572_;
    }
}
