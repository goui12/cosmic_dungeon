package net.minecraft.client.renderer.entity.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AvatarRenderer<AvatarlikeEntity extends Avatar & ClientAvatarEntity>
    extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel> {
    public AvatarRenderer(EntityRendererProvider.Context context, boolean slim) {
        super(context, new PlayerModel(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5F);
        this.addLayer(
            new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(
                    slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR,
                    context.getModelSet(),
                    p_446041_ -> new PlayerModel(p_446041_, slim)
                ),
                context.getEquipmentRenderer()
            )
        );
        this.addLayer(new PlayerItemInHandLayer<>(this));
        this.addLayer(new ArrowLayer<>(this, context));
        this.addLayer(new Deadmau5EarsLayer(this, context.getModelSet()));
        this.addLayer(new CapeLayer(this, context.getModelSet(), context.getEquipmentAssets()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
        this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer(new ParrotOnShoulderLayer(this, context.getModelSet()));
        this.addLayer(new SpinAttackEffectLayer(this, context.getModelSet()));
        this.addLayer(new BeeStingerLayer<>(this, context));
    }

    protected boolean shouldRenderLayers(AvatarRenderState renderState) {
        return !renderState.isSpectator;
    }

    public Vec3 getRenderOffset(AvatarRenderState renderState) {
        Vec3 vec3 = super.getRenderOffset(renderState);
        return renderState.isCrouching ? vec3.add(0.0, renderState.scale * -2.0F / 16.0, 0.0) : vec3;
    }

    private static HumanoidModel.ArmPose getArmPose(Avatar avatar, HumanoidArm arm) {
        ItemStack itemstack = avatar.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack itemstack1 = avatar.getItemInHand(InteractionHand.OFF_HAND);
        HumanoidModel.ArmPose humanoidmodel$armpose = getArmPose(avatar, itemstack, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose humanoidmodel$armpose1 = getArmPose(avatar, itemstack1, InteractionHand.OFF_HAND);
        if (humanoidmodel$armpose.isTwoHanded()) {
            humanoidmodel$armpose1 = itemstack1.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        }

        return avatar.getMainArm() == arm ? humanoidmodel$armpose : humanoidmodel$armpose1;
    }

    private static HumanoidModel.ArmPose getArmPose(Avatar avatar, ItemStack handItem, InteractionHand hand) {
        var extensions = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(handItem);
        var armPose = extensions.getArmPose(avatar, hand, handItem);
        if (armPose != null) {
            return armPose;
        }
        if (handItem.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        } else if (!avatar.swinging && handItem.is(Items.CROSSBOW) && CrossbowItem.isCharged(handItem)) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        } else {
            if (avatar.getUsedItemHand() == hand && avatar.getUseItemRemainingTicks() > 0) {
                ItemUseAnimation itemuseanimation = handItem.getUseAnimation();
                if (itemuseanimation == ItemUseAnimation.BLOCK) {
                    return HumanoidModel.ArmPose.BLOCK;
                }

                if (itemuseanimation == ItemUseAnimation.BOW) {
                    return HumanoidModel.ArmPose.BOW_AND_ARROW;
                }

                if (itemuseanimation == ItemUseAnimation.SPEAR) {
                    return HumanoidModel.ArmPose.THROW_SPEAR;
                }

                if (itemuseanimation == ItemUseAnimation.CROSSBOW) {
                    return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                }

                if (itemuseanimation == ItemUseAnimation.SPYGLASS) {
                    return HumanoidModel.ArmPose.SPYGLASS;
                }

                if (itemuseanimation == ItemUseAnimation.TOOT_HORN) {
                    return HumanoidModel.ArmPose.TOOT_HORN;
                }

                if (itemuseanimation == ItemUseAnimation.BRUSH) {
                    return HumanoidModel.ArmPose.BRUSH;
                }
            }

            return HumanoidModel.ArmPose.ITEM;
        }
    }

    public ResourceLocation getTextureLocation(AvatarRenderState renderState) {
        return renderState.skin.body().texturePath();
    }

    protected void scale(AvatarRenderState renderState, PoseStack poseStack) {
        float f = 0.9375F;
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public void submit(AvatarRenderState p_433493_, PoseStack p_434615_, SubmitNodeCollector p_433768_, CameraRenderState p_450931_) {
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre<>(p_433493_, this, p_433493_.partialTick, p_434615_, p_433768_)).isCanceled()) return;
        super.submit(p_433493_, p_434615_, p_433768_, p_450931_);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderPlayerEvent.Post<>(p_433493_, this, p_433493_.partialTick, p_434615_, p_433768_));
    }

    protected void submitNameTag(AvatarRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        int i = renderState.showExtraEars ? -10 : 0;
        if (renderState.scoreText != null) {
            nodeCollector.submitNameTag(
                poseStack,
                renderState.nameTagAttachment,
                i,
                renderState.scoreText,
                !renderState.isDiscrete,
                renderState.lightCoords,
                renderState.distanceToCameraSq,
                cameraRenderState
            );
            poseStack.translate(0.0F, 9.0F * 1.15F * 0.025F, 0.0F);
        }

        if (renderState.nameTag != null) {
            var event = new net.neoforged.neoforge.client.event.RenderNameTagEvent.DoRender(renderState, renderState.nameTag, this, poseStack, nodeCollector, cameraRenderState, renderState.partialTick);
            if (!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled())
            nodeCollector.submitNameTag(
                poseStack,
                renderState.nameTagAttachment,
                i,
                renderState.nameTag,
                !renderState.isDiscrete,
                renderState.lightCoords,
                renderState.distanceToCameraSq,
                cameraRenderState
            );
        }

        poseStack.popPose();
    }

    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    public void extractRenderState(AvatarlikeEntity entity, AvatarRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, reusedState, partialTick, this.itemModelResolver);
        reusedState.leftArmPose = getArmPose(entity, HumanoidArm.LEFT);
        reusedState.rightArmPose = getArmPose(entity, HumanoidArm.RIGHT);
        reusedState.skin = entity.getSkin();
        reusedState.arrowCount = entity.getArrowCount();
        reusedState.stingerCount = entity.getStingerCount();
        reusedState.isSpectator = entity.isSpectator();
        reusedState.showHat = entity.isModelPartShown(PlayerModelPart.HAT);
        reusedState.showJacket = entity.isModelPartShown(PlayerModelPart.JACKET);
        reusedState.showLeftPants = entity.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
        reusedState.showRightPants = entity.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        reusedState.showLeftSleeve = entity.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        reusedState.showRightSleeve = entity.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        reusedState.showCape = entity.isModelPartShown(PlayerModelPart.CAPE);
        this.extractFlightData(entity, reusedState, partialTick);
        this.extractCapeState(entity, reusedState, partialTick);
        if (reusedState.distanceToCameraSq < 100.0) {
            reusedState.scoreText = entity.belowNameDisplay();
        } else {
            reusedState.scoreText = null;
        }

        reusedState.parrotOnLeftShoulder = entity.getParrotVariantOnShoulder(true);
        reusedState.parrotOnRightShoulder = entity.getParrotVariantOnShoulder(false);
        reusedState.id = entity.getId();
        reusedState.showExtraEars = entity.showExtraEars();
        reusedState.heldOnHead.clear();
        if (reusedState.isUsingItem) {
            ItemStack itemstack = entity.getItemInHand(reusedState.useItemHand);
            if (itemstack.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SPYGLASS_SCOPE)) {
                this.itemModelResolver.updateForLiving(reusedState.heldOnHead, itemstack, ItemDisplayContext.HEAD, entity);
            }
        }
    }

    protected boolean shouldShowName(AvatarlikeEntity entity, double distanceToCameraSq) {
        return super.shouldShowName(entity, distanceToCameraSq)
            && (entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
    }

    private void extractFlightData(AvatarlikeEntity entity, AvatarRenderState reusedState, float partialTick) {
        reusedState.fallFlyingTimeInTicks = entity.getFallFlyingTicks() + partialTick;
        Vec3 vec3 = entity.getViewVector(partialTick);
        Vec3 vec31 = entity.avatarState().deltaMovementOnPreviousTick().lerp(entity.getDeltaMovement(), partialTick);
        if (vec31.horizontalDistanceSqr() > 1.0E-5F && vec3.horizontalDistanceSqr() > 1.0E-5F) {
            reusedState.shouldApplyFlyingYRot = true;
            double d0 = vec31.horizontal().normalize().dot(vec3.horizontal().normalize());
            double d1 = vec31.x * vec3.z - vec31.z * vec3.x;
            reusedState.flyingYRot = (float)(Math.signum(d1) * Math.acos(Math.min(1.0, Math.abs(d0))));
        } else {
            reusedState.shouldApplyFlyingYRot = false;
            reusedState.flyingYRot = 0.0F;
        }
    }

    private void extractCapeState(AvatarlikeEntity entity, AvatarRenderState renderState, float partialTick) {
        ClientAvatarState clientavatarstate = entity.avatarState();
        double d0 = clientavatarstate.getInterpolatedCloakX(partialTick) - Mth.lerp((double)partialTick, entity.xo, entity.getX());
        double d1 = clientavatarstate.getInterpolatedCloakY(partialTick) - Mth.lerp((double)partialTick, entity.yo, entity.getY());
        double d2 = clientavatarstate.getInterpolatedCloakZ(partialTick) - Mth.lerp((double)partialTick, entity.zo, entity.getZ());
        float f = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        double d3 = Mth.sin(f * (float) (Math.PI / 180.0));
        double d4 = -Mth.cos(f * (float) (Math.PI / 180.0));
        renderState.capeFlap = (float)d1 * 10.0F;
        renderState.capeFlap = Mth.clamp(renderState.capeFlap, -6.0F, 32.0F);
        renderState.capeLean = (float)(d0 * d3 + d2 * d4) * 100.0F;
        renderState.capeLean = renderState.capeLean * (1.0F - renderState.fallFlyingScale());
        renderState.capeLean = Mth.clamp(renderState.capeLean, 0.0F, 150.0F);
        renderState.capeLean2 = (float)(d0 * d4 - d2 * d3) * 100.0F;
        renderState.capeLean2 = Mth.clamp(renderState.capeLean2, -20.0F, 20.0F);
        float f1 = clientavatarstate.getInterpolatedBob(partialTick);
        float f2 = clientavatarstate.getInterpolatedWalkDistance(partialTick);
        renderState.capeFlap = renderState.capeFlap + Mth.sin(f2 * 6.0F) * 32.0F * f1;
    }

    /**
     * @deprecated Neo: use {@link #renderRightHand(PoseStack, SubmitNodeCollector,
     *             int, ResourceLocation, boolean,
     *             net.minecraft.client.player.AbstractClientPlayer)} instead
     */
    @Deprecated
    public void renderRightHand(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ResourceLocation skinTexture, boolean renderSleeve) {
        this.renderRightHand(poseStack, nodeCollector, packedLight, skinTexture, renderSleeve, net.minecraft.client.Minecraft.getInstance().player);
    }

    public void renderRightHand(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ResourceLocation skinTexture, boolean renderSleeve, net.minecraft.client.player.AbstractClientPlayer player) {
        if(!net.neoforged.neoforge.client.ClientHooks.renderSpecificFirstPersonArm(poseStack, nodeCollector, packedLight, player, HumanoidArm.RIGHT))
        this.renderHand(poseStack, nodeCollector, packedLight, skinTexture, this.model.rightArm, renderSleeve);
    }

    /**
     * @deprecated Neo: use {@link #renderLeftHand(PoseStack, SubmitNodeCollector, int
     *             , ResourceLocation, boolean,
     *             net.minecraft.client.player.AbstractClientPlayer)} instead
     */
    @Deprecated
    public void renderLeftHand(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ResourceLocation skinTexture, boolean renderSleeve) {
        this.renderLeftHand(poseStack, nodeCollector, packedLight, skinTexture, renderSleeve, net.minecraft.client.Minecraft.getInstance().player);
    }

    public void renderLeftHand(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ResourceLocation skinTexture, boolean renderSleeve, net.minecraft.client.player.AbstractClientPlayer player) {
        if(!net.neoforged.neoforge.client.ClientHooks.renderSpecificFirstPersonArm(poseStack, nodeCollector, packedLight, player, HumanoidArm.LEFT))
        this.renderHand(poseStack, nodeCollector, packedLight, skinTexture, this.model.leftArm, renderSleeve);
    }

    private void renderHand(
        PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ResourceLocation skinTexture, ModelPart arm, boolean renderSleeve
    ) {
        PlayerModel playermodel = this.getModel();
        arm.resetPose();
        arm.visible = true;
        playermodel.leftSleeve.visible = renderSleeve;
        playermodel.rightSleeve.visible = renderSleeve;
        playermodel.leftArm.zRot = -0.1F;
        playermodel.rightArm.zRot = 0.1F;
        nodeCollector.submitModelPart(arm, poseStack, RenderType.entityTranslucent(skinTexture), packedLight, OverlayTexture.NO_OVERLAY, null);
    }

    protected void setupRotations(AvatarRenderState renderState, PoseStack poseStack, float bodyRot, float scale) {
        float f = renderState.swimAmount;
        float f1 = renderState.xRot;
        if (renderState.isFallFlying) {
            super.setupRotations(renderState, poseStack, bodyRot, scale);
            float f2 = renderState.fallFlyingScale();
            if (!renderState.isAutoSpinAttack) {
                poseStack.mulPose(Axis.XP.rotationDegrees(f2 * (-90.0F - f1)));
            }

            if (renderState.shouldApplyFlyingYRot) {
                poseStack.mulPose(Axis.YP.rotation(renderState.flyingYRot));
            }
        } else if (f > 0.0F) {
            super.setupRotations(renderState, poseStack, bodyRot, scale);
            float f4 = renderState.isInWater ? -90.0F - f1 : -90.0F;
            float f3 = Mth.lerp(f, 0.0F, f4);
            poseStack.mulPose(Axis.XP.rotationDegrees(f3));
            if (renderState.isVisuallySwimming) {
                poseStack.translate(0.0F, -1.0F, 0.3F);
            }
        } else {
            super.setupRotations(renderState, poseStack, bodyRot, scale);
        }
    }

    public boolean isEntityUpsideDown(AvatarlikeEntity entity) {
        if (entity.isModelPartShown(PlayerModelPart.CAPE)) {
            return entity instanceof Player player ? isPlayerUpsideDown(player) : super.isEntityUpsideDown(entity);
        } else {
            return false;
        }
    }

    public static boolean isPlayerUpsideDown(Player player) {
        return isUpsideDownName(player.getGameProfile().name());
    }
}
