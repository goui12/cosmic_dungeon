package net.minecraft.world.level.chunk.storage;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.PriorityConsecutiveExecutor;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class IOWorker implements ChunkScanAccess, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final AtomicBoolean shutdownRequested = new AtomicBoolean();
    private final PriorityConsecutiveExecutor consecutiveExecutor;
    private final RegionFileStorage storage;
    private final SequencedMap<ChunkPos, IOWorker.PendingStore> pendingWrites = new LinkedHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<CompletableFuture<BitSet>> regionCacheForBlender = new Long2ObjectLinkedOpenHashMap<>();
    private static final int REGION_CACHE_SIZE = 1024;

    protected IOWorker(RegionStorageInfo info, Path folder, boolean sync) {
        this.storage = new RegionFileStorage(info, folder, sync);
        this.consecutiveExecutor = new PriorityConsecutiveExecutor(IOWorker.Priority.values().length, Util.ioPool(), "IOWorker-" + info.type());
    }

    public boolean isOldChunkAround(ChunkPos chunkPos, int radius) {
        ChunkPos chunkpos = new ChunkPos(chunkPos.x - radius, chunkPos.z - radius);
        ChunkPos chunkpos1 = new ChunkPos(chunkPos.x + radius, chunkPos.z + radius);

        for (int i = chunkpos.getRegionX(); i <= chunkpos1.getRegionX(); i++) {
            for (int j = chunkpos.getRegionZ(); j <= chunkpos1.getRegionZ(); j++) {
                BitSet bitset = this.getOrCreateOldDataForRegion(i, j).join();
                if (!bitset.isEmpty()) {
                    ChunkPos chunkpos2 = ChunkPos.minFromRegion(i, j);
                    int k = Math.max(chunkpos.x - chunkpos2.x, 0);
                    int l = Math.max(chunkpos.z - chunkpos2.z, 0);
                    int i1 = Math.min(chunkpos1.x - chunkpos2.x, 31);
                    int j1 = Math.min(chunkpos1.z - chunkpos2.z, 31);

                    for (int k1 = k; k1 <= i1; k1++) {
                        for (int l1 = l; l1 <= j1; l1++) {
                            int i2 = l1 * 32 + k1;
                            if (bitset.get(i2)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private CompletableFuture<BitSet> getOrCreateOldDataForRegion(int chunkX, int chunkZ) {
        long i = ChunkPos.asLong(chunkX, chunkZ);
        synchronized (this.regionCacheForBlender) {
            CompletableFuture<BitSet> completablefuture = this.regionCacheForBlender.getAndMoveToFirst(i);
            if (completablefuture == null) {
                completablefuture = this.createOldDataForRegion(chunkX, chunkZ);
                this.regionCacheForBlender.putAndMoveToFirst(i, completablefuture);
                if (this.regionCacheForBlender.size() > 1024) {
                    this.regionCacheForBlender.removeLast();
                }
            }

            return completablefuture;
        }
    }

    private CompletableFuture<BitSet> createOldDataForRegion(int chunkX, int chunkZ) {
        return CompletableFuture.supplyAsync(
            () -> {
                ChunkPos chunkpos = ChunkPos.minFromRegion(chunkX, chunkZ);
                ChunkPos chunkpos1 = ChunkPos.maxFromRegion(chunkX, chunkZ);
                BitSet bitset = new BitSet();
                ChunkPos.rangeClosed(chunkpos, chunkpos1)
                    .forEach(
                        p_223480_ -> {
                            CollectFields collectfields = new CollectFields(
                                new FieldSelector(IntTag.TYPE, "DataVersion"), new FieldSelector(CompoundTag.TYPE, "blending_data")
                            );

                            try {
                                this.scanChunk(p_223480_, collectfields).join();
                            } catch (Exception exception) {
                                LOGGER.warn("Failed to scan chunk {}", p_223480_, exception);
                                return;
                            }

                            if (collectfields.getResult() instanceof CompoundTag compoundtag && this.isOldChunk(compoundtag)) {
                                int i = p_223480_.getRegionLocalZ() * 32 + p_223480_.getRegionLocalX();
                                bitset.set(i);
                            }
                        }
                    );
                return bitset;
            },
            Util.backgroundExecutor()
        );
    }

    private boolean isOldChunk(CompoundTag chunkData) {
        return chunkData.getIntOr("DataVersion", 0) < 4295 ? true : chunkData.getCompound("blending_data").isPresent();
    }

    public CompletableFuture<Void> store(ChunkPos chunkPos, @Nullable CompoundTag chunkData) {
        return this.store(chunkPos, () -> chunkData);
    }

    public CompletableFuture<Void> store(ChunkPos chunkPos, Supplier<CompoundTag> dataSupplier) {
        return this.<CompletableFuture<Void>>submitTask(() -> {
            CompoundTag compoundtag = dataSupplier.get();
            IOWorker.PendingStore ioworker$pendingstore = this.pendingWrites.computeIfAbsent(chunkPos, p_223488_ -> new IOWorker.PendingStore(compoundtag));
            ioworker$pendingstore.data = compoundtag;
            return ioworker$pendingstore.result;
        }).thenCompose(Function.identity());
    }

    public CompletableFuture<Optional<CompoundTag>> loadAsync(ChunkPos chunkPos) {
        return this.submitThrowingTask(() -> {
            IOWorker.PendingStore ioworker$pendingstore = this.pendingWrites.get(chunkPos);
            if (ioworker$pendingstore != null) {
                return Optional.ofNullable(ioworker$pendingstore.copyData());
            } else {
                try {
                    CompoundTag compoundtag = this.storage.read(chunkPos);
                    return Optional.ofNullable(compoundtag);
                } catch (Exception exception) {
                    LOGGER.warn("Failed to read chunk {}", chunkPos, exception);
                    throw exception;
                }
            }
        });
    }

    public CompletableFuture<Void> synchronize(boolean flushStorage) {
        CompletableFuture<Void> completablefuture = this.<CompletableFuture<Void>>submitTask(
                () -> CompletableFuture.allOf(this.pendingWrites.values().stream().map(p_223475_ -> p_223475_.result).toArray(CompletableFuture[]::new))
            )
            .thenCompose(Function.identity());
        return flushStorage ? completablefuture.thenCompose(p_371174_ -> this.submitThrowingTask(() -> {
            try {
                this.storage.flush();
                return null;
            } catch (Exception exception) {
                LOGGER.warn("Failed to synchronize chunks", (Throwable)exception);
                throw exception;
            }
        })) : completablefuture.thenCompose(p_223477_ -> this.submitTask(() -> null));
    }

    @Override
    public CompletableFuture<Void> scanChunk(ChunkPos chunkPos, StreamTagVisitor visitor) {
        return this.submitThrowingTask(() -> {
            try {
                IOWorker.PendingStore ioworker$pendingstore = this.pendingWrites.get(chunkPos);
                if (ioworker$pendingstore != null) {
                    if (ioworker$pendingstore.data != null) {
                        ioworker$pendingstore.data.acceptAsRoot(visitor);
                    }
                } else {
                    this.storage.scanChunk(chunkPos, visitor);
                }

                return null;
            } catch (Exception exception) {
                LOGGER.warn("Failed to bulk scan chunk {}", chunkPos, exception);
                throw exception;
            }
        });
    }

    private <T> CompletableFuture<T> submitThrowingTask(IOWorker.ThrowingSupplier<T> task) {
        return this.consecutiveExecutor.scheduleWithResult(IOWorker.Priority.FOREGROUND.ordinal(), p_371168_ -> {
            if (!this.shutdownRequested.get()) {
                try {
                    p_371168_.complete(task.get());
                } catch (Exception exception) {
                    p_371168_.completeExceptionally(exception);
                }
            }

            this.tellStorePending();
        });
    }

    private <T> CompletableFuture<T> submitTask(Supplier<T> task) {
        return this.consecutiveExecutor.scheduleWithResult(IOWorker.Priority.FOREGROUND.ordinal(), p_371173_ -> {
            if (!this.shutdownRequested.get()) {
                p_371173_.complete(task.get());
            }

            this.tellStorePending();
        });
    }

    private void storePendingChunk() {
        Entry<ChunkPos, IOWorker.PendingStore> entry = this.pendingWrites.pollFirstEntry();
        if (entry != null) {
            this.runStore(entry.getKey(), entry.getValue());
            this.tellStorePending();
        }
    }

    private void tellStorePending() {
        this.consecutiveExecutor.schedule(new StrictQueue.RunnableWithPriority(IOWorker.Priority.BACKGROUND.ordinal(), this::storePendingChunk));
    }

    private void runStore(ChunkPos chunkPos, IOWorker.PendingStore pendingStore) {
        try {
            this.storage.write(chunkPos, pendingStore.data);
            pendingStore.result.complete(null);
        } catch (Exception exception) {
            LOGGER.error("Failed to store chunk {}", chunkPos, exception);
            pendingStore.result.completeExceptionally(exception);
        }
    }

    @Override
    public void close() throws IOException {
        if (this.shutdownRequested.compareAndSet(false, true)) {
            this.waitForShutdown();
            this.consecutiveExecutor.close();

            try {
                this.storage.close();
            } catch (Exception exception) {
                LOGGER.error("Failed to close storage", (Throwable)exception);
            }
        }
    }

    private void waitForShutdown() {
        this.consecutiveExecutor.scheduleWithResult(IOWorker.Priority.SHUTDOWN.ordinal(), p_371169_ -> p_371169_.complete(Unit.INSTANCE)).join();
    }

    public RegionStorageInfo storageInfo() {
        return this.storage.info();
    }

    static class PendingStore {
        @Nullable
        CompoundTag data;
        final CompletableFuture<Void> result = new CompletableFuture<>();

        public PendingStore(@Nullable CompoundTag data) {
            this.data = data;
        }

        @Nullable
        CompoundTag copyData() {
            CompoundTag compoundtag = this.data;
            return compoundtag == null ? null : compoundtag.copy();
        }
    }

    static enum Priority {
        FOREGROUND,
        BACKGROUND,
        SHUTDOWN;
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        @Nullable
        T get() throws Exception;
    }
}
