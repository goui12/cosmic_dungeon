package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SnowGolemRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnowGolemHeadLayer extends RenderLayer<SnowGolemRenderState, SnowGolemModel> {
    private final BlockRenderDispatcher blockRenderer;

    public SnowGolemHeadLayer(RenderLayerParent<SnowGolemRenderState, SnowGolemModel> renderer, BlockRenderDispatcher blockRenderer) {
        super(renderer);
        this.blockRenderer = blockRenderer;
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, SnowGolemRenderState renderState, float yRot, float xRot) {
        if (renderState.hasPumpkin) {
            if (!renderState.isInvisible || renderState.appearsGlowing()) {
                poseStack.pushPose();
                this.getParentModel().getHead().translateAndRotate(poseStack);
                float f = 0.625F;
                poseStack.translate(0.0F, -0.34375F, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.625F, -0.625F, -0.625F);
                BlockState blockstate = Blocks.CARVED_PUMPKIN.defaultBlockState();
                BlockStateModel blockstatemodel = this.blockRenderer.getBlockModel(blockstate);
                int i = LivingEntityRenderer.getOverlayCoords(renderState, 0.0F);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                RenderType rendertype = renderState.appearsGlowing() && renderState.isInvisible
                    ? RenderType.outline(TextureAtlas.LOCATION_BLOCKS)
                    : ItemBlockRenderTypes.getRenderType(blockstate);
                nodeCollector.submitBlockModel(poseStack, rendertype, blockstatemodel, 0.0F, 0.0F, 0.0F, packedLight, i, renderState.outlineColor);
                poseStack.popPose();
            }
        }
    }
}
