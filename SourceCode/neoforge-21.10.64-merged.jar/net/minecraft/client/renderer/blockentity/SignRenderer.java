package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SignRenderer extends AbstractSignRenderer {
    public static final float RENDER_SCALE = 0.6666667F;
    private static final Vec3 TEXT_OFFSET = new Vec3(0.0, 0.33333334F, 0.046666667F);
    private final Map<WoodType, SignRenderer.Models> signModels;

    public SignRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.signModels = WoodType.values()
            .collect(
                ImmutableMap.toImmutableMap(
                    p_173645_ -> (WoodType)p_173645_,
                    p_432289_ -> new SignRenderer.Models(
                        createSignModel(context.entityModelSet(), p_432289_, true), createSignModel(context.entityModelSet(), p_432289_, false)
                    )
                )
            );
    }

    @Override
    protected Model.Simple getSignModel(BlockState blockState, WoodType woodType) {
        SignRenderer.Models signrenderer$models = this.signModels.get(woodType);
        return blockState.getBlock() instanceof StandingSignBlock ? signrenderer$models.standing() : signrenderer$models.wall();
    }

    @Override
    protected Material getSignMaterial(WoodType woodType) {
        return Sheets.getSignMaterial(woodType);
    }

    @Override
    protected float getSignModelRenderScale() {
        return 0.6666667F;
    }

    @Override
    protected float getSignTextRenderScale() {
        return 0.6666667F;
    }

    private static void translateBase(PoseStack poseStack, float yRot) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
    }

    @Override
    protected void translateSign(PoseStack poseStack, float yRot, BlockState state) {
        translateBase(poseStack, yRot);
        if (!(state.getBlock() instanceof StandingSignBlock)) {
            poseStack.translate(0.0F, -0.3125F, -0.4375F);
        }
    }

    @Override
    protected Vec3 getTextOffset() {
        return TEXT_OFFSET;
    }

    public static void submitSpecial(
        MaterialSet materials, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, Model.Simple model, Material material
    ) {
        poseStack.pushPose();
        applyInHandTransforms(poseStack);
        nodeCollector.submitModel(
            model, Unit.INSTANCE, poseStack, material.renderType(model::renderType), packedLight, packedOverlay, -1, materials.get(material), 0, null
        );
        poseStack.popPose();
    }

    public static void applyInHandTransforms(PoseStack poseStack) {
        translateBase(poseStack, 0.0F);
        poseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
    }

    public static Model.Simple createSignModel(EntityModelSet modelSet, WoodType woodType, boolean standingSign) {
        ModelLayerLocation modellayerlocation = standingSign ? ModelLayers.createStandingSignModelName(woodType) : ModelLayers.createWallSignModelName(woodType);
        return new Model.Simple(modelSet.bakeLayer(modellayerlocation), RenderType::entityCutoutNoCull);
    }

    public static LayerDefinition createSignLayer(boolean standingSign) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("sign", CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, -14.0F, -1.0F, 24.0F, 12.0F, 2.0F), PartPose.ZERO);
        if (standingSign) {
            partdefinition.addOrReplaceChild("stick", CubeListBuilder.create().texOffs(0, 14).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 14.0F, 2.0F), PartPose.ZERO);
        }

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @OnlyIn(Dist.CLIENT)
    record Models(Model.Simple standing, Model.Simple wall) {
    }
}
