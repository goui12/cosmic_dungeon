package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpawnerRenderer implements BlockEntityRenderer<SpawnerBlockEntity, SpawnerRenderState> {
    private final EntityRenderDispatcher entityRenderer;

    public SpawnerRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    public SpawnerRenderState createRenderState() {
        return new SpawnerRenderState();
    }

    public void extractRenderState(
        SpawnerBlockEntity blockEntity, SpawnerRenderState renderState, float partialTick, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        if (blockEntity.getLevel() != null) {
            BaseSpawner basespawner = blockEntity.getSpawner();
            Entity entity = basespawner.getOrCreateDisplayEntity(blockEntity.getLevel(), blockEntity.getBlockPos());
            TrialSpawnerRenderer.extractSpawnerData(renderState, partialTick, entity, this.entityRenderer, basespawner.getOSpin(), basespawner.getSpin());
        }
    }

    public void submit(SpawnerRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.displayEntity != null) {
            submitEntityInSpawner(poseStack, nodeCollector, renderState.displayEntity, this.entityRenderer, renderState.spin, renderState.scale, cameraRenderState);
        }
    }

    public static void submitEntityInSpawner(
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        EntityRenderState displayEntity,
        EntityRenderDispatcher entityRenderer,
        float spin,
        float scale,
        CameraRenderState cameraRenderState
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.4F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.translate(0.0F, -0.2F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
        poseStack.scale(scale, scale, scale);
        entityRenderer.submit(displayEntity, cameraRenderState, 0.0, 0.0, 0.0, poseStack, nodeCollector);
        poseStack.popPose();
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(SpawnerBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0, pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
    }
}
