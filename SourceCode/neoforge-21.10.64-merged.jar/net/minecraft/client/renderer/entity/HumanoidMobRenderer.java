package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class HumanoidMobRenderer<T extends Mob, S extends HumanoidRenderState, M extends HumanoidModel<S>> extends AgeableMobRenderer<T, S, M> {
    public HumanoidMobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
        this(context, model, model, shadowRadius);
    }

    public HumanoidMobRenderer(EntityRendererProvider.Context context, M adultModel, M babyModel, float shadowRadius) {
        this(context, adultModel, babyModel, shadowRadius, CustomHeadLayer.Transforms.DEFAULT);
    }

    public HumanoidMobRenderer(EntityRendererProvider.Context context, M adultModel, M babyModel, float shadowRadius, CustomHeadLayer.Transforms transforms) {
        super(context, adultModel, babyModel, shadowRadius);
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache(), transforms));
        this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer(new ItemInHandLayer<>(this));
    }

    protected HumanoidModel.ArmPose getArmPose(T mob, HumanoidArm arm) {
        return HumanoidModel.ArmPose.EMPTY;
    }

    public void extractRenderState(T entity, S reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        extractHumanoidRenderState(entity, reusedState, partialTick, this.itemModelResolver);
        reusedState.leftArmPose = this.getArmPose(entity, HumanoidArm.LEFT);
        reusedState.rightArmPose = this.getArmPose(entity, HumanoidArm.RIGHT);
    }

    public static void extractHumanoidRenderState(LivingEntity entity, HumanoidRenderState reusedState, float partialTick, ItemModelResolver itemModelResolver) {
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, reusedState, itemModelResolver);
        reusedState.isCrouching = entity.isCrouching();
        reusedState.isFallFlying = entity.isFallFlying();
        reusedState.isVisuallySwimming = entity.isVisuallySwimming();
        reusedState.isPassenger = entity.isPassenger() && (entity.getVehicle() != null && entity.getVehicle().shouldRiderSit());
        reusedState.speedValue = 1.0F;
        if (reusedState.isFallFlying) {
            reusedState.speedValue = (float)entity.getDeltaMovement().lengthSqr();
            reusedState.speedValue /= 0.2F;
            reusedState.speedValue = reusedState.speedValue * (reusedState.speedValue * reusedState.speedValue);
        }

        if (reusedState.speedValue < 1.0F) {
            reusedState.speedValue = 1.0F;
        }

        reusedState.attackTime = entity.getAttackAnim(partialTick);
        reusedState.swimAmount = entity.getSwimAmount(partialTick);
        reusedState.attackArm = getAttackArm(entity);
        reusedState.useItemHand = entity.getUsedItemHand();
        reusedState.maxCrossbowChargeDuration = CrossbowItem.getChargeDuration(entity.getUseItem(), entity);
        reusedState.ticksUsingItem = entity.getTicksUsingItem();
        reusedState.isUsingItem = entity.isUsingItem();
        reusedState.elytraRotX = entity.elytraAnimationState.getRotX(partialTick);
        reusedState.elytraRotY = entity.elytraAnimationState.getRotY(partialTick);
        reusedState.elytraRotZ = entity.elytraAnimationState.getRotZ(partialTick);
        reusedState.headEquipment = getEquipmentIfRenderable(entity, EquipmentSlot.HEAD);
        reusedState.chestEquipment = getEquipmentIfRenderable(entity, EquipmentSlot.CHEST);
        reusedState.legsEquipment = getEquipmentIfRenderable(entity, EquipmentSlot.LEGS);
        reusedState.feetEquipment = getEquipmentIfRenderable(entity, EquipmentSlot.FEET);
    }

    private static ItemStack getEquipmentIfRenderable(LivingEntity entity, EquipmentSlot slot) {
        ItemStack itemstack = entity.getItemBySlot(slot);
        return HumanoidArmorLayer.shouldRender(itemstack, slot) ? itemstack.copy() : ItemStack.EMPTY;
    }

    private static HumanoidArm getAttackArm(LivingEntity entity) {
        HumanoidArm humanoidarm = entity.getMainArm();
        return entity.swingingArm == InteractionHand.MAIN_HAND ? humanoidarm : humanoidarm.getOpposite();
    }
}
