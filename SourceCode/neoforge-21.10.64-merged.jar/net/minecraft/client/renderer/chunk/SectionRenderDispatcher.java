package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.TracingExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SectionRenderDispatcher {
    private final CompileTaskDynamicQueue compileQueue = new CompileTaskDynamicQueue();
    private final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
    final Executor mainThreadUploadExecutor = this.toUpload::add;
    final Queue<SectionMesh> toClose = Queues.newConcurrentLinkedQueue();
    final SectionBufferBuilderPack fixedBuffers;
    private final SectionBufferBuilderPool bufferPool;
    volatile boolean closed;
    private final ConsecutiveExecutor consecutiveExecutor;
    private final TracingExecutor executor;
    ClientLevel level;
    final LevelRenderer renderer;
    Vec3 cameraPosition = Vec3.ZERO;
    final SectionCompiler sectionCompiler;

    public SectionRenderDispatcher(
        ClientLevel level,
        LevelRenderer renderer,
        TracingExecutor executor,
        RenderBuffers buffer,
        BlockRenderDispatcher blockRenderer,
        BlockEntityRenderDispatcher blockEntityRenderer
    ) {
        this.level = level;
        this.renderer = renderer;
        this.fixedBuffers = buffer.fixedBufferPack();
        this.bufferPool = buffer.sectionBufferPool();
        this.executor = executor;
        this.consecutiveExecutor = new ConsecutiveExecutor(executor, "Section Renderer");
        this.consecutiveExecutor.schedule(this::runTask);
        this.sectionCompiler = new SectionCompiler(blockRenderer, blockEntityRenderer);
    }

    public void setLevel(ClientLevel level) {
        this.level = level;
    }

    private void runTask() {
        if (!this.closed && !this.bufferPool.isEmpty()) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.compileQueue.poll(this.cameraPosition);
            if (sectionrenderdispatcher$rendersection$compiletask != null) {
                SectionBufferBuilderPack sectionbufferbuilderpack = Objects.requireNonNull(this.bufferPool.acquire());
                CompletableFuture.<CompletableFuture<SectionRenderDispatcher.SectionTaskResult>>supplyAsync(
                        () -> sectionrenderdispatcher$rendersection$compiletask.doTask(sectionbufferbuilderpack),
                        this.executor.forName(sectionrenderdispatcher$rendersection$compiletask.name())
                    )
                    .thenCompose(p_296185_ -> (CompletionStage<SectionRenderDispatcher.SectionTaskResult>)p_296185_)
                    .whenComplete((p_370310_, p_370311_) -> {
                        if (p_370311_ != null) {
                            Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_370311_, "Batching sections"));
                        } else {
                            sectionrenderdispatcher$rendersection$compiletask.isCompleted.set(true);
                            this.consecutiveExecutor.schedule(() -> {
                                if (p_370310_ == SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL) {
                                    sectionbufferbuilderpack.clearAll();
                                } else {
                                    sectionbufferbuilderpack.discardAll();
                                }

                                this.bufferPool.release(sectionbufferbuilderpack);
                                this.runTask();
                            });
                        }
                    });
            }
        }
    }

    public void setCameraPosition(Vec3 cameraPosition) {
        this.cameraPosition = cameraPosition;
    }

    public void uploadAllPendingUploads() {
        Runnable runnable;
        while ((runnable = this.toUpload.poll()) != null) {
            runnable.run();
        }

        SectionMesh sectionmesh;
        while ((sectionmesh = this.toClose.poll()) != null) {
            sectionmesh.close();
        }
    }

    public void rebuildSectionSync(SectionRenderDispatcher.RenderSection section, RenderRegionCache regionCache) {
        section.compileSync(regionCache);
    }

    public void schedule(SectionRenderDispatcher.RenderSection.CompileTask task) {
        if (!this.closed) {
            this.consecutiveExecutor.schedule(() -> {
                if (!this.closed) {
                    this.compileQueue.add(task);
                    this.runTask();
                }
            });
        }
    }

    public void clearCompileQueue() {
        this.compileQueue.clear();
    }

    public boolean isQueueEmpty() {
        return this.compileQueue.size() == 0 && this.toUpload.isEmpty();
    }

    public void dispose() {
        this.closed = true;
        this.clearCompileQueue();
        this.uploadAllPendingUploads();
    }

    @VisibleForDebug
    public String getStats() {
        return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.compileQueue.size(), this.toUpload.size(), this.bufferPool.getFreeBufferCount());
    }

    @VisibleForDebug
    public int getCompileQueueSize() {
        return this.compileQueue.size();
    }

    @VisibleForDebug
    public int getToUpload() {
        return this.toUpload.size();
    }

    @VisibleForDebug
    public int getFreeBufferCount() {
        return this.bufferPool.getFreeBufferCount();
    }

    @OnlyIn(Dist.CLIENT)
    public class RenderSection implements net.neoforged.neoforge.client.IRenderableSection {
        public static final int SIZE = 16;
        public final int index;
        public final AtomicReference<SectionMesh> sectionMesh = new AtomicReference<>(CompiledSectionMesh.UNCOMPILED);
        @Nullable
        private SectionRenderDispatcher.RenderSection.RebuildTask lastRebuildTask;
        @Nullable
        private SectionRenderDispatcher.RenderSection.ResortTransparencyTask lastResortTransparencyTask;
        private AABB bb;
        private boolean dirty = true;
        volatile long sectionNode = SectionPos.asLong(-1, -1, -1);
        final BlockPos.MutableBlockPos renderOrigin = new BlockPos.MutableBlockPos(-1, -1, -1);
        private boolean playerChanged;

        public RenderSection(int index, long sectionNode) {
            this.index = index;
            this.setSectionNode(sectionNode);
        }

        private boolean doesChunkExistAt(long pos) {
            ChunkAccess chunkaccess = SectionRenderDispatcher.this.level.getChunk(SectionPos.x(pos), SectionPos.z(pos), ChunkStatus.FULL, false);
            return chunkaccess != null && SectionRenderDispatcher.this.level.getLightEngine().lightOnInColumn(SectionPos.getZeroNode(pos));
        }

        public boolean hasAllNeighbors() {
            return this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.WEST))
                && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.NORTH))
                && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.EAST))
                && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.SOUTH))
                && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, -1, 0, -1))
                && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, -1, 0, 1))
                && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, 1, 0, -1))
                && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, 1, 0, 1));
        }

        public AABB getBoundingBox() {
            return this.bb;
        }

        public CompletableFuture<Void> upload(Map<ChunkSectionLayer, MeshData> renderedLayers, CompiledSectionMesh mesh) {
            if (SectionRenderDispatcher.this.closed) {
                renderedLayers.values().forEach(MeshData::close);
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.runAsync(() -> renderedLayers.forEach((p_426923_, p_426924_) -> {
                    try (Zone zone = Profiler.get().zone("Upload Section Layer")) {
                        mesh.uploadMeshLayer(p_426923_, p_426924_, this.sectionNode);
                        p_426924_.close();
                    }
                }), SectionRenderDispatcher.this.mainThreadUploadExecutor);
            }
        }

        public CompletableFuture<Void> uploadSectionIndexBuffer(CompiledSectionMesh mesh, ByteBufferBuilder.Result result, ChunkSectionLayer layer) {
            if (SectionRenderDispatcher.this.closed) {
                result.close();
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.runAsync(() -> {
                    try (Zone zone = Profiler.get().zone("Upload Section Indices")) {
                        mesh.uploadLayerIndexBuffer(layer, result, this.sectionNode);
                        result.close();
                    }
                }, SectionRenderDispatcher.this.mainThreadUploadExecutor);
            }
        }

        public void setSectionNode(long sectionNode) {
            this.reset();
            this.sectionNode = sectionNode;
            int i = SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode));
            int j = SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode));
            int k = SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode));
            this.renderOrigin.set(i, j, k);
            this.bb = new AABB(i, j, k, i + 16, j + 16, k + 16);
        }

        public SectionMesh getSectionMesh() {
            return this.sectionMesh.get();
        }

        public void reset() {
            this.cancelTasks();
            this.sectionMesh.getAndSet(CompiledSectionMesh.UNCOMPILED).close();
            this.dirty = true;
        }

        public BlockPos getRenderOrigin() {
            return this.renderOrigin;
        }

        public long getSectionNode() {
            return this.sectionNode;
        }

        public void setDirty(boolean playerChanged) {
            boolean flag = this.dirty;
            this.dirty = true;
            this.playerChanged = playerChanged | (flag && this.playerChanged);
        }

        public void setNotDirty() {
            this.dirty = false;
            this.playerChanged = false;
        }

        public boolean isDirty() {
            return this.dirty;
        }

        public boolean isDirtyFromPlayer() {
            return this.dirty && this.playerChanged;
        }

        public long getNeighborSectionNode(Direction direction) {
            return SectionPos.offset(this.sectionNode, direction);
        }

        public void resortTransparency(SectionRenderDispatcher dispatcher) {
            if (this.getSectionMesh() instanceof CompiledSectionMesh compiledsectionmesh) {
                this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(compiledsectionmesh);
                dispatcher.schedule(this.lastResortTransparencyTask);
            }
        }

        public boolean hasTranslucentGeometry() {
            return this.getSectionMesh().hasTranslucentGeometry();
        }

        public boolean transparencyResortingScheduled() {
            return this.lastResortTransparencyTask != null && !this.lastResortTransparencyTask.isCompleted.get();
        }

        protected void cancelTasks() {
            if (this.lastRebuildTask != null) {
                this.lastRebuildTask.cancel();
                this.lastRebuildTask = null;
            }

            if (this.lastResortTransparencyTask != null) {
                this.lastResortTransparencyTask.cancel();
                this.lastResortTransparencyTask = null;
            }
        }

        public SectionRenderDispatcher.RenderSection.CompileTask createCompileTask(RenderRegionCache regionCache) {
            this.cancelTasks();
            var additionalRenderers = net.neoforged.neoforge.client.ClientHooks.gatherAdditionalRenderers(this.renderOrigin, SectionRenderDispatcher.this.level);
            RenderSectionRegion rendersectionregion = regionCache.createRegion(SectionRenderDispatcher.this.level, this.sectionNode);
            boolean flag = this.sectionMesh.get() != CompiledSectionMesh.UNCOMPILED;
            this.lastRebuildTask = new SectionRenderDispatcher.RenderSection.RebuildTask(rendersectionregion, flag, additionalRenderers);
            return this.lastRebuildTask;
        }

        public void rebuildSectionAsync(RenderRegionCache regionCache) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(regionCache);
            SectionRenderDispatcher.this.schedule(sectionrenderdispatcher$rendersection$compiletask);
        }

        public void compileSync(RenderRegionCache regionCache) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(regionCache);
            sectionrenderdispatcher$rendersection$compiletask.doTask(SectionRenderDispatcher.this.fixedBuffers);
        }

        void setSectionMesh(SectionMesh sectionMesh) {
            SectionMesh sectionmesh = this.sectionMesh.getAndSet(sectionMesh);
            SectionRenderDispatcher.this.toClose.add(sectionmesh);
            SectionRenderDispatcher.this.renderer.addRecentlyCompiledSection(this);
        }

        VertexSorting createVertexSorting(SectionPos sectionPos) {
            Vec3 vec3 = SectionRenderDispatcher.this.cameraPosition;
            return VertexSorting.byDistance(
                (float)(vec3.x - sectionPos.minBlockX()), (float)(vec3.y - sectionPos.minBlockY()), (float)(vec3.z - sectionPos.minBlockZ())
            );
        }

        // Neo: start

        @Override
        public boolean isEmpty() {
            return !getSectionMesh().hasRenderableLayers();
        }

        // Neo: end

        @OnlyIn(Dist.CLIENT)
        public abstract class CompileTask {
            protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
            protected final AtomicBoolean isCompleted = new AtomicBoolean(false);
            protected final boolean isRecompile;

            public CompileTask(boolean isRecompile) {
                this.isRecompile = isRecompile;
            }

            public abstract CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack sectionBufferBuilderPack);

            public abstract void cancel();

            protected abstract String name();

            public boolean isRecompile() {
                return this.isRecompile;
            }

            public BlockPos getRenderOrigin() {
                return RenderSection.this.renderOrigin;
            }
        }

        @OnlyIn(Dist.CLIENT)
        class RebuildTask extends SectionRenderDispatcher.RenderSection.CompileTask {
            protected final RenderSectionRegion region;
            private final java.util.List<net.neoforged.neoforge.client.event.AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers;

            /**
 * @deprecated Neo: use {@link #RebuildTask(RenderSectionRegion, boolean,
 *             java.util.List)} instead
 */
            @Deprecated
            public RebuildTask(RenderSectionRegion region, boolean isRecompile) {
                this(region, isRecompile, java.util.List.of());
            }

            public RebuildTask(RenderSectionRegion region, boolean isRecompile, java.util.List<net.neoforged.neoforge.client.event.AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers) {
                super(isRecompile);
                this.region = region;
                this.additionalRenderers = additionalRenderers;
            }

            @Override
            protected String name() {
                return "rend_chk_rebuild";
            }

            @Override
            public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack sectionBufferBuilderPack) {
                if (this.isCancelled.get()) {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                } else {
                    long i = RenderSection.this.sectionNode;
                    SectionPos sectionpos = SectionPos.of(i);
                    if (this.isCancelled.get()) {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    } else {
                        SectionCompiler.Results sectioncompiler$results;
                        try (Zone zone = Profiler.get().zone("Compile Section")) {
                            sectioncompiler$results = SectionRenderDispatcher.this.sectionCompiler
                                .compile(sectionpos, this.region, RenderSection.this.createVertexSorting(sectionpos), sectionBufferBuilderPack, this.additionalRenderers);
                        }

                        TranslucencyPointOfView translucencypointofview = TranslucencyPointOfView.of(SectionRenderDispatcher.this.cameraPosition, i);
                        if (this.isCancelled.get()) {
                            sectioncompiler$results.release();
                            return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                        } else {
                            CompiledSectionMesh compiledsectionmesh = new CompiledSectionMesh(translucencypointofview, sectioncompiler$results);
                            CompletableFuture<Void> completablefuture = RenderSection.this.upload(sectioncompiler$results.renderedLayers, compiledsectionmesh);
                            return completablefuture.handle((p_428089_, p_428090_) -> {
                                if (p_428090_ != null && !(p_428090_ instanceof CancellationException) && !(p_428090_ instanceof InterruptedException)) {
                                    Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_428090_, "Rendering section"));
                                }

                                if (!this.isCancelled.get() && !SectionRenderDispatcher.this.closed) {
                                    RenderSection.this.setSectionMesh(compiledsectionmesh);
                                    return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                                } else {
                                    SectionRenderDispatcher.this.toClose.add(compiledsectionmesh);
                                    return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void cancel() {
                if (this.isCancelled.compareAndSet(false, true)) {
                    RenderSection.this.setDirty(false);
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.CompileTask {
            private final CompiledSectionMesh compiledSectionMesh;

            public ResortTransparencyTask(CompiledSectionMesh compiledSectionMesh) {
                super(true);
                this.compiledSectionMesh = compiledSectionMesh;
            }

            @Override
            protected String name() {
                return "rend_chk_sort";
            }

            @Override
            public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack sectionBufferBuilderPack) {
                if (this.isCancelled.get()) {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                } else {
                    MeshData.SortState meshdata$sortstate = this.compiledSectionMesh.getTransparencyState();
                    if (meshdata$sortstate != null && !this.compiledSectionMesh.isEmpty(ChunkSectionLayer.TRANSLUCENT)) {
                        long i = RenderSection.this.sectionNode;
                        VertexSorting vertexsorting = RenderSection.this.createVertexSorting(SectionPos.of(i));
                        TranslucencyPointOfView translucencypointofview = TranslucencyPointOfView.of(SectionRenderDispatcher.this.cameraPosition, i);
                        if (!this.compiledSectionMesh.isDifferentPointOfView(translucencypointofview) && !translucencypointofview.isAxisAligned()) {
                            return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                        } else {
                            ByteBufferBuilder.Result bytebufferbuilder$result = meshdata$sortstate.buildSortedIndexBuffer(
                                sectionBufferBuilderPack.buffer(ChunkSectionLayer.TRANSLUCENT), vertexsorting
                            );
                            if (bytebufferbuilder$result == null) {
                                return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                            } else if (this.isCancelled.get()) {
                                bytebufferbuilder$result.close();
                                return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                            } else {
                                CompletableFuture<Void> completablefuture = RenderSection.this.uploadSectionIndexBuffer(
                                    this.compiledSectionMesh, bytebufferbuilder$result, ChunkSectionLayer.TRANSLUCENT
                                );
                                return completablefuture.handle((p_426929_, p_426930_) -> {
                                    if (p_426930_ != null && !(p_426930_ instanceof CancellationException) && !(p_426930_ instanceof InterruptedException)) {
                                        Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_426930_, "Rendering section"));
                                    }

                                    if (this.isCancelled.get()) {
                                        return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                                    } else {
                                        this.compiledSectionMesh.setTranslucencyPointOfView(translucencypointofview);
                                        return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                                    }
                                });
                            }
                        }
                    } else {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    }
                }
            }

            @Override
            public void cancel() {
                this.isCancelled.set(true);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum SectionTaskResult {
        SUCCESSFUL,
        CANCELLED;
    }
}
