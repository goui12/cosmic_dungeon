package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.SectionPos;
import net.minecraft.util.TriState;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;

public abstract class DistanceManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final int PLAYER_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
    final Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk = new Long2ObjectOpenHashMap<>();
    private final LoadingChunkTracker loadingChunkTracker;
    private final SimulationChunkTracker simulationChunkTracker;
    final TicketStorage ticketStorage;
    private final DistanceManager.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter = new DistanceManager.FixedPlayerDistanceChunkTracker(8);
    private final DistanceManager.PlayerTicketTracker playerTicketManager = new DistanceManager.PlayerTicketTracker(32);
    protected final Set<ChunkHolder> chunksToUpdateFutures = new ReferenceOpenHashSet<>();
    final ThrottlingChunkTaskDispatcher ticketDispatcher;
    final LongSet ticketsToRelease = new LongOpenHashSet();
    final Executor mainThreadExecutor;
    private int simulationDistance = 10;

    protected DistanceManager(TicketStorage ticketStorage, Executor dispatcher, Executor mainThreadExecutor) {
        this.ticketStorage = ticketStorage;
        this.loadingChunkTracker = new LoadingChunkTracker(this, ticketStorage);
        this.simulationChunkTracker = new SimulationChunkTracker(ticketStorage);
        TaskScheduler<Runnable> taskscheduler = TaskScheduler.wrapExecutor("player ticket throttler", mainThreadExecutor);
        this.ticketDispatcher = new ThrottlingChunkTaskDispatcher(taskscheduler, dispatcher, 4);
        this.mainThreadExecutor = mainThreadExecutor;
    }

    protected abstract boolean isChunkToRemove(long chunkPos);

    @Nullable
    protected abstract ChunkHolder getChunk(long chunkPos);

    @Nullable
    protected abstract ChunkHolder updateChunkScheduling(long chunkPos, int newLevel, @Nullable ChunkHolder holder, int oldLevel);

    public boolean runAllUpdates(ChunkMap chunkMap) {
        this.naturalSpawnChunkCounter.runAllUpdates();
        this.simulationChunkTracker.runAllUpdates();
        this.playerTicketManager.runAllUpdates();
        int i = Integer.MAX_VALUE - this.loadingChunkTracker.runDistanceUpdates(Integer.MAX_VALUE);
        boolean flag = i != 0;
        if (flag && SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
            LOGGER.debug("DMU {}", i);
        }

        if (!this.chunksToUpdateFutures.isEmpty()) {
            for (ChunkHolder chunkholder1 : this.chunksToUpdateFutures) {
                chunkholder1.updateHighestAllowedStatus(chunkMap);
            }

            for (ChunkHolder chunkholder2 : this.chunksToUpdateFutures) {
                chunkholder2.updateFutures(chunkMap, this.mainThreadExecutor);
            }

            this.chunksToUpdateFutures.clear();
            return true;
        } else {
            if (!this.ticketsToRelease.isEmpty()) {
                LongIterator longiterator = this.ticketsToRelease.iterator();

                while (longiterator.hasNext()) {
                    long j = longiterator.nextLong();
                    if (this.ticketStorage.getTickets(j).stream().anyMatch(p_392783_ -> p_392783_.getType() == TicketType.PLAYER_LOADING)) {
                        ChunkHolder chunkholder = chunkMap.getUpdatingChunkIfPresent(j);
                        if (chunkholder == null) {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<ChunkResult<LevelChunk>> completablefuture = chunkholder.getEntityTickingChunkFuture();
                        completablefuture.thenAccept(p_331640_ -> this.mainThreadExecutor.execute(() -> this.ticketDispatcher.release(j, () -> {}, false)));
                    }
                }

                this.ticketsToRelease.clear();
            }

            return flag;
        }
    }

    public void addPlayer(SectionPos sectionPos, ServerPlayer player) {
        ChunkPos chunkpos = sectionPos.chunk();
        long i = chunkpos.toLong();
        this.playersPerChunk.computeIfAbsent(i, p_183921_ -> new ObjectOpenHashSet<>()).add(player);
        this.naturalSpawnChunkCounter.update(i, 0, true);
        this.playerTicketManager.update(i, 0, true);
        this.ticketStorage.addTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunkpos);
    }

    public void removePlayer(SectionPos sectionPos, ServerPlayer player) {
        ChunkPos chunkpos = sectionPos.chunk();
        long i = chunkpos.toLong();
        ObjectSet<ServerPlayer> objectset = this.playersPerChunk.get(i);
        objectset.remove(player);
        if (objectset.isEmpty()) {
            this.playersPerChunk.remove(i);
            this.naturalSpawnChunkCounter.update(i, Integer.MAX_VALUE, false);
            this.playerTicketManager.update(i, Integer.MAX_VALUE, false);
            this.ticketStorage.removeTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunkpos);
        }
    }

    private int getPlayerTicketLevel() {
        return Math.max(0, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING) - this.simulationDistance);
    }

    public boolean inEntityTickingRange(long chunkPos) {
        return ChunkLevel.isEntityTicking(this.simulationChunkTracker.getLevel(chunkPos));
    }

    public boolean inBlockTickingRange(long chunkPos) {
        return ChunkLevel.isBlockTicking(this.simulationChunkTracker.getLevel(chunkPos));
    }

    public int getChunkLevel(long chunkPos, boolean simulate) {
        return simulate ? this.simulationChunkTracker.getLevel(chunkPos) : this.loadingChunkTracker.getLevel(chunkPos);
    }

    protected void updatePlayerTickets(int viewDistance) {
        this.playerTicketManager.updateViewDistance(viewDistance);
    }

    public void updateSimulationDistance(int simulationDistance) {
        if (simulationDistance != this.simulationDistance) {
            this.simulationDistance = simulationDistance;
            this.ticketStorage.replaceTicketLevelOfType(this.getPlayerTicketLevel(), TicketType.PLAYER_SIMULATION);
        }
    }

    public int getNaturalSpawnChunkCount() {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.size();
    }

    public TriState hasPlayersNearby(long chunkPos) {
        this.naturalSpawnChunkCounter.runAllUpdates();
        int i = this.naturalSpawnChunkCounter.getLevel(chunkPos);
        if (i <= NaturalSpawner.INSCRIBED_SQUARE_SPAWN_DISTANCE_CHUNK) {
            return TriState.TRUE;
        } else {
            return i > 8 ? TriState.FALSE : TriState.DEFAULT;
        }
    }

    public void forEachEntityTickingChunk(LongConsumer action) {
        for (Entry entry : Long2ByteMaps.fastIterable(this.simulationChunkTracker.chunks)) {
            byte b0 = entry.getByteValue();
            long i = entry.getLongKey();
            if (ChunkLevel.isEntityTicking(b0)) {
                action.accept(i);
            }
        }
    }

    public LongIterator getSpawnCandidateChunks() {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.keySet().iterator();
    }

    public String getDebugStatus() {
        return this.ticketDispatcher.getDebugStatus();
    }

    public boolean hasTickets() {
        return this.ticketStorage.hasTickets();
    }

    class FixedPlayerDistanceChunkTracker extends ChunkTracker {
        /**
         * Chunks that are at most {@link #range} chunks away from the closest player.
         */
        protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
        protected final int maxDistance;

        protected FixedPlayerDistanceChunkTracker(int maxDistance) {
            super(maxDistance + 2, 16, 256);
            this.maxDistance = maxDistance;
            this.chunks.defaultReturnValue((byte)(maxDistance + 2));
        }

        @Override
        protected int getLevel(long sectionPos) {
            return this.chunks.get(sectionPos);
        }

        @Override
        protected void setLevel(long sectionPos, int level) {
            byte b0;
            if (level > this.maxDistance) {
                b0 = this.chunks.remove(sectionPos);
            } else {
                b0 = this.chunks.put(sectionPos, (byte)level);
            }

            this.onLevelChange(sectionPos, b0, level);
        }

        /**
         * Called after {@link PlayerChunkTracker#setLevel(long, int)} puts/removes chunk into/from {@link #chunksInRange}.
         *
         * @param oldLevel Previous level of the chunk if it was smaller than {@link #
         *                 range}, {@code range + 2} otherwise.
         */
        protected void onLevelChange(long chunkPos, int oldLevel, int newLevel) {
        }

        @Override
        protected int getLevelFromSource(long pos) {
            return this.havePlayer(pos) ? 0 : Integer.MAX_VALUE;
        }

        private boolean havePlayer(long chunkPos) {
            ObjectSet<ServerPlayer> objectset = DistanceManager.this.playersPerChunk.get(chunkPos);
            return objectset != null && !objectset.isEmpty();
        }

        public void runAllUpdates() {
            this.runUpdates(Integer.MAX_VALUE);
        }
    }

    class PlayerTicketTracker extends DistanceManager.FixedPlayerDistanceChunkTracker {
        private int viewDistance;
        private final Long2IntMap queueLevels = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
        private final LongSet toUpdate = new LongOpenHashSet();

        protected PlayerTicketTracker(int maxDistance) {
            super(maxDistance);
            this.viewDistance = 0;
            this.queueLevels.defaultReturnValue(maxDistance + 2);
        }

        /**
         * Called after {@link PlayerChunkTracker#setLevel(long, int)} puts/removes chunk into/from {@link #chunksInRange}.
         *
         * @param oldLevel Previous level of the chunk if it was smaller than {@link #
         *                 range}, {@code range + 2} otherwise.
         */
        @Override
        protected void onLevelChange(long chunkPos, int oldLevel, int newLevel) {
            this.toUpdate.add(chunkPos);
        }

        public void updateViewDistance(int viewDistance) {
            for (Entry entry : this.chunks.long2ByteEntrySet()) {
                byte b0 = entry.getByteValue();
                long i = entry.getLongKey();
                this.onLevelChange(i, b0, this.haveTicketFor(b0), b0 <= viewDistance);
            }

            this.viewDistance = viewDistance;
        }

        private void onLevelChange(long chunkPos, int level, boolean hadTicket, boolean hasTicket) {
            if (hadTicket != hasTicket) {
                Ticket ticket = new Ticket(TicketType.PLAYER_LOADING, DistanceManager.PLAYER_TICKET_LEVEL);
                if (hasTicket) {
                    DistanceManager.this.ticketDispatcher.submit(() -> DistanceManager.this.mainThreadExecutor.execute(() -> {
                        if (this.haveTicketFor(this.getLevel(chunkPos))) {
                            DistanceManager.this.ticketStorage.addTicket(chunkPos, ticket);
                            DistanceManager.this.ticketsToRelease.add(chunkPos);
                        } else {
                            DistanceManager.this.ticketDispatcher.release(chunkPos, () -> {}, false);
                        }
                    }), chunkPos, () -> level);
                } else {
                    DistanceManager.this.ticketDispatcher
                        .release(
                            chunkPos,
                            () -> DistanceManager.this.mainThreadExecutor.execute(() -> DistanceManager.this.ticketStorage.removeTicket(chunkPos, ticket)),
                            true
                        );
                }
            }
        }

        @Override
        public void runAllUpdates() {
            super.runAllUpdates();
            if (!this.toUpdate.isEmpty()) {
                LongIterator longiterator = this.toUpdate.iterator();

                while (longiterator.hasNext()) {
                    long i = longiterator.nextLong();
                    int j = this.queueLevels.get(i);
                    int k = this.getLevel(i);
                    if (j != k) {
                        DistanceManager.this.ticketDispatcher.onLevelChange(new ChunkPos(i), () -> this.queueLevels.get(i), k, p_140928_ -> {
                            if (p_140928_ >= this.queueLevels.defaultReturnValue()) {
                                this.queueLevels.remove(i);
                            } else {
                                this.queueLevels.put(i, p_140928_);
                            }
                        });
                        this.onLevelChange(i, k, this.haveTicketFor(j), this.haveTicketFor(k));
                    }
                }

                this.toUpdate.clear();
            }
        }

        private boolean haveTicketFor(int level) {
            return level <= this.viewDistance;
        }
    }
}
