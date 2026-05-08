package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.client.renderer.blockentity.state.TestInstanceRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TestInstanceRenderer implements BlockEntityRenderer<TestInstanceBlockEntity, TestInstanceRenderState> {
    private static final float ERROR_PADDING = 0.02F;
    private final BeaconRenderer<TestInstanceBlockEntity> beacon = new BeaconRenderer<>();
    private final BlockEntityWithBoundingBoxRenderer<TestInstanceBlockEntity> box = new BlockEntityWithBoundingBoxRenderer<>();
    private final Font font;
    private final EntityRenderDispatcher entityRenderer;

    public TestInstanceRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
        this.entityRenderer = context.entityRenderer();
    }

    public TestInstanceRenderState createRenderState() {
        return new TestInstanceRenderState();
    }

    public void extractRenderState(
        TestInstanceBlockEntity blockEntity,
        TestInstanceRenderState renderState,
        float partialTick,
        Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.beaconRenderState = new BeaconRenderState();
        BlockEntityRenderState.extractBase(blockEntity, renderState.beaconRenderState, breakProgress);
        BeaconRenderer.extract(blockEntity, renderState.beaconRenderState, partialTick, cameraPosition);
        renderState.blockEntityWithBoundingBoxRenderState = new BlockEntityWithBoundingBoxRenderState();
        BlockEntityRenderState.extractBase(blockEntity, renderState.blockEntityWithBoundingBoxRenderState, breakProgress);
        BlockEntityWithBoundingBoxRenderer.extract(blockEntity, renderState.blockEntityWithBoundingBoxRenderState);
        renderState.errorMarkers.clear();

        for (TestInstanceBlockEntity.ErrorMarker testinstanceblockentity$errormarker : blockEntity.getErrorMarkers()) {
            renderState.errorMarkers
                .add(
                    new TestInstanceBlockEntity.ErrorMarker(
                        testinstanceblockentity$errormarker.pos().subtract(blockEntity.getBlockPos()), testinstanceblockentity$errormarker.text()
                    )
                );
        }
    }

    public void submit(TestInstanceRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        this.beacon.submit(renderState.beaconRenderState, poseStack, nodeCollector, cameraRenderState);
        this.box.submit(renderState.blockEntityWithBoundingBoxRenderState, poseStack, nodeCollector, cameraRenderState);

        for (TestInstanceBlockEntity.ErrorMarker testinstanceblockentity$errormarker : renderState.errorMarkers) {
            this.submitErrorMarker(poseStack, nodeCollector, testinstanceblockentity$errormarker, cameraRenderState);
        }
    }

    private void submitErrorMarker(
        PoseStack poseStack, SubmitNodeCollector nodeCollector, TestInstanceBlockEntity.ErrorMarker marker, CameraRenderState cameraRenderState
    ) {
        BlockPos blockpos = marker.pos();
        nodeCollector.order(1).submitCustomGeometry(poseStack, RenderType.debugFilledBox(), (p_449349_, p_449011_) -> {
            float f1 = blockpos.getX() - 0.02F;
            float f2 = blockpos.getY() - 0.02F;
            float f3 = blockpos.getZ() - 0.02F;
            float f4 = blockpos.getX() + 1.0F + 0.02F;
            float f5 = blockpos.getY() + 1.0F + 0.02F;
            float f6 = blockpos.getZ() + 1.0F + 0.02F;
            PoseStack posestack = new PoseStack();
            posestack.last().set(p_449349_);
            ShapeRenderer.addChainedFilledBoxVertices(posestack, p_449011_, f1, f2, f3, f4, f5, f6, 1.0F, 0.0F, 0.0F, 0.375F);
        });
        FormattedCharSequence formattedcharsequence = marker.text().getVisualOrderText();
        int i = this.font.width(formattedcharsequence);
        float f = 0.01F;
        poseStack.pushPose();
        poseStack.translate(blockpos.getX() + 0.5F, blockpos.getY() + 1.2F, blockpos.getZ() + 0.5F);
        poseStack.mulPose(cameraRenderState.orientation);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        nodeCollector.order(2).submitText(poseStack, -i / 2.0F, 0.0F, formattedcharsequence, false, Font.DisplayMode.SEE_THROUGH, 15728880, -1, 0, 0);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return this.beacon.shouldRenderOffScreen() || this.box.shouldRenderOffScreen();
    }

    @Override
    public int getViewDistance() {
        return Math.max(this.beacon.getViewDistance(), this.box.getViewDistance());
    }

    public boolean shouldRender(TestInstanceBlockEntity blockEntity, Vec3 cameraPos) {
        return this.beacon.shouldRender(blockEntity, cameraPos) || this.box.shouldRender(blockEntity, cameraPos);
    }
}
