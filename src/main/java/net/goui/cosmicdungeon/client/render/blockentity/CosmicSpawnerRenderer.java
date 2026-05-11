// file: src/main/java/net/goui/cosmicdungeon/client/render/blockentity/CosmicSpawnerRenderer.java
package net.goui.cosmicdungeon.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.goui.cosmicdungeon.client.SpawnerLabelState;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CosmicSpawnerRenderer implements BlockEntityRenderer<CosmicSpawnerBlockEntity, CosmicSpawnerRenderer.State> {

    private static final double MAX_RENDER_DIST = 48.0;
    private static final double LABEL_HEIGHT = 2.0;
    private static final int FULLBRIGHT = LightTexture.pack(15, 15);

    public static class State extends BlockEntityRenderState {
        public @Nullable BlockState resolvedState;
        public @Nullable CosmicSpawnerBlockEntity be;
        public float partialTick;

        // Name tag
        public boolean renderName = false;
        public Component nameText = Component.empty();

        // submitNameTag inputs
        public Vec3 nameAnchor = Vec3.ZERO;       // camera-relative anchor (world - camera)
        public double distanceToCameraSq = 0.0;   // MUST be squared distance (vanilla behavior)
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

        // Default: don't render the nameplate unless explicitly enabled by the client toggle.
        // (Keeps "default hide" behavior, and allows /spawner label show|hide to control it.)
        if (!SpawnerLabelState.isEnabled()) {
            state.renderName = false;
            return;
        }

        String id = be.getSpawnerDisplayEntityId();
        if (id == null) id = "";
        id = id.trim();

        if (id.isEmpty() || id.equalsIgnoreCase("none")) {
            state.renderName = false;
            return;
        }

        state.nameText = Component.literal(id)
                .withStyle(style -> style.withColor(0x00FFD5)); // neon teal

        // World position: center of block + 2.0 up
        Vec3 labelWorld = Vec3.atCenterOf(be.getBlockPos()).add(0.0, LABEL_HEIGHT, 0.0);

        // submitNameTag wants camera-relative anchor
        state.nameAnchor = labelWorld.subtract(cameraPosition);

        // Vanilla scaling uses squared distance
        state.distanceToCameraSq = state.nameAnchor.lengthSqr();

        // Cutoff at 48 blocks
        double maxSq = MAX_RENDER_DIST * MAX_RENDER_DIST;
        state.renderName = state.distanceToCameraSq <= maxSq;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // 1) Render the block model
        if (state.resolvedState != null) {
            collector.submitBlock(
                    poseStack,
                    state.resolvedState,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );
        }

        CosmicSpawnerBlockEntity be = state.be;
        if (be == null) return;

        // 2) Render preview entity
        Level level = be.getLevel();
        if (level != null) {
            Entity display = be.getSpawner().getOrCreateDisplayEntity(level, be.getBlockPos());
            if (display != null) {
                if (be.getSpawnerPreset() != null) {
                    be.getSpawnerPreset().applyToEntity(display);
                }
                EntityRenderState ers = this.dispatcher.extractEntity(display, state.partialTick);

                // Force fullbright for the preview entity (ignores world light)
                ers.lightCoords = LightTexture.pack(15, 15);

                poseStack.pushPose();
                poseStack.translate(0.5D, 0.1D, 0.5D);

                float maxDim = Math.max(display.getBbWidth(), display.getBbHeight());
                float scale = (maxDim > 1.0F) ? (0.8F / maxDim) : 0.5F;
                poseStack.scale(scale, scale, scale);

                float yawDeg = be.getRenderSpinDegrees(state.partialTick);
                poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));

                this.dispatcher.submit(ers, cameraState, 0.0D, 0.0D, 0.0D, poseStack, collector);
                poseStack.popPose();
            }
        }

        // 3) Static nameplate (vanilla behavior)
        if (state.renderName) {
            collector.submitNameTag(
                    poseStack,                 // PoseStack
                    state.nameAnchor,          // Vec3: (worldPos - cameraPos)
                    0,                         // int: reserved/unused (Mojang passes 0)
                    state.nameText,            // Component: text
                    true,                      // boolean: see-through-ish
                    FULLBRIGHT,                // int: packed light (fullbright)
                    state.distanceToCameraSq,  // double: squared distance (scales naturally)
                    cameraState                // CameraRenderState
            );
        }
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