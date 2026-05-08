package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SpecialBlockModelRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockRenderDispatcher implements ResourceManagerReloadListener {
    private final BlockModelShaper blockModelShaper;
    private final MaterialSet materials;
    private final ModelBlockRenderer modelRenderer;
    private final Supplier<SpecialBlockModelRenderer> specialBlockModelRenderer;
    private final LiquidBlockRenderer liquidBlockRenderer;
    private final RandomSource singleThreadRandom = RandomSource.create();
    private final List<BlockModelPart> singleThreadPartList = new ArrayList<>();
    private final BlockColors blockColors;

    public BlockRenderDispatcher(BlockModelShaper blockModelShaper, MaterialSet materials, Supplier<SpecialBlockModelRenderer> specialBlockModelRenderer, BlockColors blockColors) {
        this.blockModelShaper = blockModelShaper;
        this.materials = materials;
        this.specialBlockModelRenderer = specialBlockModelRenderer;
        this.blockColors = blockColors;
        this.modelRenderer = new ModelBlockRenderer(this.blockColors);
        this.liquidBlockRenderer = new LiquidBlockRenderer();
    }

    public BlockModelShaper getBlockModelShaper() {
        return this.blockModelShaper;
    }

    public void renderBreakingTexture(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer consumer) {
        if (state.getRenderShape() == RenderShape.MODEL) {
            BlockStateModel blockstatemodel = this.blockModelShaper.getBlockModel(state);
            this.singleThreadRandom.setSeed(state.getSeed(pos));
            this.singleThreadPartList.clear();
            blockstatemodel.collectParts(level, pos, state, this.singleThreadRandom, this.singleThreadPartList);
            this.modelRenderer
                .tesselateBlock(level, this.singleThreadPartList, state, pos, poseStack, type -> consumer, true, OverlayTexture.NO_OVERLAY);
        }
    }

    @Deprecated // Neo: Buffer lookup parameter
    public void renderBatched(
        BlockState state,
        BlockPos pos,
        BlockAndTintGetter level,
        PoseStack poseStack,
        VertexConsumer consumer,
        boolean checkSides,
        List<BlockModelPart> parts
    ) {
        this.renderBatched(state, pos, level, poseStack, type -> consumer, checkSides, parts);
    }

    public void renderBatched(
        BlockState p_234356_,
        BlockPos p_234357_,
        BlockAndTintGetter p_234358_,
        PoseStack p_234359_,
        java.util.function.Function<net.minecraft.client.renderer.chunk.ChunkSectionLayer, VertexConsumer> bufferLookup,
        boolean p_234361_,
        List<BlockModelPart> p_410643_
    ) {
        try {
            this.modelRenderer.tesselateBlock(p_234358_, p_410643_, p_234356_, p_234357_, p_234359_, bufferLookup, p_234361_, OverlayTexture.NO_OVERLAY);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, p_234358_, p_234357_, p_234356_);
            throw new ReportedException(crashreport);
        }
    }

    public void renderLiquid(BlockPos pos, BlockAndTintGetter level, VertexConsumer consumer, BlockState blockState, FluidState fluidState) {
        try {
            if (net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluidState).renderFluid(fluidState, level, pos, consumer, blockState)) return;
            this.liquidBlockRenderer.tesselate(level, pos, consumer, blockState, fluidState);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating liquid in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, level, pos, blockState);
            throw new ReportedException(crashreport);
        }
    }

    public ModelBlockRenderer getModelRenderer() {
        return this.modelRenderer;
    }

    public BlockStateModel getBlockModel(BlockState state) {
        return this.blockModelShaper.getBlockModel(state);
    }

    @Deprecated // Neo: Level and pos parameters
    public void renderSingleBlock(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderSingleBlock(state, poseStack, bufferSource, packedLight, packedOverlay, net.minecraft.world.level.EmptyBlockAndTintGetter.INSTANCE, BlockPos.ZERO);
    }

    public void renderSingleBlock(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BlockAndTintGetter level, BlockPos pos) {
        RenderShape rendershape = state.getRenderShape();
        if (rendershape != RenderShape.INVISIBLE) {
            BlockStateModel blockstatemodel = this.getBlockModel(state);
            int i = this.blockColors.getColor(state, null, null, 0);
            float f = (i >> 16 & 0xFF) / 255.0F;
            float f1 = (i >> 8 & 0xFF) / 255.0F;
            float f2 = (i & 0xFF) / 255.0F;
            ModelBlockRenderer.renderModel(
                    poseStack.last(), bufferSource, blockstatemodel, f, f1, f2, packedLight, packedOverlay, level, pos, state
            );
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.liquidBlockRenderer.setupSprites(this.blockModelShaper, this.materials);
    }

    public LiquidBlockRenderer getLiquidBlockRenderer() {
        return this.liquidBlockRenderer;
    }
}
