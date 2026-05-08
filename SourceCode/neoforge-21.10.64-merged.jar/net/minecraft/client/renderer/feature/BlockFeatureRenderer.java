package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockFeatureRenderer {
    private final PoseStack poseStack = new PoseStack();

    public void render(SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource, BlockRenderDispatcher blockRenderDispatcher, OutlineBufferSource outlineBufferSource) {
        for (SubmitNodeStorage.MovingBlockSubmit submitnodestorage$movingblocksubmit : nodeCollection.getMovingBlockSubmits()) {
            MovingBlockRenderState movingblockrenderstate = submitnodestorage$movingblocksubmit.movingBlockRenderState();
            BlockState blockstate = movingblockrenderstate.blockState;
            List<BlockModelPart> list = blockRenderDispatcher.getBlockModel(blockstate)
                .collectParts(movingblockrenderstate.level, movingblockrenderstate.blockPos, movingblockrenderstate.blockState, RandomSource.create(blockstate.getSeed(movingblockrenderstate.randomSeedPos)));
            PoseStack posestack = new PoseStack();
            posestack.mulPose(submitnodestorage$movingblocksubmit.pose());
            blockRenderDispatcher.getModelRenderer()
                .tesselateBlock(
                    movingblockrenderstate,
                    list,
                    blockstate,
                    movingblockrenderstate.blockPos,
                    posestack,
                    // TODO: this needs further thought as it violates the "one submit == one rendertype" contract
                    renderType -> bufferSource.getBuffer(net.neoforged.neoforge.client.RenderTypeHelper.getMovingBlockRenderType(renderType)),
                    false,
                    OverlayTexture.NO_OVERLAY
                );
        }

        for (SubmitNodeStorage.BlockSubmit submitnodestorage$blocksubmit : nodeCollection.getBlockSubmits()) {
            this.poseStack.pushPose();
            this.poseStack.last().set(submitnodestorage$blocksubmit.pose());
            blockRenderDispatcher.renderSingleBlock(
                submitnodestorage$blocksubmit.state(),
                this.poseStack,
                bufferSource,
                submitnodestorage$blocksubmit.lightCoords(),
                submitnodestorage$blocksubmit.overlayCoords()
            );
            if (submitnodestorage$blocksubmit.outlineColor() != 0) {
                outlineBufferSource.setColor(submitnodestorage$blocksubmit.outlineColor());
                blockRenderDispatcher.renderSingleBlock(
                    submitnodestorage$blocksubmit.state(),
                    this.poseStack,
                    outlineBufferSource,
                    submitnodestorage$blocksubmit.lightCoords(),
                    submitnodestorage$blocksubmit.overlayCoords()
                );
            }

            this.poseStack.popPose();
        }

        for (SubmitNodeStorage.BlockModelSubmit submitnodestorage$blockmodelsubmit : nodeCollection.getBlockModelSubmits()) {
            ModelBlockRenderer.renderModel(
                submitnodestorage$blockmodelsubmit.pose(),
                bufferSource.getBuffer(submitnodestorage$blockmodelsubmit.renderType()),
                submitnodestorage$blockmodelsubmit.model(),
                submitnodestorage$blockmodelsubmit.r(),
                submitnodestorage$blockmodelsubmit.g(),
                submitnodestorage$blockmodelsubmit.b(),
                submitnodestorage$blockmodelsubmit.lightCoords(),
                submitnodestorage$blockmodelsubmit.overlayCoords()
            );
            if (submitnodestorage$blockmodelsubmit.outlineColor() != 0) {
                outlineBufferSource.setColor(submitnodestorage$blockmodelsubmit.outlineColor());
                ModelBlockRenderer.renderModel(
                    submitnodestorage$blockmodelsubmit.pose(),
                    outlineBufferSource.getBuffer(submitnodestorage$blockmodelsubmit.renderType()),
                    submitnodestorage$blockmodelsubmit.model(),
                    submitnodestorage$blockmodelsubmit.r(),
                    submitnodestorage$blockmodelsubmit.g(),
                    submitnodestorage$blockmodelsubmit.b(),
                    submitnodestorage$blockmodelsubmit.lightCoords(),
                    submitnodestorage$blockmodelsubmit.overlayCoords()
                );
            }
        }
    }
}
