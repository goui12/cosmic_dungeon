package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.LecternRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LecternRenderer implements BlockEntityRenderer<LecternBlockEntity, LecternRenderState> {
    private final MaterialSet materials;
    private final BookModel bookModel;
    private final BookModel.State bookState = new BookModel.State(0.0F, 0.1F, 0.9F, 1.2F);

    public LecternRenderer(BlockEntityRendererProvider.Context context) {
        this.materials = context.materials();
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    public LecternRenderState createRenderState() {
        return new LecternRenderState();
    }

    public void extractRenderState(
        LecternBlockEntity blockEntity, LecternRenderState renderState, float partialTick, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.hasBook = blockEntity.getBlockState().getValue(LecternBlock.HAS_BOOK);
        renderState.yRot = blockEntity.getBlockState().getValue(LecternBlock.FACING).getClockWise().toYRot();
    }

    public void submit(LecternRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.hasBook) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 1.0625F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-renderState.yRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(67.5F));
            poseStack.translate(0.0F, -0.125F, 0.0F);
            nodeCollector.submitModel(
                this.bookModel,
                this.bookState,
                poseStack,
                EnchantTableRenderer.BOOK_LOCATION.renderType(RenderType::entitySolid),
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                -1,
                this.materials.get(EnchantTableRenderer.BOOK_LOCATION),
                0,
                renderState.breakProgress
            );
            poseStack.popPose();
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(LecternBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.5, pos.getZ() + 1.0);
    }
}
