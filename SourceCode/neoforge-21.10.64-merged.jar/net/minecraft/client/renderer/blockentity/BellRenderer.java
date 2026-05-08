package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.model.BellModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BellRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BellRenderer implements BlockEntityRenderer<BellBlockEntity, BellRenderState> {
    public static final Material BELL_RESOURCE_LOCATION = Sheets.BLOCK_ENTITIES_MAPPER.defaultNamespaceApply("bell/bell_body");
    private final MaterialSet materials;
    private final BellModel model;

    public BellRenderer(BlockEntityRendererProvider.Context context) {
        this.materials = context.materials();
        this.model = new BellModel(context.bakeLayer(ModelLayers.BELL));
    }

    public BellRenderState createRenderState() {
        return new BellRenderState();
    }

    public void extractRenderState(
        BellBlockEntity blockEntity, BellRenderState renderState, float partialTick, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.ticks = blockEntity.ticks + partialTick;
        renderState.shakeDirection = blockEntity.shaking ? blockEntity.clickDirection : null;
    }

    public void submit(BellRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        BellModel.State bellmodel$state = new BellModel.State(renderState.ticks, renderState.shakeDirection);
        this.model.setupAnim(bellmodel$state);
        RenderType rendertype = BELL_RESOURCE_LOCATION.renderType(RenderType::entitySolid);
        nodeCollector.submitModel(
            this.model,
            bellmodel$state,
            poseStack,
            rendertype,
            renderState.lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,
            this.materials.get(BELL_RESOURCE_LOCATION),
            0,
            renderState.breakProgress
        );
    }
}
