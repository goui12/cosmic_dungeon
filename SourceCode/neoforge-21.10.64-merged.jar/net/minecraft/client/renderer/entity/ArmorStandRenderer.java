package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.model.ArmorStandArmorModel;
import net.minecraft.client.model.ArmorStandModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmorStandRenderer extends LivingEntityRenderer<ArmorStand, ArmorStandRenderState, ArmorStandArmorModel> {
    /**
     * A constant instance of the armor stand texture, wrapped inside a ResourceLocation wrapper.
     */
    public static final ResourceLocation DEFAULT_SKIN_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/armorstand/wood.png");
    private final ArmorStandArmorModel bigModel = this.getModel();
    private final ArmorStandArmorModel smallModel;

    public ArmorStandRenderer(EntityRendererProvider.Context p_173915_) {
        super(p_173915_, new ArmorStandModel(p_173915_.bakeLayer(ModelLayers.ARMOR_STAND)), 0.0F);
        this.smallModel = new ArmorStandModel(p_173915_.bakeLayer(ModelLayers.ARMOR_STAND_SMALL));
        this.addLayer(
            new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR, p_173915_.getModelSet(), ArmorStandArmorModel::new),
                ArmorModelSet.bake(ModelLayers.ARMOR_STAND_SMALL_ARMOR, p_173915_.getModelSet(), ArmorStandArmorModel::new),
                p_173915_.getEquipmentRenderer()
            )
        );
        this.addLayer(new ItemInHandLayer<>(this));
        this.addLayer(new WingsLayer<>(this, p_173915_.getModelSet(), p_173915_.getEquipmentRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, p_173915_.getModelSet(), p_173915_.getPlayerSkinRenderCache()));
    }

    public ResourceLocation getTextureLocation(ArmorStandRenderState renderState) {
        return DEFAULT_SKIN_LOCATION;
    }

    public ArmorStandRenderState createRenderState() {
        return new ArmorStandRenderState();
    }

    public void extractRenderState(ArmorStand entity, ArmorStandRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, reusedState, partialTick, this.itemModelResolver);
        reusedState.yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        reusedState.isMarker = entity.isMarker();
        reusedState.isSmall = entity.isSmall();
        reusedState.showArms = entity.showArms();
        reusedState.showBasePlate = entity.showBasePlate();
        reusedState.bodyPose = entity.getBodyPose();
        reusedState.headPose = entity.getHeadPose();
        reusedState.leftArmPose = entity.getLeftArmPose();
        reusedState.rightArmPose = entity.getRightArmPose();
        reusedState.leftLegPose = entity.getLeftLegPose();
        reusedState.rightLegPose = entity.getRightLegPose();
        reusedState.wiggle = (float)(entity.level().getGameTime() - entity.lastHit) + partialTick;
    }

    public void submit(ArmorStandRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        this.model = renderState.isSmall ? this.smallModel : this.bigModel;
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
    }

    protected void setupRotations(ArmorStandRenderState renderState, PoseStack poseStack, float bodyRot, float scale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        if (renderState.wiggle < 5.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(renderState.wiggle / 1.5F * (float) Math.PI) * 3.0F));
        }
    }

    protected boolean shouldShowName(ArmorStand entity, double distanceToCameraSq) {
        return entity.isCustomNameVisible();
    }

    @Nullable
    protected RenderType getRenderType(ArmorStandRenderState renderState, boolean isVisible, boolean renderTranslucent, boolean appearsGlowing) {
        if (!renderState.isMarker) {
            return super.getRenderType(renderState, isVisible, renderTranslucent, appearsGlowing);
        } else {
            ResourceLocation resourcelocation = this.getTextureLocation(renderState);
            if (renderTranslucent) {
                return RenderType.entityTranslucent(resourcelocation, false);
            } else {
                return isVisible ? RenderType.entityCutoutNoCull(resourcelocation, false) : null;
            }
        }
    }
}
