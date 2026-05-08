package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Set;
import javax.annotation.Nullable;
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
import net.minecraft.client.renderer.blockentity.state.BedRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class BedRenderer implements BlockEntityRenderer<BedBlockEntity, BedRenderState> {
    private final MaterialSet materials;
    private final Model.Simple headModel;
    private final Model.Simple footModel;

    public BedRenderer(BlockEntityRendererProvider.Context context) {
        this(context.materials(), context.entityModelSet());
    }

    public BedRenderer(SpecialModelRenderer.BakingContext context) {
        this(context.materials(), context.entityModelSet());
    }

    public BedRenderer(MaterialSet materials, EntityModelSet modelSet) {
        this.materials = materials;
        this.headModel = new Model.Simple(modelSet.bakeLayer(ModelLayers.BED_HEAD), RenderType::entitySolid);
        this.footModel = new Model.Simple(modelSet.bakeLayer(ModelLayers.BED_FOOT), RenderType::entitySolid);
    }

    public static LayerDefinition createHeadLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 6.0F), PartPose.ZERO);
        partdefinition.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(50, 6).addBox(0.0F, 6.0F, 0.0F, 3.0F, 3.0F, 3.0F),
            PartPose.rotation((float) (Math.PI / 2), 0.0F, (float) (Math.PI / 2))
        );
        partdefinition.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(50, 18).addBox(-16.0F, 6.0F, 0.0F, 3.0F, 3.0F, 3.0F),
            PartPose.rotation((float) (Math.PI / 2), 0.0F, (float) Math.PI)
        );
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public static LayerDefinition createFootLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 22).addBox(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 6.0F), PartPose.ZERO);
        partdefinition.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(50, 0).addBox(0.0F, 6.0F, -16.0F, 3.0F, 3.0F, 3.0F),
            PartPose.rotation((float) (Math.PI / 2), 0.0F, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(50, 12).addBox(-16.0F, 6.0F, -16.0F, 3.0F, 3.0F, 3.0F),
            PartPose.rotation((float) (Math.PI / 2), 0.0F, (float) (Math.PI * 3.0 / 2.0))
        );
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public BedRenderState createRenderState() {
        return new BedRenderState();
    }

    public void extractRenderState(
        BedBlockEntity blockEntity, BedRenderState renderState, float partialTick, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.color = blockEntity.getColor();
        renderState.facing = blockEntity.getBlockState().getValue(BedBlock.FACING);
        renderState.isHead = blockEntity.getBlockState().getValue(BedBlock.PART) == BedPart.HEAD;
        if (blockEntity.getLevel() != null) {
            DoubleBlockCombiner.NeighborCombineResult<? extends BedBlockEntity> neighborcombineresult = DoubleBlockCombiner.combineWithNeigbour(
                BlockEntityType.BED,
                BedBlock::getBlockType,
                BedBlock::getConnectedDirection,
                ChestBlock.FACING,
                blockEntity.getBlockState(),
                blockEntity.getLevel(),
                blockEntity.getBlockPos(),
                (p_112202_, p_112203_) -> false
            );
            renderState.lightCoords = neighborcombineresult.apply(new BrightnessCombiner<>()).get(renderState.lightCoords);
        }
    }

    public void submit(BedRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        Material material = Sheets.getBedMaterial(renderState.color);
        this.submitPiece(
            poseStack,
            nodeCollector,
            renderState.isHead ? this.headModel : this.footModel,
            renderState.facing,
            material,
            renderState.lightCoords,
            OverlayTexture.NO_OVERLAY,
            false,
            renderState.breakProgress,
            0
        );
    }

    public void submitSpecial(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, Material material, int outlineColor) {
        this.submitPiece(poseStack, nodeCollector, this.headModel, Direction.SOUTH, material, packedLight, packedOverlay, false, null, outlineColor);
        this.submitPiece(poseStack, nodeCollector, this.footModel, Direction.SOUTH, material, packedLight, packedOverlay, true, null, outlineColor);
    }

    private void submitPiece(
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        Model.Simple model,
        Direction direction,
        Material material,
        int packedLight,
        int packedOverlay,
        boolean isFeet,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
        int outlineColor
    ) {
        poseStack.pushPose();
        preparePose(poseStack, isFeet, direction);
        nodeCollector.submitModel(
            model,
            Unit.INSTANCE,
            poseStack,
            material.renderType(RenderType::entitySolid),
            packedLight,
            packedOverlay,
            -1,
            this.materials.get(material),
            outlineColor,
            crumblingOverlay
        );
        poseStack.popPose();
    }

    private static void preparePose(PoseStack poseStack, boolean isFeet, Direction direction) {
        poseStack.translate(0.0F, 0.5625F, isFeet ? -1.0F : 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + direction.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    public void getExtents(Set<Vector3f> output) {
        PoseStack posestack = new PoseStack();
        preparePose(posestack, false, Direction.SOUTH);
        this.headModel.root().getExtentsForGui(posestack, output);
        posestack.setIdentity();
        preparePose(posestack, true, Direction.SOUTH);
        this.footModel.root().getExtentsForGui(posestack, output);
    }
}
