package net.minecraft.client.renderer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemInHandRenderer {
    private static final RenderType MAP_BACKGROUND = RenderType.text(ResourceLocation.withDefaultNamespace("textures/map/map_background.png"));
    private static final RenderType MAP_BACKGROUND_CHECKERBOARD = RenderType.text(
        ResourceLocation.withDefaultNamespace("textures/map/map_background_checkerboard.png")
    );
    private static final float ITEM_SWING_X_POS_SCALE = -0.4F;
    private static final float ITEM_SWING_Y_POS_SCALE = 0.2F;
    private static final float ITEM_SWING_Z_POS_SCALE = -0.2F;
    private static final float ITEM_HEIGHT_SCALE = -0.6F;
    private static final float ITEM_POS_X = 0.56F;
    private static final float ITEM_POS_Y = -0.52F;
    private static final float ITEM_POS_Z = -0.72F;
    private static final float ITEM_PRESWING_ROT_Y = 45.0F;
    private static final float ITEM_SWING_X_ROT_AMOUNT = -80.0F;
    private static final float ITEM_SWING_Y_ROT_AMOUNT = -20.0F;
    private static final float ITEM_SWING_Z_ROT_AMOUNT = -20.0F;
    private static final float EAT_JIGGLE_X_ROT_AMOUNT = 10.0F;
    private static final float EAT_JIGGLE_Y_ROT_AMOUNT = 90.0F;
    private static final float EAT_JIGGLE_Z_ROT_AMOUNT = 30.0F;
    private static final float EAT_JIGGLE_X_POS_SCALE = 0.6F;
    private static final float EAT_JIGGLE_Y_POS_SCALE = -0.5F;
    private static final float EAT_JIGGLE_Z_POS_SCALE = 0.0F;
    private static final double EAT_JIGGLE_EXPONENT = 27.0;
    private static final float EAT_EXTRA_JIGGLE_CUTOFF = 0.8F;
    private static final float EAT_EXTRA_JIGGLE_SCALE = 0.1F;
    private static final float ARM_SWING_X_POS_SCALE = -0.3F;
    private static final float ARM_SWING_Y_POS_SCALE = 0.4F;
    private static final float ARM_SWING_Z_POS_SCALE = -0.4F;
    private static final float ARM_SWING_Y_ROT_AMOUNT = 70.0F;
    private static final float ARM_SWING_Z_ROT_AMOUNT = -20.0F;
    private static final float ARM_HEIGHT_SCALE = -0.6F;
    private static final float ARM_POS_SCALE = 0.8F;
    private static final float ARM_POS_X = 0.8F;
    private static final float ARM_POS_Y = -0.75F;
    private static final float ARM_POS_Z = -0.9F;
    private static final float ARM_PRESWING_ROT_Y = 45.0F;
    private static final float ARM_PREROTATION_X_OFFSET = -1.0F;
    private static final float ARM_PREROTATION_Y_OFFSET = 3.6F;
    private static final float ARM_PREROTATION_Z_OFFSET = 3.5F;
    private static final float ARM_POSTROTATION_X_OFFSET = 5.6F;
    private static final int ARM_ROT_X = 200;
    private static final int ARM_ROT_Y = -135;
    private static final int ARM_ROT_Z = 120;
    private static final float MAP_SWING_X_POS_SCALE = -0.4F;
    private static final float MAP_SWING_Z_POS_SCALE = -0.2F;
    private static final float MAP_HANDS_POS_X = 0.0F;
    private static final float MAP_HANDS_POS_Y = 0.04F;
    private static final float MAP_HANDS_POS_Z = -0.72F;
    private static final float MAP_HANDS_HEIGHT_SCALE = -1.2F;
    private static final float MAP_HANDS_TILT_SCALE = -0.5F;
    private static final float MAP_PLAYER_PITCH_SCALE = 45.0F;
    private static final float MAP_HANDS_Z_ROT_AMOUNT = -85.0F;
    private static final float MAPHAND_X_ROT_AMOUNT = 45.0F;
    private static final float MAPHAND_Y_ROT_AMOUNT = 92.0F;
    private static final float MAPHAND_Z_ROT_AMOUNT = -41.0F;
    private static final float MAP_HAND_X_POS = 0.3F;
    private static final float MAP_HAND_Y_POS = -1.1F;
    private static final float MAP_HAND_Z_POS = 0.45F;
    private static final float MAP_SWING_X_ROT_AMOUNT = 20.0F;
    private static final float MAP_PRE_ROT_SCALE = 0.38F;
    private static final float MAP_GLOBAL_X_POS = -0.5F;
    private static final float MAP_GLOBAL_Y_POS = -0.5F;
    private static final float MAP_GLOBAL_Z_POS = 0.0F;
    private static final float MAP_FINAL_SCALE = 0.0078125F;
    private static final int MAP_BORDER = 7;
    private static final int MAP_HEIGHT = 128;
    private static final int MAP_WIDTH = 128;
    private static final float BOW_CHARGE_X_POS_SCALE = 0.0F;
    private static final float BOW_CHARGE_Y_POS_SCALE = 0.0F;
    private static final float BOW_CHARGE_Z_POS_SCALE = 0.04F;
    private static final float BOW_CHARGE_SHAKE_X_SCALE = 0.0F;
    private static final float BOW_CHARGE_SHAKE_Y_SCALE = 0.004F;
    private static final float BOW_CHARGE_SHAKE_Z_SCALE = 0.0F;
    private static final float BOW_CHARGE_Z_SCALE = 0.2F;
    private static final float BOW_MIN_SHAKE_CHARGE = 0.1F;
    private final Minecraft minecraft;
    private final MapRenderState mapRenderState = new MapRenderState();
    private ItemStack mainHandItem = ItemStack.EMPTY;
    private ItemStack offHandItem = ItemStack.EMPTY;
    private float mainHandHeight;
    private float oMainHandHeight;
    private float offHandHeight;
    private float oOffHandHeight;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final ItemRenderer itemRenderer;
    private final ItemModelResolver itemModelResolver;

    public ItemInHandRenderer(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, ItemRenderer itemRenderer, ItemModelResolver itemModelResolver) {
        this.minecraft = minecraft;
        this.entityRenderDispatcher = entityRenderDispatcher;
        this.itemRenderer = itemRenderer;
        this.itemModelResolver = itemModelResolver;
    }

    public void renderItem(
        LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight
    ) {
        if (!stack.isEmpty()) {
            ItemStackRenderState itemstackrenderstate = new ItemStackRenderState();
            this.itemModelResolver
                .updateForTopItem(itemstackrenderstate, stack, displayContext, entity.level(), entity, entity.getId() + displayContext.ordinal());
            itemstackrenderstate.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, 0);
        }
    }

    /**
     * Return the angle to render the Map
     */
    private float calculateMapTilt(float pitch) {
        float f = 1.0F - pitch / 45.0F + 0.1F;
        f = Mth.clamp(f, 0.0F, 1.0F);
        return -Mth.cos(f * (float) Math.PI) * 0.5F + 0.5F;
    }

    private void renderMapHand(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, HumanoidArm arm) {
        AvatarRenderer<AbstractClientPlayer> avatarrenderer = this.entityRenderDispatcher.getPlayerRenderer(this.minecraft.player);
        poseStack.pushPose();
        float f = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * -41.0F));
        poseStack.translate(f * 0.3F, -1.1F, 0.45F);
        ResourceLocation resourcelocation = this.minecraft.player.getSkin().body().texturePath();
        if (arm == HumanoidArm.RIGHT) {
            avatarrenderer.renderRightHand(
                poseStack, nodeCollector, packedLight, resourcelocation, this.minecraft.player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), this.minecraft.player
            );
        } else {
            avatarrenderer.renderLeftHand(
                poseStack, nodeCollector, packedLight, resourcelocation, this.minecraft.player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), this.minecraft.player
            );
        }

        poseStack.popPose();
    }

    private void renderOneHandedMap(
        PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, float equippedProgress, HumanoidArm arm, float swingProgress, ItemStack stack
    ) {
        float f = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate(f * 0.125F, -0.125F, 0.0F);
        if (!this.minecraft.player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(f * 10.0F));
            this.renderPlayerArm(poseStack, nodeCollector, packedLight, equippedProgress, swingProgress, arm);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(f * 0.51F, -0.08F + equippedProgress * -1.2F, -0.75F);
        float f1 = Mth.sqrt(swingProgress);
        float f2 = Mth.sin(f1 * (float) Math.PI);
        float f3 = -0.5F * f2;
        float f4 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f5 = -0.3F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(f * f3, f4 - 0.3F * f2, f5);
        poseStack.mulPose(Axis.XP.rotationDegrees(f2 * -45.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(f * f2 * -30.0F));
        this.renderMap(poseStack, nodeCollector, packedLight, stack);
        poseStack.popPose();
    }

    private void renderTwoHandedMap(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, float pitch, float equippedProgress, float swingProgress) {
        float f = Mth.sqrt(swingProgress);
        float f1 = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        float f2 = -0.4F * Mth.sin(f * (float) Math.PI);
        poseStack.translate(0.0F, -f1 / 2.0F, f2);
        float f3 = this.calculateMapTilt(pitch);
        poseStack.translate(0.0F, 0.04F + equippedProgress * -1.2F + f3 * -0.5F, -0.72F);
        poseStack.mulPose(Axis.XP.rotationDegrees(f3 * -85.0F));
        if (!this.minecraft.player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            this.renderMapHand(poseStack, nodeCollector, packedLight, HumanoidArm.RIGHT);
            this.renderMapHand(poseStack, nodeCollector, packedLight, HumanoidArm.LEFT);
            poseStack.popPose();
        }

        float f4 = Mth.sin(f * (float) Math.PI);
        poseStack.mulPose(Axis.XP.rotationDegrees(f4 * 20.0F));
        poseStack.scale(2.0F, 2.0F, 2.0F);
        this.renderMap(poseStack, nodeCollector, packedLight, this.mainHandItem);
    }

    private void renderMap(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, ItemStack stack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.38F, 0.38F, 0.38F);
        poseStack.translate(-0.5F, -0.5F, 0.0F);
        poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
        MapId mapid = stack.get(DataComponents.MAP_ID);
        MapItemSavedData mapitemsaveddata = MapItem.getSavedData(stack, this.minecraft.level);
        RenderType rendertype = mapitemsaveddata == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD;
        nodeCollector.submitCustomGeometry(poseStack, rendertype, (p_439463_, p_439903_) -> {
            p_439903_.addVertex(p_439463_, -7.0F, 135.0F, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setLight(packedLight);
            p_439903_.addVertex(p_439463_, 135.0F, 135.0F, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setLight(packedLight);
            p_439903_.addVertex(p_439463_, 135.0F, -7.0F, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setLight(packedLight);
            p_439903_.addVertex(p_439463_, -7.0F, -7.0F, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(packedLight);
        });
        if (mapitemsaveddata != null) {
            MapRenderer maprenderer = this.minecraft.getMapRenderer();
            maprenderer.extractRenderState(mapid, mapitemsaveddata, this.mapRenderState);
            maprenderer.render(this.mapRenderState, poseStack, nodeCollector, false, packedLight);
        }
    }

    private void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, float equippedProgress, float swingProgress, HumanoidArm arm) {
        boolean flag = arm != HumanoidArm.LEFT;
        float f = flag ? 1.0F : -1.0F;
        float f1 = Mth.sqrt(swingProgress);
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(f * (f2 + 0.64000005F), f3 + -0.6F + equippedProgress * -0.6F, f4 + -0.71999997F);
        poseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        float f5 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float f6 = Mth.sin(f1 * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));
        AbstractClientPlayer abstractclientplayer = this.minecraft.player;
        poseStack.translate(f * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        poseStack.translate(f * 5.6F, 0.0F, 0.0F);
        AvatarRenderer<AbstractClientPlayer> avatarrenderer = this.entityRenderDispatcher.getPlayerRenderer(abstractclientplayer);
        ResourceLocation resourcelocation = abstractclientplayer.getSkin().body().texturePath();
        if (flag) {
            avatarrenderer.renderRightHand(
                poseStack, nodeCollector, packedLight, resourcelocation, abstractclientplayer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), abstractclientplayer
            );
        } else {
            avatarrenderer.renderLeftHand(poseStack, nodeCollector, packedLight, resourcelocation, abstractclientplayer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), abstractclientplayer);
        }
    }

    private void applyEatTransform(PoseStack poseStack, float partialTick, HumanoidArm arm, ItemStack stack, Player player) {
        float f = player.getUseItemRemainingTicks() - partialTick + 1.0F;
        float f1 = f / stack.getUseDuration(player);
        if (f1 < 0.8F) {
            float f2 = Mth.abs(Mth.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            poseStack.translate(0.0F, f2, 0.0F);
        }

        float f3 = 1.0F - (float)Math.pow(f1, 27.0);
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(f3 * 0.6F * i, f3 * -0.5F, f3 * 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(i * f3 * 90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(f3 * 10.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(i * f3 * 30.0F));
    }

    private void applyBrushTransform(PoseStack poseStack, float partialTick, HumanoidArm arm, ItemStack stack, Player player, float equippedProgress) {
        this.applyItemArmTransform(poseStack, arm, equippedProgress);
        float f = player.getUseItemRemainingTicks() % 10;
        float f1 = f - partialTick + 1.0F;
        float f2 = 1.0F - f1 / 10.0F;
        float f3 = -90.0F;
        float f4 = 60.0F;
        float f5 = 150.0F;
        float f6 = -15.0F;
        int i = 2;
        float f7 = -15.0F + 75.0F * Mth.cos(f2 * 2.0F * (float) Math.PI);
        if (arm != HumanoidArm.RIGHT) {
            poseStack.translate(0.1, 0.83, 0.35);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(f7));
            poseStack.translate(-0.3, 0.22, 0.35);
        } else {
            poseStack.translate(-0.25, 0.22, 0.35);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(f7));
        }
    }

    private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm hand, float swingProgress) {
        int i = hand == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0F + f * -20.0F)));
        float f1 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        poseStack.mulPose(Axis.ZP.rotationDegrees(i * f1 * -20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(f1 * -80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0F));
    }

    private void applyItemArmTransform(PoseStack poseStack, HumanoidArm hand, float equippedProg) {
        int i = hand == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(i * 0.56F, -0.52F + equippedProg * -0.6F, -0.72F);
    }

    public void renderHandsWithItems(float partialTick, PoseStack poseStack, SubmitNodeCollector nodeCollector, LocalPlayer player, int packedLight) {
        float f = player.getAttackAnim(partialTick);
        InteractionHand interactionhand = MoreObjects.firstNonNull(player.swingingArm, InteractionHand.MAIN_HAND);
        float f1 = player.getXRot(partialTick);
        ItemInHandRenderer.HandRenderSelection iteminhandrenderer$handrenderselection = evaluateWhichHandsToRender(player);
        float f2 = Mth.lerp(partialTick, player.xBobO, player.xBob);
        float f3 = Mth.lerp(partialTick, player.yBobO, player.yBob);
        poseStack.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(partialTick) - f2) * 0.1F));
        poseStack.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(partialTick) - f3) * 0.1F));
        if (iteminhandrenderer$handrenderselection.renderMainHand) {
            float f4 = interactionhand == InteractionHand.MAIN_HAND ? f : 0.0F;
            float f5 = 1.0F - Mth.lerp(partialTick, this.oMainHandHeight, this.mainHandHeight);
            if(!net.neoforged.neoforge.client.ClientHooks.renderSpecificFirstPersonHand(InteractionHand.MAIN_HAND, poseStack, nodeCollector, packedLight, partialTick, f1, f4, f5, this.mainHandItem))
            this.renderArmWithItem(player, partialTick, f1, InteractionHand.MAIN_HAND, f4, this.mainHandItem, f5, poseStack, nodeCollector, packedLight);
        }

        if (iteminhandrenderer$handrenderselection.renderOffHand) {
            float f6 = interactionhand == InteractionHand.OFF_HAND ? f : 0.0F;
            float f7 = 1.0F - Mth.lerp(partialTick, this.oOffHandHeight, this.offHandHeight);
            if(!net.neoforged.neoforge.client.ClientHooks.renderSpecificFirstPersonHand(InteractionHand.OFF_HAND, poseStack, nodeCollector, packedLight, partialTick, f1, f6, f7, this.offHandItem))
            this.renderArmWithItem(player, partialTick, f1, InteractionHand.OFF_HAND, f6, this.offHandItem, f7, poseStack, nodeCollector, packedLight);
        }

        this.minecraft.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
        this.minecraft.renderBuffers().bufferSource().endBatch();
    }

    @VisibleForTesting
    static ItemInHandRenderer.HandRenderSelection evaluateWhichHandsToRender(LocalPlayer player) {
        ItemStack itemstack = player.getMainHandItem();
        ItemStack itemstack1 = player.getOffhandItem();
        boolean flag = itemstack.is(Items.BOW) || itemstack1.is(Items.BOW);
        boolean flag1 = itemstack.is(Items.CROSSBOW) || itemstack1.is(Items.CROSSBOW);
        if (!flag && !flag1) {
            return ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
        } else if (player.isUsingItem()) {
            return selectionUsingItemWhileHoldingBowLike(player);
        } else {
            return isChargedCrossbow(itemstack)
                ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY
                : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
        }
    }

    private static ItemInHandRenderer.HandRenderSelection selectionUsingItemWhileHoldingBowLike(LocalPlayer player) {
        ItemStack itemstack = player.getUseItem();
        InteractionHand interactionhand = player.getUsedItemHand();
        if (!itemstack.is(Items.BOW) && !itemstack.is(Items.CROSSBOW)) {
            return interactionhand == InteractionHand.MAIN_HAND && isChargedCrossbow(player.getOffhandItem())
                ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY
                : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
        } else {
            return ItemInHandRenderer.HandRenderSelection.onlyForHand(interactionhand);
        }
    }

    private static boolean isChargedCrossbow(ItemStack stack) {
        return stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack);
    }

    private void renderArmWithItem(
        AbstractClientPlayer player,
        float partialTick,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        ItemStack item,
        float equippedProgress,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight
    ) {
        if (!player.isScoping()) {
            boolean flag = hand == InteractionHand.MAIN_HAND;
            HumanoidArm humanoidarm = flag ? player.getMainArm() : player.getMainArm().getOpposite();
            poseStack.pushPose();
            if (item.isEmpty()) {
                if (flag && !player.isInvisible()) {
                    this.renderPlayerArm(poseStack, nodeCollector, packedLight, equippedProgress, swingProgress, humanoidarm);
                }
            } else if (item.getItem() instanceof MapItem) {
                if (flag && this.offHandItem.isEmpty()) {
                    this.renderTwoHandedMap(poseStack, nodeCollector, packedLight, pitch, equippedProgress, swingProgress);
                } else {
                    this.renderOneHandedMap(poseStack, nodeCollector, packedLight, equippedProgress, humanoidarm, swingProgress, item);
                }
            } else if (item.getItem() instanceof CrossbowItem) {
                boolean flag1 = CrossbowItem.isCharged(item);
                boolean flag2 = humanoidarm == HumanoidArm.RIGHT;
                int i = flag2 ? 1 : -1;
                if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand && !flag1) {
                    this.applyItemArmTransform(poseStack, humanoidarm, equippedProgress);
                    poseStack.translate(i * -0.4785682F, -0.094387F, 0.05731531F);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-11.935F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(i * 65.3F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(i * -9.785F));
                    float f = item.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
                    float f1 = f / CrossbowItem.getChargeDuration(item, player);
                    if (f1 > 1.0F) {
                        f1 = 1.0F;
                    }

                    if (f1 > 0.1F) {
                        float f2 = Mth.sin((f - 0.1F) * 1.3F);
                        float f3 = f1 - 0.1F;
                        float f4 = f2 * f3;
                        poseStack.translate(f4 * 0.0F, f4 * 0.004F, f4 * 0.0F);
                    }

                    poseStack.translate(f1 * 0.0F, f1 * 0.0F, f1 * 0.04F);
                    poseStack.scale(1.0F, 1.0F, 1.0F + f1 * 0.2F);
                    poseStack.mulPose(Axis.YN.rotationDegrees(i * 45.0F));
                } else {
                    this.swingArm(swingProgress, equippedProgress, poseStack, i, humanoidarm);
                    if (flag1 && swingProgress < 0.001F && flag) {
                        poseStack.translate(i * -0.641864F, 0.0F, 0.0F);
                        poseStack.mulPose(Axis.YP.rotationDegrees(i * 10.0F));
                    }
                }

                this.renderItem(
                    player,
                    item,
                    flag2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    poseStack,
                    nodeCollector,
                    packedLight
                );
            } else {
                boolean flag3 = humanoidarm == HumanoidArm.RIGHT;
                int j = flag3 ? 1 : -1;
                // Neo: Allow items to define custom arm animation
                if (!net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(item).applyForgeHandTransform(poseStack, minecraft.player, humanoidarm, item, partialTick, equippedProgress, swingProgress))
                if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                    switch (item.getUseAnimation()) {
                        case NONE:
                            this.applyItemArmTransform(poseStack, humanoidarm, equippedProgress);
                            break;
                        case EAT:
                        case DRINK:
                            this.applyEatTransform(poseStack, partialTick, humanoidarm, item, player);
                            this.applyItemArmTransform(poseStack, humanoidarm, equippedProgress);
                            break;
                        case BLOCK:
                            this.applyItemArmTransform(poseStack, humanoidarm, equippedProgress);
                            if (!(item.getItem() instanceof ShieldItem)) {
                                poseStack.translate(j * -0.14142136F, 0.08F, 0.14142136F);
                                poseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
                                poseStack.mulPose(Axis.YP.rotationDegrees(j * 13.365F));
                                poseStack.mulPose(Axis.ZP.rotationDegrees(j * 78.05F));
                            }
                            break;
                        case BOW:
                            this.applyItemArmTransform(poseStack, humanoidarm, equippedProgress);
                            poseStack.translate(j * -0.2785682F, 0.18344387F, 0.15731531F);
                            poseStack.mulPose(Axis.XP.rotationDegrees(-13.935F));
                            poseStack.mulPose(Axis.YP.rotationDegrees(j * 35.3F));
                            poseStack.mulPose(Axis.ZP.rotationDegrees(j * -9.785F));
                            float f6 = item.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
                            float f8 = f6 / 20.0F;
                            f8 = (f8 * f8 + f8 * 2.0F) / 3.0F;
                            if (f8 > 1.0F) {
                                f8 = 1.0F;
                            }

                            if (f8 > 0.1F) {
                                float f10 = Mth.sin((f6 - 0.1F) * 1.3F);
                                float f12 = f8 - 0.1F;
                                float f14 = f10 * f12;
                                poseStack.translate(f14 * 0.0F, f14 * 0.004F, f14 * 0.0F);
                            }

                            poseStack.translate(f8 * 0.0F, f8 * 0.0F, f8 * 0.04F);
                            poseStack.scale(1.0F, 1.0F, 1.0F + f8 * 0.2F);
                            poseStack.mulPose(Axis.YN.rotationDegrees(j * 45.0F));
                            break;
                        case SPEAR:
                            this.applyItemArmTransform(poseStack, humanoidarm, equippedProgress);
                            poseStack.translate(j * -0.5F, 0.7F, 0.1F);
                            poseStack.mulPose(Axis.XP.rotationDegrees(-55.0F));
                            poseStack.mulPose(Axis.YP.rotationDegrees(j * 35.3F));
                            poseStack.mulPose(Axis.ZP.rotationDegrees(j * -9.785F));
                            float f5 = item.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
                            float f7 = f5 / 10.0F;
                            if (f7 > 1.0F) {
                                f7 = 1.0F;
                            }

                            if (f7 > 0.1F) {
                                float f9 = Mth.sin((f5 - 0.1F) * 1.3F);
                                float f11 = f7 - 0.1F;
                                float f13 = f9 * f11;
                                poseStack.translate(f13 * 0.0F, f13 * 0.004F, f13 * 0.0F);
                            }

                            poseStack.translate(0.0F, 0.0F, f7 * 0.2F);
                            poseStack.scale(1.0F, 1.0F, 1.0F + f7 * 0.2F);
                            poseStack.mulPose(Axis.YN.rotationDegrees(j * 45.0F));
                            break;
                        case BRUSH:
                            this.applyBrushTransform(poseStack, partialTick, humanoidarm, item, player, equippedProgress);
                            break;
                        case BUNDLE:
                            this.swingArm(swingProgress, equippedProgress, poseStack, j, humanoidarm);
                    }
                } else if (player.isAutoSpinAttack()) {
                    this.applyItemArmTransform(poseStack, humanoidarm, equippedProgress);
                    poseStack.translate(j * -0.4F, 0.8F, 0.3F);
                    poseStack.mulPose(Axis.YP.rotationDegrees(j * 65.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(j * -85.0F));
                } else {
                    this.swingArm(swingProgress, equippedProgress, poseStack, j, humanoidarm);
                }

                this.renderItem(
                    player,
                    item,
                    flag3 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    poseStack,
                    nodeCollector,
                    packedLight
                );
            }

            poseStack.popPose();
        }
    }

    private void swingArm(float swingProgress, float equippedProgress, PoseStack poseStack, int direction, HumanoidArm arm) {
        float f = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        float f1 = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
        float f2 = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(direction * f, f1, f2);
        this.applyItemArmTransform(poseStack, arm, equippedProgress);
        this.applyItemArmAttackTransform(poseStack, arm, swingProgress);
    }

    private boolean shouldInstantlyReplaceVisibleItem(ItemStack oldItem, ItemStack newItem) {
        return ItemStack.matches(oldItem, newItem) ? true : !this.itemModelResolver.shouldPlaySwapAnimation(newItem);
    }

    public void tick() {
        this.oMainHandHeight = this.mainHandHeight;
        this.oOffHandHeight = this.offHandHeight;
        LocalPlayer localplayer = this.minecraft.player;
        ItemStack itemstack = localplayer.getMainHandItem();
        ItemStack itemstack1 = localplayer.getOffhandItem();
        if (this.shouldInstantlyReplaceVisibleItem(this.mainHandItem, itemstack)) {
            this.mainHandItem = itemstack;
        }

        if (this.shouldInstantlyReplaceVisibleItem(this.offHandItem, itemstack1)) {
            this.offHandItem = itemstack1;
        }

        if (localplayer.isHandsBusy()) {
            this.mainHandHeight = Mth.clamp(this.mainHandHeight - 0.4F, 0.0F, 1.0F);
            this.offHandHeight = Mth.clamp(this.offHandHeight - 0.4F, 0.0F, 1.0F);
        } else {
            // Neo: allow suppressing re-equip animation when only the stack's data changes
            boolean swapAnimMain = net.neoforged.neoforge.client.ClientHooks.shouldCauseReequipAnimation(this.mainHandItem, itemstack, localplayer.getInventory().getSelectedSlot());
            boolean swapAnimOff = net.neoforged.neoforge.client.ClientHooks.shouldCauseReequipAnimation(this.offHandItem, itemstack1, -1);

            if (!swapAnimMain && this.mainHandItem != itemstack)
                this.mainHandItem = itemstack;
            if (!swapAnimOff && this.offHandItem != itemstack1)
                this.offHandItem = itemstack1;

            float f = localplayer.getAttackStrengthScale(1.0F);
            float f1 = swapAnimMain ? 0.0F : f * f * f;
            float f2 = swapAnimOff ? 0.0F : 1.0F;
            this.mainHandHeight = this.mainHandHeight + Mth.clamp(f1 - this.mainHandHeight, -0.4F, 0.4F);
            this.offHandHeight = this.offHandHeight + Mth.clamp(f2 - this.offHandHeight, -0.4F, 0.4F);
        }

        if (this.mainHandHeight < 0.1F) {
            this.mainHandItem = itemstack;
        }

        if (this.offHandHeight < 0.1F) {
            this.offHandItem = itemstack1;
        }
    }

    public void itemUsed(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            this.mainHandHeight = 0.0F;
        } else {
            this.offHandHeight = 0.0F;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @VisibleForTesting
    static enum HandRenderSelection {
        RENDER_BOTH_HANDS(true, true),
        RENDER_MAIN_HAND_ONLY(true, false),
        RENDER_OFF_HAND_ONLY(false, true);

        final boolean renderMainHand;
        final boolean renderOffHand;

        private HandRenderSelection(boolean renderMainHand, boolean renderOffHand) {
            this.renderMainHand = renderMainHand;
            this.renderOffHand = renderOffHand;
        }

        public static ItemInHandRenderer.HandRenderSelection onlyForHand(InteractionHand hand) {
            return hand == InteractionHand.MAIN_HAND ? RENDER_MAIN_HAND_ONLY : RENDER_OFF_HAND_ONLY;
        }
    }
}
