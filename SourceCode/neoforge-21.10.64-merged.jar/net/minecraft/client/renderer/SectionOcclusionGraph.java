package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3d;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SectionOcclusionGraph {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
    private static final int MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE = SectionPos.blockToSectionCoord(60);
    private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);
    private boolean needsFullUpdate = true;
    @Nullable
    private Future<?> fullUpdateTask;
    @Nullable
    private ViewArea viewArea;
    private final AtomicReference<SectionOcclusionGraph.GraphState> currentGraph = new AtomicReference<>();
    private final AtomicReference<SectionOcclusionGraph.GraphEvents> nextGraphEvents = new AtomicReference<>();
    private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);

    public void waitAndReset(@Nullable ViewArea viewArea) {
        if (this.fullUpdateTask != null) {
            try {
                this.fullUpdateTask.get();
                this.fullUpdateTask = null;
            } catch (Exception exception) {
                LOGGER.warn("Full update failed", (Throwable)exception);
            }
        }

        this.viewArea = viewArea;
        if (viewArea != null) {
            this.currentGraph.set(new SectionOcclusionGraph.GraphState(viewArea));
            this.invalidate();
        } else {
            this.currentGraph.set(null);
        }
    }

    public void invalidate() {
        this.needsFullUpdate = true;
    }

    public void addSectionsInFrustum(
        Frustum frustum, List<SectionRenderDispatcher.RenderSection> visibleSections, List<SectionRenderDispatcher.RenderSection> nearbyVisibleSections
    ) {
        this.currentGraph.get().storage().sectionTree.visitNodes((p_370300_, p_370301_, p_370302_, p_370303_) -> {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = p_370300_.getSection();
            if (sectionrenderdispatcher$rendersection != null) {
                visibleSections.add(sectionrenderdispatcher$rendersection);
                if (p_370303_) {
                    nearbyVisibleSections.add(sectionrenderdispatcher$rendersection);
                }
            }
        }, frustum, 32);
    }

    public boolean consumeFrustumUpdate() {
        return this.needsFrustumUpdate.compareAndSet(true, false);
    }

    public void onChunkReadyToRender(ChunkPos chunkPos) {
        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();
        if (sectionocclusiongraph$graphevents != null) {
            this.addNeighbors(sectionocclusiongraph$graphevents, chunkPos);
        }

        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;
        if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents) {
            this.addNeighbors(sectionocclusiongraph$graphevents1, chunkPos);
        }
    }

    public void schedulePropagationFrom(SectionRenderDispatcher.RenderSection section) {
        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();
        if (sectionocclusiongraph$graphevents != null) {
            sectionocclusiongraph$graphevents.sectionsToPropagateFrom.add(section);
        }

        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;
        if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents) {
            sectionocclusiongraph$graphevents1.sectionsToPropagateFrom.add(section);
        }
    }

    public void update(boolean smartCull, Camera camera, Frustum frustum, List<SectionRenderDispatcher.RenderSection> visibleSections, LongOpenHashSet loadedEmptySections) {
        Vec3 vec3 = camera.getPosition();
        if (this.needsFullUpdate && (this.fullUpdateTask == null || this.fullUpdateTask.isDone())) {
            this.scheduleFullUpdate(smartCull, camera, vec3, loadedEmptySections);
        }

        this.runPartialUpdate(smartCull, frustum, visibleSections, vec3, loadedEmptySections);
    }

    private void scheduleFullUpdate(boolean smartCull, Camera camera, Vec3 cameraPosition, LongOpenHashSet loadedEmptySections) {
        this.needsFullUpdate = false;
        LongOpenHashSet longopenhashset = loadedEmptySections.clone();
        this.fullUpdateTask = CompletableFuture.runAsync(() -> {
            SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = new SectionOcclusionGraph.GraphState(this.viewArea);
            this.nextGraphEvents.set(sectionocclusiongraph$graphstate.events);
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();
            this.initializeQueueForFullUpdate(camera, queue);
            queue.forEach(p_295724_ -> sectionocclusiongraph$graphstate.storage.sectionToNodeMap.put(p_295724_.section, p_295724_));
            this.runUpdates(sectionocclusiongraph$graphstate.storage, cameraPosition, queue, smartCull, p_294678_ -> {}, longopenhashset);
            this.currentGraph.set(sectionocclusiongraph$graphstate);
            this.nextGraphEvents.set(null);
            this.needsFrustumUpdate.set(true);
        }, Util.backgroundExecutor());
    }

    private void runPartialUpdate(
        boolean smartCull, Frustum p_frustum, List<SectionRenderDispatcher.RenderSection> visibleSections, Vec3 cameraPosition, LongOpenHashSet loadedEmptySections
    ) {
        SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = this.currentGraph.get();
        this.queueSectionsWithNewNeighbors(sectionocclusiongraph$graphstate);
        if (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty()) {
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();

            while (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty()) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$graphstate.events
                    .sectionsToPropagateFrom
                    .poll();
                SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionocclusiongraph$graphstate.storage
                    .sectionToNodeMap
                    .get(sectionrenderdispatcher$rendersection);
                if (sectionocclusiongraph$node != null && sectionocclusiongraph$node.section == sectionrenderdispatcher$rendersection) {
                    queue.add(sectionocclusiongraph$node);
                }
            }

            Frustum frustum = LevelRenderer.offsetFrustum(p_frustum);
            Consumer<SectionRenderDispatcher.RenderSection> consumer = p_370305_ -> {
                if (frustum.isVisible(p_370305_.getBoundingBox())) {
                    this.needsFrustumUpdate.set(true);
                }
            };
            this.runUpdates(sectionocclusiongraph$graphstate.storage, cameraPosition, queue, smartCull, consumer, loadedEmptySections);
        }
    }

    private void queueSectionsWithNewNeighbors(SectionOcclusionGraph.GraphState graphState) {
        LongIterator longiterator = graphState.events.chunksWhichReceivedNeighbors.iterator();

        while (longiterator.hasNext()) {
            long i = longiterator.nextLong();
            List<SectionRenderDispatcher.RenderSection> list = graphState.storage.chunksWaitingForNeighbors.get(i);
            if (list != null && list.get(0).hasAllNeighbors()) {
                graphState.events.sectionsToPropagateFrom.addAll(list);
                graphState.storage.chunksWaitingForNeighbors.remove(i);
            }
        }

        graphState.events.chunksWhichReceivedNeighbors.clear();
    }

    private void addNeighbors(SectionOcclusionGraph.GraphEvents graphEvents, ChunkPos chunkPos) {
        graphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(chunkPos.x - 1, chunkPos.z));
        graphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(chunkPos.x, chunkPos.z - 1));
        graphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(chunkPos.x + 1, chunkPos.z));
        graphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(chunkPos.x, chunkPos.z + 1));
        graphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(chunkPos.x - 1, chunkPos.z - 1));
        graphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(chunkPos.x - 1, chunkPos.z + 1));
        graphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(chunkPos.x + 1, chunkPos.z - 1));
        graphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(chunkPos.x + 1, chunkPos.z + 1));
    }

    private void initializeQueueForFullUpdate(Camera camera, Queue<SectionOcclusionGraph.Node> nodeQueue) {
        BlockPos blockpos = camera.getBlockPosition();
        long i = SectionPos.asLong(blockpos);
        int j = SectionPos.y(i);
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSection(i);
        if (sectionrenderdispatcher$rendersection == null) {
            LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
            boolean flag = j < levelheightaccessor.getMinSectionY();
            int k = flag ? levelheightaccessor.getMinSectionY() : levelheightaccessor.getMaxSectionY();
            int l = this.viewArea.getViewDistance();
            List<SectionOcclusionGraph.Node> list = Lists.newArrayList();
            int i1 = SectionPos.x(i);
            int j1 = SectionPos.z(i);

            for (int k1 = -l; k1 <= l; k1++) {
                for (int l1 = -l; l1 <= l; l1++) {
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.viewArea
                        .getRenderSection(SectionPos.asLong(k1 + i1, k, l1 + j1));
                    if (sectionrenderdispatcher$rendersection1 != null && this.isInViewDistance(i, sectionrenderdispatcher$rendersection1.getSectionNode())) {
                        Direction direction = flag ? Direction.UP : Direction.DOWN;
                        SectionOcclusionGraph.Node sectionocclusiongraph$node = new SectionOcclusionGraph.Node(
                            sectionrenderdispatcher$rendersection1, direction, 0
                        );
                        sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, direction);
                        if (k1 > 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.EAST);
                        } else if (k1 < 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.WEST);
                        }

                        if (l1 > 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.SOUTH);
                        } else if (l1 < 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.NORTH);
                        }

                        list.add(sectionocclusiongraph$node);
                    }
                }
            }

            list.sort(Comparator.comparingDouble(p_392553_ -> blockpos.distSqr(SectionPos.of(p_392553_.section.getSectionNode()).center())));
            nodeQueue.addAll(list);
        } else {
            nodeQueue.add(new SectionOcclusionGraph.Node(sectionrenderdispatcher$rendersection, null, 0));
        }
    }

    private void runUpdates(
        SectionOcclusionGraph.GraphStorage storage,
        Vec3 cameraPosition,
        Queue<SectionOcclusionGraph.Node> queue,
        boolean smartCull,
        Consumer<SectionRenderDispatcher.RenderSection> visibleSectionConsumer,
        LongOpenHashSet loadedEmptySection
    ) {
        SectionPos sectionpos = SectionPos.of(cameraPosition);
        long i = sectionpos.asLong();
        BlockPos blockpos = sectionpos.center();

        while (!queue.isEmpty()) {
            SectionOcclusionGraph.Node sectionocclusiongraph$node = queue.poll();
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$node.section;
            if (!loadedEmptySection.contains(sectionocclusiongraph$node.section.getSectionNode())) {
                if (storage.sectionTree.add(sectionocclusiongraph$node.section)) {
                    visibleSectionConsumer.accept(sectionocclusiongraph$node.section);
                }
            } else {
                sectionocclusiongraph$node.section.sectionMesh.compareAndSet(CompiledSectionMesh.UNCOMPILED, CompiledSectionMesh.EMPTY);
            }

            long j = sectionrenderdispatcher$rendersection.getSectionNode();
            boolean flag = Math.abs(SectionPos.x(j) - sectionpos.x()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE
                || Math.abs(SectionPos.y(j) - sectionpos.y()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE
                || Math.abs(SectionPos.z(j) - sectionpos.z()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE;

            for (Direction direction : DIRECTIONS) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.getRelativeFrom(
                    i, sectionrenderdispatcher$rendersection, direction
                );
                if (sectionrenderdispatcher$rendersection1 != null && (!smartCull || !sectionocclusiongraph$node.hasDirection(direction.getOpposite()))) {
                    if (smartCull && sectionocclusiongraph$node.hasSourceDirections()) {
                        SectionMesh sectionmesh = sectionrenderdispatcher$rendersection.getSectionMesh();
                        boolean flag1 = false;

                        for (int k = 0; k < DIRECTIONS.length; k++) {
                            if (sectionocclusiongraph$node.hasSourceDirection(k) && sectionmesh.facesCanSeeEachother(DIRECTIONS[k].getOpposite(), direction)) {
                                flag1 = true;
                                break;
                            }
                        }

                        if (!flag1) {
                            continue;
                        }
                    }

                    if (smartCull && flag) {
                        int l = SectionPos.sectionToBlockCoord(SectionPos.x(j));
                        int i1 = SectionPos.sectionToBlockCoord(SectionPos.y(j));
                        int j1 = SectionPos.sectionToBlockCoord(SectionPos.z(j));
                        boolean flag2 = direction.getAxis() == Direction.Axis.X ? blockpos.getX() > l : blockpos.getX() < l;
                        boolean flag3 = direction.getAxis() == Direction.Axis.Y ? blockpos.getY() > i1 : blockpos.getY() < i1;
                        boolean flag4 = direction.getAxis() == Direction.Axis.Z ? blockpos.getZ() > j1 : blockpos.getZ() < j1;
                        Vector3d vector3d = new Vector3d(l + (flag2 ? 16 : 0), i1 + (flag3 ? 16 : 0), j1 + (flag4 ? 16 : 0));
                        Vector3d vector3d1 = new Vector3d(cameraPosition.x, cameraPosition.y, cameraPosition.z).sub(vector3d).normalize().mul(CEILED_SECTION_DIAGONAL);
                        boolean flag5 = true;

                        while (vector3d.distanceSquared(cameraPosition.x, cameraPosition.y, cameraPosition.z) > 3600.0) {
                            vector3d.add(vector3d1);
                            LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
                            if (vector3d.y > levelheightaccessor.getMaxY() || vector3d.y < levelheightaccessor.getMinY()) {
                                break;
                            }

                            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 = this.viewArea
                                .getRenderSectionAt(BlockPos.containing(vector3d.x, vector3d.y, vector3d.z));
                            if (sectionrenderdispatcher$rendersection2 == null
                                || storage.sectionToNodeMap.get(sectionrenderdispatcher$rendersection2) == null) {
                                flag5 = false;
                                break;
                            }
                        }

                        if (!flag5) {
                            continue;
                        }
                    }

                    SectionOcclusionGraph.Node sectionocclusiongraph$node1 = storage.sectionToNodeMap.get(sectionrenderdispatcher$rendersection1);
                    if (sectionocclusiongraph$node1 != null) {
                        sectionocclusiongraph$node1.addSourceDirection(direction);
                    } else {
                        SectionOcclusionGraph.Node sectionocclusiongraph$node2 = new SectionOcclusionGraph.Node(
                            sectionrenderdispatcher$rendersection1, direction, sectionocclusiongraph$node.step + 1
                        );
                        sectionocclusiongraph$node2.setDirections(sectionocclusiongraph$node.directions, direction);
                        if (sectionrenderdispatcher$rendersection1.hasAllNeighbors()) {
                            queue.add(sectionocclusiongraph$node2);
                            storage.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                        } else if (this.isInViewDistance(i, sectionrenderdispatcher$rendersection1.getSectionNode())) {
                            storage.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                            long k1 = SectionPos.sectionToChunk(sectionrenderdispatcher$rendersection1.getSectionNode());
                            storage.chunksWaitingForNeighbors.computeIfAbsent(k1, p_294377_ -> new ArrayList<>()).add(sectionrenderdispatcher$rendersection1);
                        }
                    }
                }
            }
        }
    }

    private boolean isInViewDistance(long centerPos, long pos) {
        return ChunkTrackingView.isInViewDistance(
            SectionPos.x(centerPos), SectionPos.z(centerPos), this.viewArea.getViewDistance(), SectionPos.x(pos), SectionPos.z(pos)
        );
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection getRelativeFrom(long sectionPos, SectionRenderDispatcher.RenderSection section, Direction direction) {
        long i = section.getNeighborSectionNode(direction);
        if (!this.isInViewDistance(sectionPos, i)) {
            return null;
        } else {
            return Mth.abs(SectionPos.y(sectionPos) - SectionPos.y(i)) > this.viewArea.getViewDistance() ? null : this.viewArea.getRenderSection(i);
        }
    }

    @Nullable
    @VisibleForDebug
    public SectionOcclusionGraph.Node getNode(SectionRenderDispatcher.RenderSection section) {
        return this.currentGraph.get().storage.sectionToNodeMap.get(section);
    }

    public Octree getOctree() {
        return this.currentGraph.get().storage.sectionTree;
    }

    @OnlyIn(Dist.CLIENT)
    record GraphEvents(LongSet chunksWhichReceivedNeighbors, BlockingQueue<SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom) {
        GraphEvents() {
            this(new LongOpenHashSet(), new LinkedBlockingQueue<>());
        }
    }

    @OnlyIn(Dist.CLIENT)
    record GraphState(SectionOcclusionGraph.GraphStorage storage, SectionOcclusionGraph.GraphEvents events) {
        GraphState(ViewArea p_366764_) {
            this(new SectionOcclusionGraph.GraphStorage(p_366764_), new SectionOcclusionGraph.GraphEvents());
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class GraphStorage {
        public final SectionOcclusionGraph.SectionToNodeMap sectionToNodeMap;
        public final Octree sectionTree;
        public final Long2ObjectMap<List<SectionRenderDispatcher.RenderSection>> chunksWaitingForNeighbors;

        public GraphStorage(ViewArea viewArea) {
            this.sectionToNodeMap = new SectionOcclusionGraph.SectionToNodeMap(viewArea.sections.length);
            this.sectionTree = new Octree(viewArea.getCameraSectionPos(), viewArea.getViewDistance(), viewArea.sectionGridSizeY, viewArea.level.getMinY());
            this.chunksWaitingForNeighbors = new Long2ObjectOpenHashMap<>();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @VisibleForDebug
    public static class Node {
        @VisibleForDebug
        protected final SectionRenderDispatcher.RenderSection section;
        private byte sourceDirections;
        byte directions;
        @VisibleForDebug
        public final int step;

        Node(SectionRenderDispatcher.RenderSection section, @Nullable Direction sourceDirection, int step) {
            this.section = section;
            if (sourceDirection != null) {
                this.addSourceDirection(sourceDirection);
            }

            this.step = step;
        }

        void setDirections(byte currentValue, Direction direction) {
            this.directions = (byte)(this.directions | currentValue | 1 << direction.ordinal());
        }

        boolean hasDirection(Direction direction) {
            return (this.directions & 1 << direction.ordinal()) > 0;
        }

        void addSourceDirection(Direction sourceDirection) {
            this.sourceDirections = (byte)(this.sourceDirections | this.sourceDirections | 1 << sourceDirection.ordinal());
        }

        @VisibleForDebug
        public boolean hasSourceDirection(int direction) {
            return (this.sourceDirections & 1 << direction) > 0;
        }

        boolean hasSourceDirections() {
            return this.sourceDirections != 0;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(this.section.getSectionNode());
        }

        @Override
        public boolean equals(Object other) {
            return !(other instanceof SectionOcclusionGraph.Node sectionocclusiongraph$node)
                ? false
                : this.section.getSectionNode() == sectionocclusiongraph$node.section.getSectionNode();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class SectionToNodeMap {
        private final SectionOcclusionGraph.Node[] nodes;

        SectionToNodeMap(int size) {
            this.nodes = new SectionOcclusionGraph.Node[size];
        }

        public void put(SectionRenderDispatcher.RenderSection section, SectionOcclusionGraph.Node node) {
            this.nodes[section.index] = node;
        }

        @Nullable
        public SectionOcclusionGraph.Node get(SectionRenderDispatcher.RenderSection section) {
            int i = section.index;
            return i >= 0 && i < this.nodes.length ? this.nodes[i] : null;
        }
    }
}
