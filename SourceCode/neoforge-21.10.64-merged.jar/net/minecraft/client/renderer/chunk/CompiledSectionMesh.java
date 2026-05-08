package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompiledSectionMesh implements SectionMesh {
    public static final SectionMesh UNCOMPILED = new SectionMesh() {
        @Override
        public boolean facesCanSeeEachother(Direction p_427455_, Direction p_427422_) {
            return false;
        }
    };
    public static final SectionMesh EMPTY = new SectionMesh() {
        @Override
        public boolean facesCanSeeEachother(Direction p_427417_, Direction p_427302_) {
            return true;
        }
    };
    private final List<BlockEntity> renderableBlockEntities;
    private final VisibilitySet visibilitySet;
    @Nullable
    private final MeshData.SortState transparencyState;
    @Nullable
    private TranslucencyPointOfView translucencyPointOfView;
    private final Map<ChunkSectionLayer, SectionBuffers> buffers = new EnumMap<>(ChunkSectionLayer.class);

    public CompiledSectionMesh(TranslucencyPointOfView translucencyPointOfView, SectionCompiler.Results results) {
        this.translucencyPointOfView = translucencyPointOfView;
        this.visibilitySet = results.visibilitySet;
        this.renderableBlockEntities = results.blockEntities;
        this.transparencyState = results.transparencyState;
    }

    public void setTranslucencyPointOfView(TranslucencyPointOfView translucencyPointOfView) {
        this.translucencyPointOfView = translucencyPointOfView;
    }

    @Override
    public boolean isDifferentPointOfView(TranslucencyPointOfView pointOfView) {
        return !pointOfView.equals(this.translucencyPointOfView);
    }

    @Override
    public boolean hasRenderableLayers() {
        return !this.buffers.isEmpty();
    }

    @Override
    public boolean isEmpty(ChunkSectionLayer layer) {
        return !this.buffers.containsKey(layer);
    }

    @Override
    public List<BlockEntity> getRenderableBlockEntities() {
        return this.renderableBlockEntities;
    }

    @Override
    public boolean facesCanSeeEachother(Direction face1, Direction face2) {
        return this.visibilitySet.visibilityBetween(face1, face2);
    }

    @Nullable
    @Override
    public SectionBuffers getBuffers(ChunkSectionLayer layer) {
        return this.buffers.get(layer);
    }

    public void uploadMeshLayer(ChunkSectionLayer layer, MeshData meshData, long sectionNode) {
        CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
        SectionBuffers sectionbuffers = this.getBuffers(layer);
        if (sectionbuffers != null) {
            if (sectionbuffers.getVertexBuffer().size() < meshData.vertexBuffer().remaining()) {
                sectionbuffers.getVertexBuffer().close();
                sectionbuffers.setVertexBuffer(
                    RenderSystem.getDevice()
                        .createBuffer(
                            () -> "Section vertex buffer - layer: "
                                + layer.label()
                                + "; cords: "
                                + SectionPos.x(sectionNode)
                                + ", "
                                + SectionPos.y(sectionNode)
                                + ", "
                                + SectionPos.z(sectionNode),
                            40,
                            meshData.vertexBuffer()
                        )
                );
            } else if (!sectionbuffers.getVertexBuffer().isClosed()) {
                commandencoder.writeToBuffer(sectionbuffers.getVertexBuffer().slice(), meshData.vertexBuffer());
            }

            ByteBuffer bytebuffer = meshData.indexBuffer();
            if (bytebuffer != null) {
                if (sectionbuffers.getIndexBuffer() != null && sectionbuffers.getIndexBuffer().size() >= bytebuffer.remaining()) {
                    if (!sectionbuffers.getIndexBuffer().isClosed()) {
                        commandencoder.writeToBuffer(sectionbuffers.getIndexBuffer().slice(), bytebuffer);
                    }
                } else {
                    if (sectionbuffers.getIndexBuffer() != null) {
                        sectionbuffers.getIndexBuffer().close();
                    }

                    sectionbuffers.setIndexBuffer(
                        RenderSystem.getDevice()
                            .createBuffer(
                                () -> "Section index buffer - layer: "
                                    + layer.label()
                                    + "; cords: "
                                    + SectionPos.x(sectionNode)
                                    + ", "
                                    + SectionPos.y(sectionNode)
                                    + ", "
                                    + SectionPos.z(sectionNode),
                                72,
                                bytebuffer
                            )
                    );
                }
            } else if (sectionbuffers.getIndexBuffer() != null) {
                sectionbuffers.getIndexBuffer().close();
                sectionbuffers.setIndexBuffer(null);
            }

            sectionbuffers.setIndexCount(meshData.drawState().indexCount());
            sectionbuffers.setIndexType(meshData.drawState().indexType());
        } else {
            GpuBuffer gpubuffer1 = RenderSystem.getDevice()
                .createBuffer(
                    () -> "Section vertex buffer - layer: "
                        + layer.label()
                        + "; cords: "
                        + SectionPos.x(sectionNode)
                        + ", "
                        + SectionPos.y(sectionNode)
                        + ", "
                        + SectionPos.z(sectionNode),
                    40,
                    meshData.vertexBuffer()
                );
            ByteBuffer bytebuffer1 = meshData.indexBuffer();
            GpuBuffer gpubuffer = bytebuffer1 != null
                ? RenderSystem.getDevice()
                    .createBuffer(
                        () -> "Section index buffer - layer: "
                            + layer.label()
                            + "; cords: "
                            + SectionPos.x(sectionNode)
                            + ", "
                            + SectionPos.y(sectionNode)
                            + ", "
                            + SectionPos.z(sectionNode),
                        72,
                        bytebuffer1
                    )
                : null;
            SectionBuffers sectionbuffers1 = new SectionBuffers(gpubuffer1, gpubuffer, meshData.drawState().indexCount(), meshData.drawState().indexType());
            this.buffers.put(layer, sectionbuffers1);
        }
    }

    public void uploadLayerIndexBuffer(ChunkSectionLayer layer, ByteBufferBuilder.Result result, long sectionNode) {
        SectionBuffers sectionbuffers = this.getBuffers(layer);
        if (sectionbuffers != null) {
            if (sectionbuffers.getIndexBuffer() == null) {
                sectionbuffers.setIndexBuffer(
                    RenderSystem.getDevice()
                        .createBuffer(
                            () -> "Section index buffer - layer: "
                                + layer.label()
                                + "; cords: "
                                + SectionPos.x(sectionNode)
                                + ", "
                                + SectionPos.y(sectionNode)
                                + ", "
                                + SectionPos.z(sectionNode),
                            72,
                            result.byteBuffer()
                        )
                );
            } else {
                CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
                if (!sectionbuffers.getIndexBuffer().isClosed()) {
                    commandencoder.writeToBuffer(sectionbuffers.getIndexBuffer().slice(), result.byteBuffer());
                }
            }
        }
    }

    @Override
    public boolean hasTranslucentGeometry() {
        return this.buffers.containsKey(ChunkSectionLayer.TRANSLUCENT);
    }

    @Nullable
    public MeshData.SortState getTransparencyState() {
        return this.transparencyState;
    }

    @Override
    public void close() {
        this.buffers.values().forEach(SectionBuffers::close);
        this.buffers.clear();
    }
}
