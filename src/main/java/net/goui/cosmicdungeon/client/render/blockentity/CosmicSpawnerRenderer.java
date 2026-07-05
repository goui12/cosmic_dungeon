// file: src/main/java/net/goui/cosmicdungeon/client/render/blockentity/CosmicSpawnerRenderer.java
package net.goui.cosmicdungeon.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CosmicSpawnerRenderer implements BlockEntityRenderer<CosmicSpawnerBlockEntity, CosmicSpawnerRenderer.State> {

    public static class State extends BlockEntityRenderState {
        public @Nullable BlockState resolvedState;
        public @Nullable CosmicSpawnerBlockEntity be;
        public float partialTick;
    }

    private final EntityRenderDispatcher dispatcher;

    public CosmicSpawnerRenderer(BlockEntityRendererProvider.Context ctx) {
        this.dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            CosmicSpawnerBlockEntity be,
            State state,
            float partialTick,
            Vec3 cameraPosition,
            @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        state.resolvedState = be.getBlockState();
        state.be = be;
        state.partialTick = partialTick;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.resolvedState != null) {
            collector.submitBlock(poseStack, state.resolvedState, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }

        CosmicSpawnerBlockEntity be = state.be;
        if (be == null) return;

        Level level = be.getLevel();
        if (level == null) return;

        Entity display = be.getSpawner().getOrCreateDisplayEntity(level, be.getBlockPos());
        if (display == null) return;

        if (be.getSpawnerPreset() != null) {
            be.getSpawnerPreset().applyToEntity(display);
        }
        EntityRenderState ers = this.dispatcher.extractEntity(display, state.partialTick);
        ers.lightCoords = LightTexture.pack(15, 15);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.1D, 0.5D);

        float maxDim = Math.max(display.getBbWidth(), display.getBbHeight());
        float scale = (maxDim > 1.0F) ? (0.8F / maxDim) : 0.5F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(be.getRenderSpinDegrees(state.partialTick)));

        this.dispatcher.submit(ers, cameraState, 0.0D, 0.0D, 0.0D, poseStack, collector);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
