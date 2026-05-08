package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HangingSignRenderer extends AbstractSignRenderer {
    private static final String PLANK = "plank";
    private static final String V_CHAINS = "vChains";
    private static final String NORMAL_CHAINS = "normalChains";
    private static final String CHAIN_L_1 = "chainL1";
    private static final String CHAIN_L_2 = "chainL2";
    private static final String CHAIN_R_1 = "chainR1";
    private static final String CHAIN_R_2 = "chainR2";
    private static final String BOARD = "board";
    public static final float MODEL_RENDER_SCALE = 1.0F;
    private static final float TEXT_RENDER_SCALE = 0.9F;
    private static final Vec3 TEXT_OFFSET = new Vec3(0.0, -0.32F, 0.073F);
    private final Map<HangingSignRenderer.ModelKey, Model.Simple> hangingSignModels;

    public HangingSignRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        Stream<HangingSignRenderer.ModelKey> stream = WoodType.values()
            .flatMap(
                p_382549_ -> Arrays.stream(HangingSignRenderer.AttachmentType.values())
                    .map(p_382553_ -> new HangingSignRenderer.ModelKey(p_382549_, p_382553_))
            );
        this.hangingSignModels = stream.collect(
            ImmutableMap.toImmutableMap(
                p_382879_ -> (HangingSignRenderer.ModelKey)p_382879_,
                p_432287_ -> createSignModel(context.entityModelSet(), p_432287_.woodType, p_432287_.attachmentType)
            )
        );
    }

    public static Model.Simple createSignModel(EntityModelSet modelSet, WoodType woodType, HangingSignRenderer.AttachmentType attachmentType) {
        return new Model.Simple(modelSet.bakeLayer(ModelLayers.createHangingSignModelName(woodType, attachmentType)), RenderType::entityCutoutNoCull);
    }

    @Override
    protected float getSignModelRenderScale() {
        return 1.0F;
    }

    @Override
    protected float getSignTextRenderScale() {
        return 0.9F;
    }

    public static void translateBase(PoseStack poseStack, float yRot) {
        poseStack.translate(0.5, 0.9375, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(0.0F, -0.3125F, 0.0F);
    }

    @Override
    protected void translateSign(PoseStack poseStack, float yRot, BlockState state) {
        translateBase(poseStack, yRot);
    }

    @Override
    protected Model.Simple getSignModel(BlockState blockState, WoodType woodType) {
        HangingSignRenderer.AttachmentType hangingsignrenderer$attachmenttype = HangingSignRenderer.AttachmentType.byBlockState(blockState);
        return this.hangingSignModels.get(new HangingSignRenderer.ModelKey(woodType, hangingsignrenderer$attachmenttype));
    }

    @Override
    protected Material getSignMaterial(WoodType woodType) {
        return Sheets.getHangingSignMaterial(woodType);
    }

    @Override
    protected Vec3 getTextOffset() {
        return TEXT_OFFSET;
    }

    public static void submitSpecial(
        MaterialSet materials, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, Model.Simple model, Material material
    ) {
        poseStack.pushPose();
        translateBase(poseStack, 0.0F);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        nodeCollector.submitModel(
            model,
            Unit.INSTANCE,
            poseStack,
            material.renderType(model::renderType),
            packedLight,
            packedOverlay,
            -1,
            materials.get(material),
            OverlayTexture.NO_OVERLAY,
            null
        );
        poseStack.popPose();
    }

    public static LayerDefinition createHangingSignLayer(HangingSignRenderer.AttachmentType attachmentType) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("board", CubeListBuilder.create().texOffs(0, 12).addBox(-7.0F, 0.0F, -1.0F, 14.0F, 10.0F, 2.0F), PartPose.ZERO);
        if (attachmentType == HangingSignRenderer.AttachmentType.WALL) {
            partdefinition.addOrReplaceChild("plank", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -6.0F, -2.0F, 16.0F, 2.0F, 4.0F), PartPose.ZERO);
        }

        if (attachmentType == HangingSignRenderer.AttachmentType.WALL || attachmentType == HangingSignRenderer.AttachmentType.CEILING) {
            PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("normalChains", CubeListBuilder.create(), PartPose.ZERO);
            partdefinition1.addOrReplaceChild(
                "chainL1",
                CubeListBuilder.create().texOffs(0, 6).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
                PartPose.offsetAndRotation(-5.0F, -6.0F, 0.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
            );
            partdefinition1.addOrReplaceChild(
                "chainL2",
                CubeListBuilder.create().texOffs(6, 6).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
                PartPose.offsetAndRotation(-5.0F, -6.0F, 0.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
            );
            partdefinition1.addOrReplaceChild(
                "chainR1",
                CubeListBuilder.create().texOffs(0, 6).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
                PartPose.offsetAndRotation(5.0F, -6.0F, 0.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
            );
            partdefinition1.addOrReplaceChild(
                "chainR2",
                CubeListBuilder.create().texOffs(6, 6).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
                PartPose.offsetAndRotation(5.0F, -6.0F, 0.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
            );
        }

        if (attachmentType == HangingSignRenderer.AttachmentType.CEILING_MIDDLE) {
            partdefinition.addOrReplaceChild("vChains", CubeListBuilder.create().texOffs(14, 6).addBox(-6.0F, -6.0F, 0.0F, 12.0F, 6.0F, 0.0F), PartPose.ZERO);
        }

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum AttachmentType implements StringRepresentable {
        WALL("wall"),
        CEILING("ceiling"),
        CEILING_MIDDLE("ceiling_middle");

        private final String name;

        private AttachmentType(String name) {
            this.name = name;
        }

        public static HangingSignRenderer.AttachmentType byBlockState(BlockState blockState) {
            if (blockState.getBlock() instanceof CeilingHangingSignBlock) {
                return blockState.getValue(BlockStateProperties.ATTACHED) ? CEILING_MIDDLE : CEILING;
            } else {
                return WALL;
            }
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record ModelKey(WoodType woodType, HangingSignRenderer.AttachmentType attachmentType) {
    }
}
