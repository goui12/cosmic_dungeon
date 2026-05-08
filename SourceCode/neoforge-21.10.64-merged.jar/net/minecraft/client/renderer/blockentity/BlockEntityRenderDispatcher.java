package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockEntityRenderDispatcher implements ResourceManagerReloadListener {
    private Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> renderers = ImmutableMap.of();
    private final Font font;
    private final Supplier<EntityModelSet> entityModelSet;
    private Vec3 cameraPos;
    private final BlockRenderDispatcher blockRenderDispatcher;
    private final ItemModelResolver itemModelResolver;
    private final ItemRenderer itemRenderer;
    private final EntityRenderDispatcher entityRenderer;
    private final MaterialSet materials;
    private final PlayerSkinRenderCache playerSkinRenderCache;

    public BlockEntityRenderDispatcher(
        Font font,
        Supplier<EntityModelSet> entityModelSet,
        BlockRenderDispatcher blockRenderDispatcher,
        ItemModelResolver itemModelResolver,
        ItemRenderer itemRenderer,
        EntityRenderDispatcher entityRenderer,
        MaterialSet materials,
        PlayerSkinRenderCache playerSkinRenderCache
    ) {
        this.itemRenderer = itemRenderer;
        this.itemModelResolver = itemModelResolver;
        this.entityRenderer = entityRenderer;
        this.font = font;
        this.entityModelSet = entityModelSet;
        this.blockRenderDispatcher = blockRenderDispatcher;
        this.materials = materials;
        this.playerSkinRenderCache = playerSkinRenderCache;
    }

    @Nullable
    public <E extends BlockEntity, S extends BlockEntityRenderState> BlockEntityRenderer<E, S> getRenderer(E blockEntity) {
        return (BlockEntityRenderer<E, S>)this.renderers.get(blockEntity.getType());
    }

    @Nullable
    public <E extends BlockEntity, S extends BlockEntityRenderState> BlockEntityRenderer<E, S> getRenderer(S renderState) {
        return (BlockEntityRenderer<E, S>)this.renderers.get(renderState.blockEntityType);
    }

    public void prepare(Camera camera) {
        this.cameraPos = camera.getPosition();
    }

    /**
     * @deprecated Neo: use {@link #tryExtractRenderState(BlockEntity, float,
     *             ModelFeatureRenderer.CrumblingOverlay,
     *             net.minecraft.client.renderer.culling.Frustum)} instead
     */
    @Nullable
    public <E extends BlockEntity, S extends BlockEntityRenderState> S tryExtractRenderState(
        E blockEntity, float partialTick, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        return tryExtractRenderState(blockEntity, partialTick, breakProgress, null);
    }

    @Nullable
    public <E extends BlockEntity, S extends BlockEntityRenderState> S tryExtractRenderState(
            E blockEntity, float partialTick, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress, @Nullable net.minecraft.client.renderer.culling.Frustum frustum
    ) {
        BlockEntityRenderer<E, S> blockentityrenderer = this.getRenderer(blockEntity);
        if (blockentityrenderer == null) {
            return null;
        } else if (!blockEntity.hasLevel() || !blockEntity.getType().isValid(blockEntity.getBlockState())) {
            return null;
        } else if (frustum != null && !frustum.isVisible(blockentityrenderer.getRenderBoundingBox(blockEntity))) {
            return null;
        } else if (!blockentityrenderer.shouldRender(blockEntity, this.cameraPos)) {
            return null;
        } else {
            Vec3 vec3 = this.cameraPos;
            S s = blockentityrenderer.createRenderState();
            blockentityrenderer.extractRenderState(blockEntity, s, partialTick, vec3, breakProgress);
            return s;
        }
    }

    public <S extends BlockEntityRenderState> void submit(S renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        BlockEntityRenderer<?, S> blockentityrenderer = this.getRenderer(renderState);
        if (blockentityrenderer != null) {
            try {
                blockentityrenderer.submit(renderState, poseStack, nodeCollector, cameraRenderState);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering Block Entity");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Block Entity Details");
                renderState.fillCrashReportCategory(crashreportcategory);
                throw new ReportedException(crashreport);
            }
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        BlockEntityRendererProvider.Context blockentityrendererprovider$context = new BlockEntityRendererProvider.Context(
            this,
            this.blockRenderDispatcher,
            this.itemModelResolver,
            this.itemRenderer,
            this.entityRenderer,
            this.entityModelSet.get(),
            this.font,
            this.materials,
            this.playerSkinRenderCache
        );
        this.renderers = BlockEntityRenderers.createEntityRenderers(blockentityrendererprovider$context);
    }
}
