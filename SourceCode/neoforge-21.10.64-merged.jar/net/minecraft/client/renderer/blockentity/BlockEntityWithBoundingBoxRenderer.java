package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BoundingBoxRenderable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class BlockEntityWithBoundingBoxRenderer<T extends BlockEntity & BoundingBoxRenderable>
    implements BlockEntityRenderer<T, BlockEntityWithBoundingBoxRenderState> {
    public BlockEntityWithBoundingBoxRenderState createRenderState() {
        return new BlockEntityWithBoundingBoxRenderState();
    }

    public void extractRenderState(
        T blockEntity,
        BlockEntityWithBoundingBoxRenderState renderState,
        float partialTick,
        Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        extract(blockEntity, renderState);
    }

    public static <T extends BlockEntity & BoundingBoxRenderable> void extract(T blockEntity, BlockEntityWithBoundingBoxRenderState renderState) {
        LocalPlayer localplayer = Minecraft.getInstance().player;
        renderState.isVisible = localplayer.canUseGameMasterBlocks() || localplayer.isSpectator();
        renderState.box = blockEntity.getRenderableBox();
        renderState.mode = blockEntity.renderMode();
        BlockPos blockpos = renderState.box.localPos();
        Vec3i vec3i = renderState.box.size();
        BlockPos blockpos1 = renderState.blockPos;
        BlockPos blockpos2 = blockpos1.offset(blockpos);
        if (renderState.isVisible && blockEntity.getLevel() != null && renderState.mode == BoundingBoxRenderable.Mode.BOX_AND_INVISIBLE_BLOCKS) {
            renderState.invisibleBlocks = new BlockEntityWithBoundingBoxRenderState.InvisibleBlockType[vec3i.getX() * vec3i.getY() * vec3i.getZ()];

            for (int i = 0; i < vec3i.getX(); i++) {
                for (int j = 0; j < vec3i.getY(); j++) {
                    for (int k = 0; k < vec3i.getZ(); k++) {
                        int l = k * vec3i.getX() * vec3i.getY() + j * vec3i.getX() + i;
                        BlockState blockstate = blockEntity.getLevel().getBlockState(blockpos2.offset(i, j, k));
                        if (blockstate.isAir()) {
                            renderState.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR;
                        } else if (blockstate.is(Blocks.STRUCTURE_VOID)) {
                            renderState.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCUTRE_VOID;
                        } else if (blockstate.is(Blocks.BARRIER)) {
                            renderState.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER;
                        } else if (blockstate.is(Blocks.LIGHT)) {
                            renderState.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT;
                        }
                    }
                }
            }
        } else {
            renderState.invisibleBlocks = null;
        }

        if (renderState.isVisible) {
        }

        renderState.structureVoids = null;
    }

    public void submit(BlockEntityWithBoundingBoxRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.isVisible) {
            BoundingBoxRenderable.Mode boundingboxrenderable$mode = renderState.mode;
            if (boundingboxrenderable$mode != BoundingBoxRenderable.Mode.NONE) {
                BoundingBoxRenderable.RenderableBox boundingboxrenderable$renderablebox = renderState.box;
                BlockPos blockpos = boundingboxrenderable$renderablebox.localPos();
                Vec3i vec3i = boundingboxrenderable$renderablebox.size();
                if (vec3i.getX() >= 1 && vec3i.getY() >= 1 && vec3i.getZ() >= 1) {
                    float f = 1.0F;
                    float f1 = 0.9F;
                    float f2 = 0.5F;
                    BlockPos blockpos1 = blockpos.offset(vec3i);
                    nodeCollector.submitCustomGeometry(
                        poseStack,
                        RenderType.lines(),
                        (p_438793_, p_438794_) -> ShapeRenderer.renderLineBox(
                            p_438793_,
                            p_438794_,
                            blockpos.getX(),
                            blockpos.getY(),
                            blockpos.getZ(),
                            blockpos1.getX(),
                            blockpos1.getY(),
                            blockpos1.getZ(),
                            0.9F,
                            0.9F,
                            0.9F,
                            1.0F,
                            0.5F,
                            0.5F,
                            0.5F
                        )
                    );
                    this.submitInvisibleBlocks(renderState, blockpos, vec3i, nodeCollector, poseStack);
                }
            }
        }
    }

    private void submitInvisibleBlocks(
        BlockEntityWithBoundingBoxRenderState renderState, BlockPos localPos, Vec3i size, SubmitNodeCollector nodeCollector, PoseStack poseStack
    ) {
        if (renderState.invisibleBlocks != null) {
            BlockPos blockpos = renderState.blockPos;
            BlockPos blockpos1 = blockpos.offset(localPos);
            nodeCollector.submitCustomGeometry(
                poseStack,
                RenderType.lines(),
                (p_445231_, p_445232_) -> {
                    for (int i = 0; i < size.getX(); i++) {
                        for (int j = 0; j < size.getY(); j++) {
                            for (int k = 0; k < size.getZ(); k++) {
                                int l = k * size.getX() * size.getY() + j * size.getX() + i;
                                BlockEntityWithBoundingBoxRenderState.InvisibleBlockType blockentitywithboundingboxrenderstate$invisibleblocktype = renderState.invisibleBlocks[l];
                                if (blockentitywithboundingboxrenderstate$invisibleblocktype != null) {
                                    float f = blockentitywithboundingboxrenderstate$invisibleblocktype
                                            == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR
                                        ? 0.05F
                                        : 0.0F;
                                    double d0 = blockpos1.getX() + i - blockpos.getX() + 0.45F - f;
                                    double d1 = blockpos1.getY() + j - blockpos.getY() + 0.45F - f;
                                    double d2 = blockpos1.getZ() + k - blockpos.getZ() + 0.45F - f;
                                    double d3 = blockpos1.getX() + i - blockpos.getX() + 0.55F + f;
                                    double d4 = blockpos1.getY() + j - blockpos.getY() + 0.55F + f;
                                    double d5 = blockpos1.getZ() + k - blockpos.getZ() + 0.55F + f;
                                    if (blockentitywithboundingboxrenderstate$invisibleblocktype
                                        == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR) {
                                        ShapeRenderer.renderLineBox(p_445231_, p_445232_, d0, d1, d2, d3, d4, d5, 0.5F, 0.5F, 1.0F, 1.0F, 0.5F, 0.5F, 1.0F);
                                    } else if (blockentitywithboundingboxrenderstate$invisibleblocktype
                                        == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCUTRE_VOID) {
                                        ShapeRenderer.renderLineBox(p_445231_, p_445232_, d0, d1, d2, d3, d4, d5, 1.0F, 0.75F, 0.75F, 1.0F, 1.0F, 0.75F, 0.75F);
                                    } else if (blockentitywithboundingboxrenderstate$invisibleblocktype
                                        == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER) {
                                        ShapeRenderer.renderLineBox(p_445231_, p_445232_, d0, d1, d2, d3, d4, d5, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F);
                                    } else if (blockentitywithboundingboxrenderstate$invisibleblocktype
                                        == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT) {
                                        ShapeRenderer.renderLineBox(p_445231_, p_445232_, d0, d1, d2, d3, d4, d5, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F);
                                    }
                                }
                            }
                        }
                    }
                }
            );
        }
    }

    private void renderStructureVoids(
        BlockEntityWithBoundingBoxRenderState renderState, BlockPos localPos, Vec3i size, VertexConsumer consumer, Matrix4f pose
    ) {
        if (renderState.structureVoids != null) {
            BlockPos blockpos = renderState.blockPos;
            DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(size.getX(), size.getY(), size.getZ());

            for (int i = 0; i < size.getX(); i++) {
                for (int j = 0; j < size.getY(); j++) {
                    for (int k = 0; k < size.getZ(); k++) {
                        int l = k * size.getX() * size.getY() + j * size.getX() + i;
                        if (renderState.structureVoids[l]) {
                            discretevoxelshape.fill(i, j, k);
                        }
                    }
                }
            }

            discretevoxelshape.forAllFaces((p_438799_, p_438800_, p_438801_, p_438802_) -> {
                float f = 0.48F;
                float f1 = p_438800_ + localPos.getX() - blockpos.getX() + 0.5F - 0.48F;
                float f2 = p_438801_ + localPos.getY() - blockpos.getY() + 0.5F - 0.48F;
                float f3 = p_438802_ + localPos.getZ() - blockpos.getZ() + 0.5F - 0.48F;
                float f4 = p_438800_ + localPos.getX() - blockpos.getX() + 0.5F + 0.48F;
                float f5 = p_438801_ + localPos.getY() - blockpos.getY() + 0.5F + 0.48F;
                float f6 = p_438802_ + localPos.getZ() - blockpos.getZ() + 0.5F + 0.48F;
                ShapeRenderer.renderFace(pose, consumer, p_438799_, f1, f2, f3, f4, f5, f6, 0.75F, 0.75F, 1.0F, 0.2F);
            });
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(BlockEntity blockEntity) {
        return net.minecraft.world.phys.AABB.INFINITE;
    }
}
