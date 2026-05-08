package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TrialSpawnerRenderer implements BlockEntityRenderer<TrialSpawnerBlockEntity, SpawnerRenderState> {
    private final EntityRenderDispatcher entityRenderer;

    public TrialSpawnerRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    public SpawnerRenderState createRenderState() {
        return new SpawnerRenderState();
    }

    public void extractRenderState(
        TrialSpawnerBlockEntity blockEntity,
        SpawnerRenderState renderState,
        float partialTick,
        Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        if (blockEntity.getLevel() != null) {
            TrialSpawner trialspawner = blockEntity.getTrialSpawner();
            TrialSpawnerStateData trialspawnerstatedata = trialspawner.getStateData();
            Entity entity = trialspawnerstatedata.getOrCreateDisplayEntity(trialspawner, blockEntity.getLevel(), trialspawner.getState());
            extractSpawnerData(renderState, partialTick, entity, this.entityRenderer, trialspawnerstatedata.getOSpin(), trialspawnerstatedata.getSpin());
        }
    }

    static void extractSpawnerData(
        SpawnerRenderState renderState, float partialTick, @Nullable Entity entity, EntityRenderDispatcher entityRenderer, double oSpin, double spin
    ) {
        if (entity != null) {
            renderState.displayEntity = entityRenderer.extractEntity(entity, partialTick);
            renderState.displayEntity.lightCoords = renderState.lightCoords;
            renderState.spin = (float)Mth.lerp((double)partialTick, oSpin, spin) * 10.0F;
            renderState.scale = 0.53125F;
            float f = Math.max(entity.getBbWidth(), entity.getBbHeight());
            if (f > 1.0) {
                renderState.scale /= f;
            }
        }
    }

    public void submit(SpawnerRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.displayEntity != null) {
            SpawnerRenderer.submitEntityInSpawner(
                poseStack, nodeCollector, renderState.displayEntity, this.entityRenderer, renderState.spin, renderState.scale, cameraRenderState
            );
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(TrialSpawnerBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0, pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
    }
}
