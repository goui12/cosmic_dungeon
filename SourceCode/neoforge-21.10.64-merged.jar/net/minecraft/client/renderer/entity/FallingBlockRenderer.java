package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FallingBlockRenderer extends EntityRenderer<FallingBlockEntity, FallingBlockRenderState> {
    public FallingBlockRenderer(EntityRendererProvider.Context p_174112_) {
        super(p_174112_);
        this.shadowRadius = 0.5F;
    }

    public boolean shouldRender(FallingBlockEntity p_362415_, Frustum p_364047_, double p_362218_, double p_363427_, double p_361722_) {
        return !super.shouldRender(p_362415_, p_364047_, p_362218_, p_363427_, p_361722_)
            ? false
            : p_362415_.getBlockState() != p_362415_.level().getBlockState(p_362415_.blockPosition());
    }

    public void submit(FallingBlockRenderState p_450955_, PoseStack p_435557_, SubmitNodeCollector p_433266_, CameraRenderState p_451470_) {
        BlockState blockstate = p_450955_.movingBlockRenderState.blockState;
        if (blockstate.getRenderShape() == RenderShape.MODEL) {
            p_435557_.pushPose();
            p_435557_.translate(-0.5, 0.0, -0.5);
            p_433266_.submitMovingBlock(p_435557_, p_450955_.movingBlockRenderState);
            p_435557_.popPose();
            super.submit(p_450955_, p_435557_, p_433266_, p_451470_);
        }
    }

    public FallingBlockRenderState createRenderState() {
        return new FallingBlockRenderState();
    }

    public void extractRenderState(FallingBlockEntity p_364559_, FallingBlockRenderState p_360509_, float p_361019_) {
        super.extractRenderState(p_364559_, p_360509_, p_361019_);
        BlockPos blockpos = BlockPos.containing(p_364559_.getX(), p_364559_.getBoundingBox().maxY, p_364559_.getZ());
        p_360509_.movingBlockRenderState.randomSeedPos = p_364559_.getStartPos();
        p_360509_.movingBlockRenderState.blockPos = blockpos;
        p_360509_.movingBlockRenderState.blockState = p_364559_.getBlockState();
        p_360509_.movingBlockRenderState.biome = p_364559_.level().getBiome(blockpos);
        p_360509_.movingBlockRenderState.level = p_364559_.level();
    }
}
