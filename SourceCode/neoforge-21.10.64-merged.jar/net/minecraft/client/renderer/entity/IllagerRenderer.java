package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.item.CrossbowItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class IllagerRenderer<T extends AbstractIllager, S extends IllagerRenderState> extends MobRenderer<T, S, IllagerModel<S>> {
    protected IllagerRenderer(EntityRendererProvider.Context context, IllagerModel<S> model, float shadowRadius) {
        super(context, model, shadowRadius);
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    public void extractRenderState(T entity, S reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, reusedState, this.itemModelResolver);
        reusedState.isRiding = entity.isPassenger();
        reusedState.mainArm = entity.getMainArm();
        reusedState.armPose = entity.getArmPose();
        reusedState.maxCrossbowChargeDuration = reusedState.armPose == AbstractIllager.IllagerArmPose.CROSSBOW_CHARGE
            ? CrossbowItem.getChargeDuration(entity.getUseItem(), entity)
            : 0;
        reusedState.ticksUsingItem = entity.getTicksUsingItem();
        reusedState.attackAnim = entity.getAttackAnim(partialTick);
        reusedState.isAggressive = entity.isAggressive();
    }
}
