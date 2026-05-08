package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ShulkerBoxRenderer implements BlockEntityRenderer<ShulkerBoxBlockEntity, ShulkerBoxRenderState> {
    private final MaterialSet materials;
    private final ShulkerBoxRenderer.ShulkerBoxModel model;

    public ShulkerBoxRenderer(BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet(), context.materials());
    }

    public ShulkerBoxRenderer(SpecialModelRenderer.BakingContext context) {
        this(context.entityModelSet(), context.materials());
    }

    public ShulkerBoxRenderer(EntityModelSet modelSet, MaterialSet materials) {
        this.materials = materials;
        this.model = new ShulkerBoxRenderer.ShulkerBoxModel(modelSet.bakeLayer(ModelLayers.SHULKER_BOX));
    }

    public ShulkerBoxRenderState createRenderState() {
        return new ShulkerBoxRenderState();
    }

    public void extractRenderState(
        ShulkerBoxBlockEntity blockEntity,
        ShulkerBoxRenderState renderState,
        float partialTick,
        Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.direction = blockEntity.getBlockState().getValueOrElse(ShulkerBoxBlock.FACING, Direction.UP);
        renderState.color = blockEntity.getColor();
        renderState.progress = blockEntity.getProgress(partialTick);
    }

    public void submit(ShulkerBoxRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        DyeColor dyecolor = renderState.color;
        Material material;
        if (dyecolor == null) {
            material = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION;
        } else {
            material = Sheets.getShulkerBoxMaterial(dyecolor);
        }

        this.submit(
            poseStack,
            nodeCollector,
            renderState.lightCoords,
            OverlayTexture.NO_OVERLAY,
            renderState.direction,
            renderState.progress,
            renderState.breakProgress,
            material,
            0
        );
    }

    public void submit(
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        Direction direction,
        float progress,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
        Material material,
        int outlineColor
    ) {
        poseStack.pushPose();
        this.prepareModel(poseStack, direction, progress);
        nodeCollector.submitModel(
            this.model,
            progress,
            poseStack,
            material.renderType(this.model::renderType),
            packedLight,
            packedOverlay,
            -1,
            this.materials.get(material),
            outlineColor,
            crumblingOverlay
        );
        poseStack.popPose();
    }

    private void prepareModel(PoseStack poseStack, Direction direction, float progress) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        float f = 0.9995F;
        poseStack.scale(0.9995F, 0.9995F, 0.9995F);
        poseStack.mulPose(direction.getRotation());
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.0F, -1.0F, 0.0F);
        this.model.setupAnim(progress);
    }

    public void getExtents(Direction direction, float progress, Set<Vector3f> output) {
        PoseStack posestack = new PoseStack();
        this.prepareModel(posestack, direction, progress);
        this.model.root().getExtentsForGui(posestack, output);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(ShulkerBoxBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX() - 0.5, pos.getY() - 0.5, pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 1.5, pos.getZ() + 1.5);
    }

    @OnlyIn(Dist.CLIENT)
    static class ShulkerBoxModel extends Model<Float> {
        private final ModelPart lid;

        public ShulkerBoxModel(ModelPart root) {
            super(root, RenderType::entityCutoutNoCull);
            this.lid = root.getChild("lid");
        }

        public void setupAnim(Float renderState) {
            super.setupAnim(renderState);
            this.lid.setPos(0.0F, 24.0F - renderState * 0.5F * 16.0F, 0.0F);
            this.lid.yRot = 270.0F * renderState * (float) (Math.PI / 180.0);
        }
    }
}
