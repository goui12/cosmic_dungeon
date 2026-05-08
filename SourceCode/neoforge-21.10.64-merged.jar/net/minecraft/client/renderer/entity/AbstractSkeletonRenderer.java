package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSkeletonRenderer<T extends AbstractSkeleton, S extends SkeletonRenderState> extends HumanoidMobRenderer<T, S, SkeletonModel<S>> {
    public AbstractSkeletonRenderer(EntityRendererProvider.Context context, ModelLayerLocation skeletonLayer, ArmorModelSet<ModelLayerLocation> armorModelSet) {
        this(context, armorModelSet, new SkeletonModel<>(context.bakeLayer(skeletonLayer)));
    }

    public AbstractSkeletonRenderer(EntityRendererProvider.Context context, ArmorModelSet<ModelLayerLocation> armorModelSet, SkeletonModel<S> model) {
        super(context, model, 0.5F);
        this.addLayer(
            new HumanoidArmorLayer<>(this, ArmorModelSet.bake(armorModelSet, context.getModelSet(), SkeletonModel::new), context.getEquipmentRenderer())
        );
    }

    public void extractRenderState(T entity, S reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.isAggressive = entity.isAggressive();
        reusedState.isShaking = entity.isShaking();
        reusedState.isHoldingBow = entity.getMainHandItem().is(Items.BOW);
    }

    protected boolean isShaking(S renderState) {
        return renderState.isShaking;
    }

    protected HumanoidModel.ArmPose getArmPose(AbstractSkeleton mob, HumanoidArm arm) {
        return mob.getMainArm() == arm && mob.isAggressive() && mob.getMainHandItem().is(Items.BOW)
            ? HumanoidModel.ArmPose.BOW_AND_ARROW
            : HumanoidModel.ArmPose.EMPTY;
    }
}
