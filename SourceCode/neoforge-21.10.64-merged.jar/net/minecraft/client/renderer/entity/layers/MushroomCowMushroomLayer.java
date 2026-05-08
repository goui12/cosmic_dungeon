package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MushroomCowMushroomLayer extends RenderLayer<MushroomCowRenderState, CowModel> {
    private final BlockRenderDispatcher blockRenderer;

    public MushroomCowMushroomLayer(RenderLayerParent<MushroomCowRenderState, CowModel> renderer, BlockRenderDispatcher blockRenderer) {
        super(renderer);
        this.blockRenderer = blockRenderer;
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, MushroomCowRenderState renderState, float yRot, float xRot) {
        if (!renderState.isBaby) {
            boolean flag = renderState.appearsGlowing() && renderState.isInvisible;
            if (!renderState.isInvisible || flag) {
                BlockState blockstate = renderState.variant.getBlockState();
                int i = LivingEntityRenderer.getOverlayCoords(renderState, 0.0F);
                BlockStateModel blockstatemodel = this.blockRenderer.getBlockModel(blockstate);
                poseStack.pushPose();
                poseStack.translate(0.2F, -0.35F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-48.0F));
                poseStack.scale(-1.0F, -1.0F, 1.0F);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                this.submitMushroomBlock(poseStack, nodeCollector, packedLight, flag, renderState.outlineColor, blockstate, i, blockstatemodel);
                poseStack.popPose();
                poseStack.pushPose();
                poseStack.translate(0.2F, -0.35F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(42.0F));
                poseStack.translate(0.1F, 0.0F, -0.6F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-48.0F));
                poseStack.scale(-1.0F, -1.0F, 1.0F);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                this.submitMushroomBlock(poseStack, nodeCollector, packedLight, flag, renderState.outlineColor, blockstate, i, blockstatemodel);
                poseStack.popPose();
                poseStack.pushPose();
                this.getParentModel().getHead().translateAndRotate(poseStack);
                poseStack.translate(0.0F, -0.7F, -0.2F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-78.0F));
                poseStack.scale(-1.0F, -1.0F, 1.0F);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                this.submitMushroomBlock(poseStack, nodeCollector, packedLight, flag, renderState.outlineColor, blockstate, i, blockstatemodel);
                poseStack.popPose();
            }
        }
    }

    private void submitMushroomBlock(
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        boolean renderOutline,
        int outlineColor,
        BlockState blockState,
        int packedOverlay,
        BlockStateModel model
    ) {
        if (renderOutline) {
            nodeCollector.submitBlockModel(
                poseStack, RenderType.outline(TextureAtlas.LOCATION_BLOCKS), model, 0.0F, 0.0F, 0.0F, packedLight, packedOverlay, outlineColor
            );
        } else {
            nodeCollector.submitBlock(poseStack, blockState, packedLight, packedOverlay, outlineColor);
        }
    }
}
