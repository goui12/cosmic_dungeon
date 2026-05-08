package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BrushableBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BrushableBlockRenderer implements BlockEntityRenderer<BrushableBlockEntity, BrushableBlockRenderState> {
    private final ItemModelResolver itemModelResolver;

    public BrushableBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public BrushableBlockRenderState createRenderState() {
        return new BrushableBlockRenderState();
    }

    public void extractRenderState(
        BrushableBlockEntity blockEntity,
        BrushableBlockRenderState renderState,
        float partialTick,
        Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.hitDirection = blockEntity.getHitDirection();
        renderState.dustProgress = blockEntity.getBlockState().getValue(BlockStateProperties.DUSTED);
        if (blockEntity.getLevel() != null && blockEntity.getHitDirection() != null) {
            renderState.lightCoords = LevelRenderer.getLightColor(
                LevelRenderer.BrightnessGetter.DEFAULT,
                blockEntity.getLevel(),
                blockEntity.getBlockState(),
                blockEntity.getBlockPos().relative(blockEntity.getHitDirection())
            );
        }

        this.itemModelResolver.updateForTopItem(renderState.itemState, blockEntity.getItem(), ItemDisplayContext.FIXED, blockEntity.getLevel(), null, 0);
    }

    public void submit(BrushableBlockRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.dustProgress > 0 && renderState.hitDirection != null && !renderState.itemState.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.5F, 0.0F);
            float[] afloat = this.translations(renderState.hitDirection, renderState.dustProgress);
            poseStack.translate(afloat[0], afloat[1], afloat[2]);
            poseStack.mulPose(Axis.YP.rotationDegrees(75.0F));
            boolean flag = renderState.hitDirection == Direction.EAST || renderState.hitDirection == Direction.WEST;
            poseStack.mulPose(Axis.YP.rotationDegrees((flag ? 90 : 0) + 11));
            poseStack.scale(0.5F, 0.5F, 0.5F);
            renderState.itemState.submit(poseStack, nodeCollector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    private float[] translations(Direction direction, int dustedLevel) {
        float[] afloat = new float[]{0.5F, 0.0F, 0.5F};
        float f = dustedLevel / 10.0F * 0.75F;
        switch (direction) {
            case EAST:
                afloat[0] = 0.73F + f;
                break;
            case WEST:
                afloat[0] = 0.25F - f;
                break;
            case UP:
                afloat[1] = 0.25F + f;
                break;
            case DOWN:
                afloat[1] = -0.23F - f;
                break;
            case NORTH:
                afloat[2] = 0.25F - f;
                break;
            case SOUTH:
                afloat[2] = 0.73F + f;
        }

        return afloat;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(BrushableBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX() - .25, pos.getY() - .25, pos.getZ() - .25, pos.getX() + 1.25, pos.getY() + 1.25, pos.getZ() + 1.25);
    }
}
