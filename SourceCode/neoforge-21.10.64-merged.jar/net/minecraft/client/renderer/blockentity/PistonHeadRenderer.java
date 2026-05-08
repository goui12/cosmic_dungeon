package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PistonHeadRenderer implements BlockEntityRenderer<PistonMovingBlockEntity, PistonHeadRenderState> {
    public PistonHeadRenderState createRenderState() {
        return new PistonHeadRenderState();
    }

    public void extractRenderState(
        PistonMovingBlockEntity blockEntity,
        PistonHeadRenderState renderState,
        float partialTick,
        Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.xOffset = blockEntity.getXOff(partialTick);
        renderState.yOffset = blockEntity.getYOff(partialTick);
        renderState.zOffset = blockEntity.getZOff(partialTick);
        renderState.block = null;
        renderState.base = null;
        BlockState blockstate = blockEntity.getMovedState();
        Level level = blockEntity.getLevel();
        if (level != null && !blockstate.isAir()) {
            BlockPos blockpos = blockEntity.getBlockPos().relative(blockEntity.getMovementDirection().getOpposite());
            Holder<Biome> holder = level.getBiome(blockpos);
            if (blockstate.is(Blocks.PISTON_HEAD) && blockEntity.getProgress(partialTick) <= 4.0F) {
                blockstate = blockstate.setValue(PistonHeadBlock.SHORT, blockEntity.getProgress(partialTick) <= 0.5F);
                renderState.block = createMovingBlock(blockpos, blockstate, holder, level);
            } else if (blockEntity.isSourcePiston() && !blockEntity.isExtending()) {
                PistonType pistontype = blockstate.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT;
                BlockState blockstate1 = Blocks.PISTON_HEAD
                    .defaultBlockState()
                    .setValue(PistonHeadBlock.TYPE, pistontype)
                    .setValue(PistonHeadBlock.FACING, blockstate.getValue(PistonBaseBlock.FACING));
                blockstate1 = blockstate1.setValue(PistonHeadBlock.SHORT, blockEntity.getProgress(partialTick) >= 0.5F);
                renderState.block = createMovingBlock(blockpos, blockstate1, holder, level);
                BlockPos blockpos1 = blockpos.relative(blockEntity.getMovementDirection());
                blockstate = blockstate.setValue(PistonBaseBlock.EXTENDED, true);
                renderState.base = createMovingBlock(blockpos1, blockstate, holder, level);
            } else {
                renderState.block = createMovingBlock(blockpos, blockstate, holder, level);
            }
        }
    }

    public void submit(PistonHeadRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.block != null) {
            poseStack.pushPose();
            poseStack.translate(renderState.xOffset, renderState.yOffset, renderState.zOffset);
            nodeCollector.submitMovingBlock(poseStack, renderState.block);
            poseStack.popPose();
            if (renderState.base != null) {
                nodeCollector.submitMovingBlock(poseStack, renderState.base);
            }
        }
    }

    private static MovingBlockRenderState createMovingBlock(BlockPos pos, BlockState state, Holder<Biome> biome, Level level) {
        MovingBlockRenderState movingblockrenderstate = new MovingBlockRenderState();
        movingblockrenderstate.randomSeedPos = pos;
        movingblockrenderstate.blockPos = pos;
        movingblockrenderstate.blockState = state;
        movingblockrenderstate.biome = biome;
        movingblockrenderstate.level = level;
        return movingblockrenderstate;
    }

    @Override
    public int getViewDistance() {
        return 68;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(PistonMovingBlockEntity blockEntity) {
        return net.minecraft.world.phys.AABB.INFINITE;
    }
}
