package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SectionCompiler {
    private final BlockRenderDispatcher blockRenderer;
    private final BlockEntityRenderDispatcher blockEntityRenderer;

    public SectionCompiler(BlockRenderDispatcher blockRenderer, BlockEntityRenderDispatcher blockEntityRenderer) {
        this.blockRenderer = blockRenderer;
        this.blockEntityRenderer = blockEntityRenderer;
    }

    public SectionCompiler.Results compile(SectionPos sectionPos, RenderSectionRegion region, VertexSorting vertexSorting, SectionBufferBuilderPack sectionBufferBuilderPack) {
        return compile(sectionPos, region, vertexSorting, sectionBufferBuilderPack, List.of());
    }

    public SectionCompiler.Results compile(SectionPos sectionPos, RenderSectionRegion region, VertexSorting vertexSorting, SectionBufferBuilderPack sectionBufferBuilderPack, List<net.neoforged.neoforge.client.event.AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers) {
        SectionCompiler.Results sectioncompiler$results = new SectionCompiler.Results();
        BlockPos blockpos = sectionPos.origin();
        BlockPos blockpos1 = blockpos.offset(15, 15, 15);
        VisGraph visgraph = new VisGraph();
        PoseStack posestack = new PoseStack();
        ModelBlockRenderer.enableCaching();
        Map<ChunkSectionLayer, BufferBuilder> map = new EnumMap<>(ChunkSectionLayer.class);
        // Neo: use a SingleThreadedRandomSource to avoid overhead of atomics
        RandomSource randomsource = new net.minecraft.world.level.levelgen.SingleThreadedRandomSource(net.minecraft.world.level.levelgen.RandomSupport.generateUniqueSeed());
        List<BlockModelPart> list = new ObjectArrayList<>();
        java.util.function.Function<ChunkSectionLayer, com.mojang.blaze3d.vertex.VertexConsumer> bufferLookup = renderType -> this.getOrBeginLayer(map, sectionBufferBuilderPack, renderType);

        for (BlockPos blockpos2 : BlockPos.betweenClosed(blockpos, blockpos1)) {
            BlockState blockstate = region.getBlockState(blockpos2);
            if (blockstate.isSolidRender()) {
                visgraph.setOpaque(blockpos2);
            }

            if (blockstate.hasBlockEntity()) {
                BlockEntity blockentity = region.getBlockEntity(blockpos2);
                if (blockentity != null) {
                    this.handleBlockEntity(sectioncompiler$results, blockentity);
                }
            }

            FluidState fluidstate = blockstate.getFluidState();
            if (!fluidstate.isEmpty()) {
                ChunkSectionLayer chunksectionlayer = ItemBlockRenderTypes.getRenderLayer(fluidstate);
                BufferBuilder bufferbuilder = this.getOrBeginLayer(map, sectionBufferBuilderPack, chunksectionlayer);
                this.blockRenderer.renderLiquid(blockpos2, region, bufferbuilder, blockstate, fluidstate);
            }

            if (blockstate.getRenderShape() == RenderShape.MODEL) {
                randomsource.setSeed(blockstate.getSeed(blockpos2));
                this.blockRenderer.getBlockModel(blockstate).collectParts(region, blockpos2, blockstate, randomsource, list);
                posestack.pushPose();
                posestack.translate(
                    (float)SectionPos.sectionRelative(blockpos2.getX()),
                    (float)SectionPos.sectionRelative(blockpos2.getY()),
                    (float)SectionPos.sectionRelative(blockpos2.getZ())
                );
                this.blockRenderer.renderBatched(blockstate, blockpos2, region, posestack, bufferLookup, true, list);
                posestack.popPose();
                list.clear();
            }
        }
        net.neoforged.neoforge.client.ClientHooks.addAdditionalGeometry(additionalRenderers, bufferLookup, region, posestack);
        for (Entry<ChunkSectionLayer, BufferBuilder> entry : map.entrySet()) {
            ChunkSectionLayer chunksectionlayer1 = entry.getKey();
            MeshData meshdata = entry.getValue().build();
            if (meshdata != null) {
                if (chunksectionlayer1 == ChunkSectionLayer.TRANSLUCENT) {
                    sectioncompiler$results.transparencyState = meshdata.sortQuads(sectionBufferBuilderPack.buffer(chunksectionlayer1), vertexSorting);
                }

                sectioncompiler$results.renderedLayers.put(chunksectionlayer1, meshdata);
            }
        }

        ModelBlockRenderer.clearCache();
        sectioncompiler$results.visibilitySet = visgraph.resolve();
        return sectioncompiler$results;
    }

    private BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> builders, SectionBufferBuilderPack sectionBufferBuilderPack, ChunkSectionLayer layer) {
        BufferBuilder bufferbuilder = builders.get(layer);
        if (bufferbuilder == null) {
            ByteBufferBuilder bytebufferbuilder = sectionBufferBuilderPack.buffer(layer);
            bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            builders.put(layer, bufferbuilder);
        }

        return bufferbuilder;
    }

    private <E extends BlockEntity> void handleBlockEntity(SectionCompiler.Results results, E blockEntity) {
        BlockEntityRenderer<E, ?> blockentityrenderer = this.blockEntityRenderer.getRenderer(blockEntity);
        if (blockentityrenderer != null && !blockentityrenderer.shouldRenderOffScreen()) {
            results.blockEntities.add(blockEntity);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class Results {
        public final List<BlockEntity> blockEntities = new ArrayList<>();
        public final Map<ChunkSectionLayer, MeshData> renderedLayers = new EnumMap<>(ChunkSectionLayer.class);
        public VisibilitySet visibilitySet = new VisibilitySet();
        @Nullable
        public MeshData.SortState transparencyState;

        public void release() {
            this.renderedLayers.values().forEach(MeshData::close);
        }
    }
}
